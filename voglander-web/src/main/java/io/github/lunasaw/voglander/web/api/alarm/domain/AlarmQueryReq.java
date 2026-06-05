package io.github.lunasaw.voglander.web.api.alarm.domain;

import lombok.Data;

@Data
public class AlarmQueryReq {
    private String  deviceId;
    private Integer alarmLevel;
    private Integer alarmType;
    private String  startTime;
    private String  endTime;
    private int     page = 1;
    private int     size = 20;
}
