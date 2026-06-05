package io.github.lunasaw.voglander.service.command.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;

import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm.VoglanderServerAlarmCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.config.VoglanderServerConfigCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D4 红线：回放 {@code /control} 必须真正下发（而非占位 {@code return true}）。
 * <p>
 * L1（本次）：pause/resume —— {@code PLAY_NOW}/{@code PLAY_RESUME} 无需 param，经 streamId 反查 deviceId 后
 * 调 {@code controlPlayBack} 真实下发；会话不存在返回失败；{@code PLAY_RANGE}/{@code PLAY_SPEED} 显式不支持。
 *
 * @author luna
 */
@DisplayName("D4 — 回放 control 接通(L1 pause/resume)")
@ExtendWith(MockitoExtension.class)
class GbDeviceCommandServicePlaybackTest {

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
    @Mock
    private MediaSessionManager          mediaSessionManager;

    @InjectMocks
    private GbDeviceCommandService        service;

    private MediaSessionDTO session(String streamId, String deviceId) {
        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setStreamId(streamId);
        dto.setDeviceId(deviceId);
        return dto;
    }

    @Test
    @DisplayName("PLAY_NOW(继续) → 反查 deviceId 真实下发 controlPlayBack")
    void controlPlayback_playNow_dispatches() {
        when(mediaSessionManager.getByStreamId("s1")).thenReturn(session("s1", "dev1"));
        when(mediaCommand.controlPlayBack(eq("dev1"), eq(PlayActionEnums.PLAY_NOW)))
            .thenReturn(ResultDTOUtils.success());

        ResultDTO<Void> r = service.controlPlayback("s1", "PLAY_NOW", null);

        assertTrue(r.isSuccess());
        verify(mediaCommand).controlPlayBack("dev1", PlayActionEnums.PLAY_NOW);
    }

    @Test
    @DisplayName("PLAY_RESUME(暂停) → 真实下发 controlPlayBack")
    void controlPlayback_playResume_dispatches() {
        when(mediaSessionManager.getByStreamId("s1")).thenReturn(session("s1", "dev1"));
        when(mediaCommand.controlPlayBack(eq("dev1"), eq(PlayActionEnums.PLAY_RESUME)))
            .thenReturn(ResultDTOUtils.success());

        ResultDTO<Void> r = service.controlPlayback("s1", "PLAY_RESUME", null);

        assertTrue(r.isSuccess());
        verify(mediaCommand).controlPlayBack("dev1", PlayActionEnums.PLAY_RESUME);
    }

    @Test
    @DisplayName("会话不存在 → 返回失败，不下发")
    void controlPlayback_sessionNotFound_fails() {
        when(mediaSessionManager.getByStreamId("ghost")).thenReturn(null);

        ResultDTO<Void> r = service.controlPlayback("ghost", "PLAY_NOW", null);

        assertFalse(r.isSuccess());
        verify(mediaCommand, never()).controlPlayBack(any(), any());
    }

    @Test
    @DisplayName("PLAY_RANGE(seek) → 显式不支持，不假成功、不下发")
    void controlPlayback_playRange_unsupported() {
        // 动作先于会话校验：seek 不支持，提前返回，无需 stub getByStreamId
        ResultDTO<Void> r = service.controlPlayback("s1", "PLAY_RANGE", "100");

        assertFalse(r.isSuccess(), "seek 本次不支持，必须返回失败");
        verify(mediaCommand, never()).controlPlayBack(any(), any());
    }

    @Test
    @DisplayName("PLAY_SPEED(倍速) → 显式不支持")
    void controlPlayback_playSpeed_unsupported() {
        ResultDTO<Void> r = service.controlPlayback("s1", "PLAY_SPEED", "2.0");

        assertFalse(r.isSuccess());
        verify(mediaCommand, never()).controlPlayBack(any(), any());
    }

    @Test
    @DisplayName("未知 action → 返回失败")
    void controlPlayback_unknownAction_fails() {
        ResultDTO<Void> r = service.controlPlayback("s1", "WARP_SPEED", null);

        assertFalse(r.isSuccess());
        verify(mediaCommand, never()).controlPlayBack(any(), any());
    }
}
