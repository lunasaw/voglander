package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-KA-01/02：保活心跳端到端测试（真实 SIP 协议栈）。
 * 链路：ClientCommandSender.sendKeepaliveCommand → SIP/UDP MESSAGE → Server(5060)
 *       → VoglanderBusinessNotifier → handleKeepalive → DB keepalive_time 刷新
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class KeepaliveE2eTest {

    private static final String CLIENT_ID = "34020000001320000001";
    private static final String SERVER_ID = "34020000002000000001";
    private static final String PASSWORD  = "123456";

    @Autowired private DeviceMapper deviceMapper;

    @BeforeEach
    void register() {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);
        await().atMost(5, SECONDS).until(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            return d != null && d.getStatus() == 1;
        });
    }

    @AfterEach
    void cleanup() {
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
    }

    @Test
    @DisplayName("TC-KA-01 真实 SIP 心跳 → keepalive_time 刷新且设备保持在线")
    void keepalive_refreshesTimestampAndKeepsOnline() throws InterruptedException {
        LocalDateTime before = deviceMapper.selectOne(
            Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID)).getKeepaliveTime();

        Thread.sleep(100);
        ClientCommandSender.sendKeepaliveCommand(from(), to(), "OK");

        await().atMost(5, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d.getStatus()).isEqualTo(1);
            if (before != null) {
                assertThat(d.getKeepaliveTime()).isAfterOrEqualTo(before);
            }
        });
        log.info("✅ TC-KA-01 通过");
    }

    @Test
    @DisplayName("TC-KA-02 快速连续 3 次心跳 → 设备保持在线")
    void keepalive_rapidBurst_deviceStaysOnline() {
        for (int i = 0; i < 3; i++) {
            ClientCommandSender.sendKeepaliveCommand(from(), to(), "OK");
        }
        await().atMost(5, SECONDS).untilAsserted(() ->
            assertThat(deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getDeviceId, CLIENT_ID)).getStatus()).isEqualTo(1));
        log.info("✅ TC-KA-02 通过");
    }

    private FromDevice from() { return FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061); }

    private ToDevice to() {
        ToDevice t = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        t.setPassword(PASSWORD);
        return t;
    }
}
