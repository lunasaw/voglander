package io.github.lunasaw.voglander.service.sse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitterTestCapture;

class SseEventBusAuthorizationTest {

    @Test
    void localAndRedisDeliveryApplyTheSameTaskTypeAuthorization() {
        assertDelivery(new LocalSseEventBus(new SseDeliveryAuthorizer()), false);
        assertDelivery(redisBus(), false);
        assertDelivery(redisBus(), true);
    }

    @Test
    void redisBroadcastContainsOnlyTheBusinessEventAndNeverTheSubscriptionSnapshot() {
        RedisBackedSseEventBus bus = redisBus();
        StringRedisTemplate redis = (StringRedisTemplate)ReflectionTestUtils.getField(bus, "stringRedisTemplate");
        SseSubscriptionContext context = SseSubscriptionContext.authorized("42",
            Collections.singleton("business.task"), false, true, false);
        bus.register(context);

        bus.publish(taskEvent("IMAGE_COLLECTION", "allowed"));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(redis).convertAndSend(eq("sse:broadcast"), json.capture());
        assertFalse(json.getValue().contains(context.getEmitterId()));
        assertFalse(json.getValue().contains(context.getUserId()));
        assertFalse(json.getValue().contains("allowedTaskTypes"));
    }

    private void assertDelivery(SseEventBus bus, boolean remote) {
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        SseSubscriptionContext context = SseSubscriptionContext.authorized("7",
            Collections.singleton("business.task"), false, true, false);
        SseEmitter emitter = bus.register(context);
        capture.attach(emitter);

        if (remote) {
            RedisBackedSseEventBus redis = (RedisBackedSseEventBus)bus;
            SseEvent allowed = taskEvent("IMAGE_COLLECTION", "allowed");
            allowed.setOriginId("remote-node");
            redis.handleRemote(allowed);
            SseEvent denied = taskEvent("DATA_EXPORT", "denied");
            denied.setOriginId("remote-node");
            redis.handleRemote(denied);
        } else {
            bus.publishLocal(taskEvent("IMAGE_COLLECTION", "allowed"));
            bus.publishLocal(taskEvent("DATA_EXPORT", "denied"));
        }

        assertTrue(capture.dump().contains("allowed"));
        assertFalse(capture.dump().contains("denied"));
    }

    private RedisBackedSseEventBus redisBus() {
        RedisBackedSseEventBus bus = new RedisBackedSseEventBus(new SseDeliveryAuthorizer());
        ReflectionTestUtils.setField(bus, "stringRedisTemplate", mock(StringRedisTemplate.class));
        return bus;
    }

    private SseEvent taskEvent(String taskType, String marker) {
        return new SseEvent("business.task.state", Map.of("taskType", taskType, "marker", marker));
    }
}
