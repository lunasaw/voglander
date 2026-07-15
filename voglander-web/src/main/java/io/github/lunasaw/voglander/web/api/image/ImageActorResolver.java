package io.github.lunasaw.voglander.web.api.image;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.service.AuthService;

/** Resolves a user only from a validated Bearer token; request bodies cannot supply ownership. */
@Component
public class ImageActorResolver {
    private final AuthService authService;

    public ImageActorResolver(AuthService authService) { this.authService = authService; }

    public UserDTO resolve(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED);
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) throw new ServiceException(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED);
        UserDTO actor = authService.getUserByToken(token);
        if (actor == null || actor.getId() == null) throw new ServiceException(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED);
        return actor;
    }

    public void require(UserDTO actor, String permission) {
        if (actor == null || actor.getPermissions() == null || !actor.getPermissions().contains(permission)) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED);
        }
    }
}
