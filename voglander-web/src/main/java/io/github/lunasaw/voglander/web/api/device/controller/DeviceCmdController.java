package io.github.lunasaw.voglander.web.api.device.controller;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceAlarmQueryReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceConfigReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRecordControlReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRecordQueryReq;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.client.service.device.Gb28181DeviceCommandService;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.web.api.device.req.DeviceBroadcastReq;
import io.github.lunasaw.voglander.web.api.device.req.DeviceConfigDownloadReq;
import io.github.lunasaw.voglander.web.api.device.req.DeviceMobilePositionReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 设备主动指令控制器（平台端 → 设备下发）。
 *
 * <p>
 * 1.0.7 S2：4 个旧端点入参由 {@code Map<String,String>} 升级为类型化 {@code *Req}；
 * 新增 GB 专属支链端点（查询补全 / 配置 / 录像控制 / 报警 / 广播）。
 * </p>
 *
 * <p>
 * 注入策略（ARCHITECTURE.md 1.0.7 §8.1）：
 * </p>
 * <ul>
 * <li>协议无关动作（query-catalog/query-info/reboot）��入 {@link DeviceCommandService}（此处用 GB bean，单协议下等价）；</li>
 * <li>GB 专属支链注入 {@link Gb28181DeviceCommandService} 子接口，<b>不经 DeviceAgreementService 多协议路由</b>。</li>
 * </ul>
 *
 * <p>
 * 注意：PTZ 走 {@code PtzController /api/v1/ptz}、实时点播走 {@code /api/v1/live}、录像回放走
 * {@code /api/v1/playback}，均不在本控制器重复造。
 * </p>
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/device-cmd")
@Tag(name = "设备主动指令")
public class DeviceCmdController {

    /**
     * GB 专属命令子接口（同时是协议��关 SPI 的 GB 实现）。bean 名 GbDeviceCommandService。
     */
    @Autowired
    private Gb28181DeviceCommandService gbCommandService;

    // ================================
    // 旧端点 Req 化（路径不变）
    // ================================

    @PostMapping("/query-catalog")
    @Operation(summary = "查询设备目录")
    public AjaxResult<Boolean> queryCatalog(@Valid @RequestBody DeviceQueryReq req) {
        ResultDTO<Void> r = gbCommandService.queryCatalog(req.getDeviceId());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/query-info")
    @Operation(summary = "查询设备信息")
    public AjaxResult<Boolean> queryInfo(@Valid @RequestBody DeviceQueryReq req) {
        ResultDTO<Void> r = gbCommandService.queryDeviceInfo(req.getDeviceId());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/reboot")
    @Operation(summary = "重启设备（记录操作日志）")
    public AjaxResult<Boolean> reboot(@Valid @RequestBody DeviceQueryReq req, HttpServletRequest request) {
        log.info("[AUDIT] reboot device={}, ip={}", req.getDeviceId(), request.getRemoteAddr());
        ResultDTO<Void> r = gbCommandService.reboot(req.getDeviceId());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/record")
    @Operation(summary = "查询录像（触发，结果由入站缓存）")
    public AjaxResult<Boolean> record(@Valid @RequestBody DeviceRecordQueryReq req) {
        ResultDTO<Void> r = gbCommandService.queryRecord(req);
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    // ================================
    // 查询补全支链
    // ================================

    @PostMapping("/query-status")
    @Operation(summary = "查询设备状态")
    public AjaxResult<Boolean> queryStatus(@Valid @RequestBody DeviceQueryReq req) {
        ResultDTO<Void> r = gbCommandService.queryDeviceStatus(req.getDeviceId());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/query-preset")
    @Operation(summary = "查询预置位")
    public AjaxResult<Boolean> queryPreset(@Valid @RequestBody DeviceQueryReq req) {
        ResultDTO<Void> r = gbCommandService.queryPreset(req.getDeviceId());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/query-mobile-position")
    @Operation(summary = "查询移动位置订阅")
    public AjaxResult<Boolean> queryMobilePosition(@Valid @RequestBody DeviceMobilePositionReq req) {
        ResultDTO<Void> r = gbCommandService.queryMobilePosition(req.getDeviceId(), req.getInterval());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    // ================================
    // 配置支链
    // ================================

    @PostMapping("/config/download")
    @Operation(summary = "下载设备配置")
    public AjaxResult<Boolean> configDownload(@Valid @RequestBody DeviceConfigDownloadReq req) {
        ResultDTO<Void> r = gbCommandService.downloadConfig(req.getDeviceId(), req.getConfigType());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/config/set")
    @Operation(summary = "下发设备配置（记录操作日志）")
    public AjaxResult<Boolean> configSet(@Valid @RequestBody DeviceConfigReq req, HttpServletRequest request) {
        log.info("[AUDIT] config-set device={}, ip={}", req.getDeviceId(), request.getRemoteAddr());
        ResultDTO<Void> r = gbCommandService.setDeviceConfig(req);
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    // ================================
    // 录像控制支链
    // ================================

    @PostMapping("/record/start")
    @Operation(summary = "开始录像（记录操作日志）")
    public AjaxResult<Boolean> recordStart(@Valid @RequestBody DeviceRecordControlReq req, HttpServletRequest request) {
        log.info("[AUDIT] record-start device={}, ip={}", req.getDeviceId(), request.getRemoteAddr());
        ResultDTO<Void> r = gbCommandService.controlRecord(req.getDeviceId(), true);
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/record/stop")
    @Operation(summary = "停止录像（记录操作日志）")
    public AjaxResult<Boolean> recordStop(@Valid @RequestBody DeviceRecordControlReq req, HttpServletRequest request) {
        log.info("[AUDIT] record-stop device={}, ip={}", req.getDeviceId(), request.getRemoteAddr());
        ResultDTO<Void> r = gbCommandService.controlRecord(req.getDeviceId(), false);
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    // ================================
    // 报警支链
    // ================================

    @PostMapping("/alarm/query")
    @Operation(summary = "查询设备报警")
    public AjaxResult<Boolean> alarmQuery(@Valid @RequestBody DeviceAlarmQueryReq req) {
        ResultDTO<Void> r = gbCommandService.queryAlarm(req);
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/alarm/control")
    @Operation(summary = "报警控制/复位（记录操作日志）")
    public AjaxResult<Boolean> alarmControl(@Valid @RequestBody DeviceAlarmQueryReq req, HttpServletRequest request) {
        log.info("[AUDIT] alarm-control device={}, ip={}", req.getDeviceId(), request.getRemoteAddr());
        ResultDTO<Void> r = gbCommandService.controlAlarm(req.getDeviceId(), req.getAlarmMethod(), req.getAlarmType());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    // ================================
    // 广播支链
    // ================================

    @PostMapping("/broadcast")
    @Operation(summary = "语音广播（记录操作日志）")
    public AjaxResult<Boolean> broadcast(@Valid @RequestBody DeviceBroadcastReq req, HttpServletRequest request) {
        log.info("[AUDIT] broadcast device={}, ip={}", req.getDeviceId(), request.getRemoteAddr());
        ResultDTO<Void> r = gbCommandService.broadcast(req.getDeviceId());
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }
}
