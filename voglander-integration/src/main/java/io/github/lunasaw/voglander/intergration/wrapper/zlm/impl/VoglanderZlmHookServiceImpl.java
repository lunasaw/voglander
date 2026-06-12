package io.github.lunasaw.voglander.intergration.wrapper.zlm.impl;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.common.event.NodeExitedEvent;
import io.github.lunasaw.voglander.common.event.StreamOfflineEvent;
import io.github.lunasaw.voglander.common.event.StreamReadyEvent;

import io.github.lunasaw.voglander.intergration.wrapper.zlm.auth.ZlmHookAuthService;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.zlm.entity.ServerNodeConfig;
import io.github.lunasaw.zlm.entity.StreamKey;
import io.github.lunasaw.zlm.entity.StreamProxyItem;
import io.github.lunasaw.zlm.hook.param.*;
import io.github.lunasaw.zlm.hook.service.AbstractZlmHookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * ZLM Hook服务实现
 * 用于处理ZLMediaKit的各种Hook回调
 *
 * @author luna
 * @date 2025/01/22
 */
@Slf4j
@Service
public class VoglanderZlmHookServiceImpl extends AbstractZlmHookService {

    @Autowired
    private MediaNodeManager       mediaNodeManager;

    @Autowired
    private StreamProxyManager     streamProxyManager;

    @Autowired
    private MediaSessionManager    mediaSessionManager;

    @Autowired
    private ZlmHookAuthService     zlmHookAuthService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 无人观看到点是否主动 BYE 回收（默认 true，按 SIP/GB28181 标准）。
     * 置 false 退回旧保守行为（不关流，回收交给 pending_close / GC 对账）。
     */
    @Value("${live.none-reader.reclaim-enabled:true}")
    private boolean                noneReaderReclaimEnabled;

    /**
     * 首播宽限期（秒）：会话建立此时长内即便无人观看也不回收，
     * 防"流刚上线、播放器还没连上"竞态。
     */
    @Value("${live.none-reader.grace-sec:30}")
    private int                    noneReaderGraceSec;

    @Override
    public void onServerKeepLive(OnServerKeepaliveHookParam param, HttpServletRequest request) {
        String serverId = param.getMediaServerId();
        try {
            // 更新节点状态：在线状态，心跳时间为当前时间戳
            Long keepalive = System.currentTimeMillis();
            mediaNodeManager.saveOrUpdateNodeStatus(serverId, null, keepalive, null, null);
        } catch (Exception e) {
            log.error("处理心跳回调失败，节点ID: {}, 错误: {}", serverId, e.getMessage(), e);
        }
    }

    @Override
    public HookResult onPlay(OnPlayHookParam param, HttpServletRequest request) {
        log.info("ZLM播放开始回调 - ID: {}, 应用: {}, 流名: {}, 客户端IP: {}",
            param.getId(), param.getApp(), param.getStream(), param.getIp());

        HookResult result = new HookResult();
        if (!zlmHookAuthService.validatePlay(param.getIp(), param.getApp(), param.getStream())) {
            result.setCode(401);
            result.setMsg("鉴权失败");
            return result;
        }
        result.setCode(0);
        result.setMsg("允许播放");
        return result;
    }

    @Override
    public HookResultForOnPublish onPublish(OnPublishHookParam param, HttpServletRequest request) {
        log.info("ZLM推流开始回调 - ID: {}, 应用: {}, 流名: {}, 客户端IP: {}",
            param.getId(), param.getApp(), param.getStream(), param.getIp());

        HookResultForOnPublish result = new HookResultForOnPublish();
        if (!zlmHookAuthService.validatePublish(param.getIp(), param.getApp(), param.getStream())) {
            result.setCode(401);
            result.setMsg("鉴权失败");
            return result;
        }
        result.setCode(0);
        result.setMsg("允许推流");
        // 启用的输出协议必须覆盖 getPlaybackUrls 返回给前端/播放器的全部协议，
        // 否则播放器选了未启用的协议拉流会触发 onStreamNotFound，
        // 而真正存在的协议流(如 hls)无人拉又触发 onStreamNoneReader。
        // 注意 HTTP-FLV/WS-FLV 没有独立 schema，与 RTMP 共用 FLV muxer，由 enableRtmp 统一控制——
        // flv.js 拉 .live.flv 在 ZLM 侧归为 rtmp schema，故 FLV 能否播放取决于 enableRtmp。
        // 资源充足，直接全协议开启(对齐 config.ini 全局意图)，彻底杜绝协议不匹配。
        result.setEnableHls(true);
        result.setEnableMp4(true);
        result.setEnableRtmp(true);
        result.setEnableTs(true);
        result.setEnableRtsp(true);
        result.setEnableFmp4(true);
        return result;
    }

