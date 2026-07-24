package io.github.lunasaw.voglander.web.api.task.assembler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.assembler.BusinessTaskDataSanitizer;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskStatisticsDTO;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.enums.task.TaskExecutionStateEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskStateEnum;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskExecutionPageReq;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskPageReq;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskDetailVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskConstraintsVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskEventVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskExecutionDetailVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskExecutionVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskStatisticsVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskVO;

/** Web-boundary conversion for the unified durable task API. */
@Component
public class BusinessTaskWebAssembler {

    public BizTaskQueryDTO pageReqToQuery(BusinessTaskPageReq source) {
        BizTaskQueryDTO target = new BizTaskQueryDTO();
        if (source == null) {
            return target;
        }
        target.setTaskId(source.getTaskId());
        target.setTaskType(source.getTaskType());
        target.setState(source.getState());
        target.setTaskName(source.getTaskName());
        target.setOwnerType(source.getOwnerType());
        target.setOwnerId(source.getOwnerId());
        target.setOrganizationId(source.getOrganizationId());
        target.setSubjectType(source.getSubjectType());
        target.setSubjectId(source.getSubjectId());
        target.setBizKey(source.getBizKey());
        target.setCreateStartTime(toLocalDateTime(source.getCreateStartTime()));
        target.setCreateEndTime(toLocalDateTime(source.getCreateEndTime()));
        target.setScheduleStartTime(toLocalDateTime(source.getScheduleStartTime()));
        target.setScheduleEndTime(toLocalDateTime(source.getScheduleEndTime()));
        target.setSortField(source.getSortField());
        target.setSortDirection(source.getSortDirection());
        return target;
    }

    public BizTaskExecutionQueryDTO executionPageReqToQuery(BusinessTaskExecutionPageReq source) {
        BizTaskExecutionQueryDTO target = new BizTaskExecutionQueryDTO();
        if (source == null) {
            return target;
        }
        target.setExecutionId(source.getExecutionId());
        target.setTaskId(source.getTaskId());
        target.setState(source.getState());
        target.setRetryable(source.getRetryable());
        target.setPlannedStartTime(toLocalDateTime(source.getPlannedStartTime()));
        target.setPlannedEndTime(toLocalDateTime(source.getPlannedEndTime()));
        target.setCreateStartTime(toLocalDateTime(source.getCreateStartTime()));
        target.setCreateEndTime(toLocalDateTime(source.getCreateEndTime()));
        target.setSortField(source.getSortField());
        target.setSortDirection(source.getSortDirection());
        return target;
    }

    public BusinessTaskVO toTaskVO(BizTaskDTO source) {
        if (source == null) {
            return null;
        }
        BusinessTaskVO target = new BusinessTaskVO();
        target.setCreateTime(toEpochMilli(source.getCreateTime()));
        target.setUpdateTime(toEpochMilli(source.getUpdateTime()));
        target.setTaskId(source.getTaskId());
        target.setTaskType(source.getTaskType());
        target.setTaskName(source.getTaskName());
        target.setDescription(source.getDescription());
        target.setTaskMode(source.getTaskMode());
        target.setScheduleStartTime(toEpochMilli(source.getScheduleStartTime()));
        target.setScheduleEndTime(toEpochMilli(source.getScheduleEndTime()));
        target.setIntervalSeconds(source.getIntervalSeconds());
        target.setNextPlanTime(toEpochMilli(source.getNextPlanTime()));
        target.setScheduleVersion(source.getScheduleVersion());
        target.setState(source.getState());
        target.setPriority(source.getPriority());
        target.setLastExecutionId(source.getLastExecutionId());
        target.setLastExecuteTime(toEpochMilli(source.getLastExecuteTime()));
        target.setCompletedTime(toEpochMilli(source.getCompletedTime()));
        target.setPlannedCount(source.getPlannedCount());
        target.setSuccessCount(source.getSuccessCount());
        target.setFailedCount(source.getFailedCount());
        target.setMissedCount(source.getMissedCount());
        target.setCancelledCount(source.getCancelledCount());
        target.setProgressCurrent(source.getProgressCurrent());
        target.setProgressTotal(source.getProgressTotal());
        target.setProgressMessage(source.getProgressMessage());
        target.setProgressRevision(source.getProgressRevision());
        target.setBizKey(source.getBizKey());
        target.setSubjectType(source.getSubjectType());
        target.setSubjectId(source.getSubjectId());
        target.setResultRefType(source.getResultRefType());
        target.setResultRefId(source.getResultRefId());
        target.setResultSummary(BusinessTaskDataSanitizer.sanitizeJson(source.getResultSummary()));
        target.setLastFailureCode(source.getLastFailureCode());
        target.setLastFailureMessage(source.getLastFailureMessage());
        target.setOriginTaskId(source.getOriginTaskId());
        target.setOriginExecutionId(source.getOriginExecutionId());
        target.setOwnerType(source.getOwnerType());
        target.setOwnerId(source.getOwnerId());
        target.setOrganizationId(source.getOrganizationId());
        target.setVersion(source.getVersion());
        return target;
    }

