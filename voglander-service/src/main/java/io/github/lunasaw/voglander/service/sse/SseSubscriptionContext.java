package io.github.lunasaw.voglander.service.sse;

import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.util.StringUtils;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import lombok.Builder;
import lombok.Value;

/** Immutable authorization snapshot retained only for one SSE connection. */
@Value
@Builder(toBuilder = true)
public class SseSubscriptionContext {
    private static final Set<String> ALLOWED_ROOTS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "business.task", "image.asset", "device", "live", "alarm")));

    String emitterId;
    String userId;
    Set<String> topics;
    boolean taskQueryAllowed;
    boolean imageCollectionQueryAllowed;
    boolean imageAssetQueryAllowed;
    Set<String> allowedTaskTypes;

    public static SseSubscriptionContext authorized(String userId, Set<String> topics,
        boolean taskQueryAllowed, boolean imageCollectionQueryAllowed, boolean imageAssetQueryAllowed) {
        if (!StringUtils.hasText(userId)) throw invalidSubscription();
        Set<String> normalized = normalize(topics);
        for (String topic : normalized) {
            String root = rootOf(topic);
            if (!ALLOWED_ROOTS.contains(root)) throw invalidSubscription();
            if ("business.task".equals(root) && !taskQueryAllowed && !imageCollectionQueryAllowed) {
                throw invalidSubscription();
            }
            if ("image.asset".equals(root) && !imageAssetQueryAllowed) throw invalidSubscription();
        }
        Set<String> allowedTypes = taskQueryAllowed ? null
            : imageCollectionQueryAllowed ? Collections.singleton("IMAGE_COLLECTION") : Collections.emptySet();
        return SseSubscriptionContext.builder()
            .emitterId(UUID.randomUUID().toString())
            .userId(userId)
            .topics(normalized)
            .taskQueryAllowed(taskQueryAllowed)
            .imageCollectionQueryAllowed(imageCollectionQueryAllowed)
            .imageAssetQueryAllowed(imageAssetQueryAllowed)
            .allowedTaskTypes(allowedTypes)
            .build();
    }

    private static Set<String> normalize(Set<String> topics) {
        if (topics == null || topics.isEmpty()) throw invalidSubscription();
        Set<String> result = new LinkedHashSet<>();
        for (String topic : topics) if (StringUtils.hasText(topic)) result.add(topic.trim());
        if (result.isEmpty()) throw invalidSubscription();
        return Collections.unmodifiableSet(result);
    }

    static String rootOf(String topic) {
        if (topic == null) return "";
        if (topic.equals("business.task") || topic.startsWith("business.task.")) return "business.task";
        if (topic.equals("image.asset") || topic.startsWith("image.asset.")) return "image.asset";
        int separator = topic.indexOf('.');
        return separator < 0 ? topic : topic.substring(0, separator);
    }

    private static ServiceException invalidSubscription() {
        return new ServiceException(ServiceExceptionEnum.SSE_TOPIC_INVALID);
    }
}
