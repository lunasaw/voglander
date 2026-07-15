package io.github.lunasaw.voglander.service.task;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.common.anno.TechnicalScheduler;
import io.github.lunasaw.voglander.manager.service.BizTaskEventRetentionService;
import lombok.extern.slf4j.Slf4j;

/**
 * Maintenance scheduler for event retention. It does not create or dispatch business tasks.
 */
@Slf4j
@Component
@TechnicalScheduler(category = TechnicalScheduler.Category.MAINTENANCE)
public class BusinessTaskEventRetentionScheduler {

    private final BizTaskEventRetentionService retentionService;
    private final BusinessTaskProperties properties;
    private final Clock clock;

    @Autowired
    public BusinessTaskEventRetentionScheduler(BizTaskEventRetentionService retentionService,
        BusinessTaskProperties properties) {
        this(retentionService, properties, Clock.systemDefaultZone());
    }

    BusinessTaskEventRetentionScheduler(BizTaskEventRetentionService retentionService,
        BusinessTaskProperties properties, Clock clock) {
        this.retentionService = retentionService;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${voglander.task.event-cleanup-interval-ms:3600000}")
    public void cleanup() {
        cleanup(LocalDateTime.now(clock));
    }

    int cleanup(LocalDateTime now) {
        if (!properties.isEventRetentionEnabled()) {
            return 0;
        }
        LocalDateTime cutoff = now.minusDays(properties.getEventRetentionDays());
        int deleted = retentionService.deleteBefore(cutoff, properties.getEventCleanupBatchSize());
        if (deleted > 0) {
            log.info("Business-task event retention cleanup removed {} rows before {}", deleted, cutoff);
        }
        return deleted;
    }
}
