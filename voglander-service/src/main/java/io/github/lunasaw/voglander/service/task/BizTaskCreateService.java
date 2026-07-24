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
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCreateResultDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.service.idempotency.IdempotencyKeyValidator;
import io.github.lunasaw.voglander.service.idempotency.IdempotencyMetrics;

/** Trusted internal API for accepting domain-validated durable tasks. */
@Service
public class BizTaskCreateService {

    private final LongTaskHandlerRegistry handlerRegistry;
    private final BizTaskManager bizTaskManager;
    private final Clock clock;
    private final IdempotencyMetrics idempotencyMetrics;

    public BizTaskCreateService(LongTaskHandlerRegistry handlerRegistry, BizTaskManager bizTaskManager) {
        this(handlerRegistry, bizTaskManager, Clock.systemDefaultZone(), null);
    }

    @Autowired
    public BizTaskCreateService(LongTaskHandlerRegistry handlerRegistry, BizTaskManager bizTaskManager,
        IdempotencyMetrics idempotencyMetrics) {
        this(handlerRegistry, bizTaskManager, Clock.systemDefaultZone(), idempotencyMetrics);
    }

    BizTaskCreateService(LongTaskHandlerRegistry handlerRegistry, BizTaskManager bizTaskManager, Clock clock) {
        this(handlerRegistry, bizTaskManager, clock, null);
    }

    BizTaskCreateService(LongTaskHandlerRegistry handlerRegistry, BizTaskManager bizTaskManager, Clock clock,
        IdempotencyMetrics idempotencyMetrics) {
        this.handlerRegistry = handlerRegistry;
        this.bizTaskManager = bizTaskManager;
        this.clock = clock;
        this.idempotencyMetrics = idempotencyMetrics;
    }

    /** Accepts only task types and payload versions registered by a trusted domain Handler. */
    public BizTaskDTO create(TaskCreateCommand command) {
        return createResult(command).getAcceptedTask();
    }

    /** Returns whether this call created the durable task or replayed the database winner. */
    public BizTaskCreateResultDTO createResult(TaskCreateCommand command) {
        return createResult(command, null, null, false);
    }

    /**
     * Performs validation and lets the insert be the first database operation.
     * This is used by domain transactions that already ran their replay fast path outside the transaction.
     * Correctness still comes from Manager insert-if-absent and canonical winner comparison.
     */
    public BizTaskCreateResultDTO createResultByDatabaseArbitration(TaskCreateCommand command) {
        PreparedCreate prepared = prepare(command, null, null, false);
        return createPrepared(command, prepared, null, null);
    }

    /** Creates a new ONCE task from a terminal failed execution without mutating its original history. */
    public BizTaskDTO manualRetry(BizTaskDTO originalTask, BizTaskExecutionDTO failedExecution,
        String idempotencyKey) {
        Assert.notNull(originalTask, "原任务不能为空");
        Assert.notNull(failedExecution, "失败执行不能为空");
        IdempotencyKeyValidator.validateRequired(idempotencyKey);
        boolean imageCollection = io.github.lunasaw.voglander.common.constant.image.ImageConstant
            .TASK_TYPE_IMAGE_COLLECTION.equals(originalTask.getTaskType());
        if (!TaskStateEnum.FAILED.name().equals(originalTask.getState())
            && (imageCollection || !TaskStateEnum.PARTIAL_COMPLETED.name().equals(originalTask.getState()))) {
            throw new ServiceException(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED);
        }
        if (!TaskExecutionStateEnum.FAILED.name().equals(failedExecution.getState())) {
            throw new ServiceException(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED);
        }
        if (!originalTask.getTaskId().equals(failedExecution.getTaskId())) {
            throw new ServiceException(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED);
        }
        Assert.hasText(originalTask.getPayload(), "原任务payload不能为空");
        int maxAttempts = failedExecution.getMaxAttempts() == null || failedExecution.getMaxAttempts() <= 0
            ? 1 : failedExecution.getMaxAttempts();
        TaskCreateCommand command = new TaskCreateCommand(originalTask.getTaskType(), originalTask.getTaskName(),
            originalTask.getDescription(), TaskModeEnum.ONCE.name(), null, null, null,
            JSON.parseObject(originalTask.getPayload()), originalTask.getPayloadVersion(), originalTask.getBizKey(),
            originalTask.getSubjectType(), originalTask.getSubjectId(), originalTask.getOwnerType(),
            originalTask.getOwnerId(), originalTask.getOrganizationId(), idempotencyKey, maxAttempts);
        return createResult(command, originalTask.getTaskId(), failedExecution.getExecutionId(), true)
            .getAcceptedTask();
    }

    private BizTaskCreateResultDTO createResult(TaskCreateCommand command, String originTaskId,
        String originExecutionId,
        boolean manualRetry) {
        ReplayPreflight preflight = replayPreflight(command, originTaskId, originExecutionId);
        BizTaskCreateResultDTO replay = findReplay(preflight);
        if (replay != null) {
            return replay;
        }

        PreparedCreate prepared = prepare(command, originTaskId, originExecutionId, manualRetry, preflight);
        return createPrepared(command, prepared, originTaskId, originExecutionId);
    }

