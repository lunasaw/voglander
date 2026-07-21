package io.github.lunasaw.voglander.web.api.task.req;

import java.io.Serializable;

import lombok.Data;

/** Safe execution-history query filters. Time values are Unix milliseconds. */
@Data
public class BusinessTaskExecutionPageReq implements Serializable {
    private static final long serialVersionUID = 1L;
    private String executionId;
    private String taskId;
    private String state;
    private Boolean retryable;
    private Long plannedStartTime;
    private Long plannedEndTime;
    private Long createStartTime;
    private Long createEndTime;
    private String sortField;
    private String sortDirection;
}
