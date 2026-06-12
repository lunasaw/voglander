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

import java.util.Date;

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
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;

/**
 * VoglanderServerRecordCommand envelope 改造单元测试
 *
 * <p>
 * Schema：
 * <ul>
 * <li>{@code gb28181.Query.RecordInfo}（白名单）：{@code {startTime: long, endTime: long}}（Number 类型）</li>
 * <li>{@code gb28181.Control.Record}��declare 表）：{@code {recordCmd: String}}</li>
 * </ul>
 * </p>
 *
 * @author luna
 * @since 2026/06/01
 */
@DisplayName("VoglanderServerRecordCommand envelope 改造")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VoglanderServerRecordCommandEnvelopeTest {

    private static final String          DEVICE_ID         = "34020000001320000001";
    private static final String          CALL_ID           = "record-corr-id";
    private static final String          NODE_ID           = "node-1";
    private static final String          TYPE_RECORD_INFO  = "gb28181.Query.RecordInfo";
    private static final String          TYPE_CTRL_RECORD  = "gb28181.Control.Record";

    @Mock
    private CommandHandlerRegistry       registry;

    @Mock
    private CommandHandler               handler;

    @InjectMocks
    private VoglanderServerRecordCommand command;

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
    @DisplayName("queryDeviceRecord(long, long) → payload {startTime:long, endTime:long}")
    public void queryDeviceRecordLongTimestamps() {
        long start = 1700000000000L;
        long end = 1700003600000L;
        ResultDTO<Void> result = command.queryDeviceRecord(DEVICE_ID, start, end);

        assertNotNull(result);
        assertTrue(result.isSuccess());

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_RECORD_INFO));
        verify(handler).handle(captor.capture());

        GatewayCommand cmd = captor.getValue();
        assertEquals(TYPE_RECORD_INFO, cmd.type());
        assertEquals(DEVICE_ID, cmd.deviceId());
        // payload 字段必须是 Number 类型，不是 String
        Object st = cmd.payload().get("startTime");
        Object et = cmd.payload().get("endTime");
        assertTrue(st instanceof Number, "startTime 必须是 Number 类型");
        assertTrue(et instanceof Number, "endTime 必须是 Number 类型");
        assertEquals(start, ((Number) st).longValue());
        assertEquals(end, ((Number) et).longValue());

    }

    @Test
    @DisplayName("queryDeviceRecord(Date, Date) → 转 long 后下发")
    public void queryDeviceRecordDateOverload() {
        Date start = new Date(1700000000000L);
        Date end = new Date(1700003600000L);
        command.queryDeviceRecord(DEVICE_ID, start, end);

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals(start.getTime(), ((Number) captor.getValue().payload().get("startTime")).longValue());
        assertEquals(end.getTime(), ((Number) captor.getValue().payload().get("endTime")).longValue());
    }

    @Test
    @DisplayName("queryDeviceRecord(String, String, String) → ISO 时间字符串转 long 后下发")
    public void queryDeviceRecordStringTimes() {
        // ISO 格式：yyyy-MM-ddTHH:mm:ss（与现存 API 兼容）。改造后内部统一转 long
        String startIso = "2026-06-01T08:00:00";
        String endIso = "2026-06-01T09:00:00";
        ResultDTO<Void> result = command.queryDeviceRecord(DEVICE_ID, startIso, endIso);

        assertTrue(result.isSuccess());
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        Object st = captor.getValue().payload().get("startTime");
        Object et = captor.getValue().payload().get("endTime");
        assertTrue(st instanceof Number);
        assertTrue(et instanceof Number);
    }

    @Test
    @DisplayName("controlDeviceRecord(deviceId, recordCmd) → gb28181.Control.Record + payload {recordCmd}")
    public void controlDeviceRecordDispatchesEnvelope() {
        command.controlDeviceRecord(DEVICE_ID, "Record");

        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(registry).require(eq(TYPE_CTRL_RECORD));
        verify(handler).handle(captor.capture());

        assertEquals(DEVICE_ID, captor.getValue().deviceId());
        assertEquals("Record", captor.getValue().payload().get("recordCmd"));
    }

    @Test
    @DisplayName("startDeviceRecord → recordCmd=Record")
    public void startDeviceRecordSendsRecord() {
        command.startDeviceRecord(DEVICE_ID);
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals("Record", captor.getValue().payload().get("recordCmd"));
    }

    @Test
    @DisplayName("stopDeviceRecord → recordCmd=StopRecord")
    public void stopDeviceRecordSendsStopRecord() {
        command.stopDeviceRecord(DEVICE_ID);
        ArgumentCaptor<GatewayCommand> captor = ArgumentCaptor.forClass(GatewayCommand.class);
        verify(handler).handle(captor.capture());
        assertEquals("StopRecord", captor.getValue().payload().get("recordCmd"));
    }

    @Test
    @DisplayName("startTime >= endTime → IllegalArgumentException, 不触发 envelope")
    public void rejectsInvalidTimeRange() {
        try {
            command.queryDeviceRecord(DEVICE_ID, 1700003600000L, 1700000000000L);
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
    }

    @Test
    @DisplayName("空 deviceId → 不触发 envelope")
    public void emptyDeviceIdRejected() {
        try {
            command.queryDeviceRecord("", 1L, 2L);
        } catch (IllegalArgumentException ignored) {
        }
        verify(registry, never()).require(anyString());
    }
}
