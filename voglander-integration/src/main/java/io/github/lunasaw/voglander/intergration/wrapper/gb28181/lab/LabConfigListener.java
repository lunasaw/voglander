package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gb28181.common.entity.control.DeviceConfigControl;
import io.github.lunasaw.gbproxy.client.api.ConfigListener;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Lab 配置监听器（ConfigListener 多实例，与 CascadeControlHandler 可共存）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabConfigListener implements ConfigListener {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onBasicParamConfig(String platformId, DeviceConfigControl cfg) {
        Map<String, Object> d = new HashMap<>();
        d.put("platformId", platformId != null ? platformId : "");
        d.put("deviceId", cfg.getDeviceId() != null ? cfg.getDeviceId() : "");
        d.put("ts", System.currentTimeMillis());
        log.debug("Lab 收到基础参数配置, platformId={}", platformId);
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.config.basicparam", d));
    }
}
