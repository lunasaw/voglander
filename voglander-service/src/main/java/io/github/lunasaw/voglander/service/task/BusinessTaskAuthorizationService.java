package io.github.lunasaw.voglander.service.task;

import java.util.Collections;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;

/** Creates trusted task scopes and enforces task-type-specific control permissions. */
@Service
public class BusinessTaskAuthorizationService {

    public BizTaskAccessScopeDTO queryScope(UserDTO actor, String requestedTaskType) {
        if (hasPermission(actor, TaskConstant.PERMISSION_QUERY)) {
            return globalScope(StringUtils.hasText(requestedTaskType)
                ? Collections.singleton(requestedTaskType) : null);
        }
        if (hasPermission(actor, ImageConstant.PERMISSION_COLLECTION_QUERY)
            && (!StringUtils.hasText(requestedTaskType)
                || ImageConstant.TASK_TYPE_IMAGE_COLLECTION.equals(requestedTaskType))) {
            return globalScope(Collections.singleton(ImageConstant.TASK_TYPE_IMAGE_COLLECTION));
        }
        throw denied();
    }

    public BizTaskAccessScopeDTO imageCollectionQueryScope(UserDTO actor) {
        if (!hasPermission(actor, ImageConstant.PERMISSION_COLLECTION_QUERY)) throw denied();
        return globalScope(Collections.singleton(ImageConstant.TASK_TYPE_IMAGE_COLLECTION));
    }

    public void requireControl(UserDTO actor, String taskType) {
        if (!hasPermission(actor, TaskConstant.PERMISSION_CONTROL)) throw denied();
        if (ImageConstant.TASK_TYPE_IMAGE_COLLECTION.equals(taskType)
            && !hasPermission(actor, ImageConstant.PERMISSION_COLLECTION_CONTROL)) throw denied();
    }

    public BizTaskAccessScopeDTO controlLookupScope(UserDTO actor) {
        if (!hasPermission(actor, TaskConstant.PERMISSION_CONTROL)) throw denied();
        if (hasPermission(actor, ImageConstant.PERMISSION_COLLECTION_CONTROL)) return BizTaskAccessScopeDTO.global();
        Set<String> nonImageTypes = new java.util.LinkedHashSet<>();
        for (io.github.lunasaw.voglander.common.enums.task.BusinessTaskTypeEnum value
            : io.github.lunasaw.voglander.common.enums.task.BusinessTaskTypeEnum.values()) {
            if (!ImageConstant.TASK_TYPE_IMAGE_COLLECTION.equals(value.name())) nonImageTypes.add(value.name());
        }
        return globalScope(Collections.unmodifiableSet(nonImageTypes));
    }

    /** A partial controller can operate non-image tasks but must not distinguish hidden image IDs from missing IDs. */
    public boolean hasTaskControlWithoutImageControl(UserDTO actor) {
        return hasPermission(actor, TaskConstant.PERMISSION_CONTROL)
            && !hasPermission(actor, ImageConstant.PERMISSION_COLLECTION_CONTROL);
    }

    public boolean hasPermission(UserDTO actor, String permission) {
        return actor != null && actor.getId() != null && actor.getPermissions() != null
            && actor.getPermissions().contains(permission);
    }

    private BizTaskAccessScopeDTO globalScope(Set<String> allowedTaskTypes) {
        BizTaskAccessScopeDTO scope = BizTaskAccessScopeDTO.global();
        scope.setAllowedTaskTypes(allowedTaskTypes);
        return scope;
    }

    private ServiceException denied() {
        return new ServiceException(ServiceExceptionEnum.TASK_PERMISSION_DENIED);
    }
}
