package io.github.lunasaw.voglander.intergration.wrapper.zlm.impl;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.common.event.StreamOfflineEvent;
import io.github.lunasaw.voglander.common.event.StreamReadyEvent;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.auth.ZlmHookAuthService;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.zlm.hook.param.OnRtpServerTimeoutHookParam;
import io.github.lunasaw.zlm.hook.param.OnSendRtpStoppedHookParam;
import io.github.lunasaw.zlm.hook.param.OnStreamChangedHookParam;
import io.github.lunasaw.zlm.hook.param.OnStreamNotFoundHookParam;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * VoglanderZlmHookServiceImpl 单测：流上线发布 StreamReadyEvent，流下线发布 StreamOfflineEvent。
 */
@ExtendWith(MockitoExtension.class)
class VoglanderZlmHookServiceImplTest {

    @InjectMocks
    VoglanderZlmHookServiceImpl hookService;

    @Mock
    MediaNodeManager             mediaNodeManager;
    @Mock
    StreamProxyManager           streamProxyManager;
    @Mock
    MediaSessionManager          mediaSessionManager;
    @Mock
    ZlmHookAuthService           zlmHookAuthService;
    @Mock
    ApplicationEventPublisher    eventPublisher;

    @Test
    void testOnStreamChanged_Regist_PublishesStreamReadyEvent() {
        OnStreamChangedHookParam param = new OnStreamChangedHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");
        param.setRegist(true);

        // Manager returns null (no existing proxy), that's fine — event still fires
        when(streamProxyManager.get(any())).thenReturn(null);

        hookService.onStreamChanged(param, null);

        verify(eventPublisher, times(1)).publishEvent(any(StreamReadyEvent.class));
        // 上线分支不应发下线事件
        verify(eventPublisher, never()).publishEvent(any(StreamOfflineEvent.class));
    }

