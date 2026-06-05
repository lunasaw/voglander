package io.github.lunasaw.voglander.web.api.live.domain;

import io.github.lunasaw.zlm.entity.PlayUrl;
import lombok.Data;

@Data
public class LivePlayVO {
    private String  streamId;
    private String  callId;
    private Integer status;
    private PlayUrl playUrls;
    private long    refCount;
}
