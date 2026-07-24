package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseAsyncTest;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskEventService;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;
import io.github.lunasaw.voglander.repository.mapper.ImageCollectionConfigMapper;

class BizTaskCreateConcurrencyTest extends BaseAsyncTest {

    @Autowired
    private BizTaskCreateService createService;
    @Autowired
    private BizTaskService taskService;
    @Autowired
    private BizTaskExecutionService executionService;
    @Autowired
    private BizTaskEventService eventService;
    @Autowired
    private ImageCollectionConfigMapper configMapper;

    private String ownerId;

    @AfterEach
    void tearDown() {
        if (ownerId == null) {
            return;
        }
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
    }

    @Test
    void sameCanonicalCommand_shouldCreateOneTaskAndOneFirstExecution() throws Exception {
        String suffix = suffix();
        ownerId = "owner-concurrency-same-" + suffix;
        TaskCreateCommand command = command(ownerId, "key-" + suffix, "same-name");

        List<Attempt> attempts = race(command, command);

        assertTrue(attempts.get(0).error == null);
        assertTrue(attempts.get(1).error == null);
        assertEquals(attempts.get(0).task.getTaskId(), attempts.get(1).task.getTaskId());
        BizTaskDO winner = oneTask(ownerId);
        assertEquals(1L, countTasks(ownerId));
        assertEquals(1L, executionService.count(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getTaskId, winner.getTaskId())));
    }

    @Test
    void differentCanonicalCommands_shouldProduceOneWinnerAndOneStableConflict() throws Exception {
        String suffix = suffix();
        ownerId = "owner-concurrency-conflict-" + suffix;
        String key = "key-" + suffix;

        List<Attempt> attempts = race(command(ownerId, key, "name-a"), command(ownerId, key, "name-b"));

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
        BizTaskDO winner = oneTask(ownerId);
        assertEquals(1L, countTasks(ownerId));
        assertEquals(1L, executionService.count(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getTaskId, winner.getTaskId())));
    }

    private List<Attempt> race(TaskCreateCommand first, TaskCreateCommand second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Attempt> left = executor.submit(() -> attempt(first, ready, start));
            Future<Attempt> right = executor.submit(() -> attempt(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<Attempt> result = new ArrayList<Attempt>();
            result.add(left.get(15, TimeUnit.SECONDS));
            result.add(right.get(15, TimeUnit.SECONDS));
            return result;
        } finally {
            executor.shutdownNow();
        }
    }

    private Attempt attempt(TaskCreateCommand command, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Attempt(null, new IllegalStateException("race start timed out"));
            }
            return new Attempt(createService.create(command), null);
        } catch (Throwable error) {
            return new Attempt(null, error);
        }
    }

    private long countTasks(String owner) {
        return taskService.count(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getOwnerType, "USER")
            .eq(BizTaskDO::getOwnerId, owner));
    }

    private BizTaskDO oneTask(String owner) {
        return taskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getOwnerType, "USER")
            .eq(BizTaskDO::getOwnerId, owner));
    }

    private static TaskCreateCommand command(String owner, String key, String taskName) {
        JSONObject payload = new JSONObject();
        payload.put("payloadVersion", ImageConstant.TASK_PAYLOAD_VERSION);
        payload.put("deviceId", "device-concurrency");
        payload.put("channelId", "channel-concurrency");
        payload.put("collectionMode", "ONCE");
        return new TaskCreateCommand(ImageConstant.TASK_TYPE_IMAGE_COLLECTION, taskName, null, "ONCE",
            null, null, null, payload, ImageConstant.TASK_PAYLOAD_VERSION,
            "IMAGE_COLLECTION:device-concurrency:channel-concurrency", "CAMERA", "device-concurrency",
            "USER", owner, null, key, 3);
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
