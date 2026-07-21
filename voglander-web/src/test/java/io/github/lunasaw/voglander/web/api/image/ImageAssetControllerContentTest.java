package io.github.lunasaw.voglander.web.api.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.service.image.ImageAssetLifecycleService;
import io.github.lunasaw.voglander.service.image.ImageIngestService;
import io.github.lunasaw.voglander.service.task.BusinessTaskAuditService;
import io.github.lunasaw.voglander.web.api.image.assembler.ImageAssetWebAssembler;

class ImageAssetControllerContentTest {
    @Test
    void contentUsesChecksumEtagAndDoesNotOpenOn304() throws Exception {
        ImageActorResolver actorResolver = mock(ImageActorResolver.class); UserDTO actor = actor();
        when(actorResolver.resolve("Bearer token")).thenReturn(actor);
        ImageAssetManager assets = mock(ImageAssetManager.class); ImageAssetDTO asset = asset();
        when(assets.getByAssetId("img_1")).thenReturn(asset);
        ImageStorageService storage = mock(ImageStorageService.class);
        ImageAssetController controller = controller(actorResolver, assets, storage);

        ResponseEntity<StreamingResponseBody> result = controller.content("Bearer token", "\"sha256:abc\"", "img_1");

        assertEquals(304, result.getStatusCode().value());
        org.mockito.Mockito.verify(storage, org.mockito.Mockito.never()).open("images/x.jpg");
    }

    @Test
    void downloadRequiresDownloadPermissionAndUsesSafeAttachmentName() throws Exception {
        ImageActorResolver actorResolver = mock(ImageActorResolver.class); UserDTO actor = actor();
        when(actorResolver.resolve("Bearer token")).thenReturn(actor);
        ImageAssetManager assets = mock(ImageAssetManager.class); when(assets.getByAssetId("img_1")).thenReturn(asset());
        ImageAssetSourceDTO source = new ImageAssetSourceDTO(); source.setOriginalFilename("../safe\r\n.jpg");
        when(assets.getSourceByAssetId("img_1")).thenReturn(source);
        ImageStorageService storage = mock(ImageStorageService.class);
        when(storage.open("images/x.jpg")).thenReturn(new ImageContent(new ByteArrayInputStream(new byte[] {1, 2}), 2));
        ImageAssetController controller = controller(actorResolver, assets, storage);

        ResponseEntity<StreamingResponseBody> result = controller.download("Bearer token", "img_1");

        assertTrue(result.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("safe.jpg"));
        assertEquals("\"sha256:abc\"", result.getHeaders().getETag());
    }

    @Test
    void deleteAndRetryWriteStructuredAuditWithoutStoragePath() {
        ImageActorResolver actorResolver = mock(ImageActorResolver.class); UserDTO actor = actor();
        when(actorResolver.resolve("Bearer token")).thenReturn(actor);
        ImageAssetManager assets = mock(ImageAssetManager.class);
        ImageAssetLifecycleService lifecycle = mock(ImageAssetLifecycleService.class);
        when(lifecycle.delete("img_1", null, null)).thenReturn(true);
        when(lifecycle.retryDelete("img_1", null, null)).thenReturn(true);
        ImageAssetController controller = controller(actorResolver, assets, mock(ImageStorageService.class));
        ReflectionTestUtils.setField(controller, "lifecycleService", lifecycle);
        BusinessTaskAuditService audit = new BusinessTaskAuditService();
        ReflectionTestUtils.setField(controller, "auditService", audit);

        controller.delete("Bearer token", "img_1", null);
        controller.retryDelete("Bearer token", "img_1");

        assertEquals(2, audit.recentRecords().size());
        assertEquals("img_1", audit.recentRecords().get(0).getTaskId());
        assertEquals("ASSET_DELETE", audit.recentRecords().get(0).getCommand());
        assertTrue(!audit.recentRecords().get(0).toStructuredData().containsKey("storageKey"));
    }

    private static ImageAssetController controller(ImageActorResolver resolver, ImageAssetManager assets, ImageStorageService storage) {
        ImageAssetController controller = new ImageAssetController(resolver, assets, new ImageAssetWebAssembler(),
            mock(ImageIngestService.class), mock(ImageAssetLifecycleService.class), storage, new ImageProperties());
        org.mockito.Mockito.doNothing().when(resolver).require(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
        return controller;
    }

    private static UserDTO actor() { UserDTO user = new UserDTO(); user.setId(7L); user.setPermissions(Arrays.asList(ImageConstant.PERMISSION_ASSET_VIEW, ImageConstant.PERMISSION_ASSET_DOWNLOAD)); return user; }
    private static ImageAssetDTO asset() { ImageAssetDTO value = new ImageAssetDTO(); value.setAssetId("img_1"); value.setStatus("AVAILABLE"); value.setChecksum("abc"); value.setStorageKey("images/x.jpg"); value.setContentType("image/jpeg"); value.setImageFormat("JPEG"); value.setAssetName("image.jpg"); value.setCapturedAt(LocalDateTime.now()); return value; }
}
