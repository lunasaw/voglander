package io.github.lunasaw.voglander.manager.cascade;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceMobileQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant.SubType;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeSubscribeHandler;
import io.github.lunasaw.voglander.manager.manager.CascadeSubscribeManager;

/**
 * CascadeSubscribeHandler 纯单元测试（C2）。
 *
 * @author luna
 */
@DisplayName("CascadeSubscribeHandler 单元测试")
@ExtendWith(MockitoExtension.class)
class CascadeSubscribeHandlerTest {

    @Mock
    private CascadeSubscribeManager cascadeSubscribeManager;

    @InjectMocks
    private CascadeSubscribeHandler handler;

    private DeviceQuery deviceQuery(String sn) {
        DeviceQuery q = new DeviceQuery();
        q.setSn(sn);
        return q;
    }

    @Test
    @DisplayName("onCatalogSubscribe expires>0 应登记 ACTIVE")
    void catalog_subscribe_should_register() {
        handler.onCatalogSubscribe("PF1", 3600, deviceQuery("100"));
        verify(cascadeSubscribeManager).upsertActive("PF1", SubType.CATALOG, "100", 3600, null);
    }

    @Test
    @DisplayName("onCatalogSubscribe expires=0 应退订")
    void catalog_unsubscribe_when_expires_zero() {
        handler.onCatalogSubscribe("PF1", 0, deviceQuery("100"));
        verify(cascadeSubscribeManager).expire("PF1", SubType.CATALOG);
        verify(cascadeSubscribeManager, never()).upsertActive(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("onCatalogSubscribe expires=null 应退订")
    void catalog_unsubscribe_when_expires_null() {
        handler.onCatalogSubscribe("PF1", null, deviceQuery("100"));
        verify(cascadeSubscribeManager).expire("PF1", SubType.CATALOG);
    }

    @Test
    @DisplayName("onAlarmSubscribe 应登记 ALARM")
    void alarm_subscribe_should_register() {
        DeviceAlarmQuery q = new DeviceAlarmQuery();
        q.setSn("200");
        handler.onAlarmSubscribe("PF2", 1800, q);
        verify(cascadeSubscribeManager).upsertActive("PF2", SubType.ALARM, "200", 1800, null);
    }

    @Test
    @DisplayName("onMobilePositionSubscribe 应解析 interval 并登记")
    void mobile_subscribe_should_parse_interval() {
        DeviceMobileQuery q = new DeviceMobileQuery();
        q.setSn("300");
        q.setInterval("5");
        handler.onMobilePositionSubscribe("PF3", 3600, q);
        verify(cascadeSubscribeManager).upsertActive("PF3", SubType.MOBILE_POSITION, "300", 3600, 5);
    }

    @Test
    @DisplayName("onMobilePositionSubscribe interval 非法应回 null")
    void mobile_subscribe_invalid_interval_should_be_null() {
        DeviceMobileQuery q = new DeviceMobileQuery();
        q.setSn("300");
        q.setInterval("abc");
        handler.onMobilePositionSubscribe("PF3", 3600, q);
        verify(cascadeSubscribeManager).upsertActive("PF3", SubType.MOBILE_POSITION, "300", 3600, null);
    }

    @Test
    @DisplayName("platformId 为空应忽略，不触碰 manager")
    void blank_platform_should_ignore() {
        handler.onCatalogSubscribe("  ", 3600, deviceQuery("100"));
        verifyNoInteractions(cascadeSubscribeManager);
    }
}
