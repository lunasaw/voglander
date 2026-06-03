package io.github.lunasaw.voglander.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @TableName tb_device_channel
 */
@TableName(value = "tb_device_channel")
@Data
public class DeviceChannelDO implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 创建时间
     */
    private LocalDateTime     createTime;
    /**
     * 修改时间
     */
    private LocalDateTime     updateTime;
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

    /**
     * 通道最近一次被目录/会话感知的时间（1.0.4）
     */
    private LocalDateTime lastSeenTime;

    /**
     * 当前 status 的来源：CATALOG / OFFLINE_CASCADE / SESSION / MANUAL / MISSING（1.0.4）
     */
    private String statusSource;

    /**
     * 连续目录响应中未出现的次数（1.0.4 Stage 4）
     */
    private Integer missingCount;
}