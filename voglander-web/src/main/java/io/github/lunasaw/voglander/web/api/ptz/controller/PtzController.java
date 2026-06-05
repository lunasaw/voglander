package io.github.lunasaw.voglander.web.api.ptz.controller;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.web.api.ptz.assembler.PtzWebAssembler;
import io.github.lunasaw.voglander.web.api.ptz.domain.PresetReq;
import io.github.lunasaw.voglander.web.api.ptz.domain.PtzControlReq;
import io.github.lunasaw.voglander.web.api.ptz.domain.PtzStopReq;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/ptz")
@Tag(name = "PTZ 控制")
public class PtzController {

    @Autowired private DeviceCommandService deviceCommandService;
    @Autowired private PtzWebAssembler      ptzWebAssembler;

    @PostMapping("/control")
    public AjaxResult<Boolean> control(@Valid @RequestBody PtzControlReq req) {
        ResultDTO<Void> r = deviceCommandService.ptzControl(ptzWebAssembler.toReq(req));
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/stop")
    public AjaxResult<Boolean> stop(@Valid @RequestBody PtzStopReq req) {
        ResultDTO<Void> r = deviceCommandService.ptzControl(ptzWebAssembler.toStopReq(req));
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/preset")
    public AjaxResult<Boolean> preset(@Valid @RequestBody PresetReq req) {
        // D6 决策（2026-06-05，档 B 保底）：PTZControlEnum 无 PRESET 常量、wrapper 亦无 SET/GOTO/DEL 下发命令，
        // 预置位实为未实现。直接返回明确「不支持」，杜绝构造无效 PRESET_* 串经 valueOf 假成功 500 误导前端。
        // 真实下发（档 A）待预置位需求正式排期、确认上游指令构造器能力后单独落地。
        return AjaxResult.error("预置位暂不支持");
    }
}
