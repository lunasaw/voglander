package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.common.enums.task.TaskFailureCodeEnum;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Business-task retry decision normalization")
class TaskRetryDecisionNormalizerTest {

    @Mock
    private LongTaskHandler handler;

    @Test
    @DisplayName("空的 Handler 分类应降级为不含异常明细的稳定失败")
    void normalize_shouldFallbackForMissingHandlerClassification() {
        IllegalStateException failure = new IllegalStateException("password=secret-token");
        TaskAttemptContext context = attemptContext();

        RetryDecision decision = TaskRetryDecisionNormalizer.normalize(handler, failure, context);

        assertFalse(decision.isRetryable());
        assertEquals(TaskFailureCodeEnum.SYSTEM_ERROR.name(), decision.failureCode());
        assertEquals("Business task handler failed", decision.failureMessage());
        assertFalse(decision.failureMessage().contains("secret-token"));
    }

    @Test
    @DisplayName("分类器自身异常时也必须降级为安全失败")
    void normalize_shouldFallbackWhenHandlerClassificationThrows() {
        IllegalStateException failure = new IllegalStateException("storage key=/private/key");
        when(handler.classify(any(), any())).thenThrow(new IllegalArgumentException("classification failed"));

        RetryDecision decision = TaskRetryDecisionNormalizer.normalize(handler, failure, attemptContext());

        assertFalse(decision.isRetryable());
        assertEquals(TaskFailureCodeEnum.SYSTEM_ERROR.name(), decision.failureCode());
        assertEquals("Business task handler failed", decision.failureMessage());
    }

    @Test
    @DisplayName("有效的 Handler 分类应保留其稳定业务决定")
    void normalize_shouldKeepValidHandlerClassification() {
        IllegalStateException failure = new IllegalStateException("temporary timeout");
        TaskAttemptContext context = attemptContext();
        RetryDecision classified = RetryDecision.retryable("MEDIA_TIMEOUT", "media request timed out");
        when(handler.classify(failure, context)).thenReturn(classified);

        RetryDecision decision = TaskRetryDecisionNormalizer.normalize(handler, failure, context);

        assertSame(classified, decision);
    }

    private TaskAttemptContext attemptContext() {
        return new TaskAttemptContext("btask_retry", "bexec_retry", 1, 3,
            LocalDateTime.of(2026, 7, 15, 12, 30));
    }
}
