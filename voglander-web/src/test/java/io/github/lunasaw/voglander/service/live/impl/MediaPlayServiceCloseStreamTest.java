package io.github.lunasaw.voglander.service.live.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luna.common.dto.ResultDTOUtils;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.service.live.LiveStreamRegistry;
import io.github.lunasaw.voglander.service.sse.SseEvent;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.node.service.NodeService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * D5 红线：GC 回收必须<b>真实关流</b>——下沉到编排层 {@code closeStream(streamId)}：
 * 解析会话所在节点 → {@code closeRtpServer} + {@code sendBye} → 标会话 CLOSED → SSE {@code live.closed}
 * → 清 Registry + 删 pending_close key。旧实现只 {@code registry.remove}+SSE，未真正 closeRtpServer/BYE。
 *
 * @author luna
 */
@DisplayName("D5 — closeStream 真实关流编排")
@ExtendWith(MockitoExtension.class)
class MediaPlayServiceCloseStreamTest {

    private static final String STREAM_ID = "gb_live_dev1_ch1";

    @Mock
    private NodeService                 nodeService;
    @Mock
    private MediaNodeManager            mediaNodeManager;
    @Mock
    private MediaSessionManager         mediaSessionManager;
    @Mock
    private LiveStreamRegistry          liveStreamRegistry;
    @Mock
    private VoglanderServerMediaCommand voglanderServerMediaCommand;
    @Mock
    private RedisLockUtil               redisLockUtil;
    @Mock
    private SseEventBus                 sseEventBus;
    @Mock
    private StringRedisTemplate         stringRedisTemplate;

    @InjectMocks
    private MediaPlayServiceImpl        service;

    private MediaSessionDTO session() {
        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setId(42L);
        dto.setStreamId(STREAM_ID);
        dto.setDeviceId("dev1");
        dto.setCallId("call-123");
        dto.setNodeServerId("zlm-1");
        dto.setStatus(MediaSessionConstant.Status.ACTIVE);
        return dto;
    }

    private ZlmNode node() {
        ZlmNode n = new ZlmNode();
        n.setServerId("zlm-1");
        n.setHost("http://10.0.0.5:9092");
        n.setSecret("sec");
        return n;
    }

    @Test
    @DisplayName("closeStream → closeRtpServer + BYE + 标 CLOSED + SSE live.closed + 清 Registry + 删 pending key")
    void closeStream_realClose() {
        when(redisLockUtil.lock(any(), any(), any())).thenReturn(true);
        when(mediaSessionManager.getByStreamId(STREAM_ID)).thenReturn(session());
        when(nodeService.getAvailableNode("zlm-1")).thenReturn(node());
        when(voglanderServerMediaCommand.sendBye("call-123")).thenReturn(ResultDTOUtils.success());

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            service.closeStream(STREAM_ID);

            // 1. 真实 closeRtpServer 到会话所在节点
            zlm.verify(() -> ZlmRestService.closeRtpServer("http://10.0.0.5:9092", "sec", STREAM_ID));
        }
        // 2. 真实 BYE
        verify(voglanderServerMediaCommand).sendBye("call-123");
        // 3. 标会话 CLOSED
        verify(mediaSessionManager).forceClose(42L);
        // 4. SSE live.closed
        verify(sseEventBus).publish(any(SseEvent.class));
        // 5. 清 Registry
        verify(liveStreamRegistry).remove(STREAM_ID);
        // 6. 删 pending_close key
        verify(stringRedisTemplate).delete(eq("live:pending_close:" + STREAM_ID));
    }

    @Test
    @DisplayName("会话不存在 → 仍清 Registry + 删 pending key（幂等收尾），不抛异常")
    void closeStream_noSession_stillCleansUp() {
        when(redisLockUtil.lock(any(), any(), any())).thenReturn(true);
        when(mediaSessionManager.getByStreamId(STREAM_ID)).thenReturn(null);

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            service.closeStream(STREAM_ID);
            // 无会话 → 不应尝试 closeRtpServer
            zlm.verifyNoInteractions();
        }
        verify(voglanderServerMediaCommand, never()).sendBye(any());
        verify(liveStreamRegistry).remove(STREAM_ID);
        verify(stringRedisTemplate).delete(eq("live:pending_close:" + STREAM_ID));
    }

    @Test
    @DisplayName("closeStream(reason) → SSE live.closed 的 reason 取传入值（stream_offline/none_reader 等），BYE 仍发出")
    void closeStream_withReason_propagatesToSse() {
        when(redisLockUtil.lock(any(), any(), any())).thenReturn(true);
        when(mediaSessionManager.getByStreamId(STREAM_ID)).thenReturn(session());
        when(nodeService.getAvailableNode("zlm-1")).thenReturn(node());
        when(voglanderServerMediaCommand.sendBye("call-123")).thenReturn(ResultDTOUtils.success());

        ArgumentCaptor<SseEvent> captor = ArgumentCaptor.forClass(SseEvent.class);
        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            service.closeStream(STREAM_ID, "stream_offline");
            zlm.verify(() -> ZlmRestService.closeRtpServer("http://10.0.0.5:9092", "sec", STREAM_ID));
        }
        // 标准 BYE 仍发出（平台主动结束对话）
        verify(voglanderServerMediaCommand).sendBye("call-123");
        verify(sseEventBus).publish(captor.capture());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) captor.getValue().getData();
        org.junit.jupiter.api.Assertions.assertEquals("live.closed", captor.getValue().getTopic());
        org.junit.jupiter.api.Assertions.assertEquals("stream_offline", data.get("reason"));
    }

    @Test
    @DisplayName("无参 closeStream → reason 默认 idle_gc")
    void closeStream_default_reasonIdleGc() {
        when(redisLockUtil.lock(any(), any(), any())).thenReturn(true);
        when(mediaSessionManager.getByStreamId(STREAM_ID)).thenReturn(session());
        when(nodeService.getAvailableNode("zlm-1")).thenReturn(node());
        when(voglanderServerMediaCommand.sendBye("call-123")).thenReturn(ResultDTOUtils.success());

        ArgumentCaptor<SseEvent> captor = ArgumentCaptor.forClass(SseEvent.class);
        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            service.closeStream(STREAM_ID);
        }
        verify(sseEventBus).publish(captor.capture());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) captor.getValue().getData();
        org.junit.jupiter.api.Assertions.assertEquals("idle_gc", data.get("reason"));
    }

    @Test
    @DisplayName("关流去重：抢锁失败（已有线程在关）→ 直接短路，不发 BYE / 不 forceClose / 不清 Registry")
    void closeStream_lockHeld_skipsAllTeardown() {
        // ZLM 多 schema 并发回调下，落败线程抢不到关流锁 → 全部短路，真实收尾只由抢到锁的线程做一次
        when(redisLockUtil.lock(any(), any(), any())).thenReturn(false);

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            service.closeStream(STREAM_ID, "stream_offline");
            zlm.verifyNoInteractions();
        }
        // 抢锁失败：不查会话、不发 BYE、不标 CLOSED、不清 Registry、不删 key、不推 SSE
        verify(mediaSessionManager, never()).getByStreamId(any());
        verify(voglanderServerMediaCommand, never()).sendBye(any());
        verify(mediaSessionManager, never()).forceClose(any());
        verify(liveStreamRegistry, never()).remove(any());
        verify(sseEventBus, never()).publish(any());
        // 短路路径不应去解锁（锁不归自己持有）
        verify(redisLockUtil, never()).unLock(any(), any());
    }
}
