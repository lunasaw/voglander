package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;

class BusinessTaskAuthorizationServiceTest {

    private final BusinessTaskAuthorizationService service = new BusinessTaskAuthorizationService();

    @Test
    void imageCollectionQueryPermissionOnlyAllowsImageCollectionTasks() {
        BizTaskAccessScopeDTO scope = service.queryScope(
            actor(ImageConstant.PERMISSION_COLLECTION_QUERY), null);

        assertEquals(Collections.singleton(ImageConstant.TASK_TYPE_IMAGE_COLLECTION), scope.getAllowedTaskTypes());
    }

    @Test
    void taskQueryAllowsAllTaskTypesUnlessRequestNarrowsTheScope() {
        BizTaskAccessScopeDTO global = service.queryScope(actor(TaskConstant.PERMISSION_QUERY), null);
        BizTaskAccessScopeDTO narrowed = service.queryScope(actor(TaskConstant.PERMISSION_QUERY), "DATA_EXPORT");

        assertNull(global.getAllowedTaskTypes());
        assertEquals(Collections.singleton("DATA_EXPORT"), narrowed.getAllowedTaskTypes());
    }

    @Test
    void imageCollectionQueryCannotExpandToAnotherTaskType() {
        ServiceException error = assertThrows(ServiceException.class,
            () -> service.queryScope(actor(ImageConstant.PERMISSION_COLLECTION_QUERY), "DATA_EXPORT"));

        assertEquals(ServiceExceptionEnum.TASK_PERMISSION_DENIED.getCode(), error.getCode());
    }

    @Test
    void imageCollectionControlRequiresBothPermissions() {
        assertThrows(ServiceException.class, () -> service.requireControl(
            actor(TaskConstant.PERMISSION_CONTROL), ImageConstant.TASK_TYPE_IMAGE_COLLECTION));
        assertThrows(ServiceException.class, () -> service.requireControl(
            actor(ImageConstant.PERMISSION_COLLECTION_CONTROL), ImageConstant.TASK_TYPE_IMAGE_COLLECTION));

        service.requireControl(actor(TaskConstant.PERMISSION_CONTROL,
            ImageConstant.PERMISSION_COLLECTION_CONTROL), ImageConstant.TASK_TYPE_IMAGE_COLLECTION);
    }

    private UserDTO actor(String... permissions) {
        UserDTO actor = new UserDTO();
        actor.setId(7L);
        actor.setPermissions(Arrays.asList(permissions));
        return actor;
    }
}
