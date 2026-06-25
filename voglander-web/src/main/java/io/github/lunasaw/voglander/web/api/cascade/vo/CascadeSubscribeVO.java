package io.github.lunasaw.voglander.web.api.cascade.vo;

import java.io.Serializable;

import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeSubscribeDTO;
import lombok.Data;

/**
 * 级联上级订阅 VO（Web 出参，时间字段统一 Unix 毫秒）。
 *
 * <p>展示「哪个上级平台订阅了本平台哪类信息」，用于前端订阅状态可视化。</p>
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadeSubscribeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long    id;

    /**
     * 创建时间（unix 毫秒）
     */
    private Long    createTime;

    /**
     * 更新时间（unix 毫秒）
     */
    private Long    updateTime;

    /**
     * 发起订阅的上级平台国标 ID
     */
    private String  platformId;

    /**
     * 订阅类型 CATALOG / ALARM / MOBILE_POSITION
     */
    private String  subType;

    /**
     * 订阅类型显示名称
     */
    private String  subTypeName;

    /**
     * 订阅会话 callId
     */
    private String  callId;

    /**
     * 订阅请求 SN
     */
    private String  sn;

    /**
     * 订阅有效期（秒）
     */
    private Integer expires;

    /**
     * 移动位置上报间隔（秒，仅 MOBILE_POSITION）
     */
    private Integer intervalSec;

    /**
     * 过期时间（unix 毫秒）
     */
    private Long    expireTime;

    /**
     * 订阅状态 0已过期 1活跃
     */
    private Integer status;

    /**
     * 订阅状态显示名称
     */
    private String  statusName;

    /**
     * DTO → VO 转换（时间转 Unix 毫秒，subType/status 派生展示名）。
     *
     * @param dto 订阅 DTO
     * @return VO；入参为空返回 null
     */
    public static CascadeSubscribeVO convertVO(CascadeSubscribeDTO dto) {
        if (dto == null) {
            return null;
        }
        CascadeSubscribeVO vo = new CascadeSubscribeVO();
        vo.setId(dto.getId());
        vo.setCreateTime(toEpochMilli(dto.getCreateTime()));
        vo.setUpdateTime(toEpochMilli(dto.getUpdateTime()));
        vo.setPlatformId(dto.getPlatformId());
        vo.setSubType(dto.getSubType());
        vo.setSubTypeName(getSubTypeName(dto.getSubType()));
        vo.setCallId(dto.getCallId());
        vo.setSn(dto.getSn());
        vo.setExpires(dto.getExpires());
        vo.setIntervalSec(dto.getIntervalSec());
        vo.setExpireTime(toEpochMilli(dto.getExpireTime()));
        vo.setStatus(dto.getStatus());
        vo.setStatusName(getStatusName(dto.getStatus()));
        return vo;
    }

    private static Long toEpochMilli(java.time.LocalDateTime time) {
        return time != null ? time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    private static String getSubTypeName(String subType) {
        if (subType == null) {
            return "未知";
        }
        switch (subType) {
            case "CATALOG":
                return "目录订阅";
            case "ALARM":
                return "告警订阅";
            case "MOBILE_POSITION":
                return "位置订阅";
            default:
                return subType;
        }
    }

    private static String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "活跃" : "已过期";
    }
}
