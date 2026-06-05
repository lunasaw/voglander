package io.github.lunasaw.voglander.web.api.ptz.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PtzStopReq {
    @NotBlank private String deviceId;
    @NotBlank private String channelId;
}
