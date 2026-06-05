package io.github.lunasaw.voglander.web.api.live.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecordQueryReq {
    @NotBlank private String deviceId;
    @NotBlank private String channelId;
    @NotBlank private String startTime;
    @NotBlank private String endTime;
}
