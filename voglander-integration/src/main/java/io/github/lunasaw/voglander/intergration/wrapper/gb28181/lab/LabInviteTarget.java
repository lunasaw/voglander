package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 平台 INVITE 解析出的收流目标（不可变值对象）。
 * <p>
 * 由 {@link LabMediaPushService#parseTarget} 从 {@code ClientInviteEvent} 的 SDP 提取，
 * 供回 200 OK 应答与 ffmpeg 推流地址构造使用。
 * </p>
 *
 * @author luna
 */
@Getter
@ToString
@AllArgsConstructor
public class LabInviteTarget {

    /** 本次会话 Call-ID。 */
    private final String callId;

    /** 本端设备编码（deviceId）。 */
    private final String userId;

    /** 平台收流 IP（SDP {@code c=} 行）。 */
    private final String mediaIp;

    /** 平台收流端口（SDP {@code m=} 行）。 */
    private final int    mediaPort;

    /** SSRC（SDP {@code y=} 行），应答时回显。 */
    private final String ssrc;

    /** 传输方式 UDP / TCP（SDP {@code m=} 协议字段）。 */
    private final String transport;

    /** 会话类型 Play / PlayBack（SDP {@code s=} 行）。 */
    private final String sessionType;

    /** {@code SipTransactionRegistry} 上下文键，异步回 200 OK 用。 */
    private final String ctxKey;

    /**
     * TCP 连接角色（SDP {@code a=setup:} 属性）。
     * {@code "active"} — 设备主动连平台；{@code "passive"} — 设备监听；{@code null} — UDP���
     */
    private final String tcpSetup;
}
