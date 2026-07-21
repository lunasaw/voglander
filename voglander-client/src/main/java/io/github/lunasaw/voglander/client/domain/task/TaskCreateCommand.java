package io.github.lunasaw.voglander.client.domain.task;

import java.time.LocalDateTime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** Trusted internal task creation command. It is never bound directly from a generic Web request. */
public final class TaskCreateCommand {
    private final String taskType;
    private final String taskName;
    private final String description;
    private final String taskMode;
    private final LocalDateTime scheduleStartTime;
    private final LocalDateTime scheduleEndTime;
    private final Long intervalSeconds;
    private final JSONObject payload;
    private final int payloadVersion;
    private final String bizKey;
    private final String subjectType;
    private final String subjectId;
    private final String ownerType;
    private final String ownerId;
    private final String organizationId;
    private final String idempotencyKey;
    private final int maxAttempts;

    public TaskCreateCommand(String taskType, String taskName, String description, String taskMode,
        LocalDateTime scheduleStartTime, LocalDateTime scheduleEndTime, Long intervalSeconds, JSONObject payload,
        int payloadVersion, String bizKey, String subjectType, String subjectId, String ownerType, String ownerId,
        String organizationId, String idempotencyKey, int maxAttempts) {
        this.taskType = taskType;
        this.taskName = taskName;
        this.description = description;
        this.taskMode = taskMode;
        this.scheduleStartTime = scheduleStartTime;
        this.scheduleEndTime = scheduleEndTime;
        this.intervalSeconds = intervalSeconds;
        this.payload = deepCopy(payload);
        this.payloadVersion = payloadVersion;
        this.bizKey = bizKey;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.organizationId = organizationId;
        this.idempotencyKey = idempotencyKey;
        this.maxAttempts = maxAttempts;
    }

    public String taskType() { return taskType; }
    public String taskName() { return taskName; }
    public String description() { return description; }
    public String taskMode() { return taskMode; }
    public LocalDateTime scheduleStartTime() { return scheduleStartTime; }
    public LocalDateTime scheduleEndTime() { return scheduleEndTime; }
    public Long intervalSeconds() { return intervalSeconds; }
    public JSONObject payload() { return deepCopy(payload); }
    public int payloadVersion() { return payloadVersion; }
    public String bizKey() { return bizKey; }
    public String subjectType() { return subjectType; }
    public String subjectId() { return subjectId; }
    public String ownerType() { return ownerType; }
    public String ownerId() { return ownerId; }
    public String organizationId() { return organizationId; }
    public String idempotencyKey() { return idempotencyKey; }
    public int maxAttempts() { return maxAttempts; }

    private static JSONObject deepCopy(JSONObject source) {
        return source == null ? new JSONObject() : JSON.parseObject(JSON.toJSONString(source));
    }
}
