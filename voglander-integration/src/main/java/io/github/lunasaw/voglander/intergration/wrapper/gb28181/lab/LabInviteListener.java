package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Lab INVITE 监听器：平台向设备 UA 发送点播请求时推 SSE clientcmd.invite。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabInviteListener {

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onInvite(ClientInviteEvent e) {
        Map<String, Object> d = new HashMap<>();
        d.put("callId", e.getCallId() != null ? e.getCallId() : "");
        d.put("clientId", e.getUserId() != null ? e.getUserId() : "");
        d.put("ts", System.currentTimeMillis());
        log.debug("Lab 收到 INVITE, callId={}", e.getCallId());
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.invite", d));
    }
}
