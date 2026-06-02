package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 1.0.4 Stage 1-4 验收测试：batchUpsertWithStatus / cascadeOffline / patchChannelStatus / markMissing
 */
@Slf4j
@DisplayName("1.0.4 DeviceChannelManager Stage 1-4")
class DeviceChannel104Test extends BaseTest {

    @Autowired
    private DeviceChannelManager deviceChannelManager;
    @Autowired
    private DeviceChannelService deviceChannelService;

    private String deviceId;

    @BeforeEach
    void setUp() {
        deviceId = UniqueKeyFactory.deviceId();
    }

    @AfterEach
    void cleanup() {
        deviceChannelService.remove(
            new LambdaQueryWrapper<DeviceChannelDO>().eq(DeviceChannelDO::getDeviceId, deviceId));
    }

    // =========================================================
    // helpers
    // =========================================================

    private DeviceChannelDTO dto(String channelId, Integer status, LocalDateTime lastSeen) {
        DeviceChannelDTO d = new DeviceChannelDTO();
        d.setDeviceId(deviceId);
        d.setChannelId(channelId);
        d.setName("ch-" + channelId);
        d.setStatus(status);
        d.setLastSeenTime(lastSeen);
        d.setStatusSource("CATALOG");
        return d;
    }

    private DeviceChannelDO row(String channelId) {
        return deviceChannelService.getOne(
            new LambdaQueryWrapper<DeviceChannelDO>()
                .eq(DeviceChannelDO::getDeviceId, deviceId)
                .eq(DeviceChannelDO::getChannelId, channelId));
    }

    // =========================================================
    // Stage 1: batchUpsertWithStatus
    // =========================================================

    @Nested
    @DisplayName("Stage 1: batchUpsertWithStatus")
    class Stage1 {

