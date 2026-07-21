package io.github.lunasaw.voglander.manager.manager;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent;
import lombok.RequiredArgsConstructor;

/** Publishes task refresh hints only after the surrounding database transaction commits. */
@Component
@RequiredArgsConstructor
public class BusinessTaskSseEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ConcurrentHashMap<String, Boolean> publishedKeys = new ConcurrentHashMap<>();

    public void publish(BusinessTaskSseEvent event) {
        if (event == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishCommitted(event);
                }
            });
            return;
        }
        // Non-transactional maintenance paths have no pending database work to wait for.
        publishCommitted(event);
    }

    private void publishCommitted(BusinessTaskSseEvent event) {
        String key = String.valueOf(event.getTopic()) + '|' + event.getTaskId() + '|'
            + event.getExecutionId() + '|' + event.getEventType() + '|' + event.getTaskState() + '|'
            + event.getExecutionState() + '|' + event.getFailureCode() + '|' + event.getProgressRevision()
            + '|' + event.getTimestamp();
        if (publishedKeys.putIfAbsent(key, Boolean.TRUE) == null) {
            applicationEventPublisher.publishEvent(event);
            if (publishedKeys.size() > 10_000) {
                publishedKeys.keySet().stream().limit(1_000).forEach(publishedKeys::remove);
            }
        }
    }
}
