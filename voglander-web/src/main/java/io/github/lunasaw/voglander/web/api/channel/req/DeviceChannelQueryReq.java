package io.github.lunasaw.voglander.web.api.channel.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备通道查询请求对象
 * 
 * @author luna
 * @date 2024/01/31
 */
@Data
public class DeviceChannelQueryReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据库主键ID
     */
    private Long              id;

    /**
     * 通道Id
     */
    private String            channelId;

    /**
     * 设备ID
     */
    private String            deviceId;

    /**
     * 通道名称
     */
    private String            name;

    /**
     * 状态 1在线 0离线
     */
    private Integer           status;
}