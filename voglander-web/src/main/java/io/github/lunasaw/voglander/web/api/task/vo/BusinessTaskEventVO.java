package io.github.lunasaw.voglander.web.api.task.vo;

import java.io.Serializable;

import lombok.Data;

/** Sanitized append-only task event representation. */
@Data
public class BusinessTaskEventVO implements Serializable {
    private static final long serialVersionUID = 1L;
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
    private String eventData;
    private Long occurredAt;
}
