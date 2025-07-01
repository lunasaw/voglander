package io.github.lunasaw.voglander.manager.domaon.dto;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;

import io.github.lunasaw.voglander.common.enums.export.ExportTaskStatusEnum;
import io.github.lunasaw.voglander.common.enums.export.ExportTaskTypeEnums;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;
import lombok.Data;

/**
 * (ExportTask)实体类
 *
 * @author chenzhangyue
 * @since 2024-01-26 15:21:45
 */
@Data
public class ExportTaskDTO implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * ID自增
     */
    private Long              id;
    /**
     * 创建时间
     */
    private Date              gmtCreate;
    /**
     * 更新时间
     */
    private Date              gmtUpdate;
    /**
     * 任务唯一Id
     */
    private Long              bizId;
    /**
     * 导出的客户总数
     */
    private Long              memberCnt;
    /**
     * 文件格式
     */
    private String            format;
    /**
     * 申请时间
     */
    private Date              applyTime;
    /**
     * 导出报表时间
     */
    private Date              exportTime;
    /**
     * 文件下载地址, 多个url用、隔开
     */
    private String            url;
    /**
     * {@link ExportTaskStatusEnum}
     * 是否完成，1 -> 完成, 0->处理中, -1 -> 出错
     */
    private Integer           status;
    /**
     * 是否过期，1 -> 过期，0 -> 未过期
     */
    private Integer           expired;
    /**
     * 是否删除，1 -> 删除, 0 -> 未删除
     */
    private Integer           deleted;
    /**
     * 搜索条件序列化
     */
    private String            param;

    /**
     * 导出名称
     */
    private String            name;

    /**
     * {@link ExportTaskTypeEnums}
     * 导出类型
     */
    private Integer           type;

    private String            applyUser;

    private String            extend;

    public static ExportTaskDTO do2Dto(ExportTaskDO exportTaskDO) {
        if (exportTaskDO == null) {
            return null;
        }
        ExportTaskDTO exportTaskDTO = new ExportTaskDTO();
        exportTaskDTO.setId(exportTaskDO.getId());
        exportTaskDTO.setGmtCreate(exportTaskDO.getGmtCreate());
        exportTaskDTO.setGmtUpdate(exportTaskDO.getGmtUpdate());
        exportTaskDTO.setBizId(exportTaskDO.getBizId());
        exportTaskDTO.setMemberCnt(exportTaskDO.getMemberCnt());
        exportTaskDTO.setFormat(exportTaskDO.getFormat());
        exportTaskDTO.setApplyTime(exportTaskDO.getApplyTime());
        exportTaskDTO.setExportTime(exportTaskDO.getExportTime());
        exportTaskDTO.setUrl(exportTaskDO.getUrl());
        exportTaskDTO.setStatus(exportTaskDO.getStatus());
        exportTaskDTO.setExpired(exportTaskDO.getExpired());
        exportTaskDTO.setDeleted(exportTaskDO.getDeleted());
        exportTaskDTO.setParam(exportTaskDO.getParam());
        exportTaskDTO.setName(exportTaskDO.getName());
        exportTaskDTO.setType(exportTaskDO.getType());
        exportTaskDTO.setApplyUser(exportTaskDO.getApplyUser());
        exportTaskDTO.setExtend(exportTaskDO.getExtend());
        return exportTaskDTO;
    }

    // ================ 时间转换领域方法 ================
    // TODO: 根据新规范，后续需要将Date类型改为LocalDateTime

    /**
     * 获取创建时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long gmtCreateToEpochMilli() {
        return gmtCreate != null ? gmtCreate.getTime() : null;
    }

    /**
     * 获取更新时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long gmtUpdateToEpochMilli() {
        return gmtUpdate != null ? gmtUpdate.getTime() : null;
    }

    /**
     * 获取申请时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long applyTimeToEpochMilli() {
        return applyTime != null ? applyTime.getTime() : null;
    }

    /**
     * 获取导出时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long exportTimeToEpochMilli() {
        return exportTime != null ? exportTime.getTime() : null;
    }
}
