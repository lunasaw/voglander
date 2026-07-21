package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.client.domain.task.TaskCompletionContext;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.domain.task.TaskResultReference;
import io.github.lunasaw.voglander.client.service.task.TaskCompletionParticipant;
import io.github.lunasaw.voglander.common.enums.task.TaskEventTypeEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskExecutionStateEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskStateEnum;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;
import io.github.lunasaw.voglander.manager.assembler.BusinessTaskDataSanitizer;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;
import io.github.lunasaw.voglander.repository.mapper.BizTaskExecutionMapper;
import io.github.lunasaw.voglander.repository.mapper.BizTaskMapper;

/** Authoritative transaction for successful durable execution completion. */
@Component
public class BizTaskCompletionManager {

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskExecutionService bizTaskExecutionService;

    @Autowired
    private BizTaskMapper bizTaskMapper;

    @Autowired
    private BizTaskExecutionMapper bizTaskExecutionMapper;

    @Autowired
    private BizTaskEventManager bizTaskEventManager;

    @Autowired
    private BizTaskAssembler bizTaskAssembler;

    @Autowired(required = false)
    private BusinessTaskSseEventPublisher businessTaskSseEventPublisher;

    /**
     * Commits the domain participant, execution terminal fact, task aggregate and success event as one unit.
     */
    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO completeSuccess(BizTaskExecutionDTO command, TaskExecutionResult result,
        TaskCompletionParticipant participant, BizTaskEventDTO successEvent) {
        validateCommand(command, result, successEvent);
        BizTaskExecutionDO execution = requireRunningExecution(command);
        BizTaskDO task = requireTask(execution.getTaskId());
        Assert.isTrue(task.getTaskId().equals(command.getTaskId()), "完成命令任务与执行不匹配");

        TaskCompletionParticipant completionParticipant = participant == null
            ? TaskCompletionParticipant.noop() : participant;
        completionParticipant.complete(new TaskCompletionContext(task.getTaskId(), execution.getExecutionId(),
            nonNull(execution.getAttemptCount())), result.completionData());

        TaskResultReference reference = result.resultReference();
        String resultRefType = reference == null ? null : reference.type();
        String resultRefId = reference == null ? null : reference.id();
        String resultSummary = BusinessTaskDataSanitizer.sanitizeJson(JSON.toJSONString(result.resultSummary()));
        long executionTotal = nonNull(execution.getProgressTotal());
        long executionCurrent = executionTotal > 0 ? executionTotal : nonNull(execution.getProgressCurrent());
        long executionRevision = Math.addExact(nonNull(execution.getProgressRevision()), 1L);
        LocalDateTime finishedAt = command.getFinishedAt();
        int executionAffected = bizTaskExecutionMapper.completeSuccess(execution.getExecutionId(),
            command.getClaimToken(), finishedAt, executionCurrent, executionTotal, execution.getProgressMessage(),
            executionRevision, resultRefType, resultRefId, resultSummary, command.getUpdateTime());
        if (executionAffected != 1) {
            throw stateConflict();
        }

        long successCount = (long) nonNull(task.getSuccessCount()) + 1L;
        long terminalCount = successCount + nonNull(task.getFailedCount()) + nonNull(task.getMissedCount())
            + nonNull(task.getCancelledCount());
        int plannedCount = nonNull(task.getPlannedCount());
        Assert.isTrue(plannedCount > 0 && terminalCount <= plannedCount, "任务终态计数超过计划数");
        TaskAggregate aggregate = aggregate(task, successCount, terminalCount, plannedCount, executionCurrent,
            executionTotal, finishedAt);
        long taskRevision = Math.addExact(nonNull(task.getProgressRevision()), 1L);
        int taskAffected = bizTaskMapper.applyExecutionSuccess(task.getTaskId(), nonNull(task.getVersion()),
            execution.getExecutionId(), finishedAt, aggregate.progressCurrent, aggregate.progressTotal,
            execution.getProgressMessage(), taskRevision, resultRefType, resultRefId, resultSummary,
            aggregate.state, aggregate.completedTime, command.getUpdateTime());
        if (taskAffected != 1) {
            throw stateConflict();
        }

        if (!bizTaskEventManager.append(successEvent)) {
            throw stateConflict();
        }
        BizTaskDO completedTask = requireTask(task.getTaskId());
        BizTaskExecutionDO completedExecution = bizTaskExecutionService.getOne(
            new LambdaQueryWrapper<BizTaskExecutionDO>()
                .eq(BizTaskExecutionDO::getExecutionId, execution.getExecutionId()).last("LIMIT 1"));
        publishExecutionTerminalEvent(completedTask,
            completedExecution == null ? execution : completedExecution, finishedAt);
        publishTaskStateEvent(completedTask, aggregate.state, finishedAt);
        return bizTaskAssembler.doToDto(completedTask);
    }

