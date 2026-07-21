package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.gb28181.common.entity.enums.InviteSessionNameEnum;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gb28181.common.entity.sdp.TransportEnum;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry.TransactionContextInfo;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;

/**
 * LabMediaPushService 单元测试。
 *
 * <p>覆盖：parseTarget（SDP 字段提取 + null 兜底）、buildCmd（loop/transcode/ssrc 各开关组合）、
 * validateFile（越界/不存在/正常）、startPush（无目标/非UDP/非法文件抛异常；假 ffmpeg 脚本验进程起停）、
 * acceptInvite（回 200 OK；ctx 失效跳过）。
 */
@ExtendWith(MockitoExtension.class)
class LabMediaPushServiceTest {

    @Mock
    LabPushProperties            props;
    @Mock
    VoglanderSipClientProperties clientProps;
    @Mock
    org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    LabMediaPushService          service;

    @BeforeEach
    void stub() {
        lenient().when(props.getFfmpegPath()).thenReturn("ffmpeg");
        lenient().when(props.isLoop()).thenReturn(true);
        lenient().when(props.isTranscode()).thenReturn(true);
        lenient().when(props.isAuto()).thenReturn(true);
        lenient().when(props.isZlmMode()).thenReturn(false);  // 默认禁用 ZLM 模式，使用直推
        lenient().when(clientProps.getDomain()).thenReturn("127.0.0.1");
        lenient().when(clientProps.getPort()).thenReturn(5061);

        // 显式设置运行时模式为非 ZLM（@PostConstruct 在单元测试中不自动调用）
        service.setZlmModeRuntime(false);
    }

    private LabInviteTarget udpTarget() {
        return new LabInviteTarget("call-1", "34020000001320000001", "127.0.0.1", 30000,
            "0987654321", "UDP", "Play", "ctx-key-1", null);
    }

    private LabInviteTarget tcpActiveTarget() {
        return new LabInviteTarget("call-2", "34020000001320000001", "127.0.0.1", 30000,
            null, "TCP", "Play", "ctx-key-2", "active");
    }

    private LabInviteTarget tcpPassiveTarget() {
        return new LabInviteTarget("call-3", "34020000001320000001", "127.0.0.1", 30001,
            null, "TCP", "Play", "ctx-key-3", "passive");
    }

    // ── parseTarget ──────────────────────────────────────────────────────

    @Test
    @DisplayName("parseTarget: 从 GbSessionDescription 提取 ip/port/ssrc/transport/sessionType")
    void parseTarget_fromGbSdp() {
        GbSessionDescription gb = new GbSessionDescription(null);
        gb.setAddress("10.0.0.5");
        gb.setPort(40000);
        gb.setSsrc("1234567890");
        gb.setTransport(TransportEnum.UDP);
        gb.setSessionType(InviteSessionNameEnum.PLAY);
        ClientInviteEvent e = new ClientInviteEvent(this, "callX", "devY", gb, "ctxZ");

        LabInviteTarget t = service.parseTarget(e);

        assertThat(t.getCallId()).isEqualTo("callX");
        assertThat(t.getUserId()).isEqualTo("devY");
        assertThat(t.getMediaIp()).isEqualTo("10.0.0.5");
        assertThat(t.getMediaPort()).isEqualTo(40000);
        assertThat(t.getSsrc()).isEqualTo("1234567890");
        assertThat(t.getTransport()).isEqualTo("UDP");
        assertThat(t.getSessionType()).isEqualTo("Play");
        assertThat(t.getCtxKey()).isEqualTo("ctxZ");
    }

    @Test
    @DisplayName("parseTarget: SDP 为 null 时兜底默认值，不抛异常")
    void parseTarget_nullSdp() {
        ClientInviteEvent e = new ClientInviteEvent(this, "callX", "devY", null, "ctxZ");

        LabInviteTarget t = service.parseTarget(e);

        assertThat(t.getCallId()).isEqualTo("callX");
        assertThat(t.getMediaIp()).isNull();
        assertThat(t.getMediaPort()).isZero();
        assertThat(t.getTransport()).isEqualTo("UDP");
        assertThat(t.getSessionType()).isEqualTo("Play");
    }

