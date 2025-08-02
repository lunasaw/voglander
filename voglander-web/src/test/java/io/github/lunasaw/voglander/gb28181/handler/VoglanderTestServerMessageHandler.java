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
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "sip.server.enabled", havingValue = "true")
public class VoglanderTestServerMessageHandler implements ServerMessageProcessorHandler, ServerInfoProcessorHandler {

    // 各种消息类型的接收状态
    private static final AtomicBoolean                         receivedKeepalive      = new AtomicBoolean(false);
    private static final AtomicBoolean                         receivedAlarm          = new AtomicBoolean(false);
    private static final AtomicBoolean                         receivedCatalog        = new AtomicBoolean(false);
    private static final AtomicBoolean                         receivedDeviceInfo     = new AtomicBoolean(false);
    private static final AtomicBoolean                         receivedDeviceStatus   = new AtomicBoolean(false);
    private static final AtomicBoolean                         receivedDeviceRecord   = new AtomicBoolean(false);
    private static final AtomicBoolean                         receivedDeviceConfig   = new AtomicBoolean(false);
    private static final AtomicBoolean                         receivedMobilePosition = new AtomicBoolean(false);

    // 消息内容存储
    private static final AtomicReference<DeviceKeepLiveNotify> keepaliveNotify        = new AtomicReference<>();
    private static final AtomicReference<DeviceAlarmNotify>    alarmNotify            = new AtomicReference<>();
    private static final AtomicReference<DeviceResponse>       catalogResponse        = new AtomicReference<>();
    private static final AtomicReference<DeviceInfo>           deviceInfo             = new AtomicReference<>();
    private static final AtomicReference<DeviceStatus>         deviceStatus           = new AtomicReference<>();
    private static final AtomicReference<DeviceRecord>         deviceRecord           = new AtomicReference<>();
    private static final AtomicReference<DeviceConfigResponse> deviceConfig           = new AtomicReference<>();
    private static final AtomicReference<MobilePositionNotify> mobilePosition         = new AtomicReference<>();

    // 同步等待锁
    private static final AtomicReference<CountDownLatch>       keepaliveLatch         = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>       alarmLatch             = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>       catalogLatch           = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>       deviceInfoLatch        = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>       deviceStatusLatch      = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>       deviceRecordLatch      = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>       deviceConfigLatch      = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>       mobilePositionLatch    = new AtomicReference<>();

    /**
     * 重置所有测试状态
     */
    public static void resetTestState() {
        log.debug("重置VoglanderTestServerMessageHandler测试状态");

        // 重置接收状态
        receivedKeepalive.set(false);
        receivedAlarm.set(false);
        receivedCatalog.set(false);
        receivedDeviceInfo.set(false);
        receivedDeviceStatus.set(false);
        receivedDeviceRecord.set(false);
        receivedDeviceConfig.set(false);
        receivedMobilePosition.set(false);

        // 清空消息内容
        keepaliveNotify.set(null);
        alarmNotify.set(null);
        catalogResponse.set(null);
        deviceInfo.set(null);
        deviceStatus.set(null);
        deviceRecord.set(null);
        deviceConfig.set(null);
        mobilePosition.set(null);

        // 重置等待锁
        keepaliveLatch.set(new CountDownLatch(1));
        alarmLatch.set(new CountDownLatch(1));
        catalogLatch.set(new CountDownLatch(1));
        deviceInfoLatch.set(new CountDownLatch(1));
        deviceStatusLatch.set(new CountDownLatch(1));
        deviceRecordLatch.set(new CountDownLatch(1));
        deviceConfigLatch.set(new CountDownLatch(1));
        mobilePositionLatch.set(new CountDownLatch(1));
    }

    // ==================== 等待方法 ====================

