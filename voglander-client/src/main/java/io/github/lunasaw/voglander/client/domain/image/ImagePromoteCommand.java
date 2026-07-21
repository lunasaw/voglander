package io.github.lunasaw.voglander.client.domain.image;

/** Request to promote a staged object to a generated permanent key. */
public final class ImagePromoteCommand {
    private final String stagingKey;
    private final String finalKey;

    public ImagePromoteCommand(String stagingKey, String finalKey) {
        if (stagingKey == null || stagingKey.trim().isEmpty()) {
            throw new IllegalArgumentException("stagingKey must not be blank");
        }
        if (finalKey == null || finalKey.trim().isEmpty()) {
            throw new IllegalArgumentException("finalKey must not be blank");
        }
        this.stagingKey = stagingKey;
        this.finalKey = finalKey;
    }

    public String stagingKey() {
        return stagingKey;
    }

    public String finalKey() {
        return finalKey;
    }
}