    public BusinessTaskDetailVO toTaskDetailVO(BizTaskDTO source, BizTaskExecutionDTO activeExecution,
        List<String> capabilities) {
        BusinessTaskVO base = toTaskVO(source);
        if (base == null) {
            return null;
        }
        BusinessTaskDetailVO target = new BusinessTaskDetailVO();
        copyTask(base, target);
        target.setActiveExecution(toExecutionVO(activeExecution));
        target.setCapabilities(capabilities);
        return target;
    }

    public BusinessTaskExecutionVO toExecutionVO(BizTaskExecutionDTO source) {
        if (source == null) {
            return null;
        }
        BusinessTaskExecutionVO target = new BusinessTaskExecutionVO();
        target.setExecutionId(source.getExecutionId());
        target.setTaskId(source.getTaskId());
        target.setScheduleVersion(source.getScheduleVersion());
        target.setPlannedAt(toEpochMilli(source.getPlannedAt()));
        target.setDeadlineAt(toEpochMilli(source.getDeadlineAt()));
        target.setState(source.getState());
        target.setAttemptCount(source.getAttemptCount());
        target.setMaxAttempts(source.getMaxAttempts());
        target.setNextAttemptTime(toEpochMilli(source.getNextAttemptTime()));
        target.setStartedAt(toEpochMilli(source.getStartedAt()));
        target.setHeartbeatAt(toEpochMilli(source.getHeartbeatAt()));
        target.setFinishedAt(toEpochMilli(source.getFinishedAt()));
        target.setProgressCurrent(source.getProgressCurrent());
        target.setProgressTotal(source.getProgressTotal());
        target.setProgressMessage(source.getProgressMessage());
        target.setProgressRevision(source.getProgressRevision());
        target.setResultRefType(source.getResultRefType());
        target.setResultRefId(source.getResultRefId());
        target.setResultSummary(BusinessTaskDataSanitizer.sanitizeJson(source.getResultSummary()));
        target.setFailureCode(source.getFailureCode());
        target.setFailureMessage(source.getFailureMessage());
        target.setRetryable(source.getRetryable());
        target.setRetryOriginExecutionId(source.getRetryOriginExecutionId());
        return target;
    }

    public BusinessTaskExecutionDetailVO toExecutionDetailVO(BizTaskExecutionDTO source,
        List<BizTaskEventDTO> events) {
        BusinessTaskExecutionVO base = toExecutionVO(source);
        if (base == null) {
            return null;
        }
        BusinessTaskExecutionDetailVO target = new BusinessTaskExecutionDetailVO();
        copyExecution(base, target);
        target.setEvents(events == null ? java.util.Collections.emptyList() : events.stream()
            .map(this::toEventVO).collect(Collectors.toList()));
        return target;
    }

    public BusinessTaskEventVO toEventVO(BizTaskEventDTO source) {
        if (source == null) {
            return null;
        }
        BusinessTaskEventVO target = new BusinessTaskEventVO();
        target.setEventId(source.getEventId());
        target.setTaskId(source.getTaskId());
        target.setExecutionId(source.getExecutionId());
        target.setEventType(source.getEventType());
        target.setFromState(source.getFromState());
        target.setToState(source.getToState());
        target.setAttemptNo(source.getAttemptNo());
        target.setProgressCurrent(source.getProgressCurrent());
        target.setProgressTotal(source.getProgressTotal());
        target.setProgressMessage(source.getProgressMessage());
        target.setFailureCode(source.getFailureCode());
        target.setFailureMessage(source.getFailureMessage());
        target.setActorType(source.getActorType());
        target.setActorId(source.getActorId());
        target.setEventData(BusinessTaskDataSanitizer.sanitizeJson(source.getEventData()));
        target.setOccurredAt(toEpochMilli(source.getOccurredAt()));
        return target;
    }

    public BusinessTaskStatisticsVO toStatisticsVO(BizTaskStatisticsDTO source) {
        if (source == null) {
            return null;
        }
        BusinessTaskStatisticsVO target = new BusinessTaskStatisticsVO();
        target.setScheduledCount(source.getScheduledCount());
        target.setRunningCount(source.getRunningCount());
        target.setPausedCount(source.getPausedCount());
        target.setCancellingCount(source.getCancellingCount());
        target.setCompletedTodayCount(source.getCompletedTodayCount());
        target.setFailedCount(source.getFailedCount());
        return target;
    }

