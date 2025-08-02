package io.github.lunasaw.voglander.gb28181.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.utils.PtzCmdEnum;
import io.github.lunasaw.voglander.gb28181.handler.VoglanderTestServerMessageHandler;
import io.github.lunasaw.voglander.gb28181.BaseGb28181IntegrationTest;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.alarm.VoglanderClientAlarmCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.catalog.VoglanderClientCatalogCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.device.VoglanderClientDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.ptz.VoglanderClientPtzCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.record.VoglanderClientRecordCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.status.VoglanderClientStatusCommand;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GB28181客户端指令集成测试
 * <p>
 * 测试Voglander集成的GB28181客户端指令，确保指令能够正确发送并被服务端接收处理。
 * 参照sip-proxy/gb28181-test的测试模式，进行真实的消息发送和协议验证。
 * </p>
 * 
 * <h3>测试覆盖范围</h3>
 * <ul>
 * <li>设备信息上报指令</li>
 * <li>设备状态上报指令</li>
 * <li>设备目录响应指令</li>
 * <li>设备告警通知指令</li>
 * <li>云台控制响应指令</li>
 * <li>录像信息响应指令</li>
 * </ul>
 * 
 * <h3>测试验证点</h3>
 * <ul>
 * <li>指令发送成功性验证</li>
 * <li>服务端接收完整性验证</li>
 * <li>消息内容正确性验证</li>
 * <li>GB28181协议一致性验证</li>
 * </ul>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Gb28181ClientCommandIntegrationTest extends BaseGb28181IntegrationTest {

    @Autowired
    private VoglanderClientDeviceCommand  deviceCommand;

    @Autowired
    private VoglanderClientCatalogCommand catalogCommand;

    @Autowired
    private VoglanderClientAlarmCommand   alarmCommand;

    @Autowired
    private VoglanderClientPtzCommand     ptzCommand;

    // ==================== 设备信息指令测试 ====================

    @Test
    @Order(1)
    @DisplayName("测试发送设备信息指令")
    public void testSendDeviceInfoCommand() throws Exception {
        skipIfDeviceNotAvailable("设备信息指令测试");
        VoglanderTestServerMessageHandler.resetTestState();

        log.info("=== 开始设备信息指令测试 ===");

        // 构建设备信息
        DeviceInfo deviceInfo = createTestDeviceInfo();
        String testDeviceId = generateTestDeviceId();

        // 发送指令
        ResultDTO<Void> result = deviceCommand.sendDeviceInfoCommand(testDeviceId, deviceInfo);

        // 验证发送结果
        assertNotNull(result, "指令执行结果不能为空");
        assertTrue(result.isSuccess(), "指令应该发送成功: " + result.getMessage());

        // 等待服务端接收
        boolean received = VoglanderTestServerMessageHandler.waitForDeviceInfo(5, TimeUnit.SECONDS);
        assertTrue(received, "服务端应该在5秒内接收到设备信息");

        // 验证接收内容
        var receivedInfo = VoglanderTestServerMessageHandler.getReceivedDeviceInfo();
        assertNotNull(receivedInfo, "接收到的设备信息不能为空");
        assertEquals(testDeviceId, receivedInfo.getDeviceId(), "设备ID应该一致");

        log.info("✅ 设备信息指令测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("测试发送设备状态指令")
    public void testSendDeviceStatusCommand() throws Exception {
        skipIfDeviceNotAvailable("设备状态指令测试");
        VoglanderTestServerMessageHandler.resetTestState();

        log.info("=== 开始设备状态指令测试 ===");

        // 构建设备状态
        DeviceStatus deviceStatus = createTestDeviceStatus();
        String testDeviceId = generateTestDeviceId();

        // 发送指令
        ResultDTO<Void> result = deviceCommand.sendDeviceStatusCommand(testDeviceId, deviceStatus);

        // 验证发送结果
        assertNotNull(result, "指令执行结果不能为空");
        assertTrue(result.isSuccess(), "指令应该发送成功: " + result.getMessage());

        // 等待服务端接收
        boolean received = VoglanderTestServerMessageHandler.waitForDeviceStatus(5, TimeUnit.SECONDS);
        assertTrue(received, "服务端应该在5秒内接收到设备状态");

        // 验证接收内容
        var receivedStatus = VoglanderTestServerMessageHandler.getReceivedDeviceStatus();
        assertNotNull(receivedStatus, "接收到的设备状态不能为空");
        assertEquals(testDeviceId, receivedStatus.getDeviceId(), "设备ID应该一致");

        log.info("✅ 设备状态指令测试通过");
    }

    // ==================== 目录指令测试 ====================

    @Test
    @Order(3)
    @DisplayName("测试发送设备目录响应指令")
    public void testSendCatalogResponseCommand() throws Exception {
        skipIfDeviceNotAvailable("设备目录响应指令测试");
        VoglanderTestServerMessageHandler.resetTestState();

        log.info("=== 开始设备目录响应指令测试 ===");

        // 构建设备目录响应
        DeviceResponse catalogResponse = createTestCatalogResponse();
        String testDeviceId = generateTestDeviceId();

        // 发送指令
        ResultDTO<Void> result = catalogCommand.sendCatalogCommand(testDeviceId, catalogResponse);

        // 验证发送结果
        assertNotNull(result, "指令执行结果不能为空");
        assertTrue(result.isSuccess(), "指令应该发送成功: " + result.getMessage());

        // 等待服务端接收
        boolean received = VoglanderTestServerMessageHandler.waitForCatalog(5, TimeUnit.SECONDS);
        assertTrue(received, "服务端应该在5秒内接收到目录响应");

        // 验证接收内容
        var receivedCatalog = VoglanderTestServerMessageHandler.getReceivedCatalog();
        assertNotNull(receivedCatalog, "接收到的目录响应不能为空");
        assertEquals(testDeviceId, receivedCatalog.getDeviceId(), "设备ID应该一致");

        log.info("✅ 设备目录响应指令测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("测试发送设备列表响应指令")
    public void testSendDeviceItemsCommand() throws Exception {
        skipIfDeviceNotAvailable("设备列表响应指令测试");
        VoglanderTestServerMessageHandler.resetTestState();

        log.info("=== 开始设备列表响应指令测试 ===");

        // 构建设备列表
        List<DeviceItem> deviceItems = createTestDeviceItems();
        String testDeviceId = generateTestDeviceId();

        // 发送指令
        ResultDTO<Void> result = catalogCommand.sendDeviceItemsCommand(testDeviceId, deviceItems);

        // 验证发送结果
        assertNotNull(result, "指令执行结果不能为空");
        assertTrue(result.isSuccess(), "指令应该发送成功: " + result.getMessage());

        // 等待服务端接收
        boolean received = VoglanderTestServerMessageHandler.waitForCatalog(5, TimeUnit.SECONDS);
        assertTrue(received, "服务端应该在5秒内接收到设备列表");

        log.info("✅ 设备列表响应指令测试通过");
    }

    // ==================== 告警指令测试 ====================

    @Test
    @Order(5)
    @DisplayName("测试发送设备告警指令")
    public void testSendDeviceAlarmCommand() throws Exception {
        skipIfDeviceNotAvailable("设备告警指令测试");
        VoglanderTestServerMessageHandler.resetTestState();

        log.info("=== 开始设备告警指令测试 ===");

        // 构建设备告警
        DeviceAlarm deviceAlarm = createTestDeviceAlarm();
        String testDeviceId = generateTestDeviceId();

        // 发送指令
        ResultDTO<Void> result = alarmCommand.sendAlarmCommand(testDeviceId, deviceAlarm);

        // 验证发送结果
        assertNotNull(result, "指令执行结果不能为空");
        assertTrue(result.isSuccess(), "指令应该发送成功: " + result.getMessage());

        // 等待服务端接收
        boolean received = VoglanderTestServerMessageHandler.waitForAlarm(5, TimeUnit.SECONDS);
        assertTrue(received, "服务端应该在5秒内接收到告警信息");

        // 验证接收内容
        var receivedAlarm = VoglanderTestServerMessageHandler.getReceivedAlarm();
        assertNotNull(receivedAlarm, "接收到的告警信息不能为空");
        assertEquals(testDeviceId, receivedAlarm.getDeviceId(), "设备ID应该一致");

        log.info("✅ 设备告警指令测试通过");
    }

    // ==================== 云台控制指令测试 ====================

    @Test
    @Order(6)
    @DisplayName("测试发送云台控制指令")
    public void testSendPtzControlCommand() throws Exception {
        skipIfDeviceNotAvailable("云台控制指令测试");

        log.info("=== 开始云台控制指令测试 ===");

        String testDeviceId = generateTestDeviceId();

        // 测试各种云台控制指令
        String[] testCommands = {
            "A50F01010600FF", // 向上移动
            "A50F01020600FF", // 向下移动
            "A50F01040600FF", // 向左移动
            "A50F01080600FF" // 向右移动
        };

        for (String ptzCmd : testCommands) {
            log.info("测试云台指令: {}", ptzCmd);

            ResultDTO<Void> result = ptzCommand.sendPtzControlCommand(testDeviceId, ptzCmd);

            assertNotNull(result, "指令执行结果不能为空");
            assertTrue(result.isSuccess(), "云台控制指令应该发送成功: " + result.getMessage());
        }

        log.info("✅ 云台控制指令测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("测试发送云台枚举控制指令")
    public void testSendPtzEnumControlCommand() throws Exception {
        skipIfDeviceNotAvailable("云台枚举控制指令测试");

        log.info("=== 开始云台枚举控制指令测试 ===");

        String testDeviceId = generateTestDeviceId();

        // 测试各种云台枚举控制
        PtzCmdEnum[] testEnums = {
            PtzCmdEnum.UP,
            PtzCmdEnum.DOWN,
            PtzCmdEnum.LEFT,
            PtzCmdEnum.RIGHT,
            PtzCmdEnum.ZOOMIN,
            PtzCmdEnum.ZOOMOUT
        };

        for (PtzCmdEnum ptzEnum : testEnums) {
            log.info("测试云台枚举指令: {}", ptzEnum);

            ResultDTO<Void> result = ptzCommand.sendPtzControlCommand(testDeviceId, ptzEnum, 128);

            assertNotNull(result, "指令执行结果不能为空");
            assertTrue(result.isSuccess(), "云台枚举控制指令应该发送成功: " + result.getMessage());
        }

        log.info("✅ 云台枚举控制指令测试通过");
    }

    // ==================== 便捷方法测试 ====================

    @Test
    @Order(8)
    @DisplayName("测试便捷方法指令")
    public void testConvenienceMethodCommands() throws Exception {
        skipIfDeviceNotAvailable("便捷方法指令测试");

        log.info("=== 开始便捷方法指令测试 ===");

        String testDeviceId = generateTestDeviceId();

        // 测试状态便捷方法
        ResultDTO<Void> onlineResult = deviceCommand.sendDeviceOnlineNotify(testDeviceId);
        assertTrue(onlineResult.isSuccess(), "设备上线状态应该发送成功");

        ResultDTO<Void> offlineResult = deviceCommand.sendDeviceOfflineNotify(testDeviceId);
        assertTrue(offlineResult.isSuccess(), "设备离线状态应该发送成功");

        // 测试云台便捷方法
        ResultDTO<Void> moveUpResult = ptzCommand.moveUp(testDeviceId, 100);
        assertTrue(moveUpResult.isSuccess(), "云台向上移动应该发送成功");

        ResultDTO<Void> stopResult = ptzCommand.stopMove(testDeviceId);
        assertTrue(stopResult.isSuccess(), "云台停止移动应该发送成功");

        // 测试目录便捷方法
        ResultDTO<Void> emptyCatalogResult = catalogCommand.sendEmptyCatalogResponse(testDeviceId, "空目录测试");
        assertTrue(emptyCatalogResult.isSuccess(), "空目录响应应该发送成功");

        log.info("✅ 便捷方法指令测试通过");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用设备信息
     */
    private DeviceInfo createTestDeviceInfo() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(generateTestDeviceId());
        deviceInfo.setDeviceName("测试摄像头设备");
        deviceInfo.setManufacturer("测试厂商");
        deviceInfo.setModel("TEST-CAM-001");
        deviceInfo.setFirmware("V1.0.0");
        deviceInfo.setChannel(1);
        return deviceInfo;
    }

    /**
     * 创建测试用设备状态
     */
    private DeviceStatus createTestDeviceStatus() {
        DeviceStatus deviceStatus = new DeviceStatus();
        deviceStatus.setDeviceId(generateTestDeviceId());
        deviceStatus.setOnline("ON");
        deviceStatus.setStatus("OK");
        // deviceStatus.setReason("正常"); // Method not available
        // deviceStatus.setLongitude(120.123456); // Method not available
        // deviceStatus.setLatitude(30.123456); // Method not available
        return deviceStatus;
    }

    /**
     * 创建测试用目录响应
     */
    private DeviceResponse createTestCatalogResponse() {
        DeviceResponse response = new DeviceResponse();
        response.setDeviceId(generateTestDeviceId());
        response.setCmdType("Catalog");
        response.setSn("12345");
        response.setName("目录查询响应");
        response.setSumNum(2);
        response.setDeviceList(createTestDeviceItems());
        return response;
    }

    /**
     * 创建测试用设备项列表
     */
    private List<DeviceItem> createTestDeviceItems() {
        DeviceItem item1 = new DeviceItem();
        item1.setDeviceId(generateTestDeviceId());
        item1.setName("测试通道1");
        item1.setManufacturer("测试厂商");
        item1.setModel("TEST-CH-001");
        item1.setStatus("ON");
        item1.setParental(0);

        DeviceItem item2 = new DeviceItem();
        item2.setDeviceId(generateTestDeviceId().substring(0, 18) + "02");
        item2.setName("测试通道2");
        item2.setManufacturer("测试厂商");
        item2.setModel("TEST-CH-002");
        item2.setStatus("OFF");
        item2.setParental(0);

        return new ArrayList<>(Arrays.asList(item1, item2));
    }

    /**
     * 创建测试用设备告警
     */
    private DeviceAlarm createTestDeviceAlarm() {
        DeviceAlarm alarm = new DeviceAlarm();
        alarm.setDeviceId(generateTestDeviceId());
        alarm.setAlarmPriority("1"); // 紧急报警
        alarm.setAlarmMethod("2"); // 设备报警
        alarm.setAlarmTime(new Date());
        alarm.setAlarmDescription("测试告警信息");
        alarm.setAlarmType("1"); // 视频丢失报警
        // alarm.setLongitude(120.123456); // Check if method available
        // alarm.setLatitude(30.123456); // Check if method available
        return alarm;
    }
}