package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateContext;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.service.task.LongTaskContext;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.common.exception.ServiceException;

class LongTaskHandlerRegistryTest {

    @Test
    void validHandlerIsResolvedByStableType() {
        LongTaskHandler handler = handler("TEST", 1);
        LongTaskHandlerRegistry registry = new LongTaskHandlerRegistry(Collections.singletonList(handler));
        assertSame(handler, registry.require("TEST"));
        assertSame(handler, registry.require("TEST", 1));
        assertEquals(1, registry.all().size());
    }

    @Test
    void duplicateBlankAndInvalidVersionAreRejectedAtStartup() {
        assertThrows(IllegalStateException.class,
            () -> new LongTaskHandlerRegistry(Arrays.asList(handler("TEST", 1), handler("TEST", 1))));
        assertThrows(IllegalStateException.class,
            () -> new LongTaskHandlerRegistry(Collections.singletonList(handler(" ", 1))));
        assertThrows(IllegalStateException.class,
            () -> new LongTaskHandlerRegistry(Collections.singletonList(handler("TEST", 0))));
        assertThrows(IllegalStateException.class,
            () -> new LongTaskHandlerRegistry(Collections.singletonList(handlerWithNullCapabilities())));
    }

    @Test
    void unsupportedPayloadVersionIsRejectedBeforeExecution() {
        LongTaskHandlerRegistry registry =
            new LongTaskHandlerRegistry(Collections.singletonList(handler("TEST", 1)));

        ServiceException error = assertThrows(ServiceException.class, () -> registry.require("TEST", 2));
        assertEquals(720007, error.getCode());
    }

    @Test
    void unknownTypeIsRejectedBeforePersistence() {
        LongTaskHandlerRegistry registry = new LongTaskHandlerRegistry(Collections.emptyList());
        assertThrows(ServiceException.class, () -> registry.require("UNKNOWN"));
    }

    private LongTaskHandler handler(final String type, final int version) {
        return new LongTaskHandler() {
            @Override public String taskType() { return type; }
            @Override public int payloadVersion() { return version; }
            @Override public TaskCapabilities capabilities() { return TaskCapabilities.none(); }
            @Override public void validate(TaskCreateContext context, JSONObject payload) { }
            @Override public TaskExecutionResult execute(LongTaskContext context, JSONObject payload) { return null; }
            @Override public RetryDecision classify(Throwable throwable, TaskAttemptContext context) {
                return RetryDecision.permanent("TEST", "test");
            }
        };
    }

    private LongTaskHandler handlerWithNullCapabilities() {
        return new LongTaskHandler() {
            @Override public String taskType() { return "TEST"; }
            @Override public int payloadVersion() { return 1; }
            @Override public TaskCapabilities capabilities() { return null; }
            @Override public void validate(TaskCreateContext context, JSONObject payload) { }
            @Override public TaskExecutionResult execute(LongTaskContext context, JSONObject payload) { return null; }
            @Override public RetryDecision classify(Throwable throwable, TaskAttemptContext context) {
                return RetryDecision.permanent("TEST", "test");
            }
        };
    }
}
