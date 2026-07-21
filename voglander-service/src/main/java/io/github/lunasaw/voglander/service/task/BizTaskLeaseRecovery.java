package io.github.lunasaw.voglander.service.task;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.util.StringUtils;

import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.common.enums.task.TaskExecutionStateEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskFailureCodeEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;

/** Recovers stale claimed executions through the Manager's claim and version conditional update. */
final class BizTaskLeaseRecovery {

    private static final String LEASE_EXPIRED_MESSAGE = "Execution lease expired";

    private final BizTaskExecutionManager executionManager;
    private final Clock clock;
    private final int batchSize;
    private final TaskRetryPlanner retryPlanner;

    BizTaskLeaseRecovery(BizTaskExecutionManager executionManager, Clock clock, int batchSize,
        int retryInitialDelaySeconds, int retryMaxDelaySeconds) {
        this.executionManager = Objects.requireNonNull(executionManager, "executionManager");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (batchSize <= 0 || batchSize > 1000) {
            throw new IllegalArgumentException("Business-task lease recovery batch size is invalid");
        }
        this.batchSize = batchSize;
        this.retryPlanner = new TaskRetryPlanner(retryInitialDelaySeconds, retryMaxDelaySeconds);
    }

    void recoverExpiredLeases() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<BizTaskExecutionDTO> expiredExecutions = executionManager.findExpiredLeases(now, batchSize);
        if (expiredExecutions == null) {
            return;
        }
        for (BizTaskExecutionDTO execution : expiredExecutions) {
            if (!isRecoverable(execution, now)) {
                continue;
            }
            executionManager.recoverExpiredLease(recoveryCommand(execution, now));
        }
    }

    private BizTaskExecutionDTO recoveryCommand(BizTaskExecutionDTO execution, LocalDateTime now) {
        TaskAttemptContext attemptContext = new TaskAttemptContext(execution.getTaskId(), execution.getExecutionId(),
            positiveValue(execution.getAttemptCount()), boundedMaxAttempts(execution), execution.getDeadlineAt());
        LocalDateTime retryAt = retryPlanner.nextAttemptTime(
            RetryDecision.retryable(TaskFailureCodeEnum.LEASE_EXPIRED.name(), LEASE_EXPIRED_MESSAGE),
            attemptContext, now);
        BizTaskExecutionDTO command = new BizTaskExecutionDTO();
        command.setExecutionId(execution.getExecutionId());
        command.setVersion(execution.getVersion());
        command.setClaimToken(execution.getClaimToken());
        command.setFailureCode(TaskFailureCodeEnum.LEASE_EXPIRED.name());
        command.setFailureMessage(LEASE_EXPIRED_MESSAGE);
        command.setUpdateTime(now);
        if (retryAt == null) {
            command.setState(TaskExecutionStateEnum.FAILED.name());
            command.setFinishedAt(now);
        } else {
            command.setState(TaskExecutionStateEnum.RETRY_WAIT.name());
            command.setNextAttemptTime(retryAt);
        }
        return command;
    }

    private boolean isRecoverable(BizTaskExecutionDTO execution, LocalDateTime now) {
        return execution != null && StringUtils.hasText(execution.getExecutionId())
            && StringUtils.hasText(execution.getClaimToken()) && execution.getVersion() != null
            && TaskExecutionStateEnum.RUNNING.name().equals(execution.getState())
            && execution.getLeaseUntil() != null && execution.getLeaseUntil().isBefore(now);
    }

    private int positiveValue(Integer value) {
        return value == null || value <= 0 ? 0 : value;
    }

    private int boundedMaxAttempts(BizTaskExecutionDTO execution) {
        Integer maxAttempts = execution.getMaxAttempts();
        int attempt = positiveValue(execution.getAttemptCount());
        return maxAttempts == null || maxAttempts <= 0 ? attempt : maxAttempts;
    }
}
