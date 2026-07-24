package io.github.lunasaw.voglander.web.api.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCommandDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskEventManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.service.task.BizTaskCreateService;
import io.github.lunasaw.voglander.service.task.BusinessTaskAuditService;
import io.github.lunasaw.voglander.service.task.BusinessTaskAuthorizationService;
import io.github.lunasaw.voglander.service.task.LongTaskHandlerRegistry;
import io.github.lunasaw.voglander.web.api.auth.AuthenticatedUserResolver;
import io.github.lunasaw.voglander.web.api.task.assembler.BusinessTaskWebAssembler;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskExecutionPageReq;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskPageReq;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskControlReq;
import io.github.lunasaw.voglander.web.api.task.resp.BusinessTaskExecutionListResp;
import io.github.lunasaw.voglander.web.api.task.resp.BusinessTaskListResp;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskConstraintsVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskDetailVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskExecutionDetailVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskExecutionVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskStatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Unified read-only task and execution query endpoints. */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1)
@Tag(name = "业务任务", description = "统一业务任务查询、执行历史与控制接口")
public class BusinessTaskController {

    private final BizTaskManager taskManager;
    private final BizTaskExecutionManager executionManager;
    private final BizTaskEventManager eventManager;
    private final LongTaskHandlerRegistry handlerRegistry;
    private final BusinessTaskWebAssembler assembler;
    private final BizTaskCreateService createService;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final BusinessTaskAuthorizationService authorizationService;

    @Autowired(required = false)
    private BusinessTaskAuditService auditService;

    @Autowired
    public BusinessTaskController(BizTaskManager taskManager, BizTaskExecutionManager executionManager,
        BizTaskEventManager eventManager, LongTaskHandlerRegistry handlerRegistry,
        BusinessTaskWebAssembler assembler, BizTaskCreateService createService,
        AuthenticatedUserResolver authenticatedUserResolver,
        BusinessTaskAuthorizationService authorizationService) {
        this.taskManager = taskManager;
        this.executionManager = executionManager;
        this.eventManager = eventManager;
        this.handlerRegistry = handlerRegistry;
        this.assembler = assembler;
        this.createService = createService;
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/business-tasks/getPage")
    @Operation(summary = "任务分页查询", description = "按稳定任务字段筛选并返回确定性分页结果")
    @ApiResponse(responseCode = "200", description = "成功",
        content = @Content(schema = @Schema(implementation = io.github.lunasaw.voglander.common.domain.AjaxResult.class)))
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskListResp> getTaskPage(
        @RequestHeader("Authorization") String authorization,
        @RequestBody(required = false) BusinessTaskPageReq request,
        @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页大小，最大1000") @RequestParam(defaultValue = "10") int size) {
        UserDTO actor = authenticatedUserResolver.resolveBearer(authorization);
        BizTaskQueryDTO query = assembler.pageReqToQuery(request);
        BizTaskAccessScopeDTO scope = authorizationService.queryScope(actor, query.getTaskType());
        Page<BizTaskDTO> source = taskManager.getPage(query, scope, page, size);
        BusinessTaskListResp response = new BusinessTaskListResp();
        response.setTotal(source.getTotal());
        List<io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskVO> items = new ArrayList<>();
        for (BizTaskDTO task : source.getRecords()) {
            items.add(assembler.toTaskVO(task));
        }
        response.setItems(items);
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(response);
    }

    @GetMapping("/business-tasks/{taskId}")
    @Operation(summary = "任务详情", description = "返回脱敏任务事实、汇总进度和当前动作能力")
    @ApiResponse(responseCode = "200", description = "成功",
        content = @Content(schema = @Schema(implementation = io.github.lunasaw.voglander.common.domain.AjaxResult.class)))
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskDetailVO> getTask(
        @RequestHeader("Authorization") String authorization,
        @Parameter(description = "稳定业务任务ID") @PathVariable String taskId) {
        BizTaskAccessScopeDTO scope = authorizationService.queryScope(
            authenticatedUserResolver.resolveBearer(authorization), null);
        BizTaskDTO task = taskManager.getByTaskId(taskId, scope);
        if (task == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_NOT_FOUND);
        }
        BizTaskExecutionDTO activeExecution = activeExecution(task, scope);
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(
            assembler.toTaskDetailVO(task, activeExecution, capabilities(task)));
    }

