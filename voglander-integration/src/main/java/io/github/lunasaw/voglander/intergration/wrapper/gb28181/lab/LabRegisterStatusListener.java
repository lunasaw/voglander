package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterChallengeEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterFailureEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterSuccessEvent;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Lab 注册状态监听器：把客户端注册结果推 SSE clientcmd.register.*。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabRegisterStatusListener {

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onSuccess(ClientRegisterSuccessEvent e) {
        publish("clientcmd.register.ok", e.getUserId(), null);
    }

    @EventListener
    public void onFailure(ClientRegisterFailureEvent e) {
        Map<String, Object> d = base(e.getUserId());
        d.put("statusCode", e.getStatusCode());
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.register.fail", d));
    }

    @EventListener
    public void onChallenge(ClientRegisterChallengeEvent e) {
        publish("clientcmd.register.challenge", e.getUserId(), null);
    }

    private void publish(String topic, String clientId, Object extra) {
        Map<String, Object> d = base(clientId);
        eventPublisher.publishEvent(new SseRelayEvent(topic, d));
        log.debug("Lab 注册事件 SSE, topic={}", topic);
    }

    private Map<String, Object> base(String clientId) {
        Map<String, Object> d = new HashMap<>();
        d.put("clientId", clientId != null ? clientId : "");
        d.put("ts", System.currentTimeMillis());
        return d;
    }
}
