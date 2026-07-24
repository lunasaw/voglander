package io.github.lunasaw.voglander.service.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitterTestCapture;

class SseEmitterLifecycleTest {

    @Test
    void completionTimeoutAndErrorReleaseLocalSubscriptionContexts() {
        assertLifecycle(new LocalSseEventBus(new SseDeliveryAuthorizer()));
    }

    @Test
    void completionTimeoutAndErrorReleaseRedisSubscriptionContexts() {
        RedisBackedSseEventBus bus = new RedisBackedSseEventBus(new SseDeliveryAuthorizer());
        ReflectionTestUtils.setField(bus, "stringRedisTemplate", mock(StringRedisTemplate.class));
        assertLifecycle(bus);
    }

    @Test
    void heartbeatIsOnlyPingAndDoesNotBypassTopicFiltering() {
        LocalSseEventBus bus = new LocalSseEventBus(new SseDeliveryAuthorizer());
        SseEmitterTestCapture capture = register(bus, "heartbeat");

        bus.heartbeat();
        bus.publishLocal(new SseEvent("image.asset.created",
            Map.of("marker", "unsubscribed-event")));

        assertTrue(capture.dump().contains("ping"));
        assertFalse(capture.dump().contains("unsubscribed-event"));
        assertEquals(1, bus.emitterCount());
    }

    @Test
    void highFrequencyDeliveryAndReconnectDoNotRetainClosedConnections() {
        LocalSseEventBus bus = new LocalSseEventBus(new SseDeliveryAuthorizer());
        SseEmitterTestCapture first = register(bus, "first");
        for (int i = 0; i < 200; i++) {
            bus.publishLocal(new SseEvent("business.task.state",
                Map.of("taskType", "IMAGE_COLLECTION", "sequence", i)));
        }
        assertTrue(first.dump().contains("199"));

        first.triggerCompletion();
        SseEmitterTestCapture reconnected = register(bus, "reconnected");
        bus.publishLocal(new SseEvent("business.task.state",
            Map.of("taskType", "IMAGE_COLLECTION", "marker", "after-reconnect")));

        assertEquals(1, bus.emitterCount());
        assertTrue(reconnected.dump().contains("after-reconnect"));
        assertFalse(first.dump().contains("after-reconnect"));
    }

    private void assertLifecycle(SseEventBus bus) {
        SseEmitterTestCapture completion = register(bus, "completion");
        SseEmitterTestCapture timeout = register(bus, "timeout");
        SseEmitterTestCapture error = register(bus, "error");
        assertEquals(3, count(bus));

        completion.triggerCompletion();
        timeout.triggerTimeout();
        error.triggerError(new IllegalStateException("closed"));

        assertEquals(0, count(bus));
        bus.publishLocal(new SseEvent("business.task.state",
            Map.of("taskType", "IMAGE_COLLECTION", "marker", "must-not-deliver")));
        assertFalse(completion.dump().contains("must-not-deliver"));
        assertFalse(timeout.dump().contains("must-not-deliver"));
        assertFalse(error.dump().contains("must-not-deliver"));
    }

    private SseEmitterTestCapture register(SseEventBus bus, String userId) {
        SseSubscriptionContext context = SseSubscriptionContext.authorized(userId,
            Collections.singleton("business.task"));
        SseEmitter emitter = bus.register(context);
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        capture.attach(emitter);
        return capture;
    }

    private int count(SseEventBus bus) {
        if (bus instanceof LocalSseEventBus) {
            return ((LocalSseEventBus)bus).emitterCount();
        }
        return ((RedisBackedSseEventBus)bus).emitterCount();
    }
}
