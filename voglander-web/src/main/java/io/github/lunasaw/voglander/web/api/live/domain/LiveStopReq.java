package io.github.lunasaw.voglander.web.api.live.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LiveStopReq {
    @NotBlank private String streamId;
}
