package io.github.lunasaw.voglander.manager.manager;

import io.github.lunasaw.voglander.common.enums.task.TaskStateEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;

/** Resolves the natural terminal state after every scheduled execution fact is terminal. */
public class TaskTerminalStateEvaluator {

    /**
     * Returns the terminal state, or {@code null} while schedule or execution facts remain incomplete.
     */
    public String resolve(BizTaskDTO task) {
        if (task == null || task.getNextPlanTime() != null) {
            return null;
        }
        long plannedCount = nonNull(task.getPlannedCount());
        long successCount = nonNull(task.getSuccessCount());
        long failedCount = nonNull(task.getFailedCount());
        long missedCount = nonNull(task.getMissedCount());
        long cancelledCount = nonNull(task.getCancelledCount());
        long terminalCount = successCount + failedCount + missedCount + cancelledCount;
        if (plannedCount <= 0 || terminalCount != plannedCount) {
            return null;
        }
        if (TaskStateEnum.CANCELLING.name().equals(task.getState())) {
            return TaskStateEnum.CANCELLED.name();
        }
        if (!TaskStateEnum.RUNNING.name().equals(task.getState())) {
            return null;
        }
        if (successCount == plannedCount) {
            return TaskStateEnum.COMPLETED.name();
        }
        return successCount > 0 ? TaskStateEnum.PARTIAL_COMPLETED.name() : TaskStateEnum.FAILED.name();
    }

    private long nonNull(Integer value) {
        return value == null ? 0L : value.longValue();
    }
}
