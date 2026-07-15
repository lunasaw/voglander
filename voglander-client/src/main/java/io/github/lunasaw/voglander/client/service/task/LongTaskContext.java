package io.github.lunasaw.voglander.client.service.task;

/**
 * Controlled view of one successfully claimed execution attempt.
 *
 * <p>Handlers never update core persistence directly. Progress must be non-negative and monotonic, heartbeat is bound
 * to the active claim token, and cancellation is cooperative.</p>
 */
public interface LongTaskContext {
    /** Stable task business ID. */
    String taskId();

    /** Stable execution business ID shared by automatic attempts. */
    String executionId();

    /** One-based attempt number for the current claim. */
    int attempt();

    /** Cooperative cancellation token; implementations must poll it between external stages. */
    TaskCancellationToken cancellationToken();

    /** Reports monotonic progress; total {@code 0} represents an indeterminate operation. */
    void reportProgress(long current, long total, String message);

    /** Renews the active lease and returns {@code false} when this worker no longer owns the claim. */
    boolean heartbeat();
}
