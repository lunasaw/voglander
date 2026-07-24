package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.service.idempotency.IdempotencyMetrics;
import io.github.lunasaw.voglander.service.sse.SseDomainMetrics;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ImageOperationalMetricsContractTest {

    private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
        "userId", "user_id", "assetId", "asset_id", "taskId", "task_id", "token",
        "key", "idempotency_key", "storageKey", "storage_key", "node", "nodeId", "node_id");

    @Test
    void thumbnailIdempotencyAndSseMetricsExistAndRejectHighCardinalityTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ImageDomainMetrics image = new ImageDomainMetrics(registry);
        IdempotencyMetrics idempotency = new IdempotencyMetrics(registry);
        SseDomainMetrics sse = new SseDomainMetrics(registry);

        image.thumbnailRequest("table", "GENERATED", null, Duration.ofMillis(4));
        image.thumbnailCacheHit("gallery");
        image.thumbnailOutput("gallery", 1024);
        image.thumbnailQueueRejected("table");
        image.thumbnailTimeout("table");
        image.snapshot(Duration.ofMillis(1), "node-server-sensitive-123");
        image.orphan("SENSITIVE_DYNAMIC_CODE");
        image.reconciliation("SECRET_DYNAMIC_CODE", 1);
        idempotency.record("CREATED", null);
        idempotency.record("REPLAYED", null);
        idempotency.record("CONFLICT", "600007");
        idempotency.record("COMPENSATION_FAILURE", "710008");
        ConcurrentHashMap<String, Object> emitters = new ConcurrentHashMap<>();
        emitters.put("opaque-token-user-asset-task-storage-key", new Object());
        sse.bindEmitterCount("LOCAL", emitters);
        sse.registrationDenied("REDIS", "700007");
        sse.deliveryFiltered("REDIS");
        sse.sendFailure("LOCAL");

        assertNotNull(registry.find("image_thumbnail_requests_total").counter());
        assertNotNull(registry.find("image_thumbnail_cache_hits_total").counter());
        assertNotNull(registry.find("image_thumbnail_duration_seconds").timer());
        assertNotNull(registry.find("image_thumbnail_output_bytes").summary());
        assertNotNull(registry.find("image_thumbnail_queue_rejections_total").counter());
        assertNotNull(registry.find("image_thumbnail_timeouts_total").counter());
        assertNotNull(registry.find("image_idempotency_total").counter());
        assertNotNull(registry.find("sse_registration_denied_total").counter());
        assertNotNull(registry.find("sse_delivery_filtered_total").counter());
        assertNotNull(registry.find("sse_send_failures_total").counter());
        assertNotNull(registry.find("sse_emitter_count").gauge());
        assertNotNull(registry.find("image_storage_orphan_total").tag("kind", "UNKNOWN").counter());
        assertNotNull(registry.find("image_storage_reconciliation_total").tag("kind", "UNKNOWN").counter());

        for (Meter meter : registry.getMeters()) {
            meter.getId().getTags().forEach(tag -> {
                assertFalse(FORBIDDEN_TAG_KEYS.contains(tag.getKey()), meter.getId().toString());
                assertFalse(tag.getValue().contains("sensitive"), meter.getId().toString());
                assertFalse(tag.getValue().contains("opaque-token"), meter.getId().toString());
            });
        }
    }
}
