package io.github.lunasaw.voglander.client.domain.device.qo;

import java.io.Serializable;

import lombok.Data;

/**
 * GB28181 报警查询请求（对应底层 VoglanderServerAlarmCommand.queryDeviceAlarm）。
 *
 * <p>
 * 时间为 Unix 毫秒（Web 入参规约）；门面内转底层所需 Date。
 * </p>
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DeviceAlarmQueryReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备国标 ID
     */
    private String deviceId;

    /**
     * 起始时间（Unix 毫秒）
     */
    private Long   startTime;

    /**
     * 结束时间（Unix 毫秒）
     */
    private Long   endTime;

    /**
     * 报警起始级别
     */
    private String startPriority;

    /**
     * 报警终止级别
     */
    private String endPriority;

    /**
     * 报警方式条件
     */
    private String alarmMethod;

    /**
     * 报警类型
     */
    private String alarmType;
}
