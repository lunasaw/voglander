package io.github.lunasaw.voglander.service.live.protocol;

import io.github.lunasaw.zlm.config.ZlmNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 媒体终止/回收上下文（PROTOCOL-S5）。
 * <p>
 * {@link MediaProtocolHandler#terminate} 的入参，建流失败回滚与正常关流共用。
 * 协议特定收尾：GB28181 = closeRtpServer（按 node）+ sendBye（按 callId）；ONVIF/RTSP = deleteStreamProxy。
 * 实现须<strong>幂等</strong>，缺字段（node/callId 为空）时跳过对应步骤而非报错。
 * </p>
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTerminateContext {

    /**
     * 会话所在 ZLM 节点（closeRtpServer 用）；解析失败/无会话时可为空，实现跳过收流端清理。
     */
    private ZlmNode node;

    /**
     * 流名 streamId。
     */
    private String  streamId;

    /**
     * 协议会话标识（GB28181 SIP Call-ID，sendBye 用）；为空则跳过会话终止。
     */
    private String  callId;

    /**
     * 终止原因（日志/SSE 透传）。
     */
    private String  reason;
}
