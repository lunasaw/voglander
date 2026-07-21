package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.manager.service.BizTaskEventRetentionService;

class BusinessTaskEventRetentionSchedulerTest {

    @Test
    void cleanupUsesConfiguredCutoffAndIsMaintenanceOnly() {
        BizTaskEventRetentionService retention = org.mockito.Mockito.mock(BizTaskEventRetentionService.class);
        org.mockito.Mockito.when(retention.deleteBefore(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(1000))).thenReturn(3);
        BusinessTaskProperties properties = new BusinessTaskProperties();
        properties.setEventRetentionDays(7);
        properties.setEventCleanupBatchSize(1000);
        BusinessTaskEventRetentionScheduler scheduler = new BusinessTaskEventRetentionScheduler(retention,
            properties, Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC));

        assertEquals(3, scheduler.cleanup(java.time.LocalDateTime.of(2026, 7, 15, 0, 0)));
        verify(retention).deleteBefore(java.time.LocalDateTime.of(2026, 7, 8, 0, 0), 1000);
    }

    @Test
    void disabledRetentionDoesNotDelete() {
        BizTaskEventRetentionService retention = org.mockito.Mockito.mock(BizTaskEventRetentionService.class);
        BusinessTaskProperties properties = new BusinessTaskProperties();
        properties.setEventRetentionEnabled(false);
        BusinessTaskEventRetentionScheduler scheduler = new BusinessTaskEventRetentionScheduler(retention,
            properties);

        assertEquals(0, scheduler.cleanup(java.time.LocalDateTime.now()));
        org.mockito.Mockito.verifyNoInteractions(retention);
    }
}
