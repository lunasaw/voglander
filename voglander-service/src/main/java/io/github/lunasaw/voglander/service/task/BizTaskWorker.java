package io.github.lunasaw.voglander.service.task;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.util.StringUtils;

import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.service.task.LongTaskContext;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.enums.task.TaskEventTypeEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskExecutionStateEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskStateEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskCompletionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import lombok.extern.slf4j.Slf4j;

/** Claims one durable execution under a short competition-reduction lock and database CAS. */
@Slf4j
final class BizTaskWorker implements BusinessTaskExecutionWorker {

    private final BizTaskExecutionManager executionManager;
    private final BizTaskManager bizTaskManager;
    private final LongTaskHandlerRegistry handlerRegistry;
    private final BizTaskCompletionManager completionManager;
    private final RedisLockUtil redisLockUtil;
    private final TransactionOperations transactionOperations;
    private final Clock clock;
    private final String workerNode;
    private final int leaseSeconds;
    private final int lockTimeoutSeconds;
    private final long progressMinIntervalMillis;
    private final TaskRetryPlanner retryPlanner;
    private final Supplier<String> claimTokenSupplier;

    BizTaskWorker(BizTaskExecutionManager executionManager, BizTaskManager bizTaskManager,
        LongTaskHandlerRegistry handlerRegistry, BizTaskCompletionManager completionManager,
        RedisLockUtil redisLockUtil, TransactionOperations transactionOperations, Clock clock, String workerNode,
        int leaseSeconds, int lockTimeoutSeconds, Supplier<String> claimTokenSupplier) {
        this(executionManager, bizTaskManager, handlerRegistry, completionManager, redisLockUtil,
            transactionOperations, clock, workerNode, leaseSeconds, lockTimeoutSeconds,
            TaskConstant.DEFAULT_PROGRESS_MIN_INTERVAL_MS, TaskConstant.DEFAULT_RETRY_INITIAL_DELAY_SECONDS,
            TaskConstant.DEFAULT_RETRY_MAX_DELAY_SECONDS, claimTokenSupplier);
    }

    BizTaskWorker(BizTaskExecutionManager executionManager, BizTaskManager bizTaskManager,
        LongTaskHandlerRegistry handlerRegistry, BizTaskCompletionManager completionManager,
        RedisLockUtil redisLockUtil, TransactionOperations transactionOperations, Clock clock, String workerNode,
        int leaseSeconds, int lockTimeoutSeconds, long progressMinIntervalMillis,
        Supplier<String> claimTokenSupplier) {
        this(executionManager, bizTaskManager, handlerRegistry, completionManager, redisLockUtil,
            transactionOperations, clock, workerNode, leaseSeconds, lockTimeoutSeconds, progressMinIntervalMillis,
            TaskConstant.DEFAULT_RETRY_INITIAL_DELAY_SECONDS, TaskConstant.DEFAULT_RETRY_MAX_DELAY_SECONDS,
            claimTokenSupplier);
    }

    BizTaskWorker(BizTaskExecutionManager executionManager, BizTaskManager bizTaskManager,
        LongTaskHandlerRegistry handlerRegistry, BizTaskCompletionManager completionManager,
        RedisLockUtil redisLockUtil, TransactionOperations transactionOperations, Clock clock, String workerNode,
        int leaseSeconds, int lockTimeoutSeconds, long progressMinIntervalMillis, int retryInitialDelaySeconds,
        int retryMaxDelaySeconds, Supplier<String> claimTokenSupplier) {
        this.executionManager = Objects.requireNonNull(executionManager, "executionManager");
        this.bizTaskManager = Objects.requireNonNull(bizTaskManager, "bizTaskManager");
        this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry");
        this.completionManager = Objects.requireNonNull(completionManager, "completionManager");
        this.redisLockUtil = Objects.requireNonNull(redisLockUtil, "redisLockUtil");
        this.transactionOperations = Objects.requireNonNull(transactionOperations, "transactionOperations");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (!StringUtils.hasText(workerNode)) {
            throw new IllegalArgumentException("Business-task worker node must not be blank");
        }
        if (leaseSeconds <= 0 || lockTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Business-task lease and lock timeout must be positive");
        }
        if (progressMinIntervalMillis <= 0) {
            throw new IllegalArgumentException("Business-task progress persistence interval must be positive");
        }
        this.workerNode = workerNode;
        this.leaseSeconds = leaseSeconds;
        this.lockTimeoutSeconds = lockTimeoutSeconds;
        this.progressMinIntervalMillis = progressMinIntervalMillis;
        this.retryPlanner = new TaskRetryPlanner(retryInitialDelaySeconds, retryMaxDelaySeconds);
        this.claimTokenSupplier = Objects.requireNonNull(claimTokenSupplier, "claimTokenSupplier");
    }

