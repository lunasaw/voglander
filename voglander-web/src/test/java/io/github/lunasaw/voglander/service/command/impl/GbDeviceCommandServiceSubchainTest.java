package io.github.lunasaw.voglander.service.command.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;

import io.github.lunasaw.voglander.client.domain.device.qo.DeviceAlarmQueryReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceConfigReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRecordQueryReq;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm.VoglanderServerAlarmCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.config.VoglanderServerConfigCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;

/**
 * GbDeviceCommandService GB 专属支链委托测试（S2，纯 Mockito）。
 *
 * <p>
 * 验证 GB 专属方法正确翻译参数并委托底层 6 个 command bean。
 * </p>
 *
 * @author luna
 */
@DisplayName("GbDeviceCommandService GB专属支链委托测试 (S2)")
@ExtendWith(MockitoExtension.class)
class GbDeviceCommandServiceSubchainTest {

    @InjectMocks
    GbDeviceCommandService        service;

    @Mock
    VoglanderServerDeviceCommand  deviceCommand;
    @Mock
    VoglanderServerPtzCommand     ptzCommand;
    @Mock
    VoglanderServerMediaCommand   mediaCommand;
    @Mock
    VoglanderServerConfigCommand  configCommand;
    @Mock
    VoglanderServerAlarmCommand   alarmCommand;
    @Mock
    VoglanderServerRecordCommand  recordCommand;
    @Mock
    io.github.lunasaw.voglander.manager.manager.MediaSessionManager mediaSessionManager;

    @Test
    @DisplayName("queryDeviceStatus 委托 deviceCommand.queryDeviceStatus")
    void queryDeviceStatus_delegates() {
        when(deviceCommand.queryDeviceStatus("d1")).thenReturn(ResultDTOUtils.success(null));
        ResultDTO<Void> r = service.queryDeviceStatus("d1");
        assertTrueResult(r);
        verify(deviceCommand).queryDeviceStatus("d1");
    }

    @Test
    @DisplayName("queryPreset 委托 deviceCommand.queryDevicePreset")
    void queryPreset_delegates() {
        when(deviceCommand.queryDevicePreset("d1")).thenReturn(ResultDTOUtils.success(null));
        service.queryPreset("d1");
        verify(deviceCommand).queryDevicePreset("d1");
    }

    @Test
    @DisplayName("queryMobilePosition 委托 deviceCommand.queryDeviceMobilePosition(id,interval)")
    void queryMobilePosition_delegates() {
        when(deviceCommand.queryDeviceMobilePosition("d1", "5")).thenReturn(ResultDTOUtils.success(null));
        service.queryMobilePosition("d1", "5");
        verify(deviceCommand).queryDeviceMobilePosition("d1", "5");
    }

    @Test
    @DisplayName("downloadConfig 委托 configCommand.downloadDeviceConfig")
    void downloadConfig_delegates() {
        when(configCommand.downloadDeviceConfig("d1", "BASIC")).thenReturn(ResultDTOUtils.success(null));
        service.downloadConfig("d1", "BASIC");
        verify(configCommand).downloadDeviceConfig("d1", "BASIC");
    }

    @Test
    @DisplayName("setDeviceConfig 委托 configCommand.configDevice(5参)")
    void setDeviceConfig_delegates() {
        when(configCommand.configDevice("d1", "cam", "3600", "60", "3")).thenReturn(ResultDTOUtils.success(null));
        DeviceConfigReq req = new DeviceConfigReq();
        req.setDeviceId("d1");
        req.setName("cam");
        req.setExpiration("3600");
        req.setHeartBeatInterval("60");
        req.setHeartBeatCount("3");
        service.setDeviceConfig(req);
        verify(configCommand).configDevice("d1", "cam", "3600", "60", "3");
    }

    @Test
    @DisplayName("controlRecord(true) 委托 startDeviceRecord")
    void controlRecord_start_delegates() {
        when(recordCommand.startDeviceRecord("d1")).thenReturn(ResultDTOUtils.success(null));
        service.controlRecord("d1", true);
        verify(recordCommand).startDeviceRecord("d1");
        verify(recordCommand, never()).stopDeviceRecord(anyString());
    }

    @Test
    @DisplayName("controlRecord(false) 委托 stopDeviceRecord")
    void controlRecord_stop_delegates() {
        when(recordCommand.stopDeviceRecord("d1")).thenReturn(ResultDTOUtils.success(null));
        service.controlRecord("d1", false);
        verify(recordCommand).stopDeviceRecord("d1");
        verify(recordCommand, never()).startDeviceRecord(anyString());
    }

    @Test
    @DisplayName("queryRecord 委托 recordCommand.queryDeviceRecord(long毫秒)")
    void queryRecord_delegates() {
        when(recordCommand.queryDeviceRecord(eq("d1"), anyLong(), anyLong())).thenReturn(ResultDTOUtils.success(null));
        DeviceRecordQueryReq req = new DeviceRecordQueryReq();
        req.setDeviceId("d1");
        req.setStartTime(1000L);
        req.setEndTime(2000L);
        service.queryRecord(req);
        verify(recordCommand).queryDeviceRecord("d1", 1000L, 2000L);
    }

    @Test
    @DisplayName("queryAlarm 委托 alarmCommand.queryDeviceAlarm")
    void queryAlarm_delegates() {
        when(alarmCommand.queryDeviceAlarm(eq("d1"), any(Date.class), any(Date.class), any(), any(), any(), any()))
            .thenReturn(ResultDTOUtils.success(null));
        DeviceAlarmQueryReq req = new DeviceAlarmQueryReq();
        req.setDeviceId("d1");
        req.setStartTime(1000L);
        req.setEndTime(2000L);
        req.setStartPriority("0");
        req.setEndPriority("9");
        req.setAlarmMethod("2");
        req.setAlarmType("1");
        service.queryAlarm(req);
        verify(alarmCommand).queryDeviceAlarm(eq("d1"), any(Date.class), any(Date.class), eq("0"), eq("9"), eq("2"), eq("1"));
    }

    @Test
    @DisplayName("controlAlarm 委托 alarmCommand.controlDeviceAlarm")
    void controlAlarm_delegates() {
        when(alarmCommand.controlDeviceAlarm("d1", "2", "1")).thenReturn(ResultDTOUtils.success(null));
        service.controlAlarm("d1", "2", "1");
        verify(alarmCommand).controlDeviceAlarm("d1", "2", "1");
    }

    @Test
    @DisplayName("broadcast 委托 mediaCommand.sendBroadcast")
    void broadcast_delegates() {
        when(mediaCommand.sendBroadcast("d1")).thenReturn(ResultDTOUtils.success(null));
        service.broadcast("d1");
        verify(mediaCommand).sendBroadcast("d1");
    }

    private void assertTrueResult(ResultDTO<Void> r) {
        org.junit.jupiter.api.Assertions.assertTrue(r.isSuccess());
    }
}
