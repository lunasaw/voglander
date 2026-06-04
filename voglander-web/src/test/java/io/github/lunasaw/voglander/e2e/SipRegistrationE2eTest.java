package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-SIP-01/02/03：真实 SIP 协议栈端到端注册测试。
 * <p>
 * 链路：{@code ClientCommandSender.sendRegisterCommand} → SIP/UDP → Server(5060)
 * → {@code VoglanderBusinessNotifier}(@Async) → {@code ShardDispatcher}
 * → {@code Gb28181ProtocolHandler} → {@code DeviceRegisterService.login} → SQLite
 * </p>
 * 不加 {@code @Transactional}：异步链路在独立线程写 DB，由 {@code @AfterEach} 手动清理。
 */
@Slf4j
class SipRegistrationE2eTest extends BaseE2eTest {

    // 必须与 VoglanderSipClientProperties 默认绑定一致：
    // 上下文复用时 @TestPropertySource 无法重新初始化已绑定的 SIP 协议栈
    private static final String CLIENT_ID  = "34020000001320000001";
    private static final String SERVER_ID  = "34020000002000000001";
    private static final String PASSWORD   = "123456";

    @Autowired private DeviceMapper deviceMapper;

    @AfterEach
    void cleanup() {
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
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
    @DisplayName("TC-SIP-01 真实 SIP REGISTER → 401 → Digest → 200 OK → DB 在线")
    void fullSipRegister_shouldPersistOnlineDevice() {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d).as("设备记录应已写入 tb_device").isNotNull();
            assertThat(d.getStatus()).as("状态应为在线(1)").isEqualTo(1);
            assertThat(d.getIp()).as("IP 应为 127.0.0.1").isEqualTo("127.0.0.1");
        });
        log.info("✅ TC-SIP-01 通过");
    }

    @Test
    @DisplayName("TC-SIP-02 真实 SIP REGISTER(expires=0) → 设备状态下线(0)")
    void fullSipUnregister_shouldSetDeviceOffline() {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);
        await().atMost(5, SECONDS).until(() ->
            deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getDeviceId, CLIENT_ID)) != null);

        ClientCommandSender.sendUnregisterCommand(from(), to());

        await().atMost(5, SECONDS).untilAsserted(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(d).isNotNull();
            assertThat(d.getStatus()).as("注销后状态应为离线(0)").isEqualTo(0);
        });
        log.info("✅ TC-SIP-02 通过");
    }

    @Test
    @DisplayName("TC-SIP-03 重复 REGISTER 幂等：不产生重复记录")
    void duplicateRegister_shouldNotCreateDuplicateRecord() throws InterruptedException {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);
        await().atMost(5, SECONDS).until(() ->
            deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getDeviceId, CLIENT_ID)) != null);

        Long firstId = deviceMapper.selectOne(
            Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID)).getId();

        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);
        Thread.sleep(2000);

        List<DeviceDO> records = deviceMapper.selectList(
            Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
        assertThat(records).as("重复注册不应产生多条记录").hasSize(1);
        assertThat(records.get(0).getId()).as("upsert 应复用同一行").isEqualTo(firstId);
        log.info("✅ TC-SIP-03 通过");
    }
}
