package io.github.lunasaw.voglander.service.live.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.service.live.LiveSessionInfo;
import io.github.lunasaw.voglander.service.live.LiveStreamRegistry;
import io.github.lunasaw.voglander.service.live.dto.LivePlayDTO;
import io.github.lunasaw.voglander.service.live.dto.LiveStartDTO;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import io.github.lunasaw.zlm.node.service.NodeService;

/**
 * S3.2：复用分支可选探活开关 {@code live.reuse-verify-enabled}。
 * <ul>
 * <li>开关关闭（默认）→ 命中 ACTIVE 缓存直接复用，不向 ZLM 探活。</li>
 * <li>开关开启 + ZLM 查无 → 清缓存落入首播重建（此处用 selectNode=null 触发可断言的早退证明已离开复用分支）。</li>
 * </ul>
 *
 * @author luna
 */
@DisplayName("S3.2 — 复用前可选探活开关")
@ExtendWith(MockitoExtension.class)
class MediaPlayServiceReuseVerifyTest {

    private static final String STREAM_ID = "gb_live_dev1_ch1";
    private static final String SERVER_ID = "zlm-1";

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

    private LiveStartDTO startDto() {
        LiveStartDTO dto = new LiveStartDTO();
        dto.setDeviceId("dev1");
        dto.setChannelId("ch1");
        return dto;
    }

    private LiveSessionInfo activeInfo() {
        LiveSessionInfo info = new LiveSessionInfo();
        info.setNodeServerId(SERVER_ID);
        info.setStatus(MediaSessionConstant.Status.ACTIVE);
        return info;
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

    @Test
    @DisplayName("开关关闭 → 直接复用，不探活")
    void reuse_verifyDisabled_reusesWithoutProbe() {
        ReflectionTestUtils.setField(service, "reuseVerifyEnabled", false);
        when(redisLockUtil.generateLockValue()).thenReturn("v1");
        when(redisLockUtil.tryLock(any(), any(), any(), any())).thenReturn(true);
        when(liveStreamRegistry.getSession(STREAM_ID)).thenReturn(activeInfo());

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            LivePlayDTO result = service.startLive(startDto());
            Assertions.assertEquals(STREAM_ID, result.getStreamId());
            // 复用分支：不应向 ZLM 探活
            zlm.verifyNoInteractions();
        }
        verify(liveStreamRegistry).incRef(STREAM_ID);
    }

    @Test
    @DisplayName("开关开启 + ZLM 查无 → 清缓存离开复用分支（落首播重建）")
    void reuse_verifyEnabled_deadStream_fallsThroughToRebuild() {
        ReflectionTestUtils.setField(service, "reuseVerifyEnabled", true);
        when(redisLockUtil.generateLockValue()).thenReturn("v1");
        when(redisLockUtil.tryLock(any(), any(), any(), any())).thenReturn(true);
        when(liveStreamRegistry.getSession(STREAM_ID)).thenReturn(activeInfo());
        when(nodeService.getAvailableNode(SERVER_ID)).thenReturn(node());
        // selectNode 返回 null → 离开复用分支后首播���建因无节点抛 LIVE_NODE_UNAVAILABLE，证明已落入重建
        when(nodeService.selectNode()).thenReturn(null);

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            zlm.when(() -> ZlmRestService.isMediaOnline(any(), any(), any(MediaReq.class)))
                .thenReturn(online(false));

            Assertions.assertThrows(ServiceException.class, () -> service.startLive(startDto()));
        }
        // 探活判死后清缓存
        verify(liveStreamRegistry).remove(STREAM_ID);
    }
}
