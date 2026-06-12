package io.github.lunasaw.voglander.service.live.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 媒体建流结果（PROTOCOL-S5）。
 * <p>
 * {@link MediaProtocolHandler#establish} 的返回，承载协议特定建流产生、需由编排层落库/回填的信息：
 * 协议会话标识（GB28181 = SIP Call-ID）、实际收流端点（GB28181 = sdpIp + rtpPort）。
 * 非 SIP 协议（ONVIF/RTSP）的 callId/rtpPort 可为空。
 * </p>
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaEstablishResult {

    /**
     * 是否建流成功（下发指令成功，不代表流已就绪——就绪由编排层等 on_stream_changed）。
     */
    private boolean success;

    /**
     * 协议会话标识：GB28181 为真实 SIP Call-ID（关流据此发 BYE）；非 SIP 协议可为空。
     */
    private String  callId;

    /**
     * 实际下发给设备的收流 IP（GB28181 SDP 用；落 LiveSessionInfo）；非 SIP 协议可为空。
     */
    private String  sdpIp;

    /**
     * 实际开启的收流端口（GB28181 RTP 端口；落 LiveSessionInfo）；非 SIP 协议可为空。
     */
    private Integer rtpPort;

    /**
     * 失败原因摘要（success=false 时填充，便于编排层日志/SSE）。
     */
    private String  failReason;

    public static MediaEstablishResult success(String callId, String sdpIp, Integer rtpPort) {
        return MediaEstablishResult.builder().success(true).callId(callId).sdpIp(sdpIp).rtpPort(rtpPort).build();
    }

    public static MediaEstablishResult failure(String reason) {
        return MediaEstablishResult.builder().success(false).failReason(reason).build();
    }
}
