package io.github.lunasaw.voglander.web.api.live.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LiveStartReq {
    @NotBlank private String deviceId;
    @NotBlank private String channelId;
    private String protocol   = "FLV";
    private String streamMode = "UDP";
}
