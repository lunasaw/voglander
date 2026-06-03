package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 级联上级平台表
 */
@Data
@TableName("tb_cascade_platform")
public class CascadePlatformDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long          id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 上级国标ID (SIP Server ID) */
    private String        platformId;
    private String        platformIp;
    private Integer       platformPort;
    private String        platformDomain;
    private String        username;
    private String        password;

    /** 本地模拟客户端ID */
    private String        localClientId;
    private String        localIp;
    private Integer       localPort;

    /** 1-启用 0-禁用 */
    private Integer       enabled;
    /** 0-离线 1-在线 2-注册中 3-失败 */
    private Integer       registerStatus;
    private Integer       keepaliveInterval;
    private Integer       registerExpires;
    private String        charset;
    private String        transport;
    private String        extend;
}
