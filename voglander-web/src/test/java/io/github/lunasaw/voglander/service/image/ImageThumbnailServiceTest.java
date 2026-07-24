package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.common.enums.image.ThumbnailProfile;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;

class ImageThumbnailServiceTest {

    private ImageThumbnailService service;

    @AfterEach
    void closeExecutor() {
        if (service != null) service.close();
    }

    @Test
    void stableEtagSupports304BeforeStorageOpenOrTransform() throws Exception {
        ImageAssetReadService readService = mock(ImageAssetReadService.class);
        ImageThumbnailTransformer transformer = mock(ImageThumbnailTransformer.class);
        ImageAssetDTO asset = availableAsset();
        when(readService.requireReadable("img_1")).thenReturn(asset);
        ImageProperties properties = properties();
        service = new ImageThumbnailService(readService, transformer, properties);
        String etag = ImageThumbnailService.deriveEtag(asset.getChecksum(), ThumbnailProfile.TABLE,
            properties.getThumbnail().getAlgorithmVersion());

        ImageThumbnailResult result = service.get("img_1", "table", etag);

        assertTrue(result.isNotModified());
        assertEquals(etag, result.getEtag());
        verify(readService, never()).open(any());
        verify(transformer, never()).transform(any(), any(), any(Long.class), any(Long.class), any(Long.class));
    }

    @Test
    void transformResultIsCachedByDerivedEtag() throws Exception {
        ImageAssetReadService readService = mock(ImageAssetReadService.class);
        ImageThumbnailTransformer transformer = mock(ImageThumbnailTransformer.class);
        ImageAssetDTO asset = availableAsset();
        when(readService.requireReadable("img_1")).thenReturn(asset);
        when(readService.open(asset)).thenReturn(
            new ImageContent(new ByteArrayInputStream(new byte[] {1}), 1));
        when(transformer.transform(any(), any(), any(Long.class), any(Long.class), any(Long.class)))
            .thenReturn(new byte[] {9, 8, 7});
        service = new ImageThumbnailService(readService, transformer, properties());

        ImageThumbnailResult first = service.get("img_1", "table", null);
        ImageThumbnailResult replay = service.get("img_1", "table", null);

        assertEquals(3, first.getContent().length);
        assertEquals(first.getEtag(), replay.getEtag());
        verify(readService, org.mockito.Mockito.times(1)).open(asset);
        verify(transformer, org.mockito.Mockito.times(1))
            .transform(any(), any(), any(Long.class), any(Long.class), any(Long.class));
    }

    @Test
    void disabledDerivationReturnsExplicitUnavailableError() {
        ImageAssetReadService readService = mock(ImageAssetReadService.class);
        when(readService.requireReadable("img_1")).thenReturn(availableAsset());
        ImageProperties properties = properties();
        properties.getThumbnail().setEnabled(false);
        service = new ImageThumbnailService(readService, mock(ImageThumbnailTransformer.class), properties);

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.get("img_1", "gallery", null));
        assertEquals(ServiceExceptionEnum.IMAGE_THUMBNAIL_UNAVAILABLE.getCode(), error.getCode());
    }

    private ImageProperties properties() {
        ImageProperties properties = new ImageProperties();
        properties.getThumbnail().setWorkerCount(1);
        properties.getThumbnail().setQueueCapacity(1);
        properties.getThumbnail().setTimeoutMillis(1000);
        return properties;
    }

    private ImageAssetDTO availableAsset() {
        ImageAssetDTO asset = new ImageAssetDTO();
        asset.setAssetId("img_1");
        asset.setStatus("AVAILABLE");
        asset.setChecksum("asset-checksum");
        asset.setStorageKey("private/key");
        return asset;
    }
}
