package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.enums.image.ImageAssetStatusEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;

class ImageAssetLifecycleServiceTest {
    @Mock private ImageAssetManager assetManager;
    @Mock private ImageStorageService storage;
    private ImageAssetLifecycleService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ImageAssetLifecycleService(assetManager, storage);
    }

    @Test
    void delete_shouldCasThenRemoveProviderObjectThenMarkDeleted() throws Exception {
        ImageAssetDTO asset = asset("AVAILABLE", 3);
        when(assetManager.getByAssetId("img_1", "USER", "7")).thenReturn(asset);
        when(assetManager.markDeleting(eq("img_1"), eq(3), any())).thenReturn(true);
        when(assetManager.getByAssetId("img_1")).thenReturn(asset("DELETING", 4));
        when(storage.delete("images/a.jpg")).thenReturn(true);
        when(assetManager.markDeleted(eq("img_1"), eq(4), any())).thenReturn(true);

        assertEquals(true, service.delete("img_1", "USER", "7"));
        verify(storage).delete("images/a.jpg");
        verify(assetManager).markDeleted(eq("img_1"), eq(4), any());
    }

    @Test
    void delete_shouldTreatAlreadyDeletedAsIdempotent() throws Exception {
        when(assetManager.getByAssetId("img_1", "USER", "7")).thenReturn(asset("DELETED", 8));

        assertEquals(true, service.delete("img_1", "USER", "7"));
        verifyNoStorageCalls();
        verify(assetManager, never()).markDeleting(any(), any(Integer.class), any());
    }

    @Test
    void retryDelete_shouldOnlyRetryDeleteFailedAndMissingProviderObjectIsSuccess() throws Exception {
        when(assetManager.getByAssetId("img_1", "USER", "7")).thenReturn(asset("DELETE_FAILED", 8));
        when(assetManager.markDeleting(eq("img_1"), eq(8), any())).thenReturn(true);
        when(assetManager.getByAssetId("img_1")).thenReturn(asset("DELETING", 9));
        when(storage.delete("images/a.jpg")).thenReturn(false);
        when(storage.exists("images/a.jpg")).thenReturn(false);
        when(assetManager.markDeleted(eq("img_1"), eq(9), any())).thenReturn(true);

        assertEquals(true, service.retryDelete("img_1", "USER", "7"));
        verify(storage).exists("images/a.jpg");
        verify(assetManager).markDeleted(eq("img_1"), eq(9), any());
    }

    @Test
    void retryDelete_shouldRejectNonFailedState() throws Exception {
        when(assetManager.getByAssetId("img_1", "USER", "7")).thenReturn(asset("AVAILABLE", 1));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.retryDelete("img_1", "USER", "7"));
        assertEquals(ServiceExceptionEnum.IMAGE_ASSET_STATE_CONFLICT.getCode(), exception.getCode());
        verifyNoStorageCalls();
    }

    @Test
    void delete_shouldReturnConflictWhenCompareAndSetLosesRace() throws Exception {
        when(assetManager.getByAssetId("img_1", "USER", "7")).thenReturn(asset("AVAILABLE", 1));
        when(assetManager.markDeleting(eq("img_1"), eq(1), any())).thenReturn(false);
        when(assetManager.getByAssetId("img_1")).thenReturn(asset("AVAILABLE", 2));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.delete("img_1", "USER", "7"));
        assertEquals(ServiceExceptionEnum.IMAGE_ASSET_STATE_CONFLICT.getCode(), exception.getCode());
        verifyNoStorageCalls();
    }

    @Test
    void delete_shouldPersistSanitizedFailureAndMapProviderError() throws Exception {
        when(assetManager.getByAssetId("img_1", "USER", "7")).thenReturn(asset("AVAILABLE", 3));
        when(assetManager.markDeleting(eq("img_1"), eq(3), any())).thenReturn(true);
        when(assetManager.getByAssetId("img_1")).thenReturn(asset("DELETING", 4));
        when(storage.delete("images/a.jpg")).thenThrow(new java.io.IOException("/srv/secret\r\nkey"));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.delete("img_1", "USER", "7"));
        assertEquals(ServiceExceptionEnum.IMAGE_STORAGE_DELETE_FAILED.getCode(), exception.getCode());
        verify(assetManager).markDeleteFailed(eq("img_1"), eq(4), eq("STORAGE_DELETE_FAILED"),
            eq("provider deletion failed"), any());
    }

    private void verifyNoStorageCalls() throws Exception {
        verify(storage, never()).delete(any());
        verify(storage, never()).exists(any());
    }

    private static ImageAssetDTO asset(String status, int version) {
        ImageAssetDTO asset = new ImageAssetDTO();
        asset.setAssetId("img_1");
        asset.setStatus(status);
        asset.setVersion(version);
        asset.setStorageKey("images/a.jpg");
        return asset;
    }
}
