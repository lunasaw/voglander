package io.github.lunasaw.voglander.client.domain.device.qo;

import java.io.Serializable;

import lombok.Data;

/**
 * GB28181 录像控制请求（start/stop，对应底层 VoglanderServerRecordCommand.start/stopDeviceRecord）。
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DeviceRecordControlReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备国标 ID
     */
    private String  deviceId;

    /**
     * true=开始录像 / false=停止录像
     */
    private Boolean start;
}
