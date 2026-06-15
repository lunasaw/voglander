package io.github.lunasaw.voglander.manager.domaon.dto.cascade;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 级联录像查询请求上下文 DTO
 *
 * @author luna
 */
@Data
public class CascadeRecordRequestDTO {

    private Long          id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String        platformId;
    private String        superiorSn;
    private String        cascadeChannelId;
    private String        localDeviceId;
    private String        localChannelId;
    private String        localSn;
    private String        startTime;
    private String        endTime;
    private Integer       status;
    private String        extend;
}
