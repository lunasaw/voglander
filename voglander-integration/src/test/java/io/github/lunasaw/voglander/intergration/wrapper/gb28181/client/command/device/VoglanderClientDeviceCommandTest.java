package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import com.luna.common.dto.constant.ResultCode;

import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;

/**
 * GB28181客户端设备指令测试类
 * 
 * @author luna
 * @since 2025/8/2
 */
@ExtendWith(MockitoExtension.class)
class VoglanderClientDeviceCommandTest {

    @Mock
    private ClientDeviceSupplier         clientDeviceSupplier;

    @Mock
    private FromDevice                   fromDevice;

    @Mock
    private ToDevice                     toDevice;

    @InjectMocks
    private VoglanderClientDeviceCommand deviceCommand;

    private static final String          TEST_DEVICE_ID = "34020000001320000001";
    private static final String          TEST_CALL_ID   = "test-call-id-123";

    @BeforeEach
    void setUp() {
        when(clientDeviceSupplier.getClientFromDevice()).thenReturn(fromDevice);
        when(clientDeviceSupplier.getToDevice(TEST_DEVICE_ID)).thenReturn(toDevice);
    }

    @Test
    void sendDeviceInfoCommand_Success() {
        // Given
        DeviceInfo deviceInfo = createTestDeviceInfo();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceInfoCommand(fromDevice, toDevice, deviceInfo))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = deviceCommand.sendDeviceInfoCommand(TEST_DEVICE_ID, deviceInfo);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendDeviceInfoCommand(fromDevice, toDevice, deviceInfo));
        }
    }

    @Test
    void sendDeviceInfoCommand_NullDeviceId_ThrowsException() {
        // Given
        DeviceInfo deviceInfo = createTestDeviceInfo();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendDeviceInfoCommand(null, deviceInfo));
        assertEquals("发送设备信息指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceInfoCommand_EmptyDeviceId_ThrowsException() {
        // Given
        DeviceInfo deviceInfo = createTestDeviceInfo();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendDeviceInfoCommand("", deviceInfo));
        assertEquals("发送设备信息指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceInfoCommand_WhitespaceDeviceId_ThrowsException() {
        // Given
        DeviceInfo deviceInfo = createTestDeviceInfo();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendDeviceInfoCommand("   ", deviceInfo));
        assertEquals("发送设备信息指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceInfoCommand_NullDeviceInfo_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendDeviceInfoCommand(TEST_DEVICE_ID, null));
        assertEquals("设备信息不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceStatusCommand_WithStringStatus_Success() {
        // Given
        String status = "ONLINE";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, status))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = deviceCommand.sendDeviceStatusCommand(TEST_DEVICE_ID, status);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, status));
        }
    }

    @Test
    void sendDeviceStatusCommand_WithStringStatus_NullDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendDeviceStatusCommand(null, "ONLINE"));
        assertEquals("发送设备状态指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceStatusCommand_WithStringStatus_NullStatus_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendDeviceStatusCommand(TEST_DEVICE_ID, (String)null));
        assertEquals("设备状态不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceStatusCommand_WithStringStatus_ValidStatuses() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceStatusCommand(any(), any(), anyString()))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // Test ONLINE status
            ResultDTO<Void> result1 = deviceCommand.sendDeviceStatusCommand(TEST_DEVICE_ID, "ONLINE");
            assertTrue(result1.isSuccess());

            // Test OFFLINE status
            ResultDTO<Void> result2 = deviceCommand.sendDeviceStatusCommand(TEST_DEVICE_ID, "OFFLINE");
            assertTrue(result2.isSuccess());

            // Test custom status
            ResultDTO<Void> result3 = deviceCommand.sendDeviceStatusCommand(TEST_DEVICE_ID, "UNKNOWN");
            assertTrue(result3.isSuccess());
        }
    }

    @Test
    void sendDeviceStatusCommand_WithDeviceStatusObject_Success() {
        // Given
        DeviceStatus deviceStatus = createTestDeviceStatus();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, deviceStatus))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = deviceCommand.sendDeviceStatusCommand(TEST_DEVICE_ID, deviceStatus);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, deviceStatus));
        }
    }

    @Test
    void sendDeviceStatusCommand_WithDeviceStatusObject_NullDeviceStatus_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendDeviceStatusCommand(TEST_DEVICE_ID, (DeviceStatus)null));
        assertEquals("设备状态对象不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceOnlineNotify_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, "ONLINE"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = deviceCommand.sendDeviceOnlineNotify(TEST_DEVICE_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, "ONLINE"));
        }
    }

    @Test
    void sendDeviceOfflineNotify_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, "OFFLINE"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = deviceCommand.sendDeviceOfflineNotify(TEST_DEVICE_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, "OFFLINE"));
        }
    }

    @Test
    void createDeviceInfo_WithAllParameters_Success() {
        // Given
        String deviceName = "测试摄像头";
        String manufacturer = "海康威视";
        String model = "DS-2CD2T46DWD-I5";
        String firmware = "V5.6.3";

        // When
        DeviceInfo result = deviceCommand.createDeviceInfo(TEST_DEVICE_ID, deviceName, manufacturer, model, firmware);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(deviceName, result.getDeviceName());
        assertEquals(manufacturer, result.getManufacturer());
        assertEquals(model, result.getModel());
        assertEquals(firmware, result.getFirmware());
    }

    @Test
    void createDeviceInfo_WithNullParameters_Success() {
        // When
        DeviceInfo result = deviceCommand.createDeviceInfo(TEST_DEVICE_ID, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertNull(result.getDeviceName());
        assertNull(result.getManufacturer());
        assertNull(result.getModel());
        assertNull(result.getFirmware());
    }

    @Test
    void createDeviceInfo_WithEmptyStrings_Success() {
        // When
        DeviceInfo result = deviceCommand.createDeviceInfo(TEST_DEVICE_ID, "", "", "", "");

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals("", result.getDeviceName());
        assertEquals("", result.getManufacturer());
        assertEquals("", result.getModel());
        assertEquals("", result.getFirmware());
    }

    @Test
    void sendSimpleDeviceInfoCommand_Success() {
        // Given
        String deviceName = "简单摄像头";
        String manufacturer = "大华";
        String model = "DH-IPC-HFW2431S-S";
        String firmware = "2.800.0000000.25.R";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceInfoCommand(eq(fromDevice), eq(toDevice), any(DeviceInfo.class)))
                .thenAnswer(invocation -> {
                    DeviceInfo info = invocation.getArgument(2);
                    assertEquals(TEST_DEVICE_ID, info.getDeviceId());
                    assertEquals(deviceName, info.getDeviceName());
                    assertEquals(manufacturer, info.getManufacturer());
                    assertEquals(model, info.getModel());
                    assertEquals(firmware, info.getFirmware());
                    return TEST_CALL_ID;
                });
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = deviceCommand.sendSimpleDeviceInfoCommand(TEST_DEVICE_ID, deviceName, manufacturer, model, firmware);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendDeviceInfoCommand(eq(fromDevice), eq(toDevice), any(DeviceInfo.class)));
        }
    }

    @Test
    void sendSimpleDeviceInfoCommand_WithNullParameters_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceInfoCommand(eq(fromDevice), eq(toDevice), any(DeviceInfo.class)))
                .thenAnswer(invocation -> {
                    DeviceInfo info = invocation.getArgument(2);
                    assertEquals(TEST_DEVICE_ID, info.getDeviceId());
                    assertNull(info.getDeviceName());
                    assertNull(info.getManufacturer());
                    assertNull(info.getModel());
                    assertNull(info.getFirmware());
                    return TEST_CALL_ID;
                });
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = deviceCommand.sendSimpleDeviceInfoCommand(TEST_DEVICE_ID, null, null, null, null);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void sendDeviceInfoCommand_ClientCommandSenderThrowsException_ReturnsFailure() {
        // Given
        DeviceInfo deviceInfo = createTestDeviceInfo();
        Exception testException = new RuntimeException("发送失败");

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceInfoCommand(fromDevice, toDevice, deviceInfo))
                .thenThrow(testException);
            mockedUtils.when(() -> ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "发送失败"))
                .thenReturn(ResultDTO.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "发送失败"));

            // When
            ResultDTO<Void> result = deviceCommand.sendDeviceInfoCommand(TEST_DEVICE_ID, deviceInfo);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(ResultCode.ERROR_SYSTEM_EXCEPTION, result.getCode());
            assertEquals("发送失败", result.getMsg());
        }
    }

    @Test
    void sendDeviceStatusCommand_ClientCommandSenderThrowsException_ReturnsFailure() {
        // Given
        Exception testException = new RuntimeException("状态发送失败");

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, "ONLINE"))
                .thenThrow(testException);
            mockedUtils.when(() -> ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "状态发送失败"))
                .thenReturn(ResultDTO.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "状态发送失败"));

            // When
            ResultDTO<Void> result = deviceCommand.sendDeviceStatusCommand(TEST_DEVICE_ID, "ONLINE");

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(ResultCode.ERROR_SYSTEM_EXCEPTION, result.getCode());
            assertEquals("状态发送失败", result.getMsg());
        }
    }

    @Test
    void sendDeviceOnlineNotify_NullDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendDeviceOnlineNotify(null));
        assertEquals("发送设备状态指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceOfflineNotify_EmptyDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendDeviceOfflineNotify(""));
        assertEquals("发送设备状态指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendSimpleDeviceInfoCommand_NullDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> deviceCommand.sendSimpleDeviceInfoCommand(null, "测试", "制造商", "型号", "版本"));
        assertEquals("发送设备信息指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void deviceInfo_ObjectCreationAndPropertyAccess() {
        // Given
        DeviceInfo deviceInfo = new DeviceInfo();

        // When
        deviceInfo.setDeviceId(TEST_DEVICE_ID);
        deviceInfo.setDeviceName("测试设备");
        deviceInfo.setManufacturer("测试制造商");
        deviceInfo.setModel("测试型号");
        deviceInfo.setFirmware("测试版本");

        // Then
        assertEquals(TEST_DEVICE_ID, deviceInfo.getDeviceId());
        assertEquals("测试设备", deviceInfo.getDeviceName());
        assertEquals("测试制造商", deviceInfo.getManufacturer());
        assertEquals("测试型号", deviceInfo.getModel());
        assertEquals("测试版本", deviceInfo.getFirmware());
    }

    @Test
    void deviceStatus_ObjectCreationAndPropertyAccess() {
        // Given
        DeviceStatus deviceStatus = new DeviceStatus();

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            deviceStatus.setDeviceId(TEST_DEVICE_ID);
            deviceStatus.setOnline("ONLINE");
            deviceStatus.setResult("OK");
        });
    }

    private DeviceInfo createTestDeviceInfo() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(TEST_DEVICE_ID);
        deviceInfo.setDeviceName("测试摄像头");
        deviceInfo.setManufacturer("海康威视");
        deviceInfo.setModel("DS-2CD2T46DWD-I5");
        deviceInfo.setFirmware("V5.6.3");
        return deviceInfo;
    }

    private DeviceStatus createTestDeviceStatus() {
        DeviceStatus deviceStatus = new DeviceStatus();
        deviceStatus.setDeviceId(TEST_DEVICE_ID);
        deviceStatus.setOnline("ONLINE");
        deviceStatus.setResult("OK");
        return deviceStatus;
    }
}