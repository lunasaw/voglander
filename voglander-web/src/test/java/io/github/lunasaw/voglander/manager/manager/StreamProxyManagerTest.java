package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * StreamProxyManager 集成测试
 *
 * @author luna
 */
@DisplayName("StreamProxyManager 集成测试")
@Transactional
class StreamProxyManagerTest extends BaseTest {

    @Autowired
    private StreamProxyManager streamProxyManager;

    private StreamProxyDTO buildDto(String app, String stream) {
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setApp(app);
        dto.setStream(stream);
        dto.setUrl("rtsp://127.0.0.1/test");
        return dto;
    }

    @Nested
    @DisplayName("createStreamProxy 默认值")
    class Create {

        @Test
        @DisplayName("创建代理应设置 status=1, onlineStatus=0 并返回ID")
        void should_set_defaults_and_return_id() {
            String app = UniqueKeyFactory.app();
            String stream = UniqueKeyFactory.stream();
            Long id = streamProxyManager.createStreamProxy(buildDto(app, stream));
            assertNotNull(id);

            StreamProxyDTO saved = streamProxyManager.getById(id);
            assertNotNull(saved);
            assertEquals(1, saved.getStatus());
            assertEquals(0, saved.getOnlineStatus());
        }

        @Test
        @DisplayName("缺少 url 应抛出参数异常")
        void should_throw_when_url_missing() {
            StreamProxyDTO dto = new StreamProxyDTO();
            dto.setApp(UniqueKeyFactory.app());
            dto.setStream(UniqueKeyFactory.stream());
            assertThrows(Exception.class, () -> streamProxyManager.add(dto));
        }
    }

    @Nested
    @DisplayName("updateStreamProxyOnlineStatus")
    class OnlineStatus {

        @Test
        @DisplayName("应只更新 onlineStatus 字段")
        void should_update_only_online_status() {
            String app = UniqueKeyFactory.app();
            String stream = UniqueKeyFactory.stream();
            Long id = streamProxyManager.createStreamProxy(buildDto(app, stream));

            streamProxyManager.updateStreamProxyOnlineStatus(id, 1, "上线");

            StreamProxyDTO updated = streamProxyManager.getById(id);
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
            String app = UniqueKeyFactory.app();
            String stream = UniqueKeyFactory.stream();
            Long id = streamProxyManager.createStreamProxy(buildDto(app, stream));

            // 设置 proxyKey
            streamProxyManager.updateStreamProxyKey(id, "key-" + id, "设置密钥");

            boolean result = streamProxyManager.deleteByProxyKey("key-" + id, "删除");
            assertTrue(result);
            assertNull(streamProxyManager.getById(id));
        }

        @Test
        @DisplayName("deleteOne 按 app+stream 应删除对应记录")
        void should_delete_by_app_stream() {
            String app = UniqueKeyFactory.app();
            String stream = UniqueKeyFactory.stream();
            Long id = streamProxyManager.createStreamProxy(buildDto(app, stream));

            StreamProxyDTO deleteDto = new StreamProxyDTO();
            deleteDto.setApp(app);
            deleteDto.setStream(stream);
            assertTrue(streamProxyManager.deleteOne(deleteDto));
            assertNull(streamProxyManager.getById(id));
        }
    }

    @Nested
    @DisplayName("getPage 分页排序")
    class Pagination {

        @Test
        @DisplayName("getPage 应按 createTime 降序返回结果")
        void should_order_by_create_time_desc() throws InterruptedException {
            String app1 = UniqueKeyFactory.app();
            String app2 = UniqueKeyFactory.app();
            streamProxyManager.createStreamProxy(buildDto(app1, UniqueKeyFactory.stream()));
            Thread.sleep(5);
            streamProxyManager.createStreamProxy(buildDto(app2, UniqueKeyFactory.stream()));

            Page<StreamProxyDTO> page = streamProxyManager.getPage(null, 1, 10);
            assertTrue(page.getTotal() >= 2);
            if (page.getRecords().size() >= 2) {
                assertTrue(
                    !page.getRecords().get(0).getCreateTime()
                        .isBefore(page.getRecords().get(1).getCreateTime()),
                    "首条记录 createTime 应 >= 第二条");
            }
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
            streamProxyManager.createStreamProxy(buildDto(app, stream));

            // 通过 update 接口，先查再更新（不调用 add）
            StreamProxyDTO updateDto = buildDto(app, stream);
            updateDto.setUrl("rtsp://127.0.0.1/updated");
            assertDoesNotThrow(() -> streamProxyManager.update(updateDto));
        }
    }
}
