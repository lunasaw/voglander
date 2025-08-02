package io.github.lunasaw.voglander.gb28181;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.voglander.BaseTest;
import lombok.extern.slf4j.Slf4j;

import javax.sip.SipListener;

/**
 * GB28181集成测试基类
 * <p>
 * 提供GB28181协议测试的基础环境和工具方法，结合Voglander业务项目的测试基础设施。
 * 确保真实的消息发送模拟和协议一致性。
 * </p>
 * 
 * <h3>测试环境特性</h3>
 * <ul>
 * <li>完整的Spring Boot应用上下文</li>
 * <li>真实的SIP协议栈和消息传输</li>
 * <li>标准化的设备配置和管理</li>
 * <li>端到端的消息发送和接收验证</li>
 * <li>GB28181协议一致性保证</li>
 * </ul>
 * 
 * <h3>设备配置</h3>
 * <ul>
 * <li>服务端监听端口：5060</li>
 * <li>客户端监听端口：5061</li>
 * <li>支持UDP和TCP传输协议</li>
 * <li>标准GB28181设备ID格式</li>
 * </ul>
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
@TestPropertySource(properties = {
    "local.sip.server.enabled=true",
    "local.sip.server.ip=127.0.0.1",
    "local.sip.server.port=5060",
    "local.sip.server.domain=34020000002000000001",
    "local.sip.server.serverId=34020000002000000001",
    "local.sip.server.serverName=GB28181-Server",
    "local.sip.server.enableUdp=true",
    "local.sip.server.enableTcp=false",
    "local.sip.client.enabled=true",
    "local.sip.client.clientId=34020000001320000001",
    "local.sip.client.clientName=GB28181-Client",
    "local.sip.client.username=admin",
    "local.sip.client.password=123456",
    "local.sip.client.ip=127.0.0.1",
    "local.sip.client.port=5061",
    "local.sip.client.realm=34020000",
    "sip.enable=true",
    "sip.enable-log=true",
    "sip.server.enabled=true",
    "sip.client.enabled=true",
    "logging.level.io.github.lunasaw.sip=DEBUG",
    "logging.level.io.github.lunasaw.gbproxy=DEBUG"
})
public abstract class BaseGb28181IntegrationTest extends BaseTest {

    @Autowired(required = false)
    protected SipLayer            sipLayer;

    @Autowired(required = false)
    protected SipListener         sipListener;

    // ==================== 测试设备配置 ====================

    /**
     * 测试用设备ID（服务端设备）
     */
    protected static final String TEST_SERVER_DEVICE_ID = "34020000002000000001";

    /**
     * 测试用设备ID（客户端设备）
     */
    protected static final String TEST_CLIENT_DEVICE_ID = "34020000001320000001";

    /**
     * 测试用IP地址
     */
    protected static final String TEST_IP               = "127.0.0.1";

    /**
     * 测试用服务端口
     */
    protected static final int    TEST_SERVER_PORT      = 5060;

    /**
     * 测试用客户端端口
     */
    protected static final int    TEST_CLIENT_PORT      = 5061;

    // ==================== 测试环境验证 ====================

    /**
     * 测试前初始化
     */
    @BeforeEach
    protected void setupGb28181Test() {
        log.info("=== GB28181集成测试环境初始化 ===");

        if (sipLayer == null) {
            log.warn("SipLayer未注入，某些测试功能可能受限");
        }

        if (sipListener == null) {
            log.warn("SipListener未注入，某些测试功能可能受限");
        }

        log.info("GB28181测试环境初始化完成");
    }

    /**
     * 检查设备是否可用于测试
     * 
     * @return 设备是否可用
     */
    protected boolean isDeviceAvailable() {
        // 简化实现：总是返回true，在实际测试中可以根据需要扩展
        return true;
    }

    /**
     * 跳过设备不可用的测试
     * 
     * @param testName 测试名称
     */
    protected void skipIfDeviceNotAvailable(String testName) {
        if (!isDeviceAvailable()) {
            log.warn("跳过测试 {} - 设备不可用", testName);
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "设备不可用，跳过测试");
        }
    }

    // ==================== 设备ID生成工具 ====================

    /**
     * 生成测试用设备ID
     * 
     * @return 符合GB28181标准的20位设备ID
     */
    protected String generateTestDeviceId() {
        return TEST_CLIENT_DEVICE_ID;
    }

    /**
     * 生成测试用服务器ID
     * 
     * @return 符合GB28181标准的20位服务器ID
     */
    protected String generateTestServerId() {
        return TEST_SERVER_DEVICE_ID;
    }

    /**
     * 验证设备ID格式
     * 
     * @param deviceId 设备ID
     * @return 是否符合GB28181标准
     */
    protected boolean validateDeviceId(String deviceId) {
        return deviceId != null && deviceId.matches("^[0-9]{20}$");
    }

    // ==================== 设备对象创建工具 ====================

    /**
     * 获取客户端发送设备信息
     * 
     * @return FromDevice对象
     */
    protected FromDevice getClientFromDevice() {
        FromDevice fromDevice = new FromDevice();
        fromDevice.setUserId(TEST_CLIENT_DEVICE_ID);
        fromDevice.setIp(TEST_IP);
        fromDevice.setPort(TEST_CLIENT_PORT);
        fromDevice.setRealm(TEST_CLIENT_DEVICE_ID.substring(0, 8));
        return fromDevice;
    }

    /**
     * 获取服务端发送设备信息
     * 
     * @return FromDevice对象
     */
    protected FromDevice getServerFromDevice() {
        FromDevice fromDevice = new FromDevice();
        fromDevice.setUserId(TEST_SERVER_DEVICE_ID);
        fromDevice.setIp(TEST_IP);
        fromDevice.setPort(TEST_SERVER_PORT);
        fromDevice.setRealm(TEST_SERVER_DEVICE_ID.substring(0, 8));
        return fromDevice;
    }

    /**
     * 获取客户端目标设备信息
     * 
     * @return ToDevice对象
     */
    protected ToDevice getClientToDevice() {
        ToDevice toDevice = new ToDevice();
        toDevice.setUserId(TEST_SERVER_DEVICE_ID);
        toDevice.setIp(TEST_IP);
        toDevice.setPort(TEST_SERVER_PORT);
        toDevice.setRealm(TEST_SERVER_DEVICE_ID.substring(0, 8));
        return toDevice;
    }

    /**
     * 获取服务端目标设备信息
     * 
     * @param deviceId 目标设备ID
     * @return ToDevice对象
     */
    protected ToDevice getServerToDevice(String deviceId) {
        ToDevice toDevice = new ToDevice();
        toDevice.setUserId(deviceId != null ? deviceId : TEST_CLIENT_DEVICE_ID);
        toDevice.setIp(TEST_IP);
        toDevice.setPort(TEST_CLIENT_PORT);
        toDevice.setRealm(deviceId != null ? deviceId.substring(0, 8) : TEST_CLIENT_DEVICE_ID.substring(0, 8));
        return toDevice;
    }

    // ==================== 测试辅助方法 ====================

    /**
     * 等待异步操作完成
     * 
     * @param milliseconds 等待时间（毫秒）
     */
    protected void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待被中断: {}", e.getMessage());
        }
    }

    /**
     * 创建详细的设备信息用于测试
     * 
     * @param deviceId 设备ID
     * @return DeviceInfo对象
     */
    protected io.github.lunasaw.gb28181.common.entity.response.DeviceInfo createDetailedDeviceInfo(String deviceId) {
        io.github.lunasaw.gb28181.common.entity.response.DeviceInfo deviceInfo =
            new io.github.lunasaw.gb28181.common.entity.response.DeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceName("测试GB28181摄像头");
        deviceInfo.setManufacturer("Voglander测试");
        deviceInfo.setModel("VGL-CAM-001");
        deviceInfo.setFirmware("v2.1.0");
        deviceInfo.setChannel(1);
        return deviceInfo;
    }

    // ==================== 日志辅助方法 ====================

    /**
     * 记录测试开始日志
     * 
     * @param testName 测试名称
     */
    protected void logTestStart(String testName) {
        log.info("=== 开始执行测试: {} ===", testName);
    }

    /**
     * 记录测试完成日志
     * 
     * @param testName 测试名称
     */
    protected void logTestComplete(String testName) {
        log.info("✅ 测试完成: {}", testName);
    }

    /**
     * 记录测试跳过日志
     * 
     * @param testName 测试名称
     * @param reason 跳过原因
     */
    protected void logTestSkipped(String testName, String reason) {
        log.warn("⏭️ 跳过测试: {} - {}", testName, reason);
    }
}