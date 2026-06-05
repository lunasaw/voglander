package io.github.lunasaw.voglander.web.api.alarm.domain;

import lombok.Data;

@Data
public class AlarmVO {
    private Long    id;
    private String  deviceId;
    private String  channelId;
    private Integer alarmType;
    private Integer alarmLevel;
    private Long    alarmTime;
    private String  description;
    private Integer ackStatus;
}
