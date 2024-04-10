package io.github.lunasaw.voglander.repository.local;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;

/**
 * 本地缓存
 *
 * @author linyong
 */
public class LocalCacheBase {

    private static final Logger                       logger   = LoggerFactory.getLogger(LocalCacheBase.class);
    private static final Map<String, Cache<String, Object>> cacheMap = Maps.newConcurrentMap();

    private String                                    prefixKey;

    public LocalCacheBase(String prefixKey) {
        if (StringUtils.isBlank(prefixKey)) {
            return;
        }

        if (!cacheMap.containsKey(prefixKey)) {
            Cache<String, Object> cache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.SECONDS).build();
            cacheMap.put(prefixKey, cache);
        }

        this.prefixKey = prefixKey;
    }

    public LocalCacheBase(String prefixKey, int maxSize) {
        if (StringUtils.isBlank(prefixKey)) {
            return;
        }

        if (!cacheMap.containsKey(prefixKey)) {
            Cache<String, Object> cache = CacheBuilder.newBuilder().maximumSize(maxSize).expireAfterWrite(5, TimeUnit.SECONDS).build();
            cacheMap.put(prefixKey, cache);
        }

        this.prefixKey = prefixKey;
    }

    /**
     * @param prefixKey
     * @param duration 过期时间，秒
     */
    public LocalCacheBase(String prefixKey, long duration) {
        if (StringUtils.isBlank(prefixKey)) {
            return;
        }

        if (!cacheMap.containsKey(prefixKey)) {
            Cache<String, Object> cache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(duration, TimeUnit.SECONDS).build();
            cacheMap.put(prefixKey, cache);
        }

        this.prefixKey = prefixKey;
    }

    public static void main(String[] args) {
        Cache<String, Object> cache = CacheBuilder.newBuilder().maximumSize(2).expireAfterWrite(2, TimeUnit.SECONDS).build();
        cache.put("test", "123");
        cache.put("test2", "456");
        cache.put("test3", "789");
        System.out.println(cache.size());
        System.out.println(cache.getIfPresent("test"));
        System.out.println(cache.getIfPresent("test2"));
        System.out.println(cache.getIfPresent("test3"));
    }

    /**
     * 获取缓存中的value，有可能为Null
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> T get(String key) {
        if (key == null) {
            return null;
        }

        try {
            Cache<String, Object> cache = cacheMap.get(prefixKey);
            if (cache == null) {
                return null;
            }

            Object res = cache.getIfPresent(key);
            if (res == null) {
                return null;
            }

            return (T)res;
        } catch (Exception e) {
            logger.error("LocalCacheBase get throw exception.", e);
        }

        return null;
    }

    public <T> void set(String key, T value) {
        if (StringUtils.isBlank(key)) {
            return;
        }

        if (value == null) {
            return;
        }

        try {
            Cache<String, Object> cache = cacheMap.get(prefixKey);
            if (cache == null) {
                return;
            }

            cache.put(key, value);
        } catch (Exception e) {
            logger.error("LocalCacheBase set throw exception.", e);
        }
    }

    public <T> void mset(Map<String, T> map) {
        if (MapUtils.isEmpty(map)) {
            return;
        }

        try {
            Cache<String, Object> cache = cacheMap.get(prefixKey);
            if (cache == null) {
                return;
            }

            cache.putAll(map);
        } catch (Exception e) {
            logger.error("LocalCacheBase mset throw exception.", e);
        }
    }

    public <T> Map<String, T> mget(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return null;
        }

        try {
            Cache<String, Object> cache = cacheMap.get(prefixKey);
            if (cache == null) {
                return null;
            }

            Map<String, T> res = (Map<String, T>)cache.getAllPresent(keys);
            return res;
        } catch (Exception e) {
            logger.error("LocalCacheBase mget throw exception.", e);
        }

        return null;
    }
}
