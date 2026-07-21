package io.github.lunasaw.voglander.manager.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCommandDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskProgressDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskStatisticsDTO;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;

@DisplayName("Durable business-task Manager DTO and Assembler contracts")
class BusinessTaskAssemblerTest {

    private static final List<String> TASK_QUERY_FIELDS = Arrays.asList(
        "taskId", "taskType", "state", "taskName", "ownerType", "ownerId", "organizationId", "subjectType",
        "subjectId", "bizKey", "createStartTime", "createEndTime", "scheduleStartTime", "scheduleEndTime",
        "sortField", "sortDirection");

    private static final List<String> EXECUTION_QUERY_FIELDS = Arrays.asList(
        "executionId", "taskId", "state", "retryable", "workerNode", "plannedStartTime", "plannedEndTime",
        "createStartTime", "createEndTime", "sortField", "sortDirection");

    @Test
    @DisplayName("task、execution、event DTO 应完整映射对应 DO")
    void coreDtos_shouldRoundTripEveryRepositoryField() throws Exception {
        assertRoundTrip(new BizTaskDO(), new BizTaskAssembler(), BizTaskDTO.class);
        assertRoundTrip(new BizTaskExecutionDO(), new BizTaskExecutionAssembler(), BizTaskExecutionDTO.class);
        assertRoundTrip(new BizTaskEventDO(), new BizTaskEventAssembler(), BizTaskEventDTO.class);
    }

    @Test
    @DisplayName("查询、进度、统计和命令 DTO 应提供稳定 Manager 契约")
    void supportingDtos_shouldExposeRequiredFields() throws Exception {
        assertFields(BizTaskQueryDTO.class, TASK_QUERY_FIELDS);
        assertFields(BizTaskExecutionQueryDTO.class, EXECUTION_QUERY_FIELDS);
        assertFields(BizTaskProgressDTO.class,
            Arrays.asList("taskId", "executionId", "claimToken", "current", "total", "message", "revision",
                "forcePersist", "reportedAt"));
        assertFields(BizTaskStatisticsDTO.class,
            Arrays.asList("scheduledCount", "runningCount", "pausedCount", "cancellingCount",
                "completedTodayCount", "failedCount"));
        assertFields(BizTaskCommandDTO.class,
            Arrays.asList("taskId", "executionId", "command", "actorType", "actorId", "expectedVersion",
                "scheduleStartTime", "scheduleEndTime", "intervalSeconds", "idempotencyKey", "reason",
                "requestedAt"));
    }

    @Test
    @DisplayName("payloadVersion 与安全 result/event summary 应在转换后保持正确")
    void safeMapping_shouldKeepVersionAndRemoveSensitiveJsonFields() {
        BizTaskDO task = new BizTaskDO();
        task.setPayloadVersion(7);
        task.setResultSummary("{\"assetId\":\"img_1\",\"secret\":\"zlm\",\"nested\":{\"token\":\"jwt\"}}");
        BizTaskDTO taskDTO = new BizTaskAssembler().doToSafeDto(task);
        assertEquals(7, taskDTO.getPayloadVersion());
        assertSafeJson(taskDTO.getResultSummary(), "assetId", "img_1");

        BizTaskExecutionDO execution = new BizTaskExecutionDO();
        execution.setResultSummary("{\"result\":\"ok\",\"storageKey\":\"images/private.jpg\"}");
        BizTaskExecutionDTO executionDTO = new BizTaskExecutionAssembler().doToSafeDto(execution);
        assertSafeJson(executionDTO.getResultSummary(), "result", "ok");

        BizTaskEventDO event = new BizTaskEventDO();
        event.setEventData("{\"phase\":\"stored\",\"absolutePath\":\"/tmp/private.jpg\",\"stackTrace\":\"boom\"}");
        BizTaskEventDTO eventDTO = new BizTaskEventAssembler().doToSafeDto(event);
        assertSafeJson(eventDTO.getEventData(), "phase", "stored");
    }

    @Test
    @DisplayName("Assembler 空值转换应稳定")
    void assemblers_shouldKeepNullContract() {
        assertNull(new BizTaskAssembler().doToDto(null));
        assertNull(new BizTaskAssembler().dtoToDo(null));
        assertNull(new BizTaskExecutionAssembler().doToDto(null));
        assertNull(new BizTaskExecutionAssembler().dtoToDo(null));
        assertNull(new BizTaskEventAssembler().doToDto(null));
        assertNull(new BizTaskEventAssembler().dtoToDo(null));
    }

    private void assertRoundTrip(Object dataObject, Object assembler, Class<?> dtoType) throws Exception {
        populate(dataObject);
        Method doToDto = assembler.getClass().getMethod("doToDto", dataObject.getClass());
        Object dto = doToDto.invoke(assembler, dataObject);
        assertNotNull(dto);
        assertEquals(dtoType, dto.getClass());
        assertSameFields(dataObject, dto);

        Method dtoToDo = assembler.getClass().getMethod("dtoToDo", dtoType);
        Object roundTrip = dtoToDo.invoke(assembler, dto);
        assertNotNull(roundTrip);
        assertSameFields(dataObject, roundTrip);
    }

    private void populate(Object target) throws IllegalAccessException {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            if (field.getType() == String.class) {
                field.set(target, field.getName() + "-value");
            } else if (field.getType() == Long.class) {
                field.set(target, 11L);
            } else if (field.getType() == Integer.class) {
                field.set(target, 7);
            } else if (field.getType() == Boolean.class) {
                field.set(target, true);
            } else if (field.getType() == LocalDateTime.class) {
                field.set(target, LocalDateTime.of(2026, 7, 14, 12, 30));
            }
        }
    }

    private void assertSameFields(Object expected, Object actual) throws Exception {
        for (Field expectedField : expected.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(expectedField.getModifiers())) {
                continue;
            }
            expectedField.setAccessible(true);
            Field actualField = actual.getClass().getDeclaredField(expectedField.getName());
            actualField.setAccessible(true);
            assertEquals(expectedField.get(expected), actualField.get(actual), expectedField.getName());
        }
    }

    private void assertFields(Class<?> type, List<String> expectedFields) throws Exception {
        for (String field : expectedFields) {
            assertNotNull(type.getDeclaredField(field), type.getSimpleName() + " 缺少字段 " + field);
        }
    }

    private void assertSafeJson(String json, String retainedKey, String retainedValue) {
        JSONObject object = JSON.parseObject(json);
        assertEquals(retainedValue, object.getString(retainedKey));
        String lower = json.toLowerCase();
        assertFalse(lower.contains("secret"));
        assertFalse(lower.contains("token"));
        assertFalse(lower.contains("storagekey"));
        assertFalse(lower.contains("absolutepath"));
        assertFalse(lower.contains("stacktrace"));
        assertTrue(json.length() < 8192);
    }
}
