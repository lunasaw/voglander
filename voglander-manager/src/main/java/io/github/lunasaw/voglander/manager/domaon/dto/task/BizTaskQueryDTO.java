package io.github.lunasaw.voglander.manager.domaon.dto.task;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Fixed task query contract; sort fields are validated against a Manager allowlist. */
@Data
public class BizTaskQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String taskId;
    private String taskType;
    private String taskMode;
    private String state;
    private String taskName;
    private String ownerType;
    private String ownerId;
    private String organizationId;
    private String subjectType;
    private String subjectId;
    private String bizKey;
    private LocalDateTime createStartTime;
    private LocalDateTime createEndTime;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private String sortField;
    private String sortDirection;
}
