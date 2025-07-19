package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Date;

import io.github.lunasaw.voglander.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import io.github.lunasaw.voglander.manager.assembler.DeviceAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存功能测试类，验证Spring Cache的参数名解析是否正常工作
 *
 * @author luna
 * @date 2023/12/30
 */
@Slf4j
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(
    properties = {
        "spring.cache.type=simple",
        "spring.main.banner-mode=off",
        "logging.level.org.springframework=ERROR"
    })
public class CacheTest {

    private final String                                                     TEST_DEVICE_ID = "TEST_DEVICE_001";
    private final Long                                                       TEST_ID        = 1L;

    @Autowired
    private DeviceManager                                                    deviceManager;

    @Autowired
    private CacheManager                                                     cacheManager;

    @MockBean
    private DeviceService                                                    deviceService;

    @MockBean
    private DeviceAssembler                                                  deviceAssembler;

    @MockBean
    private RedisCache                                                       redisCache;

    @MockBean
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