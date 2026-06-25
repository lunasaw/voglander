package io.github.lunasaw.voglander.web.api.cascade.vo;

import java.io.Serializable;

import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import lombok.Data;

/**
 * 级联上报通道 VO（Web 出参，时间字段统一 Unix 毫秒）。
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadeChannelVO implements Serializable {

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
     * 所属上级平台国标 ID
     */
    private String  platformId;

    /**
     * 本地设备国标 ID
     */
    private String  localDeviceId;

    /**
     * 本地通道国标 ID
     */
    private String  localChannelId;

    /**
     * 对上级暴露的级联通道编码（国标 20 位）
     */
    private String  cascadeChannelId;

    /**
     * 对上级暴露的通道名称
     */
    private String  cascadeName;

    /**
     * 启用状态 1启用 / 0停用
     */
    private Integer enabled;

    /**
     * DTO → VO 转换（时间转 Unix 毫秒）。
     *
     * @param dto 通道 DTO
     * @return VO；入参为空返回 null
     */
    public static CascadeChannelVO convertVO(CascadeChannelDTO dto) {
        if (dto == null) {
            return null;
        }
        CascadeChannelVO vo = new CascadeChannelVO();
        vo.setId(dto.getId());
        vo.setCreateTime(dto.createTimeToEpochMilli());
        vo.setUpdateTime(dto.updateTimeToEpochMilli());
        vo.setPlatformId(dto.getPlatformId());
        vo.setLocalDeviceId(dto.getLocalDeviceId());
        vo.setLocalChannelId(dto.getLocalChannelId());
        vo.setCascadeChannelId(dto.getCascadeChannelId());
        vo.setCascadeName(dto.getCascadeName());
        vo.setEnabled(dto.getEnabled());
        return vo;
    }
}
