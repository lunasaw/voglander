package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-ALM-01/02：告警通知端到端测试（真实 SIP 协议栈）。
 * 链路：ClientCommandSender.sendAlarmNotify → SIP/UDP MESSAGE → Server(5060)
 *       → VoglanderBusinessNotifier → handleAlarm → ShardDispatcher.dispatch
 * <p>
 * 使用专属 CLIENT_ID（不与其他 E2E 复用），@BeforeEach 直接写 DB 而非 SIP REGISTER：
 * 避免高并发套件下 JAIN-SIP 事务冲突("Transaction exists -- cannot send response statelessly")
 * 和 SIP retransmit 在整个超时窗口内污染其他测试的设备状态。
 * Alarm NOTIFY 本身仍走真实 SIP 协议栈验证链路。
 */
@Slf4j
class AlarmE2eTest extends BaseE2eTest {

    private static final String CLIENT_ID = "34020000001320000201";
    private static final String SERVER_ID = "34020000002000000001";
    private static final String PASSWORD  = "123456";

    @Autowired private DeviceMapper deviceMapper;

    @BeforeEach
    void register() {
        // 直接写 DB，不走 SIP REGISTER：避免并发下 JAIN-SIP 事务冲突
        DeviceDO device = new DeviceDO();
        device.setDeviceId(CLIENT_ID);
        device.setStatus(1);
        device.setIp("127.0.0.1");
        device.setPort(5060);
        device.setServerIp("127.0.0.1");
        device.setType(1);
        device.setRegisterTime(LocalDateTime.now());
        device.setKeepaliveTime(LocalDateTime.now());
        deviceMapper.insert(device);
    }

    @AfterEach
    void cleanup() {
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
    }

    @Test
    @DisplayName("TC-ALM-01 真实 SIP Alarm NOTIFY → handler 链路走通无异常")
    void alarmNotify_handlerInvoked() {
        ClientCommandSender.sendAlarmNotify(from(), to(), buildAlarm(1, "2"));

        await().atMost(10, SECONDS).untilAsserted(() ->
            Mockito.verify(shardSpy, Mockito.atLeastOnce()).dispatch(
                Mockito.argThat(e -> CLIENT_ID.equals(e.deviceId()))));
        log.info("✅ TC-ALM-01 通过");
    }

    @Test
    @DisplayName("TC-ALM-02 真实 SIP Alarm NOTIFY → deviceId 正确传递")
    void alarmNotify_deviceIdPropagated() {
        ClientCommandSender.sendAlarmNotify(from(), to(), buildAlarm(2, "3"));

        await().atMost(10, SECONDS).untilAsserted(() ->
            Mockito.verify(shardSpy, Mockito.atLeastOnce()).dispatch(
                Mockito.argThat((DeviceEvent e) -> CLIENT_ID.equals(e.deviceId()))));
        log.info("✅ TC-ALM-02 通过");
    }

    private DeviceAlarmNotify buildAlarm(int type, String priority) {
        DeviceAlarmNotify notify = new DeviceAlarmNotify();
        notify.setDeviceId(CLIENT_ID);
        notify.setAlarmMethod("1");
        notify.setAlarmPriority(priority);
        notify.setAlarmTime("2024-01-01T12:00:00");
        DeviceAlarmNotify.AlarmInfo info = new DeviceAlarmNotify.AlarmInfo();
        info.setAlarmType(String.valueOf(type));
        notify.setInfo(info);
        return notify;
    }

    private FromDevice from() { return FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061); }

    private ToDevice to() {
        ToDevice t = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        t.setPassword(PASSWORD);
        return t;
    }
}