    @Override
    public void execute(String executionId) {
        BizTaskExecutionDTO claimed = claim(executionId);
        if (claimed == null) {
            return;
        }
        executeClaimed(claimed);
    }

    BizTaskExecutionDTO claim(String executionId) {
        if (!StringUtils.hasText(executionId)) {
            throw new IllegalArgumentException("executionId must not be blank");
        }
        String lockKey = TaskConstant.EXECUTION_LOCK_PREFIX + executionId;
        String lockValue = redisLockUtil.generateLockValue();
        if (!Boolean.TRUE.equals(redisLockUtil.lock(lockKey, lockValue, lockTimeoutSeconds))) {
            return null;
        }
        try {
            return transactionOperations.execute(status -> claimCurrent(executionId));
        } finally {
            redisLockUtil.unLock(lockKey, lockValue);
        }
    }

    private BizTaskExecutionDTO claimCurrent(String executionId) {
        LocalDateTime now = LocalDateTime.now(clock);
        BizTaskExecutionDTO current = executionManager.getForClaim(executionId);
        if (!isRunnable(current, now)) {
            return null;
        }
        String claimToken = claimTokenSupplier.get();
        if (!StringUtils.hasText(claimToken)) {
            throw new IllegalStateException("Business-task claim token must not be blank");
        }
        BizTaskExecutionDTO command = new BizTaskExecutionDTO();
        command.setExecutionId(executionId);
        command.setVersion(current.getVersion());
        command.setClaimToken(claimToken);
        command.setWorkerNode(workerNode);
        command.setUpdateTime(now);
        command.setLeaseUntil(now.plusSeconds(leaseSeconds));
        return executionManager.claim(command);
    }

    private boolean isRunnable(BizTaskExecutionDTO execution, LocalDateTime now) {
        if (execution == null || execution.getVersion() == null) {
            return false;
        }
        boolean runnableState = TaskExecutionStateEnum.PENDING.name().equals(execution.getState())
            || TaskExecutionStateEnum.RETRY_WAIT.name().equals(execution.getState());
        return runnableState
            && (execution.getNextAttemptTime() == null || !execution.getNextAttemptTime().isAfter(now));
    }

