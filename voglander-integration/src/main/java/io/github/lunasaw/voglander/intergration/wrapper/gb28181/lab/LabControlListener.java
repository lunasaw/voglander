package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gb28181.common.entity.control.*;
import io.github.lunasaw.gbproxy.client.api.ControlListener;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Lab 控制监听器：把平台下发到设备 UA 的控制指令推 SSE clientcmd.*。
 * ControlListener 是多实例观察者，与 CascadeControlHandler 可共存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabControlListener implements ControlListener {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onPtzControl(String platformId, DeviceControlPtz cmd) {
        Map<String, Object> data = new HashMap<>();
        data.put("platformId", platformId);
        data.put("channelId", cmd.getDeviceId() != null ? cmd.getDeviceId() : "");
        String hex = cmd.getPtzCmd() != null ? cmd.getPtzCmd() : "";
        data.put("ptzCmd", hex);
        data.put("parsed", PtzCmdParser.parse(hex));
        data.put("ts", System.currentTimeMillis());
        publish("clientcmd.ptz", data);
    }

    @Override
    public void onTeleBoot(String platformId, DeviceControlTeleBoot cmd) {
        publish("clientcmd.reboot", simple(platformId, cmd.getDeviceId()));
    }

    @Override
    public void onRecord(String platformId, DeviceControlRecordCmd cmd) {
        Map<String, Object> data = simple(platformId, cmd.getDeviceId());
        data.put("recordCmd", cmd.getRecordCmd());
        publish("clientcmd.record", data);
    }

    @Override
    public void onGuard(String platformId, DeviceControlGuard cmd) {
        publish("clientcmd.guard", simple(platformId, cmd.getDeviceId()));
    }

    @Override
    public void onAlarmReset(String platformId, DeviceControlAlarm cmd) {
        publish("clientcmd.alarmreset", simple(platformId, cmd.getDeviceId()));
    }

    @Override
    public void onIFrame(String platformId, DeviceControlIFame cmd) {
        publish("clientcmd.iframe", simple(platformId, cmd.getDeviceId()));
    }

    private Map<String, Object> simple(String platformId, String channelId) {
        Map<String, Object> d = new HashMap<>();
        d.put("platformId", platformId != null ? platformId : "");
        d.put("channelId", channelId != null ? channelId : "");
        d.put("ts", System.currentTimeMillis());
        return d;
    }

    private void publish(String topic, Map<String, Object> data) {
        log.debug("Lab 收到控制指令, topic={}", topic);
        eventPublisher.publishEvent(new SseRelayEvent(topic, data));
    }
}
