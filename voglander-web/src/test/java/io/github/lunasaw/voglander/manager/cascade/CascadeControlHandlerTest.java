package io.github.lunasaw.voglander.manager.cascade;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlAlarm;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlGuard;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTeleBoot;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeControlHandler;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm.VoglanderServerAlarmCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.guard.VoglanderServerGuardCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("CascadeControlHandler 单元测试")
class CascadeControlHandlerTest {

    @Mock
    CascadeChannelManager       cascadeChannelManager;
    @Mock
    VoglanderServerPtzCommand   serverPtzCommand;
    @Mock
    VoglanderServerRecordCommand serverRecordCommand;
    @Mock
    VoglanderServerGuardCommand  serverGuardCommand;
    @Mock
    VoglanderServerAlarmCommand  serverAlarmCommand;
    @Mock
    VoglanderServerDeviceCommand serverDeviceCommand;
    @InjectMocks
    CascadeControlHandler        handler;

    private void stubChannel(String platformId, String cascadeChannelId, String localDeviceId) {
        CascadeChannelDTO ch = new CascadeChannelDTO();
        ch.setLocalDeviceId(localDeviceId);
        when(cascadeChannelManager.getByPlatformAndCascadeChannelId(platformId, cascadeChannelId)).thenReturn(ch);
    }

    private DeviceControlPtz ptz(String deviceId, String hexCmd) {
        DeviceControlPtz c = new DeviceControlPtz();
        c.setDeviceId(deviceId);
        c.setPtzCmd(hexCmd);
        return c;
    }

    @Test
    @DisplayName("PTZ 转发：找到通道后应调用 serverPtzCommand")
    void ptz_forwards_to_real_device() {
        stubChannel("platform-A", "cascade-ch-01", "real-device-01");
        handler.onPtzControl("platform-A", ptz("cascade-ch-01", "A50F01020000004D"));
        verify(serverPtzCommand).controlDevicePtz("real-device-01", "A50F01020000004D");
    }

    @Test
    @DisplayName("PTZ 转发：通道不存在时不调用 serverPtzCommand")
    void ptz_skips_when_channel_not_found() {
        when(cascadeChannelManager.getByPlatformAndCascadeChannelId(any(), any())).thenReturn(null);
        handler.onPtzControl("platform-X", ptz("unknown-ch", "A50F01020000004D"));
        verifyNoInteractions(serverPtzCommand);
    }

    @Test
    @DisplayName("布防/撤防转发：找到通道后透传 guardCmd")
    void guard_forwards_to_real_device() {
        stubChannel("platform-A", "cascade-ch-01", "real-device-01");
        DeviceControlGuard cmd = new DeviceControlGuard();
        cmd.setDeviceId("cascade-ch-01");
        cmd.setGuardCmd("SetGuard");
        handler.onGuard("platform-A", cmd);
        verify(serverGuardCommand).controlDeviceGuard("real-device-01", "SetGuard");
    }

    @Test
    @DisplayName("告警复位转发：找到通道后透传 method/type")
    void alarm_reset_forwards_to_real_device() {
        stubChannel("platform-A", "cascade-ch-01", "real-device-01");
        DeviceControlAlarm cmd = new DeviceControlAlarm();
        cmd.setDeviceId("cascade-ch-01");
        DeviceControlAlarm.AlarmInfo info = new DeviceControlAlarm.AlarmInfo();
        info.setAlarmMethod("1");
        info.setAlarmType("2");
        cmd.setAlarmInfo(info);
        handler.onAlarmReset("platform-A", cmd);
        verify(serverAlarmCommand).controlDeviceAlarm("real-device-01", "1", "2");
    }

    @Test
    @DisplayName("远程重启转发：找到通道后透传 teleBoot")
    void teleboot_forwards_to_real_device() {
        stubChannel("platform-A", "cascade-ch-01", "real-device-01");
        DeviceControlTeleBoot cmd = new DeviceControlTeleBoot();
        cmd.setDeviceId("cascade-ch-01");
        cmd.setTeleBoot("Boot");
        handler.onTeleBoot("platform-A", cmd);
        verify(serverDeviceCommand).teleBoot("real-device-01");
    }

    @Test
    @DisplayName("控制转发：通道不存在时全部不透传")
    void control_skips_when_channel_not_found() {
        when(cascadeChannelManager.getByPlatformAndCascadeChannelId(any(), any())).thenReturn(null);
        DeviceControlGuard guard = new DeviceControlGuard();
        guard.setDeviceId("x");
        guard.setGuardCmd("SetGuard");
        handler.onGuard("p", guard);
        DeviceControlTeleBoot boot = new DeviceControlTeleBoot();
        boot.setDeviceId("x");
        handler.onTeleBoot("p", boot);
        verifyNoInteractions(serverGuardCommand, serverDeviceCommand);
    }
}
