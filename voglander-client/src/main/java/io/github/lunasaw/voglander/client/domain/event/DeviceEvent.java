package io.github.lunasaw.voglander.client.domain.event;

import java.util.Map;
import java.util.Objects;

/**
 * Voglander 自有归一化设备事件（Phase 3 / PROTOCOL S1）。
 * <p>
 * 各 gateway 适配器负责把原生事件翻译成本模型，与具体 gateway 产品（sip-gateway 等）解耦。
 * <strong>不含任何 sip-gateway 框架类型</strong>，可被各层引用，是业务核心与任意 gateway 之间的入站端口模型。
 * </p>
 * <p>
 * 设计为不可变值对象（accessor 命名对齐 record 风格 {@code protocol()/group()/...}）。
 * 因 voglander-client 模块按 Java 8 源码级编译，此处用普通 final 类而非 record。
 * </p>
 *
 * @author luna
 */
public final class DeviceEvent {

    /** 协议标识，对应 {@code DeviceProtocolEnum} 的 code 名（小写），如 "gb28181"、"onvif"。 */
    private final String              protocol;
    /** 事件分组，如 "Lifecycle"、"Notify"、"Response"、"Session"。 */
    private final String              group;
    /** 事件名，如 "Register"、"Keepalive"、"InviteOk"。 */
    private final String              name;
    /** 设备 ID（可空，如纯 callId 类会话事件）。 */
    private final String              deviceId;
    /** 关联 ID（sn / callId）。 */
    private final String              correlationId;
    /** 事件时间戳（毫秒）。 */
    private final long                timestampMs;
    /** 原始负载（FastJSON2 可反序列化的 Map），由各 handler 自行解析。 */
    private final Map<String, Object> payload;
    /** 产生事件的网关节点 ID。 */
    private final String              nodeId;

    public DeviceEvent(String protocol, String group, String name, String deviceId,
        String correlationId, long timestampMs, Map<String, Object> payload, String nodeId) {
        this.protocol = protocol;
        this.group = group;
        this.name = name;
        this.deviceId = deviceId;
        this.correlationId = correlationId;
        this.timestampMs = timestampMs;
        this.payload = payload;
        this.nodeId = nodeId;
    }

    public String protocol() {
        return protocol;
    }

    public String group() {
        return group;
    }

    public String name() {
        return name;
    }

    public String deviceId() {
        return deviceId;
    }

    public String correlationId() {
        return correlationId;
    }

    public long timestampMs() {
        return timestampMs;
    }

    public Map<String, Object> payload() {
        return payload;
    }

    public String nodeId() {
        return nodeId;
    }

    /**
     * 三段式类型，如 {@code "gb28181.Lifecycle.Register"}。
     */
    public String type() {
        return protocol + "." + group + "." + name;
    }

    /**
     * 两段式 group.name，如 {@code "Lifecycle.Register"}，供 handler 内二次路由。
     */
    public String groupName() {
        return group + "." + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeviceEvent)) {
            return false;
        }
        DeviceEvent that = (DeviceEvent) o;
        return timestampMs == that.timestampMs
            && Objects.equals(protocol, that.protocol)
            && Objects.equals(group, that.group)
            && Objects.equals(name, that.name)
            && Objects.equals(deviceId, that.deviceId)
            && Objects.equals(correlationId, that.correlationId)
            && Objects.equals(payload, that.payload)
            && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, group, name, deviceId, correlationId, timestampMs, payload, nodeId);
    }

    @Override
    public String toString() {
        return "DeviceEvent{type=" + type() + ", deviceId=" + deviceId
            + ", correlationId=" + correlationId + ", nodeId=" + nodeId + ", timestampMs=" + timestampMs + "}";
    }
}
