package io.github.lunasaw.voglander.web.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.luna.common.dto.ResultDTOUtils;

import io.github.lunasaw.voglander.client.domain.device.qo.DeviceAlarmQueryReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceConfigReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRecordQueryReq;
import io.github.lunasaw.voglander.client.service.device.Gb28181DeviceCommandService;
import io.github.lunasaw.voglander.web.api.device.controller.DeviceCmdController;

/**
 * DeviceCmdController GB 专属支链端点测试（S2，纯 Mockito + MockMvc）。
 *
 * <p>
 * 验证：①4 个旧端点 Req 化后仍连通；②新增支链端点正确委托 Gb28181DeviceCommandService。
 * </p>
 *
 * @author luna
 */
@DisplayName("DeviceCmdController 支链端点测试 (S2)")
@ExtendWith(MockitoExtension.class)
class DeviceCmdControllerTest {

    @InjectMocks
    DeviceCmdController          controller;
    @Mock
    Gb28181DeviceCommandService  gbCommandService;

    MockMvc                      mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("query-catalog Req 化连通")
    void queryCatalog_ok() throws Exception {
        when(gbCommandService.queryCatalog("d1")).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/query-catalog")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
        verify(gbCommandService).queryCatalog("d1");
    }

    @Test
    @DisplayName("reboot Req 化连通")
    void reboot_ok() throws Exception {
        when(gbCommandService.reboot("d1")).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/reboot")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).reboot("d1");
    }

    @Test
    @DisplayName("query-status 委托 queryDeviceStatus")
    void queryStatus_ok() throws Exception {
        when(gbCommandService.queryDeviceStatus("d1")).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/query-status")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).queryDeviceStatus("d1");
    }

    @Test
    @DisplayName("query-preset 委托 queryPreset")
    void queryPreset_ok() throws Exception {
        when(gbCommandService.queryPreset("d1")).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/query-preset")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).queryPreset("d1");
    }

    @Test
    @DisplayName("query-mobile-position 委托 queryMobilePosition")
    void queryMobilePosition_ok() throws Exception {
        when(gbCommandService.queryMobilePosition("d1", "5")).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/query-mobile-position")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\",\"interval\":\"5\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).queryMobilePosition("d1", "5");
    }

    @Test
    @DisplayName("config/download 委托 downloadConfig")
    void configDownload_ok() throws Exception {
        when(gbCommandService.downloadConfig("d1", "BASIC")).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/config/download")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\",\"configType\":\"BASIC\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).downloadConfig("d1", "BASIC");
    }

    @Test
    @DisplayName("config/download 缺 configType 返回 400")
    void configDownload_missingType_400() throws Exception {
        mvc.perform(post("/api/v1/device-cmd/config/download")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("config/set 委托 setDeviceConfig")
    void configSet_ok() throws Exception {
        when(gbCommandService.setDeviceConfig(any(DeviceConfigReq.class))).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/config/set")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\",\"name\":\"cam\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).setDeviceConfig(any(DeviceConfigReq.class));
    }

    @Test
    @DisplayName("record/start 委托 controlRecord(true)")
    void recordStart_ok() throws Exception {
        when(gbCommandService.controlRecord("d1", true)).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/record/start")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\",\"start\":true}"))
            .andExpect(status().isOk());
        verify(gbCommandService).controlRecord("d1", true);
    }

    @Test
    @DisplayName("record/stop 委托 controlRecord(false)")
    void recordStop_ok() throws Exception {
        when(gbCommandService.controlRecord("d1", false)).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/record/stop")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).controlRecord("d1", false);
    }

    @Test
    @DisplayName("alarm/query 委托 queryAlarm")
    void alarmQuery_ok() throws Exception {
        when(gbCommandService.queryAlarm(any(DeviceAlarmQueryReq.class))).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/alarm/query")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).queryAlarm(any(DeviceAlarmQueryReq.class));
    }

    @Test
    @DisplayName("alarm/control 委托 controlAlarm")
    void alarmControl_ok() throws Exception {
        when(gbCommandService.controlAlarm("d1", "2", "1")).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/alarm/control")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deviceId\":\"d1\",\"alarmMethod\":\"2\",\"alarmType\":\"1\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).controlAlarm("d1", "2", "1");
    }

    @Test
    @DisplayName("broadcast 委托 broadcast")
    void broadcast_ok() throws Exception {
        when(gbCommandService.broadcast("d1")).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/broadcast")
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"d1\"}"))
            .andExpect(status().isOk());
        verify(gbCommandService).broadcast("d1");
    }

    @Test
    @DisplayName("record 触发录像查询委托 queryRecord")
    void record_ok() throws Exception {
        when(gbCommandService.queryRecord(any(DeviceRecordQueryReq.class))).thenReturn(ResultDTOUtils.success(null));
        mvc.perform(post("/api/v1/device-cmd/record")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deviceId\":\"d1\",\"startTime\":1000,\"endTime\":2000}"))
            .andExpect(status().isOk());
        verify(gbCommandService).queryRecord(any(DeviceRecordQueryReq.class));
    }
}
