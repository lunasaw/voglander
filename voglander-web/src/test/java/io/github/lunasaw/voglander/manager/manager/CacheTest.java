package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存功能集成测试类，验证Spring Cache在完整数据链路下的功能
 * 使用真实的数据库操作和完整的Spring容器
 *
 * @author luna
 * @date 2023/12/30
 */
@Slf4j
public class CacheTest extends BaseTest {

    private final String  TEST_DEVICE_ID         = "TEST_DEVICE_001";
    private final String  NON_EXISTENT_DEVICE_ID = "NON_EXISTENT_DEVICE";

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private CacheManager  cacheManager;

    @Autowired
    private DeviceService deviceService;


    @BeforeEach
    public void setUp() {
        // 清理测试数据和缓存
        cleanUpTestData();

        log.debug("Test setup completed - data and cache cleared");
    }

    @AfterEach
    public void tearDown() {
        // 测试结束后清理数据
        cleanUpTestData();

        log.debug("Test teardown completed - data cleaned up");
    }

    /**
     * 清理测试数据和缓存
     */
    private void cleanUpTestData() {
        // 清理缓存
        Cache deviceCache = cacheManager.getCache("device");
        if (deviceCache != null) {
            deviceCache.clear();
        }

        // 清理测试设备数据
        try {
            QueryWrapper<DeviceDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("device_id", TEST_DEVICE_ID);
            deviceService.remove(queryWrapper);
        } catch (Exception e) {
            log.debug("Failed to clean up test device: {}", e.getMessage());
        }
    }

    /**
     * 创建并插入测试设备到数据库
     */
    private DeviceDTO createAndInsertTestDevice() {
        DeviceDO deviceDO = new DeviceDO();
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

        // 插入到数据库
        boolean saved = deviceService.save(deviceDO);
        if (!saved) {
            throw new RuntimeException("Failed to save test device");
        }

        // 通过Manager获取DTO（会触发缓存）
        return deviceManager.getDtoByDeviceId(TEST_DEVICE_ID);
    }

    @Test
    public void testCacheableAnnotation_getDtoByDeviceId() {
        // Given - 创建并插入测试设备
        DeviceDTO insertedDevice = createAndInsertTestDevice();

        Cache deviceCache = cacheManager.getCache("device");
        assertNotNull(deviceCache, "Device cache should exist");

        // 清理缓存以确保测试的准确性
        deviceCache.clear();

        // When - 第一次调用，应该执行方法并缓存结果
        DeviceDTO result1 = deviceManager.getDtoByDeviceId(TEST_DEVICE_ID);

        // Then - 验证结果正确
        assertNotNull(result1);
        assertEquals(TEST_DEVICE_ID, result1.getDeviceId());
        assertEquals(insertedDevice.getName(), result1.getName());

        // 验证缓存中有数据
        Cache.ValueWrapper cachedValue = deviceCache.get(TEST_DEVICE_ID);
        assertNotNull(cachedValue, "Value should be cached");
        assertNotNull(cachedValue.get(), "Cached value should not be null");

        // When - 第二次调用，应该从缓存获取
        DeviceDTO result2 = deviceManager.getDtoByDeviceId(TEST_DEVICE_ID);

        // Then - 结果应该一致
        assertEquals(result1.getDeviceId(), result2.getDeviceId());
        assertEquals(result1.getName(), result2.getName());

        log.info("testCacheableAnnotation_getDtoByDeviceId passed - 缓存功能正常");
    }

    @Test
    public void testCacheEvictAnnotation_deleteDevice() {
        // Given - 创建并插入测试设备
        DeviceDTO insertedDevice = createAndInsertTestDevice();

        Cache deviceCache = cacheManager.getCache("device");
        assertNotNull(deviceCache);

        // 验证设备已被缓存（因为createAndInsertTestDevice中调用了getDtoByDeviceId）
        Cache.ValueWrapper cachedValue = deviceCache.get(TEST_DEVICE_ID);
        assertNotNull(cachedValue, "Device should be cached after creation");

        // When - 调用删除方法（带有@CacheEvict注解）
        Boolean result = deviceManager.deleteDevice(TEST_DEVICE_ID);

        // Then - 验证删除成功
        assertTrue(result);

        // 验证缓存被清除
        cachedValue = deviceCache.get(TEST_DEVICE_ID);
        assertNull(cachedValue, "Cache should be evicted after delete");

        // 验证数据库中设备也被删除
        DeviceDTO deletedDevice = null;
        try {
            deletedDevice = deviceManager.getDtoByDeviceId(TEST_DEVICE_ID);
        } catch (Exception e) {
            log.debug("Device not found after deletion, which is expected: {}", e.getMessage());
        }
        assertNull(deletedDevice, "Device should be deleted from database");

        log.info("testCacheEvictAnnotation_deleteDevice passed - 缓存清除功能正常");
    }

    @Test
    public void testCacheConditional_getDtoByDeviceId_NullResult() {
        // Given
        Cache deviceCache = cacheManager.getCache("device");
        assertNotNull(deviceCache);
        deviceCache.clear();

        // When - 调用不存在的设备ID
        DeviceDTO result = deviceManager.getDtoByDeviceId(NON_EXISTENT_DEVICE_ID);

        // Then - 验证结果为null
        assertNull(result);

        // 验证null结果没有被缓存（因为有unless = "#result == null"条件）
        Cache.ValueWrapper cachedValue = deviceCache.get(NON_EXISTENT_DEVICE_ID);
        assertNull(cachedValue, "Null result should not be cached due to 'unless' condition");

        log.info("testCacheConditional_getDtoByDeviceId_NullResult passed - 缓存条件判断功能正常");
    }
}