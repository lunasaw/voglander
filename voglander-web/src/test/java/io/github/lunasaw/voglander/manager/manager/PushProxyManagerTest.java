package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * PushProxyManager 集成测试
 *
 * @author luna
 */
@DisplayName("PushProxyManager 集成测试")
@Transactional
class PushProxyManagerTest extends BaseTest {

    @Autowired
    private PushProxyManager pushProxyManager;

    private PushProxyDTO buildDto(String app, String stream) {
        PushProxyDTO dto = new PushProxyDTO();
        dto.setApp(app);
        dto.setStream(stream);
        dto.setDstUrl("rtmp://127.0.0.1/live");
        return dto;
    }

    @Nested
    @DisplayName("createPushProxy 默认值")
    class Create {

        @Test
        @DisplayName("创建推流代理应设置 status=1, onlineStatus=0 并返回ID")
        void should_set_defaults_and_return_id() {
            Long id = pushProxyManager.createPushProxy(buildDto(UniqueKeyFactory.app(), UniqueKeyFactory.stream()));
            assertNotNull(id);

            PushProxyDTO saved = pushProxyManager.getById(id);
            assertNotNull(saved);
            assertEquals(1, saved.getStatus());
            assertEquals(0, saved.getOnlineStatus());
        }

        @Test
        @DisplayName("缺少 dstUrl 应抛出参数异常")
        void should_throw_when_dst_url_missing() {
            PushProxyDTO dto = new PushProxyDTO();
            dto.setApp(UniqueKeyFactory.app());
            dto.setStream(UniqueKeyFactory.stream());
            assertThrows(Exception.class, () -> pushProxyManager.add(dto));
        }
    }

    @Nested
    @DisplayName("updatePushProxyOnlineStatus")
    class OnlineStatus {

        @Test
        @DisplayName("应只更新 onlineStatus 字段")
        void should_update_only_online_status() {
            Long id = pushProxyManager.createPushProxy(buildDto(UniqueKeyFactory.app(), UniqueKeyFactory.stream()));

            pushProxyManager.updatePushProxyOnlineStatus(id, 1, "上线");

            PushProxyDTO updated = pushProxyManager.getById(id);
            assertEquals(1, updated.getOnlineStatus());
            assertEquals(1, updated.getStatus(), "status 不应被改变");
        }
    }

    @Nested
    @DisplayName("deleteByProxyKey")
    class Delete {

        @Test
        @DisplayName("deleteByProxyKey 应删除记录")
        void should_delete_by_proxy_key() {
            Long id = pushProxyManager.createPushProxy(buildDto(UniqueKeyFactory.app(), UniqueKeyFactory.stream()));
            pushProxyManager.updatePushProxyKey(id, "pkey-" + id, "设置密钥");

            assertTrue(pushProxyManager.deleteByProxyKey("pkey-" + id, "删除"));
            assertNull(pushProxyManager.getById(id));
        }
    }

    @Nested
    @DisplayName("(app, stream) 复合 UNIQUE")
    class UniqueConstraint {

        @Test
        @DisplayName("重复 app+stream 应先查后更新，不抛 UNIQUE 异常")
        void should_update_on_duplicate_app_stream() {
            String app = UniqueKeyFactory.app();
            String stream = UniqueKeyFactory.stream();
            pushProxyManager.createPushProxy(buildDto(app, stream));

            PushProxyDTO updateDto = buildDto(app, stream);
            updateDto.setDstUrl("rtmp://127.0.0.1/updated");
            assertDoesNotThrow(() -> pushProxyManager.update(updateDto));
        }
    }
}
