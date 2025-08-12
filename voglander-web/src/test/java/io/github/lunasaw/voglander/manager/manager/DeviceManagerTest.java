package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.assembler.DeviceAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;

/**
 * DeviceManager集成测试类
 * 按照标准Manager层测试规范实现
 * 
 * 测试特点：
 * - 被测试的Manager使用@Autowired真实注入
 * - 基础Service层使用@Autowired真实注入
 * - 业务Assembler层使用@MockitoBean模拟
 * - 完整的Spring上下文和数据库事务支持
 * - 每个测试方法独立的数据清理
 *
 * @author luna
 * @date 2023/12/30
 */
@Slf4j
public class DeviceManagerTest extends BaseTest {

    // 测试数据常量 - 使用有意义的业务数据
    private static final String  TEST_DEVICE_ID   = "TEST_DEVICE_001";
    private static final String  TEST_DEVICE_ID_2 = "TEST_DEVICE_002";
    private static final String  TEST_IP          = "192.168.1.100";
    private static final Integer TEST_PORT        = 5060;
    private static final Integer TEST_TYPE        = 1;
    private static final String  TEST_NAME        = "测试设备";

    // 被测试对象 - 使用真实注入
    @Autowired
    private DeviceManager        deviceManager;

    @Autowired
    private CacheManager         cacheManager;

    // 基础Service层 - 继承IService<DO>的服务使用真实注入
    @Autowired
    private DeviceService        deviceService;

    // 业务组装层 - 数据转换逻辑使用模拟
    @MockitoBean
    private DeviceAssembler      deviceAssembler;

    // 测试数据对象
    private DeviceDO             testDO;
    private DeviceDTO            testDTO;

    @BeforeEach
    public void setUp() {
        log.info("开始设置测试数据");

        // 1. 清理数据库中的测试数据
        cleanupTestData();

        // 2. 创建测试用的DO和DTO对象
        testDO = createTestDO();
        testDTO = createTestDTO();

        // 3. 设置Assembler的模拟行为
        setupAssemblerMocks();

        log.info("测试数据设置完成");
    }

    @AfterEach
    public void tearDown() {
        log.info("开始清理测试数据");
        cleanupTestData();
        log.info("测试数据清理完成");
    }

