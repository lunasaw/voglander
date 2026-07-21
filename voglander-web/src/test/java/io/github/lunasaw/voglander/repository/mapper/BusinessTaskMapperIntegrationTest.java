package io.github.lunasaw.voglander.repository.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;

@DisplayName("Durable business-task Mapper integration")
class BusinessTaskMapperIntegrationTest extends BaseTest {

    @Autowired
    private BizTaskMapper taskMapper;

    @Autowired
    private BizTaskExecutionMapper executionMapper;

    @Autowired
    private BizTaskEventMapper eventMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("taskId 与非空幂等键唯一，可空幂等键允许多行")
    void taskUniqueConstraints_shouldMatchContract() {
        String suffix = suffix();
        taskMapper.insert(task("btask_null_1_" + suffix, null));
        taskMapper.insert(task("btask_null_2_" + suffix, null));
        taskMapper.insert(task("btask_key_1_" + suffix, "same-" + suffix));

        assertThrows(DataAccessException.class,
            () -> taskMapper.insert(task("btask_key_2_" + suffix, "same-" + suffix)));
        assertThrows(DataAccessException.class,
            () -> taskMapper.insert(task("btask_null_1_" + suffix, null)));
    }

    @Test
    @DisplayName("executionId 与计划点唯一，insert-if-absent 重放返回 0")
    void executionInsertIfAbsent_shouldEnforceBothIdentities() {
        String suffix = suffix();
        LocalDateTime plannedAt = LocalDateTime.now().withNano(0);
        BizTaskExecutionDO first = execution("bexec_1_" + suffix, "btask_" + suffix, plannedAt, "PENDING");
        BizTaskExecutionDO duplicatePlan = execution("bexec_2_" + suffix, "btask_" + suffix, plannedAt, "PENDING");

        assertEquals(1, executionMapper.insertIfAbsent(first));
        assertEquals(0, executionMapper.insertIfAbsent(first));
        assertEquals(0, executionMapper.insertIfAbsent(duplicatePlan));
        assertEquals(1, countExecutions(first.getTaskId()));
    }

    @Test
    @DisplayName("eventId 与非空 dedupeKey 唯一，NULL dedupeKey 可重复")
    void eventInsertIfAbsent_shouldKeepAppendOnlyDedupeSemantics() {
        String suffix = suffix();
        assertEquals(1, eventMapper.insertIfAbsent(event("bevt_1_" + suffix, "btask_" + suffix, null)));
        assertEquals(1, eventMapper.insertIfAbsent(event("bevt_2_" + suffix, "btask_" + suffix, null)));
        assertEquals(1,
            eventMapper.insertIfAbsent(event("bevt_3_" + suffix, "btask_" + suffix, "terminal-" + suffix)));
        assertEquals(0,
            eventMapper.insertIfAbsent(event("bevt_4_" + suffix, "btask_" + suffix, "terminal-" + suffix)));
        assertEquals(0,
            eventMapper.insertIfAbsent(event("bevt_3_" + suffix, "btask_other_" + suffix, null)));
    }

    @Test
    @DisplayName("到期、可运行和过期租约扫描使用固定状态与确定排序")
    void scans_shouldUseFixedAllowlistAndDeterministicOrder() {
        String suffix = suffix();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskDO later = task("btask_later_" + suffix, null);
        later.setState("RUNNING");
        later.setNextPlanTime(now.minusSeconds(1));
        BizTaskDO earlier = task("btask_earlier_" + suffix, null);
        earlier.setNextPlanTime(now.minusSeconds(2));
        BizTaskDO paused = task("btask_paused_" + suffix, null);
        paused.setState("PAUSED");
        paused.setNextPlanTime(now.minusSeconds(3));
        taskMapper.insert(later);
        taskMapper.insert(earlier);
        taskMapper.insert(paused);

        List<String> dueIds = taskMapper.selectDueTasks(now, 100).stream().map(BizTaskDO::getTaskId)
            .filter(id -> id.endsWith(suffix)).toList();
        assertEquals(Arrays.asList(earlier.getTaskId(), later.getTaskId()), dueIds);

        executionMapper.insert(execution("bexec_pending_" + suffix, earlier.getTaskId(), now, "PENDING"));
        BizTaskExecutionDO retry = execution("bexec_retry_" + suffix, earlier.getTaskId(), now.plusSeconds(1),
            "RETRY_WAIT");
        retry.setNextAttemptTime(now.minusSeconds(1));
        executionMapper.insert(retry);
        BizTaskExecutionDO running = execution("bexec_running_" + suffix, later.getTaskId(), now, "RUNNING");
        running.setLeaseUntil(now.minusSeconds(1));
        executionMapper.insert(running);

        assertTrue(executionMapper.selectRunnable(now, 100).stream()
            .anyMatch(row -> row.getExecutionId().equals("bexec_pending_" + suffix)));
        assertTrue(executionMapper.selectRunnable(now, 100).stream()
            .anyMatch(row -> row.getExecutionId().equals("bexec_retry_" + suffix)));
        assertTrue(executionMapper.selectExpiredLeases(now, 100).stream()
            .anyMatch(row -> row.getExecutionId().equals("bexec_running_" + suffix)));
    }

