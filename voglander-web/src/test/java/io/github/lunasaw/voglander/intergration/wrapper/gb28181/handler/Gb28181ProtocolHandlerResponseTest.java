package io.github.lunasaw.voglander.intergration.wrapper.gb28181.handler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigDownloadResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.response.PTZPositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.PresetQueryResponse;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.client.service.device.DeviceRegisterService;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.manager.manager.RecordInfoCacheManager;

/**
 * S3：Gb28181ProtocolHandler 入站响应回填单元测试（纯 Mockito）。
 *
 * <p>
 * 喂入 forwarder 风格的 payload Map（实体 JSON 往返），验证 5 类核心响应落库/回填 + 录像缓存 + SSE。
 * </p>
 *
 * @author luna
 */
@DisplayName("Gb28181ProtocolHandler 入站响应回填测试 (S3)")
@ExtendWith(MockitoExtension.class)
class Gb28181ProtocolHandlerResponseTest {

    private static final String   DEVICE_ID = "34020000001320000001";

    @Mock
    private DeviceRegisterService deviceRegisterService;
    @Mock
    private DeviceManager         deviceManager;
    @Mock
    private DeviceChannelManager  deviceChannelManager;
    @Mock
    private MediaSessionManager   mediaSessionManager;
    @Mock
    private io.github.lunasaw.voglander.manager.manager.AlarmManager alarmManager;
    @Mock
    private RecordInfoCacheManager recordInfoCacheManager;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private io.github.lunasaw.voglander.manager.routing.DeviceNodeRouteService deviceNodeRouteService;

    @InjectMocks
    private Gb28181ProtocolHandler handler;

    /** forwarder 风格：实体 → JSON → Map 作为 payload */
    private Map<String, Object> asPayload(Object entity) {
        return JSON.parseObject(JSON.toJSONString(entity), Map.class);
    }

    private DeviceEvent event(String name, String correlationId, Map<String, Object> payload) {
        return new DeviceEvent("gb28181", "Response", name, DEVICE_ID, correlationId, 1000L, payload, "node-1");
    }

    /** 在 patchExtendInfo mock 上执行真实的 consumer，得到回填后的 ExtendInfo */
    private DeviceDTO.ExtendInfo capturePatch(String name, Map<String, Object> payload, String correlationId) {
        DeviceDTO.ExtendInfo ext = new DeviceDTO.ExtendInfo();
        doAnswer(inv -> {
            Consumer<DeviceDTO.ExtendInfo> c = inv.getArgument(1);
            c.accept(ext);
            return null;
        }).when(deviceManager).patchExtendInfo(eq(DEVICE_ID), any());
        handler.handle(event(name, correlationId, payload));
        return ext;
    }

    @Test
    @DisplayName("DeviceStatus 回填 extend.deviceStatus + 推 device.status SSE")
    void deviceStatus_backfill() {
        DeviceStatus st = new DeviceStatus();
        st.setOnline("ONLINE");
        st.setResult("OK");
        DeviceDTO.ExtendInfo ext = capturePatch("DeviceStatus", asPayload(st), "sn-1");
        org.junit.jupiter.api.Assertions.assertNotNull(ext.getDeviceStatus());
        org.junit.jupiter.api.Assertions.assertTrue(ext.getDeviceStatus().contains("ONLINE"));
        verifySse("device.status");
    }

    @Test
    @DisplayName("PtzPosition 回填 extend.ptzPosition")
    void ptzPosition_backfill() {
        PTZPositionResponse pos = new PTZPositionResponse();
        pos.setPan(120.0);
        pos.setTilt(45.0);
        pos.setZoom(2.0);
        DeviceDTO.ExtendInfo ext = capturePatch("PtzPosition", asPayload(pos), null);
        org.junit.jupiter.api.Assertions.assertNotNull(ext.getPtzPosition());
        verifySse("device.ptz_position");
    }

    @Test
    @DisplayName("PresetQuery 回填 extend.presets")
    void preset_backfill() {
        PresetQueryResponse preset = new PresetQueryResponse();
        preset.setSn("sn-9");
        preset.setDeviceId(DEVICE_ID);
        DeviceDTO.ExtendInfo ext = capturePatch("PresetQuery", asPayload(preset), "sn-9");
        org.junit.jupiter.api.Assertions.assertNotNull(ext.getPresets());
        verifySse("device.preset");
    }

    @Test
    @DisplayName("Config 回填 extend.config")
    void config_backfill() {
        DeviceConfigResponse cfg = new DeviceConfigResponse();
        cfg.setResult("OK");
        DeviceDTO.ExtendInfo ext = capturePatch("Config", asPayload(cfg), "sn-2");
        org.junit.jupiter.api.Assertions.assertNotNull(ext.getConfig());
        verifySse("device.config");
    }

    @Test
    @DisplayName("ConfigDownload 回填 extend.configDownload（独立实体）")
    void configDownload_backfill() {
        DeviceConfigDownloadResponse cfg = new DeviceConfigDownloadResponse();
        cfg.setResult("OK");
        DeviceDTO.ExtendInfo ext = capturePatch("ConfigDownload", asPayload(cfg), "sn-3");
        org.junit.jupiter.api.Assertions.assertNotNull(ext.getConfigDownload());
        verifySse("device.config_download");
    }

    @Test
    @DisplayName("RecordInfo 走 RedisCache（key=deviceId+sn），不落 extend，推 device.recordinfo")
    void recordInfo_cached() {
        DeviceRecord rec = new DeviceRecord();
        rec.setSumNum(3);
        handler.handle(event("RecordInfo", "sn-rec", asPayload(rec)));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(recordInfoCacheManager).put(eq(DEVICE_ID), eq("sn-rec"), json.capture());
        org.junit.jupiter.api.Assertions.assertTrue(json.getValue().contains("sumNum"));
        // RecordInfo 不走 extend 回填
        verify(deviceManager, never()).patchExtendInfo(anyString(), any());
        verifySse("device.recordinfo");
    }

    @Test
    @DisplayName("空 payload 不触发回填")
    void emptyPayload_noop() {
        handler.handle(event("DeviceStatus", "sn", Map.of()));
        verify(deviceManager, never()).patchExtendInfo(anyString(), any());
    }

    private void verifySse(String topic) {
        ArgumentCaptor<SseRelayEvent> captor = ArgumentCaptor.forClass(SseRelayEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        boolean found = captor.getAllValues().stream().anyMatch(e -> topic.equals(e.getTopic()));
        org.junit.jupiter.api.Assertions.assertTrue(found, "应推送 SSE topic=" + topic);
    }
}
