package io.github.lunasaw.voglander.service.task;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;

/** Coordinates due-task scans, per-task schedule locks and transactional task re-reads. */
final class BizTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BizTaskScheduler.class);

    private final BizTaskManager bizTaskManager;
    private final RedisLockUtil redisLockUtil;
    private final TransactionOperations transactionOperations;
    private final DueTaskMaterializer dueTaskMaterializer;
    private final Clock clock;
    private final int scanBatchSize;
    private final int lockTimeoutSeconds;

    BizTaskScheduler(BizTaskManager bizTaskManager, RedisLockUtil redisLockUtil,
        TransactionOperations transactionOperations, DueTaskMaterializer dueTaskMaterializer, Clock clock,
        int scanBatchSize, int lockTimeoutSeconds) {
        this.bizTaskManager = Objects.requireNonNull(bizTaskManager, "bizTaskManager");
        this.redisLockUtil = Objects.requireNonNull(redisLockUtil, "redisLockUtil");
        this.transactionOperations = Objects.requireNonNull(transactionOperations, "transactionOperations");
        this.dueTaskMaterializer = Objects.requireNonNull(dueTaskMaterializer, "dueTaskMaterializer");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (scanBatchSize <= 0 || scanBatchSize > 1000 || lockTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Task scheduler batch size or lock timeout is invalid");
        }
        this.scanBatchSize = scanBatchSize;
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    void scanDueTasks() {
        LocalDateTime scanTime = LocalDateTime.now(clock);
        List<BizTaskDTO> dueTasks = bizTaskManager.findDueTasks(scanTime, scanBatchSize);
        for (BizTaskDTO dueTask : dueTasks) {
            scheduleOne(dueTask, scanTime);
        }
    }

    private void scheduleOne(BizTaskDTO dueTask, LocalDateTime scanTime) {
        if (dueTask == null || dueTask.getTaskId() == null) {
            return;
        }
        String lockKey = TaskConstant.SCHEDULE_LOCK_PREFIX + dueTask.getTaskId();
        String lockValue = redisLockUtil.generateLockValue();
        if (!Boolean.TRUE.equals(redisLockUtil.lock(lockKey, lockValue, lockTimeoutSeconds))) {
            return;
        }
        try {
            transactionOperations.executeWithoutResult(status -> {
                BizTaskDTO current = bizTaskManager.getForScheduling(dueTask.getTaskId());
                if (isStillDue(current, scanTime)) {
                    dueTaskMaterializer.materialize(current);
                }
            });
        } catch (RuntimeException exception) {
            LOGGER.error("Business-task schedule materialization failed: taskId={}", dueTask.getTaskId(),
                exception);
        } finally {
            redisLockUtil.unLock(lockKey, lockValue);
        }
    }

    private boolean isStillDue(BizTaskDTO task, LocalDateTime scanTime) {
        if (task == null || task.getNextPlanTime() == null || task.getNextPlanTime().isAfter(scanTime)) {
            return false;
        }
        return "SCHEDULED".equals(task.getState()) || "RUNNING".equals(task.getState());
    }
}
