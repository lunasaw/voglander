package io.github.lunasaw.voglander.web.api.device.req;

import java.io.Serializable;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import lombok.Data;

/**
 * 设备分页条件查询请求（Web 入参，S1 设备列表筛选）。
 *
 * <p>
 * 命名说明：client 层已存在 {@code DeviceQueryReq}（qo 包，被命令门面 queryChannel/queryDevice 占用），
 * 为避免同名不同包导致 import 混淆，本类命名为 {@code DevicePageReq}（语义=分页条件查询请求）。
 * </p>
 *
 * <p>
 * 时间字段统一 Unix 毫秒（Web 出入参规约），由 DeviceWebAssembler 转 LocalDateTime 后下传 Manager。
 * subType/protocol 是 VO 派生展示字段、DeviceDO 无对应列，不可作筛选条件，故不在此出现。
 * </p>
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DevicePageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long    id;

    /**
     * 设备国标 ID（精确匹配）
     */
    private String  deviceId;

    /**
     * 设备名称（模糊匹配）
     */
    private String  name;

    /**
     * 在线状态 1在线 / 0离线
     */
    private Integer status;

    /**
     * 协议类型 {@link DeviceAgreementEnum}
     */
    private Integer type;

    /**
     * 设备 IP（模糊匹配）
     */
    private String  ip;

    /**
     * 注册节点 IP（精确匹配）
     */
    private String  serverIp;

    /**
     * 心跳时间范围起（Unix 毫秒，含）
     */
    private Long    keepaliveTimeStart;

    /**
     * 心跳时间范围止（Unix 毫秒，含）
     */
    private Long    keepaliveTimeEnd;

    /**
     * 注册时间范围起（Unix 毫秒，含）
     */
    private Long    registerTimeStart;

    /**
     * 注册时间范围止（Unix 毫秒，含）
     */
    private Long    registerTimeEnd;
}
