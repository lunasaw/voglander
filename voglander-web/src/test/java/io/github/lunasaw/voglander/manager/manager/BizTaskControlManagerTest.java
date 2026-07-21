package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskStateEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCommandDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskService;

@DisplayName("BizTaskManager task-control state matrix")
class BizTaskControlManagerTest extends BaseTest {

    @Autowired
    private BizTaskManager bizTaskManager;

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskAssembler bizTaskAssembler;

    @ParameterizedTest(name = "pause from {0}")
    @EnumSource(TaskStateEnum.class)
    @DisplayName("pause 应覆盖完整任务状态矩阵")
    void pause_shouldFollowFullStateMatrix(TaskStateEnum source) {
        BizTaskDTO task = save(task("btask_pause_" + source + "_" + suffix(), TaskModeEnum.FIXED_RATE, source));
        BizTaskCommandDTO command = command(task);

        if (source == TaskStateEnum.SCHEDULED || source == TaskStateEnum.RUNNING) {
            BizTaskDTO result = bizTaskManager.pause(command);
            assertEquals(TaskStateEnum.PAUSED.name(), result.getState());
            assertEquals(1, result.getVersion());
            assertEquals(task.getNextPlanTime(), result.getNextPlanTime());
        } else if (source == TaskStateEnum.PAUSED) {
            BizTaskDTO result = bizTaskManager.pause(command);
            assertEquals(TaskStateEnum.PAUSED.name(), result.getState());
            assertEquals(0, result.getVersion());
            assertEquals(task.getNextPlanTime(), result.getNextPlanTime());
        } else {
            assertThrows(ServiceException.class, () -> bizTaskManager.pause(command));
            assertEquals(source.name(), reload(task.getTaskId()).getState());
        }
    }

    @ParameterizedTest(name = "resume from {0}")
    @EnumSource(TaskStateEnum.class)
    @DisplayName("resume 应覆盖完整任务状态矩阵")
    void resume_shouldFollowFullStateMatrix(TaskStateEnum source) {
        BizTaskDTO task = save(task("btask_resume_" + source + "_" + suffix(), TaskModeEnum.FIXED_RATE, source));
        BizTaskCommandDTO command = command(task);

        if (source == TaskStateEnum.PAUSED) {
            BizTaskDTO result = bizTaskManager.resume(command);
            assertEquals(TaskStateEnum.SCHEDULED.name(), result.getState());
            assertEquals(1, result.getVersion());
            assertEquals(task.getNextPlanTime(), result.getNextPlanTime());
        } else if (source == TaskStateEnum.SCHEDULED || source == TaskStateEnum.RUNNING) {
            BizTaskDTO result = bizTaskManager.resume(command);
            assertEquals(source.name(), result.getState());
            assertEquals(0, result.getVersion());
        } else {
            assertThrows(ServiceException.class, () -> bizTaskManager.resume(command));
            assertEquals(source.name(), reload(task.getTaskId()).getState());
        }
    }

    @ParameterizedTest(name = "cancel from {0}")
    @EnumSource(TaskStateEnum.class)
    @DisplayName("cancel 应覆盖完整任务状态矩阵并保持已取消幂等")
    void cancel_shouldFollowFullStateMatrix(TaskStateEnum source) {
        BizTaskDTO task = save(task("btask_cancel_" + source + "_" + suffix(), TaskModeEnum.FIXED_RATE, source));
        BizTaskCommandDTO command = command(task);

        if (source == TaskStateEnum.SCHEDULED || source == TaskStateEnum.RUNNING
            || source == TaskStateEnum.PAUSED) {
            BizTaskDTO result = bizTaskManager.cancel(command);
            assertEquals(TaskStateEnum.CANCELLING.name(), result.getState());
            assertEquals(1, result.getVersion());
        } else if (source == TaskStateEnum.CANCELLING || source == TaskStateEnum.CANCELLED) {
            BizTaskDTO result = bizTaskManager.cancel(command);
            assertEquals(source.name(), result.getState());
            assertEquals(0, result.getVersion());
        } else {
            assertThrows(ServiceException.class, () -> bizTaskManager.cancel(command));
            assertEquals(source.name(), reload(task.getTaskId()).getState());
        }
    }

    @Test
    @DisplayName("ONCE 任务恢复后应回 RUNNING")
    void resumeOnce_shouldReturnToRunning() {
        BizTaskDTO task = save(task("btask_resume_once_" + suffix(), TaskModeEnum.ONCE, TaskStateEnum.PAUSED));

        BizTaskDTO result = bizTaskManager.resume(command(task));

        assertEquals(TaskStateEnum.RUNNING.name(), result.getState());
    }

    @Test
    @DisplayName("FIXED_RATE 改期应仅在 PAUSED 下更新计划、游标、版本和剩余总数")
    void rescheduleFixedRate_shouldUpdateScheduleAtomically() {
        BizTaskDTO task = task("btask_reschedule_" + suffix(), TaskModeEnum.FIXED_RATE, TaskStateEnum.PAUSED);
        task.setSuccessCount(1);
        task.setFailedCount(1);
        task.setPlannedCount(8);
        save(task);
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 8, 0);
        BizTaskCommandDTO command = command(task);
        command.setScheduleStartTime(start);
        command.setScheduleEndTime(start.plusSeconds(20));
        command.setIntervalSeconds(10L);

