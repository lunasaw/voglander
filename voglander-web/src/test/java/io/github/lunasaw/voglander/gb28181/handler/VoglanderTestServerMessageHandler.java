package io.github.lunasaw.voglander.gb28181.handler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gbproxy.server.transmit.request.info.ServerInfoProcessorHandler;
import io.github.lunasaw.gbproxy.server.transmit.request.message.ServerMessageProcessorHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * Voglander测试用服务端消息处理器
 * <p>
 * 用于GB28181集成测试，捕获和验证从客户端发送到服务端的消息。
 * 提供异步等待和验证机制，确保测试的可靠性。
 * </p>
 * 
 * <p>
 * <strong>重要修复</strong>：使用ThreadLocal隔离测试状态，避免多个测试间的静态变量竞争问题。
 * 每个测试线程维护独立的消息接收状态，确保测试执行的可靠性。
 * </p>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.1 - 修复静态变量竞争问题
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "sip.enable", havingValue = "true")
public class VoglanderTestServerMessageHandler implements ServerMessageProcessorHandler, ServerInfoProcessorHandler {

    // 测试状态类，包含单个测试所需的所有状态
    private static class TestState {
        // 各种消息类型的接收状态
        final AtomicBoolean                         receivedKeepalive      = new AtomicBoolean(false);
        final AtomicBoolean                         receivedAlarm          = new AtomicBoolean(false);
        final AtomicBoolean                         receivedCatalog        = new AtomicBoolean(false);
        final AtomicBoolean                         receivedDeviceInfo     = new AtomicBoolean(false);
        final AtomicBoolean                         receivedDeviceStatus   = new AtomicBoolean(false);
        final AtomicBoolean                         receivedDeviceRecord   = new AtomicBoolean(false);
        final AtomicBoolean                         receivedDeviceConfig   = new AtomicBoolean(false);
        final AtomicBoolean                         receivedMobilePosition = new AtomicBoolean(false);

        // 消息内容存储
        final AtomicReference<DeviceKeepLiveNotify> keepaliveNotify        = new AtomicReference<>();
        final AtomicReference<DeviceAlarmNotify>    alarmNotify            = new AtomicReference<>();
        final AtomicReference<DeviceResponse>       catalogResponse        = new AtomicReference<>();
        final AtomicReference<DeviceInfo>           deviceInfo             = new AtomicReference<>();
        final AtomicReference<DeviceStatus>         deviceStatus           = new AtomicReference<>();
        final AtomicReference<DeviceRecord>         deviceRecord           = new AtomicReference<>();
        final AtomicReference<DeviceConfigResponse> deviceConfig           = new AtomicReference<>();
        final AtomicReference<MobilePositionNotify> mobilePosition         = new AtomicReference<>();

        // 同步等待锁
        final AtomicReference<CountDownLatch>       keepaliveLatch         = new AtomicReference<>(new CountDownLatch(1));
        final AtomicReference<CountDownLatch>       alarmLatch             = new AtomicReference<>(new CountDownLatch(1));
        final AtomicReference<CountDownLatch>       catalogLatch           = new AtomicReference<>(new CountDownLatch(1));
        final AtomicReference<CountDownLatch>       deviceInfoLatch        = new AtomicReference<>(new CountDownLatch(1));
        final AtomicReference<CountDownLatch>       deviceStatusLatch      = new AtomicReference<>(new CountDownLatch(1));
        final AtomicReference<CountDownLatch>       deviceRecordLatch      = new AtomicReference<>(new CountDownLatch(1));
        final AtomicReference<CountDownLatch>       deviceConfigLatch      = new AtomicReference<>(new CountDownLatch(1));
        final AtomicReference<CountDownLatch>       mobilePositionLatch    = new AtomicReference<>(new CountDownLatch(1));
    }

    // 使用ConcurrentHashMap基于测试会话ID管理状态，支持跨线程访问
    private static final java.util.concurrent.ConcurrentHashMap<String, TestState> testStateMap         =
        new java.util.concurrent.ConcurrentHashMap<>();

    // 当前活跃的测试会话ID（用于跨线程访问）- 使用ThreadLocal作为主要机制
    private static final ThreadLocal<String>                                       currentTestSessionId = new ThreadLocal<>();

