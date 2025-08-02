package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;

/**
 * GB28181客户端状态指令测试类
 * 
 * @author luna
 * @since 2025/8/2
 */
@ExtendWith(MockitoExtension.class)
class VoglanderClientStatusCommandTest {

    @Mock
    private ClientDeviceSupplier         clientDeviceSupplier;

    @Mock
    private FromDevice                   fromDevice;

    @Mock
    private ToDevice                     toDevice;

    @InjectMocks
    private VoglanderClientStatusCommand statusCommand;

    private static final String          TEST_DEVICE_ID   = "34020000001320000001";
    private static final String          TEST_DEVICE_ID_2 = "34020000001320000002";
    private static final String          TEST_CALL_ID     = "test-call-id-123";

    @BeforeEach
    void setUp() {
        when(clientDeviceSupplier.getClientFromDevice()).thenReturn(fromDevice);
        when(clientDeviceSupplier.getToDevice(anyString())).thenReturn(toDevice);
    }

    @Test
    void sendKeepaliveCommand_WithStringStatus_Success() {
        // Given
        String status = "OK";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, status))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendKeepaliveCommand(TEST_DEVICE_ID, status);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, status));
        }
    }

    @Test
    void sendKeepaliveCommand_WithNullStatus_UsesDefault() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "OK"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendKeepaliveCommand(TEST_DEVICE_ID, null);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "OK"));
        }
    }

    @Test
    void sendKeepaliveCommand_WithStringStatus_NullDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> statusCommand.sendKeepaliveCommand(null, "OK"));
        assertEquals("发送心跳保活指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendKeepaliveCommand_WithDeviceKeepLiveNotify_Success() {
        // Given
        DeviceKeepLiveNotify keepLiveNotify = createTestDeviceKeepLiveNotify();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, keepLiveNotify))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendKeepaliveCommand(TEST_DEVICE_ID, keepLiveNotify);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, keepLiveNotify));
        }
    }

    @Test
    void sendKeepaliveCommand_WithDeviceKeepLiveNotify_NullNotify_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> statusCommand.sendKeepaliveCommand(TEST_DEVICE_ID, (DeviceKeepLiveNotify)null));
        assertEquals("心跳保活通知对象不能为空", exception.getMessage());
    }

    @Test
    void sendMobilePositionCommand_Success() {
        // Given
        MobilePositionNotify positionNotify = createTestMobilePositionNotify();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendMobilePositionNotify(fromDevice, toDevice, positionNotify))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendMobilePositionCommand(TEST_DEVICE_ID, positionNotify);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendMobilePositionNotify(fromDevice, toDevice, positionNotify));
        }
    }

    @Test
    void sendMobilePositionCommand_NullDeviceId_ThrowsException() {
        // Given
        MobilePositionNotify positionNotify = createTestMobilePositionNotify();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> statusCommand.sendMobilePositionCommand(null, positionNotify));
        assertEquals("发送位置信息指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendMobilePositionCommand_NullPositionNotify_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> statusCommand.sendMobilePositionCommand(TEST_DEVICE_ID, null));
        assertEquals("位置通知对象不能为空", exception.getMessage());
    }

    @Test
    void sendMediaStatusCommand_Success() {
        // Given
        String notifyType = "121";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, notifyType))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendMediaStatusCommand(TEST_DEVICE_ID, notifyType);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, notifyType));
        }
    }

    @Test
    void sendMediaStatusCommand_WithNullNotifyType_UsesDefault() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, "121"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendMediaStatusCommand(TEST_DEVICE_ID, null);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, "121"));
        }
    }

    @Test
    void sendMediaStatusCommand_NullDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> statusCommand.sendMediaStatusCommand(null, "121"));
        assertEquals("发送媒体状态指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void createMobilePositionNotify_WithAllParameters_Success() {
        // Given
        Double longitude = 116.397128;
        Double latitude = 39.916527;
        String time = "2025-01-01T12:00:00";
        Double speed = 60.5;
        Double direction = 180.0;
        Double altitude = 100.0;

        // When
        MobilePositionNotify result = statusCommand.createMobilePositionNotify(TEST_DEVICE_ID, longitude, latitude,
            time, speed, direction, altitude);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(longitude.toString(), result.getLongitude());
        assertEquals(latitude.toString(), result.getLatitude());
        assertEquals(time, result.getTime());
        assertEquals(speed.toString(), result.getSpeed());
        assertEquals(direction.toString(), result.getDirection());
        assertEquals(altitude.toString(), result.getAltitude());
    }

    @Test
    void createMobilePositionNotify_WithNullParameters_Success() {
        // When
        MobilePositionNotify result = statusCommand.createMobilePositionNotify(TEST_DEVICE_ID, null, null,
            null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertNull(result.getLongitude());
        assertNull(result.getLatitude());
        assertNull(result.getTime());
        assertNull(result.getSpeed());
        assertNull(result.getDirection());
        assertNull(result.getAltitude());
    }

    @Test
    void createSimplePositionNotify_Success() {
        // Given
        Double longitude = 116.397128;
        Double latitude = 39.916527;

        // When
        MobilePositionNotify result = statusCommand.createSimplePositionNotify(TEST_DEVICE_ID, longitude, latitude);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(longitude.toString(), result.getLongitude());
        assertEquals(latitude.toString(), result.getLatitude());
        assertNull(result.getTime());
        assertNull(result.getSpeed());
        assertNull(result.getDirection());
        assertNull(result.getAltitude());
    }

    @Test
    void sendSimplePositionCommand_Success() {
        // Given
        Double longitude = 116.397128;
        Double latitude = 39.916527;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendMobilePositionNotify(eq(fromDevice), eq(toDevice), any(MobilePositionNotify.class)))
                .thenAnswer(invocation -> {
                    MobilePositionNotify notify = invocation.getArgument(2);
                    assertEquals(TEST_DEVICE_ID, notify.getDeviceId());
                    assertEquals(longitude.toString(), notify.getLongitude());
                    assertEquals(latitude.toString(), notify.getLatitude());
                    return TEST_CALL_ID;
                });
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendSimplePositionCommand(TEST_DEVICE_ID, longitude, latitude);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void sendSimplePositionCommand_NullLongitude_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> statusCommand.sendSimplePositionCommand(TEST_DEVICE_ID, null, 39.916527));
        assertEquals("经度不能为空", exception.getMessage());
    }

    @Test
    void sendSimplePositionCommand_NullLatitude_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> statusCommand.sendSimplePositionCommand(TEST_DEVICE_ID, 116.397128, null));
        assertEquals("纬度不能为空", exception.getMessage());
    }

    @Test
    void sendNormalKeepalive_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "OK"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendNormalKeepalive(TEST_DEVICE_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "OK"));
        }
    }

    @Test
    void sendErrorKeepalive_WithStatus_Success() {
        // Given
        String errorStatus = "ERROR_CONNECTION";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, errorStatus))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendErrorKeepalive(TEST_DEVICE_ID, errorStatus);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, errorStatus));
        }
    }

    @Test
    void sendErrorKeepalive_WithNullStatus_UsesDefault() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "ERROR"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendErrorKeepalive(TEST_DEVICE_ID, null);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "ERROR"));
        }
    }

    @Test
    void sendMediaStartNotify_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, "121"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendMediaStartNotify(TEST_DEVICE_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, "121"));
        }
    }

    @Test
    void sendMediaStopNotify_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, "122"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendMediaStopNotify(TEST_DEVICE_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, "122"));
        }
    }

    @Test
    void sendBatchKeepaliveCommand_Success() {
        // Given
        List<String> deviceIds = Arrays.asList(TEST_DEVICE_ID, TEST_DEVICE_ID_2);
        String status = "OK";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendKeepaliveCommand(any(FromDevice.class), any(ToDevice.class), eq(status)))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendBatchKeepaliveCommand(deviceIds, status);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendKeepaliveCommand(any(FromDevice.class), any(ToDevice.class), eq(status)),
                times(deviceIds.size()));
        }
    }

    @Test
    void sendBatchKeepaliveCommand_NullDeviceIds_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> statusCommand.sendBatchKeepaliveCommand(null, "OK"));
        assertEquals("设备ID列表不能为空", exception.getMessage());
    }

    @Test
    void sendBatchKeepaliveCommand_EmptyDeviceIds_Success() {
        // Given
        List<String> emptyList = Collections.emptyList();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendBatchKeepaliveCommand(emptyList, "OK");

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verifyNoInteractions();
        }
    }

    @Test
    void sendBatchKeepaliveCommand_WithNullStatus_UsesDefault() {
        // Given
        List<String> deviceIds = Arrays.asList(TEST_DEVICE_ID);

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendKeepaliveCommand(any(FromDevice.class), any(ToDevice.class), eq("OK")))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // When
            ResultDTO<Void> result = statusCommand.sendBatchKeepaliveCommand(deviceIds, null);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendKeepaliveCommand(any(FromDevice.class), any(ToDevice.class), eq("OK")));
        }
    }

    @Test
    void sendKeepaliveCommand_ClientCommandSenderThrowsException_ReturnsFailure() {
        // Given
        Exception testException = new RuntimeException("心跳发送失败");

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "OK"))
                .thenThrow(testException);
            mockedUtils.when(() -> ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "心跳发送失败"))
                .thenReturn(ResultDTO.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "心跳发送失败"));

            // When
            ResultDTO<Void> result = statusCommand.sendKeepaliveCommand(TEST_DEVICE_ID, "OK");

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(ResultCode.ERROR_SYSTEM_EXCEPTION, result.getCode());
            assertEquals("心跳发送失败", result.getMsg());
        }
    }

    @Test
    void coordinateConversion_DoubleToString_Success() {
        // Test coordinate precision handling
        Double preciseLongitude = 116.39712812345678;
        Double preciseLatitude = 39.91652756789012;

        MobilePositionNotify notify = statusCommand.createSimplePositionNotify(TEST_DEVICE_ID, preciseLongitude, preciseLatitude);

        assertEquals(preciseLongitude.toString(), notify.getLongitude());
        assertEquals(preciseLatitude.toString(), notify.getLatitude());
    }

    @Test
    void mediaStatusTypes_DifferentNotifyTypes_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendMediaStatusCommand(any(), any(), anyString()))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTO.success());

            // Test various media status types
            assertDoesNotThrow(() -> statusCommand.sendMediaStatusCommand(TEST_DEVICE_ID, "120"));
            assertDoesNotThrow(() -> statusCommand.sendMediaStatusCommand(TEST_DEVICE_ID, "121"));
            assertDoesNotThrow(() -> statusCommand.sendMediaStatusCommand(TEST_DEVICE_ID, "122"));
            assertDoesNotThrow(() -> statusCommand.sendMediaStatusCommand(TEST_DEVICE_ID, "123"));
        }
    }

    private DeviceKeepLiveNotify createTestDeviceKeepLiveNotify() {
        DeviceKeepLiveNotify notify = new DeviceKeepLiveNotify();
        notify.setDeviceId(TEST_DEVICE_ID);
        notify.setStatus("OK");
        notify.setResult("Success");
        return notify;
    }

    private MobilePositionNotify createTestMobilePositionNotify() {
        MobilePositionNotify notify = new MobilePositionNotify();
        notify.setDeviceId(TEST_DEVICE_ID);
        notify.setLongitude("116.397128");
        notify.setLatitude("39.916527");
        notify.setTime("2025-01-01T12:00:00");
        notify.setSpeed("60.5");
        notify.setDirection("180.0");
        notify.setAltitude("100.0");
        return notify;
    }
}