        BizTaskDTO result = bizTaskManager.reschedule(command);

        assertEquals(TaskStateEnum.PAUSED.name(), result.getState());
        assertEquals(start, result.getScheduleStartTime());
        assertEquals(start.plusSeconds(20), result.getScheduleEndTime());
        assertEquals(10L, result.getIntervalSeconds());
        assertEquals(start, result.getNextPlanTime());
        assertEquals(2, result.getScheduleVersion());
        assertEquals(5, result.getPlannedCount());
        assertEquals(1, result.getVersion());
    }

    @Test
    @DisplayName("AT_TIME 改期应只保存一个新计划点")
    void rescheduleAtTime_shouldClearIntervalAndEndTime() {
        BizTaskDTO task = save(task("btask_reschedule_at_" + suffix(), TaskModeEnum.AT_TIME,
            TaskStateEnum.PAUSED));
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 9, 0);
        BizTaskCommandDTO command = command(task);
        command.setScheduleStartTime(start);

        BizTaskDTO result = bizTaskManager.reschedule(command);

        assertEquals(start, result.getScheduleStartTime());
        assertEquals(start, result.getNextPlanTime());
        assertNull(result.getScheduleEndTime());
        assertNull(result.getIntervalSeconds());
        assertEquals(1, result.getPlannedCount());
    }

    @Test
    @DisplayName("改期应拒绝非 PAUSED、ONCE 和非法范围")
    void reschedule_shouldRejectInvalidStateModeAndRange() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 10, 0);
        BizTaskDTO running = save(task("btask_reschedule_running_" + suffix(), TaskModeEnum.FIXED_RATE,
            TaskStateEnum.RUNNING));
        BizTaskCommandDTO runningCommand = command(running);
        runningCommand.setScheduleStartTime(start);
        runningCommand.setScheduleEndTime(start.plusMinutes(1));
        runningCommand.setIntervalSeconds(10L);
        assertThrows(ServiceException.class, () -> bizTaskManager.reschedule(runningCommand));

        BizTaskDTO once = save(task("btask_reschedule_once_" + suffix(), TaskModeEnum.ONCE,
            TaskStateEnum.PAUSED));
        BizTaskCommandDTO onceCommand = command(once);
        onceCommand.setScheduleStartTime(start);
        assertThrows(ServiceException.class, () -> bizTaskManager.reschedule(onceCommand));

        BizTaskDTO fixed = save(task("btask_reschedule_invalid_" + suffix(), TaskModeEnum.FIXED_RATE,
            TaskStateEnum.PAUSED));
        BizTaskCommandDTO invalid = command(fixed);
        invalid.setScheduleStartTime(start);
        invalid.setScheduleEndTime(start.minusSeconds(1));
        invalid.setIntervalSeconds(0L);
        assertThrows(IllegalArgumentException.class, () -> bizTaskManager.reschedule(invalid));
    }

    @Test
    @DisplayName("控制命令应通过 expectedVersion 拒绝跨状态的陈旧写入")
    void controls_shouldRejectStaleOptimisticVersion() {
        BizTaskDTO task = save(task("btask_control_cas_" + suffix(), TaskModeEnum.FIXED_RATE,
            TaskStateEnum.RUNNING));
        BizTaskCommandDTO pause = command(task);
        assertEquals(TaskStateEnum.PAUSED.name(), bizTaskManager.pause(pause).getState());

        BizTaskCommandDTO staleCancel = command(task);
        assertThrows(ServiceException.class, () -> bizTaskManager.cancel(staleCancel));
        assertEquals(TaskStateEnum.PAUSED.name(), reload(task.getTaskId()).getState());
    }

    private BizTaskCommandDTO command(BizTaskDTO task) {
        BizTaskCommandDTO command = new BizTaskCommandDTO();
        command.setTaskId(task.getTaskId());
        command.setExpectedVersion(task.getVersion());
        command.setActorType("USER");
        command.setActorId("OWNER_A");
        command.setRequestedAt(LocalDateTime.now().withNano(0));
        return command;
    }

    private BizTaskDTO task(String taskId, TaskModeEnum mode, TaskStateEnum state) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskDTO task = new BizTaskDTO();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setTaskId(taskId);
        task.setTaskType("CONTROL_TEST");
        task.setTaskName("control test");
        task.setTaskMode(mode.name());
        task.setScheduleStartTime(mode == TaskModeEnum.ONCE ? null : now.plusMinutes(1));
        task.setScheduleEndTime(mode == TaskModeEnum.FIXED_RATE ? now.plusMinutes(2) : null);
        task.setIntervalSeconds(mode == TaskModeEnum.FIXED_RATE ? 30L : null);
        task.setNextPlanTime(mode == TaskModeEnum.ONCE ? null : now.plusMinutes(1));
        task.setScheduleVersion(1);
        task.setState(state.name());
        task.setPriority(0);
        task.setPlannedCount(1);
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
        task.setOwnerId("OWNER_A");
        task.setVersion(0);
        return task;
    }

    private BizTaskDTO save(BizTaskDTO task) {
        bizTaskService.save(bizTaskAssembler.dtoToDo(task));
        return task;
    }

    private BizTaskDTO reload(String taskId) {
        return bizTaskAssembler.doToDto(bizTaskService.lambdaQuery().eq(io.github.lunasaw.voglander.repository.entity.BizTaskDO::getTaskId,
            taskId).one());
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