    public BusinessTaskConstraintsVO toConstraintsVO(List<String> registeredTypes,
        Map<String, List<String>> capabilities) {
        BusinessTaskConstraintsVO target = new BusinessTaskConstraintsVO();
        target.setTaskTypes(registeredTypes == null ? java.util.Collections.emptyList() : registeredTypes);
        target.setTaskModes(java.util.Arrays.stream(TaskModeEnum.values()).map(Enum::name)
            .collect(Collectors.toList()));
        target.setTaskStates(java.util.Arrays.stream(TaskStateEnum.values()).map(Enum::name)
            .collect(Collectors.toList()));
        target.setExecutionStates(java.util.Arrays.stream(TaskExecutionStateEnum.values()).map(Enum::name)
            .collect(Collectors.toList()));
        target.setCapabilities(capabilities == null ? java.util.Collections.emptyMap() : capabilities);
        target.setMaxPlannedCount(TaskConstant.DEFAULT_MAX_PLANNED_COUNT);
        target.setMaxScheduleDurationDays(TaskConstant.DEFAULT_MAX_SCHEDULE_DURATION_DAYS);
        target.setMaxPayloadBytes(TaskConstant.DEFAULT_MAX_PAYLOAD_BYTES);
        return target;
    }

    public static Long toEpochMilli(LocalDateTime source) {
        return source == null ? null : source.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static LocalDateTime toLocalDateTime(Long source) {
        return source == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(source), ZoneId.systemDefault());
    }

    private static void copyTask(BusinessTaskVO source, BusinessTaskDetailVO target) {
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setTaskId(source.getTaskId());
        target.setTaskType(source.getTaskType());
        target.setTaskName(source.getTaskName());
        target.setDescription(source.getDescription());
        target.setTaskMode(source.getTaskMode());
        target.setScheduleStartTime(source.getScheduleStartTime());
        target.setScheduleEndTime(source.getScheduleEndTime());
        target.setIntervalSeconds(source.getIntervalSeconds());
        target.setNextPlanTime(source.getNextPlanTime());
        target.setScheduleVersion(source.getScheduleVersion());
        target.setState(source.getState());
        target.setPriority(source.getPriority());
        target.setLastExecutionId(source.getLastExecutionId());
        target.setLastExecuteTime(source.getLastExecuteTime());
        target.setCompletedTime(source.getCompletedTime());
        target.setPlannedCount(source.getPlannedCount());
        target.setSuccessCount(source.getSuccessCount());
        target.setFailedCount(source.getFailedCount());
        target.setMissedCount(source.getMissedCount());
        target.setCancelledCount(source.getCancelledCount());
        target.setProgressCurrent(source.getProgressCurrent());
        target.setProgressTotal(source.getProgressTotal());
        target.setProgressMessage(source.getProgressMessage());
        target.setProgressRevision(source.getProgressRevision());
        target.setBizKey(source.getBizKey());
        target.setSubjectType(source.getSubjectType());
        target.setSubjectId(source.getSubjectId());
        target.setResultRefType(source.getResultRefType());
        target.setResultRefId(source.getResultRefId());
        target.setResultSummary(source.getResultSummary());
        target.setLastFailureCode(source.getLastFailureCode());
        target.setLastFailureMessage(source.getLastFailureMessage());
        target.setOriginTaskId(source.getOriginTaskId());
        target.setOriginExecutionId(source.getOriginExecutionId());
        target.setOwnerType(source.getOwnerType());
        target.setOwnerId(source.getOwnerId());
        target.setOrganizationId(source.getOrganizationId());
        target.setVersion(source.getVersion());
    }

    private static void copyExecution(BusinessTaskExecutionVO source, BusinessTaskExecutionDetailVO target) {
        target.setExecutionId(source.getExecutionId());
        target.setTaskId(source.getTaskId());
        target.setScheduleVersion(source.getScheduleVersion());
        target.setPlannedAt(source.getPlannedAt());
        target.setDeadlineAt(source.getDeadlineAt());
        target.setState(source.getState());
        target.setAttemptCount(source.getAttemptCount());
        target.setMaxAttempts(source.getMaxAttempts());
        target.setNextAttemptTime(source.getNextAttemptTime());
        target.setStartedAt(source.getStartedAt());
        target.setHeartbeatAt(source.getHeartbeatAt());
        target.setFinishedAt(source.getFinishedAt());
        target.setProgressCurrent(source.getProgressCurrent());
        target.setProgressTotal(source.getProgressTotal());
        target.setProgressMessage(source.getProgressMessage());
        target.setProgressRevision(source.getProgressRevision());
        target.setResultRefType(source.getResultRefType());
        target.setResultRefId(source.getResultRefId());
        target.setResultSummary(source.getResultSummary());
        target.setFailureCode(source.getFailureCode());
        target.setFailureMessage(source.getFailureMessage());
        target.setRetryable(source.getRetryable());
        target.setRetryOriginExecutionId(source.getRetryOriginExecutionId());
    }
}
