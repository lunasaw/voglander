package io.github.lunasaw.voglander.manager.manager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.enums.task.TaskExecutionStateEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent;
import io.github.lunasaw.voglander.manager.assembler.BizTaskExecutionAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskProgressDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.domain.task.BizTaskExecutionQueryCondition;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;
import io.github.lunasaw.voglander.repository.mapper.BizTaskExecutionMapper;
import io.github.lunasaw.voglander.repository.mapper.BizTaskMapper;

/** Manager boundary for durable execution materialization, queries and conditional state operations. */
@Component
public class BizTaskExecutionManager {

    @Autowired
    private BizTaskExecutionService bizTaskExecutionService;

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskExecutionMapper bizTaskExecutionMapper;

    @Autowired
    private BizTaskMapper bizTaskMapper;

    @Autowired
    private BizTaskExecutionAssembler bizTaskExecutionAssembler;

    @Autowired(required = false)
    private BusinessTaskSseEventPublisher businessTaskSseEventPublisher;

    /** Materializes one execution fact without changing an existing execution or plan point. */
    public boolean insertIfAbsent(BizTaskExecutionDTO execution) {
        validateExecution(execution);
        BizTaskExecutionDO row = bizTaskExecutionAssembler.dtoToDo(execution);
        LocalDateTime now = LocalDateTime.now();
        row.setCreateTime(row.getCreateTime() == null ? now : row.getCreateTime());
        row.setUpdateTime(row.getUpdateTime() == null ? now : row.getUpdateTime());
        row.setAttemptCount(row.getAttemptCount() == null ? 0 : row.getAttemptCount());
        row.setMaxAttempts(row.getMaxAttempts() == null ? 1 : row.getMaxAttempts());
        row.setProgressCurrent(row.getProgressCurrent() == null ? 0L : row.getProgressCurrent());
        row.setProgressTotal(row.getProgressTotal() == null ? 0L : row.getProgressTotal());
        row.setProgressRevision(row.getProgressRevision() == null ? 0L : row.getProgressRevision());
        row.setRetryable(row.getRetryable() == null ? Boolean.FALSE : row.getRetryable());
        row.setVersion(row.getVersion() == null ? 0 : row.getVersion());
        return bizTaskExecutionMapper.insertIfAbsent(row) == 1;
    }

    /** Returns runnable durable facts for ID-only dispatch submission. */
    public List<BizTaskExecutionDTO> findRunnable(LocalDateTime now, int limit) {
        validateScan(now, limit);
        return bizTaskExecutionMapper.selectRunnable(now, limit).stream()
            .map(bizTaskExecutionAssembler::doToDto)
            .collect(Collectors.toList());
    }

    /** Returns expired RUNNING leases with internal claim/version facts required for recovery CAS. */
    public List<BizTaskExecutionDTO> findExpiredLeases(LocalDateTime now, int limit) {
        validateScan(now, limit);
        return bizTaskExecutionMapper.selectExpiredLeases(now, limit).stream()
            .map(bizTaskExecutionAssembler::doToDto)
            .collect(Collectors.toList());
    }

    /** Returns an access-scoped, safely mapped execution page with a fixed sorting allowlist. */
    public Page<BizTaskExecutionDTO> getPage(BizTaskExecutionQueryDTO query, BizTaskAccessScopeDTO scope,
        int page, int size) {
        Assert.isTrue(page > 0, "页码必须大于0");
        Assert.isTrue(size > 0 && size <= 1000, "页大小必须在1-1000之间");
        BizTaskExecutionQueryCondition condition = buildCondition(query, scope);
        Page<BizTaskExecutionDO> source = bizTaskExecutionMapper.selectPageWithScope(
            new Page<BizTaskExecutionDO>(page, size), condition);
        Page<BizTaskExecutionDTO> target = new Page<BizTaskExecutionDTO>(page, size);
        target.setCurrent(source.getCurrent());
        target.setSize(source.getSize());
        target.setTotal(source.getTotal());
        target.setPages(source.getPages());
        List<BizTaskExecutionDTO> records = source.getRecords().stream()
            .map(bizTaskExecutionAssembler::doToSafeDto)
            .collect(Collectors.toList());
        target.setRecords(records);
        return target;
    }

