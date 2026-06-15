package io.github.lunasaw.voglander.common.event;

/**
 * 本地移动位置事件（设备/通道 GPS 上报）。
 *
 * <p>由 {@code Gb28181ProtocolHandler.handleMobilePosition} 落库后发布，
 * 级联事件桥接监听后主动 NOTIFY 推送已订阅 MobilePosition 的上级。
 *
 * <p>经纬度等以 String 携带（与 DO/落库层一致，null 安全）。
 *
 * @author luna
 */
public class LocalMobilePositionEvent {

    private final String deviceId;
    private final String channelId;
    private final String time;
    private final String longitude;
    private final String latitude;
    private final String speed;
    private final String direction;
    private final String altitude;

    public LocalMobilePositionEvent(String deviceId, String channelId, String time,
        String longitude, String latitude, String speed, String direction, String altitude) {
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.time = time;
        this.longitude = longitude;
        this.latitude = latitude;
        this.speed = speed;
        this.direction = direction;
        this.altitude = altitude;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getTime() {
        return time;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getSpeed() {
        return speed;
    }

    public String getDirection() {
        return direction;
    }

    public String getAltitude() {
        return altitude;
    }
}
