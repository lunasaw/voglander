package io.github.lunasaw.voglander.service.live;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import io.github.lunasaw.zlm.node.service.NodeService;

/**
 * S2 红线：GC 以 ZLM {@code isMediaOnline} 为准对账 ACTIVE 会话。
 * <ul>
 * <li>ZLM 查无（online=false）→ 委托 {@code closeStream} 幂等收尾。</li>
 * <li>ZLM 在线（online=true）→ 不动。</li>
 * <li>查询异常 → 保守保留，不误杀。</li>
 * <li>宽限期内（createTime 太近）→ 跳过对账。</li>
 * <li>多节点：拿不到分布式锁 → 整轮跳过。</li>
 * </ul>
 *
 * @author luna
 */
@DisplayName("S2 — GC reconcileActiveSessions 以 ZLM 为准对账")
@ExtendWith(MockitoExtension.class)
class LiveSessionGcReconcileTest {

    private static final String STREAM_ID = "gb_live_dev1_ch1";
    private static final String SERVER_ID = "zlm-1";
    private static final String LOCK_KEY  = "live:gc:reconcile:lock";

    @Mock
    private MediaSessionManager mediaSessionManager;
    @Mock
    private LiveStreamRegistry  liveStreamRegistry;
    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    @Mock
    private MediaPlayService    mediaPlayService;
    @Mock
    private NodeService         nodeService;
    @Mock
    private RedisLockUtil       redisLockUtil;

    @InjectMocks
    private LiveSessionGcService gcService;

    private MediaSessionDTO activeSession() {
        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setId(42L);
        dto.setStreamId(STREAM_ID);
        dto.setNodeServerId(SERVER_ID);
        dto.setStatus(MediaSessionConstant.Status.ACTIVE);
        // 远早于宽限期，正常对账
        dto.setCreateTime(LocalDateTime.now().minusMinutes(10));
        return dto;
    }

    private ZlmNode node() {
        ZlmNode n = new ZlmNode();
        n.setServerId(SERVER_ID);
        n.setHost("http://10.0.0.5:9092");
        n.setSecret("sec");
        return n;
    }

    private MediaOnlineStatus online(Boolean v) {
        MediaOnlineStatus st = new MediaOnlineStatus();
        st.setOnline(v);
        return st;
    }

    private void lockAcquired() {
        when(redisLockUtil.generateLockValue()).thenReturn("v1");
        when(redisLockUtil.lock(eq(LOCK_KEY), eq("v1"), anyInt())).thenReturn(true);
    }

    @Test
    @DisplayName("ZLM 查无(online=false) → 委托 closeStream(streamId)")
    void reconcile_streamDead_closesStream() {
        lockAcquired();
        when(mediaSessionManager.getActiveSessions()).thenReturn(List.of(activeSession()));
        when(nodeService.getAvailableNode(SERVER_ID)).thenReturn(node());

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            zlm.when(() -> ZlmRestService.isMediaOnline(any(), any(), any(MediaReq.class)))
                .thenReturn(online(false));

            gcService.reconcileActiveSessions();
        }

        verify(mediaPlayService).closeStream(STREAM_ID);
        verify(redisLockUtil).unLock(LOCK_KEY, "v1");
    }

    @Test
    @DisplayName("ZLM 在线(online=true) → 不关流")
    void reconcile_streamAlive_keepsStream() {
        lockAcquired();
        when(mediaSessionManager.getActiveSessions()).thenReturn(List.of(activeSession()));
        when(nodeService.getAvailableNode(SERVER_ID)).thenReturn(node());

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            zlm.when(() -> ZlmRestService.isMediaOnline(any(), any(), any(MediaReq.class)))
                .thenReturn(online(true));

            gcService.reconcileActiveSessions();
        }

        verify(mediaPlayService, never()).closeStream(any());
    }

    @Test
    @DisplayName("判活异常 → 保守保留，不误杀")
    void reconcile_queryThrows_keepsConservatively() {
        lockAcquired();
        when(mediaSessionManager.getActiveSessions()).thenReturn(List.of(activeSession()));
        when(nodeService.getAvailableNode(SERVER_ID)).thenReturn(node());

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            zlm.when(() -> ZlmRestService.isMediaOnline(any(), any(), any(MediaReq.class)))
                .thenThrow(new RuntimeException("network jitter"));

            gcService.reconcileActiveSessions();
        }

        verify(mediaPlayService, never()).closeStream(any());
    }

    @Test
    @DisplayName("宽限期内(createTime 太近) → 跳过对账，不调 isMediaOnline / closeStream")
    void reconcile_withinGrace_skips() {
        lockAcquired();
        MediaSessionDTO fresh = activeSession();
        fresh.setCreateTime(LocalDateTime.now().minusSeconds(5));
        when(mediaSessionManager.getActiveSessions()).thenReturn(List.of(fresh));

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            gcService.reconcileActiveSessions();
            zlm.verifyNoInteractions();
        }

        verify(nodeService, never()).getAvailableNode(any());
        verify(mediaPlayService, never()).closeStream(any());
    }

    @Test
    @DisplayName("拿不到分布式锁 → 整轮跳过，不查 getActiveSessions")
    void reconcile_lockNotAcquired_skipsRound() {
        when(redisLockUtil.generateLockValue()).thenReturn("v1");
        when(redisLockUtil.lock(eq(LOCK_KEY), eq("v1"), anyInt())).thenReturn(false);

        gcService.reconcileActiveSessions();

        verify(mediaSessionManager, never()).getActiveSessions();
        verify(redisLockUtil, never()).unLock(any(), any());
    }

    @Test
    @DisplayName("节点已下线(getAvailableNode=null) → 跳过该会话，交给 NodeExitedEvent")
    void reconcile_nodeGone_skipsSession() {
        lockAcquired();
        when(mediaSessionManager.getActiveSessions()).thenReturn(List.of(activeSession()));
        when(nodeService.getAvailableNode(SERVER_ID)).thenReturn(null);

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            gcService.reconcileActiveSessions();
            zlm.verifyNoInteractions();
        }

        verify(mediaPlayService, never()).closeStream(any());
    }
}
