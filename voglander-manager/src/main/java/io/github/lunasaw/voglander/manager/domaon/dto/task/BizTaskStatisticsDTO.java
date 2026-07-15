package io.github.lunasaw.voglander.manager.domaon.dto.task;

import java.io.Serializable;

import lombok.Data;

/** Access-scoped task-center summary counters. */
@Data
public class BizTaskStatisticsDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long scheduledCount;
    private Long runningCount;
    private Long pausedCount;
    private Long cancellingCount;
    private Long completedTodayCount;
    private Long failedCount;
}
