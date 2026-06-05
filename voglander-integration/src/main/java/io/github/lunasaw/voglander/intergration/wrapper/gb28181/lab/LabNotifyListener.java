package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gbproxy.client.api.NotifyListener;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Lab 通知监听器（NotifyListener 多实例，可共存）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabNotifyListener implements NotifyListener {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onBroadcastNotify(String platformId, DeviceBroadcastNotify notify) {
        Map<String, Object> d = new HashMap<>();
        d.put("platformId", platformId != null ? platformId : "");
        d.put("deviceId", notify.getDeviceId() != null ? notify.getDeviceId() : "");
        d.put("ts", System.currentTimeMillis());
        log.debug("Lab 收到广播通知, platformId={}", platformId);
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.broadcast", d));
    }
}