    @Override
    public void onStreamChanged(OnStreamChangedHookParam param, HttpServletRequest request) {
        log.info("ZLM流状态变化回调 - 应用: {}, 流名: {}, 状态: {}, 总观看数: {}, 存活时间: {}秒",
            param.getApp(), param.getStream(), param.isRegist() ? "上线" : "下线",
            param.getTotalReaderCount(), param.getAliveSecond());

        try {
            // 参数验证 - 当app或stream为null或空字符串时优雅处理
            if (param.getApp() == null || param.getStream() == null ||
                param.getApp().trim().isEmpty() || param.getStream().trim().isEmpty()) {
                log.debug("流状态变化回调参数不完整，跳过处理 - app: {}, stream: {}",
                    param.getApp(), param.getStream());
                return;
            }

            // 构建查询条件
            StreamProxyDTO queryDTO = new StreamProxyDTO();
            queryDTO.setApp(param.getApp());
            queryDTO.setStream(param.getStream());

            // 查找现有记录
            StreamProxyDTO existingProxy = streamProxyManager.get(queryDTO);
            if (existingProxy != null) {
                // 构建流状态变化的扩展信息
                String extendInfo = buildStreamChangedExtendInfo(existingProxy, param);

                // 构建更新DTO - 更新在线状态和扩展字段
                StreamProxyDTO updateDTO = new StreamProxyDTO();
                updateDTO.setId(existingProxy.getId());
                updateDTO.setOnlineStatus(param.isRegist() ? 1 : 0);
                updateDTO.setExtend(extendInfo);

                Boolean updated = streamProxyManager.updateStreamProxy(updateDTO,
                    param.isRegist() ? "流上线状态更新" : "流下线状态更新");

                if (updated) {
                    log.info("流{}状态更新成功 - 代理ID: {}, app: {}, stream: {}",
                        param.isRegist() ? "上线" : "下线", existingProxy.getId(), param.getApp(), param.getStream());
                } else {
                    log.warn("流{}状态更新失败 - app: {}, stream: {}",
                        param.isRegist() ? "上线" : "下线", param.getApp(), param.getStream());
                }
            } else {
                log.warn("流{}状态更新失败，未找到匹配的流代理记录 - app: {}, stream: {}",
                    param.isRegist() ? "上线" : "下线", param.getApp(), param.getStream());
            }

            // 流上线：唤醒首播等待 future，推 SSE live.ready
            // 流下线：对称清直播缓存（不依赖 StreamProxy 表，rtp 直播流也覆盖）
            if (param.isRegist()) {
                eventPublisher.publishEvent(new StreamReadyEvent(param.getStream()));
            } else {
                eventPublisher.publishEvent(new StreamOfflineEvent(param.getStream(), param.getMediaServerId()));
            }
        } catch (Exception e) {
            log.error("处理流状态变化回调失败 - app: {}, stream: {}, 状态: {}, 错误: {}",
                param.getApp(), param.getStream(), param.isRegist() ? "上线" : "下线", e.getMessage(), e);
            // 不抛出异常，避免影响ZLM的Hook回调处理
        }
    }

    @Override
    public HookResultForStreamNoneReader onStreamNoneReader(OnStreamNoneReaderHookParam param, HttpServletRequest request) {
        String streamId = param.getStream();
        log.info("ZLM流无人观看回调 - 应用: {}, 流名: {}", param.getApp(), streamId);

        HookResultForStreamNoneReader result = new HookResultForStreamNoneReader();
        result.setCode(0);

        // 开关关闭 → 保持旧保守行为（不关流）
        if (!noneReaderReclaimEnabled || streamId == null || streamId.trim().isEmpty()) {
            result.setClose(false);
            return result;
        }
        // 首播宽限期内 → 不回收，防"流刚上线、播放器还没连上"竞态（用 DB 会话 createTime）
        if (isWithinNoneReaderGrace(streamId)) {
            log.info("无人观看但在首播宽限期内，暂不回收, streamId={}", streamId);
            result.setClose(false);
            return result;
        }
        // 到点回收：发 StreamOfflineEvent(reason=none_reader) → service 委托 closeStream（含标准 BYE，让设备停推）
        // 同时 close=true 让 ZLM 立即丢弃流对象（释放下游 muxer）；两者职责不同，都要做。
        log.info("无人观看到点，主动 BYE 回收, streamId={}", streamId);
        eventPublisher.publishEvent(new StreamOfflineEvent(streamId, param.getMediaServerId(), "none_reader"));
        result.setClose(true);
        return result;
    }

