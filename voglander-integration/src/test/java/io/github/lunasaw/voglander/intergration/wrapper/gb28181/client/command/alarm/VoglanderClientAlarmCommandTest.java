package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;

/**
 * VoglanderClientAlarmCommand 单元测试
 * 
 * @author luna
 * @since 2025/8/1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoglanderClientAlarmCommandTest {

    @Mock
    private ClientDeviceSupplier        clientDeviceSupplier;

    @Mock
    private FromDevice                  fromDevice;

    @Mock
    private ToDevice                    toDevice;

    private VoglanderClientAlarmCommand alarmCommand;

    private static final String         TEST_DEVICE_ID = "34020000001320000001";
    private static final String         TEST_CALL_ID   = "test-call-id";

    @BeforeEach
    void setUp() {
        alarmCommand = new VoglanderClientAlarmCommand();
        alarmCommand.clientDeviceSupplier = clientDeviceSupplier;

        when(clientDeviceSupplier.getClientFromDevice()).thenReturn(fromDevice);
        when(clientDeviceSupplier.getToDevice(TEST_DEVICE_ID)).thenReturn(toDevice);
    }

    @Test
    void testSendAlarmCommandSuccess() {
        DeviceAlarm deviceAlarm = createTestDeviceAlarm();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class)) {
            mockedSender.when(() -> ClientCommandSender.sendAlarmCommand(fromDevice, toDevice, deviceAlarm))
                .thenReturn(TEST_CALL_ID);

            ResultDTO<Void> result = alarmCommand.sendAlarmCommand(TEST_DEVICE_ID, deviceAlarm);

            assertTrue(result.isSuccess());
            assertNull(result.getData());

            mockedSender.verify(() -> ClientCommandSender.sendAlarmCommand(fromDevice, toDevice, deviceAlarm));
            verify(clientDeviceSupplier).getClientFromDevice();
            verify(clientDeviceSupplier).getToDevice(TEST_DEVICE_ID);
        }
    }

    @Test
    void testSendAlarmCommandNullDeviceId() {
        DeviceAlarm deviceAlarm = createTestDeviceAlarm();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> alarmCommand.sendAlarmCommand(null, deviceAlarm));

        assertEquals("发送告警指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void testSendAlarmCommandEmptyDeviceId() {
        DeviceAlarm deviceAlarm = createTestDeviceAlarm();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> alarmCommand.sendAlarmCommand("", deviceAlarm));

        assertEquals("发送告警指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void testSendAlarmCommandNullDeviceAlarm() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> alarmCommand.sendAlarmCommand(TEST_DEVICE_ID, null));

        assertEquals("告警信息不能为空", exception.getMessage());
    }

    @Test
    void testSendAlarmCommandException() {
        DeviceAlarm deviceAlarm = createTestDeviceAlarm();
        String errorMessage = "发送失败";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class)) {
            mockedSender.when(() -> ClientCommandSender.sendAlarmCommand(fromDevice, toDevice, deviceAlarm))
                .thenThrow(new RuntimeException(errorMessage));

            ResultDTO<Void> result = alarmCommand.sendAlarmCommand(TEST_DEVICE_ID, deviceAlarm);

            assertFalse(result.isSuccess());
            assertEquals(errorMessage, result.getMessage());
        }
    }

    @Test
    void testSendAlarmNotifyCommandSuccess() {
        DeviceAlarmNotify deviceAlarmNotify = createTestDeviceAlarmNotify();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class)) {
            mockedSender.when(() -> ClientCommandSender.sendAlarmCommand(fromDevice, toDevice, deviceAlarmNotify))
                .thenReturn(TEST_CALL_ID);

            ResultDTO<Void> result = alarmCommand.sendAlarmNotifyCommand(TEST_DEVICE_ID, deviceAlarmNotify);

            assertTrue(result.isSuccess());
            assertNull(result.getData());

            mockedSender.verify(() -> ClientCommandSender.sendAlarmCommand(fromDevice, toDevice, deviceAlarmNotify));
        }
    }

    @Test
    void testSendAlarmNotifyCommandNullDeviceId() {
        DeviceAlarmNotify deviceAlarmNotify = createTestDeviceAlarmNotify();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> alarmCommand.sendAlarmNotifyCommand(null, deviceAlarmNotify));

        assertEquals("发送告警通知指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void testSendAlarmNotifyCommandNullNotify() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> alarmCommand.sendAlarmNotifyCommand(TEST_DEVICE_ID, null));

        assertEquals("告警通知信息不能为空", exception.getMessage());
    }

    @Test
    void testSendSimpleAlarmCommandSuccess() {
        String alarmType = "1";
        String alarmPriority = "3";
        String alarmMethod = "2";
        String alarmDescription = "测试告警";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class)) {
            mockedSender.when(() -> ClientCommandSender.sendAlarmCommand(eq(fromDevice), eq(toDevice), any(DeviceAlarm.class)))
                .thenReturn(TEST_CALL_ID);

            ResultDTO<Void> result = alarmCommand.sendSimpleAlarmCommand(TEST_DEVICE_ID, alarmType, alarmPriority, alarmMethod, alarmDescription);

            assertTrue(result.isSuccess());
            assertNull(result.getData());

            mockedSender.verify(() -> ClientCommandSender.sendAlarmCommand(eq(fromDevice), eq(toDevice), any(DeviceAlarm.class)));
        }
    }

    @Test
    void testSendSimpleAlarmCommandWithDefaults() {
        String alarmType = "1";
        String alarmPriority = "3";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class)) {
            mockedSender.when(() -> ClientCommandSender.sendAlarmCommand(eq(fromDevice), eq(toDevice), any(DeviceAlarm.class)))
                .thenReturn(TEST_CALL_ID);

            ResultDTO<Void> result = alarmCommand.sendSimpleAlarmCommand(TEST_DEVICE_ID, alarmType, alarmPriority, null, null);

            assertTrue(result.isSuccess());

            mockedSender.verify(() -> ClientCommandSender.sendAlarmCommand(eq(fromDevice), eq(toDevice), any(DeviceAlarm.class)));
        }
    }

    @Test
    void testSendSimpleAlarmCommandNullAlarmType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> alarmCommand.sendSimpleAlarmCommand(TEST_DEVICE_ID, null, "3", "1", "描述"));

        assertEquals("告警类型不能为空", exception.getMessage());
    }

    @Test
    void testSendSimpleAlarmCommandNullAlarmPriority() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> alarmCommand.sendSimpleAlarmCommand(TEST_DEVICE_ID, "1", null, "1", "描述"));

        assertEquals("告警级别不能为空", exception.getMessage());
    }

    private DeviceAlarm createTestDeviceAlarm() {
        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setDeviceId(TEST_DEVICE_ID);
        deviceAlarm.setAlarmType("1");
        deviceAlarm.setAlarmPriority("3");
        deviceAlarm.setAlarmMethod("1");
        deviceAlarm.setAlarmDescription("测试告警");
        deviceAlarm.setAlarmTime(new Date());
        return deviceAlarm;
    }

    private DeviceAlarmNotify createTestDeviceAlarmNotify() {
        DeviceAlarmNotify deviceAlarmNotify = new DeviceAlarmNotify();
        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setAlarmType("1");
        deviceAlarm.setAlarmTime(new Date()); // 修复空指针问题
        deviceAlarmNotify.setAlarm(deviceAlarm);
        deviceAlarmNotify.setAlarmPriority("3");
        return deviceAlarmNotify;
    }
}