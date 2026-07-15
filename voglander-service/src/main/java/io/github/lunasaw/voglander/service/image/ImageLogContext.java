package io.github.lunasaw.voglander.service.image;

import java.util.Map;

import org.slf4j.MDC;

/** Bounded MDC context for image operations; values are stable IDs/codes only. */
public final class ImageLogContext implements AutoCloseable {
    private final Map<String, String> previous;

    private ImageLogContext(Map<String, String> previous) {
        this.previous = previous;
    }

    public static ImageLogContext open(String taskId, String executionId, String assetId,
        String deviceId, String channelId, String actorId) {
        Map<String, String> old = MDC.getCopyOfContextMap();
        put("taskId", taskId);
        put("executionId", executionId);
        put("assetId", assetId);
        put("deviceId", deviceId);
        put("channelId", channelId);
        put("actorId", actorId);
        return new ImageLogContext(old);
    }

    private static void put(String key, String value) {
        if (value != null && !value.isBlank()) MDC.put(key, ImageLogSanitizer.identifier(value));
    }

    @Override
    public void close() {
        if (previous == null) MDC.clear(); else MDC.setContextMap(previous);
    }
}
