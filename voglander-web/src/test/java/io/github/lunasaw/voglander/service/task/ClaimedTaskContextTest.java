package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskProgressDTO;

@DisplayName("Claimed LongTaskContext progress invariants")
class ClaimedTaskContextTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 13, 0);

    private List<BizTaskProgressDTO> reports;
    private Clock clock;

    @BeforeEach
    void setUp() {
        reports = new ArrayList<BizTaskProgressDTO>();
        clock = Clock.fixed(Instant.parse("2026-07-15T13:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("应从持久化 revision 继续递增，并允许未知 total 变为确定 total")
    void reportProgress_shouldEmitStrictlyIncreasingRevisions() {
        ClaimedTaskContext context = context(2, 0, 4);

        context.reportProgress(3, 0, "scanning");
        context.reportProgress(3, 10, "counted");
        context.reportProgress(6, 12, "processing");

        assertEquals(3, reports.size());
        assertReport(reports.get(0), 3, 0, 5, "scanning");
        assertReport(reports.get(1), 3, 10, 6, "counted");
        assertReport(reports.get(2), 6, 12, 7, "processing");
    }

    @Test
    @DisplayName("current、total 不得为负，确定进度不得超过 total")
    void reportProgress_shouldRejectInvalidNumbers() {
        ClaimedTaskContext context = context(0, 0, 0);

        assertProgressFailure(() -> context.reportProgress(-1, 0, "negative current"));
        assertProgressFailure(() -> context.reportProgress(0, -1, "negative total"));
        assertProgressFailure(() -> context.reportProgress(2, 1, "over total"));
        assertEquals(0, reports.size());
    }

    @Test
    @DisplayName("current 与已确定 total 均不得回退或退回未知")
    void reportProgress_shouldRejectRegressions() {
        ClaimedTaskContext context = context(2, 10, 8);

        context.reportProgress(4, 10, "accepted");

        assertProgressFailure(() -> context.reportProgress(3, 10, "current regression"));
        assertProgressFailure(() -> context.reportProgress(5, 9, "total regression"));
        assertProgressFailure(() -> context.reportProgress(5, 0, "total reset"));
        assertEquals(1, reports.size());
        assertReport(reports.get(0), 4, 10, 9, "accepted");
    }

    @Test
    @DisplayName("取消令牌应在每次读取时轮询，并在请求取消后抛出稳定状态冲突")
    void cancellationToken_shouldPollAndThrowWhenCancellationIsRequested() {
        AtomicBoolean cancellationRequested = new AtomicBoolean(false);
        ClaimedTaskContext context = context(cancellationRequested::get);

        assertFalse(context.cancellationToken().isCancellationRequested());
        cancellationRequested.set(true);
        assertTrue(context.cancellationToken().isCancellationRequested());
        ServiceException failure = assertThrows(ServiceException.class,
            () -> context.cancellationToken().throwIfCancellationRequested());
        assertEquals(720003, failure.getCode());
    }

    private ClaimedTaskContext context(long current, long total, long revision) {
        return context(() -> false, current, total, revision);
    }

    private ClaimedTaskContext context(BooleanSupplier cancellationRequested) {
        return context(cancellationRequested, 0, 0, 0);
    }

    private ClaimedTaskContext context(BooleanSupplier cancellationRequested, long current, long total, long revision) {
        return new ClaimedTaskContext("btask_progress", "bexec_progress", "claim-progress", 2,
            current, total, revision, clock, reports::add, () -> true, cancellationRequested);
    }

    private void assertReport(BizTaskProgressDTO report, long current, long total, long revision,
        String message) {
        assertEquals("btask_progress", report.getTaskId());
        assertEquals("bexec_progress", report.getExecutionId());
        assertEquals("claim-progress", report.getClaimToken());
        assertEquals(current, report.getCurrent());
        assertEquals(total, report.getTotal());
        assertEquals(revision, report.getRevision());
        assertEquals(message, report.getMessage());
        assertEquals(NOW, report.getReportedAt());
    }

    private void assertProgressFailure(Runnable report) {
        ServiceException failure = assertThrows(ServiceException.class, report::run);
        assertEquals(720010, failure.getCode());
    }
}
