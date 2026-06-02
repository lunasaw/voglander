package io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.manager.event.InboundEventDispatcher;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181 网关业务回调（Phase 3 回退版本）。
 * <p>
 * 当 {@code voglander.event.shard.enabled=false} 时激活，直接调用 {@link InboundEventDispatcher}，
 * 不经过分片。用于灰度回退或单机小流量场景。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.event.shard.enabled", havingValue = "false")
public class VoglanderBusinessNotifierFallback implements BusinessNotifier {

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
