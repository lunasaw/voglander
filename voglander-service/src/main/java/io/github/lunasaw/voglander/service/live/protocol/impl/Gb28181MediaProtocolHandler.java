package io.github.lunasaw.voglander.service.live.protocol.impl;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.service.live.protocol.MediaEstablishContext;
import io.github.lunasaw.voglander.service.live.protocol.MediaEstablishResult;
import io.github.lunasaw.voglander.service.live.protocol.MediaProtocolHandler;
import io.github.lunasaw.voglander.service.live.protocol.MediaTerminateContext;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.rtp.OpenRtpServerReq;
import io.github.lunasaw.zlm.entity.rtp.OpenRtpServerResult;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181 媒体协议处理器（PROTOCOL-S5）。
 * <p>
 * "设备推 RTP" 模型：建流 = 解析媒体 IP + {@code openRtpServer}（开收流端口）+ 发 INVITE（设备据此推流，
 * 标准寻址到通道，返回真实 SIP Call-ID）；终止 = {@code closeRtpServer} + {@code sendBye}。
 * 这些逻辑由 {@code MediaPlayServiceImpl} 迁入，编排层不再直接依赖 GB28181 命令与 RTP 细节。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class Gb28181MediaProtocolHandler implements MediaProtocolHandler {

    @Autowired
    private VoglanderServerMediaCommand voglanderServerMediaCommand;
    @Autowired
    private MediaNodeManager            mediaNodeManager;

    @Override
    public Set<Integer> supportProtocols() {
        return Set.of(DeviceProtocolEnum.GB28181.getType());
    }

    @Override
    public MediaEstablishResult establish(MediaEstablishContext ctx) {
        ZlmNode node = ctx.getNode();
        String streamId = ctx.getStreamId();

        // 1. 解析下发给设备的媒体 IP（NAT 环境可由 tb_media_node.extend.mediaIp 覆盖）
        String sdpIp = resolveMediaIp(node);

        // 2. 开 RTP 接收端口
        OpenRtpServerReq rtpReq = new OpenRtpServerReq();
        rtpReq.setPort(0);
        rtpReq.setTcpMode(toTcpMode(ctx.getStreamMode()));
        rtpReq.setStreamId(streamId);
        OpenRtpServerResult rtpResult = ZlmRestService.openRtpServer(node.getHost(), node.getSecret(), rtpReq);
        if (rtpResult == null || rtpResult.getPort() == null) {
            return MediaEstablishResult.failure("openRtpServer 失败");
        }
        int rtpPort = Integer.parseInt(rtpResult.getPort());

        // 3. 发 INVITE（GB28181 标准：寻址到通道；同步返回真实 SIP Call-ID）
        ResultDTO<String> inviteResult = voglanderServerMediaCommand.inviteRealTimePlayWithCallId(
            ctx.getDeviceId(), ctx.getChannelId(), sdpIp, rtpPort, toStreamModeEnum(ctx.getStreamMode()));
        if (inviteResult == null || !inviteResult.isSuccess()) {
            return MediaEstablishResult.failure("INVITE 下发失败");
        }
        return MediaEstablishResult.success(inviteResult.getData(), sdpIp, rtpPort);
    }

    @Override
    public void terminate(MediaTerminateContext ctx) {
        // 1. 关闭收流端口（按会话所在节点；无节点则跳过）
        if (ctx.getNode() != null) {
            try {
                ZlmRestService.closeRtpServer(ctx.getNode().getHost(), ctx.getNode().getSecret(), ctx.getStreamId());
            } catch (Exception e) {
                log.warn("[gb28181.terminate] closeRtpServer 失败, streamId={}: {}", ctx.getStreamId(), e.getMessage());
            }
        }
        // 2. 发 BYE（平台作为 UAC 主动结束对话；无 callId 则跳过）
        if (ctx.getCallId() != null && !ctx.getCallId().isBlank()) {
            try {
                voglanderServerMediaCommand.sendBye(ctx.getCallId());
            } catch (Exception e) {
                log.warn("[gb28181.terminate] sendBye 失败, callId={}: {}", ctx.getCallId(), e.getMessage());
            }
        }
    }

    // ================================
    // 私有辅助（由 MediaPlayServiceImpl 迁入）
    // ================================

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
