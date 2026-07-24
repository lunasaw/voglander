package io.github.lunasaw.voglander.web.api.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskEventManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.service.task.LongTaskHandlerRegistry;
import io.github.lunasaw.voglander.service.task.BusinessTaskAuthorizationService;
import io.github.lunasaw.voglander.web.api.auth.AuthenticatedUserResolver;
import io.github.lunasaw.voglander.web.api.task.assembler.BusinessTaskWebAssembler;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskExecutionPageReq;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskPageReq;
import io.github.lunasaw.voglander.web.api.task.resp.BusinessTaskExecutionListResp;
import io.github.lunasaw.voglander.web.api.task.resp.BusinessTaskListResp;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskExecutionDetailVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskDetailVO;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unified business-task query Controller")
class BusinessTaskControllerTest {

    @Mock
    private BizTaskManager taskManager;
    @Mock
    private BizTaskExecutionManager executionManager;
    @Mock
    private BizTaskEventManager eventManager;
    @Mock
    private LongTaskHandlerRegistry handlerRegistry;
    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Test
    @DisplayName("task page delegates fixed query and returns safe VO list")
    void taskPage_shouldDelegateAndReturnItems() {
        authorizeQuery();
        BusinessTaskController controller = controller();
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_1");
        task.setPayload("{\"secret\":\"hidden\"}");
        Page<BizTaskDTO> page = new Page<BizTaskDTO>(2, 10);
        page.setTotal(1);
        page.setRecords(Collections.singletonList(task));
        when(taskManager.getPage(any(BizTaskQueryDTO.class), any(), eq(2), eq(10))).thenReturn(page);

        BusinessTaskPageReq request = new BusinessTaskPageReq();
        Object data = controller.getTaskPage("Bearer token", request, 2, 10).get("data");
        BusinessTaskListResp response = data instanceof BusinessTaskListResp ? (BusinessTaskListResp)data : null;

        assertEquals(1L, response.getTotal());
        assertEquals("btask_1", response.getItems().get(0).getTaskId());
        verify(taskManager).getPage(any(BizTaskQueryDTO.class), any(), eq(2), eq(10));
    }

    @Test
    @DisplayName("task detail missing resource returns stable 720000")
    void taskDetail_shouldRejectMissingResource() {
        authorizeQuery();
        when(taskManager.getByTaskId(eq("btask_missing"), any())).thenReturn(null);
        ServiceException error = assertThrows(ServiceException.class,
            () -> controller().getTask("Bearer token", "btask_missing"));
        assertEquals(ServiceExceptionEnum.TASK_NOT_FOUND.getCode(), error.getCode());
        verify(eventManager, never()).getTimeline(any(), any(), any(), any(Integer.class));
    }

    @Test
    @DisplayName("execution page and detail include sanitized event timeline")
    void executionEndpoints_shouldReturnSafeExecutionFactsAndTimeline() {
        authorizeQuery();
        BusinessTaskController controller = controller();
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId("bexec_1");
        execution.setTaskId("btask_1");
        execution.setClaimToken("must-not-cross-boundary");
        Page<BizTaskExecutionDTO> page = new Page<BizTaskExecutionDTO>(1, 10);
        page.setTotal(1);
        page.setRecords(Collections.singletonList(execution));
        when(executionManager.getPage(any(), any(), eq(1), eq(10))).thenReturn(page);
        when(executionManager.getByExecutionId(eq("bexec_1"), any())).thenReturn(execution);
        BizTaskEventDTO event = new BizTaskEventDTO();
        event.setTaskId("btask_1");
        event.setEventId("bevt_1");
        when(eventManager.getTimeline(eq("btask_1"), eq("bexec_1"), any(), any(Integer.class)))
            .thenReturn(Collections.singletonList(event));

        BusinessTaskExecutionListResp list = (BusinessTaskExecutionListResp)controller
            .getExecutionPage("Bearer token", new BusinessTaskExecutionPageReq(), 1, 10).get("data");
        BusinessTaskExecutionDetailVO detail = (BusinessTaskExecutionDetailVO)controller
            .getExecution("Bearer token", "bexec_1").get("data");

        assertEquals("bexec_1", list.getItems().get(0).getExecutionId());
        assertEquals("bexec_1", detail.getExecutionId());
        assertEquals(1, detail.getEvents().size());
        verify(eventManager).getTimeline(eq("btask_1"), eq("bexec_1"), any(), any(Integer.class));
    }

    @Test
    @DisplayName("SSE断线重连后仍通过数据库权威详情查询恢复最新事实")
    void reconnect_shouldUseAuthoritativeTaskDetailQuery() {
        authorizeQuery();
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_reconnect");
        task.setTaskType("IMAGE_COLLECTION");
        task.setState("RUNNING");
        when(taskManager.getByTaskId(eq("btask_reconnect"), any())).thenReturn(task);

        controller().getTask("Bearer token", "btask_reconnect");

        verify(taskManager).getByTaskId(eq("btask_reconnect"), any());
    }