    /** Returns null for both missing and out-of-scope execution identities. */
    public BizTaskExecutionDTO getByExecutionId(String executionId, BizTaskAccessScopeDTO scope) {
        Assert.hasText(executionId, "executionId不能为空");
        BizTaskExecutionQueryDTO query = new BizTaskExecutionQueryDTO();
        query.setExecutionId(executionId);
        BizTaskExecutionQueryCondition condition = buildCondition(query, scope);
        return bizTaskExecutionAssembler.doToSafeDto(
            bizTaskExecutionMapper.selectByExecutionIdWithScope(condition));
    }

    /** Returns the current internal execution fact for the trusted Worker claim boundary. */
    public BizTaskExecutionDTO getForClaim(String executionId) {
        Assert.hasText(executionId, "executionId不能为空");
        return bizTaskExecutionAssembler.doToDto(findInternal(executionId));
    }

    /** Claims a runnable execution and returns the internal execution DTO only to the Worker boundary. */
    public BizTaskExecutionDTO claim(BizTaskExecutionDTO command) {
        Assert.notNull(command, "claim命令不能为空");
        Assert.hasText(command.getExecutionId(), "executionId不能为空");
        Assert.notNull(command.getVersion(), "version不能为空");
        Assert.hasText(command.getClaimToken(), "claimToken不能为空");
        Assert.hasText(command.getWorkerNode(), "workerNode不能为空");
        LocalDateTime now = commandTime(command);
        Assert.notNull(command.getLeaseUntil(), "leaseUntil不能为空");
        Assert.isTrue(command.getLeaseUntil().isAfter(now), "leaseUntil必须晚于claim时间");
        int affected = bizTaskExecutionMapper.claim(command.getExecutionId(), command.getVersion(),
            command.getClaimToken(), command.getWorkerNode(), now, command.getLeaseUntil());
        if (affected == 0) {
            return null;
        }
        BizTaskExecutionDTO claimed = bizTaskExecutionAssembler.doToDto(findInternal(command.getExecutionId()));
        if (claimed != null) {
            publishExecutionEvent(claimed, TaskConstant.SSE_EXECUTION_STATE, "CLAIMED", null);
        }
        return claimed;
    }

    /** Renews only the lease owned by the current claim token. */
    public boolean heartbeat(BizTaskExecutionDTO command) {
        Assert.notNull(command, "heartbeat命令不能为空");
        Assert.hasText(command.getExecutionId(), "executionId不能为空");
        Assert.hasText(command.getClaimToken(), "claimToken不能为空");
        LocalDateTime heartbeatAt = commandTime(command);
        Assert.notNull(command.getLeaseUntil(), "leaseUntil不能为空");
        Assert.isTrue(command.getLeaseUntil().isAfter(heartbeatAt), "leaseUntil必须晚于heartbeat时间");
        return bizTaskExecutionMapper.heartbeat(command.getExecutionId(), command.getClaimToken(), heartbeatAt,
            command.getLeaseUntil()) == 1;
    }

