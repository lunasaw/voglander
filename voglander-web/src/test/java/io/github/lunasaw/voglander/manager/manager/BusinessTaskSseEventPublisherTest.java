package io.github.lunasaw.voglander.manager.manager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent;

class BusinessTaskSseEventPublisherTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void eventIsInvisibleUntilCommit() {
        ApplicationEventPublisher applicationEvents = mock(ApplicationEventPublisher.class);
        BusinessTaskSseEventPublisher publisher = new BusinessTaskSseEventPublisher(applicationEvents);
        BusinessTaskSseEvent event = new BusinessTaskSseEvent();

        TransactionSynchronizationManager.initSynchronization();
        publisher.publish(event);
        verifyNoInteractions(applicationEvents);

        TransactionSynchronizationUtils.triggerAfterCommit();

        verify(applicationEvents).publishEvent(event);
    }

    @Test
    void rolledBackSynchronizationDoesNotPublish() {
        ApplicationEventPublisher applicationEvents = mock(ApplicationEventPublisher.class);
        BusinessTaskSseEventPublisher publisher = new BusinessTaskSseEventPublisher(applicationEvents);

        TransactionSynchronizationManager.initSynchronization();
        publisher.publish(new BusinessTaskSseEvent());
        TransactionSynchronizationUtils.invokeAfterCompletion(
            TransactionSynchronizationManager.getSynchronizations(),
            org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);

        verifyNoInteractions(applicationEvents);
    }

    @Test
    void duplicateTerminalHintIsPublishedOnce() {
        ApplicationEventPublisher applicationEvents = mock(ApplicationEventPublisher.class);
        BusinessTaskSseEventPublisher publisher = new BusinessTaskSseEventPublisher(applicationEvents);
        BusinessTaskSseEvent event = new BusinessTaskSseEvent("business.task.execution-state", "btask_1",
            "bexec_1", "IMAGE_COLLECTION", "COMPLETED", "SUCCEEDED", "SUCCEEDED", null, null, null,
            null, null, 1, 1_700_000_000_000L);

        publisher.publish(event);
        publisher.publish(event);

        verify(applicationEvents, org.mockito.Mockito.times(1)).publishEvent(event);
    }
}
