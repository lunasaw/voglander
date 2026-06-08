package io.github.lunasaw.voglander.intergration.wrapper.gb28181.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSON;

/**
 * SIP/GB28181 协议层消息链路追踪器。
 * <p>
 * 在协议层<strong>所有出站发送与入站接收</strong>的咽喉点，统一打印完整 JSON body 与
 * {@code callId}/{@code deviceId}，用于跨节点、跨线程追踪一次点播/控制的完整消息路径。
 * </p>
 *
 * <h3>日志检索约定</h3>
 * <ul>
 * <li>入站统一前缀 {@code [SIP-RECV]}；出站统一前缀 {@code [SIP-SEND]}。</li>
 * <li>按 callId 串联：{@code grep 'callId=<x>'}；按设备串联：{@code grep 'deviceId=<x>'}。</li>
 * </ul>
 *
 * <h3>独立 logger</h3>
 * <p>
 * 使用独立 logger 名 {@code SIP_MSG_TRACE}，便于运维单独调整级别或关闭，
 * 不影响各业务类自身的 {@code @Slf4j} 日志。默认 INFO 级别即输出完整 body；
 * 如需临时关闭：在 logback 中将 {@code <logger name="SIP_MSG_TRACE" level="OFF"/>}。
 * </p>
 *
 * @author luna
 * @since 2026-06-06
 */
public final class SipMessageTracer {

    /** 独立追踪 logger，可在 logback 中按名单独配置级别。 */
    private static final Logger TRACE = LoggerFactory.getLogger("SIP_MSG_TRACE");

    private static final String RECV  = "[SIP-RECV]";
    private static final String SEND  = "[SIP-SEND]";

    private SipMessageTracer() {}

    /**
     * 记录一条<strong>入站</strong>协议消息（设备/平台 → voglander）。
     *
     * @param type     三段式事件类型，如 {@code gb28181.Session.InviteOk}
     * @param deviceId 设备 ID（可空）
     * @param callId   关联 ID（sn / callId，可空）
     * @param nodeId   产生事件的网关节点 ID（可空）
     * @param body     原始负载对象，序列化为完整 JSON
     */
    public static void recv(String type, String deviceId, String callId, String nodeId, Object body) {
        if (!TRACE.isInfoEnabled()) {
            return;
        }
        TRACE.info("{} type={}, deviceId={}, callId={}, nodeId={}, body={}",
            RECV, type, deviceId, callId, nodeId, toJson(body));
    }

    /**
     * 记录一条<strong>出站</strong>协议消息（voglander → 设备/平台）。
     *
     * @param type     命令类型或方法名，如 {@code gb28181.Control.Ptz} / {@code sendKeepaliveCommand}
     * @param deviceId 设备 ID（可空）
     * @param callId   下发后生成的 callId（可空，发送失败时为 null）
     * @param body     命令负载对象，序列化为完整 JSON
     */
    public static void send(String type, String deviceId, String callId, Object body) {
        if (!TRACE.isInfoEnabled()) {
            return;
        }
        TRACE.info("{} type={}, deviceId={}, callId={}, body={}",
            SEND, type, deviceId, callId, toJson(body));
    }

    /**
     * 容错序列化：序列化失败时降级为 {@code toString()}，绝不影响主流程。
     */
    private static String toJson(Object body) {
        if (body == null) {
            return "null";
        }
        try {
            return JSON.toJSONString(body);
        } catch (Throwable t) {
            return "<json-serialize-failed:" + t.getClass().getSimpleName() + "> " + String.valueOf(body);
        }
    }
}
