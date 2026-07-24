package io.github.lunasaw.voglander.service.sse;

import java.util.Set;

import org.springframework.stereotype.Component;

/** Shared local/Redis topic-matching decision for an authenticated subscription. */
@Component
public class SseDeliveryAuthorizer {

    public boolean allow(SseSubscriptionContext context, SseEvent event) {
        return context != null && event != null && matches(context.getTopics(), event.getTopic());
    }

    private boolean matches(Set<String> subscribed, String topic) {
        if (subscribed == null || topic == null) return false;
        if (subscribed.contains(topic)) return true;
        for (String candidate : subscribed) {
            if (candidate != null && !candidate.isEmpty() && topic.startsWith(candidate + ".")) return true;
        }
        return false;
    }
}
