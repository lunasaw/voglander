package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier.VoglanderBusinessNotifier;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;
import io.github.lunasaw.voglander.repository.mapper.MediaSessionMapper;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-PB-01/02：历史录像回放端到端测试。
 * <p>
 * 复用 Session.InviteOk/Bye 事件，通过 sessionType=PLAYBACK 区分。
 * 不使用 shardSpy doAnswer（避免 SIP retransmit 在 Mockito OngoingStubbing 建立期间并发
 * 触发 dispatch，导致 UnfinishedStubbingException），改为直接 await DB 状态验证。
 */
@Slf4j
class PlaybackE2eTest extends BaseE2eTest {

    private static final String DEVICE_ID  = "34020000001320000061";
    private static final String CHANNEL_ID = "34020000131600000002";

    @Autowired private VoglanderBusinessNotifier notifier;
    @Autowired private MediaSessionMapper        sessionMapper;

    @AfterEach
    void cleanup() {
        sessionMapper.delete(Wrappers.<MediaSessionDO>lambdaQuery().eq(MediaSessionDO::getDeviceId, DEVICE_ID));
    }

    @Test
    @DisplayName("TC-PB-01 PlaybackInviteOk → MediaSession ACTIVE 创建成功")
    void playbackInviteOk_persistsSession() {
        String callId = UniqueKeyFactory.callId();

        notifier.notify(new GatewayEvent("gb28181.Session.InviteOk", DEVICE_ID, callId,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID, "ssrc", "00000002",
                   "sessionType", "PLAYBACK",
                   "startTime", "2024-01-01T00:00:00", "endTime", "2024-01-01T01:00:00",
                   "mediaServerId", "node-local"),
            "node-local"));

        await().atMost(8, SECONDS).untilAsserted(() -> {
            MediaSessionDO s = sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId));
            assertThat(s).isNotNull();
            assertThat(s.getStatus()).isEqualTo(1);         // ACTIVE
        });
        log.info("✅ TC-PB-01 通过");
    }

    @Test
    @DisplayName("TC-PB-02 PlaybackBye → MediaSession CLOSED")
    void playbackBye_closesSession() {
        String callId = UniqueKeyFactory.callId();

        notifier.notify(new GatewayEvent("gb28181.Session.InviteOk", DEVICE_ID, callId,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID, "ssrc", "00000002",
                   "sessionType", "PLAYBACK", "mediaServerId", "node-local"),
            "node-local"));

        await().atMost(8, SECONDS).until(() ->
            sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId)) != null);

        notifier.notify(new GatewayEvent("gb28181.Session.Bye", DEVICE_ID, null,
            System.currentTimeMillis(), Map.of("channelId", CHANNEL_ID), "node-local"));

        // dispatch 后的状态落库在分片线程异步完成
        await().atMost(8, SECONDS).untilAsserted(() -> {
            MediaSessionDO s = sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId));
            assertThat(s.getStatus()).isEqualTo(0);         // CLOSED
        });
        log.info("✅ TC-PB-02 通过");
    }
}
