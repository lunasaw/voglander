package io.github.lunasaw.voglander.web.api.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.service.image.ImageAssetLifecycleService;
import io.github.lunasaw.voglander.service.image.ImageIngestService;
import io.github.lunasaw.voglander.web.api.image.assembler.ImageAssetWebAssembler;

class ImageControllerPermissionTest {
    @Test
    void missingImagePermissionIsForbiddenBeforeManagerAccess() {
        AuthService auth = mock(AuthService.class);
        UserDTO actor = new UserDTO(); actor.setId(7L); actor.setPermissions(Collections.emptyList());
        when(auth.getUserByToken("token")).thenReturn(actor);
        ImageActorResolver resolver = new ImageActorResolver(auth);
        ImageAssetManager manager = mock(ImageAssetManager.class);
        ImageAssetController controller = new ImageAssetController(resolver, manager, new ImageAssetWebAssembler(),
            mock(ImageIngestService.class), mock(ImageAssetLifecycleService.class), mock(io.github.lunasaw.voglander.client.service.image.ImageStorageService.class), new ImageProperties());

        ServiceException failure = assertThrows(ServiceException.class, () -> controller.statistics("Bearer token"));
        assertEquals(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED.getCode(), failure.getCode());
        org.mockito.Mockito.verifyNoInteractions(manager);
    }

    @Test
    void authorizedQueryStillReturnsNotFoundForMissingAsset() {
        AuthService auth = mock(AuthService.class);
        UserDTO actor = new UserDTO(); actor.setId(7L); actor.setPermissions(java.util.Arrays.asList(
            ImageConstant.PERMISSION_ASSET_QUERY, ImageConstant.PERMISSION_ASSET_VIEW));
        when(auth.getUserByToken("token")).thenReturn(actor);
        ImageAssetManager manager = mock(ImageAssetManager.class);
        when(manager.getByAssetId("missing")).thenReturn(null);
        ImageAssetController controller = new ImageAssetController(new ImageActorResolver(auth), manager,
            new ImageAssetWebAssembler(), mock(ImageIngestService.class), mock(ImageAssetLifecycleService.class),
            mock(io.github.lunasaw.voglander.client.service.image.ImageStorageService.class), new ImageProperties());

        ServiceException failure = assertThrows(ServiceException.class,
            () -> controller.detail("Bearer token", "missing"));
        assertEquals(ServiceExceptionEnum.IMAGE_ASSET_NOT_FOUND.getCode(), failure.getCode());
    }

    @Test
    void assetDetailRequiresViewBeforeLookingUpTheAsset() {
        AuthService auth = mock(AuthService.class);
        UserDTO actor = new UserDTO(); actor.setId(7L);
        actor.setPermissions(Collections.singletonList(ImageConstant.PERMISSION_ASSET_QUERY));
        when(auth.getUserByToken("token")).thenReturn(actor);
        ImageAssetManager manager = mock(ImageAssetManager.class);
        ImageAssetController controller = new ImageAssetController(new ImageActorResolver(auth), manager,
            new ImageAssetWebAssembler(), mock(ImageIngestService.class), mock(ImageAssetLifecycleService.class),
            mock(io.github.lunasaw.voglander.client.service.image.ImageStorageService.class), new ImageProperties());

        ServiceException failure = assertThrows(ServiceException.class,
            () -> controller.detail("Bearer token", "exists-or-not"));

        assertEquals(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED.getCode(), failure.getCode());
        org.mockito.Mockito.verifyNoInteractions(manager);
    }
}
