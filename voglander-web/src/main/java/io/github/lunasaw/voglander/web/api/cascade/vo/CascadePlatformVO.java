package io.github.lunasaw.voglander.web.api.cascade.vo;

import java.io.Serializable;

import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import lombok.Data;

/**
 * 级联上级平台 VO（Web 出参，时间字段统一 Unix 毫秒）。
 *
 * <p>
 * 字段名与 {@code tb_cascade_platform} / {@link CascadePlatformDTO} 对齐：
 * platformIp / platformPort / platformDomain / localIp / localPort（非 serverIp 等臆造名）。
 * </p>
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadePlatformVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long    id;

    /**
     * 创建时间（unix 时间戳，毫秒级）
     */
    private Long    createTime;

    /**
     * 更新时间（unix 时间戳，毫秒级）
     */
    private Long    updateTime;

    /**
     * 上级平台国标 ID（20 位）
     */
    private String  platformId;

    /**
     * 上级平台 IP
     */
    private String  platformIp;

    /**
     * 上级平台端口
     */
    private Integer platformPort;

    /**
     * 上级平台域（SIP domain）
     */
    private String  platformDomain;

    /**
     * 认证用户名
     */
    private String  username;

    /**
     * 认证密码
     */
    private String  password;

    /**
     * 本端客户端国标 ID（20 位）
     */
    private String  localClientId;

    /**
     * 本端 IP
     */
    private String  localIp;

    /**
     * 本端端口
     */
    private Integer localPort;

    /**
     * 启用状态 1启用 / 0停用
     */
    private Integer enabled;

    /**
     * 注册状态 0离线 / 1在线 / 2注册中 / 3失败
     */
    private Integer registerStatus;

    /**
     * 注册状态显示名称
     */
    private String  registerStatusName;

    /**
     * 保活心跳间隔（秒）
     */
    private Integer keepaliveInterval;

    /**
     * 注册有效期（秒）
     */
    private Integer registerExpires;

    /**
     * 编码（GB2312/UTF-8）
     */
    private String  charset;

    /**
     * 传输协议 UDP / TCP
     */
    private String  transport;

    /**
     * 扩展字段
     */
    private String  extend;

    /**
     * DTO → VO 转换（时间转 Unix 毫秒，registerStatus 派生中文名）。
     *
     * @param dto 平台 DTO
     * @return VO；入参为空返回 null
     */
    public static CascadePlatformVO convertVO(CascadePlatformDTO dto) {
        if (dto == null) {
            return null;
        }
        CascadePlatformVO vo = new CascadePlatformVO();
        vo.setId(dto.getId());
        vo.setCreateTime(dto.createTimeToEpochMilli());
        vo.setUpdateTime(dto.updateTimeToEpochMilli());
        vo.setPlatformId(dto.getPlatformId());
        vo.setPlatformIp(dto.getPlatformIp());
        vo.setPlatformPort(dto.getPlatformPort());
        vo.setPlatformDomain(dto.getPlatformDomain());
        vo.setUsername(dto.getUsername());
        vo.setPassword(dto.getPassword());
        vo.setLocalClientId(dto.getLocalClientId());
        vo.setLocalIp(dto.getLocalIp());
        vo.setLocalPort(dto.getLocalPort());
        vo.setEnabled(dto.getEnabled());
        vo.setRegisterStatus(dto.getRegisterStatus());
        vo.setRegisterStatusName(getRegisterStatusName(dto.getRegisterStatus()));
        vo.setKeepaliveInterval(dto.getKeepaliveInterval());
        vo.setRegisterExpires(dto.getRegisterExpires());
        vo.setCharset(dto.getCharset());
        vo.setTransport(dto.getTransport());
        vo.setExtend(dto.getExtend());
        return vo;
    }

    /**
     * 注册状态显示名称映射。
     *
     * @param registerStatus 注册状态码
     * @return 中文名称
     */
    private static String getRegisterStatusName(Integer registerStatus) {
        if (registerStatus == null) {
            return "未知";
        }
        switch (registerStatus) {
            case 0:
                return "离线";
            case 1:
                return "在线";
            case 2:
                return "注册中";
            case 3:
                return "注册失败";
            default:
                return "未知";
        }
    }
}
