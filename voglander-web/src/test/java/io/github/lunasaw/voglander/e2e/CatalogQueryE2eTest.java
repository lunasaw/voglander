package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
 * TC-CAT-01/03：设备目录查询端到端测试（真实 SIP 协议栈）。
 * 链路：ClientCommandSender.sendCatalogCommand → SIP/UDP MESSAGE → Server(5060)
 *       → VoglanderBusinessNotifier → handleCatalog → tb_device_channel 批量 upsert
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class CatalogQueryE2eTest {

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
    @DisplayName("TC-CAT-01 真实 SIP Catalog 响应 → 批量 upsert 10 条通道")
    void catalogResponse_persistsChannels() {
        ClientCommandSender.sendCatalogCommand(from(), to(), catalogResponse(buildItems(10)));

        await().atMost(8, SECONDS).untilAsserted(() ->
            assertThat(channelMapper.selectCount(Wrappers.<DeviceChannelDO>lambdaQuery()
                .eq(DeviceChannelDO::getDeviceId, CLIENT_ID)
                .likeRight(DeviceChannelDO::getChannelId, CLIENT_ID.substring(0, 16)))).isEqualTo(10L));
        log.info("✅ TC-CAT-01 通过");
    }

    @Test
    @DisplayName("TC-CAT-03 真实 SIP Catalog 重复上报 → 幂等写保护（5 条不重复）")
    void catalogResponse_idempotentUpsert() {
        DeviceResponse resp = catalogResponse(buildItems(5));
        ClientCommandSender.sendCatalogCommand(from(), to(), resp);
        ClientCommandSender.sendCatalogCommand(from(), to(), resp);
        ClientCommandSender.sendCatalogCommand(from(), to(), resp);

        await().atMost(8, SECONDS).untilAsserted(() ->
            assertThat(channelMapper.selectCount(Wrappers.<DeviceChannelDO>lambdaQuery()
                .eq(DeviceChannelDO::getDeviceId, CLIENT_ID)
                .likeRight(DeviceChannelDO::getChannelId, CLIENT_ID.substring(0, 16)))).isEqualTo(5L));
        log.info("✅ TC-CAT-03 通过");
    }

    private List<DeviceItem> buildItems(int n) {
        List<DeviceItem> items = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            DeviceItem item = new DeviceItem();
            item.setDeviceId(CLIENT_ID.substring(0, 16) + String.format("%04d", i));
            item.setName("CH-" + i);
            item.setStatus("ON");
            items.add(item);
        }
        return items;
    }

    private DeviceResponse catalogResponse(List<DeviceItem> items) {
        DeviceResponse r = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), "1", CLIENT_ID);
        r.setSumNum(items.size());
        r.setDeviceList(items);
        return r;
    }

    private FromDevice from() { return FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061); }

    private ToDevice to() {
        ToDevice t = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        t.setPassword(PASSWORD);
        return t;
    }
}
