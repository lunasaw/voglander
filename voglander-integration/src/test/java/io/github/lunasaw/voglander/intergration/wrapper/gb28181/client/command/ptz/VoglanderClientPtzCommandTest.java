package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.ptz;

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

import io.github.lunasaw.gb28181.common.entity.utils.PtzCmdEnum;
import io.github.lunasaw.gb28181.common.entity.utils.PtzUtils;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;

/**
 * GB28181客户端PTZ控制指令测试类
 * 
 * @author luna
 * @since 2025/8/2
 */
@ExtendWith(MockitoExtension.class)
class VoglanderClientPtzCommandTest {

    @Mock
    private ClientDeviceSupplier      clientDeviceSupplier;

    @Mock
    private FromDevice                fromDevice;

    @Mock
    private ToDevice                  toDevice;

    @InjectMocks
    private VoglanderClientPtzCommand ptzCommand;

    private static final String       TEST_DEVICE_ID = "34020000001320000001";
    private static final String       TEST_CALL_ID   = "test-call-id-123";
    private static final String       TEST_PTZ_CMD   = "A50F01010000050000FF";

    @BeforeEach
    void setUp() {
        when(clientDeviceSupplier.getClientFromDevice()).thenReturn(fromDevice);
        when(clientDeviceSupplier.getToDevice(TEST_DEVICE_ID)).thenReturn(toDevice);
    }

