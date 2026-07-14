package io.github.lunasaw.voglander.service.live;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import io.github.lunasaw.voglander.BaseAsyncTest;
import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * RedisLiveStreamRegistry 集成测试。
 * <p>
 * 依赖真实 Redis；不可用时通过 {@link org.junit.jupiter.api.Assumptions} 自动跳过。
 * 覆盖：并发 incRef 幂等（8 线程 → 8）、decRef 不低于 0、putSession/getSession 往返、completeFuture 唤醒。
 * </p>
 * <p>
 * 继承 {@link BaseAsyncTest}：并发操作无 @Transactional，使用 @AfterEach 手动清理。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class LiveStreamRegistryTest extends BaseAsyncTest {

    private static final String STREAM_ID = "gb_live_test_dev_test_ch";

    @Autowired
    private LiveStreamRegistry  liveStreamRegistry;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    public void checkRedisAndClean() {
        boolean available;
        try {
            connectionFactory.getConnection().ping();
            available = true;
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过测试: {}", e.getMessage());
            available = false;
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(available, "Redis 不可用，跳过");
        liveStreamRegistry.remove(STREAM_ID);
    }

    @AfterEach
    public void cleanup() {
        try {
            liveStreamRegistry.remove(STREAM_ID);
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testIncRef_8ThreadsConcurrent_ResultIs8() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    liveStreamRegistry.incRef(STREAM_ID);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "并发任务未在限定时间内完成");
        pool.shutdown();

        assertEquals(8, liveStreamRegistry.getRef(STREAM_ID), "8 线程并发 incRef 应得 8");
    }

    @Test
    public void testDecRef_NotBelowZero() {
        liveStreamRegistry.incRef(STREAM_ID); // 1
        assertEquals(0, liveStreamRegistry.decRef(STREAM_ID), "1->0");
        // 再次 decRef 不应为负
        assertEquals(0, liveStreamRegistry.decRef(STREAM_ID), "0->0 不低于 0");
        assertEquals(0, liveStreamRegistry.getRef(STREAM_ID));
    }

    @Test
    public void testPutGetSession_RoundTrip() {
        LiveSessionInfo info = new LiveSessionInfo();
        info.setCallId("call-xyz");
        info.setNodeServerId("zlm-1");
        info.setSdpIp("10.0.0.1");
        info.setRtpPort(40000);
        info.setStatus(MediaSessionConstant.Status.ACTIVE);
        info.setSessionType(MediaSessionConstant.Type.PLAY);
        info.setCreateMs(1234567890L);
        info.setPlayUrlsJson("{\"httpFlv\":\"http://x/live.flv\"}");

        liveStreamRegistry.putSession(STREAM_ID, info);

        LiveSessionInfo got = liveStreamRegistry.getSession(STREAM_ID);
        assertNotNull(got);
        assertEquals("call-xyz", got.getCallId());
        assertEquals("zlm-1", got.getNodeServerId());
        assertEquals(40000, got.getRtpPort());
        assertEquals(MediaSessionConstant.Status.ACTIVE, got.getStatus());
        assertEquals("{\"httpFlv\":\"http://x/live.flv\"}", got.getPlayUrlsJson());
    }

    @Test
    public void testCompleteFuture_WakesWaiter() throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();
        liveStreamRegistry.registerFuture(STREAM_ID, future);

        // 异步触发完成
        Executors.newSingleThreadScheduledExecutor().schedule(
            () -> liveStreamRegistry.completeFuture(STREAM_ID), 200, TimeUnit.MILLISECONDS);

        // 应在超时前被唤醒
        assertDoesNotThrow(() -> future.get(3, TimeUnit.SECONDS));
        assertTrue(future.isDone());
    }

    @Test
    public void testRemove_ClearsRefAndSession() {
        liveStreamRegistry.incRef(STREAM_ID);
        LiveSessionInfo info = new LiveSessionInfo();
        info.setCallId("c1");
        liveStreamRegistry.putSession(STREAM_ID, info);

        liveStreamRegistry.remove(STREAM_ID);

        assertEquals(0, liveStreamRegistry.getRef(STREAM_ID));
        assertNull(liveStreamRegistry.getSession(STREAM_ID));
    }
}
