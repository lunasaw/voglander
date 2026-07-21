package io.github.lunasaw.voglander.service.image;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.lunasaw.voglander.service.live.MediaPlayService;

/** Owns exactly one MediaPlayService.startLive reference. */
public final class CaptureStreamLease implements AutoCloseable {
    private final MediaPlayService mediaPlayService;
    private final String streamId;
    private final String nodeServerId;
    private final String snapshotUrl;
    private final AtomicBoolean closed = new AtomicBoolean();

    CaptureStreamLease(MediaPlayService mediaPlayService, String streamId, String nodeServerId, String snapshotUrl) {
        this.mediaPlayService = mediaPlayService;
        this.streamId = streamId;
        this.nodeServerId = nodeServerId;
        this.snapshotUrl = snapshotUrl;
    }

    public String getStreamId() { return streamId; }
    public String getNodeServerId() { return nodeServerId; }
    public String getSnapshotUrl() { return snapshotUrl; }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            mediaPlayService.stopLive(streamId);
        }
    }
}
