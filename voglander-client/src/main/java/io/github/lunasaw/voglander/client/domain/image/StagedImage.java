package io.github.lunasaw.voglander.client.domain.image;

/** Facts produced while staging exact image bytes. */
public final class StagedImage {
    private final String stagingKey;
    private final long fileSize;
    private final String checksum;

    public StagedImage(String stagingKey, long fileSize, String checksum) {
        if (stagingKey == null || stagingKey.trim().isEmpty()) {
            throw new IllegalArgumentException("stagingKey must not be blank");
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize must not be negative");
        }
        if (checksum == null || checksum.trim().isEmpty()) {
            throw new IllegalArgumentException("checksum must not be blank");
        }
        this.stagingKey = stagingKey;
        this.fileSize = fileSize;
        this.checksum = checksum;
    }

    public String stagingKey() {
        return stagingKey;
    }

    public long fileSize() {
        return fileSize;
    }

    public String checksum() {
        return checksum;
    }
}
