package io.github.lunasaw.voglander.common.event;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal, database-authoritative business-task refresh hint.
 *
 * <p>This value intentionally contains identifiers and stable codes only. It is not a
 * replacement for querying the task tables and must never carry payloads, messages,
 * paths, secrets or exception details.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTaskSseEvent {

    private String topic;
    private String taskId;
    private String executionId;
    private String taskType;
    private String taskState;
    private String executionState;
    private String eventType;
    private String failureCode;
    private Long progressCurrent;
    private Long progressTotal;
    private Long progressRevision;
    private Integer scheduleVersion;
    private Integer attemptNo;
    private Long timestamp;

    /** Returns the allowlisted, null-free data sent to an SSE client. */
    public Map<String, Object> toData() {
        Map<String, Object> data = new LinkedHashMap<>();
        put(data, "taskId", taskId);
        put(data, "executionId", executionId);
        put(data, "taskType", taskType);
        put(data, "taskState", taskState);
        put(data, "executionState", executionState);
        put(data, "eventType", eventType);
        put(data, "failureCode", failureCode);
        put(data, "progressCurrent", progressCurrent);
        put(data, "progressTotal", progressTotal);
        put(data, "progressRevision", progressRevision);
        put(data, "scheduleVersion", scheduleVersion);
        put(data, "attemptNo", attemptNo);
        put(data, "timestamp", timestamp);
        return data;
    }

    private void put(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }
}
