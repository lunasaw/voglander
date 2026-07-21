package io.github.lunasaw.voglander.service.sse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.common.anno.TechnicalScheduler;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地内存版本的 SSE 事件总线实现（单机模式，无 Redis 依赖）。
 * <p>
 * 当 Redis 不可用时自动启用此实现，仅支持单节点内的 SSE 事件分发。
 * 15s 心跳维持连接（防 Nginx/代理超时断连），emitter 完成/超时/错误时自动回收。
 * </p>
 * <p>
 * 与 {@link RedisBackedSseEventBus} 区别：不支持跨节点广播，适用于单节点部署或开发环境。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sse.type", havingValue = "local", matchIfMissing = true)
@TechnicalScheduler(category = TechnicalScheduler.Category.MAINTENANCE)
public class LocalSseEventBus implements SseEventBus {

    private static final int                          MAX_EMITTERS = 5000;
    private static final long                         HEARTBEAT_MS = 15_000;

    private final ConcurrentHashMap<String, EmitterHolder> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter register(String userId, Set<String> topics) {
        if (emitters.size() >= MAX_EMITTERS) {
            log.warn("SSE emitter 数已达上限 {}，拒绝新连接, userId={}", MAX_EMITTERS, userId);
            throw new ServiceException(ServiceExceptionEnum.SSE_CONNECTION_LIMIT);
        }
        String emitterId = userId + ":" + System.nanoTime();
        /* 0L = 不超时，由心跳维持连接 */
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(emitterId, new EmitterHolder(emitter, userId, topics));

        Runnable cleanup = () -> emitters.remove(emitterId);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        log.debug("SSE 注册成功 (本地模式), emitterId={}, topics={}, 当前连接数={}", emitterId, topics, emitters.size());
        return emitter;
    }

    @Override
    public void publish(SseEvent event) {
        /* 本地模式直接投递，无跨节点广播 */
        publishLocal(event);
    }

    @Override
    public void publishLocal(SseEvent event) {
        String data = JSON.toJSONString(event.getData());
        emitters.forEach((id, holder) -> {
            if (holder.topics != null && !matches(holder.topics, event.getTopic())) {
                return;
            }
            try {
                holder.emitter.send(SseEmitter.event()
                    .name(event.getTopic())
                    .data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitters.remove(id);
            }
        });
    }

    /**
     * topic 订阅匹配：精确命中或前缀域命中（如订阅 "live" 收 "live.ready"）。
     */
    private boolean matches(Set<String> subscribed, String topic) {
        if (topic == null) {
            return false;
        }
        if (subscribed.contains(topic)) {
            return true;
        }
        for (String candidate : subscribed) {
            if (candidate != null && !candidate.isEmpty() && topic.startsWith(candidate + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 15s 心跳，防止 Nginx/代理超时断连，并回收已死连接。
     */
    @Scheduled(fixedDelay = HEARTBEAT_MS)
    public void heartbeat() {
        emitters.forEach((id, holder) -> {
            try {
                holder.emitter.send(SseEmitter.event().name("ping").data(""));
            } catch (Exception e) {
                emitters.remove(id);
            }
        });
    }

    /**
     * 当前本节点 emitter 数量（监控/测试用）。
     *
     * @return emitter 数量
     */
    public int emitterCount() {
        return emitters.size();
    }

    @Data
    @AllArgsConstructor
    private static class EmitterHolder {
        private SseEmitter  emitter;
        private String      userId;
        private Set<String> topics;
    }
}
