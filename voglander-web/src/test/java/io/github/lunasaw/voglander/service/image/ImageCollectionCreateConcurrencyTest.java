package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseAsyncTest;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskEventService;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;
import io.github.lunasaw.voglander.repository.mapper.ImageCollectionConfigMapper;

class ImageCollectionCreateConcurrencyTest extends BaseAsyncTest {

    @Autowired
    private ImageCollectionApplicationService applicationService;
    @Autowired
    private BizTaskService taskService;
    @Autowired
    private BizTaskExecutionService executionService;
    @Autowired
    private BizTaskEventService eventService;
    @Autowired
    private ImageCollectionConfigMapper configMapper;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceChannelService channelService;

    private String ownerId;
    private String deviceId;
    private String channelId;

    @BeforeEach
    void setUpCamera() {
        String suffix = suffix();
        ownerId = "collection-concurrency-owner-" + suffix;
        deviceId = "collection-concurrency-device-" + suffix;
        channelId = "collection-concurrency-channel-" + suffix;
        LocalDateTime now = LocalDateTime.now();

        DeviceDO device = new DeviceDO();
        device.setCreateTime(now);
        device.setUpdateTime(now);
        device.setDeviceId(deviceId);
        device.setType(1);
        device.setStatus(1);
        device.setName("Concurrent camera");
        device.setIp("127.0.0.1");
        device.setPort(5060);
        device.setRegisterTime(now);
        device.setKeepaliveTime(now);
        device.setServerIp("127.0.0.1");
        deviceService.save(device);

        DeviceChannelDO channel = new DeviceChannelDO();
        channel.setCreateTime(now);
        channel.setUpdateTime(now);
        channel.setStatus(1);
        channel.setChannelId(channelId);
        channel.setDeviceId(deviceId);
        channel.setName("Concurrent channel");
        channel.setMissingCount(0);
        channelService.save(channel);
    }

    @AfterEach
    void tearDown() {
        List<BizTaskDO> tasks = taskService.list(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getOwnerType, "USER")
            .eq(BizTaskDO::getOwnerId, ownerId));
        List<String> taskIds = new ArrayList<String>();
        for (BizTaskDO task : tasks) {
            taskIds.add(task.getTaskId());
        }
        if (!taskIds.isEmpty()) {
            configMapper.delete(new LambdaQueryWrapper<ImageCollectionConfigDO>()
                .in(ImageCollectionConfigDO::getTaskId, taskIds));
            eventService.remove(new LambdaQueryWrapper<BizTaskEventDO>()
                .in(BizTaskEventDO::getTaskId, taskIds));
            executionService.remove(new LambdaQueryWrapper<BizTaskExecutionDO>()
                .in(BizTaskExecutionDO::getTaskId, taskIds));
            taskService.remove(new LambdaQueryWrapper<BizTaskDO>().in(BizTaskDO::getTaskId, taskIds));
        }
        channelService.remove(new LambdaQueryWrapper<DeviceChannelDO>()
            .eq(DeviceChannelDO::getDeviceId, deviceId));
        deviceService.remove(new LambdaQueryWrapper<DeviceDO>().eq(DeviceDO::getDeviceId, deviceId));
    }

    @Test
    void sameCanonicalCommandCreatesOneTaskExecutionAndConfig() throws Exception {
        String key = "collection-key-" + suffix();
        ImageCollectionCreateCommand command = command(key, "same-name");

        List<Attempt> attempts = race(command, command);

        assertTrue(attempts.get(0).error == null, String.valueOf(attempts.get(0).error));
        assertTrue(attempts.get(1).error == null, String.valueOf(attempts.get(1).error));
        assertEquals(attempts.get(0).task.getTaskId(), attempts.get(1).task.getTaskId());
        assertSingleDurableFacts();
    }

    @Test
    void differentCanonicalCommandsCreateOneWinnerAndOneStableConflict() throws Exception {
        String key = "collection-key-" + suffix();

        List<Attempt> attempts = race(command(key, "name-a"), command(key, "name-b"));

        int successes = 0;
        int conflicts = 0;
        for (Attempt attempt : attempts) {
            if (attempt.task != null) {
                successes++;
            } else if (attempt.error instanceof ServiceException
                && ((ServiceException) attempt.error).getCode()
                    == ServiceExceptionEnum.IDEMPOTENCY_KEY_REUSED.getCode()) {
                conflicts++;
            }
        }
        assertEquals(1, successes);
        assertEquals(1, conflicts);
        assertSingleDurableFacts();
    }

    private void assertSingleDurableFacts() {
        List<BizTaskDO> tasks = taskService.list(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getOwnerType, "USER")
            .eq(BizTaskDO::getOwnerId, ownerId));
        assertEquals(1, tasks.size());
        String taskId = tasks.get(0).getTaskId();
        assertEquals(1L, executionService.count(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getTaskId, taskId)));
        assertEquals(1L, configMapper.selectCount(new LambdaQueryWrapper<ImageCollectionConfigDO>()
            .eq(ImageCollectionConfigDO::getTaskId, taskId)));
    }

    private List<Attempt> race(ImageCollectionCreateCommand first, ImageCollectionCreateCommand second)
        throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Attempt> left = executor.submit(() -> attempt(first, ready, start));
            Future<Attempt> right = executor.submit(() -> attempt(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<Attempt> attempts = new ArrayList<Attempt>();
            attempts.add(left.get(15, TimeUnit.SECONDS));
            attempts.add(right.get(15, TimeUnit.SECONDS));
            return attempts;
        } finally {
            executor.shutdownNow();
        }
    }

    private Attempt attempt(ImageCollectionCreateCommand command, CountDownLatch ready,
        CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Attempt(null, new IllegalStateException("race start timed out"));
            }
            return new Attempt(applicationService.create(command), null);
        } catch (Throwable error) {
            return new Attempt(null, error);
        }
    }

    private ImageCollectionCreateCommand command(String key, String taskName) {
        return new ImageCollectionCreateCommand(taskName, "ONCE", deviceId, channelId,
            null, null, null, "PERMANENT", "USER", ownerId, null, key);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static final class Attempt {
        private final BizTaskDTO task;
        private final Throwable error;

        private Attempt(BizTaskDTO task, Throwable error) {
            this.task = task;
            this.error = error;
        }
    }
}
