package io.github.lunasaw.voglander.manager.domaon.dto.task;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Auditable Manager command used for task controls and rescheduling. */
@Data
public class BizTaskCommandDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String taskId;
    private String executionId;
    private String command;
    private String actorType;
    private String actorId;
    private Integer expectedVersion;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private Long intervalSeconds;
    private String idempotencyKey;
    private String reason;
    private LocalDateTime requestedAt;
}
