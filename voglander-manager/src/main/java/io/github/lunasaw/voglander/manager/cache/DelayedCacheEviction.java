package io.github.lunasaw.voglander.manager.cache;

import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * 延迟双删（Phase 1）。
 * <p>
 * 写操作先立即 evict，再把 key 投入 Redis ZSet {@code cache:evict:delay}（score = now+500ms），
 * 由所有节点共消费的定时扫描器到点取出再 evict 一次，修 A4（JVM 定时器进程崩溃后脏读到 TTL）。
 * </p>
 * <p>member 格式：{@code cacheName::key}，分隔符双冒号避免 key 本身带单冒号时歧义。</p>
 *
 * @author luna
 */
@Slf4j
public class DelayedCacheEviction {

    public static final String EVICT_QUEUE_KEY = "cache:evict:delay";

    private static final long  DELAY_MS        = 500L;

    private static final String SEPARATOR = "::";

    private final StringRedisTemplate redisTemplate;
    private final CacheManager        cacheManager;

    public DelayedCacheEviction(StringRedisTemplate redisTemplate, CacheManager cacheManager) {
        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManager;
    }

    /**
     * 将 (cacheName, key) 投入延迟队列，500ms 后由 {@link #drainDue} 消费。
     */
    public void scheduleEvict(String cacheName, String key) {
        try {
            String member = cacheName + SEPARATOR + key;
            double score = System.currentTimeMillis() + DELAY_MS;
            redisTemplate.opsForZSet().add(EVICT_QUEUE_KEY, member, score);
        } catch (Exception e) {
            log.warn("scheduleEvict 写入 Redis 失败（降级为无延迟）: cacheName={}, key={}", cacheName, key, e);
        }
    }

    /**
     * 取出 score ≤ nowMs 的到期条目并 evict，原子 ZREM 保证多节点幂等。
     */
    public void drainDue(long nowMs) {
        try {
            Set<String> due = redisTemplate.opsForZSet().rangeByScore(EVICT_QUEUE_KEY, 0, nowMs);
            if (due == null || due.isEmpty()) {
                return;
            }
            for (String member : due) {
                // ZREM 原子取出：返回 1 才执行 evict，防止他节点重复执行
                Long removed = redisTemplate.opsForZSet().remove(EVICT_QUEUE_KEY, member);
                if (removed != null && removed > 0) {
                    evictMember(member);
                }
            }
        } catch (Exception e) {
            log.warn("drainDue Redis 故障，跳过本轮延迟 evict", e);
        }
    }

    private void evictMember(String member) {
        int sep = member.indexOf(SEPARATOR);
        if (sep <= 0) {
            return;
        }
        String cacheName = member.substring(0, sep);
        String key = member.substring(sep + SEPARATOR.length());
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("延迟 evict: cache={}, key={}", cacheName, key);
        }
    }
}
