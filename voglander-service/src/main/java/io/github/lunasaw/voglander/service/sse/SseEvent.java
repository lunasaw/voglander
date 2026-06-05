package io.github.lunasaw.voglander.service.sse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 实时事件值对象。
 * <p>
 * {@code topic} 用于 emitter 订阅匹配与 SSE {@code event:} 字段（如 {@code live.ready}、{@code device.online}）；
 * {@code data} 为事件体，下发前整体 FastJSON2 序列化。
 * {@code originId} 标记发布节点（回路抑制用，见 {@link RedisBackedSseEventBus}）。
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

    /**
     * 发布节点标识（origin 回路抑制）：{@code publish} 时由本节点 {@code nodeId} 填充，
     * Redis 监听器收到 {@code originId == nodeId} 的回路广播时跳过本地分发（本节点已直发）。
     * 异节点 {@code originId} 不同，照常分发。
     */
    private String originId;

    /**
     * 业务构造：仅 topic + data，originId 由 {@code publish} 阶段回填。保持既有调用方兼容。
     *
     * @param topic 事件主题
     * @param data 事件体
     */
    public SseEvent(String topic, Object data) {
        this.topic = topic;
        this.data = data;
    }
}
