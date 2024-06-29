package io.github.lunasaw.voglander.repository.service;

import com.luna.common.thread.AsyncEngineUtils;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.manager.ConcurrentProcessHelper;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtils;
import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author luna
 * @date 2024/1/17
 */
@Slf4j
@SpringBootTest(classes = ApplicationWeb.class)
public class RedisTest {

    @Autowired
    private RedisLockUtils redisLockUtils;

    @Autowired
    private RedisLockUtil  redisLockUtil;
    AtomicInteger          atomicInteger = new AtomicInteger(0);

    int                    i             = 0;

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

    @Autowired
    private DeviceManager deviceManager;

    @Test
    public void dtest() {

        // 方法内部嵌套调用不会走缓存，本质上是spring的代理机制导致的
        DeviceDTO dtoByDeviceId = deviceManager.getDtoByDeviceId("33010602010000000001");
        System.out.println(dtoByDeviceId);

        DeviceDTO deviceDTO = deviceManager.getDtoByDeviceId("33010602010000000001");
        System.out.println(deviceDTO);

        DeviceDO byDeviceId = deviceManager.getByDeviceId("33010602010000000001");
        System.out.println(byDeviceId);

        DeviceDO byDeviceId1 = deviceManager.getByDeviceId("33010602010000000001");
        System.out.println(byDeviceId1);
    }

    @Autowired
    private ConcurrentProcessHelper concurrentProcessHelper;

    @Test
    public void etest() {

        TransactionCallback<Boolean> cust = new TransactionCallback<>() {

            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                try {
                    DeviceDTO dtoByDeviceId = deviceManager.getDtoByDeviceId("33010602010000000001");
                    dtoByDeviceId.setName("cust2");
                    deviceManager.saveOrUpdate(dtoByDeviceId);
                    int i = 1 / 0;
                    return true;
                } catch (Exception e) {
                    log.error("doInTransaction::status = {} ", status, e);
                    status.setRollbackOnly();
                    return false;
                }
            }
        };

        concurrentProcessHelper.process("save", cust);
    }
}
