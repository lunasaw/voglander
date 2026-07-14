package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseAsyncTest;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.repository.entity.MediaNodeDO;
import io.github.lunasaw.voglander.repository.mapper.MediaNodeMapper;
import io.github.lunasaw.voglander.support.CacheInspector;
import io.github.lunasaw.voglander.support.TestDataCleaner;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * MediaNodeManager 集成测试
 * <p>
 * 继承 {@link BaseAsyncTest}：并发测试无 @Transactional，使用 @AfterEach 手动清理
 *
 * @author luna
 */
@DisplayName("MediaNodeManager 集成测试")
class MediaNodeManagerTest extends BaseAsyncTest {

    @Autowired
    private MediaNodeManager mediaNodeManager;

    @Autowired
    private MediaNodeMapper  mediaNodeMapper;

    @Autowired
    private CacheManager     cacheManager;

    @Autowired
    private TestDataCleaner  cleaner;

    // =========== 基础 CRUD ===========

    @Nested
    @DisplayName("saveOrUpdateNodeStatus")
    class SaveOrUpdate {

        @Test
        @DisplayName("首次调用应新增节点并返回ID")
        void should_create_node_when_not_exists() {
            String serverId = UniqueKeyFactory.serverId();
            Long id = mediaNodeManager.saveOrUpdateNodeStatus(serverId, "secret", System.currentTimeMillis(), "127.0.0.1", "node1");
            assertNotNull(id);

            MediaNodeDO saved = mediaNodeMapper.selectById(id);
            assertNotNull(saved);
            assertEquals(serverId, saved.getServerId());
            assertEquals(1, saved.getStatus());
            assertTrue(saved.getEnabled());
            assertTrue(saved.getHookEnabled());

            // 清理（无事务回滚）
            cleaner.deleteMediaNodeByServerId(serverId);
        }

        @Test
        @DisplayName("已存在节点应更新状态和心跳，不创建新行")
        void should_update_status_when_exists() {
            String serverId = UniqueKeyFactory.serverId();
            Long id1 = mediaNodeManager.saveOrUpdateNodeStatus(serverId, "s", 1000L, "127.0.0.1", "n");
            Long id2 = mediaNodeManager.saveOrUpdateNodeStatus(serverId, "s2", 2000L, "127.0.0.2", "n2");

            assertEquals(id1, id2, "二次调用应返回同一ID");

            long count = mediaNodeMapper.selectCount(
                new LambdaQueryWrapper<MediaNodeDO>().eq(MediaNodeDO::getServerId, serverId));
            assertEquals(1, count, "DB 应只有一条记录");

            cleaner.deleteMediaNodeByServerId(serverId);
        }
    }

    @Nested
    @DisplayName("updateNodeOffline")
    class Offline {

        @Test
        @DisplayName("updateNodeOffline 应将 status 置为 0，enabled 保持不变")
        void should_set_status_offline_keep_enabled() {
            String serverId = UniqueKeyFactory.serverId();
            mediaNodeManager.saveOrUpdateNodeStatus(serverId, "s", 1000L, "127.0.0.1", "n");

            mediaNodeManager.updateNodeOffline(serverId);

            MediaNodeDO node = mediaNodeMapper.selectOne(
                new LambdaQueryWrapper<MediaNodeDO>().eq(MediaNodeDO::getServerId, serverId));
            assertNotNull(node);
            assertEquals(0, node.getStatus());
            assertTrue(node.getEnabled(), "enabled 应保持 true");

            cleaner.deleteMediaNodeByServerId(serverId);
        }
    }

    @Nested
    @DisplayName("过滤查询")
    class FilterQuery {

