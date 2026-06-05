package io.github.lunasaw.voglander.manager.assembler;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;

/**
 * 媒体会话装配器
 * 负责 DTO 和 DO 之间的转换
 *
 * @author luna
 * @since 2025-05-29
 */
@Component
public class MediaSessionAssembler {

    /**
     * DTO 转 DO
     *
     * @param dto 数据传输对象
     * @return 数据库实体对象
     */
    public MediaSessionDO dtoToDo(MediaSessionDTO dto) {
        if (dto == null) {
            return null;
        }

        MediaSessionDO mediaSessionDO = new MediaSessionDO();
        mediaSessionDO.setId(dto.getId());
        mediaSessionDO.setCreateTime(dto.getCreateTime());
        mediaSessionDO.setUpdateTime(dto.getUpdateTime());
        mediaSessionDO.setCallId(dto.getCallId());
        mediaSessionDO.setDeviceId(dto.getDeviceId());
        mediaSessionDO.setChannelId(dto.getChannelId());
        mediaSessionDO.setSsrc(dto.getSsrc());
        mediaSessionDO.setStream(dto.getStream());
        mediaSessionDO.setStatus(dto.getStatus());
        mediaSessionDO.setSessionType(dto.getSessionType());
        mediaSessionDO.setExtend(dto.getExtend());
        mediaSessionDO.setStreamId(dto.getStreamId());
        mediaSessionDO.setNodeServerId(dto.getNodeServerId());
        mediaSessionDO.setRefCount(dto.getRefCount());
        return mediaSessionDO;
    }

    /**
     * DO 转 DTO
     *
     * @param mediaSessionDO 数据库实体对象
     * @return 数据传输对象
     */
    public MediaSessionDTO doToDto(MediaSessionDO mediaSessionDO) {
        if (mediaSessionDO == null) {
            return null;
        }

        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setId(mediaSessionDO.getId());
        dto.setCreateTime(mediaSessionDO.getCreateTime());
        dto.setUpdateTime(mediaSessionDO.getUpdateTime());
        dto.setCallId(mediaSessionDO.getCallId());
        dto.setDeviceId(mediaSessionDO.getDeviceId());
        dto.setChannelId(mediaSessionDO.getChannelId());
        dto.setSsrc(mediaSessionDO.getSsrc());
        dto.setStream(mediaSessionDO.getStream());
        dto.setStatus(mediaSessionDO.getStatus());
        dto.setSessionType(mediaSessionDO.getSessionType());
        dto.setExtend(mediaSessionDO.getExtend());
        dto.setStreamId(mediaSessionDO.getStreamId());
        dto.setNodeServerId(mediaSessionDO.getNodeServerId());
        dto.setRefCount(mediaSessionDO.getRefCount());
        return dto;
    }

    /**
     * DO列表 转 DTO列表
     *
     * @param doList 数据库实体对象列表
     * @return 数据传输对象列表
     */
    public List<MediaSessionDTO> doListToDtoList(List<MediaSessionDO> doList) {
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
    public List<MediaSessionDO> dtoListToDoList(List<MediaSessionDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        return dtoList.stream()
            .map(this::dtoToDo)
            .collect(Collectors.toList());
    }
}
