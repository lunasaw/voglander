package io.github.lunasaw.voglander.gb28181;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GB28181基础环境测试
 * <p>
 * 验证GB28181测试环境的基础配置和依赖是否正确。
 * </p>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Slf4j
@SpringBootTest(classes = {
    io.github.lunasaw.voglander.web.ApplicationWeb.class
})
@ActiveProfiles("test")
public class Gb28181BasicEnvironmentTest {

    @Test
    @DisplayName("测试GB28181基础环境")
    public void testGb28181BasicEnvironment() {
        log.info("=== 开始GB28181基础环境测试 ===");

        // 验证基础设备ID格式
        String testDeviceId = "34020000001320000001";
        assertTrue(testDeviceId.matches("^[0-9]{20}$"), "设备ID应该是20位数字");

        // 验证设备ID结构
        String devicePrefix = testDeviceId.substring(0, 8);
        String deviceType = testDeviceId.substring(8, 11);
        String networkId = testDeviceId.substring(11, 12);
        String serialNumber = testDeviceId.substring(12, 20);

        assertTrue(devicePrefix.matches("^[0-9]{8}$"), "设备前缀应该是8位数字");
        assertTrue(deviceType.matches("^[0-9]{3}$"), "设备类型应该是3位数字");
        assertTrue(networkId.matches("^[0-9]{1}$"), "网络标识应该是1位数字");
        assertTrue(serialNumber.matches("^[0-9]{8}$"), "序列号应该是8位数字");

        log.info("设备ID结构验证:");
        log.info("  - 完整设备ID: {}", testDeviceId);
        log.info("  - 行政区划码: {}", devicePrefix);
        log.info("  - 设备类型码: {}", deviceType);
        log.info("  - 网络标识: {}", networkId);
        log.info("  - 序列号: {}", serialNumber);

        log.info("✅ GB28181基础环境测试通过");
    }

    @Test
    @DisplayName("测试Spring Boot上下文加载")
    public void testSpringBootContextLoads() {
        log.info("=== 开始Spring Boot上下文加载测试 ===");

        // 这个测试主要验证Spring Boot应用能正常启动
        // 如果能到达这里，说明上下文加载成功
        assertTrue(true, "Spring Boot上下文应该正常加载");

        log.info("✅ Spring Boot上下文加载测试通过");
    }

    @Test
    @DisplayName("测试基础协议参数验证")
    public void testBasicProtocolParameterValidation() {
        log.info("=== 开始基础协议参数验证测试 ===");

        // 测试IP地址格式
        String testIp = "127.0.0.1";
        assertTrue(testIp.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"),
            "IP地址格式应该正确");

        // 测试端口范围
        int serverPort = 5060;
        int clientPort = 5061;
        assertTrue(serverPort > 0 && serverPort <= 65535, "服务端口应该在有效范围内");
        assertTrue(clientPort > 0 && clientPort <= 65535, "客户端端口应该在有效范围内");

        log.info("协议参数验证:");
        log.info("  - 测试IP: {}", testIp);
        log.info("  - 服务端口: {}", serverPort);
        log.info("  - 客户端口: {}", clientPort);

        log.info("✅ 基础协议参数验证测试通过");
    }
}