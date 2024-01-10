package io.github.lunasaw.voglander.repository.tair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Resource;

import io.github.lunasaw.voglander.common.constant.CacheConstants;
import io.github.lunasaw.voglander.repository.local.LocalCacheBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

import io.github.lunasaw.voglander.repository.redis.RedisCache;

@Component
public class TairManager {

    private static final Logger logger = LoggerFactory.getLogger("cache-log");

    @Resource
    private RedisCache vdianRedisClient;

    public <T> List<T> getMultiDataList(List<String/**key**/> keys, Function<List<String/**key**/>, Map<String/**key**/, List<T>>> function, Class<T> dataClass,TairContext context) {
        List<T> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(keys)) {
            return result;
        }
        KeyPrefixEnum keyPrefixEnum = context.getKeyPrefixEnum();
        List<String> unCachedKeyList = new ArrayList<>();
        for (String key : keys) {
            if (!context.isLocalCacheQuery()){
                unCachedKeyList.add(key);
                continue;
            }
            if (CacheConstants.TAIR_USE_HOT_CACHE || CacheConstants.TAIR_USE_LOCAL_CACHE){
                if (CacheConstants.TAIR_USE_HOT_CACHE){
//                    Object value = WdHotKeyStore.getValue(key);
//                    if (value != null) {
//                        Collection<T> values = (Collection<T>) value;
//                        result.addAll(values);
//                    } else {
//                        unCachedKeyList.add(key);
//                    }
                }else if (CacheConstants.TAIR_USE_LOCAL_CACHE){
                    LocalCacheBase localCache = new LocalCacheBase(keyPrefixEnum.getPrefix(), keyPrefixEnum.getMaxSize());
                    Object value = localCache.get(key);
                    if (value != null) {
                        Collection<T> values = (Collection<T>) value;
                        result.addAll(values);
                    } else {
                        unCachedKeyList.add(key);
                    }
                }
            }else {
                unCachedKeyList.add(key);
            }
        }
        if (CollectionUtils.isEmpty(unCachedKeyList)){
            return result;
        }
        if (context.isRedisCacheQuery()){
            try {
                Stopwatch started = null;
                if (CacheConstants.PRINT_TAIR_USE_TIME_LOG) {
                    started = Stopwatch.createStarted();
                }
                List<String> cacheResultList = vdianRedisClient.getCacheList(unCachedKeyList);
                if (CacheConstants.PRINT_TAIR_USE_TIME_LOG) {
                    long elapsed = started.elapsed(TimeUnit.MILLISECONDS);
                    logger.warn("getMultiDataList from redis use time:{}ms,keys:{}", elapsed,unCachedKeyList.toString());
                }
                List<String> redisCachedKeys = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(cacheResultList)) {
                    for (int i = 0; i < cacheResultList.size(); i++) {
                        String cacheObjStr = cacheResultList.get(i);
                        if (StringUtils.isNotEmpty(cacheObjStr)) {
                            List<T> redisDataList = JSON.parseArray(cacheObjStr, dataClass);
                            String key = unCachedKeyList.get(i);
                            if (CacheConstants.TAIR_USE_HOT_CACHE){
//                                WdHotKeyStore.smartSet(key, redisDataList);
                            }else if (CacheConstants.TAIR_USE_LOCAL_CACHE){
                                LocalCacheBase localCache = new LocalCacheBase(keyPrefixEnum.getPrefix(),keyPrefixEnum.getMaxSize());
                                localCache.set(key,redisDataList);
                            }
                            result.addAll(redisDataList);
                            redisCachedKeys.add(key);
                        }
                    }
                }
                unCachedKeyList.removeAll(redisCachedKeys);
            } catch (Exception e) {
                logger.error("getMultiDataList from redis error,keys:{}", unCachedKeyList.toString(), e);
            }
        }
        if (CollectionUtils.isNotEmpty(unCachedKeyList)) {
            Stopwatch started = null;
            if (CacheConstants.PRINT_TAIR_USE_TIME_LOG) {
                started = Stopwatch.createStarted();
            }
            Map<String, List<T>> dbDataMap = null;
            try {
                dbDataMap = function.apply(unCachedKeyList);
            } catch (Exception e) {
                logger.error("getMultiDataList from db error,keys:{}", unCachedKeyList.toString(), e);
            }
            if (CacheConstants.PRINT_TAIR_USE_TIME_LOG) {
                long elapsed = started.elapsed(TimeUnit.MILLISECONDS);
                logger.warn("getMultiDataList from db use time:{}ms,keys:{}", elapsed,unCachedKeyList.toString());
            }
            if (MapUtils.isNotEmpty(dbDataMap)) {
                dbDataMap.values().forEach(result::addAll);
                Map<String, String> redisMap = Maps.newHashMapWithExpectedSize(dbDataMap.size());
                dbDataMap.entrySet().forEach(entry -> {
                    String key = entry.getKey();
                    List<T> value = entry.getValue();
                    if (CacheConstants.PRINT_TAIR_USE_TIME_LOG && value.size() > CacheConstants.TAIR_MULTI_DATA_SIZE){
                        logger.warn("multiDataList size too much,key:{},size:{}", key,value.size());
                    }
                    if (CacheConstants.TAIR_USE_HOT_CACHE){
                        WdHotKeyStore.smartSet(key, value);
                    }else if (CacheConstants.TAIR_USE_LOCAL_CACHE){
                        LocalCacheBase localCache = new LocalCacheBase(keyPrefixEnum.getPrefix(),keyPrefixEnum.getMaxSize());
                        localCache.set(key,value);
                    }
                    redisMap.put(key, JSON.toJSONString(value));
                });
                try {
                    if (CacheConstants.PRINT_TAIR_USE_TIME_LOG) {
                        started.reset().start();
                    }
                    vdianRedisClient.multiSet(redisMap, context.getRedisCacheSeconds());
                    if (CacheConstants.PRINT_TAIR_USE_TIME_LOG) {
                        long elapsed = started.elapsed(TimeUnit.MILLISECONDS);
                        logger.warn("getMultiDataList set to redis use time:{}ms", elapsed);
                    }
                } catch (Exception e) {
                    logger.error("set data to redis error,keys:{}", redisMap.keySet().toString(), e);
                }

            }
        }
        return result;
    }


    public <T> List<T> getSingleDataList(List<String/**key**/> keys, Function<List<String/**key**/>, Map<String/**key**/, T>> function, Class<T> dataClass,TairContext context) {
        List<T> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(keys)) {
            return result;
        }
        KeyPrefixEnum keyPrefixEnum = context.getKeyPrefixEnum();
        List<String> unCachedKeyList = new ArrayList<>();
        for (String key : keys) {
            if (!context.isLocalCacheQuery()){
                unCachedKeyList.add(key);
                continue;
            }
            if (CacheConstants.TAIR_USE_HOT_CACHE || CacheConstants.TAIR_USE_LOCAL_CACHE){
                if (CacheConstants.TAIR_USE_HOT_CACHE){
//                    Object value = WdHotKeyStore.getValue(key);
//                    if (value != null) {
//                        result.add((T) value);
//                    } else {
//                        unCachedKeyList.add(key);
//                    }
                }else if (CacheConstants.TAIR_USE_LOCAL_CACHE){
                    LocalCacheBase localCache = new LocalCacheBase(keyPrefixEnum.getPrefix(),keyPrefixEnum.getMaxSize());
                    Object value = localCache.get(key);
                    if (value != null) {
                        result.add((T) value);
                    } else {
                        unCachedKeyList.add(key);
                    }
                }
            }else {
                unCachedKeyList.add(key);
            }
        }
        if (CollectionUtils.isEmpty(unCachedKeyList)){
            return result;
        }
        if (context.isRedisCacheQuery()){
            try {
                List<String> cacheResultList = vdianRedisClient.getCacheList(unCachedKeyList);
                List<String> redisCachedKeys = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(cacheResultList)) {
                    for (int i = 0; i < cacheResultList.size(); i++) {
                        String cacheObjStr = cacheResultList.get(i);
                        if (StringUtils.isNotEmpty(cacheObjStr)) {
                            String key = unCachedKeyList.get(i);
                            T data = JSON.parseObject(cacheObjStr, dataClass);
                            if (CacheConstants.TAIR_USE_HOT_CACHE){
//                                WdHotKeyStore.smartSet(key, data);
                            }else if (CacheConstants.TAIR_USE_LOCAL_CACHE){
                                LocalCacheBase localCache = new LocalCacheBase(keyPrefixEnum.getPrefix(),keyPrefixEnum.getMaxSize());
                                localCache.set(key,data);
                            }
                            result.add(data);
                            redisCachedKeys.add(key);
                        }
                    }
                }
                unCachedKeyList.removeAll(redisCachedKeys);
            } catch (Exception e) {
                logger.error("getSingleDataList from redis error,keys:{}", unCachedKeyList.toString(), e);
            }
        }
        if (CollectionUtils.isNotEmpty(unCachedKeyList)) {
            Map<String, T> dbDataMap = null;
            try {
                dbDataMap = function.apply(unCachedKeyList);
            } catch (Exception e) {
                logger.error("getSingleDataList from db error,keys:{}", unCachedKeyList.toString(), e);
            }
            if (MapUtils.isNotEmpty(dbDataMap)) {
                result.addAll(dbDataMap.values());
                Map<String, String> redisMap = Maps.newHashMapWithExpectedSize(dbDataMap.size());
                dbDataMap.entrySet().forEach(entry -> {
                    String key = entry.getKey();
                    T value = entry.getValue();
                    if (CacheConstants.TAIR_USE_HOT_CACHE){
//                        WdHotKeyStore.smartSet(key, value);
                    }else if (CacheConstants.TAIR_USE_LOCAL_CACHE){
                        LocalCacheBase localCache = new LocalCacheBase(keyPrefixEnum.getPrefix(),keyPrefixEnum.getMaxSize());
                        localCache.set(key,value);
                    }
                    redisMap.put(key, JSON.toJSONString(value));
                });
                try {
                    vdianRedisClient.multiSet(redisMap, context.getRedisCacheSeconds());
                } catch (Exception e) {
                    logger.error("set data to redis error.keys:{}", redisMap.keySet().toString(), e);
                }
            }
        }
        return result;
    }

}
