package io.github.lunasaw.voglander.service.sse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * SSE 中继监听器（service 层）。
 * <p>
 * 监听 {@code integration} 层发布的 {@link SseRelayEvent}，转投到 {@link SseEventBus}。
 * 这是 integration（位于 service 之下，无法直接依赖 {@code SseEventBus}）向前端实时推送事件的
 * 标准通道：integration {@code publishEvent(SseRelayEvent)} → 本监听器 → {@code SseEventBus.publish}。
 * </p>
 * <p>
 * 与 {@code LiveStreamEventListener#onAlarmCreated} 风格一致，但通用：topic + data 直传，
 * 一个监听器中继全部 {@code device.*} / {@code clientcmd.*} / {@code session.*} 主题。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class SseRelayListener {

    @Autowired
    private SseEventBus sseEventBus;

    /**
     * 中继：把 common 层的 {@link SseRelayEvent} 转为 {@link SseEvent} 推送。
     */
    @EventListener
    public void onSseRelay(SseRelayEvent event) {
        if (event.getTopic() == null) {
            return;
        }
        sseEventBus.publish(new SseEvent(event.getTopic(), event.getData()));
        log.debug("SSE 中继推送, topic={}", event.getTopic());
    }
}
