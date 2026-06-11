package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;

/**
 * LabByeListener 单元测试：收到 BYE 按 callId 停流。
 */
@ExtendWith(MockitoExtension.class)
class LabByeListenerTest {

    @Mock
    LabMediaPushService pushService;

    @InjectMocks
    LabByeListener      listener;

    @Test
    @DisplayName("onBye: 透传 callId 给 stopByCallId")
    void onBye_stopsByCallId() {
        ClientByeEvent e = new ClientByeEvent(this, "call-9", 200);

        listener.onBye(e);

        verify(pushService).stopByCallId("call-9");
    }
}
