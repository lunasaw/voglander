package io.github.lunasaw.voglander.service.live;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.common.event.StreamOfflineEvent;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.service.sse.SseEvent;
import io.github.lunasaw.voglander.service.sse.SseEventBus;

/**
 * S1 红线：流下线事件监听器 —— 清直播缓存 + 标 DB 会话 CLOSED + 推 SSE live.closed/stream_offline。
 * 与上线 {@code StreamReadyEvent} 对称，覆盖 rtp 直播流（不依赖 StreamProxy 表）。
 *
 * @author luna
 */
@DisplayName("S1 — LiveStreamEventListener.onStreamOffline 下线清缓存")
@ExtendWith(MockitoExtension.class)
class LiveStreamEventListenerOfflineTest {

    private static final String STREAM_ID = "gb_live_dev1_ch1";

    @Mock
    private LiveStreamRegistry  liveStreamRegistry;
    @Mock
    private SseEventBus         sseEventBus;
    @Mock
    private MediaSessionManager mediaSessionManager;

    @InjectMocks
    private LiveStreamEventListener listener;

    private MediaSessionDTO session(Integer status) {
        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setId(42L);
        dto.setStreamId(STREAM_ID);
        dto.setStatus(status);
        return dto;
    }

    @Test
    @DisplayName("ACTIVE 会话下线 → remove + forceClose + SSE live.closed/stream_offline")
    void onStreamOffline_active_cleansAndClosesAndPublishes() {
        when(mediaSessionManager.getByStreamId(STREAM_ID))
            .thenReturn(session(MediaSessionConstant.Status.ACTIVE));

        listener.onStreamOffline(new StreamOfflineEvent(STREAM_ID, "zlm-1"));

        verify(liveStreamRegistry).remove(STREAM_ID);
        verify(mediaSessionManager).forceClose(42L);
        verify(sseEventBus).publish(any(SseEvent.class));
    }

    @Test
    @DisplayName("会话不存在 → 不调 forceClose，仍清缓存 + 推 SSE（幂等）")
    void onStreamOffline_noSession_stillCleansAndPublishes() {
        when(mediaSessionManager.getByStreamId(STREAM_ID)).thenReturn(null);

        listener.onStreamOffline(new StreamOfflineEvent(STREAM_ID, "zlm-1"));

        verify(liveStreamRegistry).remove(STREAM_ID);
        verify(mediaSessionManager, never()).forceClose(any());
        verify(sseEventBus).publish(any(SseEvent.class));
    }

    @Test
    @DisplayName("会话已 CLOSED → 不重复 forceClose，仍清缓存 + 推 SSE（幂等）")
    void onStreamOffline_alreadyClosed_skipsForceClose() {
        when(mediaSessionManager.getByStreamId(STREAM_ID))
            .thenReturn(session(MediaSessionConstant.Status.CLOSED));

        listener.onStreamOffline(new StreamOfflineEvent(STREAM_ID, "zlm-1"));

        verify(liveStreamRegistry).remove(STREAM_ID);
        verify(mediaSessionManager, never()).forceClose(any());
        verify(sseEventBus).publish(any(SseEvent.class));
    }

    @Test
    @DisplayName("streamId 为空 → 直接返回，不触碰缓存/DB/SSE")
    void onStreamOffline_blankStreamId_noop() {
        listener.onStreamOffline(new StreamOfflineEvent("  ", "zlm-1"));

        verify(liveStreamRegistry, never()).remove(any());
        verify(mediaSessionManager, never()).getByStreamId(any());
        verify(sseEventBus, never()).publish(any());
    }
}
