package io.github.lunasaw.voglander.support;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 缓存验证工具，封装 CacheManager 提供便利断言方法
 *
 * @author luna
 */
public class CacheInspector {

    private final CacheManager cacheManager;

    public CacheInspector(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Object get(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return null;
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper != null ? wrapper.get() : null;
    }

    public boolean isHit(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return false;
        return cache.get(key) != null;
    }

    public void assertHit(String cacheName, Object key) {
        assertNotNull(get(cacheName, key),
            String.format("期望缓存命中 [%s / %s]，但未命中", cacheName, key));
    }

    public void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.evict(key);
    }

    public void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
    }
}
