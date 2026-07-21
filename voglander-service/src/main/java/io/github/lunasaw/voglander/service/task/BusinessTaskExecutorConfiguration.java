package io.github.lunasaw.voglander.service.task;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.github.lunasaw.voglander.common.constant.task.TaskConstant;

/** Creates the isolated bounded executor used only by durable business-task workers. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BusinessTaskProperties.class)
public class BusinessTaskExecutorConfiguration {

    /** Queue saturation is reported to the dispatcher and never runs work on its scanning thread. */
    @Bean(name = TaskConstant.EXECUTOR_BEAN_NAME)
    @ConditionalOnProperty(prefix = "voglander.task", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    public ThreadPoolTaskExecutor businessTaskExecutor(BusinessTaskProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getExecutorCoreSize());
        executor.setMaxPoolSize(properties.getExecutorMaxSize());
        executor.setQueueCapacity(properties.getExecutorQueueCapacity());
        executor.setThreadNamePrefix("business-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.getExecutorShutdownAwaitSeconds());
        return executor;
    }
}
