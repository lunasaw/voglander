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
 * TC-VOICE-01/02：语音广播端到端测试。
 * 不使用 shardSpy doAnswer（Mockito OngoingStubbing 在并发 SIP retransmit 下不安全），
 * 改为直接 await DB 状态验证。
 */
@Slf4j
class VoiceBroadcastE2eTest extends BaseE2eTest {

    private static final String DEVICE_ID  = "34020000001320000121";
    private static final String CHANNEL_ID = "34020000131600000005";

    @Autowired private VoglanderBusinessNotifier notifier;
    @Autowired private MediaSessionMapper        sessionMapper;

    @AfterEach
    void cleanup() {
        sessionMapper.delete(Wrappers.<MediaSessionDO>lambdaQuery().eq(MediaSessionDO::getDeviceId, DEVICE_ID));
    }

    @Test
    @DisplayName("TC-VOICE-01 语音广播建立 → MediaSession ACTIVE 创建成功")
    void broadcastInviteOk_persistsSession() {
        String callId = UniqueKeyFactory.callId();

        notifier.notify(new GatewayEvent("gb28181.Session.InviteOk", DEVICE_ID, callId,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID, "ssrc", "00000005",
                   "sessionType", "TALK",
                   "audioCodec", "PCMA/8000", "direction", "sendonly",
                   "mediaServerId", "node-local"),
            "node-local"));

        await().atMost(8, SECONDS).untilAsserted(() -> {
            MediaSessionDO s = sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId));
            assertThat(s).isNotNull();
            assertThat(s.getStatus()).isEqualTo(1);         // ACTIVE
        });
        log.info("✅ TC-VOICE-01 通过");
    }

    @Test
    @DisplayName("TC-VOICE-02 语音广播结束 → MediaSession CLOSED")
    void broadcastBye_closesSession() {
        String callId = UniqueKeyFactory.callId();

        notifier.notify(new GatewayEvent("gb28181.Session.InviteOk", DEVICE_ID, callId,
            System.currentTimeMillis(),
            Map.of("channelId", CHANNEL_ID, "ssrc", "00000005",
                   "sessionType", "TALK", "mediaServerId", "node-local"),
            "node-local"));

        await().atMost(8, SECONDS).until(() ->
            sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId)) != null);

        notifier.notify(new GatewayEvent("gb28181.Session.Bye", DEVICE_ID, null,
            System.currentTimeMillis(), Map.of("channelId", CHANNEL_ID), "node-local"));

        await().atMost(8, SECONDS).untilAsserted(() -> {
            MediaSessionDO s = sessionMapper.selectOne(Wrappers.<MediaSessionDO>lambdaQuery()
                .eq(MediaSessionDO::getCallId, callId));
            assertThat(s.getStatus()).isEqualTo(0);         // CLOSED
        });
        log.info("✅ TC-VOICE-02 通过");
    }
}
