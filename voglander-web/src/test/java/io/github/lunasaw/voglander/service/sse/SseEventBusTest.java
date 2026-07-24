package io.github.lunasaw.voglander.service.sse;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitterTestCapture;

import io.github.lunasaw.voglander.BaseTest;
import lombok.extern.slf4j.Slf4j;

/**
 * RedisBackedSseEventBus 单节点集成测试。
 * <p>
 * 通过 {@link SseEmitterTestCapture} 捕获 emitter 实际下发内容，验证：
 * 订阅同 topic 的 emitter 收到事件；不同 topic 的不收到；topic 域前缀匹配（订阅 "live" 收 "live.ready"）。
 * 依赖真实 Redis；不可用时自动跳过。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class SseEventBusTest extends BaseTest {

    @Autowired
    private SseEventBus            sseEventBus;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    public void checkRedis() {
        boolean available;
        try {
            connectionFactory.getConnection().ping();
            available = true;
        } catch (Exception e) {
            available = false;
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(available, "Redis 不可用，跳过");
    }

    @Test
    public void testPublishLocal_SameTopicReceives_OtherTopicDoesNot() {
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        SseEmitter emitter = sseEventBus.register(context("u1", "live"));
        capture.attach(emitter);

        sseEventBus.publishLocal(new SseEvent("live.ready", Map.of("streamId", "s1")));
        sseEventBus.publishLocal(new SseEvent("alarm.new", Map.of("id", 1)));

        String all = capture.dump();
        assertTrue(all.contains("live.ready"), "订阅 live 应收到 live.ready, 实际=" + all);
        assertTrue(all.contains("s1"), "应包含事件数据");
        assertFalse(all.contains("alarm.new"), "未订阅 alarm 不应收到 alarm.new");
    }

    @Test
    public void testPublish_DeliversToMatchingEmitter() throws Exception {
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        SseEmitter emitter = sseEventBus.register(context("u2", "device"));
        capture.attach(emitter);

        sseEventBus.publish(new SseEvent("device.online", Map.of("deviceId", "dev-9")));

        // publish 本地直发（跨节点 Redis 回环可能再投递一次），断言至少收到一次
        Thread.sleep(300);
        assertTrue(capture.dump().contains("device.online"), "publish 应投递到匹配 emitter");
        assertTrue(capture.dump().contains("dev-9"));
    }

    @Test
    public void testExactTopicMatch() {
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        SseEmitter emitter = sseEventBus.register(context("u3", "live.ready"));
        capture.attach(emitter);

        sseEventBus.publishLocal(new SseEvent("live.ready", Map.of("streamId", "s2")));
        assertTrue(capture.dump().contains("s2"), "精确订阅 live.ready 应收到");
    }

    private SseSubscriptionContext context(String userId, String topic) {
        return SseSubscriptionContext.authorized(userId, new HashSet<>(Set.of(topic)), true, true, true);
    }
}
