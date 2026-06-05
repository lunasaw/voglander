package io.github.lunasaw.voglander.client.domain.device.qo;

import lombok.Data;

/**
 * 协议无关的 PTZ 控制请求
 */
@Data
public class DevicePtzReq {
    private String deviceId;
    /**
     * PTZ 方向/动作。接受前端约定词表 {@code UP/DOWN/LEFT/RIGHT/UP_LEFT/UP_RIGHT/DOWN_LEFT/DOWN_RIGHT/ZOOM_IN/ZOOM_OUT/STOP}
     * （大小写无关），由门面层 {@code GbDeviceCommandService} 翻译为 {@code PTZControlEnum} 规范枚举；
     * 亦兼容直接传规范枚举名（如 {@code TILT_UP}）。
     */
    private String control;
    /** 速度（1-255），默认128 */
    private Integer speed = 128;
}
