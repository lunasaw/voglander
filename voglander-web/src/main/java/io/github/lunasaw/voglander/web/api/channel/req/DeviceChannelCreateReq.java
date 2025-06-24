package io.github.lunasaw.voglander.web.api.channel.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备通道创建请求对象
 * @author luna
 * @date 2024/01/30
 */
@Data
public class DeviceChannelCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通道Id
     */
    private String channelId;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 通道名称
     */
    private String name;

    /**
     * 扩展信息
     */
    private ExtendInfoReq extendInfo;

    @Data
    public static class ExtendInfoReq {
        /**
         * 设备通道信息
         */
        private String channelInfo;
    }
}