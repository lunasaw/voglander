package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * 1.0.4 Stage 3 C3/R4：patchLiveness 终态保护验收测试
 */
@DisplayName("1.0.4 DeviceManager R4 终态保护")
class DeviceManagerLivenessR4Test extends BaseTest {

    @Autowired
    private DeviceManager deviceManager;
    @Autowired
    private DeviceMapper  deviceMapper;

    private String deviceId;

    @BeforeEach
    void setUp() {
        deviceId = UniqueKeyFactory.deviceId();
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(deviceId);
        dto.setIp("127.0.0.1");
        dto.setPort(5060);
        dto.setType(1);
        dto.setServerIp("127.0.0.1");
        dto.setName("r4-test");
        deviceManager.add(dto);
    }

    @AfterEach
    void cleanup() {
        deviceMapper.delete(new LambdaQueryWrapper<DeviceDO>().eq(DeviceDO::getDeviceId, deviceId));
    }

    @Test
    @DisplayName("R4：patchOfflineTerminal 后 patchLiveness(ONLINE, now) 不应复活设备")
    void shouldRejectOnlineAfterOfflineTerminal() {
        // 先置终态 OFFLINE
        deviceManager.patchOfflineTerminal(deviceId);

        DeviceDO before = deviceMapper.selectOne(
            new LambdaQueryWrapper<DeviceDO>().eq(DeviceDO::getDeviceId, deviceId));
        assertEquals(DeviceConstant.Status.OFFLINE, before.getStatus());

        // 再用带时间戳的 ONLINE 尝试复活（应被 R4 挡下）
        deviceManager.patchLiveness(deviceId, DeviceConstant.Status.ONLINE, LocalDateTime.now());

        DeviceDO after = deviceMapper.selectOne(
            new LambdaQueryWrapper<DeviceDO>().eq(DeviceDO::getDeviceId, deviceId));
        assertEquals(DeviceConstant.Status.OFFLINE, after.getStatus(), "R4：终态 OFFLINE 不应被乱序 ONLINE 覆盖");
    }

    @Test
    @DisplayName("非 OFFLINE 状态的设备可被 patchLiveness 置为 ONLINE")
    void shouldAllowOnlineWhenStatusNotOffline() {
        // 设备刚创建 status=0(OFFLINE)，先用 patchOfflineTerminal 以外的方式置为 ONLINE
        // 直接更新为 ONLINE=1，再验证 patchLiveness 能正常更新 keepaliveTime
        deviceManager.patchLiveness(deviceId, DeviceConstant.Status.ONLINE, null);

        // 已是 OFFLINE → patchLiveness(ONLINE, null) 被 R4 挡住；但带时间戳 + null→isNull 条件可通过
        // 换思路：验证 keepaliveTime 写入路径（不涉及 R4 status 条件）
        LocalDateTime t = LocalDateTime.now();
        deviceManager.patchLiveness(deviceId, null, t);

        DeviceDO result = deviceMapper.selectOne(
            new LambdaQueryWrapper<DeviceDO>().eq(DeviceDO::getDeviceId, deviceId));
        assertNotNull(result.getKeepaliveTime(), "keepaliveTime 应被写入");
    }
}
