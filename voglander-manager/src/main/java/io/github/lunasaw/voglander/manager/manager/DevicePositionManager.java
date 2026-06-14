package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.service.DevicePositionService;
import io.github.lunasaw.voglander.repository.entity.DevicePositionDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备移动位置落库管理器（GB28181-2022 移动位置订阅）。
 * <p>
 * 位置高频推送（默认 5s/次），每次落一条轨迹。经纬度等字段由调用方（{@code Gb28181ProtocolHandler}）
 * 从框架 {@code MobilePositionNotify}（Double）转 String 后传入，本管理器与框架实体解耦。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class DevicePositionManager {

    @Autowired
    private DevicePositionService positionService;

    /**
     * 记录一条移动位置。
     *
     * @param deviceId     设备国标编码（必填）
     * @param channelId    通道国标编码（可空）
     * @param longitude    经度
     * @param latitude     纬度
     * @param speed        速度
     * @param direction    方向
     * @param altitude     海拔
     * @param positionTime 设备上报的定位时间（可空）
     * @return 落库记录主键 ID；deviceId 为空返回 null
     */
    public Long record(String deviceId, String channelId, String longitude, String latitude,
        String speed, String direction, String altitude, LocalDateTime positionTime) {
        if (deviceId == null || deviceId.isEmpty()) {
            log.warn("记录移动位置失败：deviceId 为空");
            return null;
        }
        DevicePositionDO src = new DevicePositionDO();
        LocalDateTime now = LocalDateTime.now();
        src.setDeviceId(deviceId);
        src.setChannelId(channelId);
        src.setLongitude(longitude);
        src.setLatitude(latitude);
        src.setSpeed(speed);
        src.setDirection(direction);
        src.setAltitude(altitude);
        src.setPositionTime(positionTime);
        src.setCreateTime(now);
        src.setUpdateTime(now);
        positionService.save(src);
        return src.getId();
    }
}
