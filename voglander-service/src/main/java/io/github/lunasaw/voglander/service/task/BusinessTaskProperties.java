package io.github.lunasaw.voglander.service.task;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import lombok.Data;

/** Validated runtime settings for the durable business-task engine. */
@Data
@Validated
@ConfigurationProperties(prefix = "voglander.task")
public class BusinessTaskProperties {

    @Min(1)
    private int executorCoreSize = TaskConstant.DEFAULT_EXECUTOR_CORE_SIZE;

    @Min(1)
    private int executorMaxSize = TaskConstant.DEFAULT_EXECUTOR_MAX_SIZE;

    @Min(1)
    private int executorQueueCapacity = TaskConstant.DEFAULT_EXECUTOR_QUEUE_CAPACITY;

    @Min(1)
    private int executorShutdownAwaitSeconds = TaskConstant.DEFAULT_EXECUTOR_SHUTDOWN_AWAIT_SECONDS;

    @Min(1)
    private long progressMinIntervalMs = TaskConstant.DEFAULT_PROGRESS_MIN_INTERVAL_MS;

    private boolean enabled = true;

    private boolean eventRetentionEnabled = true;

    @Min(1)
    private int eventRetentionDays = 30;

    @Min(1)
    private long eventCleanupIntervalMs = 3_600_000L;

    @Min(1)
    private int eventCleanupBatchSize = 1_000;

    /** Core workers must fit inside the configured maximum pool size. */
    @AssertTrue(message = "voglander.task.executor-core-size must not exceed executor-max-size")
    public boolean isExecutorRangeValid() {
        return executorCoreSize <= executorMaxSize;
    }
}
