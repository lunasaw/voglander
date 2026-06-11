package io.github.lunasaw.voglander.intergration.wrapper.zlm.impl;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.lunasaw.voglander.common.event.StreamOfflineEvent;
import io.github.lunasaw.voglander.common.event.StreamReadyEvent;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.auth.ZlmHookAuthService;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.zlm.hook.param.OnStreamChangedHookParam;

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
    void testOnStreamNoneReader_Conservative_KeepsStreamOpen() {
        io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam param =
            new io.github.lunasaw.zlm.hook.param.OnStreamNoneReaderHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");

        io.github.lunasaw.zlm.hook.param.HookResultForStreamNoneReader result =
            hookService.onStreamNoneReader(param, null);

        // 保守方案：无人观看不立即关流（close=false），真正回收交给 pending_close / GC 对账
        assertEquals(0, result.getCode());
        org.junit.jupiter.api.Assertions.assertFalse(result.isClose());
        // 不应因无人观看就发下线事件（避免与"刚断开马上重连"竞态）
        verify(eventPublisher, never()).publishEvent(any(StreamOfflineEvent.class));
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
}
