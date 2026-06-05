package io.github.lunasaw.voglander.service.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitterTestCapture;

/**
 * D8 红线：单机 SSE 重复投递。{@code publish} 先本地直发再 Redis 广播，本节点监听器收到自己的广播后
 * 再次 {@code publishLocal} → 同一 emitter 收到 2 份。修复：origin 回路抑制——事件带 {@code originId}，
 * 本节点发出的广播回到本节点时跳过本地分发。
 * <p>
 * 纯单元测试（不依赖真实 Redis）：mock {@code StringRedisTemplate}（广播 no-op），直接驱动
 * {@link RedisBackedSseEventBus#handleRemote(SseEvent)} 模拟 Redis 回路。
 *
 * @author luna
 */
@DisplayName("D8 — SSE origin 回路抑制去重")
class SseOriginSuppressionTest {

    private RedisBackedSseEventBus newBus() {
        RedisBackedSseEventBus bus = new RedisBackedSseEventBus();
        // 广播 no-op：mock StringRedisTemplate，convertAndSend 不做事
        ReflectionTestUtils.setField(bus, "stringRedisTemplate",
            org.mockito.Mockito.mock(StringRedisTemplate.class));
        return bus;
    }

    private long countOccurrences(String haystack, String needle) {
        long count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    @Test
    @DisplayName("publish 一次 → 本地仅 1 份；本节点广播回路被 originId 抑制")
    void publishThenLoopback_onlyOneCopy() {
        RedisBackedSseEventBus bus = newBus();
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        SseEmitter emitter = bus.register("u1", new HashSet<>(Set.of("live")));
        capture.attach(emitter);

        SseEvent event = new SseEvent("live.ready", Map.of("streamId", "s1"));
        bus.publish(event);                 // 本地直发 1 份，并 stamp originId=nodeId
        // 模拟本节点发出的广播经 Redis 回到本节点
        bus.handleRemote(event);            // 应被 origin 抑制，不再本地分发

        long copies = countOccurrences(capture.dump(), "s1");
        assertEquals(1, copies, "本节点 publish 后回路应被抑制，emitter 仅应收到 1 份，实际 " + copies);
    }

    @Test
    @DisplayName("异节点广播(originId 不同) → 照常本地分发")
    void remoteFromOtherNode_delivered() {
        RedisBackedSseEventBus bus = newBus();
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        SseEmitter emitter = bus.register("u2", new HashSet<>(Set.of("device")));
        capture.attach(emitter);

        SseEvent remote = new SseEvent("device.online", Map.of("deviceId", "dev-9"));
        remote.setOriginId("some-other-node-uuid"); // 异节点来源
        bus.handleRemote(remote);

        assertEquals(1, countOccurrences(capture.dump(), "dev-9"),
            "异节点广播应正常本地分发 1 份");
    }
}