    @Test
    @DisplayName("任务游标和状态转换使用 version CAS，只有一次竞争更新成功")
    void taskConditionalUpdates_shouldRejectStaleVersion() {
        String suffix = suffix();
        BizTaskDO row = task("btask_cas_" + suffix, null);
        row.setNextPlanTime(LocalDateTime.now());
        taskMapper.insert(row);
        LocalDateTime now = LocalDateTime.now().withNano(0);

        assertEquals(1, taskMapper.advanceCursor(row.getTaskId(), 0, now.plusMinutes(1), "RUNNING", now));
        assertEquals(0, taskMapper.advanceCursor(row.getTaskId(), 0, now.plusMinutes(2), "RUNNING", now));
        assertEquals(1,
            taskMapper.transitionState(row.getTaskId(), 1, Arrays.asList("RUNNING"), "PAUSED", now.plusSeconds(1)));
        assertEquals(0,
            taskMapper.transitionState(row.getTaskId(), 1, Arrays.asList("RUNNING"), "FAILED", now.plusSeconds(2)));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("两个调度节点竞争时每个 task/version/plannedAt 只保留一条 execution")
    void competingSchedulerNodes_shouldLeaveOneExecutionFact() throws Exception {
        String suffix = suffix();
        String taskId = "btask_schedule_race_" + suffix;
        LocalDateTime plannedAt = LocalDateTime.now().withNano(0);
        BizTaskDO task = task(taskId, null);
        task.setTaskMode("FIXED_RATE");
        task.setScheduleStartTime(plannedAt);
        task.setScheduleEndTime(plannedAt.plusMinutes(2));
        task.setIntervalSeconds(60L);
        task.setNextPlanTime(plannedAt);
        task.setPlannedCount(3);
        taskMapper.insert(task);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> nodeA = executor.submit(() -> materializeCompetingPoint(
                execution("bexec_node_a_" + suffix, taskId, plannedAt, "PENDING"), ready, start));
            Future<Boolean> nodeB = executor.submit(() -> materializeCompetingPoint(
                execution("bexec_node_b_" + suffix, taskId, plannedAt, "PENDING"), ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int committed = (nodeA.get(10, TimeUnit.SECONDS) ? 1 : 0)
                + (nodeB.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, committed);
            assertEquals(1, countExecutions(taskId));
            BizTaskDO stored = taskMapper.selectOne(new LambdaQueryWrapper<BizTaskDO>()
                .eq(BizTaskDO::getTaskId, taskId));
            assertEquals(1, stored.getVersion());
            assertEquals(plannedAt.plusMinutes(1), stored.getNextPlanTime());
        } finally {
            start.countDown();
            executor.shutdownNow();
            executionMapper.delete(new LambdaQueryWrapper<BizTaskExecutionDO>()
                .eq(BizTaskExecutionDO::getTaskId, taskId));
            taskMapper.delete(new LambdaQueryWrapper<BizTaskDO>().eq(BizTaskDO::getTaskId, taskId));
        }
    }

    @Test
    @DisplayName("claim、heartbeat、retry-wait 和 terminal 均绑定当前 claim token")
    void executionConditionalUpdates_shouldRejectStaleWorker() {
        String suffix = suffix();
        String executionId = "bexec_claim_" + suffix;
        LocalDateTime now = LocalDateTime.now().withNano(0);
        executionMapper.insert(execution(executionId, "btask_" + suffix, now, "PENDING"));

        assertEquals(1, executionMapper.claim(executionId, 0, "claim-a", "node-a", now, now.plusMinutes(1)));
        assertEquals(0, executionMapper.claim(executionId, 0, "claim-b", "node-b", now, now.plusMinutes(1)));
        assertEquals(0, executionMapper.heartbeat(executionId, "stale", now.plusSeconds(1), now.plusMinutes(2)));
        assertEquals(1, executionMapper.heartbeat(executionId, "claim-a", now.plusSeconds(1), now.plusMinutes(2)));
        assertEquals(0, executionMapper.markRetryWait(executionId, "stale", now.plusSeconds(5), "TEMP", "retry", now));
        assertEquals(1,
            executionMapper.markRetryWait(executionId, "claim-a", now.plusSeconds(5), "TEMP", "retry", now));

        BizTaskExecutionDO retry = findExecution(executionId);
        assertEquals("RETRY_WAIT", retry.getState());
        assertEquals(1, executionMapper.claim(executionId, retry.getVersion(), "claim-c", "node-c",
            now.plusSeconds(5), now.plusMinutes(2)));
        assertEquals(0, executionMapper.markTerminal(executionId, "stale", "SUCCEEDED", now.plusSeconds(6),
            "TEST", "result", "{}", null, null, false, now.plusSeconds(6)));
        assertEquals(1, executionMapper.markTerminal(executionId, "claim-c", "SUCCEEDED", now.plusSeconds(6),
            "TEST", "result", "{}", null, null, false, now.plusSeconds(6)));
        assertEquals("SUCCEEDED", findExecution(executionId).getState());
    }

    @Test
    @DisplayName("execution 与 event timeline 使用固定升序")
    void timelines_shouldBeDeterministic() {
        String suffix = suffix();
        String taskId = "btask_timeline_" + suffix;
        LocalDateTime now = LocalDateTime.now().withNano(0);
        executionMapper.insert(execution("bexec_later_" + suffix, taskId, now.plusSeconds(1), "SUCCEEDED"));
        executionMapper.insert(execution("bexec_earlier_" + suffix, taskId, now, "SUCCEEDED"));
        BizTaskEventDO later = event("bevt_later_" + suffix, taskId, null);
        later.setOccurredAt(now.plusSeconds(1));
        BizTaskEventDO earlier = event("bevt_earlier_" + suffix, taskId, null);
        earlier.setOccurredAt(now);
        eventMapper.insertIfAbsent(later);
        eventMapper.insertIfAbsent(earlier);

        assertEquals(Arrays.asList("bexec_earlier_" + suffix, "bexec_later_" + suffix),
            executionMapper.selectTaskTimeline(taskId, 10).stream().map(BizTaskExecutionDO::getExecutionId).toList());
        assertEquals(Arrays.asList("bevt_earlier_" + suffix, "bevt_later_" + suffix),
            eventMapper.selectTimeline(taskId, null, 10).stream().map(BizTaskEventDO::getEventId).toList());
    }

    private BizTaskDO task(String taskId, String idempotencyKey) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskDO task = new BizTaskDO();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setTaskId(taskId);
        task.setTaskType("TEST_TASK");
        task.setTaskName("test");
        task.setTaskMode("ONCE");
        task.setScheduleVersion(1);
        task.setState("SCHEDULED");
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
        task.setOwnerId("owner-1");
        task.setIdempotencyKey(idempotencyKey);
        task.setVersion(0);
        return task;
    }

    private BizTaskExecutionDO execution(String executionId, String taskId, LocalDateTime plannedAt, String state) {
        BizTaskExecutionDO execution = new BizTaskExecutionDO();
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

    private BizTaskEventDO event(String eventId, String taskId, String dedupeKey) {
        BizTaskEventDO event = new BizTaskEventDO();
        event.setCreateTime(LocalDateTime.now().withNano(0));
        event.setEventId(eventId);
        event.setTaskId(taskId);
        event.setEventType("STATE_CHANGED");
        event.setDedupeKey(dedupeKey);
        event.setOccurredAt(LocalDateTime.now().withNano(0));
        return event;
    }

    private boolean materializeCompetingPoint(BizTaskExecutionDO execution, CountDownLatch ready,
        CountDownLatch start) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Boolean committed = transaction.execute(status -> {
            ready.countDown();
            await(start);
            int inserted = executionMapper.insertIfAbsent(execution);
            int advanced = taskMapper.advanceCursor(execution.getTaskId(), 0,
                execution.getPlannedAt().plusMinutes(1), "RUNNING", execution.getPlannedAt());
            if (inserted != 1 || advanced != 1) {
                status.setRollbackOnly();
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        });
        return Boolean.TRUE.equals(committed);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating scheduler race", exception);
        }
    }

    private long countExecutions(String taskId) {
        return executionMapper.selectCount(new LambdaQueryWrapper<BizTaskExecutionDO>().eq(BizTaskExecutionDO::getTaskId,
            taskId));
    }

    private BizTaskExecutionDO findExecution(String executionId) {
        return executionMapper.selectOne(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getExecutionId, executionId));
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
