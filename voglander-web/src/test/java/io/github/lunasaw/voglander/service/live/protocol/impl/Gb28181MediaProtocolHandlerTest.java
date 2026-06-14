package io.github.lunasaw.voglander.service.live.protocol.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luna.common.dto.ResultDTOUtils;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.service.live.protocol.MediaEstablishContext;
import io.github.lunasaw.voglander.service.live.protocol.MediaEstablishResult;
import io.github.lunasaw.voglander.service.live.protocol.MediaTerminateContext;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.rtp.OpenRtpServerReq;
import io.github.lunasaw.zlm.entity.rtp.OpenRtpServerResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * S5：GB28181 媒体协议处理器单测——验证从 {@code MediaPlayServiceImpl} 迁入的协议特定 I/O。
 * <ul>
 * <li>establish = resolveMediaIp + openRtpServer + 发 INVITE（返回真实 Call-ID + rtpPort + sdpIp）</li>
 * <li>terminate = closeRtpServer + sendBye（幂等：node/callId 缺失时跳过对应步骤）</li>
 * </ul>
 *
 * @author luna
 */
@DisplayName("S5 — Gb28181MediaProtocolHandler 建流/拆流")
@ExtendWith(MockitoExtension.class)
class Gb28181MediaProtocolHandlerTest {

    private static final String         STREAM_ID = "gb_live_dev1_ch1";

    @Mock
    private VoglanderServerMediaCommand voglanderServerMediaCommand;
    @Mock
    private MediaNodeManager            mediaNodeManager;

    @InjectMocks
    private Gb28181MediaProtocolHandler handler;

    private ZlmNode node() {
        ZlmNode n = new ZlmNode();
        n.setServerId("zlm-1");
        n.setHost("http://10.0.0.5:9092");
        n.setSecret("sec");
        return n;
    }

    private MediaEstablishContext ctx() {
        return MediaEstablishContext.builder()
            .node(node()).streamId(STREAM_ID).deviceId("dev1").channelId("ch1").streamMode("UDP").build();
    }

    @Test
    @DisplayName("supportProtocols = GB28181")
    void supportProtocols_isGb28181() {
        assertTrue(handler.supportProtocols().contains(DeviceProtocolEnum.GB28181.getType()));
    }

    @Test
    @DisplayName("establish → openRtpServer + INVITE，返回 callId/rtpPort/sdpIp")
    void establish_opensRtpAndInvites() {
        OpenRtpServerResult rtp = new OpenRtpServerResult();
        rtp.setCode("0");
        rtp.setPort("40000");
        when(voglanderServerMediaCommand.inviteRealTimePlayWithCallId(eq("dev1"), eq("ch1"), any(), eq(40000), any()))
            .thenReturn(ResultDTOUtils.success("call-xyz"));

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            zlm.when(() -> ZlmRestService.openRtpServer(any(), any(), any(OpenRtpServerReq.class))).thenReturn(rtp);

            MediaEstablishResult result = handler.establish(ctx());

            assertTrue(result.isSuccess());
            assertEquals("call-xyz", result.getCallId());
            assertEquals(40000, result.getRtpPort());
            // sdpIp 由节点 host 提取（无 tb_media_node 覆盖）
            assertEquals("10.0.0.5", result.getSdpIp());
            zlm.verify(() -> ZlmRestService.openRtpServer(eq("http://10.0.0.5:9092"), eq("sec"), any(OpenRtpServerReq.class)));
        }
        verify(voglanderServerMediaCommand)
            .inviteRealTimePlayWithCallId(eq("dev1"), eq("ch1"), eq("10.0.0.5"), eq(40000), eq(StreamModeEnum.UDP));
    }

    @Test
    @DisplayName("establish：openRtpServer 失败 → 不发 INVITE，返回 failure")
    void establish_openRtpFails_returnsFailure() {
        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            zlm.when(() -> ZlmRestService.openRtpServer(any(), any(), any(OpenRtpServerReq.class))).thenReturn(null);

            MediaEstablishResult result = handler.establish(ctx());

            assertFalse(result.isSuccess());
        }
        verify(voglanderServerMediaCommand, never())
            .inviteRealTimePlayWithCallId(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("terminate → closeRtpServer + sendBye")
    void terminate_closesRtpAndByes() {
        when(voglanderServerMediaCommand.sendBye("call-xyz")).thenReturn(ResultDTOUtils.success());

        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            handler.terminate(MediaTerminateContext.builder()
                .node(node()).streamId(STREAM_ID).callId("call-xyz").reason("idle_gc").build());

            zlm.verify(() -> ZlmRestService.closeRtpServer("http://10.0.0.5:9092", "sec", STREAM_ID));
        }
        verify(voglanderServerMediaCommand).sendBye("call-xyz");
    }

    @Test
    @DisplayName("terminate 幂等：node 为空跳过 closeRtpServer，callId 为空跳过 sendBye")
    void terminate_missingFields_skipsSteps() {
        try (MockedStatic<ZlmRestService> zlm = mockStatic(ZlmRestService.class)) {
            handler.terminate(MediaTerminateContext.builder()
                .node(null).streamId(STREAM_ID).callId(null).reason("establish_failed").build());

            zlm.verifyNoInteractions();
        }
        verify(voglanderServerMediaCommand, never()).sendBye(any());
    }
}
