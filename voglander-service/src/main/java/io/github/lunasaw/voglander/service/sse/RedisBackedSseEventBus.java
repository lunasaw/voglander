package io.github.lunasaw.voglander.service.sse;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
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
 * 基于 Redis Pub/Sub 跨节点扇出的 SSE 事件总线实现。
 * <p>
 * 本地维护 {@code emitterId -> EmitterHolder} 映射，{@link #publish} 先本地直发再广播给其他节点；
 * 其他节点经 {@link RedisMessageListenerContainer} 收到后仅本地分发（{@link #publishLocal}），避免回路。
 * 15s 心跳维持连接（防 Nginx/代理超时断连），emitter 完成/超时/错误时自动回收。
 * </p>
 * <p>
 * 使用主 Redis-A（{@code stringRedisTemplate} / 默认 {@code redisConnectionFactory}），不混入 invite Redis-B。
 * </p>
 * <p>
 * 条件启用：需显式配置 {@code sse.type=redis} 启用。测试环境默认使用 {@link LocalSseEventBus}（{@code sse.type=local} 或未配置）。
 * 生产环境多节点部署必须显式设置 {@code sse.type=redis}。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "sse.type", havingValue = "redis")
@TechnicalScheduler(category = TechnicalScheduler.Category.MAINTENANCE)
public class RedisBackedSseEventBus implements SseEventBus, InitializingBean, DisposableBean {

    private static final String                       DEFAULT_REDIS_CHANNEL = "sse:broadcast";
    private static final int                          MAX_EMITTERS  = 5000;
    private static final long                         HEARTBEAT_MS  = 15_000;

    /**
     * 本节点唯一标识（origin 回路抑制）。本实例发出的广播经 Redis 回到本节点时据此跳过二次本地分发，
     * 多节点部署下异节点 nodeId 不同照常分发。
     */
    private final String                              nodeId        = java.util.UUID.randomUUID().toString();

    @Autowired
    private StringRedisTemplate                       stringRedisTemplate;

    @Autowired
    private RedisConnectionFactory                    redisConnectionFactory;

    private final ConcurrentHashMap<String, EmitterHolder> emitters = new ConcurrentHashMap<>();
    private final SseDeliveryAuthorizer authorizer;
    private final String redisChannel;
    private final SseDomainMetrics metrics;
    private RedisMessageListenerContainer listenerContainer;

    @Autowired
    public RedisBackedSseEventBus(SseDeliveryAuthorizer authorizer, SseDomainMetrics metrics) {
        this(authorizer, metrics, DEFAULT_REDIS_CHANNEL);
    }

    public RedisBackedSseEventBus(SseDeliveryAuthorizer authorizer) {
        this(authorizer, null, DEFAULT_REDIS_CHANNEL);
    }

    RedisBackedSseEventBus(SseDeliveryAuthorizer authorizer, String redisChannel) {
        this(authorizer, null, redisChannel);
    }

    RedisBackedSseEventBus(SseDeliveryAuthorizer authorizer, SseDomainMetrics metrics, String redisChannel) {
        this.authorizer = authorizer;
        this.metrics = metrics;
        this.redisChannel = redisChannel;
        if (metrics != null) metrics.bindEmitterCount("REDIS", emitters);
    }

    @Override
    public SseEmitter register(SseSubscriptionContext context) {
        if (emitters.size() >= MAX_EMITTERS) {
            log.warn("SSE emitter 数已达上限 {}，拒绝新连接", MAX_EMITTERS);
            if (metrics != null) metrics.registrationDenied("REDIS", "700006");
            throw new ServiceException(ServiceExceptionEnum.SSE_CONNECTION_LIMIT);
        }
        if (context == null) {
            if (metrics != null) metrics.registrationDenied("REDIS", "700007");
            throw new ServiceException(ServiceExceptionEnum.SSE_TOPIC_INVALID);
        }
        String emitterId = context.getEmitterId();
        // 0L = 不超时，由心跳维持连接
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(emitterId, new EmitterHolder(emitter, context));

        Runnable cleanup = () -> emitters.remove(emitterId);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        log.debug("SSE 注册成功, emitterId={}, topics={}, 当前连接数={}",
            emitterId, context.getTopics(), emitters.size());
        return emitter;
    }

    @Override
    public void publish(SseEvent event) {
        // 标记本节点为来源，供回路抑制
        event.setOriginId(nodeId);
        // 本节点直发 + 广播给其他节点
        publishLocal(event);
        try {
            stringRedisTemplate.convertAndSend(redisChannel, JSON.toJSONString(event));
        } catch (Exception e) {
            if (metrics != null) metrics.sendFailure("REDIS");
            log.warn("SSE Redis 广播失败, topic={}", event.getTopic(), e);
        }
    }

    /**
     * 处理 Redis 跨节点广播到达的事件：origin 回路抑制——本节点发出的广播（{@code originId == nodeId}）
     * 已在 {@link #publish} 本地直发，直接跳过避免单机重复投递；异节点来源照常本地分发。
     *
     * @param event 跨节点广播事件
     */
    void handleRemote(SseEvent event) {
        if (event == null) {
            return;
        }
        if (nodeId.equals(event.getOriginId())) {
            // 本节点回路：已本地直发，跳过
            return;
        }
        publishLocal(event);
    }

    @Override
    public void publishLocal(SseEvent event) {
        String data = JSON.toJSONString(event.getData());
        emitters.forEach((id, holder) -> {
            if (!authorizer.allow(holder.context, event)) {
                if (metrics != null) metrics.deliveryFiltered("REDIS");
                return;
            }
            try {
                holder.emitter.send(SseEmitter.event()
                    .name(event.getTopic())
                    .data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitters.remove(id);
                if (metrics != null) metrics.sendFailure("REDIS");
            }
        });
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
                if (metrics != null) metrics.sendFailure("REDIS");
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

    /**
     * 订阅 Redis 跨节点广播，收到后仅本地分发。
     */
    @Override
    public void afterPropertiesSet() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                SseEvent event = JSON.parseObject(new String(message.getBody()), SseEvent.class);
                handleRemote(event);
            } catch (Exception e) {
                log.warn("SSE 跨节点消息解析失败", e);
            }
        }, new ChannelTopic(redisChannel));
        container.afterPropertiesSet();
        container.start();
        listenerContainer = container;
        log.info("SSE Event Bus: RedisBackedSseEventBus (distributed) activated, channel={}", redisChannel);
    }

    @Override
    public void destroy() {
        emitters.forEach((id, holder) -> holder.emitter.complete());
        emitters.clear();
        RedisMessageListenerContainer container = listenerContainer;
        listenerContainer = null;
        if (container != null) {
            try {
                container.destroy();
            } catch (Exception e) {
                log.warn("SSE Redis listener 关闭失败", e);
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class EmitterHolder {
        private SseEmitter  emitter;
        private SseSubscriptionContext context;
    }
}
