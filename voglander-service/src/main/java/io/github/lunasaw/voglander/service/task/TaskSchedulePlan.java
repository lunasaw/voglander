package io.github.lunasaw.voglander.service.task;

import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;

/** Validated immutable creation-time schedule facts. */
final class TaskSchedulePlan {

    private final TaskModeEnum mode;
    private final int plannedCount;

    TaskSchedulePlan(TaskModeEnum mode, int plannedCount) {
        this.mode = mode;
        this.plannedCount = plannedCount;
    }

    TaskModeEnum mode() {
        return mode;
    }

    int plannedCount() {
        return plannedCount;
    }
}
