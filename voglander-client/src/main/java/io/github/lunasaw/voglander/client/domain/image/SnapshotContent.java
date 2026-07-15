package io.github.lunasaw.voglander.client.domain.image;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Snapshot bytes plus capture facts and deterministic temporary-resource cleanup. */
public final class SnapshotContent implements AutoCloseable {

    private final InputStream inputStream;
    private final long contentLength;
    private final Instant capturedAt;
    private final Runnable cleanup;
    private final AtomicBoolean closed = new AtomicBoolean();

    public SnapshotContent(InputStream inputStream, long contentLength, Instant capturedAt, Runnable cleanup) {
        this.inputStream = Objects.requireNonNull(inputStream, "inputStream");
        if (contentLength <= 0) {
            throw new IllegalArgumentException("contentLength must be positive");
        }
        this.contentLength = contentLength;
        this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
        this.cleanup = Objects.requireNonNull(cleanup, "cleanup");
    }

    public InputStream inputStream() {
        return inputStream;
    }

    public long contentLength() {
        return contentLength;
    }

    public Instant capturedAt() {
        return capturedAt;
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        IOException closeFailure = null;
        try {
            inputStream.close();
        } catch (IOException e) {
            closeFailure = e;
        } finally {
            cleanup.run();
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }
}
