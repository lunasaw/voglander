package io.github.lunasaw.voglander.client.domain.task;

/** Immutable action capabilities declared by a task Handler. */
public final class TaskCapabilities {
    private final boolean pause;
    private final boolean cancel;
    private final boolean manualRetry;
    private final boolean progress;
    private final boolean reschedule;

    public TaskCapabilities(boolean pause, boolean cancel, boolean manualRetry, boolean progress,
        boolean reschedule) {
        this.pause = pause;
        this.cancel = cancel;
        this.manualRetry = manualRetry;
        this.progress = progress;
        this.reschedule = reschedule;
    }

    public static TaskCapabilities none() {
        return new TaskCapabilities(false, false, false, false, false);
    }

    public boolean supportsPause() { return pause; }
    public boolean supportsCancel() { return cancel; }
    public boolean supportsManualRetry() { return manualRetry; }
    public boolean supportsProgress() { return progress; }
    public boolean supportsReschedule() { return reschedule; }
}
