package io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.manager.event.ShardDispatcher;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181 网关业务回调，sip-gateway 1.8.0 接入业务层的唯一入口。
 *
 * <p>
 * Phase 4 后本类仅做<strong>轻量翻译</strong>：把 {@link GatewayEvent} 翻译为 {@link DeviceEvent}，
 * 提交到 {@link ShardDispatcher} 分片路由，<strong>立即归还 SIP 线程</strong>。
 * 重活（payload 反序列化、业务逻辑、DB 写）全在分片槽单线程中执行。
 * </p>
 *
 * <p>
 * 灰度开关：{@code voglander.event.shard.enabled=false} 时回退旧路径（直接调 dispatcher.dispatch）。
 * </p>
 *
 * @author luna
 * @since 2025-05-29
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.event.shard.enabled", havingValue = "true", matchIfMissing = true)
public class VoglanderBusinessNotifier implements BusinessNotifier {

    @Autowired
    private ShardDispatcher shardDispatcher;

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
            // 仅做轻量翻译，payload 保持原始 Map 引用，不做 FastJSON2 反序列化
            DeviceEvent deviceEvent = new DeviceEvent(
                seg[0], seg[1], seg[2],
                event.deviceId(), event.correlationId(),
                event.timestampMs(), event.payload(), event.nodeId());

            // 提交到分片调度器，立即返回
            shardDispatcher.dispatch(deviceEvent);
        } catch (Exception e) {
            log.error("翻译/分发网关事件异常, type={}, deviceId={}, correlationId={}",
                event.type(), event.deviceId(), event.correlationId(), e);
        }
    }
}
