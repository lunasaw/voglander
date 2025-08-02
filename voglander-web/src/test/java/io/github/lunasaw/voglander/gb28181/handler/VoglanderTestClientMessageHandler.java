package io.github.lunasaw.voglander.gb28181.handler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.query.*;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Voglander测试用客户端消息处理器
 * <p>
 * 用于GB28181集成测试，模拟客户端响应服务端的查询和控制指令。
 * 提供异步等待和验证机制，确保测试的可靠性。
 * </p>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Slf4j
@Component
public class VoglanderTestClientMessageHandler implements MessageRequestHandler {

    // 各种查询请求的接收状态
    private static final AtomicBoolean                          receivedDeviceRecordQuery   = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedDeviceStatusQuery   = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedDeviceInfoQuery     = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedDeviceItemQuery     = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedBroadcastNotify     = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedDeviceAlarmQuery    = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedDeviceConfigQuery   = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedConfigDownloadQuery = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedDeviceControl       = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedPresetQuery         = new AtomicBoolean(false);
    private static final AtomicBoolean                          receivedMobilePositionQuery = new AtomicBoolean(false);

    // 查询内容存储
    private static final AtomicReference<DeviceRecordQuery>     deviceRecordQuery           = new AtomicReference<>();
    private static final AtomicReference<String>                deviceStatusUserId          = new AtomicReference<>();
    private static final AtomicReference<String>                deviceInfoUserId            = new AtomicReference<>();
    private static final AtomicReference<String>                deviceItemUserId            = new AtomicReference<>();
    private static final AtomicReference<DeviceBroadcastNotify> broadcastNotify             = new AtomicReference<>();
    private static final AtomicReference<DeviceAlarmQuery>      deviceAlarmQuery            = new AtomicReference<>();
    private static final AtomicReference<DeviceConfigDownload>  deviceConfigDownload        = new AtomicReference<>();
    private static final AtomicReference<String>                configDownloadUserId        = new AtomicReference<>();
    private static final AtomicReference<Object>                deviceControlBase           = new AtomicReference<>();
    private static final AtomicReference<PresetQuery>           presetQuery                 = new AtomicReference<>();
    private static final AtomicReference<MobilePositionQuery>   mobilePositionQuery         = new AtomicReference<>();

    // 同步等待锁
    private static final AtomicReference<CountDownLatch>        deviceRecordLatch           = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        deviceStatusLatch           = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        deviceInfoLatch             = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        deviceItemLatch             = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        broadcastLatch              = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        deviceAlarmLatch            = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        deviceConfigLatch           = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        configDownloadLatch         = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        deviceControlLatch          = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        presetQueryLatch            = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch>        mobilePositionLatch         = new AtomicReference<>();

    /**
     * 重置所有测试状态
     */
    public static void resetTestState() {
        log.debug("重置VoglanderTestClientMessageHandler测试状态");

        // 重置接收状态
        receivedDeviceRecordQuery.set(false);
        receivedDeviceStatusQuery.set(false);
        receivedDeviceInfoQuery.set(false);
        receivedDeviceItemQuery.set(false);
        receivedBroadcastNotify.set(false);
        receivedDeviceAlarmQuery.set(false);
        receivedDeviceConfigQuery.set(false);
        receivedConfigDownloadQuery.set(false);
        receivedDeviceControl.set(false);
        receivedPresetQuery.set(false);
        receivedMobilePositionQuery.set(false);

        // 清空内容
        deviceRecordQuery.set(null);
        deviceStatusUserId.set(null);
        deviceInfoUserId.set(null);
        deviceItemUserId.set(null);
        broadcastNotify.set(null);
        deviceAlarmQuery.set(null);
        deviceConfigDownload.set(null);
        configDownloadUserId.set(null);
        deviceControlBase.set(null);
        presetQuery.set(null);
        mobilePositionQuery.set(null);

        // 重置等待锁
        deviceRecordLatch.set(new CountDownLatch(1));
        deviceStatusLatch.set(new CountDownLatch(1));
        deviceInfoLatch.set(new CountDownLatch(1));
        deviceItemLatch.set(new CountDownLatch(1));
        broadcastLatch.set(new CountDownLatch(1));
        deviceAlarmLatch.set(new CountDownLatch(1));
        deviceConfigLatch.set(new CountDownLatch(1));
        configDownloadLatch.set(new CountDownLatch(1));
        deviceControlLatch.set(new CountDownLatch(1));
        presetQueryLatch.set(new CountDownLatch(1));
        mobilePositionLatch.set(new CountDownLatch(1));
    }