    /**
     * 首播宽限期判定：会话 createTime 在 {@link #noneReaderGraceSec} 内则跳过回收。
     * 无会话 / 无创建时间 / 查询异常 → 不豁免（照常回收孤儿流）。
     */
    private boolean isWithinNoneReaderGrace(String streamId) {
        try {
            io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO s =
                mediaSessionManager.getByStreamId(streamId);
            if (s == null || s.getCreateTime() == null) {
                return false;
            }
            return s.getCreateTime().isAfter(java.time.LocalDateTime.now().minusSeconds(noneReaderGraceSec));
        } catch (Exception e) {
            log.warn("无人观看宽���期判定异常，按不豁免处理, streamId={}: {}", streamId, e.getMessage());
            return false;
        }
    }

    @Override
    public void onStreamNotFound(OnStreamNotFoundHookParam param, HttpServletRequest request) {
        String streamId = param.getStream();
        log.info("ZLM流未找到回调 - 应用: {}, 流名: {}", param.getApp(), streamId);

        // 仅处理 GB28181 流（gb_live_ 直播 / gb_back_ 回放）；其余（外部拉流等）不在本入口管控
        if (streamId == null || !(streamId.startsWith("gb_live_") || streamId.startsWith("gb_back_"))) {
            return;
        }

        // 按需拉流（on-demand INVITE）由前端 /api/v1/live/start 主动编排，这里不重发 INVITE，避免 INVITE 风暴。
        // 本回调只做"幽灵会话"兜底：DB 会话仍 ACTIVE 但 ZLM 查无该流（如节点重启/丢流），
        // 发 StreamOfflineEvent 让 service 层 closeStream 标准收尾（清缓存 + 标 CLOSED + 推 SSE），避免 GC 周期内残留。
        try {
            MediaSessionDTO session = mediaSessionManager.getByStreamId(streamId);
            if (session != null && MediaSessionConstant.Status.ACTIVE == (session.getStatus() == null ? -1 : session.getStatus())) {
                log.warn("直播流查无但会话仍 ACTIVE（幽灵会话），兜底回收, streamId={}", streamId);
                eventPublisher.publishEvent(new StreamOfflineEvent(streamId, param.getMediaServerId()));
            } else {
                // 无会话 / 会话非 ACTIVE：玩家请求了不存在或未就绪的流，属预期，仅日志
                log.warn("直播流查无且无活跃会话, streamId={}", streamId);
            }
        } catch (Exception e) {
            log.warn("onStreamNotFound 处理异常, streamId={}: {}", streamId, e.getMessage());
        }
    }

