package io.github.lunasaw.voglander.web.api.task.vo;

import java.io.Serializable;

import lombok.Data;

/** Safe execution fact; claim token, worker node and lease internals are never exposed. */
@Data
public class BusinessTaskExecutionVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String executionId;
    private String taskId;
    private Integer scheduleVersion;
    private Long plannedAt;
    private Long deadlineAt;
    private String state;
    private Integer attemptCount;
    private Integer maxAttempts;
    private Long nextAttemptTime;
    private Long startedAt;
    private Long heartbeatAt;
    private Long finishedAt;
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
}
