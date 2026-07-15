package io.github.lunasaw.voglander.service.subscription;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.anno.TechnicalScheduler;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181 订阅续订定时任务。
 * <p>
 * 过期前 {@code refresh-ahead-seconds} 秒续订即将过期的 ACTIVE 订阅，维持订阅不中断。
 * 依赖 {@code ApplicationWeb} 的 {@code @EnableScheduling}（已核验存在）。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@TechnicalScheduler(category = TechnicalScheduler.Category.PROTOCOL)
@ConditionalOnProperty(name = "gateway.gb28181.subscription.refresh-enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionRefreshScheduler {

    @Autowired
    private DeviceSubscriptionService subscriptionService;

    /**
     * 每 60s（可配）续订即将过期的订阅。
     */
    @Scheduled(fixedDelayString = "${gateway.gb28181.subscription.refresh-interval-ms:60000}")
    public void refresh() {
        try {
            subscriptionService.refreshExpiring();
        } catch (Exception e) {
            log.warn("订阅续订任务异常: {}", e.getMessage());
        }
    }
}
