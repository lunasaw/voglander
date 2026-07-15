package io.github.lunasaw.voglander.client.domain.task;

import java.time.LocalDateTime;

/** Immutable attempt facts supplied to Handler retry classification. */
public final class TaskAttemptContext {
    private final String taskId;
    private final String executionId;
    private final int attempt;
    private final int maxAttempts;
    private final LocalDateTime deadlineAt;

    public TaskAttemptContext(String taskId, String executionId, int attempt, int maxAttempts,
        LocalDateTime deadlineAt) {
        this.taskId = taskId;
        this.executionId = executionId;
        this.attempt = attempt;
        this.maxAttempts = maxAttempts;
        this.deadlineAt = deadlineAt;
    }

    public String taskId() { return taskId; }
    public String executionId() { return executionId; }
    public int attempt() { return attempt; }
    public int maxAttempts() { return maxAttempts; }
    public LocalDateTime deadlineAt() { return deadlineAt; }
}
