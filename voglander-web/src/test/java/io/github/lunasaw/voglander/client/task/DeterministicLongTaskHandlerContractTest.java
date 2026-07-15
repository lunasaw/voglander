package io.github.lunasaw.voglander.client.task;

import com.alibaba.fastjson2.JSONObject;
import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateContext;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.domain.task.TaskResultReference;
import io.github.lunasaw.voglander.client.service.task.LongTaskContext;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.client.service.task.TaskCancellationToken;

class DeterministicLongTaskHandlerContractTest extends LongTaskHandlerContractTest {

    @Override
    protected LongTaskHandler handler() {
        return new LongTaskHandler() {
            @Override public String taskType() { return "TEST"; }
            @Override public int payloadVersion() { return 1; }
            @Override public TaskCapabilities capabilities() {
                return new TaskCapabilities(true, true, true, true, false);
            }
            @Override public void validate(TaskCreateContext context, JSONObject payload) {
                if (payload == null || payload.getString("value") == null) {
                    throw new IllegalArgumentException("value is required");
                }
            }
            @Override public TaskExecutionResult execute(LongTaskContext context, JSONObject payload) {
                return TaskExecutionResult.success(new TaskResultReference("TEST_RESULT", context.executionId()),
                    JSONObject.of("processed", 1));
            }
            @Override public RetryDecision classify(Throwable throwable, TaskAttemptContext context) {
                return RetryDecision.permanent("TEST_FAILURE", "test failure");
            }
        };
    }

    @Override
    protected JSONObject validPayload() {
        return JSONObject.of("value", "safe");
    }

    @Override
    protected JSONObject invalidPayload() {
        return new JSONObject();
    }

    @Override
    protected LongTaskContext executionContext() {
        return new LongTaskContext() {
            @Override public String taskId() { return "btask_test"; }
            @Override public String executionId() { return "bexec_test"; }
            @Override public int attempt() { return 1; }
            @Override public TaskCancellationToken cancellationToken() {
                return new TaskCancellationToken() {
                    @Override public boolean isCancellationRequested() { return false; }
                    @Override public void throwIfCancellationRequested() { }
                };
            }
            @Override public void reportProgress(long current, long total, String message) { }
            @Override public boolean heartbeat() { return true; }
        };
    }
}