    // ==================== 等待方法 ====================

    public static boolean waitForDeviceRecordQuery(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceRecordLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceStatusQuery(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceStatusLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceInfoQuery(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceInfoLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceItemQuery(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceItemLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForBroadcastNotify(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = broadcastLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceAlarmQuery(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceAlarmLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceConfigQuery(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceConfigLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForConfigDownloadQuery(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = configDownloadLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForDeviceControl(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = deviceControlLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForPresetQuery(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = presetQueryLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    public static boolean waitForMobilePositionQuery(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = mobilePositionLatch.get();
        return latch != null && latch.await(timeout, unit);
    }

    // ==================== 状态检查方法 ====================

    public static boolean hasReceivedDeviceRecordQuery() {
        return receivedDeviceRecordQuery.get();
    }

    public static boolean hasReceivedDeviceStatusQuery() {
        return receivedDeviceStatusQuery.get();
    }

    public static boolean hasReceivedDeviceInfoQuery() {
        return receivedDeviceInfoQuery.get();
    }

    public static boolean hasReceivedDeviceItemQuery() {
        return receivedDeviceItemQuery.get();
    }

    public static boolean hasReceivedBroadcastNotify() {
        return receivedBroadcastNotify.get();
    }

    public static boolean hasReceivedDeviceAlarmQuery() {
        return receivedDeviceAlarmQuery.get();
    }

    public static boolean hasReceivedDeviceConfigQuery() {
        return receivedDeviceConfigQuery.get();
    }

    public static boolean hasReceivedConfigDownloadQuery() {
        return receivedConfigDownloadQuery.get();
    }

    public static boolean hasReceivedDeviceControl() {
        return receivedDeviceControl.get();
    }

    public static boolean hasReceivedPresetQuery() {
        return receivedPresetQuery.get();
    }

    public static boolean hasReceivedMobilePositionQuery() {
        return receivedMobilePositionQuery.get();
    }

    // ==================== 内容获取方法 ====================

    public static DeviceRecordQuery getReceivedDeviceRecordQuery() {
        return deviceRecordQuery.get();
    }

    public static String getReceivedDeviceStatusUserId() {
        return deviceStatusUserId.get();
    }

    public static String getReceivedDeviceInfoUserId() {
        return deviceInfoUserId.get();
    }

    public static String getReceivedDeviceItemUserId() {
        return deviceItemUserId.get();
    }

    public static DeviceBroadcastNotify getReceivedBroadcastNotify() {
        return broadcastNotify.get();
    }

    public static DeviceAlarmQuery getReceivedDeviceAlarmQuery() {
        return deviceAlarmQuery.get();
    }

    public static DeviceConfigDownload getReceivedDeviceConfigDownload() {
        return deviceConfigDownload.get();
    }

    public static String getReceivedConfigDownloadUserId() {
        return configDownloadUserId.get();
    }

    public static Object getReceivedDeviceControlBase() {
        return deviceControlBase.get();
    }

    public static PresetQuery getReceivedPresetQuery() {
        return presetQuery.get();
    }

    public static MobilePositionQuery getReceivedMobilePositionQuery() {
        return mobilePositionQuery.get();
    }

    // ==================== 接口实现方法 ====================

    @Override
    public DeviceRecord getDeviceRecord(DeviceRecordQuery deviceRecordQuery) {
        log.info("测试接收到设备录像记录查询 - deviceId: {}",
            deviceRecordQuery != null ? deviceRecordQuery.getDeviceId() : "null");

        if (deviceRecordQuery != null) {
            this.deviceRecordQuery.set(deviceRecordQuery);
            receivedDeviceRecordQuery.set(true);

            CountDownLatch latch = deviceRecordLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的设备录像记录
        return createTestDeviceRecord();
    }

    @Override
    public DeviceStatus getDeviceStatus(String userId) {
        log.info("测试接收到设备状态查询 - userId: {}", userId);

        if (userId != null) {
            deviceStatusUserId.set(userId);
            receivedDeviceStatusQuery.set(true);

            CountDownLatch latch = deviceStatusLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的设备状态
        return createTestDeviceStatus();
    }

    @Override
    public DeviceInfo getDeviceInfo(String userId) {
        log.info("测试接收到设备信息查询 - userId: {}", userId);

        if (userId != null) {
            deviceInfoUserId.set(userId);
            receivedDeviceInfoQuery.set(true);

            CountDownLatch latch = deviceInfoLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的设备信息
        return createTestDeviceInfo();
    }

    @Override
    public DeviceResponse getDeviceItem(String userId) {
        log.info("测试接收到设备项目信息查询 - userId: {}", userId);

        if (userId != null) {
            deviceItemUserId.set(userId);
            receivedDeviceItemQuery.set(true);

            CountDownLatch latch = deviceItemLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的设备响应
        return createTestDeviceResponse();
    }

    @Override
    public void broadcastNotify(DeviceBroadcastNotify deviceBroadcastNotify) {
        log.info("测试接收到广播通知 - cmdType: {}",
            deviceBroadcastNotify != null ? deviceBroadcastNotify.getCmdType() : "null");

        if (deviceBroadcastNotify != null) {
            broadcastNotify.set(deviceBroadcastNotify);
            receivedBroadcastNotify.set(true);

            CountDownLatch latch = broadcastLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public DeviceAlarmNotify getDeviceAlarmNotify(DeviceAlarmQuery deviceAlarmQuery) {
        log.info("测试接收到设备告警通知查询 - deviceId: {}",
            deviceAlarmQuery != null ? deviceAlarmQuery.getDeviceId() : "null");

        if (deviceAlarmQuery != null) {
            this.deviceAlarmQuery.set(deviceAlarmQuery);
            receivedDeviceAlarmQuery.set(true);

            CountDownLatch latch = deviceAlarmLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的告警通知
        return createTestDeviceAlarmNotify();
    }

    @Override
    public DeviceConfigResponse getDeviceConfigResponse(DeviceConfigDownload deviceConfigDownload) {
        log.info("测试接收到设备配置响应查询 - deviceId: {}, configType: {}",
            deviceConfigDownload != null ? deviceConfigDownload.getDeviceId() : "null",
            deviceConfigDownload != null ? deviceConfigDownload.getConfigType() : "null");

        if (deviceConfigDownload != null) {
            this.deviceConfigDownload.set(deviceConfigDownload);
            receivedDeviceConfigQuery.set(true);

            CountDownLatch latch = deviceConfigLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的配置响应
        return createTestDeviceConfigResponse();
    }

    @Override
    public ConfigDownloadResponse getConfigDownloadResponse(String userId, String configType) {
        log.info("测试接收到配置下载响应查询 - userId: {}, configType: {}", userId, configType);

        if (userId != null) {
            configDownloadUserId.set(userId);
            receivedConfigDownloadQuery.set(true);

            CountDownLatch latch = configDownloadLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的配置下载响应
        return createTestConfigDownloadResponse();
    }

    @Override
    public <T> void deviceControl(T deviceControlBase) {
        log.info("测试接收到设备控制指令 - controlType: {}",
            deviceControlBase != null ? deviceControlBase.getClass().getSimpleName() : "null");

        if (deviceControlBase != null) {
            this.deviceControlBase.set(deviceControlBase);
            receivedDeviceControl.set(true);

            CountDownLatch latch = deviceControlLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public PresetQueryResponse getDevicePresetQueryResponse(PresetQuery presetQuery) {
        log.info("测试接收到设备预置位查询响应 - deviceId: {}",
            presetQuery != null ? presetQuery.getDeviceId() : "null");

        if (presetQuery != null) {
            this.presetQuery.set(presetQuery);
            receivedPresetQuery.set(true);

            CountDownLatch latch = presetQueryLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的预置位查询响应
        return createTestPresetQueryResponse();
    }

    @Override
    public PresetQueryResponse getPresetQueryResponse(String userId) {
        log.info("测试接收到预置位查询响应 - userId: {}", userId);

        if (userId != null) {
            receivedPresetQuery.set(true);

            CountDownLatch latch = presetQueryLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的预置位查询响应
        return createTestPresetQueryResponse();
    }

    @Override
    public MobilePositionNotify getMobilePositionNotify(MobilePositionQuery mobilePositionQuery) {
        log.info("测试接收到移动位置通知查询 - deviceId: {}",
            mobilePositionQuery != null ? mobilePositionQuery.getDeviceId() : "null");

        if (mobilePositionQuery != null) {
            this.mobilePositionQuery.set(mobilePositionQuery);
            receivedMobilePositionQuery.set(true);

            CountDownLatch latch = mobilePositionLatch.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        // 返回测试用的移动位置通知
        return createTestMobilePositionNotify();
    }

    // ==================== 测试数据创建方法 ====================

    private DeviceRecord createTestDeviceRecord() {
        DeviceRecord record = new DeviceRecord();
        // record.setName("测试录像记录"); // Method not available
        record.setDeviceId("34020000001320000001");
        return record;
    }

    private DeviceStatus createTestDeviceStatus() {
        DeviceStatus status = new DeviceStatus();
        status.setStatus("ON");
        status.setResult("OK");
        status.setDeviceId("34020000001320000001");
        return status;
    }

    private DeviceInfo createTestDeviceInfo() {
        DeviceInfo info = new DeviceInfo();
        info.setDeviceId("34020000001320000001");
        info.setDeviceName("测试GB28181设备");
        info.setManufacturer("Luna测试");
        info.setModel("Test-Model-001");
        info.setFirmware("v1.0.0");
        return info;
    }

    private DeviceResponse createTestDeviceResponse() {
        DeviceResponse response = new DeviceResponse();
        response.setDeviceId("34020000001320000001");
        // response.setName("测试设备响应"); // Method not available
        return response;
    }

    private DeviceAlarmNotify createTestDeviceAlarmNotify() {
        DeviceAlarmNotify notify = new DeviceAlarmNotify();
        notify.setDeviceId("34020000001320000001");
        notify.setAlarmPriority("3");
        notify.setAlarmMethod("1");
        return notify;
    }

    private DeviceConfigResponse createTestDeviceConfigResponse() {
        DeviceConfigResponse response = new DeviceConfigResponse();
        response.setDeviceId("34020000001320000001");
        response.setResult("OK");
        return response;
    }

    private ConfigDownloadResponse createTestConfigDownloadResponse() {
        ConfigDownloadResponse response = new ConfigDownloadResponse();
        response.setResult("OK");
        return response;
    }

    private PresetQueryResponse createTestPresetQueryResponse() {
        PresetQueryResponse response = new PresetQueryResponse();
        response.setDeviceId("34020000001320000001");
        return response;
    }

    private MobilePositionNotify createTestMobilePositionNotify() {
        MobilePositionNotify notify = new MobilePositionNotify();
        notify.setDeviceId("34020000001320000001");
        notify.setLongitude(Double.parseDouble("116.407395"));
        notify.setLatitude(Double.parseDouble("39.904211"));
        return notify;
    }
}