package io.github.lunasaw.voglander.service.sse;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/** Local/Redis SSE operational metrics with finite bus and error-code tags. */
@Component
public class SseDomainMetrics {

    private final MeterRegistry registry;

    public SseDomainMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void bindEmitterCount(String busType, Map<?, ?> emitters) {
        Gauge.builder("sse_emitter_count", emitters, value -> value.size())
            .tag("bus_type", busType(busType)).register(registry);
    }

    public void registrationDenied(String busType, String stableErrorCode) {
        Counter.builder("sse_registration_denied_total").tag("bus_type", busType(busType))
            .tag("stable_error_code", errorCode(stableErrorCode)).register(registry).increment();
    }

    public void deliveryFiltered(String busType) {
        Counter.builder("sse_delivery_filtered_total").tag("bus_type", busType(busType))
            .register(registry).increment();
    }

    public void sendFailure(String busType) {
        Counter.builder("sse_send_failures_total").tag("bus_type", busType(busType))
            .register(registry).increment();
    }

    private String busType(String value) {
        return "LOCAL".equals(value) || "REDIS".equals(value) ? value : "UNKNOWN";
    }

    private String errorCode(String value) {
        return "700006".equals(value) || "700007".equals(value) ? value : "UNKNOWN";
    }
}
