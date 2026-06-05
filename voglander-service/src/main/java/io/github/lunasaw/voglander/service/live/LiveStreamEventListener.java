package io.github.lunasaw.voglander.service.live;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.event.AlarmCreatedEvent;
import io.github.lunasaw.voglander.common.event.NodeExitedEvent;
import io.github.lunasaw.voglander.common.event.StreamReadyEvent;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.service.sse.SseEvent;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

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
     * 告警新建：推 SSE alarm.new。
     */
    @EventListener
    public void onAlarmCreated(AlarmCreatedEvent event) {
        sseEventBus.publish(new SseEvent("alarm.new",
            Map.of("deviceId", event.getDeviceId() != null ? event.getDeviceId() : "")));
    }
}
