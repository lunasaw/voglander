package io.github.lunasaw.voglander.manager.domaon.dto.task;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Fixed execution query contract; sort fields are validated against a Manager allowlist. */
@Data
public class BizTaskExecutionQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String executionId;
    private String taskId;
    private String state;
    private Boolean retryable;
    private String workerNode;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime createStartTime;
    private LocalDateTime createEndTime;
    private String sortField;
    private String sortDirection;
}
