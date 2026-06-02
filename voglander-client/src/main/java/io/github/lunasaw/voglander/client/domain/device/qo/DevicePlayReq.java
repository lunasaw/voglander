package io.github.lunasaw.voglander.client.domain.device.qo;

import lombok.Data;

/**
 * 协议无关的实时播放请求
 */
@Data
public class DevicePlayReq {
    private String deviceId;
    private String sdpIp;
    private Integer mediaPort;
    /** 流模式：UDP / TCP_ACTIVE / TCP_PASSIVE，默认UDP */
    private String streamMode = "UDP";
}
