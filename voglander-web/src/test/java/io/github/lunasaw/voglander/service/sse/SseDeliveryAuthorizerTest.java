package io.github.lunasaw.voglander.service.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.exception.ServiceException;

class SseDeliveryAuthorizerTest {

    private final SseDeliveryAuthorizer authorizer = new SseDeliveryAuthorizer();

    @Test
    void deliveryUsesOnlyExactOrDottedPrefixTopicMatching() {
        SseSubscriptionContext context = SseSubscriptionContext.authorized("7",
            Collections.singleton("business.task"));

        assertTrue(authorizer.allow(context,
            new SseEvent("business.task.state", Collections.singletonMap("taskType", "DATA_EXPORT"))));
        assertFalse(authorizer.allow(context,
            new SseEvent("business.tasks", Collections.emptyMap())));
        assertFalse(authorizer.allow(context,
            new SseEvent("image.asset.created", Collections.emptyMap())));
    }

    @Test
    void arbitraryTopicRootsAreAcceptedAndDelivered() {
        SseSubscriptionContext context = SseSubscriptionContext.authorized("7",
            Collections.singleton("future-domain"));

        assertTrue(authorizer.allow(context,
            new SseEvent("future-domain.created", Collections.emptyMap())));
    }

    @Test
    void subscriptionNormalizesTopicsAndRejectsOnlyAnEmptyResult() {
        SseSubscriptionContext context = SseSubscriptionContext.authorized("7",
            new HashSet<>(Arrays.asList(" future-domain ", "future-domain", " ")));

        assertEquals(Collections.singleton("future-domain"), context.getTopics());
        assertThrows(ServiceException.class,
            () -> SseSubscriptionContext.authorized("7", Collections.singleton(" ")));
    }
}
