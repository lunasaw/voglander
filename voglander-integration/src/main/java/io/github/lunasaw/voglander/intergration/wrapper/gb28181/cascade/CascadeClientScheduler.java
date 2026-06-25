package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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

    /** platformId → ��册续期任务 */
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

    @PostConstruct
    public void init() {
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
            ClientCommandSender.sendRegisterCommand(from, to, platform.getRegisterExpires());
        } catch (Exception e) {
            log.error("级联注册失败: platformId={}", platform.getPlatformId(), e);
            cascadePlatformManager.updateRegisterStatus(platform.getId(), CascadeConstant.RegisterStatus.FAILED);
        }
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
}
