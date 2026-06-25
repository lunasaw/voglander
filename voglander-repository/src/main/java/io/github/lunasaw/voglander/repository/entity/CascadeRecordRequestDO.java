package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 级联录像查询请求上下文表（上级查录像 → 转查真实设备 → 异步聚合回包）
 *
 * @author luna
 */
@Data
@TableName("tb_cascade_record_request")
public class CascadeRecordRequestDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long          id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 发起查询的上级 */
    private String        platformId;
    /** 上级查询的 SN（回包须原样带回） */
    private String        superiorSn;
    /** 上级请求的通道（级联编码） */
    private String        cascadeChannelId;
    /** 转查的真实设备 */
    private String        localDeviceId;
    /** 转查的真实通道 */
    private String        localChannelId;
    /** 诊断字段（server 录像命令不回传 sn，关联走 deviceId+时间窗） */
    private String        localSn;
    private String        startTime;
    private String        endTime;
    /** 0=PENDING 1=RESPONDED 2=TIMEOUT */
    private Integer       status;
    private String        extend;
}
