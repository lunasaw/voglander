package io.github.lunasaw.voglander.service.task;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDateTime;

import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;

/** Plans one durable execution fact from the persisted theoretical schedule cursor. */
final class TaskSchedulePlanner {

    private final Clock clock;
    private final int allowedDelaySeconds;

    TaskSchedulePlanner(Clock clock, int allowedDelaySeconds) {
        if (clock == null || allowedDelaySeconds < 0) {
            throw invalid("Task schedule planner configuration is invalid");
        }
        this.clock = clock;
        this.allowedDelaySeconds = allowedDelaySeconds;
    }

    TaskSchedulePoint plan(BizTaskDTO task) {
        if (task == null || task.getNextPlanTime() == null || task.getScheduleVersion() == null
            || task.getScheduleVersion() <= 0) {
            throw invalid("Task schedule cursor or version is invalid");
        }

        LocalDateTime plannedAt = task.getNextPlanTime();
        LocalDateTime deadlineAt;
        LocalDateTime nextPlanTime;
        try {
            deadlineAt = plannedAt.plusSeconds(allowedDelaySeconds);
            nextPlanTime = nextPlanTime(task, plannedAt);
        } catch (DateTimeException | ArithmeticException exception) {
            throw invalid("Task schedule point cannot be represented safely");
        }

        boolean missed = LocalDateTime.now(clock).isAfter(deadlineAt);
        return new TaskSchedulePoint(plannedAt, deadlineAt, nextPlanTime, task.getScheduleVersion(), missed);
    }

    private LocalDateTime nextPlanTime(BizTaskDTO task, LocalDateTime plannedAt) {
        TaskModeEnum mode = parseMode(task.getTaskMode());
        if (mode == TaskModeEnum.AT_TIME) {
            return null;
        }
        if (mode != TaskModeEnum.FIXED_RATE || task.getScheduleEndTime() == null
            || task.getIntervalSeconds() == null || task.getIntervalSeconds() <= 0
            || plannedAt.isAfter(task.getScheduleEndTime())) {
            throw invalid("Task schedule fields do not match a materializable mode");
        }

        LocalDateTime candidate = plannedAt.plusSeconds(task.getIntervalSeconds());
        return candidate.isAfter(task.getScheduleEndTime()) ? null : candidate;
    }

    private TaskModeEnum parseMode(String value) {
        try {
            return TaskModeEnum.valueOf(value);
        } catch (RuntimeException exception) {
            throw invalid("Unsupported task schedule mode");
        }
    }

    private static ServiceException invalid(String detailMessage) {
        return new ServiceException(ServiceExceptionEnum.TASK_SCHEDULE_INVALID).setDetailMessage(detailMessage);
    }
}
