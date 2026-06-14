package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceMobileQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gbproxy.client.api.SubscribeListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议验证台订阅监听器（Lab 模式专用）。
 * <p>
 * <strong>非 GB28181 标准实现，仅 lab 自环联调便利</strong>（CLAUDE.md 协议合规铁律）。
 * 接收平台发起的 SUBSCRIBE，按订阅类型登记周期/即时推送（推送规则单点收口于
 * {@link LabSubscribePushService} + {@link LabSipClient}）。
 * </p>
 * <p>
 * 与现有 lab listener 一致：{@code @Component implements XxxListener} +
 * {@code @ConditionalOnProperty(name="voglander.protocol-lab.enabled")}，框架 client 端多实例观察者自动收集，无需额外 @Bean。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabSubscribeListener implements SubscribeListener {

    private final LabSubscribePushService labSubscribePushService;

    @Override
    public void onCatalogSubscribe(String platformId, Integer expires, DeviceQuery query) {
        log.info("Lab 收到目录订阅, platformId={}, expires={}", platformId, expires);
        labSubscribePushService.startCatalogPush(platformId, expires);
    }

    @Override
    public void onMobilePositionSubscribe(String platformId, Integer expires, DeviceMobileQuery query) {
        Integer interval = parseInterval(query);
        log.info("Lab 收到位置订阅, platformId={}, expires={}, interval={}", platformId, expires, interval);
        labSubscribePushService.startPositionPush(platformId, expires, interval);
    }

    @Override
    public void onAlarmSubscribe(String platformId, Integer expires, DeviceAlarmQuery query) {
        log.info("Lab 收到告警订阅, platformId={}, expires={}", platformId, expires);
        labSubscribePushService.startAlarmPush(platformId, expires);
    }

    private Integer parseInterval(DeviceMobileQuery query) {
        if (query == null || query.getInterval() == null) {
            return null;
        }
        try {
            return Integer.parseInt(query.getInterval().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