    @Test
    void testOnStreamChanged_Unregist_PublishesStreamOfflineEvent() {
        OnStreamChangedHookParam param = new OnStreamChangedHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");
        param.setRegist(false);
        param.setMediaServerId("zlm-1");

        when(streamProxyManager.get(any())).thenReturn(null);

        hookService.onStreamChanged(param, null);

        // 下线无条件发 StreamOfflineEvent，且 streamId/serverId 来自 param
        ArgumentCaptor<StreamOfflineEvent> captor = ArgumentCaptor.forClass(StreamOfflineEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        StreamOfflineEvent event = captor.getValue();
        assertEquals("gb_live_dev1_ch1", event.getStreamId());
        assertEquals("zlm-1", event.getServerId());
        // 下线分支不应发上线事件
        verify(eventPublisher, never()).publishEvent(any(StreamReadyEvent.class));
    }

    @Test
    void testOnStreamNoneReader_ReclaimEnabled_PastGrace_PublishesOfflineEventAndCloses() {
        // 默认开关开启 + 会话 createTime 早于宽限期 → 主动 BYE 回收
        org.springframework.test.util.ReflectionTestUtils.setField(hookService, "noneReaderReclaimEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(hookService, "noneReaderGraceSec", 30);

        MediaSessionDTO old = new MediaSessionDTO();
        old.setStreamId("gb_live_dev1_ch1");
        old.setCreateTime(java.time.LocalDateTime.now().minusSeconds(120));
        when(mediaSessionManager.getByStreamId("gb_live_dev1_ch1")).thenReturn(old);

        io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam param =
            new io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");
        param.setMediaServerId("zlm-1");

        io.github.lunasaw.zlm.hook.param.HookResultForStreamNoneReader result =
            hookService.onStreamNoneReader(param, null);

        assertEquals(0, result.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(result.isClose(), "到点应让 ZLM 关流");
        // 发 StreamOfflineEvent(reason=none_reader)，由 service 委托 closeStream 发标准 BYE
        ArgumentCaptor<StreamOfflineEvent> captor = ArgumentCaptor.forClass(StreamOfflineEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertEquals("gb_live_dev1_ch1", captor.getValue().getStreamId());
        assertEquals("none_reader", captor.getValue().getReason());
    }

    @Test
    void testOnStreamNoneReader_WithinGrace_KeepsStreamOpen() {
        // 会话 createTime 在宽限期内 → 不回收，防启动竞态
        org.springframework.test.util.ReflectionTestUtils.setField(hookService, "noneReaderReclaimEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(hookService, "noneReaderGraceSec", 30);

        MediaSessionDTO fresh = new MediaSessionDTO();
        fresh.setStreamId("gb_live_dev1_ch1");
        fresh.setCreateTime(java.time.LocalDateTime.now().minusSeconds(5));
        when(mediaSessionManager.getByStreamId("gb_live_dev1_ch1")).thenReturn(fresh);

        io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam param =
            new io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");

        io.github.lunasaw.zlm.hook.param.HookResultForStreamNoneReader result =
            hookService.onStreamNoneReader(param, null);

        assertEquals(0, result.getCode());
        org.junit.jupiter.api.Assertions.assertFalse(result.isClose(), "宽限期内不关流");
        verify(eventPublisher, never()).publishEvent(any(StreamOfflineEvent.class));
    }

    @Test
    void testOnStreamNoneReader_ReclaimDisabled_KeepsStreamOpen() {
        // 开关关闭 → 退回旧保守行为
        org.springframework.test.util.ReflectionTestUtils.setField(hookService, "noneReaderReclaimEnabled", false);

        io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam param =
            new io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");

        io.github.lunasaw.zlm.hook.param.HookResultForStreamNoneReader result =
            hookService.onStreamNoneReader(param, null);

        assertEquals(0, result.getCode());
        org.junit.jupiter.api.Assertions.assertFalse(result.isClose());
        verify(eventPublisher, never()).publishEvent(any(StreamOfflineEvent.class));
    }

    @Test
    void testOnStreamNoneReader_OrphanStream_NoSession_Reclaims() {
        // 无对应会话（孤儿流）→ 不豁免，照常回收
        org.springframework.test.util.ReflectionTestUtils.setField(hookService, "noneReaderReclaimEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(hookService, "noneReaderGraceSec", 30);
        when(mediaSessionManager.getByStreamId("gb_live_dev1_ch1")).thenReturn(null);

        io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam param =
            new io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");
        param.setMediaServerId("zlm-1");

        io.github.lunasaw.zlm.hook.param.HookResultForStreamNoneReader result =
            hookService.onStreamNoneReader(param, null);

        org.junit.jupiter.api.Assertions.assertTrue(result.isClose());
        verify(eventPublisher, times(1)).publishEvent(any(StreamOfflineEvent.class));
    }

    @Test
    void testOnPublish_EnablesAllPlaybackProtocols() {
        io.github.lunasaw.zlm.hook.param.OnPublishHookParam param =
            new io.github.lunasaw.zlm.hook.param.OnPublishHookParam();
        param.setIp("127.0.0.1");
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");

        when(zlmHookAuthService.validatePublish("127.0.0.1", "rtp", "gb_live_dev1_ch1")).thenReturn(true);

        io.github.lunasaw.zlm.hook.param.HookResultForOnPublish result =
            hookService.onPublish(param, null);

        // 启用的输出协议必须覆盖 getPlaybackUrls 返回给播放器的全部协议(全开)，
        // 否则播放器选未启用协议会触发 onStreamNotFound + onStreamNoneReader。
        // FLV 与 RTMP 共用 muxer，由 enableRtmp 控制——这是 FLV 能否播放的关键。
        assertEquals(0, result.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(result.isEnableHls(), "应启用 HLS");
        org.junit.jupiter.api.Assertions.assertTrue(result.isEnableMp4(), "应启用 MP4");
        org.junit.jupiter.api.Assertions.assertTrue(result.isEnableRtmp(), "应启用 RTMP/FLV");
        org.junit.jupiter.api.Assertions.assertTrue(result.isEnableTs(), "应启用 TS");
        org.junit.jupiter.api.Assertions.assertTrue(result.isEnableRtsp(), "应启用 RTSP");
        org.junit.jupiter.api.Assertions.assertTrue(result.isEnableFmp4(), "应启用 fMP4");
    }

    // ==================== onStreamNotFound：幽灵会话兜底（不自动 INVITE） ====================

    @Test
    void testOnStreamNotFound_ActiveSession_PublishesStreamOfflineEvent() {
        // DB 说 ACTIVE 但 ZLM 查无该流 = 幽灵会话，应发下线事件兜底回收
        OnStreamNotFoundHookParam param = new OnStreamNotFoundHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");
        param.setMediaServerId("zlm-1");

        MediaSessionDTO session = new MediaSessionDTO();
        session.setId(100L);
        session.setStreamId("gb_live_dev1_ch1");
        session.setStatus(MediaSessionConstant.Status.ACTIVE);
        when(mediaSessionManager.getByStreamId("gb_live_dev1_ch1")).thenReturn(session);

        hookService.onStreamNotFound(param, null);

        ArgumentCaptor<StreamOfflineEvent> captor = ArgumentCaptor.forClass(StreamOfflineEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        StreamOfflineEvent event = captor.getValue();
        assertEquals("gb_live_dev1_ch1", event.getStreamId());
        assertEquals("zlm-1", event.getServerId());
    }

    @Test
    void testOnStreamNotFound_NoOrInactiveSession_NoEvent() {
        // 会话为 null（玩家请求了不存在的流，属预期）→ 不发事件
        OnStreamNotFoundHookParam noSession = new OnStreamNotFoundHookParam();
        noSession.setApp("rtp");
        noSession.setStream("gb_live_dev1_ch1");
        when(mediaSessionManager.getByStreamId("gb_live_dev1_ch1")).thenReturn(null);

        hookService.onStreamNotFound(noSession, null);
        verify(eventPublisher, never()).publishEvent(any(StreamOfflineEvent.class));

        // 会话存在但非 ACTIVE（如 INVITING/FAILED）→ 不发事件，交由 GC 兜底
        OnStreamNotFoundHookParam invitingParam = new OnStreamNotFoundHookParam();
        invitingParam.setApp("rtp");
        invitingParam.setStream("gb_live_dev2_ch1");
        MediaSessionDTO inviting = new MediaSessionDTO();
        inviting.setId(101L);
        inviting.setStreamId("gb_live_dev2_ch1");
        inviting.setStatus(MediaSessionConstant.Status.INVITING);
        when(mediaSessionManager.getByStreamId("gb_live_dev2_ch1")).thenReturn(inviting);

        hookService.onStreamNotFound(invitingParam, null);
        verify(eventPublisher, never()).publishEvent(any(StreamOfflineEvent.class));
    }

    @Test
    void testOnStreamNotFound_NonGb28181Stream_SkipsLookupAndEvent() {
        // 非 GB28181 流名（无 gb_live_/gb_back_ 前缀）→ 不查会话、不发事件
        OnStreamNotFoundHookParam param = new OnStreamNotFoundHookParam();
        param.setApp("live");
        param.setStream("some_external_stream");

        hookService.onStreamNotFound(param, null);

        verify(mediaSessionManager, never()).getByStreamId(any());
        verify(eventPublisher, never()).publishEvent(any(StreamOfflineEvent.class));
    }

    // ==================== onRtpServerTimeout：RTP 收流超时，会话已死，触发回收 ====================

    @Test
    void testOnRtpServerTimeout_WithStreamId_PublishesStreamOfflineEvent() {
        OnRtpServerTimeoutHookParam param = new OnRtpServerTimeoutHookParam();
        param.setStreamId("gb_live_dev1_ch1");
        param.setMediaServerId("zlm-1");

        hookService.onRtpServerTimeout(param, null);

        ArgumentCaptor<StreamOfflineEvent> captor = ArgumentCaptor.forClass(StreamOfflineEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        StreamOfflineEvent event = captor.getValue();
        assertEquals("gb_live_dev1_ch1", event.getStreamId());
        assertEquals("zlm-1", event.getServerId());
    }

    @Test
    void testOnRtpServerTimeout_BlankStreamId_NoEvent() {
        OnRtpServerTimeoutHookParam param = new OnRtpServerTimeoutHookParam();
        param.setStreamId("");
        param.setMediaServerId("zlm-1");

        hookService.onRtpServerTimeout(param, null);

        verify(eventPublisher, never()).publishEvent(any(StreamOfflineEvent.class));
    }

    // ==================== onSendRtpStopped：级联推流停止，不误拆共享源流 ====================

    @Test
    void testOnSendRtpStopped_NeverPublishesStreamOfflineEvent() {
        // 停止 RTP 发送 ≠ 本地收流源消失（可能仍有其他观看者），仅日志，绝不发下线事件
        OnSendRtpStoppedHookParam param = new OnSendRtpStoppedHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");
        param.setMediaServerId("zlm-1");

        hookService.onSendRtpStopped(param, null);

        verify(eventPublisher, never()).publishEvent(any(StreamOfflineEvent.class));
    }
}
