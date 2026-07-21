package io.github.lunasaw.voglander.manager.service;

import java.time.LocalDateTime;

/** Maintenance boundary for bounded deletion of expired append-only task events. */
public interface BizTaskEventRetentionService {
    int deleteBefore(LocalDateTime cutoff, int batchSize);
}
