package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.constant.ResultCode;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;

/**
 * AbstractVoglanderClientCommand 单元测试
 * 
 * @author luna
 * @since 2025/8/1
 */
@ExtendWith(MockitoExtension.class)
class AbstractVoglanderClientCommandTest {

    @Mock
    private ClientDeviceSupplier           clientDeviceSupplier;

    @Mock
    private FromDevice                     fromDevice;

    @Mock
    private ToDevice                       toDevice;

    private TestableVoglanderClientCommand testCommand;

    @BeforeEach
    void setUp() {
        testCommand = new TestableVoglanderClientCommand();
        testCommand.clientDeviceSupplier = clientDeviceSupplier;

        when(clientDeviceSupplier.getClientFromDevice()).thenReturn(fromDevice);
        when(clientDeviceSupplier.getToDevice(anyString())).thenReturn(toDevice);
    }

    @Test
    void testGetClientFromDevice() {
        FromDevice result = testCommand.getClientFromDevice();
        assertEquals(fromDevice, result);
        verify(clientDeviceSupplier).getClientFromDevice();
    }

    @Test
    void testGetToDevice() {
        String deviceId = "34020000001320000001";
        ToDevice result = testCommand.getToDevice(deviceId);
        assertEquals(toDevice, result);
        verify(clientDeviceSupplier).getToDevice(deviceId);
    }

    @Test
    void testExecuteCommandSuccess() {
        String deviceId = "34020000001320000001";
        String expectedCallId = "test-call-id";

        ResultDTO<Void> result = testCommand.executeCommand("testMethod", deviceId,
            () -> expectedCallId, "param1", "param2");

        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void testExecuteCommandException() {
        String deviceId = "34020000001320000001";
        String errorMessage = "Test exception";

        ResultDTO<Void> result = testCommand.executeCommand("testMethod", deviceId,
            () -> {
                throw new RuntimeException(errorMessage);
            }, "param1");

        assertFalse(result.isSuccess());
        assertEquals(ResultCode.ERROR_SYSTEM_EXCEPTION, result.getCode());
        assertEquals(errorMessage, result.getMsg());
    }

    @Test
    void testExecuteCommandWithResultSuccess() {
        String deviceId = "34020000001320000001";
        String expectedResult = "test-result";

        ResultDTO<String> result = testCommand.executeCommandWithResult("testMethod", deviceId,
            () -> expectedResult, "param1");

        assertTrue(result.isSuccess());
        assertEquals(expectedResult, result.getData());
    }

    @Test
    void testExecuteCommandWithResultException() {
        String deviceId = "34020000001320000001";
        String errorMessage = "Test exception";

        ResultDTO<String> result = testCommand.executeCommandWithResult("testMethod", deviceId,
            () -> {
                throw new RuntimeException(errorMessage);
            }, "param1");

        assertFalse(result.isSuccess());
        assertEquals(ResultCode.ERROR_SYSTEM_EXCEPTION, result.getCode());
        assertEquals(errorMessage, result.getMsg());
    }

    @Test
    void testExecuteCommandWithoutDeviceIdSuccess() {
        String expectedCallId = "test-call-id";

        ResultDTO<Void> result = testCommand.executeCommand("testMethod",
            () -> expectedCallId, "param1");

        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void testValidateDeviceIdValid() {
        assertDoesNotThrow(() -> testCommand.validateDeviceId("34020000001320000001", "设备ID不能为空"));
    }

    @Test
    void testValidateDeviceIdNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> testCommand.validateDeviceId(null, "设备ID不能为空"));
        assertEquals("设备ID不能为空", exception.getMessage());
    }

    @Test
    void testValidateDeviceIdEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> testCommand.validateDeviceId("", "设备ID不能为空"));
        assertEquals("设备ID不能为空", exception.getMessage());
    }

    @Test
    void testValidateDeviceIdBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> testCommand.validateDeviceId("   ", "设备ID不能为空"));
        assertEquals("设备ID不能为空", exception.getMessage());
    }

    @Test
    void testValidateDeviceIdDefaultMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> testCommand.validateDeviceId(null, null));
        assertEquals("设备ID不能为空", exception.getMessage());
    }

    @Test
    void testValidateNotNullValid() {
        Object param = new Object();
        assertDoesNotThrow(() -> testCommand.validateNotNull(param, "参数不能为空"));
    }

    @Test
    void testValidateNotNullInvalid() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> testCommand.validateNotNull(null, "参数不能为空"));
        assertEquals("参数不能为空", exception.getMessage());
    }

    @Test
    void testValidateNotNullDefaultMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> testCommand.validateNotNull(null, null));
        assertEquals("参数不能为空", exception.getMessage());
    }

    /**
     * 测试用的具体实现类，用于测试抽象类的方法
     */
    private static class TestableVoglanderClientCommand extends AbstractVoglanderClientCommand {
        // 仅用于测试，无需额外实现
    }
}