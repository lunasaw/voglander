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
    void localAndRedisDeliveryDoNotFilterByTaskType() {
        assertDelivery(new LocalSseEventBus(new SseDeliveryAuthorizer()), false);
        assertDelivery(redisBus(), false);
        assertDelivery(redisBus(), true);
    }

    @Test
    void redisBroadcastContainsOnlyTheBusinessEventAndNeverTheSubscriptionSnapshot() {
        RedisBackedSseEventBus bus = redisBus();
        StringRedisTemplate redis = (StringRedisTemplate)ReflectionTestUtils.getField(bus, "stringRedisTemplate");
        SseSubscriptionContext context = SseSubscriptionContext.authorized("42",
            Collections.singleton("business.task"));
        bus.register(context);

        bus.publish(taskEvent("IMAGE_COLLECTION", "allowed"));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(redis).convertAndSend(eq("sse:broadcast"), json.capture());
        for (String subscriptionField : new String[] {"emitterId", "userId", "topics", "allowedTaskTypes"}) {
            assertFalse(json.getValue().contains("\"" + subscriptionField + "\""));
        }
    }

    private void assertDelivery(SseEventBus bus, boolean remote) {
        SseEmitterTestCapture capture = new SseEmitterTestCapture();
        SseSubscriptionContext context = SseSubscriptionContext.authorized("7",
            Collections.singleton("business.task"));
        SseEmitter emitter = bus.register(context);
        capture.attach(emitter);

        if (remote) {
            RedisBackedSseEventBus redis = (RedisBackedSseEventBus)bus;
            SseEvent collection = taskEvent("IMAGE_COLLECTION", "collection");
            collection.setOriginId("remote-node");
            redis.handleRemote(collection);
            SseEvent export = taskEvent("DATA_EXPORT", "export");
            export.setOriginId("remote-node");
            redis.handleRemote(export);
        } else {
            bus.publishLocal(taskEvent("IMAGE_COLLECTION", "collection"));
            bus.publishLocal(taskEvent("DATA_EXPORT", "export"));
        }

        assertTrue(capture.dump().contains("collection"));
        assertTrue(capture.dump().contains("export"));
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
