package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议验证台订阅推送调度（Lab 模式专用）。
 * <p>
 * <strong>非 GB28181 标准实现，仅 lab 自环联调便利</strong>（CLAUDE.md 协议合规铁律）。
 * 平台向 lab 设备发起 SUBSCRIBE 后，由本服务按订阅类型登记周期推送：
 * 位置按 interval 周期推 GPS，目录立即推一条 UPDATE，告警可手动触发。
 * expires 到期自动停推。推送规则单点收口于此 + {@link LabSipClient}。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabSubscribePushService {

    /** 位置推送默认间隔（秒），interval 非正时回落。 */
    private static final int               DEFAULT_POSITION_INTERVAL_SEC = 5;

    private final LabSipClient             labSipClient;

    /** 订阅类型 → 周期推送任务。 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "lab-subscribe-push");
        t.setDaemon(true);
        return t;
    });

    /**
     * 位置订阅：按 interval 周期推送模拟 GPS，expires 后停推。
     * <p>
     * {@code expires <= 0} 为标准退订语义（RFC 6665 / GB28181-2022 §9.11：同 dialog 发 Expires:0 的
     * SUBSCRIBE 表示终止订阅），此时仅取消周期任务、停止推送，不再重排。
     * </p>
     */
    public void startPositionPush(String platformId, Integer expires, Integer interval) {
        if (isUnsubscribe(expires)) {
            cancel("MOBILE_POSITION");
            log.info("Lab 收到位置退订(expires<=0)，停止推送, platformId={}", platformId);
            return;
        }
        int sec = interval != null && interval > 0 ? interval : DEFAULT_POSITION_INTERVAL_SEC;
        scheduleRepeating("MOBILE_POSITION", sec, expires, () -> labSipClient.pushMobilePosition());
        log.info("Lab 位置订阅推送已登记, platformId={}, intervalSec={}, expires={}", platformId, sec, expires);
    }

    /**
     * 目录订阅：立即推一条 UPDATE 目录变更（lab 简化，不做周期）。
     * <p>
     * {@code expires <= 0} 为退订语义，不推送、直接返回。
     * </p>
     */
    public void startCatalogPush(String platformId, Integer expires) {
        if (isUnsubscribe(expires)) {
            log.info("Lab 收到目录退订(expires<=0)，跳过推送, platformId={}", platformId);
            return;
        }
        try {
            labSipClient.pushCatalogChange("UPDATE");
        } catch (Exception e) {
            log.warn("Lab 目录变更推送失败: {}", e.getMessage());
        }
        log.info("Lab 目录订阅已登记并推送一条变更, platformId={}, expires={}", platformId, expires);
    }

    /**
     * 告警订阅：登记，由 {@link #triggerAlarm()} 或测试手动触发推送（lab 简化，不周期）。
     * <p>
     * {@code expires <= 0} 为退订语义，仅记录、不再接受手动触发的语义无副作用，此处直接返回。
     * </p>
     */
    public void startAlarmPush(String platformId, Integer expires) {
        if (isUnsubscribe(expires)) {
            log.info("Lab 收到告警退订(expires<=0), platformId={}", platformId);
            return;
        }
        log.info("Lab 告警订阅已登记（手动触发推送）, platformId={}, expires={}", platformId, expires);
    }

    /**
     * 标准退订判定：Expires 头为 0（或缺省/非正）即终止订阅。
     */
    private boolean isUnsubscribe(Integer expires) {
        return expires == null || expires <= 0;
    }

    /**
     * 手动触发一条模拟告警推送（供联调/测试）。
     */
    public String triggerAlarm() {
        return labSipClient.pushAlarm(2, 1, null);
    }

    private synchronized void scheduleRepeating(String key, int intervalSec, Integer expires, Runnable push) {
        cancel(key);
        // 防御：expires<=0 不应重排（入口已拦退订，此处兜底，避免永不停止的周期任务）
        if (isUnsubscribe(expires)) {
            return;
        }
        ScheduledFuture<?> task = executor.scheduleWithFixedDelay(() -> {
            try {
                push.run();
            } catch (Exception e) {
                log.warn("Lab 订阅推送异常, key={}: {}", key, e.getMessage());
            }
        }, 0L, intervalSec, TimeUnit.SECONDS);
        tasks.put(key, task);
        // expires 到期停推
        executor.schedule(() -> cancel(key), expires, TimeUnit.SECONDS);
    }

    private synchronized void cancel(String key) {
        ScheduledFuture<?> old = tasks.remove(key);
        if (old != null) {
            old.cancel(false);
        }
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }
}
