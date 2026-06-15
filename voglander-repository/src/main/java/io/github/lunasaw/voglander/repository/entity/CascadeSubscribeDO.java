package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 级联上级订阅表（上级订本平台 → 本平台据此主动推送）
 *
 * @author luna
 */
@Data
@TableName("tb_cascade_subscribe")
public class CascadeSubscribeDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long          id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 发起订阅的上级平台ID */
    private String        platformId;
    /** 订阅类型 CATALOG / ALARM / MOBILE_POSITION */
    private String        subType;
    /** 订阅 dialog 标识（框架自管，通常为空） */
    private String        callId;
    /** 订阅请求 SN */
    private String        sn;
    /** 订阅有效期(秒) */
    private Integer       expires;
    /** 位置上报间隔(秒)，仅 MOBILE_POSITION */
    private Integer       intervalSec;
    /** 过期时间(=最后订阅时间+expires) */
    private LocalDateTime expireTime;
    /** 1=ACTIVE 0=EXPIRED */
    private Integer       status;
    private String        extend;
}
