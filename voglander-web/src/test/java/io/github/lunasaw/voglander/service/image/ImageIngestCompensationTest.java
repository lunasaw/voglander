package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class ImageIngestCompensationTest {

    private static final byte[] CONTENT = new byte[] {1, 2, 3};
    private static final String CHECKSUM = "checksum-a";
    private static final String STAGING_KEY = ".staging/worker-a/upload.part";
    private static final String FINAL_KEY = "images/loser.jpg";

    @Mock
    private ImageStorageService storage;
    @Mock
    private ImageValidationService validation;
    @Mock
    private ImageAssetManager assetManager;
    @Mock
    private ImageOrphanRecorder orphanRecorder;

    private ImageIngestService service;

    @BeforeEach
    void setUp() throws Exception {
        ImageProperties properties = new ImageProperties();
        properties.getStorage().setWorkerNode("worker-a");
        properties.getStorage().setMaxUploadBytes(1024);
        properties.getCollection().setMaxPixels(1000);
        service = new ImageIngestService(storage, validation, assetManager, properties, orphanRecorder);

        when(validation.sanitizeOptionalFilename("photo.jpg")).thenReturn("photo.jpg");
        when(storage.stage(any(), any())).thenReturn(new StagedImage(STAGING_KEY, CONTENT.length, CHECKSUM));
        when(storage.openStaged(STAGING_KEY)).thenReturn(new ByteArrayInputStream(CONTENT));
        when(validation.inspect(any(), any(Long.class), any(), any(Long.class), any(Long.class)))
            .thenReturn(new VerifiedImage(ImageFormatEnum.JPEG, "image/jpeg", CONTENT.length, 2, 2));
        lenient().when(storage.promote(any())).thenReturn(new StoredImage(FINAL_KEY, CONTENT.length));
    }

    @Test
    void managerFailureWithSuccessfulDeleteRemovesFinalWithoutOrphan() throws Exception {
        when(assetManager.createWithSource(any(), any())).thenThrow(new IllegalStateException("db failed"));
        when(storage.delete(FINAL_KEY)).thenReturn(true);

        ServiceException error = assertThrows(ServiceException.class, this::ingest);

        assertEquals(ServiceExceptionEnum.IMAGE_STORAGE_WRITE_FAILED.getCode(), error.getCode());
        verify(storage).delete(FINAL_KEY);
        verify(orphanRecorder, never()).record(any(), any());
    }

    @Test
    void managerFailureWithDeleteFalseRecordsStableOrphanCode() throws Exception {
        when(assetManager.createWithSource(any(), any())).thenThrow(new IllegalStateException("db failed"));
        when(storage.delete(FINAL_KEY)).thenReturn(false);

        ServiceException error = assertThrows(ServiceException.class, this::ingest);

        assertEquals(ServiceExceptionEnum.IMAGE_STORAGE_WRITE_FAILED.getCode(), error.getCode());
        verify(orphanRecorder).record("INGEST_COMPENSATION_DELETE_FALSE", FINAL_KEY);
    }

    @Test
    void managerFailureWithDeleteExceptionRecordsStableOrphanCode() throws Exception {
        when(assetManager.createWithSource(any(), any())).thenThrow(new IllegalStateException("db failed"));
        when(storage.delete(FINAL_KEY)).thenThrow(new IllegalStateException("delete failed"));

        ServiceException error = assertThrows(ServiceException.class, this::ingest);

        assertEquals(ServiceExceptionEnum.IMAGE_STORAGE_WRITE_FAILED.getCode(), error.getCode());
        verify(orphanRecorder).record("INGEST_COMPENSATION_DELETE_FAILED", FINAL_KEY);
    }

    @Test
    void lostRaceWithDeleteFalseReturnsReplayAndRecordsStableOrphanCode() throws Exception {
        ImageAssetDTO winner = winner(CHECKSUM);
        when(assetManager.createWithSource(any(), any()))
            .thenReturn(new ImageAssetCreateResultDTO(false, winner, source()));
        when(storage.delete(FINAL_KEY)).thenReturn(false);

        assertSame(winner, ingest());
        verify(orphanRecorder).record("INGEST_RACE_DELETE_FALSE", FINAL_KEY);
    }

    @Test
    void lostRaceWithDeleteExceptionKeepsConflictAsPrimaryResult() throws Exception {
        ImageAssetDTO winner = winner("different-checksum");
        when(assetManager.createWithSource(any(), any()))
            .thenReturn(new ImageAssetCreateResultDTO(false, winner, source()));
        when(storage.delete(FINAL_KEY)).thenThrow(new IllegalStateException("delete failed"));

        ServiceException error = assertThrows(ServiceException.class, this::ingest);

        assertEquals(ServiceExceptionEnum.IDEMPOTENCY_KEY_REUSED.getCode(), error.getCode());
        verify(orphanRecorder).record("INGEST_RACE_DELETE_FAILED", FINAL_KEY);
    }

    @Test
    void replayWithStagingDiscardExceptionStillReturnsWinnerAndRecordsOrphan() throws Exception {
        ImageAssetDTO winner = winner(CHECKSUM);
        when(assetManager.findByIdempotency("USER", "7", "same-key")).thenReturn(winner);
        when(assetManager.getSourceByAssetId("img_winner")).thenReturn(source());
        doThrow(new IllegalStateException("discard failed")).when(storage).discardStaged(STAGING_KEY);

        assertSame(winner, ingest());
        verify(orphanRecorder).record("INGEST_STAGING_DISCARD_FAILED", STAGING_KEY);
        verify(storage, never()).promote(any());
    }

    @Test
    void conflictWithStagingDiscardExceptionKeepsConflictAsPrimaryResult() throws Exception {
        ImageAssetDTO winner = winner("different-checksum");
        when(assetManager.findByIdempotency("USER", "7", "same-key")).thenReturn(winner);
        when(assetManager.getSourceByAssetId("img_winner")).thenReturn(source());
        doThrow(new IllegalStateException("discard failed")).when(storage).discardStaged(STAGING_KEY);

        ServiceException error = assertThrows(ServiceException.class, this::ingest);

        assertEquals(ServiceExceptionEnum.IDEMPOTENCY_KEY_REUSED.getCode(), error.getCode());
        verify(orphanRecorder).record("INGEST_STAGING_DISCARD_FAILED", STAGING_KEY);
        verify(storage, never()).promote(any());
    }

    private ImageAssetDTO ingest() throws Exception {
        return service.ingestUpload(command(), new ByteArrayInputStream(CONTENT));
    }

    private static ImageIngestCommand command() {
        return new ImageIngestCommand("USER", "7", null, "same-key", "photo.jpg", "image/jpeg",
            "photo.jpg");
    }

    private static ImageAssetDTO winner(String checksum) {
        ImageAssetDTO asset = new ImageAssetDTO();
        asset.setAssetId("img_winner");
        asset.setOwnerType("USER");
        asset.setOwnerId("7");
        asset.setChecksum(checksum);
        asset.setAssetName("photo.jpg");
        return asset;
    }

    private static ImageAssetSourceDTO source() {
        ImageAssetSourceDTO source = new ImageAssetSourceDTO();
        source.setAssetId("img_winner");
        source.setOriginalFilename("photo.jpg");
        return source;
    }
}
