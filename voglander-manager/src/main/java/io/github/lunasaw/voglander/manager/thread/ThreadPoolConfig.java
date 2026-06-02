package io.github.lunasaw.voglander.manager.thread;

import io.github.lunasaw.voglander.common.util.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author ruoyi
 **/
@Configuration
@EnableAsync
public class ThreadPoolConfig
{
    // 核心线程池大小
    private final int corePoolSize     = 50;

    // 最大可创建的线程数
    private final int maxPoolSize      = 200;

    // 队列最大长度
    private final int queueCapacity    = 1000;

    // 线程池维护线程所允许的空闲时间
    private final int keepAliveSeconds = 300;

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 线程池对拒绝任务(无线程可用)的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * SIP 网关事件 Ingress 执行器（Phase 4）。
     * <p>
     * 供 {@code VoglanderBusinessNotifier#notify} 使用，仅做轻量翻译 + 分片路由，
     * 立即归还 SIP 线程。核心 2~4 线程 + 大队列，<strong>禁用 CallerRunsPolicy</strong>，
     * 满载时丢弃冗余 Keepalive（保 Register/Invite/Offline）。
     * </p>
     */
    @Bean(name = "sipNotifierExecutor")
    public ThreadPoolTaskExecutor sipNotifierExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10000);
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("sip-ingress-");
        // Phase 4: 禁用 CallerRunsPolicy，满载时丢弃（由 ShardDispatcher 处理）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        return executor;
    }

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService()
    {
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy())
        {
            @Override
            protected void afterExecute(Runnable r, Throwable t)
            {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }
}
