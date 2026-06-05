package io.github.lunasaw.voglander.web.api.ptz.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PtzControlReq {
    @NotBlank private String  deviceId;
    @NotBlank private String  channelId;
    /** UP/DOWN/LEFT/RIGHT/UP_LEFT/UP_RIGHT/DOWN_LEFT/DOWN_RIGHT/ZOOM_IN/ZOOM_OUT/STOP (PTZControlEnum) */
    @NotBlank private String  command;
    private Integer speed = 128;
}
