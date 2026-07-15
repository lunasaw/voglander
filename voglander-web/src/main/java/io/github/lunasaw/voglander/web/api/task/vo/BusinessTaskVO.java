package io.github.lunasaw.voglander.web.api.task.vo;

import java.io.Serializable;

import lombok.Data;

/** Safe task representation; payload and persistence internals are intentionally absent. */
@Data
public class BusinessTaskVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long createTime;
    private Long updateTime;
    private String taskId;
    private String taskType;
    private String taskName;
    private String description;
    private String taskMode;
    private Long scheduleStartTime;
    private Long scheduleEndTime;
    private Long intervalSeconds;
    private Long nextPlanTime;
    private Integer scheduleVersion;
    private String state;
    private Integer priority;
    private String lastExecutionId;
    private Long lastExecuteTime;
    private Long completedTime;
    private Integer plannedCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer missedCount;
    private Integer cancelledCount;
    private Long progressCurrent;
    private Long progressTotal;
    private String progressMessage;
    private Long progressRevision;
    private String bizKey;
    private String subjectType;
    private String subjectId;
    private String resultRefType;
    private String resultRefId;
    private String resultSummary;
    private String lastFailureCode;
    private String lastFailureMessage;
    private String originTaskId;
    private String originExecutionId;
    private String ownerType;
    private String ownerId;
    private String organizationId;
}
