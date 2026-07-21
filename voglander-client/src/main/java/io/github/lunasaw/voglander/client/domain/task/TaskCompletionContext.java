package io.github.lunasaw.voglander.client.domain.task;

/** IDs and attempt facts exposed to same-datasource completion participants. */
public final class TaskCompletionContext {
    private final String taskId;
    private final String executionId;
    private final int attempt;

    public TaskCompletionContext(String taskId, String executionId, int attempt) {
        this.taskId = taskId;
        this.executionId = executionId;
        this.attempt = attempt;
    }

    public String taskId() { return taskId; }
    public String executionId() { return executionId; }
    public int attempt() { return attempt; }
}
