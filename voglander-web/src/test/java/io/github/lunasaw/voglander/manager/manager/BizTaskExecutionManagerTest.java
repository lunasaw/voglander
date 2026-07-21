package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;
import io.github.lunasaw.voglander.manager.assembler.BizTaskExecutionAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskProgressDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;

@DisplayName("BizTaskExecutionManager durable execution operations")
class BizTaskExecutionManagerTest extends BaseTest {

    @Autowired
    private BizTaskExecutionManager bizTaskExecutionManager;

    @Autowired
    private BizTaskExecutionService bizTaskExecutionService;

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskExecutionAssembler bizTaskExecutionAssembler;

    @Autowired
    private BizTaskAssembler bizTaskAssembler;

    @Test
    @DisplayName("insertIfAbsent 应按 executionId 与计划点保持幂等")
    void insertIfAbsent_shouldReturnWhetherExecutionWasMaterialized() {
        String suffix = suffix();
        BizTaskExecutionDTO execution = execution("bexec_insert_" + suffix, "btask_insert_" + suffix,
            LocalDateTime.now().withNano(0), "PENDING");

        assertTrue(bizTaskExecutionManager.insertIfAbsent(execution));
        assertFalse(bizTaskExecutionManager.insertIfAbsent(execution));

        BizTaskExecutionDTO duplicatePlan = execution("bexec_duplicate_" + suffix, execution.getTaskId(),
            execution.getPlannedAt(), "PENDING");
        assertFalse(bizTaskExecutionManager.insertIfAbsent(duplicatePlan));
        assertEquals(1L, bizTaskExecutionService.count(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getTaskId, execution.getTaskId())));
    }

    @Test
    @DisplayName("Manager 扫描应返回到期 runnable 与过期 lease 的 DTO")
    void scans_shouldExposeDurableCandidatesWithoutLeakingDo() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskExecutionDTO pending = execution("bexec_scan_pending_" + suffix, "btask_scan_" + suffix,
            now.minusSeconds(2), "PENDING");
        BizTaskExecutionDTO futureRetry = execution("bexec_scan_retry_" + suffix, "btask_scan_" + suffix,
            now.minusSeconds(1), "RETRY_WAIT");
        futureRetry.setScheduleVersion(2);
        futureRetry.setNextAttemptTime(now.plusMinutes(1));
        BizTaskExecutionDTO expired = running("bexec_scan_expired_" + suffix, now.minusSeconds(1),
            "claim-expired");
        BizTaskExecutionDTO active = running("bexec_scan_active_" + suffix, now.plusMinutes(1), "claim-active");
        assertTrue(bizTaskExecutionManager.insertIfAbsent(pending));
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(futureRetry));
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(expired));
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(active));

        List<BizTaskExecutionDTO> runnable = bizTaskExecutionManager.findRunnable(now, 100);
        List<BizTaskExecutionDTO> expiredLeases = bizTaskExecutionManager.findExpiredLeases(now, 100);

        assertTrue(runnable.stream().anyMatch(row -> pending.getExecutionId().equals(row.getExecutionId())));
        assertFalse(runnable.stream().anyMatch(row -> futureRetry.getExecutionId().equals(row.getExecutionId())));
        assertTrue(expiredLeases.stream().anyMatch(row -> expired.getExecutionId().equals(row.getExecutionId())));
        assertFalse(expiredLeases.stream().anyMatch(row -> active.getExecutionId().equals(row.getExecutionId())));
    }

    @Test
    @DisplayName("执行分页和详情应使用 task access scope、脱敏结果与确定排序")
    void queries_shouldApplyTaskScopeAndSafeDeterministicMapping() {
        String suffix = suffix();
        LocalDateTime sameTime = LocalDateTime.of(2026, 7, 14, 13, 0);
        String visibleTaskId = "btask_visible_" + suffix;
        String hiddenTaskId = "btask_hidden_" + suffix;
        String visibleOwnerId = "OWNER_A_" + suffix;
        saveTask(task(visibleTaskId, visibleOwnerId));
        saveTask(task(hiddenTaskId, "OWNER_B_" + suffix));

        BizTaskExecutionDTO first = execution("bexec_a_" + suffix, visibleTaskId, sameTime, "RUNNING");
        first.setClaimToken("claim-secret-a");
        first.setResultSummary("{\"assetId\":\"img-1\",\"secret\":\"hidden\"}");
        BizTaskExecutionDTO second = execution("bexec_b_" + suffix, visibleTaskId, sameTime, "SUCCEEDED");
        second.setScheduleVersion(2);
        second.setResultSummary("{\"assetId\":\"img-2\"}");
        BizTaskExecutionDTO hidden = execution("bexec_hidden_" + suffix, hiddenTaskId, sameTime.plusMinutes(1),
            "FAILED");
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(first));
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(second));
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(hidden));

        Page<BizTaskExecutionDTO> page = bizTaskExecutionManager.getPage(new BizTaskExecutionQueryDTO(),
            BizTaskAccessScopeDTO.owner("USER", visibleOwnerId), 1, 10);

        assertEquals(2L, page.getTotal());
        assertEquals(Arrays.asList(second.getExecutionId(), first.getExecutionId()),
            page.getRecords().stream().map(BizTaskExecutionDTO::getExecutionId).toList());
        assertNull(page.getRecords().get(1).getClaimToken());
        assertFalse(page.getRecords().get(1).getResultSummary().toLowerCase().contains("secret"));
        assertNotNull(bizTaskExecutionManager.getByExecutionId(first.getExecutionId(),
            BizTaskAccessScopeDTO.owner("USER", visibleOwnerId)));
        assertNull(bizTaskExecutionManager.getByExecutionId(hidden.getExecutionId(),
            BizTaskAccessScopeDTO.owner("USER", visibleOwnerId)));
    }

    @Test
    @DisplayName("claim 和 heartbeat 应只接受当前 version 与 claim token")
    void claimAndHeartbeat_shouldRejectStaleWorkers() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskExecutionDTO execution = execution("bexec_claim_" + suffix, "btask_claim_" + suffix, now,
            "PENDING");
        assertTrue(bizTaskExecutionManager.insertIfAbsent(execution));

        BizTaskExecutionDTO claim = command(execution.getExecutionId(), 0, "claim-a", now);
        claim.setWorkerNode("node-a");
        claim.setLeaseUntil(now.plusMinutes(1));
        BizTaskExecutionDTO claimed = bizTaskExecutionManager.claim(claim);
        assertNotNull(claimed);
        assertEquals("RUNNING", claimed.getState());
        assertEquals(1, claimed.getAttemptCount());
        assertEquals("claim-a", claimed.getClaimToken());

        BizTaskExecutionDTO competing = command(execution.getExecutionId(), 0, "claim-b", now);
        competing.setWorkerNode("node-b");
        competing.setLeaseUntil(now.plusMinutes(1));
        assertNull(bizTaskExecutionManager.claim(competing));

        BizTaskExecutionDTO staleHeartbeat = command(execution.getExecutionId(), claimed.getVersion(), "stale",
            now.plusSeconds(1));
        staleHeartbeat.setLeaseUntil(now.plusMinutes(2));
        assertFalse(bizTaskExecutionManager.heartbeat(staleHeartbeat));

        BizTaskExecutionDTO heartbeat = command(execution.getExecutionId(), claimed.getVersion(), "claim-a",
            now.plusSeconds(1));
        heartbeat.setLeaseUntil(now.plusMinutes(2));
        assertTrue(bizTaskExecutionManager.heartbeat(heartbeat));
    }

    @Test
    @DisplayName("ONCE/AT_TIME 执行进度应原子镜像到任务级进度")
    void updateProgress_shouldMirrorSingleExecutionTaskModes() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        for (String taskMode : Arrays.asList("ONCE", "AT_TIME")) {
            String taskId = "btask_progress_" + taskMode.toLowerCase() + "_" + suffix;
            String executionId = "bexec_progress_" + taskMode.toLowerCase() + "_" + suffix;
            BizTaskDTO task = task(taskId, "OWNER_PROGRESS_" + suffix);
            task.setTaskMode(taskMode);
            saveTask(task);
            saveClaimedExecution(executionId, taskId, now, "claim-" + taskMode.toLowerCase());

            BizTaskProgressDTO progress = progress(taskId, executionId,
                "claim-" + taskMode.toLowerCase(), 4, 10, 1, "processing", now.plusSeconds(1));

            assertTrue(bizTaskExecutionManager.updateProgress(progress));

            BizTaskExecutionDO storedExecution = find(executionId);
            BizTaskDO storedTask = findTask(taskId);
            assertEquals(4L, storedExecution.getProgressCurrent());
            assertEquals(10L, storedExecution.getProgressTotal());
            assertEquals(1L, storedExecution.getProgressRevision());
            assertEquals(4L, storedTask.getProgressCurrent());
            assertEquals(10L, storedTask.getProgressTotal());
            assertEquals("processing", storedTask.getProgressMessage());
            assertEquals(1L, storedTask.getProgressRevision());
        }
    }

    @Test
    @DisplayName("FIXED_RATE 任务保持终态计数聚合，活动执行单独保存细粒度进度")
    void updateProgress_shouldKeepFixedRateAggregateAndActiveExecutionProgressSeparate() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String taskId = "btask_progress_fixed_" + suffix;
        String executionId = "bexec_progress_fixed_" + suffix;
        BizTaskDTO task = task(taskId, "OWNER_PROGRESS_" + suffix);
        task.setTaskMode("FIXED_RATE");
        task.setPlannedCount(5);
        task.setSuccessCount(1);
        task.setFailedCount(1);
        task.setMissedCount(1);
        task.setProgressCurrent(3L);
        task.setProgressTotal(5L);
        saveTask(task);
        saveClaimedExecution(executionId, taskId, now, "claim-fixed");

        assertTrue(bizTaskExecutionManager.updateProgress(progress(taskId, executionId, "claim-fixed",
            4, 10, 1, "active execution", now.plusSeconds(1))));

        BizTaskExecutionDO storedExecution = find(executionId);
        BizTaskDO storedTask = findTask(taskId);
        assertEquals(4L, storedExecution.getProgressCurrent());
        assertEquals(10L, storedExecution.getProgressTotal());
        assertEquals("active execution", storedExecution.getProgressMessage());
        assertEquals(3L, storedTask.getProgressCurrent());
        assertEquals(5L, storedTask.getProgressTotal());
        assertEquals(0L, storedTask.getProgressRevision());
    }

    @Test
    @DisplayName("进度 revision 倒退应被拒绝且不覆盖已持久化进度")
    void updateProgress_shouldRejectStaleRevision() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String taskId = "btask_progress_stale_" + suffix;
        String executionId = "bexec_progress_stale_" + suffix;
        saveTask(task(taskId, "OWNER_PROGRESS_" + suffix));
        saveClaimedExecution(executionId, taskId, now, "claim-stale-progress");

        assertTrue(bizTaskExecutionManager.updateProgress(progress(taskId, executionId, "claim-stale-progress",
            6, 10, 2, "newer progress", now.plusSeconds(1))));
        assertFalse(bizTaskExecutionManager.updateProgress(progress(taskId, executionId, "claim-stale-progress",
            4, 10, 1, "stale progress", now.plusSeconds(2))));

        BizTaskExecutionDO storedExecution = find(executionId);
        BizTaskDO storedTask = findTask(taskId);
        assertEquals(6L, storedExecution.getProgressCurrent());
        assertEquals(2L, storedExecution.getProgressRevision());
        assertEquals("newer progress", storedExecution.getProgressMessage());
        assertEquals(6L, storedTask.getProgressCurrent());
        assertEquals(2L, storedTask.getProgressRevision());
    }

    @Test
    @DisplayName("已量化进度的总量不得缩小或退回不确定状态")
    void updateProgress_shouldRejectShrinkingOrClearingExistingTotal() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String taskId = "btask_progress_total_" + suffix;
        String executionId = "bexec_progress_total_" + suffix;
        saveTask(task(taskId, "OWNER_PROGRESS_" + suffix));
        saveClaimedExecution(executionId, taskId, now, "claim-total-progress");

        assertTrue(bizTaskExecutionManager.updateProgress(progress(taskId, executionId, "claim-total-progress",
            5, 10, 1, "quantified", now.plusSeconds(1))));
        assertFalse(bizTaskExecutionManager.updateProgress(progress(taskId, executionId, "claim-total-progress",
            6, 9, 2, "shrunk", now.plusSeconds(2))));
        assertFalse(bizTaskExecutionManager.updateProgress(progress(taskId, executionId, "claim-total-progress",
            6, 0, 2, "cleared", now.plusSeconds(2))));

        BizTaskExecutionDO storedExecution = find(executionId);
        BizTaskDO storedTask = findTask(taskId);
        assertEquals(5L, storedExecution.getProgressCurrent());
        assertEquals(10L, storedExecution.getProgressTotal());
        assertEquals(1L, storedExecution.getProgressRevision());
        assertEquals(5L, storedTask.getProgressCurrent());
        assertEquals(10L, storedTask.getProgressTotal());
        assertEquals(1L, storedTask.getProgressRevision());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("相同 revision 的并发进度写入只能有一个成功")
    void updateProgress_shouldAllowExactlyOneConcurrentSameRevisionWrite() throws Exception {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String taskId = "btask_progress_concurrent_" + suffix;
        String executionId = "bexec_progress_concurrent_" + suffix;
        String claimToken = "claim-concurrent-progress";
        saveTask(task(taskId, "OWNER_PROGRESS_" + suffix));
        saveClaimedExecution(executionId, taskId, now, claimToken);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> updateProgressAfterStart(ready, start,
                progress(taskId, executionId, claimToken, 5, 10, 1, "concurrent", now.plusSeconds(1))));
            Future<Boolean> second = executor.submit(() -> updateProgressAfterStart(ready, start,
                progress(taskId, executionId, claimToken, 5, 10, 1, "concurrent", now.plusSeconds(1))));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int successCount = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successCount);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        BizTaskExecutionDO storedExecution = find(executionId);
        BizTaskDO storedTask = findTask(taskId);
        assertEquals(5L, storedExecution.getProgressCurrent());
        assertEquals(10L, storedExecution.getProgressTotal());
        assertEquals(1L, storedExecution.getProgressRevision());
        assertEquals(5L, storedTask.getProgressCurrent());
        assertEquals(10L, storedTask.getProgressTotal());
        assertEquals(1L, storedTask.getProgressRevision());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("任务级镜像冲突应回滚已更新的执行级进度")
    void updateProgress_shouldRollbackExecutionWhenTaskMirrorConflicts() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String taskId = "btask_progress_rollback_" + suffix;
        String executionId = "bexec_progress_rollback_" + suffix;
        saveTask(task(taskId, "OWNER_PROGRESS_" + suffix));
        BizTaskDO taskWithNewerProgress = findTask(taskId);
        taskWithNewerProgress.setProgressCurrent(7L);
        taskWithNewerProgress.setProgressTotal(10L);
        taskWithNewerProgress.setProgressMessage("already mirrored");
        taskWithNewerProgress.setProgressRevision(1L);
        assertTrue(bizTaskService.updateById(taskWithNewerProgress));
        BizTaskDO beforeProgressWrite = findTask(taskId);
        saveClaimedExecution(executionId, taskId, now, "claim-rollback-progress");

        ServiceException error = assertThrows(ServiceException.class,
            () -> bizTaskExecutionManager.updateProgress(progress(taskId, executionId, "claim-rollback-progress",
                5, 10, 1, "should roll back", now.plusSeconds(1))));

        assertEquals(ServiceExceptionEnum.TASK_STATE_CONFLICT.getCode(), error.getCode());
        BizTaskExecutionDO storedExecution = find(executionId);
        BizTaskDO storedTask = findTask(taskId);
        assertEquals(0L, storedExecution.getProgressCurrent());
        assertEquals(0L, storedExecution.getProgressTotal());
        assertEquals(0L, storedExecution.getProgressRevision());
        assertNull(storedExecution.getProgressMessage());
        assertEquals(7L, storedTask.getProgressCurrent());
        assertEquals(10L, storedTask.getProgressTotal());
        assertEquals(1L, storedTask.getProgressRevision());
        assertEquals(beforeProgressWrite.getVersion(), storedTask.getVersion());
    }

    @Test
    @DisplayName("retry-wait 应绑定当前 claim 并清理租约所有权")
    void markRetryWait_shouldRejectStaleClaimAndReleaseLease() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskExecutionDTO execution = execution("bexec_retry_" + suffix, "btask_retry_" + suffix, now,
            "PENDING");
        assertTrue(bizTaskExecutionManager.insertIfAbsent(execution));
        BizTaskExecutionDTO claim = command(execution.getExecutionId(), 0, "claim-current", now);
        claim.setWorkerNode("node-a");
        claim.setLeaseUntil(now.plusMinutes(1));
        assertNotNull(bizTaskExecutionManager.claim(claim));

        BizTaskExecutionDTO stale = command(execution.getExecutionId(), null, "claim-stale", now.plusSeconds(1));
        stale.setNextAttemptTime(now.plusSeconds(10));
        stale.setFailureCode("TEMPORARY");
        stale.setFailureMessage("temporary failure");
        assertFalse(bizTaskExecutionManager.markRetryWait(stale));

        BizTaskExecutionDTO retry = command(execution.getExecutionId(), null, "claim-current", now.plusSeconds(1));
        retry.setNextAttemptTime(now.plusSeconds(10));
        retry.setFailureCode("TEMPORARY");
        retry.setFailureMessage("temporary failure");
        assertTrue(bizTaskExecutionManager.markRetryWait(retry));

        BizTaskExecutionDO stored = find(execution.getExecutionId());
        assertEquals("RETRY_WAIT", stored.getState());
        assertNull(stored.getClaimToken());
        assertNull(stored.getWorkerNode());
        assertNull(stored.getLeaseUntil());
    }

    @Test
    @DisplayName("missed 应只允许当前 PENDING version 一次性终止")
    void markMissed_shouldUsePendingVersionCas() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskExecutionDTO execution = execution("bexec_missed_" + suffix, "btask_missed_" + suffix, now,
            "PENDING");
        assertTrue(bizTaskExecutionManager.insertIfAbsent(execution));

        BizTaskExecutionDTO missed = command(execution.getExecutionId(), 0, null, now.plusSeconds(1));
        missed.setFinishedAt(now.plusSeconds(1));
        missed.setFailureCode("ALLOWED_DELAY_EXCEEDED");
        missed.setFailureMessage("planned point expired");
        assertTrue(bizTaskExecutionManager.markMissed(missed));
        assertFalse(bizTaskExecutionManager.markMissed(missed));

        BizTaskExecutionDO stored = find(execution.getExecutionId());
        assertEquals("MISSED", stored.getState());
        assertEquals(now.plusSeconds(1), stored.getFinishedAt());
    }

    @Test
    @DisplayName("租约恢复应要求已过期、当前 version/token，并支持 retry-wait 或 failed")
    void recoverExpiredLease_shouldRejectHeartbeatRaceAndTerminalizeWhenRequested() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskExecutionDTO retryable = running("bexec_recover_retry_" + suffix, now.minusSeconds(1),
            "claim-retry");
        BizTaskExecutionDTO active = running("bexec_recover_active_" + suffix, now.plusMinutes(1),
            "claim-active");
        BizTaskExecutionDTO failed = running("bexec_recover_failed_" + suffix, now.minusSeconds(1),
            "claim-failed");
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(retryable));
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(active));
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(failed));

        BizTaskExecutionDTO activeRecovery = recovery(active, "RETRY_WAIT", now);
        activeRecovery.setNextAttemptTime(now.plusSeconds(5));
        assertFalse(bizTaskExecutionManager.recoverExpiredLease(activeRecovery));

        BizTaskExecutionDTO retryRecovery = recovery(retryable, "RETRY_WAIT", now);
        retryRecovery.setNextAttemptTime(now.plusSeconds(5));
        assertTrue(bizTaskExecutionManager.recoverExpiredLease(retryRecovery));
        assertEquals("RETRY_WAIT", find(retryable.getExecutionId()).getState());

        BizTaskExecutionDTO staleRecovery = recovery(failed, "FAILED", now);
        BizTaskExecutionDTO heartbeat = command(failed.getExecutionId(), 0, failed.getClaimToken(), now);
        heartbeat.setLeaseUntil(now.plusMinutes(1));
        assertTrue(bizTaskExecutionManager.heartbeat(heartbeat));
        assertFalse(bizTaskExecutionManager.recoverExpiredLease(staleRecovery));

        BizTaskExecutionDTO current = bizTaskExecutionAssembler.doToDto(find(failed.getExecutionId()));
        current.setState("FAILED");
        current.setUpdateTime(now.plusMinutes(2));
        current.setFinishedAt(now.plusMinutes(2));
        current.setFailureCode("LEASE_EXPIRED");
        current.setFailureMessage("execution lease expired");
        assertTrue(bizTaskExecutionManager.recoverExpiredLease(current));
        assertEquals("FAILED", find(failed.getExecutionId()).getState());
    }

    private BizTaskExecutionDTO recovery(BizTaskExecutionDTO source, String targetState, LocalDateTime now) {
        BizTaskExecutionDTO command = command(source.getExecutionId(), source.getVersion(), source.getClaimToken(), now);
        command.setState(targetState);
        command.setFinishedAt("FAILED".equals(targetState) ? now : null);
        command.setFailureCode("LEASE_EXPIRED");
        command.setFailureMessage("execution lease expired");
        return command;
    }

    private BizTaskExecutionDTO running(String executionId, LocalDateTime leaseUntil, String claimToken) {
        BizTaskExecutionDTO execution = execution(executionId, "btask_" + executionId, leaseUntil.minusMinutes(1),
            "RUNNING");
        execution.setAttemptCount(1);
        execution.setClaimToken(claimToken);
        execution.setWorkerNode("node-a");
        execution.setHeartbeatAt(leaseUntil.minusSeconds(10));
        execution.setLeaseUntil(leaseUntil);
        return execution;
    }

    private BizTaskExecutionDTO command(String executionId, Integer version, String claimToken,
        LocalDateTime updateTime) {
        BizTaskExecutionDTO command = new BizTaskExecutionDTO();
        command.setExecutionId(executionId);
        command.setVersion(version);
        command.setClaimToken(claimToken);
        command.setHeartbeatAt(updateTime);
        command.setUpdateTime(updateTime);
        return command;
    }

    private BizTaskProgressDTO progress(String taskId, String executionId, String claimToken, long current,
        long total, long revision, String message, LocalDateTime reportedAt) {
        BizTaskProgressDTO progress = new BizTaskProgressDTO();
        progress.setTaskId(taskId);
        progress.setExecutionId(executionId);
        progress.setClaimToken(claimToken);
        progress.setCurrent(current);
        progress.setTotal(total);
        progress.setRevision(revision);
        progress.setMessage(message);
        progress.setReportedAt(reportedAt);
        return progress;
    }

    private boolean updateProgressAfterStart(CountDownLatch ready, CountDownLatch start,
        BizTaskProgressDTO progress) throws InterruptedException {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS));
        return bizTaskExecutionManager.updateProgress(progress);
    }

    private void saveClaimedExecution(String executionId, String taskId, LocalDateTime now,
        String claimToken) {
        BizTaskExecutionDTO execution = execution(executionId, taskId, now, "RUNNING");
        execution.setAttemptCount(1);
        execution.setClaimToken(claimToken);
        execution.setWorkerNode("node-progress");
        execution.setHeartbeatAt(now);
        execution.setLeaseUntil(now.plusMinutes(1));
        bizTaskExecutionService.save(bizTaskExecutionAssembler.dtoToDo(execution));
    }

    private BizTaskDTO task(String taskId, String ownerId) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskDTO task = new BizTaskDTO();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setTaskId(taskId);
        task.setTaskType("QUERY_TEST");
        task.setTaskName("query test");
        task.setTaskMode("ONCE");
        task.setScheduleVersion(1);
        task.setState("RUNNING");
        task.setPriority(0);
        task.setPlannedCount(1);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setMissedCount(0);
        task.setCancelledCount(0);
        task.setProgressCurrent(0L);
        task.setProgressTotal(0L);
        task.setProgressRevision(0L);
        task.setPayload("{}");
        task.setPayloadVersion(1);
        task.setOwnerType("USER");
        task.setOwnerId(ownerId);
        task.setVersion(0);
        return task;
    }

    private BizTaskExecutionDTO execution(String executionId, String taskId, LocalDateTime plannedAt, String state) {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setCreateTime(plannedAt);
        execution.setUpdateTime(plannedAt);
        execution.setExecutionId(executionId);
        execution.setTaskId(taskId);
        execution.setScheduleVersion(1);
        execution.setPlannedAt(plannedAt);
        execution.setState(state);
        execution.setAttemptCount(0);
        execution.setMaxAttempts(3);
        execution.setProgressCurrent(0L);
        execution.setProgressTotal(0L);
        execution.setProgressRevision(0L);
        execution.setRetryable(false);
        execution.setVersion(0);
        return execution;
    }

    private void saveTask(BizTaskDTO task) {
        bizTaskService.save(bizTaskAssembler.dtoToDo(task));
    }

    private BizTaskExecutionDO find(String executionId) {
        return bizTaskExecutionService.getOne(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getExecutionId, executionId));
    }

    private BizTaskDO findTask(String taskId) {
        return bizTaskService.getOne(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, taskId));
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
