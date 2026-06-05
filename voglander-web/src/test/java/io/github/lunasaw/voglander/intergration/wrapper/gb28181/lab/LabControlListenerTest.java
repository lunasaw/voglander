package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTeleBoot;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class LabControlListenerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LabControlListener listener;

    @Test
    public void testPtzControlPublishesClientcmdPtzWithParsedData() {
        DeviceControlPtz cmd = new DeviceControlPtz();
        cmd.setDeviceId("34020000001320000001");
        // byte[3]=0x08(UP), data1=0x80, data2=0x80
        cmd.setPtzCmd("A50F01018000800000");

        listener.onPtzControl("platform-001", cmd);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        SseRelayEvent evt = (SseRelayEvent) captor.getValue();
        assertEquals("clientcmd.ptz", evt.getTopic());
        assertEquals("platform-001", evt.getData().get("platformId"));
        assertEquals("34020000001320000001", evt.getData().get("channelId"));
        assertNotNull(evt.getData().get("parsed"));
        log.info("LabControlListener.onPtzControl→clientcmd.ptz 校验通过");
    }

    @Test
    public void testTeleBootPublishesClientcmdReboot() {
        DeviceControlTeleBoot cmd = new DeviceControlTeleBoot();
        cmd.setDeviceId("ch-1");

        listener.onTeleBoot("plat", cmd);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertEquals("clientcmd.reboot", ((SseRelayEvent) captor.getValue()).getTopic());
        log.info("LabControlListener.onTeleBoot→clientcmd.reboot 校验通过");
    }
}
