package io.github.lunasaw.voglander.service.image;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;

/**
 * Image visibility policy boundary.  Version 1.0.9 deliberately exposes a module-wide scope;
 * organization/device ACLs can be added here without changing repository query contracts.
 */
@Service
public class ImageAccessService {
    public ImageAccessScope require(UserDTO actor, String permission) {
        Objects.requireNonNull(permission, "permission");
        if (actor == null || actor.getId() == null || actor.getPermissions() == null
            || !actor.getPermissions().contains(permission)) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED);
        }
        return ImageAccessScope.global();
    }

    public static final class ImageAccessScope {
        private final Set<String> organizationIds;
        private ImageAccessScope(Set<String> organizationIds) {
            this.organizationIds = organizationIds;
        }
        public static ImageAccessScope global() {
            return new ImageAccessScope(Collections.emptySet());
        }
        public boolean isGlobal() {
            return organizationIds.isEmpty();
        }
    }
}
