package io.github.lunasaw.voglander.client.domain.image;

/** Provider-independent request for a bounded staging write. */
public final class ImageStageCommand {
    private final String assetId;
    private final String workerNode;
    private final long maxBytes;

    public ImageStageCommand(String assetId, String workerNode, long maxBytes) {
        if (assetId == null || assetId.trim().isEmpty()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
        if (workerNode == null || workerNode.trim().isEmpty()) {
            throw new IllegalArgumentException("workerNode must not be blank");
        }
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        this.assetId = assetId;
        this.workerNode = workerNode;
        this.maxBytes = maxBytes;
    }

    public String assetId() {
        return assetId;
    }

    public String workerNode() {
        return workerNode;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
