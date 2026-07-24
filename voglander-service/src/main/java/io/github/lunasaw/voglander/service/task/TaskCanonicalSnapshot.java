package io.github.lunasaw.voglander.service.task;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.service.idempotency.CanonicalJsonFingerprint;

/** Canonical task command reconstructed from stable persisted facts only. */
final class TaskCanonicalSnapshot {

    private final Map<String, Object> fields;

    private TaskCanonicalSnapshot(Map<String, Object> fields) {
        this.fields = fields;
    }

    static TaskCanonicalSnapshot fromCommand(TaskCreateCommand command, String payloadSnapshot,
        String originTaskId, String originExecutionId) {
        return create(command.taskType(), command.taskName(), command.description(), command.taskMode(),
            command.scheduleStartTime(), command.scheduleEndTime(), command.intervalSeconds(),
            command.payloadVersion(), payloadSnapshot, command.bizKey(), command.subjectType(), command.subjectId(),
            command.ownerType(), command.ownerId(), command.organizationId(), originTaskId, originExecutionId,
            command.maxAttempts());
    }

    static TaskCanonicalSnapshot fromAccepted(BizTaskDTO task, BizTaskExecutionDTO firstExecution) {
        int maxAttempts = firstExecution == null || firstExecution.getMaxAttempts() == null
            ? 0 : firstExecution.getMaxAttempts();
        return create(task.getTaskType(), task.getTaskName(), task.getDescription(), task.getTaskMode(),
            task.getScheduleStartTime(), task.getScheduleEndTime(), task.getIntervalSeconds(),
            task.getPayloadVersion() == null ? 0 : task.getPayloadVersion(), task.getPayload(), task.getBizKey(),
            task.getSubjectType(), task.getSubjectId(), task.getOwnerType(), task.getOwnerId(),
            task.getOrganizationId(), task.getOriginTaskId(), task.getOriginExecutionId(), maxAttempts);
    }

    String fingerprint() {
        return CanonicalJsonFingerprint.sha256(fields);
    }

    private static TaskCanonicalSnapshot create(String taskType, String taskName, String description,
        String taskMode, LocalDateTime scheduleStartTime, LocalDateTime scheduleEndTime, Long intervalSeconds,
        int payloadVersion, String payloadSnapshot, String bizKey, String subjectType, String subjectId,
        String ownerType, String ownerId, String organizationId, String originTaskId, String originExecutionId,
        int maxAttempts) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("bizKey", normalized(bizKey));
        values.put("description", normalized(description));
        values.put("intervalSeconds", intervalSeconds);
        values.put("organizationId", normalized(organizationId));
        values.put("originExecutionId", normalized(originExecutionId));
        values.put("originTaskId", normalized(originTaskId));
        values.put("ownerId", normalized(ownerId));
        values.put("ownerType", normalized(ownerType));
        values.put("payload", canonicalPayload(payloadSnapshot));
        values.put("payloadVersion", payloadVersion);
        values.put("scheduleEndTime", time(scheduleEndTime));
        values.put("scheduleStartTime", time(scheduleStartTime));
        values.put("subjectId", normalized(subjectId));
        values.put("subjectType", normalized(subjectType));
        values.put("taskMode", normalized(taskMode));
        values.put("taskName", normalized(taskName));
        values.put("taskType", normalized(taskType));
        if ("ONCE".equals(taskMode)) {
            values.put("maxAttempts", maxAttempts);
        }
        return new TaskCanonicalSnapshot(values);
    }

    private static JSONObject canonicalPayload(String payloadSnapshot) {
        if (payloadSnapshot == null || payloadSnapshot.trim().isEmpty()) {
            return new JSONObject();
        }
        return JSON.parseObject(payloadSnapshot);
    }

    private static String normalized(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private static String time(LocalDateTime value) {
        return value == null ? null : value.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
