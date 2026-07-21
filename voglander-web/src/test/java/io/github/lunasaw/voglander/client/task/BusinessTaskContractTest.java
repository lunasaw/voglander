package io.github.lunasaw.voglander.client.task;

import com.alibaba.fastjson2.JSONObject;
import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.domain.task.TaskResultReference;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessTaskContractTest {

    @Test
    void taskConstantsExposeStableIdsLocksTopicsPermissionsAndLimits() {
        assertEquals("btask_", TaskConstant.TASK_ID_PREFIX);
        assertEquals("bexec_", TaskConstant.EXECUTION_ID_PREFIX);
        assertEquals("bevt_", TaskConstant.EVENT_ID_PREFIX);
        assertEquals("biz:task:schedule:", TaskConstant.SCHEDULE_LOCK_PREFIX);
        assertEquals("biz:task:execution:", TaskConstant.EXECUTION_LOCK_PREFIX);
        assertEquals("business.task.state", TaskConstant.SSE_TASK_STATE);
        assertEquals("business.task.progress", TaskConstant.SSE_TASK_PROGRESS);
        assertEquals("business.task.execution-state", TaskConstant.SSE_EXECUTION_STATE);
        assertEquals("Task:Query", TaskConstant.PERMISSION_QUERY);
        assertEquals("Task:Control", TaskConstant.PERMISSION_CONTROL);
        assertTrue(TaskConstant.DEFAULT_SCAN_BATCH > 0);
        assertTrue(TaskConstant.DEFAULT_CATCHUP_BATCH > 0);
        assertTrue(TaskConstant.DEFAULT_MAX_PLANNED_COUNT > 0);
        assertTrue(TaskConstant.DEFAULT_MAX_PAYLOAD_BYTES > 0);
        assertTrue(TaskConstant.DEFAULT_MAX_EVENT_DATA_BYTES > 0);
        assertTrue(TaskConstant.DEFAULT_LEASE_SECONDS > TaskConstant.DEFAULT_ALLOWED_DELAY_SECONDS);
    }

    @Test
    void createCommandOwnsAnImmutablePayloadSnapshot() {
        JSONObject payload = JSONObject.of("configId", "cfg-1", "options", JSONObject.of("timeoutSeconds", 15));
        TaskCreateCommand command = new TaskCreateCommand("IMAGE_COLLECTION", "capture", null, "ONCE",
            LocalDateTime.now(), null, null, payload, 1, null, "CAMERA", "cam-1", "USER", "7", null,
            "idem-1", 2);

        payload.put("configId", "mutated");
        payload.getJSONObject("options").put("timeoutSeconds", 99);
        JSONObject returned = command.payload();
        returned.put("secret", "must-not-leak-back");
        returned.getJSONObject("options").put("timeoutSeconds", 120);

        assertEquals("cfg-1", command.payload().getString("configId"));
        assertEquals(15, command.payload().getJSONObject("options").getIntValue("timeoutSeconds"));
        assertFalse(command.payload().containsKey("secret"));
    }

    @Test
    void resultOwnsSanitizedSummaryAndCompletionSnapshots() {
        JSONObject summary = JSONObject.of("assetId", "img-1", "metadata", JSONObject.of("width", 1920));
        JSONObject completion = JSONObject.of("storageKey", "internal-key", "metadata", JSONObject.of("height", 1080));
        TaskExecutionResult result = new TaskExecutionResult(new TaskResultReference("IMAGE_ASSET", "img-1"),
            summary, completion, null);

        summary.clear();
        completion.clear();
        result.resultSummary().put("assetId", "mutated");
        result.resultSummary().getJSONObject("metadata").put("width", 1);
        result.completionData().put("storageKey", "mutated");
        result.completionData().getJSONObject("metadata").put("height", 1);

        assertEquals("img-1", result.resultSummary().getString("assetId"));
        assertEquals(1920, result.resultSummary().getJSONObject("metadata").getIntValue("width"));
        assertEquals("internal-key", result.completionData().getString("storageKey"));
        assertEquals(1080, result.completionData().getJSONObject("metadata").getIntValue("height"));
        result.compensation().compensate();
    }

    @Test
    void capabilitiesRetryAndResultReferencesAreExplicit() {
        TaskCapabilities capabilities = new TaskCapabilities(true, true, true, true, false);
        assertTrue(capabilities.supportsPause());
        assertTrue(capabilities.supportsCancel());
        assertTrue(capabilities.supportsManualRetry());
        assertTrue(capabilities.supportsProgress());
        assertFalse(capabilities.supportsReschedule());

        RetryDecision retryable = RetryDecision.retryable("TIMEOUT", "temporary");
        assertTrue(retryable.isRetryable());
        assertEquals("TIMEOUT", retryable.failureCode());
        assertThrows(IllegalArgumentException.class, () -> RetryDecision.permanent(" ", "invalid"));
        assertThrows(IllegalArgumentException.class, () -> new TaskResultReference("IMAGE_ASSET", " "));
    }
}
