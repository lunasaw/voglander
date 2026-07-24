package io.github.lunasaw.voglander.web.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.service.AuthService;

class AuthenticatedUserResolverTest {

    @Test
    void bearerAndSseTokenResolveTheStableAuthenticatedUserId() {
        AuthService authService = Mockito.mock(AuthService.class);
        UserDTO user = new UserDTO();
        user.setId(17L);
        when(authService.getUserByToken("opaque-token")).thenReturn(user);
        AuthenticatedUserResolver resolver = new AuthenticatedUserResolver(authService);

        assertEquals(17L, resolver.resolveBearer("Bearer opaque-token").getId());
        assertEquals(17L, resolver.resolveToken("opaque-token").getId());
    }

    @Test
    void invalidOrIdentityLessTokenIsRejectedWithoutUsingTokenAsIdentity() {
        AuthService authService = Mockito.mock(AuthService.class);
        UserDTO identityLess = new UserDTO();
        when(authService.getUserByToken("identity-less")).thenReturn(identityLess);
        AuthenticatedUserResolver resolver = new AuthenticatedUserResolver(authService);

        ServiceException missingBearer = assertThrows(ServiceException.class,
            () -> resolver.resolveBearer("opaque-token"));
        ServiceException missingIdentity = assertThrows(ServiceException.class,
            () -> resolver.resolveToken("identity-less"));

        assertEquals(ServiceExceptionEnum.LOGIN_REQUIRED.getCode(), missingBearer.getCode());
        assertEquals(ServiceExceptionEnum.LOGIN_REQUIRED.getCode(), missingIdentity.getCode());
    }
}
