package io.github.lunasaw.voglander.manager.cascade;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeMediaInviteListener;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("CascadeMediaInviteListener 单元测试")
class CascadeMediaInviteListenerTest {

    @Mock  CascadeChannelManager      cascadeChannelManager;
    @Mock  VoglanderServerMediaCommand serverMediaCommand;
    @InjectMocks CascadeMediaInviteListener listener;

    /** SDP 为 null 时，INVITE 应优雅失败（不抛异常） */
    @Test
    @DisplayName("INVITE：SDP 为 null 时不抛异常")
    void invite_null_sdp_no_exception() {
        ClientInviteEvent event = new ClientInviteEvent(this, "call-001", "client-id", null, "ctx-key");
        // SDP null → 走 sendInviteError 分支，不应抛出
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> listener.onInvite(event));
        verifyNoInteractions(cascadeChannelManager);
    }

    /** BYE 事件：callId 未注册时不抛异常（幂等） */
    @Test
    @DisplayName("BYE：未注册的 callId 应幂等处理")
    void bye_unknown_call_id_no_exception() {
        ClientByeEvent event = new ClientByeEvent(this, "unknown-call", 200);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> listener.onBye(event));
    }

    /** BYE 事件：已注册 callId 的 ssrc 被移除（内存清理） */
    @Test
    @DisplayName("BYE：已注册 callId 会从 activeSessions 中移除")
    void bye_removes_active_session() throws Exception {
        // 通过反射向 activeSessions 插入一条记录，模拟已建立的推流
        java.lang.reflect.Field field =
            CascadeMediaInviteListener.class.getDeclaredField("activeSessions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentHashMap<String, String> sessions =
            (java.util.concurrent.ConcurrentHashMap<String, String>) field.get(listener);
        sessions.put("call-abc", "12345");

        ClientByeEvent event = new ClientByeEvent(this, "call-abc", 200);
        // zlmConfig 为 null → 只清内存，不调 ZLM
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> listener.onBye(event));

        org.junit.jupiter.api.Assertions.assertFalse(sessions.containsKey("call-abc"));
    }
}
