package io.github.lunasaw.voglander.manager.domaon.dto.task;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Complete Manager-layer representation of one durable execution fact. */
@Data
public class BizTaskExecutionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String executionId;
    private String taskId;
    private Integer scheduleVersion;
    private LocalDateTime plannedAt;
    private LocalDateTime deadlineAt;
    private String state;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime nextAttemptTime;
    private LocalDateTime startedAt;
    private LocalDateTime heartbeatAt;
    private LocalDateTime finishedAt;
    private String claimToken;
    private String workerNode;
    private LocalDateTime leaseUntil;
    private Long progressCurrent;
    private Long progressTotal;
    private String progressMessage;
    private Long progressRevision;
    private String resultRefType;
    private String resultRefId;
    private String resultSummary;
    private String failureCode;
    private String failureMessage;
    private Boolean retryable;
    private String retryOriginExecutionId;
    private Integer version;
}
