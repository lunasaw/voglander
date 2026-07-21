package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;

class ImageStorageReconciliationServiceTest {
    @Test
    void reconcile_shouldReportStagingUnregisteredAndMissingWithoutDeletingByDefault() throws Exception {
        ImageStorageService storage = org.mockito.Mockito.mock(ImageStorageService.class);
        ImageAssetManager assets = org.mockito.Mockito.mock(ImageAssetManager.class);
        ImageProperties properties = new ImageProperties();
        ImageAssetDTO registered = new ImageAssetDTO(); registered.setAssetId("img_1"); registered.setStorageKey("images/1.jpg");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ImageAssetDTO> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 1000, 1);
        page.setRecords(java.util.List.of(registered));
        when(assets.getPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(1000L)))
            .thenReturn(page);
        when(storage.sweepStaging(properties.getStorage().getStagingTtl())).thenReturn(2);
        when(storage.listFinalKeys()).thenReturn(Set.of("images/1.jpg", "images/orphan.jpg"));
        when(storage.exists("images/1.jpg")).thenReturn(false);

        ImageStorageReconciliationReport report = new ImageStorageReconciliationService(storage, assets, properties).reconcile();

        assertEquals(2, report.expiredStaging());
        assertEquals(1, report.unregisteredObjects());
        assertEquals(1, report.missingAssets());
        verify(storage, never()).delete("images/orphan.jpg");
    }
}
