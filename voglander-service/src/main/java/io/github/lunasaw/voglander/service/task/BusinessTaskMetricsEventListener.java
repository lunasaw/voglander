package io.github.lunasaw.voglander.service.task;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent;
import lombok.RequiredArgsConstructor;

/** Records bounded metrics from the same committed refresh hints sent to clients. */
@Component
@RequiredArgsConstructor
public class BusinessTaskMetricsEventListener {

    private final BusinessTaskMetrics metrics;

    @EventListener
    public void onCommitted(BusinessTaskSseEvent event) {
        if (event == null) {
            return;
        }
        if (event.getTaskState() != null) {
            metrics.recordTask(event.getTaskType(), event.getTaskState());
        }
        if (event.getExecutionState() != null) {
            metrics.recordExecution(event.getTaskType(), event.getExecutionState());
        }
        if (TaskConstant.SSE_TASK_PROGRESS.equals(event.getTopic())) {
            metrics.recordProgressWrite(event.getTaskType(), false);
        }
    }
}
