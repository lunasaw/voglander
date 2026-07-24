package io.github.lunasaw.voglander.web.api.image;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.web.api.auth.AuthenticatedUserResolver;

/** Resolves a user only from a validated Bearer token; request bodies cannot supply ownership. */
@Component
public class ImageActorResolver {
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Autowired
    public ImageActorResolver(AuthenticatedUserResolver authenticatedUserResolver) {
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    /** Compatibility constructor for focused tests outside a Spring application context. */
    public ImageActorResolver(AuthService authService) {
        this(new AuthenticatedUserResolver(authService));
    }

    public UserDTO resolve(String authorization) {
        return authenticatedUserResolver.resolveBearer(authorization);
    }

    public void require(UserDTO actor, String permission) {
        if (actor == null || actor.getPermissions() == null || !actor.getPermissions().contains(permission)) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED);
        }
    }
}
