package io.github.lunasaw.voglander.web.api.alarm.domain;

import java.util.List;
import lombok.Data;

@Data
public class AlarmListResp {
    private Long            total;
    private List<AlarmVO>   items;
}
