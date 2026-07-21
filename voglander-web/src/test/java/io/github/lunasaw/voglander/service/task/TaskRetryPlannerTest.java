package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;

@DisplayName("Business-task retry planning")
class TaskRetryPlannerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 13, 0);

    private final TaskRetryPlanner planner = new TaskRetryPlanner(2, 30);

    @Test
    @DisplayName("可重试失败应使用有上限的指数退避")
    void nextAttemptTime_shouldUseBoundedExponentialBackoff() {
        RetryDecision retryable = RetryDecision.retryable("MEDIA_TIMEOUT", "media request timed out");

        assertEquals(NOW.plusSeconds(2), planner.nextAttemptTime(retryable, attempt(1, 8, null), NOW));
        assertEquals(NOW.plusSeconds(4), planner.nextAttemptTime(retryable, attempt(2, 8, null), NOW));
        assertEquals(NOW.plusSeconds(30), planner.nextAttemptTime(retryable, attempt(6, 8, null), NOW));
    }

    @Test
    @DisplayName("永久失败、次数耗尽或越过 deadline 时不得进入 retry-wait")
    void nextAttemptTime_shouldRejectPermanentExhaustedAndExpiredRetries() {
        RetryDecision retryable = RetryDecision.retryable("MEDIA_TIMEOUT", "media request timed out");

        assertNull(planner.nextAttemptTime(RetryDecision.permanent("INVALID", "invalid request"),
            attempt(1, 3, null), NOW));
        assertNull(planner.nextAttemptTime(retryable, attempt(3, 3, null), NOW));
        assertNull(planner.nextAttemptTime(retryable, attempt(1, 3, NOW.plusSeconds(1)), NOW));
        assertEquals(NOW.plusSeconds(2), planner.nextAttemptTime(retryable,
            attempt(1, 3, NOW.plusSeconds(2)), NOW));
    }

    private TaskAttemptContext attempt(int attempt, int maxAttempts, LocalDateTime deadlineAt) {
        return new TaskAttemptContext("btask_retry", "bexec_retry", attempt, maxAttempts, deadlineAt);
    }
}
