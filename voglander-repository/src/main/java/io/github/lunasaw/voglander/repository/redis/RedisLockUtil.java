package io.github.lunasaw.voglander.repository.redis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author luna
 * @Description
 * @Date 2021/1/26 11:06
 **/
@Component
public class RedisLockUtil {
    // 获取锁重试超时时间(秒)
    static Integer     GET_TIME_OUT  = 2;
    // 锁持有时间(秒)
    static Integer     LOCK_TIME_OUT = 30;
    // 锁前缀
    static String      LOCK_PREFIX   = "DCS_LOCK_";

    @Autowired
    private RedisCache redisCache;

    /**
     * 设置分布式锁
     *
     * @param key 锁标识
     * @param value 内容
     * @param lockTimeOut 锁有效时间（秒）
     * @return
     */
    public Boolean lock(String key, String value, Integer lockTimeOut) {
        // "SET" "${key}" "\"${value}\"" "EX" "${lockTimeOut}" "NX"
        // setIfAbsent 对应redis命令如上，通过set命令的参数NX，完成排斥锁和原子性
        return redisCache.setCacheObjectIfAbsent(LOCK_PREFIX + key, value, lockTimeOut, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取分布式锁
     *
     * @param key 锁标识
     * @param value 内容
     * @param lockTimeOut 锁有效时间（秒）
     * @param getTimeOut 获取锁重试超时时间
     * @return
     */
    public Boolean tryLock(String key, String value, Integer lockTimeOut, Integer getTimeOut) {
        Boolean lock;
        Boolean flag = true;
        long begin = System.currentTimeMillis();

        do {
            lock = this.lock(key, value, lockTimeOut);
            if (lock) {
                return true;
            }
            try {
                // 休眠0.1秒后重试，直到重试超时getTimeOut
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            long endTime = System.currentTimeMillis();
            if (endTime - begin > (getTimeOut * 1000)) {
                flag = false;
                // 退出循环
            }
        } while (flag);

        return false;
    }

    /**
     * 尝试获取锁
     *
     * @param key 锁标识
     * @return
     */
    public Boolean tryLock(String key) {
        return this.tryLock(key, key, LOCK_TIME_OUT, GET_TIME_OUT);
    }

    /**
     * 尝试获取锁
     *
     * @param key 锁标识
     * @param getTimeOut 获取锁重试超时时间
     * @return
     */
    public Boolean tryLock(String key, Integer getTimeOut) {
        return this.tryLock(key, key, LOCK_TIME_OUT, getTimeOut);
    }

    /**
     * 尝试获取锁
     *
     * @param key 锁标识
     * @param value 内容
     * @param getTimeOut 获取锁重试超时时间
     * @return
     */
    public Boolean tryLock(String key, String value, Integer getTimeOut) {
        return this.tryLock(key, value, LOCK_TIME_OUT, getTimeOut);
    }

    /**
     * 尝试获取锁
     *
     * @param key 锁标识
     * @param value 内容
     * @return
     */
    public Boolean tryLock(String key, String value) {
        return this.tryLock(key, value, LOCK_TIME_OUT, GET_TIME_OUT);
    }

    /**
     * 释放锁
     *
     * @param key 锁标识
     * @return
     */
    public Boolean unLock(String key) {
        return redisCache.deleteKey(LOCK_PREFIX + key);
    }

    /**
     * 释放锁
     *
     * @param keys 锁标识
     * @return
     */
    public Boolean unLocks(List<String> keys) {
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
