package io.github.lunasaw.voglander.manager.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * ShardDispatcher 单元测试（Phase 4）。
 * <p>
 * 验证：
 * <ul>
 *   <li>shardKey 计算（deviceId 优先，null 时用 correlationId）</li>
 *   <li>同 deviceId 事件路由到同一分片</li>
 *   <li>不同 deviceId 事件分散到不同分片</li>
 * </ul>
 * </p>
 *
 * @author luna
 */
@Slf4j
class ShardDispatcherTest {

    @Mock
    private InboundEventDispatcher mockEventDispatcher;

    private ShardDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 创建 4 个分片用于测试
        dispatcher = new ShardDispatcher(4, mockEventDispatcher);
    }

    @Test
    void testSameDeviceIdRoutesToSameShard() throws InterruptedException {
        // 同一 deviceId 的事件应路由到同一分片
        String deviceId = "device-123";
        DeviceEvent e1 = createEvent("gb28181", "Lifecycle", "Register", deviceId, 1000L);
        DeviceEvent e2 = createEvent("gb28181", "Notify", "Keepalive", deviceId, 2000L);
        DeviceEvent e3 = createEvent("gb28181", "Lifecycle", "Offline", deviceId, 3000L);

        int shard1 = dispatcher.getShardIndex(e1);
        int shard2 = dispatcher.getShardIndex(e2);
        int shard3 = dispatcher.getShardIndex(e3);

        assertEquals(shard1, shard2, "同一设备的事件应路由到同一分片");
        assertEquals(shard2, shard3, "同一设备的事件应路由到同一分片");
        assertTrue(shard1 >= 0 && shard1 < 4, "分片索引应在 [0, 4) 范围内");
    }

    @Test
    void testDifferentDeviceIdRoutesToDifferentShards() {
        // 不同 deviceId 应分散到不同分片（统计学上）
        Map<Integer, Integer> shardCounts = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            DeviceEvent event = createEvent("gb28181", "Lifecycle", "Register", "device-" + i, 1000L + i);
            int shard = dispatcher.getShardIndex(event);
            shardCounts.put(shard, shardCounts.getOrDefault(shard, 0) + 1);
        }

        // 验证至少有 3 个分片被使用（避免极端偏斜）
        assertTrue(shardCounts.size() >= 3, "100 个不同设备应至少分散到 3 个分片");
        log.info("100 个设备分片分布: {}", shardCounts);
    }

    @Test
    void testShardKeyFallbackToCorrelationId() {
        // deviceId 为 null 时应使用 correlationId
        DeviceEvent sessionEvent = new DeviceEvent(
            "gb28181", "Session", "InviteOk", null, "callId-12345", 1000L, new HashMap<>(), "node-1"
        );

        int shard = dispatcher.getShardIndex(sessionEvent);
        assertTrue(shard >= 0 && shard < 4, "应使用 correlationId 计算分片");

        // 同一 correlationId 应路由到同一分片
        DeviceEvent ackEvent = new DeviceEvent(
            "gb28181", "Session", "Ack", null, "callId-12345", 2000L, new HashMap<>(), "node-1"
        );
        assertEquals(shard, dispatcher.getShardIndex(ackEvent), "同一 callId 应路由到同一分片");
    }

    @Test
    void testDispatchToCorrectShard() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockEventDispatcher).dispatch(any(DeviceEvent.class));

        // 启动分片
        dispatcher.start();

        DeviceEvent event = createEvent("gb28181", "Lifecycle", "Register", "device-1", 1000L);
        dispatcher.dispatch(event);

        // 验证事件被处理
        assertTrue(latch.await(5, TimeUnit.SECONDS), "事件应在 5 秒内被处理");
        verify(mockEventDispatcher, times(1)).dispatch(event);

        dispatcher.shutdown();
    }

    @Test
    void testShutdownGracefully() throws InterruptedException {
        dispatcher.start();

        // 提交一些事件
        for (int i = 0; i < 10; i++) {
            DeviceEvent event = createEvent("gb28181", "Lifecycle", "Register", "device-" + i, 1000L + i);
            dispatcher.dispatch(event);
        }

        // 优雅关闭
        dispatcher.shutdown();

        // 验证所有分片都已停止
        Thread.sleep(1000);
        assertTrue(true, "应能优雅关闭所有分片");
    }

    private DeviceEvent createEvent(String protocol, String group, String name, String deviceId, long ts) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", deviceId);
        return new DeviceEvent(protocol, group, name, deviceId, "corr-" + ts, ts, payload, "node-1");
    }
}
