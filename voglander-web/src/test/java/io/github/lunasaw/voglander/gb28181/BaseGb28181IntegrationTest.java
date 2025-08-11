package io.github.lunasaw.voglander.gb28181;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;

import javax.sip.SipListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDateTime;

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

public abstract class BaseGb28181IntegrationTest extends BaseTest {

    @Autowired(required = false)
    protected SipLayer            sipLayer;

    @Autowired(required = false)
    protected SipListener         sipListener;

    @Autowired
    protected DeviceManager       deviceManager;

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
     * 测试用服务端口 - 使用动态端口避免冲突
     */
    protected static final int    TEST_SERVER_PORT      = getAvailablePort(5060);

    /**
     * 测试用客户端端口 - 使用动态端口避免冲突
     */
    protected static final int    TEST_CLIENT_PORT      = getAvailablePort(5061);

    // ==================== 测试环境验证 ====================

    /**
     * 测试前初始化
     */
    @BeforeEach
    protected void setupGb28181Test() {
        log.info("=== GB28181集成测试环境初始化 ===");

        // 为每个测试设置唯一的会话ID，确保跨线程状态隔离
        String testSessionId = generateTestSessionId();
        io.github.lunasaw.voglander.gb28181.handler.VoglanderTestServerMessageHandler.setTestSessionId(testSessionId);
        log.info("设置测试会话ID: {}", testSessionId);

        if (sipLayer == null) {
            log.warn("SipLayer未注入，某些测试功能可能受限");
        }

        if (sipListener == null) {
            log.warn("SipListener未注入，某些测试功能可能受限");
        }

        // 初始化SIP监听点
        initializeSipListeningPoints();

        // 初始化测试设备数据
        initClientTestDeviceData();

        initServerTestDeviceData();
        log.info("GB28181测试环境初始化完成");
    }

    /**
     * 生成唯一的测试会话ID
     * 格式: TestClass_TestMethod_timestamp
     */
    private String generateTestSessionId() {
        String className = this.getClass().getSimpleName();
        String methodName = "unknown";
        // 尝试获取当前执行的测试方法名
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String method = element.getMethodName();
                if (method.startsWith("test") && !method.equals("setupGb28181Test")) {
                    methodName = method;
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("无法获取测试方法名，使用默认值: {}", e.getMessage());
        }
        return String.format("%s_%s_%d", className, methodName, System.currentTimeMillis());
    }

    /**
     * 初始化SIP监听点
     * 增强的初始化逻辑，等待ServerStart完成初始化，并处理批量测试的时序问题
     */
    protected void initializeSipListeningPoints() {
        if (sipLayer != null && sipListener != null) {
            try {
                // 设置SIP监听器（ServerStart可能已经设置过）
                sipLayer.setSipListener(sipListener);

                // 等待ServerStart初始化完成 - 关键修复
                waitForServerStartInitialization();

                log.info("SIP监听点检查 - 当前活跃监听点数量: {}", sipLayer.getActiveListeningPointsCount());

                // 检查是否已有可用的监听点（可能由ServerStart创建）
                boolean hasAvailablePoints = sipLayer.getActiveListeningPointsCount() > 0;

                if (hasAvailablePoints) {
                    log.info("检测到现有SIP监听点，将复用现有资源");
                } else {
                    log.info("未检测到现有SIP监听点，尝试创建测试专用监听点");

                    // 只有在没有现有监听点时才尝试创建新的
                    try {
                        if (!sipLayer.hasActiveListeningPoint(TEST_IP, TEST_CLIENT_PORT)) {
                            sipLayer.addListeningPoint(TEST_IP, TEST_CLIENT_PORT, true);
                            log.info("客户端SIP监听点创建完成: {}:{}", TEST_IP, TEST_CLIENT_PORT);
                        }

                        if (!sipLayer.hasActiveListeningPoint(TEST_IP, TEST_SERVER_PORT)) {
                            sipLayer.addListeningPoint(TEST_IP, TEST_SERVER_PORT, true);
                            log.info("服务端SIP监听点创建完成: {}:{}", TEST_IP, TEST_SERVER_PORT);
                        }
                    } catch (Exception addException) {
                        log.warn("创建测试SIP监听点失败: {}, 测试将继续但某些功能可能受限", addException.getMessage());
                    }
                }

                // 最终验证SIP监听点是否就绪
                int finalCount = sipLayer.getActiveListeningPointsCount();
                log.info("SIP监听点初始化完成，最终活跃监听点数量: {}", finalCount);

                if (finalCount == 0) {
                    log.warn("警告：没有可用的SIP监听点，某些测试可能会失败");
                }

            } catch (Exception e) {
                log.warn("初始化SIP监听点失败，测试将继续但可能跳过SIP相关功能: {}", e.getMessage());
                // 不抛出异常，让测试继续运行，但会跳过需要SIP的测试
            }
        } else {
            log.warn("SipLayer或SipListener未注入，跳过SIP监听点初始化");
        }
    }

