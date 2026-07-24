package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateContext;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCreateResultDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.service.idempotency.IdempotencyMetrics;

@DisplayName("BizTaskCreateService trusted internal creation gate")
class BizTaskCreateServiceTest {

    @Test
    @DisplayName("未知 taskType 在只读重放查询后拒绝且不创建任务")
    void create_shouldRejectUnregisteredHandlerBeforePersistence() {
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager);
        TaskCreateCommand command = onceCommand("UNKNOWN");
        when(registry.require("UNKNOWN", 1))
            .thenThrow(new ServiceException(ServiceExceptionEnum.TASK_TYPE_UNREGISTERED));

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(command));

        assertEquals(ServiceExceptionEnum.TASK_TYPE_UNREGISTERED.getCode(), error.getCode());
        verify(registry).require("UNKNOWN", 1);
        verify(manager).findCreateResultByIdempotency("USER", "owner-create-test", "UNKNOWN", "idem-create");
        verify(manager, never()).create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class));
    }

    @Test
    @DisplayName("已注册 Handler 应先校验领域 payload 再交给 Manager 原子创建")
    void create_shouldValidateRegisteredHandlerAndDelegateInternalFacts() {
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        LongTaskHandler handler = mock(LongTaskHandler.class);
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager, Clock.systemDefaultZone(), metrics);
        TaskCreateCommand command = onceCommand("REGISTERED");
        when(registry.require("REGISTERED", 1)).thenReturn(handler);
        when(manager.create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class)))
            .thenAnswer(invocation -> new BizTaskCreateResultDTO(true, invocation.getArgument(0),
                invocation.getArgument(1)));

        BizTaskDTO accepted = service.create(command);

        ArgumentCaptor<TaskCreateContext> context = ArgumentCaptor.forClass(TaskCreateContext.class);
        ArgumentCaptor<JSONObject> payload = ArgumentCaptor.forClass(JSONObject.class);
        verify(handler).validate(context.capture(), payload.capture());
        assertEquals("USER", context.getValue().ownerType());
        assertEquals("owner-create-test", context.getValue().ownerId());
        assertEquals("value", payload.getValue().getString("field"));
        ArgumentCaptor<BizTaskDTO> task = ArgumentCaptor.forClass(BizTaskDTO.class);
        ArgumentCaptor<BizTaskExecutionDTO> execution = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(manager).create(task.capture(), execution.capture());
        assertEquals("REGISTERED", task.getValue().getTaskType());
        assertEquals("ONCE", task.getValue().getTaskMode());
        assertEquals("RUNNING", task.getValue().getState());
        assertEquals(task.getValue().getTaskId(), execution.getValue().getTaskId());
        assertEquals("PENDING", execution.getValue().getState());
        assertNotNull(accepted.getTaskId());
        verify(metrics).record("CREATED", null);
    }

    @Test
    @DisplayName("相同 canonical command 应在当前 Handler 约束前重放首次任务")
    void create_shouldReplayBeforeCurrentHandlerValidation() {
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager, Clock.systemDefaultZone(), metrics);
        TaskCreateCommand command = onceCommand("REGISTERED");
        BizTaskDTO original = acceptedTask(command, "btask_original");
        BizTaskExecutionDTO originalExecution = acceptedExecution(original.getTaskId(), 2);
        when(manager.findCreateResultByIdempotency("USER", "owner-create-test", "REGISTERED", "idem-create"))
            .thenReturn(new BizTaskCreateResultDTO(false, original, originalExecution));

        BizTaskDTO replay = service.create(command);

        assertSame(original, replay);
        verify(manager).findCreateResultByIdempotency("USER", "owner-create-test", "REGISTERED", "idem-create");
        verifyNoInteractions(registry);
        verify(manager, never()).create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class));
        verify(metrics).record("REPLAYED", null);
    }

    @Test
    @DisplayName("相同 identity 的不同 canonical command 应返回稳定幂等冲突")
    void create_shouldRejectReusedKeyWithDifferentCanonicalCommand() {
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        LongTaskHandler handler = mock(LongTaskHandler.class);
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager, Clock.systemDefaultZone(), metrics);
        TaskCreateCommand command = onceCommand("REGISTERED");
        BizTaskDTO original = acceptedTask(command, "btask_original");
        original.setTaskName("different task name");
        when(registry.require("REGISTERED", 1)).thenReturn(handler);
        when(manager.findCreateResultByIdempotency("USER", "owner-create-test", "REGISTERED", "idem-create"))
            .thenReturn(new BizTaskCreateResultDTO(false, original, acceptedExecution(original.getTaskId(), 2)));

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(command));

        assertEquals(ServiceExceptionEnum.IDEMPOTENCY_KEY_REUSED.getCode(), error.getCode());
        verify(manager, never()).create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class));
        verify(metrics).record("CONFLICT", "600007");
    }

    @Test
    @DisplayName("payload 应按 UTF-8 字节数而非字符数执行硬上限")
    void create_shouldRejectPayloadLargerThanUtf8ByteLimit() {
        JSONObject payload = new JSONObject();
        char[] characters = new char[TaskConstant.DEFAULT_MAX_PAYLOAD_BYTES / 2];
        Arrays.fill(characters, '图');
        payload.put("content", new String(characters));

        assertPayloadRejected(payload, 1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("prohibitedPayloads")
    @DisplayName("payload 应递归拒绝敏感键、Base64 内容和绝对路径")
    void create_shouldRejectProhibitedPayloadContent(String description, JSONObject payload) {
        assertPayloadRejected(payload, 1);
    }

    @Test
    @DisplayName("payloadVersion 必须为正数且与已注册 Handler 完全匹配")
    void create_shouldRejectUnsupportedPayloadVersion() {
        LongTaskHandler handler = mock(LongTaskHandler.class);
        when(handler.taskType()).thenReturn("REGISTERED");
        when(handler.payloadVersion()).thenReturn(1);
        when(handler.capabilities()).thenReturn(TaskCapabilities.none());
        LongTaskHandlerRegistry registry = new LongTaskHandlerRegistry(Arrays.asList(handler));
        BizTaskManager manager = mock(BizTaskManager.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager);
        TaskCreateCommand command = command("REGISTERED", new JSONObject(), 2);

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(command));

        assertEquals(ServiceExceptionEnum.TASK_PAYLOAD_INVALID.getCode(), error.getCode());
        verify(manager).findCreateResultByIdempotency("USER", "owner-create-test", "REGISTERED", "idem-create");
        verify(manager, never()).create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class));
    }

    @Test
    @DisplayName("Handler 和调用方修改对象均不得改变持久化 payload 快照")
    void create_shouldPersistImmutablePayloadSnapshot() {
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        LongTaskHandler handler = mock(LongTaskHandler.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager);
        JSONObject nested = new JSONObject();
        nested.put("field", "original");
        JSONObject source = new JSONObject();
        source.put("nested", nested);
        TaskCreateCommand command = command("REGISTERED", source, 1);
        source.getJSONObject("nested").put("field", "caller-mutated");
        when(registry.require("REGISTERED", 1)).thenReturn(handler);
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.<JSONObject>getArgument(1).getJSONObject("nested").put("field", "handler-mutated");
            return null;
        }).when(handler).validate(any(TaskCreateContext.class), any(JSONObject.class));
        when(manager.create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class)))
            .thenAnswer(invocation -> new BizTaskCreateResultDTO(true, invocation.getArgument(0),
                invocation.getArgument(1)));

        service.create(command);
        JSONObject getterCopy = command.payload();
        getterCopy.getJSONObject("nested").put("field", "getter-mutated");

        ArgumentCaptor<BizTaskDTO> task = ArgumentCaptor.forClass(BizTaskDTO.class);
        verify(manager).create(task.capture(), any(BizTaskExecutionDTO.class));
        JSONObject persisted = JSON.parseObject(task.getValue().getPayload());
        assertEquals("original", persisted.getJSONObject("nested").getString("field"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validSchedulePlans")
    @DisplayName("ONCE、AT_TIME 和 FIXED_RATE 应计算正确的 inclusive plannedCount")
    void create_shouldCalculatePlannedCount(String description, String mode, LocalDateTime start,
        LocalDateTime end, Long intervalSeconds, int expectedCount) {
        BizTaskDTO accepted = createAccepted(command("REGISTERED", mode, start, end, intervalSeconds,
            new JSONObject(), 1));

        assertEquals(expectedCount, accepted.getPlannedCount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSchedulePlans")
    @DisplayName("不完整、反向或无法安全推进的计划应返回 TASK_SCHEDULE_INVALID")
    void create_shouldRejectInvalidSchedule(String description, String mode, LocalDateTime start,
        LocalDateTime end, Long intervalSeconds) {
        assertScheduleRejected(command("REGISTERED", mode, start, end, intervalSeconds, new JSONObject(), 1),
            ServiceExceptionEnum.TASK_SCHEDULE_INVALID);
    }

    @Test
    @DisplayName("FIXED_RATE 超过核心最大时长应返回 TASK_LIMIT_EXCEEDED")
    void create_shouldRejectScheduleBeyondMaximumDuration() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        assertScheduleRejected(command("REGISTERED", "FIXED_RATE", start, start.plusDays(366), 86400L,
            new JSONObject(), 1), ServiceExceptionEnum.TASK_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("FIXED_RATE inclusive 计划数超过上限应返回 TASK_LIMIT_EXCEEDED")
    void create_shouldRejectScheduleBeyondMaximumCount() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        assertScheduleRejected(command("REGISTERED", "FIXED_RATE", start,
            start.plusSeconds(TaskConstant.DEFAULT_MAX_PLANNED_COUNT), 1L, new JSONObject(), 1),
            ServiceExceptionEnum.TASK_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("ONCE 应以注入时钟的当前时刻原子持久化 task 和首个 PENDING execution")
    void create_shouldPersistOnceExecutionAtCurrentTime() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-15T02:03:04Z"), ZoneOffset.UTC);
        LocalDateTime expectedNow = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        LongTaskHandler handler = mock(LongTaskHandler.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager, clock);
        when(registry.require("REGISTERED", 1)).thenReturn(handler);
        when(manager.create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class)))
            .thenAnswer(invocation -> new BizTaskCreateResultDTO(true, invocation.getArgument(0),
                invocation.getArgument(1)));

        service.create(command("REGISTERED", "ONCE", null, null, null, new JSONObject(), 1));

        ArgumentCaptor<BizTaskDTO> task = ArgumentCaptor.forClass(BizTaskDTO.class);
        ArgumentCaptor<BizTaskExecutionDTO> execution = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(manager).create(task.capture(), execution.capture());
        assertEquals("RUNNING", task.getValue().getState());
        assertEquals(1, task.getValue().getPlannedCount());
        assertEquals(null, task.getValue().getNextPlanTime());
        assertEquals(expectedNow, task.getValue().getCreateTime());
        assertEquals("PENDING", execution.getValue().getState());
        assertEquals(expectedNow, execution.getValue().getPlannedAt());
        assertEquals(task.getValue().getTaskId(), execution.getValue().getTaskId());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scheduledPersistencePlans")
    @DisplayName("AT_TIME/FIXED_RATE 应只持久化 cursor，不应提前创建 execution")
    void create_shouldPersistScheduledCursorWithoutEarlyExecution(String description, String mode,
        LocalDateTime start, LocalDateTime end, Long intervalSeconds, int plannedCount) {
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        LongTaskHandler handler = mock(LongTaskHandler.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager);
        when(registry.require("REGISTERED", 1)).thenReturn(handler);
        when(manager.create(any(BizTaskDTO.class), isNull())).thenAnswer(invocation ->
            new BizTaskCreateResultDTO(true, invocation.getArgument(0), null));

        BizTaskDTO accepted = service.create(command("REGISTERED", mode, start, end, intervalSeconds,
            new JSONObject(), 1));

        assertEquals("SCHEDULED", accepted.getState());
        assertEquals(start, accepted.getNextPlanTime());
        assertEquals(start, accepted.getScheduleStartTime());
        assertEquals(end, accepted.getScheduleEndTime());
        assertEquals(intervalSeconds, accepted.getIntervalSeconds());
        assertEquals(plannedCount, accepted.getPlannedCount());
        verify(manager).create(any(BizTaskDTO.class), isNull());
    }

    private BizTaskDTO createAccepted(TaskCreateCommand command) {
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        LongTaskHandler handler = mock(LongTaskHandler.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager);
        when(registry.require("REGISTERED", 1)).thenReturn(handler);
        when(manager.create(any(BizTaskDTO.class), any())).thenAnswer(invocation ->
            new BizTaskCreateResultDTO(true, invocation.getArgument(0), invocation.getArgument(1)));
        return service.create(command);
    }

    private static BizTaskDTO acceptedTask(TaskCreateCommand command, String taskId) {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId(taskId);
        task.setTaskType(command.taskType());
        task.setTaskName(command.taskName());
        task.setDescription(command.description());
        task.setTaskMode(command.taskMode());
        task.setScheduleStartTime(command.scheduleStartTime());
        task.setScheduleEndTime(command.scheduleEndTime());
        task.setIntervalSeconds(command.intervalSeconds());
        task.setPayload(JSON.toJSONString(command.payload()));
        task.setPayloadVersion(command.payloadVersion());
        task.setBizKey(command.bizKey());
        task.setSubjectType(command.subjectType());
        task.setSubjectId(command.subjectId());
        task.setOwnerType(command.ownerType());
        task.setOwnerId(command.ownerId());
        task.setOrganizationId(command.organizationId());
        task.setIdempotencyKey(command.idempotencyKey());
        return task;
    }

    private static BizTaskExecutionDTO acceptedExecution(String taskId, int maxAttempts) {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setTaskId(taskId);
        execution.setMaxAttempts(maxAttempts);
        return execution;
    }

    private void assertScheduleRejected(TaskCreateCommand command, ServiceExceptionEnum expected) {
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        LongTaskHandler handler = mock(LongTaskHandler.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager);
        when(registry.require("REGISTERED", 1)).thenReturn(handler);

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(command));

        assertEquals(expected.getCode(), error.getCode());
        verify(manager, never()).create(any(BizTaskDTO.class), any());
    }

    private void assertPayloadRejected(JSONObject payload, int payloadVersion) {
        LongTaskHandlerRegistry registry = mock(LongTaskHandlerRegistry.class);
        BizTaskManager manager = mock(BizTaskManager.class);
        LongTaskHandler handler = mock(LongTaskHandler.class);
        BizTaskCreateService service = new BizTaskCreateService(registry, manager);
        TaskCreateCommand command = command("REGISTERED", payload, payloadVersion);
        when(registry.require("REGISTERED", payloadVersion)).thenReturn(handler);

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(command));

        assertEquals(ServiceExceptionEnum.TASK_PAYLOAD_INVALID.getCode(), error.getCode());
        verify(handler, never()).validate(any(TaskCreateContext.class), any(JSONObject.class));
        verify(manager, never()).create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class));
    }

    private static Stream<Arguments> prohibitedPayloads() {
        JSONObject nestedPassword = new JSONObject();
        nestedPassword.put("password", "do-not-store");
        JSONArray items = new JSONArray();
        items.add(nestedPassword);
        JSONObject nestedArray = new JSONObject();
        nestedArray.put("items", items);

        return Stream.of(
            Arguments.of("nested password key", nestedArray),
            Arguments.of("credential key", payload("clientCredential", "do-not-store")),
            Arguments.of("secret key", payload("media_secret", "do-not-store")),
            Arguments.of("token key", payload("accessToken", "do-not-store")),
            Arguments.of("authorization key", payload("Authorization", "Bearer do-not-store")),
            Arguments.of("explicit Base64 content", payload("content", "data:image/png;base64,iVBORw0KGgo=")),
            Arguments.of("unmarked Base64 binary", payload("content", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")),
            Arguments.of("Unix absolute path", payload("content", "/var/lib/voglander/private.jpg")),
            Arguments.of("Windows absolute path", payload("content", "C:\\voglander\\private.jpg")));
    }

    private static Stream<Arguments> validSchedulePlans() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 10, 0);
        return Stream.of(
            Arguments.of("ONCE", "ONCE", null, null, null, 1),
            Arguments.of("AT_TIME", "AT_TIME", start, null, null, 1),
            Arguments.of("FIXED_RATE inclusive end", "FIXED_RATE", start, start.plusMinutes(10), 300L, 3),
            Arguments.of("FIXED_RATE non-aligned end", "FIXED_RATE", start, start.plusMinutes(11), 300L, 3),
            Arguments.of("FIXED_RATE exact maximum", "FIXED_RATE", start,
                start.plusSeconds(TaskConstant.DEFAULT_MAX_PLANNED_COUNT - 1L), 1L,
                TaskConstant.DEFAULT_MAX_PLANNED_COUNT));
    }

    private static Stream<Arguments> invalidSchedulePlans() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 10, 0);
        return Stream.of(
            Arguments.of("unknown mode", "CRON", null, null, null),
            Arguments.of("ONCE with start", "ONCE", start, null, null),
            Arguments.of("AT_TIME without start", "AT_TIME", null, null, null),
            Arguments.of("AT_TIME with end", "AT_TIME", start, start.plusHours(1), null),
            Arguments.of("AT_TIME with interval", "AT_TIME", start, null, 60L),
            Arguments.of("FIXED_RATE without end", "FIXED_RATE", start, null, 60L),
            Arguments.of("FIXED_RATE reversed", "FIXED_RATE", start, start.minusSeconds(1), 60L),
            Arguments.of("FIXED_RATE zero interval", "FIXED_RATE", start, start.plusHours(1), 0L),
            Arguments.of("FIXED_RATE negative interval", "FIXED_RATE", start, start.plusHours(1), -1L),
            Arguments.of("FIXED_RATE interval overflow", "FIXED_RATE", start, start.plusDays(1), Long.MAX_VALUE));
    }

    private static Stream<Arguments> scheduledPersistencePlans() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 10, 0);
        return Stream.of(
            Arguments.of("AT_TIME", "AT_TIME", start, null, null, 1),
            Arguments.of("FIXED_RATE", "FIXED_RATE", start, start.plusMinutes(10), 300L, 3));
    }

    private static JSONObject payload(String key, String value) {
        JSONObject payload = new JSONObject();
        payload.put(key, value);
        return payload;
    }

    private TaskCreateCommand onceCommand(String taskType) {
        JSONObject payload = new JSONObject();
        payload.put("field", "value");
        return command(taskType, payload, 1);
    }

    private TaskCreateCommand command(String taskType, JSONObject payload, int payloadVersion) {
        return command(taskType, "ONCE", null, null, null, payload, payloadVersion);
    }

    private TaskCreateCommand command(String taskType, String mode, LocalDateTime start, LocalDateTime end,
        Long intervalSeconds, JSONObject payload, int payloadVersion) {
        return new TaskCreateCommand(taskType, "internal task", null, mode, start, end, intervalSeconds, payload,
            payloadVersion,
            "biz-key", "SUBJECT", "subject-1", "USER", "owner-create-test", null, "idem-create", 2);
    }
}
