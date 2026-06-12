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

import io.github.lunasaw.sipgateway.core.api.CommandHandler;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.config.VoglanderServerConfigCommand;

/**
 * VoglanderServerConfigCommand envelope 改造单元测试
 *
 * <p>
 * Schema：
 * <ul>
 * <li>{@code gb28181.Config.BasicParam}: {@code {name, expiration, heartBeatInterval, heartBeatCount}} 全部 String</li>
 * <li>{@code gb28181.Config.ConfigDownload}: {@code {configType: String}}</li>
 * <li>{@code gb28181.Control.Reboot}: payload {} 仅 deviceId</li>
 * </ul>
 * </p>
 *
 * @author luna
 * @since 2026/06/01
 */
@DisplayName("VoglanderServerConfigCommand envelope 改造")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VoglanderServerConfigCommandEnvelopeTest {

    private static final String          DEVICE_ID                = "34020000001320000001";
    private static final String          CALL_ID                  = "config-corr-id";
    private static final String          NODE_ID                  = "node-1";
    private static final String          TYPE_BASIC_PARAM         = "gb28181.Config.BasicParam";
    private static final String          TYPE_CONFIG_DOWNLOAD     = "gb28181.Config.ConfigDownload";
    private static final String          TYPE_REBOOT              = "gb28181.Control.Reboot";

    @Mock
    private CommandHandlerRegistry       registry;

    @Mock
    private CommandHandler               handler;

    @InjectMocks
    private VoglanderServerConfigCommand command;

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
    @DisplayName("configDevice 完整参数 → BasicParam payload 4 个字段")
    public void configDeviceFullParams() {
        ResultDTO<Void> result = command.configDevice(DEVICE_ID, "MyCam", "7200", "60", "3");
        assertNotNull(result);
        assertTrue(result.isSuccess());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_BASIC_PARAM));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        Map<String, Object> p = cmd.payload();
        assertEquals(DEVICE_ID, cmd.deviceId());
        assertEquals("MyCam", p.get("name"));
        assertEquals("7200", p.get("expiration"));
        assertEquals("60", p.get("heartBeatInterval"));
        assertEquals("3", p.get("heartBeatCount"));

    }

    @Test
    @DisplayName("configDevice(deviceId, name) → 默认 expiration=3600, heartBeat 60/3")
    public void configDeviceDefaultParams() {
        command.configDevice(DEVICE_ID, "MyCam");

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        Map<String, Object> p = captor.getValue().payload();
        assertEquals("3600", p.get("expiration"));
        assertEquals("60", p.get("heartBeatInterval"));
        assertEquals("3", p.get("heartBeatCount"));
    }

    @Test
    @DisplayName("downloadDeviceConfig → ConfigDownload payload {configType}")
    public void downloadDeviceConfigDispatchesEnvelope() {
        command.downloadDeviceConfig(DEVICE_ID, "VideoParamOpt");

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_CONFIG_DOWNLOAD));
        verify(handler).handle(captor.capture());
        assertEquals("VideoParamOpt", captor.getValue().payload().get("configType"));
    }

    @Test
    @DisplayName("downloadBasicConfig → configType=BasicParam")
    public void downloadBasicConfig() {
        command.downloadBasicConfig(DEVICE_ID);
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals("BasicParam", captor.getValue().payload().get("configType"));
    }

    @Test
    @DisplayName("rebootDevice → gb28181.Control.Reboot payload 为空")
    public void rebootDeviceDispatchesEnvelope() {
        command.rebootDevice(DEVICE_ID);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_REBOOT));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals(DEVICE_ID, cmd.deviceId());
        assertTrue(cmd.payload() == null || cmd.payload().isEmpty(), "payload 应为空");
    }

    @Test
    @DisplayName("configHighFrequencyDevice → heartBeatInterval=30, heartBeatCount=5")
    public void configHighFrequencyDevice() {
        command.configHighFrequencyDevice(DEVICE_ID, "MyCam");
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals("30", captor.getValue().payload().get("heartBeatInterval"));
        assertEquals("5", captor.getValue().payload().get("heartBeatCount"));
    }

    @Test
    @DisplayName("configLowFrequencyDevice → expiration=7200, heartBeatInterval=120, count=2")
    public void configLowFrequencyDevice() {
        command.configLowFrequencyDevice(DEVICE_ID, "MyCam");
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        Map<String, Object> p = captor.getValue().payload();
        assertEquals("7200", p.get("expiration"));
        assertEquals("120", p.get("heartBeatInterval"));
        assertEquals("2", p.get("heartBeatCount"));
    }

    @Test
    @DisplayName("空 deviceId → 不触发 envelope")
    public void emptyDeviceIdRejected() {
        try {
            command.rebootDevice("");
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
    }
}