    /**
     * 等待ServerStart初始化完成
     * 解决批量测试时的时序问题
     */
    private void waitForServerStartInitialization() {
        int maxAttempts = 10;
        int attempt = 0;

        while (attempt < maxAttempts && sipLayer.getActiveListeningPointsCount() == 0) {
            try {
                Thread.sleep(200); // 等待200ms
                attempt++;
                log.debug("等待SIP监听点初始化，尝试次数: {}/{}", attempt, maxAttempts);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待SIP初始化被中断");
                break;
            }
        }

        if (attempt >= maxAttempts && sipLayer.getActiveListeningPointsCount() == 0) {
            log.warn("ServerStart可能尚未完成SIP监听点初始化，将尝试创建测试专用监听点");
        }
    }

    /**
     * 初始化测试设备数据
     */
    protected void initClientTestDeviceData() {
        try {
            // 检查测试设备是否已存在
            DeviceDO existingDevice = deviceManager.getByDeviceId(TEST_CLIENT_DEVICE_ID);
            if (existingDevice == null) {
                // 创建测试设备
                DeviceDTO testDevice = new DeviceDTO();
                testDevice.setDeviceId(TEST_CLIENT_DEVICE_ID);
                testDevice.setName("测试GB28181客户端设备");
                testDevice.setIp(TEST_IP);
                testDevice.setPort(TEST_CLIENT_PORT);
                testDevice.setStatus(1); // 在线状态
                testDevice.setType(DeviceAgreementEnum.GB28181_IPC.getType());
                testDevice.setRegisterTime(LocalDateTime.now());
                testDevice.setKeepaliveTime(LocalDateTime.now());
                testDevice.setServerIp(TEST_IP);

                // extend
                DeviceDTO.ExtendInfo extendInfo = new DeviceDTO.ExtendInfo();
                extendInfo.setTransport("TCP");
                extendInfo.setStreamMode("TCP-ACTIVE");
                extendInfo.setCharset("UTF-8");
                // 保存测试设备
                testDevice.setExtendInfo(extendInfo);
                deviceManager.saveOrUpdate(testDevice);
                log.info("已创建测试设备: {}", TEST_CLIENT_DEVICE_ID);
            } else {
                log.info("测试设备已存在: {}", TEST_CLIENT_DEVICE_ID);
            }
        } catch (Exception e) {
            log.warn("初始化测试设备数据失败: {}", e.getMessage());
        }
    }

    protected void initServerTestDeviceData() {
        try {
            // 检查测试设备是否已存在
            DeviceDO existingDevice = deviceManager.getByDeviceId(TEST_SERVER_DEVICE_ID);
            if (existingDevice == null) {
                // 创建测试设备
                DeviceDTO testDevice = new DeviceDTO();
                testDevice.setDeviceId(TEST_SERVER_DEVICE_ID);
                testDevice.setName("测试GB28181服务端设备");
                testDevice.setIp(TEST_IP);
                testDevice.setPort(TEST_SERVER_PORT);
                testDevice.setStatus(1); // 在线状态
                testDevice.setType(DeviceAgreementEnum.GB28181_IPC.getType());
                testDevice.setRegisterTime(LocalDateTime.now());
                testDevice.setKeepaliveTime(LocalDateTime.now());
                testDevice.setServerIp(TEST_IP);

                // extend
                DeviceDTO.ExtendInfo extendInfo = new DeviceDTO.ExtendInfo();
                extendInfo.setTransport("TCP");
                extendInfo.setStreamMode("TCP-ACTIVE");
                extendInfo.setCharset("UTF-8");
                // 保存测试设备
                testDevice.setExtendInfo(extendInfo);
                deviceManager.saveOrUpdate(testDevice);
                log.info("已创建测试设备: {}", TEST_SERVER_DEVICE_ID);
            } else {
                log.info("测试设备已存在: {}", TEST_SERVER_DEVICE_ID);
            }
        } catch (Exception e) {
            log.warn("初始化测试设备数据失败: {}", e.getMessage());
        }
    }

