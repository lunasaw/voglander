package io.github.lunasaw.voglander.service.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 事件总线。
 * <p>
 * 负责本地 emitter 注册/心跳/回收，以及跨节点事件扇出（Redis Pub/Sub）。
 * 业务侧（设备上下线、流就绪、告警等）统一调用 {@link #publish(SseEvent)} 投递事件。
 * </p>
 *
 * @author luna
 */
public interface SseEventBus {

    /**
     * 注册一个订阅给定 topic 集合的 SSE emitter。
     *
     * @param context 已认证且已授权的连接上下文
     * @return SseEmitter（交由 Spring MVC 异步写出）
     */
    SseEmitter register(SseSubscriptionContext context);

    /**
     * 投递事件：本节点直发 + 广播给其他节点。
     *
     * @param event 事件
     */
    void publish(SseEvent event);

    /**
     * 仅向本节点订阅匹配 topic 的 emitter 下发（供 Pub/Sub 监听器调用，避免广播回路）。
     *
     * @param event 事件
     */
    void publishLocal(SseEvent event);
}
