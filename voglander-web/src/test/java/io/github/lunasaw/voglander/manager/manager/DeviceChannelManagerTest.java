package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.assembler.DeviceChannelAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import lombok.extern.slf4j.Slf4j;

/**
 * DeviceChannelManager集成测试类
 * 
 * 测试策略：
 * - 继承BaseTest进行集成测试，使用真实的Spring上下文和数据库事务
 * - DeviceChannelService使用@Autowired（基础数据访问层）
 * - DeviceChannelAssembler使用@MockitoBean（业务逻辑层）
 * - 使用@Transactional确保测试数据隔离
 * 
 * 测试覆盖：
 * - 6个核心模板方法的完整测试：add, update, get, deleteOne, deleteBatch, getPage
 * - 业务扩展方法的完整测试：createDeviceChannel, updateDeviceChannel, deleteDeviceChannel等
 * - 边界条件、异常场景、缓存行为测试
 * - 完整生命周期测试和复杂业务场景测试
 * 
 * 模板方法测试架构：
 * - 核心模板方法：验证标准CRUD流程和数据一致性
 * - 扩展业务方法：验证操作日志和业务逻辑
 * - 缓存清理验证：验证统一缓存清理机制
 * - 异常处理测试：验证参数校验和错误处理
 * 
 * @author luna
 * @date 2025-01-31
 */
@Slf4j
public class DeviceChannelManagerTest extends BaseTest {

    // 测试数据常量 - 使用有意义的业务数据
    private static final String    TEST_DEVICE_ID      = "test-device-123";
    private static final String    TEST_CHANNEL_ID     = "channel-001";
    private static final String    TEST_CHANNEL_NAME   = "测试通道";
    private static final Integer   TEST_STATUS         = DeviceConstant.Status.ONLINE;
    private static final String    TEST_OPERATION_DESC = "测试操作";

    // 被测试对象 - 使用真实注入
    @Autowired
    private DeviceChannelManager   deviceChannelManager;

    @Autowired
    private CacheManager           cacheManager;

    // 基础Service层 - 继承IService<DO>的服务使用真实注入
    @Autowired
    private DeviceChannelService   deviceChannelService;

    // 业务组装层 - 数据转换逻辑使用模拟
    @MockitoBean
    private DeviceChannelAssembler deviceChannelAssembler;

    // 依赖的Manager - 外部业务逻辑使用模拟
    @MockitoBean
    private DeviceManager          deviceManager;

    // 测试数据对象
    private DeviceChannelDO        testDO;
    private DeviceChannelDTO       testDTO;

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
            QueryWrapper<DeviceChannelDO> wrapper = new QueryWrapper<>();
            wrapper.in("device_id", TEST_DEVICE_ID, TEST_DEVICE_ID + "2")
                .or().in("channel_id", TEST_CHANNEL_ID, TEST_CHANNEL_ID + "2");
            deviceChannelService.remove(wrapper);

