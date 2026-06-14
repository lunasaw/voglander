package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 设备移动位置实体（GB28181-2022 移动位置订阅落库）。
 * <p>
 * 经纬度/速度/方向/海拔在框架 {@code MobilePositionNotify} 中为 Double，存入 VARCHAR 列时统一 {@code String.valueOf} 转换。
 * </p>
 *
 * @author luna
 */
@TableName(value = "tb_device_position")
@Data
public class DevicePositionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long          id;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 设备国标编码。
     */
    private String        deviceId;

    /**
     * 通道国标编码。
     */
    private String        channelId;

    private String        longitude;

    private String        latitude;

    private String        speed;

    private String        direction;

    private String        altitude;

    /**
     * 设备上报的定位时间。
     */
    private LocalDateTime positionTime;

    private String        extend;
}