    /** Commits a permanent Handler failure, task counters and failure event as one unit. */
    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO completeFailure(BizTaskExecutionDTO command, String failureCode, String failureMessage,
        BizTaskEventDTO failureEvent) {
        validateFailureCommand(command, failureCode, failureEvent);
        BizTaskExecutionDO execution = requireRunningExecution(command);
        BizTaskDO task = requireTask(execution.getTaskId());
        Assert.isTrue(task.getTaskId().equals(command.getTaskId()), "失败命令任务与执行不匹配");

        int executionAffected = bizTaskExecutionMapper.markTerminal(execution.getExecutionId(),
            command.getClaimToken(), TaskExecutionStateEnum.FAILED.name(), command.getFinishedAt(), null, null,
            null, failureCode, BusinessTaskDataSanitizer.sanitizeMessage(failureMessage), false,
            command.getUpdateTime());
        if (executionAffected != 1) {
            throw stateConflict();
        }

        long failedCount = (long) nonNull(task.getFailedCount()) + 1L;
        long terminalCount = (long) nonNull(task.getSuccessCount()) + failedCount
            + nonNull(task.getMissedCount()) + nonNull(task.getCancelledCount());
        int plannedCount = nonNull(task.getPlannedCount());
        Assert.isTrue(plannedCount > 0 && terminalCount <= plannedCount, "任务终态计数超过计划数");
        String aggregateState = task.getState();
        LocalDateTime completedTime = null;
        if (terminalCount == plannedCount) {
            if (TaskStateEnum.CANCELLING.name().equals(task.getState())) {
                aggregateState = TaskStateEnum.CANCELLED.name();
            } else if (failedCount == plannedCount) {
                aggregateState = TaskStateEnum.FAILED.name();
            } else {
                aggregateState = TaskStateEnum.PARTIAL_COMPLETED.name();
            }
            completedTime = command.getFinishedAt();
        }
        int taskAffected = bizTaskMapper.applyExecutionFailure(task.getTaskId(), nonNull(task.getVersion()),
            execution.getExecutionId(), command.getFinishedAt(), nonNull(execution.getProgressCurrent()),
            nonNull(execution.getProgressTotal()), execution.getProgressMessage(),
            nonNull(execution.getProgressRevision()), aggregateState, completedTime, failureCode,
            BusinessTaskDataSanitizer.sanitizeMessage(failureMessage), command.getUpdateTime());
        if (taskAffected != 1) {
            throw stateConflict();
        }
        if (!bizTaskEventManager.append(failureEvent)) {
            throw stateConflict();
        }
        BizTaskDO updated = requireTask(task.getTaskId());
        publishFailureEvents(updated, execution, failureEvent, command.getFinishedAt(), aggregateState);
        return bizTaskAssembler.doToDto(updated);
    }

