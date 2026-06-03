package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier.VoglanderBusinessNotifier;
import io.github.lunasaw.voglander.manager.event.ShardDispatcher;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-01/02/04：设备注册链路端到端测试（事件边界注入）。
 * <p>
 * 测试路径：GatewayEvent → VoglanderBusinessNotifier(@Async) → ShardDispatcher
 * → Gb28181ProtocolHandler → DeviceRegisterService.login → SQLite
 * </p>
 * 不使用 @Transactional：异步线程写 DB 在独立事务，@AfterEach 手动清理。
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class DeviceRegistrationE2eTest {

    private static final String CLIENT_ID = "34020000001320000099";

    @Autowired
    private VoglanderBusinessNotifier notifier;
    @Autowired
    private DeviceMapper              deviceMapper;

    /** 用 @MockitoSpyBean 包装真实 ShardDispatcher，捕获 dispatch 调用但保留真实逻辑 */
    @MockitoSpyBean
    private ShardDispatcher shardSpy;

    @AfterEach
    void cleanup() {
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
    }

    /**
     * 在 spy 上挂 latch，既捕获事件又继续真实 dispatch 逻辑。
     */
    private CopyOnWriteArrayList<DeviceEvent> armLatch(CountDownLatch latch) {
        CopyOnWriteArrayList<DeviceEvent> captured = new CopyOnWriteArrayList<>();
        doAnswer(inv -> {
            DeviceEvent e = inv.getArgument(0);
            captured.add(e);
            latch.countDown();
            inv.callRealMethod();
            return null;
        }).when(shardSpy).dispatch(any(DeviceEvent.class));
        return captured;
    }

    @Test
    @DisplayName("TC-01 Register 事件 → 异步分片 → handler → DB 入库在线设备")
    void register_event_persists_online_record() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        var captured = armLatch(latch);

        notifier.notify(new GatewayEvent("gb28181.Lifecycle.Register", CLIENT_ID, null,
            System.currentTimeMillis(),
            Map.of("remoteIp", "127.0.0.1", "remotePort", 5061, "expire", 3600,
                   "localIp", "127.0.0.1", "transport", "UDP"), "node-local"));

        assertThat(latch.await(5, SECONDS)).as("ShardDispatcher 超时未收到事件").isTrue();
        assertThat(captured.get(0).groupName()).isEqualTo("Lifecycle.Register");
        assertThat(captured.get(0).deviceId()).isEqualTo(CLIENT_ID);

        await().atMost(3, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d).isNotNull();
            assertThat(d.getStatus()).isEqualTo(1);
        });
        log.info("✅ TC-01 通过");
    }

    @Test
    @DisplayName("TC-02 Offline 事件 → cascadeOffline → status=0")
    void offline_event_sets_status_zero() throws InterruptedException {
        DeviceDO device = new DeviceDO();
        device.setDeviceId(CLIENT_ID);
        device.setStatus(1);
        device.setIp("127.0.0.1");
        device.setPort(5061);
        device.setServerIp("127.0.0.1");
        device.setType(1);
        deviceMapper.insert(device);

        CountDownLatch latch = new CountDownLatch(1);
        var captured = armLatch(latch);

        notifier.notify(new GatewayEvent("gb28181.Lifecycle.Offline", CLIENT_ID, null,
            System.currentTimeMillis(), null, "node-local"));

        assertThat(latch.await(5, SECONDS)).isTrue();
        assertThat(captured.get(0).groupName()).isEqualTo("Lifecycle.Offline");

        await().atMost(3, SECONDS).untilAsserted(() ->
            assertThat(deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID))
                .getStatus()).isEqualTo(0));
        log.info("✅ TC-02 通过");
    }

    @Test
    @DisplayName("TC-04 重复注册事件幂等，不产生重复记录")
    void duplicate_register_is_idempotent() throws InterruptedException {
        GatewayEvent event = new GatewayEvent("gb28181.Lifecycle.Register", CLIENT_ID, null,
            System.currentTimeMillis(),
            Map.of("remoteIp", "127.0.0.1", "remotePort", 5061, "expire", 3600,
                   "localIp", "127.0.0.1", "transport", "UDP"), "node-local");

        CountDownLatch latch1 = new CountDownLatch(1);
        armLatch(latch1);
        notifier.notify(event);
        latch1.await(5, SECONDS);
        await().atMost(3, SECONDS).until(() ->
            deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID)) != null);

        Long firstId = deviceMapper.selectOne(
            Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID)).getId();

        CountDownLatch latch2 = new CountDownLatch(1);
        armLatch(latch2);
        notifier.notify(event);
        latch2.await(5, SECONDS);
        Thread.sleep(500);

        assertThat(deviceMapper.selectList(
            Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID)))
            .hasSize(1)
            .allMatch(d -> d.getId().equals(firstId));
        log.info("✅ TC-04 通过");
    }
}
