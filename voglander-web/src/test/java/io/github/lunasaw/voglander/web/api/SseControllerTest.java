package io.github.lunasaw.voglander.web.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import io.github.lunasaw.voglander.service.sse.SseSubscriptionContext;
import io.github.lunasaw.voglander.service.task.BusinessTaskAuthorizationService;
import io.github.lunasaw.voglander.web.api.auth.AuthenticatedUserResolver;
import io.github.lunasaw.voglander.web.api.sse.controller.SseController;

@ExtendWith(MockitoExtension.class)
class SseControllerTest {

    SseController controller;
    @Mock SseEventBus          sseEventBus;
    @Mock AuthService          authService;

    MockMvc mvc;

    @BeforeEach void setup() {
        controller = new SseController(sseEventBus, new AuthenticatedUserResolver(authService),
            new BusinessTaskAuthorizationService());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void subscribe_noToken_throwsServiceException() {
        // authService.getUserByToken(null) returns null by default from Mockito — no stub needed
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
            () -> mvc.perform(get("/api/v1/stream/events")).andReturn());
    }

    @Test
    void subscribe_validToken_returnsSse() throws Exception {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setPermissions(java.util.Collections.emptyList());
        when(authService.getUserByToken("tok")).thenReturn(user);
        when(sseEventBus.register(any(SseSubscriptionContext.class))).thenReturn(new SseEmitter());
        mvc.perform(get("/api/v1/stream/events").param("token", "tok"))
            .andExpect(status().isOk());
    }

    @Test
    void subscribe_neverUsesOpaqueTokenAsEmitterIdentity() throws Exception {
        UserDTO user = new UserDTO();
        user.setId(42L);
        user.setPermissions(java.util.Collections.emptyList());
        when(authService.getUserByToken("secret-token-value")).thenReturn(user);
        when(sseEventBus.register(any(SseSubscriptionContext.class))).thenReturn(new SseEmitter());
        org.mockito.ArgumentCaptor<SseSubscriptionContext> context =
            org.mockito.ArgumentCaptor.forClass(SseSubscriptionContext.class);

        mvc.perform(get("/api/v1/stream/events")
                .param("token", "secret-token-value")
                .param("topics", "device,live"))
            .andExpect(status().isOk());

        verify(sseEventBus).register(context.capture());
        org.junit.jupiter.api.Assertions.assertEquals("42", context.getValue().getUserId());
        org.junit.jupiter.api.Assertions.assertFalse(
            context.getValue().getEmitterId().contains("secret-token-value"));
    }
}
