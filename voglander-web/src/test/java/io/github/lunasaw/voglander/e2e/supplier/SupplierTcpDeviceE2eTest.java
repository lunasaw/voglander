package io.github.lunasaw.voglander.e2e.supplier;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.e2e.BaseE2eTest;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier.VoglanderServerDeviceSupplier;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Supplier TCP/streamMode 场景端到端测试。
 * <p>
 * 验证 {@link VoglanderServerDeviceSupplier#getToDevice(String)} 在不同 transport/streamMode 组合下的行为：
 * <ul>
 *   <li>TCP-ACTIVE / TCP-PASSIVE 连字符与下划线格式归一化</li>
 *   <li>UDP 传输模式正确映射</li>
 *   <li>密码、hostAddress、默认兜底等通用字段逻辑</li>
 * </ul>
 * </p>
 * <p>
 * 继承 {@link BaseE2eTest} 以获得完整的 Spring 上下文、数据库访问、缓存隔离。
 * 每个测试方法通过 nextId() 获取唯一 deviceId 避免缓存污染。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class SupplierTcpDeviceE2eTest extends BaseE2eTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private DeviceMapper                  deviceMapper;
    @Autowired private VoglanderServerDeviceSupplier serverSupplier;

    private String lastId;

    @AfterEach
    void cleanup() {
        if (lastId != null) {
            deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, lastId));
        }
    }

    private String nextId() {
        lastId = String.format("3402000000131%07d", SEQ.incrementAndGet());
        return lastId;
    }

    private ToDevice insertAndGet(String transport, String streamMode) {
        return insertAndGet(transport, streamMode, null);
    }

    private ToDevice insertAndGet(String transport, String streamMode, String password) {
        String deviceId = nextId();
        JSONObject ext = new JSONObject();
        ext.put("transport", transport);
        ext.put("streamMode", streamMode);
        ext.put("charset", "UTF-8");
        if (password != null) ext.put("password", password);

        DeviceDO d = new DeviceDO();
        d.setDeviceId(deviceId);
        d.setIp("192.168.1.10");
        d.setPort(5060);
        d.setStatus(1);
        d.setExtend(ext.toJSONString());
        d.setServerIp("127.0.0.1");
        d.setRegisterTime(LocalDateTime.now());
        d.setKeepaliveTime(LocalDateTime.now());
        deviceMapper.insert(d);

        return serverSupplier.getToDevice(deviceId);
    }

    // ── transport=TCP, streamMode 各格式 ─────────────────────────────────

    @Test @DisplayName("TCP + TCP-ACTIVE → streamMode=TCP-ACTIVE")
    void tcp_active_hyphen() {
        ToDevice to = insertAndGet("TCP", "TCP-ACTIVE");
        assertThat(to.getTransport()).isEqualTo("TCP");
        assertThat(to.getStreamMode()).isEqualTo("TCP-ACTIVE");
        assertThat(StreamModeEnum.isValid(to.getStreamMode())).isTrue();
        log.info("✅ TCP-ACTIVE(连字符) → {}", to.getStreamMode());
    }

    @Test @DisplayName("TCP + TCP-PASSIVE → streamMode=TCP-PASSIVE")
    void tcp_passive_hyphen() {
        ToDevice to = insertAndGet("TCP", "TCP-PASSIVE");
        assertThat(to.getTransport()).isEqualTo("TCP");
        assertThat(to.getStreamMode()).isEqualTo("TCP-PASSIVE");
        assertThat(StreamModeEnum.isValid(to.getStreamMode())).isTrue();
        log.info("✅ TCP-PASSIVE(连字符) → {}", to.getStreamMode());
    }

    @Test @DisplayName("TCP + TCP_ACTIVE(下划线) → streamMode=TCP-ACTIVE")
    void tcp_active_underscore() {
        ToDevice to = insertAndGet("TCP", "TCP_ACTIVE");
        assertThat(to.getTransport()).isEqualTo("TCP");
        assertThat(to.getStreamMode()).isEqualTo("TCP-ACTIVE");
        log.info("✅ TCP_ACTIVE(下划线) → {}", to.getStreamMode());
    }

    @Test @DisplayName("TCP + TCP_PASSIVE(下划线) → streamMode=TCP-PASSIVE")
    void tcp_passive_underscore() {
        ToDevice to = insertAndGet("TCP", "TCP_PASSIVE");
        assertThat(to.getTransport()).isEqualTo("TCP");
        assertThat(to.getStreamMode()).isEqualTo("TCP-PASSIVE");
        log.info("✅ TCP_PASSIVE(下划线) → {}", to.getStreamMode());
    }

    @Test @DisplayName("UDP + UDP → transport=UDP, streamMode=UDP")
    void udp_udp() {
        ToDevice to = insertAndGet("UDP", "UDP");
        assertThat(to.getTransport()).isEqualTo("UDP");
        assertThat(to.getStreamMode()).isEqualTo("UDP");
        log.info("✅ UDP/UDP → {}/{}", to.getTransport(), to.getStreamMode());
    }

    // ── 通用字段验证 ──────────────────────────────────────────────────────

    @Test @DisplayName("hostAddress = ip:port")
    void hostAddress_correct() {
        ToDevice to = insertAndGet("TCP", "TCP-ACTIVE");
        assertThat(to.getHostAddress()).isEqualTo("192.168.1.10:5060");
        assertThat(to.getCallId()).isNotBlank();
        assertThat(to.getToTag()).isNotBlank();
        assertThat(to.getExpires()).isEqualTo(3600);
    }

    @Test @DisplayName("password 正确传递")
    void password_propagated() {
        ToDevice to = insertAndGet("TCP", "TCP-ACTIVE", "mypass");
        assertThat(to.getPassword()).isEqualTo("mypass");
    }

    @Test @DisplayName("设备不存在 → createDefaultToDevice 兜底，不抛异常")
    void unknown_device_returns_default() {
        lastId = null; // 无需清理
        ToDevice to = serverSupplier.getToDevice("00000000000000000000");
        assertThat(to).isNotNull();
    }
}
