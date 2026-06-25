package io.github.lunasaw.voglander.manager.domaon.dto.cascade;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 级联上级订阅 DTO
 *
 * @author luna
 */
@Data
public class CascadeSubscribeDTO {

    private Long          id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String        platformId;
    private String        subType;
    private String        callId;
    private String        sn;
    private Integer       expires;
    private Integer       intervalSec;
    private LocalDateTime expireTime;
    private Integer       status;
    private String        extend;
}
