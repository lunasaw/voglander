package io.github.lunasaw.voglander.service.task;

import java.time.LocalDateTime;

/** Immutable materialization decision for one durable task schedule point. */
final class TaskSchedulePoint {

    private final LocalDateTime plannedAt;
    private final LocalDateTime deadlineAt;
    private final LocalDateTime nextPlanTime;
    private final int scheduleVersion;
    private final boolean missed;

    TaskSchedulePoint(LocalDateTime plannedAt, LocalDateTime deadlineAt, LocalDateTime nextPlanTime,
        int scheduleVersion, boolean missed) {
        this.plannedAt = plannedAt;
        this.deadlineAt = deadlineAt;
        this.nextPlanTime = nextPlanTime;
        this.scheduleVersion = scheduleVersion;
        this.missed = missed;
    }

    LocalDateTime plannedAt() {
        return plannedAt;
    }

    LocalDateTime deadlineAt() {
        return deadlineAt;
    }

    LocalDateTime nextPlanTime() {
        return nextPlanTime;
    }

    int scheduleVersion() {
        return scheduleVersion;
    }

    boolean missed() {
        return missed;
    }
}
