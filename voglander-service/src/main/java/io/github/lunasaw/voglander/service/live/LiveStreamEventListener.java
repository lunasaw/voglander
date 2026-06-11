package io.github.lunasaw.voglander.service.live;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.common.event.AlarmCreatedEvent;
import io.github.lunasaw.voglander.common.event.NodeExitedEvent;
import io.github.lunasaw.voglander.common.event.StreamOfflineEvent;
import io.github.lunasaw.voglander.common.event.StreamReadyEvent;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.service.sse.SseEvent;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 直播流 Spring 事件监听器。
 * <p>
 * 消费 integration 层发布的 {@link StreamReadyEvent} / {@link NodeExitedEvent}，
 * 处理首播 future 唤醒、SSE 推送、节点故障流关闭等，
 * 不直接依赖 integration 层 Bean，打破 service↔integration 循环依赖。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class LiveStreamEventListener {

    @Autowired
    private LiveStreamRegistry  liveStreamRegistry;
    @Autowired
    private SseEventBus         sseEventBus;
    @Autowired
    private MediaSessionManager mediaSessionManager;

    /**
     * 流上线：唤醒本节点首播 future + 推 SSE live.ready。
     */
    @EventListener
    public void onStreamReady(StreamReadyEvent event) {
        String streamId = event.getStreamId();
        log.info("流上线事件, streamId={}", streamId);
        liveStreamRegistry.completeFuture(streamId);
        sseEventBus.publish(new SseEvent("live.ready", Map.of("streamId", streamId)));
    }

    /**
     * 节点退出：关闭该节点上所有 ACTIVE 会话，推 SSE live.closed（前端自动重连）。
     */
    @EventListener
    public void onNodeExited(NodeExitedEvent event) {
        String serverId = event.getServerId();
        log.warn("ZLM 节点退出, serverId={}", serverId);
        List<MediaSessionDTO> activeSessions = mediaSessionManager.getActiveSessionsByNode(serverId);
        for (MediaSessionDTO session : activeSessions) {
            try {
                mediaSessionManager.forceClose(session.getId());
                if (session.getStreamId() != null) {
                    liveStreamRegistry.remove(session.getStreamId());
                    sseEventBus.publish(new SseEvent("live.closed",
                        Map.of("streamId", session.getStreamId(), "reason", "node_exited")));
                }
            } catch (Exception e) {
                log.warn("关闭节点退出会话失败, sessionId={}: {}", session.getId(), e.getMessage());
            }
        }
        log.info("节点退出处理完成, serverId={}, 关闭会话数={}", serverId, activeSessions.size());
    }

    /**
     * 流下线：清直播缓存 + 标 DB 会话 CLOSED + 推 SSE live.closed（前端据此自动重连或提示）。
     * <p>
     * 与上线 {@link StreamReadyEvent} 对称，由 ZLM {@code onStreamChanged(regist=false)} 驱动，
     * 覆盖 rtp 直播流（不依赖 StreamProxy 表）。幂等：缓存不存在时 remove 是 no-op；
     * 会话不存在或已 CLOSED 时跳过标记。
     * </p>
     */
    @EventListener
    public void onStreamOffline(StreamOfflineEvent event) {
        String streamId = event.getStreamId();
        if (streamId == null || streamId.isBlank()) {
            return;
        }
        log.info("流下线事件, streamId={}, serverId={}", streamId, event.getServerId());

        // 1. 清 Redis 缓存（session + refcount + 本地 future）
        liveStreamRegistry.remove(streamId);

        // 2. 标 DB 会话 CLOSED（best-effort，不阻断）——必做，否则 GC reconcile 会重复捞到 + 重复 closeStream/SSE
        try {
            MediaSessionDTO session = mediaSessionManager.getByStreamId(streamId);
            if (session != null && session.getId() != null
                && !Objects.equals(session.getStatus(), MediaSessionConstant.Status.CLOSED)) {
                mediaSessionManager.forceClose(session.getId());
            }
        } catch (Exception e) {
            log.warn("流下线标记 DB CLOSED 失败, streamId={}: {}", streamId, e.getMessage());
        }

        // 3. 推 SSE（reason=stream_offline，与 idle_gc / node_exited 区分来源）
        sseEventBus.publish(new SseEvent("live.closed",
            Map.of("streamId", streamId, "reason", "stream_offline")));
    }

    /**
     * 告警新建：推 SSE alarm.new。
     */
    @EventListener
    public void onAlarmCreated(AlarmCreatedEvent event) {
        sseEventBus.publish(new SseEvent("alarm.new",
            Map.of("deviceId", event.getDeviceId() != null ? event.getDeviceId() : "")));
    }
}
