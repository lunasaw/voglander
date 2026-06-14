package io.github.lunasaw.voglander.service.live.protocol;

import io.github.lunasaw.zlm.config.ZlmNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 媒体建流上下文（PROTOCOL-S5）。
 * <p>
 * {@link MediaProtocolHandler#establish} 的入参，承载协议无关编排已确定的信息：
 * 已选中的 ZLM 节点、streamId、设备/通道寻址、流模式。具体"如何让流进 ZLM"由各协议 handler 决定
 * （GB28181：openRtpServer + INVITE；ONVIF/RTSP：addStreamProxy）。
 * </p>
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaEstablishContext {

    /**
     * 已选中的 ZLM 媒体节点（由编排层负载均衡选出）。
     */
    private ZlmNode node;

    /**
     * 前端稳定主键 streamId（ZLM 收流流名）。
     */
    private String  streamId;

    /**
     * 设备 ID（GB28181 为父设备国标编码；信令传输地址解析依据）。
     */
    private String  deviceId;

    /**
     * 通道 ID（GB28181 为通道国标编码，INVITE 寻址用；可为空）。
     */
    private String  channelId;

    /**
     * 流传输模式（UDP / TCP_ACTIVE / TCP_PASSIVE）。
     */
    private String  streamMode;
}