    @Test
    @DisplayName("task detail returns only the scoped active last execution")
    void taskDetail_shouldResolveActiveExecutionWithTheSameScope() {
        authorizeQuery();
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_active");
        task.setTaskType("IMAGE_COLLECTION");
        task.setLastExecutionId("bexec_active");
        when(taskManager.getByTaskId(eq("btask_active"), any())).thenReturn(task);
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId("bexec_active");
        execution.setTaskId("btask_active");
        execution.setState("RETRY_WAIT");
        when(executionManager.getByExecutionId(eq("bexec_active"), any())).thenReturn(execution);

        BusinessTaskDetailVO result = (BusinessTaskDetailVO)controller()
            .getTask("Bearer token", "btask_active").get("data");

        assertEquals("bexec_active", result.getActiveExecution().getExecutionId());
        org.mockito.ArgumentCaptor<BizTaskAccessScopeDTO> taskScope =
            org.mockito.ArgumentCaptor.forClass(BizTaskAccessScopeDTO.class);
        org.mockito.ArgumentCaptor<BizTaskAccessScopeDTO> executionScope =
            org.mockito.ArgumentCaptor.forClass(BizTaskAccessScopeDTO.class);
        verify(taskManager).getByTaskId(eq("btask_active"), taskScope.capture());
        verify(executionManager).getByExecutionId(eq("bexec_active"), executionScope.capture());
        assertEquals(taskScope.getValue(), executionScope.getValue());
    }

    @Test
    @DisplayName("task detail suppresses a terminal last execution")
    void taskDetail_shouldNotExposeTerminalExecutionAsActive() {
        authorizeQuery();
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_terminal");
        task.setTaskType("IMAGE_COLLECTION");
        task.setLastExecutionId("bexec_terminal");
        when(taskManager.getByTaskId(eq("btask_terminal"), any())).thenReturn(task);
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId("bexec_terminal");
        execution.setTaskId("btask_terminal");
        execution.setState("COMPLETED");
        when(executionManager.getByExecutionId(eq("bexec_terminal"), any())).thenReturn(execution);

        BusinessTaskDetailVO result = (BusinessTaskDetailVO)controller()
            .getTask("Bearer token", "btask_terminal").get("data");

        org.junit.jupiter.api.Assertions.assertNull(result.getActiveExecution());
    }

    @Test
    @DisplayName("query permission is enforced before task Manager access")
    void taskPage_shouldRejectBeforeManagerWhenActorCannotQueryAnyTaskType() {
        UserDTO user = user(Collections.<String>emptyList());
        when(authenticatedUserResolver.resolveBearer("Bearer denied")).thenReturn(user);

        ServiceException error = assertThrows(ServiceException.class,
            () -> controller().getTaskPage("Bearer denied", new BusinessTaskPageReq(), 1, 10));

        assertEquals(ServiceExceptionEnum.TASK_PERMISSION_DENIED.getCode(), error.getCode());
        verify(taskManager, never()).getPage(any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    @DisplayName("collection-only reader is scoped to IMAGE_COLLECTION for task reads")
    void taskPage_shouldPassTrustedImageCollectionScope() {
        UserDTO user = user(Collections.singletonList(ImageConstant.PERMISSION_COLLECTION_QUERY));
        when(authenticatedUserResolver.resolveBearer("Bearer collection")).thenReturn(user);
        Page<BizTaskDTO> page = new Page<BizTaskDTO>(1, 10);
        page.setRecords(Collections.<BizTaskDTO>emptyList());
        when(taskManager.getPage(any(), any(), eq(1), eq(10))).thenReturn(page);
        org.mockito.ArgumentCaptor<BizTaskAccessScopeDTO> scope =
            org.mockito.ArgumentCaptor.forClass(BizTaskAccessScopeDTO.class);

        controller().getTaskPage("Bearer collection", new BusinessTaskPageReq(), 1, 10);

        verify(taskManager).getPage(any(), scope.capture(), eq(1), eq(10));
        assertEquals(Set.of(ImageConstant.TASK_TYPE_IMAGE_COLLECTION), scope.getValue().getAllowedTaskTypes());
    }

    private BusinessTaskController controller() {
        return new BusinessTaskController(taskManager, executionManager, eventManager, handlerRegistry,
            new BusinessTaskWebAssembler(), null, authenticatedUserResolver,
            new BusinessTaskAuthorizationService());
    }

    private void authorizeQuery() {
        when(authenticatedUserResolver.resolveBearer("Bearer token"))
            .thenReturn(user(Collections.singletonList(TaskConstant.PERMISSION_QUERY)));
    }

    private UserDTO user(java.util.List<String> permissions) {
        UserDTO user = new UserDTO();
        user.setId(7L);
        user.setPermissions(permissions);
        return user;
    }
}
