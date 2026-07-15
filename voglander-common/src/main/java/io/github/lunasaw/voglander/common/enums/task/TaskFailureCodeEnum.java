package io.github.lunasaw.voglander.common.enums.task;

/** Stable execution failure facts; these are persisted strings rather than HTTP error codes. */
public enum TaskFailureCodeEnum {
    HANDLER_NOT_FOUND,
    PAYLOAD_INVALID,
    CLAIM_CONFLICT,
    LEASE_EXPIRED,
    EXECUTION_TIMEOUT,
    CANCELLED,
    QUEUE_SATURATED,
    RETRY_EXHAUSTED,
    COMPLETION_FAILED,
    SYSTEM_ERROR
}
