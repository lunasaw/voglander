package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.client.domain.image.StagedImage;
import io.github.lunasaw.voglander.client.domain.image.StoredImage;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;

class ImageIngestServiceTest {
    @Mock private ImageStorageService storage;
    @Mock private ImageValidationService validation;
    @Mock private ImageAssetManager assetManager;
    private ImageProperties properties;
    private ImageIngestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new ImageProperties();
        properties.getStorage().setWorkerNode("worker-a");
        properties.getStorage().setMaxUploadBytes(1024);
        properties.getCollection().setMaxPixels(1000);
        service = new ImageIngestService(storage, validation, assetManager, properties);
    }

    @Test
    void ingest_shouldStageValidatePromoteThenRegisterAndDiscard() throws Exception {
        byte[] bytes = "staged-bytes".getBytes(StandardCharsets.UTF_8);
        StagedImage staged = new StagedImage(".staging/worker-a/upload.part", bytes.length, "abc123");
        StoredImage stored = new StoredImage("images/2026/07/15/img_1.jpg", bytes.length);
        VerifiedImage verified = new VerifiedImage(io.github.lunasaw.voglander.common.enums.image.ImageFormatEnum.JPEG,
            "image/jpeg", bytes.length, 2, 2);
        ImageAssetDTO persisted = new ImageAssetDTO();
        persisted.setAssetId("img_1");
        when(assetManager.findByIdempotency("USER", "7", "idem-1")).thenReturn(null);
        when(storage.stage(any(), any())).thenReturn(staged);
        when(storage.openStaged(staged.stagingKey())).thenReturn(new ByteArrayInputStream(bytes));
        when(validation.inspect(any(), any(Long.class), any(), any(Long.class), any(Long.class))).thenReturn(verified);
        when(validation.sanitizeFilename("photo.jpg", "img_1")).thenReturn("photo.jpg");
        when(storage.promote(any())).thenReturn(stored);
        when(assetManager.createWithSource(any(ImageAssetDTO.class), any(ImageAssetSourceDTO.class))).thenReturn(persisted);

        ImageAssetDTO result = service.ingestUpload(
            new ImageIngestCommand("USER", "7", null, "idem-1", "photo.jpg", "image/jpeg", null),
            new ByteArrayInputStream(bytes));

        assertSame(persisted, result);
        InOrder order = inOrder(storage, validation, assetManager);
        order.verify(storage).stage(any(), any());
        order.verify(storage).openStaged(staged.stagingKey());
        order.verify(validation).inspect(any(), any(Long.class), any(), any(Long.class), any(Long.class));
        order.verify(storage).promote(argThat(command -> command.stagingKey().equals(staged.stagingKey())
            && command.finalKey().matches("images/\\d{4}/\\d{2}/\\d{2}/img_[0-9a-f]{32}\\.jpg")));
        order.verify(assetManager).createWithSource(any(ImageAssetDTO.class), any(ImageAssetSourceDTO.class));
        verify(storage).discardStaged(staged.stagingKey());
    }

    @Test
    void ingest_shouldReturnIdempotentAssetWithoutReadingOrStagingAgain() throws Exception {
        ImageAssetDTO existing = new ImageAssetDTO();
        existing.setAssetId("img_existing");
        when(assetManager.findByIdempotency("USER", "7", "same-key")).thenReturn(existing);

        ImageAssetDTO result = service.ingestUpload(
            new ImageIngestCommand("USER", "7", null, "same-key", "ignored.jpg", "image/jpeg", null),
            new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertSame(existing, result);
        verifyNoInteractions(storage, validation);
        verify(assetManager, never()).createWithSource(any(), any());
    }

    @Test
    void ingest_shouldDeletePromotedObjectWhenDatabaseRegistrationFails() throws Exception {
        byte[] bytes = new byte[] {1, 2, 3};
        StagedImage staged = new StagedImage(".staging/worker-a/failure.part", bytes.length, "deadbeef");
        StoredImage stored = new StoredImage("images/2026/07/15/failure.jpg", bytes.length);
        VerifiedImage verified = new VerifiedImage(io.github.lunasaw.voglander.common.enums.image.ImageFormatEnum.JPEG,
            "image/jpeg", bytes.length, 1, 1);
        when(storage.stage(any(), any())).thenReturn(staged);
        when(storage.openStaged(staged.stagingKey())).thenReturn(new ByteArrayInputStream(bytes));
        when(validation.inspect(any(), any(Long.class), any(), any(Long.class), any(Long.class))).thenReturn(verified);
        when(validation.sanitizeFilename(any(), any())).thenReturn("failure.jpg");
        when(storage.promote(any())).thenReturn(stored);
        RuntimeException registrationFailure = new RuntimeException("db unavailable");
        when(assetManager.createWithSource(any(), any())).thenThrow(registrationFailure);

        io.github.lunasaw.voglander.common.exception.ServiceException thrown =
            org.junit.jupiter.api.Assertions.assertThrows(
                io.github.lunasaw.voglander.common.exception.ServiceException.class,
                () -> service.ingestUpload(new ImageIngestCommand("USER", "7", null, "idem-failure", "failure.jpg",
                    "image/jpeg", null), new ByteArrayInputStream(bytes)));
        assertEquals(io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum.IMAGE_STORAGE_WRITE_FAILED.getCode(),
            thrown.getCode());

        verify(storage).delete(argThat(key -> key.matches("images/\\d{4}/\\d{2}/\\d{2}/img_[0-9a-f]{32}\\.jpg")));
        verify(storage).discardStaged(staged.stagingKey());
    }
}
