package io.github.lunasaw.voglander.client.service.task;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskCompletionContext;

/**
 * Same-datasource, idempotent domain write invoked inside the authoritative completion transaction.
 *
 * <p>Implementations may only persist domain facts through the same transaction manager. They must tolerate repeated
 * invocation using stable task/execution IDs and must not perform network, file, media or other external I/O.</p>
 */
@FunctionalInterface
public interface TaskCompletionParticipant {
    /** Persists or resolves the domain result for this completion attempt. */
    void complete(TaskCompletionContext context, JSONObject completionData);

    static TaskCompletionParticipant noop() {
        return new TaskCompletionParticipant() {
            @Override
            public void complete(TaskCompletionContext context, JSONObject completionData) {
            }
        };
    }
}
