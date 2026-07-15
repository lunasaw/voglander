package io.github.lunasaw.voglander.service.task;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.manager.manager.TaskTerminalStateEvaluator;

/** Materializes one execution fact and advances its task cursor in the caller's transaction. */
final class TaskScheduleMaterializer implements DueTaskMaterializer {

    private final TaskSchedulePlanner planner;
    private final BizTaskExecutionManager executionManager;
    private final BizTaskManager taskManager;
    private final Clock clock;
    private final int defaultMaxAttempts;
    private final int catchupBatchSize;
    private final Supplier<String> executionIdSupplier;
    private final TaskTerminalStateEvaluator terminalStateEvaluator;

    TaskScheduleMaterializer(TaskSchedulePlanner planner, BizTaskExecutionManager executionManager,
        BizTaskManager taskManager, Clock clock, int defaultMaxAttempts, Supplier<String> executionIdSupplier) {
        this(planner, executionManager, taskManager, clock, defaultMaxAttempts,
            TaskConstant.DEFAULT_CATCHUP_BATCH, executionIdSupplier);
    }

    TaskScheduleMaterializer(TaskSchedulePlanner planner, BizTaskExecutionManager executionManager,
        BizTaskManager taskManager, Clock clock, int defaultMaxAttempts, int catchupBatchSize,
        Supplier<String> executionIdSupplier) {
        this(planner, executionManager, taskManager, clock, defaultMaxAttempts, catchupBatchSize,
            executionIdSupplier, new TaskTerminalStateEvaluator());
    }

    TaskScheduleMaterializer(TaskSchedulePlanner planner, BizTaskExecutionManager executionManager,
        BizTaskManager taskManager, Clock clock, int defaultMaxAttempts, int catchupBatchSize,
        Supplier<String> executionIdSupplier, TaskTerminalStateEvaluator terminalStateEvaluator) {
        this.planner = Objects.requireNonNull(planner, "planner");
        this.executionManager = Objects.requireNonNull(executionManager, "executionManager");
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (defaultMaxAttempts <= 0 || catchupBatchSize <= 0 || catchupBatchSize > 1000) {
            throw new IllegalArgumentException("Task attempt or catch-up batch configuration is invalid");
        }
        this.defaultMaxAttempts = defaultMaxAttempts;
        this.catchupBatchSize = catchupBatchSize;
        this.executionIdSupplier = Objects.requireNonNull(executionIdSupplier, "executionIdSupplier");
        this.terminalStateEvaluator = Objects.requireNonNull(terminalStateEvaluator, "terminalStateEvaluator");
    }

    static Supplier<String> randomExecutionIdSupplier() {
        return () -> TaskConstant.EXECUTION_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void materialize(BizTaskDTO task) {
        if (task == null || task.getVersion() == null || task.getVersion() < 0) {
            throw invalid("Task version is invalid at the schedule cursor boundary");
        }
        LocalDateTime batchTime = LocalDateTime.now(clock);
        int materialized = 0;
        while (materialized < catchupBatchSize && task.getNextPlanTime() != null
            && !task.getNextPlanTime().isAfter(batchTime)) {
            TaskSchedulePoint point = planner.plan(task);
            BizTaskExecutionDTO execution = execution(task, point, batchTime);
            boolean inserted = executionManager.insertIfAbsent(execution);
            if (point.missed() && inserted && !executionManager.markMissed(missed(execution, batchTime))) {
                throw new ServiceException(ServiceExceptionEnum.TASK_STATE_CONFLICT)
                    .setDetailMessage("Task MISSED execution CAS conflict");
            }
            boolean advanced = point.missed()
                ? taskManager.advanceScheduleCursor(task.getTaskId(), task.getVersion(), point.nextPlanTime(),
                    "RUNNING", batchTime, true)
                : taskManager.advanceScheduleCursor(task.getTaskId(), task.getVersion(), point.nextPlanTime(),
                    "RUNNING", batchTime);
            if (!advanced) {
                throw new ServiceException(ServiceExceptionEnum.TASK_STATE_CONFLICT)
                    .setDetailMessage("Task schedule cursor CAS conflict");
            }
            advanceLocalCursor(task, point);
            markNaturalTerminal(task, batchTime);
            materialized++;
        }
    }

    private void markNaturalTerminal(BizTaskDTO task, LocalDateTime completedTime) {
        String terminalState = terminalStateEvaluator.resolve(task);
        if (terminalState == null) {
            return;
        }
        if (!taskManager.markNaturalTerminal(task.getTaskId(), task.getVersion(), terminalState, completedTime)) {
            throw new ServiceException(ServiceExceptionEnum.TASK_STATE_CONFLICT)
                .setDetailMessage("Task natural terminal CAS conflict");
        }
        task.setState(terminalState);
        task.setCompletedTime(completedTime);
        incrementLocalVersion(task);
    }

    private BizTaskExecutionDTO missed(BizTaskExecutionDTO execution, LocalDateTime finishedAt) {
        BizTaskExecutionDTO command = new BizTaskExecutionDTO();
        command.setExecutionId(execution.getExecutionId());
        command.setVersion(execution.getVersion());
        command.setFinishedAt(finishedAt);
        command.setUpdateTime(finishedAt);
        command.setFailureCode("ALLOWED_DELAY_EXCEEDED");
        command.setFailureMessage("planned point exceeded its allowed delay");
        return command;
    }

    private void advanceLocalCursor(BizTaskDTO task, TaskSchedulePoint point) {
        task.setNextPlanTime(point.nextPlanTime());
        incrementLocalVersion(task);
        task.setState("RUNNING");
        if (point.missed()) {
            int missedCount = task.getMissedCount() == null ? 0 : task.getMissedCount();
            task.setMissedCount(Math.addExact(missedCount, 1));
        }
    }

    private void incrementLocalVersion(BizTaskDTO task) {
        try {
            task.setVersion(Math.addExact(task.getVersion(), 1));
        } catch (ArithmeticException exception) {
            throw invalid("Task version overflow at the schedule cursor boundary");
        }
    }

    private BizTaskExecutionDTO execution(BizTaskDTO task, TaskSchedulePoint point, LocalDateTime now) {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId(executionIdSupplier.get());
        execution.setTaskId(task.getTaskId());
        execution.setScheduleVersion(point.scheduleVersion());
        execution.setPlannedAt(point.plannedAt());
        execution.setDeadlineAt(point.deadlineAt());
        execution.setState("PENDING");
        execution.setAttemptCount(0);
        execution.setMaxAttempts(defaultMaxAttempts);
        execution.setRetryable(Boolean.FALSE);
        execution.setProgressCurrent(0L);
        execution.setProgressTotal(0L);
        execution.setProgressRevision(0L);
        execution.setVersion(0);
        execution.setCreateTime(now);
        execution.setUpdateTime(now);
        return execution;
    }

    private ServiceException invalid(String detailMessage) {
        return new ServiceException(ServiceExceptionEnum.TASK_SCHEDULE_INVALID).setDetailMessage(detailMessage);
    }
}
