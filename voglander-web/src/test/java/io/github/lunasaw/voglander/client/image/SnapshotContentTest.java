package io.github.lunasaw.voglander.client.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.domain.image.SnapshotContent;

class SnapshotContentTest {

    @Test
    void closeReleasesTemporaryResourceExactlyOnce() throws Exception {
        AtomicInteger cleanupCount = new AtomicInteger();
        SnapshotContent content = new SnapshotContent(new ByteArrayInputStream(new byte[] {1}), 1,
            Instant.parse("2026-07-14T00:00:00Z"), cleanupCount::incrementAndGet);

        content.close();
        content.close();

        assertEquals(1, cleanupCount.get());
    }
}
