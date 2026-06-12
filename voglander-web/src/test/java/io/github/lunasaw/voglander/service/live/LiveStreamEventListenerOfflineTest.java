package io.github.lunasaw.voglander.service.live;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.event.StreamOfflineEvent;

/**
 * S1 红线（协议合规版）：流下线事件监听器 —— 委托 {@code closeStream(streamId, reason)} 做标准 BYE 收尾
 * （closeRtpServer + sendBye + 标 CLOSED + SSE live.closed + 清缓存 + 删 pending key）。
 * <p>
 * 与上线 {@code StreamReadyEvent} 对称，覆盖 rtp 直播流（不依赖 StreamProxy 表）。
 * 符合 SIP/GB28181：established 对话由平台作为 UAC 主动发 BYE 终止，而非单方面删缓存。
 * </p>
 *
 * @author luna
 */
@DisplayName("S1 — LiveStreamEventListener.onStreamOffline 委托 closeStream 标准 BYE 收尾")
@ExtendWith(MockitoExtension.class)
class LiveStreamEventListenerOfflineTest {

    private static final String STREAM_ID = "gb_live_dev1_ch1";

    @Mock
    private LiveStreamRegistry liveStreamRegistry;
    @Mock
    private io.github.lunasaw.voglander.service.sse.SseEventBus sseEventBus;
    @Mock
    private io.github.lunasaw.voglander.manager.manager.MediaSessionManager mediaSessionManager;
    @Mock
    private MediaPlayService    mediaPlayService;

    @InjectMocks
    private LiveStreamEventListener listener;

    @Test
    @DisplayName("常规下线 → 委托 closeStream(streamId, stream_offline)")
    void onStreamOffline_delegatesCloseStream_streamOffline() {
        listener.onStreamOffline(new StreamOfflineEvent(STREAM_ID, "zlm-1"));

        verify(mediaPlayService).closeStream(STREAM_ID, "stream_offline");
    }

    @Test
    @DisplayName("无人观看回收 → reason=none_reader 透传到 closeStream")
    void onStreamOffline_delegatesCloseStream_noneReader() {
        listener.onStreamOffline(new StreamOfflineEvent(STREAM_ID, "zlm-1", "none_reader"));

        verify(mediaPlayService).closeStream(STREAM_ID, "none_reader");
    }

    @Test
    @DisplayName("streamId 为空 → 直接返回��不调 closeStream")
    void onStreamOffline_blankStreamId_noop() {
        listener.onStreamOffline(new StreamOfflineEvent("  ", "zlm-1"));

        verify(mediaPlayService, never()).closeStream(any(), any());
        verify(mediaPlayService, never()).closeStream(any());
    }
}