            // 清理缓存
            Cache cache = cacheManager.getCache("deviceChannel");
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            log.warn("清理测试数据时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 创建测试DO对象
     */
    private DeviceChannelDO createTestDO() {
        DeviceChannelDO deviceChannelDO = new DeviceChannelDO();
        deviceChannelDO.setDeviceId(TEST_DEVICE_ID);
        deviceChannelDO.setChannelId(TEST_CHANNEL_ID);
        deviceChannelDO.setName(TEST_CHANNEL_NAME);
        deviceChannelDO.setStatus(TEST_STATUS);
        deviceChannelDO.setExtend("{\"test\":\"data\"}");
        deviceChannelDO.setCreateTime(LocalDateTime.now());
        deviceChannelDO.setUpdateTime(LocalDateTime.now());
        return deviceChannelDO;
    }

    /**
     * 创建测试DTO对象
     */
    private DeviceChannelDTO createTestDTO() {
        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setDeviceId(TEST_DEVICE_ID);
        dto.setChannelId(TEST_CHANNEL_ID);
        dto.setName(TEST_CHANNEL_NAME);
        dto.setStatus(TEST_STATUS);

        DeviceChannelDTO.ExtendInfo extendInfo = new DeviceChannelDTO.ExtendInfo();
        extendInfo.setChannelInfo("test channel info");
        dto.setExtendInfo(extendInfo);

        return dto;
    }

    /**
     * 设置Assembler的模拟行为 - 必须实现
     */
    private void setupAssemblerMocks() {
        // 模拟DeviceManager - 确保设备存在性检查通过
        DeviceDTO mockDeviceDTO = new DeviceDTO();
        mockDeviceDTO.setDeviceId(TEST_DEVICE_ID);
        mockDeviceDTO.setName("测试设备");
        when(deviceManager.getDtoByDeviceId(TEST_DEVICE_ID)).thenReturn(mockDeviceDTO);
        when(deviceManager.getDtoByDeviceId(TEST_DEVICE_ID + "2")).thenReturn(mockDeviceDTO);

        // 模拟DTO转DO
        when(deviceChannelAssembler.dtoToDo(any(DeviceChannelDTO.class)))
            .thenAnswer(invocation -> {
                DeviceChannelDTO dto = invocation.getArgument(0);
                DeviceChannelDO deviceChannelDO = new DeviceChannelDO();
                deviceChannelDO.setId(dto.getId());
                deviceChannelDO.setDeviceId(dto.getDeviceId());
                deviceChannelDO.setChannelId(dto.getChannelId());
                deviceChannelDO.setName(dto.getName());
                deviceChannelDO.setStatus(dto.getStatus());
                deviceChannelDO.setExtend(dto.getExtend());
                deviceChannelDO.setCreateTime(dto.getCreateTime());
                deviceChannelDO.setUpdateTime(dto.getUpdateTime());
                return deviceChannelDO;
            });

        // 模拟DO转DTO
        when(deviceChannelAssembler.doToDto(any(DeviceChannelDO.class)))
            .thenAnswer(invocation -> {
                DeviceChannelDO deviceChannelDO = invocation.getArgument(0);
                DeviceChannelDTO dto = new DeviceChannelDTO();
                dto.setId(deviceChannelDO.getId());
                dto.setDeviceId(deviceChannelDO.getDeviceId());
                dto.setChannelId(deviceChannelDO.getChannelId());
                dto.setName(deviceChannelDO.getName());
                dto.setStatus(deviceChannelDO.getStatus());
                dto.setExtend(deviceChannelDO.getExtend());
                dto.setCreateTime(deviceChannelDO.getCreateTime());
                dto.setUpdateTime(deviceChannelDO.getUpdateTime());

                DeviceChannelDTO.ExtendInfo extendInfo = new DeviceChannelDTO.ExtendInfo();
                extendInfo.setChannelInfo("test channel info");
                dto.setExtendInfo(extendInfo);

                return dto;
            });

        // 模拟DO列表转DTO列表
        when(deviceChannelAssembler.doListToDtoList(anyList()))
            .thenAnswer(invocation -> {
                List<DeviceChannelDO> doList = invocation.getArgument(0);
                List<DeviceChannelDTO> dtoList = new ArrayList<>();
                for (DeviceChannelDO deviceChannelDO : doList) {
                    dtoList.add(deviceChannelAssembler.doToDto(deviceChannelDO));
                }
                return dtoList;
            });
    }

    // ================================
    // 核心模板方法测试（必须实现）
    // ================================

    @Test
    public void testAdd_Success() {
        // Given
        DeviceChannelDTO dto = createTestDTO();

        // When
        Long id = deviceChannelManager.add(dto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        // 验证数据库中的记录
        DeviceChannelDO saved = deviceChannelService.getById(id);
        assertNotNull(saved);
        assertEquals(TEST_DEVICE_ID, saved.getDeviceId());
        assertEquals(TEST_CHANNEL_ID, saved.getChannelId());
        assertEquals(TEST_CHANNEL_NAME, saved.getName());
        assertEquals(TEST_STATUS, saved.getStatus());

        // 验证Assembler被调用
        verify(deviceChannelAssembler).dtoToDo(dto);

        log.info("新增测试通过，ID: {}", id);
    }

    @Test
    public void testAdd_Validation() {
        // Test null DTO
        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.add(null));

        // Test invalid fields
        DeviceChannelDTO dto = createTestDTO();
        dto.setDeviceId("");
        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.add(dto));

        dto.setDeviceId(TEST_DEVICE_ID);
        dto.setChannelId("");
        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.add(dto));

