package io.github.lunasaw.voglander.web.api.live.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaybackControlReq {
    @NotBlank private String streamId;
    /** PLAY_RESUME / PLAY_RANGE / PLAY_SPEED / PLAY_NOW */
    @NotBlank private String action;
    private String param;
}
