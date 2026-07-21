package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.service.ImageAssetService;
import io.github.lunasaw.voglander.manager.service.ImageAssetSourceService;
import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;
import io.github.lunasaw.voglander.repository.mapper.ImageAssetMapper;
import io.github.lunasaw.voglander.repository.mapper.ImageAssetSourceMapper;

class ImageAssetManagerTest {
    @Mock private ImageAssetService assetService;
    @Mock private ImageAssetSourceService sourceService;
    @Mock private ImageAssetMapper assetMapper;
    @Mock private ImageAssetSourceMapper sourceMapper;
    @Mock private BusinessTaskSseEventPublisher publisher;
    private ImageAssetManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new ImageAssetManager();
        ReflectionTestUtils.setField(manager, "assetService", assetService);
        ReflectionTestUtils.setField(manager, "sourceService", sourceService);
        ReflectionTestUtils.setField(manager, "assetMapper", assetMapper);
        ReflectionTestUtils.setField(manager, "sourceMapper", sourceMapper);
        ReflectionTestUtils.setField(manager, "sseEventPublisher", publisher);
    }

    @Test
    void createWithSourceIsIdempotentByAssetId() {
        ImageAssetDO existing = new ImageAssetDO(); existing.setAssetId("img_existing"); existing.setAssetName("old");
        when(assetMapper.selectByAssetId("img_existing")).thenReturn(existing);
        ImageAssetDTO asset = validAsset("img_existing"); ImageAssetSourceDTO source = validSource("img_existing");

        ImageAssetDTO result = manager.createWithSource(asset, source);

        assertEquals("img_existing", result.getAssetId());
        verify(assetMapper, never()).insertIfAbsent(any());
        verify(sourceMapper, never()).insertIfAbsent(any());
    }

    @Test
    void createWithSourceSetsSafeDefaultsAndWritesBothFacts() {
        when(assetMapper.selectByAssetId("img_new")).thenReturn(null);
        when(assetMapper.insertIfAbsent(any())).thenReturn(1);
        when(sourceMapper.insertIfAbsent(any())).thenReturn(1);
        ImageAssetDTO asset = validAsset("img_new"); ImageAssetSourceDTO source = validSource("img_new");

        ImageAssetDTO result = manager.createWithSource(asset, source);

        assertEquals("AVAILABLE", result.getStatus());
        assertEquals("PERMANENT", result.getRetentionPolicy());
        assertEquals("SHA256", result.getChecksumAlgorithm());
        verify(assetMapper).insertIfAbsent(any()); verify(sourceMapper).insertIfAbsent(any());
        ArgumentCaptor<io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent> event = ArgumentCaptor.forClass(io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent.class);
        verify(publisher).publish(event.capture());
        assertEquals("image.asset.created", event.getValue().getTopic());
    }

    @Test
    void compareAndSetDeleteTransitionsDelegateToConditionalMapper() {
        ImageAssetDO asset = new ImageAssetDO(); asset.setAssetId("img_1"); asset.setStatus("AVAILABLE"); asset.setVersion(1);
        when(assetMapper.selectByAssetId("img_1")).thenReturn(asset);
        when(assetMapper.markDeleting("img_1", 0, LocalDateTime.MIN)).thenReturn(1);
        when(assetMapper.markDeleted("img_1", 1, LocalDateTime.MIN, LocalDateTime.MIN)).thenReturn(1);
        when(assetMapper.markDeleteFailed("img_1", 1, "FAILED", "safe", LocalDateTime.MIN)).thenReturn(1);
        assertEquals(true, manager.markDeleting("img_1", 0, LocalDateTime.MIN));
        assertEquals(true, manager.markDeleted("img_1", 1, LocalDateTime.MIN));
        assertEquals(true, manager.markDeleteFailed("img_1", 1, "FAILED", "safe", LocalDateTime.MIN));
        ArgumentCaptor<io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent> events = ArgumentCaptor.forClass(io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent.class);
        verify(publisher, org.mockito.Mockito.atLeastOnce()).publish(events.capture());
        assertEquals("image.asset.deleted", events.getAllValues().get(events.getAllValues().size() - 1).getTopic());
    }

    @Test
    void statisticsUsesOneAccessScopeForEveryCounter() {
        when(assetMapper.countVisible("USER", "7", null, null)).thenReturn(4L);
        when(assetMapper.countVisible("USER", "7", "AVAILABLE", null)).thenReturn(3L);
        when(assetMapper.countVisible("USER", "7", "DELETE_FAILED", null)).thenReturn(1L);
        io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetStatisticsDTO stats = manager.statistics("USER", "7");
        assertEquals(4L, stats.getTotal()); assertEquals(3L, stats.getAvailable()); assertEquals(1L, stats.getDeleteFailed());
        verify(assetMapper).countVisible("USER", "7", null, null);
        verify(assetMapper).countVisible("USER", "7", "AVAILABLE", null);
    }

    @Test
    void composedPagePropagatesRepositoryFailureInsteadOfReturningPartialData() {
        when(assetMapper.selectPageByCondition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("query failed"));
        io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetQueryDTO query =
            new io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetQueryDTO();

        assertThrows(IllegalStateException.class, () -> manager.getPage(query, 1, 20));
    }

    @Test
    void sourceLookupSupportsAssetTaskAndExecutionIdentities() {
        ImageAssetSourceDO source = new ImageAssetSourceDO();
        source.setAssetId("img_1"); source.setSourceTaskId("btask_1"); source.setSourceExecutionId("bexec_1");
        when(sourceMapper.selectByAssetId("img_1")).thenReturn(source);
        when(sourceMapper.selectByTaskId("btask_1")).thenReturn(source);
        when(sourceMapper.selectByExecutionId("bexec_1")).thenReturn(source);

        assertEquals("img_1", manager.getSourceByAssetId("img_1").getAssetId());
        assertEquals("btask_1", manager.getSourceByTaskId("btask_1").getSourceTaskId());
        assertEquals("bexec_1", manager.getSourceByExecutionId("bexec_1").getSourceExecutionId());
    }

    private static ImageAssetDTO validAsset(String id) {
        ImageAssetDTO asset = new ImageAssetDTO(); asset.setAssetId(id); asset.setAssetName("image"); asset.setOwnerType("USER");
        asset.setOwnerId("7"); asset.setStorageKey("images/2026/01/01/" + id + ".jpg"); asset.setContentType("image/jpeg");
        asset.setImageFormat("JPEG"); asset.setFileSize(1L); asset.setWidth(1); asset.setHeight(1); asset.setChecksum("a");
        return asset;
    }

    private static ImageAssetSourceDTO validSource(String id) { ImageAssetSourceDTO source = new ImageAssetSourceDTO(); source.setAssetId(id); source.setSourceType("USER_UPLOAD"); return source; }
}
