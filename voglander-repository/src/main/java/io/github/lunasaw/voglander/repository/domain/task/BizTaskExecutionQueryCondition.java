package io.github.lunasaw.voglander.repository.domain.task;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Repository-only, parameterized execution query condition. */
@Data
public class BizTaskExecutionQueryCondition implements Serializable {
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
    private boolean globalScope;
    private String ownerType;
    private String ownerId;
    private String organizationId;
    private String sortField;
    private boolean sortAscending;
}