    private BizTaskCreateResultDTO createPrepared(TaskCreateCommand command, PreparedCreate prepared,
        String originTaskId, String originExecutionId) {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        BizTaskDTO task = buildTask(command, prepared.schedulePlan, now, prepared.payloadSnapshot,
            originTaskId, originExecutionId);
        BizTaskExecutionDTO firstExecution = prepared.schedulePlan.mode() == TaskModeEnum.ONCE
            ? buildFirstExecution(command, task.getTaskId(), now) : null;
        BizTaskCreateResultDTO result = bizTaskManager.create(task, firstExecution);
        if (!result.isCreated()) {
            requireSameSnapshot(prepared.snapshot, result);
        }
        recordDecision(prepared.command, result.isCreated() ? "CREATED" : "REPLAYED");
        return result;
    }

    /**
     * Returns an accepted canonical replay without consulting mutable domain resources.
     * A missing identity returns {@code null}; a reused identity with different content is rejected.
     */
    public BizTaskDTO findAcceptedReplay(TaskCreateCommand command) {
        BizTaskCreateResultDTO result = findAcceptedReplayResult(command);
        return result == null ? null : result.getAcceptedTask();
    }

    /** Same as {@link #findAcceptedReplay(TaskCreateCommand)}, retaining the authoritative creation decision. */
    public BizTaskCreateResultDTO findAcceptedReplayResult(TaskCreateCommand command) {
        return findReplay(replayPreflight(command, null, null));
    }

    private PreparedCreate prepare(TaskCreateCommand command, String originTaskId, String originExecutionId,
        boolean manualRetry) {
        return prepare(command, originTaskId, originExecutionId, manualRetry,
            replayPreflight(command, originTaskId, originExecutionId));
    }

    private PreparedCreate prepare(TaskCreateCommand command, String originTaskId, String originExecutionId,
        boolean manualRetry, ReplayPreflight preflight) {
        LongTaskHandler handler = handlerRegistry.require(command.taskType(), command.payloadVersion());
        if (manualRetry) {
            TaskCapabilities capabilities = handler.capabilities();
            if (capabilities == null || !capabilities.supportsManualRetry()) {
                throw new ServiceException(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED);
            }
        }
        TaskSchedulePlan schedulePlan = TaskScheduleCalculator.calculate(command);
        TaskCreateContext context = new TaskCreateContext(command.ownerType(), command.ownerId(),
            command.organizationId(), command.subjectType(), command.subjectId());
        handler.validate(context, TaskPayloadValidator.copyOf(preflight.payloadSnapshot));
        return new PreparedCreate(command, schedulePlan, preflight.payloadSnapshot, preflight.snapshot);
    }

    private ReplayPreflight replayPreflight(TaskCreateCommand command, String originTaskId,
        String originExecutionId) {
        Assert.notNull(command, "任务创建命令不能为空");
        Assert.hasText(command.taskType(), "taskType不能为空");
        IdempotencyKeyValidator.validateOptional(command.idempotencyKey());
        String payloadSnapshot = TaskPayloadValidator.validateAndSerialize(command.payload());
        TaskCanonicalSnapshot snapshot = TaskCanonicalSnapshot.fromCommand(command, payloadSnapshot,
            originTaskId, originExecutionId);
        return new ReplayPreflight(command, payloadSnapshot, snapshot);
    }

    private BizTaskCreateResultDTO findReplay(ReplayPreflight preflight) {
        BizTaskCreateResultDTO accepted = bizTaskManager.findCreateResultByIdempotency(
            preflight.command.ownerType(), preflight.command.ownerId(), preflight.command.taskType(),
            preflight.command.idempotencyKey());
        if (accepted == null) {
            return null;
        }
        requireSameSnapshot(preflight.snapshot, accepted);
        recordDecision(preflight.command, "REPLAYED");
        return accepted;
    }

    private void requireSameSnapshot(TaskCanonicalSnapshot requested, BizTaskCreateResultDTO accepted) {
        BizTaskDTO task = accepted.getAcceptedTask();
        if (TaskModeEnum.ONCE.name().equals(task.getTaskMode()) && accepted.getAcceptedFirstExecution() == null) {
            throw new IllegalStateException("accepted ONCE task is missing its first execution");
        }
        TaskCanonicalSnapshot winner = TaskCanonicalSnapshot.fromAccepted(task,
            accepted.getAcceptedFirstExecution());
        if (!requested.fingerprint().equals(winner.fingerprint())) {
            if (idempotencyMetrics != null) idempotencyMetrics.record("CONFLICT", "600007");
            throw new ServiceException(ServiceExceptionEnum.IDEMPOTENCY_KEY_REUSED);
        }
    }

    private void recordDecision(TaskCreateCommand command, String outcome) {
        if (idempotencyMetrics != null && command != null
            && org.springframework.util.StringUtils.hasText(command.idempotencyKey())) {
            idempotencyMetrics.record(outcome, null);
        }
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

    private static final class PreparedCreate {
        private final TaskCreateCommand command;
        private final TaskSchedulePlan schedulePlan;
        private final String payloadSnapshot;
        private final TaskCanonicalSnapshot snapshot;

        private PreparedCreate(TaskCreateCommand command, TaskSchedulePlan schedulePlan, String payloadSnapshot,
            TaskCanonicalSnapshot snapshot) {
            this.command = command;
            this.schedulePlan = schedulePlan;
            this.payloadSnapshot = payloadSnapshot;
            this.snapshot = snapshot;
        }
    }

    private static final class ReplayPreflight {
        private final TaskCreateCommand command;
        private final String payloadSnapshot;
        private final TaskCanonicalSnapshot snapshot;

        private ReplayPreflight(TaskCreateCommand command, String payloadSnapshot,
            TaskCanonicalSnapshot snapshot) {
            this.command = command;
            this.payloadSnapshot = payloadSnapshot;
            this.snapshot = snapshot;
        }
    }
}