    @GetMapping("/business-tasks/statistics")
    @Operation(summary = "任务统计", description = "返回当前任务中心统计卡片数据")
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskStatisticsVO> getStatistics(
        @RequestHeader("Authorization") String authorization) {
        BizTaskAccessScopeDTO scope = authorizationService.queryScope(
            authenticatedUserResolver.resolveBearer(authorization), null);
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(
            assembler.toStatisticsVO(taskManager.getStatistics(scope)));
    }

    @GetMapping("/business-tasks/constraints")
    @Operation(summary = "任务约束与能力", description = "返回稳定状态、任务类型和 Handler 能力码")
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskConstraintsVO> getConstraints(
        @RequestHeader("Authorization") String authorization) {
        BizTaskAccessScopeDTO scope = authorizationService.queryScope(
            authenticatedUserResolver.resolveBearer(authorization), null);
        List<String> types = new ArrayList<>();
        Map<String, List<String>> capabilityMap = new LinkedHashMap<>();
        if (handlerRegistry.all() != null) {
            for (Map.Entry<String, LongTaskHandler> entry : handlerRegistry.all().entrySet()) {
                if (!isTaskTypeAllowed(scope, entry.getKey())) {
                    continue;
                }
                types.add(entry.getKey());
                capabilityMap.put(entry.getKey(), capabilities(entry.getValue().capabilities()));
            }
        }
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(
            assembler.toConstraintsVO(types, capabilityMap));
    }

    @PostMapping("/business-task-executions/getPage")
    @Operation(summary = "执行分页查询", description = "返回脱敏执行事实并按计划时间稳定排序")
    @ApiResponse(responseCode = "200", description = "成功",
        content = @Content(schema = @Schema(implementation = io.github.lunasaw.voglander.common.domain.AjaxResult.class)))
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskExecutionListResp> getExecutionPage(
        @RequestHeader("Authorization") String authorization,
        @RequestBody(required = false) BusinessTaskExecutionPageReq request,
        @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页大小，最大1000") @RequestParam(defaultValue = "10") int size) {
        BizTaskAccessScopeDTO scope = authorizationService.queryScope(
            authenticatedUserResolver.resolveBearer(authorization), null);
        BizTaskExecutionQueryDTO query = assembler.executionPageReqToQuery(request);
        Page<BizTaskExecutionDTO> source = executionManager.getPage(query, scope, page, size);
        BusinessTaskExecutionListResp response = new BusinessTaskExecutionListResp();
        response.setTotal(source.getTotal());
        List<BusinessTaskExecutionVO> items = new ArrayList<>();
        for (BizTaskExecutionDTO execution : source.getRecords()) {
            items.add(assembler.toExecutionVO(execution));
        }
        response.setItems(items);
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(response);
    }

