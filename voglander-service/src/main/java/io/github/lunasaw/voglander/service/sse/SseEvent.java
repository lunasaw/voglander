package io.github.lunasaw.voglander.service.sse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 实时事件值对象。
 * <p>
 * {@code topic} 用于 emitter 订阅匹配与 SSE {@code event:} 字段（如 {@code live.ready}、{@code device.online}）；
 * {@code data} 为事件体，下发前整体 FastJSON2 序列化。
 * </p>
 *
 * @author luna
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseEvent {

    /**
     * 事件主题，如 "live.ready" / "device.online" / "alarm.new"。
     */
    private String topic;

    /**
     * 事件体，序列化为 JSON 下发。
     */
    private Object data;
}
