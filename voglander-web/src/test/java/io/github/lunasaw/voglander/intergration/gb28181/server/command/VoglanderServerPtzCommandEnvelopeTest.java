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

import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.sipgateway.core.api.CommandHandler;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;

/**
 * VoglanderServerPtzCommand envelope 改造单元测试
 *
 * <p>
 * 验证 PTZ 命令必须经 envelope 通道（{@link CommandHandlerRegistry#require(String)} → {@link CommandHandler#handle(GatewayCommand)}）下发，
 * </p>
 *
 * <h3>Payload schema 约束（见 Gb28181WhitelistHandlers#ptz）</h3>
 * <ul>
 * <li>枚举模式：{@code {cmd: PTZControlEnum 名称, speed: int}}</li>
 * <li>16 进制模式：{@code {hex: String}}</li>
 * </ul>
 *
 * @author luna
 * @since 2026/06/01
 */
@DisplayName("VoglanderServerPtzCommand envelope 改造")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VoglanderServerPtzCommandEnvelopeTest {

    private static final String       DEVICE_ID    = "34020000001320000001";
    private static final String       CALL_ID      = "test-correlation-id";
    private static final String       NODE_ID      = "node-1";
    private static final String       TYPE_PTZ     = "gb28181.Control.Ptz";

    @Mock
    private CommandHandlerRegistry    registry;

    @Mock
    private CommandHandler            handler;

    @InjectMocks
    private VoglanderServerPtzCommand command;

    @BeforeEach
    public void setUp() {
        when(registry.require(anyString())).thenReturn(handler);
        when(handler.handle(any(GatewayCommand.class)))
            .thenReturn(new GatewayCommandResult(CALL_ID, TYPE_PTZ, NODE_ID));
    }

    @Test
    @DisplayName("controlDevicePtz(deviceId, hex) → envelope 含 hex 字段")
    public void controlDevicePtzWithHex() {
        String hexCmd = "A50F010000000000";
        ResultDTO<Void> result = command.controlDevicePtz(DEVICE_ID, hexCmd);

        assertNotNull(result);
        assertTrue(result.isSuccess());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_PTZ));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals(TYPE_PTZ, cmd.type());
        assertEquals(DEVICE_ID, cmd.deviceId());
        Map<String, Object> payload = cmd.payload();
        assertEquals(hexCmd, payload.get("hex"));

    }

    @Test
    @DisplayName("controlDevicePtz(deviceId, PTZControlEnum, speed) → envelope 含 cmd 与 speed")
    public void controlDevicePtzWithEnumAndSpeed() {
        ResultDTO<Void> result = command.controlDevicePtz(DEVICE_ID, PTZControlEnum.TILT_UP, 100);

        assertTrue(result.isSuccess());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_PTZ));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals(DEVICE_ID, cmd.deviceId());
        Map<String, Object> payload = cmd.payload();
        assertEquals(PTZControlEnum.TILT_UP.name(), payload.get("cmd"));
        assertEquals(100, payload.get("speed"));

    }

    @Test
    @DisplayName("speed 为 null 时使用默认速度 128")
    public void controlDevicePtzWithNullSpeed() {
        command.controlDevicePtz(DEVICE_ID, PTZControlEnum.PAN_LEFT, null);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals(128, captor.getValue().payload().get("speed"));
    }

    @Test
    @DisplayName("moveUp(deviceId, speed) → cmd=TILT_UP")
    public void moveUpDispatchesTiltUp() {
        command.moveUp(DEVICE_ID, 200);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals(PTZControlEnum.TILT_UP.name(), captor.getValue().payload().get("cmd"));
        assertEquals(200, captor.getValue().payload().get("speed"));
    }

    @Test
    @DisplayName("zoomIn 默认速度便捷方法 → cmd=ZOOM_IN, speed=128")
    public void zoomInWithDefaultSpeed() {
        command.zoomIn(DEVICE_ID);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals(PTZControlEnum.ZOOM_IN.name(), captor.getValue().payload().get("cmd"));
        assertEquals(128, captor.getValue().payload().get("speed"));
    }

    @Test
    @DisplayName("stopDevicePtz → cmd=STOP, speed=0")
    public void stopDevicePtzSendsStop() {
        command.stopDevicePtz(DEVICE_ID);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals(PTZControlEnum.STOP.name(), captor.getValue().payload().get("cmd"));
        assertEquals(0, captor.getValue().payload().get("speed"));
    }

    @Test
    @DisplayName("registry/handler 抛异常 → 返回失败 ResultDTO，不直接调 sender")
    public void exceptionInHandlerReturnsFailure() {
        when(handler.handle(any())).thenThrow(new RuntimeException("envelope dispatch fail"));

        ResultDTO<Void> result = command.controlDevicePtz(DEVICE_ID, PTZControlEnum.TILT_UP, 100);

        assertNotNull(result);
        // ResultDTO 失败时 isSuccess 为 false
        assertEquals(false, result.isSuccess());
    }

    @Test
    @DisplayName("参数校验：deviceId 为空 → IllegalArgumentException, 不触发 envelope")
    public void emptyDeviceIdRejected() {
        try {
            command.controlDevicePtz("", PTZControlEnum.TILT_UP, 128);
        } catch (IllegalArgumentException ignored) {
            // expected
        }
        verify(registry, never()).require(anyString());
    }
}
