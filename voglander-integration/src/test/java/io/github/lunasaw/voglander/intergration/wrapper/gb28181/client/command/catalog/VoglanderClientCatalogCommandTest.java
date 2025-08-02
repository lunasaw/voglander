package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.catalog;

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

import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;

/**
 * GB28181客户端目录指令测试类
 * 
 * @author luna
 * @since 2025/8/2
 */
@ExtendWith(MockitoExtension.class)
class VoglanderClientCatalogCommandTest {

    @Mock
    private ClientDeviceSupplier          clientDeviceSupplier;

    @Mock
    private FromDevice                    fromDevice;

    @Mock
    private ToDevice                      toDevice;

    @InjectMocks
    private VoglanderClientCatalogCommand catalogCommand;

    private static final String           TEST_DEVICE_ID = "34020000001320000001";
    private static final String           TEST_CALL_ID   = "test-call-id-123";

    @BeforeEach
    void setUp() {
        when(clientDeviceSupplier.getClientFromDevice()).thenReturn(fromDevice);
        when(clientDeviceSupplier.getToDevice(TEST_DEVICE_ID)).thenReturn(toDevice);
    }

    @Test
    void sendCatalogCommand_Success() {
        // Given
        DeviceResponse deviceResponse = createTestDeviceResponse();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(fromDevice, toDevice, deviceResponse))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = catalogCommand.sendCatalogCommand(TEST_DEVICE_ID, deviceResponse);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendCatalogCommand(fromDevice, toDevice, deviceResponse));
        }
    }

    @Test
    void sendCatalogCommand_NullDeviceId_ThrowsException() {
        // Given
        DeviceResponse deviceResponse = createTestDeviceResponse();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> catalogCommand.sendCatalogCommand(null, deviceResponse));
        assertEquals("发送设备目录响应指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendCatalogCommand_EmptyDeviceId_ThrowsException() {
        // Given
        DeviceResponse deviceResponse = createTestDeviceResponse();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> catalogCommand.sendCatalogCommand("", deviceResponse));
        assertEquals("发送设备目录响应指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendCatalogCommand_NullDeviceResponse_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> catalogCommand.sendCatalogCommand(TEST_DEVICE_ID, null));
        assertEquals("设备响应对象不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceItemsCommand_Success() {
        // Given
        List<DeviceItem> deviceItems = createTestDeviceItems();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(fromDevice, toDevice, deviceItems))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = catalogCommand.sendDeviceItemsCommand(TEST_DEVICE_ID, deviceItems);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendCatalogCommand(fromDevice, toDevice, deviceItems));
        }
    }

    @Test
    void sendDeviceItemsCommand_NullDeviceId_ThrowsException() {
        // Given
        List<DeviceItem> deviceItems = createTestDeviceItems();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> catalogCommand.sendDeviceItemsCommand(null, deviceItems));
        assertEquals("发送设备列表响应指令时设备ID不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceItemsCommand_NullDeviceItems_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> catalogCommand.sendDeviceItemsCommand(TEST_DEVICE_ID, null));
        assertEquals("设备列表不能为空", exception.getMessage());
    }

    @Test
    void sendDeviceItemsCommand_EmptyDeviceItems_Success() {
        // Given
        List<DeviceItem> emptyList = Collections.emptyList();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(fromDevice, toDevice, emptyList))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = catalogCommand.sendDeviceItemsCommand(TEST_DEVICE_ID, emptyList);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void sendSingleDeviceItemCommand_Success() {
        // Given
        DeviceItem deviceItem = createTestDeviceItem();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(fromDevice, toDevice, deviceItem))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = catalogCommand.sendSingleDeviceItemCommand(TEST_DEVICE_ID, deviceItem);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendCatalogCommand(fromDevice, toDevice, deviceItem));
        }
    }

    @Test
    void sendSingleDeviceItemCommand_NullDeviceItem_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> catalogCommand.sendSingleDeviceItemCommand(TEST_DEVICE_ID, null));
        assertEquals("设备项不能为空", exception.getMessage());
    }

    @Test
    void createDeviceResponse_WithAllParameters_Success() {
        // Given
        String name = "测试目录查询";
        Integer sumNum = 3;
        List<DeviceItem> deviceItems = createTestDeviceItems();

        // When
        DeviceResponse result = catalogCommand.createDeviceResponse(TEST_DEVICE_ID, name, sumNum, deviceItems);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(name, result.getName());
        assertEquals(sumNum, result.getSumNum());
        assertEquals(deviceItems, result.getDeviceList());
    }

    @Test
    void createDeviceResponse_WithNullName_UsesDefault() {
        // Given
        List<DeviceItem> deviceItems = createTestDeviceItems();

        // When
        DeviceResponse result = catalogCommand.createDeviceResponse(TEST_DEVICE_ID, null, 2, deviceItems);

        // Then
        assertNotNull(result);
        assertEquals("目录查询", result.getName());
    }

    @Test
    void createDeviceResponse_WithNullSumNum_UsesDeviceItemsSize() {
        // Given
        List<DeviceItem> deviceItems = createTestDeviceItems();

        // When
        DeviceResponse result = catalogCommand.createDeviceResponse(TEST_DEVICE_ID, "测试", null, deviceItems);

        // Then
        assertNotNull(result);
        assertEquals(deviceItems.size(), result.getSumNum());
    }

    @Test
    void createDeviceResponse_WithNullDeviceItems_UsesZeroSumNum() {
        // When
        DeviceResponse result = catalogCommand.createDeviceResponse(TEST_DEVICE_ID, "测试", null, null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getSumNum());
        assertNull(result.getDeviceList());
    }

    @Test
    void createDeviceResponse_WithEmptyDeviceItems_DoesNotSetDeviceList() {
        // Given
        List<DeviceItem> emptyList = Collections.emptyList();

        // When
        DeviceResponse result = catalogCommand.createDeviceResponse(TEST_DEVICE_ID, "测试", 5, emptyList);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getSumNum());
        assertNull(result.getDeviceList());
    }

    @Test
    void createDeviceItem_WithAllParameters_Success() {
        // Given
        String name = "测试摄像头";
        String manufacturer = "海康威视";
        String model = "DS-2CD2T46DWD-I5";
        String owner = "测试单位";
        String civilCode = "440300";
        String address = "深圳市南山区";
        Integer parental = 1;
        String parentId = "34020000001320000000";
        Integer safetyWay = 2;
        Integer registerWay = 1;
        Integer secrecy = 0;
        String status = "ON";

        // When
        DeviceItem result = catalogCommand.createDeviceItem(TEST_DEVICE_ID, name, manufacturer, model,
            owner, civilCode, address, parental, parentId, safetyWay, registerWay, secrecy, status);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(name, result.getName());
        assertEquals(manufacturer, result.getManufacturer());
        assertEquals(model, result.getModel());
        assertEquals(owner, result.getOwner());
        assertEquals(civilCode, result.getCivilCode());
        assertEquals(address, result.getAddress());
        assertEquals(parental, result.getParental());
        assertEquals(parentId, result.getParentId());
        assertEquals(safetyWay, result.getSafetyWay());
        assertEquals(registerWay, result.getRegisterWay());
        assertEquals(secrecy, result.getSecrecy());
        assertEquals(status, result.getStatus());
    }

    @Test
    void createDeviceItem_WithNullOptionalParameters_UsesDefaults() {
        // When
        DeviceItem result = catalogCommand.createDeviceItem(TEST_DEVICE_ID, "测试", null, null,
            null, null, null, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals("测试", result.getName());
        assertEquals(0, result.getParental());
        assertEquals(0, result.getSafetyWay());
        assertEquals(1, result.getRegisterWay());
        assertEquals(0, result.getSecrecy());
        assertEquals("ON", result.getStatus());
    }

    @Test
    void createSimpleDeviceItem_Success() {
        // Given
        String name = "简单设备";
        String status = "OFF";

        // When
        DeviceItem result = catalogCommand.createSimpleDeviceItem(TEST_DEVICE_ID, name, status);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(name, result.getName());
        assertEquals(status, result.getStatus());
        assertEquals(0, result.getParental());
        assertEquals(1, result.getRegisterWay());
    }

    @Test
    void sendSimpleCatalogResponse_Success() {
        // Given
        String queryName = "简单查询";
        List<DeviceItem> deviceItems = createTestDeviceItems();

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(eq(fromDevice), eq(toDevice), any(DeviceResponse.class)))
                .thenReturn(TEST_CALL_ID);
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = catalogCommand.sendSimpleCatalogResponse(TEST_DEVICE_ID, queryName, deviceItems);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            mockedSender.verify(() -> ClientCommandSender.sendCatalogCommand(eq(fromDevice), eq(toDevice), any(DeviceResponse.class)));
        }
    }

    @Test
    void sendEmptyCatalogResponse_Success() {
        // Given
        String queryName = "空查询";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(eq(fromDevice), eq(toDevice), any(DeviceResponse.class)))
                .thenAnswer(invocation -> {
                    DeviceResponse response = invocation.getArgument(2);
                    assertEquals(queryName, response.getName());
                    assertEquals(0, response.getSumNum());
                    assertNull(response.getDeviceList());
                    return TEST_CALL_ID;
                });
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = catalogCommand.sendEmptyCatalogResponse(TEST_DEVICE_ID, queryName);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void sendDeviceStatusNotify_Success() {
        // Given
        String name = "状态通知设备";
        String status = "ON";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(eq(fromDevice), eq(toDevice), any(DeviceItem.class)))
                .thenAnswer(invocation -> {
                    DeviceItem item = invocation.getArgument(2);
                    assertEquals(TEST_DEVICE_ID, item.getDeviceId());
                    assertEquals(name, item.getName());
                    assertEquals(status, item.getStatus());
                    return TEST_CALL_ID;
                });
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = catalogCommand.sendDeviceStatusNotify(TEST_DEVICE_ID, name, status);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void sendDeviceOnlineNotify_Success() {
        // Given
        String name = "在线设备";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(eq(fromDevice), eq(toDevice), any(DeviceItem.class)))
                .thenAnswer(invocation -> {
                    DeviceItem item = invocation.getArgument(2);
                    assertEquals("ON", item.getStatus());
                    return TEST_CALL_ID;
                });
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = catalogCommand.sendDeviceOnlineNotify(TEST_DEVICE_ID, name);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void sendDeviceOfflineNotify_Success() {
        // Given
        String name = "离线设备";

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(eq(fromDevice), eq(toDevice), any(DeviceItem.class)))
                .thenAnswer(invocation -> {
                    DeviceItem item = invocation.getArgument(2);
                    assertEquals("OFF", item.getStatus());
                    return TEST_CALL_ID;
                });
            mockedUtils.when(ResultDTOUtils::success).thenReturn(ResultDTOUtils.success());

            // When
            ResultDTO<Void> result = catalogCommand.sendDeviceOfflineNotify(TEST_DEVICE_ID, name);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void sendCatalogCommand_ClientCommandSenderThrowsException_ReturnsFailure() {
        // Given
        DeviceResponse deviceResponse = createTestDeviceResponse();
        Exception testException = new RuntimeException("发送失败");

        try (MockedStatic<ClientCommandSender> mockedSender = mockStatic(ClientCommandSender.class);
            MockedStatic<ResultDTOUtils> mockedUtils = mockStatic(ResultDTOUtils.class)) {

            mockedSender.when(() -> ClientCommandSender.sendCatalogCommand(fromDevice, toDevice, deviceResponse))
                .thenThrow(testException);
            mockedUtils.when(() -> ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "发送失败"))
                .thenReturn(ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "发送失败"));

            // When
            ResultDTO<Void> result = catalogCommand.sendCatalogCommand(TEST_DEVICE_ID, deviceResponse);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(ResultCode.ERROR_SYSTEM_EXCEPTION, result.getCode());
            assertEquals("发送失败", result.getMessage());
        }
    }

    private DeviceResponse createTestDeviceResponse() {
        DeviceResponse deviceResponse = new DeviceResponse();
        deviceResponse.setDeviceId(TEST_DEVICE_ID);
        deviceResponse.setName("测试目录查询");
        deviceResponse.setSumNum(2);
        deviceResponse.setDeviceList(createTestDeviceItems());
        return deviceResponse;
    }

    private List<DeviceItem> createTestDeviceItems() {
        DeviceItem item1 = createTestDeviceItem();
        DeviceItem item2 = new DeviceItem();
        item2.setDeviceId("34020000001320000002");
        item2.setName("测试设备2");
        item2.setStatus("OFF");
        item2.setParental(0);
        return Arrays.asList(item1, item2);
    }

    private DeviceItem createTestDeviceItem() {
        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setDeviceId(TEST_DEVICE_ID);
        deviceItem.setName("测试摄像头");
        deviceItem.setManufacturer("海康威视");
        deviceItem.setModel("DS-2CD2T46DWD-I5");
        deviceItem.setOwner("测试单位");
        deviceItem.setCivilCode("440300");
        deviceItem.setAddress("深圳市南山区");
        deviceItem.setParental(0);
        deviceItem.setSafetyWay(0);
        deviceItem.setRegisterWay(1);
        deviceItem.setSecrecy(0);
        deviceItem.setStatus("ON");
        return deviceItem;
    }
}