package io.github.lunasaw.voglander.web.api.live.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.service.live.MediaPlayService;
import io.github.lunasaw.voglander.web.api.live.assembler.LiveWebAssembler;
import io.github.lunasaw.voglander.web.api.live.domain.LivePlayVO;
import io.github.lunasaw.voglander.web.api.live.domain.LiveStartReq;
import io.github.lunasaw.voglander.web.api.live.domain.LiveStopReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/live")
@Tag(name = "直播管理")
public class LivePlayController {

    @Autowired private MediaPlayService  mediaPlayService;
    @Autowired private LiveWebAssembler  liveWebAssembler;

    @PostMapping("/start")
    @Operation(summary = "开始直播（首播/复用）")
    public AjaxResult<LivePlayVO> start(@Valid @RequestBody LiveStartReq req) {
        return AjaxResult.success(liveWebAssembler.dtoToVo(
            mediaPlayService.startLive(liveWebAssembler.startReqToDto(req))));
    }

    @PostMapping("/stop")
    @Operation(summary = "停止直播（引用计数）")
    public AjaxResult<Boolean> stop(@Valid @RequestBody LiveStopReq req) {
        return AjaxResult.success(mediaPlayService.stopLive(req.getStreamId()));
    }

    @GetMapping("/{streamId}")
    @Operation(summary = "查询直播状态（轮询兜底）")
    public AjaxResult<LivePlayVO> get(@PathVariable String streamId) {
        return AjaxResult.success(liveWebAssembler.dtoToVo(mediaPlayService.getLive(streamId)));
    }

    @PostMapping("/keepalive")
    @Operation(summary = "心跳续约")
    public AjaxResult<Boolean> keepalive(@RequestBody Map<String, String> body) {
        mediaPlayService.keepAlive(body.get("streamId"));
        return AjaxResult.success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "活跃会话列表")
    public AjaxResult<List<LivePlayVO>> list(@RequestParam(required = false) String deviceId) {
        return AjaxResult.success(Collections.emptyList());
    }
}
