package io.github.lunasaw.voglander.manager.manager;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

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
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.event.ShardDispatcher;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-QUERY-01/04：设备信息查询测试。
 * <p>
 * 不继承 BaseTest（避免 @Transactional 与 @Async 的事务隔离冲突），改用 @AfterEach 手动清理。
 */
@Slf4j
@DisplayName("设备信息查询 Manager 集成测试")
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class DeviceQueryManagerTest {

    @Autowired private DeviceManager             deviceManager;
    @Autowired private DeviceMapper              deviceMapper;
    @Autowired private VoglanderBusinessNotifier notifier;
    @MockitoSpyBean private ShardDispatcher      shardSpy;

    private String lastDeviceId;

    @AfterEach
    void cleanup() {
        if (lastDeviceId != null) {
            deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, lastDeviceId));
        }
    }

    @Test
    @DisplayName("TC-QUERY-01 DeviceInfo 响应 → extend 字段包含厂商信息")
    void deviceInfoResponse_updatesDeviceExtend() throws InterruptedException {
        lastDeviceId = UniqueKeyFactory.deviceId();
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(lastDeviceId);
        dto.setStatus(1);
        dto.setIp("127.0.0.1");
        dto.setPort(5060);
        dto.setType(1);
        dto.setServerIp("127.0.0.1");
        deviceManager.add(dto);

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return inv.callRealMethod(); })
            .when(shardSpy).dispatch(any(DeviceEvent.class));

        notifier.notify(new GatewayEvent("gb28181.Response.DeviceInfo", lastDeviceId, null,
            System.currentTimeMillis(),
            Map.of("manufacturer", "Hikvision", "model", "DS-2CD2T47G2",
                   "firmware", "V5.7.3", "sn", "1"),
            "node-local"));

        assertThat(latch.await(5, SECONDS)).isTrue();
        await().atMost(3, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getDeviceId, lastDeviceId));
            assertThat(d).isNotNull();
            assertThat(d.getExtend()).isNotNull().contains("Hikvision");
        });
        log.info("✅ TC-QUERY-01 通过");
    }

    @Test
    @DisplayName("TC-QUERY-04 缓存命中 → 重复 get() 返回一致结果")
    void deviceGet_repeatedCalls_consistent() {
        lastDeviceId = UniqueKeyFactory.deviceId();
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(lastDeviceId);
        dto.setStatus(1);
        dto.setIp("127.0.0.1");
        dto.setPort(5060);
        dto.setType(1);
        dto.setServerIp("127.0.0.1");
        deviceManager.add(dto);

        DeviceDTO r1 = deviceManager.getDtoByDeviceId(lastDeviceId);
        DeviceDTO r2 = deviceManager.getDtoByDeviceId(lastDeviceId);
        DeviceDTO r3 = deviceManager.getDtoByDeviceId(lastDeviceId);

        assertThat(r1).isNotNull();
        assertThat(r2.getDeviceId()).isEqualTo(r1.getDeviceId());
        assertThat(r3.getDeviceId()).isEqualTo(r1.getDeviceId());
        log.info("✅ TC-QUERY-04 通过");
    }
}
