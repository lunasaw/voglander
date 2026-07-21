package io.github.lunasaw.voglander.service.task;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;

import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

/** Validates task schedule shape and calculates the inclusive plan size without cursor drift. */
final class TaskScheduleCalculator {

    private static final Duration MAX_DURATION = Duration.ofDays(TaskConstant.DEFAULT_MAX_SCHEDULE_DURATION_DAYS);

    private TaskScheduleCalculator() {
    }

    static TaskSchedulePlan calculate(TaskCreateCommand command) {
        TaskModeEnum mode = parseMode(command.taskMode());
        if (mode == TaskModeEnum.ONCE) {
            require(command.scheduleStartTime() == null && command.scheduleEndTime() == null
                && command.intervalSeconds() == null);
            return new TaskSchedulePlan(mode, 1);
        }
        if (mode == TaskModeEnum.AT_TIME) {
            require(command.scheduleStartTime() != null && command.scheduleEndTime() == null
                && command.intervalSeconds() == null);
            return new TaskSchedulePlan(mode, 1);
        }
        return fixedRate(command, mode);
    }

    private static TaskSchedulePlan fixedRate(TaskCreateCommand command, TaskModeEnum mode) {
        LocalDateTime start = command.scheduleStartTime();
        LocalDateTime end = command.scheduleEndTime();
        Long intervalSeconds = command.intervalSeconds();
        require(start != null && end != null && intervalSeconds != null && intervalSeconds > 0
            && !end.isBefore(start));

        Duration duration;
        try {
            duration = Duration.between(start, end);
            start.plusSeconds(intervalSeconds);
        } catch (DateTimeException | ArithmeticException exception) {
            throw invalid("Task schedule cannot be represented safely");
        }
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw limit("Task schedule duration exceeds the core limit");
        }

        long plannedCount;
        try {
            plannedCount = Math.addExact(duration.getSeconds() / intervalSeconds, 1L);
        } catch (ArithmeticException exception) {
            throw invalid("Task planned count overflow");
        }
        if (plannedCount > TaskConstant.DEFAULT_MAX_PLANNED_COUNT) {
            throw limit("Task planned count exceeds the core limit");
        }
        return new TaskSchedulePlan(mode, (int)plannedCount);
    }

    private static TaskModeEnum parseMode(String value) {
        try {
            return TaskModeEnum.valueOf(value);
        } catch (RuntimeException exception) {
            throw invalid("Unsupported task schedule mode");
        }
    }

    private static void require(boolean valid) {
        if (!valid) {
            throw invalid("Task schedule fields do not match the mode");
        }
    }

    private static ServiceException invalid(String detailMessage) {
        return new ServiceException(ServiceExceptionEnum.TASK_SCHEDULE_INVALID).setDetailMessage(detailMessage);
    }

    private static ServiceException limit(String detailMessage) {
        return new ServiceException(ServiceExceptionEnum.TASK_LIMIT_EXCEEDED).setDetailMessage(detailMessage);
    }
}
