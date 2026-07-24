package io.github.lunasaw.voglander.web.api.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskEventManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.service.task.BusinessTaskAuthorizationService;
import io.github.lunasaw.voglander.service.task.BizTaskCreateService;
import io.github.lunasaw.voglander.service.task.LongTaskHandlerRegistry;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.web.api.task.assembler.BusinessTaskWebAssembler;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskControlReq;
import io.github.lunasaw.voglander.web.api.auth.AuthenticatedUserResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("Business-task control permission and state boundary")
class BusinessTaskControlControllerTest {

    @Mock private BizTaskManager taskManager;
    @Mock private BizTaskExecutionManager executionManager;
    @Mock private BizTaskEventManager eventManager;
    @Mock private LongTaskHandlerRegistry handlerRegistry;
    @Mock private BizTaskCreateService createService;
    @Mock private AuthService authService;
    @Mock private LongTaskHandler handler;

    @Test
    @DisplayName("missing Task:Control returns 403 code before task mutation")
    void control_shouldRejectMissingPermissionBeforeManager() {
        UserDTO user = new UserDTO();
        user.setId(7L);
        user.setPermissions(Collections.singletonList(TaskConstant.PERMISSION_QUERY));
        when(authService.getUserByToken("token")).thenReturn(user);

        ServiceException error = assertThrows(ServiceException.class,
            () -> controller().pause("btask_1", "Bearer token", command(0)));

        assertEquals(ServiceExceptionEnum.TASK_PERMISSION_DENIED.getCode(), error.getCode());
        verify(taskManager, never()).pause(any());
    }

    @Test
    @DisplayName("Task-only control receives 403 for an image task hidden by the lookup scope")
    void control_shouldRejectImageCollectionWhenImageControlPermissionIsMissing() {
        UserDTO user = new UserDTO();
        user.setId(7L);
        user.setPermissions(Collections.singletonList(TaskConstant.PERMISSION_CONTROL));
        when(authService.getUserByToken("token")).thenReturn(user);
        when(taskManager.getByTaskId(eq("btask_1"), any())).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class,
            () -> controller().pause("btask_1", "Bearer token", command(3)));

