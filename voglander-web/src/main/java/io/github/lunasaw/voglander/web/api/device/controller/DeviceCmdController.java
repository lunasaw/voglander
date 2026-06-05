package io.github.lunasaw.voglander.web.api.device.controller;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/device-cmd")
@Tag(name = "设备主动指令")
public class DeviceCmdController {

    @Autowired private DeviceCommandService deviceCommandService;

    @PostMapping("/query-catalog")
    public AjaxResult<Void> queryCatalog(@RequestBody Map<String, String> body) {
        deviceCommandService.queryCatalog(body.get("deviceId"));
        return AjaxResult.success(null);
    }

    @PostMapping("/query-info")
    public AjaxResult<Void> queryInfo(@RequestBody Map<String, String> body) {
        deviceCommandService.queryDeviceInfo(body.get("deviceId"));
        return AjaxResult.success(null);
    }

    @PostMapping("/reboot")
    @Operation(summary = "重启设备（记录操作日志）")
    public AjaxResult<Boolean> reboot(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String deviceId = body.get("deviceId");
        log.info("[AUDIT] reboot device={}, ip={}", deviceId, request.getRemoteAddr());
        ResultDTO<Void> r = deviceCommandService.reboot(deviceId);
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/record")
    public AjaxResult<Void> record(@RequestBody Map<String, String> body) {
        DeviceQueryReq req = new DeviceQueryReq();
        req.setDeviceId(body.get("deviceId"));
        deviceCommandService.queryDevice(req);
        return AjaxResult.success(null);
    }
}
