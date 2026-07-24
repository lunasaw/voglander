package io.github.lunasaw.voglander.web.api.auth;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.service.AuthService;

/** Resolves stable user facts from validated bearer or SSE tokens. */
@Component
public class AuthenticatedUserResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthenticatedUserResolver(AuthService authService) {
        this.authService = authService;
    }

    public UserDTO resolveBearer(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw loginRequired();
        }
        return resolveToken(authorization.substring(BEARER_PREFIX.length()).trim());
    }

    public UserDTO resolveToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw loginRequired();
        }
        UserDTO user = authService.getUserByToken(token);
        if (user == null || user.getId() == null) {
            throw loginRequired();
        }
        return user;
    }

    private ServiceException loginRequired() {
        return new ServiceException(ServiceExceptionEnum.LOGIN_REQUIRED);
    }
}
