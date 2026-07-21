package io.github.lunasaw.voglander.service.task;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Bounded-cardinality metrics for the task engine.
 *
 * <p>Only fixed tag keys and allowlisted enum-like values are accepted. High-cardinality
 * identifiers and free-form messages are intentionally collapsed to {@code unknown}.</p>
 */
@Component
public class BusinessTaskMetrics {

    private static final Pattern STABLE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,47}");
    private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
        "taskId", "userId", "subjectId", "message", "path", "secret",
        "task_id", "user_id", "subject_id");

    private final MeterRegistry meterRegistry;

    public BusinessTaskMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordTask(String taskType, String state) {
        counter("voglander.business.task.count", "task_type", taskTypeTag(taskType), "state", stateTag(state))
            .increment();
    }

    public void recordExecution(String taskType, String state) {
        counter("voglander.business.execution.count", "task_type", taskTypeTag(taskType), "state", stateTag(state))
            .increment();
    }

    public void recordExecutionDuration(String taskType, Duration duration) {
        if (duration == null || duration.isNegative()) {
            return;
        }
        Timer.builder("voglander.business.execution.duration")
            .tag("task_type", taskTypeTag(taskType))
            .register(meterRegistry)
            .record(duration);
    }

    public void recordSchedulerLag(String taskType, Duration lag) {
        if (lag == null || lag.isNegative()) {
            return;
        }
        Timer.builder("voglander.business.scheduler.lag")
            .tag("task_type", taskTypeTag(taskType))
            .register(meterRegistry)
            .record(lag);
    }

    public void recordQueueDepth(int depth) {
        meterRegistry.gauge("voglander.business.queue.depth", Math.max(0, depth));
    }

    public void recordRetry(String taskType, String result) {
        counter("voglander.business.retry.count", "task_type", taskTypeTag(taskType), "result", resultTag(result))
            .increment();
    }

    public void recordLeaseRecovery(String taskType, String result) {
        counter("voglander.business.lease.recovery", "task_type", taskTypeTag(taskType), "result", resultTag(result))
            .increment();
    }

    public void recordProgressWrite(String taskType, boolean throttled) {
        counter("voglander.business.progress.write", "task_type", taskTypeTag(taskType), "result",
            throttled ? "THROTTLED" : "PERSISTED").increment();
    }

    /** Exposed for contract tests and future instrumentation adapters. */
    public static Set<String> forbiddenTagKeys() {
        return FORBIDDEN_TAG_KEYS;
    }

    private Counter counter(String name, String key1, String value1, String key2, String value2) {
        return Counter.builder(name).tag(key1, value1).tag(key2, value2).register(meterRegistry);
    }

    private String taskTypeTag(String value) {
        return stableTag(value);
    }

    private String stateTag(String value) {
        return stableTag(value);
    }

    private String resultTag(String value) {
        return stableTag(value);
    }

    private String stableTag(String value) {
        if (value == null) {
            return "unknown";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!STABLE_CODE.matcher(normalized).matches() || normalized.startsWith("BTASK_")
            || normalized.startsWith("BEXEC_") || normalized.startsWith("BEVT_")) {
            return "unknown";
        }
        return normalized;
    }
}
