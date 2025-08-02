package io.github.lunasaw.voglander.gb28181.server;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gb28181.common.entity.utils.PtzCmdEnum;
import io.github.lunasaw.gbproxy.server.entity.InviteRequest;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.voglander.gb28181.handler.VoglanderTestClientMessageHandler;
import io.github.lunasaw.voglander.gb28181.BaseGb28181IntegrationTest;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm.VoglanderServerAlarmCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.config.VoglanderServerConfigCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GB28181服务端指令集成测试
 * <p>
 * 测试Voglander集成的GB28181服务端指令，确保指令能够正确发送并被客户端接收处理。
 * 参照sip-proxy/gb28181-test的测试模式，进行真实的消息发送和协议验证。
 * </p>
 * 
 * <h3>测试覆盖范围</h3>
 * <ul>
 * <li>设备查询指令（信息、状态、目录、预设位）</li>
 * <li>录像查询指令（录像信息、录像控制）</li>
 * <li>告警查询指令（告警信息、告警控制）</li>
 * <li>云台控制指令（方向控制、变焦控制）</li>
 * <li>设备配置指令（参数配置、重启控制）</li>
 * <li>媒体流指令（实时流、回放流、会话控制）</li>
 * </ul>
 * 
 * <h3>测试验证点</h3>
 * <ul>
 * <li>指令发送成功性验证</li>
 * <li>客户端接收完整性验证</li>
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
public class Gb28181ServerCommandIntegrationTest extends BaseGb28181IntegrationTest {

    @Autowired
    private VoglanderServerDeviceCommand deviceCommand;

    @Autowired
    private VoglanderServerRecordCommand recordCommand;

    @Autowired
    private VoglanderServerAlarmCommand  alarmCommand;

    @Autowired
    private VoglanderServerPtzCommand    ptzCommand;

    @Autowired
    private VoglanderServerConfigCommand configCommand;

    @Autowired
    private VoglanderServerMediaCommand  mediaCommand;

    // ==================== 设备查询指令测试 ====================

