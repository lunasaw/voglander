package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceChannelMapper;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-SUB-CAT-02/03：目录通知端到端测试（真实 SIP 协议栈）。
 * 链路：ClientCommandSender.sendCatalogCommand → SIP/UDP MESSAGE → Server(5060)
 *       → handleCatalog → tb_device_channel upsert
 */
@Slf4j
class CatalogSubscribeE2eTest extends BaseE2eTest {

    private static final String CLIENT_ID = "34020000001320000001";
    private static final String SERVER_ID = "34020000002000000001";
    private static final String PASSWORD  = "123456";

    @Autowired private DeviceChannelMapper channelMapper;
    @Autowired private DeviceMapper        deviceMapper;

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
        channelMapper.delete(Wrappers.<DeviceChannelDO>lambdaQuery().eq(DeviceChannelDO::getDeviceId, CLIENT_ID));
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
    }

    @Test
    @DisplayName("TC-SUB-CAT-02 设备上报 Catalog NOTIFY → 通道入库")
    void catalogNotify_persistsChannel() {
        String chId = CLIENT_ID.substring(0, 16) + "0001";
        ClientCommandSender.sendCatalogCommand(from(), to(), catalog(List.of(item(chId, "CH-01"))));

        await().atMost(15, SECONDS).untilAsserted(() ->
            assertThat(channelMapper.selectOne(Wrappers.<DeviceChannelDO>lambdaQuery()
                .eq(DeviceChannelDO::getDeviceId, CLIENT_ID)
                .eq(DeviceChannelDO::getChannelId, chId))).isNotNull());
        log.info("✅ TC-SUB-CAT-02 通过");
    }

    @Test
    @DisplayName("TC-SUB-CAT-03 部分通道上报 → 已有通道不被删除（失踪扫描默认关闭）")
    void catalogNotify_missingChannel_notDeleted() {
        String ch01 = CLIENT_ID.substring(0, 16) + "0001";
        String ch02 = CLIENT_ID.substring(0, 16) + "0002";

        ClientCommandSender.sendCatalogCommand(from(), to(),
            catalog(List.of(item(ch01, "CH-01"), item(ch02, "CH-02"))));
        await().atMost(15, SECONDS).until(() ->
            channelMapper.selectOne(Wrappers.<DeviceChannelDO>lambdaQuery()
                .eq(DeviceChannelDO::getDeviceId, CLIENT_ID)
                .eq(DeviceChannelDO::getChannelId, ch02)) != null);

        ClientCommandSender.sendCatalogCommand(from(), to(), catalog(List.of(item(ch01, "CH-01"))));

        await().atMost(5, SECONDS).untilAsserted(() -> {
            // ch02 仍存在（失踪扫描关闭）
            assertThat(channelMapper.selectOne(Wrappers.<DeviceChannelDO>lambdaQuery()
                .eq(DeviceChannelDO::getDeviceId, CLIENT_ID)
                .eq(DeviceChannelDO::getChannelId, ch02))).isNotNull();
            // ch01 仍在线
            assertThat(channelMapper.selectOne(Wrappers.<DeviceChannelDO>lambdaQuery()
                .eq(DeviceChannelDO::getChannelId, ch01)
                .eq(DeviceChannelDO::getDeviceId, CLIENT_ID)).getStatus()).isEqualTo(1);
        });
        log.info("✅ TC-SUB-CAT-03 通过");
    }

    private DeviceItem item(String id, String name) {
        DeviceItem i = new DeviceItem();
        i.setDeviceId(id);
        i.setName(name);
        i.setStatus("ON");
        return i;
    }

    private DeviceResponse catalog(List<DeviceItem> items) {
        DeviceResponse r = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), "1", CLIENT_ID);
        r.setSumNum(items.size());
        r.setDeviceList(new java.util.ArrayList<>(items));
        return r;
    }

    private FromDevice from() { return FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061); }

    private ToDevice to() {
        ToDevice t = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        t.setPassword(PASSWORD);
        return t;
    }
}
