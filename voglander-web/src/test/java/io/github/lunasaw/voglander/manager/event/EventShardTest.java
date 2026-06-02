package io.github.lunasaw.voglander.manager.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * EventShard 单元测试（Phase 4）。
 * <p>
 * 验证：
 * <ul>
 *   <li>单线程顺序消费</li>
 *   <li>队列满时丢弃冗余 Keepalive</li>
 *   <li>关键事件（Register/Invite/Offline）不丢弃</li>
 * </ul>
 * </p>
 *
 * @author luna
 */
@Slf4j
class EventShardTest {

    @Mock
    private InboundEventDispatcher mockDispatcher;

    private EventShard shard;
    private BlockingQueue<DeviceEvent> testQueue;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testQueue = new LinkedBlockingQueue<>(10);
        shard = new EventShard(0, testQueue, mockDispatcher);
    }

    @Test
    void testSequentialProcessing() throws InterruptedException {
        // 验证同一设备的事件按顺序处理
        AtomicInteger processCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        doAnswer(invocation -> {
            processCount.incrementAndGet();
            latch.countDown();
            return null;
        }).when(mockDispatcher).dispatch(any(DeviceEvent.class));

        // 启动消费线程
        Thread consumerThread = new Thread(shard::start);
        consumerThread.start();

        // 提交 3 个事件
        DeviceEvent e1 = createEvent("gb28181", "Lifecycle", "Register", "dev1", 1000L);
        DeviceEvent e2 = createEvent("gb28181", "Lifecycle", "Keepalive", "dev1", 2000L);
        DeviceEvent e3 = createEvent("gb28181", "Lifecycle", "Offline", "dev1", 3000L);

        shard.offer(e1);
        shard.offer(e2);
        shard.offer(e3);

        // 等待处理完成
        assertTrue(latch.await(5, TimeUnit.SECONDS), "事件应在 5 秒内处理完成");
        assertEquals(3, processCount.get(), "应处理 3 个事件");

        shard.shutdown();
        consumerThread.join(1000);
    }

    @Test
    void testKeepaliveDiscardWhenFull() {
        // 填满队列
        for (int i = 0; i < 10; i++) {
            DeviceEvent e = createEvent("gb28181", "Lifecycle", "Register", "dev" + i, 1000L + i);
            assertTrue(testQueue.offer(e), "队列应能容纳 10 个事件");
        }

        // 尝试添加第 11 个 Keepalive（应丢弃）
        DeviceEvent keepalive = createEvent("gb28181", "Notify", "Keepalive", "dev100", 2000L);
        boolean offered = shard.offer(keepalive);
        assertFalse(offered, "队列满时 Keepalive 应被丢弃");
        assertEquals(10, testQueue.size(), "队列大小应保持 10");
    }

    @Test
    void testCriticalEventNotDiscarded() {
        // 填满队列
        for (int i = 0; i < 10; i++) {
            DeviceEvent e = createEvent("gb28181", "Lifecycle", "Register", "dev" + i, 1000L + i);
            testQueue.offer(e);
        }

        // 尝试添加关键事件（Register/Invite/Offline）应阻塞等待
        DeviceEvent register = createEvent("gb28181", "Lifecycle", "Register", "dev100", 2000L);

        // 在另一线程中尝试 offer（会阻塞）
        Thread offerThread = new Thread(() -> {
            boolean result = shard.offer(register);
            assertTrue(result, "关键事件应最终入队");
        });
        offerThread.start();

        // 等待一小段时间，确认线程被阻塞
        try {
            Thread.sleep(100);
            assertTrue(offerThread.isAlive(), "offer 线程应被阻塞");

            // 消费一个事件腾出空间
            testQueue.poll();
            Thread.sleep(100);

            // 关键事件应能入队
            offerThread.join(1000);
            assertFalse(offerThread.isAlive(), "offer 线程应完成");
        } catch (InterruptedException e) {
            fail("测试被中断");
        }
    }

    @Test
    void testShardIdAssignment() {
        assertEquals(0, shard.getShardId(), "分片 ID 应为 0");
    }

    private DeviceEvent createEvent(String protocol, String group, String name, String deviceId, long ts) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", deviceId);
        return new DeviceEvent(protocol, group, name, deviceId, "corr-" + ts, ts, payload, "node-1");
    }
}
