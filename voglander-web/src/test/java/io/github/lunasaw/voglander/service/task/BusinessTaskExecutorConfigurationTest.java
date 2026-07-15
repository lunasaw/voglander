package io.github.lunasaw.voglander.service.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.github.lunasaw.voglander.common.constant.task.TaskConstant;

@DisplayName("Dedicated business-task executor configuration")
class BusinessTaskExecutorConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(BusinessTaskExecutorConfiguration.class);

    @Test
    @DisplayName("应创建命名隔离、有界队列且使用 Abort 拒绝策略的执行器")
    void configuration_shouldCreateDedicatedBoundedAbortExecutor() {
        contextRunner.withPropertyValues(
            "voglander.task.executor-core-size=1",
            "voglander.task.executor-max-size=2",
            "voglander.task.executor-queue-capacity=3",
            "voglander.task.progress-min-interval-ms=250")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean(TaskConstant.EXECUTOR_BEAN_NAME);
                ThreadPoolTaskExecutor executor = context.getBean(TaskConstant.EXECUTOR_BEAN_NAME,
                    ThreadPoolTaskExecutor.class);
                ThreadPoolExecutor delegate = executor.getThreadPoolExecutor();
                assertThat(executor.getCorePoolSize()).isEqualTo(1);
                assertThat(executor.getMaxPoolSize()).isEqualTo(2);
                assertThat(delegate.getQueue().remainingCapacity()).isEqualTo(3);
                assertThat(delegate.getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
                assertThat(executor.getThreadNamePrefix()).isEqualTo("business-task-");
                assertThat(context.getBean(BusinessTaskProperties.class).getProgressMinIntervalMs())
                    .isEqualTo(250);
            });
    }

    @Test
    @DisplayName("关闭时应等待运行任务并排空已入队的 executionId 工作")
    void shutdown_shouldFinishRunningAndQueuedWork() throws Exception {
        BusinessTaskProperties properties = new BusinessTaskProperties();
        properties.setExecutorCoreSize(1);
        properties.setExecutorMaxSize(1);
        properties.setExecutorQueueCapacity(1);
        properties.setExecutorShutdownAwaitSeconds(3);
        ThreadPoolTaskExecutor executor =
            new BusinessTaskExecutorConfiguration().businessTaskExecutor(properties);
        executor.initialize();
        CountDownLatch runningStarted = new CountDownLatch(1);
        CountDownLatch releaseRunning = new CountDownLatch(1);
        CountDownLatch runningFinished = new CountDownLatch(1);
        CountDownLatch queuedFinished = new CountDownLatch(1);
        CountDownLatch shutdownStarted = new CountDownLatch(1);
        CountDownLatch shutdownReturned = new CountDownLatch(1);
        Thread shutdownThread = null;
        try {
            executor.execute(() -> {
                runningStarted.countDown();
                try {
                    releaseRunning.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    runningFinished.countDown();
                }
            });
            assertThat(runningStarted.await(2, TimeUnit.SECONDS)).isTrue();
            executor.execute(queuedFinished::countDown);

            shutdownThread = new Thread(() -> {
                shutdownStarted.countDown();
                executor.shutdown();
                shutdownReturned.countDown();
            }, "business-task-shutdown-test");
            shutdownThread.start();
            assertThat(shutdownStarted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(shutdownReturned.await(200, TimeUnit.MILLISECONDS)).isFalse();
            releaseRunning.countDown();
            shutdownThread.join(TimeUnit.SECONDS.toMillis(4));

            assertThat(shutdownThread.isAlive()).isFalse();
            assertThat(shutdownReturned.getCount()).isZero();
            assertThat(runningFinished.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(queuedFinished.await(1, TimeUnit.SECONDS)).isTrue();
        } finally {
            releaseRunning.countDown();
            executor.shutdown();
            if (shutdownThread != null) {
                shutdownThread.join(TimeUnit.SECONDS.toMillis(1));
            }
        }
    }

    @ParameterizedTest(name = "core={0}, max={1}, queue={2}, shutdownAwait={3}, progressInterval={4}")
    @MethodSource("invalidSettings")
    @DisplayName("非正容量或 core 大于 max 应在启动期失败")
    void configuration_shouldRejectInvalidSettings(int coreSize, int maxSize, int queueCapacity,
        int shutdownAwaitSeconds, long progressMinIntervalMs) {
        contextRunner.withPropertyValues(
            "voglander.task.executor-core-size=" + coreSize,
            "voglander.task.executor-max-size=" + maxSize,
            "voglander.task.executor-queue-capacity=" + queueCapacity,
            "voglander.task.executor-shutdown-await-seconds=" + shutdownAwaitSeconds,
            "voglander.task.progress-min-interval-ms=" + progressMinIntervalMs)
            .run(context -> assertThat(context).hasFailed());
    }

    private static Stream<Arguments> invalidSettings() {
        return Stream.of(
            Arguments.of(0, 2, 3, 3, 500),
            Arguments.of(3, 2, 3, 3, 500),
            Arguments.of(1, 2, 0, 3, 500),
            Arguments.of(1, 2, 3, 0, 500),
            Arguments.of(1, 2, 3, 3, 0));
    }
}
