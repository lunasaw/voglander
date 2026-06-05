package io.github.lunasaw.voglander.web.api.lab.domain;

import lombok.Data;

/** 设备信息上报请求 */
@Data
public class LabDeviceInfoPushReq {
    private String manufacturer = "Voglander";
    private String model = "LabDevice";
    private String firmware = "v1.0.6";
}
