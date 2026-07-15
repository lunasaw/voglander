package io.github.lunasaw.voglander.intergration.wrapper.image.zlm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.client.domain.image.MediaSnapshotCommand;
import io.github.lunasaw.voglander.client.domain.image.SnapshotContent;
import io.github.lunasaw.voglander.client.service.image.MediaSnapshotAdapter;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.req.SnapshotReq;
import io.github.lunasaw.zlm.node.service.NodeService;

/** ZLM-backed snapshot adapter with strict temporary-file ownership. */
@Component
public class ZlmMediaSnapshotAdapter implements MediaSnapshotAdapter {

    private final NodeService nodeService;

    public ZlmMediaSnapshotAdapter(NodeService nodeService) {
        this.nodeService = Objects.requireNonNull(nodeService, "nodeService");
    }

    @Override
    public SnapshotContent capture(MediaSnapshotCommand command) throws IOException {
        Objects.requireNonNull(command, "command");
        ZlmNode node;
        try {
            node = nodeService.getAvailableNode(command.nodeServerId());
        } catch (RuntimeException exception) {
            throw new IOException("ZLM snapshot node is unavailable", exception);
        }
        if (node == null || node.getHost() == null || node.getSecret() == null) {
            throw new IOException("ZLM snapshot node is unavailable");
        }
        Path temporary = Files.createTempFile("voglander-snapshot-", ".jpg").toAbsolutePath().normalize();
        Path canonical = temporary.toFile().getCanonicalFile().toPath();
        try {
            SnapshotReq request = new SnapshotReq();
            request.setUrl(command.snapshotUrl());
            request.setTimeoutSec(command.timeoutSeconds());
            request.setExpireSec(0);
            request.setSavePath(canonical.toString());
            String returned = ZlmRestService.getSnap(node.getHost(), node.getSecret(), request);
            if (returned == null || !canonical.equals(Path.of(returned).toFile().getCanonicalFile().toPath())
                || !Files.isRegularFile(canonical) || Files.size(canonical) <= 0) {
                throw new IOException("ZLM snapshot response is empty or escaped temporary path");
            }
            InputStream input = Files.newInputStream(canonical);
            long size = Files.size(canonical);
            return new SnapshotContent(input, size, Instant.now(), () -> deleteQuietly(canonical));
        } catch (Exception exception) {
            deleteQuietly(canonical);
            if (exception instanceof IOException) throw (IOException) exception;
            throw new IOException("ZLM snapshot failed", exception);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Cleanup is best effort; the original operation remains the source of truth.
        }
    }
}
