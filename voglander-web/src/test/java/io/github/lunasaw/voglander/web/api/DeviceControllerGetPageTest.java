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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceQueryDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.web.api.device.DeviceController;
import io.github.lunasaw.voglander.web.api.device.assembler.DeviceWebAssembler;

/**
 * DeviceController#getPage 纯 Mockito 单元测试（S1）。
 *
 * <p>
 * 验证：请求转 DeviceQueryDTO 后调 Manager 分页；逐设备回填 channelCount；包装为 total + items。
 * </p>
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class DeviceControllerGetPageTest {

    @InjectMocks
    DeviceController     controller;
    @Mock
    DeviceManager        deviceManager;
    @Mock
    DeviceChannelManager deviceChannelManager;
    @Mock
    DeviceWebAssembler   deviceWebAssembler;

    MockMvc              mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getPage_returnsTotalAndItemsWithChannelCount() throws Exception {
        DeviceDTO dto = new DeviceDTO();
        dto.setId(1L);
        dto.setDeviceId("34020000001320000001");
        dto.setStatus(1);
        dto.setType(1);

        Page<DeviceDTO> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(dto));
        page.setTotal(1L);

        when(deviceWebAssembler.pageReqToQueryDto(any())).thenReturn(new DeviceQueryDTO());
        when(deviceManager.getPage(any(DeviceQueryDTO.class), anyInt(), anyInt())).thenReturn(page);
        when(deviceChannelManager.countByDeviceId("34020000001320000001")).thenReturn(3L);

        mvc.perform(post("/api/v1/device/getPage")
            .param("page", "1").param("size", "10")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].deviceId").value("34020000001320000001"))
            .andExpect(jsonPath("$.data.items[0].channelCount").value(3))
            .andExpect(jsonPath("$.data.items[0].statusName").value("在线"));

        verify(deviceChannelManager).countByDeviceId("34020000001320000001");
    }

    @Test
    void getPage_emptyResult_returnsZeroTotal() throws Exception {
        Page<DeviceDTO> page = new Page<>(1, 10);
        page.setRecords(java.util.Collections.emptyList());
        page.setTotal(0L);

        when(deviceWebAssembler.pageReqToQueryDto(any())).thenReturn(new DeviceQueryDTO());
        when(deviceManager.getPage(any(DeviceQueryDTO.class), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(post("/api/v1/device/getPage")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        verify(deviceChannelManager, never()).countByDeviceId(anyString());
    }
}