    // ── buildCmd ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("buildCmd: loop+transcode+ssrc 全开，生成完整 libx264 命令数组")
    void buildCmd_loopTranscodeSsrc() {
        String[] cmd = service.buildCmd("/usr/local/bin/ffmpeg", "/tmp/a.mp4", udpTarget());

        assertThat(cmd[0]).isEqualTo("/usr/local/bin/ffmpeg");
        assertThat(cmd).contains("-re", "-stream_loop", "-1", "-i", "/tmp/a.mp4", "-an",
            "libx264", "-f", "rtp_mpegts");
        assertThat(cmd[cmd.length - 1])
            .isEqualTo("rtp://127.0.0.1:30000?pkt_size=1316&ssrc=0987654321");
    }

    @Test
    @DisplayName("buildCmd: loop=false 不含 -stream_loop")
    void buildCmd_noLoop() {
        when(props.isLoop()).thenReturn(false);
        String[] cmd = service.buildCmd("ffmpeg", "/tmp/a.mp4", udpTarget());
        assertThat(cmd).doesNotContain("-stream_loop");
    }

    @Test
    @DisplayName("buildCmd: transcode=false 用 -c:v copy，不含 libx264/bsf")
    void buildCmd_copy() {
        when(props.isTranscode()).thenReturn(false);
        String[] cmd = service.buildCmd("ffmpeg", "/tmp/a.mp4", udpTarget());
        assertThat(cmd).contains("-c:v", "copy");
        assertThat(cmd).doesNotContain("libx264", "h264_mp4toannexb");
    }

    @Test
    @DisplayName("buildCmd: ssrc 为空时 rtp url 不带 ssrc 参数")
    void buildCmd_noSsrc() {
        LabInviteTarget t = new LabInviteTarget("c", "u", "127.0.0.1", 30000, null, "UDP", "Play", "k", null);
        String[] cmd = service.buildCmd("ffmpeg", "/tmp/a.mp4", t);
        assertThat(cmd[cmd.length - 1]).isEqualTo("rtp://127.0.0.1:30000?pkt_size=1316");
    }

    @Test
    @DisplayName("buildCmd: TCP 主动模式 → -f mpegts tcp://ip:port")
    void buildCmd_tcpActive() {
        String[] cmd = service.buildCmd("ffmpeg", "/tmp/a.mp4", tcpActiveTarget());
        assertThat(cmd).contains("-f", "mpegts");
        assertThat(cmd[cmd.length - 1]).isEqualTo("tcp://127.0.0.1:30000");
    }

    @Test
    @DisplayName("buildCmd: TCP 被动模式 → -f mpegts tcp://0.0.0.0:port?listen=1")
    void buildCmd_tcpPassive() {
        String[] cmd = service.buildCmd("ffmpeg", "/tmp/a.mp4", tcpPassiveTarget());
        assertThat(cmd).contains("-f", "mpegts");
        assertThat(cmd[cmd.length - 1]).isEqualTo("tcp://0.0.0.0:30001?listen=1");
    }

    // ── validateFile ─────────────────────────────────────────────────────

