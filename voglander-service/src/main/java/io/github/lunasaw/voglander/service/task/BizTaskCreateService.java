package io.github.lunasaw.voglander.service.task;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateContext;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.enums.task.TaskExecutionStateEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskStateEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;

/** Trusted internal API for accepting domain-validated durable tasks. */
@Service
public class BizTaskCreateService {

    private final LongTaskHandlerRegistry handlerRegistry;
    private final BizTaskManager bizTaskManager;
    private final Clock clock;

    @Autowired
    public BizTaskCreateService(LongTaskHandlerRegistry handlerRegistry, BizTaskManager bizTaskManager) {
        this(handlerRegistry, bizTaskManager, Clock.systemDefaultZone());
    }

    BizTaskCreateService(LongTaskHandlerRegistry handlerRegistry, BizTaskManager bizTaskManager, Clock clock) {
        this.handlerRegistry = handlerRegistry;
        this.bizTaskManager = bizTaskManager;
        this.clock = clock;
    }

    /** Accepts only task types and payload versions registered by a trusted domain Handler. */
    public BizTaskDTO create(TaskCreateCommand command) {
        return create(command, null, null);
    }

    /** Creates a new ONCE task from a terminal failed execution without mutating its original history. */
    public BizTaskDTO manualRetry(BizTaskDTO originalTask, BizTaskExecutionDTO failedExecution,
        String idempotencyKey) {
        Assert.notNull(originalTask, "原任务不能为空");
        Assert.notNull(failedExecution, "失败执行不能为空");
        Assert.hasText(idempotencyKey, "手动重试幂等key不能为空");
        if (!TaskStateEnum.FAILED.name().equals(originalTask.getState())
            && !TaskStateEnum.PARTIAL_COMPLETED.name().equals(originalTask.getState())) {
            throw new ServiceException(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED);
        }
        if (!TaskExecutionStateEnum.FAILED.name().equals(failedExecution.getState())) {
            throw new ServiceException(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED);
        }
        Assert.isTrue(originalTask.getTaskId().equals(failedExecution.getTaskId()),
            "失败执行不属于原任务");
        Assert.hasText(originalTask.getPayload(), "原任务payload不能为空");
        int maxAttempts = failedExecution.getMaxAttempts() == null || failedExecution.getMaxAttempts() <= 0
            ? 1 : failedExecution.getMaxAttempts();
        TaskCreateCommand command = new TaskCreateCommand(originalTask.getTaskType(), originalTask.getTaskName(),
            originalTask.getDescription(), TaskModeEnum.ONCE.name(), null, null, null,
            JSON.parseObject(originalTask.getPayload()), originalTask.getPayloadVersion(), originalTask.getBizKey(),
            originalTask.getSubjectType(), originalTask.getSubjectId(), originalTask.getOwnerType(),
            originalTask.getOwnerId(), originalTask.getOrganizationId(), idempotencyKey, maxAttempts);
        return create(command, originalTask.getTaskId(), failedExecution.getExecutionId(), true);
    }

    private BizTaskDTO create(TaskCreateCommand command, String originTaskId, String originExecutionId) {
        return create(command, originTaskId, originExecutionId, false);
    }

    private BizTaskDTO create(TaskCreateCommand command, String originTaskId, String originExecutionId,
        boolean manualRetry) {
        Assert.notNull(command, "任务创建命令不能为空");
        Assert.hasText(command.taskType(), "taskType不能为空");
        LongTaskHandler handler = handlerRegistry.require(command.taskType(), command.payloadVersion());
        BizTaskDTO existing = bizTaskManager.findByIdempotency(command.ownerType(), command.ownerId(),
            command.taskType(), command.idempotencyKey());
        if (existing != null) {
            return existing;
        }
        if (manualRetry) {
            TaskCapabilities capabilities = handler.capabilities();
            if (capabilities == null || !capabilities.supportsManualRetry()) {
                throw new ServiceException(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED);
            }
        }
        TaskSchedulePlan schedulePlan = TaskScheduleCalculator.calculate(command);
        TaskCreateContext context = new TaskCreateContext(command.ownerType(), command.ownerId(),
            command.organizationId(), command.subjectType(), command.subjectId());
        String payloadSnapshot = TaskPayloadValidator.validateAndSerialize(command.payload());
        handler.validate(context, TaskPayloadValidator.copyOf(payloadSnapshot));

        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        BizTaskDTO task = buildTask(command, schedulePlan, now, payloadSnapshot, originTaskId, originExecutionId);
        BizTaskExecutionDTO firstExecution = schedulePlan.mode() == TaskModeEnum.ONCE
            ? buildFirstExecution(command, task.getTaskId(), now) : null;
        return bizTaskManager.create(task, firstExecution);
    }

    private BizTaskDTO buildTask(TaskCreateCommand command, TaskSchedulePlan schedulePlan, LocalDateTime now,
        String payloadSnapshot, String originTaskId, String originExecutionId) {
        TaskModeEnum mode = schedulePlan.mode();
        BizTaskDTO task = new BizTaskDTO();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setTaskId(id(TaskConstant.TASK_ID_PREFIX));
        task.setTaskType(command.taskType());
        task.setTaskName(command.taskName());
        task.setDescription(command.description());
        task.setTaskMode(mode.name());
        task.setScheduleStartTime(command.scheduleStartTime());
        task.setScheduleEndTime(command.scheduleEndTime());
        task.setIntervalSeconds(command.intervalSeconds());
        task.setNextPlanTime(mode == TaskModeEnum.ONCE ? null : command.scheduleStartTime());
        task.setScheduleVersion(1);
        task.setState(mode == TaskModeEnum.ONCE ? TaskStateEnum.RUNNING.name() : TaskStateEnum.SCHEDULED.name());
        task.setPriority(0);
        task.setPlannedCount(schedulePlan.plannedCount());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setMissedCount(0);
        task.setCancelledCount(0);
        task.setProgressCurrent(0L);
        task.setProgressTotal(0L);
        task.setProgressRevision(0L);
        task.setBizKey(command.bizKey());
        task.setSubjectType(command.subjectType());
        task.setSubjectId(command.subjectId());
        task.setPayload(payloadSnapshot);
        task.setPayloadVersion(command.payloadVersion());
        task.setOwnerType(command.ownerType());
        task.setOwnerId(command.ownerId());
        task.setOrganizationId(command.organizationId());
        task.setIdempotencyKey(command.idempotencyKey());
        task.setOriginTaskId(originTaskId);
        task.setOriginExecutionId(originExecutionId);
        task.setVersion(0);
        return task;
    }

    private BizTaskExecutionDTO buildFirstExecution(TaskCreateCommand command, String taskId,
        LocalDateTime now) {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setCreateTime(now);
        execution.setUpdateTime(now);
        execution.setExecutionId(id(TaskConstant.EXECUTION_ID_PREFIX));
        execution.setTaskId(taskId);
        execution.setScheduleVersion(1);
        execution.setPlannedAt(now);
        execution.setState(TaskExecutionStateEnum.PENDING.name());
        execution.setAttemptCount(0);
        execution.setMaxAttempts(command.maxAttempts());
        execution.setProgressCurrent(0L);
        execution.setProgressTotal(0L);
        execution.setProgressRevision(0L);
        execution.setRetryable(Boolean.FALSE);
        execution.setVersion(0);
        return execution;
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
