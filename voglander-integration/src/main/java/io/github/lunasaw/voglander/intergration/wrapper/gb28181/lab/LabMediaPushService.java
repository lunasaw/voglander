package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.sip.message.Response;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.client.entity.InviteResponseEntity;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry.TransactionContextInfo;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议验证台模拟推流服务（Lab 模式专用）。
 *
 * <p>职责：
 * <ol>
 *   <li>{@link #parseTarget} 从平台 INVITE 的 SDP 解析收流目标；</li>
 *   <li>{@link #acceptInvite} 同步回 200 OK 应答（必须及时，平台 8s 超时 + 事务 32s 失效）；</li>
 *   <li>{@link #startPush} 用 ffmpeg 把视频文件按 TS-over-RTP 推到平台 ZLM 收流端口；</li>
 *   <li>{@link #stop}/{@link #stopByCallId} 停止推流（BYE 或前端触发）。</li>
 * </ol>
 *
 * <p>单流模型：一次只维护一路推流，起新流前先停旧流。仅在 {@code voglander.protocol-lab.enabled=true}
 * 时注册，生产 profile 不激活。
 *
 * @author luna
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabMediaPushService {

    private final LabPushProperties            props;
    private final VoglanderSipClientProperties clientProps;

    /** 最近一次 INVITE 目标（供手动推流）。 */
    private final AtomicReference<LabInviteTarget> lastTarget = new AtomicReference<>();

    /** 当前推流会话（单流）。 */
    private final AtomicReference<PushSession>     current    = new AtomicReference<>();

    public boolean isAutoPush() {
        return props.isAuto();
    }

    public LabInviteTarget lastTarget() {
        return lastTarget.get();
    }

    // ============================ INVITE 目标解析 ============================

    /**
     * 从 {@link ClientInviteEvent} 的 SDP 提取收流目标。SDP 为 null 时兜底默认值。
     */
    public LabInviteTarget parseTarget(ClientInviteEvent e) {
        String mediaIp = null;
        String ssrc = null;
        String transport = "UDP";
        String sessionType = "Play";
        int port = 0;

        SdpSessionDescription sdp = e.getSessionDescription();
        if (sdp instanceof GbSessionDescription gb) {
            mediaIp = gb.getAddress();
            port = gb.getPort() != null ? gb.getPort() : 0;
            ssrc = gb.getSsrc();
            if (gb.getTransport() != null) {
                transport = gb.getTransport().name();
            }
            if (gb.getSessionType() != null) {
                sessionType = gb.getSessionType().getType();
            }
        }
        return new LabInviteTarget(e.getCallId(), e.getUserId(), mediaIp, port, ssrc,
            transport, sessionType, e.getTransactionContextKey());
    }

    // ============================ 回 200 OK ============================

    /**
     * 同步回 200 OK 应答 + 缓存目标。事务失效时仅记日志，不抛异常。
     * <p>
     * 应答 SDP 用框架 {@link InviteResponseEntity#getAckPlayBody} 构造（{@code sendonly, PS/90000}），
     * 须回显 INVITE 的 {@code y=ssrc}。
     */
    public boolean acceptInvite(LabInviteTarget t) {
        lastTarget.set(t);
        try {
            TransactionContextInfo ctx = SipTransactionRegistry.getContext(t.getCtxKey());
            if (ctx == null) {
                log.warn("Lab INVITE 事务已失效，无法回 200 OK, callId={}", t.getCallId());
                return false;
            }
            String mediaIp = StringUtils.firstNonBlank(props.getMediaIp(), clientProps.getDomain());
            String sdp = InviteResponseEntity.getAckPlayBody(
                t.getUserId(), mediaIp, clientProps.getPort(),
                StringUtils.defaultIfBlank(t.getSsrc(), "0")).toString();
            ResponseCmd.sendResponse(Response.OK, sdp,
                ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader(), ctx.getOriginalEvent());
            log.info("Lab 已回 200 OK, callId={}, 推流目标={}:{} ssrc={}",
                t.getCallId(), t.getMediaIp(), t.getMediaPort(), t.getSsrc());
            return true;
        } catch (Exception ex) {
            log.error("Lab 回 200 OK 失败, callId={}", t.getCallId(), ex);
            return false;
        }
    }

    // ============================ ffmpeg 推流 ============================

    /**
     * 启动 ffmpeg 推流。target 为 null 时用最近一次 INVITE 缓存目标；ffmpeg/file 覆盖配置默认值。
     *
     * @throws IllegalStateException    无目标 / 非 UDP
     * @throws IllegalArgumentException 文件非法
     */
    public synchronized PushStatus startPush(LabInviteTarget target, String ffmpegOverride, String fileOverride) {
        LabInviteTarget t = target != null ? target : lastTarget.get();
        if (t == null) {
            throw new IllegalStateException("无 INVITE 目标，请先在平台端发起实时点播");
        }
        if (StringUtils.isBlank(t.getMediaIp()) || t.getMediaPort() <= 0) {
            throw new IllegalStateException("INVITE SDP 未解析出收流地址/端口");
        }
        if (!"UDP".equalsIgnoreCase(t.getTransport())) {
            throw new IllegalStateException("当前仅支持 UDP 推流，平台请用 streamMode=UDP 点播");
        }

        String ffmpeg = StringUtils.firstNonBlank(ffmpegOverride, props.getFfmpegPath());
        String file = StringUtils.firstNonBlank(fileOverride, props.getMediaFile());
        validateFile(file);

        stopInternal();
        String[] cmd = buildCmd(ffmpeg, file, t);
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            Process p = pb.start();
            PushSession s = new PushSession(p, t, cmd, System.currentTimeMillis());
            current.set(s);
            drainLogAsync(s);
            log.info("Lab ffmpeg 推流启动, callId={}, cmd={}", t.getCallId(), String.join(" ", cmd));
            return s.toStatus();
        } catch (Exception ex) {
            log.error("Lab ffmpeg 启动失败", ex);
            throw new IllegalStateException("ffmpeg 启动失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 构造 ffmpeg 命令数组（包级可见供单测）。
     * <p>
     * TS-over-RTP（{@code -f rtp_mpegts}）：ffmpeg 无 GB28181 PS/RTP muxer，靠 ZLM 单端口自动探测 PS/TS。
     */
    String[] buildCmd(String ffmpeg, String file, LabInviteTarget t) {
        List<String> c = new ArrayList<>();
        c.add(ffmpeg);
        c.add("-re");
        if (props.isLoop()) {
            c.add("-stream_loop");
            c.add("-1");
        }
        c.add("-i");
        c.add(file);
        c.add("-an");
        if (props.isTranscode()) {
            c.add("-c:v");
            c.add("libx264");
            c.add("-preset");
            c.add("ultrafast");
            c.add("-tune");
            c.add("zerolatency");
            c.add("-bsf:v");
            c.add("h264_mp4toannexb");
        } else {
            c.add("-c:v");
            c.add("copy");
        }
        c.add("-f");
        c.add("rtp_mpegts");
        String ssrc = StringUtils.isNotBlank(t.getSsrc()) ? ("&ssrc=" + t.getSsrc()) : "";
        c.add("rtp://" + t.getMediaIp() + ":" + t.getMediaPort() + "?pkt_size=1316" + ssrc);
        return c.toArray(new String[0]);
    }

    /**
     * 文件校验（包级可见供单测）：路径穿越 + 存在/可读。
     */
    void validateFile(String file) {
        if (StringUtils.isBlank(file)) {
            throw new IllegalArgumentException("未指定推流文件路径");
        }
        Path real = Path.of(file).toAbsolutePath().normalize();
        if (StringUtils.isNotBlank(props.getAllowedRoot())) {
            Path root = Path.of(props.getAllowedRoot()).toAbsolutePath().normalize();
            if (!real.startsWith(root)) {
                throw new IllegalArgumentException("文件路径越界，须位于 " + root);
            }
        }
        File f = real.toFile();
        if (!f.isFile() || !f.canRead()) {
            throw new IllegalArgumentException("文件不存在或不可读: " + real);
        }
    }

    public synchronized void stopByCallId(String callId) {
        PushSession s = current.get();
        if (s != null && StringUtils.equals(s.getTarget().getCallId(), callId)) {
            stopInternal();
        }
    }

    public synchronized PushStatus stop() {
        stopInternal();
        return PushStatus.idle();
    }

    private void stopInternal() {
        PushSession s = current.getAndSet(null);
        if (s == null || s.getProcess() == null) {
            return;
        }
        Process p = s.getProcess();
        p.destroy();
        try {
            if (!p.waitFor(2, TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
        }
        log.info("Lab ffmpeg 推流停止, callId={}", s.getTarget().getCallId());
    }

    /**
     * 当前推流状态。进程已退出时反映为 IDLE。
     */
    public synchronized PushStatus status() {
        PushSession s = current.get();
        if (s == null) {
            return PushStatus.idle();
        }
        if (s.getProcess() != null && !s.getProcess().isAlive()) {
            // 进程已退出（推完/崩溃）→ 视为空闲，清理引用
            current.compareAndSet(s, null);
            return PushStatus.idle();
        }
        return s.toStatus();
    }

    /**
     * 独立 daemon 线程消费 ffmpeg 输出（合并 stderr），保留最近若干行供状态回显，
     * 防止子进程输出缓冲区写满阻塞。
     */
    private void drainLogAsync(PushSession s) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                new InputStreamReader(s.getProcess().getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    s.appendLog(line);
                }
            } catch (Exception ignored) {
                // 进程结束/流关闭，正常退出
            }
        }, "lab-ffmpeg-log-" + s.getTarget().getCallId());
        t.setDaemon(true);
        t.start();
    }

    @PreDestroy
    public void shutdown() {
        stopInternal();
    }

    // ============================ 内部类 ============================

    /** 推流会话（持有进程 + 目标 + 命令 + 日志环形缓冲）。 */
    static class PushSession {
        private static final int     MAX_LOG_LINES = 30;
        private final Process        process;
        private final LabInviteTarget target;
        private final String[]       cmd;
        private final long           startMs;
        private final Deque<String>  logBuf = new ArrayDeque<>(MAX_LOG_LINES);

        PushSession(Process process, LabInviteTarget target, String[] cmd, long startMs) {
            this.process = process;
            this.target = target;
            this.cmd = cmd;
            this.startMs = startMs;
        }

        Process getProcess() {
            return process;
        }

        LabInviteTarget getTarget() {
            return target;
        }

        synchronized void appendLog(String line) {
            if (logBuf.size() >= MAX_LOG_LINES) {
                logBuf.pollFirst();
            }
            logBuf.offerLast(line);
        }

        synchronized String lastLog() {
            return String.join("\n", logBuf);
        }

        PushStatus toStatus() {
            boolean alive = process != null && process.isAlive();
            PushStatus s = new PushStatus();
            s.setState(alive ? "RUNNING" : "STOPPED");
            s.setCallId(target.getCallId());
            s.setMediaIp(target.getMediaIp());
            s.setMediaPort(target.getMediaPort());
            s.setSsrc(target.getSsrc());
            s.setCmd(String.join(" ", cmd));
            s.setStartMs(startMs);
            s.setLastLog(lastLog());
            return s;
        }
    }

    /** 推流状态视图（Controller 直接序列化返回）。 */
    @Data
    public static class PushStatus {
        /** IDLE / RUNNING / STOPPED / FAILED。 */
        private String  state;
        private String  callId;
        private String  mediaIp;
        private int     mediaPort;
        private String  ssrc;
        private String  cmd;
        private long    startMs;
        private String  lastLog;

        public static PushStatus idle() {
            PushStatus s = new PushStatus();
            s.setState("IDLE");
            return s;
        }
    }
}
