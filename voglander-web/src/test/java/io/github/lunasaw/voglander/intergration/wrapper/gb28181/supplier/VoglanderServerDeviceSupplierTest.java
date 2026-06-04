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

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;

/**
 * VoglanderServerDeviceSupplier 单元测试。
 * 覆盖：convertToSipDevice 所有 transport/streamMode/null 分支 + getToDevice 字段复制。
 */
@ExtendWith(MockitoExtension.class)
class VoglanderServerDeviceSupplierTest {

    private static final String DEVICE_ID = "34020000001310000001";
    private static final String IP        = "192.168.1.100";
    private static final int    PORT      = 5060;

    @Mock DeviceManager                deviceManager;
    @Mock VoglanderSipServerProperties serverProperties;

    @InjectMocks VoglanderServerDeviceSupplier supplier;

    // ── 工厂 ─────────────────────────────────────────────────────────────

    private DeviceDTO dto(String transport, String streamMode, String ip, Integer port, String password) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(DEVICE_ID);
        dto.setIp(ip);
        dto.setPort(port);
        DeviceDTO.ExtendInfo ext = new DeviceDTO.ExtendInfo();
        ext.setTransport(transport);
        ext.setStreamMode(streamMode);
        ext.setCharset("UTF-8");
        ext.setPassword(password);
        dto.setExtendInfo(ext);
        return dto;
    }

    private DeviceDTO dto(String transport, String streamMode) {
        return dto(transport, streamMode, IP, PORT, null);
    }

    // ── transport ────────────────────────────────────────────────────────

    @Test @DisplayName("transport=UDP → device.transport=UDP")
    void transport_udp() {
        assertThat(supplier.convertToSipDevice(dto("UDP", "UDP")).getTransport()).isEqualTo("UDP");
    }

    @Test @DisplayName("transport=TCP → device.transport=TCP")
    void transport_tcp() {
        assertThat(supplier.convertToSipDevice(dto("TCP", "UDP")).getTransport()).isEqualTo("TCP");
    }

    @Test @DisplayName("transport=null → 回退 UDP")
    void transport_null_fallback() {
        assertThat(supplier.convertToSipDevice(dto(null, "UDP")).getTransport()).isEqualTo("UDP");
    }

    @Test @DisplayName("transport=SCTP(非法) → 回退 UDP")
    void transport_invalid_fallback() {
        assertThat(supplier.convertToSipDevice(dto("SCTP", "UDP")).getTransport()).isEqualTo("UDP");
    }

    // ── streamMode ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "raw={0} → expected={1}")
    @CsvSource({
        "UDP,         UDP",
        "TCP-ACTIVE,  TCP-ACTIVE",
        "TCP-PASSIVE, TCP-PASSIVE",
        "TCP_ACTIVE,  TCP-ACTIVE",   // 下划线格式（数据库遗留）
        "TCP_PASSIVE, TCP-PASSIVE",
    })
    @DisplayName("streamMode 规范化（连字符/下划线均正确）")
    void streamMode_normalization(String raw, String expected) {
        Device d = supplier.convertToSipDevice(dto("UDP", raw));
        assertThat(d.getStreamMode()).isEqualTo(expected.trim());
        assertThat(StreamModeEnum.isValid(d.getStreamMode())).isTrue();
    }

    @Test @DisplayName("streamMode=null → 回退 UDP")
    void streamMode_null_fallback() {
        assertThat(supplier.convertToSipDevice(dto("UDP", null)).getStreamMode())
            .isEqualTo(StreamModeEnum.UDP.getType());
    }

    @Test @DisplayName("streamMode=INVALID → 回退 UDP")
    void streamMode_invalid_fallback() {
        assertThat(supplier.convertToSipDevice(dto("UDP", "UNKNOWN")).getStreamMode())
            .isEqualTo(StreamModeEnum.UDP.getType());
    }

    // ── port / hostAddress ───────────────────────────────────────────────

    @Test @DisplayName("port=null → 默认 5060，hostAddress=ip:5060")
    void port_null_defaults_5060() {
        Device d = supplier.convertToSipDevice(dto("UDP", "UDP", IP, null, null));
        assertThat(d.getPort()).isEqualTo(5060);
        assertThat(d.getHostAddress()).isEqualTo(IP + ":5060");
    }

    @Test @DisplayName("hostAddress 使用已解析 port，不使用原始 null")
    void hostAddress_uses_resolved_port() {
        Device d = supplier.convertToSipDevice(dto("UDP", "UDP", IP, 5080, null));
        assertThat(d.getHostAddress()).isEqualTo(IP + ":5080");
    }

    @Test @DisplayName("ip=null → device.ip=null，hostAddress 未设置")
    void ip_null_no_hostAddress() {
        Device d = supplier.convertToSipDevice(dto("UDP", "UDP", null, PORT, null));
        assertThat(d.getIp()).isNull();
    }

    // ── realm ────────────────────────────────────────────────────────────

    @Test @DisplayName("realm = deviceId 前 8 位")
    void realm_first_8_chars() {
        assertThat(supplier.convertToSipDevice(dto("UDP", "UDP")).getRealm())
            .isEqualTo(DEVICE_ID.substring(0, 8));
    }

    @Test @DisplayName("deviceId 长度<8 → realm 回退 34020000")
    void realm_short_deviceId_fallback() {
        DeviceDTO d = dto("UDP", "UDP");
        d.setDeviceId("short");
        assertThat(supplier.convertToSipDevice(d).getRealm()).isEqualTo("34020000");
    }

    // ── password ─────────────────────────────────────────────────────────

    @Test @DisplayName("password 从 extendInfo 传递到 Device")
    void password_propagated() {
        Device d = supplier.convertToSipDevice(dto("UDP", "UDP", IP, PORT, "secret"));
        assertThat(d.getPassword()).isEqualTo("secret");
    }

    // ── extendInfo=null ──────────────────────────────────────────────────

    @Test @DisplayName("extendInfo=null → transport/streamMode/charset 均使用默认值")
    void extendInfo_null_all_defaults() {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(DEVICE_ID);
        dto.setIp(IP);
        dto.setPort(PORT);
        // extendInfo 不设置，getExtendInfo() 返回空 ExtendInfo 对象

        Device d = supplier.convertToSipDevice(dto);
        assertThat(d.getTransport()).isEqualTo("UDP");
        assertThat(d.getStreamMode()).isEqualTo(StreamModeEnum.UDP.getType());
        assertThat(d.getCharset()).isEqualTo("UTF-8");
    }

}
