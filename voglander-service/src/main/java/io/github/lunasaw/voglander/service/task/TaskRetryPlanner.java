package io.github.lunasaw.voglander.service.task;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Objects;

import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;

/** Calculates bounded retry timing without changing durable execution facts. */
final class TaskRetryPlanner {

    private final int initialDelaySeconds;
    private final int maxDelaySeconds;

    TaskRetryPlanner(int initialDelaySeconds, int maxDelaySeconds) {
        if (initialDelaySeconds <= 0 || maxDelaySeconds <= 0 || initialDelaySeconds > maxDelaySeconds) {
            throw new IllegalArgumentException("Business-task retry delays are invalid");
        }
        this.initialDelaySeconds = initialDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
    }

    LocalDateTime nextAttemptTime(RetryDecision decision, TaskAttemptContext context, LocalDateTime now) {
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(now, "now");
        if (!decision.isRetryable() || context.attempt() <= 0 || context.maxAttempts() <= context.attempt()) {
            return null;
        }
        try {
            LocalDateTime nextAttemptTime = now.plusSeconds(delaySeconds(context.attempt()));
            return context.deadlineAt() == null || !nextAttemptTime.isAfter(context.deadlineAt())
                ? nextAttemptTime : null;
        } catch (DateTimeException ignored) {
            return null;
        }
    }

    private long delaySeconds(int attempt) {
        long delay = initialDelaySeconds;
        for (int index = 1; index < attempt && delay < maxDelaySeconds; index++) {
            delay = delay > maxDelaySeconds / 2 ? maxDelaySeconds : delay * 2;
        }
        return Math.min(delay, (long)maxDelaySeconds);
    }
}
