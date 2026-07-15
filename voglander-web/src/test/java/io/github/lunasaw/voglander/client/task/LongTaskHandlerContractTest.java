package io.github.lunasaw.voglander.client.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateContext;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.service.task.LongTaskContext;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reusable contract suite for every durable business-task Handler. */
abstract class LongTaskHandlerContractTest {

    protected abstract LongTaskHandler handler();

    protected abstract JSONObject validPayload();

    protected abstract JSONObject invalidPayload();

    protected abstract LongTaskContext executionContext();

    @Test
    void declaresStableTypeVersionAndCapabilities() {
        LongTaskHandler handler = handler();
        assertNotNull(handler.taskType());
        assertFalse(handler.taskType().isBlank());
        assertTrue(handler.taskType().matches("[A-Z][A-Z0-9_]*"));
        assertTrue(handler.payloadVersion() > 0);
        assertNotNull(handler.capabilities());
        assertNotNull(handler.completionParticipant());
    }

    @Test
    void validatesPayloadBeforePersistence() {
        TaskCreateContext context = new TaskCreateContext("USER", "7", null, "TEST_SUBJECT", "subject-1");
        assertDoesNotThrow(() -> handler().validate(context, validPayload()));
        assertThrows(RuntimeException.class, () -> handler().validate(context, invalidPayload()));
    }

    @Test
    void returnsStableSanitizedResult() throws Exception {
        TaskExecutionResult result = handler().execute(executionContext(), validPayload());
        assertNotNull(result);
        assertNotNull(result.resultReference());
        assertFalse(result.resultReference().type().isBlank());
        assertFalse(result.resultReference().id().isBlank());
        assertSanitized(result.resultSummary());
        assertSanitized(result.completionData());
    }

    @Test
    void classifiesFailuresWithoutSensitiveOutput() {
        RetryDecision decision = handler().classify(new IllegalStateException("test failure"),
            new TaskAttemptContext("btask_test", "bexec_test", 1, 2, LocalDateTime.now().plusMinutes(1)));
        assertNotNull(decision);
        assertNotNull(decision.failureCode());
        assertFalse(decision.failureCode().isBlank());
        assertSanitized(decision.failureMessage());
    }

    private void assertSanitized(JSONObject value) {
        assertSanitized(JSON.toJSONString(value));
    }

    private void assertSanitized(String value) {
        String normalized = value == null ? "" : value.toLowerCase();
        List.of("password", "secret", "bearer ", "base64", "/tmp/", "file:/", "http://", "https://")
            .forEach(forbidden -> assertFalse(normalized.contains(forbidden),
                () -> "Handler output contains sensitive token: " + forbidden));
    }
}
