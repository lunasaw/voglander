package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent;
import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskStateEnum;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;
import io.github.lunasaw.voglander.manager.assembler.BizTaskExecutionAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCommandDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskStatisticsDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;
import io.github.lunasaw.voglander.repository.mapper.BizTaskMapper;

/** Transaction boundary for durable task facts. */
@Component
public class BizTaskManager {

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskMapper bizTaskMapper;

    @Autowired
    private BizTaskExecutionService bizTaskExecutionService;

    @Autowired
    private BizTaskAssembler bizTaskAssembler;

    @Autowired
    private BizTaskExecutionAssembler bizTaskExecutionAssembler;

    @Autowired(required = false)
    private BusinessTaskSseEventPublisher businessTaskSseEventPublisher;

    /**
     * Persists a task and its optional first execution atomically.
     *
     * <p>A non-blank owner/type/idempotency key is checked before any conversion or insert. Replays return the
     * original accepted task and never persist the supplied first execution.</p>
     *
     * @param task task fact prepared by the trusted domain service
     * @param firstExecution first ONCE execution, or {@code null} for a future schedule
     * @return the accepted task
     */
    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO create(BizTaskDTO task, BizTaskExecutionDTO firstExecution) {
        validateTask(task);
        BizTaskDTO existing = findByIdempotency(task.getOwnerType(), task.getOwnerId(), task.getTaskType(),
            task.getIdempotencyKey());
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        BizTaskDO taskDO = bizTaskAssembler.dtoToDo(task);
        taskDO.setCreateTime(taskDO.getCreateTime() == null ? now : taskDO.getCreateTime());
        taskDO.setUpdateTime(taskDO.getUpdateTime() == null ? now : taskDO.getUpdateTime());
        if (!bizTaskService.save(taskDO)) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "业务任务插入失败");
        }

        if (firstExecution != null) {
            Assert.hasText(firstExecution.getExecutionId(), "executionId不能为空");
            Assert.isTrue(task.getTaskId().equals(firstExecution.getTaskId()), "首执行必须属于当前任务");
            BizTaskExecutionDO executionDO = bizTaskExecutionAssembler.dtoToDo(firstExecution);
            executionDO.setCreateTime(executionDO.getCreateTime() == null ? now : executionDO.getCreateTime());
            executionDO.setUpdateTime(executionDO.getUpdateTime() == null ? now : executionDO.getUpdateTime());
            if (!bizTaskExecutionService.save(executionDO)) {
                throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "业务任务首执行插入失败");
            }
        }
        publishTaskEvent(taskDO, null, taskDO.getState(), "CREATED", taskDO.getCreateTime());
        return bizTaskAssembler.doToDto(taskDO);
    }

    /** Finds an accepted task using the database-authoritative idempotency identity. */
    public BizTaskDTO findByIdempotency(String ownerType, String ownerId, String taskType, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        Assert.hasText(ownerType, "ownerType不能为空");
        Assert.hasText(ownerId, "ownerId不能为空");
        Assert.hasText(taskType, "taskType不能为空");
        BizTaskDO existing = bizTaskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getOwnerType, ownerType)
            .eq(BizTaskDO::getOwnerId, ownerId)
            .eq(BizTaskDO::getTaskType, taskType)
            .eq(BizTaskDO::getIdempotencyKey, idempotencyKey)
            .last("LIMIT 1"));
        return bizTaskAssembler.doToDto(existing);
    }

    /** Returns a safely mapped deterministic task page within the trusted access scope. */
    public Page<BizTaskDTO> getPage(BizTaskQueryDTO query, BizTaskAccessScopeDTO scope, int page, int size) {
        Assert.isTrue(page > 0, "页码必须大于0");
        Assert.isTrue(size > 0 && size <= 1000, "页大小必须在1-1000之间");
        LambdaQueryWrapper<BizTaskDO> wrapper = buildQuery(query, scope);
        applySort(wrapper, query);
        Page<BizTaskDO> source = bizTaskService.page(new Page<BizTaskDO>(page, size), wrapper);
        Page<BizTaskDTO> target = new Page<BizTaskDTO>(page, size);
        target.setCurrent(source.getCurrent());
        target.setSize(source.getSize());
        target.setTotal(source.getTotal());
        target.setPages(source.getPages());
        List<BizTaskDTO> records = source.getRecords().stream()
            .map(bizTaskAssembler::doToSafeDto)
            .collect(Collectors.toList());
        target.setRecords(records);
        return target;
    }

    /** Returns null for both missing and out-of-scope task identities. */
    public BizTaskDTO getByTaskId(String taskId, BizTaskAccessScopeDTO scope) {
        Assert.hasText(taskId, "taskId不能为空");
        LambdaQueryWrapper<BizTaskDO> wrapper = new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, taskId);
        applyScope(wrapper, scope);
        return bizTaskAssembler.doToSafeDto(bizTaskService.getOne(wrapper.last("LIMIT 1")));
    }

    /** Returns due tasks with internal schedule/version facts for the trusted scheduler boundary. */
    public List<BizTaskDTO> findDueTasks(LocalDateTime now, int limit) {
        Assert.notNull(now, "调度扫描时间不能为空");
        Assert.isTrue(limit > 0 && limit <= 1000, "调度扫描批大小必须在1-1000之间");
        return bizTaskMapper.selectDueTasks(now, limit).stream()
            .map(bizTaskAssembler::doToDto)
            .collect(Collectors.toList());
    }

    /** Re-reads one task with internal cursor/version facts inside the scheduler transaction. */
    public BizTaskDTO getForScheduling(String taskId) {
        Assert.hasText(taskId, "taskId不能为空");
        BizTaskDO task = bizTaskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, taskId).last("LIMIT 1"));
        return bizTaskAssembler.doToDto(task);
    }

    /** Returns the immutable payload and Handler binding facts for the trusted Worker boundary. */
    public BizTaskDTO getForExecution(String taskId) {
        Assert.hasText(taskId, "taskId不能为空");
        BizTaskDO task = bizTaskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, taskId).last("LIMIT 1"));
        return bizTaskAssembler.doToDto(task);
    }

    /** Advances only the cursor version observed by the transactional scheduler re-read. */
    public boolean advanceScheduleCursor(String taskId, int expectedVersion, LocalDateTime nextPlanTime,
        String state, LocalDateTime updateTime) {
        return advanceScheduleCursor(taskId, expectedVersion, nextPlanTime, state, updateTime, false);
    }

    /** Advances the cursor and atomically accounts for one newly persisted MISSED execution. */
    public boolean advanceScheduleCursor(String taskId, int expectedVersion, LocalDateTime nextPlanTime,
        String state, LocalDateTime updateTime, boolean incrementMissed) {
        Assert.hasText(taskId, "taskId不能为空");
        Assert.isTrue(expectedVersion >= 0, "expectedVersion不能为负数");
        Assert.isTrue(TaskStateEnum.RUNNING.name().equals(state), "物化计划点后任务必须处于RUNNING");
        Assert.notNull(updateTime, "cursor更新时间不能为空");
        if (incrementMissed) {
            return bizTaskMapper.advanceCursorAndIncrementMissed(taskId, expectedVersion, nextPlanTime, state,
                updateTime) == 1;
        }
        return bizTaskMapper.advanceCursor(taskId, expectedVersion, nextPlanTime, state, updateTime) == 1;
    }

    /** Marks a fully materialized scheduled task terminal using its latest aggregate version. */
    public boolean markNaturalTerminal(String taskId, int expectedVersion, String terminalState,
        LocalDateTime completedTime) {
        Assert.hasText(taskId, "taskId不能为空");
        Assert.isTrue(expectedVersion >= 0, "expectedVersion不能为负数");
        Assert.isTrue(TaskStateEnum.COMPLETED.name().equals(terminalState)
            || TaskStateEnum.PARTIAL_COMPLETED.name().equals(terminalState)
            || TaskStateEnum.FAILED.name().equals(terminalState)
            || TaskStateEnum.CANCELLED.name().equals(terminalState), "自然终态不合法");
        Assert.notNull(completedTime, "任务完成时间不能为空");
        return bizTaskMapper.markNaturalTerminal(taskId, expectedVersion, terminalState, completedTime) == 1;
    }

    /** Calculates task-center counters using exactly the same trusted access scope as queries. */
    public BizTaskStatisticsDTO getStatistics(BizTaskAccessScopeDTO scope) {
        BizTaskStatisticsDTO statistics = new BizTaskStatisticsDTO();
        statistics.setScheduledCount(countState(scope, "SCHEDULED"));
        statistics.setRunningCount(countState(scope, "RUNNING"));
        statistics.setPausedCount(countState(scope, "PAUSED"));
        statistics.setCancellingCount(countState(scope, "CANCELLING"));
        statistics.setFailedCount(countState(scope, "FAILED"));
        LambdaQueryWrapper<BizTaskDO> completedToday = new LambdaQueryWrapper<BizTaskDO>()
            .in(BizTaskDO::getState, "COMPLETED", "PARTIAL_COMPLETED")
            .ge(BizTaskDO::getCompletedTime, LocalDate.now().atStartOfDay());
        applyScope(completedToday, scope);
        statistics.setCompletedTodayCount(bizTaskService.count(completedToday));
        return statistics;
    }

    /** Pauses an active task while preserving its schedule cursor. */
    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO pause(BizTaskCommandDTO command) {
        BizTaskDO task = requireTask(command);
        TaskStateEnum state = taskState(task);
        if (state == TaskStateEnum.PAUSED) {
            return bizTaskAssembler.doToDto(task);
        }
        if (state != TaskStateEnum.SCHEDULED && state != TaskStateEnum.RUNNING) {
            throw stateConflict();
        }
        return transition(command, Arrays.asList(TaskStateEnum.SCHEDULED.name(), TaskStateEnum.RUNNING.name()),
            TaskStateEnum.PAUSED);
    }

    /** Resumes a paused task to the state appropriate for its schedule mode. */
    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO resume(BizTaskCommandDTO command) {
        BizTaskDO task = requireTask(command);
        TaskStateEnum state = taskState(task);
        if (state == TaskStateEnum.SCHEDULED || state == TaskStateEnum.RUNNING) {
            return bizTaskAssembler.doToDto(task);
        }
        if (state != TaskStateEnum.PAUSED) {
            throw stateConflict();
        }
        TaskStateEnum target = taskMode(task) == TaskModeEnum.ONCE
            ? TaskStateEnum.RUNNING : TaskStateEnum.SCHEDULED;
        return transition(command, Collections.singletonList(TaskStateEnum.PAUSED.name()), target);
    }

    /** Moves an active task into cooperative cancellation. */
    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO cancel(BizTaskCommandDTO command) {
        BizTaskDO task = requireTask(command);
        TaskStateEnum state = taskState(task);
        if (state == TaskStateEnum.CANCELLING || state == TaskStateEnum.CANCELLED) {
            return bizTaskAssembler.doToDto(task);
        }
        if (state != TaskStateEnum.SCHEDULED && state != TaskStateEnum.RUNNING
            && state != TaskStateEnum.PAUSED) {
            throw stateConflict();
        }
        return transition(command,
            Arrays.asList(TaskStateEnum.SCHEDULED.name(), TaskStateEnum.RUNNING.name(),
                TaskStateEnum.PAUSED.name()),
            TaskStateEnum.CANCELLING);
    }

    /** Replaces a paused future schedule and advances its schedule version atomically. */
    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO reschedule(BizTaskCommandDTO command) {
        BizTaskDO task = requireTask(command);
        if (taskState(task) != TaskStateEnum.PAUSED) {
            throw stateConflict();
        }
        TaskModeEnum mode = taskMode(task);
        if (mode == TaskModeEnum.ONCE) {
            throw stateConflict();
        }
        Assert.notNull(command.getScheduleStartTime(), "计划开始时间不能为空");

        LocalDateTime scheduleEndTime = null;
        Long intervalSeconds = null;
        long newPlanPoints = 1L;
        if (mode == TaskModeEnum.FIXED_RATE) {
            Assert.notNull(command.getScheduleEndTime(), "固定间隔计划结束时间不能为空");
            Assert.notNull(command.getIntervalSeconds(), "固定间隔秒数不能为空");
            Assert.isTrue(command.getIntervalSeconds() > 0, "固定间隔秒数必须大于0");
            Assert.isTrue(!command.getScheduleEndTime().isBefore(command.getScheduleStartTime()),
                "计划结束时间不能早于开始时间");
            scheduleEndTime = command.getScheduleEndTime();
            intervalSeconds = command.getIntervalSeconds();
            long durationSeconds = Duration.between(command.getScheduleStartTime(), scheduleEndTime).getSeconds();
            newPlanPoints = durationSeconds / intervalSeconds + 1L;
        }

        long terminalCount = (long) nonNull(task.getSuccessCount()) + nonNull(task.getFailedCount())
            + nonNull(task.getMissedCount()) + nonNull(task.getCancelledCount());
        long plannedCount = terminalCount + newPlanPoints;
        Assert.isTrue(plannedCount <= Integer.MAX_VALUE, "计划点数量超过整数范围");
        int scheduleVersion = Math.addExact(nonNull(task.getScheduleVersion()), 1);
        LocalDateTime updateTime = commandTime(command);
        int affected = bizTaskMapper.reschedulePaused(command.getTaskId(), command.getExpectedVersion(),
            command.getScheduleStartTime(), scheduleEndTime, intervalSeconds, command.getScheduleStartTime(),
            scheduleVersion, (int) plannedCount, updateTime);
        if (affected != 1) {
            throw stateConflict();
        }
        return reload(command.getTaskId());
    }

    private LambdaQueryWrapper<BizTaskDO> buildQuery(BizTaskQueryDTO query, BizTaskAccessScopeDTO scope) {
        BizTaskQueryDTO condition = query == null ? new BizTaskQueryDTO() : query;
        LambdaQueryWrapper<BizTaskDO> wrapper = new LambdaQueryWrapper<BizTaskDO>()
            .eq(StringUtils.hasText(condition.getTaskId()), BizTaskDO::getTaskId, condition.getTaskId())
            .eq(StringUtils.hasText(condition.getTaskType()), BizTaskDO::getTaskType, condition.getTaskType())
            .eq(StringUtils.hasText(condition.getTaskMode()), BizTaskDO::getTaskMode, condition.getTaskMode())
            .eq(StringUtils.hasText(condition.getState()), BizTaskDO::getState, condition.getState())
            .like(StringUtils.hasText(condition.getTaskName()), BizTaskDO::getTaskName, condition.getTaskName())
            .eq(StringUtils.hasText(condition.getOwnerType()), BizTaskDO::getOwnerType, condition.getOwnerType())
            .eq(StringUtils.hasText(condition.getOwnerId()), BizTaskDO::getOwnerId, condition.getOwnerId())
            .eq(StringUtils.hasText(condition.getOrganizationId()), BizTaskDO::getOrganizationId,
                condition.getOrganizationId())
            .eq(StringUtils.hasText(condition.getSubjectType()), BizTaskDO::getSubjectType,
                condition.getSubjectType())
            .eq(StringUtils.hasText(condition.getSubjectId()), BizTaskDO::getSubjectId, condition.getSubjectId())
            .eq(StringUtils.hasText(condition.getBizKey()), BizTaskDO::getBizKey, condition.getBizKey())
            .ge(condition.getCreateStartTime() != null, BizTaskDO::getCreateTime, condition.getCreateStartTime())
            .le(condition.getCreateEndTime() != null, BizTaskDO::getCreateTime, condition.getCreateEndTime())
            .ge(condition.getScheduleStartTime() != null, BizTaskDO::getScheduleStartTime,
                condition.getScheduleStartTime())
            .le(condition.getScheduleEndTime() != null, BizTaskDO::getScheduleEndTime,
                condition.getScheduleEndTime());
        applyScope(wrapper, scope);
        return wrapper;
    }

    private void applyScope(LambdaQueryWrapper<BizTaskDO> wrapper, BizTaskAccessScopeDTO scope) {
        Assert.notNull(scope, "任务访问范围不能为空");
        if (scope.isGlobalScope()) {
            return;
        }
        boolean hasOwner = StringUtils.hasText(scope.getOwnerType()) && StringUtils.hasText(scope.getOwnerId());
        boolean hasOrganization = StringUtils.hasText(scope.getOrganizationId());
        Assert.isTrue(hasOwner || hasOrganization, "非全局任务范围必须包含 owner 或 organization");
        wrapper.eq(hasOwner, BizTaskDO::getOwnerType, scope.getOwnerType())
            .eq(hasOwner, BizTaskDO::getOwnerId, scope.getOwnerId())
            .eq(hasOrganization, BizTaskDO::getOrganizationId, scope.getOrganizationId());
    }

    private void applySort(LambdaQueryWrapper<BizTaskDO> wrapper, BizTaskQueryDTO query) {
        String sortField = query == null ? null : query.getSortField();
        String direction = query == null ? null : query.getSortDirection();
        if (!StringUtils.hasText(sortField)) {
            wrapper.orderByDesc(BizTaskDO::getCreateTime).orderByDesc(BizTaskDO::getTaskId);
            return;
        }
        boolean ascending = "ASC".equalsIgnoreCase(direction);
        Assert.isTrue(ascending || "DESC".equalsIgnoreCase(direction), "排序方向只允许 ASC 或 DESC");
        if ("createTime".equals(sortField)) {
            order(wrapper, ascending, BizTaskDO::getCreateTime);
        } else if ("taskName".equals(sortField)) {
            order(wrapper, ascending, BizTaskDO::getTaskName);
        } else if ("state".equals(sortField)) {
            order(wrapper, ascending, BizTaskDO::getState);
        } else if ("nextPlanTime".equals(sortField)) {
            order(wrapper, ascending, BizTaskDO::getNextPlanTime);
        } else if ("completedTime".equals(sortField)) {
            order(wrapper, ascending, BizTaskDO::getCompletedTime);
        } else if ("scheduleStartTime".equals(sortField)) {
            order(wrapper, ascending, BizTaskDO::getScheduleStartTime);
        } else if ("taskId".equals(sortField)) {
            order(wrapper, ascending, BizTaskDO::getTaskId);
            return;
        } else {
            throw new IllegalArgumentException("不支持的任务排序字段: " + sortField);
        }
        wrapper.orderByDesc(BizTaskDO::getTaskId);
    }

    private <T> void order(LambdaQueryWrapper<BizTaskDO> wrapper, boolean ascending,
        com.baomidou.mybatisplus.core.toolkit.support.SFunction<BizTaskDO, T> column) {
        if (ascending) {
            wrapper.orderByAsc(column);
        } else {
            wrapper.orderByDesc(column);
        }
    }

    private long countState(BizTaskAccessScopeDTO scope, String state) {
        LambdaQueryWrapper<BizTaskDO> wrapper = new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getState, state);
        applyScope(wrapper, scope);
        return bizTaskService.count(wrapper);
    }

    private BizTaskDO requireTask(BizTaskCommandDTO command) {
        Assert.notNull(command, "任务控制命令不能为空");
        Assert.hasText(command.getTaskId(), "taskId不能为空");
        Assert.notNull(command.getExpectedVersion(), "expectedVersion不能为空");
        BizTaskDO task = bizTaskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, command.getTaskId()).last("LIMIT 1"));
        if (task == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_NOT_FOUND);
        }
        return task;
    }

    private BizTaskDTO transition(BizTaskCommandDTO command, List<String> fromStates, TaskStateEnum target) {
        BizTaskDO before = bizTaskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, command.getTaskId()).last("LIMIT 1"));
        int affected = bizTaskMapper.transitionState(command.getTaskId(), command.getExpectedVersion(), fromStates,
            target.name(), commandTime(command));
        if (affected != 1) {
            throw stateConflict();
        }
        BizTaskDTO updated = reload(command.getTaskId());
        String eventType = target == TaskStateEnum.PAUSED ? "PAUSED"
            : target == TaskStateEnum.CANCELLING ? "CANCELLING" : "RESUMED";
        if (before != null) {
            publishTaskEvent(before, before.getState(), target.name(), eventType, commandTime(command));
        }
        return updated;
    }

    private BizTaskDTO reload(String taskId) {
        BizTaskDO task = bizTaskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, taskId).last("LIMIT 1"));
        if (task == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_NOT_FOUND);
        }
        return bizTaskAssembler.doToDto(task);
    }

    private TaskStateEnum taskState(BizTaskDO task) {
        try {
            return TaskStateEnum.valueOf(task.getState());
        } catch (RuntimeException exception) {
            throw stateConflict();
        }
    }

    private TaskModeEnum taskMode(BizTaskDO task) {
        try {
            return TaskModeEnum.valueOf(task.getTaskMode());
        } catch (RuntimeException exception) {
            throw new ServiceException(ServiceExceptionEnum.TASK_SCHEDULE_INVALID);
        }
    }

    private LocalDateTime commandTime(BizTaskCommandDTO command) {
        return command.getRequestedAt() == null ? LocalDateTime.now() : command.getRequestedAt();
    }

    private int nonNull(Integer value) {
        return value == null ? 0 : value;
    }

    private ServiceException stateConflict() {
        return new ServiceException(ServiceExceptionEnum.TASK_STATE_CONFLICT);
    }

    private void publishTaskEvent(BizTaskDO task, String fromState, String toState, String eventType,
        LocalDateTime occurredAt) {
        if (businessTaskSseEventPublisher == null || task == null) {
            return;
        }
        BusinessTaskSseEvent event = new BusinessTaskSseEvent();
        event.setTopic(TaskConstant.SSE_TASK_STATE);
        event.setTaskId(task.getTaskId());
        event.setTaskType(task.getTaskType());
        event.setTaskState(toState);
        event.setEventType(eventType);
        event.setScheduleVersion(task.getScheduleVersion());
        event.setTimestamp(toEpochMillis(occurredAt));
        businessTaskSseEventPublisher.publish(event);
    }

    private Long toEpochMillis(LocalDateTime value) {
        return value == null ? null : value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void validateTask(BizTaskDTO task) {
        Assert.notNull(task, "业务任务不能为空");
        Assert.hasText(task.getTaskId(), "taskId不能为空");
        Assert.hasText(task.getTaskType(), "taskType不能为空");
        Assert.hasText(task.getOwnerType(), "ownerType不能为空");
        Assert.hasText(task.getOwnerId(), "ownerId不能为空");
    }
}