        @Test
        @DisplayName("getEnabledNodes 只返回 enabled=true 的节点")
        void should_return_only_enabled_nodes() {
            String srv1 = UniqueKeyFactory.serverId();
            String srv2 = UniqueKeyFactory.serverId();
            mediaNodeManager.saveOrUpdateNodeStatus(srv1, "s", 1L, "127.0.0.1", "n1");
            // srv1 在线后置离线（enabled 仍 true）
            mediaNodeManager.updateNodeOffline(srv1);

            // 创建 srv2 后直接禁用
            mediaNodeManager.saveOrUpdateNodeStatus(srv2, "s", 1L, "127.0.0.2", "n2");
            mediaNodeMapper.update(new MediaNodeDO() {{
                setEnabled(false);
            }}, new LambdaQueryWrapper<MediaNodeDO>().eq(MediaNodeDO::getServerId, srv2));

            List<MediaNodeDTO> enabled = mediaNodeManager.getEnabledNodes();
            assertTrue(enabled.stream().anyMatch(n -> srv1.equals(n.getServerId())));
            assertTrue(enabled.stream().noneMatch(n -> srv2.equals(n.getServerId())));

            cleaner.deleteMediaNodeByServerId(srv1);
            cleaner.deleteMediaNodeByServerId(srv2);
        }

        @Test
        @DisplayName("getOnlineNodes 只返回 enabled=true 且 status=1 的节点")
        void should_return_only_online_nodes() {
            String onlineSrv = UniqueKeyFactory.serverId();
            String offlineSrv = UniqueKeyFactory.serverId();
            mediaNodeManager.saveOrUpdateNodeStatus(onlineSrv, "s", 1L, "127.0.0.1", "online");
            mediaNodeManager.saveOrUpdateNodeStatus(offlineSrv, "s", 1L, "127.0.0.2", "offline");
            mediaNodeManager.updateNodeOffline(offlineSrv);

            List<MediaNodeDTO> online = mediaNodeManager.getOnlineNodes();
            assertTrue(online.stream().anyMatch(n -> onlineSrv.equals(n.getServerId())));
            assertTrue(online.stream().noneMatch(n -> offlineSrv.equals(n.getServerId())));

            cleaner.deleteMediaNodeByServerId(onlineSrv);
            cleaner.deleteMediaNodeByServerId(offlineSrv);
        }
    }

    @Nested
    @DisplayName("并发场景")
    class Concurrent {

        @Test
        @DisplayName("两线程并发上报同 serverId 应转更新而非抛异常，DB 只有一行")
        void should_upsert_on_duplicate_when_concurrent_same_serverId() throws Exception {
            String serverId = UniqueKeyFactory.serverId();
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);
            AtomicReference<Throwable> err = new AtomicReference<>();

            Runnable task = () -> {
                try {
                    start.await();
                    mediaNodeManager.saveOrUpdateNodeStatus(
                        serverId, "secret-" + Thread.currentThread().getId(),
                        System.currentTimeMillis(), "127.0.0.1", "node");
                } catch (Throwable t) {
                    err.set(t);
                } finally {
                    done.countDown();
                }
            };

            new Thread(task).start();
            new Thread(task).start();
            start.countDown();
            assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertNull(err.get(), () -> "并发不应抛异常: " + (err.get() != null ? err.get().getMessage() : ""));

            long count = mediaNodeMapper.selectCount(
                new LambdaQueryWrapper<MediaNodeDO>().eq(MediaNodeDO::getServerId, serverId));
            assertEquals(1, count, "DB 应只有一条记录");

            cleaner.deleteMediaNodeByServerId(serverId);
        }
    }

    @Nested
    @DisplayName("缓存行为")
    class Cache {

        @Test
        @DisplayName("getByServerId 二次调用应命中缓存")
        void should_hit_cache_on_second_call() {
            String serverId = UniqueKeyFactory.serverId();
            mediaNodeManager.saveOrUpdateNodeStatus(serverId, "s", 1L, "127.0.0.1", "n");

            // 触发写缓存
            mediaNodeManager.getByServerId(serverId);
            // 验证缓存命中
            CacheInspector ci = new CacheInspector(cacheManager);
            assertTrue(ci.isHit("mediaNode", "unique:" + serverId));

            cleaner.deleteMediaNodeByServerId(serverId);
        }
    }
}
