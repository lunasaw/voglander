package io.github.lunasaw.voglander.common.event;

/**
 * 本地通道变更事件（通道上/下线、增删改）。
 *
 * <p>由 {@code Gb28181ProtocolHandler} 在目录响应/通道状态变更后发布，
 * 级联事件桥接 {@code CascadeEventBridge} 监听后主动 NOTIFY 推送已订阅 Catalog 的上级。
 *
 * <p>event 取值对齐 GB28181 目录更新通知：ON / OFF / ADD / DEL / UPDATE。
 *
 * @author luna
 */
public class LocalChannelChangeEvent {

    private final String deviceId;
    private final String channelId;
    private final String event;

    public LocalChannelChangeEvent(String deviceId, String channelId, String event) {
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.event = event;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getEvent() {
        return event;
    }
}