    @GetMapping("/business-task-executions/{executionId}")
    @Operation(summary = "执行详情与事件时间线", description = "返回脱敏执行事实和只追加事件摘要")
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskExecutionDetailVO> getExecution(
        @RequestHeader("Authorization") String authorization,
        @Parameter(description = "稳定业务执行ID") @PathVariable String executionId) {
        BizTaskAccessScopeDTO scope = authorizationService.queryScope(
            authenticatedUserResolver.resolveBearer(authorization), null);
        BizTaskExecutionDTO execution = executionManager.getByExecutionId(executionId, scope);
        if (execution == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_EXECUTION_NOT_FOUND);
        }
        List<BizTaskEventDTO> events = eventManager.getTimeline(execution.getTaskId(), executionId,
            scope, 1000);
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(
            assembler.toExecutionDetailVO(execution, events));
    }

    @PostMapping("/business-tasks/{taskId}:pause")
    @Operation(summary = "暂停任务", description = "幂等暂停任务并保留计划游标")
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskDetailVO> pause(
        @Parameter(description = "稳定业务任务ID") @PathVariable String taskId,
        @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization,
        @RequestBody(required = false) BusinessTaskControlReq request) {
        UserDTO actor = resolveActor(authorization);
        requireExpectedVersion(request);
        BizTaskDTO task = requireControlTask(taskId, actor);
        requireCapability(task, "PAUSE");
        BizTaskDTO updated;
        try {
            updated = taskManager.pause(command(taskId, request, actor));
        } catch (ServiceException exception) {
            auditRejected(taskId, null, "PAUSE", resultCode(exception));
            throw exception;
        }
        auditAccepted("PAUSE", task, updated, actor);
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(
            assembler.toTaskDetailVO(updated, null, capabilities(updated)));
    }

    @PostMapping("/business-tasks/{taskId}:resume")
    @Operation(summary = "恢复任务", description = "幂等恢复已暂停任务")
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskDetailVO> resume(
        @Parameter(description = "稳定业务任务ID") @PathVariable String taskId,
        @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization,
        @RequestBody(required = false) BusinessTaskControlReq request) {
        UserDTO actor = resolveActor(authorization);
        requireExpectedVersion(request);
        BizTaskDTO task = requireControlTask(taskId, actor);
        requireCapability(task, "PAUSE");
        BizTaskDTO updated;
        try {
            updated = taskManager.resume(command(taskId, request, actor));
        } catch (ServiceException exception) {
            auditRejected(taskId, null, "RESUME", resultCode(exception));
            throw exception;
        }
        auditAccepted("RESUME", task, updated, actor);
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(
            assembler.toTaskDetailVO(updated, null, capabilities(updated)));
    }

    @PostMapping("/business-tasks/{taskId}:cancel")
    @Operation(summary = "取消任务", description = "发起协作取消并停止后续计划点")
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskDetailVO> cancel(
        @Parameter(description = "稳定业务任务ID") @PathVariable String taskId,
        @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization,
        @RequestBody(required = false) BusinessTaskControlReq request) {
        UserDTO actor = resolveActor(authorization);
        requireExpectedVersion(request);
        BizTaskDTO task = requireControlTask(taskId, actor);
        requireCapability(task, "CANCEL");
        BizTaskDTO updated;
        try {
            updated = taskManager.cancel(command(taskId, request, actor));
        } catch (ServiceException exception) {
            auditRejected(taskId, null, "CANCEL", resultCode(exception));
            throw exception;
        }
        auditAccepted("CANCEL", task, updated, actor);
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(
            assembler.toTaskDetailVO(updated, null, capabilities(updated)));
    }

    @PostMapping("/business-tasks/{taskId}:retry")
    @Operation(summary = "人工重试任务", description = "以新的 ONCE 任务创建事实并保留原历史")
    public io.github.lunasaw.voglander.common.domain.AjaxResult<BusinessTaskDetailVO> retry(
        @Parameter(description = "稳定业务任务ID") @PathVariable String taskId,
        @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization,
        @RequestBody BusinessTaskControlReq request) {
        UserDTO actor = resolveActor(authorization);
        BizTaskAccessScopeDTO scope = authorizationService.controlLookupScope(actor);
        BizTaskDTO task = requireControlTask(taskId, actor, scope);
        requireCapability(task, "MANUAL_RETRY");
        if (request == null || !org.springframework.util.StringUtils.hasText(request.getExecutionId())
            || !org.springframework.util.StringUtils.hasText(request.getIdempotencyKey())) {
            auditRejected(taskId, null, "MANUAL_RETRY", "TASK_RETRY_NOT_ALLOWED");
            throw new ServiceException(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED);
        }
        BizTaskExecutionDTO execution = executionManager.getByExecutionId(request.getExecutionId(),
            scope);
        if (execution == null || !taskId.equals(execution.getTaskId())) {
            auditRejected(taskId, request.getExecutionId(), "MANUAL_RETRY", "TASK_RETRY_NOT_ALLOWED");
            throw new ServiceException(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED);
        }
        BizTaskDTO retry;
        try {
            retry = createService.manualRetry(task, execution, request.getIdempotencyKey());
        } catch (ServiceException exception) {
            auditRejected(taskId, request.getExecutionId(), "MANUAL_RETRY", resultCode(exception));
            throw exception;
        }
        auditAccepted("MANUAL_RETRY", task, retry, actor);
        return io.github.lunasaw.voglander.common.domain.AjaxResult.success(
            assembler.toTaskDetailVO(retry, null, capabilities(retry)));
    }

    private UserDTO resolveActor(String authorization) {
        return authenticatedUserResolver.resolveBearer(authorization);
    }

    private BizTaskDTO requireControlTask(String taskId, UserDTO actor) {
        return requireControlTask(taskId, actor, authorizationService.controlLookupScope(actor));
    }

    private BizTaskDTO requireControlTask(String taskId, UserDTO actor, BizTaskAccessScopeDTO scope) {
        BizTaskDTO task = taskManager.getByTaskId(taskId, scope);
        if (task == null) {
            if (authorizationService.hasTaskControlWithoutImageControl(actor)) {
                auditRejected(taskId, null, "CONTROL", "TASK_PERMISSION_DENIED");
                throw new ServiceException(ServiceExceptionEnum.TASK_PERMISSION_DENIED);
            }
            auditRejected(taskId, null, "CONTROL", "TASK_NOT_FOUND");
            throw new ServiceException(ServiceExceptionEnum.TASK_NOT_FOUND);
        }
        authorizationService.requireControl(actor, task.getTaskType());
        return task;
    }

    private void requireExpectedVersion(BusinessTaskControlReq request) {
        if (request == null || request.getExpectedVersion() == null || request.getExpectedVersion() < 0) {
            throw new ServiceException(ServiceExceptionEnum.PARAM_ERROR)
                .setDetailMessage("expectedVersion is required and must be non-negative");
        }
    }

    private boolean isTaskTypeAllowed(BizTaskAccessScopeDTO scope, String taskType) {
        return scope.getAllowedTaskTypes() == null || scope.getAllowedTaskTypes().contains(taskType);
    }

    private void requireCapability(BizTaskDTO task, String capability) {
        LongTaskHandler handler = task.getPayloadVersion() == null
            ? handlerRegistry.require(task.getTaskType())
            : handlerRegistry.require(task.getTaskType(), task.getPayloadVersion());
        List<String> declared = capabilities(handler.capabilities());
        if (!declared.contains(capability)) {
            auditRejected(task.getTaskId(), null, capability,
                "MANUAL_RETRY".equals(capability) ? "TASK_RETRY_NOT_ALLOWED" : "TASK_STATE_CONFLICT");
            throw new ServiceException("MANUAL_RETRY".equals(capability)
                ? ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED : ServiceExceptionEnum.TASK_STATE_CONFLICT);
        }
    }

    private BizTaskCommandDTO command(String taskId, BusinessTaskControlReq request, UserDTO actor) {
        BizTaskCommandDTO command = new BizTaskCommandDTO();
        command.setTaskId(taskId);
        command.setExpectedVersion(request == null ? null : request.getExpectedVersion());
        command.setReason(request == null ? null : request.getReason());
        command.setActorType("USER");
        command.setActorId(actor.getId() == null ? null : actor.getId().toString());
        command.setRequestedAt(java.time.LocalDateTime.now());
        return command;
    }

    private List<String> capabilities(String taskType) {
        if (handlerRegistry.all() == null) {
            return Collections.emptyList();
        }
        LongTaskHandler handler = handlerRegistry.all().get(taskType);
        return handler == null ? Collections.emptyList() : capabilities(handler.capabilities());
    }

    private List<String> capabilities(BizTaskDTO task) {
        if (task == null) {
            return Collections.emptyList();
        }
        if (task.getPayloadVersion() == null) {
            return capabilities(task.getTaskType());
        }
        return capabilities(handlerRegistry.require(task.getTaskType(), task.getPayloadVersion()).capabilities());
    }

    private BizTaskExecutionDTO activeExecution(BizTaskDTO task, BizTaskAccessScopeDTO scope) {
        if (task == null || !org.springframework.util.StringUtils.hasText(task.getLastExecutionId())) {
            return null;
        }
        BizTaskExecutionDTO execution = executionManager.getByExecutionId(task.getLastExecutionId(), scope);
        if (execution == null) {
            return null;
        }
        String state = execution.getState();
        return "PENDING".equals(state) || "RUNNING".equals(state) || "RETRY_WAIT".equals(state)
            ? execution : null;
    }

    private List<String> capabilities(TaskCapabilities value) {
        if (value == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        if (value.supportsPause()) result.add("PAUSE");
        if (value.supportsCancel()) result.add("CANCEL");
        if (value.supportsManualRetry()) result.add("MANUAL_RETRY");
        if (value.supportsProgress()) result.add("PROGRESS");
        if (value.supportsReschedule()) result.add("RESCHEDULE");
        return result;
    }

    private void auditAccepted(String command, BizTaskDTO before, BizTaskDTO after, UserDTO actor) {
        if (auditService == null) {
            return;
        }
        auditService.record(new io.github.lunasaw.voglander.common.event.BusinessTaskAuditRecord(true, null,
            "USER", actor == null || actor.getId() == null ? null : actor.getId().toString(),
            before == null ? null : before.getTaskId(), null, command,
            before == null ? null : before.getState(), after == null ? null : after.getState(), "OK",
            System.currentTimeMillis()));
    }

    private void auditRejected(String taskId, String executionId, String command, String resultCode) {
        if (auditService == null) {
            return;
        }
        auditService.record(new io.github.lunasaw.voglander.common.event.BusinessTaskAuditRecord(false, null,
            "USER", null, taskId, executionId, command, null, null, resultCode,
            System.currentTimeMillis()));
    }

    private String resultCode(ServiceException exception) {
        return exception == null ? "REJECTED" : String.valueOf(exception.getCode());
    }
}
