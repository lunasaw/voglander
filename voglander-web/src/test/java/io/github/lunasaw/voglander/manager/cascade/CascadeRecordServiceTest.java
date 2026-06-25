package io.github.lunasaw.voglander.manager.cascade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.common.event.LocalRecordInfoEvent;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeDeviceSupplier;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeRecordService;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeRecordRequestDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.github.lunasaw.voglander.manager.manager.CascadeRecordRequestManager;

/**
 * CascadeRecordService 纯单元测试（C6）。
 *
 * @author luna
 */
@DisplayName("CascadeRecordService 单元测试")
@ExtendWith(MockitoExtension.class)
class CascadeRecordServiceTest {

    @Mock
    private CascadeRecordRequestManager cascadeRecordRequestManager;
    @Mock
    private CascadePlatformManager      cascadePlatformManager;
    @Mock
    private CascadeChannelManager       cascadeChannelManager;
    @Mock
    private CascadeDeviceSupplier       cascadeDeviceSupplier;

    @InjectMocks
    private CascadeRecordService        service;

    private CascadeRecordRequestDTO req(String platformId, String superiorSn, String localCh, String cascadeCh) {
        CascadeRecordRequestDTO r = new CascadeRecordRequestDTO();
        r.setId(1L);
        r.setPlatformId(platformId);
        r.setSuperiorSn(superiorSn);
        r.setLocalChannelId(localCh);
        return r;
    }

    private CascadeChannelDTO channel(String platformId, String local, String cascade) {
        CascadeChannelDTO ch = new CascadeChannelDTO();
        ch.setPlatformId(platformId);
        ch.setLocalChannelId(local);
        ch.setCascadeChannelId(cascade);
        return ch;
    }

    private void stubPlatformEndpoint(String platformId) {
        CascadePlatformDTO p = new CascadePlatformDTO();
        p.setPlatformId(platformId);
        when(cascadePlatformManager.getByPlatformId(platformId)).thenReturn(p);
        when(cascadeDeviceSupplier.buildFromDevice(p)).thenReturn(new FromDevice());
        when(cascadeDeviceSupplier.buildToDevice(p)).thenReturn(new ToDevice());
    }

    @Test
    @DisplayName("录像响应到达 → 找到请求 → remap deviceId/sn → 回包上级")
    void record_info_should_remap_and_send() {
        String pf = "PF1";
        when(cascadeRecordRequestManager.findPending("DEV", null, null))
            .thenReturn(req(pf, "S100", "LOCAL01", null));
        when(cascadeChannelManager.getByPlatformAndChannel(pf, "LOCAL01"))
            .thenReturn(channel(pf, "LOCAL01", "CASCADE01"));
        stubPlatformEndpoint(pf);

        DeviceRecord record = new DeviceRecord();
        record.setDeviceId("DEV");
        record.setSn("internal-sn");
        record.setSumNum(2);
        LocalRecordInfoEvent event = new LocalRecordInfoEvent("DEV", JSON.toJSONString(record));

        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            service.onRecordInfo(event);

            mocked.verify(() -> ClientCommandSender.sendDeviceRecordCommand(
                any(FromDevice.class), any(ToDevice.class),
                org.mockito.ArgumentMatchers.argThat((DeviceRecord r) ->
                    "CASCADE01".equals(r.getDeviceId()) && "S100".equals(r.getSn()) && r.getSumNum() == 2)));
            verify(cascadeRecordRequestManager).markResponded(1L);
        }
    }

    @Test
    @DisplayName("未找到请求时不回包")
    void no_pending_request_should_not_send() {
        when(cascadeRecordRequestManager.findPending("DEV", null, null)).thenReturn(null);
        DeviceRecord record = new DeviceRecord();
        record.setDeviceId("DEV");
        LocalRecordInfoEvent event = new LocalRecordInfoEvent("DEV", JSON.toJSONString(record));

        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            service.onRecordInfo(event);
            mocked.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("找到请求但通道映射缺失 → markResponded + 不回包")
    void missing_channel_mapping_should_mark_responded_but_not_send() {
        String pf = "PF1";
        when(cascadeRecordRequestManager.findPending("DEV", null, null))
            .thenReturn(req(pf, "S100", "LOCAL01", null));
        when(cascadeChannelManager.getByPlatformAndChannel(pf, "LOCAL01")).thenReturn(null);

        DeviceRecord record = new DeviceRecord();
        record.setDeviceId("DEV");
        LocalRecordInfoEvent event = new LocalRecordInfoEvent("DEV", JSON.toJSONString(record));

        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            service.onRecordInfo(event);
            verify(cascadeRecordRequestManager).markResponded(1L);
            mocked.verify(() -> ClientCommandSender.sendDeviceRecordCommand(
                any(FromDevice.class), any(ToDevice.class), any(DeviceRecord.class)), never());
        }
    }
}
