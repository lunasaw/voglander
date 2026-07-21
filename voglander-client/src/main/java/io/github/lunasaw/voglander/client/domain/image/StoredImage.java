package io.github.lunasaw.voglander.client.domain.image;

/** Permanent provider reference returned after promotion. */
public final class StoredImage {
    private final String storageKey;
    private final long fileSize;

    public StoredImage(String storageKey, long fileSize) {
        if (storageKey == null || storageKey.trim().isEmpty()) {
            throw new IllegalArgumentException("storageKey must not be blank");
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize must not be negative");
        }
        this.storageKey = storageKey;
        this.fileSize = fileSize;
    }

    public String storageKey() {
        return storageKey;
    }

    public long fileSize() {
        return fileSize;
    }
}
