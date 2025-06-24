package io.github.lunasaw.voglander.web.api.channel.vo;

import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备通道 VO 模型
 * @author luna
 * @date 2024/01/30
 */
@Data
public class DeviceChannelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Date createTime;
    private Date updateTime;

    /**
     * 状态 1在线 0离线
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

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
     * 扩展字段
     */
    private String extend;

    /**
     * 扩展信息
     */
    private ExtendInfoVO extendInfo;

    /**
     * 获取状态显示名称
     */
    private static String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "在线" : "离线";
    }

    /**
     * 将DeviceChannelDTO转换为DeviceChannelVO
     */
    public static DeviceChannelVO convertVO(DeviceChannelDTO dto) {
        if (dto == null) {
            return null;
        }

        DeviceChannelVO vo = new DeviceChannelVO();
        vo.setId(dto.getId());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setStatus(dto.getStatus());
        vo.setStatusName(getStatusName(dto.getStatus()));
        vo.setChannelId(dto.getChannelId());
        vo.setDeviceId(dto.getDeviceId());
        vo.setName(dto.getName());
        vo.setExtend(dto.getExtend());

        // 转换扩展信息
        if (dto.getExtendInfo() != null) {
            ExtendInfoVO extendInfoVO = new ExtendInfoVO();
            extendInfoVO.setChannelInfo(dto.getExtendInfo().getChannelInfo());
            vo.setExtendInfo(extendInfoVO);
        }

        return vo;
    }

    @Data
    public static class ExtendInfoVO {
        /**
         * 设备通道信息
         */
        private String channelInfo;
    }
}