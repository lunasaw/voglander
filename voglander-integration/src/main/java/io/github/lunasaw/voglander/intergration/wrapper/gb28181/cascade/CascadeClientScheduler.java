package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联上级平台注册与心跳调度器。
 *
 * <p>每个 enabled 平台拥有两套独立的 {@link ScheduledFuture}（C1）：
 * <ul>
 *   <li><b>注册续期</b>：按 {@code registerExpires} 的 2/3 周期重发 REGISTER，保持注册有效；</li>
 *   <li><b>保活心跳</b>：独立的 Keepalive MESSAGE 心跳（默认 60s），仅在 ONLINE 时发送。</li>
 * </ul>
 * 注册续期与保活心跳是 GB28181 §9.1 的两个独立机制。单个平台失败不影响其他平台。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CascadeClientScheduler {

    private final CascadePlatformManager cascadePlatformManager;
    private final CascadeDeviceSupplier  cascadeDeviceSupplier;

    /** REGISTER 发出后等待成功/失败事件的最长时间，超时仍为 REGISTERING 则转 FAILED。 */
    @Value("${gateway.cascade.register-timeout-sec:" + CascadeConstant.DEFAULT_REGISTER_TIMEOUT_SEC + "}")
    private long                         registerTimeoutSeconds = CascadeConstant.DEFAULT_REGISTER_TIMEOUT_SEC;

    /** platformId → 注册续期任务 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> registerTasks  = new ConcurrentHashMap<>();
    /** platformId → 保活心跳任务 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> keepaliveTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor =
        Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "cascade-register");
            t.setDaemon(true);
            return t;
        });

    /* ------------------------------------------------------------------ */
    /* 生命周期                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * 启动调度。改用 {@link ApplicationReadyEvent} 而非 {@code @PostConstruct}：
     * 注册任务以 initialDelay=0 立即触发，必须确保此刻
     * <ul>
     *   <li>框架 {@code ClientCommandSender.INSTANCE} 已由 ApplicationContextAware 注入（否则 REGISTER 抛 "尚未初始化"）；</li>
     *   <li>{@code ServerStart}(CommandLineRunner) 已绑定 SIP 监听点；</li>
     *   <li>建表已完成。</li>
     * </ul>
     * 这些都在容器 ready 后才全部就绪，@PostConstruct 阶段均无保证。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        refreshRegistrations();
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    /* ------------------------------------------------------------------ */
    /* 公开 API                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * 批量刷新：加载所有 enabled=1 平台，为尚未调度的平台启动任务。
     * 由外部也可直接调用（测试 / 管理端手动触发）。
     */
    public void refreshRegistrations() {
        CascadePlatformDTO query = new CascadePlatformDTO();
        query.setEnabled(1);
        List<CascadePlatformDTO> platforms;
        try {
            platforms = cascadePlatformManager.getPage(query, 1, 200).getRecords();
        } catch (Exception e) {
            // 空库 + @PostConstruct 竞态：tb_cascade_platform 可能尚未建出（与 SqliteSchemaInitializer
            // 无依赖顺序保证）。缺表/查询失败时跳过本次刷新并告警，不把次生 DB 异常上抛成启动阻断。
            log.warn("级联平台加载失败（疑似建表未就绪或空库），跳过本次刷新：{}", e.getMessage());
            return;
        }

        for (CascadePlatformDTO p : platforms) {
            if (!registerTasks.containsKey(p.getPlatformId())) {
                startPlatform(p);
            }
        }
    }

    /** 运行时新增 / 重启单个平台的注册 + 保活任务（方案 4.4 / C1）。 */
    public void startPlatform(CascadePlatformDTO platform) {
        stopPlatform(platform.getId()); // 先取消旧任务（注册 + 保活）
        scheduleRegister(platform);
        scheduleKeepalive(platform);
        log.info("级联调度已启动: platformId={}", platform.getPlatformId());
    }

    /** 运行时停止平台任务（注册 + 保活），置为离线（方案 4.4）。 */
    public void stopPlatform(Long id) {
        if (id == null) return;
        CascadePlatformDTO dto = cascadePlatformManager.getById(id);
        if (dto == null) return;

        cancel(registerTasks.remove(dto.getPlatformId()));
        cancel(keepaliveTasks.remove(dto.getPlatformId()));
        cascadePlatformManager.updateRegisterStatus(id, CascadeConstant.RegisterStatus.OFFLINE);
        log.info("级联调度已停止: platformId={}", dto.getPlatformId());
    }

    /* ------------------------------------------------------------------ */
    /* 内部实现                                                             */
    /* ------------------------------------------------------------------ */

    /** 注册续期：按 registerExpires 的 2/3 周期重发 REGISTER（最短 60s），提前续期避免边界过期。 */
    private void scheduleRegister(CascadePlatformDTO platform) {
        long expires = platform.getRegisterExpires() != null ? platform.getRegisterExpires() : 3600L;
        long period = Math.max(60L, expires * 2 / 3);
        ScheduledFuture<?> future = executor.scheduleWithFixedDelay(
            () -> doRegister(platform), 0, period, TimeUnit.SECONDS);
        registerTasks.put(platform.getPlatformId(), future);
    }

    /** 保活心跳：独立的 Keepalive MESSAGE 周期任务（默认 60s）。 */
    private void scheduleKeepalive(CascadePlatformDTO platform) {
        long interval = platform.getKeepaliveInterval() != null
            ? platform.getKeepaliveInterval() : CascadeConstant.DEFAULT_KEEPALIVE_INTERVAL;
        ScheduledFuture<?> future = executor.scheduleWithFixedDelay(
            () -> doKeepalive(platform), interval, interval, TimeUnit.SECONDS);
        keepaliveTasks.put(platform.getPlatformId(), future);
    }

    private void doRegister(CascadePlatformDTO platform) {
        try {
            FromDevice from = cascadeDeviceSupplier.buildFromDevice(platform);
            ToDevice   to   = cascadeDeviceSupplier.buildToDevice(platform);
            log.info("发起级联注册: {} -> {}", platform.getLocalClientId(), platform.getPlatformId());
            cascadePlatformManager.updateRegisterStatus(platform.getId(), CascadeConstant.RegisterStatus.REGISTERING);
            String callId = ClientCommandSender.sendRegisterCommand(from, to, platform.getRegisterExpires());
            log.info("级联 REGISTER 已发送: platformId={}, localClientId={}, callId={}",
                platform.getPlatformId(), platform.getLocalClientId(), callId);
            scheduleRegisterTimeoutCheck(platform, callId);
        } catch (Exception e) {
            log.error("级联注册失败: platformId={}", platform.getPlatformId(), e);
            cascadePlatformManager.updateRegisterStatus(platform.getId(), CascadeConstant.RegisterStatus.FAILED);
        }
    }

    private void scheduleRegisterTimeoutCheck(CascadePlatformDTO platform, String callId) {
        long timeout = Math.max(0L, registerTimeoutSeconds);
        executor.schedule(() -> {
            try {
                CascadePlatformDTO current = cascadePlatformManager.getById(platform.getId());
                if (current != null && Objects.equals(current.getRegisterStatus(), CascadeConstant.RegisterStatus.REGISTERING)) {
                    log.warn("级联注册响应超时: platformId={}, localClientId={}, callId={}, timeout={}s",
                        platform.getPlatformId(), platform.getLocalClientId(), callId, timeout);
                    cascadePlatformManager.updateRegisterStatus(platform.getId(), CascadeConstant.RegisterStatus.FAILED);
                }
            } catch (Exception e) {
                log.warn("级联注册超时检查失败: platformId={}, callId={}, err={}",
                    platform.getPlatformId(), callId, e.getMessage());
            }
        }, timeout, TimeUnit.SECONDS);
    }

    /** 保活心跳：仅在平台 ONLINE 时发 Keepalive MESSAGE；离线交由注册续期任务恢复。 */
    private void doKeepalive(CascadePlatformDTO platform) {
        try {
            CascadePlatformDTO current = cascadePlatformManager.getById(platform.getId());
            if (current == null
                || !Objects.equals(current.getRegisterStatus(), CascadeConstant.RegisterStatus.ONLINE)) {
                return;
            }
            FromDevice from = cascadeDeviceSupplier.buildFromDevice(platform);
            ToDevice   to   = cascadeDeviceSupplier.buildToDevice(platform);
            ClientCommandSender.sendKeepaliveCommand(from, to, platform.getLocalClientId());
        } catch (Exception e) {
            log.warn("级联保活失败: platformId={}, err={}", platform.getPlatformId(), e.getMessage());
        }
    }

    private void cancel(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(false);
        }
    }

    void setRegisterTimeoutSeconds(long registerTimeoutSeconds) {
        this.registerTimeoutSeconds = registerTimeoutSeconds;
    }
}
