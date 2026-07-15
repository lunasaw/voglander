package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("Durable business-task dispatcher")
class BizTaskDispatcherTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 11, 0);

    @Mock
    private BizTaskExecutionManager executionManager;

    @Mock
    private BusinessTaskExecutionWorker worker;

    private List<Runnable> submissions;
    private BizTaskDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-15T11:00:00Z"), ZoneOffset.UTC);
        submissions = new ArrayList<Runnable>();
        Executor recordingExecutor = submissions::add;
        dispatcher = new BizTaskDispatcher(executionManager, recordingExecutor, worker, clock, 2);
    }

    @Test
    @DisplayName("扫描 durable runnable facts 后应只把稳定 executionId 提交给 Worker")
    void dispatch_shouldScanDurableFactsAndSubmitExecutionIdsOnly() {
        BizTaskExecutionDTO first = execution("bexec_first");
        BizTaskExecutionDTO second = execution("bexec_second");
        when(executionManager.findRunnable(NOW, 2)).thenReturn(Arrays.asList(first, second));

        dispatcher.dispatchRunnableExecutions();

        verify(executionManager).findRunnable(NOW, 2);
        assertEquals(2, submissions.size());
        first.setExecutionId("mutated_after_submission");
        submissions.forEach(Runnable::run);
        InOrder order = inOrder(worker);
        order.verify(worker).execute("bexec_first");
        order.verify(worker).execute("bexec_second");
    }

    @Test
    @DisplayName("执行器饱和时 PENDING/RETRY_WAIT 应保持 durable 且不得 CallerRuns")
    void dispatch_shouldLeaveRunnableFactsUnchangedWhenExecutorIsSaturated() throws Exception {
        BusinessTaskProperties properties = new BusinessTaskProperties();
        properties.setExecutorCoreSize(1);
        properties.setExecutorMaxSize(1);
        properties.setExecutorQueueCapacity(1);
        ThreadPoolTaskExecutor saturatedExecutor =
            new BusinessTaskExecutorConfiguration().businessTaskExecutor(properties);
        saturatedExecutor.initialize();
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            saturatedExecutor.execute(() -> {
                running.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(running.await(5, TimeUnit.SECONDS));
            saturatedExecutor.execute(() -> { });
            BizTaskExecutionDTO pending = execution("bexec_pending");
            BizTaskExecutionDTO retryWait = execution("bexec_retry_wait");
            retryWait.setState("RETRY_WAIT");
            when(executionManager.findRunnable(NOW, 2)).thenReturn(Arrays.asList(pending, retryWait));
            BizTaskDispatcher saturated = new BizTaskDispatcher(executionManager, saturatedExecutor, worker,
                Clock.fixed(Instant.parse("2026-07-15T11:00:00Z"), ZoneOffset.UTC), 2);

            assertDoesNotThrow(saturated::dispatchRunnableExecutions);

            assertEquals("PENDING", pending.getState());
            assertEquals("RETRY_WAIT", retryWait.getState());
            verifyNoInteractions(worker);
        } finally {
            release.countDown();
            saturatedExecutor.shutdown();
        }
    }

    private BizTaskExecutionDTO execution(String executionId) {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId(executionId);
        execution.setTaskId("btask_dispatch");
        execution.setState("PENDING");
        return execution;
    }
}
