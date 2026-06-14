package io.github.lunasaw.voglander.service.live.protocol;

import java.util.Set;

/**
 * 媒体协议处理器（PROTOCOL-S5：收窄抽象，对称出站命令 SPI）。
 * <p>
 * 只抽象<strong>协议特定的建流/拆流两个动作</strong>，把选节点、引用计数、占位会话、首播 future 等待、
 * 拉 PlayUrls、GC、关流去重等<strong>协议无关编排</strong>留在 {@code MediaPlayServiceImpl}。
 * 新增协议（ONVIF/RTSP）只需新增一个本接口实现并声明 {@link #supportProtocols()}，编排层零改动。
 * </p>
 * <ul>
 * <li>GB28181：establish = resolveMediaIp + openRtpServer + 发 INVITE（设备推 RTP）；terminate = closeRtpServer + sendBye</li>
 * <li>ONVIF/RTSP（未来）：establish = addStreamProxy（ZLM 主动拉流）；terminate = deleteStreamProxy</li>
 * </ul>
 *
 * @author luna
 */
public interface MediaProtocolHandler {

    /**
     * 本实现支持的纯协议类型集合（对应 {@code DeviceProtocolEnum.getType()}）。
     * 抽象方法强制显式声明，避免新协议忘声明导致静默无法路由。
     *
     * @return 支持的纯协议 type 集合
     */
    Set<Integer> supportProtocols();

    /**
     * 建流：让流以 {@code ctx.streamId} 进入 {@code ctx.node} 指定的 ZLM 节点。
     * 仅负责下发协议指令，<strong>不等待流就绪</strong>（就绪由编排层 on_stream_changed future 处理）。
     *
     * @param ctx 建流上下文
     * @return 建流结果（含协议会话标识 callId、收流端点 sdpIp/rtpPort，供编排层落库/回填）
     */
    MediaEstablishResult establish(MediaEstablishContext ctx);

    /**
     * ���止/回收：关闭收流端 + 终止协议会话。建流失败回滚与正常关流共用，须<strong>幂等</strong>。
     *
     * @param ctx 终止上下文（node/callId 可为空，对应步骤跳过）
     */
    void terminate(MediaTerminateContext ctx);
}