    private void validateCommand(BizTaskExecutionDTO command, TaskExecutionResult result,
        BizTaskEventDTO successEvent) {
        Assert.notNull(command, "执行完成命令不能为空");
        Assert.hasText(command.getExecutionId(), "executionId不能为空");
        Assert.hasText(command.getTaskId(), "taskId不能为空");
        Assert.hasText(command.getClaimToken(), "claimToken不能为空");
        Assert.isTrue(TaskExecutionStateEnum.SUCCEEDED.name().equals(command.getState()),
            "成功完成命令状态必须为SUCCEEDED");
        Assert.notNull(command.getFinishedAt(), "finishedAt不能为空");
        Assert.notNull(command.getUpdateTime(), "updateTime不能为空");
        Assert.notNull(result, "成功执行结果不能为空");
        Assert.notNull(successEvent, "成功完成事件不能为空");
        Assert.isTrue(command.getTaskId().equals(successEvent.getTaskId()), "完成事件taskId不匹配");
        Assert.isTrue(command.getExecutionId().equals(successEvent.getExecutionId()),
            "完成事件executionId不匹配");
        Assert.isTrue(TaskEventTypeEnum.SUCCEEDED.name().equals(successEvent.getEventType()),
            "成功完成事件类型必须为SUCCEEDED");
    }

    private void validateFailureCommand(BizTaskExecutionDTO command, String failureCode,
        BizTaskEventDTO failureEvent) {
        Assert.notNull(command, "执行失败命令不能为空");
        Assert.hasText(command.getExecutionId(), "executionId不能为空");
        Assert.hasText(command.getTaskId(), "taskId不能为空");
        Assert.hasText(command.getClaimToken(), "claimToken不能为空");
        Assert.hasText(failureCode, "failureCode不能为空");
        Assert.isTrue(TaskExecutionStateEnum.FAILED.name().equals(command.getState()),
            "失败完成命令状态必须为FAILED");
        Assert.notNull(command.getFinishedAt(), "finishedAt不能为空");
        Assert.notNull(command.getUpdateTime(), "updateTime不能为空");
        Assert.notNull(failureEvent, "失败完成事件不能为空");
        Assert.isTrue(command.getTaskId().equals(failureEvent.getTaskId()), "失败事件taskId不匹配");
        Assert.isTrue(command.getExecutionId().equals(failureEvent.getExecutionId()),
            "失败事件executionId不匹配");
        Assert.isTrue(TaskEventTypeEnum.FAILED.name().equals(failureEvent.getEventType()),
            "失败完成事件类型必须为FAILED");
    }