        @Test
        @DisplayName("DeviceItem.status=ON 应落库 status=1")
        void shouldMapOnlineStatus() {
            String ch = UniqueKeyFactory.channelId();
            int n = deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now())));
            assertTrue(n > 0);
            assertEquals(DeviceConstant.Status.ONLINE, row(ch).getStatus());
        }

        @Test
        @DisplayName("DeviceItem.status=OFF 应落库 status=0")
        void shouldMapOfflineStatus() {
            String ch = UniqueKeyFactory.channelId();
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.OFFLINE, LocalDateTime.now())));
            assertEquals(DeviceConstant.Status.OFFLINE, row(ch).getStatus());
        }

        @Test
        @DisplayName("item.status=null 应保留 DB 原值")
        void shouldKeepExistingStatusWhenNull() {
            String ch = UniqueKeyFactory.channelId();
            LocalDateTime t1 = LocalDateTime.now().minusSeconds(10);
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, t1)));

            // 第二次 status=null，不覆盖
            DeviceChannelDTO d2 = dto(ch, null, LocalDateTime.now());
            d2.setStatusSource(null);
            deviceChannelManager.batchUpsertWithStatus(deviceId, List.of(d2));

            assertEquals(DeviceConstant.Status.ONLINE, row(ch).getStatus(), "status 应保持 ONLINE");
        }

        @Test
        @DisplayName("lastSeenTime 应写入 DB")
        void shouldFillLastSeenTime() {
            String ch = UniqueKeyFactory.channelId();
            LocalDateTime now = LocalDateTime.now();
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, now)));
            assertNotNull(row(ch).getLastSeenTime());
        }

        @Test
        @DisplayName("R2 单调：旧时间戳不覆盖新 status")
        void shouldRejectOlderLastSeenTime() {
            String ch = UniqueKeyFactory.channelId();
            LocalDateTime newer = LocalDateTime.now();
            LocalDateTime older = newer.minusSeconds(30);

            // 先用 newer 写 ONLINE
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, newer)));

            // 再用 older 试图写 OFFLINE → 应被单调挡下
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.OFFLINE, older)));

            assertEquals(DeviceConstant.Status.ONLINE, row(ch).getStatus(), "单调条件应挡住旧时间戳覆盖");
        }

        @Test
        @DisplayName("missing_count 应在通道再次出现时归零")
        void shouldResetMissingCountOnReappearance() {
            String ch = UniqueKeyFactory.channelId();
            // 先建通道，手动设 missing_count=2
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now().minusSeconds(60))));
            deviceChannelService.lambdaUpdate()
                .eq(DeviceChannelDO::getDeviceId, deviceId)
                .eq(DeviceChannelDO::getChannelId, ch)
                .set(DeviceChannelDO::getMissingCount, 2)
                .update();

            // 通道再次出现
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now())));

            assertEquals(0, row(ch).getMissingCount(), "再次出现应重置 missing_count=0");
        }
    }

    // =========================================================
    // Stage 2: cascadeOffline
    // =========================================================

    @Nested
    @DisplayName("Stage 2: cascadeOffline")
    class Stage2 {

        @Test
        @DisplayName("设备离线应把所有通道写 OFFLINE")
        void shouldOfflineAllChannels() {
            List<DeviceChannelDTO> channels = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                channels.add(dto(UniqueKeyFactory.channelId(), DeviceConstant.Status.ONLINE, LocalDateTime.now()));
            }
            deviceChannelManager.batchUpsertWithStatus(deviceId, channels);

            deviceChannelManager.cascadeOffline(deviceId);

            long onlineCount = deviceChannelService.count(
                new LambdaQueryWrapper<DeviceChannelDO>()
                    .eq(DeviceChannelDO::getDeviceId, deviceId)
                    .eq(DeviceChannelDO::getStatus, DeviceConstant.Status.ONLINE));
            assertEquals(0, onlineCount, "级联后所有通道应为 OFFLINE");
        }

        @Test
        @DisplayName("cascadeOffline 应写入 statusSource=OFFLINE_CASCADE")
        void shouldSetStatusSource() {
            String ch = UniqueKeyFactory.channelId();
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now())));

            deviceChannelManager.cascadeOffline(deviceId);

            assertEquals("OFFLINE_CASCADE", row(ch).getStatusSource());
        }

        @Test
        @DisplayName("cascadeOffline 幂等：重复调用不抛异常，第二次行数=0")
        void shouldBeIdempotent() {
            String ch = UniqueKeyFactory.channelId();
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now())));

            deviceChannelManager.cascadeOffline(deviceId);
            assertDoesNotThrow(() -> {
                int second = deviceChannelManager.cascadeOffline(deviceId);
                assertEquals(0, second, "第二次调用应返回 0");
            });
        }

        @Test
        @DisplayName("R1：cascadeOffline 不受 lastSeenTime 单调条件约束")
        void shouldNotApplyMonotonicGuard() {
            String ch = UniqueKeyFactory.channelId();
            // 故意设一个未来时间
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now().plusHours(1))));

            deviceChannelManager.cascadeOffline(deviceId);

            assertEquals(DeviceConstant.Status.OFFLINE, row(ch).getStatus(), "终态强写不受单调条件约束");
        }

        @Test
        @DisplayName("cascadeOffline 不影响其他设备通道")
        void shouldNotAffectOtherDevice() {
            String otherDevice = UniqueKeyFactory.deviceId();
            String otherCh = UniqueKeyFactory.channelId();
            try {
                DeviceChannelDTO other = new DeviceChannelDTO();
                other.setDeviceId(otherDevice);
                other.setChannelId(otherCh);
                other.setName("other");
                other.setStatus(DeviceConstant.Status.ONLINE);
                other.setLastSeenTime(LocalDateTime.now());
                other.setStatusSource("CATALOG");
                deviceChannelManager.batchUpsertWithStatus(otherDevice, List.of(other));

                // 对 deviceId 执行级联
                deviceChannelManager.cascadeOffline(deviceId);

                DeviceChannelDO otherRow = deviceChannelService.getOne(
                    new LambdaQueryWrapper<DeviceChannelDO>()
                        .eq(DeviceChannelDO::getDeviceId, otherDevice)
                        .eq(DeviceChannelDO::getChannelId, otherCh));
                assertNotNull(otherRow);
                assertEquals(DeviceConstant.Status.ONLINE, otherRow.getStatus(), "其他设备通道不受影响");
            } finally {
                deviceChannelService.remove(
                    new LambdaQueryWrapper<DeviceChannelDO>().eq(DeviceChannelDO::getDeviceId, otherDevice));
            }
        }
    }

    // =========================================================
    // Stage 3: patchChannelStatus 单调写
    // =========================================================

    @Nested
    @DisplayName("Stage 3: patchChannelStatus 单调写")
    class Stage3 {

        @Test
        @DisplayName("新时间戳应覆盖成功")
        void shouldAcceptNewerLastSeenTime() {
            String ch = UniqueKeyFactory.channelId();
            LocalDateTime t1 = LocalDateTime.now().minusSeconds(10);
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.OFFLINE, t1)));

            LocalDateTime t2 = LocalDateTime.now();
            boolean updated = deviceChannelManager.patchChannelStatus(
                deviceId, ch, DeviceConstant.Status.ONLINE, t2, "CATALOG");

            assertTrue(updated);
            assertEquals(DeviceConstant.Status.ONLINE, row(ch).getStatus());
        }

        @Test
        @DisplayName("旧时间戳应被单调条件挡下")
        void shouldRejectOlderTimestamp() {
            String ch = UniqueKeyFactory.channelId();
            LocalDateTime t100 = LocalDateTime.now();
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, t100)));

            LocalDateTime t50 = t100.minusSeconds(50);
            boolean updated = deviceChannelManager.patchChannelStatus(
                deviceId, ch, DeviceConstant.Status.OFFLINE, t50, "CATALOG");

            assertFalse(updated, "旧时间戳应被单调挡下");
            assertEquals(DeviceConstant.Status.ONLINE, row(ch).getStatus(), "status 应保持 ONLINE");
        }

        @Test
        @DisplayName("cascadeOffline 应绕过单调条件（R1）")
        void cascadeOfflineShouldBypassMonotonic() {
            String ch = UniqueKeyFactory.channelId();
            // 设一个未来时间
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now().plusHours(1))));

            deviceChannelManager.cascadeOffline(deviceId);

            assertEquals(DeviceConstant.Status.OFFLINE, row(ch).getStatus());
        }
    }

    // =========================================================
    // Stage 4: markMissingChannels（需开启 enable-missing-scan）
    // Stage 4 的 enable-missing-scan 默认 false，通过直接 SQL 操作验证 missing_count 字段存在
    // =========================================================

    @Nested
    @DisplayName("Stage 4: missing_count 字段验证")
    class Stage4 {

        @Test
        @DisplayName("missing_count 字段默认值应为 0")
        void shouldHaveMissingCountDefault() {
            String ch = UniqueKeyFactory.channelId();
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now())));

            DeviceChannelDO saved = row(ch);
            assertNotNull(saved.getMissingCount());
            assertEquals(0, saved.getMissingCount(), "新建通道 missing_count 应为 0");
        }

        @Test
        @DisplayName("通道再次出现时 missing_count 应重置为 0")
        void shouldResetMissingCountWhenReappears() {
            String ch = UniqueKeyFactory.channelId();
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now().minusMinutes(1))));

            // 手动设 missing_count=3 模拟失踪
            deviceChannelService.lambdaUpdate()
                .eq(DeviceChannelDO::getDeviceId, deviceId)
                .eq(DeviceChannelDO::getChannelId, ch)
                .set(DeviceChannelDO::getMissingCount, 3)
                .update();

            // 通道再次出现（时间戳更新）
            deviceChannelManager.batchUpsertWithStatus(deviceId,
                List.of(dto(ch, DeviceConstant.Status.ONLINE, LocalDateTime.now())));

            assertEquals(0, row(ch).getMissingCount(), "再次出现应重置 missing_count=0");
        }
    }
}
