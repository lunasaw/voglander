package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.service.live.LiveSessionInfo;
import io.github.lunasaw.voglander.service.live.LiveStreamRegistry;
import io.github.lunasaw.voglander.service.live.MediaPlayService;
import io.github.lunasaw.voglander.service.live.dto.LivePlayDTO;
import io.github.lunasaw.zlm.entity.PlayUrl;

class CaptureStreamLeaseServiceTest {
    @Mock private MediaPlayService media;
    @Mock private LiveStreamRegistry registry;
    private CaptureStreamLeaseService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CaptureStreamLeaseService(media, registry);
    }

    @Test
    void acquireAndClose_shouldReleaseExactlyOneReferenceAndCloseIsIdempotent() {
        LivePlayDTO play = play("stream-1", urls("rtsp://camera"));
        LiveSessionInfo session = session("node-1", null);
        when(media.startLive(any())).thenReturn(play);
        when(registry.getSession("stream-1")).thenReturn(session);

        CaptureStreamLease lease = service.acquire("device-1", "channel-1", "RTSP");
        assertEquals("node-1", lease.getNodeServerId());
        assertEquals("rtsp://camera", lease.getSnapshotUrl());

        lease.close();
        lease.close();
        verify(media).stopLive("stream-1");
    }

    @Test
    void acquire_shouldFallbackToSessionUrlsAndReleaseWhenNodeOrUrlIsMissing() {
        PlayUrl sessionUrls = urls(null);
        sessionUrls.setHttpFlv("http://fallback/live.flv");
        LivePlayDTO play = play("stream-2", null);
        when(media.startLive(any())).thenReturn(play);
        when(registry.getSession("stream-2")).thenReturn(session("node-2", JSON.toJSONString(sessionUrls)));

        CaptureStreamLease lease = service.acquire("d", "c", "HTTP_FLV");
        assertEquals("http://fallback/live.flv", lease.getSnapshotUrl());
        lease.close();
        verify(media).stopLive("stream-2");

        when(media.startLive(any())).thenReturn(play("stream-3", urls("rtsp://x")));
        when(registry.getSession("stream-3")).thenReturn(session(null, null));
        assertThrows(ServiceException.class, () -> service.acquire("d", "c"));
        verify(media).stopLive("stream-3");
    }

    @Test
    void acquire_shouldRejectInvalidOrUnstartedStreamsWithoutStop() {
        assertThrows(ServiceException.class, () -> service.acquire("", "c"));
        when(media.startLive(any())).thenReturn(null);
        ServiceException timeout = assertThrows(ServiceException.class, () -> service.acquire("d", "c"));
        assertEquals(ServiceExceptionEnum.IMAGE_STREAM_ESTABLISH_TIMEOUT.getCode(), timeout.getCode());
        verify(media, never()).stopLive(any());
    }

    @Test
    void acquire_shouldReleaseAfterUrlResolutionFailure() {
        LivePlayDTO play = play("stream-4", urls(null));
        when(media.startLive(any())).thenReturn(play);
        when(registry.getSession("stream-4")).thenReturn(session("node-4", null));

        assertThrows(ServiceException.class, () -> service.acquire("d", "c", "HLS"));
        verify(media).stopLive("stream-4");
    }

    private static LivePlayDTO play(String streamId, PlayUrl urls) {
        LivePlayDTO play = new LivePlayDTO();
        play.setStreamId(streamId); play.setPlayUrls(urls);
        return play;
    }

    private static LiveSessionInfo session(String node, String urlsJson) {
        LiveSessionInfo info = new LiveSessionInfo();
        info.setNodeServerId(node); info.setPlayUrlsJson(urlsJson);
        return info;
    }

    private static PlayUrl urls(String rtsp) {
        PlayUrl urls = new PlayUrl();
        urls.setRtsp(rtsp);
        return urls;
    }
}
