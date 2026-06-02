package io.github.lunasaw.voglander.manager.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import lombok.extern.slf4j.Slf4j;

/**
 * Phase 1: 延迟双删单元测试
 * 验证：写操作触发立即 evict + 500ms 后二次 evict（通过 Redis ZSet）
 *
 * @author luna
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class DelayedCacheEvictionTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zsetOps;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private DelayedCacheEviction eviction;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zsetOps);
        eviction = new DelayedCacheEviction(redisTemplate, cacheManager);
    }

    @Test
    void testScheduleEvictEnqueuesKeyToRedisZSet() {
        // 调用 scheduleEvict 后，应将 "cacheName:key" 放入 Redis ZSet
        eviction.scheduleEvict("device", "deviceId:123");

        ArgumentCaptor<String> memberCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        verify(zsetOps, times(1)).add(
            eq(DelayedCacheEviction.EVICT_QUEUE_KEY),
            memberCaptor.capture(),
            scoreCaptor.capture()
        );

        String member = memberCaptor.getValue();
        double score = scoreCaptor.getValue();

        assertEquals("device::deviceId:123", member, "member 格式应为 cacheName::key");
        assertTrue(score > System.currentTimeMillis(), "score 应大于当前时间戳（延迟后的到期时间）");
        assertTrue(score <= System.currentTimeMillis() + 2000, "score 延迟不超过 2s");
        log.info("scheduleEvict 写入 ZSet: member={}, score={}", member, score);
    }

    @Test
    void testDrainDueEvictsFromCache() {
        // 模拟 ZSet 中有到期的 entry
        long nowMs = System.currentTimeMillis();
        Set<String> dueMembers = Set.of("device::deviceId:123", "device::id:456");
        when(zsetOps.rangeByScore(DelayedCacheEviction.EVICT_QUEUE_KEY, 0, nowMs)).thenReturn(dueMembers);
        when(zsetOps.remove(eq(DelayedCacheEviction.EVICT_QUEUE_KEY), any())).thenReturn(1L);
        when(cacheManager.getCache("device")).thenReturn(cache);

        eviction.drainDue(nowMs);

        // 验证每个到期 entry 都 evict 了对应缓存
        verify(cache, times(2)).evict(any());
        log.info("drainDue 正确 evict {} 个缓存条目", dueMembers.size());
    }

    @Test
    void testDrainDueSkipsIfAlreadyConsumedByOtherNode() {
        // ZREM 返回 0 表示已被其他节点消费，不应再 evict
        long nowMs = System.currentTimeMillis();
        Set<String> dueMembers = Set.of("device::deviceId:999");
        when(zsetOps.rangeByScore(DelayedCacheEviction.EVICT_QUEUE_KEY, 0, nowMs)).thenReturn(dueMembers);
        when(zsetOps.remove(eq(DelayedCacheEviction.EVICT_QUEUE_KEY), any())).thenReturn(0L);

        eviction.drainDue(nowMs);

        // ZREM 返回 0 → 不 evict
        verify(cacheManager, never()).getCache(any());
        log.info("ZREM=0 时不重复 evict（幂等保护）");
    }

    @Test
    void testDrainDueSilentlyHandlesRedisFail() {
        long nowMs = System.currentTimeMillis();
        when(zsetOps.rangeByScore(any(), anyDouble(), anyDouble())).thenThrow(new RuntimeException("redis down"));

        // 不抛异常
        assertDoesNotThrow(() -> eviction.drainDue(nowMs));
        log.info("drainDue Redis 故障时静默降级");
    }
}
