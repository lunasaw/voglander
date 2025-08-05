package io.github.lunasaw.voglander.web.api.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.web.api.device.assembler.DeviceWebAssembler;
import lombok.extern.slf4j.Slf4j;

/**
 * DeviceController 纯单元测试
 * 不依赖Spring上下文，只关注当前控制器逻辑
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class DeviceControllerTest {

    @Mock
    private DeviceService      deviceService;

    @Mock
    private DeviceManager      deviceManager;

    @Mock
    private DeviceWebAssembler deviceWebAssembler;

    @InjectMocks
    private DeviceController   deviceController;

    @BeforeEach
    public void setUp() {
        // Initialize Mockito annotations to fix null injection issues
        org.mockito.MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testControllerInstantiation() {
        // 验证控制器能够正常实例化，依赖正确注入
        log.info("DeviceController 单元测试启动成功");
        assertNotNull(deviceController);
        assertNotNull(deviceService);
        assertNotNull(deviceManager);
        assertNotNull(deviceWebAssembler);
    }

    /**
     * 测试根据ID获取设备 - 设备不存在场景
     */
    @Test
    public void testGetById_NotFound() {
        // Given
        Long testDeviceId = 999L;
        when(deviceManager.getDeviceDTOById(testDeviceId)).thenReturn(null);

        // When
        AjaxResult result = deviceController.getById(testDeviceId);

        // Then
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("设备不存在", result.getMsg());

        // 验证Manager方法调用
        verify(deviceManager).getDeviceDTOById(testDeviceId);

        log.info("testGetById_NotFound passed");
    }

    /**
     * 测试根据条件查询设备 - 设备不存在场景
     */
    @Test
    public void testGetByEntity_NotFound() {
        // Given
        io.github.lunasaw.voglander.repository.entity.DeviceDO deviceDO =
            new io.github.lunasaw.voglander.repository.entity.DeviceDO();
        when(deviceManager.getDeviceDTOByEntity(any())).thenReturn(null);

        // When
        AjaxResult result = deviceController.getByEntity(deviceDO);

        // Then
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("设备不存在", result.getMsg());

        // 验证Manager方法调用
        verify(deviceManager).getDeviceDTOByEntity(any());

        log.info("testGetByEntity_NotFound passed");
    }

    /**
     * 测试获取设备列表 - 验证方法调用
     */
    @Test
    public void testList_MethodInvocation() {
        // Given
        io.github.lunasaw.voglander.repository.entity.DeviceDO deviceDO =
            new io.github.lunasaw.voglander.repository.entity.DeviceDO();
        when(deviceManager.listDeviceDTO(any())).thenReturn(java.util.Collections.emptyList());

        // When
        AjaxResult result = deviceController.list(deviceDO);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());

        // 验证Manager方法调用
        verify(deviceManager).listDeviceDTO(any());

        log.info("testList_MethodInvocation passed - 方法调用验证成功");
    }

    /**
     * 测试分页查询设备 - 验证方法调用
     */
    @Test
    public void testListPage_MethodInvocation() {
        // Given
        int page = 1;
        int size = 10;
        com.baomidou.mybatisplus.extension.plugins.pagination.Page mockPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page();
        mockPage.setRecords(java.util.Collections.emptyList());
        when(deviceManager.pageQuerySimple(page, size)).thenReturn(mockPage);

        // When
        AjaxResult result = deviceController.listPage(page, size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());

        // 验证Manager方法调用
        verify(deviceManager).pageQuerySimple(page, size);

        log.info("testListPage_MethodInvocation passed - 方法调用验证成功");
    }
}