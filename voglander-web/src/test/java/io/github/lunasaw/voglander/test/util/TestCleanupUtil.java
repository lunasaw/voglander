package io.github.lunasaw.voglander.test.util;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试数据清理工具类
 * 提供常见的测试清理操作，用于异步测试的手动清理
 *
 * <h3>使用场景</h3>
 * <p>
 * 在 {@link io.github.lunasaw.voglander.BaseAsyncTest} 的 {@code @AfterEach} 方法中，
 * 手动清理测试数据和缓存。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @SpringBootTest
 * class MyAsyncTest extends BaseAsyncTest {
 *
 *     @Autowired
 *     private CacheManager cacheManager;
 *
 *     @Autowired
 *     private DeviceMapper deviceMapper;
 *
 *     private Long testDeviceId;
 *
 *     @Test
 *     void testAsyncOperation() {
 *         // 测试逻辑
 *         testDeviceId = 12345L;
 *     }
 *
 *     @AfterEach
 *     void cleanup() {
 *         // 清理数据库
 *         TestCleanupUtil.safeDelete(testDeviceId, deviceMapper::deleteById);
 *
 *         // 清理缓存
 *         TestCleanupUtil.clearCache(cacheManager, "deviceCache", testDeviceId);
 *     }
 * }
 * }</pre>
 *
 * @author luna
 * @date 2026/07/08
 */
@Slf4j
public class TestCleanupUtil {

    /**
     * 安全删除：检查 ID 不为 null 后再删除
     *
     * @param id            要删除的 ID
     * @param deleteAction  删除操作（如 mapper::deleteById）
     * @param <ID>          ID 类型
     */
    public static <ID> void safeDelete(ID id, java.util.function.Consumer<ID> deleteAction) {
        if (id != null) {
            try {
                deleteAction.accept(id);
                log.debug("Test cleanup: deleted entity with id={}", id);
            } catch (Exception e) {
                log.warn("Test cleanup failed for id={}: {}", id, e.getMessage());
            }
        }
    }

    /**
     * 批量安全删除
     *
     * @param ids           要删除的 ID 列表
     * @param deleteAction  删除操作
     * @param <ID>          ID 类型
     */
    public static <ID> void safeDeleteBatch(java.util.Collection<ID> ids, java.util.function.Consumer<ID> deleteAction) {
        if (ids != null && !ids.isEmpty()) {
            ids.forEach(id -> safeDelete(id, deleteAction));
        }
    }

    /**
     * 清理指定缓存的特定键
     *
     * @param cacheManager  缓存管理器
     * @param cacheName     缓存名称
     * @param key           缓存键
     */
    public static void clearCache(CacheManager cacheManager, String cacheName, Object key) {
        if (cacheManager == null || cacheName == null || key == null) {
            return;
        }

        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                log.debug("Test cleanup: evicted cache {}#{}", cacheName, key);
            }
        } catch (Exception e) {
            log.warn("Test cleanup: failed to evict cache {}#{}: {}", cacheName, key, e.getMessage());
        }
    }

    /**
     * 清空指定缓存的所有内容
     *
     * @param cacheManager  缓存管理器
     * @param cacheName     缓存名称
     */
    public static void clearAllCache(CacheManager cacheManager, String cacheName) {
        if (cacheManager == null || cacheName == null) {
            return;
        }

        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Test cleanup: cleared all cache in {}", cacheName);
            }
        } catch (Exception e) {
            log.warn("Test cleanup: failed to clear cache {}: {}", cacheName, e.getMessage());
        }
    }

    /**
     * 清空所有缓存
     *
     * @param cacheManager  缓存管理器
     */
    public static void clearAllCaches(CacheManager cacheManager) {
        if (cacheManager == null) {
            return;
        }

        try {
            cacheManager.getCacheNames().forEach(cacheName -> clearAllCache(cacheManager, cacheName));
            log.debug("Test cleanup: cleared all caches");
        } catch (Exception e) {
            log.warn("Test cleanup: failed to clear all caches: {}", e.getMessage());
        }
    }

    /**
     * 按前缀删除测试数据
     * 用于批量清理以特定前缀开始的测试数据
     *
     * @param prefix        测试数据 ID 前缀（如 "test-"）
     * @param queryAction   查询操作，返回匹配前缀的 ID 列表
     * @param deleteAction  删除操作
     * @param <ID>          ID 类型
     */
    public static <ID> void deleteByPrefix(String prefix,
        java.util.function.Function<String, java.util.List<ID>> queryAction,
        java.util.function.Consumer<ID> deleteAction) {

        if (prefix == null || queryAction == null || deleteAction == null) {
            return;
        }

        try {
            java.util.List<ID> ids = queryAction.apply(prefix);
            if (ids != null && !ids.isEmpty()) {
                safeDeleteBatch(ids, deleteAction);
                log.debug("Test cleanup: deleted {} entities with prefix '{}'", ids.size(), prefix);
            }
        } catch (Exception e) {
            log.warn("Test cleanup: failed to delete by prefix '{}': {}", prefix, e.getMessage());
        }
    }

    /**
     * 等待异步操作完成的辅助方法
     *
     * @param latch          CountDownLatch
     * @param timeoutSeconds 超时秒数
     * @param operationDesc  操作描述（用于日志）
     * @return true 如果在超时前完成，false 如果超时
     */
    public static boolean awaitCompletion(java.util.concurrent.CountDownLatch latch, long timeoutSeconds,
        String operationDesc) {
        try {
            boolean completed = latch.await(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            if (completed) {
                log.debug("Async operation '{}' completed within {} seconds", operationDesc, timeoutSeconds);
            } else {
                log.warn("Async operation '{}' timed out after {} seconds", operationDesc, timeoutSeconds);
            }
            return completed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Async operation '{}' interrupted", operationDesc, e);
            return false;
        }
    }
}
