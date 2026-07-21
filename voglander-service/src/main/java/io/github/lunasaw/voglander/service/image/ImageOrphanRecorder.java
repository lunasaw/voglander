package io.github.lunasaw.voglander.service.image;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** Small, bounded signal for provider objects that could not be compensated. */
@Component
@Slf4j
public class ImageOrphanRecorder {
    private final AtomicLong count = new AtomicLong();
    private final ImageDomainMetrics metrics;

    public ImageOrphanRecorder() {
        this(null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ImageOrphanRecorder(ImageDomainMetrics metrics) {
        this.metrics = metrics;
    }

    public void record(String kind, String storageKey) {
        count.incrementAndGet();
        if (metrics != null) metrics.orphan(kind);
        log.error("Image storage orphan recorded: kind={}, storageKeyPresent={}", ImageLogSanitizer.code(kind),
            storageKey != null && !storageKey.isBlank());
    }

    public long count() {
        return count.get();
    }
}
