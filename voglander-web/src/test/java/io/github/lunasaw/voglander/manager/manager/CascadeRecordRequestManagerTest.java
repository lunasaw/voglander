package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeRecordRequestDTO;

/**
 * CascadeRecordRequestManager 集成测试
 *
 * @author luna
 */
@DisplayName("CascadeRecordRequestManager 集成测试")
@Transactional
class CascadeRecordRequestManagerTest extends BaseTest {

    @Autowired
    private CascadeRecordRequestManager manager;

    private CascadeChannelDTO channel(String localDeviceId) {
        CascadeChannelDTO ch = new CascadeChannelDTO();
        ch.setPlatformId("PF" + System.nanoTime());
        ch.setLocalDeviceId(localDeviceId);
        ch.setLocalChannelId(localDeviceId + "01");
        ch.setCascadeChannelId(localDeviceId + "01");
        return ch;
    }

    @Nested
    @DisplayName("create + findPending")
    class CreateFind {

        @Test
        @DisplayName("登记请求应为 PENDING 并能按设备+时间窗找回")
        void should_create_and_find_by_device_and_window() {
            String dev = "34020000001320" + (System.nanoTime() % 1000000);
            CascadeChannelDTO ch = channel(dev);
            Long id = manager.create(ch.getPlatformId(), "S1", ch, "2026-06-14T00:00:00", "2026-06-14T23:59:59");
            assertNotNull(id);

            CascadeRecordRequestDTO found = manager.findPending(dev, "2026-06-14T00:00:00", "2026-06-14T23:59:59");
            assertNotNull(found);
            assertEquals("S1", found.getSuperiorSn());
            assertEquals(CascadeConstant.RecordReqStatus.PENDING, found.getStatus());
        }

        @Test
        @DisplayName("时间窗不匹配应放宽为按设备取最早 PENDING")
        void should_relax_to_device_only_when_window_mismatch() {
            String dev = "34020000001320" + (System.nanoTime() % 1000000);
            CascadeChannelDTO ch = channel(dev);
            manager.create(ch.getPlatformId(), "S1", ch, "2026-06-14T00:00:00", "2026-06-14T23:59:59");

            CascadeRecordRequestDTO found = manager.findPending(dev, "9999-01-01T00:00:00", "9999-01-01T00:00:00");
            assertNotNull(found, "时间窗不匹配应放宽");
            assertEquals("S1", found.getSuperiorSn());
        }
    }

    @Nested
    @DisplayName("状态流转")
    class Status {

        @Test
        @DisplayName("markResponded 应置 RESPONDED 且不再被 findPending 命中")
        void should_mark_responded() {
            String dev = "34020000001320" + (System.nanoTime() % 1000000);
            CascadeChannelDTO ch = channel(dev);
            Long id = manager.create(ch.getPlatformId(), "S1", ch, "a", "b");
            assertTrue(manager.markResponded(id));

            assertEquals(CascadeConstant.RecordReqStatus.RESPONDED, manager.getById(id).getStatus());
            assertNull(manager.findPending(dev, "a", "b"), "已响应不应再被命中");
        }
    }

    @Nested
    @DisplayName("cleanTimeout")
    class CleanTimeout {

        @Test
        @DisplayName("应将早于 cutoff 的 PENDING 标为 TIMEOUT")
        void should_timeout_stale_pending() {
            String dev = "34020000001320" + (System.nanoTime() % 1000000);
            CascadeChannelDTO ch = channel(dev);
            Long id = manager.create(ch.getPlatformId(), "S1", ch, "a", "b");

            // cutoff 设为未来，确保覆盖刚创建的记录
            int cleaned = manager.cleanTimeout(LocalDateTime.now().plusSeconds(10));
            assertTrue(cleaned >= 1);
            assertEquals(CascadeConstant.RecordReqStatus.TIMEOUT, manager.getById(id).getStatus());
        }
    }
}
