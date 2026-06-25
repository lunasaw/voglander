package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.event.AlarmCreatedEvent;
import io.github.lunasaw.voglander.common.event.LocalChannelChangeEvent;
import io.github.lunasaw.voglander.common.event.LocalMobilePositionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联本地事件桥接（C3/C4/C5 触发源）。
 *
 * <p>监听本地数据变更的 Spring 事件，调 {@link CascadeNotifyPublisher} 主动推送上级。
 * 与既有入站管线（{@code Gb28181ProtocolHandler}）解耦：handler 只管落库 + 发本地事件，
 * 不直接依赖 cascade 推送。{@code sip.client.enabled=false}（无级联）时本 Bean 整个不加载，零开销。
 *
 * <p>必须异步（{@code @Async("sipNotifierExecutor")}），避免推送阻塞入站管线。
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeEventBridge {

    private final CascadeNotifyPublisher notifyPublisher;

    /** 告警：复用既有 AlarmCreatedEvent（payload 含 channelId/alarmType 等） */
    @EventListener
    @Async("sipNotifierExecutor")
    public void onAlarm(AlarmCreatedEvent e) {
        Map<String, Object> p = e.getPayload() != null ? e.getPayload() : Map.of();
        notifyPublisher.pushAlarm(
            e.getDeviceId(),
            str(p.get("channelId")),
            str(p.get("alarmType")),
            str(p.get("alarmPriority")),
            str(p.get("alarmTime")));
    }

    @EventListener
    @Async("sipNotifierExecutor")
    public void onChannelChange(LocalChannelChangeEvent e) {
        notifyPublisher.pushCatalogChange(e.getDeviceId(), e.getChannelId(), e.getEvent());
    }

    @EventListener
    @Async("sipNotifierExecutor")
    public void onMobilePosition(LocalMobilePositionEvent e) {
        notifyPublisher.pushMobilePosition(e);
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
