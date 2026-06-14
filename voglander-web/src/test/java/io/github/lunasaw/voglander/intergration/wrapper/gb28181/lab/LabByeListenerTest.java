package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;

/**
 * LabByeListener 单元测试：收到 BYE 按 callId 停流 + 推 clientcmd.bye SSE。
 */
@ExtendWith(MockitoExtension.class)
class LabByeListenerTest {

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    LabMediaPushService       pushService;

    @InjectMocks
    LabByeListener            listener;

    @Test
    @DisplayName("onBye: 透传 callId 给 stopByCallId，并推 clientcmd.bye SSE")
    void onBye_stopsByCallId() {
        ClientByeEvent e = new ClientByeEvent(this, "call-9", 200);

        listener.onBye(e);

        verify(pushService).stopByCallId("call-9");

        // 与 clientcmd.invite 对称：推 clientcmd.bye 供前端时间线展示关流
        ArgumentCaptor<SseRelayEvent> captor = ArgumentCaptor.forClass(SseRelayEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        SseRelayEvent sre = captor.getValue();
        assertEquals("clientcmd.bye", sre.getTopic());
        assertEquals("call-9", sre.getData().get("callId"));
        assertEquals(200, sre.getData().get("statusCode"));
    }
}
