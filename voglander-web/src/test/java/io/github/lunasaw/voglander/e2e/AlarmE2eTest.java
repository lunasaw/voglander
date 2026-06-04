package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.manager.event.ShardDispatcher;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-ALM-01/02：告警通知端到端测试（真实 SIP 协议栈）。
 * 链路：ClientCommandSender.sendAlarmNotify → SIP/UDP MESSAGE → Server(5060)
 *       → VoglanderBusinessNotifier → handleAlarm
 * 注：tb_alarm 落库尚未实现，当前验证事件链路走通。
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class AlarmE2eTest {

    private static final String CLIENT_ID = "34020000001320000001";
    private static final String SERVER_ID = "34020000002000000001";
    private static final String PASSWORD  = "123456";

    @Autowired private DeviceMapper deviceMapper;
    @MockitoSpyBean private ShardDispatcher shardSpy;

    @BeforeEach
    void register() {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);
        await().atMost(5, SECONDS).until(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            return d != null && d.getStatus() == 1;
        });
    }

    @AfterEach
    void cleanup() {
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
    }

    @Test
    @DisplayName("TC-ALM-01 真实 SIP Alarm NOTIFY → handler 链路走通无异常")
    void alarmNotify_handlerInvoked() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return inv.callRealMethod(); })
            .when(shardSpy).dispatch(any(DeviceEvent.class));

        ClientCommandSender.sendAlarmNotify(from(), to(), buildAlarm(1, "2"));

        assertThat(latch.await(5, SECONDS)).as("ShardDispatcher 未收到 Alarm 事件").isTrue();
        // TODO：待 tb_alarm 落库实现后，追加 DB 断言
        log.info("✅ TC-ALM-01 通过");
    }

    @Test
    @DisplayName("TC-ALM-02 真实 SIP Alarm NOTIFY → deviceId 正确传递")
    void alarmNotify_deviceIdPropagated() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        DeviceEvent[] captured = new DeviceEvent[1];
        doAnswer(inv -> {
            captured[0] = inv.getArgument(0);
            latch.countDown();
            return inv.callRealMethod();
        }).when(shardSpy).dispatch(any(DeviceEvent.class));

        ClientCommandSender.sendAlarmNotify(from(), to(), buildAlarm(2, "3"));

        assertThat(latch.await(5, SECONDS)).isTrue();
        assertThat(captured[0].deviceId()).isEqualTo(CLIENT_ID);
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
