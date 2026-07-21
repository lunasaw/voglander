package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ImageDomainMetricsTest {
    @Test
    void imageMetersUseStableNamesAndBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry(); ImageDomainMetrics metrics = new ImageDomainMetrics(registry);
        metrics.handler("SUCCESS", ""); metrics.snapshot(Duration.ofMillis(1), "NODE_A"); metrics.storage(Duration.ofMillis(1), "PROMOTE", "LOCAL"); metrics.orphan("COMPENSATION_DELETE_FAILED"); metrics.reconciliation("MISSING", 2);
        assertNotNull(registry.find("image_collection_handler_total").counter()); assertNotNull(registry.find("image_snapshot_duration_seconds").timer());
        assertNotNull(registry.find("image_storage_operation_duration_seconds").timer()); assertNotNull(registry.find("image_storage_orphan_total").counter());
        assertNotNull(registry.find("image_storage_reconciliation_total").counter());
    }
}
