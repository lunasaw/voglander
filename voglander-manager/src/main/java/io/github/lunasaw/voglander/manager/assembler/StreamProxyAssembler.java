package io.github.lunasaw.voglander.manager.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import org.springframework.stereotype.Component;

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
        streamProxyDO.setEnabled(dto.getEnabled());
        streamProxyDO.setExtend(dto.getExtend());

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
        dto.setEnabled(streamProxyDO.getEnabled());
        dto.setExtend(streamProxyDO.getExtend());

        return dto;
    }
}