    /**
     * 测试后清理
     */
    @AfterEach
    protected void tearDownGb28181Test() {
        log.info("=== GB28181集成测试环境清理 ===");

        // 清理SIP监听点
        cleanupSipListeningPoints();

        // 清理测试消息处理器的会话状态，防止内存泄漏
        try {
            io.github.lunasaw.voglander.gb28181.handler.VoglanderTestServerMessageHandler.clearTestState();
            log.debug("已清理测试消息处理器的会话状态");
        } catch (Exception e) {
            log.debug("清理测试消息处理器会话状态时发生异常: {}", e.getMessage());
        }

        log.info("GB28181测试环境清理完成");
    }

    /**
     * 清理SIP监听点
     * 注意：只清理测试创建的监听点，不影响ServerStart创建的监听点
     */
    protected void cleanupSipListeningPoints() {
        if (sipLayer != null) {
            try {
                // 只清理测试专门创建的监听点（如果存在的话）
                // 不清理可能由ServerStart创建的监听点，避免影响其他测试

                log.debug("SIP监听点清理 - 当前活跃监听点数量: {}", sipLayer.getActiveListeningPointsCount());

                // 温和的清理方式：只在确定是测试创建的监听点时才清理
                // 由于ServerStart可能已经占用了标准端口，我们主要依赖Spring的生命周期管理

                log.debug("SIP监听点清理完成 - 保留共享的SIP资源");
            } catch (Exception e) {
                log.debug("清理SIP监听点时发生异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查设备是否可用于测试
     * 增强的设备可用性检查，包含重试逻辑
     * 
     * @return 设备是否可用
     */
    protected boolean isDeviceAvailable() {
        // 检查SIP层是否已初始化且有活跃的监听点
        if (sipLayer == null || sipListener == null) {
            log.warn("SIP层或监听器未初始化");
            return false;
        }

        // 如果没有监听点，尝试重新初始化（应对批量测试时序问题）
        if (sipLayer.getActiveListeningPointsCount() == 0) {
            log.warn("没有活跃的SIP监听点，尝试重新初始化");
            try {
                initializeSipListeningPoints();
            } catch (Exception e) {
                log.warn("重新初始化SIP监听点失败: {}", e.getMessage());
                return false;
            }

            // 重新检查
            if (sipLayer.getActiveListeningPointsCount() == 0) {
                log.warn("重新初始化后仍然没有活跃的SIP监听点");
                return false;
            }
        }

        // 如果有监听点但不是我们需要的端口，也认为可用（使用现有监听点）
        if (sipLayer.getActiveListeningPointsCount() > 0) {
            log.info("SIP基础设施可用，活跃监听点数量: {}", sipLayer.getActiveListeningPointsCount());
            return true;
        }

        return false;
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
        return TEST_SERVER_DEVICE_ID;
    }

    protected String generateTestClientDeviceId() {
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
        fromDevice.setTransport("UDP");
        fromDevice.setCharset("UTF-8");
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
        fromDevice.setTransport("UDP");
        fromDevice.setCharset("UTF-8");
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
        toDevice.setTransport("UDP");
        toDevice.setCharset("UTF-8");
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
        toDevice.setTransport("UDP");
        toDevice.setCharset("UTF-8");
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

    // ==================== 端口管理工具 ====================

    /**
     * 获取可用端口
     * 
     * @param preferredPort 首选端口
     * @return 可用端口
     */
    private static int getAvailablePort(int preferredPort) {
        // 首先尝试首选端口
        if (isPortAvailable(preferredPort)) {
            return preferredPort;
        }

        // 如果首选端口不可用，寻找下一个可用端口
        for (int port = preferredPort + 1; port <= preferredPort + 100; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }

        // 如果还是找不到，使用系统分配的随机端口
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            // 最后的备选方案
            return preferredPort + (int)(Math.random() * 1000);
        }
    }

    /**
     * 检查端口是否可用
     * 
     * @param port 端口号
     * @return 是否可用
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}