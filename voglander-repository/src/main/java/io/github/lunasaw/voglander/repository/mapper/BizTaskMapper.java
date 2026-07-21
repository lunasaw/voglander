package io.github.lunasaw.voglander.repository.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.github.lunasaw.voglander.repository.entity.BizTaskDO;

@Mapper
public interface BizTaskMapper extends BaseMapper<BizTaskDO> {
    List<BizTaskDO> selectDueTasks(@Param("now") LocalDateTime now, @Param("limit") int limit);
    int advanceCursor(@Param("taskId") String taskId, @Param("version") int version,
        @Param("nextPlanTime") LocalDateTime nextPlanTime, @Param("state") String state,
        @Param("updateTime") LocalDateTime updateTime);
    int advanceCursorAndIncrementMissed(@Param("taskId") String taskId, @Param("version") int version,
        @Param("nextPlanTime") LocalDateTime nextPlanTime, @Param("state") String state,
        @Param("updateTime") LocalDateTime updateTime);
    int markNaturalTerminal(@Param("taskId") String taskId, @Param("version") int version,
        @Param("terminalState") String terminalState, @Param("completedTime") LocalDateTime completedTime);
    int transitionState(@Param("taskId") String taskId, @Param("version") int version,
        @Param("fromStates") List<String> fromStates, @Param("toState") String toState,
        @Param("updateTime") LocalDateTime updateTime);
    int reschedulePaused(@Param("taskId") String taskId, @Param("version") int version,
        @Param("scheduleStartTime") LocalDateTime scheduleStartTime,
        @Param("scheduleEndTime") LocalDateTime scheduleEndTime,
        @Param("intervalSeconds") Long intervalSeconds,
        @Param("nextPlanTime") LocalDateTime nextPlanTime,
        @Param("scheduleVersion") int scheduleVersion,
        @Param("plannedCount") int plannedCount,
        @Param("updateTime") LocalDateTime updateTime);
    int mirrorExecutionProgress(@Param("taskId") String taskId, @Param("progressCurrent") long progressCurrent,
        @Param("progressTotal") long progressTotal, @Param("progressMessage") String progressMessage,
        @Param("progressRevision") long progressRevision, @Param("updateTime") LocalDateTime updateTime);
    int applyExecutionSuccess(@Param("taskId") String taskId, @Param("version") int version,
        @Param("lastExecutionId") String lastExecutionId,
        @Param("lastExecuteTime") LocalDateTime lastExecuteTime,
        @Param("progressCurrent") long progressCurrent, @Param("progressTotal") long progressTotal,
        @Param("progressMessage") String progressMessage, @Param("progressRevision") long progressRevision,
        @Param("resultRefType") String resultRefType, @Param("resultRefId") String resultRefId,
        @Param("resultSummary") String resultSummary, @Param("state") String state,
        @Param("completedTime") LocalDateTime completedTime, @Param("updateTime") LocalDateTime updateTime);
    int applyExecutionFailure(@Param("taskId") String taskId, @Param("version") int version,
        @Param("lastExecutionId") String lastExecutionId, @Param("lastExecuteTime") LocalDateTime lastExecuteTime,
        @Param("progressCurrent") long progressCurrent, @Param("progressTotal") long progressTotal,
        @Param("progressMessage") String progressMessage, @Param("progressRevision") long progressRevision,
        @Param("state") String state, @Param("completedTime") LocalDateTime completedTime,
        @Param("failureCode") String failureCode, @Param("failureMessage") String failureMessage,
        @Param("updateTime") LocalDateTime updateTime);
}
