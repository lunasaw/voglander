package io.github.lunasaw.voglander.web.api.ptz.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PtzControlReq {
    @NotBlank private String  deviceId;
    @NotBlank private String  channelId;
    /**
     * PTZ 指令词表（大小写无关）：UP/DOWN/LEFT/RIGHT/UP_LEFT/UP_RIGHT/DOWN_LEFT/DOWN_RIGHT/ZOOM_IN/ZOOM_OUT/STOP。
     * 门面层翻译为 PTZControlEnum 规范枚举后下发；亦兼容直接传规范枚举名（如 TILT_UP）。
     */
    @NotBlank private String  command;
    private Integer speed = 128;
}
