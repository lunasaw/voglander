package io.github.lunasaw.voglander.web.api.task.req;

import java.io.Serializable;

import lombok.Data;

/** Safe task-center query filters. Time values are Unix milliseconds. */
@Data
public class BusinessTaskPageReq implements Serializable {
    private static final long serialVersionUID = 1L;
    private String taskId;
    private String taskType;
    private String state;
    private String taskName;
    private String ownerType;
    private String ownerId;
    private String organizationId;
    private String subjectType;
    private String subjectId;
    private String bizKey;
    private Long createStartTime;
    private Long createEndTime;
    private Long scheduleStartTime;
    private Long scheduleEndTime;
    private String sortField;
    private String sortDirection;
}
