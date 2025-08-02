package io.github.lunasaw.voglander.manager.domaon.dto;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceChannelReq;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * @author luna
 * @date 2023/12/31
 */
@Data
public class DeviceChannelDTO {

    private Long id;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 状态 1在线 0离线
     */
    private Integer status;
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


    private ExtendInfo extendInfo;

    public static DeviceChannelDTO convertDTO(DeviceChannelDO deviceChannelDO) {
        if (deviceChannelDO == null) {
            return null;
        }
        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setId(deviceChannelDO.getId());
        dto.setCreateTime(deviceChannelDO.getCreateTime());
        dto.setUpdateTime(deviceChannelDO.getUpdateTime());
        dto.setStatus(deviceChannelDO.getStatus());
        dto.setChannelId(deviceChannelDO.getChannelId());
        dto.setDeviceId(deviceChannelDO.getDeviceId());
        dto.setName(deviceChannelDO.getName());
        dto.setExtend(deviceChannelDO.getExtend());
        dto.setExtendInfo(getExtendObj(deviceChannelDO.getExtend()));
        return dto;
    }

    public static DeviceChannelDO convertDO(DeviceChannelDTO dto) {
        if (dto == null) {
            return null;
        }
        DeviceChannelDO deviceChannelDO = new DeviceChannelDO();
        deviceChannelDO.setId(dto.getId());
        deviceChannelDO.setCreateTime(dto.getCreateTime());
        deviceChannelDO.setUpdateTime(dto.getUpdateTime());
        deviceChannelDO.setStatus(dto.getStatus());
        deviceChannelDO.setChannelId(dto.getChannelId());
        deviceChannelDO.setDeviceId(dto.getDeviceId());
        deviceChannelDO.setName(dto.getName());
        deviceChannelDO.setExtend(JSON.toJSONString(dto.getExtendInfo()));
        return deviceChannelDO;
    }

    public static DeviceChannelDTO req2dto(DeviceChannelReq req) {
        if (req == null) {
            return null;
        }
        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setStatus(DeviceConstant.Status.ONLINE);
        dto.setDeviceId(req.getDeviceId());
        dto.setName(req.getChannelName());
        dto.setChannelId(req.getChannelId());
        ExtendInfo extendInfo = new ExtendInfo();
        extendInfo.setChannelInfo(req.getChannelInfo());
        dto.setExtendInfo(extendInfo);

        return dto;
    }

    private static ExtendInfo getExtendObj(String extentInfo) {
        if (StringUtils.isBlank(extentInfo)) {
            return new ExtendInfo();
        }
        String extend = Optional.of(extentInfo).orElse(StringUtils.EMPTY);
        return Optional.ofNullable(JSON.parseObject(extend, ExtendInfo.class)).orElse(new ExtendInfo());
    }

    @Data
    public static class ExtendInfo {
        /**
         * 设备通道信息
         */
        private String channelInfo;
    }

    // ================ 时间转换领域方法 ================


    /**
     * 获取创建时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long createTimeToEpochMilli() {
        return createTime != null ? createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    /**
     * 获取更新时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long updateTimeToEpochMilli() {
        return updateTime != null ? updateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }
}
