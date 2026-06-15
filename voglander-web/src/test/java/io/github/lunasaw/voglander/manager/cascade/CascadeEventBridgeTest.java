package io.github.lunasaw.voglander.manager.cascade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.event.AlarmCreatedEvent;
import io.github.lunasaw.voglander.common.event.LocalChannelChangeEvent;
import io.github.lunasaw.voglander.common.event.LocalMobilePositionEvent;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeEventBridge;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeNotifyPublisher;

/**
 * CascadeEventBridge 纯单元测试（C3/C4/C5 桥接）。
 *
 * @author luna
 */
@DisplayName("CascadeEventBridge 单元测试")
@ExtendWith(MockitoExtension.class)
class CascadeEventBridgeTest {

    @Mock
    private CascadeNotifyPublisher notifyPublisher;

    @InjectMocks
    private CascadeEventBridge      bridge;

    @Test
    @DisplayName("告警事件 → pushAlarm（解析 payload channelId/alarmType）")
    void alarm_event_should_call_push_alarm() {
        Map<String, Object> payload = Map.of(
            "channelId", "LOCAL01", "alarmType", "1", "alarmPriority", "1", "alarmTime", "2026-06-14T10:00:00");
        bridge.onAlarm(new AlarmCreatedEvent("DEV", payload));
        verify(notifyPublisher).pushAlarm("DEV", "LOCAL01", "1", "1", "2026-06-14T10:00:00");
    }

    @Test
    @DisplayName("通道变更事件 → pushCatalogChange")
    void channel_change_should_call_push_catalog() {
        bridge.onChannelChange(new LocalChannelChangeEvent("DEV", "LOCAL01", "OFF"));
        verify(notifyPublisher).pushCatalogChange("DEV", "LOCAL01", "OFF");
    }

    @Test
    @DisplayName("位置事件 → pushMobilePosition")
    void mobile_position_should_call_push() {
        LocalMobilePositionEvent e = new LocalMobilePositionEvent(
            "DEV", "LOCAL01", "2026-06-14T10:00:00", "116.3", "39.9", "0", "0", "50");
        bridge.onMobilePosition(e);
        verify(notifyPublisher).pushMobilePosition(eq(e));
    }

    @Test
    @DisplayName("告警事件 payload 为 null 不抛异常")
    void alarm_event_null_payload_should_not_throw() {
        bridge.onAlarm(new AlarmCreatedEvent("DEV", null));
        verify(notifyPublisher).pushAlarm(eq("DEV"), any(), any(), any(), any());
    }
}
