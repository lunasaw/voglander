package io.github.lunasaw.voglander.service.subscription;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.luna.common.dto.ResultDTOUtils;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.constant.device.SubscriptionConstant;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.subscribe.VoglanderServerSubscribeCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceSubscriptionDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.DeviceSubscriptionManager;

/**
 * {@link DeviceSubscriptionService} 纯单元测试（Mockito）。
 * <p>
 * 覆盖：enable 在线下发/离线 PENDING、disable 撤销、resubscribeOnRegister 遍历、refresh 失败回退完整 SUBSCRIBE。
 * </p>
 *
 * @author luna
 */
@DisplayName("DeviceSubscriptionService 编排")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DeviceSubscriptionServiceTest {

    private static final String                     DEVICE_ID = "34020000001320000001";

    @Mock
    private DeviceSubscriptionManager               subscriptionManager;

    @Mock
    private DeviceManager                           deviceManager;

    @Mock
    private VoglanderServerSubscribeCommand         subscribeCommand;

    @InjectMocks
    private DeviceSubscriptionService               service;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(service, "subscriptionEnabled", true);
        ReflectionTestUtils.setField(service, "defaultExpires", 3600);
        ReflectionTestUtils.setField(service, "positionInterval", 5);
    }

    private DeviceDTO onlineDevice() {
        DeviceDTO d = new DeviceDTO();
        d.setDeviceId(DEVICE_ID);
        d.setStatus(DeviceConstant.Status.ONLINE);
        return d;
    }

    @Test
    @DisplayName("enable 设备在线 → 下发 SUBSCRIBE + markActive")
    public void enableOnlineSubscribes() {
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(onlineDevice());
        when(subscribeCommand.subscribeCatalog(eq(DEVICE_ID), anyInt()))
            .thenReturn(ResultDTOUtils.success("call-cat"));

        boolean ok = service.enable(DEVICE_ID, SubscriptionConstant.Type.CATALOG);

        assertTrue(ok);
        verify(subscriptionManager).upsertIntent(DEVICE_ID, SubscriptionConstant.Type.CATALOG, true);
        verify(subscribeCommand).subscribeCatalog(eq(DEVICE_ID), eq(3600));
        verify(subscriptionManager).markActive(DEVICE_ID, SubscriptionConstant.Type.CATALOG, "call-cat", 3600);
        verify(subscriptionManager, never()).markPending(anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("enable 设备离线 → 仅持久化意图 + markPending，不下发")
    public void enableOfflinePending() {
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(null);

        boolean ok = service.enable(DEVICE_ID, SubscriptionConstant.Type.ALARM);

        assertTrue(ok);
        verify(subscriptionManager).upsertIntent(DEVICE_ID, SubscriptionConstant.Type.ALARM, true);
        verify(subscriptionManager).markPending(DEVICE_ID, SubscriptionConstant.Type.ALARM);
        verify(subscribeCommand, never()).subscribeAlarm(anyString(), anyInt());
    }

    @Test
    @DisplayName("总开关关闭 → 仅持久化意图 + markPending，不查设备不下发")
    public void enableWithMasterSwitchOffOnlyPersistsIntent() {
        ReflectionTestUtils.setField(service, "subscriptionEnabled", false);

        boolean ok = service.enable(DEVICE_ID, SubscriptionConstant.Type.CATALOG);

        assertTrue(ok);
        verify(subscriptionManager).upsertIntent(DEVICE_ID, SubscriptionConstant.Type.CATALOG, true);
        verify(subscriptionManager).markPending(DEVICE_ID, SubscriptionConstant.Type.CATALOG);
        verify(deviceManager, never()).getDtoByDeviceId(anyString());
        verify(subscribeCommand, never()).subscribeCatalog(anyString(), anyInt());
    }

    @Test
    @DisplayName("enable 在线但 SUBSCRIBE 失败 → markFailed")
    public void enableOnlineSubscribeFailsMarksFailed() {
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(onlineDevice());
        when(subscribeCommand.subscribeMobilePosition(eq(DEVICE_ID), anyInt(), anyInt()))
            .thenReturn(ResultDTOUtils.failure(500, "device timeout", null));

        boolean ok = service.enable(DEVICE_ID, SubscriptionConstant.Type.MOBILE_POSITION);

        assertFalse(ok);
        verify(subscriptionManager).markFailed(DEVICE_ID, SubscriptionConstant.Type.MOBILE_POSITION);
    }

    @Test
    @DisplayName("disable ACTIVE 订阅 → unsubscribe + 关意图 + markInactive")
    public void disableActiveUnsubscribes() {
        DeviceSubscriptionDTO sub = new DeviceSubscriptionDTO();
        sub.setDeviceId(DEVICE_ID);
        sub.setSubType("CATALOG");
        sub.setStatus(SubscriptionConstant.Status.ACTIVE);
        sub.setCallId("call-cat");
        when(subscriptionManager.getByDeviceAndType(DEVICE_ID, SubscriptionConstant.Type.CATALOG)).thenReturn(sub);
        when(subscribeCommand.unsubscribe("call-cat")).thenReturn(ResultDTOUtils.success());

        boolean ok = service.disable(DEVICE_ID, SubscriptionConstant.Type.CATALOG);

        assertTrue(ok);
        verify(subscribeCommand).unsubscribe("call-cat");
        verify(subscriptionManager).upsertIntent(DEVICE_ID, SubscriptionConstant.Type.CATALOG, false);
        verify(subscriptionManager).markInactive(DEVICE_ID, SubscriptionConstant.Type.CATALOG);
    }

    @Test
    @DisplayName("disable 无 ACTIVE dialog → 不下发 unsubscribe，仍关意图")
    public void disableInactiveSkipsUnsubscribe() {
        when(subscriptionManager.getByDeviceAndType(DEVICE_ID, SubscriptionConstant.Type.ALARM)).thenReturn(null);

        boolean ok = service.disable(DEVICE_ID, SubscriptionConstant.Type.ALARM);

        assertTrue(ok);
        verify(subscribeCommand, never()).unsubscribe(anyString());
        verify(subscriptionManager).upsertIntent(DEVICE_ID, SubscriptionConstant.Type.ALARM, false);
    }

    @Test
    @DisplayName("resubscribeOnRegister 遍历意图开启的订阅逐类重订阅")
    public void resubscribeOnRegisterIteratesEnabled() {
        DeviceSubscriptionDTO cat = new DeviceSubscriptionDTO();
        cat.setDeviceId(DEVICE_ID);
        cat.setSubType("CATALOG");
        DeviceSubscriptionDTO alarm = new DeviceSubscriptionDTO();
        alarm.setDeviceId(DEVICE_ID);
        alarm.setSubType("ALARM");
        when(subscriptionManager.listEnabledByDevice(DEVICE_ID)).thenReturn(List.of(cat, alarm));
        when(subscribeCommand.subscribeCatalog(eq(DEVICE_ID), anyInt())).thenReturn(ResultDTOUtils.success("c1"));
        when(subscribeCommand.subscribeAlarm(eq(DEVICE_ID), anyInt())).thenReturn(ResultDTOUtils.success("c2"));

        service.resubscribeOnRegister(DEVICE_ID);

        verify(subscribeCommand).subscribeCatalog(eq(DEVICE_ID), eq(3600));
        verify(subscribeCommand).subscribeAlarm(eq(DEVICE_ID), eq(3600));
        verify(subscriptionManager).markActive(DEVICE_ID, SubscriptionConstant.Type.CATALOG, "c1", 3600);
        verify(subscriptionManager).markActive(DEVICE_ID, SubscriptionConstant.Type.ALARM, "c2", 3600);
    }

    @Test
    @DisplayName("refreshExpiring 成功 → markActive；失败 → 回退完整 SUBSCRIBE")
    public void refreshExpiringSuccessAndFallback() {
        DeviceSubscriptionDTO ok = new DeviceSubscriptionDTO();
        ok.setDeviceId("dev-ok");
        ok.setSubType("CATALOG");
        ok.setCallId("call-ok");
        DeviceSubscriptionDTO bad = new DeviceSubscriptionDTO();
        bad.setDeviceId("dev-bad");
        bad.setSubType("ALARM");
        bad.setCallId("call-bad");
        when(subscriptionManager.listExpiring(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
            .thenReturn(List.of(ok, bad));
        when(subscribeCommand.refresh("call-ok", 3600)).thenReturn(ResultDTOUtils.success("call-ok"));
        when(subscribeCommand.refresh("call-bad", 3600)).thenReturn(ResultDTOUtils.failure(500, "gone", null));
        when(subscribeCommand.subscribeAlarm(eq("dev-bad"), anyInt())).thenReturn(ResultDTOUtils.success("call-bad2"));

        service.refreshExpiring();

        verify(subscriptionManager).markActive("dev-ok", SubscriptionConstant.Type.CATALOG, "call-ok", 3600);
        // 失败回退：重发完整 SUBSCRIBE
        verify(subscribeCommand).subscribeAlarm(eq("dev-bad"), eq(3600));
        verify(subscriptionManager).markActive("dev-bad", SubscriptionConstant.Type.ALARM, "call-bad2", 3600);
    }
}
