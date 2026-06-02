package io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.manager.event.InboundEventDispatcher;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181 网关业务回调，sip-gateway 1.8.0 接入业务层的唯一入口。
 *
 * <p>
 * Phase 3（PROTOCOL S2）后本类退化为<strong>纯翻译器</strong>：仅把 sip-gateway 的 {@link GatewayEvent}
 * 翻译为 voglander 自有的 {@link DeviceEvent}，交给协议无关的 {@link InboundEventDispatcher} 路由。
 * 具体协议处理（原整段 switch）已迁出到 {@code Gb28181ProtocolHandler}。本类是<strong>唯一</strong>
 * import sip-gateway 框架入站类型（{@code BusinessNotifier}/{@code GatewayEvent}）的地方，
 * 框架耦合收敛于此——新增协议（如 onvif.*）时本类零改动，仅需新增对应 ProtocolEventHandler。
 * </p>
 *
 * <p>
 * 直接实现 {@link BusinessNotifier}（覆盖默认 {@code NoopBusinessNotifier}，后者为
 * {@code @ConditionalOnMissingBean}）。<strong>不继承</strong> {@code AbstractProtocolBusinessNotifier}——
 * 其 {@code notify()} 为 {@code final} 且自调用 {@code onProtocolEvent}，无法施加有效 {@code @Async}。
 * 本类在自有 {@link #notify(GatewayEvent)} 上标注 {@code @Async("sipNotifierExecutor")}，
 * Spring 代理可生效，满足框架"notify 必须异步否则设备超时重传"的约束。
 * </p>
 *
 * @author luna
 * @since 2025-05-29
 */
@Slf4j
@Component
public class VoglanderBusinessNotifier implements BusinessNotifier {

    @Autowired
    private InboundEventDispatcher dispatcher;

    @Override
    @Async("sipNotifierExecutor")
    public void notify(GatewayEvent event) {
        if (event == null || event.type() == null) {
            log.warn("收到空网关事件，忽略");
            return;
        }
        // 三段式 type 切分：gb28181.Lifecycle.Register → (gb28181, Lifecycle, Register)
        String[] seg = event.type().split("\\.", 3);
        if (seg.length < 3) {
            log.warn("非三段式网关事件 type: {}，忽略", event.type());
            return;
        }
        try {
            // payload 保持原始 Map，昂贵的反序列化留待 handler（项目类型转换规范）
            DeviceEvent deviceEvent = new DeviceEvent(
                seg[0], seg[1], seg[2],
                event.deviceId(), event.correlationId(),
                event.timestampMs(), event.payload(), event.nodeId());
            dispatcher.dispatch(deviceEvent);
        } catch (Exception e) {
            log.error("翻译/分发网关事件异常, type={}, deviceId={}, correlationId={}",
                event.type(), event.deviceId(), event.correlationId(), e);
        }
    }
}
