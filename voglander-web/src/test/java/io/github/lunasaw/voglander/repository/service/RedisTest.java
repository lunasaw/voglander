package io.github.lunasaw.voglander.repository.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.luna.common.thread.AsyncEngineUtils;

import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtils;
import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2024/1/17
 */
@Slf4j
@SpringBootTest(classes = ApplicationWeb.class)
public class RedisTest {

    AtomicInteger          atomicInteger = new AtomicInteger(0);
    int                    i             = 0;
    @Autowired
    private RedisLockUtils redisLockUtils;
    @Autowired
    private RedisLockUtil  redisLockUtil;

    @Test
    public void ctest() {
        boolean b = redisLockUtil.tryLock("lock", "lock");
        if (b) {
            System.out.println("获取锁成功");
        } else {
            System.out.println("获取锁失败");
            return;
        }
        try {
            i++;
            atomicInteger.incrementAndGet();
            System.out.println(b);
            Thread.sleep(20);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            redisLockUtil.unLock("lock");
        }
    }

    @Test
    public void atest() {
        boolean b = redisLockUtils.tryLock("lock", "lock", Duration.ofSeconds(4));
        if (b) {
            System.out.println("获取锁成功");
        } else {
            System.out.println("获取锁失败");
            return;
        }
        try {
            i++;
            atomicInteger.incrementAndGet();
            System.out.println(b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            redisLockUtils.releaseLock("lock", "lock");
        }
    }

    @Test
    public void btest() throws InterruptedException {

        for (int i = 0; i < 100; i++) {
            AsyncEngineUtils.execute(this::ctest);
        }
        Thread.sleep(10000);
        System.out.println(atomicInteger.get());
        System.out.println(i);
    }

}
