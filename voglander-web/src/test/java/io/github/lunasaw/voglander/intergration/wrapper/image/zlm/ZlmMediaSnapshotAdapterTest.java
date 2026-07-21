package io.github.lunasaw.voglander.intergration.wrapper.image.zlm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.github.lunasaw.voglander.client.domain.image.MediaSnapshotCommand;
import io.github.lunasaw.voglander.client.domain.image.SnapshotContent;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.req.SnapshotReq;
import io.github.lunasaw.zlm.node.service.NodeService;

class ZlmMediaSnapshotAdapterTest {

    @Test
    void capture_shouldUseResolvedNodeAndStrictSnapshotOptions_thenDeleteOwnedFileOnClose() throws Exception {
        NodeService nodeService = mock(NodeService.class);
        ZlmNode node = node("node-1", "http://zlm", "secret");
        when(nodeService.getAvailableNode("node-1")).thenReturn(node);
        ZlmMediaSnapshotAdapter adapter = new ZlmMediaSnapshotAdapter(nodeService);
        AtomicReference<SnapshotReq> requestRef = new AtomicReference<>();

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            zlm.when(() -> ZlmRestService.getSnap(any(String.class), any(String.class), any(SnapshotReq.class)))
                .thenAnswer(invocation -> {
                    SnapshotReq request = invocation.getArgument(2);
                    requestRef.set(request);
                    Files.writeString(Path.of(request.getSavePath()), "jpeg-bytes");
                    return request.getSavePath();
                });

            SnapshotContent content = adapter.capture(new MediaSnapshotCommand("node-1", "rtsp://camera/live", 15));
            Path ownedPath = Path.of(requestRef.get().getSavePath());
            assertTrue(Files.isRegularFile(ownedPath));
            assertNotNull(content);
            assertTrue(content.contentLength() > 0);
            assertTrue(ownedPath.isAbsolute());
            assertTrue(ownedPath.normalize().equals(ownedPath));
            content.close();
            assertFalse(Files.exists(ownedPath));

            SnapshotReq request = requestRef.get();
            assertTrue(request.getUrl().equals("rtsp://camera/live"));
            assertTrue(request.getTimeoutSec() == 15);
            assertTrue(request.getExpireSec() == 0);
            verify(nodeService).getAvailableNode("node-1");
        }
    }

    @Test
    void capture_shouldRejectEscapedOrEmptyReturnedPath_andCleanTemporaryFile() {
        NodeService nodeService = mock(NodeService.class);
        when(nodeService.getAvailableNode("node-1")).thenReturn(node("node-1", "http://zlm", "secret"));
        ZlmMediaSnapshotAdapter adapter = new ZlmMediaSnapshotAdapter(nodeService);

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            zlm.when(() -> ZlmRestService.getSnap(any(String.class), any(String.class), any(SnapshotReq.class)))
                .thenAnswer(invocation -> Path.of(invocation.<SnapshotReq>getArgument(2).getSavePath())
                    .resolveSibling("outside.jpg").toString());
            assertThrows(java.io.IOException.class,
                () -> adapter.capture(new MediaSnapshotCommand("node-1", "http://camera/live", 15)));
        }
    }

    @Test
    void capture_shouldFailBeforeCallingZlmWhenNodeIsUnavailable() {
        NodeService nodeService = mock(NodeService.class);
        when(nodeService.getAvailableNode("missing")).thenReturn(null);
        ZlmMediaSnapshotAdapter adapter = new ZlmMediaSnapshotAdapter(nodeService);
        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            assertThrows(java.io.IOException.class,
                () -> adapter.capture(new MediaSnapshotCommand("missing", "http://camera/live", 15)));
            zlm.verifyNoInteractions();
        }
    }

    private static ZlmNode node(String id, String host, String secret) {
        ZlmNode node = new ZlmNode();
        node.setServerId(id);
        node.setHost(host);
        node.setSecret(secret);
        node.setEnabled(true);
        return node;
    }
}
