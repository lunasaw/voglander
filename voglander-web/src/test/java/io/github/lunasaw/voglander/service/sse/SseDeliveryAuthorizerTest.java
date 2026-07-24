package io.github.lunasaw.voglander.service.sse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.exception.ServiceException;

class SseDeliveryAuthorizerTest {

    private final SseDeliveryAuthorizer authorizer = new SseDeliveryAuthorizer();

    @Test
    void imageCollectionSubscriberOnlyReceivesImageCollectionTaskEvents() {
        SseSubscriptionContext context = SseSubscriptionContext.builder()
            .userId("7")
            .topics(Collections.singleton("business.task"))
            .imageCollectionQueryAllowed(true)
            .allowedTaskTypes(Collections.singleton("IMAGE_COLLECTION"))
            .build();

        assertTrue(authorizer.allow(context, event("IMAGE_COLLECTION")));
        assertFalse(authorizer.allow(context, event("DATA_EXPORT")));
        assertFalse(authorizer.allow(context, new SseEvent("business.task.state", Collections.emptyMap())));
    }

    @Test
    void assetEventsRequireAssetQueryPermission() {
        SseSubscriptionContext denied = SseSubscriptionContext.builder()
            .userId("7")
            .topics(Collections.singleton("image.asset"))
            .build();
        SseSubscriptionContext allowed = denied.toBuilder().imageAssetQueryAllowed(true).build();

        SseEvent event = new SseEvent("image.asset.created", Collections.emptyMap());
        assertFalse(authorizer.allow(denied, event));
        assertTrue(authorizer.allow(allowed, event));
    }

    @Test
    void subscriptionRejectsUnknownAndEmptyTopics() {
        assertThrows(ServiceException.class,
            () -> SseSubscriptionContext.authorized("7", Collections.singleton("unknown.topic"), true,
                false, false));
        assertThrows(ServiceException.class,
            () -> SseSubscriptionContext.authorized("7", Collections.emptySet(), true, false, false));
    }

    private SseEvent event(String taskType) {
        return new SseEvent("business.task.state", Map.of("taskType", taskType));
    }
}
