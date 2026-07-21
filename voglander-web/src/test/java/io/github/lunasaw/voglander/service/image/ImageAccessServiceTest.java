package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;

class ImageAccessServiceTest {
    @Test
    void moduleScopeIsGlobalForAuthorizedActorAndReservedForFutureAcl() {
        UserDTO actor = new UserDTO();
        actor.setId(7L);
        actor.setPermissions(Collections.singletonList(ImageConstant.PERMISSION_ASSET_QUERY));

        ImageAccessService.ImageAccessScope scope = new ImageAccessService().require(actor,
            ImageConstant.PERMISSION_ASSET_QUERY);

        assertTrue(scope.isGlobal());
    }

    @Test
    void missingPermissionIsRejectedBeforeResourceLookup() {
        UserDTO actor = new UserDTO();
        actor.setId(7L);
        actor.setPermissions(Collections.emptyList());

        ServiceException exception = assertThrows(ServiceException.class,
            () -> new ImageAccessService().require(actor, ImageConstant.PERMISSION_ASSET_VIEW));

        assertEquals(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED.getCode(), exception.getCode());
    }
}
