package io.github.lunasaw.voglander.manager.domaon.dto.task;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Complete Manager-layer representation of an append-only task event. */
@Data
public class BizTaskEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private LocalDateTime createTime;
    private String eventId;
    private String taskId;
    private String executionId;
    private String eventType;
    private String fromState;
    private String toState;
    private Integer attemptNo;
    private Long progressCurrent;
    private Long progressTotal;
    private String progressMessage;
    private String failureCode;
    private String failureMessage;
    private String actorType;
    private String actorId;
    private String workerNode;
    private String traceId;
    private String dedupeKey;
    private String eventData;
    private LocalDateTime occurredAt;
}
