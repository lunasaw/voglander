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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.github.lunasaw.voglander.service.live.MediaPlayService;
import io.github.lunasaw.voglander.service.live.dto.LivePlayDTO;
import io.github.lunasaw.voglander.web.api.live.assembler.LiveWebAssembler;
import io.github.lunasaw.voglander.web.api.live.controller.LivePlayController;

@ExtendWith(MockitoExtension.class)
class LivePlayControllerTest {

    @InjectMocks LivePlayController controller;
    @Mock MediaPlayService          mediaPlayService;
    @Mock LiveWebAssembler          liveWebAssembler;

    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void start_validReq_returns200() throws Exception {
        when(mediaPlayService.startLive(any())).thenReturn(new LivePlayDTO());
        when(liveWebAssembler.startReqToDto(any())).thenCallRealMethod();
        when(liveWebAssembler.dtoToVo(any())).thenCallRealMethod();

        mvc.perform(post("/api/v1/live/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deviceId\":\"dev1\",\"channelId\":\"ch1\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void start_missingDeviceId_returns400() throws Exception {
        mvc.perform(post("/api/v1/live/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channelId\":\"ch1\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void stop_validReq_returns200() throws Exception {
        when(mediaPlayService.stopLive(any())).thenReturn(true);
        mvc.perform(post("/api/v1/live/stop")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"streamId\":\"gb_live_dev1_ch1\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void get_validStreamId_returns200() throws Exception {
        when(mediaPlayService.getLive("s1")).thenReturn(new LivePlayDTO());
        when(liveWebAssembler.dtoToVo(any())).thenCallRealMethod();
        mvc.perform(get("/api/v1/live/s1")).andExpect(status().isOk());
    }
}
