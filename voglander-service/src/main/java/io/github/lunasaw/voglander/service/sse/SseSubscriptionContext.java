package io.github.lunasaw.voglander.service.sse;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.util.StringUtils;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import lombok.Value;

/** Immutable authenticated subscription retained only for one SSE connection. */
@Value
public class SseSubscriptionContext {
    String emitterId;
    String userId;
    Set<String> topics;

    public static SseSubscriptionContext authorized(String userId, Set<String> topics) {
        if (!StringUtils.hasText(userId)) throw invalidSubscription();
        Set<String> normalized = normalize(topics);
        return new SseSubscriptionContext(UUID.randomUUID().toString(), userId, normalized);
    }

    private static Set<String> normalize(Set<String> topics) {
        if (topics == null || topics.isEmpty()) throw invalidSubscription();
        Set<String> result = new LinkedHashSet<>();
        for (String topic : topics) if (StringUtils.hasText(topic)) result.add(topic.trim());
        if (result.isEmpty()) throw invalidSubscription();
        return Collections.unmodifiableSet(result);
    }

    private static ServiceException invalidSubscription() {
        return new ServiceException(ServiceExceptionEnum.SSE_TOPIC_INVALID);
    }
}
