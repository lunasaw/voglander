package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import io.github.lunasaw.gb28181.common.entity.enums.InviteSessionNameEnum;
import io.github.lunasaw.gb28181.common.entity.mansrtsp.ManSrtspRequest;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.client.entity.InviteResponseEntity;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInfoEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.config.ZlmIntegrationConfig;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.entity.rtp.CloseSendRtpReq;
import io.github.lunasaw.zlm.entity.rtp.StartSendRtpReq;
import io.github.lunasaw.zlm.entity.rtp.StartSendRtpResult;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sip.message.Response;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage 4 — 响应上级平台的 INVITE/BYE/INFO，完成双向媒体转发（实时点播 + 回放点播 + 回放控制）。
 *
 * <p>流程：上级 INVITE → 向真实 IPC 拉流（实时 inviteRealTimePlay / 回放 invitePlayBack）
 * → 调用 ZLM startSendRtp 向上级推流 → 回 200 OK（带本端发流 SDP，框架
 * {@link InviteResponseEntity#getAckPlayBody}/{@code getAckPlayBackBody} 构造，sendonly）。
 * 上级 BYE → stopSendRtp；上级 INFO（回放控制 PLAY/PAUSE/SCALE/拖动）→ 透传真实设备
 * {@link VoglanderServerMediaCommand#controlPlayBack}。
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeMediaInviteListener {

    private final CascadeChannelManager       cascadeChannelManager;
    private final VoglanderServerMediaCommand serverMediaCommand;

    @Autowired(required = false)
    private ZlmIntegrationConfig              zlmConfig;

    /** callId → 媒体会话上下文（ssrc + 真实设备 + 上级 userId），用于 BYE 停推流；INFO 按 userId 反查。 */
    private final ConcurrentHashMap<String, MediaSession> activeSessions = new ConcurrentHashMap<>();

    @EventListener
    @Async("sipNotifierExecutor")
    public void onInvite(ClientInviteEvent event) {
        String callId = event.getCallId();
        String userId = event.getUserId();
        try {
            GbSessionDescription sdp = (GbSessionDescription) event.getSessionDescription();
            if (sdp == null) {
                log.warn("INVITE SDP 为空: callId={}", callId);
                sendInviteError(event);
                return;
            }

            String cascadeChannelId = parseCascadeChannelId(sdp);
            String platformId = parsePlatformId(sdp, userId);
            CascadeChannelDTO channel = cascadeChannelManager.getByPlatformAndCascadeChannelId(platformId, cascadeChannelId);
            if (channel == null) {
                log.warn("未找到级联通道: platformId={}, cascadeChannelId={}", platformId, cascadeChannelId);
                sendInviteError(event);
                return;
            }

            boolean playback = isPlayback(sdp);
            /* 1. 向真实 IPC 拉流（实时 / 回放分支） */
            ensureStreamOnline(channel, sdp, playback);

            /* 2. 调用 ZLM startSendRtp 推流给上级 */
            String ssrc = sdp.getSsrc() != null ? sdp.getSsrc() : "0";
            if (zlmConfig == null) {
                log.error("无 ZLM 配置: callId={}", callId);
                sendInviteError(event);
                return;
            }
            ZlmIntegrationConfig.ZlmServerConfig node = zlmConfig.getDefaultServer();
            if (node == null) {
                log.error("无可用 ZLM 节点: callId={}", callId);
                sendInviteError(event);
                return;
            }
            String zlmApp = "rtp";
            String zlmStream = channel.getLocalDeviceId() + "_" + channel.getLocalChannelId();
            StartSendRtpReq req = buildStartSendRtpReq(zlmApp, zlmStream, sdp, ssrc);
            StartSendRtpResult result = ZlmRestService.startSendRtp(node.getHostPort(), node.getSecret(), req);
            if (result == null || !"0".equals(result.getCode())) {
                log.error("ZLM startSendRtp 失败: callId={}, result={}", callId, result);
                sendInviteError(event);
                return;
            }
            activeSessions.put(callId, new MediaSession(ssrc, channel.getLocalDeviceId(), userId, playback));

            /* 3. 回包 200 OK（带本端发流 SDP，sendonly） */
            String localSendIp = hostOf(node.getHostPort());
            sendInviteOk(event, userId, localSendIp, result.getLocalPort(), ssrc, sdp);
            log.info("级联 INVITE 处理成功: callId={}, cascadeChannelId={}, playback={}", callId, cascadeChannelId, playback);

        } catch (Exception e) {
            log.error("处理级联 INVITE 失败: callId={}", callId, e);
            sendInviteError(event);
        }
    }

    @EventListener
    @Async("sipNotifierExecutor")
    public void onBye(ClientByeEvent event) {
        String callId = event.getCallId();
        MediaSession session = activeSessions.remove(callId);
        if (session == null) {
            return;
        }
        try {
            if (zlmConfig != null) {
                ZlmIntegrationConfig.ZlmServerConfig node = zlmConfig.getDefaultServer();
                if (node != null) {
                    CloseSendRtpReq req = new CloseSendRtpReq();
                    req.setSsrc(session.ssrc);
                    ZlmRestService.stopSendRtp(node.getHostPort(), node.getSecret(), req);
                }
            }
            log.info("级联 BYE 处理成功: callId={}", callId);
        } catch (Exception e) {
            log.error("处理级联 BYE 失败: callId={}", callId, e);
        }
    }

    /**
     * 上级在回放会话内发 INFO（RTSP 风格 PLAY/PAUSE/SCALE/拖动）→ 透传真实设备。
     * <p>
     * {@link ClientInfoEvent#getUserId()} 为 To 头 userId（上级寻址的级联通道编码），按此反查活跃回放会话取真实设备。
     */
    @EventListener
    @Async("sipNotifierExecutor")
    public void onInfo(ClientInfoEvent event) {
        String userId = event.getUserId();
        ManSrtspRequest rtsp = event.getParsed();
        if (rtsp == null) {
            log.debug("INFO 无 MANSRTSP 解析体，忽略: userId={}", userId);
            return;
        }
        MediaSession session = findPlaybackSessionByUserId(userId);
        if (session == null) {
            log.warn("回放控制找不到活跃会话: userId={}", userId);
            return;
        }
        String localDeviceId = session.localDeviceId;
        String method = rtsp.getMethod() == null ? "" : rtsp.getMethod().toUpperCase();
        try {
            switch (method) {
                case "PAUSE":
                    serverMediaCommand.controlPlayBack(localDeviceId, PlayActionEnums.PLAY_RESUME);
                    log.info("回放控制透传: PAUSE → localDevice={}", localDeviceId);
                    break;
                case "PLAY":
                    if (rtsp.getScale() != null) {
                        serverMediaCommand.controlPlayBack(localDeviceId, PlayActionEnums.PLAY_SPEED, rtsp.getScale());
                        log.info("回放控制透传: SCALE={} → localDevice={}", rtsp.getScale(), localDeviceId);
                    } else if (StringUtils.isNotBlank(rtsp.getRange())) {
                        Long seek = parseRangeSeconds(rtsp.getRange());
                        if (seek != null) {
                            serverMediaCommand.controlPlayBack(localDeviceId, PlayActionEnums.PLAY_RANGE, seek);
                            log.info("回放控制透传: SEEK={}s → localDevice={}", seek, localDeviceId);
                        } else {
                            serverMediaCommand.controlPlayBack(localDeviceId, PlayActionEnums.PLAY_NOW);
                            log.info("回放控制透传: PLAY(Range 无法解析,降级恢复) → localDevice={}", localDeviceId);
                        }
                    } else {
                        serverMediaCommand.controlPlayBack(localDeviceId, PlayActionEnums.PLAY_NOW);
                        log.info("回放控制透传: PLAY(恢复) → localDevice={}", localDeviceId);
                    }
                    break;
                default:
                    log.warn("回放控制未知 RTSP 方法: method={}, userId={}", method, userId);
            }
        } catch (Exception e) {
            log.error("回放控制透传失败: userId={}, method={}", userId, method, e);
        }
    }

    private MediaSession findPlaybackSessionByUserId(String userId) {
        if (userId == null) {
            return null;
        }
        return activeSessions.values().stream()
            .filter(s -> s.playback && userId.equals(s.upperUserId))
            .findFirst()
            .orElse(null);
    }

    /**
     * 解析 RTSP Range 头（{@code npt=10-} / {@code npt=10.5-30}）的起始秒数。
     */
    public static Long parseRangeSeconds(String range) {
        if (StringUtils.isBlank(range)) {
            return null;
        }
        try {
            String v = range.trim();
            int eq = v.indexOf('=');
            if (eq >= 0) {
                v = v.substring(eq + 1);
            }
            int dash = v.indexOf('-');
            if (dash >= 0) {
                v = v.substring(0, dash);
            }
            v = v.trim();
            if (v.isEmpty()) {
                return null;
            }
            return (long) Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
    }

    /** 触发向真实设备的拉流（实时 / 回放）。 */
    private void ensureStreamOnline(CascadeChannelDTO channel, GbSessionDescription sdp, boolean playback) {
        String sdpIp = sdp.getAddress();
        int port = sdp.getPort() != null ? sdp.getPort() : 10000;
        if (playback) {
            String[] window = parsePlaybackWindow(sdp);
            serverMediaCommand.invitePlayBack(channel.getLocalDeviceId(), sdpIp, port, window[0], window[1]);
        } else {
            serverMediaCommand.inviteRealTimePlay(channel.getLocalDeviceId(), sdpIp, port);
        }
    }

    private StartSendRtpReq buildStartSendRtpReq(String app, String stream,
        GbSessionDescription sdp, String ssrc) {
        StartSendRtpReq req = new StartSendRtpReq();
        req.setApp(app);
        req.setStream(stream);
        req.setDstUrl(sdp.getAddress());
        req.setDstPort(sdp.getPort() != null ? sdp.getPort() : 10000);
        req.setSsrc(parseSsrc(ssrc));
        req.setUdp(sdp.getTransport() == null || !sdp.getTransport().name().contains("TCP"));
        req.setUsePs(1);
        return req;
    }

    /**
     * 回 200 OK 带本端发流 SDP（sendonly）。实时用 {@link InviteResponseEntity#getAckPlayBody}，
     * 回放用 {@code getAckPlayBackBody}（携带回放时间窗）。
     */
    private void sendInviteOk(ClientInviteEvent event, String userId, String sendIp, String sendPort,
        String ssrc, GbSessionDescription upSdp) {
        SipTransactionRegistry.TransactionContextInfo ctx =
            SipTransactionRegistry.getContext(event.getTransactionContextKey());
        if (ctx == null) {
            log.warn("事务上下文已失效: key={}", event.getTransactionContextKey());
            return;
        }
        int port = parsePort(sendPort);
        String safeSsrc = StringUtils.defaultIfBlank(ssrc, "0");
        String sdpBody;
        if (isPlayback(upSdp)) {
            String[] window = parsePlaybackWindow(upSdp);
            sdpBody = InviteResponseEntity.getAckPlayBackBody(
                userId, sendIp, port, parseGbTime(window[0]), parseGbTime(window[1]), safeSsrc).toString();
        } else {
            sdpBody = InviteResponseEntity.getAckPlayBody(userId, sendIp, port, safeSsrc).toString();
        }
        ResponseCmd.sendResponse(Response.OK, sdpBody,
            ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader(),
            ctx.getOriginalEvent(), ctx.getServerTransaction());
    }

    private void sendInviteError(ClientInviteEvent event) {
        SipTransactionRegistry.TransactionContextInfo ctx =
            SipTransactionRegistry.getContext(event.getTransactionContextKey());
        if (ctx != null) {
            ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR,
                ctx.getOriginalEvent(), ctx.getServerTransaction());
        }
    }

    /** SDP s= 行：Playback / Download 视为回放，Play 为实时。 */
    private boolean isPlayback(GbSessionDescription sdp) {
        InviteSessionNameEnum type = sdp.getSessionType();
        if (type == null) {
            return false;
        }
        return type == InviteSessionNameEnum.PLAY_BACK || type == InviteSessionNameEnum.DOWNLOAD;
    }

    /** 回放时间窗：取 SDP t= 行 [start, stop]（GB28181 用 Unix 秒，直接读原始 t= 行避免 NTP 偏移）。 */
    private String[] parsePlaybackWindow(GbSessionDescription sdp) {
        String start = "0";
        String stop = "0";
        try {
            String raw = sdp.getBaseSdb() != null ? sdp.getBaseSdb().toString() : "";
            for (String line : raw.split("\\r?\\n")) {
                String l = line.trim();
                if (l.startsWith("t=")) {
                    String[] parts = l.substring(2).trim().split("\\s+");
                    if (parts.length >= 2) {
                        start = parts[0];
                        stop = parts[1];
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("解析回放时间窗失败，回退 0/0: {}", e.getMessage());
        }
        return new String[] {start, stop};
    }

    /** 从 host:port 提取 host。 */
    private String hostOf(String hostPort) {
        if (StringUtils.isBlank(hostPort)) {
            return hostPort;
        }
        int idx = hostPort.lastIndexOf(':');
        return idx > 0 ? hostPort.substring(0, idx) : hostPort;
    }

    private String parseCascadeChannelId(GbSessionDescription sdp) {
        if (sdp.getBaseSdb() == null) {
            return "";
        }
        try {
            return sdp.getBaseSdb().getSessionName().getValue();
        } catch (Exception e) {
            return "";
        }
    }

    private String parsePlatformId(GbSessionDescription sdp, String fallback) {
        if (sdp.getBaseSdb() == null) {
            return fallback;
        }
        try {
            return sdp.getBaseSdb().getOrigin().getUsername();
        } catch (Exception e) {
            return fallback;
        }
    }

    private int parseSsrc(String ssrc) {
        try {
            return Integer.parseInt(ssrc);
        } catch (Exception e) {
            return 0;
        }
    }

    private int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseGbTime(String t) {
        try {
            return Long.parseLong(t);
        } catch (Exception e) {
            return 0L;
        }
    }

    /** 媒体会话上下文。 */
    @AllArgsConstructor
    static class MediaSession {
        final String  ssrc;
        final String  localDeviceId;
        final String  upperUserId;
        final boolean playback;
    }
}
