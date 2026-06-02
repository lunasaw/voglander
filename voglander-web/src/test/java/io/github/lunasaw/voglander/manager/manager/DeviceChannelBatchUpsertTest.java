package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 4：设备通道批量幂等 upsert 集成测试（修 P4 目录 N+1 + §8 幂等）。
 * <p>
 * 校验 {@link DeviceChannelManager#batchUpsert(List)}：
 * </p>
 * <ul>
 * <li>首次批量：全部新增；</li>
 * <li>重复投递（同 device+channel）：命中 UNIQUE，转为更新，不新增重复行、不抛 UNIQUE 异常（幂等）；</li>
 * <li>混合批：部分新增 + 部分更新一次完成。</li>
 * </ul>
 *
 * @author luna
 */
@Slf4j
public class DeviceChannelBatchUpsertTest extends BaseTest {

    private static final String TEST_DEVICE = "BATCH_UPSERT_DEVICE";

    @Autowired
    private DeviceChannelManager deviceChannelManager;

    @Autowired
    private DeviceChannelService deviceChannelService;

    @BeforeEach
    public void setUp() {
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        deviceChannelService.lambdaUpdate().eq(DeviceChannelDO::getDeviceId, TEST_DEVICE).remove();
    }

    private DeviceChannelDTO channel(String channelId, String name) {
        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setDeviceId(TEST_DEVICE);
        dto.setChannelId(channelId);
        dto.setName(name);
        dto.setStatus(1);
        return dto;
    }

    private long countChannels() {
        return deviceChannelService.lambdaQuery().eq(DeviceChannelDO::getDeviceId, TEST_DEVICE).count();
    }

    @Test
    public void testFirstBatchAllInserted() {
        List<DeviceChannelDTO> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            list.add(channel("ch" + i, "name" + i));
        }
        int affected = deviceChannelManager.batchUpsert(list);
        assertEquals(3, affected, "首次批量应处理 3 条");
        assertEquals(3, countChannels(), "应有 3 条通道");
        log.info("首次批量全部新增校验通过");
    }

    @Test
    public void testRepeatBatchIsIdempotent() {
        List<DeviceChannelDTO> list = new ArrayList<>();
        list.add(channel("ch1", "name1"));
        list.add(channel("ch2", "name2"));
        deviceChannelManager.batchUpsert(list);
        assertEquals(2, countChannels());

        // 重复投递同样的 device+channel（重传目录）：命中 UNIQUE → 转更新，不新增重复
        List<DeviceChannelDTO> repeat = new ArrayList<>();
        repeat.add(channel("ch1", "name1-updated"));
        repeat.add(channel("ch2", "name2-updated"));
        deviceChannelManager.batchUpsert(repeat);

        assertEquals(2, countChannels(), "幂等：重复投递不得新增重复行");
        DeviceChannelDO ch1 = deviceChannelService.lambdaQuery()
            .eq(DeviceChannelDO::getDeviceId, TEST_DEVICE)
            .eq(DeviceChannelDO::getChannelId, "ch1").one();
        assertNotNull(ch1);
        assertEquals("name1-updated", ch1.getName(), "重复投递应更新为新值");
        log.info("重复投递幂等校验通过");
    }

    @Test
    public void testMixedInsertAndUpdate() {
        deviceChannelManager.batchUpsert(List.of(channel("ch1", "orig1")));
        assertEquals(1, countChannels());

        // 混合：ch1 已存在（更新）、ch2/ch3 新增
        List<DeviceChannelDTO> mixed = new ArrayList<>();
        mixed.add(channel("ch1", "orig1-updated"));
        mixed.add(channel("ch2", "new2"));
        mixed.add(channel("ch3", "new3"));
        deviceChannelManager.batchUpsert(mixed);

        assertEquals(3, countChannels(), "混合批应得到 3 条");
        log.info("混合新增+更新校验通过");
    }
}
