package io.github.lunasaw.voglander.service.live.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.service.live.LiveSessionInfo;
import io.github.lunasaw.voglander.service.live.LiveStreamRegistry;
import io.github.lunasaw.voglander.service.live.MediaPlayService;
import io.github.lunasaw.voglander.service.live.dto.LivePlayDTO;
import io.github.lunasaw.voglander.service.live.dto.LiveStartDTO;
import io.github.lunasaw.voglander.service.sse.SseEvent;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.PlayUrl;
import io.github.lunasaw.zlm.entity.ServerResponse;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import io.github.lunasaw.zlm.entity.rtp.OpenRtpServerReq;
import io.github.lunasaw.zlm.entity.rtp.OpenRtpServerResult;
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

    @Autowired
    private NodeService                 nodeService;
    @Autowired
    private MediaNodeManager            mediaNodeManager;
    @Autowired
    private MediaSessionManager         mediaSessionManager;
    @Autowired
    private LiveStreamRegistry          liveStreamRegistry;
    @Autowired
    private VoglanderServerMediaCommand voglanderServerMediaCommand;
    @Autowired
    private RedisLockUtil               redisLockUtil;
    @Autowired
    private SseEventBus                 sseEventBus;
    @Autowired
    private StringRedisTemplate         stringRedisTemplate;

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
                liveStreamRegistry.incRef(streamId);
                liveStreamRegistry.keepAlive(streamId, KEEPALIVE_SEC);
                return buildDTO(streamId, existing);
            }

            // 2. 选节点（负载均衡），亲和由 nodeServerId 持久化承载
            ZlmNode node = selectNode();
            if (node == null) {
                throw new ServiceException(ServiceExceptionEnum.LIVE_NODE_UNAVAILABLE);
            }

            // 3. 解析下发给设备的媒体 IP（NAT 环境可由 tb_media_node.extend.mediaIp 覆盖）
            String sdpIp = resolveMediaIp(node);

            // 4. 开 RTP 接收端口
            OpenRtpServerReq rtpReq = new OpenRtpServerReq();
            rtpReq.setPort(0);
            rtpReq.setTcpMode(toTcpMode(dto.getStreamMode()));
            rtpReq.setStreamId(streamId);
            OpenRtpServerResult rtpResult = ZlmRestService.openRtpServer(node.getHost(), node.getSecret(), rtpReq);
            if (rtpResult == null || rtpResult.getPort() == null) {
                throw new ServiceException(ServiceExceptionEnum.ZLM_UNAVAILABLE, "openRtpServer 失败");
            }
            int rtpPort = Integer.parseInt(rtpResult.getPort());

            // 5. 预写 INVITING 占位会话（callId 暂用 streamId，InviteOk 回填真实 callId）
            writePlaceholder(streamId, dto, node.getServerId());

            // 6. 注册首播等待 future（先注册，避免 onStreamChanged 先到找不到 future）
            CompletableFuture<Void> future = new CompletableFuture<>();
            liveStreamRegistry.registerFuture(streamId, future);

            // 7. 发 INVITE
            ResultDTO<Void> inviteResult = voglanderServerMediaCommand.inviteRealTimePlay(
                dto.getDeviceId(), sdpIp, rtpPort, toStreamModeEnum(dto.getStreamMode()));
            if (inviteResult == null || !inviteResult.isSuccess()) {
                cleanupFailed(streamId, node);
                throw new ServiceException(ServiceExceptionEnum.LIVE_INVITE_TIMEOUT);
            }

            // 8. 等待流就绪（ZLM on_stream_changed 触发 future）
            try {
                future.get(FUTURE_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                cleanupFailed(streamId, node);
                sseEventBus.publish(new SseEvent("live.failed",
                    java.util.Map.of("streamId", streamId, "reason", "timeout")));
                throw new ServiceException(ServiceExceptionEnum.LIVE_INVITE_TIMEOUT);
            } catch (Exception e) {
                cleanupFailed(streamId, node);
                throw new ServiceException(ServiceExceptionEnum.STREAM_NOT_READY, e.getMessage());
            }

            // 9. 拉 PlayUrls
            PlayUrl playUrl = fetchPlayUrls(node, streamId);

            // 10. 写 Registry + 引用计数 + 续约
            LiveSessionInfo info = new LiveSessionInfo();
            info.setNodeServerId(node.getServerId());
            info.setSdpIp(sdpIp);
            info.setRtpPort(rtpPort);
            info.setStatus(MediaSessionConstant.Status.ACTIVE);
            info.setSessionType(MediaSessionConstant.Type.PLAY);
            info.setCreateMs(System.currentTimeMillis());
            info.setPlayUrlsJson(playUrl == null ? null : JSON.toJSONString(playUrl));
            liveStreamRegistry.putSession(streamId, info);
            liveStreamRegistry.incRef(streamId);
            liveStreamRegistry.keepAlive(streamId, KEEPALIVE_SEC);

            log.info("直播首播建立成功, streamId={}, node={}, rtpPort={}", streamId, node.getServerId(), rtpPort);
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
        Assert.hasText(streamId, "streamId不能为空");
        MediaSessionDTO session = mediaSessionManager.getByStreamId(streamId);

        // 1. 真实 closeRtpServer 到会话所在节点（解析失败/无会话则跳过，不阻断收尾）
        if (session != null && session.getNodeServerId() != null) {
            try {
                ZlmNode node = nodeService.getAvailableNode(session.getNodeServerId());
                if (node != null) {
                    ZlmRestService.closeRtpServer(node.getHost(), node.getSecret(), streamId);
                }
            } catch (Exception e) {
                log.warn("[closeStream] closeRtpServer 失败, streamId={}: {}", streamId, e.getMessage());
            }
        }

        // 2. 真实 BYE（callId 由 InviteOk 回填，可能为空）
        if (session != null && session.getCallId() != null) {
            try {
                voglanderServerMediaCommand.sendBye(session.getCallId());
            } catch (Exception e) {
                log.warn("[closeStream] sendBye 失败, streamId={}: {}", streamId, e.getMessage());
            }
        }

        // 3. 标会话 CLOSED
        if (session != null && session.getId() != null) {
            try {
                mediaSessionManager.forceClose(session.getId());
            } catch (Exception e) {
                log.warn("[closeStream] 标记会话 CLOSED 失败, streamId={}: {}", streamId, e.getMessage());
            }
        }

        // 4. SSE live.closed + 5. 清 Registry + 6. 删 pending_close key（幂等收尾，总是执行）
        sseEventBus.publish(new SseEvent("live.closed",
            java.util.Map.of("streamId", streamId, "reason", "idle_gc")));
        liveStreamRegistry.remove(streamId);
        stringRedisTemplate.delete(PENDING_CLOSE_PREFIX + streamId);
        log.info("[closeStream] 空闲流真实关流完成, streamId={}", streamId);
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
     * 选节点：负载均衡选择。节点亲和通过会话持久化的 nodeServerId 承载，首播按 LB 落点。
     */
    private ZlmNode selectNode() {
        try {
            return nodeService.selectNode();
        } catch (Exception e) {
            log.warn("选择媒体节点失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析下发给设备的媒体 IP：优先 tb_media_node.extend.mediaIp（NAT 覆盖），否则由节点 host 提取 IP。
     */
    private String resolveMediaIp(ZlmNode node) {
        MediaNodeDTO mediaNode = null;
        try {
            mediaNode = mediaNodeManager.getDTOByServerId(node.getServerId());
        } catch (Exception ignored) {
        }
        if (mediaNode != null) {
            if (mediaNode.getExtend() != null && !mediaNode.getExtend().isBlank()) {
                try {
                    JSONObject ext = JSON.parseObject(mediaNode.getExtend());
                    String mediaIp = ext.getString("mediaIp");
                    if (mediaIp != null && !mediaIp.isBlank()) {
                        return mediaIp;
                    }
                } catch (Exception ignored) {
                }
            }
            if (mediaNode.getHost() != null && !mediaNode.getHost().isBlank()) {
                return stripHostToIp(mediaNode.getHost());
            }
        }
        return stripHostToIp(node.getHost());
    }

    /**
     * 从形如 http://1.2.3.4:9092 / 1.2.3.4:9092 / 1.2.3.4 的串中提取主机 IP。
     */
    private String stripHostToIp(String host) {
        if (host == null) {
            return null;
        }
        String h = host;
        int scheme = h.indexOf("://");
        if (scheme >= 0) {
            h = h.substring(scheme + 3);
        }
        int slash = h.indexOf('/');
        if (slash >= 0) {
            h = h.substring(0, slash);
        }
        int colon = h.lastIndexOf(':');
        if (colon >= 0) {
            h = h.substring(0, colon);
        }
        return h;
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

    private void cleanupFailed(String streamId, ZlmNode node) {
        try {
            ZlmRestService.closeRtpServer(node.getHost(), node.getSecret(), streamId);
        } catch (Exception e) {
            log.warn("closeRtpServer 清理失败, streamId={}: {}", streamId, e.getMessage());
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

    private int toTcpMode(String streamMode) {
        if (streamMode == null) {
            return 0;
        }
        String m = streamMode.toUpperCase().replace('-', '_');
        switch (m) {
            case "TCP_PASSIVE":
                return 1;
            case "TCP_ACTIVE":
                return 2;
            default:
                return 0;
        }
    }

    private StreamModeEnum toStreamModeEnum(String streamMode) {
        if (streamMode == null) {
            return StreamModeEnum.UDP;
        }
        String m = streamMode.toUpperCase().replace('-', '_');
        switch (m) {
            case "TCP_PASSIVE":
                return StreamModeEnum.TCP_PASSIVE;
            case "TCP_ACTIVE":
                return StreamModeEnum.TCP_ACTIVE;
            default:
                return StreamModeEnum.UDP;
        }
    }
}
