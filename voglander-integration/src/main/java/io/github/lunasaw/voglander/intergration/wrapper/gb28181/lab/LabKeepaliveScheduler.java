package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议验证台周期心跳调度器（Lab 模式专用）。
 * <p>
 * 维护单个 {@link ScheduledFuture}：开关 {@link #setAuto(boolean, int)} 启用时按指定间隔周期调用
 * {@link LabSipClient#keepalive()}，关闭时取消任务。重复启用先取消旧任务再以新间隔重排，
 * 语义与 {@code CascadeClientScheduler} 的 per-平台调度一致，但 Lab 只有一个设备 UA 身份故只需单任务。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabKeepaliveScheduler {

    /** 间隔非正时回落的默认值（秒）。 */
    private static final int DEFAULT_INTERVAL_SEC = 30;

    private final LabSipClient             labSipClient;
    private final ScheduledExecutorService executor;

    private volatile ScheduledFuture<?>    task;
    private volatile boolean               enabled;
    private volatile int                   intervalSec;

    /** Spring 注入：内部自建守护线程调度器。 */
    @Autowired
    public LabKeepaliveScheduler(LabSipClient labSipClient) {
        this(labSipClient, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lab-keepalive");
            t.setDaemon(true);
            return t;
        }));
    }

    /** 可注入 executor 构造，便于单元测试。 */
    LabKeepaliveScheduler(LabSipClient labSipClient, ScheduledExecutorService executor) {
        this.labSipClient = labSipClient;
        this.executor = executor;
    }

    /**
     * 开关周期心跳。
     *
     * @param autoEnabled true 启用周期心跳，false 关闭
     * @param intervalSec 心跳间隔（秒）；≤0 时回落 {@value #DEFAULT_INTERVAL_SEC}s。关闭时忽略。
     */
    public synchronized void setAuto(boolean autoEnabled, int intervalSec) {
        cancelTask();
        if (!autoEnabled) {
            this.enabled = false;
            this.intervalSec = 0;
            log.info("Lab 周期心跳已关闭");
            return;
        }
        int interval = intervalSec > 0 ? intervalSec : DEFAULT_INTERVAL_SEC;
        this.task = executor.scheduleWithFixedDelay(this::doKeepalive, 0L, interval, TimeUnit.SECONDS);
        this.enabled = true;
        this.intervalSec = interval;
        log.info("Lab 周期心跳已启用, intervalSec={}", interval);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getIntervalSec() {
        return intervalSec;
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    private void cancelTask() {
        ScheduledFuture<?> old = this.task;
        if (old != null) {
            old.cancel(false);
            this.task = null;
        }
    }

    private void doKeepalive() {
        try {
            labSipClient.keepalive();
        } catch (Exception e) {
            log.error("Lab 周期心跳发送失败", e);
        }
    }
}
