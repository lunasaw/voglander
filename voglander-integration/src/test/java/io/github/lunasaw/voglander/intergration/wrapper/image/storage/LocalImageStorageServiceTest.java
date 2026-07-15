package io.github.lunasaw.voglander.intergration.wrapper.image.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.domain.image.ImagePromoteCommand;
import io.github.lunasaw.voglander.client.domain.image.ImageStageCommand;

class LocalImageStorageServiceTest {

    private Path root;
    private LocalImageStorageService storage;

    @BeforeEach
    void setUp() throws Exception {
        root = Files.createTempDirectory("image-storage-");
        storage = new LocalImageStorageService(root, "node-a");
        assertTrue(storage.isHealthy());
    }

    @AfterEach
    void tearDown() throws Exception {
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // best effort test cleanup
                }
            });
        }
    }

    @Test
    void stagePromoteOpenAndDelete_shouldUseRelativeContainedKeys() throws Exception {
        byte[] bytes = "jpeg-placeholder".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var staged = storage.stage(new ImageStageCommand("img_1", "node-a", 1024),
            new ByteArrayInputStream(bytes));
        assertTrue(staged.stagingKey().startsWith(".staging/node-a/"));
        assertEquals(bytes.length, staged.fileSize());

        var stored = storage.promote(new ImagePromoteCommand(staged.stagingKey(), "images/2026/07/img_1.jpg"));
        assertEquals("images/2026/07/img_1.jpg", stored.storageKey());
        try (var content = storage.open(stored.storageKey())) {
            assertArrayEquals(bytes, content.inputStream().readAllBytes());
        }
        assertTrue(storage.exists(stored.storageKey()));
        assertTrue(storage.delete(stored.storageKey()));
        assertTrue(storage.delete(stored.storageKey()));
        assertFalse(storage.exists(stored.storageKey()));
    }

    @Test
    void stage_shouldEnforceHardLimitAndCleanPartialFile() throws Exception {
        assertThrows(java.io.IOException.class,
            () -> storage.stage(new ImageStageCommand("img_2", "node-a", 3),
                new ByteArrayInputStream(new byte[] {1, 2, 3, 4})));
        try (var files = Files.list(root.resolve(".staging/node-a"))) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void keysOutsideNamespaceOrThroughSymlink_shouldBeRejected() throws Exception {
        assertThrows(java.io.IOException.class,
            () -> storage.open("/etc/passwd"));
        assertThrows(java.io.IOException.class,
            () -> storage.delete("images/../.staging/node-a/x.part"));
        Path outside = Files.createTempDirectory("image-storage-outside-");
        try {
            Files.createDirectories(root.resolve("images"));
            Files.createSymbolicLink(root.resolve("images/link"), outside);
            assertThrows(java.io.IOException.class,
                () -> storage.delete("images/link/escape.jpg"));
        } finally {
            Files.deleteIfExists(root.resolve("images/link"));
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void stagingSweep_shouldDeleteOnlyExpiredParts() throws Exception {
        var staged = storage.stage(new ImageStageCommand("img_3", "node-a", 1024),
            new ByteArrayInputStream(new byte[] {1}));
        Path file = root.resolve(staged.stagingKey());
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
        assertEquals(1, storage.sweepStaging(Instant.parse("2020-01-01T02:00:00Z"), Duration.ofHours(1)));
        assertFalse(Files.exists(file));
    }

    @Test
    void stagingSweep_shouldUseInjectedClock() throws Exception {
        LocalImageStorageService clockStorage = new LocalImageStorageService(root, "node-clock",
            Clock.fixed(Instant.parse("2020-01-01T02:00:00Z"), java.time.ZoneOffset.UTC));
        var staged = clockStorage.stage(new ImageStageCommand("img_clock", "node-clock", 1024),
            new ByteArrayInputStream(new byte[] {1}));
        Path file = root.resolve(staged.stagingKey());
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
        assertEquals(1, clockStorage.sweepStaging(Duration.ofHours(1)));
    }

    @Test
    void promote_shouldNeverOverwriteAnExistingFinalObject() throws Exception {
        var first = storage.stage(new ImageStageCommand("img_first", "node-a", 1024),
            new ByteArrayInputStream(new byte[] {1}));
        storage.promote(new ImagePromoteCommand(first.stagingKey(), "images/2026/07/same.jpg"));

        var second = storage.stage(new ImageStageCommand("img_second", "node-a", 1024),
            new ByteArrayInputStream(new byte[] {2}));
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
            () -> storage.promote(new ImagePromoteCommand(second.stagingKey(), "images/2026/07/same.jpg")));
        assertTrue(Files.exists(root.resolve(second.stagingKey())));
    }

    @Test
    void promote_shouldFallbackWhenAtomicMoveIsUnsupportedAndCountFallback() throws Exception {
        AtomicBoolean atomicAttempted = new AtomicBoolean();
        LocalImageStorageService fallbackStorage = new LocalImageStorageService(root, "node-fallback") {
            @Override
            protected void moveAtomically(Path source, Path target) throws java.io.IOException {
                atomicAttempted.set(true);
                throw new java.nio.file.AtomicMoveNotSupportedException(source.toString(), target.toString(), "test");
            }
        };
        var staged = fallbackStorage.stage(new ImageStageCommand("img_fallback", "node-fallback", 1024),
            new ByteArrayInputStream(new byte[] {3}));

        var stored = fallbackStorage.promote(new ImagePromoteCommand(staged.stagingKey(), "images/2026/07/fallback.jpg"));

        assertTrue(atomicAttempted.get());
        assertEquals(1L, fallbackStorage.getAtomicMoveFallbackCount());
        assertTrue(fallbackStorage.exists(stored.storageKey()));
    }
}
