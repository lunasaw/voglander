package io.github.lunasaw.voglander.repository.redis;

import java.time.Duration;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * redis分布式锁服务类
 * 采用LUA脚本实现，保证加锁、解锁操作原子性
 * 
 * @author luna
 */
@Component
public class RedisLockUtils {

    /**
     * 分布式锁过期时间，单位秒
     */
    private static final Long   DEFAULT_LOCK_EXPIRE_TIME = 60L;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试在指定时间内加锁
     * 
     * @param key
     * @param value
     * @param timeout 锁等待时间
     * @return
     */
    public boolean tryLock(String key, String value, Duration timeout) {
        long waitMills = timeout.toMillis();
        long currentTimeMillis = System.currentTimeMillis();
        do {
            boolean lock = lock(key, value, DEFAULT_LOCK_EXPIRE_TIME);
            if (lock) {
                return true;
            }
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        } while (System.currentTimeMillis() < currentTimeMillis + waitMills);
        return false;
    }

    /**
     * 直接加锁
     * 
     * @param key
     * @param value
     * @param expire
     * @return
     */
    public boolean lock(String key, String value, Long expire) {
        String luaScript = "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then return redis.call('expire', KEYS[1], ARGV[2]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key), value, String.valueOf(expire));
        return result != null && result.equals(1L);
    }

    /**
     * 释放锁
     * 
     * @param key
     * @param value
     * @return
     */
    public boolean releaseLock(String key, String value) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key), value);
        return result != null && result.equals(1L);
    }
}
