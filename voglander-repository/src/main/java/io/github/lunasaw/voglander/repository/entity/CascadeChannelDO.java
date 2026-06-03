package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 级联上报通道表
 */
@Data
@TableName("tb_cascade_channel")
public class CascadeChannelDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long          id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String        platformId;
    private String        localDeviceId;
    private String        localChannelId;

    /** 上报给上级平台时使用的通道ID，默认同 localChannelId */
    private String        cascadeChannelId;
    private String        cascadeName;
    /** 1-上报 0-不上报 */
    private Integer       enabled;
}
