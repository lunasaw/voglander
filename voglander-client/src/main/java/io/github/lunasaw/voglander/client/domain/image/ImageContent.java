package io.github.lunasaw.voglander.client.domain.image;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Closeable image content returned by a storage provider. */
public final class ImageContent implements AutoCloseable {

    private final InputStream inputStream;
    private final long contentLength;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ImageContent(InputStream inputStream, long contentLength) {
        this.inputStream = Objects.requireNonNull(inputStream, "inputStream");
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must not be negative");
        }
        this.contentLength = contentLength;
    }

    public InputStream inputStream() {
        return inputStream;
    }

    public long contentLength() {
        return contentLength;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            inputStream.close();
        }
    }
}
