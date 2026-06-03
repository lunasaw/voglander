package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import java.util.List;
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
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联上级平台注册与心跳调度器。
 *
 * <p>每个 enabled 平台拥有独立的 {@link ScheduledFuture}，
 * 单个平台失败不影响其他平台（方案 4.2 / 4.4）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CascadeClientScheduler {

    private final CascadePlatformManager cascadePlatformManager;
    private final CascadeDeviceSupplier  cascadeDeviceSupplier;

    /** platformId → ScheduledFuture */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

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
        List<CascadePlatformDTO> platforms =
            cascadePlatformManager.getPage(query, 1, 200).getRecords();

        for (CascadePlatformDTO p : platforms) {
            if (!tasks.containsKey(p.getPlatformId())) {
                startPlatform(p);
            }
        }
    }

    /** 运行时新增 / 重启单个平台的注册任务（方案 4.4）。 */
    public void startPlatform(CascadePlatformDTO platform) {
        stopPlatform(platform.getId()); // 先取消旧任务
        long interval = platform.getKeepaliveInterval() != null
            ? platform.getKeepaliveInterval() : 55L;

        ScheduledFuture<?> future = executor.scheduleWithFixedDelay(
            () -> doRegister(platform),
            0, interval, TimeUnit.SECONDS
        );
        tasks.put(platform.getPlatformId(), future);
        log.info("级联调度已启动: platformId={}", platform.getPlatformId());
    }

    /** 运行时停止平台任务，置为离线（方案 4.4）。 */
    public void stopPlatform(Long id) {
        if (id == null) return;
        CascadePlatformDTO dto = cascadePlatformManager.getById(id);
        if (dto == null) return;

        ScheduledFuture<?> old = tasks.remove(dto.getPlatformId());
        if (old != null) old.cancel(false);
        cascadePlatformManager.updateRegisterStatus(id, 0); // OFFLINE
        log.info("级联调度已停止: platformId={}", dto.getPlatformId());
    }

    /* ------------------------------------------------------------------ */
    /* 内部实现                                                             */
    /* ------------------------------------------------------------------ */

    private void doRegister(CascadePlatformDTO platform) {
        try {
            FromDevice from = cascadeDeviceSupplier.buildFromDevice(platform);
            ToDevice   to   = cascadeDeviceSupplier.buildToDevice(platform);
            log.info("发起级联注册: {} → {}", platform.getLocalClientId(), platform.getPlatformId());
            cascadePlatformManager.updateRegisterStatus(platform.getId(), 2); // REGISTERING
            ClientCommandSender.sendRegisterCommand(from, to, platform.getRegisterExpires());
        } catch (Exception e) {
            log.error("级联注册失败: platformId={}", platform.getPlatformId(), e);
            cascadePlatformManager.updateRegisterStatus(platform.getId(), 3); // FAILED
        }
    }
}
