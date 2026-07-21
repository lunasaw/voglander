package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.enums.task.TaskFailureCodeEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("Durable business-task expired lease recovery")
class BizTaskLeaseRecoveryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 13, 30);

    @Mock
    private BizTaskExecutionManager executionManager;

    @Test
    @DisplayName("未耗尽的到期 lease 应按 claim/version CAS 进入 retry-wait")
    void recoverExpiredLeases_shouldScheduleEligibleExecutionRetry() {
        BizTaskExecutionDTO expired = expired("bexec_lease_retry", 1, 3, NOW.plusMinutes(1));
        when(executionManager.findExpiredLeases(NOW, 100)).thenReturn(Arrays.asList(expired));
        BizTaskLeaseRecovery recovery = recovery();

        recovery.recoverExpiredLeases();

        ArgumentCaptor<BizTaskExecutionDTO> command = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(executionManager).recoverExpiredLease(command.capture());
        assertEquals(expired.getExecutionId(), command.getValue().getExecutionId());
        assertEquals(expired.getVersion(), command.getValue().getVersion());
        assertEquals(expired.getClaimToken(), command.getValue().getClaimToken());
        assertEquals("RETRY_WAIT", command.getValue().getState());
        assertEquals(NOW.plusSeconds(2), command.getValue().getNextAttemptTime());
        assertEquals(TaskFailureCodeEnum.LEASE_EXPIRED.name(), command.getValue().getFailureCode());
    }

    @Test
    @DisplayName("次数耗尽或越过 deadline 的到期 lease 应 CAS 终止为 failed")
    void recoverExpiredLeases_shouldFailExhaustedOrExpiredExecution() {
        BizTaskExecutionDTO exhausted = expired("bexec_lease_exhausted", 3, 3, NOW.plusMinutes(1));
        BizTaskExecutionDTO deadlineExpired = expired("bexec_lease_deadline", 1, 3, NOW.plusSeconds(1));
        when(executionManager.findExpiredLeases(NOW, 100)).thenReturn(Arrays.asList(exhausted, deadlineExpired));
        BizTaskLeaseRecovery recovery = recovery();

        recovery.recoverExpiredLeases();

        ArgumentCaptor<BizTaskExecutionDTO> command = ArgumentCaptor.forClass(BizTaskExecutionDTO.class);
        verify(executionManager, org.mockito.Mockito.times(2)).recoverExpiredLease(command.capture());
        command.getAllValues().forEach(value -> {
            assertEquals("FAILED", value.getState());
            assertEquals(NOW, value.getFinishedAt());
            assertEquals(TaskFailureCodeEnum.LEASE_EXPIRED.name(), value.getFailureCode());
        });
    }

    @Test
    @DisplayName("未到期或终态 execution 即使意外出现在扫描结果中也不得被恢复")
    void recoverExpiredLeases_shouldIgnoreActiveAndTerminalExecutions() {
        BizTaskExecutionDTO active = expired("bexec_lease_active", 1, 3, NOW.plusMinutes(1));
        active.setLeaseUntil(NOW.plusSeconds(1));
        BizTaskExecutionDTO terminal = expired("bexec_lease_terminal", 1, 3, NOW.plusMinutes(1));
        terminal.setState("SUCCEEDED");
        when(executionManager.findExpiredLeases(NOW, 100)).thenReturn(Arrays.asList(active, terminal));

        recovery().recoverExpiredLeases();

        verify(executionManager, never()).recoverExpiredLease(org.mockito.ArgumentMatchers.any());
    }

    private BizTaskLeaseRecovery recovery() {
        return new BizTaskLeaseRecovery(executionManager,
            Clock.fixed(Instant.parse("2026-07-15T13:30:00Z"), ZoneOffset.UTC), 100, 2, 30);
    }

    private BizTaskExecutionDTO expired(String executionId, int attempt, int maxAttempts, LocalDateTime deadlineAt) {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId(executionId);
        execution.setTaskId("btask_" + executionId);
        execution.setVersion(7);
        execution.setClaimToken("claim-" + executionId);
        execution.setState("RUNNING");
        execution.setAttemptCount(attempt);
        execution.setMaxAttempts(maxAttempts);
        execution.setDeadlineAt(deadlineAt);
        execution.setLeaseUntil(NOW.minusSeconds(1));
        return execution;
    }
}
