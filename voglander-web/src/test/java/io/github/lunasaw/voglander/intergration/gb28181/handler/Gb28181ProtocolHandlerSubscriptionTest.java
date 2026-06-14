package io.github.lunasaw.voglander.intergration.gb28181.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.constant.device.SubscriptionConstant;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.handler.Gb28181ProtocolHandler;
import io.github.lunasaw.voglander.manager.manager.AlarmManager;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.DevicePositionManager;
import io.github.lunasaw.voglander.manager.manager.DeviceSubscriptionManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.manager.manager.RecordInfoCacheManager;

/**
 * {@link Gb28181ProtocolHandler} 订阅相关入站处理纯单元测试（Mockito）。
 * <p>
 * 覆盖：NotifyUpdate ON/OFF/DEL → 对应 channel 操作 + touch CATALOG；
 * 移动位置落库 + touch MOBILE_POSITION；告警 touch ALARM。
 * </p>
 *
 * @author luna
 */
@DisplayName("Gb28181ProtocolHandler 订阅入站处理")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Gb28181ProtocolHandlerSubscriptionTest {

    private static final String          DEVICE_ID = "34020000001320000001";

    @Mock
    private io.github.lunasaw.voglander.client.service.device.DeviceRegisterService deviceRegisterService;
    @Mock
    private DeviceManager                deviceManager;
    @Mock
    private DeviceChannelManager         deviceChannelManager;
    @Mock
    private MediaSessionManager          mediaSessionManager;
    @Mock
    private AlarmManager                 alarmManager;
    @Mock
    private RecordInfoCacheManager       recordInfoCacheManager;
    @Mock
    private DeviceSubscriptionManager    subscriptionManager;
    @Mock
    private DevicePositionManager        devicePositionManager;
    @Mock
    private ApplicationEventPublisher    eventPublisher;

    @InjectMocks
    private Gb28181ProtocolHandler       handler;

    private DeviceEvent event(String group, String name, Map<String, Object> payload) {
        return new DeviceEvent("gb28181", group, name, DEVICE_ID, "sn-1",
            System.currentTimeMillis(), payload, "node-1");
    }

    @Test
    @DisplayName("NotifyUpdate ON → 通道上线 + touch CATALOG")
    public void notifyUpdateOnPatchesOnline() {
        Map<String, Object> item = new HashMap<>();
        item.put("deviceId", "34020000001320000002");
        item.put("event", "ON");
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceItemList", List.of(item));
        payload.put("sumNum", 1);

        handler.handle(event("Response", "NotifyUpdate", payload));

        verify(deviceChannelManager).patchChannelStatus(eq(DEVICE_ID), eq("34020000001320000002"),
            eq(DeviceConstant.Status.ONLINE), any(LocalDateTime.class), eq("CATALOG_NOTIFY"));
        verify(subscriptionManager).touchLastNotify(eq(DEVICE_ID),
            eq(SubscriptionConstant.Type.CATALOG), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("NotifyUpdate OFF → 通道离线")
    public void notifyUpdateOffPatchesOffline() {
        Map<String, Object> item = new HashMap<>();
        item.put("deviceId", "34020000001320000003");
        item.put("event", "OFF");
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceItemList", List.of(item));

        handler.handle(event("Response", "NotifyUpdate", payload));

        verify(deviceChannelManager).patchChannelStatus(eq(DEVICE_ID), eq("34020000001320000003"),
            eq(DeviceConstant.Status.OFFLINE), any(LocalDateTime.class), eq("CATALOG_NOTIFY"));
    }

    @Test
    @DisplayName("NotifyUpdate DEL → 删除通道")
    public void notifyUpdateDelDeletesChannel() {
        Map<String, Object> item = new HashMap<>();
        item.put("deviceId", "34020000001320000004");
        item.put("event", "DEL");
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceItemList", List.of(item));

        handler.handle(event("Response", "NotifyUpdate", payload));

        verify(deviceChannelManager).deleteDeviceChannel(DEVICE_ID, "34020000001320000004");
        verify(deviceChannelManager, never()).patchChannelStatus(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("MobilePosition → 落库 tb_device_position + touch MOBILE_POSITION")
    public void mobilePositionPersistsAndTouches() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("longitude", 116.397128);
        payload.put("latitude", 39.916527);
        payload.put("speed", 0.0);
        payload.put("direction", 90.0);
        payload.put("altitude", 50.0);

        handler.handle(event("Notify", "MobilePosition", payload));

        verify(devicePositionManager).record(eq(DEVICE_ID), isNull(),
            eq("116.397128"), eq("39.916527"), eq("0.0"), eq("90.0"), eq("50.0"), isNull());
        verify(subscriptionManager).touchLastNotify(eq(DEVICE_ID),
            eq(SubscriptionConstant.Type.MOBILE_POSITION), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Alarm → 落库 + touch ALARM")
    public void alarmTouchesSubscription() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", "34020000001320000002");
        payload.put("alarmType", 1);
        payload.put("alarmLevel", 3);
        payload.put("description", "移动侦测");

        handler.handle(event("Notify", "Alarm", payload));

        verify(alarmManager).add(any());
        verify(subscriptionManager).touchLastNotify(eq(DEVICE_ID),
            eq(SubscriptionConstant.Type.ALARM), any(LocalDateTime.class));
    }
}
