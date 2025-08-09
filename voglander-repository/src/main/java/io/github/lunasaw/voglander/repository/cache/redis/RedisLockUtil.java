package io.github.lunasaw.voglander.repository.cache.redis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Redis分布式锁工具类 - 优化版本
 * 
 * @Author luna
 * @Description 基于Redis的分布式锁实现，提供原子性保证和身份验证机制
 * @Date 2021/1/26 11:06
 **/
@Component
public class RedisLockUtil {
    private static final Logger log           = LoggerFactory.getLogger(RedisLockUtil.class);

    // 获取锁重试超时时间(秒)
    static Integer              GET_TIME_OUT  = 2;
    // 锁持有时间(秒)
    static Integer              LOCK_TIME_OUT = 30;
    // 锁前缀
    static String               LOCK_PREFIX   = "DCS_LOCK_";

    // Lua脚本：原子性解锁，只有锁的持有者才能解锁
    private static final String UNLOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";

    @Autowired
    private RedisCache redisCache;

    /**
     * 设置分布式锁（优化版本）
     *
     * @param key 锁标识
     * @param value 锁值（用于身份验证）
     * @param lockTimeOut 锁有效时间（秒）
     * @return 是否成功获取锁
     */
    public Boolean lock(String key, String value, Integer lockTimeOut) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("锁键不能为空");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("锁值不能为空");
        }
        if (lockTimeOut == null || lockTimeOut <= 0) {
            throw new IllegalArgumentException("锁超时时间必须大于0");
        }

        String lockKey = LOCK_PREFIX + key;
        Boolean result = redisCache.setCacheObjectIfAbsent(lockKey, value, lockTimeOut, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(result)) {
            log.debug("成功获取分布式锁: key={}, value={}, timeout={}s", lockKey, value, lockTimeOut);
        }

        return result;
    }

    /**
     * 尝试获取分布式锁（优化重试策略）
     *
     * @param key 锁标识
     * @param value 锁值
     * @param lockTimeOut 锁有效时间（秒）
     * @param getTimeOut 获取锁重试超时时间（秒）
     * @return 是否成功获取锁
     */
    public Boolean tryLock(String key, String value, Integer lockTimeOut, Integer getTimeOut) {
        if (getTimeOut == null || getTimeOut <= 0) {
            throw new IllegalArgumentException("获取锁超时时间必须大于0");
        }

        long startTime = System.currentTimeMillis();
        long timeoutMillis = getTimeOut * 1000L;

        // 指数退避重试策略
        int retryDelay = 50; // 初始重试间隔50ms
        int maxRetryDelay = 500; // 最大重试间隔500ms

        do {
            Boolean lockResult = lock(key, value, lockTimeOut);
            if (Boolean.TRUE.equals(lockResult)) {
                return true;
            }

            try {
                Thread.sleep(retryDelay);
                // 指数退避，但限制最大延迟
                retryDelay = Math.min(retryDelay * 2, maxRetryDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("获取锁被中断: key={}", key);
                return false;
            }

        } while ((System.currentTimeMillis() - startTime) < timeoutMillis);

        log.debug("获取锁超时: key={}, timeout={}s", key, getTimeOut);
        return false;
    }

    /**
     * 生成唯一的锁值
     * 
     * @return 唯一标识符
     */
    public String generateLockValue() {
        return Thread.currentThread().getId() + ":" + UUID.randomUUID().toString();
    }

    /**
     * 兼容性方法：尝试获取锁（自动生成唯一值）
     */
    public Boolean tryLock(String key) {
        String lockValue = generateLockValue();
        return tryLock(key, lockValue, LOCK_TIME_OUT, GET_TIME_OUT);
    }

    public Boolean tryLock(String key, Integer getTimeOut) {
        String lockValue = generateLockValue();
        return tryLock(key, lockValue, LOCK_TIME_OUT, getTimeOut);
    }

    public Boolean tryLock(String key, String value, Integer getTimeOut) {
        return tryLock(key, value, LOCK_TIME_OUT, getTimeOut);
    }

    public Boolean tryLock(String key, String value) {
        return tryLock(key, value, LOCK_TIME_OUT, GET_TIME_OUT);
    }

    /**
     * 安全释放锁（原子性，身份验证）
     *
     * @param key 锁标识
     * @param value 锁值（用于身份验证）
     * @return 是否成功释放锁
     */
    public Boolean unLock(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("锁键不能为空");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("锁值不能为空");
        }

        String lockKey = LOCK_PREFIX + key;

        try {
            Long result = redisCache.executeLuaScript(UNLOCK_SCRIPT, Arrays.asList(lockKey), value);

            boolean unlocked = result != null && result > 0;
            if (unlocked) {
                log.debug("成功释放分布式锁: key={}, value={}", lockKey, value);
            } else {
                log.warn("释放锁失败，可能锁已过期或值不匹配: key={}, value={}", lockKey, value);
            }

            return unlocked;
        } catch (Exception e) {
            log.error("释放锁异常: key={}, value={}", lockKey, value, e);
            return false;
        }
    }

    /**
     * 兼容性方法：释放锁（不推荐使用）
     * 
     * @deprecated 建议使用 unLock(key, value) 方法提供身份验证
     */
    @Deprecated
    public Boolean unLock(String key) {
        log.warn("使用了不安全的锁释放方法，建议升级到带身份验证的版本");
        return redisCache.deleteKey(LOCK_PREFIX + key);
    }

    /**
     * 批量释放锁（不推荐使用，保留用于兼容性）
     * 
     * @deprecated 批量操作可能导致不一致状态
     */
    @Deprecated
    public Boolean unLocks(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return true;
        }

        // 删除的锁，去重
        Set<String> deleteKeys = new HashSet<>();
        for (String key : keys) {
            String delKey = LOCK_PREFIX + key;
            deleteKeys.add(delKey);
        }

        Long delete = redisCache.deleteKey(deleteKeys);
        if (deleteKeys.size() > delete) {
            // 多个一起删除存在失败，重试单个删除
            for (String key : deleteKeys) {
                redisCache.deleteKey(key);
            }
        }
        return true;
    }
}
