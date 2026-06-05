package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 告警表实体类。
 * <p>
 * 由 GB28181 {@code Notify.Alarm} 事件经 {@code Gb28181ProtocolHandler} 落库，供告警中心分页查询与确认。
 * </p>
 *
 * @author luna
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_alarm")
public class AlarmDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long              id;

    /**
     * 创建时间
     */
    private LocalDateTime     createTime;

    /**
     * 修改时间
     */
    private LocalDateTime     updateTime;

    /**
     * 设备 GB28181 编码
     */
    private String            deviceId;

    /**
     * 通道 GB28181 编码
     */
    private String            channelId;

    /**
     * 告警类型 1移动侦测 2设备告警 3故障 4视频丢失
     */
    private Integer           alarmType;

    /**
     * 告警级别 1-4，4 最高
     */
    private Integer           alarmLevel;

    /**
     * 告警时间
     */
    private LocalDateTime     alarmTime;

    /**
     * 告警描述
     */
    private String            description;

    /**
     * 确认状态 0未确认 1已确认
     */
    private Integer           ackStatus;

    /**
     * 扩展字段（JSON）
     */
    private String            extend;
}
