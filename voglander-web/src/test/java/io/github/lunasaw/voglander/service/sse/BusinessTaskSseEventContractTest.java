package io.github.lunasaw.voglander.service.sse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent;

class BusinessTaskSseEventContractTest {

    @Test
    void payloadContainsOnlyStableIdentifiersCodesAndProgressFacts() {
        BusinessTaskSseEvent event = new BusinessTaskSseEvent(TaskConstant.SSE_TASK_PROGRESS, "btask_1",
            "bexec_1", "IMAGE_COLLECTION", "RUNNING", "RUNNING", "PROGRESS", "TASK_FAILED", 10L, 100L,
            2L, 1, 1, 1_700_000_000_000L);

        Map<String, Object> data = event.toData();

        assertEquals("btask_1", data.get("taskId"));
        assertEquals(1_700_000_000_000L, data.get("timestamp"));
        assertFalse(data.containsKey("payload"));
        assertFalse(data.containsKey("message"));
        assertFalse(data.containsKey("path"));
        assertFalse(data.containsKey("secret"));
        assertFalse(data.containsKey("stack"));
    }

    @Test
    void listenerBridgesEventToSseBusWithoutExpandingThePayload() {
        SseEventBus bus = mock(SseEventBus.class);
        BusinessTaskSseEventListener listener = new BusinessTaskSseEventListener(bus);
        BusinessTaskSseEvent event = new BusinessTaskSseEvent(TaskConstant.SSE_TASK_STATE, "btask_1", null,
            "IMAGE_COLLECTION", "RUNNING", null, "STARTED", null, null, null, null, null, null,
            1_700_000_000_000L);

        listener.onCommitted(event);

        var captor = org.mockito.ArgumentCaptor.forClass(SseEvent.class);
        verify(bus).publish(captor.capture());
        assertEquals(TaskConstant.SSE_TASK_STATE, captor.getValue().getTopic());
        Map<?, ?> data = (Map<?, ?>)captor.getValue().getData();
        assertEquals("btask_1", data.get("taskId"));
        assertFalse(data.containsKey("payload"));
    }
}
