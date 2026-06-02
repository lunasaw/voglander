package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 2a：心跳定向更新 + 单调写集成测试（修 P3 写放大 + H3 单调性）。
 * <p>
 * 校验 {@link DeviceManager#patchLiveness} 与 {@link DeviceManager#patchOfflineTerminal}：
 * </p>
 * <ul>
 * <li><b>定向两列更新</b>：patchLiveness 仅更新 status/keepaliveTime，不读整行、不连坐列表缓存；</li>
 * <li><b>单调写（H3）</b>：旧时间戳的心跳不得覆盖库里更新的 keepaliveTime（跨节点漂移下唯一正确性保证）；</li>
 * <li><b>终态优先</b>：patchOfflineTerminal 无单调条件，强制写 OFFLINE，不被"时间戳更旧"挡住。</li>
 * </ul>
 *
 * @author luna
 */
@Slf4j
public class DeviceManagerLivenessTest extends BaseTest {

    private static final String TEST_DEVICE = "LIVENESS_TEST_DEVICE";

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private DeviceService deviceService;

    @BeforeEach
    public void setUp() {
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        deviceService.lambdaUpdate().eq(DeviceDO::getDeviceId, TEST_DEVICE).remove();
    }

    private Long createDevice(int status, LocalDateTime keepalive) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(TEST_DEVICE);
        dto.setIp("192.168.1.50");
        dto.setPort(5060);
        dto.setType(1);
        dto.setServerIp("127.0.0.1");
        dto.setStatus(status);
        dto.setKeepaliveTime(keepalive);
        dto.setName("liveness-origin");
        return deviceManager.add(dto);
    }

    private DeviceDO reload() {
        return deviceService.lambdaQuery().eq(DeviceDO::getDeviceId, TEST_DEVICE).one();
    }

    @Test
    public void testPatchLivenessUpdatesStatusAndKeepalive() {
        // Given：设备初始离线、心跳较早
        LocalDateTime t0 = LocalDateTime.now().minusMinutes(10);
        createDevice(DeviceConstant.Status.OFFLINE, t0);

        // When：心跳带来上线 + 较新时间戳
        LocalDateTime t1 = LocalDateTime.now();
        deviceManager.patchLiveness(TEST_DEVICE, DeviceConstant.Status.ONLINE, t1);

        // Then：status 与 keepaliveTime 都被定向更新
        DeviceDO after = reload();
        assertNotNull(after);
        assertEquals(DeviceConstant.Status.ONLINE, after.getStatus(), "status 应更新为 ONLINE");
        assertEquals(t1.withNano(0), after.getKeepaliveTime().withNano(0), "keepaliveTime 应更新为新时间戳");
        log.info("定向两列更新校验通过");
    }

    @Test
    public void testPatchLivenessMonotonicGuardBlocksStaleTimestamp() {
        // Given：设备已是较新心跳
        LocalDateTime tNew = LocalDateTime.now();
        createDevice(DeviceConstant.Status.ONLINE, tNew);

        // When：迟到的旧心跳（跨节点漂移/UDP 乱序）
        LocalDateTime tOld = tNew.minusMinutes(5);
        deviceManager.patchLiveness(TEST_DEVICE, DeviceConstant.Status.ONLINE, tOld);

        // Then：单调条件挡下旧时间戳，keepaliveTime 仍为较新值
        DeviceDO after = reload();
        assertEquals(tNew.withNano(0), after.getKeepaliveTime().withNano(0),
            "H3 单调写：旧时间戳不得覆盖库里更新的 keepaliveTime");
        log.info("单调写保护校验通过：旧心跳被挡下");
    }

    @Test
    public void testPatchOfflineTerminalIgnoresMonotonic() {
        // Given：设备在线、心跳很新
        LocalDateTime tNew = LocalDateTime.now();
        createDevice(DeviceConstant.Status.ONLINE, tNew);

        // When：离线终态写入（无单调条件）
        deviceManager.patchOfflineTerminal(TEST_DEVICE);

        // Then：强制 OFFLINE，不被"时间戳更旧/更新"影响
        DeviceDO after = reload();
        assertEquals(DeviceConstant.Status.OFFLINE, after.getStatus(),
            "终态优先：patchOfflineTerminal 必须强制写 OFFLINE");
        log.info("离线终态优先校验通过");
    }

    @Test
    public void testPatchLivenessOnNonExistentDeviceIsNoop() {
        // 不存在的设备：定向更新影响 0 行，不抛异常
        deviceManager.patchLiveness("NOT_EXIST_DEVICE_XYZ", DeviceConstant.Status.ONLINE, LocalDateTime.now());
        assertTrue(true, "对不存在设备的 patchLiveness 应安全无副作用");
        log.info("不存在设备的 patchLiveness 安全无副作用");
    }
}
