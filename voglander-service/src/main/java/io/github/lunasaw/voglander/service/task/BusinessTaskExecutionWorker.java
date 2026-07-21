package io.github.lunasaw.voglander.service.task;

/** Worker boundary that reloads and processes one durable execution by stable identity. */
@FunctionalInterface
interface BusinessTaskExecutionWorker {

    void execute(String executionId);
}
