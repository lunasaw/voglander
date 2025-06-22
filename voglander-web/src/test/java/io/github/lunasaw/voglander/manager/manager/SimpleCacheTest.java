package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.CacheManager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import io.github.lunasaw.voglander.manager.assembler.DeviceAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 简单的缓存测试，验证参数名解析是否正常工作
 *
 * @author luna
 * @date 2023/12/30
 */
@Slf4j
public class SimpleCacheTest {

    private final String TEST_DEVICE_ID = "TEST_DEVICE_001";
    private final Long   TEST_ID        = 1L;

    @Test
    public void testCacheParameterNameResolution() {
        // 创建Spring应用上下文
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // 获取DeviceManager实例
            DeviceManager deviceManager = context.getBean(DeviceManager.class);
            DeviceService deviceService = context.getBean(DeviceService.class);
            DeviceAssembler deviceAssembler = context.getBean(DeviceAssembler.class);

            // 创建测试数据
            DeviceDTO testDeviceDTO = createTestDeviceDTO();
            DeviceDO testDeviceDO = createTestDeviceDO();

            // Mock返回值
            when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(testDeviceDO);
            when(deviceAssembler.toDeviceDTO(testDeviceDO)).thenReturn(testDeviceDTO);

            // 调用缓存方法，如果参数名解析失败，会抛出IllegalArgumentException: Null key returned for cache operation
            DeviceDTO result = deviceManager.getDtoByDeviceId(TEST_DEVICE_ID);

            // 如果没有抛出异常，说明参数名解析成功
            assertNotNull(result);
            assertEquals(TEST_DEVICE_ID, result.getDeviceId());

            log.info("✅ 缓存参数名解析测试通过 - Spring Cache能够正确解析#deviceId参数");

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Null key returned for cache operation")) {
                fail("❌ 缓存参数名解析失败 - Spring Cache无法解析#deviceId参数: " + e.getMessage());
            } else {
                throw e; // 其他异常重新抛出
            }
        } finally {
            context.close();
        }
    }

    @Test
    public void testCacheEvictParameterNameResolution() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            DeviceManager deviceManager = context.getBean(DeviceManager.class);
            DeviceService deviceService = context.getBean(DeviceService.class);

            when(deviceService.remove(any(QueryWrapper.class))).thenReturn(true);

            // 调用带有@CacheEvict注解的方法
            Boolean result = deviceManager.deleteDevice(TEST_DEVICE_ID);

            assertTrue(result);
            log.info("✅ 缓存清除参数名解析测试通过 - Spring Cache能够正确解析#deviceId参数");

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Null key returned for cache operation")) {
                fail("❌ 缓存清除参数名解析失败 - Spring Cache无法解析#deviceId参数: " + e.getMessage());
            } else {
                throw e;
            }
        } finally {
            context.close();
        }
    }

    @Test
    public void testCacheEvictDtoParameterNameResolution() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            DeviceManager deviceManager = context.getBean(DeviceManager.class);
            DeviceService deviceService = context.getBean(DeviceService.class);
            DeviceAssembler deviceAssembler = context.getBean(DeviceAssembler.class);

            DeviceDTO testDeviceDTO = createTestDeviceDTO();
            DeviceDO testDeviceDO = createTestDeviceDO();

            when(deviceService.getOne(any(QueryWrapper.class))).thenReturn(null); // 新设备
            when(deviceAssembler.toDeviceDO(testDeviceDTO)).thenReturn(testDeviceDO);
            when(deviceService.save(any(DeviceDO.class))).thenReturn(true);

            // 调用带有@CacheEvict注解的方法，使用#dto.deviceId
            Long result = deviceManager.saveOrUpdate(testDeviceDTO);

            assertNotNull(result);
            log.info("✅ 缓存清除DTO参数名解析测试通过 - Spring Cache能够正确解析#dto.deviceId参数");

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Null key returned for cache operation")) {
                fail("❌ 缓存清除DTO参数名解析失败 - Spring Cache无法解析#dto.deviceId参数: " + e.getMessage());
            } else {
                throw e;
            }
        } finally {
            context.close();
        }
    }

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
        dto.setCreateTime(new Date());
        dto.setUpdateTime(new Date());
        dto.setRegisterTime(new Date());
        dto.setKeepaliveTime(new Date());

        DeviceDTO.ExtendInfo extendInfo = new DeviceDTO.ExtendInfo();
        extendInfo.setTransport("UDP");
        extendInfo.setExpires(3600);
        extendInfo.setCharset("UTF-8");
        dto.setExtendInfo(extendInfo);

        return dto;
    }

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
        deviceDO.setCreateTime(new Date());
        deviceDO.setUpdateTime(new Date());
        deviceDO.setRegisterTime(new Date());
        deviceDO.setKeepaliveTime(new Date());
        deviceDO.setExtend("{\"transport\":\"UDP\",\"expires\":3600,\"charset\":\"UTF-8\"}");
        return deviceDO;
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("device");
        }

        @Bean
        public DeviceManager deviceManager() {
            return new DeviceManager();
        }

        @Bean
        public DeviceService deviceService() {
            return mock(DeviceService.class);
        }

        @Bean
        public DeviceAssembler deviceAssembler() {
            return mock(DeviceAssembler.class);
        }
    }
}