    // 备用的全局会话ID，用于跨线程访问
    private static volatile String                                                 globalTestSessionId  = null;

    // 获取当前测试状态，支持跨线程访问，优先使用ThreadLocal
    private static TestState getTestState() {
        // 1. 首先尝试从ThreadLocal获取（主线程）
        String sessionId = currentTestSessionId.get();

        // 2. 如果ThreadLocal为空，使用全局会话ID（跨线程场景）
        if (sessionId == null) {
            sessionId = globalTestSessionId;
        }

        // 3. 如果全局会话ID也为空，使用默认的fallback会话ID而不是线程名
        if (sessionId == null) {
            sessionId = "default-test-session";
            log.warn("没有找到有效的测试会话ID，使用默认会话ID: {} - Thread: {}",
                sessionId, Thread.currentThread().getName());
        }

        return testStateMap.computeIfAbsent(sessionId, k -> new TestState());
    }

    // 获取当前会话ID的工具方法
    private static String getCurrentSessionId() {
        String sessionId = currentTestSessionId.get();
        if (sessionId == null) {
            sessionId = globalTestSessionId;
        }
        if (sessionId == null) {
            sessionId = "default-test-session";
        }
        return sessionId;
    }

    // 设置当前测试会话ID
    public static void setTestSessionId(String sessionId) {
        log.debug("设置测试会话ID: {} - Thread: {}", sessionId, Thread.currentThread().getName());

        // 设置ThreadLocal（主线程使用）
        currentTestSessionId.set(sessionId);

        // 同时设置全局变量（跨线程使用）
        globalTestSessionId = sessionId;

        // 确保状态对象存在
        testStateMap.computeIfAbsent(sessionId, k -> new TestState());
    }

    /**
     * 重置指定测试会话的状态
     * 使用会话ID确保跨线程状态同步，避免测试间相互干扰
     */
    public static void resetTestState() {
        String sessionId = getCurrentSessionId();
        if (sessionId == null || sessionId.equals(Thread.currentThread().getName())) {
            // 如果会话ID不存在或者是默认线程名，说明可能没有正确设置
            log.warn("测试会话ID未正确设置，当前值: {}, 线程: {}", sessionId, Thread.currentThread().getName());
        }

        TestState state = getTestState();
        log.debug("重置测试会话状态 - SessionId: {}, Thread: {}",
            sessionId, Thread.currentThread().getName());

        // 重置接收状态
        state.receivedKeepalive.set(false);
        state.receivedAlarm.set(false);
        state.receivedCatalog.set(false);
        state.receivedDeviceInfo.set(false);
        state.receivedDeviceStatus.set(false);
        state.receivedDeviceRecord.set(false);
        state.receivedDeviceConfig.set(false);
        state.receivedMobilePosition.set(false);

        // 清空消息内容
        state.keepaliveNotify.set(null);
        state.alarmNotify.set(null);
        state.catalogResponse.set(null);
        state.deviceInfo.set(null);
        state.deviceStatus.set(null);
        state.deviceRecord.set(null);
        state.deviceConfig.set(null);
        state.mobilePosition.set(null);

        // 重置等待锁
        state.keepaliveLatch.set(new CountDownLatch(1));
        state.alarmLatch.set(new CountDownLatch(1));
        state.catalogLatch.set(new CountDownLatch(1));
        state.deviceInfoLatch.set(new CountDownLatch(1));
        state.deviceStatusLatch.set(new CountDownLatch(1));
        state.deviceRecordLatch.set(new CountDownLatch(1));
        state.deviceConfigLatch.set(new CountDownLatch(1));
        state.mobilePositionLatch.set(new CountDownLatch(1));
    }

    /**
     * 清理指定测试会话的状态
     * 防止内存泄漏，应该在测试完成后调用
     */
    public static void clearTestState() {
        String sessionId = getCurrentSessionId();
        if (sessionId != null) {
            log.debug("清理测试会话状态 - SessionId: {}, Thread: {}",
                sessionId, Thread.currentThread().getName());
            testStateMap.remove(sessionId);

            // 清理ThreadLocal
            currentTestSessionId.remove();

            // 如果全局会话ID与当前会话ID相同，也清理全局会话ID
            if (sessionId.equals(globalTestSessionId)) {
                globalTestSessionId = null;
            }
        }
    }

