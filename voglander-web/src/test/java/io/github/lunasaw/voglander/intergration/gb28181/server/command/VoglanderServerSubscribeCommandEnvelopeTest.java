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

import io.github.lunasaw.sipgateway.core.api.CommandHandler;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.subscribe.VoglanderServerSubscribeCommand;

/**
 * {@link VoglanderServerSubscribeCommand} envelope 单元测试。
 * <p>
 * 验证 5 个订阅命令的 type 逐字对齐框架注册键（{@code gb28181.Subscribe.*}）、payload 键齐全、
 * 取 callId 路径走 {@code dispatchEnvelopeWithCallId}（unsubscribe 走 {@code dispatchEnvelope}）。
 * </p>
 *
 * @author luna
 */
@DisplayName("VoglanderServerSubscribeCommand envelope")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VoglanderServerSubscribeCommandEnvelopeTest {

    private static final String              DEVICE_ID  = "34020000001320000001";
    private static final String              CALL_ID    = "subscribe-call-id";
    private static final String              NODE_ID    = "node-1";

    @Mock
    private CommandHandlerRegistry           registry;

    @Mock
    private CommandHandler                   handler;

    @InjectMocks
    private VoglanderServerSubscribeCommand  command;

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
    @DisplayName("subscribeCatalog → gb28181.Subscribe.Catalog + payload{expires, eventType=Catalog}, 返回 callId")
    public void subscribeCatalog() {
        ResultDTO<String> result = command.subscribeCatalog(DEVICE_ID, 3600);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(CALL_ID, result.getData());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq("gb28181.Subscribe.Catalog"));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals("gb28181.Subscribe.Catalog", cmd.type());
        assertEquals(DEVICE_ID, cmd.deviceId());
        assertEquals(3600, ((Number) cmd.payload().get("expires")).intValue());
        assertEquals("Catalog", cmd.payload().get("eventType"));
    }

    @Test
    @DisplayName("subscribeMobilePosition → gb28181.Subscribe.MobilePosition + payload{interval:String, expires, eventType=presence.mobile}")
    public void subscribeMobilePosition() {
        ResultDTO<String> result = command.subscribeMobilePosition(DEVICE_ID, 5, 3600);

        assertTrue(result.isSuccess());
        assertEquals(CALL_ID, result.getData());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq("gb28181.Subscribe.MobilePosition"));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals("gb28181.Subscribe.MobilePosition", cmd.type());
        // interval 框架要求 String 类型
        assertEquals("5", cmd.payload().get("interval"));
        assertEquals(3600, ((Number) cmd.payload().get("expires")).intValue());
        assertEquals("presence.mobile", cmd.payload().get("eventType"));
    }

    @Test
    @DisplayName("subscribeAlarm → gb28181.Subscribe.Alarm + payload{expires, eventType=presence.alarm}")
    public void subscribeAlarm() {
        ResultDTO<String> result = command.subscribeAlarm(DEVICE_ID, 3600);

        assertTrue(result.isSuccess());
        assertEquals(CALL_ID, result.getData());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq("gb28181.Subscribe.Alarm"));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals("gb28181.Subscribe.Alarm", cmd.type());
        assertEquals(3600, ((Number) cmd.payload().get("expires")).intValue());
        assertEquals("presence.alarm", cmd.payload().get("eventType"));
    }

    @Test
    @DisplayName("refresh → gb28181.Subscribe.Refresh + payload{callId, expires}")
    public void refresh() {
        ResultDTO<String> result = command.refresh(CALL_ID, 3600);

        assertTrue(result.isSuccess());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq("gb28181.Subscribe.Refresh"));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals("gb28181.Subscribe.Refresh", cmd.type());
        assertEquals(CALL_ID, cmd.payload().get("callId"));
        assertEquals(3600, ((Number) cmd.payload().get("expires")).intValue());
    }

    @Test
    @DisplayName("unsubscribe → gb28181.Subscribe.Unsubscribe + payload{callId}")
    public void unsubscribe() {
        ResultDTO<Void> result = command.unsubscribe(CALL_ID);

        assertTrue(result.isSuccess());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq("gb28181.Subscribe.Unsubscribe"));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals("gb28181.Subscribe.Unsubscribe", cmd.type());
        assertEquals(CALL_ID, cmd.payload().get("callId"));
    }

    @Test
    @DisplayName("空 deviceId → 不触发 envelope")
    public void emptyDeviceIdRejected() {
        try {
            command.subscribeCatalog("", 3600);
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
    }

    @Test
    @DisplayName("空 callId 续订 → 不触发 envelope")
    public void emptyCallIdRejected() {
        try {
            command.refresh(null, 3600);
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
    }
}
