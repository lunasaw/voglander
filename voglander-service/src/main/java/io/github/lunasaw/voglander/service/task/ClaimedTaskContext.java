package io.github.lunasaw.voglander.service.task;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.springframework.util.StringUtils;

import io.github.lunasaw.voglander.client.service.task.LongTaskContext;
import io.github.lunasaw.voglander.client.service.task.TaskCancellationToken;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskProgressDTO;

/** In-process invariant boundary for one successfully claimed execution attempt. */
final class ClaimedTaskContext implements LongTaskContext {
    private final String taskId;
    private final String executionId;
    private final String claimToken;
    private final int attempt;
    private final Clock clock;
    private final Consumer<BizTaskProgressDTO> progressReporter;
    private final BooleanSupplier heartbeatReporter;
    private final TaskCancellationToken cancellationToken;

    private long current;
    private long total;
    private long revision;
    private BizTaskProgressDTO latestReport;

    ClaimedTaskContext(String taskId, String executionId, String claimToken, int attempt, long current,
        long total, long revision, Clock clock, Consumer<BizTaskProgressDTO> progressReporter,
        BooleanSupplier heartbeatReporter, BooleanSupplier cancellationRequested) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(executionId)
            || !StringUtils.hasText(claimToken)) {
            throw new IllegalArgumentException("Claimed task context identities must not be blank");
        }
        if (attempt <= 0) {
            throw new IllegalArgumentException("Claimed task context attempt must be positive");
        }
        validateProgress(current, total);
        if (revision < 0) {
            throw progressFailure("Persisted progress revision must not be negative");
        }
        this.taskId = taskId;
        this.executionId = executionId;
        this.claimToken = claimToken;
        this.attempt = attempt;
        this.current = current;
        this.total = total;
        this.revision = revision;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.progressReporter = Objects.requireNonNull(progressReporter, "progressReporter");
        this.heartbeatReporter = Objects.requireNonNull(heartbeatReporter, "heartbeatReporter");
        BooleanSupplier cancellationSupplier = Objects.requireNonNull(cancellationRequested,
            "cancellationRequested");
        this.cancellationToken = new TaskCancellationToken() {
            @Override
            public boolean isCancellationRequested() {
                return cancellationSupplier.getAsBoolean();
            }

            @Override
            public void throwIfCancellationRequested() {
                if (isCancellationRequested()) {
                    throw new ServiceException(ServiceExceptionEnum.TASK_STATE_CONFLICT)
                        .setDetailMessage("Task cancellation has been requested");
                }
            }
        };
    }

    @Override
    public String taskId() {
        return taskId;
    }

    @Override
    public String executionId() {
        return executionId;
    }

    @Override
    public int attempt() {
        return attempt;
    }

    @Override
    public TaskCancellationToken cancellationToken() {
        return cancellationToken;
    }

    @Override
    public synchronized void reportProgress(long current, long total, String message) {
        validateProgress(current, total);
        if (current < this.current) {
            throw progressFailure("Progress current must not move backwards");
        }
        if (this.total > 0 && total < this.total) {
            throw progressFailure("Determinate progress total must not shrink or become indeterminate");
        }
        if (revision == Long.MAX_VALUE) {
            throw progressFailure("Progress revision is exhausted");
        }
        long nextRevision = revision + 1;
        BizTaskProgressDTO report = new BizTaskProgressDTO();
        report.setTaskId(taskId);
        report.setExecutionId(executionId);
        report.setClaimToken(claimToken);
        report.setCurrent(current);
        report.setTotal(total);
        report.setMessage(message);
        report.setRevision(nextRevision);
        report.setForcePersist(Boolean.FALSE);
        report.setReportedAt(LocalDateTime.now(clock));
        this.current = current;
        this.total = total;
        this.revision = nextRevision;
        this.latestReport = report;
        progressReporter.accept(report);
    }

    synchronized void forceProgressPersistence() {
        if (latestReport == null || Boolean.TRUE.equals(latestReport.getForcePersist())) {
            return;
        }
        BizTaskProgressDTO forced = new BizTaskProgressDTO();
        forced.setTaskId(latestReport.getTaskId());
        forced.setExecutionId(latestReport.getExecutionId());
        forced.setClaimToken(latestReport.getClaimToken());
        forced.setCurrent(latestReport.getCurrent());
        forced.setTotal(latestReport.getTotal());
        forced.setMessage(latestReport.getMessage());
        forced.setRevision(latestReport.getRevision());
        forced.setForcePersist(Boolean.TRUE);
        forced.setReportedAt(LocalDateTime.now(clock));
        latestReport = forced;
        progressReporter.accept(forced);
    }

    @Override
    public boolean heartbeat() {
        return heartbeatReporter.getAsBoolean();
    }

    private static void validateProgress(long current, long total) {
        if (current < 0 || total < 0) {
            throw progressFailure("Progress current and total must not be negative");
        }
        if (total > 0 && current > total) {
            throw progressFailure("Progress current must not exceed determinate total");
        }
    }

    private static ServiceException progressFailure(String detailMessage) {
        return new ServiceException(ServiceExceptionEnum.TASK_PROGRESS_INVALID).setDetailMessage(detailMessage);
    }
}
