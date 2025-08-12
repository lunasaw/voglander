package io.github.lunasaw.voglander.manager.assembler;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 拉流代理装配器
 * 负责 DTO 和 DO 之间的转换
 *
 * @author luna
 * @since 2025-01-23
 */
@Component
public class StreamProxyAssembler {

    /**
     * DTO 转 DO
     *
     * @param dto 数据传输对象
     * @return 数据库实体对象
     */
    public StreamProxyDO dtoToDo(StreamProxyDTO dto) {
        if (dto == null) {
            return null;
        }

        StreamProxyDO streamProxyDO = new StreamProxyDO();
        streamProxyDO.setId(dto.getId());
        streamProxyDO.setCreateTime(dto.getCreateTime());
        streamProxyDO.setUpdateTime(dto.getUpdateTime());
        streamProxyDO.setApp(dto.getApp());
        streamProxyDO.setStream(dto.getStream());
        streamProxyDO.setUrl(dto.getUrl());
        streamProxyDO.setStatus(dto.getStatus());
        streamProxyDO.setOnlineStatus(dto.getOnlineStatus());
        streamProxyDO.setProxyKey(dto.getProxyKey());
        streamProxyDO.setDescription(dto.getDescription());

        // Handle extend field conversion - prioritize extend string over extendObj
        if (dto.getExtend() != null) {
            streamProxyDO.setExtend(dto.getExtend());
        } else if (dto.getExtendObj() != null) {
            streamProxyDO.setExtend(JSON.toJSONString(dto.getExtendObj()));
        } else {
            streamProxyDO.setExtend(null);
        }

        return streamProxyDO;
    }

    /**
     * DO 转 DTO
     *
     * @param streamProxyDO 数据库实体对象
     * @return 数据传输对象
     */
    public StreamProxyDTO doToDto(StreamProxyDO streamProxyDO) {
        if (streamProxyDO == null) {
            return null;
        }

        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setId(streamProxyDO.getId());
        dto.setCreateTime(streamProxyDO.getCreateTime());
        dto.setUpdateTime(streamProxyDO.getUpdateTime());
        dto.setApp(streamProxyDO.getApp());
        dto.setStream(streamProxyDO.getStream());
        dto.setUrl(streamProxyDO.getUrl());
        dto.setStatus(streamProxyDO.getStatus());
        dto.setOnlineStatus(streamProxyDO.getOnlineStatus());
        dto.setProxyKey(streamProxyDO.getProxyKey());
        dto.setDescription(streamProxyDO.getDescription());
        dto.setExtend(streamProxyDO.getExtend());

        // Parse extend string to ExtendObj if extend is not null and not empty
        if (streamProxyDO.getExtend() != null && !streamProxyDO.getExtend().trim().isEmpty()) {
            try {
                dto.setExtendObj(JSON.parseObject(streamProxyDO.getExtend(), StreamProxyDTO.ExtendObj.class));
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
    public List<StreamProxyDTO> doListToDtoList(List<StreamProxyDO> doList) {
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
    public List<StreamProxyDO> dtoListToDoList(List<StreamProxyDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        return dtoList.stream()
            .map(this::dtoToDo)
            .collect(Collectors.toList());
    }
}