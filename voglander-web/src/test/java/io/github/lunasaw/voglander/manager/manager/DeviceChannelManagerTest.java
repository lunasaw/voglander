package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceChannelMapper;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * DeviceChannelManager 集成测试
 *
 * @author luna
 */
@DisplayName("DeviceChannelManager 集成测试")
class DeviceChannelManagerTest extends BaseTest {

    @Autowired
    private DeviceChannelManager deviceChannelManager;

    @Autowired
    private DeviceManager        deviceManager;

    @Autowired
    private DeviceChannelMapper  deviceChannelMapper;

    private String lastDeviceId;

    @AfterEach
    void cleanup() {
        if (lastDeviceId != null) {
            deviceChannelMapper.delete(
                new LambdaQueryWrapper<DeviceChannelDO>().eq(DeviceChannelDO::getDeviceId, lastDeviceId));
        }
    }

    private String createTestDevice() {
        String deviceId = UniqueKeyFactory.deviceId();
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(deviceId);
        dto.setIp("127.0.0.1");
        dto.setPort(5060);
        dto.setType(1);
        dto.setServerIp("127.0.0.1");
        dto.setName("dev-" + deviceId);
        deviceManager.add(dto);
        return deviceId;
    }

    private DeviceChannelDTO buildDto(String deviceId, String channelId) {
        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setDeviceId(deviceId);
        dto.setChannelId(channelId);
        dto.setName("ch-" + channelId);
        return dto;
    }

    @Nested
    @DisplayName("batchUpsert UNIQUE 冲突兜底")
    class BatchUpsert {

        @Test
        @DisplayName("batchUpsert 重复 channelId+deviceId 应幂等，不抛异常")
        void should_upsert_on_duplicate() {
            String deviceId = createTestDevice();
            lastDeviceId = deviceId;

            String ch1 = UniqueKeyFactory.channelId();
            String ch2 = UniqueKeyFactory.channelId();

            List<DeviceChannelDTO> batch = List.of(
                buildDto(deviceId, ch1),
                buildDto(deviceId, ch2),
                buildDto(deviceId, ch1) // 重复
            );

            assertDoesNotThrow(() -> deviceChannelManager.batchUpsert(batch));

            long count = deviceChannelMapper.selectCount(
                new LambdaQueryWrapper<DeviceChannelDO>().eq(DeviceChannelDO::getDeviceId, deviceId));
            assertEquals(2, count, "重复项应幂等处理，DB 应只有 2 条");
        }

        @Test
        @DisplayName("batchUpsert 二次调用应更新已存在记录")
        void should_update_existing_on_second_call() {
            String deviceId = createTestDevice();
            lastDeviceId = deviceId;
            String channelId = UniqueKeyFactory.channelId();

            deviceChannelManager.batchUpsert(List.of(buildDto(deviceId, channelId)));
            deviceChannelManager.batchUpsert(List.of(buildDto(deviceId, channelId)));

            long count = deviceChannelMapper.selectCount(
                new LambdaQueryWrapper<DeviceChannelDO>()
                    .eq(DeviceChannelDO::getDeviceId, deviceId)
                    .eq(DeviceChannelDO::getChannelId, channelId));
            assertEquals(1, count, "DB 应只有一条记录");
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("updateStatus 应只更新 status 字段")
        void should_update_only_status() {
            String deviceId = createTestDevice();
            lastDeviceId = deviceId;
            String channelId = UniqueKeyFactory.channelId();
            deviceChannelManager.batchUpsert(List.of(buildDto(deviceId, channelId)));

            deviceChannelManager.updateStatus(deviceId, channelId, 1);

            DeviceChannelDO result = deviceChannelMapper.selectOne(
                new LambdaQueryWrapper<DeviceChannelDO>()
                    .eq(DeviceChannelDO::getDeviceId, deviceId)
                    .eq(DeviceChannelDO::getChannelId, channelId));
            assertNotNull(result);
            assertEquals(1, result.getStatus());
        }
    }
}
