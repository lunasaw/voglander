package io.github.lunasaw.voglander.e2e;

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
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.config.MockDeviceAdapter;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceChannelMapper;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-01/02/04：设备注册完整端到端测试（真实 SIP 协议栈）。
 * <p>
 * 链路：ClientCommandSender → SIP/UDP → Server(5060) → VoglanderBusinessNotifier(@Async)
 * → ShardDispatcher → Gb28181ProtocolHandler → login/queryDeviceInfo/queryCatalog → SQLite
 * </p>
 * 覆盖：注册入库 → DeviceInfo 回查更新 → Catalog 通道写入。
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class DeviceRegistrationE2eTest {

    private static final String CLIENT_ID = "34020000001320000001";
    private static final String SERVER_ID = "34020000002000000001";
    private static final String PASSWORD  = "123456";

    @Autowired private DeviceMapper        deviceMapper;
    @Autowired private DeviceChannelMapper channelMapper;

    @AfterEach
    void cleanup() {
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
        channelMapper.delete(Wrappers.<DeviceChannelDO>lambdaQuery().eq(DeviceChannelDO::getDeviceId, CLIENT_ID));
    }

    private FromDevice from() {
        return FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061);
    }

    private ToDevice to() {
        ToDevice to = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        to.setPassword(PASSWORD);
        return to;
    }

    @Test
    @DisplayName("TC-01 真实 SIP REGISTER → DB 在线 → DeviceInfo 回查更新扩展信息")
    void register_shouldPersistOnlineAndUpdateDeviceInfo() {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);

        // 1. 注册入库
        await().atMost(5, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d).isNotNull();
            assertThat(d.getStatus()).isEqualTo(1);
        });

        // 2. DeviceInfo 查询应回写扩展信息（MockDeviceAdapter 回应 deviceName=MockCamera）
        await().atMost(8, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d.getExtend()).as("extend 字段应含 DeviceInfo 信息")
                .contains(MockDeviceAdapter.MOCK_DEVICE_NAME);
        });

        log.info("✅ TC-01 通过");
    }

    @Test
    @DisplayName("TC-02 真实 SIP REGISTER → Catalog 查询 → 通道写入 tb_device_channel")
    void register_shouldTriggerCatalogAndPersistChannel() {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);

        // 等待注册完成
        await().atMost(5, SECONDS).until(() ->
            deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getDeviceId, CLIENT_ID)) != null);

        // Catalog 响应后通道写入（MockDeviceAdapter 回应 channelId=34020000001310000001）
        await().atMost(8, SECONDS).untilAsserted(() -> {
            DeviceChannelDO ch = channelMapper.selectOne(
                Wrappers.<DeviceChannelDO>lambdaQuery()
                    .eq(DeviceChannelDO::getDeviceId, CLIENT_ID)
                    .eq(DeviceChannelDO::getChannelId, MockDeviceAdapter.MOCK_CHANNEL_ID));
            assertThat(ch).as("通道记录应已写入 tb_device_channel").isNotNull();
            assertThat(ch.getName()).isEqualTo(MockDeviceAdapter.MOCK_CHANNEL_NAME);
        });

        log.info("✅ TC-02 通过");
    }

    @Test
    @DisplayName("TC-04 重复 REGISTER 幂等：不产生重复设备记录")
    void duplicateRegister_shouldBeIdempotent() throws InterruptedException {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);
        await().atMost(5, SECONDS).until(() ->
            deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getDeviceId, CLIENT_ID)) != null);

        Long firstId = deviceMapper.selectOne(
            Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID)).getId();

        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);
        Thread.sleep(2000);

        assertThat(deviceMapper.selectList(
            Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID)))
            .hasSize(1)
            .allMatch(d -> d.getId().equals(firstId));

        log.info("✅ TC-04 通过");
    }
}
