package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.lunasaw.voglander.client.domain.image.StagedImage;
import io.github.lunasaw.voglander.client.domain.image.StoredImage;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.enums.image.ImageFormatEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetCreateResultDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.service.idempotency.IdempotencyMetrics;

class ImageIngestServiceTest {
    @Mock private ImageStorageService storage;
    @Mock private ImageValidationService validation;
    @Mock private ImageAssetManager assetManager;
    @Mock private ImageOrphanRecorder orphanRecorder;
    @Mock private IdempotencyMetrics idempotencyMetrics;
    private ImageProperties properties;
    private ImageIngestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new ImageProperties();
        properties.getStorage().setWorkerNode("worker-a");
        properties.getStorage().setMaxUploadBytes(1024);
        properties.getCollection().setMaxPixels(1000);
        service = new ImageIngestService(storage, validation, assetManager, properties, orphanRecorder, null,
            idempotencyMetrics);
    }

    @Test
    void ingestStagesAndInspectsBeforeIdempotencyCompareThenPromotesAndRegisters() throws Exception {
        byte[] bytes = "staged-bytes".getBytes(StandardCharsets.UTF_8);
        StagedImage staged = stubStaged(bytes, "abc123", "photo.jpg");
        StoredImage stored = new StoredImage("images/2026/07/15/img_1.jpg", bytes.length);
        ImageAssetDTO persisted = acceptedAsset("img_1", "abc123", "photo.jpg");
        when(assetManager.findByIdempotency("USER", "7", "idem-1")).thenReturn(null);
        when(storage.promote(any())).thenReturn(stored);
        when(assetManager.createWithSource(any(ImageAssetDTO.class), any(ImageAssetSourceDTO.class)))
            .thenReturn(new ImageAssetCreateResultDTO(true, persisted, source("img_1", "photo.jpg")));

        ImageAssetDTO result = service.ingestUpload(command("idem-1", "photo.jpg", null),
            new ByteArrayInputStream(bytes));

        assertSame(persisted, result);
        InOrder order = inOrder(storage, validation, assetManager);
        order.verify(storage).stage(any(), any());
        order.verify(storage).openStaged(staged.stagingKey());
        order.verify(validation).inspect(any(), any(Long.class), any(), any(Long.class), any(Long.class));
        order.verify(assetManager).findByIdempotency("USER", "7", "idem-1");
        order.verify(storage).promote(argThat(value -> value.stagingKey().equals(staged.stagingKey())
            && value.finalKey().matches("images/\\d{4}/\\d{2}/\\d{2}/img_[0-9a-f]{32}\\.jpg")));
        order.verify(assetManager).createWithSource(any(ImageAssetDTO.class), any(ImageAssetSourceDTO.class));
        verify(storage).discardStaged(staged.stagingKey());
        verify(storage, never()).delete(any());
        verify(idempotencyMetrics).record("CREATED", null);
    }

    @Test
    void sameKeyAndCanonicalContentReturnsWinnerAfterDiscardingNewStage() throws Exception {
        byte[] bytes = new byte[] {1, 2, 3};
        StagedImage staged = stubStaged(bytes, "same-checksum", "photo.jpg");
        ImageAssetDTO existing = acceptedAsset("img_existing", "same-checksum", "photo.jpg");
        when(assetManager.findByIdempotency("USER", "7", "same-key")).thenReturn(existing);
        when(assetManager.getSourceByAssetId("img_existing")).thenReturn(source("img_existing", "photo.jpg"));

        ImageAssetDTO result = service.ingestUpload(command("same-key", "photo.jpg", null),
            new ByteArrayInputStream(bytes));

        assertSame(existing, result);
        verify(storage, never()).promote(any());
        verify(assetManager, never()).createWithSource(any(), any());
        verify(storage).discardStaged(staged.stagingKey());
        verify(idempotencyMetrics).record("REPLAYED", null);
    }

    @Test
    void sameKeyWithDifferentContentReturnsDeterministicConflict() throws Exception {
        byte[] bytes = new byte[] {4, 5, 6};
        StagedImage staged = stubStaged(bytes, "new-checksum", "photo.jpg");
        ImageAssetDTO existing = acceptedAsset("img_existing", "old-checksum", "photo.jpg");
        when(assetManager.findByIdempotency("USER", "7", "same-key")).thenReturn(existing);
        when(assetManager.getSourceByAssetId("img_existing")).thenReturn(source("img_existing", "photo.jpg"));

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.ingestUpload(command("same-key", "photo.jpg", null),
                new ByteArrayInputStream(bytes)));

        assertEquals(ServiceExceptionEnum.IDEMPOTENCY_KEY_REUSED.getCode(), error.getCode());
        verify(storage, never()).promote(any());
        verify(storage).discardStaged(staged.stagingKey());
        verify(idempotencyMetrics).record("CONFLICT", "600007");
    }

    @Test
    void lostInsertRaceDeletesLoserFinalAndReturnsSameContentWinner() throws Exception {
        byte[] bytes = new byte[] {7, 8, 9};
        stubStaged(bytes, "same-checksum", "photo.jpg");
        StoredImage stored = new StoredImage("images/loser.jpg", bytes.length);
        ImageAssetDTO winner = acceptedAsset("img_winner", "same-checksum", "photo.jpg");
        when(assetManager.findByIdempotency("USER", "7", "race-key")).thenReturn(null);
        when(storage.promote(any())).thenReturn(stored);
        when(storage.delete(stored.storageKey())).thenReturn(true);
        when(assetManager.createWithSource(any(), any()))
            .thenReturn(new ImageAssetCreateResultDTO(false, winner, source("img_winner", "photo.jpg")));

        ImageAssetDTO result = service.ingestUpload(command("race-key", "photo.jpg", null),
            new ByteArrayInputStream(bytes));

        assertSame(winner, result);
        verify(storage).delete(stored.storageKey());
    }

    @Test
    void databaseFailureDeletesPromotedObjectAndRecordsFailedCompensation() throws Exception {
        byte[] bytes = new byte[] {1, 2, 3};
        StagedImage staged = stubStaged(bytes, "deadbeef", "failure.jpg");
        StoredImage stored = new StoredImage("images/2026/07/15/failure.jpg", bytes.length);
        when(storage.promote(any())).thenReturn(stored);
        when(assetManager.createWithSource(any(), any())).thenThrow(new RuntimeException("db unavailable"));
        when(storage.delete(stored.storageKey())).thenReturn(false);

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.ingestUpload(command("idem-failure", "failure.jpg", null),
                new ByteArrayInputStream(bytes)));

        assertEquals(ServiceExceptionEnum.IMAGE_STORAGE_WRITE_FAILED.getCode(), error.getCode());
        verify(storage).delete(stored.storageKey());
        verify(orphanRecorder).record("INGEST_COMPENSATION_DELETE_FALSE", stored.storageKey());
        verify(storage).discardStaged(staged.stagingKey());
        verify(idempotencyMetrics).record("COMPENSATION_FAILURE", "710008");
    }

    private StagedImage stubStaged(byte[] bytes, String checksum, String filename) throws Exception {
        StagedImage staged = new StagedImage(".staging/worker-a/" + checksum + ".part", bytes.length, checksum);
        VerifiedImage verified = new VerifiedImage(ImageFormatEnum.JPEG, "image/jpeg", bytes.length, 2, 2);
        when(storage.stage(any(), any())).thenReturn(staged);
        when(storage.openStaged(staged.stagingKey())).thenReturn(new ByteArrayInputStream(bytes));
        when(validation.inspect(any(), any(Long.class), any(), any(Long.class), any(Long.class)))
            .thenReturn(verified);
        when(validation.sanitizeOptionalFilename(filename)).thenReturn(filename);
        return staged;
    }

    private static ImageIngestCommand command(String key, String originalFilename, String assetName) {
        return new ImageIngestCommand("USER", "7", null, key, originalFilename, "image/jpeg", assetName);
    }

    private static ImageAssetDTO acceptedAsset(String assetId, String checksum, String assetName) {
        ImageAssetDTO asset = new ImageAssetDTO();
        asset.setAssetId(assetId);
        asset.setOwnerType("USER");
        asset.setOwnerId("7");
        asset.setChecksum(checksum);
        asset.setAssetName(assetName);
        return asset;
    }

    private static ImageAssetSourceDTO source(String assetId, String originalFilename) {
        ImageAssetSourceDTO source = new ImageAssetSourceDTO();
        source.setAssetId(assetId);
        source.setOriginalFilename(originalFilename);
        return source;
    }
}