    private void executeClaimed(BizTaskExecutionDTO execution) {
        BizTaskDTO task = bizTaskManager.getForExecution(execution.getTaskId());
        if (task == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_NOT_FOUND);
        }
        if (!execution.getTaskId().equals(task.getTaskId())) {
            throw handlerFailure("Claimed execution and task identity do not match");
        }
        Integer payloadVersion = task.getPayloadVersion();
        if (payloadVersion == null || payloadVersion <= 0) {
            throw new ServiceException(ServiceExceptionEnum.TASK_PAYLOAD_INVALID)
                .setDetailMessage("Persisted task payload version is invalid");
        }
        LongTaskHandler handler = handlerRegistry.require(task.getTaskType(), payloadVersion);
        if (!task.getTaskType().equals(handler.taskType())) {
            throw handlerFailure("Resolved Handler type does not match persisted task type");
        }
        ClaimedTaskContext context = context(execution, handler);
        TaskExecutionResult result;
        try {
            result = handler.execute(context, TaskPayloadValidator.copyOf(task.getPayload()));
        } catch (Exception exception) {
            TaskAttemptContext attemptContext = attemptContext(execution);
            RetryDecision decision = TaskRetryDecisionNormalizer.normalize(handler, exception, attemptContext);
            LocalDateTime retryAt = retryPlanner.nextAttemptTime(decision, attemptContext, LocalDateTime.now(clock));
            if (retryAt != null) {
                boolean retryScheduled = executionManager.markRetryWait(
                    retryWaitCommand(execution, decision, retryAt));
                log.warn(
                    "Business-task Handler exception retry scheduled: taskType={}, executionId={}, failureCode={}, accepted={}",
                    task.getTaskType(), execution.getExecutionId(), decision.failureCode(), retryScheduled);
                return;
            }
            context.forceProgressPersistence();
            LocalDateTime finishedAt = LocalDateTime.now(clock);
            log.warn(
                "Business-task Handler exception terminalized: taskType={}, executionId={}, failureCode={}, exceptionType={}",
                task.getTaskType(), execution.getExecutionId(), decision.failureCode(),
                exception.getClass().getName());
            completionManager.completeFailure(failureCommand(execution, finishedAt), decision.failureCode(),
                decision.failureMessage(), failureEvent(execution, decision, finishedAt));
            return;
        }
        if (result == null) {
            throw handlerFailure("Handler returned no execution result");
        }
        context.forceProgressPersistence();
        LocalDateTime finishedAt = LocalDateTime.now(clock);
        try {
            completionManager.completeSuccess(completionCommand(execution, finishedAt), result,
                handler.completionParticipant(), successEvent(execution, finishedAt));
        } catch (RuntimeException completionFailure) {
            // Promotion happens before the DB transaction.  If the transaction rolls
            // back, compensate the provider object before allowing the worker retry/
            // lease recovery path to observe the failure.
            try {
                result.compensation().compensate();
            } catch (RuntimeException compensationFailure) {
                log.error("Business-task provider compensation failed: taskType={}, executionId={}, compensationType={}",
                    task.getTaskType(), execution.getExecutionId(), compensationFailure.getClass().getSimpleName());
            }
            throw completionFailure;
        }
    }

    private ClaimedTaskContext context(BizTaskExecutionDTO execution, LongTaskHandler handler) {
        TaskAttemptContext attemptContext = attemptContext(execution);
        LocalDateTime lastPersistedAt = execution.getUpdateTime() == null
            ? LocalDateTime.now(clock) : execution.getUpdateTime();
        ThrottledTaskProgressReporter reporter = new ThrottledTaskProgressReporter(executionManager,
            progressMinIntervalMillis, progressValue(execution.getProgressCurrent()),
            progressValue(execution.getProgressTotal()), progressValue(execution.getProgressRevision()),
            execution.getProgressMessage(), lastPersistedAt);
        return new ClaimedTaskContext(execution.getTaskId(), execution.getExecutionId(), execution.getClaimToken(),
            attemptContext.attempt(), progressValue(execution.getProgressCurrent()), progressValue(execution.getProgressTotal()),
            progressValue(execution.getProgressRevision()), clock, reporter,
            () -> heartbeat(execution.getExecutionId(), execution.getClaimToken()),
            () -> isCancellationRequested(execution.getTaskId(), handler));
    }

    private TaskAttemptContext attemptContext(BizTaskExecutionDTO execution) {
        Integer attempt = execution.getAttemptCount();
        Integer maxAttempts = execution.getMaxAttempts();
        if (attempt == null || attempt <= 0) {
            throw handlerFailure("Claimed execution attempt is invalid");
        }
        int boundedMaxAttempts = maxAttempts == null || maxAttempts <= 0 ? attempt : maxAttempts;
        return new TaskAttemptContext(execution.getTaskId(), execution.getExecutionId(), attempt, boundedMaxAttempts,
            execution.getDeadlineAt());
    }

    private boolean isCancellationRequested(String taskId, LongTaskHandler handler) {
        if (!handler.capabilities().supportsCancel()) {
            return false;
        }
        BizTaskDTO current = bizTaskManager.getForExecution(taskId);
        return current != null && TaskStateEnum.CANCELLING.name().equals(current.getState());
    }

    private boolean heartbeat(String executionId, String claimToken) {
        LocalDateTime heartbeatAt = LocalDateTime.now(clock);
        BizTaskExecutionDTO command = new BizTaskExecutionDTO();
        command.setExecutionId(executionId);
        command.setClaimToken(claimToken);
        command.setHeartbeatAt(heartbeatAt);
        command.setUpdateTime(heartbeatAt);
        command.setLeaseUntil(heartbeatAt.plusSeconds(leaseSeconds));
        return executionManager.heartbeat(command);
    }

    private long progressValue(Long value) {
        return value == null ? 0L : value;
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

    private BizTaskExecutionDTO retryWaitCommand(BizTaskExecutionDTO execution, RetryDecision decision,
        LocalDateTime nextAttemptTime) {
        BizTaskExecutionDTO command = new BizTaskExecutionDTO();
        command.setExecutionId(execution.getExecutionId());
        command.setClaimToken(execution.getClaimToken());
        command.setNextAttemptTime(nextAttemptTime);
        command.setFailureCode(decision.failureCode());
        command.setFailureMessage(decision.failureMessage());
        command.setUpdateTime(LocalDateTime.now(clock));
        return command;
    }

    private BizTaskExecutionDTO failureCommand(BizTaskExecutionDTO execution, LocalDateTime finishedAt) {
        BizTaskExecutionDTO command = new BizTaskExecutionDTO();
        command.setExecutionId(execution.getExecutionId());
        command.setTaskId(execution.getTaskId());
        command.setClaimToken(execution.getClaimToken());
        command.setState(TaskExecutionStateEnum.FAILED.name());
        command.setFinishedAt(finishedAt);
        command.setUpdateTime(finishedAt);
        return command;
    }

    private BizTaskEventDTO failureEvent(BizTaskExecutionDTO execution, RetryDecision decision,
        LocalDateTime finishedAt) {
        BizTaskEventDTO event = new BizTaskEventDTO();
        event.setCreateTime(finishedAt);
        event.setEventId(TaskConstant.EVENT_ID_PREFIX + UUID.randomUUID().toString().replace("-", ""));
        event.setTaskId(execution.getTaskId());
        event.setExecutionId(execution.getExecutionId());
        event.setEventType(TaskEventTypeEnum.FAILED.name());
        event.setFromState(TaskExecutionStateEnum.RUNNING.name());
        event.setToState(TaskExecutionStateEnum.FAILED.name());
        event.setAttemptNo(execution.getAttemptCount());
        event.setWorkerNode(workerNode);
        event.setFailureCode(decision.failureCode());
        event.setFailureMessage(decision.failureMessage());
        event.setDedupeKey("execution-failed:" + execution.getExecutionId());
        event.setEventData("{}");
        event.setOccurredAt(finishedAt);
        return event;
    }

    private BizTaskEventDTO successEvent(BizTaskExecutionDTO execution, LocalDateTime finishedAt) {
        BizTaskEventDTO event = new BizTaskEventDTO();
        event.setCreateTime(finishedAt);
        event.setEventId(TaskConstant.EVENT_ID_PREFIX + UUID.randomUUID().toString().replace("-", ""));
        event.setTaskId(execution.getTaskId());
        event.setExecutionId(execution.getExecutionId());
        event.setEventType(TaskEventTypeEnum.SUCCEEDED.name());
        event.setFromState(TaskExecutionStateEnum.RUNNING.name());
        event.setToState(TaskExecutionStateEnum.SUCCEEDED.name());
        event.setAttemptNo(execution.getAttemptCount());
        event.setWorkerNode(workerNode);
        event.setDedupeKey("execution-success:" + execution.getExecutionId());
        event.setEventData("{}");
        event.setOccurredAt(finishedAt);
        return event;
    }

    private ServiceException handlerFailure(String detailMessage) {
        return new ServiceException(ServiceExceptionEnum.TASK_HANDLER_FAILED).setDetailMessage(detailMessage);
    }

}
