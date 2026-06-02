package io.github.lunasaw.voglander.support;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;

/**
 * 测试事件构建工具，统一构造 DeviceEvent 测试数据
 *
 * @author luna
 */
public class EventEmitter {

    private final String             protocol;
    private final String             group;
    private final String             name;
    private final String             deviceId;
    private String                   correlationId;
    private final Map<String, Object> payload = new HashMap<>();

    private EventEmitter(String protocol, String group, String name, String deviceId) {
        this.protocol = protocol;
        this.group = group;
        this.name = name;
        this.deviceId = deviceId;
    }

    /** eventType 格式: "Group.Name"，如 "Lifecycle.Register" */
    public static EventEmitter of(String protocol, String eventType, String deviceId) {
        String[] parts = eventType.split("\\.", 2);
        String grp = parts.length == 2 ? parts[0] : eventType;
        String nm = parts.length == 2 ? parts[1] : "";
        return new EventEmitter(protocol, grp, nm, deviceId);
    }

    public EventEmitter correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public EventEmitter payload(Map<String, Object> data) {
        payload.putAll(data);
        return this;
    }

    public EventEmitter put(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    public DeviceEvent build() {
        return new DeviceEvent(protocol, group, name, deviceId, correlationId,
            System.currentTimeMillis(), payload, null);
    }
}
