package io.github.lunasaw.voglander.manager.domaon.dto.task;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Complete Manager-layer representation of a durable business task. */
@Data
public class BizTaskDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String taskId;
    private String taskType;
    private String taskName;
    private String description;
    private String taskMode;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private Long intervalSeconds;
    private LocalDateTime nextPlanTime;
    private Integer scheduleVersion;
    private String state;
    private Integer priority;
    private String lastExecutionId;
    private LocalDateTime lastExecuteTime;
    private LocalDateTime completedTime;
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
    private String payload;
    private Integer payloadVersion;
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
    private String idempotencyKey;
    private Integer version;
}
