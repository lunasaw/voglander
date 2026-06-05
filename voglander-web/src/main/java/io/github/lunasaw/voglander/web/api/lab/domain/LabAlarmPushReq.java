package io.github.lunasaw.voglander.web.api.lab.domain;

import lombok.Data;

/** 告警上报请求 */
@Data
public class LabAlarmPushReq {
    private int alarmType = 1;
    private int priority = 1;
    private String channelId;
}
