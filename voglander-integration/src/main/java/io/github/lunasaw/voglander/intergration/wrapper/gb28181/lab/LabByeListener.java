package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Lab BYE 监听器：平台结束点播下发 BYE 时
 * <ol>
 *   <li>按 callId 停止对应 ffmpeg 推流；</li>
 *   <li>推 SSE {@code clientcmd.bye}（与 {@code clientcmd.invite} 对称，供前端时间线展示关流）。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabByeListener {

    private final ApplicationEventPublisher eventPublisher;
    private final LabMediaPushService       pushService;

    @EventListener
    public void onBye(ClientByeEvent e) {
        log.info("Lab 收到 BYE, callId={} → 停止推流", e.getCallId());

        // 1. 停止 ffmpeg 推流
        pushService.stopByCallId(e.getCallId());

        // 2. 推 SSE（与 clientcmd.invite 对称，前端时间线据此展示关流）
        Map<String, Object> d = new HashMap<>();
        d.put("callId", e.getCallId() != null ? e.getCallId() : "");
        d.put("statusCode", e.getStatusCode());
        d.put("ts", System.currentTimeMillis());
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.bye", d));
    }
}