    @Test
    @Order(1)
    @DisplayName("测试设备信息查询指令")
    public void testQueryDeviceInfo() throws Exception {
        skipIfDeviceNotAvailable("设备信息查询指令测试");
        VoglanderTestClientMessageHandler.resetTestState();

        log.info("=== 开始设备信息查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 发送查询指令
        ResultDTO<Void> result = deviceCommand.queryDeviceInfo(testDeviceId);

        // 验证发送结果
        assertNotNull(result, "指令执行结果不能为空");
        assertTrue(result.isSuccess(), "指令应该发送成功: " + result.getMessage());

        // 等待客户端接收
        boolean received = VoglanderTestClientMessageHandler.waitForDeviceInfoQuery(5, TimeUnit.SECONDS);
        assertTrue(received, "客户端应该在5秒内接收到设备信息查询");

        // 验证接收内容
        var receivedInfo = VoglanderTestClientMessageHandler.getReceivedDeviceInfoUserId();
        assertNotNull(receivedInfo, "接收到的查询请求不能为空");

        log.info("✅ 设备信息查询指令测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("测试设备状态查询指令")
    public void testQueryDeviceStatus() throws Exception {
        skipIfDeviceNotAvailable("设备状态查询指令测试");
        VoglanderTestClientMessageHandler.resetTestState();

        log.info("=== 开始设备状态查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 发送查询指令
        ResultDTO<Void> result = deviceCommand.queryDeviceStatus(testDeviceId);

        // 验证发送结果
        assertNotNull(result, "指令执行结果不能为空");
        assertTrue(result.isSuccess(), "指令应该发送成功: " + result.getMessage());

        // 等待客户端接收
        boolean received = VoglanderTestClientMessageHandler.waitForDeviceStatusQuery(5, TimeUnit.SECONDS);
        assertTrue(received, "客户端应该在5秒内接收到设备状态查询");

        log.info("✅ 设备状态查询指令测试通过");
    }

    @Test
    @Order(3)
    @DisplayName("测试设备目录查询指令")
    public void testQueryDeviceCatalog() throws Exception {
        skipIfDeviceNotAvailable("设备目录查询指令测试");
        VoglanderTestClientMessageHandler.resetTestState();

        log.info("=== 开始设备目录查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 发送查询指令
        ResultDTO<Void> result = deviceCommand.queryDeviceCatalog(testDeviceId);

        // 验证发送结果
        assertNotNull(result, "指令执行结果不能为空");
        assertTrue(result.isSuccess(), "指令应该发送成功: " + result.getMessage());

        // 等待客户端接收
        boolean received = VoglanderTestClientMessageHandler.waitForDeviceItemQuery(5, TimeUnit.SECONDS);
        assertTrue(received, "客户端应该在5秒内接收到设备目录查询");

        log.info("✅ 设备目录查询指令测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("测试设备预设位查询指令")
    public void testQueryDevicePreset() throws Exception {
        skipIfDeviceNotAvailable("设备预设位查询指令测试");

        log.info("=== 开始设备预设位查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 发送查询指令
        ResultDTO<Void> result = deviceCommand.queryDevicePreset(testDeviceId);

        // 验证发送结果
        assertNotNull(result, "指令执行结果不能为空");
        assertTrue(result.isSuccess(), "指令应该发送成功: " + result.getMessage());

        log.info("✅ 设备预设位查询指令测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("测试移动设备位置查询指令")
    public void testQueryDeviceMobilePosition() throws Exception {
        skipIfDeviceNotAvailable("移动设备位置查询指令测试");
        VoglanderTestClientMessageHandler.resetTestState();

        log.info("=== 开始移动设备位置查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 发送查询指令（使用默认间隔）
        ResultDTO<Void> result1 = deviceCommand.queryDeviceMobilePosition(testDeviceId);
        assertTrue(result1.isSuccess(), "默认间隔位置查询应该发送成功");

        // 发送查询指令（指定间隔）
        ResultDTO<Void> result2 = deviceCommand.queryDeviceMobilePosition(testDeviceId, "60");
        assertTrue(result2.isSuccess(), "指定间隔位置查询应该发送成功");

        // 等待客户端接收
        boolean received = VoglanderTestClientMessageHandler.waitForMobilePositionQuery(5, TimeUnit.SECONDS);
        assertTrue(received, "客户端应该在5秒内接收到位置查询");

        log.info("✅ 移动设备位置查询指令测试通过");
    }

    // ==================== 录像查询指令测试 ====================

    @Test
    @Order(6)
    @DisplayName("测试录像信息查询指令")
    public void testQueryDeviceRecord() throws Exception {
        skipIfDeviceNotAvailable("录像信息查询指令测试");
        VoglanderTestClientMessageHandler.resetTestState();

        log.info("=== 开始录像信息查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试不同时间格式的查询

        // 1. 使用Date对象查询
        Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Date endTime = new Date();
        ResultDTO<Void> result1 = recordCommand.queryDeviceRecord(testDeviceId, startTime, endTime);
        assertTrue(result1.isSuccess(), "Date对象录像查询应该发送成功");

        // 2. 使用时间戳查询
        long startTimestamp = System.currentTimeMillis() - 12 * 60 * 60 * 1000;
        long endTimestamp = System.currentTimeMillis();
        ResultDTO<Void> result2 = recordCommand.queryDeviceRecord(testDeviceId, startTimestamp, endTimestamp);
        assertTrue(result2.isSuccess(), "时间戳录像查询应该发送成功");

        // 3. 使用字符串查询
        String startTimeStr = "2024-01-01T08:00:00";
        String endTimeStr = "2024-01-01T18:00:00";
        ResultDTO<Void> result3 = recordCommand.queryDeviceRecord(testDeviceId, startTimeStr, endTimeStr);
        assertTrue(result3.isSuccess(), "字符串录像查询应该发送成功");

        // 等待客户端接收
        boolean received = VoglanderTestClientMessageHandler.waitForDeviceRecordQuery(5, TimeUnit.SECONDS);
        assertTrue(received, "客户端应该在5秒内接收到录像查询");

        log.info("✅ 录像信息查询指令测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("测试录像控制指令")
    public void testControlDeviceRecord() throws Exception {
        skipIfDeviceNotAvailable("录像控制指令测试");

        log.info("=== 开始录像控制指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试开始录像
        ResultDTO<Void> startResult = recordCommand.startDeviceRecord(testDeviceId);
        assertTrue(startResult.isSuccess(), "开始录像指令应该发送成功");

        // 测试停止录像
        ResultDTO<Void> stopResult = recordCommand.stopDeviceRecord(testDeviceId);
        assertTrue(stopResult.isSuccess(), "停止录像指令应该发送成功");

        log.info("✅ 录像控制指令测试通过");
    }

    @Test
    @Order(8)
    @DisplayName("测试录像便捷查询指令")
    public void testConvenienceRecordQuery() throws Exception {
        skipIfDeviceNotAvailable("录像便捷查询指令测试");

        log.info("=== 开始录像便捷查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试今日录像查询
        ResultDTO<Void> todayResult = recordCommand.queryTodayDeviceRecord(testDeviceId);
        assertTrue(todayResult.isSuccess(), "今日录像查询应该发送成功");

        // 测试昨日录像查询
        ResultDTO<Void> yesterdayResult = recordCommand.queryYesterdayDeviceRecord(testDeviceId);
        assertTrue(yesterdayResult.isSuccess(), "昨日录像查询应该发送成功");

        // 测试最近N小时录像查询
        ResultDTO<Void> recentResult = recordCommand.queryRecentDeviceRecord(testDeviceId, 6);
        assertTrue(recentResult.isSuccess(), "最近6小时录像查询应该发送成功");

        log.info("✅ 录像便捷查询指令测试通过");
    }

    // ==================== 告警查询指令测试 ====================

    @Test
    @Order(9)
    @DisplayName("测试告警信息查询指令")
    public void testQueryDeviceAlarm() throws Exception {
        skipIfDeviceNotAvailable("告警信息查询指令测试");

        log.info("=== 开始告警信息查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();
        Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Date endTime = new Date();

        // 发送告警查询指令
        ResultDTO<Void> result = alarmCommand.queryDeviceAlarm(testDeviceId, startTime, endTime,
            "1", "4", "5", "1");

        // 验证发送结果
        assertTrue(result.isSuccess(), "告警查询指令应该发送成功");

        log.info("✅ 告警信息查询指令测试通过");
    }

    @Test
    @Order(10)
    @DisplayName("测试告警控制指令")
    public void testControlDeviceAlarm() throws Exception {
        skipIfDeviceNotAvailable("告警控制指令测试");

        log.info("=== 开始告警控制指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试告警控制
        ResultDTO<Void> controlResult = alarmCommand.controlDeviceAlarm(testDeviceId, "2", "1");
        assertTrue(controlResult.isSuccess(), "告警控制指令应该发送成功");

        // 测试启用网络告警
        ResultDTO<Void> networkResult = alarmCommand.enableNetworkAlarm(testDeviceId, "1");
        assertTrue(networkResult.isSuccess(), "启用网络告警应该发送成功");

        // 测试启用视频告警
        ResultDTO<Void> videoResult = alarmCommand.enableVideoAlarm(testDeviceId, "1");
        assertTrue(videoResult.isSuccess(), "启用视频告警应该发送成功");

        log.info("✅ 告警控制指令测试通过");
    }

    @Test
    @Order(11)
    @DisplayName("测试告警便捷查询指令")
    public void testConvenienceAlarmQuery() throws Exception {
        skipIfDeviceNotAvailable("告警便捷查询指令测试");

        log.info("=== 开始告警便捷查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试今日告警查询
        ResultDTO<Void> todayResult = alarmCommand.queryTodayDeviceAlarm(testDeviceId);
        assertTrue(todayResult.isSuccess(), "今日告警查询应该发送成功");

        // 测试紧急告警查询
        Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Date endTime = new Date();
        ResultDTO<Void> emergencyResult = alarmCommand.queryEmergencyDeviceAlarm(testDeviceId, startTime, endTime);
        assertTrue(emergencyResult.isSuccess(), "紧急告警查询应该发送成功");

        // 测试设备故障告警查询
        ResultDTO<Void> faultResult = alarmCommand.queryDeviceFaultAlarm(testDeviceId, startTime, endTime);
        assertTrue(faultResult.isSuccess(), "设备故障告警查询应该发送成功");

        // 测试最近N小时告警查询
        ResultDTO<Void> recentResult = alarmCommand.queryRecentDeviceAlarm(testDeviceId, 12);
        assertTrue(recentResult.isSuccess(), "最近12小时告警查询应该发送成功");

        log.info("✅ 告警便捷查询指令测试通过");
    }

    // ==================== 云台控制指令测试 ====================

    @Test
    @Order(12)
    @DisplayName("测试云台控制指令")
    public void testControlDevicePtz() throws Exception {
        skipIfDeviceNotAvailable("云台控制指令测试");

        log.info("=== 开始云台控制指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试自定义云台指令
        ResultDTO<Void> customResult = ptzCommand.controlDevicePtz(testDeviceId, "A50F01010600FF");
        assertTrue(customResult.isSuccess(), "自定义云台控制应该发送成功");

        // 测试枚举云台指令
        ResultDTO<Void> enumResult = ptzCommand.controlDevicePtz(testDeviceId, PtzCmdEnum.UP, 128);
        assertTrue(enumResult.isSuccess(), "枚举云台控制应该发送成功");

        log.info("✅ 云台控制指令测试通过");
    }

    @Test
    @Order(13)
    @DisplayName("测试云台方向控制指令")
    public void testPtzDirectionControl() throws Exception {
        skipIfDeviceNotAvailable("云台方向控制指令测试");

        log.info("=== 开始云台方向控制指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试各方向移动（使用指定速度）
        assertTrue(ptzCommand.moveUp(testDeviceId, 100).isSuccess(), "向上移动应该发送成功");
        assertTrue(ptzCommand.moveDown(testDeviceId, 100).isSuccess(), "向下移动应该发送成功");
        assertTrue(ptzCommand.moveLeft(testDeviceId, 100).isSuccess(), "向左移动应该发送成功");
        assertTrue(ptzCommand.moveRight(testDeviceId, 100).isSuccess(), "向右移动应该发送成功");

        // 测试对角线移动
        assertTrue(ptzCommand.moveUpLeft(testDeviceId, 100).isSuccess(), "左上移动应该发送成功");
        assertTrue(ptzCommand.moveUpRight(testDeviceId, 100).isSuccess(), "右上移动应该发送成功");
        assertTrue(ptzCommand.moveDownLeft(testDeviceId, 100).isSuccess(), "左下移动应该发送成功");
        assertTrue(ptzCommand.moveDownRight(testDeviceId, 100).isSuccess(), "右下移动应该发送成功");

        // 测试变焦控制
        assertTrue(ptzCommand.zoomIn(testDeviceId, 100).isSuccess(), "放大变焦应该发送成功");
        assertTrue(ptzCommand.zoomOut(testDeviceId, 100).isSuccess(), "缩小变焦应该发送成功");

        // 测试停止移动
        assertTrue(ptzCommand.stopDevicePtz(testDeviceId).isSuccess(), "停止云台应该发送成功");

        log.info("✅ 云台方向控制指令测试通过");
    }

    @Test
    @Order(14)
    @DisplayName("测试云台默认速度控制指令")
    public void testPtzDefaultSpeedControl() throws Exception {
        skipIfDeviceNotAvailable("云台默认速度控制指令测试");

        log.info("=== 开始云台默认速度控制指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试默认速度移动
        assertTrue(ptzCommand.moveUp(testDeviceId).isSuccess(), "默认速度向上移动应该发送成功");
        assertTrue(ptzCommand.moveDown(testDeviceId).isSuccess(), "默认速度向下移动应该发送成功");
        assertTrue(ptzCommand.moveLeft(testDeviceId).isSuccess(), "默认速度向左移动应该发送成功");
        assertTrue(ptzCommand.moveRight(testDeviceId).isSuccess(), "默认速度向右移动应该发送成功");

        // 测试默认速度变焦
        assertTrue(ptzCommand.zoomIn(testDeviceId).isSuccess(), "默认速度放大变焦应该发送成功");
        assertTrue(ptzCommand.zoomOut(testDeviceId).isSuccess(), "默认速度缩小变焦应该发送成功");

        log.info("✅ 云台默认速度控制指令测试通过");
    }

    // ==================== 设备配置指令测试 ====================

    @Test
    @Order(15)
    @DisplayName("测试设备配置指令")
    public void testConfigDevice() throws Exception {
        skipIfDeviceNotAvailable("设备配置指令测试");

        log.info("=== 开始设备配置指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试完整参数配置
        ResultDTO<Void> fullConfigResult = configCommand.configDevice(testDeviceId, "测试摄像头", "3600", "60", "3");
        assertTrue(fullConfigResult.isSuccess(), "完整参数配置应该发送成功");

        // 测试默认参数配置
        ResultDTO<Void> defaultConfigResult = configCommand.configDevice(testDeviceId, "测试摄像头");
        assertTrue(defaultConfigResult.isSuccess(), "默认参数配置应该发送成功");

        log.info("✅ 设备配置指令测试通过");
    }

    @Test
    @Order(16)
    @DisplayName("测试设备配置下载指令")
    public void testDownloadDeviceConfig() throws Exception {
        skipIfDeviceNotAvailable("设备配置下载指令测试");

        log.info("=== 开始设备配置下载指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试各类配置下载
        assertTrue(configCommand.downloadBasicConfig(testDeviceId).isSuccess(), "基本配置下载应该发送成功");
        assertTrue(configCommand.downloadVideoConfig(testDeviceId).isSuccess(), "视频配置下载应该发送成功");
        assertTrue(configCommand.downloadAudioConfig(testDeviceId).isSuccess(), "音频配置下载应该发送成功");

        // 测试自定义配置下载
        assertTrue(configCommand.downloadDeviceConfig(testDeviceId, "CustomConfig").isSuccess(), "自定义配置下载应该发送成功");

        log.info("✅ 设备配置下载指令测试通过");
    }

    @Test
    @Order(17)
    @DisplayName("测试设备配置查询指令")
    public void testQueryDeviceConfig() throws Exception {
        skipIfDeviceNotAvailable("设备配置查询指令测试");

        log.info("=== 开始设备配置查询指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试各类配置查询
        assertTrue(configCommand.queryBasicConfig(testDeviceId).isSuccess(), "基本配置查询应该发送成功");
        assertTrue(configCommand.queryVideoConfig(testDeviceId).isSuccess(), "视频配置查询应该发送成功");
        assertTrue(configCommand.queryAudioConfig(testDeviceId).isSuccess(), "音频配置查询应该发送成功");

        log.info("✅ 设备配置查询指令测试通过");
    }

    @Test
    @Order(18)
    @DisplayName("测试设备重启指令")
    public void testRebootDevice() throws Exception {
        skipIfDeviceNotAvailable("设备重启指令测试");

        log.info("=== 开始设备重启指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试设备重启
        ResultDTO<Void> rebootResult = configCommand.rebootDevice(testDeviceId);
        assertTrue(rebootResult.isSuccess(), "设备重启指令应该发送成功");

        log.info("✅ 设备重启指令测试通过");
    }

    @Test
    @Order(19)
    @DisplayName("测试设备批量配置指令")
    public void testBatchConfigDevice() throws Exception {
        skipIfDeviceNotAvailable("设备批量配置指令测试");

        log.info("=== 开始设备批量配置指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试完整参数配置
        ResultDTO<Void> completeResult = configCommand.configDeviceComplete(testDeviceId, "测试设备", "7200", "120");
        assertTrue(completeResult.isSuccess(), "完整参数配置应该发送成功");

        // 测试高频心跳配置
        ResultDTO<Void> highFreqResult = configCommand.configHighFrequencyDevice(testDeviceId, "重要设备");
        assertTrue(highFreqResult.isSuccess(), "高频心跳配置应该发送成功");

        // 测试低频心跳配置
        ResultDTO<Void> lowFreqResult = configCommand.configLowFrequencyDevice(testDeviceId, "普通设备");
        assertTrue(lowFreqResult.isSuccess(), "低频心跳配置应该发送成功");

        log.info("✅ 设备批量配置指令测试通过");
    }

    // ==================== 媒体流指令测试 ====================

    @Test
    @Order(20)
    @DisplayName("测试实时流邀请指令")
    public void testInviteRealTimePlay() throws Exception {
        skipIfDeviceNotAvailable("实时流邀请指令测试");

        log.info("=== 开始实时流邀请指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();
        String sdpIp = "192.168.1.100";
        Integer mediaPort = 10000;

        // 测试基本实时流邀请
        ResultDTO<Void> basicResult = mediaCommand.inviteRealTimePlay(testDeviceId, sdpIp, mediaPort);
        assertTrue(basicResult.isSuccess(), "基本实时流邀请应该发送成功");

        // 测试UDP模式实时流邀请
        ResultDTO<Void> udpResult = mediaCommand.inviteRealTimePlayUdp(testDeviceId, sdpIp, mediaPort);
        assertTrue(udpResult.isSuccess(), "UDP实时流邀请应该发送成功");

        // 测试TCP模式实时流邀请
        ResultDTO<Void> tcpResult = mediaCommand.inviteRealTimePlayTcp(testDeviceId, sdpIp, mediaPort);
        assertTrue(tcpResult.isSuccess(), "TCP实时流邀请应该发送成功");

        // 测试使用InviteRequest对象邀请
        InviteRequest inviteRequest = mediaCommand.createInviteRequest(testDeviceId, StreamModeEnum.UDP, sdpIp, mediaPort);
        ResultDTO<Void> requestResult = mediaCommand.inviteRealTimePlay(testDeviceId, inviteRequest);
        assertTrue(requestResult.isSuccess(), "InviteRequest实时流邀请应该发送成功");

        log.info("✅ 实时流邀请指令测试通过");
    }

    @Test
    @Order(21)
    @DisplayName("测试回放流邀请指令")
    public void testInvitePlayBack() throws Exception {
        skipIfDeviceNotAvailable("回放流邀请指令测试");

        log.info("=== 开始回放流邀请指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();
        String sdpIp = "192.168.1.100";
        Integer mediaPort = 10000;
        String startTime = "2024-01-01T08:00:00";
        String endTime = "2024-01-01T18:00:00";

        // 测试基本回放流邀请
        ResultDTO<Void> basicResult = mediaCommand.invitePlayBack(testDeviceId, sdpIp, mediaPort, startTime, endTime);
        assertTrue(basicResult.isSuccess(), "基本回放流邀请应该发送成功");

        // 测试使用InviteRequest对象回放邀请
        InviteRequest playBackRequest = mediaCommand.createPlayBackInviteRequest(testDeviceId, StreamModeEnum.UDP,
            sdpIp, mediaPort, startTime, endTime);
        ResultDTO<Void> requestResult = mediaCommand.invitePlayBack(testDeviceId, playBackRequest);
        assertTrue(requestResult.isSuccess(), "InviteRequest回放流邀请应该发送成功");

        log.info("✅ 回放流邀请指令测试通过");
    }

    @Test
    @Order(22)
    @DisplayName("测试回放控制指令")
    public void testControlPlayBack() throws Exception {
        skipIfDeviceNotAvailable("回放控制指令测试");

        log.info("=== 开始回放控制指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试各种回放控制
        assertTrue(mediaCommand.controlPlayBack(testDeviceId, PlayActionEnums.PLAY_NOW).isSuccess(), "回放播放控制应该发送成功");
        assertTrue(mediaCommand.controlPlayBack(testDeviceId, PlayActionEnums.PLAY_RESUME).isSuccess(), "回放暂停控制应该发送成功");
        assertTrue(mediaCommand.controlPlayBack(testDeviceId, PlayActionEnums.PLAY_SPEED, 2.0).isSuccess(), "回放快进控制应该发送成功");
        assertTrue(mediaCommand.controlPlayBack(testDeviceId, PlayActionEnums.PLAY_SPEED, 0.5).isSuccess(), "回放慢放控制应该发送成功");
        assertTrue(mediaCommand.controlPlayBack(testDeviceId, PlayActionEnums.PLAY_RESUME).isSuccess(), "回放停止控制应该发送成功");

        // 测试便捷回放控制方法
        assertTrue(mediaCommand.playBack(testDeviceId).isSuccess(), "便捷播放控制应该发送成功");
        assertTrue(mediaCommand.pauseBack(testDeviceId).isSuccess(), "便捷暂停控制应该发送成功");
        assertTrue(mediaCommand.stopBack(testDeviceId).isSuccess(), "便捷停止控制应该发送成功");
        assertTrue(mediaCommand.fastForward(testDeviceId).isSuccess(), "便捷快进控制应该发送成功");
        assertTrue(mediaCommand.slowPlay(testDeviceId).isSuccess(), "便捷慢放控制应该发送成功");

        log.info("✅ 回放控制指令测试通过");
    }

    @Test
    @Order(23)
    @DisplayName("测试会话控制指令")
    public void testSessionControl() throws Exception {
        skipIfDeviceNotAvailable("会话控制指令测试");

        log.info("=== 开始会话控制指令测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 测试ACK响应
        assertTrue(mediaCommand.sendAck(testDeviceId).isSuccess(), "ACK响应应该发送成功");

        // 测试带CallId的ACK响应
        assertTrue(mediaCommand.sendAck(testDeviceId, "test-call-id-123").isSuccess(), "带CallId的ACK响应应该发送成功");

        // 测试BYE请求
        assertTrue(mediaCommand.sendBye(testDeviceId).isSuccess(), "BYE请求应该发送成功");

        // 测试设备广播
        assertTrue(mediaCommand.sendBroadcast(testDeviceId).isSuccess(), "设备广播应该发送成功");

        log.info("✅ 会话控制指令测试通过");
    }

    // ==================== 综合功能测试 ====================

    @Test
    @Order(24)
    @DisplayName("测试指令组合使用场景")
    public void testCombinedCommandScenarios() throws Exception {
        skipIfDeviceNotAvailable("指令组合使用场景测试");

        log.info("=== 开始指令组合使用场景测试 ===");

        String testDeviceId = generateTestClientDeviceId();

        // 场景1：设备初始化流程
        log.info("场景1：设备初始化流程");
        assertTrue(deviceCommand.queryDeviceInfo(testDeviceId).isSuccess(), "设备信息查询");
        assertTrue(deviceCommand.queryDeviceStatus(testDeviceId).isSuccess(), "设备状态查询");
        assertTrue(deviceCommand.queryDeviceCatalog(testDeviceId).isSuccess(), "设备目录查询");
        assertTrue(configCommand.configDevice(testDeviceId, "测试设备").isSuccess(), "设备配置");

        // 场景2：录像查询和回放流程
        log.info("场景2：录像查询和回放流程");
        assertTrue(recordCommand.queryTodayDeviceRecord(testDeviceId).isSuccess(), "今日录像查询");
        assertTrue(mediaCommand.invitePlayBack(testDeviceId, "192.168.1.100", 10000,
            "2024-01-01T08:00:00", "2024-01-01T18:00:00").isSuccess(), "回放流邀请");
        assertTrue(mediaCommand.playBack(testDeviceId).isSuccess(), "开始回放");
        assertTrue(mediaCommand.pauseBack(testDeviceId).isSuccess(), "暂停回放");
        assertTrue(mediaCommand.stopBack(testDeviceId).isSuccess(), "停止回放");

        // 场景3：云台控制流程
        log.info("场景3：云台控制流程");
        assertTrue(ptzCommand.moveUp(testDeviceId, 100).isSuccess(), "向上移动");
        Thread.sleep(100); // 模拟移动时间
        assertTrue(ptzCommand.moveRight(testDeviceId, 100).isSuccess(), "向右移动");
        Thread.sleep(100);
        assertTrue(ptzCommand.zoomIn(testDeviceId, 100).isSuccess(), "放大变焦");
        Thread.sleep(100);
        assertTrue(ptzCommand.stopDevicePtz(testDeviceId).isSuccess(), "停止移动");

        // 场景4：告警处理流程
        log.info("场景4：告警处理流程");
        assertTrue(alarmCommand.queryTodayDeviceAlarm(testDeviceId).isSuccess(), "今日告警查询");
        assertTrue(alarmCommand.enableNetworkAlarm(testDeviceId, "1").isSuccess(), "启用网络告警");

        log.info("✅ 指令组合使用场景测试通过");
    }
}