    @Override
    public void onServerStarted(ServerNodeConfig param, HttpServletRequest request) {
        log.info("ZLM服务器启动回调 - 服务器配置: {}", param);

        try {
            // 从ServerNodeConfig中获取节点信息
            String serverId = param.getGeneralMediaServerId();
            String httpPort = param.getHttpPort();

            // 从HTTP请求中获取真实的host地址
            String hostFromRequest = extractHostFromRequest(request);

            // 拼接host和port
            String fullHost = buildFullHostAddress(hostFromRequest, httpPort);
            // 插入或更新节点：在线状态，心跳时间为当前时间
            Long keepalive = System.currentTimeMillis();
            Long nodeId = mediaNodeManager.saveOrUpdateNodeStatus(serverId, param.getApiSecret(), keepalive, fullHost, serverId);
            log.info("处理服务器启动回调成功，节点ID: {}, 数据库ID: {}, 完整地址: {}", serverId, nodeId, fullHost);
        } catch (Exception e) {
            log.error("处理服务器启动回调失败，错误: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onSendRtpStopped(OnSendRtpStoppedHookParam param, HttpServletRequest request) {
        // ZLM 停止 RTP "发送"（如级联上级断开 / 主动 closeSendRtp）。
        // 注意：停止发送 ≠ 本地收流源消失——同一路源流可能仍有其他观看者/级联在用，
        // 故这里绝不发 StreamOfflineEvent 拆源流，仅记日志供监控；真正的源流回收由
        // onStreamChanged(regist=false) / onRtpServerTimeout / GC 对账驱动。
        log.info("ZLM RTP发送停止回调 - 服务器ID: {}, app: {}, stream: {}",
            param.getMediaServerId(), param.getApp(), param.getStream());
    }

    @Override
    public void onRtpServerTimeout(OnRtpServerTimeoutHookParam param, HttpServletRequest request) {
        String streamId = param.getStreamId();
        log.info("ZLM RTP服务器超时回调 - 服务器ID: {}, streamId: {}, ssrc: {}, 本地端口: {}",
            param.getMediaServerId(), streamId, param.getSsrc(), param.getLocalPort());

        // openRtpServer 后设备始终未推 RTP（或推流中断），收流端口超时 = 该会话已死。
        // 发 StreamOfflineEvent 让 service 层 closeStream 标准收尾（closeRtpServer + BYE + 标 CLOSED + 清缓存 + SSE）。
        if (streamId == null || streamId.trim().isEmpty()) {
            log.warn("RTP服务器超时但无 streamId，跳过回收 - 服务器ID: {}", param.getMediaServerId());
            return;
        }
        log.warn("RTP收流超时，回收会话, streamId={}", streamId);
        eventPublisher.publishEvent(new StreamOfflineEvent(streamId, param.getMediaServerId()));
    }

    @Override
    public HookResultForOnHttpAccess onHttpAccess(OnHttpAccessParam param, HttpServletRequest request) {
        log.info("ZLM HTTP访问回调 - 路径: {}, 客户端IP: {}",
            param.getPath(), param.getIp());

        // 内网受信部署：HTTP 文件/点播访问统一放行（err 为空即允许）。
        // 播放鉴权已在 onPlay（按 IP 白名单 / token）前置把关，此处不再重复拦截。
        HookResultForOnHttpAccess result = new HookResultForOnHttpAccess();
        result.setCode(0);
        result.setMsg("允许访问");
        return result;
    }

    @Override
    public HookResultForOnRtspRealm onRtspRealm(OnRtspRealmHookParam param, HttpServletRequest request) {
        log.info("ZLM RTSP Realm回调 - 服务器ID: {}", JSON.toJSONString(param));

        // realm 返回空串 = 关闭 RTSP 鉴权（内网受信，GB28181 媒体走 SIP 协商端口，不依赖 RTSP 鉴权）。
        // realm 为空时 ZLM 不会再回调 onRtspAuth。
        HookResultForOnRtspRealm result = new HookResultForOnRtspRealm();
        result.setCode(0);
        result.setRealm(StringUtils.EMPTY);
        return result;
    }

    @Override
    public HookResultForOnRtspAuth onRtspAuth(OnRtspAuthHookParam param, HttpServletRequest request) {
        log.info("ZLM RTSP认证回调 - 服务器ID: {}", JSON.toJSONString(param));

        // 正常情况下 onRtspRealm 返回空 realm 已关闭鉴权，此回调不会触发；
        // 防御性兜底：返回明文空密码放行，不留硬编码密码隐患。
        HookResultForOnRtspAuth result = new HookResultForOnRtspAuth();
        result.setCode(0);
        result.setEncrypted(false);
        result.setPasswd(StringUtils.EMPTY);
        return result;
    }

    @Override
    public void onFlowReport(OnFlowReportHookParam param, HttpServletRequest request) {
        log.info("ZLM流量报告回调 - 服务器ID: {}", JSON.toJSONString(param));

        // TODO: 实现流量统计处理逻辑
        // 可以记录流量统计、计费等
    }

    @Override
    public void onServerExited(HookParam param, HttpServletRequest request) {
        String serverId = param.getMediaServerId();
        log.info("ZLM服务器退出回调 - 服务器ID: {}", serverId);

        try {
            // 更新节点为离线状态
            mediaNodeManager.updateNodeOffline(serverId);
            log.info("处理服务器退出回调成功，节点ID: {} 已设置为离线", serverId);
            eventPublisher.publishEvent(new NodeExitedEvent(serverId));
        } catch (Exception e) {
            log.error("处理服务器退出回调失败，节点ID: {}, 错误: {}", serverId, e.getMessage(), e);
        }
    }

    @Override
    public void onRecordMp4(OnRecordMp4HookParam param, HttpServletRequest request) {
        // 当前无录像域表（tb_record / RecordManager 尚未引入），录像生命周期由 ZLM 自治管理。
        // 这里仅结构化记录文件元数据供排障/审计；后续若有录像管理需求，再落库（filePath/stream/fileSize/timeLen 等）。
        log.info("ZLM MP4录制完成回调 - 服务器ID: {}, app: {}, stream: {}, 文件: {}, 大小: {}字节, 时长: {}秒, 起始: {}",
            param.getMediaServerId(), param.getApp(), param.getStream(),
            param.getFilePath(), param.getFileSize(), param.getTimeLen(), param.getStartTime());
    }

    /**
     * 从HTTP请求中提取主机地址
     *
     * @param request HTTP请求对象
     * @return 主机地址
     */
    private String extractHostFromRequest(HttpServletRequest request) {
        if (request == null) {
            log.warn("HTTP请求对象为空，使用默认主机地址");
            return "localhost";
        }

        try {
            // 优先从X-Forwarded-Host头获取（适用于代理场景）
            String forwardedHost = request.getHeader("X-Forwarded-Host");
            if (forwardedHost != null && !forwardedHost.trim().isEmpty()) {
                // 如果有多个转发主机，取第一个
                return forwardedHost.split(",")[0].trim();
            }

            // 从Host头获取
            String hostHeader = request.getHeader("Host");
            if (hostHeader != null && !hostHeader.trim().isEmpty()) {
                // Host头可能包含端口号，需要去除
                return hostHeader.split(":")[0].trim();
            }

            // 最后使用ServerName
            String serverName = request.getServerName();
            if (serverName != null && !serverName.trim().isEmpty()) {
                return serverName;
            }

            log.warn("无法从HTTP请求中获取主机地址，使用默认值");
            return "localhost";
        } catch (Exception e) {
            log.error("提取主机地址失败，使用默认值: {}", e.getMessage());
            return "localhost";
        }
    }

    /**
     * 构建完整的主机地址（包含端口）
     *
     * @param host 主机地址
     * @param port 端口号
     * @return 完整的主机地址
     */
    private String buildFullHostAddress(String host, String port) {
        if (host == null || host.trim().isEmpty()) {
            host = "localhost";
        }

        if (port == null || port.trim().isEmpty()) {
            log.warn("端口号为空，使用默认端口80");
            port = "80";
        }

        // 如果host已经包含端口，先去除
        if (host.contains(":")) {
            host = host.split(":")[0];
        }

        // 拼接host和port
        String fullAddress = "http://" + host + ":" + port;
        log.debug("构建完整主机地址: {}", fullAddress);
        return fullAddress;
    }

    /**
     * 从ServerNodeConfig中提取服务器ID
     *
     * @param config 服务器节点配置
     * @return 服务器ID
     */
    private String extractServerIdFromConfig(ServerNodeConfig config) {
        try {
            // 尝试通过反射或toString()方法获取服务器ID
            String configStr = config.toString();
            log.debug("ServerNodeConfig内容: {}", configStr);

            // 可以根据实际的配置内容来解析ID
            // 这里先返回一个基于时间的默认ID
            return "zlm-server-" + System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("提取服务器ID失败，使用默认值: {}", e.getMessage());
            return "zlm-default-" + System.currentTimeMillis();
        }
    }

    @Override
    public void onProxyAdded(StreamProxyItem param, StreamKey streamKey, HttpServletRequest request) {
        log.info("ZLM拉流代理添加成功回调 - app: {}, stream: {}, url: {}, key: {}",
            param.getApp(), param.getStream(), param.getUrl(), streamKey != null ? streamKey.getKey() : "null");

        try {
            // 构建拉流代理扩展信息
            String extend = buildProxyExtendInfo(param);

            // 获取代理key
            String proxyKey = streamKey != null ? streamKey.getKey() : null;
            if (proxyKey == null || proxyKey.trim().isEmpty()) {
                log.error("代理key为空，无法创建代理记录 - app: {}, stream: {}", param.getApp(), param.getStream());
                return;
            }

            // 构建查询条件，先查找是否存在记录
            StreamProxyDTO queryDTO = new StreamProxyDTO();
            queryDTO.setApp(param.getApp());
            queryDTO.setStream(param.getStream());

            // 查找现有记录
            StreamProxyDTO existingProxy = streamProxyManager.get(queryDTO);

            if (existingProxy != null) {
                // 更新现有记录
                StreamProxyDTO updateDTO = new StreamProxyDTO();
                updateDTO.setId(existingProxy.getId());
                updateDTO.setProxyKey(proxyKey);
                updateDTO.setOnlineStatus(1); // 设置为在线状态
                updateDTO.setExtend(extend);

                Boolean updated = streamProxyManager.updateStreamProxy(updateDTO, "Hook回调更新代理记录");

                if (updated) {
                    log.info("处理拉流代理添加回调成功，更新现有记录 - 代理ID: {}, app: {}, stream: {}, key: {}",
                        existingProxy.getId(), param.getApp(), param.getStream(), proxyKey);
                } else {
                    log.warn("更新代理记录失败 - app: {}, stream: {}, key: {}",
                        param.getApp(), param.getStream(), proxyKey);
                }
            } else {
                // 创建新记录
                StreamProxyDTO newProxy = new StreamProxyDTO();
                newProxy.setApp(param.getApp());
                newProxy.setStream(param.getStream());
                newProxy.setUrl(param.getUrl());
                newProxy.setProxyKey(proxyKey);
                newProxy.setOnlineStatus(1); // 设置为在线状态
                newProxy.setStatus(1); // 启用状态
                newProxy.setExtend(extend);

                Long proxyId = streamProxyManager.addStreamProxy(newProxy, "Hook回调创建代理记录");

                if (proxyId != null) {
                    log.info("处理拉流代理添加回调成功，创建新记录 - 代理ID: {}, app: {}, stream: {}, key: {}",
                        proxyId, param.getApp(), param.getStream(), proxyKey);
                } else {
                    log.warn("创建代理记录失败 - app: {}, stream: {}, key: {}",
                        param.getApp(), param.getStream(), proxyKey);
                }
            }
        } catch (Exception e) {
            log.error("处理拉流代理添加回调失败，app: {}, stream: {}, key: {}, 错误: {}",
                param.getApp(), param.getStream(), streamKey != null ? streamKey.getKey() : "null", e.getMessage(), e);
            // 不抛出异常，避免影响ZLM的Hook回调处理
        }
    }

    /**
     * 构建流状态变化扩展信息
     *
     * @param existingProxy
     * @param param OnStreamChangedHookParam参数
     * @return 扩展信息JSON字符串
     */
    private String buildStreamChangedExtendInfo(StreamProxyDTO existingProxy, OnStreamChangedHookParam param) {
        try {
            // 构建扩展信息对象
            java.util.Map<String, Object> extendMap = new java.util.HashMap<>();
            extendMap.put("callbackType", "onStreamChanged");
            extendMap.put("callbackTime", System.currentTimeMillis());
            extendMap.put("regist", param.isRegist());
            extendMap.put("totalReaderCount", param.getTotalReaderCount());
            extendMap.put("aliveSecond", param.getAliveSecond());
            extendMap.put("callId", param.getCallId());
            extendMap.put("vhost", param.getVhost());

            JSONObject jsonObject = JSON.parseObject(existingProxy.getExtend());
            jsonObject.putAll(extendMap);
            // 使用 FastJSON2 序列化
            return jsonObject.toJSONString();
        } catch (Exception e) {
            log.warn("构建流状态变化扩展信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建拉流代理扩展信息（StreamProxyItem参数版本）
     *
     * @param param StreamProxyItem参数
     * @return 扩展信息JSON字符串
     */
    private String buildProxyExtendInfo(StreamProxyItem param) {
        try {
            // 使用 FastJSON2 直接序列化，避免手动字符串拼接
            return com.alibaba.fastjson2.JSON.toJSONString(param);
        } catch (Exception e) {
            log.warn("构建拉流代理扩展信息失败: {}", e.getMessage());
            return null;
        }
    }
}