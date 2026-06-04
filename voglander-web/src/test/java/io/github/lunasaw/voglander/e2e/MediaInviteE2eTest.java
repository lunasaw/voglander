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
 * TC-RTP-01/02/03：实时视音频点播端到端测试。
 * <p>
 * 注意：Session.InviteOk 使用 correlationId 作为 callId；Session.Bye 使用 deviceId。
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.voglander.web.ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class MediaInviteE2eTest {

    private static final String DEVICE_ID  = "34020000001320000051";
    private static final String CHANNEL_ID = "34020000131600000001";

    @Autowired private VoglanderBusinessNotifier notifier;
    @Autowired private MediaSessionMapper        sessionMapper;
    @MockitoSpyBean private ShardDispatcher      shardSpy;

    @AfterEach
    void cleanup() {
        sessionMapper.delete(Wrappers.<MediaSessionDO>lambdaQuery().eq(MediaSessionDO::getDeviceId, DEVICE_ID));
    }

    @Test
    @DisplayName("TC-RTP-01 InviteOk → MediaSession ACTIVE/PLAY")
    void inviteOk_persistsPlayingSession() throws InterruptedException {
        String callId = UniqueKeyFactory.callId();
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return inv.callRealMethod(); })
            .when(shardSpy).dispatch(any(DeviceEvent.class));

        // correlationId = callId
        notifier.notify(new GatewayEvent("gb28181.Session.InviteOk", DEVICE_ID, callId,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID, "ssrc", "00000001",
                   "streamUrl", "rtsp://127.0.0.1/live/test", "mediaServerId", "node-local"),
            "node-local"));

        assertThat(latch.await(5, SECONDS)).isTrue();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            MediaSessionDO s = sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId));
            assertThat(s).isNotNull();
            assertThat(s.getStatus()).isEqualTo(1);         // ACTIVE
        });
        log.info("✅ TC-RTP-01 通过");
    }

    @Test
    @DisplayName("TC-RTP-02 Bye → MediaSession CLOSED")
    void bye_closesSession() throws InterruptedException {
        String callId = UniqueKeyFactory.callId();

        // 先建立会话
        CountDownLatch inviteLatch = new CountDownLatch(1);
        doAnswer(inv -> { inviteLatch.countDown(); return inv.callRealMethod(); })
            .when(shardSpy).dispatch(any(DeviceEvent.class));

        notifier.notify(new GatewayEvent("gb28181.Session.InviteOk", DEVICE_ID, callId,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID, "ssrc", "00000001",
                   "streamUrl", "rtsp://127.0.0.1/live/test", "mediaServerId", "node-local"),
            "node-local"));

        assertThat(inviteLatch.await(5, SECONDS)).isTrue();
        await().atMost(5, SECONDS).until(() ->
            sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId)) != null);

        // 发送 BYE（用 deviceId 路由）
        CountDownLatch byeLatch = new CountDownLatch(1);
        doAnswer(inv -> { byeLatch.countDown(); return inv.callRealMethod(); })
            .when(shardSpy).dispatch(any(DeviceEvent.class));

        notifier.notify(new GatewayEvent("gb28181.Session.Bye", DEVICE_ID, null,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID), "node-local"));

        assertThat(byeLatch.await(5, SECONDS)).isTrue();
        await().atMost(3, SECONDS).untilAsserted(() -> {
            MediaSessionDO s = sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId));
            assertThat(s.getStatus()).isEqualTo(0);         // CLOSED
        });
        log.info("✅ TC-RTP-02 通过");
    }

    @Test
    @DisplayName("TC-RTP-03 InviteFailure → MediaSession FAILED")
    void inviteFailure_persistsFailedSession() throws InterruptedException {
        String callId = UniqueKeyFactory.callId();
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return inv.callRealMethod(); })
            .when(shardSpy).dispatch(any(DeviceEvent.class));

        notifier.notify(new GatewayEvent("gb28181.Session.InviteFailure", DEVICE_ID, callId,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID, "statusCode", 503), "node-local"));

        assertThat(latch.await(5, SECONDS)).isTrue();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            MediaSessionDO s = sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId));
            assertThat(s).isNotNull();
            assertThat(s.getStatus()).isEqualTo(3);         // FAILED
        });
        log.info("✅ TC-RTP-03 通过");
    }
}
