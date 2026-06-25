package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceMobileQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gbproxy.client.api.SubscribeListener;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant.SubType;
import io.github.lunasaw.voglander.manager.manager.CascadeSubscribeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联订阅接收器（C2）。
 *
 * <p>本平台作为下级，接收上级的 SUBSCRIBE（Catalog/Alarm/MobilePosition），
 * 登记订阅意图到 {@code tb_cascade_subscribe}，供后续数据变更���主动 NOTIFY 推送上级。
 *
 * <p>{@code SubscribeListener} 为框架多实例观察者，回调返回 {@code void}——框架仅把订阅请求转给我方，
 * 我方须自行持久化订阅意图。expires<=0 语义为退订（RFC 6665 / GB28181 §9.11）。
 *
 * <p>dialog/callId（V2 核验）：框架主动 NOTIFY 命令均无 callId 入参，按 From/To 自管订阅 dialog，
 * 故此处不强制存 callId。lab 模式由 LabSubscribeListener 承担，本 Bean 不注册避免重复登记。
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnExpression(
    "${sip.client.enabled:false} and not ${voglander.protocol-lab.enabled:false}"
)
@RequiredArgsConstructor
public class CascadeSubscribeHandler implements SubscribeListener {

    private final CascadeSubscribeManager cascadeSubscribeManager;

    @Override
    public void onCatalogSubscribe(String platformId, Integer expires, DeviceQuery query) {
        registerOrCancel(platformId, SubType.CATALOG, expires, query != null ? query.getSn() : null, null);
    }

    @Override
    public void onAlarmSubscribe(String platformId, Integer expires, DeviceAlarmQuery query) {
        registerOrCancel(platformId, SubType.ALARM, expires, query != null ? query.getSn() : null, null);
    }

    @Override
    public void onMobilePositionSubscribe(String platformId, Integer expires, DeviceMobileQuery query) {
        Integer interval = parseInterval(query);
        registerOrCancel(platformId, SubType.MOBILE_POSITION, expires, query != null ? query.getSn() : null, interval);
    }

    /** expires<=0 = 退订，否则登记/续订 */
    private void registerOrCancel(String platformId, SubType type, Integer expires, String sn, Integer interval) {
        if (StringUtils.isBlank(platformId)) {
            log.warn("级联订阅 platformId 为空，忽略: type={}", type);
            return;
        }
        if (expires == null || expires <= 0) {
            cascadeSubscribeManager.expire(platformId, type);
            log.info("上级退订: platformId={}, type={}", platformId, type);
            return;
        }
        cascadeSubscribeManager.upsertActive(platformId, type, sn, expires, interval);
        log.info("上级订阅登记: platformId={}, type={}, expires={}, interval={}", platformId, type, expires, interval);
    }

    /** DeviceMobileQuery.getInterval() 为 String，解析为秒；非法值回 null（用默认间隔） */
    private Integer parseInterval(DeviceMobileQuery query) {
        if (query == null || StringUtils.isBlank(query.getInterval())) {
            return null;
        }
        try {
            return Integer.parseInt(query.getInterval().trim());
        } catch (NumberFormatException e) {
            log.warn("移动位置订阅 interval 解析失败: {}", query.getInterval());
            return null;
        }
    }
}
