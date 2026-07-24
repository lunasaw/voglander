package io.github.lunasaw.voglander.service.sse;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitterTestCapture;

class RealRedisSseEventBusIntegrationTest {

    private LettuceConnectionFactory firstFactory;
    private LettuceConnectionFactory secondFactory;
    private RedisBackedSseEventBus firstBus;
    private RedisBackedSseEventBus secondBus;

    @BeforeEach
    void setUp() {
        firstFactory = connectionFactory();
        secondFactory = connectionFactory();
        boolean available;
        try {
            available = "PONG".equalsIgnoreCase(firstFactory.getConnection().ping());
        } catch (Exception e) {
            available = false;
        }
        Assumptions.assumeTrue(available, "真实 Redis 不可用，跳过跨节点 SSE 门禁");

        String channel = "sse:broadcast:test:" + UUID.randomUUID();
        firstBus = redisBus(firstFactory, channel);
        secondBus = redisBus(secondFactory, channel);
        firstBus.afterPropertiesSet();
        secondBus.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        if (firstBus != null) firstBus.destroy();
        if (secondBus != null) secondBus.destroy();
        if (firstFactory != null) firstFactory.destroy();
        if (secondFactory != null) secondFactory.destroy();
    }

    @Test
    void realRedisDeliversLocallyAndRemotelyOnceWithIdenticalAuthorization() throws Exception {
        SseEmitterTestCapture local = register(firstBus, "local-user");
        SseEmitterTestCapture remote = register(secondBus, "remote-user");

        firstBus.publish(taskEvent("IMAGE_COLLECTION", "allowed-real-redis"));

        await().atMost(Duration.ofSeconds(3))
            .until(() -> remote.dump().contains("allowed-real-redis"));
        assertEquals(1, occurrences(local.dump(), "allowed-real-redis"));
        assertEquals(1, occurrences(remote.dump(), "allowed-real-redis"));

        firstBus.publish(taskEvent("DATA_EXPORT", "denied-real-redis"));
        Thread.sleep(250);
        assertFalse(local.dump().contains("denied-real-redis"));
        assertFalse(remote.dump().contains("denied-real-redis"));
    }

    private LettuceConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("127.0.0.1", 6379);
        configuration.setDatabase(15);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet();
        return factory;
    }

    private RedisBackedSseEventBus redisBus(LettuceConnectionFactory factory, String channel) {
        RedisBackedSseEventBus bus = new RedisBackedSseEventBus(new SseDeliveryAuthorizer(), channel);
        ReflectionTestUtils.setField(bus, "redisConnectionFactory", factory);
        ReflectionTestUtils.setField(bus, "stringRedisTemplate", new StringRedisTemplate(factory));
        return bus;
    }

    private SseEmitterTestCapture register(RedisBackedSseEventBus bus, String userId) {
        SseSubscriptionContext context = SseSubscriptionContext.authorized(userId,
            Collections.singleton("business.task"), false, true, false);
        SseEmitter emitter = bus.register(context);
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        capture.attach(emitter);
        return capture;
    }

    private SseEvent taskEvent(String taskType, String marker) {
        return new SseEvent("business.task.state", Map.of("taskType", taskType, "marker", marker));
    }

    private int occurrences(String value, String marker) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(marker, offset)) >= 0) {
            count++;
            offset += marker.length();
        }
        return count;
    }
}
