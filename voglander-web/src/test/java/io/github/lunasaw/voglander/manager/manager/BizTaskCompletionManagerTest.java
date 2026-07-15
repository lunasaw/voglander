package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.domain.task.TaskResultReference;
import io.github.lunasaw.voglander.client.service.task.TaskCompletionParticipant;
import io.github.lunasaw.voglander.common.enums.task.TaskEventTypeEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskExecutionStateEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskStateEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;
import io.github.lunasaw.voglander.manager.assembler.BizTaskExecutionAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;
import io.github.lunasaw.voglander.repository.mapper.BizTaskEventMapper;
import io.github.lunasaw.voglander.repository.mapper.BizTaskMapper;

@DisplayName("BizTaskCompletionManager atomic completion")
class BizTaskCompletionManagerTest extends BaseTest {

    @Autowired
    private BizTaskCompletionManager bizTaskCompletionManager;

    @Autowired
    private BizTaskEventManager bizTaskEventManager;

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskExecutionService bizTaskExecutionService;

    @Autowired
    private BizTaskAssembler bizTaskAssembler;

    @Autowired
    private BizTaskExecutionAssembler bizTaskExecutionAssembler;

    @Autowired
    private BizTaskEventMapper bizTaskEventMapper;

    @Autowired
    private BizTaskMapper bizTaskMapper;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("成功完成应原子提交 participant、执行终态、任务汇总和事件")
    void completeSuccess_shouldCommitAllFactsAtomically() {
        String suffix = suffix();
        LocalDateTime finishedAt = LocalDateTime.now().withNano(0);
        BizTaskDTO task = saveTask("btask_completion_" + suffix);
        BizTaskExecutionDTO execution = saveRunningExecution(task.getTaskId(), "bexec_completion_" + suffix);
        BizTaskEventDTO domainEvent = event(task.getTaskId(), execution.getExecutionId(),
            "bevt_domain_" + suffix, TaskEventTypeEnum.CREATED);
        BizTaskEventDTO successEvent = event(task.getTaskId(), execution.getExecutionId(),
            "bevt_success_" + suffix, TaskEventTypeEnum.SUCCEEDED);
        AtomicBoolean participantCalled = new AtomicBoolean();
        TaskCompletionParticipant participant = (context, completionData) -> {
            participantCalled.set(true);
            assertEquals(task.getTaskId(), context.taskId());
            assertEquals(execution.getExecutionId(), context.executionId());
            assertEquals("domain-result", completionData.getString("domainId"));
            assertTrue(bizTaskEventManager.append(domainEvent));
        };
        BizTaskExecutionDTO command = completionCommand(execution, finishedAt);

        BizTaskDTO result = bizTaskCompletionManager.completeSuccess(command, result(), participant, successEvent);

        assertTrue(participantCalled.get());
        assertEquals(TaskStateEnum.COMPLETED.name(), result.getState());
        assertEquals(1, result.getSuccessCount());
        assertEquals(10L, result.getProgressCurrent());
        assertEquals(10L, result.getProgressTotal());
        assertEquals(execution.getExecutionId(), result.getLastExecutionId());
        assertEquals(finishedAt, result.getLastExecuteTime());
        assertEquals(finishedAt, result.getCompletedTime());
        assertEquals("TEST_RESULT", result.getResultRefType());
        assertEquals("result-1", result.getResultRefId());
        assertEquals(1, result.getVersion());

        BizTaskExecutionDTO completed = reloadExecution(execution.getExecutionId());
        assertEquals(TaskExecutionStateEnum.SUCCEEDED.name(), completed.getState());
        assertEquals(10L, completed.getProgressCurrent());
        assertEquals(10L, completed.getProgressTotal());
        assertEquals(finishedAt, completed.getFinishedAt());
        assertEquals("TEST_RESULT", completed.getResultRefType());
        assertEquals("result-1", completed.getResultRefId());
        assertNull(completed.getClaimToken());
        assertEquals(1, completed.getVersion());

        assertEquals(2, bizTaskEventMapper.selectTimeline(task.getTaskId(), execution.getExecutionId(), 10).size());
        assertEquals(1, countEvent(successEvent.getEventId()));
        assertEquals(1, countEvent(domainEvent.getEventId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("完成事件冲突应回滚 participant、执行和任务更新")
    void completeSuccess_shouldRollbackEveryFactWhenFinalEventConflicts() {
        String suffix = suffix();
        LocalDateTime finishedAt = LocalDateTime.now().withNano(0);
        BizTaskDTO task = saveTask("btask_completion_rollback_" + suffix);
        BizTaskExecutionDTO execution = saveRunningExecution(task.getTaskId(),
            "bexec_completion_rollback_" + suffix);
        BizTaskEventDTO duplicateSuccessEvent = event(task.getTaskId(), execution.getExecutionId(),
            "bevt_duplicate_success_" + suffix, TaskEventTypeEnum.SUCCEEDED);
        assertTrue(bizTaskEventManager.append(duplicateSuccessEvent));
        BizTaskEventDTO participantEvent = event(task.getTaskId(), execution.getExecutionId(),
            "bevt_rolled_back_domain_" + suffix, TaskEventTypeEnum.CREATED);
        TaskCompletionParticipant participant = (context, completionData) -> {
            assertTrue(bizTaskEventManager.append(participantEvent));
        };

        assertThrows(ServiceException.class, () -> bizTaskCompletionManager.completeSuccess(
            completionCommand(execution, finishedAt), result(), participant, duplicateSuccessEvent));

        BizTaskDTO unchangedTask = reloadTask(task.getTaskId());
        assertEquals(TaskStateEnum.RUNNING.name(), unchangedTask.getState());
        assertEquals(0, unchangedTask.getSuccessCount());
        assertEquals(0, unchangedTask.getVersion());
        assertNull(unchangedTask.getLastExecutionId());

        BizTaskExecutionDTO unchangedExecution = reloadExecution(execution.getExecutionId());
        assertEquals(TaskExecutionStateEnum.RUNNING.name(), unchangedExecution.getState());
        assertEquals("claim-token", unchangedExecution.getClaimToken());
        assertEquals(0, unchangedExecution.getVersion());
        assertEquals(0, countEvent(participantEvent.getEventId()));
        assertEquals(1, countEvent(duplicateSuccessEvent.getEventId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("重复完成回调只能终结一次且计数不能重复增加")
    void completeSuccess_duplicateCallbackShouldIncrementExactlyOnce() {
        String suffix = suffix();
        LocalDateTime finishedAt = LocalDateTime.now().withNano(0);
        BizTaskDTO task = saveTask("btask_completion_duplicate_" + suffix);
        BizTaskExecutionDTO execution = saveRunningExecution(task.getTaskId(),
            "bexec_completion_duplicate_" + suffix);
        BizTaskEventDTO successEvent = event(task.getTaskId(), execution.getExecutionId(),
            "bevt_completion_duplicate_" + suffix, TaskEventTypeEnum.SUCCEEDED);
        BizTaskExecutionDTO command = completionCommand(execution, finishedAt);
        AtomicInteger participantCalls = new AtomicInteger();
        TaskCompletionParticipant participant = (context, completionData) -> participantCalls.incrementAndGet();

        bizTaskCompletionManager.completeSuccess(command, result(), participant, successEvent);
        assertThrows(ServiceException.class,
            () -> bizTaskCompletionManager.completeSuccess(command, result(), participant, successEvent));

        assertEquals(1, participantCalls.get());
        assertEquals(1, reloadTask(task.getTaskId()).getSuccessCount());
        assertEquals(1, countEvent(successEvent.getEventId()));
        assertEquals(TaskExecutionStateEnum.SUCCEEDED.name(),
            reloadExecution(execution.getExecutionId()).getState());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("任务乐观版本冲突应回滚已经写入的执行终态")
    void completeSuccess_taskOptimisticConflictShouldRollbackExecutionAndCounter() {
        String suffix = suffix();
        LocalDateTime finishedAt = LocalDateTime.now().withNano(0);
        BizTaskDTO task = saveTask("btask_completion_cas_" + suffix);
        BizTaskExecutionDTO execution = saveRunningExecution(task.getTaskId(),
            "bexec_completion_cas_" + suffix);
        BizTaskEventDTO successEvent = event(task.getTaskId(), execution.getExecutionId(),
            "bevt_completion_cas_" + suffix, TaskEventTypeEnum.SUCCEEDED);
        TaskCompletionParticipant participant = (context, completionData) -> assertEquals(1,
            bizTaskMapper.transitionState(task.getTaskId(), 0,
                Arrays.asList(TaskStateEnum.RUNNING.name()), TaskStateEnum.PAUSED.name(), finishedAt));

        assertThrows(ServiceException.class, () -> bizTaskCompletionManager.completeSuccess(
            completionCommand(execution, finishedAt), result(), participant, successEvent));

        BizTaskDTO unchangedTask = reloadTask(task.getTaskId());
        assertEquals(TaskStateEnum.RUNNING.name(), unchangedTask.getState());
        assertEquals(0, unchangedTask.getVersion());
        assertEquals(0, unchangedTask.getSuccessCount());
        BizTaskExecutionDTO unchangedExecution = reloadExecution(execution.getExecutionId());
        assertEquals(TaskExecutionStateEnum.RUNNING.name(), unchangedExecution.getState());
        assertEquals("claim-token", unchangedExecution.getClaimToken());
        assertEquals(0, countEvent(successEvent.getEventId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("participant 写库后异常应回滚领域写入且不推进核心事实")
    void completeSuccess_participantFailureShouldRollbackDomainWrite() {
        String suffix = suffix();
        LocalDateTime finishedAt = LocalDateTime.now().withNano(0);
        BizTaskDTO task = saveTask("btask_completion_participant_rollback_" + suffix);
        BizTaskExecutionDTO execution = saveRunningExecution(task.getTaskId(),
            "bexec_completion_participant_rollback_" + suffix);
        BizTaskEventDTO participantEvent = event(task.getTaskId(), execution.getExecutionId(),
            "bevt_participant_rollback_" + suffix, TaskEventTypeEnum.CREATED);
        BizTaskEventDTO successEvent = event(task.getTaskId(), execution.getExecutionId(),
            "bevt_success_not_written_" + suffix, TaskEventTypeEnum.SUCCEEDED);
        TaskCompletionParticipant participant = (context, completionData) -> {
            assertTrue(bizTaskEventManager.append(participantEvent));
            throw new IllegalStateException("simulated participant failure");
        };

        assertThrows(IllegalStateException.class, () -> bizTaskCompletionManager.completeSuccess(
            completionCommand(execution, finishedAt), result(), participant, successEvent));

        assertEquals(0, countEvent(participantEvent.getEventId()));
        assertEquals(0, countEvent(successEvent.getEventId()));
        assertEquals(0, reloadTask(task.getTaskId()).getSuccessCount());
        assertEquals(TaskExecutionStateEnum.RUNNING.name(),
            reloadExecution(execution.getExecutionId()).getState());
    }

    private BizTaskDTO saveTask(String taskId) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskDTO task = new BizTaskDTO();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setTaskId(taskId);
        task.setTaskType("COMPLETION_TEST");
        task.setTaskName("completion test");
        task.setTaskMode(TaskModeEnum.ONCE.name());
        task.setScheduleVersion(1);
        task.setState(TaskStateEnum.RUNNING.name());
        task.setPriority(0);
        task.setPlannedCount(1);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setMissedCount(0);
        task.setCancelledCount(0);
        task.setProgressCurrent(0L);
        task.setProgressTotal(10L);
        task.setProgressRevision(0L);
        task.setPayload("{}");
        task.setPayloadVersion(1);
        task.setOwnerType("USER");
        task.setOwnerId("OWNER_A");
        task.setVersion(0);
        bizTaskService.save(bizTaskAssembler.dtoToDo(task));
        return task;
    }

    private BizTaskExecutionDTO saveRunningExecution(String taskId, String executionId) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setCreateTime(now);
        execution.setUpdateTime(now);
        execution.setExecutionId(executionId);
        execution.setTaskId(taskId);
        execution.setScheduleVersion(1);
        execution.setPlannedAt(now.minusSeconds(1));
        execution.setState(TaskExecutionStateEnum.RUNNING.name());
        execution.setAttemptCount(1);
        execution.setMaxAttempts(2);
        execution.setStartedAt(now.minusSeconds(1));
        execution.setClaimToken("claim-token");
        execution.setWorkerNode("worker-a");
        execution.setLeaseUntil(now.plusMinutes(1));
        execution.setProgressCurrent(7L);
        execution.setProgressTotal(10L);
        execution.setProgressMessage("persisting result");
        execution.setProgressRevision(3L);
        execution.setRetryable(Boolean.FALSE);
        execution.setVersion(0);
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(execution));
        return execution;
    }

    private BizTaskExecutionDTO completionCommand(BizTaskExecutionDTO execution, LocalDateTime finishedAt) {
        BizTaskExecutionDTO command = new BizTaskExecutionDTO();
        command.setExecutionId(execution.getExecutionId());
        command.setTaskId(execution.getTaskId());
        command.setClaimToken(execution.getClaimToken());
        command.setState(TaskExecutionStateEnum.SUCCEEDED.name());
        command.setFinishedAt(finishedAt);
        command.setUpdateTime(finishedAt);
        return command;
    }

    private TaskExecutionResult result() {
        JSONObject summary = new JSONObject();
        summary.put("display", "completed");
        summary.put("secret", "must-not-persist");
        JSONObject completionData = new JSONObject();
        completionData.put("domainId", "domain-result");
        return new TaskExecutionResult(new TaskResultReference("TEST_RESULT", "result-1"), summary,
            completionData, null);
    }

    private BizTaskEventDTO event(String taskId, String executionId, String eventId,
        TaskEventTypeEnum eventType) {
        BizTaskEventDTO event = new BizTaskEventDTO();
        event.setCreateTime(LocalDateTime.now().withNano(0));
        event.setEventId(eventId);
        event.setTaskId(taskId);
        event.setExecutionId(executionId);
        event.setEventType(eventType.name());
        event.setFromState(TaskExecutionStateEnum.RUNNING.name());
        event.setToState(eventType.name());
        event.setAttemptNo(1);
        event.setWorkerNode("worker-a");
        event.setDedupeKey(eventId);
        event.setEventData("{}");
        event.setOccurredAt(LocalDateTime.now().withNano(0));
        return event;
    }

    private BizTaskDTO reloadTask(String taskId) {
        return bizTaskAssembler.doToDto(bizTaskService.lambdaQuery()
            .eq(io.github.lunasaw.voglander.repository.entity.BizTaskDO::getTaskId, taskId).one());
    }

    private BizTaskExecutionDTO reloadExecution(String executionId) {
        return bizTaskExecutionAssembler.doToDto(bizTaskExecutionService.lambdaQuery()
            .eq(io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO::getExecutionId,
                executionId).one());
    }

    private long countEvent(String eventId) {
        return bizTaskEventMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BizTaskEventDO>()
            .eq(BizTaskEventDO::getEventId, eventId));
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
