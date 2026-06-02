package io.github.lunasaw.voglander.client.domain.device.qo;

import lombok.Data;

/**
 * 协议无关的回放请求
 */
@Data
public class DevicePlaybackReq {
    private String deviceId;
    private String sdpIp;
    private Integer mediaPort;
    /** 流模式：UDP / TCP_ACTIVE / TCP_PASSIVE，默认UDP */
    private String streamMode = "UDP";
    private String startTime;
    private String endTime;
}
