package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;

/**
 * LabInviteListener 单元测试：onInvite 解析目标→回 200 OK→推 SSE（含媒体目标字段）→auto 时起推流。
 */
@ExtendWith(MockitoExtension.class)
class LabInviteListenerTest {

    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    LabMediaPushService       pushService;

    @InjectMocks
    LabInviteListener         listener;

    private LabInviteTarget target() {
        return new LabInviteTarget("call-1", "dev-1", "127.0.0.1", 30000, "999", "UDP", "Play", "ctx-1");
    }

    @BeforeEach
    void stub() {
        when(pushService.parseTarget(any())).thenReturn(target());
    }

    @Test
    @DisplayName("onInvite: 解析目标 + 回 200 OK + 推 SSE clientcmd.invite（含 mediaIp/mediaPort/ssrc）")
    void onInvite_acceptsAndPublishes() {
        when(pushService.isAutoPush()).thenReturn(false);
        ClientInviteEvent e = new ClientInviteEvent(this, "call-1", "dev-1", null, "ctx-1");

        listener.onInvite(e);

        verify(pushService).acceptInvite(any(LabInviteTarget.class));

        ArgumentCaptor<SseRelayEvent> cap = ArgumentCaptor.forClass(SseRelayEvent.class);
        verify(eventPublisher).publishEvent(cap.capture());
        SseRelayEvent ev = cap.getValue();
        assertThat(ev.getTopic()).isEqualTo("clientcmd.invite");
        assertThat(ev.getData()).containsEntry("callId", "call-1")
            .containsEntry("clientId", "dev-1")
            .containsEntry("mediaIp", "127.0.0.1")
            .containsEntry("mediaPort", 30000)
            .containsEntry("ssrc", "999");

        // 非自动：不起推流
        verify(pushService, never()).startPush(any(), any(), any());
    }

    @Test
    @DisplayName("onInvite: auto=true 时收到 INVITE 自动起推流")
    void onInvite_autoStartsPush() {
        when(pushService.isAutoPush()).thenReturn(true);
        ClientInviteEvent e = new ClientInviteEvent(this, "call-1", "dev-1", null, "ctx-1");

        listener.onInvite(e);

        verify(pushService).acceptInvite(any(LabInviteTarget.class));
        verify(pushService, times(1)).startPush(any(LabInviteTarget.class), isNull(), isNull());
    }
}
