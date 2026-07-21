package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskProgressDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("Business-task progress persistence throttling")
class ThrottledTaskProgressReporterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 14, 0);

    @Mock
    private BizTaskExecutionManager executionManager;

    private ThrottledTaskProgressReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = new ThrottledTaskProgressReporter(executionManager, 500, 0, 100, "phase-a", NOW);
    }

    @Test
    @DisplayName("500ms 内不足 1% 应节流，达到 1% 应立即写入")
    void accept_shouldPersistAtConfiguredIntervalOrOnePercentChange() {
        when(executionManager.updateProgress(any())).thenReturn(true);

        reporter.accept(progress(0, 100, "phase-a", 1, NOW.plusNanos(100_000_000)));
        verify(executionManager, never()).updateProgress(any());

        reporter.accept(progress(1, 100, "phase-a", 2, NOW.plusNanos(101_000_000)));
        reporter.accept(progress(1, 100, "phase-a", 3, NOW.plusNanos(400_000_000)));
        reporter.accept(progress(1, 100, "phase-a", 4, NOW.plusNanos(601_000_000)));

        ArgumentCaptor<BizTaskProgressDTO> persisted = ArgumentCaptor.forClass(BizTaskProgressDTO.class);
        verify(executionManager, times(2)).updateProgress(persisted.capture());
        assertEquals(List.of(2L, 4L), persisted.getAllValues().stream()
            .map(BizTaskProgressDTO::getRevision).toList());
    }

    @Test
    @DisplayName("阶段变化和 forcePersist 应绕过时间与百分比节流")
    void accept_shouldPersistPhaseAndForcedTerminalUpdatesImmediately() {
        when(executionManager.updateProgress(any())).thenReturn(true);

        reporter.accept(progress(0, 100, "phase-b", 1, NOW.plusNanos(1_000_000)));
        BizTaskProgressDTO terminal = progress(0, 100, "phase-b", 2, NOW.plusNanos(2_000_000));
        terminal.setForcePersist(Boolean.TRUE);
        reporter.accept(terminal);

        verify(executionManager, times(2)).updateProgress(any());
    }

    private BizTaskProgressDTO progress(long current, long total, String message, long revision,
        LocalDateTime reportedAt) {
        BizTaskProgressDTO progress = new BizTaskProgressDTO();
        progress.setTaskId("btask_progress");
        progress.setExecutionId("bexec_progress");
        progress.setClaimToken("claim-progress");
        progress.setCurrent(current);
        progress.setTotal(total);
        progress.setMessage(message);
        progress.setRevision(revision);
        progress.setForcePersist(Boolean.FALSE);
        progress.setReportedAt(reportedAt);
        return progress;
    }
}
