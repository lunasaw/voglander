package io.github.lunasaw.voglander.gb28181.diagnostic;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.voglander.gb28181.BaseGb28181IntegrationTest;
import io.github.lunasaw.voglander.gb28181.handler.VoglanderTestClientMessageHandler;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SIP消息路由诊断测试类
 * <p>
 * 用于诊断和修复GB28181测试中SIP消息传递问题。
 * 分析消息路由配置、处理器注册和SIP层集成状态。
 * </p>
 *
 * @author luna
 * @since 2025/8/11
 * @version 1.0
 */
@Slf4j
public class SipMessageRoutingDiagnosticTest extends BaseGb28181IntegrationTest {

    @Autowired
    private VoglanderServerDeviceCommand deviceCommand;

    @Autowired
    private ApplicationContext           applicationContext;

    @Test
    @DisplayName("诊断Spring Bean注册状态")
    public void testSpringBeanRegistration() {
        log.info("=== Spring Bean注册状态诊断 ===");

        // 检查所有MessageRequestHandler类型的Bean
        Map<String, MessageRequestHandler> handlerBeans =
            applicationContext.getBeansOfType(MessageRequestHandler.class);

        log.info("发现 {} 个MessageRequestHandler Bean:", handlerBeans.size());

        for (Map.Entry<String, MessageRequestHandler> entry : handlerBeans.entrySet()) {
            String beanName = entry.getKey();
            MessageRequestHandler handler = entry.getValue();
            String className = handler.getClass().getSimpleName();

            // 获取是否为Primary的信息（简化版本，避免类型转换问题）
            boolean isPrimary = false;
            try {
                if (applicationContext instanceof ConfigurableApplicationContext) {
                    ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext)applicationContext;
                    isPrimary = configurableContext.getBeanFactory().getBeanDefinition(beanName).isPrimary();
                }
            } catch (Exception e) {
                log.debug("无法获取Bean的Primary状态: {}", e.getMessage());
            }

            log.info("Bean名称: {}, 类名: {}, 是否Primary: {}", beanName, className, isPrimary);

            // 检查是否是我们的测试处理器
            if (handler instanceof VoglanderTestClientMessageHandler) {
                log.info("✅ 找到VoglanderTestClientMessageHandler, Bean名称: {}", beanName);
            }
        }

        // 验证测试处理器存在且为Primary
        assertFalse(handlerBeans.isEmpty(), "应该至少有一个MessageRequestHandler Bean");
        assertTrue(handlerBeans.containsValue(applicationContext.getBean(VoglanderTestClientMessageHandler.class)),
            "VoglanderTestClientMessageHandler应该被注册为Spring Bean");

