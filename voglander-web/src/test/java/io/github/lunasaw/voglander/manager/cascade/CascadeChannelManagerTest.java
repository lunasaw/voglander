package io.github.lunasaw.voglander.manager.cascade;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * CascadeChannelManager Stage 1 集成测试（TDD 先红后绿）
 */
@DisplayName("CascadeChannelManager 集成测试")
class CascadeChannelManagerTest extends BaseTest {

    @Autowired
    private CascadeChannelManager cascadeChannelManager;

    private CascadeChannelDTO buildDto(String platformId, String localChannelId) {
        CascadeChannelDTO dto = new CascadeChannelDTO();
        dto.setPlatformId(platformId);
        dto.setLocalDeviceId(UniqueKeyFactory.deviceId());
        dto.setLocalChannelId(localChannelId);
        dto.setCascadeChannelId(localChannelId); // 默认同 localChannelId
        return dto;
    }

    @Nested
    @DisplayName("add - 新增级联通道")
    class Add {

        @Test
        @DisplayName("最小合法 DTO 应返回 ID")
        void should_return_id() {
            Long id = cascadeChannelManager.add(buildDto(UniqueKeyFactory.deviceId(), UniqueKeyFactory.channelId()));
            assertNotNull(id);
            assertTrue(id > 0);
        }

        @Test
        @DisplayName("新增后 enabled=1 默认上报")
        void should_default_enabled() {
            String platformId = UniqueKeyFactory.deviceId();
            String channelId = UniqueKeyFactory.channelId();
            cascadeChannelManager.add(buildDto(platformId, channelId));

            CascadeChannelDTO saved = cascadeChannelManager.getByPlatformAndChannel(platformId, channelId);
            assertNotNull(saved);
            assertEquals(1, saved.getEnabled());
        }

        @Test
        @DisplayName("(platformId, localChannelId) 重复应抛出异常")
        void should_throw_on_duplicate() {
            String platformId = UniqueKeyFactory.deviceId();
            String channelId = UniqueKeyFactory.channelId();
            cascadeChannelManager.add(buildDto(platformId, channelId));
            assertThrows(Exception.class, () -> cascadeChannelManager.add(buildDto(platformId, channelId)));
        }
    }

    @Nested
    @DisplayName("listByPlatformId - 查询平台下所有通道")
    class ListByPlatform {

        @Test
        @DisplayName("应返回同一 platformId 下所有 enabled 通道")
        void should_list_by_platform() {
            String platformId = UniqueKeyFactory.deviceId();
            cascadeChannelManager.add(buildDto(platformId, UniqueKeyFactory.channelId()));
            cascadeChannelManager.add(buildDto(platformId, UniqueKeyFactory.channelId()));
            // 不同 platform 的不应被查到
            cascadeChannelManager.add(buildDto(UniqueKeyFactory.deviceId(), UniqueKeyFactory.channelId()));

            java.util.List<CascadeChannelDTO> list = cascadeChannelManager.listByPlatformId(platformId);
            assertEquals(2, list.size());
        }
    }

    @Nested
    @DisplayName("deleteOne - 删除")
    class Delete {

        @Test
        @DisplayName("按 platformId+localChannelId 删除后查不到")
        void should_delete() {
            String platformId = UniqueKeyFactory.deviceId();
            String channelId = UniqueKeyFactory.channelId();
            cascadeChannelManager.add(buildDto(platformId, channelId));

            CascadeChannelDTO dto = new CascadeChannelDTO();
            dto.setPlatformId(platformId);
            dto.setLocalChannelId(channelId);
            assertTrue(cascadeChannelManager.deleteOne(dto));

            assertNull(cascadeChannelManager.getByPlatformAndChannel(platformId, channelId));
        }
    }
}
