package io.github.lunasaw.voglander.service.live;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.service.sse.SseEvent;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import lombok.extern.slf4j.Slf4j;

/**
 * 直播会话 GC 调度器。
 * <p>
 * 1. INVITING 超时 → FAILED<br>
 * 2. pending_close 到期且 refCount=0 → 清理 Registry<br>
 * </p>
 *
 * @author luna
 */
@Slf4j
@Service
public class LiveSessionGcService {

    private static final int    INVITING_TIMEOUT_MIN  = 2;
    private static final String PENDING_CLOSE_PREFIX  = "live:pending_close:";

    @Autowired private MediaSessionManager mediaSessionManager;
    @Autowired private LiveStreamRegistry  liveStreamRegistry;
    @Autowired private SseEventBus         sseEventBus;
    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Scheduled(fixedDelay = 60_000)
    public void gc() {
        // 1. INVITING 超时 → FAILED
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(INVITING_TIMEOUT_MIN);
        int failed = mediaSessionManager.markTimeoutInvitingAsFailed(deadline);
        if (failed > 0) {
            log.info("[GC] INVITING 超时标 FAILED, count={}", failed);
        }

        // 2. drainPendingClose：扫 Redis pending_close:* 键，refCount=0 则清理 Registry
        drainPendingClose();
    }

    void drainPendingClose() {
        Set<String> keys = stringRedisTemplate.keys(PENDING_CLOSE_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return;
        for (String key : keys) {
            String streamId = key.substring(PENDING_CLOSE_PREFIX.length());
            if (liveStreamRegistry.getRef(streamId) <= 0) {
                liveStreamRegistry.remove(streamId);
                sseEventBus.publish(new SseEvent("live.closed",
                    Map.of("streamId", streamId, "reason", "idle_gc")));
                stringRedisTemplate.delete(key);
                log.info("[GC] 清理空闲流, streamId={}", streamId);
            } else {
                // 仍有观看者，取消 pending
                stringRedisTemplate.delete(key);
            }
        }
    }
}
