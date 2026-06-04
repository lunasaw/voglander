package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier.VoglanderBusinessNotifier;
import io.github.lunasaw.voglander.manager.event.ShardDispatcher;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;
import io.github.lunasaw.voglander.repository.mapper.MediaSessionMapper;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-VOICE-01/02：语音广播端到端测试。
 * <p>
 * 语音广播通过 Session.InviteOk + sessionType=TALK 触发；
 * Session.Bye + deviceId 关闭。
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class VoiceBroadcastE2eTest {

    private static final String DEVICE_ID  = "34020000001320000121";
    private static final String CHANNEL_ID = "34020000131600000005";

    @Autowired private VoglanderBusinessNotifier notifier;
    @Autowired private MediaSessionMapper        sessionMapper;
    @MockitoSpyBean private ShardDispatcher      shardSpy;

    @AfterEach
    void cleanup() {
        sessionMapper.delete(Wrappers.<MediaSessionDO>lambdaQuery().eq(MediaSessionDO::getDeviceId, DEVICE_ID));
    }

    @Test
    @DisplayName("TC-VOICE-01 语音广播建立 → MediaSession ACTIVE 创建成功")
    void broadcastInviteOk_persistsSession() throws InterruptedException {
        String callId = UniqueKeyFactory.callId();
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return inv.callRealMethod(); })
            .when(shardSpy).dispatch(any(DeviceEvent.class));

        notifier.notify(new GatewayEvent("gb28181.Session.InviteOk", DEVICE_ID, callId,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID, "ssrc", "00000005",
                   "sessionType", "TALK",
                   "audioCodec", "PCMA/8000", "direction", "sendonly",
                   "mediaServerId", "node-local"),
            "node-local"));

        assertThat(latch.await(5, SECONDS)).isTrue();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            MediaSessionDO s = sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId));
            assertThat(s).isNotNull();
            assertThat(s.getStatus()).isEqualTo(1);         // ACTIVE
        });
        log.info("✅ TC-VOICE-01 通过");
    }

    @Test
    @DisplayName("TC-VOICE-02 语音广播结束 → MediaSession CLOSED")
    void broadcastBye_closesSession() throws InterruptedException {
        String callId = UniqueKeyFactory.callId();

        CountDownLatch inviteLatch = new CountDownLatch(1);
        doAnswer(inv -> { inviteLatch.countDown(); return inv.callRealMethod(); })
            .when(shardSpy).dispatch(any(DeviceEvent.class));

        notifier.notify(new GatewayEvent("gb28181.Session.InviteOk", DEVICE_ID, callId,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID, "ssrc", "00000005",
                   "sessionType", "TALK", "mediaServerId", "node-local"),
            "node-local"));

        assertThat(inviteLatch.await(5, SECONDS)).isTrue();
        await().atMost(5, SECONDS).until(() ->
            sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId)) != null);

        CountDownLatch byeLatch = new CountDownLatch(1);
        doAnswer(inv -> { byeLatch.countDown(); return inv.callRealMethod(); })
            .when(shardSpy).dispatch(any(DeviceEvent.class));

        notifier.notify(new GatewayEvent("gb28181.Session.Bye", DEVICE_ID, null,
            System.currentTimeMillis(), Map.of("channelId", CHANNEL_ID), "node-local"));

        assertThat(byeLatch.await(5, SECONDS)).isTrue();
        await().atMost(3, SECONDS).untilAsserted(() -> {
            MediaSessionDO s = sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId));
            assertThat(s.getStatus()).isEqualTo(0);         // CLOSED
        });
        log.info("✅ TC-VOICE-02 通过");
    }
}
