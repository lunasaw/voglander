package io.github.lunasaw.voglander.web.api.export.assembler;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.domaon.dto.ExportTaskDTO;
import io.github.lunasaw.voglander.web.api.export.req.ExportTaskCreateReq;
import io.github.lunasaw.voglander.web.api.export.req.ExportTaskUpdateReq;

/**
 * 导出任务Web层转换器
 * 负责 Req -> DTO 的转换
 *
 * @author chenzhangyue
 * @since 2024-01-30
 */
@Component
public class ExportTaskWebAssembler {

    /**
     * 创建请求转换为DTO
     *
     * @param createReq 创建请求
     * @return DTO对象
     */
    public ExportTaskDTO toExportTaskDTO(ExportTaskCreateReq createReq) {
        if (createReq == null) {
            return null;
        }

        ExportTaskDTO exportTaskDTO = new ExportTaskDTO();
        exportTaskDTO.setBizId(createReq.getBizId());
        exportTaskDTO.setMemberCnt(createReq.getMemberCnt());
        exportTaskDTO.setFormat(createReq.getFormat());
        exportTaskDTO.setApplyTime(createReq.getApplyTime());
        exportTaskDTO.setParam(createReq.getParam());
        exportTaskDTO.setName(createReq.getName());
        exportTaskDTO.setType(createReq.getType());
        exportTaskDTO.setApplyUser(createReq.getApplyUser());
        exportTaskDTO.setExtend(createReq.getExtend());

        return exportTaskDTO;
    }

    /**
     * 更新请求转换为DTO
     *
     * @param updateReq 更新请求
     * @return DTO对象
     */
    public ExportTaskDTO toExportTaskDTO(ExportTaskUpdateReq updateReq) {
        if (updateReq == null) {
            return null;
        }

        ExportTaskDTO exportTaskDTO = new ExportTaskDTO();
        exportTaskDTO.setId(updateReq.getId());
        exportTaskDTO.setBizId(updateReq.getBizId());
        exportTaskDTO.setMemberCnt(updateReq.getMemberCnt());
        exportTaskDTO.setFormat(updateReq.getFormat());
        exportTaskDTO.setApplyTime(updateReq.getApplyTime());
        exportTaskDTO.setExportTime(updateReq.getExportTime());
        exportTaskDTO.setUrl(updateReq.getUrl());
        exportTaskDTO.setStatus(updateReq.getStatus());
        exportTaskDTO.setExpired(updateReq.getExpired());
        exportTaskDTO.setParam(updateReq.getParam());
        exportTaskDTO.setName(updateReq.getName());
        exportTaskDTO.setType(updateReq.getType());
        exportTaskDTO.setApplyUser(updateReq.getApplyUser());
        exportTaskDTO.setExtend(updateReq.getExtend());

        return exportTaskDTO;
    }

    /**
     * 批量创建请求转换为DTO列表
     *
     * @param createReqList 创建请求列表
     * @return DTO对象列表
     */
    public List<ExportTaskDTO> toExportTaskDTOList(List<ExportTaskCreateReq> createReqList) {
        if (createReqList == null || createReqList.isEmpty()) {
            return Collections.emptyList();
        }

        return createReqList.stream()
            .map(this::toExportTaskDTO)
            .collect(Collectors.toList());
    }

    /**
     * 批量更新请求转换为DTO列表
     *
     * @param updateReqList 更新请求列表
     * @return DTO对象列表
     */
    public List<ExportTaskDTO> toUpdateExportTaskDTOList(List<ExportTaskUpdateReq> updateReqList) {
        if (updateReqList == null || updateReqList.isEmpty()) {
            return Collections.emptyList();
        }

        return updateReqList.stream()
            .map(this::toExportTaskDTO)
            .collect(Collectors.toList());
    }
}