package io.github.lunasaw.voglander.client.domain.qo;

import lombok.Data;

/**
 * @author luna
 * @date 2023/12/31
 */
@Data
public class DeviceChannelReq {

    /**
     * 设备Id
     */
    private String deviceId;

    /**
     * 通道Id
     */
    private String channelId;

    /**
     * 通道信息
     */
    private String channelInfo;

    /**
     * 通道名称
     */
    private String channelName;
}