    /**
     * 清理测试数据 - 必须实现
     */
    private void cleanupTestData() {
        try {
            // 删除测试数据 - 覆盖所有测试数据模式
            QueryWrapper<DeviceDO> wrapper = new QueryWrapper<>();
            wrapper.in("device_id", TEST_DEVICE_ID, TEST_DEVICE_ID_2)
                .or().like("name", "测试设备");
            deviceService.remove(wrapper);

            // 清理缓存
            Cache cache = cacheManager.getCache("device");
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            log.warn("清理测试数据时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 创建测试用的DeviceDO
     */
    private DeviceDO createTestDO() {
        DeviceDO deviceDO = new DeviceDO();
        deviceDO.setDeviceId(TEST_DEVICE_ID);
        deviceDO.setName(TEST_NAME);
        deviceDO.setIp(TEST_IP);
        deviceDO.setPort(TEST_PORT);
        deviceDO.setType(TEST_TYPE);
        deviceDO.setStatus(1);
        deviceDO.setServerIp("127.0.0.1"); // 设置必需的server_ip字段
        deviceDO.setCreateTime(LocalDateTime.now());
        deviceDO.setUpdateTime(LocalDateTime.now());
        return deviceDO;
    }

    /**
     * 创建测试用的DeviceDTO
     */
    private DeviceDTO createTestDTO() {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(TEST_DEVICE_ID);
        dto.setName(TEST_NAME);
        dto.setIp(TEST_IP);
        dto.setPort(TEST_PORT);
        dto.setType(TEST_TYPE);
        dto.setStatus(1);
        dto.setServerIp("127.0.0.1"); // 设置必需的server_ip字段
        return dto;
    }

    /**
     * 设置Assembler的模拟行为 - 必须实现
     */
    private void setupAssemblerMocks() {
        // 模拟DTO转DO
        when(deviceAssembler.toDeviceDO(any(DeviceDTO.class)))
            .thenAnswer(invocation -> {
                DeviceDTO dto = invocation.getArgument(0);
                DeviceDO deviceDO = new DeviceDO();
                if (dto.getId() != null)
                    deviceDO.setId(dto.getId());
                if (dto.getDeviceId() != null)
                    deviceDO.setDeviceId(dto.getDeviceId());
                if (dto.getName() != null)
                    deviceDO.setName(dto.getName());
                if (dto.getIp() != null)
                    deviceDO.setIp(dto.getIp());
                if (dto.getPort() != null)
                    deviceDO.setPort(dto.getPort());
                if (dto.getType() != null)
                    deviceDO.setType(dto.getType());
                if (dto.getStatus() != null)
                    deviceDO.setStatus(dto.getStatus());
                if (dto.getServerIp() != null)
                    deviceDO.setServerIp(dto.getServerIp());
                if (dto.getCreateTime() != null)
                    deviceDO.setCreateTime(dto.getCreateTime());
                if (dto.getUpdateTime() != null)
                    deviceDO.setUpdateTime(dto.getUpdateTime());
                return deviceDO;
            });

        // 模拟DO转DTO
        when(deviceAssembler.toDeviceDTO(any(DeviceDO.class)))
            .thenAnswer(invocation -> {
                DeviceDO deviceDO = invocation.getArgument(0);
                if (deviceDO == null)
                    return null;
                DeviceDTO dto = new DeviceDTO();
                dto.setId(deviceDO.getId());
                dto.setDeviceId(deviceDO.getDeviceId());
                dto.setName(deviceDO.getName());
                dto.setIp(deviceDO.getIp());
                dto.setPort(deviceDO.getPort());
                dto.setType(deviceDO.getType());
                dto.setStatus(deviceDO.getStatus());
                dto.setServerIp(deviceDO.getServerIp());
                dto.setCreateTime(deviceDO.getCreateTime());
                dto.setUpdateTime(deviceDO.getUpdateTime());
                return dto;
            });

        // 模拟批量转换
        when(deviceAssembler.toDeviceDTOList(any(List.class)))
            .thenAnswer(invocation -> {
                List<DeviceDO> deviceDOList = invocation.getArgument(0);
                return deviceDOList.stream()
                    .map(deviceDO -> {
                        DeviceDTO dto = new DeviceDTO();
                        dto.setId(deviceDO.getId());
                        dto.setDeviceId(deviceDO.getDeviceId());
                        dto.setName(deviceDO.getName());
                        dto.setIp(deviceDO.getIp());
                        dto.setPort(deviceDO.getPort());
                        dto.setType(deviceDO.getType());
                        dto.setStatus(deviceDO.getStatus());
                        dto.setServerIp(deviceDO.getServerIp());
                        dto.setCreateTime(deviceDO.getCreateTime());
                        dto.setUpdateTime(deviceDO.getUpdateTime());
                        return dto;
                    })
                    .toList();
            });
    }

    // ================================
    // 核心模板方法测试
    // ================================

    @Test
    public void testAdd_Success() {
        // Given
        DeviceDTO dto = createTestDTO();

        // When
        Long id = deviceManager.add(dto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        // 验证数据库中的记录
        DeviceDO saved = deviceService.getById(id);
        assertNotNull(saved);
        assertEquals(TEST_DEVICE_ID, saved.getDeviceId());
        assertEquals(TEST_NAME, saved.getName());
        assertEquals(TEST_IP, saved.getIp());
        assertEquals(TEST_PORT, saved.getPort());
        assertEquals(TEST_TYPE, saved.getType());

        // 验证Assembler被调用
        verify(deviceAssembler).toDeviceDO(dto);

        log.info("新增测试通过，ID: {}", id);
    }

    @Test
    public void testAdd_DuplicateDeviceId() {
        // Given - 先创建一个设备
        DeviceDTO dto1 = createTestDTO();
        Long id1 = deviceManager.add(dto1);
        assertNotNull(id1);

        // When & Then - 尝试创建相同deviceId的设备
        DeviceDTO dto2 = createTestDTO();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> deviceManager.add(dto2));

        assertTrue(exception.getMessage().contains("设备ID已存在"));
        log.info("重复设备ID测试通过");
    }

    @Test
    public void testAdd_Validation() {
        // Test null DTO
        assertThrows(IllegalArgumentException.class, () -> deviceManager.add(null));

            // Test empty deviceId
            DeviceDTO dto1 = createTestDTO();
            dto1.setDeviceId("");
            assertThrows(IllegalArgumentException.class, () -> deviceManager.add(dto1));

            // Test null IP
            DeviceDTO dto2 = createTestDTO();
            dto2.setIp(null);
            assertThrows(IllegalArgumentException.class, () -> deviceManager.add(dto2));

            log.info("参数校验测试通过");
    }

    @Test
    public void testUpdateById_Success() {
        // Given - 先创建一个设备
        DeviceDTO createDTO = createTestDTO();
        Long id = deviceManager.add(createDTO);
        assertNotNull(id);

        // When - 更新设备
        DeviceDTO updateDTO = new DeviceDTO();
        updateDTO.setName("更新后的设备名称");
        updateDTO.setStatus(0);

        Long result = deviceManager.updateById(id, updateDTO);

        // Then
        assertEquals(id, result);

        // 验证数据库中的记录
        DeviceDO updated = deviceService.getById(id);
        assertNotNull(updated);
        assertEquals("更新后的设备名称", updated.getName());
        assertEquals(0, updated.getStatus());

        log.info("通过ID更新测试通过");
    }

    @Test
    public void testUpdateById_NotFound() {
        // Given - 不存在的ID
        Long nonExistentId = 99999L;
        DeviceDTO updateDTO = new DeviceDTO();
        updateDTO.setName("测试");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> deviceManager.updateById(nonExistentId, updateDTO));

        assertTrue(exception.getMessage().contains("设备不存在"));
        log.info("更新不存在设备测试通过");
    }

    @Test
    public void testGet_Success() {
        // Given - 先创建一个设备
        DeviceDTO createDTO = createTestDTO();
        Long id = deviceManager.add(createDTO);
        assertNotNull(id);

        // When
        DeviceDTO queryDTO = new DeviceDTO();
        queryDTO.setDeviceId(TEST_DEVICE_ID);
        DeviceDTO result = deviceManager.get(queryDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(TEST_NAME, result.getName());

        log.info("单条查询测试通过");
    }

    @Test
    public void testGet_NotFound() {
        // Given
        DeviceDTO queryDTO = new DeviceDTO();
        queryDTO.setDeviceId("NON_EXISTENT");

        // When
        DeviceDTO result = deviceManager.get(queryDTO);

        // Then
        assertNull(result);
        log.info("查询不存在设备测试通过");
    }

    @Test
    public void testDeleteOne_Success() {
        // Given - 先创建一个设备
        DeviceDTO createDTO = createTestDTO();
        Long id = deviceManager.add(createDTO);
        assertNotNull(id);

        // When
        DeviceDTO deleteDTO = new DeviceDTO();
        deleteDTO.setDeviceId(TEST_DEVICE_ID);
        Boolean result = deviceManager.deleteOne(deleteDTO);

        // Then
        assertTrue(result);

        // 验证数据库中已删除
        DeviceDO deleted = deviceService.getById(id);
        assertNull(deleted);

        log.info("删除测试通过");
    }

    @Test
    public void testDeleteOne_NotFound() {
        // Given
        DeviceDTO deleteDTO = new DeviceDTO();
        deleteDTO.setDeviceId("NON_EXISTENT");

        // When
        Boolean result = deviceManager.deleteOne(deleteDTO);

        // Then
        assertFalse(result);
        log.info("删除不存在设备测试通过");
    }

    // ================================
    // 兼容方法测试
    // ================================

    @Test
    public void testCreateDevice_Success() {
        // Given
        DeviceDTO dto = createTestDTO();

        // When
        Long result = deviceManager.createDevice(dto);

        // Then
        assertNotNull(result);
        assertTrue(result > 0);

        // 验证数据库中的记录
        DeviceDO saved = deviceService.getById(result);
        assertNotNull(saved);
        assertEquals(TEST_DEVICE_ID, saved.getDeviceId());

        log.info("创建设备兼容接口测试通过，ID: {}", result);
    }

    @Test
    public void testUpdateDevice_Success() {
        // Given - 先创建一个设备
        DeviceDTO createDTO = createTestDTO();
        Long id = deviceManager.add(createDTO);

        // When
        DeviceDTO updateDTO = createTestDTO();
        updateDTO.setId(id);
        updateDTO.setName("更新后的名称");

        Long result = deviceManager.updateDevice(updateDTO);

        // Then
        assertEquals(id, result);

        // 验证更新结果
        DeviceDO updated = deviceService.getById(id);
        assertEquals("更新后的名称", updated.getName());

        log.info("更新设备兼容接口测试通过");
    }

    @Test
    public void testSaveOrUpdate_NewDevice() {
        // Given
        DeviceDTO dto = createTestDTO();

        // When
        Long result = deviceManager.saveOrUpdate(dto);

        // Then
        assertNotNull(result);
        DeviceDO saved = deviceService.getById(result);
        assertNotNull(saved);
        assertEquals(TEST_DEVICE_ID, saved.getDeviceId());

        log.info("保存或更新（新建）测试通过");
    }

    @Test
    public void testSaveOrUpdate_ExistingDevice() {
        // Given - 先创建设备
        DeviceDTO createDTO = createTestDTO();
        Long id = deviceManager.add(createDTO);

        // When - 使用相同deviceId再次保存
        DeviceDTO updateDTO = createTestDTO();
        updateDTO.setName("更新的设备名");
        Long result = deviceManager.saveOrUpdate(updateDTO);

        // Then
        assertEquals(id, result);
        DeviceDO updated = deviceService.getById(id);
        assertEquals("更新的设备名", updated.getName());

        log.info("保存或更新（更新）测试通过");
    }

    @Test
    public void testDeleteDevice_Success() {
        // Given - 先创建设备
        DeviceDTO createDTO = createTestDTO();
        Long id = deviceManager.add(createDTO);

        // When
        Boolean result = deviceManager.deleteDevice(TEST_DEVICE_ID);

        // Then
        assertTrue(result);
        DeviceDO deleted = deviceService.getById(id);
        assertNull(deleted);

        log.info("删除设备兼容接口测试通过");
    }

    @Test
    public void testUpdateStatus_Success() {
        // Given - 先创建设备
        DeviceDTO createDTO = createTestDTO();
        Long id = deviceManager.add(createDTO);

        // When
        deviceManager.updateStatus(TEST_DEVICE_ID, 0);

        // Then
        DeviceDO updated = deviceService.getById(id);
        assertEquals(0, updated.getStatus());

        log.info("更新状态测试通过");
    }

    // ================================
    // 批量操作测试
    // ================================

    @Test
    public void testBatchCreateDevice_Success() {
        // Given
        DeviceDTO dto1 = createTestDTO();
        DeviceDTO dto2 = createTestDTO();
        dto2.setDeviceId(TEST_DEVICE_ID_2);
        dto2.setName("测试设备2");

        List<DeviceDTO> deviceList = Arrays.asList(dto1, dto2);

        // When
        int result = deviceManager.batchCreateDevice(deviceList);

        // Then
        assertEquals(2, result);

        // 验证数据库中的记录
        QueryWrapper<DeviceDO> wrapper = new QueryWrapper<>();
        wrapper.in("device_id", TEST_DEVICE_ID, TEST_DEVICE_ID_2);
        List<DeviceDO> savedDevices = deviceService.list(wrapper);
        assertEquals(2, savedDevices.size());

        log.info("批量创建测试通过");
    }

    // ================================
    // 完整生命周期测试
    // ================================

    @Test
    public void testCompleteLifecycle() {
        // 1. 创建
        DeviceDTO createDTO = createTestDTO();
        Long id = deviceManager.add(createDTO);
        assertNotNull(id);
        log.info("1. 创建成功: {}", id);

        // 2. 查询验证
        DeviceDTO queryDTO = new DeviceDTO();
        queryDTO.setId(id);
        DeviceDTO found = deviceManager.get(queryDTO);
        assertNotNull(found);
        assertEquals(TEST_DEVICE_ID, found.getDeviceId());
        log.info("2. 查询验证成功");

        // 3. 更新
        DeviceDTO updateDTO = new DeviceDTO();
        updateDTO.setName("更新后的设备");
        updateDTO.setStatus(0);
        Long updateResult = deviceManager.updateById(id, updateDTO);
        assertEquals(id, updateResult);

        DeviceDO updated = deviceService.getById(id);
        assertEquals("更新后的设备", updated.getName());
        assertEquals(0, updated.getStatus());
        log.info("3. 更新成功");

        // 4. 删除
        DeviceDTO deleteDTO = new DeviceDTO();
        deleteDTO.setId(id);
        Boolean deleted = deviceManager.deleteOne(deleteDTO);
        assertTrue(deleted);

        DeviceDO afterDelete = deviceService.getById(id);
        assertNull(afterDelete);
        log.info("4. 删除成功");

        log.info("完整生命周期测试通过");
    }
}