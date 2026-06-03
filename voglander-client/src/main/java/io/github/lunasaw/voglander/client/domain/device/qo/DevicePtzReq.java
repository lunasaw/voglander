package io.github.lunasaw.voglander.client.domain.device.qo;

import lombok.Data;

/**
 * 协议无关的 PTZ 控制请求
 */
@Data
public class DevicePtzReq {
    private String deviceId;
    /** PTZ 方向/动作，见 PTZControlEnum.name() */
    private String control;
    /** 速度（0-255），默认128 */
    private Integer speed = 128;
}
