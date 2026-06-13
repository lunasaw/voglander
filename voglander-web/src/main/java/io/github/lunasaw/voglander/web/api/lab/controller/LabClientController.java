package io.github.lunasaw.voglander.web.api.lab.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabChannelHolder;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabMediaPushService;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabPushProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabSessionHolder;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabSipClient;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabKeepaliveScheduler;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.web.api.lab.domain.LabAlarmPushReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabCatalogPushReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabDeviceInfoPushReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabKeepaliveAutoReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabPushStartReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabRegisterReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * GB28181 协议验证台 — 客户端（设备 UA）控制台。
 * 仅在 {@code voglander.protocol-lab.enabled=true} 时注册，生产 profile 不激活。
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/lab/client")
@Tag(name = "协议验证台 - 设备端")
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabClientController {

    @Autowired private LabSipClient              labSipClient;
    @Autowired private LabKeepaliveScheduler     labKeepaliveScheduler;
    @Autowired private LabSessionHolder          labSessionHolder;
    @Autowired private LabChannelHolder          labChannelHolder;
    @Autowired private LabMediaPushService       labMediaPushService;
    @Autowired private LabPushProperties         pushProps;
    @Autowired private VoglanderSipClientProperties clientProps;
    @Autowired private VoglanderSipServerProperties serverProps;

    @PostMapping("/register")
    @Operation(summary = "设备主动注册（可带目标平台/身份覆盖；不带=自环）")
    public AjaxResult<Void> register(@RequestBody(required = false) LabRegisterReq req) {
        if (req != null && LabSessionHolder.hasOverride(req.getServerId(), req.getServerIp(), req.getServerPort(),
            req.getServerDomain(), req.getTransport(), req.getClientId(), req.getClientPassword())) {
            labSessionHolder.apply(new LabSessionHolder.Snapshot(
                req.getServerId(), req.getServerIp(), req.getServerPort(),
                req.getServerDomain(), req.getTransport(),
                req.getClientId(), req.getClientPassword()));
        } else {
            labSessionHolder.reset();   // 不填参数 = 自环
        }
        labSipClient.register(req != null ? req.getExpires() : 3600);
        return AjaxResult.success();
    }

    @PostMapping("/unregister")
    @Operation(summary = "设备注销（expires=0），同时 holder 重置回自环")
    public AjaxResult<Void> unregister() {
        labSipClient.register(0);
        labSessionHolder.reset();
        return AjaxResult.success();
    }

    @PostMapping("/keepalive")
    @Operation(summary = "发送单次心跳")
    public AjaxResult<Void> keepalive() {
        labSipClient.keepalive();
        return AjaxResult.success();
    }

    @PostMapping("/keepalive/auto")
    @Operation(summary = "周期心跳开关")
    public AjaxResult<Map<String, Object>> keepaliveAuto(@RequestBody(required = false) LabKeepaliveAutoReq req) {
        boolean enabled = req != null && req.isEnabled();
        int interval = req != null ? req.getIntervalSec() : 30;
        labKeepaliveScheduler.setAuto(enabled, interval);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("enabled", labKeepaliveScheduler.isEnabled());
        state.put("intervalSec", labKeepaliveScheduler.getIntervalSec());
        return AjaxResult.success(state);
    }

    @PostMapping("/catalog/push")
    @Operation(summary = "主动上报目录，并更新被查询时回应的通道配置")
    public AjaxResult<Void> pushCatalog(@RequestBody(required = false) LabCatalogPushReq req) {
        int count = req != null ? req.getChannelCount() : LabChannelHolder.DEFAULT_COUNT;
        // 默认 name 对齐 LabChannelHolder.DEFAULT_NAME_PREFIX("Lab-ch")，
        // 否则点一次默认上报会把被动回应通道名从 Lab-ch1 改成 Lab-Channel1
        String name = (req != null && StringUtils.isNotBlank(req.getCatalogName()))
            ? req.getCatalogName() : LabChannelHolder.DEFAULT_NAME_PREFIX;
        labChannelHolder.update(count, name);   // 被动回应同步用此配置
        labSipClient.pushCatalog(count, name);
        return AjaxResult.success();
    }

    @PostMapping("/device-info/push")
    @Operation(summary = "主动上报设备信息")
    public AjaxResult<Void> pushDeviceInfo(@RequestBody(required = false) LabDeviceInfoPushReq req) {
        String mfr   = (req != null && req.getManufacturer() != null) ? req.getManufacturer() : "Voglander";
        String model = (req != null && req.getModel() != null)        ? req.getModel()        : "LabDevice";
        String fw    = (req != null && req.getFirmware() != null)      ? req.getFirmware()     : "v1.0.6";
        labSipClient.pushDeviceInfo(mfr, model, fw);
        return AjaxResult.success();
    }

    @PostMapping("/alarm/push")
    @Operation(summary = "主动上报告警")
    public AjaxResult<Void> pushAlarm(@RequestBody(required = false) LabAlarmPushReq req) {
        labSipClient.pushAlarm(
            req != null ? req.getAlarmType() : 1,
            req != null ? req.getPriority()  : 1,
            req != null ? req.getChannelId() : null);
        return AjaxResult.success();
    }

    @PostMapping("/push/start")
    @Operation(summary = "模拟推流：用 ffmpeg 把视频推到最近一次 INVITE 的收流目标")
    public AjaxResult<Object> pushStart(@RequestBody(required = false) LabPushStartReq req) {
        return AjaxResult.success(labMediaPushService.startPush(
            null,
            req != null ? req.getFfmpegPath() : null,
            req != null ? req.getMediaFile() : null));
    }

    @PostMapping("/push/stop")
    @Operation(summary = "停止模拟推流")
    public AjaxResult<Object> pushStop() {
        return AjaxResult.success(labMediaPushService.stop());
    }

    @GetMapping("/push/status")
    @Operation(summary = "查询当前模拟推流状态")
    public AjaxResult<Object> pushStatus() {
        return AjaxResult.success(labMediaPushService.status());
    }

    @GetMapping("/config")
    @Operation(summary = "返回当前 Lab 身份与端口配置（含 holder 覆盖后的生效值）")
    public AjaxResult<Map<String, Object>> config() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("clientId",   clientProps.getClientId());
        info.put("clientIp",   clientProps.getDomain());
        info.put("clientPort", clientProps.getPort());

        LabSessionHolder.Snapshot s = labSessionHolder.current();
        boolean customized = s != null;
        // 目标返回生效值：holder 优先，否则 sip.server.*
        info.put("serverId",     customized && s.getServerId()     != null ? s.getServerId()     : serverProps.getServerId());
        info.put("serverIp",     customized && s.getServerIp()     != null ? s.getServerIp()     : serverProps.getIp());
        info.put("serverPort",   customized && s.getServerPort()   != null ? s.getServerPort()   : serverProps.getPort());
        info.put("serverDomain", customized && s.getServerDomain() != null ? s.getServerDomain() : serverProps.getDomain());
        info.put("transport",    customized && s.getTransport()    != null ? s.getTransport()    : clientProps.getTransport());
        info.put("targetCustomized", customized);

        // 模拟推流配置回显（前端表单初值）
        info.put("pushAuto",      pushProps.isAuto());
        info.put("ffmpegPath",    pushProps.getFfmpegPath());
        info.put("mediaFile",     pushProps.getMediaFile());

        info.put("topics", new String[]{
            "device.register","device.online","device.offline","device.keepalive",
            "device.catalog","device.info","session.invite_ok","session.bye",
            "clientcmd.register.ok","clientcmd.register.fail","clientcmd.register.challenge",
            "clientcmd.ptz","clientcmd.record","clientcmd.reboot","clientcmd.iframe",
            "clientcmd.alarmreset","clientcmd.query.catalog","clientcmd.query.deviceinfo",
            "clientcmd.query.devicestatus","clientcmd.config.basicparam",
            "clientcmd.query.recordinfo","clientcmd.query.configdownload",
            "clientcmd.query.preset","clientcmd.query.mobileposition","clientcmd.query.alarm",
            "clientcmd.broadcast","clientcmd.invite","clientcmd.bye","alarm.new",
            "clientcmd.push.started","clientcmd.push.stopped","clientcmd.push.failed"
        });
        return AjaxResult.success(info);
    }
}
