package io.github.lunasaw.voglander.common.event;

import java.util.Map;

/**
 * 通用 SSE 中继事件（common 层）。
 * <p>
 * 用于打破依赖方向限制：{@code voglander-integration} 在 {@code service} 之下，无法直接调用
 * {@code SseEventBus}（在 {@code service} 层）。integration 侧产生需要实时推送的事件时，
 * 发布本事件（{@code ApplicationEventPublisher#publishEvent}），由 service 层的
 * {@code SseRelayListener} 监听并转投 {@code SseEventBus.publish}。
 * </p>
 * <p>
 * 与既有 {@link AlarmCreatedEvent} 同构，但更通用：直接携带 SSE {@code topic} 与 {@code data}，
 * 一个监听器即可中继任意主题（{@code device.*} / {@code clientcmd.*} / {@code session.*}）。
 * {@code data} 只应包含 FastJSON2 可序列化的基础类型 / Map，避免依赖上层 DTO。
 * </p>
 *
 * @author luna
 */
public class SseRelayEvent {

    /**
     * SSE 主题，同时作为 SSE {@code event:} 字段，如 {@code device.register}、{@code clientcmd.ptz}。
     */
    private final String              topic;

    /**
     * 事件体，序列化为 JSON 下发。
     */
    private final Map<String, Object> data;

    public SseRelayEvent(String topic, Map<String, Object> data) {
        this.topic = topic;
        this.data = data;
    }

    public String getTopic() {
        return topic;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
