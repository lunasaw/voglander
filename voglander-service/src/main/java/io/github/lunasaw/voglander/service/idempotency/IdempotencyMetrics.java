package io.github.lunasaw.voglander.service.idempotency;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/** Shared image/task idempotency outcomes without identity-bearing tags. */
@Component
public class IdempotencyMetrics {

    private static final Set<String> OUTCOMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "CREATED", "REPLAYED", "CONFLICT", "COMPENSATION_FAILURE", "UNKNOWN")));
    private static final Set<String> ERROR_CODES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "NONE", "600007", "710008", "UNKNOWN")));

    private final MeterRegistry registry;

    public IdempotencyMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(String outcome, String stableErrorCode) {
        Counter.builder("image_idempotency_total")
            .tag("outcome", OUTCOMES.contains(outcome) ? outcome : "UNKNOWN")
            .tag("stable_error_code", stableErrorCode == null ? "NONE"
                : ERROR_CODES.contains(stableErrorCode) ? stableErrorCode : "UNKNOWN")
            .register(registry).increment();
    }
}
