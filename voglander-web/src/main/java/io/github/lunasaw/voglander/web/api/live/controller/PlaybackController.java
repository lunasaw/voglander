package io.github.lunasaw.voglander.web.api.live.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.client.domain.device.qo.DevicePlaybackReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.web.api.live.domain.PlaybackControlReq;
import io.github.lunasaw.voglander.web.api.live.domain.PlaybackStartReq;
import io.github.lunasaw.voglander.web.api.live.domain.RecordQueryReq;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/playback")
@Tag(name = "录像回放")
public class PlaybackController {

    @Autowired private DeviceCommandService deviceCommandService;

    @PostMapping("/start")
    public AjaxResult<?> start(@Valid @RequestBody PlaybackStartReq req) {
        DevicePlaybackReq pbReq = new DevicePlaybackReq();
        pbReq.setDeviceId(req.getDeviceId());
        pbReq.setStartTime(req.getStartTime());
        pbReq.setEndTime(req.getEndTime());
        pbReq.setStreamMode(req.getStreamMode());
        ResultDTO<String> r = deviceCommandService.startPlayback(pbReq);
        if (r.isSuccess()) return AjaxResult.success(r.getData());
        return AjaxResult.error(r.getMessage());
    }

    @PostMapping("/stop")
    public AjaxResult<?> stop(@RequestBody Map<String, String> body) {
        ResultDTO<Void> r = deviceCommandService.stopPlay(body.get("streamId"));
        if (r.isSuccess()) return AjaxResult.success(true);
        return AjaxResult.error(r.getMessage());
    }

    @PostMapping("/control")
    public AjaxResult<Boolean> control(@Valid @RequestBody PlaybackControlReq req) {
        // D4 L1（2026-06-05）：接通暂停/继续（PLAY_RESUME/PLAY_NOW），经 streamId 反查 deviceId 真实下发；
        // PLAY_RANGE(seek)/PLAY_SPEED(倍速) 由服务层显式返回不支持。不再占位 return true。
        ResultDTO<Void> r = deviceCommandService.controlPlayback(req.getStreamId(), req.getAction(), req.getParam());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/records")
    public AjaxResult<Void> queryRecords(@Valid @RequestBody RecordQueryReq req) {
        DeviceQueryReq qReq = new DeviceQueryReq();
        qReq.setDeviceId(req.getDeviceId());
        deviceCommandService.queryDevice(qReq);
        return AjaxResult.success(null);
    }
}
