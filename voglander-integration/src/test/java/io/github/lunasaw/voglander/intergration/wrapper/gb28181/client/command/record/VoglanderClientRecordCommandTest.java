package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.record;

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

import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;

/**
 * GB28181客户端录像指令测试类
 * 
 * @author luna
 * @since 2025/8/2
 */
@ExtendWith(MockitoExtension.class)
class VoglanderClientRecordCommandTest {

    @Mock
    private ClientDeviceSupplier         clientDeviceSupplier;

    @Mock
    private FromDevice                   fromDevice;

    @Mock
    private ToDevice                     toDevice;

    @InjectMocks
    private VoglanderClientRecordCommand recordCommand;

    private static final String          TEST_DEVICE_ID = "34020000001320000001";
    private static final String          TEST_CALL_ID   = "test-call-id-123";

    @BeforeEach
    void setUp() {
        when(clientDeviceSupplier.getClientFromDevice()).thenReturn(fromDevice);
        when(clientDeviceSupplier.getToDevice(TEST_DEVICE_ID)).thenReturn(toDevice);
    }

    @Test
    void sendDeviceRecordCommand_Success() {
        // Given
        DeviceRecord deviceRecord = createTestDeviceRecord();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceRecordCommand(fromDevice, toDevice, deviceRecord))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = recordCommand.sendDeviceRecordCommand(TEST_DEVICE_ID, deviceRecord);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendDeviceRecordCommand(fromDevice, toDevice, deviceRecord));
        }
    }

    @Test
    void sendDeviceRecordCommand_NullDeviceId_ThrowsException() {
        // Given
        DeviceRecord deviceRecord = createTestDeviceRecord();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.sendDeviceRecordCommand(null, deviceRecord));
        assertEquals("发送录像响应指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceRecordCommand_EmptyDeviceId_ThrowsException() {
        // Given
        DeviceRecord deviceRecord = createTestDeviceRecord();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.sendDeviceRecordCommand("", deviceRecord));
        assertEquals("发送录像响应指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceRecordCommand_WhitespaceDeviceId_ThrowsException() {
        // Given
        DeviceRecord deviceRecord = createTestDeviceRecord();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.sendDeviceRecordCommand("   ", deviceRecord));
        assertEquals("发送录像响应指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceRecordCommand_NullDeviceRecord_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.sendDeviceRecordCommand(TEST_DEVICE_ID, null));
        assertEquals("录像响应对象不能为空", exception.getMessage());
    }

    @Test
    void sendRecordItemsCommand_Success() {
        // Given
        List<DeviceRecord.RecordItem> recordItems = createTestRecordItems();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceRecordCommand(fromDevice, toDevice, recordItems))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = recordCommand.sendRecordItemsCommand(TEST_DEVICE_ID, recordItems);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendDeviceRecordCommand(fromDevice, toDevice, recordItems));
        }
    }

    @Test
    void sendRecordItemsCommand_NullDeviceId_ThrowsException() {
        // Given
        List<DeviceRecord.RecordItem> recordItems = createTestRecordItems();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.sendRecordItemsCommand(null, recordItems));
        assertEquals("发送录像文件列表指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendRecordItemsCommand_NullRecordItems_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.sendRecordItemsCommand(TEST_DEVICE_ID, null));
        assertEquals("录像文件列表不能为空", exception.getMessage());
    }

    @Test
    void sendRecordItemsCommand_EmptyRecordItems_Success() {
        // Given
        List<DeviceRecord.RecordItem> emptyList = Collections.emptyList();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceRecordCommand(fromDevice, toDevice, emptyList))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = recordCommand.sendRecordItemsCommand(TEST_DEVICE_ID, emptyList);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void createDeviceRecord_WithAllParameters_Success() {
        // Given
        String sn = "测试录像查询";
        Integer sumNum = 3;
        List<DeviceRecord.RecordItem> recordItems = createTestRecordItems();

        // When
        DeviceRecord result = recordCommand.createDeviceRecord(TEST_DEVICE_ID, sn, sumNum, recordItems);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(sn, result.getSn());
        assertEquals(sumNum, result.getSumNum());
        assertEquals(recordItems, result.getRecordList());
    }

    @Test
    void createDeviceRecord_WithNullSn_UsesDefault() {
        // Given
        List<DeviceRecord.RecordItem> recordItems = createTestRecordItems();

        // When
        DeviceRecord result = recordCommand.createDeviceRecord(TEST_DEVICE_ID, null, 2, recordItems);

        // Then
        assertNotNull(result);
        assertEquals("录像查询", result.getSn());
    }

    @Test
    void createDeviceRecord_WithNullSumNum_UsesRecordItemsSize() {
        // Given
        List<DeviceRecord.RecordItem> recordItems = createTestRecordItems();

        // When
        DeviceRecord result = recordCommand.createDeviceRecord(TEST_DEVICE_ID, "测试", null, recordItems);

        // Then
        assertNotNull(result);
        assertEquals(recordItems.size(), result.getSumNum());
    }

    @Test
    void createDeviceRecord_WithNullRecordItems_UsesZeroSumNum() {
        // When
        DeviceRecord result = recordCommand.createDeviceRecord(TEST_DEVICE_ID, "测试", null, null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getSumNum());
        assertNull(result.getRecordList());
    }

    @Test
    void createDeviceRecord_WithEmptyRecordItems_DoesNotSetRecordList() {
        // Given
        List<DeviceRecord.RecordItem> emptyList = Collections.emptyList();

        // When
        DeviceRecord result = recordCommand.createDeviceRecord(TEST_DEVICE_ID, "测试", 5, emptyList);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getSumNum());
        assertNull(result.getRecordList());
    }

    @Test
    void createRecordItem_WithAllParameters_Success() {
        // Given
        String name = "录像文件001.mp4";
        String filePath = "/record/2025/01/01/录像文件001.mp4";
        String startTime = "2025-01-01T10:00:00";
        String endTime = "2025-01-01T11:00:00";
        String fileSize = "1024000";
        String type = "alarm";

        // When
        DeviceRecord.RecordItem result = recordCommand.createRecordItem(TEST_DEVICE_ID, name, filePath,
            startTime, endTime, fileSize, type);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(name, result.getName());
        assertEquals(filePath, result.getFilePath());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(fileSize, result.getFileSize());
        assertEquals(type, result.getType());
    }

    @Test
    void createRecordItem_WithNullType_UsesDefault() {
        // When
        DeviceRecord.RecordItem result = recordCommand.createRecordItem(TEST_DEVICE_ID, "测试", "/path",
            "2025-01-01T10:00:00", "2025-01-01T11:00:00", "1024", null);

        // Then
        assertNotNull(result);
        assertEquals("time", result.getType());
    }

    @Test
    void createRecordItem_WithNullParameters_AcceptsNulls() {
        // When
        DeviceRecord.RecordItem result = recordCommand.createRecordItem(TEST_DEVICE_ID, null, null,
            null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertNull(result.getName());
        assertNull(result.getFilePath());
        assertNull(result.getStartTime());
        assertNull(result.getEndTime());
        assertNull(result.getFileSize());
        assertEquals("time", result.getType());
    }

    @Test
    void sendSimpleRecordResponse_Success() {
        // Given
        String queryName = "简单录像查询";
        List<DeviceRecord.RecordItem> recordItems = createTestRecordItems();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceRecordCommand(eq(fromDevice), eq(toDevice), any(DeviceRecord.class)))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = recordCommand.sendSimpleRecordResponse(TEST_DEVICE_ID, queryName, recordItems);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendDeviceRecordCommand(eq(fromDevice), eq(toDevice), any(DeviceRecord.class)));
        }
    }

    @Test
    void sendEmptyRecordResponse_Success() {
        // Given
        String queryName = "空录像查询";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceRecordCommand(eq(fromDevice), eq(toDevice), any(DeviceRecord.class)))
                .thenAnswer(invocation -> {
                    DeviceRecord record = invocation.getArgument(2);
                    assertEquals(queryName, record.getSn());
                    assertEquals(0, record.getSumNum());
                    assertNull(record.getRecordList());
                    return TEST_CALL_ID;
                });
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = recordCommand.sendEmptyRecordResponse(TEST_DEVICE_ID, queryName);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void sendRecordControlCommand_Success() {
        // Given
        String controlContent = "RECORD_CONTROL";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, controlContent))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = recordCommand.sendRecordControlCommand(TEST_DEVICE_ID, controlContent);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, controlContent));
        }
    }

    @Test
    void sendRecordControlCommand_NullDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.sendRecordControlCommand(null, "RECORD"));
        assertEquals("发送录像控制指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendRecordControlCommand_NullControlContent_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.sendRecordControlCommand(TEST_DEVICE_ID, null));
        assertEquals("录像控制内容不能为空", exception.getMessage());
    }

    @Test
    void startRecord_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, "RECORD"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = recordCommand.startRecord(TEST_DEVICE_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, "RECORD"));
        }
    }

    @Test
    void stopRecord_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, "STOP"))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = recordCommand.stopRecord(TEST_DEVICE_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, "STOP"));
        }
    }

    @Test
    void startRecord_NullDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.startRecord(null));
        assertEquals("发送录像控制指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void stopRecord_EmptyDeviceId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> recordCommand.stopRecord(""));
        assertEquals("发送录像控制指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceRecordCommand_ClientCommandSenderThrowsException_ReturnsFailure() {
        // Given
        DeviceRecord deviceRecord = createTestDeviceRecord();
        Exception testException = new RuntimeException("录像发送失败");

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendDeviceRecordCommand(fromDevice, toDevice, deviceRecord))
                .thenThrow(testException);
            mockedUtils.when(() -> ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "录像发送失败"))
                .thenReturn(ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "录像发送失败"));

            // When
            ResultDTO<Void> result = recordCommand.sendDeviceRecordCommand(TEST_DEVICE_ID, deviceRecord);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(ResultCode.ERROR_SYSTEM_EXCEPTION, result.getCode());
            assertEquals("录像发送失败", result.getMessage());
        }
    }

    @Test
    void sendRecordControlCommand_ClientCommandSenderThrowsException_ReturnsFailure() {
        // Given
        Exception testException = new RuntimeException("控制发送失败");

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(fromDevice, toDevice, "RECORD"))
                .thenThrow(testException);
            mockedUtils.when(() -> ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "控制发送失败"))
                .thenReturn(ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "控制发送失败"));

            // When
            ResultDTO<Void> result = recordCommand.sendRecordControlCommand(TEST_DEVICE_ID, "RECORD");

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(ResultCode.ERROR_SYSTEM_EXCEPTION, result.getCode());
            assertEquals("控制发送失败", result.getMessage());
        }
    }

    @Test
    void recordTypes_ValidValues_Success() {
        // Test various record types
        String[] validTypes = {"time", "alarm", "manual", "all"};

        for (String type : validTypes) {
            DeviceRecord.RecordItem item = recordCommand.createRecordItem(TEST_DEVICE_ID, "test", "/path",
                "2025-01-01T10:00:00", "2025-01-01T11:00:00", "1024", type);
            assertEquals(type, item.getType());
        }
    }

    @Test
    void recordControlCommands_DifferentControls_Success() {
        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendInvitePlayControlCommand(any(), any(), anyString()))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // Test various control commands
            assertDoesNotThrow(() -> recordCommand.sendRecordControlCommand(TEST_DEVICE_ID, "PLAY"));
            assertDoesNotThrow(() -> recordCommand.sendRecordControlCommand(TEST_DEVICE_ID, "PAUSE"));
            assertDoesNotThrow(() -> recordCommand.sendRecordControlCommand(TEST_DEVICE_ID, "TEARDOWN"));
        }
    }

    private DeviceRecord createTestDeviceRecord() {
        DeviceRecord deviceRecord = new DeviceRecord();
        deviceRecord.setDeviceId(TEST_DEVICE_ID);
        deviceRecord.setSn("测试录像查询");
        deviceRecord.setSumNum(2);
        deviceRecord.setRecordList(createTestRecordItems());
        return deviceRecord;
    }

    private List<DeviceRecord.RecordItem> createTestRecordItems() {
        DeviceRecord.RecordItem item1 = new DeviceRecord.RecordItem();
        item1.setDeviceId(TEST_DEVICE_ID);
        item1.setName("录像文件001.mp4");
        item1.setFilePath("/record/2025/01/01/录像文件001.mp4");
        item1.setStartTime("2025-01-01T10:00:00");
        item1.setEndTime("2025-01-01T11:00:00");
        item1.setFileSize("1024000");
        item1.setType("time");

        DeviceRecord.RecordItem item2 = new DeviceRecord.RecordItem();
        item2.setDeviceId(TEST_DEVICE_ID);
        item2.setName("录像文件002.mp4");
        item2.setFilePath("/record/2025/01/01/录像文件002.mp4");
        item2.setStartTime("2025-01-01T11:00:00");
        item2.setEndTime("2025-01-01T12:00:00");
        item2.setFileSize("2048000");
        item2.setType("alarm");

        return Arrays.asList(item1, item2);
    }
}