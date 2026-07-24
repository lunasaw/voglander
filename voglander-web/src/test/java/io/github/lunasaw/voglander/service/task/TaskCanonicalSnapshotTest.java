package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;

class TaskCanonicalSnapshotTest {

    @Test
    void canonicalCommandSortsPayloadAndTruncatesScheduleTimesToSeconds() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 22, 10, 11, 12, 123456789);
        JSONObject firstPayload = new JSONObject();
        firstPayload.put("z", 2);
        firstPayload.put("a", 1);
        JSONObject secondPayload = new JSONObject();
        secondPayload.put("a", 1);
        secondPayload.put("z", 2);

        TaskCreateCommand first = command("FIXED_RATE", start, firstPayload, 2);
        TaskCreateCommand second = command("FIXED_RATE", start.withNano(999999999), secondPayload, 99);

        assertEquals(TaskCanonicalSnapshot.fromCommand(first, "{\"z\":2,\"a\":1}", null, null).fingerprint(),
            TaskCanonicalSnapshot.fromCommand(second, "{\"a\":1,\"z\":2}", null, null).fingerprint());
    }

    @Test
    void acceptedTaskReconstructsSameSnapshotWithoutRuntimeFields() {
        TaskCreateCommand command = command("ONCE", null, JSONObject.of("field", "value"), 3);
        BizTaskDTO task = accepted(command, "{\"field\":\"value\"}");
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setMaxAttempts(3);
        String commandFingerprint = TaskCanonicalSnapshot.fromCommand(command, task.getPayload(), null, null)
            .fingerprint();

        task.setState("FAILED");
        task.setVersion(42);
        task.setSuccessCount(9);
        task.setNextPlanTime(LocalDateTime.now());

        assertEquals(commandFingerprint, TaskCanonicalSnapshot.fromAccepted(task, execution).fingerprint());
        execution.setMaxAttempts(4);
        assertNotEquals(commandFingerprint, TaskCanonicalSnapshot.fromAccepted(task, execution).fingerprint());
    }

    @Test
    void retryOriginIsPartOfCanonicalIdentity() {
        TaskCreateCommand command = command("ONCE", null, JSONObject.of("field", "value"), 3);
        String first = TaskCanonicalSnapshot.fromCommand(command, "{\"field\":\"value\"}", "task-a", "exec-a")
            .fingerprint();
        String second = TaskCanonicalSnapshot.fromCommand(command, "{\"field\":\"value\"}", "task-a", "exec-b")
            .fingerprint();
        assertNotEquals(first, second);
    }

    private static TaskCreateCommand command(String mode, LocalDateTime start, JSONObject payload,
        int maxAttempts) {
        LocalDateTime end = "FIXED_RATE".equals(mode) ? start.plusMinutes(10) : null;
        Long interval = "FIXED_RATE".equals(mode) ? 60L : null;
        return new TaskCreateCommand("REGISTERED", " task ", " description ", mode, start, end, interval,
            payload, 1, " biz ", "SUBJECT", "subject-1", "USER", "owner-1", "org-1", "idem", maxAttempts);
    }

    private static BizTaskDTO accepted(TaskCreateCommand command, String payload) {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskType(command.taskType());
        task.setTaskName(command.taskName());
        task.setDescription(command.description());
        task.setTaskMode(command.taskMode());
        task.setScheduleStartTime(command.scheduleStartTime());
        task.setScheduleEndTime(command.scheduleEndTime());
        task.setIntervalSeconds(command.intervalSeconds());
        task.setPayload(payload);
        task.setPayloadVersion(command.payloadVersion());
        task.setBizKey(command.bizKey());
        task.setSubjectType(command.subjectType());
        task.setSubjectId(command.subjectId());
        task.setOwnerType(command.ownerType());
        task.setOwnerId(command.ownerId());
        task.setOrganizationId(command.organizationId());
        return task;
    }
}
