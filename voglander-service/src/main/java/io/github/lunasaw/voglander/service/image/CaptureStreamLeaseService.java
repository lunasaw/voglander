package io.github.lunasaw.voglander.service.image;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.service.live.LiveSessionInfo;
import io.github.lunasaw.voglander.service.live.LiveStreamRegistry;
import io.github.lunasaw.voglander.service.live.MediaPlayService;
import io.github.lunasaw.voglander.service.live.dto.LivePlayDTO;
import io.github.lunasaw.voglander.service.live.dto.LiveStartDTO;
import io.github.lunasaw.zlm.entity.PlayUrl;

/** Starts/reuses a live stream and releases only the reference acquired by this capture. */
@Service
public class CaptureStreamLeaseService {
    private final MediaPlayService mediaPlayService;
    private final LiveStreamRegistry liveStreamRegistry;

    public CaptureStreamLeaseService(MediaPlayService mediaPlayService, LiveStreamRegistry liveStreamRegistry) {
        this.mediaPlayService = Objects.requireNonNull(mediaPlayService, "mediaPlayService");
        this.liveStreamRegistry = Objects.requireNonNull(liveStreamRegistry, "liveStreamRegistry");
    }

    public CaptureStreamLease acquire(String deviceId, String channelId) {
        return acquire(deviceId, channelId, "RTSP");
    }

    public CaptureStreamLease acquire(String deviceId, String channelId, String protocol) {
        if (!StringUtils.hasText(deviceId) || !StringUtils.hasText(channelId)) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_CAMERA_NOT_FOUND);
        }
        LiveStartDTO request = new LiveStartDTO();
        request.setDeviceId(deviceId);
        request.setChannelId(channelId);
        request.setProtocol(protocol);
        LivePlayDTO play = mediaPlayService.startLive(request);
        if (play == null || !StringUtils.hasText(play.getStreamId())) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_STREAM_ESTABLISH_TIMEOUT);
        }
        boolean acquired = true;
        try {
            LiveSessionInfo session = liveStreamRegistry.getSession(play.getStreamId());
            String node = session == null ? null : session.getNodeServerId();
            if (!StringUtils.hasText(node)) {
                throw new ServiceException(ServiceExceptionEnum.IMAGE_SNAPSHOT_FAILED)
                    .setDetailMessage("live session has no media node");
            }
            String url = chooseUrl(play.getPlayUrls(), protocol);
            if (!StringUtils.hasText(url) && session != null && StringUtils.hasText(session.getPlayUrlsJson())) {
                url = chooseUrl(JSON.parseObject(session.getPlayUrlsJson(), PlayUrl.class), protocol);
            }
            if (!StringUtils.hasText(url)) {
                throw new ServiceException(ServiceExceptionEnum.IMAGE_SNAPSHOT_FAILED)
                    .setDetailMessage("live session has no snapshot URL");
            }
            return new CaptureStreamLease(mediaPlayService, play.getStreamId(), node, url);
        } catch (RuntimeException exception) {
            if (acquired) {
                mediaPlayService.stopLive(play.getStreamId());
            }
            throw exception;
        }
    }

    private static String chooseUrl(PlayUrl urls, String protocol) {
        if (urls == null) return null;
        String preferred = protocol == null ? "RTSP" : protocol.toUpperCase();
        if ("HLS".equals(preferred) && StringUtils.hasText(urls.getHls())) return urls.getHls();
        if (("HTTP_FLV".equals(preferred) || "FLV".equals(preferred)) && StringUtils.hasText(urls.getHttpFlv())) return urls.getHttpFlv();
        if (StringUtils.hasText(urls.getRtsp())) return urls.getRtsp();
        if (StringUtils.hasText(urls.getHttpFlv())) return urls.getHttpFlv();
        return urls.getHls();
    }
}