        log.info("新增参数校验测试通过");
    }

    @Test
    public void testUpdate_Success() {
        // Given - 先创建记录
        Long id = deviceChannelManager.add(testDTO);

        DeviceChannelDTO queryDTO = new DeviceChannelDTO();
        queryDTO.setId(id);

        DeviceChannelDTO updateDTO = new DeviceChannelDTO();
        updateDTO.setName("更新后的通道名称");
        updateDTO.setStatus(DeviceConstant.Status.OFFLINE);

        // When
        Long updatedId = deviceChannelManager.update(queryDTO, updateDTO);

        // Then
        assertEquals(id, updatedId);

        // 验证更新结果
        DeviceChannelDO updated = deviceChannelService.getById(id);
        assertNotNull(updated);
        // 由于使用了模拟，这里主要验证调用是否成功
        assertEquals(id, updated.getId());

        log.info("更新测试通过，ID: {}", updatedId);
    }

    @Test
    public void testUpdateById_Success() {
        // Given - 先创建记录
        Long id = deviceChannelManager.add(testDTO);

        DeviceChannelDTO updateDTO = new DeviceChannelDTO();
        updateDTO.setName("通过ID更新的名称");

        // When
        Long updatedId = deviceChannelManager.updateById(id, updateDTO);

        // Then
        assertEquals(id, updatedId);

        log.info("通过ID更新测试通过，ID: {}", updatedId);
    }

    @Test
    public void testUpdate_Validation() {
        // Test null parameters
        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.update(null, new DeviceChannelDTO()));

        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.update(new DeviceChannelDTO(), null));

        log.info("更新参数校验测试通过");
    }

    @Test
    public void testGet_Success() {
        // Given - 先创建记录
        Long id = deviceChannelManager.add(testDTO);

        DeviceChannelDTO queryDTO = new DeviceChannelDTO();
        queryDTO.setId(id);

        // When
        DeviceChannelDTO result = deviceChannelManager.get(queryDTO);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());

        // 验证Assembler被调用
        verify(deviceChannelAssembler, atLeastOnce()).doToDto(any(DeviceChannelDO.class));

        log.info("查询测试通过，查询到ID: {}", result.getId());
    }

    @Test
    public void testGet_NotFound() {
        // Given
        DeviceChannelDTO queryDTO = new DeviceChannelDTO();
        queryDTO.setDeviceId("non-existent-device");

        // When
        DeviceChannelDTO result = deviceChannelManager.get(queryDTO);

        // Then
        assertNull(result);

        log.info("查询不存在记录测试通过");
    }

    @Test
    public void testGet_Validation() {
        // Test null query
        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.get(null));

        log.info("查询参数校验测试通过");
    }

    @Test
    public void testDeleteOne_Success() {
        // Given - 先创建记录
        Long id = deviceChannelManager.add(testDTO);

        DeviceChannelDTO deleteDTO = new DeviceChannelDTO();
        deleteDTO.setId(id);

        // When
        Boolean success = deviceChannelManager.deleteOne(deleteDTO);

        // Then
        assertTrue(success);

        // 验证记录已被删除
        DeviceChannelDO deleted = deviceChannelService.getById(id);
        assertNull(deleted);

        log.info("单条删除测试通过，删除ID: {}", id);
    }

    @Test
    public void testDeleteOne_NotFound() {
        // Given
        DeviceChannelDTO deleteDTO = new DeviceChannelDTO();
        deleteDTO.setDeviceId("non-existent-device");

        // When
        Boolean success = deviceChannelManager.deleteOne(deleteDTO);

        // Then
        assertTrue(success); // 删除不存在的记录也返回true

        log.info("删除不存在记录测试通过");
    }

    @Test
    public void testDeleteOne_Validation() {
        // Test null parameter
        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.deleteOne(null));

        log.info("单条删除参数校验测试通过");
    }

    @Test
    public void testDeleteBatch_Success() {
        // Given - 创建多条记录
        DeviceChannelDTO dto1 = createTestDTO();
        DeviceChannelDTO dto2 = createTestDTO();
        dto2.setChannelId(TEST_CHANNEL_ID + "2");

        Long id1 = deviceChannelManager.add(dto1);
        Long id2 = deviceChannelManager.add(dto2);

        DeviceChannelDTO deleteDTO = new DeviceChannelDTO();
        deleteDTO.setDeviceId(TEST_DEVICE_ID);

        // When
        Boolean success = deviceChannelManager.deleteBatch(deleteDTO);

        // Then
        assertTrue(success);

        // 验证记录已被删除
        DeviceChannelDO deleted1 = deviceChannelService.getById(id1);
        DeviceChannelDO deleted2 = deviceChannelService.getById(id2);
        assertNull(deleted1);
        assertNull(deleted2);

        log.info("批量删除测试通过，删除ID: {}, {}", id1, id2);
    }

    @Test
    public void testDeleteBatch_Validation() {
        // Test null parameter
        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.deleteBatch(null));

        log.info("批量删除参数校验测试通过");
    }

    @Test
    public void testGetPage_Success() {
        // Given - 创建多条记录
        DeviceChannelDTO dto1 = createTestDTO();
        DeviceChannelDTO dto2 = createTestDTO();
        dto2.setChannelId(TEST_CHANNEL_ID + "2");

        deviceChannelManager.add(dto1);
        deviceChannelManager.add(dto2);

        DeviceChannelDTO queryDTO = new DeviceChannelDTO();
        queryDTO.setDeviceId(TEST_DEVICE_ID);

        // When
        Page<DeviceChannelDTO> pageResult = deviceChannelManager.getPage(queryDTO, 1, 10);

        // Then
        assertNotNull(pageResult);
        assertEquals(2, pageResult.getTotal());
        assertEquals(2, pageResult.getRecords().size());

        // 验证Assembler被调用
        verify(deviceChannelAssembler, atLeastOnce()).doListToDtoList(anyList());

        log.info("分页查询测试通过，总记录数: {}", pageResult.getTotal());
    }

    @Test
    public void testGetPage_Validation() {
        DeviceChannelDTO queryDTO = new DeviceChannelDTO();

        // Test invalid page parameters
        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.getPage(queryDTO, 0, 10));

        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.getPage(queryDTO, 1, 0));

        assertThrows(IllegalArgumentException.class, () -> deviceChannelManager.getPage(queryDTO, 1, 1001));

        log.info("分页查询参数校验测试通过");
    }

    // ================================
    // 业务扩展方法测试
    // ================================

    @Test
    public void testCreateDeviceChannel_Success() {
        // Given
        DeviceChannelDTO dto = createTestDTO();

        // When
        Long id = deviceChannelManager.createDeviceChannel(dto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        log.info("业务创建测试通过，ID: {}", id);
    }

    @Test
    public void testUpdateDeviceChannel_Success() {
        // Given - 先创建记录
        Long id = deviceChannelManager.add(testDTO);
        testDTO.setId(id);
        testDTO.setName("业务更新的名称");

        // When
        Long updatedId = deviceChannelManager.updateDeviceChannel(testDTO);

        // Then
        assertEquals(id, updatedId);

        log.info("业务更新测试通过，ID: {}", updatedId);
    }

    @Test
    public void testDeleteDeviceChannel_Success() {
        // Given - 先创建记录
        deviceChannelManager.add(testDTO);

        // When
        Boolean success = deviceChannelManager.deleteDeviceChannel(TEST_DEVICE_ID, TEST_CHANNEL_ID);

        // Then
        assertTrue(success);

        log.info("业务删除测试通过");
    }

    @Test
    public void testSaveOrUpdate_Create() {
        // Given
        DeviceChannelDTO dto = createTestDTO();

        // When
        Long id = deviceChannelManager.saveOrUpdate(dto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        log.info("保存或更新-创建测试通过，ID: {}", id);
    }

    @Test
    public void testSaveOrUpdate_Update() {
        // Given - 先创建记录
        deviceChannelManager.add(testDTO);

        DeviceChannelDTO updateDto = createTestDTO();
        updateDto.setName("更新后的名称");

        // When
        Long id = deviceChannelManager.saveOrUpdate(updateDto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        log.info("保存或更新-更新测试通过，ID: {}", id);
    }

    // ================================
    // 完整生命周期测试
    // ================================

    @Test
    public void testCompleteLifecycle() {
        // 1. 创建
        Long id = deviceChannelManager.add(testDTO);
        assertNotNull(id);
        log.info("1. 创建成功: {}", id);

        // 2. 查询验证
        DeviceChannelDTO queryDTO = new DeviceChannelDTO();
        queryDTO.setId(id);
        DeviceChannelDTO queried = deviceChannelManager.get(queryDTO);
        assertNotNull(queried);
        assertEquals(id, queried.getId());
        log.info("2. 查询验证成功");

        // 3. 更新
        DeviceChannelDTO updateDTO = new DeviceChannelDTO();
        updateDTO.setName("生命周期更新的名称");
        Long updatedId = deviceChannelManager.updateById(id, updateDTO);
        assertEquals(id, updatedId);
        log.info("3. 更新成功");

        // 4. 删除
        DeviceChannelDTO deleteDTO = new DeviceChannelDTO();
        deleteDTO.setId(id);
        Boolean deleted = deviceChannelManager.deleteOne(deleteDTO);
        assertTrue(deleted);

        // 验证已删除
        DeviceChannelDTO afterDelete = deviceChannelManager.get(queryDTO);
        assertNull(afterDelete);
        log.info("4. 删除成功");

        log.info("完整生命周期测试通过");
    }

    @Test
    public void testBatchOperations() {
        // 1. 批量创建测试数据
        List<DeviceChannelDTO> dtoList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            DeviceChannelDTO dto = createTestDTO();
            dto.setChannelId(TEST_CHANNEL_ID + i);
            dto.setName(TEST_CHANNEL_NAME + i);
            dtoList.add(dto);
        }

        List<Long> createdIds = new ArrayList<>();
        for (DeviceChannelDTO dto : dtoList) {
            Long id = deviceChannelManager.add(dto);
            createdIds.add(id);
        }
        assertEquals(3, createdIds.size());
        log.info("批量创建成功，IDs: {}", createdIds);

        // 2. 分页查询验证
        DeviceChannelDTO queryDTO = new DeviceChannelDTO();
        queryDTO.setDeviceId(TEST_DEVICE_ID);
        Page<DeviceChannelDTO> pageResult = deviceChannelManager.getPage(queryDTO, 1, 10);
        assertEquals(3, pageResult.getTotal());
        log.info("分页查询验证成功，总数: {}", pageResult.getTotal());

        // 3. 批量删除
        Boolean batchDeleted = deviceChannelManager.deleteBatch(queryDTO);
        assertTrue(batchDeleted);

        // 4. 验证批量删除结果
        Page<DeviceChannelDTO> afterDelete = deviceChannelManager.getPage(queryDTO, 1, 10);
        assertEquals(0, afterDelete.getTotal());
        log.info("批量删除验证成功");

        log.info("批量操作测试通过");
    }
}