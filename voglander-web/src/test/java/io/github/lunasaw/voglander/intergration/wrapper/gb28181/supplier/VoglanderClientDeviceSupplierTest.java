package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabSessionHolder;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;

/**
 * VoglanderClientDeviceSupplier.convertToSipDevice 单元测试。
 * Client supplier 未做 transport/streamMode 规范化，此处验证现有行为并记录边界。
 */
@ExtendWith(MockitoExtension.class)
class VoglanderClientDeviceSupplierTest {

    private static final String DEVICE_ID = "34020000001320000001";
    private static final String SERVER_ID = "34020000002000000001";
    private static final String IP        = "192.168.1.200";
    private static final int    PORT      = 5061;

    @Mock DeviceManager                deviceManager;
    @Mock VoglanderSipClientProperties clientProperties;
    @Mock VoglanderSipServerProperties serverProperties;

    @InjectMocks VoglanderClientDeviceSupplier supplier;

    // ── 工厂 ─────────────────────────────────────────────────────────────

    private DeviceDTO dto(String transport, String streamMode, String ip, Integer port) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(DEVICE_ID);
        dto.setIp(ip);
        dto.setPort(port);
        DeviceDTO.ExtendInfo ext = new DeviceDTO.ExtendInfo();
        ext.setTransport(transport);
        ext.setStreamMode(streamMode);
        ext.setCharset("UTF-8");
        dto.setExtendInfo(ext);
        return dto;
    }

    // ── 基础字段映射 ─────────────────────────────────────────────────────

    @Test @DisplayName("deviceId / ip / port 正确映射到 Device")
    void basic_fields_mapped() {
        Device d = supplier.convertToSipDevice(dto("UDP", "UDP", IP, PORT));
        assertThat(d.getUserId()).isEqualTo(DEVICE_ID);
        assertThat(d.getIp()).isEqualTo(IP);
        assertThat(d.getPort()).isEqualTo(PORT);
    }

    @Test @DisplayName("realm = deviceId 前 8 位")
    void realm_first_8_chars() {
        assertThat(supplier.convertToSipDevice(dto("UDP", "UDP", IP, PORT)).getRealm())
            .isEqualTo(DEVICE_ID.substring(0, 8));
    }

    @Test @DisplayName("deviceId 长度<8 → realm 回退 34020000")
    void realm_short_deviceId_fallback() {
        DeviceDTO d = dto("UDP", "UDP", IP, PORT);
        d.setDeviceId("short");
        assertThat(supplier.convertToSipDevice(d).getRealm()).isEqualTo("34020000");
    }

    // ── transport ────────────────────────────────────────────────────────

    @ParameterizedTest(name = "transport={0} 直接透传")
    @CsvSource({"UDP", "TCP"})
    @DisplayName("transport 值直接透传（Client 不做校验）")
    void transport_passthrough(String transport) {
        assertThat(supplier.convertToSipDevice(dto(transport, "UDP", IP, PORT)).getTransport())
            .isEqualTo(transport);
    }

    // ── streamMode ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "streamMode={0} 直接透传")
    @CsvSource({"UDP", "TCP-ACTIVE", "TCP-PASSIVE"})
    @DisplayName("streamMode 值直接透传（Client 不做规范化）")
    void streamMode_passthrough(String streamMode) {
        assertThat(supplier.convertToSipDevice(dto("UDP", streamMode, IP, PORT)).getStreamMode())
            .isEqualTo(streamMode);
    }

    // ── extendInfo=null NPE 防护 ──────────────────────────────────────────

    @Test @DisplayName("extendInfo=null → 回退默认值，不抛 NPE（修复后行为）")
    void extendInfo_null_no_npe() {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(DEVICE_ID);
        dto.setIp(IP);
        dto.setPort(PORT);
        // 不设置 extendInfo

        Device d = supplier.convertToSipDevice(dto);
        assertThat(d.getUserId()).isEqualTo(DEVICE_ID);
        assertThat(d.getTransport()).isEqualTo("UDP");
        assertThat(d.getStreamMode()).isEqualTo("UDP");
        assertThat(d.getCharset()).isEqualTo("UTF-8");
    }

    // ── Lab 自环：目标=平台自身时从 server props 兜底 ───────────────────────

    @Test @DisplayName("DB 查不到 + deviceId=serverId → 从 server props 构造平台 ToDevice")
    void getToDevice_labServer_fallback() {
        when(deviceManager.getDtoByDeviceId(SERVER_ID)).thenReturn(null);
        when(serverProperties.getServerId()).thenReturn(SERVER_ID);
        when(serverProperties.getIp()).thenReturn("127.0.0.1");
        when(serverProperties.getPort()).thenReturn(5060);
        when(serverProperties.getDomain()).thenReturn(SERVER_ID);

        ToDevice to = supplier.getToDevice(SERVER_ID);

        assertThat(to).isNotNull();
        assertThat(to.getUserId()).isEqualTo(SERVER_ID);
        assertThat(to.getIp()).isEqualTo("127.0.0.1");
        assertThat(to.getPort()).isEqualTo(5060);
        assertThat(to.getRealm()).isEqualTo(SERVER_ID.substring(0, 8));
        assertThat(to.getTransport()).isEqualTo("UDP");
    }

    @Test @DisplayName("getDevice：DB 查不到 + deviceId=serverId → 同样兜底返回平台 ToDevice")
    void getDevice_labServer_fallback() {
        when(deviceManager.getDtoByDeviceId(SERVER_ID)).thenReturn(null);
        when(serverProperties.getServerId()).thenReturn(SERVER_ID);
        when(serverProperties.getIp()).thenReturn("127.0.0.1");
        when(serverProperties.getPort()).thenReturn(5060);
        when(serverProperties.getDomain()).thenReturn(SERVER_ID);

        Device d = supplier.getDevice(SERVER_ID);

        assertThat(d).isNotNull();
        assertThat(d.getUserId()).isEqualTo(SERVER_ID);
        assertThat(d.getIp()).isEqualTo("127.0.0.1");
        assertThat(d.getPort()).isEqualTo(5060);
    }

    @Test @DisplayName("普通设备 DB 查不到 + deviceId≠serverId → 仍返回 null（不污染常规路径）")
    void getToDevice_unknownDevice_returnsNull() {
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(null);
        lenient().when(serverProperties.getServerId()).thenReturn(SERVER_ID);

        assertThat(supplier.getToDevice(DEVICE_ID)).isNull();
    }

    // ── B3: 外部目标（holder 命中）401 兜底 ────────────────────────────────

    private static final String EXTERNAL_SERVER_ID = "44010000002000000001";

    @SuppressWarnings("unchecked")
    private void injectHolder(LabSessionHolder.Snapshot snapshot) {
        LabSessionHolder holder = new LabSessionHolder();
        if (snapshot != null) {
            holder.apply(snapshot);
        }
        ObjectProvider<LabSessionHolder> provider = org.mockito.Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(holder);
        ReflectionTestUtils.setField(supplier, "labSessionHolderProvider", provider);
    }

    @Test @DisplayName("holder 设外部 serverId + DB miss → 返回外部 ToDevice")
    void getToDevice_externalTarget_fromHolder() {
        injectHolder(new LabSessionHolder.Snapshot(
            EXTERNAL_SERVER_ID, "10.0.0.9", 15060, "4401000000", "TCP", null, null));
        when(deviceManager.getDtoByDeviceId(EXTERNAL_SERVER_ID)).thenReturn(null);

        ToDevice to = supplier.getToDevice(EXTERNAL_SERVER_ID);

        assertThat(to).isNotNull();
        assertThat(to.getUserId()).isEqualTo(EXTERNAL_SERVER_ID);
        assertThat(to.getIp()).isEqualTo("10.0.0.9");
        assertThat(to.getPort()).isEqualTo(15060);
        assertThat(to.getHostAddress()).isEqualTo("10.0.0.9:15060");
        assertThat(to.getRealm()).isEqualTo("44010000");
        assertThat(to.getTransport()).isEqualTo("TCP");
    }

    @Test @DisplayName("holder 设外部，但查询 deviceId≠外部 serverId 且≠本地 → null")
    void getToDevice_externalHolder_otherDevice_null() {
        injectHolder(new LabSessionHolder.Snapshot(
            EXTERNAL_SERVER_ID, "10.0.0.9", 15060, null, "TCP", null, null));
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(null);
        lenient().when(serverProperties.getServerId()).thenReturn(SERVER_ID);

        assertThat(supplier.getToDevice(DEVICE_ID)).isNull();
    }

    @Test @DisplayName("provider 注入但 holder 当前为空（自环）→ 仍只认本地 serverId")
    void getToDevice_holderEmpty_fallsBackToLocal() {
        injectHolder(null);   // holder 存在但 current()=null
        when(deviceManager.getDtoByDeviceId(SERVER_ID)).thenReturn(null);
        when(serverProperties.getServerId()).thenReturn(SERVER_ID);
        when(serverProperties.getIp()).thenReturn("127.0.0.1");
        when(serverProperties.getPort()).thenReturn(5060);
        when(serverProperties.getDomain()).thenReturn(SERVER_ID);

        ToDevice to = supplier.getToDevice(SERVER_ID);
        assertThat(to).isNotNull();
        assertThat(to.getUserId()).isEqualTo(SERVER_ID);
        assertThat(to.getPort()).isEqualTo(5060);
    }
}
