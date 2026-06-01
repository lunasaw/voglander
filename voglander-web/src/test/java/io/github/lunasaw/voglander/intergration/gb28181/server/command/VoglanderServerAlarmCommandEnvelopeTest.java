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

import java.util.Date;
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

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.sipgateway.core.api.CommandHandler;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm.VoglanderServerAlarmCommand;

/**
 * VoglanderServerAlarmCommand envelope 改造单元测试
 *
 * <p>
 * Schema：
 * <ul>
 * <li>{@code gb28181.Query.AlarmQuery}（白名单）：{@code {startTime: long, endTime: long,
 * startPriority?: String, endPriority?: String, alarmMethod?: String, alarmType?: String}}</li>
 * <li>{@code gb28181.Control.AlarmReset}（白名单）：{@code {alarmMethod?: String, alarmType?: String}}</li>
 * </ul>
 * </p>
 *
 * @author luna
 * @since 2026/06/01
 */
@DisplayName("VoglanderServerAlarmCommand envelope 改造")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VoglanderServerAlarmCommandEnvelopeTest {

    private static final String         DEVICE_ID         = "34020000001320000001";
    private static final String         CALL_ID           = "alarm-corr-id";
    private static final String         NODE_ID           = "node-1";
    private static final String         TYPE_ALARM_QUERY  = "gb28181.Query.AlarmQuery";
    private static final String         TYPE_ALARM_RESET  = "gb28181.Control.AlarmReset";

    @Mock
    private CommandHandlerRegistry      registry;

    @Mock
    private CommandHandler              handler;

    @Mock
    private ServerCommandSender         serverCommandSender;

    @InjectMocks
    private VoglanderServerAlarmCommand command;

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
    @DisplayName("queryDeviceAlarm 完整参数 → AlarmQuery payload 6 字段")
    public void queryDeviceAlarmFullParams() {
        Date start = new Date(1700000000000L);
        Date end = new Date(1700003600000L);
        ResultDTO<Void> result = command.queryDeviceAlarm(DEVICE_ID, start, end, "1", "4", "5", "1");
        assertNotNull(result);
        assertTrue(result.isSuccess());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_ALARM_QUERY));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        Map<String, Object> p = cmd.payload();
        assertEquals(DEVICE_ID, cmd.deviceId());
        // 时间字段必须 Number 类型（毫秒）
        assertTrue(p.get("startTime") instanceof Number);
        assertTrue(p.get("endTime") instanceof Number);
        assertEquals(start.getTime(), ((Number) p.get("startTime")).longValue());
        assertEquals(end.getTime(), ((Number) p.get("endTime")).longValue());
        assertEquals("1", p.get("startPriority"));
        assertEquals("4", p.get("endPriority"));
        assertEquals("5", p.get("alarmMethod"));
        assertEquals("1", p.get("alarmType"));

        verifyNoInteractions(serverCommandSender);
    }

    @Test
    @DisplayName("controlDeviceAlarm → AlarmReset payload {alarmMethod, alarmType}")
    public void controlDeviceAlarmDispatchesAlarmReset() {
        command.controlDeviceAlarm(DEVICE_ID, "5", "1");

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_ALARM_RESET));
        verify(handler).handle(captor.capture());

        Map<String, Object> p = captor.getValue().payload();
        assertEquals("5", p.get("alarmMethod"));
        assertEquals("1", p.get("alarmType"));
        verifyNoInteractions(serverCommandSender);
    }

    @Test
    @DisplayName("queryEmergencyDeviceAlarm → priority 1-1")
    public void queryEmergency() {
        command.queryEmergencyDeviceAlarm(DEVICE_ID, new Date(1L), new Date(2L));
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals("1", captor.getValue().payload().get("startPriority"));
        assertEquals("1", captor.getValue().payload().get("endPriority"));
    }

    @Test
    @DisplayName("queryVideoLossAlarm → alarmType=1")
    public void queryVideoLoss() {
        command.queryVideoLossAlarm(DEVICE_ID, new Date(1L), new Date(2L));
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals("1", captor.getValue().payload().get("alarmType"));
    }

    @Test
    @DisplayName("enableNetworkAlarm(deviceId, alarmType) → controlDeviceAlarm(method=2)")
    public void enableNetworkAlarm() {
        command.enableNetworkAlarm(DEVICE_ID, "1");
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_ALARM_RESET));
        verify(handler).handle(captor.capture());
        assertEquals("2", captor.getValue().payload().get("alarmMethod"));
        assertEquals("1", captor.getValue().payload().get("alarmType"));
    }

    @Test
    @DisplayName("queryRecentDeviceAlarm(hours<=0) → IllegalArgumentException")
    public void recentAlarmRejectsZeroHours() {
        try {
            command.queryRecentDeviceAlarm(DEVICE_ID, 0);
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
    }

    @Test
    @DisplayName("空 deviceId → 不触发 envelope")
    public void emptyDeviceIdRejected() {
        try {
            command.queryDeviceAlarm("", new Date(1L), new Date(2L), null, null, null, null);
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
    }
}
