package io.github.lunasaw.voglander.manager.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceChannelMapper;
import io.github.lunasaw.voglander.support.RedisAvailableExtension;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * TC-03：目录同步集成测试（降级为 Manager 层集成测试，避免 SIP NOTIFY 模拟成本）。
 * <p>
 * 覆盖范围：batchUpsertWithStatus 写入 status/lastSeenTime/statusSource + cascadeOffline 级联置零。
 * </p>
 */
@DisplayName("TC-03 目录同步 + 级联下线 集成测试")
@ExtendWith(RedisAvailableExtension.class)
class CatalogSyncIntegrationTest extends BaseTest {

    @Autowired
    private DeviceChannelManager deviceChannelManager;
    @Autowired
    private DeviceManager        deviceManager;
    @Autowired
    private DeviceChannelMapper  channelMapper;

    private String lastDeviceId;

    @AfterEach
    void cleanup() {
        if (lastDeviceId != null) {
            channelMapper.delete(Wrappers.<DeviceChannelDO>lambdaQuery()
                .eq(DeviceChannelDO::getDeviceId, lastDeviceId));
        }
    }

    private String prepareDevice() {
        String deviceId = UniqueKeyFactory.deviceId();
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(deviceId);
        dto.setIp("127.0.0.1");
        dto.setPort(5060);
        dto.setType(1);
        dto.setServerIp("127.0.0.1");
        deviceManager.add(dto);
        lastDeviceId = deviceId;
        return deviceId;
    }

    private DeviceChannelDTO buildChannel(String deviceId, String channelId, int status) {
        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setDeviceId(deviceId);
        dto.setChannelId(channelId);
        dto.setName("cam-" + channelId);
        dto.setStatus(status);
        dto.setLastSeenTime(LocalDateTime.now());
        dto.setStatusSource("CATALOG");
        return dto;
    }

    @Test
    @DisplayName("batchUpsertWithStatus 写入两条通道，status/statusSource/lastSeenTime 正确落库")
    void batchUpsertWithStatus_persists_channels_with_status() {
        String deviceId = prepareDevice();
        String ch1 = UniqueKeyFactory.channelId();
        String ch2 = UniqueKeyFactory.channelId();

        deviceChannelManager.batchUpsertWithStatus(deviceId,
            List.of(buildChannel(deviceId, ch1, 1), buildChannel(deviceId, ch2, 0)));

        List<DeviceChannelDO> saved = channelMapper.selectList(
            Wrappers.<DeviceChannelDO>lambdaQuery().eq(DeviceChannelDO::getDeviceId, deviceId));
        assertThat(saved).hasSize(2);

        DeviceChannelDO online = saved.stream().filter(c -> c.getChannelId().equals(ch1)).findFirst().orElseThrow();
        assertThat(online.getStatus()).isEqualTo(1);
        assertThat(online.getStatusSource()).isEqualTo("CATALOG");
        assertThat(online.getLastSeenTime()).isNotNull();
    }

    @Test
    @DisplayName("cascadeOffline 把该设备所有在线通道置为离线")
    void cascadeOffline_sets_all_channels_offline() {
        String deviceId = prepareDevice();
        String ch1 = UniqueKeyFactory.channelId();
        String ch2 = UniqueKeyFactory.channelId();

        deviceChannelManager.batchUpsertWithStatus(deviceId,
            List.of(buildChannel(deviceId, ch1, 1), buildChannel(deviceId, ch2, 1)));

        deviceChannelManager.cascadeOffline(deviceId);

        List<DeviceChannelDO> after = channelMapper.selectList(
            Wrappers.<DeviceChannelDO>lambdaQuery().eq(DeviceChannelDO::getDeviceId, deviceId));
        assertThat(after).allMatch(c -> c.getStatus() == 0);
    }

    @Test
    @DisplayName("batchUpsertWithStatus 幂等：二次调用同 channelId 只保留一条记录")
    void batchUpsertWithStatus_is_idempotent() {
        String deviceId = prepareDevice();
        String channelId = UniqueKeyFactory.channelId();

        deviceChannelManager.batchUpsertWithStatus(deviceId, List.of(buildChannel(deviceId, channelId, 1)));
        deviceChannelManager.batchUpsertWithStatus(deviceId, List.of(buildChannel(deviceId, channelId, 1)));

        long count = channelMapper.selectCount(
            Wrappers.<DeviceChannelDO>lambdaQuery()
                .eq(DeviceChannelDO::getDeviceId, deviceId)
                .eq(DeviceChannelDO::getChannelId, channelId));
        assertThat(count).isEqualTo(1);
    }
}
