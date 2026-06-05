package io.github.lunasaw.voglander.web.api.ptz.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PresetReq {
    @NotBlank private String  deviceId;
    @NotBlank private String  channelId;
    /** SET / GOTO / DEL */
    @NotBlank private String  action;
    private Integer presetId;
}
