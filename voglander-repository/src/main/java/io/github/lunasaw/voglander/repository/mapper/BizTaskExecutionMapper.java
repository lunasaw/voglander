package io.github.lunasaw.voglander.repository.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.repository.domain.task.BizTaskExecutionQueryCondition;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;

@Mapper
public interface BizTaskExecutionMapper extends BaseMapper<BizTaskExecutionDO> {
    int insertIfAbsent(@Param("execution") BizTaskExecutionDO execution);
    List<BizTaskExecutionDO> selectRunnable(@Param("now") LocalDateTime now, @Param("limit") int limit);
    List<BizTaskExecutionDO> selectExpiredLeases(@Param("now") LocalDateTime now, @Param("limit") int limit);
    List<BizTaskExecutionDO> selectTaskTimeline(@Param("taskId") String taskId, @Param("limit") int limit);
    Page<BizTaskExecutionDO> selectPageWithScope(Page<BizTaskExecutionDO> page,
        @Param("condition") BizTaskExecutionQueryCondition condition);
    BizTaskExecutionDO selectByExecutionIdWithScope(
        @Param("condition") BizTaskExecutionQueryCondition condition);
    int claim(@Param("executionId") String executionId, @Param("version") int version,
        @Param("claimToken") String claimToken, @Param("workerNode") String workerNode,
        @Param("now") LocalDateTime now, @Param("leaseUntil") LocalDateTime leaseUntil);
    int heartbeat(@Param("executionId") String executionId, @Param("claimToken") String claimToken,
        @Param("heartbeatAt") LocalDateTime heartbeatAt, @Param("leaseUntil") LocalDateTime leaseUntil);
    int updateProgress(@Param("executionId") String executionId, @Param("claimToken") String claimToken,
        @Param("current") long current, @Param("total") long total, @Param("message") String message,
        @Param("revision") long revision, @Param("updateTime") LocalDateTime updateTime);
    int transitionState(@Param("executionId") String executionId, @Param("version") int version,
        @Param("fromStates") List<String> fromStates, @Param("toState") String toState,
        @Param("updateTime") LocalDateTime updateTime);
    int markRetryWait(@Param("executionId") String executionId, @Param("claimToken") String claimToken,
        @Param("nextAttemptTime") LocalDateTime nextAttemptTime, @Param("failureCode") String failureCode,
        @Param("failureMessage") String failureMessage, @Param("updateTime") LocalDateTime updateTime);
    int markMissed(@Param("executionId") String executionId, @Param("version") int version,
        @Param("finishedAt") LocalDateTime finishedAt, @Param("failureCode") String failureCode,
        @Param("failureMessage") String failureMessage, @Param("updateTime") LocalDateTime updateTime);
    int recoverExpiredLease(@Param("executionId") String executionId, @Param("version") int version,
        @Param("claimToken") String claimToken, @Param("expiredBefore") LocalDateTime expiredBefore,
        @Param("targetState") String targetState, @Param("nextAttemptTime") LocalDateTime nextAttemptTime,
        @Param("finishedAt") LocalDateTime finishedAt, @Param("failureCode") String failureCode,
        @Param("failureMessage") String failureMessage, @Param("updateTime") LocalDateTime updateTime);
    int markTerminal(@Param("executionId") String executionId, @Param("claimToken") String claimToken,
        @Param("terminalState") String terminalState, @Param("finishedAt") LocalDateTime finishedAt,
        @Param("resultRefType") String resultRefType, @Param("resultRefId") String resultRefId,
        @Param("resultSummary") String resultSummary, @Param("failureCode") String failureCode,
        @Param("failureMessage") String failureMessage, @Param("retryable") boolean retryable,
        @Param("updateTime") LocalDateTime updateTime);
    int completeSuccess(@Param("executionId") String executionId, @Param("claimToken") String claimToken,
        @Param("finishedAt") LocalDateTime finishedAt, @Param("progressCurrent") long progressCurrent,
        @Param("progressTotal") long progressTotal, @Param("progressMessage") String progressMessage,
        @Param("progressRevision") long progressRevision, @Param("resultRefType") String resultRefType,
        @Param("resultRefId") String resultRefId, @Param("resultSummary") String resultSummary,
        @Param("updateTime") LocalDateTime updateTime);
}
