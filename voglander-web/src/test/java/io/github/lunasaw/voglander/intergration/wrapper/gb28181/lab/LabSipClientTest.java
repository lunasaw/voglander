package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;

/**
 * LabSipClient 单元测试：holder 空→自环；holder 设外部→用 holder；
 * buildFrom 身份覆盖与回退、ip/port 恒为 5061；pushCatalog 通道名 prefix+i（不含 "-ch"）。
 */
@ExtendWith(MockitoExtension.class)
class LabSipClientTest {

    @Mock VoglanderSipClientProperties clientProps;
    @Mock VoglanderSipServerProperties serverProps;
    @Mock LabSessionHolder             labSessionHolder;
    @Mock LabChannelHolder             labChannelHolder;

    @InjectMocks LabSipClient labSipClient;

    @BeforeEach
    void stubProps() {
        lenient().when(clientProps.getClientId()).thenReturn("34020000001320000001");
        lenient().when(clientProps.getDomain()).thenReturn("127.0.0.1");
        lenient().when(clientProps.getPort()).thenReturn(5061);
        lenient().when(clientProps.getRealm()).thenReturn("34020000");
        lenient().when(clientProps.getPassword()).thenReturn("123456");
        lenient().when(clientProps.getTransport()).thenReturn("UDP");
        lenient().when(serverProps.getServerId()).thenReturn("34020000002000000001");
        lenient().when(serverProps.getIp()).thenReturn("127.0.0.1");
        lenient().when(serverProps.getPort()).thenReturn(5060);
        lenient().when(serverProps.getDomain()).thenReturn("34020000002000000001");
    }

    // ── buildTo ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("holder 为空 → buildTo 用 server props（自环 5060）")
    void buildTo_selfLoop_whenHolderNull() {
        when(labSessionHolder.current()).thenReturn(null);
        ToDevice to = labSipClient.buildTo();
        assertThat(to.getUserId()).isEqualTo("34020000002000000001");
        assertThat(to.getIp()).isEqualTo("127.0.0.1");
        assertThat(to.getPort()).isEqualTo(5060);
        assertThat(to.getTransport()).isEqualTo("UDP");
    }

    @Test
    @DisplayName("holder 设外部 → buildTo 用 holder（外部平台 + TCP）")
    void buildTo_external_whenHolderSet() {
        when(labSessionHolder.current()).thenReturn(new LabSessionHolder.Snapshot(
            "44010000001110000001", "10.0.0.9", 15060, "4401000000", "TCP", null, null));
        ToDevice to = labSipClient.buildTo();
        assertThat(to.getUserId()).isEqualTo("44010000001110000001");
        assertThat(to.getIp()).isEqualTo("10.0.0.9");
        assertThat(to.getPort()).isEqualTo(15060);
        assertThat(to.getHostAddress()).isEqualTo("10.0.0.9:15060");
        assertThat(to.getRealm()).isEqualTo("44010000");
        assertThat(to.getTransport()).isEqualTo("TCP");
    }

    // ── buildFrom ────────────────────────────────────────────────────────

    @Test
    @DisplayName("holder 为空 → buildFrom 用 client props 身份，ip/port=5061")
    void buildFrom_fallback() {
        when(labSessionHolder.current()).thenReturn(null);
        FromDevice from = labSipClient.buildFrom();
        assertThat(from.getUserId()).isEqualTo("34020000001320000001");
        assertThat(from.getIp()).isEqualTo("127.0.0.1");
        assertThat(from.getPort()).isEqualTo(5061);
        assertThat(from.getPassword()).isEqualTo("123456");
    }

    @Test
    @DisplayName("holder 设身份 → buildFrom 覆盖 clientId/password，ip/port 恒为 5061")
    void buildFrom_identityOverride_ipPortFixed() {
        when(labSessionHolder.current()).thenReturn(new LabSessionHolder.Snapshot(
            "srv", "10.0.0.9", 15060, null, "TCP", "myDevice", "myPwd"));
        FromDevice from = labSipClient.buildFrom();
        assertThat(from.getUserId()).isEqualTo("myDevice");
        assertThat(from.getPassword()).isEqualTo("myPwd");
        // ip/port 不随目标改
        assertThat(from.getIp()).isEqualTo("127.0.0.1");
        assertThat(from.getPort()).isEqualTo(5061);
    }

    @Test
    @DisplayName("device-password 优先于 client props（holder 无密码时）")
    void buildFrom_labDevicePassword_overProps() {
        when(labSessionHolder.current()).thenReturn(null);
        ReflectionTestUtils.setField(labSipClient, "labDevicePassword", "labPwd");
        assertThat(labSipClient.buildFrom().getPassword()).isEqualTo("labPwd");
    }

    // ── pushCatalog 通道名格式 ─────────────────────────────────────────────

    @Test
    @DisplayName("pushCatalog 通道名为 prefix+i（不含 \"-ch\"），与被动回应同格式")
    void pushCatalog_nameFormat_noDashCh() {
        when(labSessionHolder.current()).thenReturn(null);
        when(labChannelHolder.current()).thenReturn(new LabChannelHolder.Config(4, "Lab-ch"));

        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            ArgumentCaptor<DeviceResponse> captor = ArgumentCaptor.forClass(DeviceResponse.class);
            mocked.when(() -> ClientCommandSender.sendCatalogCommand(any(), any(), captor.capture()))
                .thenReturn("callId-1");

            // 传入 count=2、name 空白 → 用 holder 前缀 Lab-ch
            labSipClient.pushCatalog(2, null);

            DeviceResponse resp = captor.getValue();
            assertThat(resp.getSumNum()).isEqualTo(2);
            assertThat(resp.getDeviceItemList()).hasSize(2);
            // 格式为 prefix+i（不是旧的 prefix+"-ch"+i）：以 Lab-ch 前缀验证不会出现重复 "-ch"
            assertThat(resp.getDeviceItemList().get(0).getName()).isEqualTo("Lab-ch1");
            assertThat(resp.getDeviceItemList().get(1).getName()).isEqualTo("Lab-ch2");
        }
    }

    @Test
    @DisplayName("pushCatalog channelCount<=0 时回落 holder 配置数量")
    void pushCatalog_countFallback() {
        when(labSessionHolder.current()).thenReturn(null);
        when(labChannelHolder.current()).thenReturn(new LabChannelHolder.Config(5, "Cam"));

        try (MockedStatic<ClientCommandSender> mocked = mockStatic(ClientCommandSender.class)) {
            ArgumentCaptor<DeviceResponse> captor = ArgumentCaptor.forClass(DeviceResponse.class);
            mocked.when(() -> ClientCommandSender.sendCatalogCommand(any(), any(), captor.capture()))
                .thenReturn("callId-2");

            labSipClient.pushCatalog(0, "Cam");

            assertThat(captor.getValue().getDeviceItemList()).hasSize(5);
            assertThat(captor.getValue().getDeviceItemList().get(0).getName()).isEqualTo("Cam1");
        }
    }
}
