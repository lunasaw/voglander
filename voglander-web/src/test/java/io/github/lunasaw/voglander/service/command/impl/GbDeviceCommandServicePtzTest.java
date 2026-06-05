package io.github.lunasaw.voglander.service.command.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luna.common.dto.ResultDTOUtils;

import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePtzReq;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm.VoglanderServerAlarmCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.config.VoglanderServerConfigCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D6 红线：前端 PTZ 词表（UP/DOWN/LEFT/...）必须经门面层翻译为 {@link PTZControlEnum} 规范枚举名后下发，
 * 而非直接 {@code valueOf(req.getControl())}（前端词表与枚举常量名不一致，旧实现全抛 {@code No enum constant}）。
 *
 * @author luna
 */
@DisplayName("D6 — PTZ 指令词表门面翻译")
@ExtendWith(MockitoExtension.class)
class GbDeviceCommandServicePtzTest {

    @Mock
    private VoglanderServerDeviceCommand deviceCommand;
    @Mock
    private VoglanderServerPtzCommand    ptzCommand;
    @Mock
    private VoglanderServerMediaCommand  mediaCommand;
    @Mock
    private VoglanderServerConfigCommand configCommand;
    @Mock
    private VoglanderServerAlarmCommand  alarmCommand;
    @Mock
    private VoglanderServerRecordCommand recordCommand;

    @InjectMocks
    private GbDeviceCommandService       service;

    @ParameterizedTest(name = "前端词 {0} → 枚举 {1}")
    @CsvSource({
        "UP,         TILT_UP",
        "DOWN,       TILT_DOWN",
        "LEFT,       PAN_LEFT",
        "RIGHT,      PAN_RIGHT",
        "UP_LEFT,    PAN_LEFT_TILT_UP",
        "UP_RIGHT,   PAN_RIGHT_TILT_UP",
        "DOWN_LEFT,  PAN_LEFT_TILT_DOWN",
        "DOWN_RIGHT, PAN_RIGHT_TILT_DOWN",
        "ZOOM_IN,    ZOOM_IN",
        "ZOOM_OUT,   ZOOM_OUT",
        "STOP,       STOP"
    })
    @DisplayName("前端词表逐一翻译为正确规范枚举")
    void ptzControl_translatesFrontendVocab(String vocab, PTZControlEnum expected) {
        when(ptzCommand.controlDevicePtz(eq("dev1"), any(PTZControlEnum.class), anyInt()))
            .thenReturn(ResultDTOUtils.success());

        DevicePtzReq req = new DevicePtzReq();
        req.setDeviceId("dev1");
        req.setControl(vocab);
        req.setSpeed(128);

        service.ptzControl(req);

        ArgumentCaptor<PTZControlEnum> captor = ArgumentCaptor.forClass(PTZControlEnum.class);
        verify(ptzCommand).controlDevicePtz(eq("dev1"), captor.capture(), eq(128));
        assertEquals(expected, captor.getValue(), "前端词 " + vocab + " 应翻译为 " + expected);
    }

    @Test
    @DisplayName("直接传规范枚举名（TILT_UP）应兼容透传")
    void ptzControl_canonicalEnumName_passthrough() {
        when(ptzCommand.controlDevicePtz(eq("dev1"), any(PTZControlEnum.class), anyInt()))
            .thenReturn(ResultDTOUtils.success());

        DevicePtzReq req = new DevicePtzReq();
        req.setDeviceId("dev1");
        req.setControl("TILT_UP");
        req.setSpeed(64);

        service.ptzControl(req);

        ArgumentCaptor<PTZControlEnum> captor = ArgumentCaptor.forClass(PTZControlEnum.class);
        verify(ptzCommand).controlDevicePtz(eq("dev1"), captor.capture(), eq(64));
        assertEquals(PTZControlEnum.TILT_UP, captor.getValue());
    }

    @Test
    @DisplayName("小写/带空格输入应大小写无关")
    void ptzControl_caseInsensitive() {
        when(ptzCommand.controlDevicePtz(eq("dev1"), any(PTZControlEnum.class), anyInt()))
            .thenReturn(ResultDTOUtils.success());

        DevicePtzReq req = new DevicePtzReq();
        req.setDeviceId("dev1");
        req.setControl("  up  ");
        req.setSpeed(128);

        service.ptzControl(req);

        ArgumentCaptor<PTZControlEnum> captor = ArgumentCaptor.forClass(PTZControlEnum.class);
        verify(ptzCommand).controlDevicePtz(eq("dev1"), captor.capture(), eq(128));
        assertEquals(PTZControlEnum.TILT_UP, captor.getValue());
    }

    @Test
    @DisplayName("未知 PTZ 指令应抛 ServiceException（非裸 IllegalArgumentException→500）")
    void ptzControl_unknownCommand_throwsServiceException() {
        DevicePtzReq req = new DevicePtzReq();
        req.setDeviceId("dev1");
        req.setControl("FLY_TO_MOON");
        req.setSpeed(128);

        assertThrows(ServiceException.class, () -> service.ptzControl(req),
            "未知 PTZ 指令应抛 ServiceException 由全局处理器转 200+错误体");
    }

    @Test
    @DisplayName("null 指令应抛 ServiceException")
    void ptzControl_nullCommand_throwsServiceException() {
        DevicePtzReq req = new DevicePtzReq();
        req.setDeviceId("dev1");
        req.setControl(null);
        req.setSpeed(128);

        assertThrows(ServiceException.class, () -> service.ptzControl(req));
    }
}
