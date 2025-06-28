package io.github.lunasaw.voglander.manager.assembler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.domaon.dto.ExportTaskDTO;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;

/**
 * 导出任务Manager层转换器
 * 负责 DTO <-> DO 的转换，处理基础数据字段
 *
 * @author chenzhangyue
 * @since 2024-01-30
 */
@Component
public class ExportTaskAssembler {

    /**
     * DTO转换为DO
     *
     * @param exportTaskDTO DTO对象
     * @return DO对象
     */
    public ExportTaskDO toExportTaskDO(ExportTaskDTO exportTaskDTO) {
        if (exportTaskDTO == null) {
            return null;
        }

        ExportTaskDO exportTaskDO = new ExportTaskDO();
        exportTaskDO.setId(exportTaskDTO.getId());
        exportTaskDO.setGmtCreate(exportTaskDTO.getGmtCreate());
        exportTaskDO.setGmtUpdate(exportTaskDTO.getGmtUpdate());
        exportTaskDO.setBizId(exportTaskDTO.getBizId());
        exportTaskDO.setMemberCnt(exportTaskDTO.getMemberCnt());
        exportTaskDO.setFormat(exportTaskDTO.getFormat());
        exportTaskDO.setApplyTime(exportTaskDTO.getApplyTime());
        exportTaskDO.setExportTime(exportTaskDTO.getExportTime());
        exportTaskDO.setUrl(exportTaskDTO.getUrl());
        exportTaskDO.setStatus(exportTaskDTO.getStatus());
        exportTaskDO.setExpired(exportTaskDTO.getExpired());
        exportTaskDO.setDeleted(exportTaskDTO.getDeleted());
        exportTaskDO.setParam(exportTaskDTO.getParam());
        exportTaskDO.setName(exportTaskDTO.getName());
        exportTaskDO.setType(exportTaskDTO.getType());
        exportTaskDO.setApplyUser(exportTaskDTO.getApplyUser());
        exportTaskDO.setExtend(exportTaskDTO.getExtend());

        return exportTaskDO;
    }

    /**
     * DO转换为DTO
     *
     * @param exportTaskDO DO对象
     * @return DTO对象
     */
    public ExportTaskDTO toExportTaskDTO(ExportTaskDO exportTaskDO) {
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

    /**
     * 创建DTO转换为DO，设置基础数据字段
     *
     * @param exportTaskDTO DTO对象
     * @return DO对象
     */
    public ExportTaskDO toCreateExportTaskDO(ExportTaskDTO exportTaskDTO) {
        ExportTaskDO exportTaskDO = toExportTaskDO(exportTaskDTO);
        if (exportTaskDO != null) {
            Date now = new Date();
            exportTaskDO.setGmtCreate(now);
            exportTaskDO.setGmtUpdate(now);

            // 设置默认值
            if (exportTaskDO.getStatus() == null) {
                exportTaskDO.setStatus(0); // 默认处理中
            }
            if (exportTaskDO.getExpired() == null) {
                exportTaskDO.setExpired(0); // 默认未过期
            }
            if (exportTaskDO.getDeleted() == null) {
                exportTaskDO.setDeleted(0); // 默认未删除
            }
            if (exportTaskDO.getApplyTime() == null) {
                exportTaskDO.setApplyTime(now); // 默认申请时间为当前时间
            }
        }

        return exportTaskDO;
    }

    /**
     * 更新DTO转换为DO，设置基础数据字段
     *
     * @param exportTaskDTO DTO对象
     * @return DO对象
     */
    public ExportTaskDO toUpdateExportTaskDO(ExportTaskDTO exportTaskDTO) {
        ExportTaskDO exportTaskDO = toExportTaskDO(exportTaskDTO);
        if (exportTaskDO != null) {
            exportTaskDO.setGmtUpdate(new Date());
        }

        return exportTaskDO;
    }

    /**
     * DO列表转换为DTO列表
     *
     * @param exportTaskDOList DO对象列表
     * @return DTO对象列表
     */
    public List<ExportTaskDTO> toExportTaskDTOList(List<ExportTaskDO> exportTaskDOList) {
        if (exportTaskDOList == null || exportTaskDOList.isEmpty()) {
            return Collections.emptyList();
        }

        return exportTaskDOList.stream()
            .map(this::toExportTaskDTO)
            .collect(Collectors.toList());
    }

    /**
     * DTO列表转换为DO列表（用于批量创建）
     *
     * @param exportTaskDTOList DTO对象列表
     * @return DO对象列表
     */
    public List<ExportTaskDO> toCreateExportTaskDOList(List<ExportTaskDTO> exportTaskDTOList) {
        if (exportTaskDTOList == null || exportTaskDTOList.isEmpty()) {
            return Collections.emptyList();
        }

        return exportTaskDTOList.stream()
            .map(this::toCreateExportTaskDO)
            .collect(Collectors.toList());
    }

    /**
     * DTO列表转换为DO列表（用于批量更新）
     *
     * @param exportTaskDTOList DTO对象列表
     * @return DO对象列表
     */
    public List<ExportTaskDO> toUpdateExportTaskDOList(List<ExportTaskDTO> exportTaskDTOList) {
        if (exportTaskDTOList == null || exportTaskDTOList.isEmpty()) {
            return Collections.emptyList();
        }

        return exportTaskDTOList.stream()
            .map(this::toUpdateExportTaskDO)
            .collect(Collectors.toList());
    }
}