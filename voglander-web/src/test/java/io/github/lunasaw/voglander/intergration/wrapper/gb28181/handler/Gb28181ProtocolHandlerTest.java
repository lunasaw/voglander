package io.github.lunasaw.voglander.intergration.wrapper.gb28181.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.client.service.device.DeviceRegisterService;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 3：Gb28181ProtocolHandler 单元测试（PROTOCOL S2，新增覆盖）。
 * <p>
 * 这是原 {@code VoglanderBusinessNotifier} 大 switch <strong>从未有过</strong>的单元覆盖——
 * 迁出到协议无关的 handler 后，可用纯 Mockito 校验各 group/name 路由到正确的业务服务调用。
 * </p>
 *
 * @author luna
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class Gb28181ProtocolHandlerTest {

    private static final String DEVICE_ID = "34020000001320000001";
    private static final String CALL_ID   = "call-abc-123";

    @Mock
    private DeviceRegisterService  deviceRegisterService;

    @Mock
    private DeviceManager          deviceManager;

    @Mock
    private DeviceChannelManager   deviceChannelManager;

    @Mock
    private MediaSessionManager    mediaSessionManager;

    @Mock
    private io.github.lunasaw.voglander.manager.manager.AlarmManager alarmManager;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private io.github.lunasaw.voglander.manager.routing.DeviceNodeRouteService deviceNodeRouteService;

    @InjectMocks
    private Gb28181ProtocolHandler handler;

    private DeviceEvent event(String group, String name, String deviceId, String correlationId, Map<String, Object> payload) {
        return new DeviceEvent("gb28181", group, name, deviceId, correlationId, 1000L, payload, "node-1");
    }

    @Test
    public void testProtocolIsGb28181() {
        assertEquals("gb28181", handler.protocol());
    }

    @Test
    public void testRegisterRoutesToLogin() {
        handler.handle(event("Lifecycle", "Register", DEVICE_ID, null, Map.of("expire", 3600, "transport", "UDP")));
        verify(deviceRegisterService, times(1)).login(any());
        log.info("Register→login 校验通过");
    }

    @Test
    public void testOnlineRoutesToPatchLiveness() {
        handler.handle(event("Lifecycle", "Online", DEVICE_ID, null, null));
        // 上线走定向更新：status=ONLINE，携带当前时间戳（C3：单调条件 + R4 终态保护生效）
        verify(deviceManager, times(1)).patchLiveness(eq(DEVICE_ID), eq(DeviceConstant.Status.ONLINE), any(java.time.LocalDateTime.class));
        log.info("Online→patchLiveness 校验通过");
    }

    @Test
    public void testOfflineRoutesToOfflineAndCascadeOffline() {
        handler.handle(event("Lifecycle", "Offline", DEVICE_ID, null, null));
        verify(deviceRegisterService, times(1)).offline(DEVICE_ID);
        verify(deviceChannelManager, times(1)).cascadeOffline(DEVICE_ID);
        log.info("Offline→offline + cascadeOffline 校验通过");
    }

    @Test
    public void testKeepaliveRoutesToKeepalive() {
        // payload 为空 → 回退用 event.deviceId()
        handler.handle(event("Notify", "Keepalive", DEVICE_ID, null, null));
        // Phase 2a：验证心跳合并版本被调用（绕过 DeviceRegisterService.keepalive）
        verify(deviceManager, times(1)).patchLivenessWithCoalesce(
            eq(DEVICE_ID),
            eq(DeviceConstant.Status.ONLINE),
            any(java.time.LocalDateTime.class)
        );
        log.info("Keepalive→patchLivenessWithCoalesce 校验通过");
    }

    @Test
    public void testCatalogRoutesToAddChannelPerItem() {
        // 构造含 3 个通道的目录 payload（FastJSON2 round-trip 得到精确 Map 形状）
        List<DeviceItem> items = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            DeviceItem item = new DeviceItem();
            item.setDeviceId(DEVICE_ID + "_ch" + i);
            item.setName("channel-" + i);
            items.add(item);
        }
        DeviceResponse resp = new DeviceResponse();
        resp.setDeviceItemList(items);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = JSON.parseObject(JSON.toJSONString(resp), Map.class);

        handler.handle(event("Response", "Catalog", DEVICE_ID, "sn-1", payload));

        // Phase 4(1.0.4)：目录改批量幂等 upsertWithStatus，含 status/lastSeenTime 字段。一次调用，含 3 个通道。
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(deviceChannelManager, times(1)).batchUpsertWithStatus(eq(DEVICE_ID), captor.capture());
        assertEquals(3, captor.getValue().size(), "应批量 upsert 3 个通道");
        log.info("Catalog→batchUpsertWithStatus(3) 校验通过");
    }

    @Test
    public void testInviteOkRoutesToMediaSessionByCallId() {
        // 完美方案：InviteOk 按 callId 关联会话置 ACTIVE，不读被 To 头污染的 event.deviceId()
        handler.handle(event("Session", "InviteOk", DEVICE_ID, CALL_ID, null));
        verify(mediaSessionManager, times(1)).onInviteOk(CALL_ID);
        log.info("InviteOk→onInviteOk(callId) 校验通过");
    }

    @Test
    public void testInviteOkPromotesChannelFromSession() {
        // promote 的 deviceId/channelId 取自会话表（权威），而非事件字段
        MediaSessionDTO sess = new MediaSessionDTO();
        sess.setCallId(CALL_ID);
        sess.setDeviceId(DEVICE_ID);
        sess.setChannelId("ch-77");
        when(mediaSessionManager.getByCallId(CALL_ID)).thenReturn(sess);

        handler.handle(event("Session", "InviteOk", DEVICE_ID, CALL_ID, null));

        verify(mediaSessionManager, times(1)).onInviteOk(CALL_ID);
        verify(deviceChannelManager, times(1))
            .promoteOnlineIfOffline(eq(DEVICE_ID), eq("ch-77"), any());
        log.info("InviteOk→promote(会话表 deviceId/channelId) 校验通过");
    }

    @Test
    public void testInviteFailureParsesStatusCode() {
        handler.handle(event("Session", "InviteFailure", DEVICE_ID, CALL_ID, Map.of("statusCode", 486)));
        verify(mediaSessionManager, times(1)).onInviteFailure(CALL_ID, 486);
        log.info("InviteFailure→onInviteFailure(486) 校验通过");
    }

    @Test
    public void testByeRoutesToOnBye() {
        handler.handle(event("Session", "Bye", DEVICE_ID, CALL_ID, null));
        verify(mediaSessionManager, times(1)).onBye(DEVICE_ID);
        log.info("Bye→onBye 校验通过");
    }

    @Test
    public void testUnknownGroupNameIsNoop() {
        handler.handle(event("Bogus", "Nonexistent", DEVICE_ID, null, null));
        // default 分支：仅日志，不触碰任何业务服务
        verifyNoInteractions(deviceRegisterService, mediaSessionManager);
        verify(deviceManager, never()).patchLiveness(any(), any(), any());
        log.info("未知事件 default 分支无副作用校验通过");
    }

    // ================================
    // D7：告警入站路由（Notify.Alarm）单测 —— 补此前"回归裸奔"的链路断言
    // ================================

    @Test
    public void testAlarmRoutesToAlarmManagerAddWithCorrectFields() {
        Map<String, Object> payload = Map.of(
            "channelId", "ch-alarm-1",
            "alarmType", 2,
            "alarmLevel", 1,
            "description", "移动侦测");

        handler.handle(event("Notify", "Alarm", DEVICE_ID, null, payload));

        ArgumentCaptor<io.github.lunasaw.voglander.manager.domaon.dto.AlarmDTO> captor =
            ArgumentCaptor.forClass(io.github.lunasaw.voglander.manager.domaon.dto.AlarmDTO.class);
        verify(alarmManager, times(1)).add(captor.capture());
        io.github.lunasaw.voglander.manager.domaon.dto.AlarmDTO dto = captor.getValue();
        assertEquals(DEVICE_ID, dto.getDeviceId(), "deviceId 应映射");
        assertEquals("ch-alarm-1", dto.getChannelId(), "channelId 应映射");
        assertEquals(2, dto.getAlarmType(), "alarmType 应映射");
        assertEquals(1, dto.getAlarmLevel(), "alarmLevel 应映射");
        assertEquals("移动侦测", dto.getDescription(), "description 应映射");
        org.junit.jupiter.api.Assertions.assertNotNull(dto.getAlarmTime(), "alarmTime 应被赋值");
        log.info("Notify.Alarm→alarmManager.add(字段映射) 校验通过");
    }

    @Test
    public void testAlarmPublishesAlarmCreatedEvent() {
        Map<String, Object> payload = Map.of("alarmType", 5, "alarmLevel", 2);

        handler.handle(event("Notify", "Alarm", DEVICE_ID, null, payload));

        ArgumentCaptor<io.github.lunasaw.voglander.common.event.AlarmCreatedEvent> captor =
            ArgumentCaptor.forClass(io.github.lunasaw.voglander.common.event.AlarmCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertEquals(DEVICE_ID, captor.getValue().getDeviceId(), "AlarmCreatedEvent.deviceId 应映射");
        log.info("Notify.Alarm→eventPublisher.publishEvent(AlarmCreatedEvent) 校验通过");
    }

    // ================================
    // 1.0.6：device.* / session.* SSE 中继事件断言
    // ================================

    @Test
    public void testRegisterPublishesSseRelayEvent() {
        handler.handle(event("Lifecycle", "Register", DEVICE_ID, null,
            Map.of("expire", 3600, "transport", "UDP", "remoteIp", "127.0.0.1", "remotePort", 5061)));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        // publishEvent 被调用 1 次（SseRelayEvent: device.register）
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        Object evt = captor.getValue();
        org.junit.jupiter.api.Assertions.assertInstanceOf(
            io.github.lunasaw.voglander.common.event.SseRelayEvent.class, evt);
        io.github.lunasaw.voglander.common.event.SseRelayEvent relay =
            (io.github.lunasaw.voglander.common.event.SseRelayEvent) evt;
        assertEquals("device.register", relay.getTopic(), "topic 应为 device.register");
        assertEquals(DEVICE_ID, relay.getData().get("deviceId"), "deviceId 应透传");
        log.info("Register→SseRelayEvent(device.register) 校验通过");
    }

    @Test
    public void testOnlinePublishesSseRelayEvent() {
        handler.handle(event("Lifecycle", "Online", DEVICE_ID, null, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        io.github.lunasaw.voglander.common.event.SseRelayEvent relay =
            (io.github.lunasaw.voglander.common.event.SseRelayEvent) captor.getValue();
        assertEquals("device.online", relay.getTopic());
        log.info("Online→SseRelayEvent(device.online) 校验通过");
    }

    @Test
    public void testOfflinePublishesSseRelayEvent() {
        handler.handle(event("Lifecycle", "Offline", DEVICE_ID, null, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        io.github.lunasaw.voglander.common.event.SseRelayEvent relay =
            (io.github.lunasaw.voglander.common.event.SseRelayEvent) captor.getValue();
        assertEquals("device.offline", relay.getTopic());
        log.info("Offline→SseRelayEvent(device.offline) 校验通过");
    }

    @Test
    public void testKeepaliveThrottlePublishesSseOnFirstCall() {
        handler.handle(event("Notify", "Keepalive", DEVICE_ID, null, null));

        // 首次心跳 → 立即发布 device.keepalive
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        io.github.lunasaw.voglander.common.event.SseRelayEvent relay =
            (io.github.lunasaw.voglander.common.event.SseRelayEvent) captor.getValue();
        assertEquals("device.keepalive", relay.getTopic());
        log.info("Keepalive(首次)→SseRelayEvent(device.keepalive) 校验通过");
    }

    @Test
    public void testKeepaliveThrottleSkipsDuplicateWithin5s() {
        // 同一 handler 实例连发两次心跳：第 2 次在节流窗口内，不应再发 SSE
        handler.handle(event("Notify", "Keepalive", DEVICE_ID, null, null));
        handler.handle(event("Notify", "Keepalive", DEVICE_ID, null, null));

        // 只有 1 次 publishEvent（第 2 次心跳被节流，不发 device.keepalive）
        verify(eventPublisher, times(1)).publishEvent((Object) any());
        log.info("Keepalive 节流（5s 内第 2 次心跳不发 SSE）校验通过");
    }

    @Test
    public void testCatalogPublishesSseRelayWithChannelCount() {
        List<io.github.lunasaw.gb28181.common.entity.response.DeviceItem> items = new java.util.ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            io.github.lunasaw.gb28181.common.entity.response.DeviceItem it =
                new io.github.lunasaw.gb28181.common.entity.response.DeviceItem();
            it.setDeviceId(DEVICE_ID + "_ch" + i);
            it.setName("ch-" + i);
            items.add(it);
        }
        io.github.lunasaw.gb28181.common.entity.response.DeviceResponse resp =
            new io.github.lunasaw.gb28181.common.entity.response.DeviceResponse();
        resp.setDeviceItemList(items);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = com.alibaba.fastjson2.JSON.parseObject(
            com.alibaba.fastjson2.JSON.toJSONString(resp), Map.class);

        handler.handle(event("Response", "Catalog", DEVICE_ID, "sn-2", payload));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        io.github.lunasaw.voglander.common.event.SseRelayEvent relay =
            (io.github.lunasaw.voglander.common.event.SseRelayEvent) captor.getValue();
        assertEquals("device.catalog", relay.getTopic());
        assertEquals(2, relay.getData().get("channelCount"), "channelCount 应为 2");
        log.info("Catalog→SseRelayEvent(device.catalog, channelCount=2) 校验通过");
    }
}
