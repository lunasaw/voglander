package io.github.lunasaw.voglander.service.task;

import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.common.enums.task.TaskFailureCodeEnum;

/** Normalizes unreliable Handler failure classifications into safe durable facts. */
final class TaskRetryDecisionNormalizer {

    private static final String UNKNOWN_FAILURE_MESSAGE = "Business task handler failed";

    private TaskRetryDecisionNormalizer() {
    }

    static RetryDecision normalize(LongTaskHandler handler, Throwable failure, TaskAttemptContext context) {
        try {
            RetryDecision decision = handler.classify(failure, context);
            if (decision != null) {
                return decision;
            }
        } catch (RuntimeException ignored) {
            // Classifier failures must not leak implementation details into durable task facts.
        }
        return RetryDecision.permanent(TaskFailureCodeEnum.SYSTEM_ERROR.name(), UNKNOWN_FAILURE_MESSAGE);
    }
}
