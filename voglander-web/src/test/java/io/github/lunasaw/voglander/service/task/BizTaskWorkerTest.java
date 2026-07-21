package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.domain.task.TaskResultReference;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.service.task.LongTaskContext;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.client.service.task.TaskCompletionParticipant;
import io.github.lunasaw.voglander.client.domain.task.TaskCompensation;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskProgressDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskCompletionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("Durable business-task Worker claim boundary")
class BizTaskWorkerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 12, 0);

    @Mock
    private BizTaskExecutionManager executionManager;

    @Mock
    private BizTaskManager bizTaskManager;

    @Mock
    private LongTaskHandlerRegistry handlerRegistry;

    @Mock
    private BizTaskCompletionManager completionManager;

    @Mock
    private RedisLockUtil redisLockUtil;

    @Mock
    private TransactionOperations transactionOperations;

    @Mock
    private LongTaskHandler handler;

    @Mock
    private TaskCompletionParticipant completionParticipant;

    private BizTaskWorker worker;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction((TransactionStatus)null);
        }).when(transactionOperations).execute(any());
        worker = new BizTaskWorker(executionManager, bizTaskManager, handlerRegistry, completionManager,
            redisLockUtil, transactionOperations,
            Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC), "node-a", 90, 5,
            () -> "claim-token-a");
    }

    @Test
    @DisplayName("应在 execution 短锁内事务重读并用 version/token/node/lease 执行 CAS claim")
    void execute_shouldLockRereadAndClaimRunnableExecution() {
        BizTaskExecutionDTO current = execution("bexec_claim", "PENDING", 7);
        BizTaskExecutionDTO claimed = execution("bexec_claim", "RUNNING", 8);
        claimed.setClaimToken("claim-token-a");
        claimed.setAttemptCount(1);
        when(redisLockUtil.generateLockValue()).thenReturn("lock-a");
        when(redisLockUtil.lock(TaskConstant.EXECUTION_LOCK_PREFIX + "bexec_claim", "lock-a", 5))
            .thenReturn(true);
        when(executionManager.getForClaim("bexec_claim")).thenReturn(current);
        when(executionManager.claim(any())).thenReturn(claimed);

        BizTaskExecutionDTO result = worker.claim("bexec_claim");

        assertSame(claimed, result);
        ArgumentCaptor<BizTaskExecutionDTO> command = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        InOrder order = inOrder(redisLockUtil, transactionOperations, executionManager);
        order.verify(redisLockUtil).lock(TaskConstant.EXECUTION_LOCK_PREFIX + "bexec_claim", "lock-a", 5);
        order.verify(transactionOperations).execute(any());
        order.verify(executionManager).getForClaim("bexec_claim");
        order.verify(executionManager).claim(command.capture());
        order.verify(redisLockUtil).unLock(TaskConstant.EXECUTION_LOCK_PREFIX + "bexec_claim", "lock-a");
        assertEquals(7, command.getValue().getVersion());
        assertEquals("claim-token-a", command.getValue().getClaimToken());
        assertEquals("node-a", command.getValue().getWorkerNode());
        assertEquals(NOW, command.getValue().getUpdateTime());
        assertEquals(NOW.plusSeconds(90), command.getValue().getLeaseUntil());
    }

    @Test
    @DisplayName("短锁竞争失败时不得重读、claim 或错误释放他人锁")
    void claim_shouldSkipWhenExecutionLockIsBusy() {
        when(redisLockUtil.generateLockValue()).thenReturn("lock-busy");
        when(redisLockUtil.lock(TaskConstant.EXECUTION_LOCK_PREFIX + "bexec_busy", "lock-busy", 5))
            .thenReturn(false);

        BizTaskExecutionDTO result = worker.claim("bexec_busy");

        assertNull(result);
        verify(executionManager, never()).getForClaim(any());
        verify(executionManager, never()).claim(any());
        verify(redisLockUtil, never()).unLock(TaskConstant.EXECUTION_LOCK_PREFIX + "bexec_busy", "lock-busy");
    }

    @Test
    @DisplayName("只有 claim 成功后才解析 Handler，并把 participant 交给原子完成事务")
    void execute_shouldInvokeResolvedHandlerAndCompleteClaimedExecution() throws Exception {
        BizTaskExecutionDTO claimed = stubSuccessfulClaim("bexec_execute");
        when(redisLockUtil.generateLockValue()).thenReturn("lock-execute");
        BizTaskDTO task = task(claimed.getTaskId(), "TEST_HANDLER", 1, "{\"value\":\"payload-a\"}");
        JSONObject summary = new JSONObject();
        summary.put("display", "completed");
        TaskExecutionResult result = TaskExecutionResult.success(
            new TaskResultReference("TEST_RESULT", "result-1"), summary);
        when(bizTaskManager.getForExecution(claimed.getTaskId())).thenReturn(task);
        when(handlerRegistry.require("TEST_HANDLER", 1)).thenReturn(handler);
        when(handler.taskType()).thenReturn("TEST_HANDLER");
        when(handler.execute(any(), any())).thenReturn(result);
        when(handler.completionParticipant()).thenReturn(completionParticipant);

        worker.execute("bexec_execute");

        ArgumentCaptor<LongTaskContext> contextCaptor = ArgumentCaptor.forClass(LongTaskContext.class);
        ArgumentCaptor<JSONObject> payloadCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(handler).execute(contextCaptor.capture(), payloadCaptor.capture());
        assertEquals(claimed.getTaskId(), contextCaptor.getValue().taskId());
        assertEquals(claimed.getExecutionId(), contextCaptor.getValue().executionId());
        assertEquals(1, contextCaptor.getValue().attempt());
        assertEquals("payload-a", payloadCaptor.getValue().getString("value"));

        ArgumentCaptor<BizTaskExecutionDTO> completionCommand =
            ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        ArgumentCaptor<BizTaskEventDTO> successEvent = ArgumentCaptor.forClass(BizTaskEventDTO.class);
        verify(completionManager).completeSuccess(completionCommand.capture(), eq(result),
            eq(completionParticipant), successEvent.capture());
        assertEquals("SUCCEEDED", completionCommand.getValue().getState());
        assertEquals("claim-token-a", completionCommand.getValue().getClaimToken());
        assertEquals(NOW, completionCommand.getValue().getFinishedAt());
        assertEquals("SUCCEEDED", successEvent.getValue().getEventType());
        assertEquals("RUNNING", successEvent.getValue().getFromState());
        assertEquals("SUCCEEDED", successEvent.getValue().getToState());
        assertEquals(1, successEvent.getValue().getAttemptNo());

        InOrder order = inOrder(redisLockUtil, executionManager, bizTaskManager, handlerRegistry, handler,
            completionManager);
        order.verify(executionManager).claim(any());
        order.verify(redisLockUtil).unLock(TaskConstant.EXECUTION_LOCK_PREFIX + "bexec_execute", "lock-execute");
        order.verify(bizTaskManager).getForExecution(claimed.getTaskId());
        order.verify(handlerRegistry).require("TEST_HANDLER", 1);
        order.verify(handler).taskType();
        order.verify(handler).execute(any(), any());
        order.verify(completionManager).completeSuccess(any(), eq(result), eq(completionParticipant), any());
    }

    @Test
    @DisplayName("完成事务失败时应执行 provider compensation，补偿失败不得吞掉原始错误")
    void execute_shouldCompensateProviderObjectWhenCompletionTransactionFails() throws Exception {
        BizTaskExecutionDTO claimed = stubSuccessfulClaim("bexec_compensation");
        when(redisLockUtil.generateLockValue()).thenReturn("lock-compensation");
        BizTaskDTO task = task(claimed.getTaskId(), "COMPENSATION_HANDLER", 1, "{}");
        AtomicBoolean compensated = new AtomicBoolean();
        TaskExecutionResult result = new TaskExecutionResult(new TaskResultReference("IMAGE_ASSET", "img_1"),
            new JSONObject(), new JSONObject(), () -> compensated.set(true));
        when(bizTaskManager.getForExecution(claimed.getTaskId())).thenReturn(task);
        when(handlerRegistry.require("COMPENSATION_HANDLER", 1)).thenReturn(handler);
        when(handler.taskType()).thenReturn("COMPENSATION_HANDLER");
        when(handler.execute(any(), any())).thenReturn(result);
        when(handler.completionParticipant()).thenReturn(completionParticipant);
        when(completionManager.completeSuccess(any(), eq(result), eq(completionParticipant), any()))
            .thenThrow(new IllegalStateException("db rollback"));

        assertThrows(IllegalStateException.class, () -> worker.execute("bexec_compensation"));
        assertTrue(compensated.get());
    }

    @Test
    @DisplayName("完成前应强制持久化被节流的最后一条进度")
    void execute_shouldForceLatestThrottledProgressBeforeCompletion() throws Exception {
        BizTaskExecutionDTO claimed = stubSuccessfulClaim("bexec_progress");
        when(redisLockUtil.generateLockValue()).thenReturn("lock-progress");
        BizTaskDTO task = task(claimed.getTaskId(), "PROGRESS_HANDLER", 1, "{}");
        TaskExecutionResult result = TaskExecutionResult.success(
            new TaskResultReference("TEST_RESULT", "result-progress"), new JSONObject());
        when(bizTaskManager.getForExecution(claimed.getTaskId())).thenReturn(task);
        when(handlerRegistry.require("PROGRESS_HANDLER", 1)).thenReturn(handler);
        when(handler.taskType()).thenReturn("PROGRESS_HANDLER");
        when(handler.execute(any(), any())).thenAnswer(invocation -> {
            LongTaskContext context = invocation.getArgument(0);
            context.reportProgress(1, 1000, "processing");
            context.reportProgress(2, 1000, "processing");
            return result;
        });
        when(handler.completionParticipant()).thenReturn(completionParticipant);
        when(executionManager.updateProgress(any())).thenReturn(true);

        worker.execute("bexec_progress");

        ArgumentCaptor<BizTaskProgressDTO> progress = ArgumentCaptor.forClass(BizTaskProgressDTO.class);
        verify(executionManager, times(2)).updateProgress(progress.capture());
        assertEquals(1L, progress.getAllValues().get(0).getRevision());
        assertEquals(Boolean.FALSE, progress.getAllValues().get(0).getForcePersist());
        assertEquals(2L, progress.getAllValues().get(1).getRevision());
        assertEquals(Boolean.TRUE, progress.getAllValues().get(1).getForcePersist());
        verify(completionManager).completeSuccess(any(), eq(result), eq(completionParticipant), any());
    }

    @Test
    @DisplayName("上下文 heartbeat 应只续约当前 claim，并将陈旧 claim 结果返回给 Handler")
    void execute_shouldBindHeartbeatToActiveClaimAndExposeStaleResult() throws Exception {
        BizTaskExecutionDTO claimed = stubSuccessfulClaim("bexec_heartbeat");
        when(redisLockUtil.generateLockValue()).thenReturn("lock-heartbeat");
        BizTaskDTO task = task(claimed.getTaskId(), "HEARTBEAT_HANDLER", 1, "{}");
        TaskExecutionResult result = TaskExecutionResult.success(
            new TaskResultReference("TEST_RESULT", "result-heartbeat"), new JSONObject());
        AtomicBoolean heartbeatAccepted = new AtomicBoolean(true);
        when(bizTaskManager.getForExecution(claimed.getTaskId())).thenReturn(task);
        when(handlerRegistry.require("HEARTBEAT_HANDLER", 1)).thenReturn(handler);
        when(handler.taskType()).thenReturn("HEARTBEAT_HANDLER");
        when(handler.execute(any(), any())).thenAnswer(invocation -> {
            heartbeatAccepted.set(((LongTaskContext)invocation.getArgument(0)).heartbeat());
            return result;
        });
        when(handler.completionParticipant()).thenReturn(completionParticipant);
        when(executionManager.heartbeat(any())).thenReturn(false);

        worker.execute("bexec_heartbeat");

        assertFalse(heartbeatAccepted.get());
        ArgumentCaptor<BizTaskExecutionDTO> heartbeat = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(executionManager).heartbeat(heartbeat.capture());
        assertEquals(claimed.getExecutionId(), heartbeat.getValue().getExecutionId());
        assertEquals("claim-token-a", heartbeat.getValue().getClaimToken());
        assertEquals(NOW, heartbeat.getValue().getUpdateTime());
        assertEquals(NOW.plusSeconds(90), heartbeat.getValue().getLeaseUntil());
        verify(completionManager).completeSuccess(any(), eq(result), eq(completionParticipant), any());
    }

    @Test
    @DisplayName("取消令牌应轮询任务状态，并只向声明 CANCEL 能力的 Handler 暴露取消")
    void execute_shouldPollCancellationForCancelCapableHandler() throws Exception {
        BizTaskExecutionDTO claimed = stubSuccessfulClaim("bexec_cancellation");
        when(redisLockUtil.generateLockValue()).thenReturn("lock-cancellation");
        BizTaskDTO runningTask = task(claimed.getTaskId(), "CANCELLATION_HANDLER", 1, "{}");
        runningTask.setState("RUNNING");
        BizTaskDTO cancellingTask = task(claimed.getTaskId(), "CANCELLATION_HANDLER", 1, "{}");
        cancellingTask.setState("CANCELLING");
        TaskExecutionResult result = TaskExecutionResult.success(
            new TaskResultReference("TEST_RESULT", "result-cancellation"), new JSONObject());
        AtomicBoolean cancellationRequested = new AtomicBoolean(false);
        when(bizTaskManager.getForExecution(claimed.getTaskId())).thenReturn(runningTask, cancellingTask);
        when(handlerRegistry.require("CANCELLATION_HANDLER", 1)).thenReturn(handler);
        when(handler.taskType()).thenReturn("CANCELLATION_HANDLER");
        when(handler.capabilities()).thenReturn(new TaskCapabilities(false, true, false, false, false));
        when(handler.execute(any(), any())).thenAnswer(invocation -> {
            cancellationRequested.set(((LongTaskContext)invocation.getArgument(0)).cancellationToken()
                .isCancellationRequested());
            return result;
        });
        when(handler.completionParticipant()).thenReturn(completionParticipant);

        worker.execute("bexec_cancellation");

        assertTrue(cancellationRequested.get());
        verify(handler).capabilities();
    }

    @Test
    @DisplayName("未声明 CANCEL 能力的 Handler 不应收到协作取消信号")
    void execute_shouldNotExposeCancellationToHandlerWithoutCancelCapability() throws Exception {
        BizTaskExecutionDTO claimed = stubSuccessfulClaim("bexec_non_cancellable");
        when(redisLockUtil.generateLockValue()).thenReturn("lock-non_cancellable");
        BizTaskDTO runningTask = task(claimed.getTaskId(), "NON_CANCELLATION_HANDLER", 1, "{}");
        runningTask.setState("RUNNING");
        BizTaskDTO cancellingTask = task(claimed.getTaskId(), "NON_CANCELLATION_HANDLER", 1, "{}");
        cancellingTask.setState("CANCELLING");
        TaskExecutionResult result = TaskExecutionResult.success(
            new TaskResultReference("TEST_RESULT", "result-non-cancellable"), new JSONObject());
        AtomicBoolean cancellationRequested = new AtomicBoolean(true);
        when(bizTaskManager.getForExecution(claimed.getTaskId())).thenReturn(runningTask, cancellingTask);
        when(handlerRegistry.require("NON_CANCELLATION_HANDLER", 1)).thenReturn(handler);
        when(handler.taskType()).thenReturn("NON_CANCELLATION_HANDLER");
        when(handler.capabilities()).thenReturn(TaskCapabilities.none());
        when(handler.execute(any(), any())).thenAnswer(invocation -> {
            cancellationRequested.set(((LongTaskContext)invocation.getArgument(0)).cancellationToken()
                .isCancellationRequested());
            return result;
        });
        when(handler.completionParticipant()).thenReturn(completionParticipant);

        worker.execute("bexec_non_cancellable");

        assertFalse(cancellationRequested.get());
        verify(handler).capabilities();
        verify(bizTaskManager, times(1)).getForExecution(claimed.getTaskId());
    }

    @Test
    @DisplayName("claim 失败时不得解析或调用 Handler")
    void execute_shouldNotResolveHandlerWhenClaimIsBusy() {
        when(redisLockUtil.generateLockValue()).thenReturn("lock-busy-execute");
        when(redisLockUtil.lock(TaskConstant.EXECUTION_LOCK_PREFIX + "bexec_busy_execute", "lock-busy-execute", 5))
            .thenReturn(false);

        worker.execute("bexec_busy_execute");

        verifyNoInteractions(bizTaskManager, handlerRegistry, completionManager, handler);
    }

    @Test
    @DisplayName("持久化 taskType 与解析出的 Handler 类型不一致时应稳定拒绝")
    void execute_shouldRejectTaskTypeHandlerMismatchAsStableFailure() throws Exception {
        BizTaskExecutionDTO claimed = stubSuccessfulClaim("bexec_mismatch");
        when(redisLockUtil.generateLockValue()).thenReturn("lock-mismatch");
        BizTaskDTO task = task(claimed.getTaskId(), "EXPECTED_HANDLER", 1, "{}");
        when(bizTaskManager.getForExecution(claimed.getTaskId())).thenReturn(task);
        when(handlerRegistry.require("EXPECTED_HANDLER", 1)).thenReturn(handler);
        when(handler.taskType()).thenReturn("OTHER_HANDLER");

        ServiceException failure = assertThrows(ServiceException.class,
            () -> worker.execute("bexec_mismatch"));

        assertEquals(720012, failure.getCode());
        verify(handler, never()).execute(any(), any());
        verifyNoInteractions(completionManager);
    }

    @Test
    @DisplayName("一个 Handler 异常不得外溢或阻止其他 task type 继续执行")
    void execute_shouldIsolateHandlerFailureAndKeepOtherTaskTypesAlive() throws Exception {
        BizTaskExecutionDTO failedClaim = stubSuccessfulClaim("bexec_failure");
        BizTaskExecutionDTO successfulClaim = stubSuccessfulClaim("bexec_success_after_failure");
        when(redisLockUtil.generateLockValue()).thenReturn("lock-failure", "lock-success_after_failure");

        BizTaskDTO failedTask = task(failedClaim.getTaskId(), "FAILING_HANDLER", 1, "{}");
        when(bizTaskManager.getForExecution(failedClaim.getTaskId())).thenReturn(failedTask);
        when(handlerRegistry.require("FAILING_HANDLER", 1)).thenReturn(handler);
        when(handler.taskType()).thenReturn("FAILING_HANDLER");
        when(handler.execute(any(), any())).thenThrow(new IllegalStateException("simulated handler failure"));

        LongTaskHandler successfulHandler = mock(LongTaskHandler.class);
        BizTaskDTO successfulTask = task(successfulClaim.getTaskId(), "SUCCESS_HANDLER", 1, "{}");
        TaskExecutionResult success = TaskExecutionResult.success(
            new TaskResultReference("TEST_RESULT", "result-after-failure"), new JSONObject());
        when(bizTaskManager.getForExecution(successfulClaim.getTaskId())).thenReturn(successfulTask);
        when(handlerRegistry.require("SUCCESS_HANDLER", 1)).thenReturn(successfulHandler);
        when(successfulHandler.taskType()).thenReturn("SUCCESS_HANDLER");
        when(successfulHandler.execute(any(), any())).thenReturn(success);
        when(successfulHandler.completionParticipant()).thenReturn(completionParticipant);

        assertDoesNotThrow(() -> worker.execute("bexec_failure"));
        assertDoesNotThrow(() -> worker.execute("bexec_success_after_failure"));

        verify(handler).execute(any(), any());
        ArgumentCaptor<TaskAttemptContext> failureAttempt = ArgumentCaptor.forClass(TaskAttemptContext.class);
        verify(handler).classify(any(IllegalStateException.class), failureAttempt.capture());
        assertEquals(failedClaim.getTaskId(), failureAttempt.getValue().taskId());
        assertEquals(failedClaim.getExecutionId(), failureAttempt.getValue().executionId());
        assertEquals(1, failureAttempt.getValue().attempt());
        assertEquals(1, failureAttempt.getValue().maxAttempts());
        verify(successfulHandler).execute(any(), any());
        ArgumentCaptor<BizTaskExecutionDTO> completed = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(completionManager, times(1)).completeSuccess(completed.capture(), eq(success),
            eq(completionParticipant), any());
        assertEquals(successfulClaim.getExecutionId(), completed.getValue().getExecutionId());
        ArgumentCaptor<BizTaskExecutionDTO> failed = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(completionManager).completeFailure(failed.capture(), eq("SYSTEM_ERROR"),
            eq("Business task handler failed"), any());
        assertEquals(failedClaim.getExecutionId(), failed.getValue().getExecutionId());
        assertEquals("FAILED", failed.getValue().getState());
    }

    @Test
    @DisplayName("可重试 Handler 失败应按当前 claim 写入有界 retry-wait")
    void execute_shouldMoveRetryableHandlerFailureToRetryWait() throws Exception {
        BizTaskExecutionDTO claimed = stubSuccessfulClaim("bexec_retryable_failure");
        claimed.setMaxAttempts(3);
        when(redisLockUtil.generateLockValue()).thenReturn("lock-retryable_failure");
        BizTaskDTO task = task(claimed.getTaskId(), "RETRYABLE_HANDLER", 1, "{}");
        when(bizTaskManager.getForExecution(claimed.getTaskId())).thenReturn(task);
        when(handlerRegistry.require("RETRYABLE_HANDLER", 1)).thenReturn(handler);
        when(handler.taskType()).thenReturn("RETRYABLE_HANDLER");
        when(handler.execute(any(), any())).thenThrow(new IllegalStateException("media timeout"));
        when(handler.classify(any(), any())).thenReturn(
            RetryDecision.retryable("MEDIA_TIMEOUT", "media request timed out"));
        when(executionManager.markRetryWait(any())).thenReturn(true);

        worker.execute("bexec_retryable_failure");

        ArgumentCaptor<BizTaskExecutionDTO> retry = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(executionManager).markRetryWait(retry.capture());
        assertEquals(claimed.getExecutionId(), retry.getValue().getExecutionId());
        assertEquals(claimed.getClaimToken(), retry.getValue().getClaimToken());
        assertEquals(NOW.plusSeconds(2), retry.getValue().getNextAttemptTime());
        assertEquals("MEDIA_TIMEOUT", retry.getValue().getFailureCode());
        assertEquals("media request timed out", retry.getValue().getFailureMessage());
        verifyNoInteractions(completionManager);
    }

    private BizTaskExecutionDTO stubSuccessfulClaim(String executionId) {
        BizTaskExecutionDTO current = execution(executionId, "PENDING", 7);
        BizTaskExecutionDTO claimed = execution(executionId, "RUNNING", 8);
        claimed.setClaimToken("claim-token-a");
        claimed.setWorkerNode("node-a");
        claimed.setAttemptCount(1);
        when(redisLockUtil.lock(TaskConstant.EXECUTION_LOCK_PREFIX + executionId,
            "lock-" + executionId.substring("bexec_".length()), 5)).thenReturn(true);
        when(executionManager.getForClaim(executionId)).thenReturn(current);
        doReturn(claimed).when(executionManager)
            .claim(argThat(command -> executionId.equals(command.getExecutionId())));
        return claimed;
    }

    private BizTaskDTO task(String taskId, String taskType, int payloadVersion, String payload) {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId(taskId);
        task.setTaskType(taskType);
        task.setPayloadVersion(payloadVersion);
        task.setPayload(payload);
        return task;
    }

    private BizTaskExecutionDTO execution(String executionId, String state, int version) {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId(executionId);
        execution.setTaskId("btask_" + executionId);
        execution.setState(state);
        execution.setVersion(version);
        execution.setAttemptCount(0);
        return execution;
    }
}
