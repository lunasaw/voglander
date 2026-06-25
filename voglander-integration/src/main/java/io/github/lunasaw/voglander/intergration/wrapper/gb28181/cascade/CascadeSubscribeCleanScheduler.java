package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.manager.CascadeSubscribeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联订阅过期清理调度器（GB28181-2022 订阅收尾）。
 *
 * <p>上级订阅（目录/位置/告警）登记到 {@code tb_cascade_subscribe} 并带 expire_time。
 * 本调度器周期性把 expire_time 早于当前时间的 ACTIVE 订阅标为 EXPIRED，
 * 避免对已过期订阅继续主动推送 Notify。周期由
 * {@code gateway.cascade.subscribe.clean-interval-ms}（默认 60s）控制。</p>
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeSubscribeCleanScheduler {

    private final CascadeSubscribeManager cascadeSubscribeManager;

    /**
     * 周期清理过期订阅。
     */
    @Scheduled(fixedDelayString = "${gateway.cascade.subscribe.clean-interval-ms:60000}")
    public void cleanExpiredSubscribes() {
        try {
            int cleaned = cascadeSubscribeManager.cleanExpired(LocalDateTime.now());
            if (cleaned > 0) {
                log.info("清理过期级联订阅: {} 条", cleaned);
            }
        } catch (Exception e) {
            log.error("清理过期级联订阅失败", e);
        }
    }
}
