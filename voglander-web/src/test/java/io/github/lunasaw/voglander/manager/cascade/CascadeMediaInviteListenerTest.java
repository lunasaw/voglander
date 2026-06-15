package io.github.lunasaw.voglander.manager.cascade;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.github.lunasaw.gb28181.common.entity.mansrtsp.ManSrtspRequest;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInfoEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeMediaInviteListener;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CascadeMediaInviteListener 单元测试")
class CascadeMediaInviteListenerTest {

    @Mock
    CascadeChannelManager       cascadeChannelManager;
    @Mock
    VoglanderServerMediaCommand serverMediaCommand;
    @InjectMocks
    CascadeMediaInviteListener  listener;

    /** SDP 为 null 时，INVITE 应优雅失败（不抛异常） */
    @Test
    @DisplayName("INVITE：SDP 为 null 时不抛异常")
    void invite_null_sdp_no_exception() {
        ClientInviteEvent event = new ClientInviteEvent(this, "call-001", "client-id", null, "ctx-key");
        Assertions.assertDoesNotThrow(() -> listener.onInvite(event));
        verifyNoInteractions(cascadeChannelManager);
    }

    /** BYE 事件：callId 未注册时不抛异常（幂等） */
    @Test
    @DisplayName("BYE：未注册的 callId 应幂等处理")
    void bye_unknown_call_id_no_exception() {
        ClientByeEvent event = new ClientByeEvent(this, "unknown-call", 200);
        Assertions.assertDoesNotThrow(() -> listener.onBye(event));
    }

    /** INFO：PAUSE → controlPlayBack(PLAY_RESUME) */
    @Test
    @DisplayName("INFO：PAUSE 透传 PLAY_RESUME")
    void info_pause_maps_resume_enum() throws Exception {
        seedSession("call-pb", "ssrc-1", "34020000001320000010", "34020000002000000001", true);
        ClientInfoEvent event = infoEvent("34020000002000000001", "PAUSE", null, null);
        listener.onInfo(event);
        verify(serverMediaCommand).controlPlayBack("34020000001320000010", PlayActionEnums.PLAY_RESUME);
    }

    /** INFO：PLAY 无 Range/Scale → controlPlayBack(PLAY_NOW) */
    @Test
    @DisplayName("INFO：PLAY 恢复透传 PLAY_NOW")
    void info_play_resume_maps_play_now() throws Exception {
        seedSession("call-pb", "ssrc-1", "34020000001320000010", "34020000002000000001", true);
        ClientInfoEvent event = infoEvent("34020000002000000001", "PLAY", null, null);
        listener.onInfo(event);
        verify(serverMediaCommand).controlPlayBack("34020000001320000010", PlayActionEnums.PLAY_NOW);
    }

    /** INFO：PLAY + Scale → controlPlayBack(PLAY_SPEED, scale) */
    @Test
    @DisplayName("INFO：倍速透传 PLAY_SPEED + 倍率值")
    void info_scale_maps_play_speed_with_value() throws Exception {
        seedSession("call-pb", "ssrc-1", "34020000001320000010", "34020000002000000001", true);
        ClientInfoEvent event = infoEvent("34020000002000000001", "PLAY", null, 2.0d);
        listener.onInfo(event);
        verify(serverMediaCommand).controlPlayBack("34020000001320000010", PlayActionEnums.PLAY_SPEED, 2.0d);
    }

    /** INFO：PLAY + Range(npt=30-) → controlPlayBack(PLAY_RANGE, 30L) */
    @Test
    @DisplayName("INFO：拖动透传 PLAY_RANGE + seek 秒")
    void info_range_maps_play_range_with_seconds() throws Exception {
        seedSession("call-pb", "ssrc-1", "34020000001320000010", "34020000002000000001", true);
        ClientInfoEvent event = infoEvent("34020000002000000001", "PLAY", "npt=30-", null);
        listener.onInfo(event);
        verify(serverMediaCommand).controlPlayBack("34020000001320000010", PlayActionEnums.PLAY_RANGE, 30L);
    }

    /** INFO：找不到活跃会话时不抛异常、不透传 */
    @Test
    @DisplayName("INFO：无活跃会话不透传")
    void info_no_session_no_forward() {
        ClientInfoEvent event = infoEvent("unknown-channel", "PAUSE", null, null);
        Assertions.assertDoesNotThrow(() -> listener.onInfo(event));
        verifyNoInteractions(serverMediaCommand);
    }

    /** Range 解析：npt=10.5-30 → 10 秒 */
    @Test
    @DisplayName("parseRangeSeconds：解析起始秒")
    void parse_range_seconds() {
        Assertions.assertEquals(10L, CascadeMediaInviteListener.parseRangeSeconds("npt=10.5-30"));
        Assertions.assertEquals(30L, CascadeMediaInviteListener.parseRangeSeconds("30-"));
        Assertions.assertNull(CascadeMediaInviteListener.parseRangeSeconds(""));
        Assertions.assertNull(CascadeMediaInviteListener.parseRangeSeconds("npt=-"));
    }

    // ============================ helpers ============================

    private ClientInfoEvent infoEvent(String userId, String method, String range, Double scale) {
        ManSrtspRequest rtsp = new ManSrtspRequest();
        rtsp.setMethod(method);
        rtsp.setRange(range);
        rtsp.setScale(scale);
        return new ClientInfoEvent(this, userId, "raw-body", "Application/MANSRTSP", rtsp);
    }

    /** 反射注入一条活跃会话到 activeSessions。 */
    @SuppressWarnings("unchecked")
    private void seedSession(String callId, String ssrc, String localDeviceId, String upperUserId, boolean playback)
        throws Exception {
        java.lang.reflect.Field field = CascadeMediaInviteListener.class.getDeclaredField("activeSessions");
        field.setAccessible(true);
        java.util.Map<String, Object> sessions = (java.util.Map<String, Object>) field.get(listener);

        Class<?> sessionClass = Class.forName(
            "io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeMediaInviteListener$MediaSession");
        Constructor<?> ctor = sessionClass.getDeclaredConstructor(
            String.class, String.class, String.class, boolean.class);
        ctor.setAccessible(true);
        Object session = ctor.newInstance(ssrc, localDeviceId, upperUserId, playback);
        sessions.put(callId, session);
    }
}
