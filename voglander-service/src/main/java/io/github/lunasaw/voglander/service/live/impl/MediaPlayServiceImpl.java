package io.github.lunasaw.voglander.service.live.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.service.live.LiveSessionInfo;
import io.github.lunasaw.voglander.service.live.LiveStreamRegistry;
import io.github.lunasaw.voglander.service.live.MediaPlayService;
import io.github.lunasaw.voglander.service.live.dto.LivePlayDTO;
import io.github.lunasaw.voglander.service.live.dto.LiveStartDTO;
import io.github.lunasaw.voglander.service.live.protocol.MediaEstablishContext;
import io.github.lunasaw.voglander.service.live.protocol.MediaEstablishResult;
import io.github.lunasaw.voglander.service.live.protocol.MediaProtocolHandler;
import io.github.lunasaw.voglander.service.live.protocol.MediaProtocolRouter;
import io.github.lunasaw.voglander.service.live.protocol.MediaTerminateContext;
import io.github.lunasaw.voglander.service.sse.SseEvent;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.PlayUrl;
import io.github.lunasaw.zlm.entity.ServerResponse;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import io.github.lunasaw.zlm.node.NodeSupplier;
import io.github.lunasaw.zlm.node.service.NodeService;
import lombok.extern.slf4j.Slf4j;

/**
 * 媒体播放编排服务实现（直播）。
 *
 * @author luna
 */
@Slf4j
@Service
public class MediaPlayServiceImpl implements MediaPlayService {

    /** ZLM 收流 app（直播流统一落在 rtp 应用下） */
    private static final String  ZLM_APP             = "rtp";
    /** 首播等待流就绪超时（秒）。需 &lt; LOCK_HOLD_SEC(20)。8s 对 ffmpeg/真实摄像头冷启动偏紧，取 15s 留足余量 */
    private static final int     FUTURE_TIMEOUT_SEC  = 15;
    /** 会话保活 TTL（秒） */
    private static final long    KEEPALIVE_SEC       = 3600;
    /** 分布式锁持有时间（秒），需大于首播全流程耗时 */
    private static final int     LOCK_HOLD_SEC       = 20;
    /** 获取锁等待时间（秒） */
    private static final int     LOCK_WAIT_SEC       = 3;
    /**
     * refCount 归零后延迟回收窗口（秒）。
     * <p>
     * 必须 &gt; GC 间隔（{@link io.github.lunasaw.voglander.service.live.LiveSessionGcService} 的 60s），
     * 否则 key 常在 GC tick 前过期 → {@code drainPendingClose} 扫不到 → 永不真实关流。取 90s 保证至少一次 tick 命中；
     * TTL 仅作多节点兜底，不再是回收主路径。
     */
    private static final int     PENDING_CLOSE_SEC   = 90;

    private static final String  LOCK_PREFIX         = "live:lock:";
    private static final String  PENDING_CLOSE_PREFIX = "live:pending_close:";
    /** 关流去重锁前缀。ZLM onStreamChanged(regist=false) 按 schema(hls/rtmp/fmp4/ts/rtsp) 各回调一次，
     *  5 个 hook 线程并发进 closeStream，靠本锁收敛为单次真实收尾，其余直接短路。 */
    private static final String  CLOSE_LOCK_PREFIX   = "live:close:";
    /** 关流锁持有时间（秒）：覆盖单次 closeRtpServer + sendBye + forceClose 全流程。 */
    private static final int     CLOSE_LOCK_HOLD_SEC = 10;

    @Autowired
    private NodeService                 nodeService;
    @Autowired
    private NodeSupplier                nodeSupplier;
    @Autowired
    private MediaSessionManager         mediaSessionManager;
    @Autowired
    private LiveStreamRegistry          liveStreamRegistry;
    @Autowired
    private MediaProtocolRouter         mediaProtocolRouter;
    @Autowired
    private RedisLockUtil               redisLockUtil;
    @Autowired
    private SseEventBus                 sseEventBus;
    @Autowired
    private StringRedisTemplate         stringRedisTemplate;

    /**
     * 复用前可选探活开关（默认关闭，不污染热路径）。
     * <p>
     * 开启后，命中 ACTIVE 缓存的复用分支返回前会按会话原节点向 ZLM {@code isMediaOnline} 探活一次，
     * 应对"S1 下线回调 + S2 GC 对账都还没跑到、但缓存已死"的极小窗口。每次复用加一次 ZLM 往返，
     * 对延迟敏感场景默认不开；S1（秒级）+ S2（分钟级）已覆盖绝大多数死流。
     * </p>
     */
    @Value("${live.reuse-verify-enabled:false}")
    private boolean                     reuseVerifyEnabled;

