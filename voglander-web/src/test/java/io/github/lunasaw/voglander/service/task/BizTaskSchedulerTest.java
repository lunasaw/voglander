package io.github.lunasaw.voglander.service.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("Durable business-task due scanner")
class BizTaskSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 10, 0);

    @Mock
    private BizTaskManager bizTaskManager;

    @Mock
    private RedisLockUtil redisLockUtil;

    @Mock
    private TransactionOperations transactionOperations;

    @Mock
    private DueTaskMaterializer dueTaskMaterializer;

    private BizTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BizTaskScheduler(bizTaskManager, redisLockUtil, transactionOperations,
            dueTaskMaterializer, Clock.fixed(Instant.parse("2026-07-15T10:00:00Z"), ZoneOffset.UTC), 2, 5);
        org.mockito.Mockito.doAnswer(invocation -> {
            java.util.function.Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionOperations).executeWithoutResult(any());
    }

    @Test
    @DisplayName("扫描结果必须逐 task 加短锁并在事务内重读后物化")
    void scanDueTasks_shouldLockAndRereadInsideTransaction() {
        BizTaskDTO scannedA = task("btask_a", "SCHEDULED", NOW.minusSeconds(1));
        BizTaskDTO scannedB = task("btask_b", "RUNNING", NOW);
        BizTaskDTO reloadedA = task("btask_a", "RUNNING", NOW.minusSeconds(1));
        when(bizTaskManager.findDueTasks(NOW, 2)).thenReturn(Arrays.asList(scannedA, scannedB));
        when(redisLockUtil.generateLockValue()).thenReturn("lock-a", "lock-b");
        when(redisLockUtil.lock(TaskConstant.SCHEDULE_LOCK_PREFIX + "btask_a", "lock-a", 5)).thenReturn(true);
        when(redisLockUtil.lock(TaskConstant.SCHEDULE_LOCK_PREFIX + "btask_b", "lock-b", 5)).thenReturn(false);
        when(bizTaskManager.getForScheduling("btask_a")).thenReturn(reloadedA);

        scheduler.scanDueTasks();

        InOrder order = inOrder(redisLockUtil, transactionOperations, bizTaskManager, dueTaskMaterializer);
        order.verify(redisLockUtil).lock(TaskConstant.SCHEDULE_LOCK_PREFIX + "btask_a", "lock-a", 5);
        order.verify(transactionOperations).executeWithoutResult(any());
        order.verify(bizTaskManager).getForScheduling("btask_a");
        order.verify(dueTaskMaterializer).materialize(reloadedA);
        order.verify(redisLockUtil).unLock(TaskConstant.SCHEDULE_LOCK_PREFIX + "btask_a", "lock-a");
        verify(bizTaskManager, never()).getForScheduling("btask_b");
        verify(redisLockUtil, never()).unLock(TaskConstant.SCHEDULE_LOCK_PREFIX + "btask_b", "lock-b");
    }

    @Test
    @DisplayName("事务重读发现任务已暂停时不得物化过期扫描快照")
    void scanDueTasks_shouldSkipTaskThatIsNoLongerDueAfterReread() {
        BizTaskDTO scanned = task("btask_stale", "SCHEDULED", NOW.minusSeconds(1));
        BizTaskDTO paused = task("btask_stale", "PAUSED", NOW.minusSeconds(1));
        when(bizTaskManager.findDueTasks(NOW, 2)).thenReturn(Arrays.asList(scanned));
        when(redisLockUtil.generateLockValue()).thenReturn("lock-stale");
        when(redisLockUtil.lock(TaskConstant.SCHEDULE_LOCK_PREFIX + "btask_stale", "lock-stale", 5))
            .thenReturn(true);
        when(bizTaskManager.getForScheduling("btask_stale")).thenReturn(paused);

        scheduler.scanDueTasks();

        verify(dueTaskMaterializer, never()).materialize(any());
        verify(redisLockUtil).unLock(TaskConstant.SCHEDULE_LOCK_PREFIX + "btask_stale", "lock-stale");
    }

    private BizTaskDTO task(String taskId, String state, LocalDateTime nextPlanTime) {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId(taskId);
        task.setState(state);
        task.setNextPlanTime(nextPlanTime);
        return task;
    }
}
