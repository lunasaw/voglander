package io.github.lunasaw.voglander.service.task;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskExecutionManager;

/** Scans durable runnable facts and submits stable execution identities to the bounded worker pool. */
final class BizTaskDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BizTaskDispatcher.class);

    private final BizTaskExecutionManager executionManager;
    private final Executor executor;
    private final BusinessTaskExecutionWorker worker;
    private final Clock clock;
    private final int scanBatchSize;

    BizTaskDispatcher(BizTaskExecutionManager executionManager, Executor executor,
        BusinessTaskExecutionWorker worker, Clock clock, int scanBatchSize) {
        this.executionManager = Objects.requireNonNull(executionManager, "executionManager");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.worker = Objects.requireNonNull(worker, "worker");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (scanBatchSize <= 0 || scanBatchSize > 1000) {
            throw new IllegalArgumentException("Task dispatch scan batch size is invalid");
        }
        this.scanBatchSize = scanBatchSize;
    }

    void dispatchRunnableExecutions() {
        LocalDateTime scanTime = LocalDateTime.now(clock);
        List<BizTaskExecutionDTO> executions = executionManager.findRunnable(scanTime, scanBatchSize);
        for (BizTaskExecutionDTO execution : executions) {
            if (execution == null || !StringUtils.hasText(execution.getExecutionId())) {
                continue;
            }
            try {
                executor.execute(new ExecutionDispatch(execution.getExecutionId(), worker));
            } catch (RejectedExecutionException exception) {
                LOGGER.warn("Business-task executor saturated; durable execution remains runnable: executionId={}",
                    execution.getExecutionId());
                break;
            }
        }
    }

    private static final class ExecutionDispatch implements Runnable {
        private final String executionId;
        private final BusinessTaskExecutionWorker worker;

        private ExecutionDispatch(String executionId, BusinessTaskExecutionWorker worker) {
            this.executionId = executionId;
            this.worker = worker;
        }

        @Override
        public void run() {
            worker.execute(executionId);
        }
    }
}