    private BizTaskExecutionDO requireRunningExecution(BizTaskExecutionDTO command) {
        BizTaskExecutionDO execution = bizTaskExecutionService.getOne(
            new LambdaQueryWrapper<BizTaskExecutionDO>()
                .eq(BizTaskExecutionDO::getExecutionId, command.getExecutionId()).last("LIMIT 1"));
        if (execution == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_EXECUTION_NOT_FOUND);
        }
        if (!TaskExecutionStateEnum.RUNNING.name().equals(execution.getState())
            || !command.getClaimToken().equals(execution.getClaimToken())) {
            throw stateConflict();
        }
        return execution;
    }

    private BizTaskDO requireTask(String taskId) {
        BizTaskDO task = bizTaskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, taskId).last("LIMIT 1"));
        if (task == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_NOT_FOUND);
        }
        return task;
    }

    private TaskAggregate aggregate(BizTaskDO task, long successCount, long terminalCount, int plannedCount,
        long executionCurrent, long executionTotal, LocalDateTime finishedAt) {
        TaskAggregate aggregate = new TaskAggregate();
        if (TaskModeEnum.FIXED_RATE.name().equals(task.getTaskMode())) {
            aggregate.progressCurrent = terminalCount;
            aggregate.progressTotal = plannedCount;
        } else {
            aggregate.progressCurrent = executionCurrent;
            aggregate.progressTotal = executionTotal;
        }
        aggregate.state = task.getState();
        if (terminalCount == plannedCount && TaskStateEnum.RUNNING.name().equals(task.getState())) {
            aggregate.state = successCount == plannedCount ? TaskStateEnum.COMPLETED.name()
                : TaskStateEnum.PARTIAL_COMPLETED.name();
            aggregate.completedTime = finishedAt;
        } else if (terminalCount == plannedCount
            && TaskStateEnum.CANCELLING.name().equals(task.getState())) {
            aggregate.state = TaskStateEnum.CANCELLED.name();
            aggregate.completedTime = finishedAt;
        }
        return aggregate;
    }

    private int nonNull(Integer value) {
        return value == null ? 0 : value;
    }

    private long nonNull(Long value) {
        return value == null ? 0L : value;
    }

    private ServiceException stateConflict() {
        return new ServiceException(ServiceExceptionEnum.TASK_STATE_CONFLICT);
    }

    private void publishExecutionTerminalEvent(BizTaskDO task, BizTaskExecutionDO execution,
        LocalDateTime occurredAt) {
        if (businessTaskSseEventPublisher == null) {
            return;
        }
        BusinessTaskSseEvent event = new BusinessTaskSseEvent();
        event.setTopic(TaskConstant.SSE_EXECUTION_STATE);
        event.setTaskId(task.getTaskId());
        event.setExecutionId(execution.getExecutionId());
        event.setTaskType(task.getTaskType());
        event.setTaskState(task.getState());
        event.setExecutionState(TaskExecutionStateEnum.SUCCEEDED.name());
        event.setEventType(TaskEventTypeEnum.SUCCEEDED.name());
        event.setProgressCurrent(execution.getProgressCurrent());
        event.setProgressTotal(execution.getProgressTotal());
        event.setProgressRevision(execution.getProgressRevision());
        event.setAttemptNo(execution.getAttemptCount());
        event.setTimestamp(toEpochMillis(occurredAt));
        businessTaskSseEventPublisher.publish(event);
    }

    private void publishTaskStateEvent(BizTaskDO task, String state, LocalDateTime occurredAt) {
        if (businessTaskSseEventPublisher == null) {
            return;
        }
        BusinessTaskSseEvent event = new BusinessTaskSseEvent();
        event.setTopic(TaskConstant.SSE_TASK_STATE);
        event.setTaskId(task.getTaskId());
        event.setTaskType(task.getTaskType());
        event.setTaskState(state);
        event.setEventType(TaskEventTypeEnum.SUCCEEDED.name());
        event.setScheduleVersion(task.getScheduleVersion());
        event.setTimestamp(toEpochMillis(occurredAt));
        businessTaskSseEventPublisher.publish(event);
    }

    private void publishFailureEvents(BizTaskDO task, BizTaskExecutionDO execution, BizTaskEventDTO failureEvent,
        LocalDateTime occurredAt, String taskState) {
        if (businessTaskSseEventPublisher == null) {
            return;
        }
        BusinessTaskSseEvent executionEvent = new BusinessTaskSseEvent();
        executionEvent.setTopic(TaskConstant.SSE_EXECUTION_STATE);
        executionEvent.setTaskId(task.getTaskId());
        executionEvent.setExecutionId(execution.getExecutionId());
        executionEvent.setTaskType(task.getTaskType());
        executionEvent.setTaskState(taskState);
        executionEvent.setExecutionState(TaskExecutionStateEnum.FAILED.name());
        executionEvent.setEventType(TaskEventTypeEnum.FAILED.name());
        executionEvent.setFailureCode(failureEvent.getFailureCode());
        executionEvent.setProgressCurrent(execution.getProgressCurrent());
        executionEvent.setProgressTotal(execution.getProgressTotal());
        executionEvent.setProgressRevision(execution.getProgressRevision());
        executionEvent.setAttemptNo(execution.getAttemptCount());
        executionEvent.setTimestamp(toEpochMillis(occurredAt));
        businessTaskSseEventPublisher.publish(executionEvent);
        publishTaskStateEvent(task, taskState, occurredAt);
    }

    private Long toEpochMillis(LocalDateTime value) {
        return value == null ? null : value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static final class TaskAggregate {
        private long progressCurrent;
        private long progressTotal;
        private String state;
        private LocalDateTime completedTime;
    }
}
