package io.github.lunasaw.voglander.web.api.image;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.service.image.ImageCollectionApplicationService;
import io.github.lunasaw.voglander.service.task.BusinessTaskAuthorizationService;
import io.github.lunasaw.voglander.web.api.image.assembler.ImageCollectionWebAssembler;
import io.github.lunasaw.voglander.web.api.image.req.ImageCollectionRescheduleReq;

class ImageCollectionControllerContractTest {
    @Test
    void reschedule_requiresCollectionControlPermissionBeforeDomainCall() {
        AuthService auth = mock(AuthService.class);
        UserDTO actor = new UserDTO(); actor.setId(7L); actor.setPermissions(Collections.emptyList());
        when(auth.getUserByToken("token")).thenReturn(actor);
        ImageCollectionApplicationService application = mock(ImageCollectionApplicationService.class);
        ImageCollectionController controller = new ImageCollectionController(new ImageActorResolver(auth), application,
            new ImageCollectionWebAssembler(), new ImageProperties(), new BusinessTaskAuthorizationService());

        assertThrows(ServiceException.class, () -> controller.reschedule("Bearer token", "btask_1",
            new ImageCollectionRescheduleReq()));
        verifyNoInteractions(application);
    }

    @Test
    void reschedule_requiresTaskControlInAdditionToCollectionControl() {
        AuthService auth = mock(AuthService.class);
        UserDTO actor = new UserDTO();
        actor.setId(7L);
        actor.setPermissions(Collections.singletonList(ImageConstant.PERMISSION_COLLECTION_CONTROL));
        when(auth.getUserByToken("token")).thenReturn(actor);
        ImageCollectionApplicationService application = mock(ImageCollectionApplicationService.class);
        ImageCollectionController controller = new ImageCollectionController(new ImageActorResolver(auth), application,
            new ImageCollectionWebAssembler(), new ImageProperties(), new BusinessTaskAuthorizationService());

        assertThrows(ServiceException.class, () -> controller.reschedule("Bearer token", "btask_1",
            new ImageCollectionRescheduleReq()));
        verifyNoInteractions(application);
    }

    @Test
    void reschedule_rejectsMissingExpectedVersionBeforeDomainCall() {
        AuthService auth = mock(AuthService.class);
        UserDTO actor = new UserDTO();
        actor.setId(7L);
        actor.setPermissions(java.util.Arrays.asList(TaskConstant.PERMISSION_CONTROL,
            ImageConstant.PERMISSION_COLLECTION_CONTROL));
        when(auth.getUserByToken("token")).thenReturn(actor);
        ImageCollectionApplicationService application = mock(ImageCollectionApplicationService.class);
        ImageCollectionController controller = new ImageCollectionController(new ImageActorResolver(auth), application,
            new ImageCollectionWebAssembler(), new ImageProperties(), new BusinessTaskAuthorizationService());

        ServiceException error = assertThrows(ServiceException.class,
            () -> controller.reschedule("Bearer token", "btask_1", new ImageCollectionRescheduleReq()));

        assertEquals(ServiceExceptionEnum.PARAM_ERROR.getCode(), error.getCode());
        verifyNoInteractions(application);
    }

    @Test
    void constraints_requiresQueryPermission() {
        AuthService auth = mock(AuthService.class);
        UserDTO actor = new UserDTO(); actor.setId(7L); actor.setPermissions(Collections.singletonList(ImageConstant.PERMISSION_COLLECTION_QUERY));
        when(auth.getUserByToken("token")).thenReturn(actor);
        ImageCollectionController controller = new ImageCollectionController(new ImageActorResolver(auth),
            mock(ImageCollectionApplicationService.class), new ImageCollectionWebAssembler(), new ImageProperties(),
            new BusinessTaskAuthorizationService());
        org.junit.jupiter.api.Assertions.assertNotNull(controller.constraints("Bearer token").getData());
    }
}
