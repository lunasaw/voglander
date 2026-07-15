package io.github.lunasaw.voglander.service.image;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Image-specific bounded metrics; task queue/lease metrics remain generic. */
@Component
public class ImageDomainMetrics {
    private final MeterRegistry registry;
    public ImageDomainMetrics(MeterRegistry registry) { this.registry = registry; }
    public void handler(String result, String failureCode) { Counter.builder("image_collection_handler_total").tag("result", safe(result)).tag("failure_code", safe(failureCode)).register(registry).increment(); }
    public void snapshot(Duration duration, String node) { if (duration != null && !duration.isNegative()) Timer.builder("image_snapshot_duration_seconds").tag("node", safe(node)).register(registry).record(duration); }
    public void storage(Duration duration, String operation, String provider) { if (duration != null && !duration.isNegative()) Timer.builder("image_storage_operation_duration_seconds").tag("operation", safe(operation)).tag("provider", safe(provider)).register(registry).record(duration); }
    public void orphan(String kind) { Counter.builder("image_storage_orphan_total").tag("kind", safe(kind)).register(registry).increment(); }
    public void reconciliation(String kind, int count) {
        Counter.builder("image_storage_reconciliation_total").tag("kind", safe(kind)).register(registry)
            .increment(Math.max(0, count));
    }
    private String safe(String value) { return value == null || !value.matches("[A-Z][A-Z0-9_]{0,31}") ? "UNKNOWN" : value; }
}
