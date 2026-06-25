package io.github.lunasaw.voglander.manager.cascade;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeQueryHandler;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("CascadeQueryHandler 单元测试")
class CascadeQueryHandlerTest {

    @Mock  CascadeChannelManager  cascadeChannelManager;
    @Mock  CascadePlatformManager cascadePlatformManager;
    @InjectMocks CascadeQueryHandler handler;

    private DeviceQuery query(String sn, String deviceId) {
        DeviceQuery q = new DeviceQuery();
        q.setSn(sn);
        q.setDeviceId(deviceId);
        return q;
    }

    /* ---- onCatalogQuery ---- */

    @Test
    @DisplayName("目录查询：返回 enabled 通道数量与 DeviceItem 列表")
    void catalogQuery_returns_enabled_channels() {
        CascadeChannelDTO ch1 = new CascadeChannelDTO();
        ch1.setCascadeChannelId("34020000001310000001");
        ch1.setLocalChannelId("cam01");
        ch1.setEnabled(1);

        CascadeChannelDTO ch2 = new CascadeChannelDTO();
        ch2.setCascadeChannelId("34020000001310000002");
        ch2.setLocalChannelId("cam02");
        ch2.setEnabled(0); // 禁用，不应上报

        when(cascadeChannelManager.listByPlatformId("platform-A"))
            .thenReturn(Arrays.asList(ch1, ch2));

        DeviceResponse resp = handler.onCatalogQuery("platform-A", query("1", "platform-A"));

        assertNotNull(resp);
        assertEquals(2, resp.getSumNum());          // sumNum = 全量
        assertEquals(1, resp.getDeviceItemList().size()); // 只有 enabled=1 的
        assertEquals("34020000001310000001",
            resp.getDeviceItemList().get(0).getDeviceId());
    }

    @Test
    @DisplayName("目录查询：无通道时返回空列表")
    void catalogQuery_empty_channels() {
        when(cascadeChannelManager.listByPlatformId(any())).thenReturn(Collections.emptyList());

        DeviceResponse resp = handler.onCatalogQuery("platform-X", query("2", "platform-X"));

        assertNotNull(resp);
        assertEquals(0, resp.getSumNum());
        assertTrue(resp.getDeviceItemList().isEmpty());
    }

    /* ---- onDeviceInfoQuery ---- */

    @Test
    @DisplayName("设备信息查询：返回固定厂商/型号/固件")
    void deviceInfo_returns_fixed_fields() {
        DeviceInfo info = handler.onDeviceInfoQuery("platform-A", query("3", "platform-A"));

        assertNotNull(info);
        assertEquals("Voglander", info.getManufacturer());
        assertEquals("CascadePlatform", info.getModel());
        assertEquals("v1.0.4", info.getFirmware());
    }

    /* ---- onDeviceStatusQuery ---- */

    @Test
    @DisplayName("状态查询：始终返回 ONLINE")
    void deviceStatus_always_online() {
        DeviceStatus status = handler.onDeviceStatusQuery("platform-A", query("4", "platform-A"));

        assertNotNull(status);
        assertEquals("ONLINE", status.getOnline());
    }

    /* ---- onConfigDownloadQuery（C11） ---- */

    @Test
    @DisplayName("配置下载查询：返回 Result=OK，回写 sn/deviceId")
    void configDownload_returns_ok() {
        io.github.lunasaw.gb28181.common.entity.query.DeviceConfigDownload q =
            new io.github.lunasaw.gb28181.common.entity.query.DeviceConfigDownload();
        q.setSn("10");
        q.setDeviceId("platform-A");
        q.setConfigType("BasicParam");

        io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse resp =
            handler.onConfigDownloadQuery("platform-A", q);

        assertNotNull(resp);
        assertEquals("10", resp.getSn());
        assertEquals("platform-A", resp.getDeviceId());
        assertEquals("OK", resp.getResult());
    }

    /* ---- onMobilePositionQuery（C11） ---- */

    @Test
    @DisplayName("移动位置查询：平台自身无位置返回 null")
    void mobilePosition_returns_null() {
        io.github.lunasaw.gb28181.common.entity.query.MobilePositionQuery q =
            new io.github.lunasaw.gb28181.common.entity.query.MobilePositionQuery();
        q.setSn("11");
        q.setDeviceId("platform-A");

        assertNull(handler.onMobilePositionQuery("platform-A", q));
    }

    /* ---- onPresetQuery（C11） ---- */

    @Test
    @DisplayName("预置位查询：首版返回空列表 Num=0")
    void presetQuery_returns_empty_list() {
        io.github.lunasaw.gb28181.common.entity.query.PresetQuery q =
            new io.github.lunasaw.gb28181.common.entity.query.PresetQuery();
        q.setSn("12");
        q.setDeviceId("34020000001310000001");

        io.github.lunasaw.gb28181.common.entity.response.PresetQueryResponse resp =
            handler.onPresetQuery("platform-A", q);

        assertNotNull(resp);
        assertEquals("12", resp.getSn());
        assertEquals("34020000001310000001", resp.getDeviceId());
        assertNotNull(resp.getPresetList());
        assertEquals(0, resp.getPresetList().getNum());
        assertTrue(resp.getPresetList().getItems().isEmpty());
    }
}
