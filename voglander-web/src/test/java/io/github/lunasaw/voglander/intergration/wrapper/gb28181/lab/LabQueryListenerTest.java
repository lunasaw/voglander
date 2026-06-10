package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;

/**
 * LabQueryListener.onCatalogQuery 单元测试：通道数/名称按 LabChannelHolder 回包。
 */
@ExtendWith(MockitoExtension.class)
class LabQueryListenerTest {

    @Mock ApplicationEventPublisher    eventPublisher;
    @Mock VoglanderSipClientProperties clientProps;
    @Mock LabChannelHolder             labChannelHolder;

    @InjectMocks LabQueryListener listener;

    private DeviceQuery query() {
        DeviceQuery q = new DeviceQuery();
        q.setSn("123");
        return q;
    }

    @Test
    @DisplayName("默认 4 通道 / Lab-ch 前缀回包")
    void catalog_default() {
        when(clientProps.getClientId()).thenReturn("34020000001320000001");
        when(labChannelHolder.current()).thenReturn(new LabChannelHolder.Config(4, "Lab-ch"));

        DeviceResponse resp = listener.onCatalogQuery("platform-1", query());

        assertThat(resp.getSumNum()).isEqualTo(4);
        assertThat(resp.getDeviceItemList()).hasSize(4);
        assertThat(resp.getDeviceItemList().get(0).getName()).isEqualTo("Lab-ch1");
        assertThat(resp.getDeviceItemList().get(3).getName()).isEqualTo("Lab-ch4");
        assertThat(resp.getDeviceItemList().get(0).getName()).doesNotContain("-ch-");
    }

    @Test
    @DisplayName("配置 8 通道 + 自定义前缀 → 回 8 通道、名为 前缀+序号")
    void catalog_customized() {
        when(clientProps.getClientId()).thenReturn("34020000001320000001");
        when(labChannelHolder.current()).thenReturn(new LabChannelHolder.Config(8, "Cam"));

        DeviceResponse resp = listener.onCatalogQuery("platform-1", query());

        assertThat(resp.getSumNum()).isEqualTo(8);
        assertThat(resp.getDeviceItemList()).hasSize(8);
        assertThat(resp.getDeviceItemList().get(0).getName()).isEqualTo("Cam1");
        assertThat(resp.getDeviceItemList().get(7).getName()).isEqualTo("Cam8");
        // deviceId = clientId + 两位序号
        assertThat(resp.getDeviceItemList().get(0).getDeviceId()).isEqualTo("3402000000132000000101");
    }
}
