package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.anno.TechnicalScheduler;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.manager.manager.CascadeRecordRequestManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联录像查询请求超时清理调度器（C6 收尾）。
 *
 * <p>上级查录像后登记到 {@code tb_cascade_record_request}，若真实下级设备迟迟不响应，
 * 请求会一直停留在 PENDING。本调度器周期性把超过
 * {@code gateway.cascade.record.request-timeout-sec}（默认
 * {@link CascadeConstant#RECORD_REQUEST_TIMEOUT_SEC}）的 PENDING 请求标为 TIMEOUT，避免泄漏。</p>
 *
 * @author luna
 */
@Slf4j
@Component
@TechnicalScheduler(category = TechnicalScheduler.Category.PROTOCOL)
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeRecordRequestScheduler {

    private final CascadeRecordRequestManager cascadeRecordRequestManager;

    /**
     * 周期清理超时的录像查询请求。
     */
    @Scheduled(fixedDelayString = "${gateway.cascade.record.clean-interval-ms:15000}")
    public void cleanTimeoutRequests() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(CascadeConstant.RECORD_REQUEST_TIMEOUT_SEC);
            int cleaned = cascadeRecordRequestManager.cleanTimeout(cutoff);
            if (cleaned > 0) {
                log.info("清理超时级联录像查询请求: {} 条 (cutoff={})", cleaned, cutoff);
            }
        } catch (Exception e) {
            log.error("清理超时级联录像查询请求失败", e);
        }
    }
}
