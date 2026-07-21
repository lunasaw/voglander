package io.github.lunasaw.voglander.client.service.task;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateContext;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;

/**
 * Stable SPI for durable business work.
 *
 * <p>Implementations must be idempotent under at-least-once execution. A Handler must not update the core task,
 * execution or event tables directly, and must not expose credentials, binary content, absolute paths or exception
 * stacks through payloads, summaries or failure messages.</p>
 */
public interface LongTaskHandler {
    /** Returns the stable, non-blank machine type used for registration and persistence. */
    String taskType();

    /** Returns the positive payload version accepted by this Handler implementation. */
    int payloadVersion();

    /** Declares control and progress capabilities; the returned value must not be {@code null}. */
    TaskCapabilities capabilities();

    /** Validates a trusted domain request before any task or execution fact is persisted. */
    void validate(TaskCreateContext context, JSONObject payload);

    /**
     * Executes one claimed attempt. External side effects must use stable idempotency facts from the context and all
     * acquired resources must be released when this method returns or throws.
     */
    TaskExecutionResult execute(LongTaskContext context, JSONObject payload) throws Exception;

    /** Converts a failure into a stable, sanitized retry decision without throwing. */
    RetryDecision classify(Throwable throwable, TaskAttemptContext context);

    /**
     * Returns an optional participant for idempotent, same-datasource domain writes in the authoritative completion
     * transaction. The participant must not perform network, file or media I/O.
     */
    default TaskCompletionParticipant completionParticipant() {
        return TaskCompletionParticipant.noop();
    }
}