    /** Persists one monotonic progress revision owned by the active claim. */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProgress(BizTaskProgressDTO progress) {
        Assert.notNull(progress, "progress命令不能为空");
        Assert.hasText(progress.getTaskId(), "taskId不能为空");
        Assert.hasText(progress.getExecutionId(), "executionId不能为空");
        Assert.hasText(progress.getClaimToken(), "claimToken不能为空");
        Assert.notNull(progress.getCurrent(), "progress current不能为空");
        Assert.notNull(progress.getTotal(), "progress total不能为空");
        Assert.notNull(progress.getRevision(), "progress revision不能为空");
        Assert.notNull(progress.getReportedAt(), "progress reportedAt不能为空");
        Assert.isTrue(progress.getCurrent() >= 0 && progress.getTotal() >= 0, "progress值不能为负");
        Assert.isTrue(progress.getTotal() == 0 || progress.getCurrent() <= progress.getTotal(),
            "progress current不能超过total");
        Assert.isTrue(progress.getRevision() > 0, "progress revision必须为正数");
        BizTaskExecutionDO execution = findInternal(progress.getExecutionId());
        if (execution == null) {
            return false;
        }
        Assert.isTrue(progress.getTaskId().equals(execution.getTaskId()), "progress taskId与execution不匹配");
        BizTaskDO task = findTask(execution.getTaskId());
        if (task == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_NOT_FOUND);
        }
        boolean executionUpdated;
        try {
            executionUpdated = bizTaskExecutionMapper.updateProgress(progress.getExecutionId(), progress.getClaimToken(),
                progress.getCurrent(), progress.getTotal(), progress.getMessage(), progress.getRevision(),
                progress.getReportedAt()) == 1;
        } catch (DataAccessException exception) {
            if (isSqliteBusy(exception)) {
                return false;
            }
            throw exception;
        }
        if (!executionUpdated || !isSingleExecutionMode(task)) {
            if (executionUpdated) {
                publishProgressEvent(task, progress);
            }
            return executionUpdated;
        }
        int taskUpdated = bizTaskMapper.mirrorExecutionProgress(task.getTaskId(), progress.getCurrent(),
            progress.getTotal(), progress.getMessage(), progress.getRevision(), progress.getReportedAt());
        if (taskUpdated != 1) {
            throw stateConflict();
        }
        publishProgressEvent(task, progress);
        return true;
    }

    /** Moves the currently claimed attempt to durable retry wait and releases lease ownership. */
    public boolean markRetryWait(BizTaskExecutionDTO command) {
        Assert.notNull(command, "retry-wait命令不能为空");
        Assert.hasText(command.getExecutionId(), "executionId不能为空");
        Assert.hasText(command.getClaimToken(), "claimToken不能为空");
        Assert.notNull(command.getNextAttemptTime(), "nextAttemptTime不能为空");
        LocalDateTime updateTime = commandTime(command);
        Assert.isTrue(command.getNextAttemptTime().isAfter(updateTime), "nextAttemptTime必须晚于更新时间");
        boolean updated = bizTaskExecutionMapper.markRetryWait(command.getExecutionId(), command.getClaimToken(),
            command.getNextAttemptTime(), command.getFailureCode(), command.getFailureMessage(), updateTime) == 1;
        if (updated) {
            BizTaskExecutionDO execution = findInternal(command.getExecutionId());
            if (execution != null) {
                publishExecutionEvent(execution, TaskConstant.SSE_EXECUTION_STATE, "RETRY_SCHEDULED",
                    command.getFailureCode());
            }
        }
        return updated;
    }

    /** Marks one still-pending planned point as missed using optimistic versioning. */
    public boolean markMissed(BizTaskExecutionDTO command) {
        Assert.notNull(command, "missed命令不能为空");
        Assert.hasText(command.getExecutionId(), "executionId不能为空");
        Assert.notNull(command.getVersion(), "version不能为空");
        Assert.notNull(command.getFinishedAt(), "finishedAt不能为空");
        LocalDateTime updateTime = commandTime(command);
        boolean updated = bizTaskExecutionMapper.markMissed(command.getExecutionId(), command.getVersion(),
            command.getFinishedAt(), command.getFailureCode(), command.getFailureMessage(), updateTime) == 1;
        if (updated) {
            BizTaskExecutionDO execution = findInternal(command.getExecutionId());
            if (execution != null) {
                publishExecutionEvent(execution, TaskConstant.SSE_EXECUTION_STATE, "MISSED",
                    command.getFailureCode());
            }
        }
        return updated;
    }

    /** Recovers an expired RUNNING lease without overwriting a heartbeat or a newer worker claim. */
    public boolean recoverExpiredLease(BizTaskExecutionDTO command) {
        Assert.notNull(command, "lease recovery命令不能为空");
        Assert.hasText(command.getExecutionId(), "executionId不能为空");
        Assert.notNull(command.getVersion(), "version不能为空");
        Assert.hasText(command.getClaimToken(), "claimToken不能为空");
        Assert.isTrue("RETRY_WAIT".equals(command.getState()) || "FAILED".equals(command.getState()),
            "租约恢复目标状态只允许RETRY_WAIT或FAILED");
        LocalDateTime updateTime = commandTime(command);
        if ("RETRY_WAIT".equals(command.getState())) {
            Assert.notNull(command.getNextAttemptTime(), "retry recovery必须包含nextAttemptTime");
        } else {
            Assert.notNull(command.getFinishedAt(), "failed recovery必须包含finishedAt");
        }
        boolean updated = bizTaskExecutionMapper.recoverExpiredLease(command.getExecutionId(), command.getVersion(),
            command.getClaimToken(), updateTime, command.getState(), command.getNextAttemptTime(),
            command.getFinishedAt(), command.getFailureCode(), command.getFailureMessage(), updateTime) == 1;
        if (updated) {
            BizTaskExecutionDO execution = findInternal(command.getExecutionId());
            if (execution != null) {
                publishExecutionEvent(execution, TaskConstant.SSE_EXECUTION_STATE, "LEASE_EXPIRED",
                    command.getFailureCode());
            }
        }
        return updated;
    }

    private BizTaskExecutionQueryCondition buildCondition(BizTaskExecutionQueryDTO query,
        BizTaskAccessScopeDTO scope) {
        validateScope(scope);
        BizTaskExecutionQueryDTO source = query == null ? new BizTaskExecutionQueryDTO() : query;
        validateSort(source);
        BizTaskExecutionQueryCondition condition = JSON.parseObject(JSON.toJSONString(source),
            BizTaskExecutionQueryCondition.class);
        condition.setGlobalScope(scope.isGlobalScope());
        condition.setOwnerType(scope.getOwnerType());
        condition.setOwnerId(scope.getOwnerId());
        condition.setOrganizationId(scope.getOrganizationId());
        condition.setAllowedTaskTypes(scope.getAllowedTaskTypes());
        condition.setSortAscending("ASC".equalsIgnoreCase(source.getSortDirection()));
        return condition;
    }

    private void validateSort(BizTaskExecutionQueryDTO query) {
        if (!StringUtils.hasText(query.getSortField())) {
            return;
        }
        boolean supported = "plannedAt".equals(query.getSortField()) || "createTime".equals(query.getSortField())
            || "state".equals(query.getSortField()) || "startedAt".equals(query.getSortField())
            || "finishedAt".equals(query.getSortField()) || "executionId".equals(query.getSortField());
        Assert.isTrue(supported, "不支持的执行排序字段");
        Assert.isTrue("ASC".equalsIgnoreCase(query.getSortDirection())
            || "DESC".equalsIgnoreCase(query.getSortDirection()), "排序方向只允许ASC或DESC");
    }

    private void validateScope(BizTaskAccessScopeDTO scope) {
        Assert.notNull(scope, "任务访问范围不能为空");
        Assert.isTrue(scope.getAllowedTaskTypes() == null || !scope.getAllowedTaskTypes().isEmpty(),
            "任务类型访问范围不能为空集合");
        if (scope.isGlobalScope()) {
            return;
        }
        boolean hasOwner = StringUtils.hasText(scope.getOwnerType()) && StringUtils.hasText(scope.getOwnerId());
        boolean hasOrganization = StringUtils.hasText(scope.getOrganizationId());
        Assert.isTrue(hasOwner || hasOrganization, "非全局任务范围必须包含owner或organization");
    }

    private void validateExecution(BizTaskExecutionDTO execution) {
        Assert.notNull(execution, "业务任务执行不能为空");
        Assert.hasText(execution.getExecutionId(), "executionId不能为空");
        Assert.hasText(execution.getTaskId(), "taskId不能为空");
        Assert.notNull(execution.getPlannedAt(), "plannedAt不能为空");
        Assert.hasText(execution.getState(), "state不能为空");
        Assert.isTrue(TaskExecutionStateEnum.PENDING.name().equals(execution.getState()),
            "新物化执行的初始状态必须为PENDING");
    }

    private void validateScan(LocalDateTime now, int limit) {
        Assert.notNull(now, "扫描时间不能为空");
        Assert.isTrue(limit > 0 && limit <= 1000, "扫描批大小必须在1-1000之间");
    }

    private LocalDateTime commandTime(BizTaskExecutionDTO command) {
        LocalDateTime value = command.getUpdateTime() == null ? command.getHeartbeatAt() : command.getUpdateTime();
        Assert.notNull(value, "命令时间不能为空");
        return value;
    }

    private BizTaskExecutionDO findInternal(String executionId) {
        return bizTaskExecutionService.getOne(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getExecutionId, executionId).last("LIMIT 1"));
    }

    private BizTaskDO findTask(String taskId) {
        return bizTaskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, taskId).last("LIMIT 1"));
    }

    private boolean isSingleExecutionMode(BizTaskDO task) {
        return TaskModeEnum.ONCE.name().equals(task.getTaskMode())
            || TaskModeEnum.AT_TIME.name().equals(task.getTaskMode());
    }

    private ServiceException stateConflict() {
        return new ServiceException(ServiceExceptionEnum.TASK_STATE_CONFLICT);
    }

    private void publishProgressEvent(BizTaskDO task, BizTaskProgressDTO progress) {
        if (businessTaskSseEventPublisher == null) {
            return;
        }
        BusinessTaskSseEvent event = new BusinessTaskSseEvent();
        event.setTopic(TaskConstant.SSE_TASK_PROGRESS);
        event.setTaskId(progress.getTaskId());
        event.setExecutionId(progress.getExecutionId());
        event.setTaskType(task.getTaskType());
        event.setTaskState(task.getState());
        event.setExecutionState(TaskExecutionStateEnum.RUNNING.name());
        event.setEventType("PROGRESS");
        event.setProgressCurrent(progress.getCurrent());
        event.setProgressTotal(progress.getTotal());
        event.setProgressRevision(progress.getRevision());
        event.setTimestamp(toEpochMillis(progress.getReportedAt()));
        businessTaskSseEventPublisher.publish(event);
    }

    private void publishExecutionEvent(BizTaskExecutionDTO execution, String topic, String eventType,
        String failureCode) {
        if (businessTaskSseEventPublisher == null || execution == null) {
            return;
        }
        BizTaskDO task = findTask(execution.getTaskId());
        BusinessTaskSseEvent event = new BusinessTaskSseEvent();
        event.setTopic(topic);
        event.setTaskId(execution.getTaskId());
        event.setExecutionId(execution.getExecutionId());
        event.setTaskType(task == null ? null : task.getTaskType());
        event.setTaskState(task == null ? null : task.getState());
        event.setExecutionState(execution.getState());
        event.setEventType(eventType);
        event.setFailureCode(failureCode);
        event.setProgressCurrent(execution.getProgressCurrent());
        event.setProgressTotal(execution.getProgressTotal());
        event.setProgressRevision(execution.getProgressRevision());
        event.setAttemptNo(execution.getAttemptCount());
        event.setTimestamp(toEpochMillis(execution.getUpdateTime()));
        businessTaskSseEventPublisher.publish(event);
    }

    private void publishExecutionEvent(BizTaskExecutionDO execution, String topic, String eventType,
        String failureCode) {
        publishExecutionEvent(bizTaskExecutionAssembler.doToDto(execution), topic, eventType, failureCode);
    }

    private Long toEpochMillis(LocalDateTime value) {
        return value == null ? null : value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private boolean isSqliteBusy(DataAccessException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException) {
                SQLException sqlException = (SQLException)cause;
                if (sqlException.getErrorCode() == 5 && sqlException.getMessage() != null
                    && sqlException.getMessage().contains("SQLITE_BUSY")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}
