package io.github.lunasaw.voglander.manager.domaon.dto.cascade;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 级联上级平台 DTO
 */
@Data
public class CascadePlatformDTO {
    private Long          id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String        platformId;
    private String        platformIp;
    private Integer       platformPort;
    private String        platformDomain;
    private String        username;
    private String        password;
    private String        localClientId;
    private String        localIp;
    private Integer       localPort;
    private Integer       enabled;
    private Integer       registerStatus;
    private Integer       keepaliveInterval;
    private Integer       registerExpires;
    private String        charset;
    private String        transport;
    private String        extend;
}