    public static boolean waitForKeepalive(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = keepaliveLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForAlarm(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = alarmLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForCatalog(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = catalogLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceInfo(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceInfoLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceStatus(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceStatusLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceRecord(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceRecordLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceConfig(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceConfigLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForMobilePosition(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = mobilePositionLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    // ==================== 状态检查方法 ====================

    public static boolean hasReceivedKeepalive() {
        return receivedKeepalive.get();
    }

    public static boolean hasReceivedAlarm() {
        return receivedAlarm.get();
    }

    public static boolean hasReceivedCatalog() {
        return receivedCatalog.get();
    }

    public static boolean hasReceivedDeviceInfo() {
        return receivedDeviceInfo.get();
    }

    public static boolean hasReceivedDeviceStatus() {
        return receivedDeviceStatus.get();
    }

    public static boolean hasReceivedDeviceRecord() {
        return receivedDeviceRecord.get();
    }

    public static boolean hasReceivedDeviceConfig() {
        return receivedDeviceConfig.get();
    }

    public static boolean hasReceivedMobilePosition() {
        return receivedMobilePosition.get();
    }

    // ==================== 内容获取方法 ====================

    public static DeviceKeepLiveNotify getReceivedKeepalive() {
        return keepaliveNotify.get();
    }

    public static DeviceAlarmNotify getReceivedAlarm() {
        return alarmNotify.get();
    }

    public static DeviceResponse getReceivedCatalog() {
        return catalogResponse.get();
    }

    public static DeviceInfo getReceivedDeviceInfo() {
        return deviceInfo.get();
    }

    public static DeviceStatus getReceivedDeviceStatus() {
        return deviceStatus.get();
    }

    public static DeviceRecord getReceivedDeviceRecord() {
        return deviceRecord.get();
    }

    public static DeviceConfigResponse getReceivedDeviceConfig() {
        return deviceConfig.get();
    }

    public static MobilePositionNotify getReceivedMobilePosition() {
        return mobilePosition.get();
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
        log.info("测试接收到设备保活通知 - deviceId: {}",
            deviceKeepLiveNotify != null ? deviceKeepLiveNotify.getDeviceId() : "null");

        if (deviceKeepLiveNotify != null) {
            keepaliveNotify.set(deviceKeepLiveNotify);
            receivedKeepalive.set(true);

            CountDownLatch latch = keepaliveLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public void updateRemoteAddress(String userId, RemoteAddressInfo remoteAddressInfo) {
        log.debug("测试更新远程地址 - userId: {}", userId);
    }

    @Override
    public void updateDeviceAlarm(DeviceAlarmNotify deviceAlarmNotify) {
        log.info("测试接收到设备告警通知 - deviceId: {}",
            deviceAlarmNotify != null ? deviceAlarmNotify.getDeviceId() : "null");

        if (deviceAlarmNotify != null) {
            alarmNotify.set(deviceAlarmNotify);
            receivedAlarm.set(true);

            CountDownLatch latch = alarmLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public void updateMobilePosition(MobilePositionNotify mobilePositionNotify) {
        log.info("测试接收到移动位置通知 - deviceId: {}",
            mobilePositionNotify != null ? mobilePositionNotify.getDeviceId() : "null");

        if (mobilePositionNotify != null) {
            mobilePosition.set(mobilePositionNotify);
            receivedMobilePosition.set(true);

            CountDownLatch latch = mobilePositionLatch.get();
            if (latch != null) {
                latch.countDown();
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
        log.info("测试接收到设备录像记录 - userId: {}", userId);

        if (deviceRecord != null) {
            this.deviceRecord.set(deviceRecord);
            receivedDeviceRecord.set(true);

            CountDownLatch latch = deviceRecordLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public void updateDeviceResponse(String userId, DeviceResponse deviceResponse) {
        log.info("测试接收到设备响应信息 - userId: {}", userId);

        if (deviceResponse != null) {
            catalogResponse.set(deviceResponse);
            receivedCatalog.set(true);

            CountDownLatch latch = catalogLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public void updateDeviceInfo(String userId, DeviceInfo deviceInfo) {
        log.info("测试接收到设备信息 - userId: {}", userId);

        if (deviceInfo != null) {
            this.deviceInfo.set(deviceInfo);
            receivedDeviceInfo.set(true);

            CountDownLatch latch = deviceInfoLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public void updateDeviceConfig(String userId, DeviceConfigResponse deviceConfigResponse) {
        log.info("测试接收到设备配置响应 - userId: {}", userId);

        if (deviceConfigResponse != null) {
            deviceConfig.set(deviceConfigResponse);
            receivedDeviceConfig.set(true);

            CountDownLatch latch = deviceConfigLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public void updateDeviceStatus(String userId, DeviceStatus deviceStatus) {
        log.info("测试接收到设备状态 - userId: {}", userId);

        if (deviceStatus != null) {
            this.deviceStatus.set(deviceStatus);
            receivedDeviceStatus.set(true);

            CountDownLatch latch = deviceStatusLatch.get();
            if (latch != null) {
                latch.countDown();
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