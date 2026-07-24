package io.github.lunasaw.voglander.service.image;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Image metrics whose tag values are restricted to finite operational enums. */
@Component
public class ImageDomainMetrics {

    private static final Set<String> HANDLER_RESULTS = values("SUCCESS", "FAILED", "UNKNOWN");
    private static final Set<String> OPERATIONS = values("STAGE", "PROMOTE", "OPEN", "DELETE", "UNKNOWN");
    private static final Set<String> PROVIDERS = values("LOCAL", "S3", "MINIO", "UNKNOWN");
    private static final Set<String> THUMBNAIL_OUTCOMES = values(
        "GENERATED", "CACHE_HIT", "NOT_MODIFIED", "QUEUE_REJECTED", "TIMEOUT", "ERROR", "UNKNOWN");
    private static final Set<String> ORPHAN_KINDS = values(
        "COMPENSATION_DELETE_FALSE", "COMPENSATION_DELETE_FAILED", "INGEST_STAGING_DISCARD_FAILED",
        "INGEST_RACE_DELETE_FALSE", "INGEST_RACE_DELETE_FAILED", "INGEST_COMPENSATION_DELETE_FALSE",
        "INGEST_COMPENSATION_DELETE_FAILED", "UNKNOWN");
    private static final Set<String> RECONCILIATION_KINDS = values(
        "STAGING", "UNREGISTERED", "MISSING", "UNKNOWN");
    private static final Set<String> FAILURE_CODES = failureCodes();

    private final MeterRegistry registry;

    public ImageDomainMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void handler(String result, String failureCode) {
        Counter.builder("image_collection_handler_total")
            .tag("result", bounded(result, HANDLER_RESULTS))
            .tag("failure_code", stableErrorCode(failureCode))
            .register(registry).increment();
    }

    public void snapshot(Duration duration, String ignoredNodeId) {
        if (valid(duration)) Timer.builder("image_snapshot_duration_seconds").register(registry).record(duration);
    }

    public void storage(Duration duration, String operation, String provider) {
        if (valid(duration)) {
            Timer.builder("image_storage_operation_duration_seconds")
                .tag("operation", bounded(operation, OPERATIONS))
                .tag("provider", bounded(provider, PROVIDERS))
                .register(registry).record(duration);
        }
    }

    public void orphan(String kind) {
        Counter.builder("image_storage_orphan_total").tag("kind", bounded(kind, ORPHAN_KINDS))
            .register(registry).increment();
    }

    public void reconciliation(String kind, int count) {
        Counter.builder("image_storage_reconciliation_total").tag("kind", bounded(kind, RECONCILIATION_KINDS))
            .register(registry).increment(Math.max(0, count));
    }

    public void thumbnailRequest(String profile, String outcome, String stableErrorCode, Duration duration) {
        String safeProfile = thumbnailProfile(profile);
        String safeOutcome = bounded(outcome, THUMBNAIL_OUTCOMES);
        Counter.builder("image_thumbnail_requests_total").tag("profile", safeProfile)
            .tag("outcome", safeOutcome).tag("stable_error_code", stableErrorCode(stableErrorCode))
            .register(registry).increment();
        if (valid(duration)) {
            Timer.builder("image_thumbnail_duration_seconds").tag("profile", safeProfile)
                .tag("outcome", safeOutcome).register(registry).record(duration);
        }
    }

    public void thumbnailCacheHit(String profile) {
        Counter.builder("image_thumbnail_cache_hits_total").tag("profile", thumbnailProfile(profile))
            .register(registry).increment();
    }

    public void thumbnailOutput(String profile, long bytes) {
        if (bytes < 0) return;
        DistributionSummary.builder("image_thumbnail_output_bytes").tag("profile", thumbnailProfile(profile))
            .register(registry).record(bytes);
    }

    public void thumbnailQueueRejected(String profile) {
        Counter.builder("image_thumbnail_queue_rejections_total").tag("profile", thumbnailProfile(profile))
            .register(registry).increment();
    }

    public void thumbnailTimeout(String profile) {
        Counter.builder("image_thumbnail_timeouts_total").tag("profile", thumbnailProfile(profile))
            .register(registry).increment();
    }

    private String thumbnailProfile(String value) {
        return "table".equals(value) || "gallery".equals(value) ? value : "UNKNOWN";
    }

    private String stableErrorCode(String value) {
        if (value == null || value.trim().isEmpty()) return "NONE";
        return FAILURE_CODES.contains(value) ? value : "UNKNOWN";
    }

    private String bounded(String value, Set<String> allowed) {
        return value != null && allowed.contains(value) ? value : "UNKNOWN";
    }

    private boolean valid(Duration duration) {
        return duration != null && !duration.isNegative();
    }

    private static Set<String> failureCodes() {
        Set<String> result = new HashSet<String>();
        result.add("NONE");
        result.add("UNKNOWN");
        result.add("IO_ERROR");
        result.add("RUNTIME_ERROR");
        result.add("600007");
        for (int code = 710000; code <= 710018; code++) result.add(String.valueOf(code));
        for (int code = 720000; code <= 720012; code++) result.add(String.valueOf(code));
        return Collections.unmodifiableSet(result);
    }

    private static Set<String> values(String... values) {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(values)));
    }
}
