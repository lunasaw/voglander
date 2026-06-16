package io.github.lunasaw.voglander.manager.domaon.dto.cascade;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    /**
     * 创建时间转 Unix 毫秒时间戳（Web VO 出参规约）。
     *
     * @return unix 时间戳（毫秒），createTime 为空返回 null
     */
    public Long createTimeToEpochMilli() {
        return createTime != null ? createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    /**
     * 更新时间转 Unix 毫秒时间戳（Web VO 出参规约）。
     *
     * @return unix 时间戳（毫秒），updateTime 为空返回 null
     */
    public Long updateTimeToEpochMilli() {
        return updateTime != null ? updateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }
}
