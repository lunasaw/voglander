package io.github.lunasaw.voglander.manager.cascade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant.SubType;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeDeviceSupplier;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeNotifyPublisher;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeSubscribeDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.github.lunasaw.voglander.manager.manager.CascadeSubscribeManager;

/**
 * CascadeNotifyPublisher 纯单元测试（C3/C4/C5）。
 *
 * <p>验证：遍历订阅清单、编码重映射 local→cascade、无订阅时不推、找不到通道映射时跳过。
 *
 * @author luna
 */
@DisplayName("CascadeNotifyPublisher 单元测试")
@ExtendWith(MockitoExtension.class)
class CascadeNotifyPublisherTest {

    @Mock
    private CascadeSubscribeManager cascadeSubscribeManager;
    @Mock
    private CascadePlatformManager  cascadePlatformManager;
    @Mock
    private CascadeChannelManager   cascadeChannelManager;
    @Mock
    private CascadeDeviceSupplier   cascadeDeviceSupplier;

    @InjectMocks
    private CascadeNotifyPublisher  publisher;

    private CascadeSubscribeDTO sub(String platformId, String sn) {
        CascadeSubscribeDTO s = new CascadeSubscribeDTO();
        s.setPlatformId(platformId);
        s.setSn(sn);
        return s;
    }

    private CascadeChannelDTO channel(String platformId, String local, String cascade) {
        CascadeChannelDTO ch = new CascadeChannelDTO();
        ch.setPlatformId(platformId);
        ch.setLocalChannelId(local);
        ch.setCascadeChannelId(cascade);
        return ch;
    }

    private void stubEndpoint(String platformId) {
        CascadePlatformDTO p = new CascadePlatformDTO();
        p.setPlatformId(platformId);
        p.setLocalClientId("340200000013200000" + platformId.hashCode());
        when(cascadePlatformManager.getByPlatformId(platformId)).thenReturn(p);
        when(cascadeDeviceSupplier.buildFromDevice(p)).thenReturn(new FromDevice());
        ToDevice to = new ToDevice();
        to.setUserId(platformId);
        when(cascadeDeviceSupplier.buildToDevice(p)).thenReturn(to);
    }

    @Test
    @DisplayName("目录变更：推送已订阅上级，deviceId 重映射为 cascadeChannelId")
    void catalog_change_should_remap_and_push() {
        String pf = "PF1";
        when(cascadeSubscribeManager.listActiveByType(SubType.CATALOG)).thenReturn(List.of(sub(pf, "10")));
        when(cascadeChannelManager.getByPlatformAndChannel(pf, "LOCAL01"))
            .thenReturn(channel(pf, "LOCAL01", "CASCADE01"));
        stubEndpoint(pf);

        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            publisher.pushCatalogChange("DEV", "LOCAL01", "OFF");

            mocked.verify(() -> ClientCommandSender.sendCatalogChangeNotify(
                any(FromDevice.class), any(ToDevice.class), eq("10"),
                org.mockito.ArgumentMatchers.argThat((List<DeviceOtherUpdateNotify.OtherItem> items) ->
                    items.size() == 1
                        && "CASCADE01".equals(items.get(0).getDeviceId())
                        && "OFF".equals(items.get(0).getEvent()))));
        }
    }

    @Test
    @DisplayName("无订阅时不推送")
    void no_subscribers_should_not_push() {
        when(cascadeSubscribeManager.listActiveByType(SubType.CATALOG)).thenReturn(List.of());
        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            publisher.pushCatalogChange("DEV", "LOCAL01", "OFF");
            mocked.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("该上级未暴露此通道时跳过")
    void unmapped_channel_should_skip() {
        String pf = "PF1";
        when(cascadeSubscribeManager.listActiveByType(SubType.CATALOG)).thenReturn(List.of(sub(pf, "10")));
        when(cascadeChannelManager.getByPlatformAndChannel(pf, "LOCAL01")).thenReturn(null);
        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            publisher.pushCatalogChange("DEV", "LOCAL01", "OFF");
            mocked.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("告警：推送已订阅上级，deviceId 重映射")
    void alarm_should_remap_and_push() {
        String pf = "PF2";
        when(cascadeSubscribeManager.listActiveByType(SubType.ALARM)).thenReturn(List.of(sub(pf, "20")));
        when(cascadeChannelManager.getByPlatformAndChannel(pf, "LOCAL01"))
            .thenReturn(channel(pf, "LOCAL01", "CASCADE01"));
        stubEndpoint(pf);

        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            publisher.pushAlarm("DEV", "LOCAL01", "1", "1", "2026-06-14T10:00:00");

            mocked.verify(() -> ClientCommandSender.sendAlarmNotify(
                any(FromDevice.class), any(ToDevice.class),
                org.mockito.ArgumentMatchers.argThat((DeviceAlarmNotify n) ->
                    "CASCADE01".equals(n.getDeviceId()) && "20".equals(n.getSn()))));
        }
    }

    @Test
    @DisplayName("告警：指明通道但上级未暴露 → 跳过")
    void alarm_with_unmapped_channel_should_skip() {
        String pf = "PF2";
        when(cascadeSubscribeManager.listActiveByType(SubType.ALARM)).thenReturn(List.of(sub(pf, "20")));
        when(cascadeChannelManager.getByPlatformAndChannel(pf, "LOCAL01")).thenReturn(null);
        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            publisher.pushAlarm("DEV", "LOCAL01", "1", "1", "t");
            mocked.verify(() -> ClientCommandSender.sendAlarmNotify(any(), any(), any()), never());
        }
    }
}
