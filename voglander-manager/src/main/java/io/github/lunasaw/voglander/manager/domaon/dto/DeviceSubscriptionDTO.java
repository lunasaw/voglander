package io.github.lunasaw.voglander.manager.domaon.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;

import io.github.lunasaw.voglander.repository.entity.DeviceSubscriptionDO;
import lombok.Data;

/**
 * 设备订阅状态 DTO（GB28181-2022 §9.11）。
 *
 * @author luna
 */
@Data
public class DeviceSubscriptionDTO {

    private Long          id;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String        deviceId;

    /**
     * 订阅类型 CATALOG / MOBILE_POSITION / ALARM。
     */
    private String        subType;

    /**
     * 意图：1=开启 0=关闭。
     */
    private Integer       enabled;

    /**
     * 运行态：0=INACTIVE 1=ACTIVE 2=PENDING 3=FAILED。
     */
    private Integer       status;

    private String        callId;

    private Integer       expires;

    private Integer       intervalSec;

    private LocalDateTime expireTime;

    private LocalDateTime lastNotifyTime;

    private String        extend;

    // ================ 转换方法 ================

    public static DeviceSubscriptionDTO convertDTO(DeviceSubscriptionDO src) {
        if (src == null) {
            return null;
        }
        DeviceSubscriptionDTO dto = new DeviceSubscriptionDTO();
        dto.setId(src.getId());
        dto.setCreateTime(src.getCreateTime());
        dto.setUpdateTime(src.getUpdateTime());
        dto.setDeviceId(src.getDeviceId());
        dto.setSubType(src.getSubType());
        dto.setEnabled(src.getEnabled());
        dto.setStatus(src.getStatus());
        dto.setCallId(src.getCallId());
        dto.setExpires(src.getExpires());
        dto.setIntervalSec(src.getIntervalSec());
        dto.setExpireTime(src.getExpireTime());
        dto.setLastNotifyTime(src.getLastNotifyTime());
        dto.setExtend(src.getExtend());
        return dto;
    }

    public static DeviceSubscriptionDO convertDO(DeviceSubscriptionDTO dto) {
        if (dto == null) {
            return null;
        }
        DeviceSubscriptionDO src = new DeviceSubscriptionDO();
        src.setId(dto.getId());
        src.setCreateTime(dto.getCreateTime());
        src.setUpdateTime(dto.getUpdateTime());
        src.setDeviceId(dto.getDeviceId());
        src.setSubType(dto.getSubType());
        src.setEnabled(dto.getEnabled());
        src.setStatus(dto.getStatus());
        src.setCallId(dto.getCallId());
        src.setExpires(dto.getExpires());
        src.setIntervalSec(dto.getIntervalSec());
        src.setExpireTime(dto.getExpireTime());
        src.setLastNotifyTime(dto.getLastNotifyTime());
        src.setExtend(dto.getExtend());
        return src;
    }

    // ================ 时间转换领域方法 ================

    public Long createTimeToEpochMilli() {
        return createTime != null ? createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    public Long updateTimeToEpochMilli() {
        return updateTime != null ? updateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    public Long expireTimeToEpochMilli() {
        return expireTime != null ? expireTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    public Long lastNotifyTimeToEpochMilli() {
        return lastNotifyTime != null ? lastNotifyTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }
}
