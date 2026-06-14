package io.github.lunasaw.voglander.common.constant.protocol;

/**
 * 协议名常量（PROTOCOL-S2：单一事实源）。
 * <p>
 * 收敛全项目散落的裸协议名字面值（如 {@code "gb28181"}）。事件三段式 type 的协议前缀段、
 * {@code ProtocolEventHandler#protocol()} 返回值、{@code DeviceProtocolEnum} 编码等均应引用此处。
 * </p>
 *
 * @author luna
 */
public final class ProtocolConstants {

    /**
     * GB28181 国标协议名（小写，三段式 type 的协议前缀段）。
     */
    public static final String GB28181 = "gb28181";

    private ProtocolConstants() {}
}
