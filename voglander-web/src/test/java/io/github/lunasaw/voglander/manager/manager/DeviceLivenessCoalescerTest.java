package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.BaseTest;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 2a: 心跳合并测试。
 * <p>
 * 验证节点本地 lastPersistTs 合并逻辑：
 * <ul>
 *   <li>距上次持久化 < coalesceWindow(30s) → 仅刷缓存，不写 DB</li>
 *   <li>距上次持久化 >= coalesceWindow → 写 DB + 更新 lastPersistTs</li>
 *   <li>首次心跳 → 立即写 DB</li>
 * </ul>
 * </p>
 * <p>
 * 继承 {@link BaseTest} 以获得统一的测试环境隔离（TestRedisConfig、CacheTestConfig、@Transactional自动回滚）。
 * </p>
 *
 * @author luna
 */
@Slf4j
class DeviceLivenessCoalescerTest extends BaseTest {

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
        device.setServerIp("127.0.0.1");  // 必填字段
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());
        deviceService.save(device);
    }

    @Test
    void testFirstHeartbeatWritesToDb() {
        // 首次心跳应立即写 DB
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());

        DeviceDTO device = deviceManager.getDtoByDeviceId(testDeviceId);
        assertNotNull(device);
        assertEquals(DeviceConstant.Status.ONLINE, device.getStatus());
        assertTrue(device.getUpdateTime().isAfter(beforeUpdate), "首次心跳应更新 DB");
        log.info("首次心跳写 DB 验证通过");
    }

    @Test
    void testCoalescedHeartbeatSkipsDb() throws InterruptedException {
        // 第一次心跳
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());
        DeviceDTO device1 = deviceManager.getDtoByDeviceId(testDeviceId);
        LocalDateTime firstUpdateTime = device1.getUpdateTime();

        // 等待 1 秒（远小于 coalesceWindow 30s）
        Thread.sleep(1000);

        // 第二次心跳（应被合并，不写 DB）
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());
        DeviceDTO device2 = deviceManager.getDtoByDeviceId(testDeviceId);

        // updateTime 应该没变（因为没写 DB）
        assertEquals(firstUpdateTime, device2.getUpdateTime(), "合并窗口内心跳不应更新 DB");
        log.info("心跳合并跳过 DB 写验证通过");
    }

    @Test
    void testHeartbeatAfterCoalesceWindowWritesToDb() throws InterruptedException {
        // 第一次心跳
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());
        DeviceDTO device1 = deviceManager.getDtoByDeviceId(testDeviceId);
        LocalDateTime firstUpdateTime = device1.getUpdateTime();

        // 模拟 coalesceWindow 过期（实际测试中无法等待 30s，通过清空 lastPersistTs 模拟）
        deviceManager.clearCoalesceCache(testDeviceId);

        Thread.sleep(100);

        // 第二次心跳（coalesceWindow 已过，应写 DB）
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());
        DeviceDTO device2 = deviceManager.getDtoByDeviceId(testDeviceId);

        // updateTime 应该更新了
        assertTrue(device2.getUpdateTime().isAfter(firstUpdateTime), "超过合并窗口后应写 DB");
        log.info("超过合并窗口写 DB 验证通过");
    }

    @Test
    void testCoalescingPreservesMonotonicity() throws InterruptedException {
        // 新设备入库时 keepalive_time 默认被 DB 填为 now（NOT NULL DEFAULT CURRENT_TIMESTAMP），
        // 故首次心跳必须用「不早于 now」的时间戳才能通过单调条件写入；这里用 now+10min。
        LocalDateTime ts1 = LocalDateTime.now().plusMinutes(10);
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, ts1);

        DeviceDTO device1 = deviceManager.getDtoByDeviceId(testDeviceId);
        assertEquals(ts1, device1.getKeepaliveTime());

        // 清理合并缓存，强制下一次心跳走 DB 写路径（否则落在 30s 合并窗口内只刷缓存，不触发单调条件）。
        deviceManager.clearCoalesceCache(testDeviceId);

        // 更早的心跳（ts2 < ts1）应被单调条件挡下
        LocalDateTime ts2 = LocalDateTime.now().minusMinutes(10);
        deviceManager.patchLivenessWithCoalesce(testDeviceId, DeviceConstant.Status.ONLINE, ts2);

        DeviceDTO device2 = deviceManager.getDtoByDeviceId(testDeviceId);

        // keepaliveTime 应该仍是 ts1（旧时间戳被挡下）
        assertEquals(ts1, device2.getKeepaliveTime(), "旧心跳时间戳不应覆盖新状态");
        log.info("心跳合并保持单调性验证通过");
    }
}