    @Override
    public LivePlayDTO startLive(LiveStartDTO dto) {
        Assert.notNull(dto, "直播请求不能为空");
        Assert.hasText(dto.getDeviceId(), "deviceId不能为空");
        Assert.hasText(dto.getChannelId(), "channelId不能为空");

        String streamId = buildLiveStreamId(dto.getDeviceId(), dto.getChannelId());
        String lockKey = LOCK_PREFIX + streamId;
        String lockValue = redisLockUtil.generateLockValue();

        boolean locked = Boolean.TRUE.equals(redisLockUtil.tryLock(lockKey, lockValue, LOCK_HOLD_SEC, LOCK_WAIT_SEC));
        if (!locked) {
            // 未拿到锁：可能另一线程正在首播，尝试复用已就绪会话
            LiveSessionInfo session = liveStreamRegistry.getSession(streamId);
            if (session != null && isActive(session.getStatus())) {
                liveStreamRegistry.incRef(streamId);
                return buildDTO(streamId, session);
            }
            throw new ServiceException(ServiceExceptionEnum.LIVE_INVITE_TIMEOUT);
        }

        try {
            // 1. 多路复用：已有 ACTIVE 会话直接复用，秒返回
            LiveSessionInfo existing = liveStreamRegistry.getSession(streamId);
            if (existing != null && isActive(existing.getStatus())) {
                if (reuseVerifyEnabled && !reuseStreamAlive(existing, streamId)) {
                    // 缓存已死（探活查无），清掉，落入下方首播重建
                    liveStreamRegistry.remove(streamId);
                } else {
                    liveStreamRegistry.incRef(streamId);
                    liveStreamRegistry.keepAlive(streamId, KEEPALIVE_SEC);
                    return buildDTO(streamId, existing);
                }
            }

            // 2. 选节点（负载均衡），亲和由 nodeServerId 持久化承载
            ZlmNode node = selectNode();
            if (node == null) {
                throw new ServiceException(ServiceExceptionEnum.LIVE_NODE_UNAVAILABLE);
            }

            // 3. 按设备协议解析媒体协议处理器（S5：新增协议只需新增 handler，本编排零改动）
            MediaProtocolHandler handler = mediaProtocolRouter.resolveForDevice(dto.getDeviceId());
            if (handler == null) {
                throw new ServiceException(ServiceExceptionEnum.PARAM_ERROR, "设备无对应媒体协议处理器: " + dto.getDeviceId());
            }

            // 4. 预写 INVITING 占位会话（callId 暂用 streamId，建流成功后回填真实 callId）
            writePlaceholder(streamId, dto, node.getServerId());

            // 5. 注册首播等待 future（先注册，避免 onStreamChanged 先到找不到 future）
            CompletableFuture<Void> future = new CompletableFuture<>();
            liveStreamRegistry.registerFuture(streamId, future);

            // 6. 建流：协议特定地让流进 ZLM（GB28181: openRtpServer + INVITE，返回真实 Call-ID）
            MediaEstablishContext establishCtx = MediaEstablishContext.builder()
                .node(node)
                .streamId(streamId)
                .deviceId(dto.getDeviceId())
                .channelId(dto.getChannelId())
                .streamMode(dto.getStreamMode())
                .build();
            MediaEstablishResult establishResult = handler.establish(establishCtx);
            if (establishResult == null || !establishResult.isSuccess()) {
                cleanupFailed(streamId, node, handler);
                throw new ServiceException(ServiceExceptionEnum.LIVE_INVITE_TIMEOUT);
            }
            // 6.1 即刻把占位行 callId 回填为真实会话标识（关流据此终止，不依赖异步 InviteOk）
            String realCallId = establishResult.getCallId();
            if (realCallId != null && !realCallId.isBlank()) {
                try {
                    mediaSessionManager.backfillCallIdByStreamId(streamId, realCallId);
                } catch (Exception e) {
                    log.warn("回填真实 callId 失败, streamId={}: {}", streamId, e.getMessage());
                }
            }

            // 7. 等待流就绪（ZLM on_stream_changed 触发 future）
            try {
                future.get(FUTURE_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                cleanupFailed(streamId, node, handler);
                sseEventBus.publish(new SseEvent("live.failed",
                    java.util.Map.of("streamId", streamId, "reason", "timeout")));
                throw new ServiceException(ServiceExceptionEnum.LIVE_INVITE_TIMEOUT);
            } catch (Exception e) {
                cleanupFailed(streamId, node, handler);
                throw new ServiceException(ServiceExceptionEnum.STREAM_NOT_READY, e.getMessage());
            }

            // 8. 拉 PlayUrls
            PlayUrl playUrl = fetchPlayUrls(node, streamId);

            // 9. 写 Registry + 引用计数 + 续约
            LiveSessionInfo info = new LiveSessionInfo();
            info.setNodeServerId(node.getServerId());
            info.setSdpIp(establishResult.getSdpIp());
            info.setRtpPort(establishResult.getRtpPort() == null ? 0 : establishResult.getRtpPort());
            info.setStatus(MediaSessionConstant.Status.ACTIVE);
            info.setSessionType(MediaSessionConstant.Type.PLAY);
            info.setCreateMs(System.currentTimeMillis());
            info.setPlayUrlsJson(playUrl == null ? null : JSON.toJSONString(playUrl));
            liveStreamRegistry.putSession(streamId, info);
            liveStreamRegistry.incRef(streamId);
            liveStreamRegistry.keepAlive(streamId, KEEPALIVE_SEC);

            log.info("直播首播建立成功, streamId={}, node={}, rtpPort={}", streamId, node.getServerId(),
                establishResult.getRtpPort());
            return buildDTO(streamId, info);
        } finally {
            redisLockUtil.unLock(lockKey, lockValue);
        }
    }

    @Override
    public boolean stopLive(String streamId) {
        Assert.hasText(streamId, "streamId不能为空");
        long ref = liveStreamRegistry.decRef(streamId);
        if (ref > 0) {
            // 仍有观看者，保活
            liveStreamRegistry.keepAlive(streamId, KEEPALIVE_SEC);
            return true;
        }
        // 归零：标记延迟回收，真正 BYE 由 GC drainPendingClose 执行
        stringRedisTemplate.opsForValue().set(
            PENDING_CLOSE_PREFIX + streamId, "1", PENDING_CLOSE_SEC, TimeUnit.SECONDS);
        log.info("直播引用计数归零，标记延迟回收, streamId={}", streamId);
        return true;
    }

    @Override
    public LivePlayDTO getLive(String streamId) {
        Assert.hasText(streamId, "streamId不能为空");
        LiveSessionInfo session = liveStreamRegistry.getSession(streamId);
        if (session == null) {
            throw new ServiceException(ServiceExceptionEnum.LIVE_STREAM_NOT_FOUND);
        }
        return buildDTO(streamId, session);
    }

    @Override
    public void keepAlive(String streamId) {
        Assert.hasText(streamId, "streamId不能为空");
        liveStreamRegistry.keepAlive(streamId, KEEPALIVE_SEC);
        // 续约即取消延迟回收标记
        stringRedisTemplate.delete(PENDING_CLOSE_PREFIX + streamId);
    }

    @Override
    public void closeStream(String streamId) {
        closeStream(streamId, "idle_gc");
    }

    @Override
    public void closeStream(String streamId, String reason) {
        Assert.hasText(streamId, "streamId不能为空");
        String sseReason = (reason == null || reason.isBlank()) ? "idle_gc" : reason;

        // 关流去重：ZLM 多 schema 并发回调下，仅首个抢到锁的线程执行真实收尾，其余短路。
        // 抢锁失败即视为"已有线程在关"，直接返回（幂等：真正的 BYE/forceClose 只发一次）。
        // 用单次非阻塞 lock（非 tryLock 退避重试），抢不到立即放弃。
        String closeLockKey = CLOSE_LOCK_PREFIX + streamId;
        String closeLockValue = redisLockUtil.generateLockValue();
        boolean closeLocked = Boolean.TRUE.equals(
            redisLockUtil.lock(closeLockKey, closeLockValue, CLOSE_LOCK_HOLD_SEC));
        if (!closeLocked) {
            log.debug("[closeStream] 已有线程在关流，跳过重复收尾, streamId={}, reason={}", streamId, sseReason);
            return;
        }
        try {
            doCloseStream(streamId, sseReason);
        } finally {
            redisLockUtil.unLock(closeLockKey, closeLockValue);
        }
    }

    /**
     * 关流真实收尾（已被 {@link #closeStream(String, String)} 的去重锁保护，单次执行）。
     */
    private void doCloseStream(String streamId, String sseReason) {
        MediaSessionDTO session = mediaSessionManager.getByStreamId(streamId);

        // 1. 协议特定收尾：关收流端 + 终止协议会话（GB28181: closeRtpServer + sendBye）。
        //    按设备协议解析 handler；解析失败（设备已删等）跳过协议收尾，不阻断后续幂等清理。
        if (session != null) {
            MediaProtocolHandler handler = mediaProtocolRouter.resolveForDevice(session.getDeviceId());
            if (handler != null) {
                ZlmNode node = null;
                if (session.getNodeServerId() != null) {
                    try {
                        node = nodeService.getAvailableNode(session.getNodeServerId());
                    } catch (Exception e) {
                        log.warn("[closeStream] 解析会话节点失败, streamId={}: {}", streamId, e.getMessage());
                    }
                }
                try {
                    handler.terminate(MediaTerminateContext.builder()
                        .node(node)
                        .streamId(streamId)
                        .callId(session.getCallId())
                        .reason(sseReason)
                        .build());
                } catch (Exception e) {
                    log.warn("[closeStream] 协议收尾失败, streamId={}: {}", streamId, e.getMessage());
                }
            } else {
                log.warn("[closeStream] 无对应媒体���议处理器，跳过协议收尾, streamId={}, deviceId={}",
                    streamId, session.getDeviceId());
            }
        }

        // 2. 标会话 CLOSED
        if (session != null && session.getId() != null) {
            try {
                mediaSessionManager.forceClose(session.getId());
            } catch (Exception e) {
                log.warn("[closeStream] 标记会话 CLOSED 失败, streamId={}: {}", streamId, e.getMessage());
            }
        }

        // 3. SSE live.closed（reason 透传来源）+ 4. 清 Registry + 5. 删 pending_close key（幂等收尾，总是执行）
        sseEventBus.publish(new SseEvent("live.closed",
            java.util.Map.of("streamId", streamId, "reason", sseReason)));
        liveStreamRegistry.remove(streamId);
        stringRedisTemplate.delete(PENDING_CLOSE_PREFIX + streamId);
        log.info("[closeStream] 关流完成, streamId={}, reason={}", streamId, sseReason);
    }

    // ================================
    // 私有辅助
    // ================================

    private String buildLiveStreamId(String deviceId, String channelId) {
        return "gb_live_" + deviceId + "_" + channelId;
    }

    private boolean isActive(Integer status) {
        return status != null && status == MediaSessionConstant.Status.ACTIVE;
    }

    /**
     * 选节点：负载均衡主选；主选不可用时按 weight 降序从候选 enabled 节点兜底（S6.1 故障转移）。
     * 节点亲和通过会话持久化的 nodeServerId 承载，首播按 LB 落点。
     */
    private ZlmNode selectNode() {
        ZlmNode primary = null;
        try {
            primary = nodeService.selectNode();
        } catch (Exception e) {
            log.warn("负载均衡选节点失败，尝试候选兜底: {}", e.getMessage());
        }
        if (primary != null) {
            return primary;
        }
        // 主选为空：按 weight 降序取候选 enabled 节点（单节点故障不再直接打断点播）
        List<ZlmNode> candidates = null;
        try {
            candidates = nodeSupplier.getNodes();
        } catch (Exception e) {
            log.warn("获取候选节点列表失败: {}", e.getMessage());
        }
        ZlmNode fallback = chooseNode(null, candidates);
        if (fallback != null) {
            log.warn("主选节点不可用，故障转移到候选节点: serverId={}, weight={}",
                fallback.getServerId(), fallback.getWeight());
        }
        return fallback;
    }

    /**
     * 节点选择纯逻辑（可单测）：主选命中即用；否则从候选按 weight 降序取首个 enabled。
     *
     * @param primary    负载均衡主选（可空）
     * @param candidates 候选节点列表（可空）
     * @return 选中节点，全不可用返回 null
     */
    static ZlmNode chooseNode(ZlmNode primary, List<ZlmNode> candidates) {
        if (primary != null) {
            return primary;
        }
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
            .filter(java.util.Objects::nonNull)
            .filter(ZlmNode::isEnabled)
            .max(java.util.Comparator.comparingInt(ZlmNode::getWeight))
            .orElse(null);
    }

    private void writePlaceholder(String streamId, LiveStartDTO dto, String nodeServerId) {
        // 清理可能残留的同 streamId 旧行（Redis 会话已过期但 DB 行未清），避免 UNIQUE 冲突
        MediaSessionDTO old = mediaSessionManager.getByStreamId(streamId);
        if (old != null) {
            MediaSessionDTO del = new MediaSessionDTO();
            del.setId(old.getId());
            mediaSessionManager.deleteOne(del);
        }
        MediaSessionDTO placeholder = new MediaSessionDTO();
        placeholder.setCallId(streamId);
        placeholder.setStreamId(streamId);
        placeholder.setDeviceId(dto.getDeviceId());
        placeholder.setChannelId(dto.getChannelId());
        placeholder.setNodeServerId(nodeServerId);
        placeholder.setStatus(MediaSessionConstant.Status.INVITING);
        placeholder.setSessionType(MediaSessionConstant.Type.PLAY);
        placeholder.setRefCount(0);
        mediaSessionManager.add(placeholder);
    }

    private PlayUrl fetchPlayUrls(ZlmNode node, String streamId) {
        try {
            MediaReq mediaReq = new MediaReq();
            mediaReq.setApp(ZLM_APP);
            mediaReq.setStream(streamId);
            ServerResponse<PlayUrl> resp = ZlmRestService.getPlaybackUrls(node.getHost(), node.getSecret(), mediaReq);
            if (resp != null && resp.getCode() != null && resp.getCode() == 0) {
                return resp.getData();
            }
            log.warn("getPlaybackUrls 返回非成功, streamId={}, resp={}", streamId, resp);
        } catch (Exception e) {
            log.warn("getPlaybackUrls 异常, streamId={}: {}", streamId, e.getMessage());
        }
        return null;
    }

    /**
     * 复用前探活：按会话<b>原节点</b>（{@code info.nodeServerId}，而非重新负载均衡）向 ZLM
     * {@code isMediaOnline} 核实流是否存活。无节点信息 / 查询异常时保守返回 true（保留复用，不误判）。
     * 与 GC 对账同款判活语义，仅用于 {@code reuseVerifyEnabled} 开启时。
     */
    private boolean reuseStreamAlive(LiveSessionInfo info, String streamId) {
        try {
            String serverId = info.getNodeServerId();
            if (serverId == null) {
                return true;
            }
            ZlmNode node = nodeService.getAvailableNode(serverId);
            if (node == null) {
                return true;
            }
            MediaReq req = new MediaReq();
            req.setApp(ZLM_APP);
            req.setStream(streamId);
            MediaOnlineStatus st = ZlmRestService.isMediaOnline(node.getHost(), node.getSecret(), req);
            return st != null && Boolean.TRUE.equals(st.getOnline());
        } catch (Exception e) {
            log.warn("[reuse] 探活异常，保守复用, streamId={}: {}", streamId, e.getMessage());
            return true;
        }
    }

    private void cleanupFailed(String streamId, ZlmNode node, MediaProtocolHandler handler) {
        // 协议特定收尾（建流失败回滚）：关收流端 + 终止会话。callId 此时多为空（建流即失败），terminate 幂等跳过。
        try {
            MediaSessionDTO row = mediaSessionManager.getByStreamId(streamId);
            String callId = row != null ? row.getCallId() : null;
            handler.terminate(MediaTerminateContext.builder()
                .node(node).streamId(streamId).callId(callId).reason("establish_failed").build());
        } catch (Exception e) {
            log.warn("建流失败回滚协议收尾异常, streamId={}: {}", streamId, e.getMessage());
        }
        try {
            MediaSessionDTO row = mediaSessionManager.getByStreamId(streamId);
            if (row != null) {
                mediaSessionManager.onInviteFailure(row.getCallId(), 408);
            }
        } catch (Exception e) {
            log.warn("标记会话失败状态异常, streamId={}: {}", streamId, e.getMessage());
        }
        liveStreamRegistry.remove(streamId);
    }

    private LivePlayDTO buildDTO(String streamId, LiveSessionInfo info) {
        LivePlayDTO dto = new LivePlayDTO();
        dto.setStreamId(streamId);
        dto.setCallId(info.getCallId());
        dto.setStatus(info.getStatus());
        dto.setRefCount(liveStreamRegistry.getRef(streamId));
        if (info.getPlayUrlsJson() != null) {
            try {
                dto.setPlayUrls(JSON.parseObject(info.getPlayUrlsJson(), PlayUrl.class));
            } catch (Exception ignored) {
            }
        }
        return dto;
    }
}
