package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("Durable schedule point materializer")
class TaskScheduleMaterializerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 10, 0);

    @Mock
    private BizTaskExecutionManager executionManager;

    @Mock
    private BizTaskManager taskManager;

    private TaskScheduleMaterializer materializer;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-15T10:00:00Z"), ZoneOffset.UTC);
        materializer = new TaskScheduleMaterializer(new TaskSchedulePlanner(clock, 30), executionManager,
            taskManager, clock, 2, () -> "bexec_fixed");
    }

    @Test
    @DisplayName("先 insert-if-absent execution，再按 task version CAS 推进固定 cursor")
    void materialize_shouldInsertExecutionBeforeAdvancingCursor() {
        BizTaskDTO task = fixedTask();
        when(executionManager.insertIfAbsent(any())).thenReturn(true);
        when(taskManager.advanceScheduleCursor("btask_fixed", 7, NOW.plusMinutes(1), "RUNNING", NOW))
            .thenReturn(true);

        materializer.materialize(task);

        ArgumentCaptor<BizTaskExecutionDTO> executionCaptor = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        InOrder order = inOrder(executionManager, taskManager);
        order.verify(executionManager).insertIfAbsent(executionCaptor.capture());
        order.verify(taskManager).advanceScheduleCursor("btask_fixed", 7, NOW.plusMinutes(1), "RUNNING", NOW);
        BizTaskExecutionDTO execution = executionCaptor.getValue();
        assertEquals("bexec_fixed", execution.getExecutionId());
        assertEquals("btask_fixed", execution.getTaskId());
        assertEquals(3, execution.getScheduleVersion());
        assertEquals(NOW, execution.getPlannedAt());
        assertEquals(NOW.plusSeconds(30), execution.getDeadlineAt());
        assertEquals("PENDING", execution.getState());
        assertEquals(0, execution.getAttemptCount());
        assertEquals(2, execution.getMaxAttempts());
        assertEquals(0, execution.getVersion());
        verify(executionManager, never()).markMissed(any());
    }

    @Test
    @DisplayName("execution 已存在时仍推进相同 cursor，以修复幂等重放")
    void materialize_shouldAdvanceCursorWhenExecutionAlreadyExists() {
        BizTaskDTO task = fixedTask();
        when(executionManager.insertIfAbsent(any())).thenReturn(false);
        when(taskManager.advanceScheduleCursor("btask_fixed", 7, NOW.plusMinutes(1), "RUNNING", NOW))
            .thenReturn(true);

        materializer.materialize(task);

        verify(taskManager).advanceScheduleCursor("btask_fixed", 7, NOW.plusMinutes(1), "RUNNING", NOW);
    }

    @Test
    @DisplayName("cursor CAS 失败必须抛冲突，让外层事务回滚 execution 插入")
    void materialize_shouldFailTransactionWhenCursorCasLoses() {
        BizTaskDTO task = fixedTask();
        when(executionManager.insertIfAbsent(any())).thenReturn(true);
        when(taskManager.advanceScheduleCursor("btask_fixed", 7, NOW.plusMinutes(1), "RUNNING", NOW))
            .thenReturn(false);

        ServiceException error = assertThrows(ServiceException.class, () -> materializer.materialize(task));

        assertEquals(ServiceExceptionEnum.TASK_STATE_CONFLICT.getCode(), error.getCode());
        assertTrue(error.getDetailMessage().contains("cursor"));
    }

    @Test
    @DisplayName("catch-up 每批有界地落 MISSED 并停在下一批理论 cursor")
    void materialize_shouldPersistExpiredPointsAsMissedWithinCatchupBatch() {
        Clock lateClock = Clock.fixed(Instant.parse("2026-07-15T10:05:00Z"), ZoneOffset.UTC);
        AtomicInteger sequence = new AtomicInteger();
        TaskScheduleMaterializer catchup = new TaskScheduleMaterializer(new TaskSchedulePlanner(lateClock, 30),
            executionManager, taskManager, lateClock, 2, 3,
            () -> "bexec_missed_" + sequence.incrementAndGet());
        BizTaskDTO task = fixedTask();
        task.setScheduleEndTime(NOW.plusMinutes(10));
        when(executionManager.insertIfAbsent(any())).thenReturn(true);
        when(executionManager.markMissed(any())).thenReturn(true);
        when(taskManager.advanceScheduleCursor("btask_fixed", 7, NOW.plusMinutes(1), "RUNNING",
            NOW.plusMinutes(5), true)).thenReturn(true);
        when(taskManager.advanceScheduleCursor("btask_fixed", 8, NOW.plusMinutes(2), "RUNNING",
            NOW.plusMinutes(5), true)).thenReturn(true);
        when(taskManager.advanceScheduleCursor("btask_fixed", 9, NOW.plusMinutes(3), "RUNNING",
            NOW.plusMinutes(5), true)).thenReturn(true);

        catchup.materialize(task);

        ArgumentCaptor<BizTaskExecutionDTO> inserted = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        ArgumentCaptor<BizTaskExecutionDTO> missed = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(executionManager, times(3)).insertIfAbsent(inserted.capture());
        verify(executionManager, times(3)).markMissed(missed.capture());
        List<BizTaskExecutionDTO> executions = inserted.getAllValues();
        assertEquals(NOW, executions.get(0).getPlannedAt());
        assertEquals(NOW.plusMinutes(1), executions.get(1).getPlannedAt());
        assertEquals(NOW.plusMinutes(2), executions.get(2).getPlannedAt());
        assertEquals("ALLOWED_DELAY_EXCEEDED", missed.getAllValues().get(0).getFailureCode());
        assertEquals(NOW.plusMinutes(3), task.getNextPlanTime());
        assertEquals(10, task.getVersion());
    }

    @Test
    @DisplayName("最后一批全部 MISSED 时应在 cursor 耗尽后自然终结为 FAILED")
    void materialize_shouldNaturallyFailAllMissedSchedule() {
        Clock lateClock = Clock.fixed(Instant.parse("2026-07-15T10:05:00Z"), ZoneOffset.UTC);
        AtomicInteger sequence = new AtomicInteger();
        TaskScheduleMaterializer catchup = new TaskScheduleMaterializer(new TaskSchedulePlanner(lateClock, 30),
            executionManager, taskManager, lateClock, 2, 10,
            () -> "bexec_terminal_" + sequence.incrementAndGet());
        BizTaskDTO task = fixedTask();
        task.setScheduleEndTime(NOW.plusMinutes(1));
        task.setPlannedCount(2);
        task.setMissedCount(0);
        when(executionManager.insertIfAbsent(any())).thenReturn(true);
        when(executionManager.markMissed(any())).thenReturn(true);
        when(taskManager.advanceScheduleCursor("btask_fixed", 7, NOW.plusMinutes(1), "RUNNING",
            NOW.plusMinutes(5), true)).thenReturn(true);
        when(taskManager.advanceScheduleCursor("btask_fixed", 8, null, "RUNNING", NOW.plusMinutes(5), true))
            .thenReturn(true);
        when(taskManager.markNaturalTerminal("btask_fixed", 9, "FAILED", NOW.plusMinutes(5)))
            .thenReturn(true);

        catchup.materialize(task);

        verify(taskManager).markNaturalTerminal("btask_fixed", 9, "FAILED", NOW.plusMinutes(5));
        assertEquals("FAILED", task.getState());
        assertEquals(10, task.getVersion());
    }

    private BizTaskDTO fixedTask() {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_fixed");
        task.setTaskMode("FIXED_RATE");
        task.setState("SCHEDULED");
        task.setNextPlanTime(NOW);
        task.setScheduleEndTime(NOW.plusMinutes(2));
        task.setIntervalSeconds(60L);
        task.setScheduleVersion(3);
        task.setVersion(7);
        return task;
    }
}
