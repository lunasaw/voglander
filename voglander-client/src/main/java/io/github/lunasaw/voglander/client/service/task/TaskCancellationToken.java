package io.github.lunasaw.voglander.client.service.task;

/** Cooperative cancellation signal; it never terminates a Handler thread forcibly. */
public interface TaskCancellationToken {
    /** Returns whether cancellation has been requested for the owning task/execution. */
    boolean isCancellationRequested();

    /** Throws the engine-defined cancellation exception when cancellation has been requested. */
    void throwIfCancellationRequested();
}
