package io.github.lunasaw.voglander.service.sse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.alibaba.fastjson2.JSON;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地（单节点）SSE 事件总线实现，用于测试环境或单机部署。
 * <p>
 * 仅本地分发，不跨节点，不依赖 Redis。
 * </p>
 * <p>
 * 条件启用：通过 {@code sse.type=local} 显式启用，或在未配置 {@code sse.type} 时作为默认实现（测试环境默认）。
 * 生产环境多节点部署应使用 {@code sse.type=redis} 启用 {@link RedisBackedSseEventBus}。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sse.type", havingValue = "local", matchIfMissing = true)
public class LocalSseEventBus implements SseEventBus {

    @Data
    @AllArgsConstructor
    private static class EmitterHolder {
        private SseEmitter emitter;
        private Set<String> topics;
    }

    private final Map<String, EmitterHolder> emitters = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("SSE Event Bus: LocalSseEventBus (single-node) activated");
    }

    @Override
    public SseEmitter register(String userId, Set<String> topics) {
        SseEmitter emitter = new SseEmitter(3600_000L);

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.debug("SSE emitter completed: {}", userId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.debug("SSE emitter timeout: {}", userId);
        });

        emitter.onError(e -> {
            emitters.remove(userId);
            log.error("SSE emitter error: {}", userId, e);
        });

        emitters.put(userId, new EmitterHolder(emitter, topics));
        log.debug("Registered SSE emitter: {} for topics: {}", userId, topics);
        return emitter;
    }

    @Override
    public void publish(SseEvent event) {
        publishLocal(event);
    }

    @Override
    public void publishLocal(SseEvent event) {
        if (event == null || event.getTopic() == null) {
            return;
        }

        String topic = event.getTopic();
        String json = JSON.toJSONString(event.getData());
        Set<String> toRemove = ConcurrentHashMap.newKeySet();

        emitters.forEach((userId, holder) -> {
            if (holder.getTopics() == null || !matches(holder.getTopics(), topic)) {
                return;
            }

            try {
                holder.getEmitter().send(SseEmitter.event()
                    .name(topic)
                    .data(json, MediaType.APPLICATION_JSON));
                log.debug("Sent SSE event to {}: topic={}", userId, topic);
            } catch (Exception e) {
                log.warn("Failed to send SSE event to {}: {}", userId, e.getMessage());
                toRemove.add(userId);
            }
        });

        toRemove.forEach(emitters::remove);
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
        int dot = topic.indexOf('.');
        if (dot > 0) {
            return subscribed.contains(topic.substring(0, dot));
        }
        return false;
    }
}
