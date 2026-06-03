package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sip.message.Response;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage 4 — 响应上级平台的 INVITE/BYE，完成双向媒体转发。
 *
 * <p>流程：上级 INVITE → 先向真实 IPC 拉流（已有 MediaSessionManager 流程）
 * → 调用 ZLM startSendRtp 向上级推流 → 回包 200 OK(SDP)。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeMediaInviteListener {

    private final CascadeChannelManager     cascadeChannelManager;
    private final VoglanderServerMediaCommand serverMediaCommand;

    @Autowired(required = false)
    private ZlmIntegrationConfig            zlmConfig;

    /** callId → ssrc，用于 BYE 时停止推流 */
    private final ConcurrentHashMap<String, String> activeSessions = new ConcurrentHashMap<>();

    @EventListener
    @Async("sipNotifierExecutor")
    public void onInvite(ClientInviteEvent event) {
        String callId = event.getCallId();
        String userId = event.getUserId(); // 本端客户端 ID（localClientId）
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

            /* 1. 确保 ZLM 上有来自真实 IPC 的流（通过现有 serverMediaCommand 触发 IPC 拉流） */
            String zlmApp = "rtp";
            String zlmStream = channel.getLocalDeviceId() + "_" + channel.getLocalChannelId();
            ensureStreamOnline(channel, sdp);

            /* 2. 调用 ZLM startSendRtp 推流给上级 */
            String ssrc = sdp.getSsrc() != null ? sdp.getSsrc() : "0";
            ZlmIntegrationConfig.ZlmServerConfig node = zlmConfig.getDefaultServer();
            if (node == null) {
                log.error("无可用 ZLM 节点: callId={}", callId);
                sendInviteError(event);
                return;
            }
            StartSendRtpReq req = buildStartSendRtpReq(zlmApp, zlmStream, sdp, ssrc);
            StartSendRtpResult result = ZlmRestService.startSendRtp(node.getHostPort(), node.getSecret(), req);
            if (result == null || !"0".equals(result.getCode())) {
                log.error("ZLM startSendRtp 失败: callId={}, result={}", callId, result);
                sendInviteError(event);
                return;
            }
            activeSessions.put(callId, ssrc);

            /* 3. 回包 200 OK */
            sendInviteOk(event, sdp, result.getLocalPort());
            log.info("级联 INVITE 处理成功: callId={}, cascadeChannelId={}", callId, cascadeChannelId);

        } catch (Exception e) {
            log.error("处理级联 INVITE 失败: callId={}", callId, e);
            sendInviteError(event);
        }
    }

    @EventListener
    @Async("sipNotifierExecutor")
    public void onBye(ClientByeEvent event) {
        String callId = event.getCallId();
        String ssrc = activeSessions.remove(callId);
        if (ssrc == null) return;
        try {
            ZlmIntegrationConfig.ZlmServerConfig node = zlmConfig.getDefaultServer();
            if (node != null) {
                CloseSendRtpReq req = new CloseSendRtpReq();
                req.setSsrc(ssrc);
                ZlmRestService.stopSendRtp(node.getHostPort(), node.getSecret(), req);
            }
            log.info("级联 BYE 处理成功: callId={}", callId);
        } catch (Exception e) {
            log.error("处理级联 BYE 失败: callId={}", callId, e);
        }
    }

    /** 触发向真实设备的拉流（ZLM 已有流则幂等） */
    private void ensureStreamOnline(CascadeChannelDTO channel, GbSessionDescription sdp) {
        // 使用现有服务端 MediaCommand 向真实 IPC 发起 INVITE 拉流
        serverMediaCommand.inviteRealTimePlay(
            channel.getLocalDeviceId(),
            sdp.getAddress(),
            sdp.getPort() != null ? sdp.getPort() : 10000
        );
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

    private void sendInviteOk(ClientInviteEvent event, GbSessionDescription sdp, String localPort) {
        SipTransactionRegistry.TransactionContextInfo ctx =
            SipTransactionRegistry.getContext(event.getTransactionContextKey());
        if (ctx == null) {
            log.warn("事务上下文已失效: key={}", event.getTransactionContextKey());
            return;
        }
        /* 构造简单 200 OK，不带 SDP（被动推流场景无需 SDP 回包） */
        ResponseCmd.sendResponse(Response.OK, ctx.getOriginalEvent(), ctx.getServerTransaction());
    }

    private void sendInviteError(ClientInviteEvent event) {
        SipTransactionRegistry.TransactionContextInfo ctx =
            SipTransactionRegistry.getContext(event.getTransactionContextKey());
        if (ctx != null) {
            ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR,
                ctx.getOriginalEvent(), ctx.getServerTransaction());
        }
    }

    /** SDP s= 行的 channelId（GB28181 约定） */
    private String parseCascadeChannelId(GbSessionDescription sdp) {
        if (sdp.getBaseSdb() == null) return "";
        try {
            return sdp.getBaseSdb().getSessionName().getValue();
        } catch (Exception e) {
            return "";
        }
    }

    /** 从 SDP o= 行取 userId 作为 platformId（上级平台 ID） */
    private String parsePlatformId(GbSessionDescription sdp, String fallback) {
        if (sdp.getBaseSdb() == null) return fallback;
        try {
            return sdp.getBaseSdb().getOrigin().getUsername();
        } catch (Exception e) {
            return fallback;
        }
    }

    private int parseSsrc(String ssrc) {
        try { return Integer.parseInt(ssrc); } catch (Exception e) { return 0; }
    }
}
