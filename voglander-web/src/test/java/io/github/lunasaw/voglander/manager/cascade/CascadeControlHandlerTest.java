package io.github.lunasaw.voglander.manager.cascade;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeControlHandler;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("CascadeControlHandler 单元测试")
class CascadeControlHandlerTest {

    @Mock  CascadeChannelManager    cascadeChannelManager;
    @Mock  VoglanderServerPtzCommand serverPtzCommand;
    @InjectMocks CascadeControlHandler handler;

    private DeviceControlPtz cmd(String deviceId, String hexCmd) {
        DeviceControlPtz c = new DeviceControlPtz();
        c.setDeviceId(deviceId);
        c.setPtzCmd(hexCmd);
        return c;
    }

    @Test
    @DisplayName("PTZ 转发：找到通道后应调用 serverPtzCommand")
    void ptz_forwards_to_real_device() {
        CascadeChannelDTO ch = new CascadeChannelDTO();
        ch.setLocalDeviceId("real-device-01");
        when(cascadeChannelManager.getByPlatformAndCascadeChannelId("platform-A", "cascade-ch-01"))
            .thenReturn(ch);

        handler.onPtzControl("platform-A", cmd("cascade-ch-01", "A50F01020000004D"));

        verify(serverPtzCommand).controlDevicePtz("real-device-01", "A50F01020000004D");
    }

    @Test
    @DisplayName("PTZ 转发：通道不存在时不调用 serverPtzCommand")
    void ptz_skips_when_channel_not_found() {
        when(cascadeChannelManager.getByPlatformAndCascadeChannelId(any(), any())).thenReturn(null);

        handler.onPtzControl("platform-X", cmd("unknown-ch", "A50F01020000004D"));

        verifyNoInteractions(serverPtzCommand);
    }
}
