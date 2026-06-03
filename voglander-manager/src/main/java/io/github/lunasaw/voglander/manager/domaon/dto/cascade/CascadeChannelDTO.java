package io.github.lunasaw.voglander.manager.domaon.dto.cascade;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 级联上报通道 DTO
 */
@Data
public class CascadeChannelDTO {
    private Long          id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String        platformId;
    private String        localDeviceId;
    private String        localChannelId;
    private String        cascadeChannelId;
    private String        cascadeName;
    private Integer       enabled;
}
