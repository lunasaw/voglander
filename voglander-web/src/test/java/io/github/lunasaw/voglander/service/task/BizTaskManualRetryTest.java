package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.common.enums.task.TaskExecutionStateEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("Durable business-task manual retry")
class BizTaskManualRetryTest {

    @Mock
    private LongTaskHandlerRegistry handlerRegistry;

    @Mock
    private BizTaskManager bizTaskManager;

    @Mock
    private LongTaskHandler handler;

    @Test
    @DisplayName("失败执行的手动重试应创建新 ONCE 事实并记录 origin")
    void manualRetry_shouldCreateNewOnceTaskWithoutMutatingOriginalFacts() {
        BizTaskDTO originalTask = originalTask();
        BizTaskExecutionDTO failedExecution = failedExecution(originalTask.getTaskId());
        when(handlerRegistry.require("IMAGE_COLLECTION", 2)).thenReturn(handler);
        when(handler.capabilities()).thenReturn(new TaskCapabilities(false, false, true, false, false));
        when(bizTaskManager.create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        BizTaskCreateService service = new BizTaskCreateService(handlerRegistry, bizTaskManager,
            java.time.Clock.fixed(java.time.Instant.parse("2026-07-15T13:00:00Z"), java.time.ZoneOffset.UTC));
        BizTaskDTO retry = service.manualRetry(originalTask, failedExecution, "manual-retry-1");

        assertNotSame(originalTask, retry);
        assertEquals("ONCE", retry.getTaskMode());
        assertEquals("IMAGE_COLLECTION", retry.getTaskType());
        assertEquals(originalTask.getPayload(), retry.getPayload());
        assertEquals(originalTask.getTaskId(), retry.getOriginTaskId());
        assertEquals(failedExecution.getExecutionId(), retry.getOriginExecutionId());
        assertEquals("manual-retry-1", retry.getIdempotencyKey());
        assertEquals("RUNNING", retry.getState());
        assertEquals("FAILED", originalTask.getState());
        assertEquals("FAILED", failedExecution.getState());

        ArgumentCaptor<BizTaskDTO> taskCaptor = ArgumentCaptor.forClass(BizTaskDTO.class);
        ArgumentCaptor<BizTaskExecutionDTO> executionCaptor = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(bizTaskManager).create(taskCaptor.capture(), executionCaptor.capture());
        assertEquals(taskCaptor.getValue().getTaskId(), executionCaptor.getValue().getTaskId());
        assertEquals(TaskExecutionStateEnum.PENDING.name(), executionCaptor.getValue().getState());
        assertEquals(3, executionCaptor.getValue().getMaxAttempts());
        verify(handler).validate(any(), eq(JSONObject.of("camera", "camera-1")));
    }

    @Test
    @DisplayName("相同 owner/type/idempotency key 的重复手动重试应返回同一个新任务")
    void manualRetry_shouldReturnExistingIdempotentTaskWithoutCreatingAgain() {
        BizTaskDTO originalTask = originalTask();
        BizTaskExecutionDTO failedExecution = failedExecution(originalTask.getTaskId());
        BizTaskDTO acceptedRetry = new BizTaskDTO();
        acceptedRetry.setTaskId("btask_retry_existing");
        when(handlerRegistry.require("IMAGE_COLLECTION", 2)).thenReturn(handler);
        when(bizTaskManager.findByIdempotency("USER", "owner-1", "IMAGE_COLLECTION", "manual-retry-1"))
            .thenReturn(acceptedRetry);

        BizTaskCreateService service = new BizTaskCreateService(handlerRegistry, bizTaskManager,
            java.time.Clock.systemUTC());
        BizTaskDTO replay = service.manualRetry(originalTask, failedExecution, "manual-retry-1");

        assertSame(acceptedRetry, replay);
        verify(bizTaskManager).findByIdempotency("USER", "owner-1", "IMAGE_COLLECTION", "manual-retry-1");
        verify(bizTaskManager, never()).create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class));
        verify(handler, never()).validate(any(), any());
    }

    @Test
    @DisplayName("原任务不是失败或部分完成时应返回稳定重试冲突码")
    void manualRetry_shouldRejectNonFailedTaskWithStableConflict() {
        BizTaskDTO originalTask = originalTask();
        originalTask.setState("RUNNING");
        BizTaskExecutionDTO failedExecution = failedExecution(originalTask.getTaskId());

        ServiceException error = assertThrows(ServiceException.class,
            () -> newService().manualRetry(originalTask, failedExecution, "manual-retry-running"));

        assertEquals(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED.getCode(), error.getCode());
        verifyNoPersistenceForRejectedRetry();
    }

    @Test
    @DisplayName("原执行不是失败态时应返回稳定重试冲突码")
    void manualRetry_shouldRejectNonFailedExecutionWithStableConflict() {
        BizTaskDTO originalTask = originalTask();
        BizTaskExecutionDTO execution = failedExecution(originalTask.getTaskId());
        execution.setState("SUCCEEDED");

        ServiceException error = assertThrows(ServiceException.class,
            () -> newService().manualRetry(originalTask, execution, "manual-retry-succeeded"));

        assertEquals(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED.getCode(), error.getCode());
        verifyNoPersistenceForRejectedRetry();
    }

    @Test
    @DisplayName("Handler 未声明手动重试能力时应返回稳定重试冲突码")
    void manualRetry_shouldRejectWhenHandlerDoesNotSupportManualRetry() {
        BizTaskDTO originalTask = originalTask();
        BizTaskExecutionDTO failedExecution = failedExecution(originalTask.getTaskId());
        when(handlerRegistry.require("IMAGE_COLLECTION", 2)).thenReturn(handler);
        when(handler.capabilities()).thenReturn(io.github.lunasaw.voglander.client.domain.task.TaskCapabilities.none());
        when(bizTaskManager.findByIdempotency("USER", "owner-1", "IMAGE_COLLECTION", "manual-retry-disabled"))
            .thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class,
            () -> newService().manualRetry(originalTask, failedExecution, "manual-retry-disabled"));

        assertEquals(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED.getCode(), error.getCode());
        verify(bizTaskManager, never()).create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class));
        verify(handler, never()).validate(any(), any());
    }

    private BizTaskCreateService newService() {
        return new BizTaskCreateService(handlerRegistry, bizTaskManager, java.time.Clock.systemUTC());
    }

    private void verifyNoPersistenceForRejectedRetry() {
        verify(bizTaskManager, never()).findByIdempotency(any(), any(), any(), any());
        verify(bizTaskManager, never()).create(any(BizTaskDTO.class), any(BizTaskExecutionDTO.class));
    }

    private BizTaskDTO originalTask() {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_original");
        task.setTaskType("IMAGE_COLLECTION");
        task.setTaskName("collect camera");
        task.setTaskMode("ONCE");
        task.setState("FAILED");
        task.setPayload("{\"camera\":\"camera-1\"}");
        task.setPayloadVersion(2);
        task.setOwnerType("USER");
        task.setOwnerId("owner-1");
        task.setOrganizationId("org-1");
        task.setSubjectType("CAMERA");
        task.setSubjectId("camera-1");
        task.setBizKey("biz-1");
        return task;
    }

    private BizTaskExecutionDTO failedExecution(String taskId) {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId("bexec_original");
        execution.setTaskId(taskId);
        execution.setState("FAILED");
        execution.setAttemptCount(2);
        execution.setMaxAttempts(3);
        execution.setFailureCode("MEDIA_TIMEOUT");
        execution.setFinishedAt(LocalDateTime.of(2026, 7, 15, 12, 59));
        return execution;
    }
}
