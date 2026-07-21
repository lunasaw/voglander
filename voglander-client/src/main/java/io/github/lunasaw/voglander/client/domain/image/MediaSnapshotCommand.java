package io.github.lunasaw.voglander.client.domain.image;

/** Provider-neutral request for a snapshot from a selected media node. */
public final class MediaSnapshotCommand {
    private final String nodeServerId;
    private final String snapshotUrl;
    private final int timeoutSeconds;

    public MediaSnapshotCommand(String nodeServerId, String snapshotUrl, int timeoutSeconds) {
        if (nodeServerId == null || nodeServerId.trim().isEmpty()) {
            throw new IllegalArgumentException("nodeServerId must not be blank");
        }
        if (snapshotUrl == null || snapshotUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("snapshotUrl must not be blank");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be positive");
        }
        this.nodeServerId = nodeServerId;
        this.snapshotUrl = snapshotUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String nodeServerId() {
        return nodeServerId;
    }

    public String snapshotUrl() {
        return snapshotUrl;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }
}
