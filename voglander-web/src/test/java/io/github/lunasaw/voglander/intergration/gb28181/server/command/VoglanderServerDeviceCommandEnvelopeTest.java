package io.github.lunasaw.voglander.intergration.gb28181.server.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.sipgateway.core.api.CommandHandler;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;

/**
 * VoglanderServerDeviceCommand envelope 改造单元测试
 *
 * <p>
 * 验证 5 个 Query 类命令均经 envelope 通道下发：
 * </p>
 * <ul>
 * <li>{@code gb28181.Query.DeviceInfo} - payload 仅 deviceId</li>
 * <li>{@code gb28181.Query.DeviceStatus} - payload 仅 deviceId</li>
 * <li>{@code gb28181.Query.Catalog} - payload 仅 deviceId</li>
 * <li>{@code gb28181.Query.PresetQuery} - payload 仅 deviceId</li>
 * <li>{@code gb28181.Query.MobilePosition} - payload {interval: String}</li>
 * </ul>
 *
 * @author luna
 * @since 2026/06/01
 */
@DisplayName("VoglanderServerDeviceCommand envelope 改造")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VoglanderServerDeviceCommandEnvelopeTest {

    private static final String          DEVICE_ID                  = "34020000001320000001";
    private static final String          CALL_ID                    = "device-cmd-corr-id";
    private static final String          NODE_ID                    = "node-1";
    private static final String          TYPE_DEVICE_INFO           = "gb28181.Query.DeviceInfo";
    private static final String          TYPE_DEVICE_STATUS         = "gb28181.Query.DeviceStatus";
    private static final String          TYPE_CATALOG               = "gb28181.Query.Catalog";
    private static final String          TYPE_PRESET_QUERY          = "gb28181.Query.PresetQuery";
    private static final String          TYPE_MOBILE_POSITION       = "gb28181.Query.MobilePosition";

    @Mock
    private CommandHandlerRegistry       registry;

    @Mock
    private CommandHandler               handler;

    @Mock
    private ServerCommandSender          serverCommandSender;

    @InjectMocks
    private VoglanderServerDeviceCommand command;

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
    @DisplayName("queryDeviceInfo → gb28181.Query.DeviceInfo, payload 仅 deviceId")
    public void queryDeviceInfoDispatchesEnvelope() {
        ResultDTO<Void> result = command.queryDeviceInfo(DEVICE_ID);

        assertNotNull(result);
        assertTrue(result.isSuccess());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_DEVICE_INFO));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals(TYPE_DEVICE_INFO, cmd.type());
        assertEquals(DEVICE_ID, cmd.deviceId());
        // payload 应为空（spec 仅 deviceId）
        assertTrue(cmd.payload() == null || cmd.payload().isEmpty(), "payload 应为空");

        verifyNoInteractions(serverCommandSender);
    }

    @Test
    @DisplayName("queryDeviceStatus → gb28181.Query.DeviceStatus")
    public void queryDeviceStatusDispatchesEnvelope() {
        command.queryDeviceStatus(DEVICE_ID);
        verify(registry).require(eq(TYPE_DEVICE_STATUS));
        verifyNoInteractions(serverCommandSender);
    }

    @Test
    @DisplayName("queryDeviceCatalog → gb28181.Query.Catalog")
    public void queryDeviceCatalogDispatchesEnvelope() {
        command.queryDeviceCatalog(DEVICE_ID);
        verify(registry).require(eq(TYPE_CATALOG));
        verifyNoInteractions(serverCommandSender);
    }

    @Test
    @DisplayName("queryDevicePreset → gb28181.Query.PresetQuery")
    public void queryDevicePresetDispatchesEnvelope() {
        command.queryDevicePreset(DEVICE_ID);
        verify(registry).require(eq(TYPE_PRESET_QUERY));
        verifyNoInteractions(serverCommandSender);
    }

    @Test
    @DisplayName("queryDeviceMobilePosition(deviceId, interval) → payload 含 interval")
    public void queryDeviceMobilePositionWithInterval() {
        command.queryDeviceMobilePosition(DEVICE_ID, "10");

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_MOBILE_POSITION));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals(DEVICE_ID, cmd.deviceId());
        assertEquals("10", cmd.payload().get("interval"));
    }

    @Test
    @DisplayName("queryDeviceMobilePosition(deviceId) → 默认 interval=5")
    public void queryDeviceMobilePositionDefaultInterval() {
        command.queryDeviceMobilePosition(DEVICE_ID);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals("5", captor.getValue().payload().get("interval"));
    }

    @Test
    @DisplayName("空 deviceId → 不触发 envelope")
    public void emptyDeviceIdRejected() {
        try {
            command.queryDeviceInfo("");
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
        verifyNoInteractions(serverCommandSender);
    }
}
