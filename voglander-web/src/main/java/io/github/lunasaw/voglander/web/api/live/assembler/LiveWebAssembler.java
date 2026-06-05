package io.github.lunasaw.voglander.web.api.live.assembler;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.service.live.dto.LivePlayDTO;
import io.github.lunasaw.voglander.service.live.dto.LiveStartDTO;
import io.github.lunasaw.voglander.web.api.live.domain.LivePlayVO;
import io.github.lunasaw.voglander.web.api.live.domain.LiveStartReq;

@Component
public class LiveWebAssembler {

    public LiveStartDTO startReqToDto(LiveStartReq req) {
        LiveStartDTO dto = new LiveStartDTO();
        dto.setDeviceId(req.getDeviceId());
        dto.setChannelId(req.getChannelId());
        dto.setProtocol(req.getProtocol());
        dto.setStreamMode(req.getStreamMode());
        return dto;
    }

    public LivePlayVO dtoToVo(LivePlayDTO dto) {
        if (dto == null) return null;
        LivePlayVO vo = new LivePlayVO();
        vo.setStreamId(dto.getStreamId());
        vo.setCallId(dto.getCallId());
        vo.setStatus(dto.getStatus());
        vo.setPlayUrls(dto.getPlayUrls());
        vo.setRefCount(dto.getRefCount());
        return vo;
    }
}
