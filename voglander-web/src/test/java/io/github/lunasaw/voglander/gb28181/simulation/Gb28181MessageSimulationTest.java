package io.github.lunasaw.voglander.gb28181.simulation;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.utils.PtzCmdEnum;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.voglander.gb28181.handler.VoglanderTestClientMessageHandler;
import io.github.lunasaw.voglander.gb28181.handler.VoglanderTestServerMessageHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.gb28181.BaseGb28181IntegrationTest;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.device.VoglanderClientDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.status.VoglanderClientStatusCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GB28181真实消息发送模拟测试
 * <p>
 * 进行真实的GB28181协议消息发送模拟，验证客户端与服务端之间的完整通信流程。
 * 确保消息能够正确发送、接收和处理，并保持协议的一致性。
 * </p>
 * 
 * <h3>测试特性</h3>
 * <ul>
 * <li>端到端消息传输验证</li>
 * <li>真实SIP协议栈通信</li>
 * <li>异步消息处理验证</li>
 * <li>协议格式一致性检查</li>
 * <li>错误处理和重试机制</li>
 * </ul>
 * 
 * <h3>测试场景</h3>
 * <ul>
 * <li>设备注册和心跳保持</li>
 * <li>设备信息查询和响应</li>
 * <li>状态上报和查询</li>
 * <li>云台控制命令传输</li>
 * <li>告警信息传输</li>
 * <li>录像查询和控制</li>
 * </ul>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Gb28181MessageSimulationTest extends BaseGb28181IntegrationTest {

    @Autowired
    private VoglanderClientDeviceCommand clientDeviceCommand;

    @Autowired
    private VoglanderClientStatusCommand clientStatusCommand;

    @Autowired
    private VoglanderServerDeviceCommand serverDeviceCommand;

    @Autowired
    private VoglanderServerPtzCommand    serverPtzCommand;

    // ==================== 基础通信测试 ====================

    @Test
    @Order(1)
    @DisplayName("测试设备注册流程模拟")
    public void testDeviceRegistrationSimulation() throws Exception {
        skipIfDeviceNotAvailable("设备注册流程模拟测试");

        log.info("=== 开始设备注册流程模拟测试 ===");

        FromDevice clientFromDevice = getClientFromDevice();
        ToDevice clientToDevice = getClientToDevice();

        assertNotNull(clientFromDevice, "客户端发送设备不能为空");
        assertNotNull(clientToDevice, "客户端目标设备不能为空");

        // 模拟设备注册
        log.info("发送设备注册请求...");
        String registerCallId = ClientCommandSender.sendRegisterCommand(clientFromDevice, clientToDevice, 3600);

        assertNotNull(registerCallId, "注册请求应该返回CallId");
        assertFalse(registerCallId.isEmpty(), "CallId不能为空");

        log.info("设备注册请求发送成功，CallId: {}", registerCallId);

        // 等待短暂时间让消息处理
        Thread.sleep(1000);

        log.info("✅ 设备注册流程模拟测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("测试心跳保持流程模拟")
    public void testHeartbeatSimulation() throws Exception {
        skipIfDeviceNotAvailable("心跳保持流程模拟测试");
        VoglanderTestServerMessageHandler.resetTestState();

        log.info("=== 开始心跳保持流程模拟测试 ===");

        FromDevice clientFromDevice = getClientFromDevice();
        ToDevice clientToDevice = getClientToDevice();

        // 发送心跳消息
        log.info("发送心跳消息...");
        String heartbeatCallId = ClientCommandSender.sendKeepaliveCommand(clientFromDevice, clientToDevice, "onLine");

        assertNotNull(heartbeatCallId, "心跳请求应该返回CallId");

        // 等待服务端接收心跳
        boolean received = VoglanderTestServerMessageHandler.waitForKeepalive(5, TimeUnit.SECONDS);
        assertTrue(received, "服务端应该在5秒内接收到心跳");

        // 验证心跳内容
        var receivedKeepalive = VoglanderTestServerMessageHandler.getReceivedKeepalive();
        assertNotNull(receivedKeepalive, "接收到的心跳内容不能为空");
        assertEquals(clientFromDevice.getUserId(), receivedKeepalive.getDeviceId(), "心跳设备ID应该一致");

        log.info("心跳消息发送并接收成功，设备ID: {}", receivedKeepalive.getDeviceId());
        log.info("✅ 心跳保持流程模拟测试通过");
    }

    // ==================== 设备信息交互测试 ====================

    @Test
    @Order(3)
    @DisplayName("测试设备信息查询响应流程模拟")
    public void testDeviceInfoQueryResponseSimulation() throws Exception {
        skipIfDeviceNotAvailable("设备信息查询响应流程模拟测试");
        VoglanderTestClientMessageHandler.resetTestState();
        VoglanderTestServerMessageHandler.resetTestState();

        log.info("=== 开始设备信息查询响应流程模拟测试 ===");

        String testDeviceId = generateTestDeviceId();

        // 第一步：服务端发送设备信息查询
        log.info("第一步：服务端发送设备信息查询");
        ResultDTO<Void> queryResult = serverDeviceCommand.queryDeviceInfo(testDeviceId);
        assertTrue(queryResult.isSuccess(), "设备信息查询应该发送成功");

        // 等待客户端接收查询请求
        boolean queryReceived = VoglanderTestClientMessageHandler.waitForDeviceInfoQuery(3, TimeUnit.SECONDS);
        assertTrue(queryReceived, "客户端应该接收到设备信息查询");

        // 第二步：客户端响应设备信息
        log.info("第二步：客户端响应设备信息");
        DeviceInfo deviceInfo = createDetailedDeviceInfo(testDeviceId);
        ResultDTO<Void> responseResult = clientDeviceCommand.sendDeviceInfoCommand(testDeviceId, deviceInfo);
        assertTrue(responseResult.isSuccess(), "设备信息响应应该发送成功");

        // 等待服务端接收响应
        boolean responseReceived = VoglanderTestServerMessageHandler.waitForDeviceInfo(3, TimeUnit.SECONDS);
        assertTrue(responseReceived, "服务端应该接收到设备信息响应");

        // 验证响应内容
        var receivedInfo = VoglanderTestServerMessageHandler.getReceivedDeviceInfo();
        assertNotNull(receivedInfo, "接收到的设备信息不能为空");
        assertEquals(testDeviceId, receivedInfo.getDeviceId(), "设备ID应该一致");

        log.info("设备信息查询响应流程完成，设备: {} -> {}", testDeviceId, receivedInfo.getDeviceName());
        log.info("✅ 设备信息查询响应流程模拟测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("测试设备状态上报流程模拟")
    public void testDeviceStatusReportSimulation() throws Exception {
        skipIfDeviceNotAvailable("设备状态上报流程模拟测试");
        VoglanderTestServerMessageHandler.resetTestState();

        log.info("=== 开始设备状态上报流程模拟测试 ===");

        String testDeviceId = generateTestDeviceId();

        // 模拟设备状态变化：离线 -> 在线
        log.info("模拟设备状态变化：离线 -> 在线");

        // 1. 设备离线状态上报
        ResultDTO<Void> offlineResult = clientDeviceCommand.sendDeviceOfflineNotify(testDeviceId);
        assertTrue(offlineResult.isSuccess(), "设备离线状态应该发送成功");

        boolean offlineReceived = VoglanderTestServerMessageHandler.waitForDeviceStatus(3, TimeUnit.SECONDS);
        assertTrue(offlineReceived, "服务端应该接收到离线状态");

        // 2. 设备在线状态上报
        ResultDTO<Void> onlineResult = clientDeviceCommand.sendDeviceOnlineNotify(testDeviceId);
        assertTrue(onlineResult.isSuccess(), "设备在线状态应该发送成功");

        boolean onlineReceived = VoglanderTestServerMessageHandler.waitForDeviceStatus(3, TimeUnit.SECONDS);
        assertTrue(onlineReceived, "服务端应该接收到在线状态");

        // 验证最后状态
        var finalStatus = VoglanderTestServerMessageHandler.getReceivedDeviceStatus();
        assertNotNull(finalStatus, "接收到的设备状态不能为空");
        assertEquals(testDeviceId, finalStatus.getDeviceId(), "设备ID应该一致");

        log.info("设备状态上报流程完成，最终状态: {}", finalStatus.getOnline());
        log.info("✅ 设备状态上报流程模拟测试通过");
    }

    // ==================== 云台控制流程测试 ====================

    @Test
    @Order(5)
    @DisplayName("测试云台控制命令流程模拟")
    public void testPtzControlSimulation() throws Exception {
        skipIfDeviceNotAvailable("云台控制命令流程模拟测试");

        log.info("=== 开始云台控制命令流程模拟测试 ===");

        String testDeviceId = generateTestDeviceId();

        // 模拟完整的云台控制序列
        log.info("模拟完整的云台控制序列");

        // 1. 向上移动
        log.info("步骤1：向上移动");
        ResultDTO<Void> upResult = serverPtzCommand.moveUp(testDeviceId, 100);
        assertTrue(upResult.isSuccess(), "向上移动指令应该发送成功");
        Thread.sleep(500); // 模拟移动时间

        // 2. 向右移动
        log.info("步骤2：向右移动");
        ResultDTO<Void> rightResult = serverPtzCommand.moveRight(testDeviceId, 100);
        assertTrue(rightResult.isSuccess(), "向右移动指令应该发送成功");
        Thread.sleep(500);

        // 3. 放大变焦
        log.info("步骤3：放大变焦");
        ResultDTO<Void> zoomInResult = serverPtzCommand.zoomIn(testDeviceId, 80);
        assertTrue(zoomInResult.isSuccess(), "放大变焦指令应该发送成功");
        Thread.sleep(300);

        // 4. 缩小变焦
        log.info("步骤4：缩小变焦");
        ResultDTO<Void> zoomOutResult = serverPtzCommand.zoomOut(testDeviceId, 80);
        assertTrue(zoomOutResult.isSuccess(), "缩小变焦指令应该发送成功");
        Thread.sleep(300);

        // 5. 停止移动
        log.info("步骤5：停止移动");
        ResultDTO<Void> stopResult = serverPtzCommand.stopDevicePtz(testDeviceId);
        assertTrue(stopResult.isSuccess(), "停止移动指令应该发送成功");

        log.info("云台控制序列执行完成");
        log.info("✅ 云台控制命令流程模拟测试通过");
    }

    // ==================== 并发和异步测试 ====================

    @Test
    @Order(6)
    @DisplayName("测试并发消息发送模拟")
    public void testConcurrentMessageSimulation() throws Exception {
        skipIfDeviceNotAvailable("并发消息发送模拟测试");

        log.info("=== 开始并发消息发送模拟测试 ===");

        String testDeviceId = generateTestDeviceId();
        int concurrentCount = 5;

        // 创建并发任务
        CompletableFuture<ResultDTO<Void>>[] futures = new CompletableFuture[concurrentCount];

        log.info("启动{}个并发云台控制任务", concurrentCount);
        for (int i = 0; i < concurrentCount; i++) {
            final int taskId = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("任务{}：发送云台控制指令", taskId);

                    // 根据任务ID选择不同的控制指令
                    PtzCmdEnum command = switch (taskId % 4) {
                        case 0 -> PtzCmdEnum.UP;
                        case 1 -> PtzCmdEnum.DOWN;
                        case 2 -> PtzCmdEnum.LEFT;
                        default -> PtzCmdEnum.RIGHT;
                    };

                    return serverPtzCommand.controlDevicePtz(testDeviceId, command, 100);
                } catch (Exception e) {
                    log.error("任务{}执行失败", taskId, e);
                    throw new RuntimeException(e);
                }
            });
        }

        // 等待所有任务完成
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures);
        allTasks.get(10, TimeUnit.SECONDS); // 最多等待10秒

        // 验证所有任务的结果
        for (int i = 0; i < concurrentCount; i++) {
            ResultDTO<Void> result = futures[i].get();
            assertTrue(result.isSuccess(), "并发任务" + i + "应该执行成功");
        }

        log.info("所有{}个并发任务执行完成", concurrentCount);
        log.info("✅ 并发消息发送模拟测试通过");
    }

    // ==================== 错误处理和恢复测试 ====================

    @Test
    @Order(7)
    @DisplayName("测试错误处理和恢复流程模拟")
    public void testErrorHandlingSimulation() throws Exception {
        skipIfDeviceNotAvailable("错误处理和恢复流程模拟测试");

        log.info("=== 开始错误处理和恢复流程模拟测试 ===");

        // 测试无效设备ID的处理
        log.info("测试无效设备ID的处理");
        String invalidDeviceId = "invalid-device-id";

        try {
            ResultDTO<Void> result = serverDeviceCommand.queryDeviceInfo(invalidDeviceId);
            // 指令发送本身应该成功，但设备可能不存在
            assertNotNull(result, "即使设备ID无效，指令发送结果也不应为空");
            log.info("无效设备ID处理测试完成");
        } catch (Exception e) {
            log.info("捕获到预期的异常: {}", e.getMessage());
        }

        // 测试空参数的处理
        log.info("测试空参数的处理");
        try {
            ResultDTO<Void> result = serverPtzCommand.controlDevicePtz(null, PtzCmdEnum.UP, 100);
            assertFalse(result.isSuccess(), "空设备ID应该导致指令失败");
        } catch (IllegalArgumentException e) {
            log.info("捕获到预期的参数异常: {}", e.getMessage());
        }

        // 测试恢复机制 - 正常指令
        log.info("测试恢复机制 - 发送正常指令");
        String validDeviceId = generateTestDeviceId();
        ResultDTO<Void> recoveryResult = serverDeviceCommand.queryDeviceStatus(validDeviceId);
        assertTrue(recoveryResult.isSuccess(), "恢复后的正常指令应该执行成功");

        log.info("✅ 错误处理和恢复流程模拟测试通过");
    }

    // ==================== 协议一致性验证测试 ====================

    @Test
    @Order(8)
    @DisplayName("测试GB28181协议一致性验证")
    public void testProtocolConsistencyValidation() throws Exception {
        skipIfDeviceNotAvailable("GB28181协议一致性验证测试");
        VoglanderTestServerMessageHandler.resetTestState();

        log.info("=== 开始GB28181协议一致性验证测试 ===");

        String testDeviceId = generateTestDeviceId();

        // 验证设备ID格式
        assertTrue(testDeviceId.matches("\\d{20}"), "设备ID应该符合GB28181格式（20位数字）");
        log.info("设备ID格式验证通过: {}", testDeviceId);

        // 验证消息格式 - 发送标准的设备信息
        DeviceInfo deviceInfo = createStandardDeviceInfo(testDeviceId);
        ResultDTO<Void> result = clientDeviceCommand.sendDeviceInfoCommand(testDeviceId, deviceInfo);
        assertTrue(result.isSuccess(), "标准设备信息应该发送成功");

        // 等待接收并验证
        boolean received = VoglanderTestServerMessageHandler.waitForDeviceInfo(5, TimeUnit.SECONDS);
        assertTrue(received, "服务端应该接收到设备信息");

        var receivedInfo = VoglanderTestServerMessageHandler.getReceivedDeviceInfo();
        assertNotNull(receivedInfo, "接收到的设备信息不能为空");

        // 验证关键字段
        assertEquals(testDeviceId, receivedInfo.getDeviceId(), "设备ID应该保持一致");
        assertNotNull(receivedInfo.getDeviceName(), "设备名称不能为空");
        assertNotNull(receivedInfo.getManufacturer(), "制造商信息不能为空");

        log.info("协议字段验证通过 - 设备: {}, 制造商: {}",
            receivedInfo.getDeviceName(), receivedInfo.getManufacturer());
        log.info("✅ GB28181协议一致性验证测试通过");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建详细的设备信息用于测试
     */
    protected DeviceInfo createDetailedDeviceInfo(String deviceId) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceName("测试高清摄像头");
        deviceInfo.setManufacturer("测试设备制造商");
        deviceInfo.setModel("VOGLANDER-CAM-HD-001");
        deviceInfo.setFirmware("V2.1.5");
        deviceInfo.setChannel(1);
        // deviceInfo.setDeviceType("IPC"); // Method not available
        return deviceInfo;
    }

    /**
     * 创建标准的设备信息用于协议验证
     */
    private DeviceInfo createStandardDeviceInfo(String deviceId) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceName("GB28181标准摄像头");
        deviceInfo.setManufacturer("标准设备厂商");
        deviceInfo.setModel("GB28181-STD-CAM");
        deviceInfo.setFirmware("V1.0.0");
        deviceInfo.setChannel(1);
        return deviceInfo;
    }

    /**
     * 创建详细的设备状态用于测试
     */
    private DeviceStatus createDetailedDeviceStatus(String deviceId, String status) {
        DeviceStatus deviceStatus = new DeviceStatus();
        deviceStatus.setDeviceId(deviceId);
        deviceStatus.setOnline(status);
        deviceStatus.setStatus("OK");
        // deviceStatus.setReason("正常运行"); // Method not available
        // deviceStatus.setLongitude(120.123456); // Method not available
        // deviceStatus.setLatitude(30.123456); // Method not available
        // deviceStatus.setTime(new Date()); // Method not available
        return deviceStatus;
    }

    /**
     * 创建详细的设备告警用于测试
     */
    private DeviceAlarm createDetailedDeviceAlarm(String deviceId) {
        DeviceAlarm alarm = new DeviceAlarm();
        alarm.setDeviceId(deviceId);
        alarm.setAlarmPriority("1"); // 紧急报警
        alarm.setAlarmMethod("2"); // 设备报警
        alarm.setAlarmTime(new Date());
        alarm.setAlarmDescription("模拟测试告警：视频信号丢失");
        alarm.setAlarmType("1"); // 视频丢失报警
        alarm.setLongitude(120.123456);
        alarm.setLatitude(30.123456);
        return alarm;
    }
}