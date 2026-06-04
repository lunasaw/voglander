package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Supplier E2E — UDP + TCP 传输协议完整链路测试。
 * 共用同一 Spring context（DEFINED_PORT + test profile）。
 *
 * <p>重要约束：{@code ClientCommandSender} 在 401 Digest 重发阶段会用协议栈绑定的固定
 * clientId（{@code 34020000001320000001}）覆盖 fromDevice.userId，因此 TCP 测试也必须
 * 用同一个 CLIENT_ID，通过 {@code FromDevice.transport=TCP} 控制走 TCP 协议路径。
 * 服务端从 Via header 读取 transport，DB extend.transport 反映真实协商结果。</p>
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class SupplierSipTransportE2eTest {

    private static final String CLIENT_ID = "34020000001320000001";
    private static final String SERVER_ID = "34020000002000000001";
    private static final String PASSWORD  = "123456";

    @Autowired private DeviceMapper                  deviceMapper;
    @Autowired private VoglanderServerDeviceSupplier serverSupplier;

    @AfterEach
    void cleanup() {
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
    }

    private FromDevice udpFrom() {
        FromDevice f = FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061);
        f.setTransport("UDP");
        return f;
    }

    private ToDevice udpTo() {
        ToDevice to = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        to.setTransport("UDP");
        to.setPassword(PASSWORD);
        return to;
    }

    /** TCP：同一 clientId，transport=TCP，服务端 Via header 记录为 TCP。*/
    private FromDevice tcpFrom() {
        FromDevice f = FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061);
        f.setTransport("TCP");
        f.setStreamMode("TCP-PASSIVE");
        return f;
    }

    private ToDevice tcpTo() {
        ToDevice to = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        to.setTransport("TCP");
        to.setPassword(PASSWORD);
        return to;
    }

    // ── SipLayer 状态验证 ─────────────────────────────────────────────────

    @Test
    @DisplayName("SipLayer 同时有 UDP 和 TCP provider")
    void sip_layer_has_both_udp_and_tcp_providers() {
        assertThat(SipLayer.getUdpSipProviderMap()).as("UDP provider 应已创建").isNotEmpty();
        assertThat(SipLayer.getTcpSipProviderMap()).as("TCP provider 应已创建").isNotEmpty();
        log.info("✅ UDP providers: {}, TCP providers: {}",
            SipLayer.getUdpSipProviderMap().keySet(), SipLayer.getTcpSipProviderMap().keySet());
    }

    // ── UDP 完整链路 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("UDP REGISTER → 401 → Digest → 200 OK → DB 在线")
    void udp_register_persists_online() {
        ClientCommandSender.sendRegisterCommand(udpFrom(), udpTo(), 3600);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d).as("设备应写入 tb_device").isNotNull();
            assertThat(d.getStatus()).as("状态应为在线(1)").isEqualTo(1);
        });
        log.info("✅ UDP REGISTER 通过");
    }

    @Test
    @DisplayName("UDP 注销 → DB 状态变离线")
    void udp_unregister_sets_offline() {
        ClientCommandSender.sendRegisterCommand(udpFrom(), udpTo(), 3600);
        await().atMost(5, SECONDS).until(() ->
            deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getDeviceId, CLIENT_ID)) != null);

        ClientCommandSender.sendUnregisterCommand(udpFrom(), udpTo());

        await().atMost(5, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d.getStatus()).as("注销后应为离线(0)").isEqualTo(0);
        });
        log.info("✅ UDP 注销通过");
    }

    @Test
    @DisplayName("UDP REGISTER 后 getToDevice: callId/toTag 非空，streamMode 合法")
    void udp_getToDevice_correct() {
        ClientCommandSender.sendRegisterCommand(udpFrom(), udpTo(), 3600);
        await().atMost(5, SECONDS).until(() ->
            deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getDeviceId, CLIENT_ID)) != null);

        ToDevice to = serverSupplier.getToDevice(CLIENT_ID);

        assertThat(to.getCallId()).isNotBlank();
        assertThat(to.getToTag()).isNotBlank();
        assertThat(to.getExpires()).isEqualTo(3600);
        // transport 取自 Via header 协商值，不做硬断言
        assertThat(to.getTransport()).isIn("UDP", "TCP");
        if (to.getStreamMode() != null) {
            assertThat(StreamModeEnum.isValid(to.getStreamMode())).isTrue();
        }
        log.info("✅ UDP getToDevice: transport={}, callId={}", to.getTransport(), to.getCallId());
    }

    // ── TCP 完整链路 ──────────────────────────────────────────────────��───

    @Test
    @DisplayName("TCP REGISTER → 401 → Digest → 200 OK → DB transport=TCP")
    void tcp_register_persists_online_with_tcp_transport() {
        ClientCommandSender.sendRegisterCommand(tcpFrom(), tcpTo(), 3600);

        await().atMost(8, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d).as("TCP 注册：设备应写入 tb_device").isNotNull();
            assertThat(d.getStatus()).as("状态应为在线(1)").isEqualTo(1);
            assertThat(d.getExtend()).as("DB extend 应含 transport:TCP").contains("\"transport\":\"TCP\"");
        });
        log.info("✅ TCP REGISTER 通过");
    }

    @Test
    @DisplayName("TCP 注销 → DB 状态变离线")
    void tcp_unregister_sets_offline() {
        ClientCommandSender.sendRegisterCommand(tcpFrom(), tcpTo(), 3600);
        await().atMost(8, SECONDS).until(() ->
            deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getDeviceId, CLIENT_ID)) != null);

        ClientCommandSender.sendUnregisterCommand(tcpFrom(), tcpTo());

        await().atMost(8, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d.getStatus()).as("注销后应为离线(0)").isEqualTo(0);
        });
        log.info("✅ TCP 注销通过");
    }

    @Test
    @DisplayName("TCP REGISTER 后 getToDevice: transport=TCP, callId/toTag 非空")
    void tcp_getToDevice_correct() {
        ClientCommandSender.sendRegisterCommand(tcpFrom(), tcpTo(), 3600);
        await().atMost(8, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d).isNotNull();
            assertThat(d.getExtend()).contains("\"transport\":\"TCP\"");
        });

        ToDevice to = serverSupplier.getToDevice(CLIENT_ID);

        assertThat(to.getTransport()).isEqualTo("TCP");
        assertThat(to.getCallId()).isNotBlank();
        assertThat(to.getToTag()).isNotBlank();
        assertThat(to.getExpires()).isEqualTo(3600);
        if (to.getStreamMode() != null) {
            assertThat(StreamModeEnum.isValid(to.getStreamMode())).isTrue();
        }
        log.info("✅ TCP getToDevice: transport={}, callId={}", to.getTransport(), to.getCallId());
    }
}