    @Test
    void sendPtzControlCommand_WithStringCmd_Success() {
        // Given
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.sendPtzControlCommand(TEST_DEVICE_ID, TEST_PTZ_CMD);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD));
        }
    }

    @Test
    void sendPtzControlCommand_WithStringCmd_NullDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.sendPtzControlCommand(null, TEST_PTZ_CMD));
        assertEquals("发送云台控制指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendPtzControlCommand_WithStringCmd_NullPtzCmd_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.sendPtzControlCommand(TEST_DEVICE_ID, null));
        assertEquals("云台控制命令不能为空", exception.getMessage());
    }

    @Test
    void sendPtzControlCommand_WithEnum_Success() {
        // Given
        int speed = 128;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.UP, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.sendPtzControlCommand(TEST_DEVICE_ID, PtzCmdEnum.UP, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedPtzUtils.verify(() -> PtzUtils.getPtzCmd(PtzCmdEnum.UP, speed));
            mockedSender.verify(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD));
        }
    }

    @Test
    void sendPtzControlCommand_WithEnum_NullEnum_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.sendPtzControlCommand(TEST_DEVICE_ID, null, 128));
        assertEquals("云台控制命令枚举不能为空", exception.getMessage());
    }

    @Test
    void sendPtzControlCommand_WithEnum_SpeedTooLow_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.sendPtzControlCommand(TEST_DEVICE_ID, PtzCmdEnum.UP, 0));
        assertEquals("云台控制速度必须在1-255之间", exception.getMessage());
    }

    @Test
    void sendPtzControlCommand_WithEnum_SpeedTooHigh_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.sendPtzControlCommand(TEST_DEVICE_ID, PtzCmdEnum.UP, 256));
        assertEquals("云台控制速度必须在1-255之间", exception.getMessage());
    }

    @Test
    void sendPtzControlCommand_WithEnum_ValidSpeedBoundaries() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(any(PtzCmdEnum.class), anyInt())).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(any(), any(), any()))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // Test minimum valid speed
            assertDoesNotThrow(() -> ptzCommand.sendPtzControlCommand(TEST_DEVICE_ID, PtzCmdEnum.UP, 1));

            // Test maximum valid speed
            assertDoesNotThrow(() -> ptzCommand.sendPtzControlCommand(TEST_DEVICE_ID, PtzCmdEnum.UP, 255));
        }
    }

    @Test
    void moveUp_Success() {
        // Given
        int speed = 100;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.UP, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.moveUp(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void moveDown_Success() {
        // Given
        int speed = 50;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.DOWN, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.moveDown(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void moveLeft_Success() {
        // Given
        int speed = 75;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.LEFT, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.moveLeft(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void moveRight_Success() {
        // Given
        int speed = 200;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.RIGHT, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.moveRight(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void moveUpLeft_Success() {
        // Given
        int speed = 150;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.UPLEFT, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.moveUpLeft(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void moveUpRight_Success() {
        // Given
        int speed = 80;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.UPRIGHT, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.moveUpRight(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void moveDownLeft_Success() {
        // Given
        int speed = 120;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.DOWNLEFT, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.moveDownLeft(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void moveDownRight_Success() {
        // Given
        int speed = 90;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.DOWNRIGHT, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.moveDownRight(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void zoomIn_Success() {
        // Given
        int speed = 60;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.ZOOMIN, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.zoomIn(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void zoomOut_Success() {
        // Given
        int speed = 40;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.ZOOMOUT, speed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.zoomOut(TEST_DEVICE_ID, speed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void stopMove_Success() {
        // Given
        String stopCmd = "A50F01010000000000FF";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(0, 0, 0, 0)).thenReturn(stopCmd);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, stopCmd))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.stopMove(TEST_DEVICE_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedPtzUtils.verify(() -> PtzUtils.getPtzCmd(0, 0, 0, 0));
        }
    }

    @Test
    void customPtzControl_Success() {
        // Given
        int cmdCode = 1;
        int horizonSpeed = 100;
        int verticalSpeed = 80;
        int zoomSpeed = 60;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(cmdCode, horizonSpeed, verticalSpeed, zoomSpeed))
                .thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.customPtzControl(TEST_DEVICE_ID, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedPtzUtils.verify(() -> PtzUtils.getPtzCmd(cmdCode, horizonSpeed, verticalSpeed, zoomSpeed));
        }
    }

    @Test
    void customPtzControl_NullDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.customPtzControl(null, 1, 100, 100, 100));
        assertEquals("发送自定义云台控制指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void customPtzControl_InvalidHorizonSpeed_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.customPtzControl(TEST_DEVICE_ID, 1, -1, 100, 100));
        assertEquals("云台速度参数必须在0-255之间", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.customPtzControl(TEST_DEVICE_ID, 1, 256, 100, 100));
        assertEquals("云台速度参数必须在0-255之间", exception.getMessage());
    }

    @Test
    void customPtzControl_InvalidVerticalSpeed_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.customPtzControl(TEST_DEVICE_ID, 1, 100, -1, 100));
        assertEquals("云台速度参数必须在0-255之间", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.customPtzControl(TEST_DEVICE_ID, 1, 100, 256, 100));
        assertEquals("云台速度参数必须在0-255之间", exception.getMessage());
    }

    @Test
    void customPtzControl_InvalidZoomSpeed_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.customPtzControl(TEST_DEVICE_ID, 1, 100, 100, -1));
        assertEquals("云台速度参数必须在0-255之间", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
            () -> ptzCommand.customPtzControl(TEST_DEVICE_ID, 1, 100, 100, 256));
        assertEquals("云台速度参数必须在0-255之间", exception.getMessage());
    }

    @Test
    void customPtzControl_ValidBoundaryValues() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(any(), any(), any()))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // Test minimum values
            assertDoesNotThrow(() -> ptzCommand.customPtzControl(TEST_DEVICE_ID, 1, 0, 0, 0));

            // Test maximum values
            assertDoesNotThrow(() -> ptzCommand.customPtzControl(TEST_DEVICE_ID, 1, 255, 255, 255));
        }
    }

    @Test
    void moveWithDefaultSpeed_Success() {
        // Given
        int defaultSpeed = 128;

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<PtzUtils> mockedPtzUtils = mockStatic(PtzUtils.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedPtzUtils.when(() -> PtzUtils.getPtzCmd(PtzCmdEnum.UP, defaultSpeed)).thenReturn(TEST_PTZ_CMD);
            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = ptzCommand.moveWithDefaultSpeed(TEST_DEVICE_ID, PtzCmdEnum.UP);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedPtzUtils.verify(() -> PtzUtils.getPtzCmd(PtzCmdEnum.UP, defaultSpeed));
        }
    }

    @Test
    void sendPtzControlCommand_ClientCommandSenderThrowsException_ReturnsFailure() {
        // Given
        Exception testException = new RuntimeException("发送失败");

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, TEST_PTZ_CMD))
                .thenThrow(testException);
            mockedUtils.when(() -> ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "发送失败"))
                .thenReturn(ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "发送失败"));

            // When
            ResultDTO<Void> result = ptzCommand.sendPtzControlCommand(TEST_DEVICE_ID, TEST_PTZ_CMD);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(ResultCode.ERROR_SYSTEM_EXCEPTION, result.getCode());
            assertEquals("发送失败", result.getMessage());
        }
    }
}