package io.github.lunasaw.voglander.intergration.wrapper.gb28181.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
        // 上线走定向更新：status=ONLINE，keepaliveTime=null（不更新心跳列）
        verify(deviceManager, times(1)).patchLiveness(eq(DEVICE_ID), eq(DeviceConstant.Status.ONLINE), isNull());
        log.info("Online→patchLiveness 校验通过");
    }

    @Test
    public void testOfflineRoutesToOffline() {
        handler.handle(event("Lifecycle", "Offline", DEVICE_ID, null, null));
        verify(deviceRegisterService, times(1)).offline(DEVICE_ID);
        log.info("Offline→offline 校验通过");
    }

    @Test
    public void testKeepaliveRoutesToKeepalive() {
        // payload 为空 → 回退用 event.deviceId()
        handler.handle(event("Notify", "Keepalive", DEVICE_ID, null, null));
        verify(deviceRegisterService, times(1)).keepalive(DEVICE_ID);
        log.info("Keepalive→keepalive 校验通过");
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

        // Phase 4：目录改批量幂等 upsert（取代 N+1 addChannel）。一次 batchUpsert，含 3 个通道。
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(deviceChannelManager, times(1)).batchUpsert(captor.capture());
        assertEquals(3, captor.getValue().size(), "应批量 upsert 3 个通道");
        log.info("Catalog→batchUpsert(3) 校验通过");
    }

    @Test
    public void testInviteOkRoutesToMediaSession() {
        handler.handle(event("Session", "InviteOk", DEVICE_ID, CALL_ID, null));
        verify(mediaSessionManager, times(1)).onInviteOk(CALL_ID, DEVICE_ID);
        log.info("InviteOk→onInviteOk 校验通过");
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
}
