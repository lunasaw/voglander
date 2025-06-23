package io.github.lunasaw.voglander.web.api.export.vo;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.github.lunasaw.voglander.manager.domaon.dto.ExportTaskDTO;

/**
 * 导出任务视图对象
 *
 * @author chenzhangyue
 * @since 2024-01-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID自增
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 更新时间
     */
    private Date gmtUpdate;

    /**
     * 任务唯一Id
     */
    private Long bizId;

    /**
     * 导出的记录总数
     */
    private Long memberCnt;

    /**
     * 文件格式
     */
    private String format;

    /**
     * 申请时间
     */
    private Date applyTime;

    /**
     * 导出报表时间
     */
    private Date exportTime;

    /**
     * 文件下载地址, 多个url用、隔开
     */
    private String url;

    /**
     * 任务状态：0-处理中，1-已完成，-1-出错
     */
    private Integer status;

    /**
     * 是否过期：0-未过期，1-过期
     */
    private Integer expired;

    /**
     * 是否删除：0-未删除，1-删除
     */
    private Integer deleted;

    /**
     * 搜索条件序列化
     */
    private String param;

    /**
     * 导出名称
     */
    private String name;

    /**
     * 导出类型
     */
    private Integer type;

    /**
     * 申请用户
     */
    private String applyUser;

    /**
     * 扩展字段
     */
    private String extend;

    /**
     * 从 DTO 转换为 VO
     *
     * @param exportTaskDTO DTO对象
     * @return VO对象
     */
    public static ExportTaskVO convertVO(ExportTaskDTO exportTaskDTO) {
        if (exportTaskDTO == null) {
            return null;
        }

        ExportTaskVO exportTaskVO = new ExportTaskVO();
        exportTaskVO.setId(exportTaskDTO.getId());
        exportTaskVO.setGmtCreate(exportTaskDTO.getGmtCreate());
        exportTaskVO.setGmtUpdate(exportTaskDTO.getGmtUpdate());
        exportTaskVO.setBizId(exportTaskDTO.getBizId());
        exportTaskVO.setMemberCnt(exportTaskDTO.getMemberCnt());
        exportTaskVO.setFormat(exportTaskDTO.getFormat());
        exportTaskVO.setApplyTime(exportTaskDTO.getApplyTime());
        exportTaskVO.setExportTime(exportTaskDTO.getExportTime());
        exportTaskVO.setUrl(exportTaskDTO.getUrl());
        exportTaskVO.setStatus(exportTaskDTO.getStatus());
        exportTaskVO.setExpired(exportTaskDTO.getExpired());
        exportTaskVO.setDeleted(exportTaskDTO.getDeleted());
        exportTaskVO.setParam(exportTaskDTO.getParam());
        exportTaskVO.setName(exportTaskDTO.getName());
        exportTaskVO.setType(exportTaskDTO.getType());
        exportTaskVO.setApplyUser(exportTaskDTO.getApplyUser());
        exportTaskVO.setExtend(exportTaskDTO.getExtend());

        return exportTaskVO;
    }
}