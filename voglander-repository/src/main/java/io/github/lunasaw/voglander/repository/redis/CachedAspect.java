package io.github.lunasaw.voglander.repository.redis;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author: luna
 * @create: 2022-04-06 17:25
 **/
@Aspect
@Component
@EnableAspectJAutoProxy(exposeProxy = true)
public class CachedAspect {

    private static final Logger logger = LoggerFactory.getLogger("cache-log");
    @Autowired
    private RedisCache          redisCache;

    private static Map<String, Object> batchParseJson(Map<String, String> values, Type returnType) {
        Map<String, Object> ret = Maps.newHashMap();
        for (Map.Entry<String, String> oneEntry : values.entrySet()) {
            String redisValue = oneEntry.getValue();
            if (redisValue != null) {
                ret.put(oneEntry.getKey(), JSONObject.parseObject(redisValue, returnType));
            }
        }

        return ret;
    }

    @Pointcut("@annotation(io.github.lunasaw.voglander.repository.redis.Cached)")
    public void cachedPointCut() {}

    @Around("cachedPointCut()")
    public Object processCached(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature)pjp.getSignature()).getMethod();
        // 修复可能无法获取注解的问题http://wf.vdian.net/browse/STORE-22
        Method realMethod = pjp.getTarget().getClass().getDeclaredMethod(pjp.getSignature().getName(),
            method.getParameterTypes());
        Cached cached = realMethod.getAnnotation(Cached.class);

        Object result;
        Class<?> returnClass = method.getReturnType();
        Type returnType = method.getGenericReturnType();

        Object[] args = pjp.getArgs();
        if (args.length == 0) {
            result = getSingleCache(pjp, cached, returnType);
        } else {
            Class<?> argClass = args[cached.keyParamIndex()].getClass();
            if (Map.class.isAssignableFrom(returnClass) || Collection.class.isAssignableFrom(argClass)) {
                if (Map.class.isAssignableFrom(returnClass)) {
                    Type valueType = ((ParameterizedType)returnType).getActualTypeArguments()[1];
                    result = getMapCache(pjp, cached, valueType);
                } else {
                    result = getCollectionCache(pjp, cached, returnType);
                }
            } else {
                result = getSingleCache(pjp, cached, returnType);
            }
        }

        return result;
    }

    /**
     * 处理返回是一个集合的调用
     *
     * @param pjp
     * @param cached
     * @param returnType
     * @return
     * @throws Throwable
     */
    private Object getCollectionCache(ProceedingJoinPoint pjp, Cached cached, Type returnType) throws Throwable {
        Map<String, Object> keyPairs = KeyUtils.getMultiCacheKey(pjp.getArgs(), cached);
        Set<String> hitKeys = new HashSet<>();

        if (null == keyPairs) {
            return pjp.proceed(pjp.getArgs());
        }

        Map<String, Object> cacheMap = commonBatchGet(keyPairs, hitKeys, returnType);
        Collection<Object> dbResult = new ArrayList<>();
        if (hitKeys.size() < keyPairs.size()) {
            Collection<Object> missedKeys = getMissedKeys(pjp, cached, hitKeys);
            dbResult = (Collection<Object>)getMissedValues(pjp, cached, missedKeys);

            try {
                Map<Object, Object> dbResultMap = convertCollectionToMap(dbResult, cached.keyNameInReturnObject());
                Map<String, Object> writeBackMap = getWriteBackMap(dbResultMap, missedKeys, cached);
                if (writeBackMap.size() > 0 && writeBackMap.size() <= cached.maxValueSize()) {

                    Map<String, String> tmpWriteBackStringMap = batchParseJson2Str(writeBackMap);
                    redisCache.multiSet(tmpWriteBackStringMap, cached.expireTime());
                }
            } catch (Exception e) {
                logger.error("getCollectionCache msetex failed.", e);
            }
        }

        return mergeCollectionResult(dbResult, cacheMap);
    }

    /**
     * 处理返回值是一个map的调用
     *
     * @param pjp
     * @param cached
     * @param returnType
     * @return
     * @throws Throwable
     */
    private Object getMapCache(ProceedingJoinPoint pjp, Cached cached, Type returnType) throws Throwable {
        Map<String, Object> keyPairs = KeyUtils.getMultiCacheKey(pjp.getArgs(), cached);
        Set<String> hitKeys = new HashSet<>();
        if (null == keyPairs) {
            return pjp.proceed(pjp.getArgs());
        }

        Map<String, Object> cacheMap = commonBatchGet(keyPairs, hitKeys, returnType);
        Map<Object, Object> dbResult = Maps.newHashMap();
        if (hitKeys.size() < keyPairs.size()) {
            Collection<Object> missedKeys = getMissedKeys(pjp, cached, hitKeys);
            dbResult = (Map<Object, Object>)getMissedValues(pjp, cached, missedKeys);
            try {
                Map<String, Object> writeBackMap = getWriteBackMap(dbResult, missedKeys, cached);
                if (writeBackMap.size() > 0 && writeBackMap.size() <= cached.maxValueSize()) {
                    Map<String, String> tmpSetMap = batchParseJson2Str(writeBackMap);
                    redisCache.multiSet(tmpSetMap, cached.expireTime());
                }
            } catch (Exception e) {
                logger.error("getMapCache msetex failed.", e);
            }
        }

        return mergeMapResult(dbResult, cacheMap, keyPairs);
    }

    private Object getSingleCache(ProceedingJoinPoint pjp, Cached cached, Type valueType) throws Throwable {
        String cacheKey = KeyUtils.getSingleCacheKey(pjp.getArgs(), cached);
        Object value = null;

        try {
            Map<String, String> tmpStringMap = getMulti(Lists.newArrayList(cacheKey));
            Map<String, Object> valueMap = batchParseJson(tmpStringMap, valueType);
            value = valueMap.get(cacheKey);
        } catch (Exception e) {
            logger.error("getSingleCache read failed.", e);
        }

        if (value == null) {
            value = pjp.proceed(pjp.getArgs());
            if (null != value || cached.cacheNull()) {
                try {
                    Map<String, Object> tmpWriteBackMap = buildWriteBackMap(pjp.getArgs(), cached, value);
                    Map<String, String> writeBackMap = batchParseJson2Str(tmpWriteBackMap);
                    if (!writeBackMap.isEmpty()) {
                        redisCache.multiSet(writeBackMap, cached.expireTime());
                    }
                } catch (Exception e) {
                    logger.error("getSingleCache msetex failed.", e);
                }
            }
        }

        return value;
    }

    private Map<String, String> getMulti(List<String> oriKeys) {
        Map<String, String> res = Maps.newHashMap();
        if (CollectionUtils.isEmpty(oriKeys)) {
            return res;
        }

        try {
            String[] keys = oriKeys.toArray(new String[oriKeys.size()]);
            List<String> values = redisCache.getCacheList(keys);
            for (int i = 0, len = values.size(); i < len; i++) {
                res.put(keys[i], values.get(i));
            }
        } catch (Exception e) {
            logger.error("aspect getMulti error！", e);
        } finally {
            return res;
        }
    }

    private Map<String, String> batchParseJson2Str(Map<String, Object> tmpWriteBackMap) {
        Map<String, String> returnJsonMap = Maps.newHashMap();
        try {
            tmpWriteBackMap.forEach((k, v) -> returnJsonMap.put(k, JSONObject.toJSONString(v)));
        } catch (Exception e) {
            logger.error("aspect batchParseJson2Str error！", e);
        }

        return returnJsonMap;
    }

    /**
     * 生成只有个返回对象时候的会写进入redis的Map
     * 
     * @param args
     * @param cached
     * @param value
     * @return
     */
    private Map<String, Object> buildWriteBackMap(Object[] args, Cached cached, Object value) {
        Map<String, Object> objMap = Maps.newHashMap();
        String[] fieldNames = cached.keyNameInReturnObject();
        String key;
        if (fieldNames != null && fieldNames.length > 0) {
            try {
                key = KeyUtils.contactCacheKey(genCacheKey(value, fieldNames), cached.cacheKeyPrefix());
                if (!hasTooBig(value, cached)) {
                    objMap.put(key, value);
                }
            } catch (Exception e) {
                logger.error("get key from field [" + fieldNames + "] failed.", e);
            }
        } else {
            key = KeyUtils.getSingleCacheKey(args, cached);
            if (!hasTooBig(value, cached)) {
                objMap.put(key, value);
            }
        }

        return objMap;
    }

    /**
     * 获取未缓存的数据
     * 
     * @param pjp
     * @param cached
     * @return
     * @throws Throwable
     */
    private Object getMissedValues(ProceedingJoinPoint pjp, Cached cached, Collection<Object> missedKeys) throws Throwable {
        pjp.getArgs()[cached.keyParamIndex()] = missedKeys;
        return pjp.proceed(pjp.getArgs());
    }

    /**
     * 获取没有命中缓存的原始key
     * 
     * @param pjp
     * @param cached
     * @param hitKeys
     * @return
     * @throws Throwable
     */
    private Collection<Object> getMissedKeys(ProceedingJoinPoint pjp, Cached cached, Set<String> hitKeys) throws Throwable {
        Object oriKeys = pjp.getArgs()[cached.keyParamIndex()];
        Collection<Object> missedKeys;
        Class keyCollectionClass = oriKeys.getClass();
        if (List.class.isAssignableFrom(keyCollectionClass)) {
            missedKeys = new ArrayList<>();
        } else if (Set.class.isAssignableFrom(keyCollectionClass)) {
            missedKeys = new HashSet<>();
        } else {
            throw new Exception("unsupported key type. only support List, Set and single Object.");
        }

        for (Object oriKey : (Collection<Object>)oriKeys) {
            String cacheKey = KeyUtils.contactCacheKey(oriKey, cached.cacheKeyPrefix());
            if (!hitKeys.contains(cacheKey)) {
                missedKeys.add(oriKey);
            }
        }
        return missedKeys;
    }

    /**
     * 生成MAP的返回结果，如果某个值是null， 则这个不返回， 这个是兼容老逻辑
     * 
     * @param dbResult
     * @param cacheResult
     * @param keyPairs
     * @return
     */
    private Map<Object, Object> mergeMapResult(Map<Object, Object> dbResult, Map<String, Object> cacheResult, Map<String, Object> keyPairs) {
        if (cacheResult != null && !cacheResult.isEmpty()) {
            for (Map.Entry<String, Object> cacheEntry : cacheResult.entrySet()) {
                if (cacheEntry.getValue() != null) {
                    Object oriKey = keyPairs.get(cacheEntry.getKey());
                    dbResult.put(oriKey, cacheEntry.getValue());
                }
            }
        }
        return dbResult;
    }

    /**
     * 根据查询结果生成需要写入到缓存的Map, 当函数结果是一个Map， 或者Collection的时候使用
     * 
     * @param dbResultMap
     * @param missedKeys
     * @param cached
     * @return
     */
    private Map<String, Object> getWriteBackMap(Map<Object, Object> dbResultMap, Collection<Object> missedKeys, Cached cached) {
        Map<String, Object> writeBackMap = Maps.newHashMap();
        if (dbResultMap == null) {
            return writeBackMap;
        }

        try {
            for (Object missedKey : missedKeys) {
                Object value = dbResultMap.get(missedKey);
                if (null != value || cached.cacheNull()) {
                    String cacheKey = KeyUtils.contactCacheKey(missedKey, cached.cacheKeyPrefix());
                    if (!hasTooBig(value, cached)) {
                        writeBackMap.put(cacheKey, value);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("getWriteBackMap missedKeys [" + missedKeys + "] failed.", e);
        }

        return writeBackMap;
    }

    private Map<String, Object> commonBatchGet(Map<String, Object> keyPairs, Set<String> hitKeys, Type returnType) {
        try {
            Map<String, String> tmpMap = getMulti(Lists.newArrayList(keyPairs.keySet()));
            for (Map.Entry<String, String> oneEntry : tmpMap.entrySet()) {
                if (oneEntry.getValue() != null) {
                    hitKeys.add(oneEntry.getKey());
                }
            }
            return batchParseJson(tmpMap, returnType);
        } catch (Exception e) {
            logger.error("cache read failed.", e);
            return Maps.newHashMap();
        }
    }

    /**
     * 将list转换为map，用于数据的查询
     * 
     * @param objects
     * @param fieldNames
     * @return
     */
    private Map<Object, Object> convertCollectionToMap(Collection<Object> objects, String[] fieldNames) {
        Map<Object, Object> ret = Maps.newHashMap();
        try {
            for (Object value : objects) {
                if (value != null) {
                    Object key = genCacheKey(value, fieldNames);
                    if (ret.containsKey(key)) {
                        Collection<Object> exist = (Collection<Object>)ret.get(key);
                        exist.add(value);
                        ret.put(key, exist);
                    } else {
                        ret.put(key, Lists.newArrayList(value));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("get key from field [" + fieldNames + "] failed.", e);
        }
        return ret;
    }

    private Object genCacheKey(Object value, String[] fieldNames) throws Exception {
        BeanWrapper bw = new DirectFieldAccessFallbackBeanWrapper(value);
        if (fieldNames.length == 1) {
            return bw.getPropertyValue(fieldNames[0]);
        }

        return String.join("_", Arrays.stream(fieldNames)
            .map(bw::getPropertyValue).filter(Objects::nonNull).map(Object::toString)
            .toArray(String[]::new));
    }

    /**
     * 生成List的返回结果，如果某个值是null， 则这个不返回
     * 
     * @param dbResult
     * @param cacheResult
     * @return
     */
    private Collection<Object> mergeCollectionResult(Collection<Object> dbResult, Map<String, Object> cacheResult) {
        if (cacheResult != null && !cacheResult.isEmpty()) {
            for (Map.Entry<String, Object> cacheEntry : cacheResult.entrySet()) {
                if (cacheEntry.getValue() != null) {
                    dbResult.addAll((Collection<?>)cacheEntry.getValue());
                }
            }
        }

        return dbResult;
    }

    /**
     * 缓存结果太大不缓存
     *
     * @param value
     * @param cached
     * @return
     */
    private boolean hasTooBig(Object value, Cached cached) {
        if (value instanceof Collection) {
            Collection list = (Collection)value;
            if (list.size() > cached.maxValueSize()) {
                return true;
            }

            return false;
        }

        if (value instanceof Map) {
            Map map = (Map)value;
            if (map.size() > cached.maxValueSize()) {
                return true;
            }

            return false;
        }

        return false;
    }
}
