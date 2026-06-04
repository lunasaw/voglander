package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;

/**
 * VoglanderClientDeviceSupplier.convertToSipDevice 单元测试。
 * Client supplier 未做 transport/streamMode 规范化，此处验证现有行为并记录边界。
 */
@ExtendWith(MockitoExtension.class)
class VoglanderClientDeviceSupplierTest {

    private static final String DEVICE_ID = "34020000001320000001";
    private static final String IP        = "192.168.1.200";
    private static final int    PORT      = 5061;

    @Mock DeviceManager                deviceManager;
    @Mock VoglanderSipClientProperties clientProperties;

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
}
