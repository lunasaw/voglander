package io.github.lunasaw.voglander.common.constant.task;

/** Shared constants for the durable business-task engine. */
public final class TaskConstant {

    public static final String TASK_ID_PREFIX = "btask_";
    public static final String EXECUTION_ID_PREFIX = "bexec_";
    public static final String EVENT_ID_PREFIX = "bevt_";

    public static final String SCHEDULE_LOCK_PREFIX = "biz:task:schedule:";
    public static final String EXECUTION_LOCK_PREFIX = "biz:task:execution:";

    public static final String EXECUTOR_BEAN_NAME = "businessTaskExecutor";

    public static final String SSE_TASK_STATE = "business.task.state";
    public static final String SSE_TASK_PROGRESS = "business.task.progress";
    public static final String SSE_EXECUTION_STATE = "business.task.execution-state";

    public static final String PERMISSION_QUERY = "Task:Query";
    public static final String PERMISSION_CONTROL = "Task:Control";

    public static final int DEFAULT_SCAN_BATCH = 100;
    public static final int DEFAULT_CATCHUP_BATCH = 100;
    public static final int DEFAULT_MAX_PLANNED_COUNT = 10000;
    public static final int DEFAULT_MAX_SCHEDULE_DURATION_DAYS = 365;
    public static final int DEFAULT_MAX_PAYLOAD_BYTES = 64 * 1024;
    public static final int DEFAULT_MAX_EVENT_DATA_BYTES = 8 * 1024;
    public static final int DEFAULT_LEASE_SECONDS = 90;
    public static final int DEFAULT_ALLOWED_DELAY_SECONDS = 30;
    public static final int DEFAULT_EXECUTOR_CORE_SIZE = 4;
    public static final int DEFAULT_EXECUTOR_MAX_SIZE = 16;
    public static final int DEFAULT_EXECUTOR_QUEUE_CAPACITY = 500;
    public static final int DEFAULT_EXECUTOR_SHUTDOWN_AWAIT_SECONDS = 30;
    public static final int DEFAULT_RETRY_INITIAL_DELAY_SECONDS = 2;
    public static final int DEFAULT_RETRY_MAX_DELAY_SECONDS = 30;
    public static final long DEFAULT_PROGRESS_MIN_INTERVAL_MS = 500L;

    private TaskConstant() {
    }
}
