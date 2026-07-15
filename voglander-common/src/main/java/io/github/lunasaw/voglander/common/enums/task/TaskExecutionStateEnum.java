package io.github.lunasaw.voglander.common.enums.task;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Durable execution states and their legal database transitions. */
public enum TaskExecutionStateEnum {
    PENDING,
    RUNNING,
    RETRY_WAIT,
    SUCCEEDED,
    FAILED,
    MISSED,
    CANCELLED;

    private static final Set<TaskExecutionStateEnum> TERMINAL =
        EnumSet.of(SUCCEEDED, FAILED, MISSED, CANCELLED);
    private static final Map<TaskExecutionStateEnum, Set<TaskExecutionStateEnum>> ALLOWED =
        new EnumMap<>(TaskExecutionStateEnum.class);

    static {
        ALLOWED.put(PENDING, EnumSet.of(RUNNING, MISSED, CANCELLED));
        ALLOWED.put(RUNNING, EnumSet.of(SUCCEEDED, FAILED, RETRY_WAIT, CANCELLED));
        ALLOWED.put(RETRY_WAIT, EnumSet.of(RUNNING, FAILED, CANCELLED));
        ALLOWED.put(SUCCEEDED, EnumSet.noneOf(TaskExecutionStateEnum.class));
        ALLOWED.put(FAILED, EnumSet.noneOf(TaskExecutionStateEnum.class));
        ALLOWED.put(MISSED, EnumSet.noneOf(TaskExecutionStateEnum.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(TaskExecutionStateEnum.class));
    }

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean isRunnable() {
        return this == PENDING || this == RETRY_WAIT;
    }

    public boolean canTransitionTo(TaskExecutionStateEnum target) {
        return target != null && (this == target || ALLOWED.get(this).contains(target));
    }
}
