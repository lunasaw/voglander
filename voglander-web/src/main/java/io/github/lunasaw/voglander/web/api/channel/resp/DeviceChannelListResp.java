package io.github.lunasaw.voglander.web.api.channel.resp;

import io.github.lunasaw.voglander.web.api.channel.vo.DeviceChannelVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 设备通道列表响应对象
 * 
 * @author luna
 * @date 2024/01/31
 */
@Data
public class DeviceChannelListResp implements Serializable {

    private static final long     serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private Long                  total;

    /**
     * 设备通道列表
     */
    private List<DeviceChannelVO> items;
}