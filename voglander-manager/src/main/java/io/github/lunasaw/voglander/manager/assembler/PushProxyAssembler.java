package io.github.lunasaw.voglander.manager.assembler;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.repository.entity.PushProxyDO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 推流代理装配器
 * 负责 DTO 和 DO 之间的转换
 *
 * @author luna
 * @since 2025-01-23
 */
@Component
public class PushProxyAssembler {

    /**
     * DTO 转 DO
     *
     * @param dto 数据传输对象
     * @return 数据库实体对象
     */
    public PushProxyDO dtoToDo(PushProxyDTO dto) {
        if (dto == null) {
            return null;
        }

        PushProxyDO pushProxyDO = new PushProxyDO();
        pushProxyDO.setId(dto.getId());
        pushProxyDO.setCreateTime(dto.getCreateTime());
        pushProxyDO.setUpdateTime(dto.getUpdateTime());
        pushProxyDO.setApp(dto.getApp());
        pushProxyDO.setStream(dto.getStream());
        pushProxyDO.setDstUrl(dto.getDstUrl());
        pushProxyDO.setSchema(dto.getSchema());
        pushProxyDO.setStatus(dto.getStatus());
        pushProxyDO.setOnlineStatus(dto.getOnlineStatus());
        pushProxyDO.setProxyKey(dto.getProxyKey());
        pushProxyDO.setServerId(dto.getServerId());
        pushProxyDO.setEnabled(dto.getEnabled());
        pushProxyDO.setDescription(dto.getDescription());

        // Handle extend field conversion - prioritize extend string over extendObj
        if (dto.getExtend() != null) {
            pushProxyDO.setExtend(dto.getExtend());
        } else if (dto.getExtendObj() != null) {
            pushProxyDO.setExtend(JSON.toJSONString(dto.getExtendObj()));
        } else {
            pushProxyDO.setExtend(null);
        }

        return pushProxyDO;
    }

    /**
     * DO 转 DTO
     *
     * @param pushProxyDO 数据库实体对象
     * @return 数据传输对象
     */
    public PushProxyDTO doToDto(PushProxyDO pushProxyDO) {
        if (pushProxyDO == null) {
            return null;
        }

        PushProxyDTO dto = new PushProxyDTO();
        dto.setId(pushProxyDO.getId());
        dto.setCreateTime(pushProxyDO.getCreateTime());
        dto.setUpdateTime(pushProxyDO.getUpdateTime());
        dto.setApp(pushProxyDO.getApp());
        dto.setStream(pushProxyDO.getStream());
        dto.setDstUrl(pushProxyDO.getDstUrl());
        dto.setSchema(pushProxyDO.getSchema());
        dto.setStatus(pushProxyDO.getStatus());
        dto.setOnlineStatus(pushProxyDO.getOnlineStatus());
        dto.setProxyKey(pushProxyDO.getProxyKey());
        dto.setServerId(pushProxyDO.getServerId());
        dto.setEnabled(pushProxyDO.getEnabled());
        dto.setDescription(pushProxyDO.getDescription());
        dto.setExtend(pushProxyDO.getExtend());

        // Parse extend string to ExtendObj if extend is not null and not empty
        if (pushProxyDO.getExtend() != null && !pushProxyDO.getExtend().trim().isEmpty()) {
            try {
                dto.setExtendObj(JSON.parseObject(pushProxyDO.getExtend(), PushProxyDTO.ExtendObj.class));
            } catch (Exception e) {
                // If parsing fails, set extendObj to null but keep the original extend string
                dto.setExtendObj(null);
            }
        } else {
            dto.setExtendObj(null);
        }

        return dto;
    }

    /**
     * DO列表 转 DTO列表
     *
     * @param doList 数据库实体对象列表
     * @return 数据传输对象列表
     */
    public List<PushProxyDTO> doListToDtoList(List<PushProxyDO> doList) {
        if (doList == null) {
            return null;
        }

        return doList.stream()
            .map(this::doToDto)
            .collect(Collectors.toList());
    }

    /**
     * DTO列表 转 DO列表
     *
     * @param dtoList 数据传输对象列表
     * @return 数据库实体对象列表
     */
    public List<PushProxyDO> dtoListToDoList(List<PushProxyDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        return dtoList.stream()
            .map(this::dtoToDo)
            .collect(Collectors.toList());
    }
}