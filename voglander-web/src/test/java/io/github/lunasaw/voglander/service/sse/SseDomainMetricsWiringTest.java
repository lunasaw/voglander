package io.github.lunasaw.voglander.service.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitterTestCapture;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class SseDomainMetricsWiringTest {

    @Test
    void localAndRedisRecordTheSameLifecycleAuthorizationAndFailureMetrics() {
        assertMetrics("LOCAL");
        assertMetrics("REDIS");
    }

    private void assertMetrics(String busType) {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SseDomainMetrics metrics = new SseDomainMetrics(registry);
        SseEventBus bus = "LOCAL".equals(busType)
            ? new LocalSseEventBus(new SseDeliveryAuthorizer(), metrics)
            : new RedisBackedSseEventBus(new SseDeliveryAuthorizer(), metrics);

        assertThrows(ServiceException.class, () -> bus.register(null));
        assertEquals(1.0, registry.get("sse_registration_denied_total")
            .tags("bus_type", busType, "stable_error_code", "700007").counter().count());

        SseEmitter emitter = bus.register(context("normal"));
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        capture.attach(emitter);
        assertEquals(1.0, registry.get("sse_emitter_count").tag("bus_type", busType).gauge().value());

        bus.publishLocal(new SseEvent("business.task.state",
            Map.of("taskType", "DATA_EXPORT", "marker", "filtered")));
        assertEquals(1.0, registry.get("sse_delivery_filtered_total")
            .tag("bus_type", busType).counter().count());
        capture.triggerCompletion();
        assertEquals(0.0, registry.get("sse_emitter_count").tag("bus_type", busType).gauge().value());

        SseEmitter failing = bus.register(context("failing"));
        SseEmitterTestCapture failingCapture = new SseEmitterTestCapture();
        failingCapture.attachFailing(failing);
        bus.publishLocal(new SseEvent("business.task.state",
            Map.of("taskType", "IMAGE_COLLECTION", "marker", "send-failure")));
        assertEquals(1.0, registry.get("sse_send_failures_total")
            .tag("bus_type", busType).counter().count());
        assertEquals(0.0, registry.get("sse_emitter_count").tag("bus_type", busType).gauge().value());
    }

    private SseSubscriptionContext context(String userId) {
        return SseSubscriptionContext.authorized(userId, Collections.singleton("business.task"),
            false, true, false);
    }
}
