package io.github.lunasaw.voglander.manager.assembler;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.domaon.dto.AlarmDTO;
import io.github.lunasaw.voglander.repository.entity.AlarmDO;

/**
 * 告警装配器：负责 DTO 与 DO 之间的转换。
 *
 * @author luna
 */
@Component
public class AlarmAssembler {

    /**
     * DTO 转 DO。
     *
     * @param dto 数据传输对象
     * @return 数据库实体对象
     */
    public AlarmDO dtoToDo(AlarmDTO dto) {
        if (dto == null) {
            return null;
        }
        AlarmDO alarmDO = new AlarmDO();
        alarmDO.setId(dto.getId());
        alarmDO.setCreateTime(dto.getCreateTime());
        alarmDO.setUpdateTime(dto.getUpdateTime());
        alarmDO.setDeviceId(dto.getDeviceId());
        alarmDO.setChannelId(dto.getChannelId());
        alarmDO.setAlarmType(dto.getAlarmType());
        alarmDO.setAlarmLevel(dto.getAlarmLevel());
        alarmDO.setAlarmTime(dto.getAlarmTime());
        alarmDO.setDescription(dto.getDescription());
        alarmDO.setAckStatus(dto.getAckStatus());
        alarmDO.setExtend(dto.getExtend());
        return alarmDO;
    }

    /**
     * DO 转 DTO。
     *
     * @param alarmDO 数据库实体对象
     * @return 数据传输对象
     */
    public AlarmDTO doToDto(AlarmDO alarmDO) {
        if (alarmDO == null) {
            return null;
        }
        AlarmDTO dto = new AlarmDTO();
        dto.setId(alarmDO.getId());
        dto.setCreateTime(alarmDO.getCreateTime());
        dto.setUpdateTime(alarmDO.getUpdateTime());
        dto.setDeviceId(alarmDO.getDeviceId());
        dto.setChannelId(alarmDO.getChannelId());
        dto.setAlarmType(alarmDO.getAlarmType());
        dto.setAlarmLevel(alarmDO.getAlarmLevel());
        dto.setAlarmTime(alarmDO.getAlarmTime());
        dto.setDescription(alarmDO.getDescription());
        dto.setAckStatus(alarmDO.getAckStatus());
        dto.setExtend(alarmDO.getExtend());
        return dto;
    }

    /**
     * DO 列表转 DTO 列表。
     *
     * @param doList 数据库实体对象列表
     * @return 数据传输对象列表
     */
    public List<AlarmDTO> doListToDtoList(List<AlarmDO> doList) {
        if (doList == null) {
            return null;
        }
        return doList.stream().map(this::doToDto).collect(Collectors.toList());
    }
}
