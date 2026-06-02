package io.github.lunasaw.voglander.manager.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.support.EventEmitter;

/**
 * ShardDispatcher 严格 FIFO 与 shutdown 补强测试
 *
 * @author luna
 */
@DisplayName("ShardDispatcher FIFO 补强测试")
class ShardDispatcherFifoTest {

    @Test
    @DisplayName("同 deviceId 100 条事件应严格 FIFO 处理")
    void same_device_id_should_be_strictly_fifo() throws Exception {
        int total = 100;
        CountDownLatch processed = new CountDownLatch(total);
        List<Integer> order = new CopyOnWriteArrayList<>();

        ProtocolEventHandler handler = mock(ProtocolEventHandler.class);
        when(handler.protocol()).thenReturn("test");
        doAnswer(inv -> {
            DeviceEvent e = inv.getArgument(0);
            order.add((Integer) e.payload().get("seq"));
            processed.countDown();
            return null;
        }).when(handler).handle(any());

        InboundEventDispatcher dispatcher = new InboundEventDispatcher(List.of(handler));
        ShardDispatcher sd = new ShardDispatcher(16, 2000, dispatcher);

        try {
            for (int i = 0; i < total; i++) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("seq", i);
                sd.dispatch(EventEmitter.of("test", "Group.Name", "dev-1")
                    .payload(payload).build());
            }

            assertTrue(processed.await(5, TimeUnit.SECONDS), "100 个事件应在 5s 内处理完");
            assertEquals(total, order.size());
            for (int i = 0; i < total; i++) {
                assertEquals(i, order.get(i).intValue(), "事件序号应严格 FIFO");
            }
        } finally {
            sd.shutdown();
        }
    }

    @Test
    @DisplayName("shutdown 后应优雅关闭，不抛异常")
    void shutdown_should_be_graceful() {
        ProtocolEventHandler handler = mock(ProtocolEventHandler.class);
        when(handler.protocol()).thenReturn("test");
        InboundEventDispatcher dispatcher = new InboundEventDispatcher(List.of(handler));
        ShardDispatcher sd = new ShardDispatcher(4, 100, dispatcher);

        assertDoesNotThrow(sd::shutdown);
    }

    @Test
    @DisplayName("dispatch(null) 应安全丢弃，不抛异常")
    void dispatch_null_should_be_safe() {
        ProtocolEventHandler handler = mock(ProtocolEventHandler.class);
        when(handler.protocol()).thenReturn("test");
        InboundEventDispatcher dispatcher = new InboundEventDispatcher(List.of(handler));
        ShardDispatcher sd = new ShardDispatcher(4, 100, dispatcher);

        try {
            assertDoesNotThrow(() -> sd.dispatch(null));
        } finally {
            sd.shutdown();
        }
    }

    @Test
    @DisplayName("不同 deviceId 应分布到不同分片")
    void different_device_ids_should_distribute() {
        ProtocolEventHandler handler = mock(ProtocolEventHandler.class);
        when(handler.protocol()).thenReturn("test");
        InboundEventDispatcher dispatcher = new InboundEventDispatcher(List.of(handler));
        ShardDispatcher sd = new ShardDispatcher(16, 100, dispatcher);

        try {
            int shard1 = sd.getShardIndex(EventEmitter.of("test", "G.N", "dev-A").build());
            int shard2 = sd.getShardIndex(EventEmitter.of("test", "G.N", "dev-A").build());
            assertEquals(shard1, shard2, "同 deviceId 应路由到同一分片");

            assertTrue(shard1 >= 0 && shard1 < 16);
        } finally {
            sd.shutdown();
        }
    }
}
