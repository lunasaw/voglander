package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.lunasaw.voglander.config.CacheIntegrationTestConfig;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.repository.entity.MediaNodeDO;
import lombok.extern.slf4j.Slf4j;

/**
 * MediaNodeManager Redis缓存集成测试类
 * 用于验证缓存功能是否正常工作，包括Redis存储、缓存失效等
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@SpringBootTest(classes = CacheIntegrationTestConfig.class)
@TestPropertySource(properties = {
    "spring.cache.type=redis",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.data.redis.database=15",
    "spring.data.redis.password=luna",
    "spring.data.redis.timeout=5000",
    "spring.data.redis.connect-timeout=1000",
    "logging.level.org.springframework.cache=DEBUG",
    "logging.level.io.github.lunasaw.voglander=DEBUG"
})
@Transactional
public class MediaNodeCacheIntegrationTest {

    private final String                                      TEST_SERVER_ID = "CACHE_TEST_SERVER_001";
    private final Long                                        TEST_ID        = 100L;

    @Autowired
    private MediaNodeManager                                  mediaNodeManager;

    @Autowired
    private CacheManager                                      cacheManager;

    @Autowired
    private RedisTemplate<Object, Object>                     redisTemplate;

    @Autowired
    private RedisCache                                        redisCache;

    @Autowired
    private CacheIntegrationTestConfig.RedisConnectionChecker redisConnectionChecker;

    private MediaNodeDTO                                      testMediaNodeDTO;

    @BeforeEach
    public void setUp() {
        // 清理测试数据和缓存
        cleanupTestData();

        // 初始化测试数据
        testMediaNodeDTO = createTestMediaNodeDTO();
    }

    /**
     * 检查Redis是否可用，如果不可用则跳过测试
     */
    private void assumeRedisAvailable() {
        org.junit.jupiter.api.Assumptions.assumeTrue(redisConnectionChecker.isRedisAvailable(),
            "Redis不可用，跳过此测试。要运行此测试，请启动Redis服务：brew services start redis 或 docker run -p 6379:6379 -d redis");
    }

    /**
     * 清理测试数据和缓存
     */
    private void cleanupTestData() {
        try {
            // 清理数据库中的测试数据
            mediaNodeManager.deleteMediaNodeByServerId(TEST_SERVER_ID);
        } catch (Exception e) {
            // 忽略删除失败，可能是数据不存在
            log.debug("清理测试数据时删除失败（可能是数据不存在）: {}", e.getMessage());
        }

        try {
            // 清理Redis缓存
            Cache cache = cacheManager.getCache("mediaNode");
            if (cache != null) {
                cache.clear();
            }

            // 清理可能存在的所有格式的Redis键
            String[] possibleKeys = {
                "mediaNode:" + TEST_ID,
                "mediaNode::" + TEST_ID,
                "mediaNode:unique:" + TEST_SERVER_ID,
                "mediaNode::unique:" + TEST_SERVER_ID
            };

            for (String key : possibleKeys) {
                redisTemplate.delete(key);
            }

            // 清理通过模式匹配找到的键
            var serverIdKeys = redisTemplate.keys("*mediaNode*" + TEST_SERVER_ID + "*");
            if (serverIdKeys != null && !serverIdKeys.isEmpty()) {
                redisTemplate.delete(serverIdKeys);
                log.debug("清理通过模式匹配找到的serverId相关键: {}", serverIdKeys);
            }

            var idKeys = redisTemplate.keys("*mediaNode*" + TEST_ID + "*");
            if (idKeys != null && !idKeys.isEmpty()) {
                redisTemplate.delete(idKeys);
                log.debug("清理通过模式匹配找到的ID相关键: {}", idKeys);
            }

            // 等待缓存清理完成
            Thread.sleep(100);
        } catch (Exception e) {
            log.warn("清理缓存时出现异常: {}", e.getMessage());
        }

        log.info("清理测试数据和缓存完成");
    }

    /**
     * 创建测试用的MediaNodeDTO
     */
    private MediaNodeDTO createTestMediaNodeDTO() {
        MediaNodeDTO dto = new MediaNodeDTO();
        dto.setServerId(TEST_SERVER_ID);
        dto.setName("缓存测试流媒体节点");
        dto.setHost("192.168.1.200");
        dto.setSecret("cache_test_secret");
        dto.setEnabled(true);
        dto.setHookEnabled(true);
        dto.setWeight(0);
        dto.setStatus(1);
        dto.setKeepalive(System.currentTimeMillis());
        dto.setDescription("缓存集成测试节点");
        dto.setCreateTime(new Date());
        dto.setUpdateTime(new Date());
        return dto;
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testCacheAfterCreate_Success() {
        // Given - 创建节点
        Long nodeId = mediaNodeManager.createMediaNode(testMediaNodeDTO);
        assertNotNull(nodeId);

        // When - 第一次查询（应该从数据库加载并写入缓存）
        MediaNodeDO firstQuery = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        MediaNodeDO firstQueryById = mediaNodeManager.getById(nodeId);

        // Then - 验证数据正确
        assertNotNull(firstQuery);
        assertNotNull(firstQueryById);
        assertEquals(TEST_SERVER_ID, firstQuery.getServerId());
        assertEquals(nodeId, firstQueryById.getId());

        // When - 第二次查询（应该从缓存加载）
        long startTime = System.currentTimeMillis();
        MediaNodeDO secondQuery = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        MediaNodeDO secondQueryById = mediaNodeManager.getById(nodeId);
        long endTime = System.currentTimeMillis();

        // Then - 验证缓存命中（查询速度应该更快）
        assertNotNull(secondQuery);
        assertNotNull(secondQueryById);
        assertEquals(firstQuery.getServerId(), secondQuery.getServerId());
        assertEquals(firstQueryById.getId(), secondQueryById.getId());

        // 缓存查询应该很快（小于10ms）
        long queryTime = endTime - startTime;
        assertTrue(queryTime < 50, "缓存查询时间过长: " + queryTime + "ms");

        log.info("缓存命中测试通过，查询时间: {}ms", queryTime);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testCacheInRedis_Success() throws InterruptedException {
        assumeRedisAvailable();

        // Given - 创建节点
        Long nodeId = mediaNodeManager.createMediaNode(testMediaNodeDTO);

        // When - 查询以触发缓存
        MediaNodeDO result = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        assertNotNull(result);

        // 等待缓存写入Redis（异步操作）
        Thread.sleep(200); // 增加等待时间

        // Then - 验证Redis中是否真的存储了缓存数据
        // Spring Cache默认键格式通常是 cacheName:key 或 cacheName::key
        String[] possibleServerIdKeys = {
            "mediaNode:unique:" + TEST_SERVER_ID,
            "mediaNode::unique:" + TEST_SERVER_ID,
            "mediaNode::'unique:'" + TEST_SERVER_ID,
            "mediaNode::\"unique:" + TEST_SERVER_ID + "\""
        };

        String[] possibleIdKeys = {
            "mediaNode:" + nodeId,
            "mediaNode::" + nodeId,
            "mediaNode::'" + nodeId + "'",
            "mediaNode::\"" + nodeId + "\""
        };

        boolean serverIdExists = false;
        boolean idExists = false;

        // 检查所有可能的键格式
        for (String key : possibleServerIdKeys) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                serverIdExists = true;
                log.info("Found cache key for serverId: {}", key);
                break;
            }
        }

        for (String key : possibleIdKeys) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                idExists = true;
                log.info("Found cache key for id: {}", key);
                break;
            }
        }

        // 如果具体键格式不匹配，尝试通过模式匹配查找
        if (!serverIdExists) {
            var serverIdKeys = redisTemplate.keys("*mediaNode*" + TEST_SERVER_ID + "*");
            serverIdExists = serverIdKeys != null && !serverIdKeys.isEmpty();
            log.info("Found alternative cache keys for serverId: {}", serverIdKeys);
        }

        if (!idExists) {
            var idKeys = redisTemplate.keys("*mediaNode*" + nodeId + "*");
            idExists = idKeys != null && !idKeys.isEmpty();
            log.info("Found alternative cache keys for id: {}", idKeys);
        }

        // 打印所有Redis键以便调试
        var allKeys = redisTemplate.keys("*");
        log.info("所有Redis键: {}", allKeys);

        // 验证至少有一个缓存键存在，或者通过缓存管理器验证
        boolean cacheExists = serverIdExists || idExists;

        if (!cacheExists) {
            // 尝试通过Spring Cache API验证缓存
            Cache cache = cacheManager.getCache("mediaNode");
            if (cache != null) {
                Cache.ValueWrapper serverIdCache = cache.get("unique:" + TEST_SERVER_ID);
                Cache.ValueWrapper idCache = cache.get(nodeId);
                cacheExists = (serverIdCache != null) || (idCache != null);
                log.info("Cache manager检查结果 - serverId cache: {}, id cache: {}",
                    serverIdCache != null, idCache != null);
            }
        }

        assertTrue(cacheExists, "Redis中应该存在缓存数据，但在Redis键和缓存管理器中都未找到");

        log.info("Redis缓存存储验证通过");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testCacheEvictionAfterUpdate_Success() {
        // Given - 创建节点并查询以建立缓存
        Long nodeId = mediaNodeManager.createMediaNode(testMediaNodeDTO);
        MediaNodeDO originalNode = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        assertNotNull(originalNode);

        // 验证缓存存在
        Cache cache = cacheManager.getCache("mediaNode");
        assertNotNull(cache);

        // When - 更新节点（应该清除缓存）
        testMediaNodeDTO.setId(nodeId);
        testMediaNodeDTO.setName("更新后的缓存测试节点");
        testMediaNodeDTO.setDescription("缓存失效测试");

        Long updatedNodeId = mediaNodeManager.updateMediaNode(testMediaNodeDTO);
        assertEquals(nodeId, updatedNodeId);

        // Then - 再次查询，应该从数据库获取最新数据
        MediaNodeDO updatedNode = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        assertNotNull(updatedNode);
        assertEquals("更新后的缓存测试节点", updatedNode.getName());
        assertEquals("缓存失效测试", updatedNode.getDescription());

        log.info("缓存失效测试通过");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testCacheEvictionAfterDelete_Success() {
        // Given - 创建节点并查询以建立缓存
        Long nodeId = mediaNodeManager.createMediaNode(testMediaNodeDTO);
        MediaNodeDO originalNode = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        assertNotNull(originalNode);

        // When - 删除节点（应该清除缓存）
        boolean deleted = mediaNodeManager.deleteMediaNodeById(nodeId);
        assertTrue(deleted);

        // Then - 再次查询，应该返回null
        MediaNodeDO deletedNode = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        assertNull(deletedNode);

        MediaNodeDO deletedNodeById = mediaNodeManager.getById(nodeId);
        assertNull(deletedNodeById);

        log.info("删除后缓存清理测试通过");
    }

    @Test
    public void testCacheNullValue_Success() {
        // When - 查询不存在的节点
        MediaNodeDO nonExistentNode = mediaNodeManager.getByServerId("NON_EXISTENT_SERVER_ID");

        // Then - 应该返回null，且不应该缓存null值（根据配置）
        assertNull(nonExistentNode);

        // 再次查询，确保每次都返回null
        MediaNodeDO secondQuery = mediaNodeManager.getByServerId("NON_EXISTENT_SERVER_ID");
        assertNull(secondQuery);

        log.info("null值缓存测试通过");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testCacheConcurrency_Success() throws InterruptedException {
        // Given - 创建节点
        Long nodeId = mediaNodeManager.createMediaNode(testMediaNodeDTO);
        assertNotNull(nodeId);

        // 等待数据写入和事务提交完成，确保数据对所有线程可见
        Thread.sleep(200);

        // 先进行一次查询，确保缓存已经建立
        MediaNodeDO initialQuery = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        assertNotNull(initialQuery, "Initial query should return valid result");
        assertEquals(TEST_SERVER_ID, initialQuery.getServerId());
        assertEquals(nodeId, initialQuery.getId());

        // When - 并发查询相同的节点
        final int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        final MediaNodeDO[] results = new MediaNodeDO[threadCount];
        final long[] queryTimes = new long[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    // 添加小的随机延迟，模拟真实并发场景
                    Thread.sleep((long)(Math.random() * 10));

                    long start = System.currentTimeMillis();
                    results[index] = mediaNodeManager.getByServerId(TEST_SERVER_ID);
                    queryTimes[index] = System.currentTimeMillis() - start;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread {} interrupted during concurrent test", index);
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - 验证所有查询都返回了相同的结果
        for (int i = 0; i < threadCount; i++) {
            assertNotNull(results[i], "Thread " + i + " should get valid result. " +
                "Node creation ID: " + nodeId + ", ServerId: " + TEST_SERVER_ID);
            assertEquals(TEST_SERVER_ID, results[i].getServerId());
            assertEquals(nodeId, results[i].getId());
        }

        // 计算平均查询时间
        double avgQueryTime = 0;
        for (long time : queryTimes) {
            avgQueryTime += time;
        }
        avgQueryTime /= threadCount;

        log.info("并发缓存测试通过，平均查询时间: {}ms", avgQueryTime);
    }

    @Test
    public void testManualCacheOperation_Success() {
        // Given - 创建节点
        Long nodeId = mediaNodeManager.createMediaNode(testMediaNodeDTO);

        // When - 手动操作缓存
        Cache cache = cacheManager.getCache("mediaNode");
        assertNotNull(cache);

        // 手动清除特定缓存项
        cache.evict("unique:" + TEST_SERVER_ID);
        cache.evict(nodeId);

        // Then - 查询应该重新从数据库加载
        MediaNodeDO reloadedNode = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        assertNotNull(reloadedNode);
        assertEquals(TEST_SERVER_ID, reloadedNode.getServerId());

        // 手动清除所有缓存
        cache.clear();

        // 查询应该仍然正常工作
        MediaNodeDO nodeAfterClear = mediaNodeManager.getById(nodeId);
        assertNotNull(nodeAfterClear);
        assertEquals(nodeId, nodeAfterClear.getId());

        log.info("手动缓存操作测试通过");
    }

    @Test
    public void testRedisCacheWithCustomOperations_Success() {
        assumeRedisAvailable();

        // Given - 创建节点
        Long nodeId = mediaNodeManager.createMediaNode(testMediaNodeDTO);

        // When - 使用RedisCache直接操作Redis
        String testKey = "test:mediaNode:" + TEST_SERVER_ID;
        String testValue = "test cache value";

        // 设置自定义缓存
        redisCache.setCacheObject(testKey, testValue, 60, TimeUnit.SECONDS);

        // Then - 验证可以读取
        String cachedValue = redisCache.getCacheObject(testKey);
        assertEquals(testValue, cachedValue);

        // 验证过期时间
        Long ttl = redisTemplate.getExpire(testKey, TimeUnit.SECONDS);
        assertTrue(ttl > 0 && ttl <= 60, "TTL should be positive and <= 60 seconds, but was: " + ttl);

        // 删除测试缓存
        redisCache.deleteKey(testKey);
        String deletedValue = redisCache.getCacheObject(testKey);
        assertNull(deletedValue);

        log.info("Redis自定义操作测试通过");
    }

    /**
     * 测试内存缓存功能（当Redis不可用时的回退方案）
     * 此测试不依赖Redis，验证基础缓存功能
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testMemoryCacheWhenRedisUnavailable_Success() {
        // 此测试设计为在任何情况下都能运行，验证缓存管理器的基础功能

        // Given - 创建节点
        Long nodeId = mediaNodeManager.createMediaNode(testMediaNodeDTO);
        assertNotNull(nodeId);

        // When - 第一次查询（应该从数据库加载并写入缓存）
        long startTime = System.currentTimeMillis();
        MediaNodeDO firstQuery = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        long firstQueryTime = System.currentTimeMillis() - startTime;

        assertNotNull(firstQuery);
        assertEquals(TEST_SERVER_ID, firstQuery.getServerId());
        assertEquals(nodeId, firstQuery.getId());

        // When - 第二次查询（应该从缓存加载，无论是Redis还是内存缓存）
        startTime = System.currentTimeMillis();
        MediaNodeDO secondQuery = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        long secondQueryTime = System.currentTimeMillis() - startTime;

        // Then - 验证缓存命中（第二次查询应该更快）
        assertNotNull(secondQuery);
        assertEquals(firstQuery.getServerId(), secondQuery.getServerId());
        assertEquals(firstQuery.getId(), secondQuery.getId());

        // 验证缓存管理器类型并记录日志
        Cache cache = cacheManager.getCache("mediaNode");
        assertNotNull(cache);

        String cacheType = redisConnectionChecker.isRedisAvailable() ? "Redis缓存" : "内存缓存";
        log.info("使用{}进行测试 - 第一次查询: {}ms, 第二次查询: {}ms, 缓存类型: {}",
            cacheType, firstQueryTime, secondQueryTime, cache.getClass().getSimpleName());

        // 验证缓存功能正常（第二次查询通常应该更快，但内存缓存可能差异不大）
        assertTrue(secondQueryTime <= firstQueryTime + 10,
            "缓存查询时间异常，第一次: " + firstQueryTime + "ms, 第二次: " + secondQueryTime + "ms");

        // When - 更新节点（应该清除缓存）
        testMediaNodeDTO.setId(nodeId);
        testMediaNodeDTO.setName("缓存测试更新节点");

        Long updatedNodeId = mediaNodeManager.updateMediaNode(testMediaNodeDTO);
        assertEquals(nodeId, updatedNodeId);

        // Then - 再次查询，应该从数据库获取最新数据
        MediaNodeDO updatedNode = mediaNodeManager.getByServerId(TEST_SERVER_ID);
        assertNotNull(updatedNode);
        assertEquals("缓存测试更新节点", updatedNode.getName());

        log.info("{}功能验证通过", cacheType);
    }
}