package io.github.lunasaw.voglander.service.sse;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent;
import lombok.RequiredArgsConstructor;

/** Bridges committed task facts to the existing local/cross-node SSE bus. */
@Component
@RequiredArgsConstructor
public class BusinessTaskSseEventListener {

    private final SseEventBus sseEventBus;

    @EventListener
    public void onCommitted(BusinessTaskSseEvent event) {
        if (event == null || event.getTopic() == null) {
            return;
        }
        sseEventBus.publish(new SseEvent(event.getTopic(), event.toData()));
    }
}
