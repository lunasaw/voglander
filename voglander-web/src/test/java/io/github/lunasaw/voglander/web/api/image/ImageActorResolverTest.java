package io.github.lunasaw.voglander.web.api.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.service.AuthService;

class ImageActorResolverTest {
    @Mock private AuthService authService;
    private ImageActorResolver resolver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolver = new ImageActorResolver(authService);
    }

    @Test
    void resolveRequiresBearerTokenAndAuthenticatedUser() {
        assertLoginRequired(null);
        assertLoginRequired("Basic token");
        assertLoginRequired("Bearer ");
        when(authService.getUserByToken("expired")).thenReturn(null);
        assertLoginRequired("Bearer expired");
    }

    @Test
    void resolveUsesOnlyTokenAndRequireChecksPermission() {
        UserDTO user = new UserDTO();
        user.setId(7L);
        user.setPermissions(Collections.singletonList(ImageConstant.PERMISSION_ASSET_VIEW));
        when(authService.getUserByToken("valid")).thenReturn(user);

        assertEquals(user, resolver.resolve("Bearer valid"));
        resolver.require(user, ImageConstant.PERMISSION_ASSET_VIEW);
        ServiceException denied = assertThrows(ServiceException.class,
            () -> resolver.require(user, ImageConstant.PERMISSION_ASSET_DELETE));
        assertEquals(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED.getCode(), denied.getCode());
        verify(authService).getUserByToken("valid");
    }

    @Test
    void resolveRejectsBodyOwnerSubstitutionByNotAcceptingAnythingExceptAuthorization() {
        UserDTO user = new UserDTO();
        user.setId(7L);
        when(authService.getUserByToken("valid")).thenReturn(user);

        assertEquals(7L, resolver.resolve("Bearer valid").getId());
        assertLoginRequired("Bearer valid ownerId=999");
    }

    private void assertLoginRequired(String header) {
        ServiceException denied = assertThrows(ServiceException.class, () -> resolver.resolve(header));
        assertEquals(ServiceExceptionEnum.LOGIN_REQUIRED.getCode(), denied.getCode());
    }
}