    // ==================== 等待方法 ====================

    public static boolean waitForKeepalive(long timeout, TimeUnit unit) throws InterruptedException {
        TestState state = getTestState();
        CountDownLatch latch = state.keepaliveLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForAlarm(long timeout, TimeUnit unit) throws InterruptedException {
        TestState state = getTestState();
        CountDownLatch latch = state.alarmLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForCatalog(long timeout, TimeUnit unit) throws InterruptedException {
        TestState state = getTestState();
        CountDownLatch latch = state.catalogLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceInfo(long timeout, TimeUnit unit) throws InterruptedException {
        TestState state = getTestState();
        CountDownLatch latch = state.deviceInfoLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceStatus(long timeout, TimeUnit unit) throws InterruptedException {
        TestState state = getTestState();
        CountDownLatch latch = state.deviceStatusLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceRecord(long timeout, TimeUnit unit) throws InterruptedException {
        TestState state = getTestState();
        CountDownLatch latch = state.deviceRecordLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceConfig(long timeout, TimeUnit unit) throws InterruptedException {
        TestState state = getTestState();
        CountDownLatch latch = state.deviceConfigLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForMobilePosition(long timeout, TimeUnit unit) throws InterruptedException {
        TestState state = getTestState();
        CountDownLatch latch = state.mobilePositionLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    // ==================== 状态检查方法 ====================

    public static boolean hasReceivedKeepalive() {
        TestState state = getTestState();
        return state.receivedKeepalive.get();
    }

    public static boolean hasReceivedAlarm() {
        TestState state = getTestState();
        return state.receivedAlarm.get();
    }

    public static boolean hasReceivedCatalog() {
        TestState state = getTestState();
        return state.receivedCatalog.get();
    }

    public static boolean hasReceivedDeviceInfo() {
        TestState state = getTestState();
        return state.receivedDeviceInfo.get();
    }

    public static boolean hasReceivedDeviceStatus() {
        TestState state = getTestState();
        return state.receivedDeviceStatus.get();
    }

    public static boolean hasReceivedDeviceRecord() {
        TestState state = getTestState();
        return state.receivedDeviceRecord.get();
    }

    public static boolean hasReceivedDeviceConfig() {
        TestState state = getTestState();
        return state.receivedDeviceConfig.get();
    }

    public static boolean hasReceivedMobilePosition() {
        TestState state = getTestState();
        return state.receivedMobilePosition.get();
    }

    // ==================== 内容获取方法 ====================

    public static DeviceKeepLiveNotify getReceivedKeepalive() {
        TestState state = getTestState();
        return state.keepaliveNotify.get();
    }

    public static DeviceAlarmNotify getReceivedAlarm() {
        TestState state = getTestState();
        return state.alarmNotify.get();
    }

    public static DeviceResponse getReceivedCatalog() {
        TestState state = getTestState();
        return state.catalogResponse.get();
    }

    public static DeviceInfo getReceivedDeviceInfo() {
        TestState state = getTestState();
        return state.deviceInfo.get();
    }

    public static DeviceStatus getReceivedDeviceStatus() {
        TestState state = getTestState();
        return state.deviceStatus.get();
    }

    public static DeviceRecord getReceivedDeviceRecord() {
        TestState state = getTestState();
        return state.deviceRecord.get();
    }

    public static DeviceConfigResponse getReceivedDeviceConfig() {
        TestState state = getTestState();
        return state.deviceConfig.get();
    }

    public static MobilePositionNotify getReceivedMobilePosition() {
        TestState state = getTestState();
        return state.mobilePosition.get();
    }

    // ==================== 接口实现方法 ====================

    @Override
    public void handleMessageRequest(RequestEvent evt, FromDevice fromDevice) {
        log.info("✅ 测试处理器接收到MESSAGE请求 - fromDevice: {}", fromDevice != null ? fromDevice.getUserId() : "null");
        // 在测试环境中，主要关注消息的接收和验证，不需要具体的业务处理
    }

    @Override
    public boolean validateDevicePermission(RequestEvent evt) {
        // 测试环境中总是返回true
        return true;
    }

    @Override
    public FromDevice getFromDevice() {
        return null; // 测试环境中不需要实现
    }

    @Override
    public void handleMessageError(RequestEvent evt, String errorMessage) {
        log.error("测试MESSAGE错误 - error: {}", errorMessage);
    }

    @Override
    public void keepLiveDevice(DeviceKeepLiveNotify deviceKeepLiveNotify) {
        log.info("测试接收到设备保活通知 - deviceId: {}, Thread: {}",
            deviceKeepLiveNotify != null ? deviceKeepLiveNotify.getDeviceId() : "null",
            Thread.currentThread().getName());

        if (deviceKeepLiveNotify != null) {
            TestState state = getTestState();
            state.keepaliveNotify.set(deviceKeepLiveNotify);
            state.receivedKeepalive.set(true);

            CountDownLatch latch = state.keepaliveLatch.get();
            if (latch != null) {
                latch.countDown();
                log.debug("设备保活CountDownLatch已通知");
            }
        }
    }

    @Override
    public void updateRemoteAddress(String userId, RemoteAddressInfo remoteAddressInfo) {
        log.debug("测试更新远程地址 - userId: {}", userId);
    }

    @Override
    public void updateDeviceAlarm(DeviceAlarmNotify deviceAlarmNotify) {
        log.info("测试接收到设备告警通知 - deviceId: {}, Thread: {}",
            deviceAlarmNotify != null ? deviceAlarmNotify.getDeviceId() : "null",
            Thread.currentThread().getName());

        if (deviceAlarmNotify != null) {
            TestState state = getTestState();
            state.alarmNotify.set(deviceAlarmNotify);
            state.receivedAlarm.set(true);

            CountDownLatch latch = state.alarmLatch.get();
            if (latch != null) {
                latch.countDown();
                log.debug("设备告警CountDownLatch已通知");
            }
        }
    }

    @Override
    public void updateMobilePosition(MobilePositionNotify mobilePositionNotify) {
        log.info("测试接收到移动位置通知 - deviceId: {}, Thread: {}",
            mobilePositionNotify != null ? mobilePositionNotify.getDeviceId() : "null",
            Thread.currentThread().getName());

        if (mobilePositionNotify != null) {
            TestState state = getTestState();
            state.mobilePosition.set(mobilePositionNotify);
            state.receivedMobilePosition.set(true);

            CountDownLatch latch = state.mobilePositionLatch.get();
            if (latch != null) {
                latch.countDown();
                log.debug("移动位置CountDownLatch已通知");
            }
        }
    }

    @Override
    public void updateMediaStatus(MediaStatusNotify mediaStatusNotify) {
        log.debug("测试接收到媒体状态通知 - deviceId: {}",
            mediaStatusNotify != null ? mediaStatusNotify.getDeviceId() : "null");
    }

    @Override
    public void updateDeviceRecord(String userId, DeviceRecord deviceRecord) {
        log.info("测试接收到设备录像记录 - userId: {}, Thread: {}",
            userId, Thread.currentThread().getName());

        if (deviceRecord != null) {
            TestState state = getTestState();
            state.deviceRecord.set(deviceRecord);
            state.receivedDeviceRecord.set(true);

            CountDownLatch latch = state.deviceRecordLatch.get();
            if (latch != null) {
                latch.countDown();
                log.debug("设备录像记录CountDownLatch已通知");
            }
        }
    }

    @Override
    public void updateDeviceResponse(String userId, DeviceResponse deviceResponse) {
        log.info("测试接收到设备响应信息 - userId: {}, Thread: {}",
            userId, Thread.currentThread().getName());

        if (deviceResponse != null) {
            TestState state = getTestState();
            state.catalogResponse.set(deviceResponse);
            state.receivedCatalog.set(true);

            CountDownLatch latch = state.catalogLatch.get();
            if (latch != null) {
                latch.countDown();
                log.debug("设备响应CountDownLatch已通知");
            }
        }
    }

    @Override
    public void updateDeviceInfo(String userId, DeviceInfo deviceInfo) {
        String sessionId = getCurrentSessionId();
        log.info("测试接收到设备信息 - userId: {}, SessionId: {}, Thread: {}",
            userId, sessionId, Thread.currentThread().getName());

        if (deviceInfo != null) {
            TestState state = getTestState();
            state.deviceInfo.set(deviceInfo);
            state.receivedDeviceInfo.set(true);

            CountDownLatch latch = state.deviceInfoLatch.get();
            if (latch != null) {
                latch.countDown();
                log.debug("设备信息CountDownLatch已通知 - SessionId: {}", sessionId);
            } else {
                log.warn("设备信息CountDownLatch为空 - SessionId: {}", sessionId);
            }
        }
    }

    @Override
    public void updateDeviceConfig(String userId, DeviceConfigResponse deviceConfigResponse) {
        log.info("测试接收到设备配置响应 - userId: {}, Thread: {}",
            userId, Thread.currentThread().getName());

        if (deviceConfigResponse != null) {
            TestState state = getTestState();
            state.deviceConfig.set(deviceConfigResponse);
            state.receivedDeviceConfig.set(true);

            CountDownLatch latch = state.deviceConfigLatch.get();
            if (latch != null) {
                latch.countDown();
                log.debug("设备配置CountDownLatch已通知");
            }
        }
    }

    @Override
    public void updateDeviceStatus(String userId, DeviceStatus deviceStatus) {
        String sessionId = getCurrentSessionId();
        log.info("测试接收到设备状态 - userId: {}, SessionId: {}, Thread: {}",
            userId, sessionId, Thread.currentThread().getName());

        if (deviceStatus != null) {
            TestState state = getTestState();
            state.deviceStatus.set(deviceStatus);
            state.receivedDeviceStatus.set(true);

            CountDownLatch latch = state.deviceStatusLatch.get();
            if (latch != null) {
                latch.countDown();
                log.debug("设备状态CountDownLatch已通知 - SessionId: {}", sessionId);
            } else {
                log.warn("设备状态CountDownLatch为空 - SessionId: {}", sessionId);
            }
        }
    }

    // ==================== ServerInfoProcessorHandler 接口实现 ====================

    @Override
    public void handleInfoRequest(String userId, String content, RequestEvent evt) {
        log.info("✅ 测试处理器接收到INFO请求 - userId: {}, content 长度: {}", userId, content != null ? content.length() : 0);

        if (content != null && content.contains("<CmdType>DeviceInfo</CmdType>")) {
            log.info("🎯 检测到DeviceInfo响应消息，解析并处理");
            // 这里模拟解析DeviceInfo并调用updateDeviceInfo
            try {
                // 简单的XML解析，提取基本信息
                DeviceInfo deviceInfo = new DeviceInfo();
                deviceInfo.setDeviceId(extractXmlValue(content, "DeviceID"));
                deviceInfo.setDeviceName(extractXmlValue(content, "DeviceName"));
                deviceInfo.setManufacturer(extractXmlValue(content, "Manufacturer"));
                deviceInfo.setModel(extractXmlValue(content, "Model"));
                deviceInfo.setFirmware(extractXmlValue(content, "Firmware"));
                String channelStr = extractXmlValue(content, "Channel");
                if (channelStr != null) {
                    try {
                        deviceInfo.setChannel(Integer.parseInt(channelStr));
                    } catch (NumberFormatException e) {
                        deviceInfo.setChannel(1);
                    }
                }

                updateDeviceInfo(userId, deviceInfo);
            } catch (Exception e) {
                log.error("解析DeviceInfo失败", e);
            }
        }
    }

    @Override
    public boolean validateDevicePermission(String userId, String sipId, RequestEvent evt) {
        log.debug("测试环境 - 设备权限验证始终返回true - userId: {}, sipId: {}", userId, sipId);
        return true;
    }

    @Override
    public void handleInfoError(String userId, String errorMessage, RequestEvent evt) {
        log.error("测试处理器处理INFO错误 - userId: {}, error: {}", userId, errorMessage);
    }

    // ==================== 辅助方法 ====================

    private String extractXmlValue(String xml, String tagName) {
        if (xml == null || tagName == null)
            return null;

        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";

        int startIndex = xml.indexOf(startTag);
        if (startIndex == -1)
            return null;

        int valueStart = startIndex + startTag.length();
        int endIndex = xml.indexOf(endTag, valueStart);
        if (endIndex == -1)
            return null;

        return xml.substring(valueStart, endIndex).trim();
    }
}