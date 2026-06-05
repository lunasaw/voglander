package io.github.lunasaw.voglander.service.live;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.service.sse.SseEventBus;

/**
 * D5 红线：GC {@code drainPendingClose} 命中 refCount=0 时，必须委托编排层
 * {@link MediaPlayService#closeStream(String)} 真实关流，而非旧实现的仅 {@code registry.remove}+SSE。
 *
 * @author luna
 */
@DisplayName("D5 — GC drainPendingClose 委托 closeStream")
@ExtendWith(MockitoExtension.class)
class LiveSessionGcServiceTest {

    private static final String PREFIX    = "live:pending_close:";
    private static final String STREAM_ID = "gb_live_dev1_ch1";

    @Mock
    private MediaSessionManager mediaSessionManager;
    @Mock
    private LiveStreamRegistry  liveStreamRegistry;
    @Mock
    private SseEventBus         sseEventBus;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private MediaPlayService    mediaPlayService;

    @InjectMocks
    private LiveSessionGcService gcService;

    @Test
    @DisplayName("refCount=0 命中 → 委托 mediaPlayService.closeStream(streamId)")
    void drainPendingClose_refZero_delegatesToCloseStream() {
        when(stringRedisTemplate.keys(PREFIX + "*")).thenReturn(Set.of(PREFIX + STREAM_ID));
        when(liveStreamRegistry.getRef(STREAM_ID)).thenReturn(0L);

        gcService.drainPendingClose();

        verify(mediaPlayService).closeStream(STREAM_ID);
    }

    @Test
    @DisplayName("仍有观看者(refCount>0) → 不关流，仅取消 pending 标记")
    void drainPendingClose_refPositive_cancelsPendingOnly() {
        when(stringRedisTemplate.keys(PREFIX + "*")).thenReturn(Set.of(PREFIX + STREAM_ID));
        when(liveStreamRegistry.getRef(STREAM_ID)).thenReturn(2L);

        gcService.drainPendingClose();

        verify(mediaPlayService, never()).closeStream(STREAM_ID);
        verify(stringRedisTemplate).delete(eq(PREFIX + STREAM_ID));
    }
}
