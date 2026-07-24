package io.github.lunasaw.voglander.web.api.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.enums.image.ThumbnailProfile;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.service.image.ImageAssetLifecycleService;
import io.github.lunasaw.voglander.service.image.ImageAssetReadService;
import io.github.lunasaw.voglander.service.image.ImageIngestService;
import io.github.lunasaw.voglander.service.image.ImageThumbnailResult;
import io.github.lunasaw.voglander.service.image.ImageThumbnailService;
import io.github.lunasaw.voglander.web.api.image.assembler.ImageAssetWebAssembler;

class ImageAssetThumbnailControllerTest {

    @Test
    void thumbnailReturnsPrivateBoundedJpegHeaders() {
        ImageThumbnailService thumbnails = mock(ImageThumbnailService.class);
        when(thumbnails.get("img_1", "table", null)).thenReturn(
            ImageThumbnailResult.content(new byte[] {1, 2, 3}, "\"sha256:derived\"", ThumbnailProfile.TABLE));

        ResponseEntity<byte[]> response = controller(thumbnails)
            .thumbnail("Bearer token", null, "img_1", "table");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(new byte[] {1, 2, 3}, response.getBody());
        assertEquals("image/jpeg", response.getHeaders().getContentType().toString());
        assertEquals(3, response.getHeaders().getContentLength());
        assertEquals("\"sha256:derived\"", response.getHeaders().getETag());
        assertEquals("Authorization", response.getHeaders().getFirst(HttpHeaders.VARY));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("inline", response.getHeaders().getContentDisposition().getType());
    }

    @Test
    void thumbnail304HasNoBodyAndPreservesPrivateValidationHeaders() {
        ImageThumbnailService thumbnails = mock(ImageThumbnailService.class);
        when(thumbnails.get("img_1", "gallery", "\"sha256:derived\"")).thenReturn(
            ImageThumbnailResult.notModified("\"sha256:derived\"", ThumbnailProfile.GALLERY));

        ResponseEntity<byte[]> response = controller(thumbnails)
            .thumbnail("Bearer token", "\"sha256:derived\"", "img_1", "gallery");

        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
        assertEquals(null, response.getBody());
        assertEquals("private, max-age=300", response.getHeaders().getCacheControl());
        assertEquals("Authorization", response.getHeaders().getFirst(HttpHeaders.VARY));
    }

    private ImageAssetController controller(ImageThumbnailService thumbnails) {
        AuthService auth = mock(AuthService.class);
        UserDTO user = new UserDTO();
        user.setId(7L);
        user.setPermissions(Collections.singletonList(ImageConstant.PERMISSION_ASSET_VIEW));
        when(auth.getUserByToken("token")).thenReturn(user);
        ImageAssetManager manager = mock(ImageAssetManager.class);
        ImageStorageService storage = mock(ImageStorageService.class);
        return new ImageAssetController(new ImageActorResolver(auth), manager, new ImageAssetWebAssembler(),
            mock(ImageIngestService.class), mock(ImageAssetLifecycleService.class), storage, new ImageProperties(),
            new ImageAssetReadService(manager, storage), thumbnails);
    }
}
