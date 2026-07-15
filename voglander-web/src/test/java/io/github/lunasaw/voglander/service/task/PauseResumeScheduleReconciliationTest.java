package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCommandDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;

@DisplayName("Pause and resume schedule reconciliation")
class PauseResumeScheduleReconciliationTest extends BaseTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 7, 15, 10, 0);

    @Autowired
    private BizTaskManager taskManager;

    @Autowired
    private BizTaskExecutionManager executionManager;

    @Autowired
    private BizTaskService taskService;

    @Autowired
    private BizTaskExecutionService executionService;

    @Autowired
    private BizTaskAssembler taskAssembler;

    @Test
    @DisplayName("resume 保留 cursor，并逐点将过期计划记为 MISSED 后继续首个可执行点")
    void resume_shouldPreserveCursorAndReconcileEachMissedPoint() {
        BizTaskDTO paused = pausedTask("btask_resume_reconcile_" + System.nanoTime());
        taskService.save(taskAssembler.dtoToDo(paused));
        LocalDateTime resumeTime = START.plusMinutes(3);
        BizTaskCommandDTO command = new BizTaskCommandDTO();
        command.setTaskId(paused.getTaskId());
        command.setExpectedVersion(0);
        command.setActorType("USER");
        command.setActorId("owner-a");
        command.setRequestedAt(resumeTime);

        BizTaskDTO resumed = taskManager.resume(command);

        assertEquals(START, resumed.getNextPlanTime());
        Clock clock = Clock.fixed(Instant.parse("2026-07-15T10:03:00Z"), ZoneOffset.UTC);
        AtomicInteger ids = new AtomicInteger();
        TaskScheduleMaterializer materializer = new TaskScheduleMaterializer(
            new TaskSchedulePlanner(clock, 30), executionManager, taskManager, clock, 2, 10,
            () -> "bexec_resume_" + ids.incrementAndGet() + "_" + paused.getTaskId());
        materializer.materialize(taskManager.getForScheduling(paused.getTaskId()));

        List<BizTaskExecutionDO> executions = executionService.list(
            new LambdaQueryWrapper<BizTaskExecutionDO>().eq(BizTaskExecutionDO::getTaskId, paused.getTaskId())
                .orderByAsc(BizTaskExecutionDO::getPlannedAt));
        assertEquals(Arrays.asList("MISSED", "MISSED", "MISSED", "PENDING"),
            executions.stream().map(BizTaskExecutionDO::getState).collect(Collectors.toList()));
        assertEquals(Arrays.asList(START, START.plusMinutes(1), START.plusMinutes(2), START.plusMinutes(3)),
            executions.stream().map(BizTaskExecutionDO::getPlannedAt).collect(Collectors.toList()));
        BizTaskDO stored = taskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, paused.getTaskId()));
        assertEquals(START.plusMinutes(4), stored.getNextPlanTime());
        assertEquals(3, stored.getMissedCount());
        assertEquals(5, stored.getVersion());
    }

    private BizTaskDTO pausedTask(String taskId) {
        BizTaskDTO task = new BizTaskDTO();
        task.setCreateTime(START.minusMinutes(1));
        task.setUpdateTime(START.minusMinutes(1));
        task.setTaskId(taskId);
        task.setTaskType("RESUME_TEST");
        task.setTaskName("resume reconciliation");
        task.setTaskMode("FIXED_RATE");
        task.setScheduleStartTime(START);
        task.setScheduleEndTime(START.plusMinutes(5));
        task.setIntervalSeconds(60L);
        task.setNextPlanTime(START);
        task.setScheduleVersion(1);
        task.setState("PAUSED");
        task.setPriority(0);
        task.setPlannedCount(6);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setMissedCount(0);
        task.setCancelledCount(0);
        task.setProgressCurrent(0L);
        task.setProgressTotal(0L);
        task.setProgressRevision(0L);
        task.setPayload("{}");
        task.setPayloadVersion(1);
        task.setOwnerType("USER");
        task.setOwnerId("owner-a");
        task.setVersion(0);
        return task;
    }
}
