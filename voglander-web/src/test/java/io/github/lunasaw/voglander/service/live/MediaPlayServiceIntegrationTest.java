package io.github.lunasaw.voglander.service.live;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.luna.common.dto.ResultDTOUtils;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.service.live.dto.LivePlayDTO;
import io.github.lunasaw.voglander.service.live.dto.LiveStartDTO;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.PlayUrl;
import io.github.lunasaw.zlm.entity.ServerResponse;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import io.github.lunasaw.zlm.entity.rtp.OpenRtpServerReq;
import io.github.lunasaw.zlm.entity.rtp.OpenRtpServerResult;
import io.github.lunasaw.zlm.node.service.NodeService;
import lombok.extern.slf4j.Slf4j;

/**
 * MediaPlayService 集成测试（首播、复用、停流、getLive、keepAlive）。
 * 依赖真实 Redis；ZLM / NodeService / GB28181 命令均通过 mock 隔离。
 */
@Slf4j
public class MediaPlayServiceIntegrationTest extends BaseTest {

    private static final String DEVICE_ID  = "dev-mps-test";
    private static final String CHANNEL_ID = "ch-mps-test";
    private static final String STREAM_ID  = "gb_live_" + DEVICE_ID + "_" + CHANNEL_ID;

    @Autowired
    private MediaPlayService         mediaPlayService;
    @Autowired
    private LiveStreamRegistry       liveStreamRegistry;
    @Autowired
    private RedisConnectionFactory   connectionFactory;

    @MockitoBean
    private NodeService              nodeService;
    @MockitoBean
    private VoglanderServerMediaCommand voglanderServerMediaCommand;

    private ZlmNode mockNode;

    @BeforeEach
    public void checkRedisAndClean() {
        boolean available;
        try {
            connectionFactory.getConnection().ping();
            available = true;
        } catch (Exception e) {
            available = false;
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(available, "Redis 不可用，跳过");
        liveStreamRegistry.remove(STREAM_ID);

        mockNode = new ZlmNode();
        mockNode.setServerId("zlm-test");
        mockNode.setHost("http://127.0.0.1:9092");
        mockNode.setSecret("test-secret");
        when(nodeService.selectNode()).thenReturn(mockNode);
    }

    @AfterEach
    public void cleanup() {
        liveStreamRegistry.remove(STREAM_ID);
    }

    @Test
    public void testStartLive_FirstPlay_CreatesActiveSession() throws Exception {
        OpenRtpServerResult rtpResult = new OpenRtpServerResult();
        rtpResult.setCode("0");
        rtpResult.setPort("40000");

        PlayUrl playUrl = new PlayUrl();
        playUrl.setHttpFlv("http://127.0.0.1:8080/rtp/" + STREAM_ID + ".flv");
        ServerResponse<PlayUrl> playResp = ServerResponse.success(playUrl);

        when(voglanderServerMediaCommand.inviteRealTimePlay(any(), any(), any(), any()))
            .thenAnswer(inv -> {
                // 模拟 ZLM on_stream_changed 延迟触发 future 完成
                Executors.newSingleThreadScheduledExecutor().schedule(
                    () -> liveStreamRegistry.completeFuture(STREAM_ID),
                    300, TimeUnit.MILLISECONDS);
                return ResultDTOUtils.success();
            });

        try (MockedStatic<ZlmRestService> zlmMock = mockStatic(ZlmRestService.class)) {
            zlmMock.when(() -> ZlmRestService.openRtpServer(any(), any(), any(OpenRtpServerReq.class)))
                .thenReturn(rtpResult);
            zlmMock.when(() -> ZlmRestService.getPlaybackUrls(any(), any(), any(MediaReq.class)))
                .thenReturn(playResp);

            LiveStartDTO req = new LiveStartDTO();
            req.setDeviceId(DEVICE_ID);
            req.setChannelId(CHANNEL_ID);

            LivePlayDTO result = mediaPlayService.startLive(req);

            assertNotNull(result);
            assertEquals(STREAM_ID, result.getStreamId());
            assertEquals(MediaSessionConstant.Status.ACTIVE, result.getStatus());
            assertEquals(1, result.getRefCount(), "首播 refCount=1");
            assertNotNull(result.getPlayUrls());
            assertEquals(playUrl.getHttpFlv(), result.getPlayUrls().getHttpFlv());
        }
    }

    @Test
    public void testStartLive_Reuse_IncrementsRefCount() {
        // 预置 ACTIVE 会话（跳过首播路径）
        LiveSessionInfo info = new LiveSessionInfo();
        info.setStatus(MediaSessionConstant.Status.ACTIVE);
        info.setCallId("call-reuse");
        info.setNodeServerId("zlm-test");
        info.setPlayUrlsJson("{\"httpFlv\":\"http://x/live.flv\"}");
        liveStreamRegistry.putSession(STREAM_ID, info);
        liveStreamRegistry.incRef(STREAM_ID); // 模拟已有 1 个观看者

        LiveStartDTO req = new LiveStartDTO();
        req.setDeviceId(DEVICE_ID);
        req.setChannelId(CHANNEL_ID);

        // 第一次复用
        LivePlayDTO r1 = mediaPlayService.startLive(req);
        assertEquals(STREAM_ID, r1.getStreamId());
        assertEquals(2, r1.getRefCount(), "复用后 refCount=2");

        // 第二次复用
        LivePlayDTO r2 = mediaPlayService.startLive(req);
        assertEquals(3, r2.getRefCount(), "再次复用 refCount=3");

        // NodeService 和 INVITE 未被调用
        verify(nodeService, never()).selectNode();
        verify(voglanderServerMediaCommand, never()).inviteRealTimePlay(any(), any(), any(), any());
    }

    @Test
    public void testStopLive_DecrementsRefCount() {
        LiveSessionInfo info = new LiveSessionInfo();
        info.setStatus(MediaSessionConstant.Status.ACTIVE);
        liveStreamRegistry.putSession(STREAM_ID, info);
        liveStreamRegistry.incRef(STREAM_ID);
        liveStreamRegistry.incRef(STREAM_ID); // ref=2

        boolean r1 = mediaPlayService.stopLive(STREAM_ID);
        assertTrue(r1);
        assertEquals(1, liveStreamRegistry.getRef(STREAM_ID), "stop 1 → ref=1");

        boolean r2 = mediaPlayService.stopLive(STREAM_ID);
        assertTrue(r2);
        assertEquals(0, liveStreamRegistry.getRef(STREAM_ID), "stop 2 → ref=0");
    }

    @Test
    public void testGetLive_NotFound_Throws() {
        ServiceException ex = assertThrows(ServiceException.class,
            () -> mediaPlayService.getLive(STREAM_ID));
        assertEquals(ServiceExceptionEnum.LIVE_STREAM_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    public void testGetLive_Found_ReturnsDTO() {
        LiveSessionInfo info = new LiveSessionInfo();
        info.setStatus(MediaSessionConstant.Status.ACTIVE);
        info.setCallId("call-get");
        liveStreamRegistry.putSession(STREAM_ID, info);

        LivePlayDTO dto = mediaPlayService.getLive(STREAM_ID);
        assertEquals(STREAM_ID, dto.getStreamId());
        assertEquals("call-get", dto.getCallId());
    }

    @Test
    public void testKeepAlive_DoesNotThrow() {
        LiveSessionInfo info = new LiveSessionInfo();
        info.setStatus(MediaSessionConstant.Status.ACTIVE);
        liveStreamRegistry.putSession(STREAM_ID, info);

        assertDoesNotThrow(() -> mediaPlayService.keepAlive(STREAM_ID));
    }
}
