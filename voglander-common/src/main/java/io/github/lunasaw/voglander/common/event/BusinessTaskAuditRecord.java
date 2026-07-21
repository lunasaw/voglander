package io.github.lunasaw.voglander.common.event;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Structured control-command audit fact with an intentionally redacted schema. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTaskAuditRecord {

    private boolean accepted;
    private String traceId;
    private String actorType;
    private String actorId;
    private String taskId;
    private String executionId;
    private String command;
    private String previousState;
    private String currentState;
    private String resultCode;
    private long timestamp;

    /** Converts to the exact structured log shape; no payload/reason/stack fields exist. */
    public Map<String, Object> toStructuredData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accepted", accepted);
        put(data, "traceId", traceId);
        put(data, "actorType", actorType);
        put(data, "actorId", actorId);
        put(data, "taskId", taskId);
        put(data, "executionId", executionId);
        put(data, "command", command);
        put(data, "previousState", previousState);
        put(data, "currentState", currentState);
        put(data, "resultCode", resultCode);
        data.put("timestamp", timestamp);
        return data;
    }

    private void put(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }
}
