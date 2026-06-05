package io.github.lunasaw.voglander.common.event;

import java.util.Map;

/**
 * 告警新建事件（由 Gb28181ProtocolHandler 发布，SseEventBus 监听推送 alarm.new）。
 * 携带原始 payload map，避免依赖 manager 层 DTO。
 */
public class AlarmCreatedEvent {

    private final String              deviceId;
    private final Map<String, Object> payload;

    public AlarmCreatedEvent(String deviceId, Map<String, Object> payload) {
        this.deviceId = deviceId;
        this.payload = payload;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
