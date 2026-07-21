package io.github.lunasaw.voglander.web.api.task.vo;

import java.io.Serializable;

import lombok.Data;

/** Access-scoped task-center counters. */
@Data
public class BusinessTaskStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long scheduledCount;
    private Long runningCount;
    private Long pausedCount;
    private Long cancellingCount;
    private Long completedTodayCount;
    private Long failedCount;
}
