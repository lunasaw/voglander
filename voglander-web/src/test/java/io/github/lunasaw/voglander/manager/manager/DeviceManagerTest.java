package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.BaseTest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.assembler.DeviceAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;

/**
 * DeviceManager单元测试类
 *
 * @author luna
 * @date 2023/12/30
 */
@Slf4j
public class DeviceManagerTest extends BaseTest {

    private final String                                                     TEST_DEVICE_ID = "TEST_DEVICE_001";
    private final Long                                                       TEST_ID        = 1L;
    @Autowired
    private DeviceManager                                                    deviceManager;

    @Autowired
    private CacheManager                                                     cacheManager;
    @MockitoBean
    private DeviceService                                                    deviceService;
    @MockitoBean
    private DeviceAssembler                                                  deviceAssembler;

    @MockitoBean
    private RedisCache                                                       redisCache;

    // Mock掉其他Manager依赖，避免复杂的Bean依赖链
    @MockitoBean
    private io.github.lunasaw.voglander.manager.manager.DeviceChannelManager deviceChannelManager;
    private DeviceDTO                                                        testDeviceDTO;
    private DeviceDO                                                         testDeviceDO;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        testDeviceDTO = createTestDeviceDTO();
        testDeviceDO = createTestDeviceDO();
    }

    /**
     * 创建测试用的DeviceDTO
     */
    private DeviceDTO createTestDeviceDTO() {
        DeviceDTO dto = new DeviceDTO();
        dto.setId(TEST_ID);
        dto.setDeviceId(TEST_DEVICE_ID);
        dto.setName("测试设备");
        dto.setIp("192.168.1.100");
        dto.setPort(5060);
        dto.setStatus(1);
        dto.setType(1);
        dto.setServerIp("192.168.1.1");
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        dto.setRegisterTime(LocalDateTime.now());
        dto.setKeepaliveTime(LocalDateTime.now());

        // 设置扩展信息
        DeviceDTO.ExtendInfo extendInfo = new DeviceDTO.ExtendInfo();
        extendInfo.setTransport("UDP");
        extendInfo.setExpires(3600);
        extendInfo.setCharset("UTF-8");
        dto.setExtendInfo(extendInfo);

        return dto;
    }

    /**
     * 创建测试用的DeviceDO
     */
    private DeviceDO createTestDeviceDO() {
        DeviceDO deviceDO = new DeviceDO();
        deviceDO.setId(TEST_ID);
        deviceDO.setDeviceId(TEST_DEVICE_ID);
        deviceDO.setName("测试设备");
        deviceDO.setIp("192.168.1.100");
        deviceDO.setPort(5060);
        deviceDO.setStatus(1);
        deviceDO.setType(1);
        deviceDO.setServerIp("192.168.1.1");
        deviceDO.setCreateTime(LocalDateTime.now());
        deviceDO.setUpdateTime(LocalDateTime.now());
        deviceDO.setRegisterTime(LocalDateTime.now());
        deviceDO.setKeepaliveTime(LocalDateTime.now());
        deviceDO.setExtend("{\"transport\":\"UDP\",\"expires\":3600,\"charset\":\"UTF-8\"}");
        return deviceDO;
    }

    @Test
    public void testCreateDevice_Success() {
        // Given
        when(deviceAssembler.toDeviceDO(testDeviceDTO)).thenReturn(testDeviceDO);
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(deviceService.save(any(DeviceDO.class))).thenReturn(true);

        // When
        Long result = deviceManager.createDevice(testDeviceDTO);

        // Then
        assertNotNull(result);
        verify(deviceService).save(any(DeviceDO.class));
        log.info("testCreateDevice_Success passed");
    }

    @Test
    public void testCreateDevice_DeviceIdAlreadyExists() {
        // Given
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(testDeviceDO);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            deviceManager.createDevice(testDeviceDTO);
        });

        assertTrue(exception.getMessage().contains("设备ID已存在"));
        log.info("testCreateDevice_DeviceIdAlreadyExists passed");
    }

    @Test
    public void testCreateDevice_NullDeviceDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deviceManager.createDevice(null);
        });
        log.info("testCreateDevice_NullDeviceDTO passed");
    }

    @Test
    public void testCreateDevice_NullDeviceId() {
        // Given
        testDeviceDTO.setDeviceId(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deviceManager.createDevice(testDeviceDTO);
        });
        log.info("testCreateDevice_NullDeviceId passed");
    }

    @Test
    public void testBatchCreateDevice_Success() {
        // Given
        List<DeviceDTO> deviceDTOList = Arrays.asList(testDeviceDTO, createTestDeviceDTO());
        deviceDTOList.get(1).setDeviceId("TEST_DEVICE_002");

        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(deviceAssembler.toDeviceDO(any(DeviceDTO.class))).thenReturn(testDeviceDO);
        when(deviceService.save(any(DeviceDO.class))).thenReturn(true);

        // When
        int result = deviceManager.batchCreateDevice(deviceDTOList);

        // Then
        assertEquals(2, result);
        verify(deviceService, times(2)).save(any(DeviceDO.class));
        log.info("testBatchCreateDevice_Success passed");
    }

    @Test
    public void testBatchCreateDevice_EmptyList() {
        // When
        int result = deviceManager.batchCreateDevice(Collections.emptyList());

        // Then
        assertEquals(0, result);
        log.info("testBatchCreateDevice_EmptyList passed");
    }

    @Test
    public void testBatchCreateDevice_NullList() {
        // When
        int result = deviceManager.batchCreateDevice(null);

        // Then
        assertEquals(0, result);
        log.info("testBatchCreateDevice_NullList passed");
    }

    @Test
    public void testUpdateDevice_Success() {
        // Given
        when(deviceAssembler.toDeviceDTO(any(DeviceDO.class))).thenReturn(testDeviceDTO);
        when(deviceService.getById(TEST_ID)).thenReturn(testDeviceDO);
        when(deviceAssembler.toDeviceDO(testDeviceDTO)).thenReturn(testDeviceDO);
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(testDeviceDO);
        when(deviceService.updateById(any(DeviceDO.class))).thenReturn(true);

        // When
        Long result = deviceManager.updateDevice(testDeviceDTO);

        // Then
        assertNotNull(result);
        verify(deviceService).updateById(any(DeviceDO.class));
        log.info("testUpdateDevice_Success passed");
    }

    @Test
    public void testUpdateDevice_DeviceNotExists() {
        // Given
        when(deviceService.getById(TEST_ID)).thenReturn(null);
        when(deviceAssembler.toDeviceDTO(null)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            deviceManager.updateDevice(testDeviceDTO);
        });

        assertTrue(exception.getMessage().contains("设备不存在"));
        log.info("testUpdateDevice_DeviceNotExists passed");
    }

    @Test
    public void testBatchUpdateDevice_Success() {
        // Given
        List<DeviceDTO> deviceDTOList = Arrays.asList(testDeviceDTO);

        when(deviceAssembler.toDeviceDTO(any(DeviceDO.class))).thenReturn(testDeviceDTO);
        when(deviceService.getById(TEST_ID)).thenReturn(testDeviceDO);
        when(deviceAssembler.toDeviceDO(any(DeviceDTO.class))).thenReturn(testDeviceDO);
        when(deviceService.updateById(any(DeviceDO.class))).thenReturn(true);

        // When
        int result = deviceManager.batchUpdateDevice(deviceDTOList);

        // Then
        assertEquals(1, result);
        verify(deviceService).updateById(any(DeviceDO.class));
        log.info("testBatchUpdateDevice_Success passed");
    }

    @Test
    public void testSaveOrUpdate_NewDevice() {
        // Given
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(deviceAssembler.toDeviceDO(testDeviceDTO)).thenReturn(testDeviceDO);
        when(deviceService.save(any(DeviceDO.class))).thenReturn(true);

        // When
        Long result = deviceManager.saveOrUpdate(testDeviceDTO);

        // Then
        assertNotNull(result);
        verify(deviceService).save(any(DeviceDO.class));
        log.info("testSaveOrUpdate_NewDevice passed");
    }

    @Test
    public void testSaveOrUpdate_ExistingDevice() {
        // Given
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(testDeviceDO);
        when(deviceAssembler.toDeviceDO(testDeviceDTO)).thenReturn(testDeviceDO);
        when(deviceService.updateById(any(DeviceDO.class))).thenReturn(true);

        // When
        Long result = deviceManager.saveOrUpdate(testDeviceDTO);

        // Then
        assertEquals(TEST_ID, result);
        verify(deviceService).updateById(any(DeviceDO.class));
        log.info("testSaveOrUpdate_ExistingDevice passed");
    }

    @Test
    public void testUpdateStatus_Success() {
        // Given
        int newStatus = 0;
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(testDeviceDO);
        when(deviceService.updateById(any(DeviceDO.class))).thenReturn(true);

        // When
        deviceManager.updateStatus(TEST_DEVICE_ID, newStatus);

        // Then
        ArgumentCaptor<DeviceDO> captor = ArgumentCaptor.forClass(DeviceDO.class);
        verify(deviceService).updateById(captor.capture());
        assertEquals(newStatus, captor.getValue().getStatus());
        log.info("testUpdateStatus_Success passed");
    }

    @Test
    public void testUpdateStatus_DeviceNotExists() {
        // Given
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(null);

        // When
        deviceManager.updateStatus(TEST_DEVICE_ID, 0);

        // Then
        verify(deviceService, never()).updateById(any(DeviceDO.class));
        log.info("testUpdateStatus_DeviceNotExists passed");
    }

    @Test
    public void testDeleteDevice_Success() {
        // Given
        when(deviceService.remove(any(QueryWrapper.class))).thenReturn(true);

        // When
        Boolean result = deviceManager.deleteDevice(TEST_DEVICE_ID);

        // Then
        assertTrue(result);
        verify(deviceService).remove(any(QueryWrapper.class));
        log.info("testDeleteDevice_Success passed");
    }

    @Test
    public void testGetByDeviceId_Success() {
        // Given
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(testDeviceDO);

        // When
        DeviceDO result = deviceManager.getByDeviceId(TEST_DEVICE_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        log.info("testGetByDeviceId_Success passed");
    }

    @Test
    public void testGetDtoByDeviceId_Success() {
        // Given
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(testDeviceDO);
        when(deviceAssembler.toDeviceDTO(testDeviceDO)).thenReturn(testDeviceDTO);

        // When
        DeviceDTO result = deviceManager.getDtoByDeviceId(TEST_DEVICE_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        log.info("testGetDtoByDeviceId_Success passed");
    }

    @Test
    public void testGetDeviceDTOById_Success() {
        // Given
        when(deviceService.getById(TEST_ID)).thenReturn(testDeviceDO);
        when(deviceAssembler.toDeviceDTO(testDeviceDO)).thenReturn(testDeviceDTO);

        // When
        DeviceDTO result = deviceManager.getDeviceDTOById(TEST_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        log.info("testGetDeviceDTOById_Success passed");
    }

    @Test
    public void testGetDeviceDTOByEntity_Success() {
        // Given
        DeviceDO queryDevice = new DeviceDO();
        queryDevice.setDeviceId(TEST_DEVICE_ID);

        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(testDeviceDO);
        when(deviceAssembler.toDeviceDTO(testDeviceDO)).thenReturn(testDeviceDTO);

        // When
        DeviceDTO result = deviceManager.getDeviceDTOByEntity(queryDevice);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        log.info("testGetDeviceDTOByEntity_Success passed");
    }

    @Test
    public void testListDeviceDTO_Success() {
        // Given
        DeviceDO queryDevice = new DeviceDO();
        queryDevice.setStatus(1);

        List<DeviceDO> deviceDOList = Arrays.asList(testDeviceDO);
        List<DeviceDTO> deviceDTOList = Arrays.asList(testDeviceDTO);

        when(deviceService.list(any(QueryWrapper.class))).thenReturn(deviceDOList);
        when(deviceAssembler.toDeviceDTOList(deviceDOList)).thenReturn(deviceDTOList);

        // When
        List<DeviceDTO> result = deviceManager.listDeviceDTO(queryDevice);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_DEVICE_ID, result.get(0).getDeviceId());
        log.info("testListDeviceDTO_Success passed");
    }

    @Test
    public void testListDeviceDTO_NullCondition() {
        // Given
        List<DeviceDO> deviceDOList = Arrays.asList(testDeviceDO);
        List<DeviceDTO> deviceDTOList = Arrays.asList(testDeviceDTO);

        when(deviceService.list(any(QueryWrapper.class))).thenReturn(deviceDOList);
        when(deviceAssembler.toDeviceDTOList(deviceDOList)).thenReturn(deviceDTOList);

        // When
        List<DeviceDTO> result = deviceManager.listDeviceDTO(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        log.info("testListDeviceDTO_NullCondition passed");
    }

    @Test
    public void testPageQuerySimple_Success() {
        // Given
        int page = 1;
        int size = 10;

        Page<DeviceDO> mockPage = new Page<>(page, size);
        mockPage.setRecords(Arrays.asList(testDeviceDO));
        mockPage.setTotal(1L);
        mockPage.setCurrent(page);
        mockPage.setSize(size);
        mockPage.setPages(1L);

        when(deviceService.page(any(Page.class))).thenReturn(mockPage);
        when(deviceAssembler.toDeviceDTOList(anyList())).thenReturn(Arrays.asList(testDeviceDTO));

        // When
        Page<DeviceDTO> result = deviceManager.pageQuerySimple(page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());
        assertEquals(page, result.getCurrent());
        assertEquals(size, result.getSize());
        log.info("testPageQuerySimple_Success passed");
    }

    @Test
    public void testPageQuery_Success() {
        // Given
        int page = 1;
        int size = 10;
        QueryWrapper<DeviceDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);

        Page<DeviceDO> mockPage = new Page<>(page, size);
        mockPage.setRecords(Arrays.asList(testDeviceDO));
        mockPage.setTotal(1L);
        mockPage.setCurrent(page);
        mockPage.setSize(size);
        mockPage.setPages(1L);

        when(deviceService.page(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);
        when(deviceAssembler.toDeviceDTOList(anyList())).thenReturn(Arrays.asList(testDeviceDTO));

        // When
        Page<DeviceDTO> result = deviceManager.pageQuery(page, size, queryWrapper);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());
        assertEquals(page, result.getCurrent());
        assertEquals(size, result.getSize());
        log.info("testPageQuery_Success passed");
    }

    @Test
    public void testGetByDeviceId_NullDeviceId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deviceManager.getByDeviceId(null);
        });
        log.info("testGetByDeviceId_NullDeviceId passed");
    }

    @Test
    public void testDeleteDevice_NullDeviceId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deviceManager.deleteDevice(null);
        });
        log.info("testDeleteDevice_NullDeviceId passed");
    }

    @Test
    public void testSaveOrUpdate_NullDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deviceManager.saveOrUpdate(null);
        });
        log.info("testSaveOrUpdate_NullDTO passed");
    }

    @Test
    public void testSaveOrUpdate_NullDeviceId() {
        // Given
        testDeviceDTO.setDeviceId(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deviceManager.saveOrUpdate(testDeviceDTO);
        });
        log.info("testSaveOrUpdate_NullDeviceId passed");
    }

    @Test
    public void testUpdateDevice_NullDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deviceManager.updateDevice(null);
        });
        log.info("testUpdateDevice_NullDTO passed");
    }

    @Test
    public void testUpdateDevice_NullId() {
        // Given
        testDeviceDTO.setId(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deviceManager.updateDevice(testDeviceDTO);
        });
        log.info("testUpdateDevice_NullId passed");
    }

    // ========== 缓存功能专项测试 ==========

    @Test
    public void testCacheableAnnotation_getDtoByDeviceId() {
        // Given
        Cache deviceCache = cacheManager.getCache("device");
        assertNotNull(deviceCache, "Device cache should exist");

        // 确保缓存是空的
        deviceCache.clear();

        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(testDeviceDO);
        when(deviceAssembler.toDeviceDTO(testDeviceDO)).thenReturn(testDeviceDTO);

        // When - 第一次调用，应该执行方法并缓存结果
        DeviceDTO result1 = deviceManager.getDtoByDeviceId(TEST_DEVICE_ID);

        // Then - 验证结果正确
        assertNotNull(result1);
        assertEquals(TEST_DEVICE_ID, result1.getDeviceId());

        // 验证缓存中有数据
        Cache.ValueWrapper cachedValue = deviceCache.get(TEST_DEVICE_ID);
        assertNotNull(cachedValue, "Value should be cached");
        assertNotNull(cachedValue.get(), "Cached value should not be null");

        // When - 第二次调用，应该从缓存获取
        DeviceDTO result2 = deviceManager.getDtoByDeviceId(TEST_DEVICE_ID);

        // Then - 结果应该一致，且Service只被调用一次（证明使用了缓存）
        assertEquals(result1.getDeviceId(), result2.getDeviceId());
        verify(deviceService, times(1)).getOne(any(QueryWrapper.class)); // 只调用一次

        log.info("testCacheableAnnotation_getDtoByDeviceId passed - 缓存功能正常");
    }

    @Test
    public void testCacheEvictAnnotation_deleteDevice() {
        // Given
        Cache deviceCache = cacheManager.getCache("device");
        assertNotNull(deviceCache);

        // 先在缓存中放入数据
        deviceCache.put(TEST_DEVICE_ID, testDeviceDTO);
        assertNotNull(deviceCache.get(TEST_DEVICE_ID), "Cache should contain the device");

        when(deviceService.remove(any(QueryWrapper.class))).thenReturn(true);

        // When - 调用删除方法（带有@CacheEvict注解）
        Boolean result = deviceManager.deleteDevice(TEST_DEVICE_ID);

        // Then - 验证删除成功
        assertTrue(result);

        // 验证缓存被清除
        Cache.ValueWrapper cachedValue = deviceCache.get(TEST_DEVICE_ID);
        assertNull(cachedValue, "Cache should be evicted after delete");

        log.info("testCacheEvictAnnotation_deleteDevice passed - 缓存清除功能正常");
    }

    @Test
    public void testCacheEvictAllEntries_batchCreateDevice() {
        // Given
        Cache deviceCache = cacheManager.getCache("device");
        assertNotNull(deviceCache);

        // 在缓存中放入多个设备数据
        deviceCache.put("DEVICE_001", testDeviceDTO);
        deviceCache.put("DEVICE_002", testDeviceDTO);
        deviceCache.put("DEVICE_003", testDeviceDTO);

        // 验证缓存中有数据
        assertNotNull(deviceCache.get("DEVICE_001"));
        assertNotNull(deviceCache.get("DEVICE_002"));
        assertNotNull(deviceCache.get("DEVICE_003"));

        List<DeviceDTO> deviceDTOList = Arrays.asList(testDeviceDTO);
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(deviceAssembler.toDeviceDO(any(DeviceDTO.class))).thenReturn(testDeviceDO);
        when(deviceService.save(any(DeviceDO.class))).thenReturn(true);

        // When - 调用批量创建方法（带有@CacheEvict(allEntries = true)注解）
        int result = deviceManager.batchCreateDevice(deviceDTOList);

        // Then - 验证操作成功
        assertEquals(1, result);

        // 验证所有缓存都被清除
        assertNull(deviceCache.get("DEVICE_001"), "All cache entries should be evicted");
        assertNull(deviceCache.get("DEVICE_002"), "All cache entries should be evicted");
        assertNull(deviceCache.get("DEVICE_003"), "All cache entries should be evicted");

        log.info("testCacheEvictAllEntries_batchCreateDevice passed - 全量缓存清除功能正常");
    }

    @Test
    public void testCacheConditional_getDtoByDeviceId_NullResult() {
        // Given
        Cache deviceCache = cacheManager.getCache("device");
        assertNotNull(deviceCache);
        deviceCache.clear();

        // Mock返回null（模拟设备不存在）
        when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(deviceAssembler.toDeviceDTO(null)).thenReturn(null);

        // When - 调用方法
        DeviceDTO result = deviceManager.getDtoByDeviceId("NON_EXISTENT_DEVICE");

        // Then - 验证结果为null
        assertNull(result);

        // 验证null结果没有被缓存（因为有unless = "#result == null"条件）
        Cache.ValueWrapper cachedValue = deviceCache.get("NON_EXISTENT_DEVICE");
        assertNull(cachedValue, "Null result should not be cached due to 'unless' condition");

        log.info("testCacheConditional_getDtoByDeviceId_NullResult passed - 缓存条件判断功能正常");
    }


}