package io.github.lunasaw.voglander.manager.manager;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeviceManager真实数据库集成测试
 * 验证数据库操作是否存在事务一致性问题
 * 
 * @author luna
 * @date 2025/8/2
 */
@Slf4j
public class DeviceManagerIntegrationTest extends BaseTest {

    @Autowired
    private DeviceManager deviceManager;

    @Test
    public void testSaveOrUpdateWithRealDatabase() {
        String testDeviceId = "TEST_REAL_DB_" + System.currentTimeMillis();

        // 1. 创建测试设备
        DeviceDTO testDevice = createTestDevice(testDeviceId);

        log.info("=== 步骤1: 创建设备 ===");
        Long createdId = deviceManager.saveOrUpdate(testDevice);
        assertNotNull(createdId, "设备创建应该成功");
        log.info("设备创建成功，ID: {}", createdId);

        // 2. 立即查询验证
        log.info("=== 步骤2: 创建后立即查询 ===");
        DeviceDO foundDevice = deviceManager.getByDeviceId(testDeviceId);

        if (foundDevice == null) {
            log.error("❌ 问题重现：设备创建成功但立即查询为null");
            log.error("设备ID: {}, 创建返回的ID: {}", testDeviceId, createdId);

            // 尝试短暂等待后再查询
            try {
                Thread.sleep(100);
                DeviceDO retryFound = deviceManager.getByDeviceId(testDeviceId);
                if (retryFound != null) {
                    log.warn("⚠️ 等待100ms后能查询到，可能是事务提交延迟问题");
                } else {
                    log.error("❌ 等待后仍查询不到，可能是严重的数据库一致性问题");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            fail("设备创建成功后立即查询应该能找到设备");
        } else {
            log.info("✅ 正常：设备创建后立即查询成功");
            assertEquals(testDeviceId, foundDevice.getDeviceId());
            assertEquals(createdId, foundDevice.getId());
        }

        // 3. 更新操作验证
        log.info("=== 步骤3: 更新设备后查询 ===");
        testDevice.setName("更新后的设备名称");
        testDevice.setId(createdId);

        Long updatedId = deviceManager.saveOrUpdate(testDevice);
        assertEquals(createdId, updatedId, "更新操作应该返回相同的ID");

        DeviceDO updatedDevice = deviceManager.getByDeviceId(testDeviceId);
        assertNotNull(updatedDevice, "更新后应该能查询到设备");
        assertEquals("更新后的设备名称", updatedDevice.getName());

        log.info("✅ 设备管理器数据库操作测试通过");
    }

    private DeviceDTO createTestDevice(String deviceId) {
        DeviceDTO device = new DeviceDTO();
        device.setDeviceId(deviceId);
        device.setName("测试设备_真实数据库");
        device.setIp("127.0.0.1");
        device.setPort(5060);
        device.setStatus(1);
        device.setType(DeviceAgreementEnum.GB28181_IPC.getType());
        device.setRegisterTime(LocalDateTime.now());
        device.setKeepaliveTime(LocalDateTime.now());
        device.setServerIp("127.0.0.1");

        DeviceDTO.ExtendInfo extendInfo = new DeviceDTO.ExtendInfo();
        extendInfo.setTransport("TCP");
        extendInfo.setStreamMode("TCP-ACTIVE");
        extendInfo.setCharset("UTF-8");
        device.setExtendInfo(extendInfo);

        return device;
    }
}