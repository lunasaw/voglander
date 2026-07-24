package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;

class ImageAssetReadServiceTest {

    @Test
    void binaryReadClassifiesEveryPersistedAssetState() {
        ImageAssetManager manager = mock(ImageAssetManager.class);
        ImageAssetReadService service = new ImageAssetReadService(manager, mock(ImageStorageService.class));
        assertCode(ServiceExceptionEnum.IMAGE_ASSET_NOT_FOUND,
            () -> service.requireReadable("missing"));
        for (String state : new String[] {"DELETING", "DELETE_FAILED"}) {
            when(manager.getByAssetId(state)).thenReturn(asset(state));
            assertCode(ServiceExceptionEnum.IMAGE_ASSET_STATE_CONFLICT,
                () -> service.requireReadable(state));
        }
        when(manager.getByAssetId("DELETED")).thenReturn(asset("DELETED"));
        assertCode(ServiceExceptionEnum.IMAGE_ASSET_GONE,
            () -> service.requireReadable("DELETED"));
        when(manager.getByAssetId("AVAILABLE")).thenReturn(asset("AVAILABLE"));
        assertEquals("AVAILABLE", service.requireReadable("AVAILABLE").getStatus());
    }

    @Test
    void providerReadFailureMapsToStableServiceUnavailableCode() throws Exception {
        ImageStorageService storage = mock(ImageStorageService.class);
        when(storage.open("private/key")).thenThrow(new IOException("provider detail must not escape"));
        ImageAssetReadService service = new ImageAssetReadService(mock(ImageAssetManager.class), storage);
        ImageAssetDTO asset = asset("AVAILABLE");
        asset.setStorageKey("private/key");

        assertCode(ServiceExceptionEnum.IMAGE_STORAGE_READ_FAILED, () -> service.open(asset));
    }

    private ImageAssetDTO asset(String state) {
        ImageAssetDTO asset = new ImageAssetDTO();
        asset.setAssetId("img_1");
        asset.setStatus(state);
        return asset;
    }

    private void assertCode(ServiceExceptionEnum expected, org.junit.jupiter.api.function.Executable executable) {
        ServiceException error = assertThrows(ServiceException.class, executable);
        assertEquals(expected.getCode(), error.getCode());
    }
}