        assertEquals(ServiceExceptionEnum.TASK_PERMISSION_DENIED.getCode(), error.getCode());
        org.mockito.ArgumentCaptor<io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO> scope =
            org.mockito.ArgumentCaptor.forClass(io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO.class);
        verify(taskManager).getByTaskId(eq("btask_1"), scope.capture());
        org.junit.jupiter.api.Assertions.assertFalse(
            scope.getValue().getAllowedTaskTypes().contains(ImageConstant.TASK_TYPE_IMAGE_COLLECTION));
        verify(taskManager, never()).pause(any());
    }

    @Test
    @DisplayName("Task-only control receives the same 403 for a nonexistent task")
    void control_shouldNotLeakMissingTaskToPartialControlActor() {
        UserDTO user = new UserDTO();
        user.setId(7L);
        user.setPermissions(Collections.singletonList(TaskConstant.PERMISSION_CONTROL));
        when(authService.getUserByToken("token")).thenReturn(user);
        when(taskManager.getByTaskId(eq("btask_missing"), any())).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class,
            () -> controller().pause("btask_missing", "Bearer token", command(3)));

        assertEquals(ServiceExceptionEnum.TASK_PERMISSION_DENIED.getCode(), error.getCode());
        verify(taskManager, never()).pause(any());
    }

    @Test
    @DisplayName("Task-only control remains valid for non-image tasks")
    void control_shouldKeepNonImageTaskCompatibility() {
        UserDTO user = new UserDTO();
        user.setId(7L);
        user.setPermissions(Collections.singletonList(TaskConstant.PERMISSION_CONTROL));
        when(authService.getUserByToken("token")).thenReturn(user);
        BizTaskDTO task = task("RUNNING");
        task.setTaskType("DATA_EXPORT");
        when(taskManager.getByTaskId(eq("btask_1"), any())).thenReturn(task);
        when(handlerRegistry.require("DATA_EXPORT", 1)).thenReturn(handler);
        when(handler.capabilities()).thenReturn(new TaskCapabilities(true, true, true, true, false));
        when(taskManager.pause(any())).thenReturn(task);

        controller().pause("btask_1", "Bearer token", command(3));

        verify(taskManager).pause(any());
    }

    @Test
    @DisplayName("pause resume and cancel reject missing expectedVersion before task lookup")
    void controls_shouldRejectMissingExpectedVersionBeforeTaskLookup() {
        authorize();
        BusinessTaskControlReq missingVersion = new BusinessTaskControlReq();

        assertEquals(ServiceExceptionEnum.PARAM_ERROR.getCode(), assertThrows(ServiceException.class,
            () -> controller().pause("btask_1", "Bearer token", missingVersion)).getCode());
        assertEquals(ServiceExceptionEnum.PARAM_ERROR.getCode(), assertThrows(ServiceException.class,
            () -> controller().resume("btask_1", "Bearer token", missingVersion)).getCode());
        assertEquals(ServiceExceptionEnum.PARAM_ERROR.getCode(), assertThrows(ServiceException.class,
            () -> controller().cancel("btask_1", "Bearer token", missingVersion)).getCode());
        verifyNoInteractions(taskManager);
    }

    @Test
    @DisplayName("authorized pause revalidates Handler capability and delegates idempotent command")
    void pause_shouldCheckCapabilityAndDelegate() {
        authorize();
        BizTaskDTO task = task("RUNNING");
        when(taskManager.getByTaskId(eq("btask_1"), any())).thenReturn(task);
        when(handlerRegistry.require("IMAGE_COLLECTION", 1)).thenReturn(handler);
        when(handler.capabilities()).thenReturn(new TaskCapabilities(true, true, true, true, false));
        when(taskManager.pause(any())).thenReturn(task);

        controller().pause("btask_1", "Bearer token", command(3));

        verify(taskManager).pause(any());
    }

    @Test
    @DisplayName("invisible task is indistinguishable from missing task and returns 404 code")
    void control_shouldRejectInvisibleTaskAsNotFound() {
        authorize();
        when(taskManager.getByTaskId(eq("btask_invisible"), any())).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class,
            () -> controller().pause("btask_invisible", "Bearer token", command(3)));

        assertEquals(ServiceExceptionEnum.TASK_NOT_FOUND.getCode(), error.getCode());
        verify(taskManager, never()).pause(any());
    }

    @Test
    @DisplayName("terminal state conflict returns 409 code and does not hide manager rejection")
    void control_shouldReturnStateConflictForTerminalTask() {
        authorize();
        BizTaskDTO task = task("COMPLETED");
        when(taskManager.getByTaskId(eq("btask_1"), any())).thenReturn(task);
        when(handlerRegistry.require("IMAGE_COLLECTION", 1)).thenReturn(handler);
        when(handler.capabilities()).thenReturn(new TaskCapabilities(true, true, true, true, false));
        when(taskManager.pause(any())).thenThrow(new ServiceException(ServiceExceptionEnum.TASK_STATE_CONFLICT));

        ServiceException error = assertThrows(ServiceException.class,
            () -> controller().pause("btask_1", "Bearer token", command(3)));

        assertEquals(ServiceExceptionEnum.TASK_STATE_CONFLICT.getCode(), error.getCode());
        verify(taskManager).pause(any());
    }

    @Test
    @DisplayName("repeated pause command is idempotent and returns the same paused fact")
    void pause_shouldRemainIdempotentOnRepeatedCommand() {
        authorize();
        BizTaskDTO paused = task("PAUSED");
        when(taskManager.getByTaskId(eq("btask_1"), any())).thenReturn(paused);
        when(handlerRegistry.require("IMAGE_COLLECTION", 1)).thenReturn(handler);
        when(handler.capabilities()).thenReturn(new TaskCapabilities(true, true, true, true, false));
        when(taskManager.pause(any())).thenReturn(paused);

        controller().pause("btask_1", "Bearer token", command(3));
        controller().pause("btask_1", "Bearer token", command(2));

        verify(taskManager, org.mockito.Mockito.times(2)).pause(any());
    }

    @Test
    @DisplayName("Handler 不支持 retry 时返回稳定重试冲突且不创建新任务")
    void retry_shouldRejectUnsupportedCapability() {
        authorize();
        BizTaskDTO task = task("FAILED");
        when(taskManager.getByTaskId(eq("btask_1"), any())).thenReturn(task);
        when(handlerRegistry.require("IMAGE_COLLECTION", 1)).thenReturn(handler);
        when(handler.capabilities()).thenReturn(TaskCapabilities.none());

        ServiceException error = assertThrows(ServiceException.class,
            () -> controller().retry("btask_1", "Bearer token", commandWithExecution("bexec_1")));

        assertEquals(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED.getCode(), error.getCode());
        verify(createService, never()).manualRetry(any(), any(), any());
    }

    @Test
    @DisplayName("authorized retry delegates new-fact creation with idempotency key")
    void retry_shouldDelegateManualRetry() {
        authorize();
        BizTaskDTO task = task("FAILED");
        BizTaskExecutionDTO execution = execution("FAILED");
        BizTaskDTO retry = task("RUNNING");
        when(taskManager.getByTaskId(eq("btask_1"), any())).thenReturn(task);
        when(executionManager.getByExecutionId(eq("bexec_1"), any())).thenReturn(execution);
        when(handlerRegistry.require("IMAGE_COLLECTION", 1)).thenReturn(handler);
        when(handler.capabilities()).thenReturn(new TaskCapabilities(false, true, true, true, false));
        when(createService.manualRetry(task, execution, "retry-1")).thenReturn(retry);

        controller().retry("btask_1", "Bearer token", commandWithExecution("bexec_1"));

        verify(createService).manualRetry(task, execution, "retry-1");
    }

    private BusinessTaskController controller() {
        return new BusinessTaskController(taskManager, executionManager, eventManager, handlerRegistry,
            new BusinessTaskWebAssembler(), createService, new AuthenticatedUserResolver(authService),
            new BusinessTaskAuthorizationService());
    }

    private void authorize() {
        UserDTO user = new UserDTO();
        user.setId(7L);
        user.setPermissions(Arrays.asList(TaskConstant.PERMISSION_CONTROL,
            ImageConstant.PERMISSION_COLLECTION_CONTROL));
        when(authService.getUserByToken("token")).thenReturn(user);
    }

    private BusinessTaskControlReq command(int version) {
        BusinessTaskControlReq command = new BusinessTaskControlReq();
        command.setExpectedVersion(version);
        command.setIdempotencyKey("retry-1");
        return command;
    }

    private BusinessTaskControlReq commandWithExecution(String executionId) {
        BusinessTaskControlReq command = command(3);
        command.setExecutionId(executionId);
        return command;
    }

    private BizTaskDTO task(String state) {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_1");
        task.setTaskType("IMAGE_COLLECTION");
        task.setPayloadVersion(1);
        task.setState(state);
        task.setVersion(3);
        return task;
    }

    private BizTaskExecutionDTO execution(String state) {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId("bexec_1");
        execution.setTaskId("btask_1");
        execution.setState(state);
        return execution;
    }
}
