package io.github.lunasaw.voglander.client.domain.device.qo;

import java.io.Serializable;

import lombok.Data;

/**
 * GB28181 设备配置下发请求（对应底层 VoglanderServerConfigCommand.configDevice）。
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DeviceConfigReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备国标 ID
     */
    private String deviceId;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 注册有效期
     */
    private String expiration;

    /**
     * 心跳间隔
     */
    private String heartBeatInterval;

    /**
     * 心跳超时次数
     */
    private String heartBeatCount;
}
