package io.github.lunasaw.voglander.common.enums.task;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Durable task states and their legal database transitions. */
public enum TaskStateEnum {
    SCHEDULED,
    RUNNING,
    PAUSED,
    CANCELLING,
    COMPLETED,
    PARTIAL_COMPLETED,
    FAILED,
    CANCELLED;

    private static final Set<TaskStateEnum> TERMINAL =
        EnumSet.of(COMPLETED, PARTIAL_COMPLETED, FAILED, CANCELLED);
    private static final Map<TaskStateEnum, Set<TaskStateEnum>> ALLOWED = new EnumMap<>(TaskStateEnum.class);

    static {
        ALLOWED.put(SCHEDULED, EnumSet.of(RUNNING, PAUSED, CANCELLING));
        ALLOWED.put(RUNNING, EnumSet.of(PAUSED, CANCELLING, COMPLETED, PARTIAL_COMPLETED, FAILED));
        ALLOWED.put(PAUSED, EnumSet.of(SCHEDULED, RUNNING, CANCELLING));
        ALLOWED.put(CANCELLING, EnumSet.of(CANCELLED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(TaskStateEnum.class));
        ALLOWED.put(PARTIAL_COMPLETED, EnumSet.noneOf(TaskStateEnum.class));
        ALLOWED.put(FAILED, EnumSet.noneOf(TaskStateEnum.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(TaskStateEnum.class));
    }

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean canTransitionTo(TaskStateEnum target) {
        return target != null && (this == target || ALLOWED.get(this).contains(target));
    }
}
