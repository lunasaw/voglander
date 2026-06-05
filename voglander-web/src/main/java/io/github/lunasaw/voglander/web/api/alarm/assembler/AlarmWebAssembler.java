package io.github.lunasaw.voglander.web.api.alarm.assembler;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lunasaw.voglander.manager.domaon.dto.AlarmDTO;
import io.github.lunasaw.voglander.web.api.alarm.domain.AlarmListResp;
import io.github.lunasaw.voglander.web.api.alarm.domain.AlarmQueryReq;
import io.github.lunasaw.voglander.web.api.alarm.domain.AlarmVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class AlarmWebAssembler {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AlarmDTO queryReqToDto(AlarmQueryReq req) {
        AlarmDTO dto = new AlarmDTO();
        dto.setDeviceId(req.getDeviceId());
        dto.setAlarmLevel(req.getAlarmLevel());
        dto.setAlarmType(req.getAlarmType());
        return dto;
    }

    public LocalDateTime parseTime(String t) {
        if (t == null || t.isBlank()) return null;
        return LocalDateTime.parse(t, FMT);
    }

    public AlarmVO dtoToVo(AlarmDTO dto) {
        if (dto == null) return null;
        AlarmVO vo = new AlarmVO();
        vo.setId(dto.getId());
        vo.setDeviceId(dto.getDeviceId());
        vo.setChannelId(dto.getChannelId());
        vo.setAlarmType(dto.getAlarmType());
        vo.setAlarmLevel(dto.getAlarmLevel());
        vo.setDescription(dto.getDescription());
        vo.setAckStatus(dto.getAckStatus());
        vo.setAlarmTime(dto.alarmTimeToEpochMilli());
        return vo;
    }

    public AlarmListResp toListResp(Page<AlarmDTO> page) {
        AlarmListResp resp = new AlarmListResp();
        resp.setTotal(page.getTotal());
        resp.setItems(page.getRecords().stream().map(this::dtoToVo).collect(Collectors.toList()));
        return resp;
    }
}
