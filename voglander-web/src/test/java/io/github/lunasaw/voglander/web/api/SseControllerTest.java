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
import io.github.lunasaw.voglander.web.api.sse.controller.SseController;

@ExtendWith(MockitoExtension.class)
class SseControllerTest {

    @InjectMocks SseController controller;
    @Mock SseEventBus          sseEventBus;
    @Mock AuthService          authService;

    MockMvc mvc;

    @BeforeEach void setup() {
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
        when(authService.getUserByToken("tok")).thenReturn(user);
        when(sseEventBus.register(any(), any())).thenReturn(new SseEmitter());
        mvc.perform(get("/api/v1/stream/events").param("token", "tok"))
            .andExpect(status().isOk());
    }
}
