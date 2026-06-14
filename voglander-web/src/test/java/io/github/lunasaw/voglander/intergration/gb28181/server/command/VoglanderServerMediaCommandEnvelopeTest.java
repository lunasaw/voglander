package io.github.lunasaw.voglander.intergration.gb28181.server.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.sipgateway.core.api.CommandHandler;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;

/**
 * VoglanderServerMediaCommand envelope 改造单元测试
 *
 * <p>
 * Schema：
 * <ul>
 * <li>{@code gb28181.Invite.Play}（白名单）��{@code {mediaIp, mediaPort:int, streamMode?: name (default UDP)}}</li>
 * <li>{@code gb28181.Invite.Playback}（白名单）：上述 + {@code startTime, endTime}（String）</li>
 * <li>{@code gb28181.Invite.PlaybackControl}（白名单）：{@code {action: PlayActionEnums.name()}}</li>
 * <li>{@code gb28181.Invite.Ack}（白名单）：{@code {callId?: String}}</li>
 * <li>{@code gb28181.Invite.Bye}（declare）：{@code {callId: String}}, deviceId 留空</li>
 * <li>{@code gb28181.Device.Broadcast}（declare）：payload {} 仅 deviceId</li>
 * </ul>
 * </p>
 *
 * @author luna
 * @since 2026/06/01
 */
@DisplayName("VoglanderServerMediaCommand envelope 改造")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VoglanderServerMediaCommandEnvelopeTest {

    private static final String         DEVICE_ID                = "34020000001320000001";
    private static final String         CALL_ID                  = "media-corr-id";
    private static final String         NODE_ID                  = "node-1";
    private static final String         TYPE_INVITE_PLAY         = "gb28181.Invite.Play";
    private static final String         TYPE_INVITE_PLAYBACK     = "gb28181.Invite.Playback";
    private static final String         TYPE_PLAYBACK_CONTROL    = "gb28181.Invite.PlaybackControl";
    private static final String         TYPE_INVITE_ACK          = "gb28181.Invite.Ack";
    private static final String         TYPE_INVITE_BYE          = "gb28181.Invite.Bye";
    private static final String         TYPE_BROADCAST           = "gb28181.Device.Broadcast";

    @Mock
    private CommandHandlerRegistry      registry;

    @Mock
    private CommandHandler              handler;

    @InjectMocks
    private VoglanderServerMediaCommand command;

    @BeforeEach
    public void setUp() {
        when(registry.require(anyString())).thenReturn(handler);
        when(handler.handle(any(GatewayCommand.class)))
            .thenAnswer(inv -> {
                GatewayCommand cmd = inv.getArgument(0);
                return new GatewayCommandResult(CALL_ID, cmd.type(), NODE_ID);
            });
    }

    @Test
    @DisplayName("inviteRealTimePlay → Invite.Play payload {mediaIp, mediaPort, streamMode}")
    public void inviteRealTimePlayDispatchesEnvelope() {
        ResultDTO<Void> result = command.inviteRealTimePlay(DEVICE_ID, "192.168.1.10", 30000, StreamModeEnum.TCP_ACTIVE);
        assertNotNull(result);
        assertTrue(result.isSuccess());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_INVITE_PLAY));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        Map<String, Object> p = cmd.payload();
        assertEquals(DEVICE_ID, cmd.deviceId());
        // 白名单字段是 mediaIp（不是 sdpIp）
        assertEquals("192.168.1.10", p.get("mediaIp"));
        assertEquals(30000, p.get("mediaPort"));
        assertEquals(StreamModeEnum.TCP_ACTIVE.name(), p.get("streamMode"));
    }

    @Test
    @DisplayName("inviteRealTimePlay 默认 → streamMode=UDP")
    public void inviteRealTimePlayDefaultUdp() {
        command.inviteRealTimePlay(DEVICE_ID, "192.168.1.10", 30000);
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals(StreamModeEnum.UDP.name(), captor.getValue().payload().get("streamMode"));
    }

    @Test
    @DisplayName("invitePlayBack → Invite.Playback payload 含 startTime/endTime")
    public void invitePlayBackDispatchesEnvelope() {
        command.invitePlayBack(DEVICE_ID, "192.168.1.10", 30000, StreamModeEnum.UDP,
            "2026-06-01T08:00:00", "2026-06-01T09:00:00");

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_INVITE_PLAYBACK));
        verify(handler).handle(captor.capture());

        Map<String, Object> p = captor.getValue().payload();
        assertEquals("192.168.1.10", p.get("mediaIp"));
        assertEquals(30000, p.get("mediaPort"));
        assertEquals(StreamModeEnum.UDP.name(), p.get("streamMode"));
        assertEquals("2026-06-01T08:00:00", p.get("startTime"));
        assertEquals("2026-06-01T09:00:00", p.get("endTime"));
    }

    @Test
    @DisplayName("controlPlayBack → Invite.PlaybackControl payload {action: PlayActionEnums.name()}")
    public void controlPlayBackDispatchesEnvelope() {
        command.controlPlayBack(DEVICE_ID, PlayActionEnums.PLAY_NOW);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_PLAYBACK_CONTROL));
        verify(handler).handle(captor.capture());
        assertEquals(PlayActionEnums.PLAY_NOW.name(), captor.getValue().payload().get("action"));
    }

    @Test
    @DisplayName("sendAck(deviceId) → Invite.Ack payload 无 callId")
    public void sendAckWithoutCallId() {
        command.sendAck(DEVICE_ID);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_INVITE_ACK));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals(DEVICE_ID, cmd.deviceId());
        assertTrue(cmd.payload() == null || !cmd.payload().containsKey("callId"));
    }

    @Test
    @DisplayName("sendAck(deviceId, callId) → Invite.Ack payload {callId}")
    public void sendAckWithCallId() {
        command.sendAck(DEVICE_ID, "abc-call-id");

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals("abc-call-id", captor.getValue().payload().get("callId"));
    }

    @Test
    @DisplayName("sendBye(callId) → Invite.Bye payload {callId}, deviceId 留空")
    public void sendByeDispatchesEnvelope() {
        command.sendBye("session-call-id");

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_INVITE_BYE));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        // declare 表 spec 里 Bye 用 arg("callId") 从 payload 取
        assertEquals("session-call-id", cmd.payload().get("callId"));
    }

    @Test
    @DisplayName("sendBroadcast → Device.Broadcast payload 为空")
    public void sendBroadcastDispatchesEnvelope() {
        command.sendBroadcast(DEVICE_ID);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_BROADCAST));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals(DEVICE_ID, cmd.deviceId());
        assertTrue(cmd.payload() == null || cmd.payload().isEmpty(), "Broadcast payload 应为空");
    }

    @Test
    @DisplayName("playBack 便捷方法 → action=PLAY_NOW")
    public void playBackDelegatesToControl() {
        command.playBack(DEVICE_ID);
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals(PlayActionEnums.PLAY_NOW.name(), captor.getValue().payload().get("action"));
    }

    @Test
    @DisplayName("空 deviceId → 不触发 envelope")
    public void emptyDeviceIdRejected() {
        try {
            command.inviteRealTimePlay("", "1.1.1.1", 1, StreamModeEnum.UDP);
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
    }

    @Test
    @DisplayName("空 callId 给 sendBye → IllegalArgumentException, 不触发 envelope")
    public void emptyCallIdRejected() {
        try {
            command.sendBye(null);
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
    }
}
