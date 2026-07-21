package io.github.lunasaw.voglander.common.enums.task;

public enum TaskEventTypeEnum {
    CREATED,
    SCHEDULED,
    CLAIMED,
    STARTED,
    PROGRESS,
    RETRY_SCHEDULED,
    SUCCEEDED,
    FAILED,
    MISSED,
    PAUSED,
    RESUMED,
    CANCELLING,
    CANCELLED,
    LEASE_EXPIRED,
    MANUAL_RETRY
}
