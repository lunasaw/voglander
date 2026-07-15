package io.github.lunasaw.voglander.service.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskProgressDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;

/** Persists progress at bounded frequency while preserving phase and terminal updates. */
final class ThrottledTaskProgressReporter implements Consumer<BizTaskProgressDTO> {

    private static final double ONE_PERCENT = 0.01D;

    private final BizTaskExecutionManager executionManager;
    private final long minIntervalMillis;

    private long lastPersistedCurrent;
    private long lastPersistedTotal;
    private long lastPersistedRevision;
    private String lastPersistedMessage;
    private LocalDateTime lastPersistedAt;

    ThrottledTaskProgressReporter(BizTaskExecutionManager executionManager, long minIntervalMillis,
        long initialCurrent, long initialTotal, String initialMessage, LocalDateTime initialPersistedAt) {
        this(executionManager, minIntervalMillis, initialCurrent, initialTotal, 0L, initialMessage,
            initialPersistedAt);
    }

    ThrottledTaskProgressReporter(BizTaskExecutionManager executionManager, long minIntervalMillis,
        long initialCurrent, long initialTotal, long initialRevision, String initialMessage,
        LocalDateTime initialPersistedAt) {
        this.executionManager = Objects.requireNonNull(executionManager, "executionManager");
        if (minIntervalMillis <= 0) {
            throw new IllegalArgumentException("Progress persistence interval must be positive");
        }
        if (initialCurrent < 0 || initialTotal < 0 || initialRevision < 0) {
            throw new IllegalArgumentException("Persisted progress values must not be negative");
        }
        this.minIntervalMillis = minIntervalMillis;
        this.lastPersistedCurrent = initialCurrent;
        this.lastPersistedTotal = initialTotal;
        this.lastPersistedRevision = initialRevision;
        this.lastPersistedMessage = initialMessage;
        this.lastPersistedAt = Objects.requireNonNull(initialPersistedAt, "initialPersistedAt");
    }

    @Override
    public synchronized void accept(BizTaskProgressDTO progress) {
        Objects.requireNonNull(progress, "progress");
        long revision = value(progress.getRevision());
        boolean forced = Boolean.TRUE.equals(progress.getForcePersist());
        if (forced && revision <= lastPersistedRevision) {
            return;
        }
        if (!shouldPersist(progress, forced)) {
            return;
        }
        if (!executionManager.updateProgress(progress)) {
            throw new ServiceException(ServiceExceptionEnum.TASK_CLAIM_CONFLICT)
                .setDetailMessage("Progress update no longer owns the active claim");
        }
        lastPersistedCurrent = value(progress.getCurrent());
        lastPersistedTotal = value(progress.getTotal());
        lastPersistedRevision = revision;
        lastPersistedMessage = progress.getMessage();
        lastPersistedAt = progress.getReportedAt();
    }

    private boolean shouldPersist(BizTaskProgressDTO progress, boolean forced) {
        if (forced || !Objects.equals(lastPersistedMessage, progress.getMessage())) {
            return true;
        }
        LocalDateTime reportedAt = progress.getReportedAt();
        if (reportedAt == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_PROGRESS_INVALID)
                .setDetailMessage("Progress report time must not be null");
        }
        long elapsedMillis = Duration.between(lastPersistedAt, reportedAt).toMillis();
        if (elapsedMillis >= minIntervalMillis) {
            return true;
        }
        long current = value(progress.getCurrent());
        long total = value(progress.getTotal());
        if (total > 0 && lastPersistedTotal == 0) {
            return true;
        }
        if (total == 0 || lastPersistedTotal == 0) {
            return false;
        }
        double previousRatio = (double)lastPersistedCurrent / (double)lastPersistedTotal;
        double currentRatio = (double)current / (double)total;
        return Math.abs(currentRatio - previousRatio) >= ONE_PERCENT;
    }

    private long value(Long value) {
        if (value == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_PROGRESS_INVALID)
                .setDetailMessage("Progress values must not be null");
        }
        return value.longValue();
    }
}
