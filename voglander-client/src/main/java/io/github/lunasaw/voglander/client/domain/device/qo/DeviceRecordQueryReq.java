package io.github.lunasaw.voglander.client.domain.device.qo;

import java.io.Serializable;

import lombok.Data;

/**
 * GB28181 录像查询请求（对应底层 VoglanderServerRecordCommand.queryDeviceRecord）。
 *
 * <p>
 * 时间为 Unix 毫秒（Web 入参规约）；门面内转底层所需的秒级 long。
 * </p>
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DeviceRecordQueryReq implements Serializable {

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
}
