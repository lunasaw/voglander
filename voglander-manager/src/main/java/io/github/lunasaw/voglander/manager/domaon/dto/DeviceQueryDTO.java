package io.github.lunasaw.voglander.manager.domaon.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import lombok.Data;

/**
 * 设备分页条件查询 DTO（Manager 对外查询条件，协议无关）。
 *
 * <p>
 * 设计说明：
 * </p>
 * <ul>
 * <li>时间字段统一用 {@link LocalDateTime}（DO/DTO 层规约），Web 层 Unix 毫秒由 DeviceWebAssembler 转换。</li>
 * <li>仅承载 DeviceDO 真实存在的列对应字段；subType/protocol 是 VO 派生展示字段、DO 无对应列，故不在此出现。</li>
 * <li>所有字段可选，由 DeviceManager.getPage 用带 condition 的 LambdaQueryWrapper 组装。</li>
 * </ul>
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DeviceQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long              id;

    /**
     * 设备国标 ID（精确匹配）
     */
    private String            deviceId;

    /**
     * 设备名称（模糊匹配）
     */
    private String            name;

    /**
     * 在线状态 1在线 / 0离线
     */
    private Integer           status;

    /**
     * 协议类型 {@link DeviceAgreementEnum}（DO 仅有 type 列，可筛）
     */
    private Integer           type;

    /**
     * 设备 IP（模糊匹配）
     */
    private String            ip;

    /**
     * 注册节点 IP（精确匹配��
     */
    private String            serverIp;

    /**
     * 心跳时间范围起（含）
     */
    private LocalDateTime     keepaliveTimeStart;

    /**
     * 心跳时间范围止（含）
     */
    private LocalDateTime     keepaliveTimeEnd;

    /**
     * 注册时间范围起（含）
     */
    private LocalDateTime     registerTimeStart;

    /**
     * 注册时间范围止（含）
     */
    private LocalDateTime     registerTimeEnd;
}
