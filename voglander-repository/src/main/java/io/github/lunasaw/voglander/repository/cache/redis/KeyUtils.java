package io.github.lunasaw.voglander.repository.cache.redis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: luna
 * @create: 2022-04-06 19:10
 **/
public class KeyUtils {

    private static final Logger logger = LoggerFactory.getLogger(KeyUtils.class);

    /**
     * 获取查询一条数据时候的缓存的key
     * 
     * @param args
     * @param cached
     * @return
     */
    public static String getSingleCacheKey(Object[] args, Cached cached) {
        int keyParamIndex = cached.keyParamIndex();

        // 指定的作为key的参数不存在
        if (keyParamIndex + 1 > args.length) {
            return cached.cacheKeyPrefix();
        }

        Object keyObj = args[keyParamIndex];
        return contactCacheKey(keyObj, cached.cacheKeyPrefix());
    }

    /**
     * 获取查询一堆数据时候的缓存key与原始key之间的对应关系
     * 
     * @param args
     * @param cached
     * @return
     */
    public static Map<String, Object> getMultiCacheKey(Object[] args, Cached cached) {
        Map<String, Object> keyPairs = new HashMap<>();
        int keyParamIndex = cached.keyParamIndex();
        //
        // 指定的作为key的参数不存在
        if (keyParamIndex + 1 > args.length) {
            logger.error("keyParamIndex greater than length of args: {}, {}", keyParamIndex, args.length);
            return null;
        }

        try {
            Object keyObjs = args[keyParamIndex];

            if (!(keyObjs instanceof Collection)) {
                logger.error("keyParam is not Collection: {}", keyObjs);
                return null;
            }

            for (Object keyObj : (Collection<Object>)keyObjs) {
                keyPairs.put(contactCacheKey(keyObj, cached.cacheKeyPrefix()), keyObj);
            }
        } catch (Exception e) {
            logger.error("getMultiCacheKey [" + args + "] failed.", e);
            return null;
        }

        return keyPairs;
    }

    public static String contactCacheKey(Object key, String cacheKey) {
        return cacheKey + key;
    }
}
