package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.config.CacheTestConfig;
import io.github.lunasaw.voglander.manager.service.MediaSessionService;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 4 / B3：媒体会话跨节点并发 upsert 幂等集成测试（修 M1）。
 * <p>
 * 模拟多节点并发对同一 callId 执行 onInviteOk —— 先查后插在并发下必有一方撞 call_id UNIQUE。
 * 校验：{@code insertOrUpdateOnDuplicate} 捕获 {@code DuplicateKeyException} 转更新兜底，
 * 全程无异常抛出、最终恰好 1 条记录、状态为 ACTIVE。
 * </p>
 * <p>
 * <strong>不使用</strong> {@code @Transactional}：并发线程的写在测试事务外执行，事务无法管理；
 * 故继承 SpringBoot 上下文但手动清理（CLAUDE.md 并发/异步测试规范）。
 * </p>
 *
 * @author luna
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "sip.enable=false")
@Import(CacheTestConfig.class)
public class MediaSessionConcurrentUpsertTest {

    private static final String CALL_ID   = "CONCURRENT_CALL_B3";
    private static final String DEVICE_ID = "CONCURRENT_DEV_B3";

    @Autowired
    private MediaSessionManager mediaSessionManager;

    @Autowired
    private MediaSessionService mediaSessionService;

    @BeforeEach
    public void setUp() {
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        mediaSessionService.lambdaUpdate().eq(MediaSessionDO::getCallId, CALL_ID).remove();
    }

    @Test
    public void testConcurrentOnInviteOkSameCallIdIsIdempotent() throws InterruptedException {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    // 所有线程同时对同一 callId upsert，最大化并发撞 UNIQUE 概率
                    mediaSessionManager.onInviteOk(CALL_ID, DEVICE_ID);
                } catch (Exception e) {
                    errors.incrementAndGet();
                    log.error("并发 onInviteOk 抛异常（不应发生）", e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS), "并发任务应在超时前全部完成");
        pool.shutdownNow();

        // 断言 1：无任何 UNIQUE 异常逃逸（DuplicateKey 已被转更新兜底）
        assertEquals(0, errors.get(), "B3：跨节点并发 upsert 同 callId 不得抛 UNIQUE 异常");

        // 断言 2：最终恰好 1 条记录（幂等，无重复行）
        long count = mediaSessionService.lambdaQuery().eq(MediaSessionDO::getCallId, CALL_ID).count();
        assertEquals(1L, count, "B3：并发 upsert 后应恰好 1 条会话记录");

        // 断言 3：状态为 ACTIVE
        MediaSessionDO one = mediaSessionService.lambdaQuery().eq(MediaSessionDO::getCallId, CALL_ID).one();
        assertEquals(MediaSessionConstant.Status.ACTIVE, one.getStatus(), "最终状态应为 ACTIVE");

        log.info("B3 跨节点并发 upsert 幂等校验通过：{} 线程并发，无异常、1 条记录、ACTIVE", threads);
    }
}
