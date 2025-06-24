package io.github.lunasaw.voglander.web.api.channel.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备通道更新请求对象
 * @author luna
 * @date 2024/01/30
 */
@Data
public class DeviceChannelUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（更新必需）
     */
    private Long id;

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
     * 状态 1在线 0离线
     */
    private Integer status;

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