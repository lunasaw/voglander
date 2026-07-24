package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ImageThumbnailMetricsWiringTest {

    @Test
    void generatedCacheHitAndNotModifiedPathsRecordActualServiceMetrics() throws Exception {
        ImageAssetReadService readService = mock(ImageAssetReadService.class);
        ImageThumbnailTransformer transformer = mock(ImageThumbnailTransformer.class);
        ImageAssetDTO asset = asset();
        when(readService.requireReadable("img-metrics")).thenReturn(asset);
        when(readService.open(asset)).thenReturn(new ImageContent(new ByteArrayInputStream(new byte[] {1}), 1));
        when(transformer.transform(any(), any(), anyLong(), anyLong(), anyLong()))
            .thenReturn(new byte[] {1, 2, 3});
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ImageThumbnailService service = new ImageThumbnailService(readService, transformer, new ImageProperties(),
            new ImageDomainMetrics(registry));

        try {
            ImageThumbnailResult generated = service.get("img-metrics", "table", null);
            service.get("img-metrics", "table", null);
            service.get("img-metrics", "table", generated.getEtag());
        } finally {
            service.close();
        }

        assertRequest(registry, "GENERATED", 1.0);
        assertRequest(registry, "CACHE_HIT", 1.0);
        assertRequest(registry, "NOT_MODIFIED", 1.0);
        assertEquals(1.0, registry.get("image_thumbnail_cache_hits_total").tag("profile", "table")
            .counter().count());
        assertEquals(2L, registry.get("image_thumbnail_output_bytes").tag("profile", "table")
            .summary().count());
    }

    private void assertRequest(SimpleMeterRegistry registry, String outcome, double expected) {
        assertEquals(expected, registry.get("image_thumbnail_requests_total")
            .tags("profile", "table", "outcome", outcome, "stable_error_code", "NONE")
            .counter().count());
    }

    private ImageAssetDTO asset() {
        ImageAssetDTO asset = new ImageAssetDTO();
        asset.setAssetId("img-metrics");
        asset.setStatus("AVAILABLE");
        asset.setChecksum("metrics-checksum");
        asset.setStorageKey("private/img-metrics");
        asset.setFileSize(1L);
        asset.setWidth(1);
        asset.setHeight(1);
        return asset;
    }
}
