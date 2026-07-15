package io.github.lunasaw.voglander.web.api.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskEventManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.service.task.LongTaskHandlerRegistry;
import io.github.lunasaw.voglander.web.api.task.assembler.BusinessTaskWebAssembler;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskExecutionPageReq;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskPageReq;
import io.github.lunasaw.voglander.web.api.task.resp.BusinessTaskExecutionListResp;
import io.github.lunasaw.voglander.web.api.task.resp.BusinessTaskListResp;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskExecutionDetailVO;

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

    @Test
    @DisplayName("task page delegates fixed query and returns safe VO list")
    void taskPage_shouldDelegateAndReturnItems() {
        BusinessTaskController controller = controller();
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_1");
        task.setPayload("{\"secret\":\"hidden\"}");
        Page<BizTaskDTO> page = new Page<BizTaskDTO>(2, 10);
        page.setTotal(1);
        page.setRecords(Collections.singletonList(task));
        when(taskManager.getPage(any(BizTaskQueryDTO.class), any(), eq(2), eq(10))).thenReturn(page);

        BusinessTaskPageReq request = new BusinessTaskPageReq();
        Object data = controller.getTaskPage(request, 2, 10).get("data");
        BusinessTaskListResp response = data instanceof BusinessTaskListResp ? (BusinessTaskListResp)data : null;

        assertEquals(1L, response.getTotal());
        assertEquals("btask_1", response.getItems().get(0).getTaskId());
        verify(taskManager).getPage(any(BizTaskQueryDTO.class), any(), eq(2), eq(10));
    }

    @Test
    @DisplayName("task detail missing resource returns stable 720000")
    void taskDetail_shouldRejectMissingResource() {
        when(taskManager.getByTaskId(eq("btask_missing"), any())).thenReturn(null);
        ServiceException error = assertThrows(ServiceException.class,
            () -> controller().getTask("btask_missing"));
        assertEquals(ServiceExceptionEnum.TASK_NOT_FOUND.getCode(), error.getCode());
        verify(eventManager, never()).getTimeline(any(), any(), any(), any(Integer.class));
    }

    @Test
    @DisplayName("execution page and detail include sanitized event timeline")
    void executionEndpoints_shouldReturnSafeExecutionFactsAndTimeline() {
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
            .getExecutionPage(new BusinessTaskExecutionPageReq(), 1, 10).get("data");
        BusinessTaskExecutionDetailVO detail = (BusinessTaskExecutionDetailVO)controller
            .getExecution("bexec_1").get("data");

        assertEquals("bexec_1", list.getItems().get(0).getExecutionId());
        assertEquals("bexec_1", detail.getExecutionId());
        assertEquals(1, detail.getEvents().size());
        verify(eventManager).getTimeline(eq("btask_1"), eq("bexec_1"), any(), any(Integer.class));
    }

    @Test
    @DisplayName("SSE断线重连后仍通过数据库权威详情查询恢复最新事实")
    void reconnect_shouldUseAuthoritativeTaskDetailQuery() {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_reconnect");
        task.setTaskType("IMAGE_COLLECTION");
        task.setState("RUNNING");
        when(taskManager.getByTaskId(eq("btask_reconnect"), any())).thenReturn(task);

        controller().getTask("btask_reconnect");

        verify(taskManager).getByTaskId(eq("btask_reconnect"), any());
    }

    private BusinessTaskController controller() {
        return new BusinessTaskController(taskManager, executionManager, eventManager, handlerRegistry,
            new BusinessTaskWebAssembler());
    }
}
