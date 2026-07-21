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
import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier.VoglanderBusinessNotifier;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;
import io.github.lunasaw.voglander.repository.mapper.MediaSessionMapper;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-VOICE-01/02：语音广播端到端测试。
 * 不使用 shardSpy doAnswer（Mockito OngoingStubbing 在并发 SIP retransmit 下不安全），
 * 改为直接 await DB 状态验证。
 * <p>
 * 关联策略对齐生产：{@code Session.InviteOk} 由 handler 以 <b>callId 单参</b>关联会话，
 * 生产链路已预写 INVITING 占位行并回填真实 callId。E2E 未走发起链路，
 * 需按同一契约先预写占位行（sessionType=TALK）再发 InviteOk。
 */
@Slf4j
class VoiceBroadcastE2eTest extends BaseE2eTest {

    private static final String DEVICE_ID  = "34020000001320000121";
    private static final String CHANNEL_ID = "34020000131600000005";

    @Autowired private VoglanderBusinessNotifier notifier;
    @Autowired private MediaSessionMapper        sessionMapper;
    @Autowired private MediaSessionManager       mediaSessionManager;

    @AfterEach
    void cleanup() {
        sessionMapper.delete(Wrappers.<MediaSessionDO>lambdaQuery().eq(MediaSessionDO::getDeviceId, DEVICE_ID));
    }

    /**
     * 预写 INVITING 语音广播占位会话（等价发起链路预写 + 回填真实 callId），使 InviteOk 能按 callId 命中。
     */
    private void prewriteInvitingPlaceholder(String callId) {
        MediaSessionDTO placeholder = new MediaSessionDTO();
        placeholder.setCallId(callId);
        placeholder.setDeviceId(DEVICE_ID);
        placeholder.setChannelId(CHANNEL_ID);
        placeholder.setNodeServerId("node-local");
        placeholder.setStatus(MediaSessionConstant.Status.INVITING);
        placeholder.setSessionType(MediaSessionConstant.Type.TALK);
        mediaSessionManager.add(placeholder);
    }

    @Test
    @DisplayName("TC-VOICE-01 语音广播建立 → MediaSession ACTIVE 创建成功")
    void broadcastInviteOk_persistsSession() {
        String callId = UniqueKeyFactory.callId();
        prewriteInvitingPlaceholder(callId);

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
        prewriteInvitingPlaceholder(callId);

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