        log.info("Spring Bean注册状态检查通过 ✅");
    }

    @Test
    @DisplayName("诊断SIP基础设施状态")
    public void testSipInfrastructureStatus() {
        log.info("=== SIP基础设施状态诊断 ===");

        // 检查SIP层是否可用
        if (sipLayer == null) {
            log.error("❌ SipLayer未注入");
            fail("SipLayer未注入，无法进行SIP测试");
        }

        if (sipListener == null) {
            log.error("❌ SipListener未注入");
            fail("SipListener未注入，无法进行SIP测试");
        }

        // 检查监听点状态
        int activePoints = sipLayer.getActiveListeningPointsCount();
        log.info("当前活跃SIP监听点数量: {}", activePoints);

        if (activePoints == 0) {
            log.warn("⚠️ 没有活跃的SIP监听点，尝试重新初始化");
            initializeSipListeningPoints();
            activePoints = sipLayer.getActiveListeningPointsCount();
            log.info("重新初始化后的监听点数量: {}", activePoints);
        }

        assertTrue(activePoints > 0, "应该至少有一个活跃的SIP监听点");
        log.info("SIP基础设施状态检查通过 ✅");
    }

    @Test
    @DisplayName("诊断设备数据和路由配置")
    public void testDeviceDataAndRouting() {
        log.info("=== 设备数据和路由配置诊断 ===");

        // 检查测试设备数据
        try {
            var clientDevice = deviceManager.getByDeviceId(TEST_CLIENT_DEVICE_ID);
            var serverDevice = deviceManager.getByDeviceId(TEST_SERVER_DEVICE_ID);

            if (clientDevice != null) {
                log.info("客户端测试设备存在: {}, IP: {}, Port: {}",
                    clientDevice.getDeviceId(), clientDevice.getIp(), clientDevice.getPort());
            } else {
                log.warn("⚠️ 客户端测试设备不存在: {}", TEST_CLIENT_DEVICE_ID);
            }

            if (serverDevice != null) {
                log.info("服务端测试设备存在: {}, IP: {}, Port: {}",
                    serverDevice.getDeviceId(), serverDevice.getIp(), serverDevice.getPort());
            } else {
                log.warn("⚠️ 服务端测试设备不存在: {}", TEST_SERVER_DEVICE_ID);
            }

            // 验证设备ID格式
            assertTrue(validateDeviceId(TEST_CLIENT_DEVICE_ID), "客户端设备ID格式应该正确");
            assertTrue(validateDeviceId(TEST_SERVER_DEVICE_ID), "服务端设备ID格式应该正确");

        } catch (Exception e) {
            log.error("设备数据检查失败: {}", e.getMessage());
            fail("无法检查设备数据: " + e.getMessage());
        }

        log.info("设备数据和路由配置检查通过 ✅");
    }

    @Test
    @DisplayName("诊断消息处理器调用路径")
    public void testMessageHandlerCallPath() throws Exception {
        skipIfDeviceNotAvailable("消息处理器调用路径诊断");

        log.info("=== 消息处理器调用路径诊断 ===");

        // 重置测试状态，添加额外日志
        log.info("重置测试状态...");
        VoglanderTestClientMessageHandler.resetTestState();

        // 获取处理器实例，验证其状态
        VoglanderTestClientMessageHandler handler =
            applicationContext.getBean(VoglanderTestClientMessageHandler.class);
        assertNotNull(handler, "应该能够获取到测试消息处理器实例");

        log.info("测试消息处理器实例: {}", handler.getClass().getSimpleName());

        // 发送测试命令
        String testDeviceId = generateTestClientDeviceId();
        log.info("向设备 {} 发送状态查询命令", testDeviceId);

        var result = deviceCommand.queryDeviceStatus(testDeviceId);
        assertNotNull(result, "命令执行结果不应为空");

        if (result.isSuccess()) {
            log.info("✅ 命令发送成功: {}", result.getMessage());
        } else {
            log.error("❌ 命令发送失败: {}", result.getMessage());
            fail("命令发送失败: " + result.getMessage());
        }

        // 等待消息传递，添加更多调试信息
        log.info("等待消息传递到客户端处理器...");

        // 多次检查，了解消息传递的时间模式
        for (int i = 1; i <= 10; i++) {
            Thread.sleep(500); // 每500ms检查一次

            boolean received = VoglanderTestClientMessageHandler.hasReceivedDeviceStatusQuery();
            log.info("第{}次检查 ({}ms后): 是否收到消息 = {}", i, i * 500, received);

            if (received) {
                log.info("✅ 客户端处理器成功接收到消息 (耗时: {}ms)", i * 500);
                String userId = VoglanderTestClientMessageHandler.getReceivedDeviceStatusUserId();
                log.info("接收到的用户ID: {}", userId);
                assertEquals(testDeviceId, userId, "接收到的设备ID应该匹配发送的设备ID");
                return; // 测试成功
            }
        }

        // 如果到这里说明消息没有被接收到
        log.error("❌ 客户端处理器在5秒内没有接收到任何消息");

        // 添加额外的诊断信息
        log.info("=== 额外诊断信息 ===");
        log.info("SIP监听点数量: {}", sipLayer.getActiveListeningPointsCount());
        log.info("测试客户端设备ID: {}", testDeviceId);
        log.info("测试服务端设备ID: {}", generateTestServerId());

        fail("客户端处理器没有接收到服务端发送的消息，存在SIP消息路由问题");
    }

    @Test
    @DisplayName("诊断CountDownLatch同步机制")
    public void testCountDownLatchSynchronization() throws Exception {
        log.info("=== CountDownLatch同步机制诊断 ===");

        // 重置状态
        VoglanderTestClientMessageHandler.resetTestState();

        // 测试wait方法是否正常工作
        long startTime = System.currentTimeMillis();

        // 在另一个线程中模拟触发CountDown
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 1秒后触发
                log.info("模拟触发CountDown...");

                // 直接调用处理器方法来模拟接收消息
                VoglanderTestClientMessageHandler handler =
                    applicationContext.getBean(VoglanderTestClientMessageHandler.class);
                handler.getDeviceStatus("test-device-123");

            } catch (Exception e) {
                log.error("模拟触发失败: {}", e.getMessage());
            }
        }).start();

        // 等待CountDown被触发
        boolean result = VoglanderTestClientMessageHandler.waitForDeviceStatusQuery(3, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        log.info("等待结果: {}, 耗时: {}ms", result, duration);

        if (result) {
            log.info("✅ CountDownLatch同步机制工作正常");
            assertTrue(duration >= 1000 && duration < 2000,
                "等待时间应该约为1秒（实际: " + duration + "ms）");
        } else {
            log.error("❌ CountDownLatch同步机制异常");
            fail("CountDownLatch同步机制没有正常工作");
        }
    }

    @Test
    @DisplayName("完整的端到端消息流诊断")
    public void testEndToEndMessageFlow() throws Exception {
        skipIfDeviceNotAvailable("端到端消息流诊断");

        log.info("=== 完整的端到端消息流诊断 ===");

        // 1. 验证所有组件就绪
        testSpringBeanRegistration();
        testSipInfrastructureStatus();
        testDeviceDataAndRouting();

        // 2. 测试消息处理器直接调用
        log.info("步骤1: 测试处理器直接调用");
        VoglanderTestClientMessageHandler handler =
            applicationContext.getBean(VoglanderTestClientMessageHandler.class);

        VoglanderTestClientMessageHandler.resetTestState();
        var deviceStatus = handler.getDeviceStatus("direct-test-123");
        assertNotNull(deviceStatus, "直接调用应该返回设备状态");
        assertTrue(VoglanderTestClientMessageHandler.hasReceivedDeviceStatusQuery(),
            "直接调用应该触发接收状态");
        log.info("✅ 处理器直接调用正常");

        // 3. 测试通过SIP发送消息（这是问题所在）
        log.info("步骤2: 测试通过SIP发送消息");
        VoglanderTestClientMessageHandler.resetTestState();

        String testDeviceId = generateTestClientDeviceId();
        var result = deviceCommand.queryDeviceStatus(testDeviceId);

        assertTrue(result.isSuccess(), "SIP命令发送应该成功");
        log.info("SIP命令发送成功: {}", result.getMessage());

        // 等待SIP消息传递
        boolean received = VoglanderTestClientMessageHandler.waitForDeviceStatusQuery(5, TimeUnit.SECONDS);

        if (received) {
            log.info("✅ 端到端消息流正常工作");
        } else {
            log.error("❌ SIP消息没有到达处理器");

            // 提供修复建议
            log.info("=== 修复建议 ===");
            log.info("1. 检查SIP消息路由配置");
            log.info("2. 验证设备ID和端口映射");
            log.info("3. 确认没有其他处理器拦截消息");
            log.info("4. 检查SIP监听点配置");

            fail("SIP消息路由存在问题，消息没有从发送端到达处理器");
        }
    }
}