    @Test
    @DisplayName("validateFile: 空路径抛异常")
    void validateFile_blank() {
        assertThatThrownBy(() -> service.validateFile(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateFile: 不存在的文件抛异常")
    void validateFile_notExist() {
        assertThatThrownBy(() -> service.validateFile("/no/such/file.mp4"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("validateFile: 越界 allowed-root 抛异常")
    void validateFile_outsideRoot(@TempDir Path tmp) throws Exception {
        Path inside = tmp.resolve("ok.mp4");
        Files.writeString(inside, "x");
        when(props.getAllowedRoot()).thenReturn(tmp.resolve("sub").toString());

        assertThatThrownBy(() -> service.validateFile(inside.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("越界");
    }

    @Test
    @DisplayName("validateFile: root 内正常文件通过")
    void validateFile_ok(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("ok.mp4");
        Files.writeString(f, "x");
        when(props.getAllowedRoot()).thenReturn(tmp.toString());
        service.validateFile(f.toString());   // 不抛即通过
    }

    // ── startPush 异常分支 ───────────────────────────────────────────────

    @Test
    @DisplayName("startPush: 无 INVITE 目标抛异常")
    void startPush_noTarget() {
        assertThatThrownBy(() -> service.startPush(null, null, null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("INVITE");
    }

    @Test
    @DisplayName("startPush: 文件不存在抛异常（TCP 目标也适用）")
    void startPush_notUdp() {
        when(props.getMediaFile()).thenReturn("/no/such.mp4");
        LabInviteTarget tcp = new LabInviteTarget("c", "u", "127.0.0.1", 30000, "1", "TCP", "Play", "k", "active");
        assertThatThrownBy(() -> service.startPush(tcp, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("startPush: 文件不存在抛异常")
    void startPush_badFile() {
        when(props.getMediaFile()).thenReturn("/no/such.mp4");
        assertThatThrownBy(() -> service.startPush(udpTarget(), null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── startPush / stop 真进程（假 ffmpeg 脚本） ─────────────────────────

    @Test
    @DisplayName("startPush+stop: 假 ffmpeg 脚本进程可启动并停止，status 反映状态")
    void startPush_thenStop(@TempDir Path tmp) throws Exception {
        // 假 ffmpeg：忽略参数，睡 30s
        Path fake = tmp.resolve("fake-ffmpeg.sh");
        Files.writeString(fake, "#!/bin/sh\nsleep 30\n");
        fake.toFile().setExecutable(true);
        Path media = tmp.resolve("v.mp4");
        Files.writeString(media, "x");

        LabMediaPushService.PushStatus s = service.startPush(udpTarget(), fake.toString(), media.toString(), null);
        assertThat(s.getState()).isEqualTo("RUNNING");
        assertThat(s.getCallId()).isEqualTo("call-1");
        assertThat(service.status().getState()).isEqualTo("RUNNING");

        service.stop();
        assertThat(service.status().getState()).isEqualTo("IDLE");

        // 推流起/停各发一次 SSE（前端实时同步）：clientcmd.push.started / clientcmd.push.stopped
        org.mockito.ArgumentCaptor<io.github.lunasaw.voglander.common.event.SseRelayEvent> cap =
            org.mockito.ArgumentCaptor.forClass(io.github.lunasaw.voglander.common.event.SseRelayEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(cap.capture());
        java.util.List<String> topics = cap.getAllValues().stream()
            .map(io.github.lunasaw.voglander.common.event.SseRelayEvent::getTopic)
            .collect(java.util.stream.Collectors.toList());
        assertThat(topics).contains("clientcmd.push.started", "clientcmd.push.stopped");
    }

    @Test
    @DisplayName("stopByCallId: callId 匹配才停流")
    void stopByCallId_match(@TempDir Path tmp) throws Exception {
        Path fake = tmp.resolve("fake-ffmpeg.sh");
        Files.writeString(fake, "#!/bin/sh\nsleep 30\n");
        fake.toFile().setExecutable(true);
        Path media = tmp.resolve("v.mp4");
        Files.writeString(media, "x");

        service.startPush(udpTarget(), fake.toString(), media.toString(), null);
        service.stopByCallId("other-call");                 // 不匹配，不停
        assertThat(service.status().getState()).isEqualTo("RUNNING");
        service.stopByCallId("call-1");                     // 匹配，停
        assertThat(service.status().getState()).isEqualTo("IDLE");
    }

    // ── acceptInvite 回 200 OK ───────────────────────────────────────────

    @Test
    @DisplayName("acceptInvite: ctx 有效时回 200 OK（带 Contact，返回 true）并缓存目标")
    void acceptInvite_sendsOk() {
        TransactionContextInfo ctx = org.mockito.Mockito.mock(TransactionContextInfo.class);
        // builder 链式自返回，避免 NPE
        ResponseCmd.SipResponseBuilder builder =
            org.mockito.Mockito.mock(ResponseCmd.SipResponseBuilder.class, org.mockito.Mockito.RETURNS_SELF);
        when(clientProps.getClientId()).thenReturn("34020000001320000001");
        try (MockedStatic<SipTransactionRegistry> reg = mockStatic(SipTransactionRegistry.class);
             MockedStatic<ResponseCmd> resp = mockStatic(ResponseCmd.class)) {
            reg.when(() -> SipTransactionRegistry.getContext("ctx-key-1")).thenReturn(ctx);
            resp.when(() -> ResponseCmd.response(200)).thenReturn(builder);

            boolean sent = service.acceptInvite(udpTarget());

            assertThat(sent).isTrue();
            verify(builder).send();
        }
        // 缓存目标供后续手动推流
        assertThat(service.lastTarget()).isNotNull();
        assertThat(service.lastTarget().getCallId()).isEqualTo("call-1");
    }

    @Test
    @DisplayName("acceptInvite: ctx 失效时不回包返回 false，不抛异常")
    void acceptInvite_ctxExpired() {
        try (MockedStatic<SipTransactionRegistry> reg = mockStatic(SipTransactionRegistry.class)) {
            reg.when(() -> SipTransactionRegistry.getContext("ctx-key-1")).thenReturn(null);

            boolean sent = service.acceptInvite(udpTarget());

            assertThat(sent).isFalse();
        }
    }
}
