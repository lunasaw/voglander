package io.github.lunasaw.voglander.manager.domaon.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 告警数据传输对象。
 *
 * @author luna
 */
@Data
public class AlarmDTO {

    /**
     * 主键ID
     */
    private Long          id;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 设备 GB28181 编码
     */
    private String        deviceId;

    /**
     * 通道 GB28181 编码
     */
    private String        channelId;

    /**
     * 告警类型 1移动侦测 2设备告警 3故障 4视频丢失
     */
    private Integer       alarmType;

    /**
     * 告警级别 1-4，4 最高
     */
    private Integer       alarmLevel;

    /**
     * 告警时间
     */
    private LocalDateTime alarmTime;

    /**
     * 告警描述
     */
    private String        description;

    /**
     * 确认状态 0未确认 1已确认
     */
    private Integer       ackStatus;

    /**
     * 扩展字段（JSON）
     */
    private String        extend;

    /**
     * 告警时间转 Unix 毫秒时间戳（VO 层使用）。
     *
     * @return Unix 毫秒，alarmTime 为空返回 null
     */
    public Long alarmTimeToEpochMilli() {
        return alarmTime == null ? null
            : alarmTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
