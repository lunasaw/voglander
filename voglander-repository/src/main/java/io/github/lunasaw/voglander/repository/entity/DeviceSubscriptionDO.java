package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 设备订阅状态实体（GB28181-2022 §9.11）。
 * <p>
 * 每台设备 × 每类订阅一行（{@code device_id + sub_type} 唯一）。同时存「意图（enabled，用户开关）」
 * 与「运行态（callId / expireTime / status）」。
 * </p>
 *
 * @author luna
 */
@TableName(value = "tb_device_subscription")
@Data
public class DeviceSubscriptionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long          id;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 设备国标编码。
     */
    private String        deviceId;

    /**
     * 订阅类型 CATALOG / MOBILE_POSITION / ALARM。
     */
    private String        subType;

    /**
     * 意图：1=开启 0=关闭（前端开关）。
     */
    private Integer       enabled;

    /**
     * 运行态：0=INACTIVE 1=ACTIVE 2=PENDING 3=FAILED。
     */
    private Integer       status;

    /**
     * SUBSCRIBE dialog callId（refresh/unsubscribe 用）。
     */
    private String        callId;

    /**
     * 订阅有效期(秒)。
     */
    private Integer       expires;

    /**
     * 位置上报间隔(秒)，仅 MOBILE_POSITION。
     */
    private Integer       intervalSec;

    /**
     * 本次订阅过期时间(=最后下发时间+expires)，refresh 判定用。
     */
    private LocalDateTime expireTime;

    /**
     * 最近一次收到该类通知的时间。
     */
    private LocalDateTime lastNotifyTime;

    /**
     * 告警过滤等扩展(FastJSON2)。
     */
    private String        extend;
}
