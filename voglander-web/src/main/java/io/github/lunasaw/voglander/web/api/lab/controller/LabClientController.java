package io.github.lunasaw.voglander.web.api.lab.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabSipClient;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabKeepaliveScheduler;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.web.api.lab.domain.LabAlarmPushReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabCatalogPushReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabDeviceInfoPushReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabKeepaliveAutoReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabRegisterReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Autowired private VoglanderSipClientProperties clientProps;
    @Autowired private VoglanderSipServerProperties serverProps;

    @PostMapping("/register")
    @Operation(summary = "设备主动注册")
    public AjaxResult<Void> register(@RequestBody(required = false) LabRegisterReq req) {
        labSipClient.register(req != null ? req.getExpires() : 3600);
        return AjaxResult.success();
    }

    @PostMapping("/unregister")
    @Operation(summary = "设备注销（expires=0）")
    public AjaxResult<Void> unregister() {
        labSipClient.register(0);
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
    @Operation(summary = "主动上报目录")
    public AjaxResult<Void> pushCatalog(@RequestBody(required = false) LabCatalogPushReq req) {
        int count = req != null ? req.getChannelCount() : 4;
        String name = (req != null && req.getCatalogName() != null) ? req.getCatalogName() : "Lab-Channel";
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

    @GetMapping("/config")
    @Operation(summary = "返回当前 Lab 身份与端口配置")
    public AjaxResult<Map<String, Object>> config() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("clientId",   clientProps.getClientId());
        info.put("clientIp",   clientProps.getDomain());
        info.put("clientPort", clientProps.getPort());
        info.put("serverId",   serverProps.getServerId());
        info.put("serverIp",   serverProps.getIp());
        info.put("serverPort", serverProps.getPort());
        info.put("topics", new String[]{
            "device.register","device.online","device.offline","device.keepalive",
            "device.catalog","device.info","session.invite_ok","session.bye",
            "clientcmd.register.ok","clientcmd.register.fail","clientcmd.register.challenge",
            "clientcmd.ptz","clientcmd.record","clientcmd.reboot","clientcmd.iframe",
            "clientcmd.alarmreset","clientcmd.query.catalog","clientcmd.query.deviceinfo",
            "clientcmd.query.devicestatus","clientcmd.config.basicparam",
            "clientcmd.broadcast","clientcmd.invite","alarm.new"
        });
        return AjaxResult.success(info);
    }
}
