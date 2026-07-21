package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.assembler.BizTaskExecutionAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;

@DisplayName("BizTaskManager transaction and idempotency")
class BizTaskManagerTest extends BaseTest {

    @Autowired
    private BizTaskManager bizTaskManager;

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskExecutionService bizTaskExecutionService;

    @Autowired
    private BizTaskExecutionAssembler bizTaskExecutionAssembler;

    private final Set<String> taskIds = new HashSet<>();
    private final Set<String> executionIds = new HashSet<>();

    @AfterEach
    void tearDown() {
        if (!executionIds.isEmpty()) {
            bizTaskExecutionService.remove(new LambdaQueryWrapper<BizTaskExecutionDO>()
                .in(BizTaskExecutionDO::getExecutionId, executionIds));
        }
        if (!taskIds.isEmpty()) {
            bizTaskService.remove(new LambdaQueryWrapper<BizTaskDO>().in(BizTaskDO::getTaskId, taskIds));
        }
    }

    @Test
    @DisplayName("创建 task 与 first execution，并按 owner/type/key 返回原受理任务")
    void create_shouldPersistTaskAndFirstExecutionAtomicallyAndIdempotently() {
        String suffix = suffix();
        BizTaskDTO task = task("btask_" + suffix, "idem-" + suffix);
        BizTaskExecutionDTO execution = execution("bexec_" + suffix, task.getTaskId());
        remember(task, execution);

        BizTaskDTO created = bizTaskManager.create(task, execution);
        assertNotNull(created.getId());
        assertEquals(task.getTaskId(), created.getTaskId());
        assertEquals(1L, countExecutions(task.getTaskId()));

        BizTaskDTO replayTask = task("btask_replay_" + suffix, task.getIdempotencyKey());
        BizTaskExecutionDTO replayExecution = execution("bexec_replay_" + suffix, replayTask.getTaskId());
        remember(replayTask, replayExecution);
        BizTaskDTO replay = bizTaskManager.create(replayTask, replayExecution);

        assertEquals(created.getTaskId(), replay.getTaskId());
        assertEquals(created.getId(), replay.getId());
        assertEquals(1L, countExecutions(task.getTaskId()));
        assertEquals(0L, countExecutions(replayTask.getTaskId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("first execution 插入失败时 task 必须回滚")
    void create_shouldRollbackTaskWhenFirstExecutionInsertFails() {
        String suffix = suffix();
        String duplicateExecutionId = "bexec_duplicate_" + suffix;
        BizTaskExecutionDTO seeded = execution(duplicateExecutionId, "btask_seed_" + suffix);
        BizTaskExecutionDO seededDO = bizTaskExecutionAssembler.dtoToDo(seeded);
        bizTaskExecutionService.save(seededDO);
        executionIds.add(duplicateExecutionId);

        BizTaskDTO task = task("btask_rollback_" + suffix, "rollback-" + suffix);
        BizTaskExecutionDTO collision = execution(duplicateExecutionId, task.getTaskId());
        remember(task, collision);

        assertThrows(DataAccessException.class, () -> bizTaskManager.create(task, collision));
        assertEquals(0L, bizTaskService.count(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, task.getTaskId())));
        assertEquals(1L, bizTaskExecutionService.count(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getExecutionId, duplicateExecutionId)));
    }

    private BizTaskDTO task(String taskId, String idempotencyKey) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskDTO task = new BizTaskDTO();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setTaskId(taskId);
        task.setTaskType("TEST_TASK");
        task.setTaskName("test task");
        task.setTaskMode("ONCE");
        task.setScheduleVersion(1);
        task.setState("RUNNING");
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
        task.setOwnerId("owner-manager-test");
        task.setIdempotencyKey(idempotencyKey);
        task.setVersion(0);
        return task;
    }

    private BizTaskExecutionDTO execution(String executionId, String taskId) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setCreateTime(now);
        execution.setUpdateTime(now);
        execution.setExecutionId(executionId);
        execution.setTaskId(taskId);
        execution.setScheduleVersion(1);
        execution.setPlannedAt(now);
        execution.setState("PENDING");
        execution.setAttemptCount(0);
        execution.setMaxAttempts(2);
        execution.setProgressCurrent(0L);
        execution.setProgressTotal(0L);
        execution.setProgressRevision(0L);
        execution.setRetryable(false);
        execution.setVersion(0);
        return execution;
    }

    private long countExecutions(String taskId) {
        return bizTaskExecutionService.count(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getTaskId, taskId));
    }

    private void remember(BizTaskDTO task, BizTaskExecutionDTO execution) {
        taskIds.add(task.getTaskId());
        executionIds.add(execution.getExecutionId());
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
