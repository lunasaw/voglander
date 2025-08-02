package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import lombok.Data;

/**
 * (DeviceDO)表实体类
 *
 * @author chenzhangyue
 * @since 2023-12-28 11:11:54
 */
@SuppressWarnings("serial")
@Data
@TableName("tb_device")
public class DeviceDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long              id;
    private LocalDateTime     createTime;
    private LocalDateTime     updateTime;
    // 设备ID
    private String            deviceId;
    // 状态 1在线 0离线
    private Integer           status;
    // 自定义名称
    private String            name;
    // IP
    private String            ip;
    // 端口
    private Integer           port;
    // 注册时间
    private LocalDateTime     registerTime;
    // 心跳时间
    private LocalDateTime     keepaliveTime;
    // 注册节点
    private String            serverIp;
    /**
     * 协议类型 {@link DeviceAgreementEnum}
     */
    private Integer           type;
    // 扩展字段
    private String            extend;

}
