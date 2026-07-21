package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class BusinessTaskMetricsContractTest {

    @Test
    void metricsUseBoundedTagsAndCollapseBusinessIdentifiers() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessTaskMetrics metrics = new BusinessTaskMetrics(registry);

        metrics.recordTask("btask_123", "RUNNING");
        metrics.recordExecution("IMAGE_COLLECTION", "RUNNING");
        metrics.recordExecutionDuration("IMAGE_COLLECTION", Duration.ofMillis(12));
        metrics.recordSchedulerLag("IMAGE_COLLECTION", Duration.ofMillis(3));
        metrics.recordRetry("IMAGE_COLLECTION", "TIMEOUT");
        metrics.recordLeaseRecovery("IMAGE_COLLECTION", "RETRY_WAIT");
        metrics.recordProgressWrite("IMAGE_COLLECTION", false);

        for (Meter meter : registry.getMeters()) {
            meter.getId().getTags().forEach(tag -> {
                assertFalse(BusinessTaskMetrics.forbiddenTagKeys().contains(tag.getKey()));
                assertFalse(tag.getValue().contains("btask_123"));
            });
        }
        assertTrue(registry.get("voglander.business.task.count").counter().count() > 0);
    }
}
