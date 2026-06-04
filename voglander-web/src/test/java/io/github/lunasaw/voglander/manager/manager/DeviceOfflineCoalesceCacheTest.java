package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 2a: 设备下线缓存清理测试。
 * <p>
 * 验证 offline() 操作清理 coalesce cache：
 * <ul>
 *   <li>设备下线后 lastPersistTs 被清空</li>
 *   <li>设备重新上线第一个心跳立即写 DB（而非误判为"30s 内重复"）</li>
 * </ul>
 * </p>
 *
 * @author luna
 */
@Slf4j
@SpringBootTest(classes = ApplicationWeb.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "sip.enable=false")
@Transactional
class DeviceOfflineCoalesceCacheTest {

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private DeviceService deviceService;

    private String testDeviceId;

    @BeforeEach
    void setUp() {
        testDeviceId = "test-device-" + System.currentTimeMillis();

        // 创建测试设备（设置所有必填字段）
        DeviceDO device = new DeviceDO();
        device.setDeviceId(testDeviceId);
        device.setName("测试设备");
        device.setType(1);
        device.setStatus(DeviceConstant.Status.OFFLINE);
        device.setIp("127.0.0.1");
        device.setPort(5060);
        device.setServerIp("127.0.0.1");
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());
        deviceService.save(device);
    }

    @Test
    void testOfflineClearsCacheAndAllowsImmediateWrite() throws InterruptedException {
        // 1. 模拟设备上线：第一次心跳写 DB
        LocalDateTime firstHeartbeat = LocalDateTime.now();
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, firstHeartbeat);

        DeviceDTO device1 = deviceManager.getDtoByDeviceId(testDeviceId);
        LocalDateTime firstUpdateTime = device1.getUpdateTime();
        log.info("第一次心跳写 DB，updateTime={}", firstUpdateTime);

        // 2. 等待 1 秒后第二次心跳（应被合并，不写 DB）
        Thread.sleep(1000);
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());

        DeviceDTO device2 = deviceManager.getDtoByDeviceId(testDeviceId);
        assertEquals(firstUpdateTime, device2.getUpdateTime(), "窗口内心跳应被合并");
        log.info("第二次心跳被合并，updateTime 未变");

        // 3. 设备下线（清理缓存）
        deviceManager.clearCoalesceCache(testDeviceId);
        log.info("设备下线，清理 coalesce cache");

        // 4. 等待 1 秒后设备重新上线（第一次心跳应立即写 DB，而非被误判为"重复"）
        Thread.sleep(1000);
        LocalDateTime reOnlineHeartbeat = LocalDateTime.now();
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, reOnlineHeartbeat);

        DeviceDTO device3 = deviceManager.getDtoByDeviceId(testDeviceId);
        assertTrue(device3.getUpdateTime().isAfter(firstUpdateTime),
                "下线后重新上线的首次心跳应立即写 DB，updateTime 应更新");
        log.info("重新上线首次心跳写 DB，updateTime={}", device3.getUpdateTime());
    }

    @Test
    void testClearCacheWithoutPriorHeartbeat() {
        // 直接清理缓存（设备从未发送过心跳）不应抛异常
        assertDoesNotThrow(() -> deviceManager.clearCoalesceCache(testDeviceId),
                "清理不存在的缓存项不应抛异常");
        log.info("清理不存在的缓存项验证通过");
    }

    @Test
    void testMultipleOfflineOnlineRounds() throws InterruptedException {
        // 第一轮：上线 → 心跳 → 下线
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());
        DeviceDTO device1 = deviceManager.getDtoByDeviceId(testDeviceId);
        LocalDateTime updateTime1 = device1.getUpdateTime();

        deviceManager.clearCoalesceCache(testDeviceId);
        Thread.sleep(500);

        // 第二轮：上线 → 心跳（应立即写 DB）
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());
        DeviceDTO device2 = deviceManager.getDtoByDeviceId(testDeviceId);
        LocalDateTime updateTime2 = device2.getUpdateTime();
        assertTrue(updateTime2.isAfter(updateTime1), "第二轮首次心跳应写 DB");

        deviceManager.clearCoalesceCache(testDeviceId);
        Thread.sleep(500);

        // 第三轮：上线 → 心跳（应立即写 DB）
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());
        DeviceDTO device3 = deviceManager.getDtoByDeviceId(testDeviceId);
        LocalDateTime updateTime3 = device3.getUpdateTime();
        assertTrue(updateTime3.isAfter(updateTime2), "第三轮首次心跳应写 DB");

        log.info("多轮下线-上线验证通过：updateTime1={}, updateTime2={}, updateTime3={}",
                updateTime1, updateTime2, updateTime3);
    }
}
