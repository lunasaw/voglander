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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.sip.header.ContactHeader;
import javax.sip.message.Response;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.client.entity.InviteResponseEntity;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry.TransactionContextInfo;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.entity.rtp.CloseSendRtpReq;
import io.github.lunasaw.zlm.entity.rtp.StartSendRtpReq;
import io.github.lunasaw.zlm.entity.rtp.StartSendRtpResult;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import io.github.lunasaw.voglander.common.event.StreamReadyEvent;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import jakarta.annotation.PostConstruct;
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
    private final ApplicationEventPublisher    eventPublisher;

    /** 最近一次 INVITE 目标（供手动推流）。 */
    private final AtomicReference<LabInviteTarget> lastTarget = new AtomicReference<>();

    /** 当前推流会话（单流）。 */
    private final AtomicReference<PushSession> current       = new AtomicReference<>();

    /** 运行时 zlmMode（前端可改，不持久化）。 */
    private final AtomicBoolean                zlmModeRuntime = new AtomicBoolean(true);

    /**
     * 待处理的流上线等待队列：streamId → CompletableFuture<Void>。
     * 当 ffmpeg 推流到 ZLM 后，等待 {@link StreamReadyEvent} 触发 {@code startSendRtp}。
     */
    private final Map<String, CompletableFuture<Void>> pendingStreamReadyFutures = new ConcurrentHashMap<>();

    @PostConstruct
    void initRuntime() {
        zlmModeRuntime.set(props.isZlmMode());
    }

    public boolean isZlmModeRuntime() {
        return zlmModeRuntime.get();
    }

    public void setZlmModeRuntime(boolean v) {
        zlmModeRuntime.set(v);
    }

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

        String tcpSetup = null;
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
            tcpSetup = gb.getTcpSetup();
        }
        return new LabInviteTarget(e.getCallId(), e.getUserId(), mediaIp, port, ssrc,
            transport, sessionType, e.getTransactionContextKey(), tcpSetup);
    }

    // ============================ 回 200 OK ============================

    /**
     * 同步回 200 OK 应答 + 缓存目标。事务失效时仅记日志，不抛异常。
     * <p>
     * 应答 SDP 用框架 {@link InviteResponseEntity#getAckPlayBody} 构造（{@code sendonly, PS/90000}），
     * 须回显 INVITE 的 {@code y=ssrc}。
     * <p>
     * <strong>Contact 头必填</strong>：JAIN-SIP 对 INVITE 的 2xx 强制要求 Contact 头
     * （{@code IllegalTransactionStateException: Contact Header is mandatory for the OK to the INVITE}），
     * 故用设备自身 SIP URI（clientId@ip:port）构造 Contact 一并下发。
     *
     * @return true=已回 200 OK；false=事务失效/异常未回包
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
            // Contact：设备自身 SIP URI（clientId @ 本机监听 ip:port）
            ContactHeader contact = SipRequestUtils.createContactHeader(
                clientProps.getClientId(), clientProps.getDomain() + ":" + clientProps.getPort());
            ResponseCmd.response(Response.OK)
                .requestEvent(ctx.getOriginalEvent())
                .content(sdp)
                .contentType(ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader())
                .header(contact)
                .send();
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
     * @throws IllegalStateException    无目标 / SDP 缺收流地址
     * @throws IllegalArgumentException 文件非法
     */
    public synchronized PushStatus startPush(LabInviteTarget target, String ffmpegOverride, String fileOverride, Boolean zlmModeOverride) {
        LabInviteTarget t = target != null ? target : lastTarget.get();
        if (t == null) {
            throw new IllegalStateException("无 INVITE 目标，请先在平台端发起实时点播");
        }
        if (StringUtils.isBlank(t.getMediaIp()) || t.getMediaPort() <= 0) {
            throw new IllegalStateException("INVITE SDP 未解析出收流地址/端口");
        }

        String ffmpeg = StringUtils.firstNonBlank(ffmpegOverride, props.getFfmpegPath());
        String file = StringUtils.firstNonBlank(fileOverride, props.getMediaFile());
        validateFile(file);

        stopInternal();
        boolean useZlm = zlmModeOverride != null ? zlmModeOverride : zlmModeRuntime.get();
        if (useZlm) {
            return startPushViaZlm(t, ffmpeg, file);
        }
        String[] cmd = buildCmd(ffmpeg, file, t);
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            Process p = pb.start();
            PushSession s = new PushSession(p, t, cmd, System.currentTimeMillis());
            current.set(s);
            drainLogAsync(s);
            log.info("Lab ffmpeg 推流启动, callId={}, cmd={}", t.getCallId(), String.join(" ", cmd));
            publishPushStarted(s);
            return s.toStatus();
        } catch (Exception ex) {
            log.error("Lab ffmpeg 启动失败", ex);
            publishPushFailed(t, ex.getMessage());
            throw new IllegalStateException("ffmpeg 启动失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * ZLM 中继推流：ffmpeg → 本地ZLM RTMP，再由 ZLM {@code startSendRtp} 推 RTP/PS 到平台。
     */
    private PushStatus startPushViaZlm(LabInviteTarget t, String ffmpeg, String file) {
        String[] cmd = buildRtmpCmd(ffmpeg, file);
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            PushSession s = new PushSession(p, t, cmd, System.currentTimeMillis());
            current.set(s);
            drainLogAsync(s);
            startZlmRtpAsync(s, t);
            log.info("Lab ZLM中继推流启动(ffmpeg→ZLM→RTP), callId={}, cmd={}", t.getCallId(), String.join(" ", cmd));
            publishPushStarted(s);
            return s.toStatus();
        } catch (Exception ex) {
            log.error("Lab ZLM中继推流启动失败", ex);
            publishPushFailed(t, ex.getMessage());
            throw new IllegalStateException("ZLM中继推流启动失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 等待 ZLM 收到 RTMP 流后，异步调用 {@code startSendRtp} 推 RTP 到平台。
     * <p>
     * <strong>改进：基于事件驱动</strong>，监听 {@link StreamReadyEvent} 而非固定延迟。
     * 超时保护 10s，避免 Hook 丢失时永久挂起。
     * </p>
     */
    private void startZlmRtpAsync(PushSession s, LabInviteTarget t) {
        String streamId = props.getZlmStream();
        CompletableFuture<Void> future = new CompletableFuture<>();
        pendingStreamReadyFutures.put(streamId, future);

        Thread th = new Thread(() -> {
            try {
                /* 等待 StreamReadyEvent 触发，超时 10s */
                future.get(10, TimeUnit.SECONDS);

                if (!s.getProcess().isAlive()) {
                    log.warn("Lab ffmpeg 进程已退出，取消 startSendRtp, callId={}", t.getCallId());
                    return;
                }

                StartSendRtpReq req = new StartSendRtpReq();
                req.setApp(props.getZlmApp());
                req.setStream(streamId);
                req.setDstUrl(t.getMediaIp());
                req.setDstPort(t.getMediaPort());
                req.setUdp(!"TCP".equalsIgnoreCase(t.getTransport()));
                if (StringUtils.isNotBlank(t.getSsrc())) {
                    req.setSsrc(Integer.parseInt(t.getSsrc()));
                }
                req.setUsePs(1);
                StartSendRtpResult result = ZlmRestService.startSendRtp(
                    props.getZlmHost(), props.getZlmSecret(), req);
                if (result != null && "0".equals(result.getCode())) {
                    s.setZlmSsrc(String.valueOf(req.getSsrc()));
                    log.info("Lab ZLM RTP推流成功, callId={}, localPort={}", t.getCallId(), result.getLocalPort());
                } else {
                    String code = result != null ? result.getCode() : "null";
                    log.error("Lab ZLM startSendRtp失败, callId={}, code={}", t.getCallId(), code);
                    publishPushFailed(t, "ZLM startSendRtp失败: " + code);
                }
            } catch (java.util.concurrent.TimeoutException e) {
                log.error("Lab 等待流上线超时(10s), callId={}, stream={}", t.getCallId(), streamId);
                publishPushFailed(t, "等待ZLM流上线超时");
            } catch (Exception e) {
                log.error("Lab ZLM RTP推流异常, callId={}", t.getCallId(), e);
                publishPushFailed(t, e.getMessage());
            } finally {
                pendingStreamReadyFutures.remove(streamId);
            }
        }, "lab-zlm-rtp-" + t.getCallId());
        th.setDaemon(true);
        th.start();
    }

    /**
     * 监听 {@link StreamReadyEvent}，触发待处理的 {@code startSendRtp}。
     * <p>
     * 当 ZLM Hook {@code onStreamChanged(regist=true)} 触发流上线事件时，
     * 完成对应的 {@link CompletableFuture}，解除 {@link #startZlmRtpAsync} 的等待。
     * </p>
     */
    @Async("sipNotifierExecutor")
    @EventListener
    public void onStreamReady(StreamReadyEvent event) {
        String streamId = event.getStreamId();
        CompletableFuture<Void> future = pendingStreamReadyFutures.get(streamId);
        if (future != null && !future.isDone()) {
            log.info("Lab 收到流上线事件，触发 startSendRtp, streamId={}", streamId);
            future.complete(null);
        }
    }

    /**
     * 构造 ffmpeg → 本地ZLM RTMP 命令（ZLM中继模式）。
     * 格式：{@code ffmpeg -re [-stream_loop -1] -i file -an -c:v [copy|libx264] -f flv rtmp://zlmIp:rtmpPort/app/stream}
     */
    String[] buildRtmpCmd(String ffmpeg, String file) {
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
            c.add("-c:v"); c.add("libx264");
            c.add("-preset"); c.add("ultrafast");
            c.add("-tune"); c.add("zerolatency");
            c.add("-bsf:v"); c.add("h264_mp4toannexb");
        } else {
            c.add("-c:v"); c.add("copy");
        }
        c.add("-f"); c.add("flv");
        /* 从 http://ip:port 或 http://ip 中提取裸 IP */
        String zlmIp = props.getZlmHost().replaceFirst("https?://", "").replaceAll("[:/].*", "");
        c.add("rtmp://" + zlmIp + ":" + props.getZlmRtmpPort()
            + "/" + props.getZlmApp() + "/" + props.getZlmStream());
        return c.toArray(new String[0]);
    }

    /**
     * 构造 ffmpeg 命令数组（包级可见供单测）。
     * <ul>
     *   <li>UDP：{@code -f rtp_mpegts rtp://ip:port?pkt_size=1316[&ssrc=x]}</li>
     *   <li>TCP 主动（a=setup:active）：{@code -f mpegts tcp://ip:port} — 设备主动连平台</li>
     *   <li>TCP 被动（a=setup:passive）：{@code -f mpegts tcp://0.0.0.0:port?listen=1} — 设备监听等平台连</li>
     * </ul>
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
        if ("TCP".equalsIgnoreCase(t.getTransport())) {
            c.add("mpegts");
            if ("passive".equalsIgnoreCase(t.getTcpSetup())) {
                /* 被动模式：设备监听，等平台发起连接 */
                c.add("tcp://0.0.0.0:" + t.getMediaPort() + "?listen=1");
            } else {
                /* 主动模式（active 或未指定）：设备主动连平台 */
                c.add("tcp://" + t.getMediaIp() + ":" + t.getMediaPort());
            }
        } else {
            c.add("rtp_mpegts");
            String ssrc = StringUtils.isNotBlank(t.getSsrc()) ? ("&ssrc=" + t.getSsrc()) : "";
            c.add("rtp://" + t.getMediaIp() + ":" + t.getMediaPort() + "?pkt_size=1316" + ssrc);
        }
        return c.toArray(new String[0]);
    }

    /**
     * 文件校验（包级可见供单测）：智能路径解析 + 路径穿越 + 存在/可读。
     * <p>
     * 解析策略：
     * <ol>
     *   <li>若配置路径本身是绝对路径且文件存在 → 直接使用；</li>
     *   <li>否则尝试相对当前工作目录解析；</li>
     *   <li>都找不到 → 打印详细日志并抛异常。</li>
     * </ol>
     */
    void validateFile(String file) {
        if (StringUtils.isBlank(file)) {
            throw new IllegalArgumentException("未指定推流文件路径");
        }

        Path candidatePath = null;
        File candidateFile = null;

        /* 1. 尝试：配置路径本身作为绝对路径 */
        Path asAbsolute = Path.of(file).isAbsolute()
            ? Path.of(file).normalize()
            : null;
        if (asAbsolute != null) {
            candidateFile = asAbsolute.toFile();
            if (candidateFile.isFile() && candidateFile.canRead()) {
                candidatePath = asAbsolute;
                log.debug("文件校验通过(绝对路径): {}", candidatePath);
            }
        }

        /* 2. 尝试：相对当前工作目录 */
        if (candidatePath == null) {
            Path asRelative = Path.of(file).toAbsolutePath().normalize();
            candidateFile = asRelative.toFile();
            if (candidateFile.isFile() && candidateFile.canRead()) {
                candidatePath = asRelative;
                log.debug("文件校验通过(相对路径): {} → {}", file, candidatePath);
            }
        }

        /* 3. 都找不到 → 打印详细日志 + 抛异常 */
        if (candidatePath == null) {
            String cwd = System.getProperty("user.dir");
            String attemptedAbsolute = asAbsolute != null ? asAbsolute.toString() : "N/A";
            String attemptedRelative = Path.of(file).toAbsolutePath().normalize().toString();
            log.error("文件校验失败 - 配置路径: {}, 当前工作目录: {}, 尝试绝对路径: {}, 尝试相对路径: {}",
                file, cwd, attemptedAbsolute, attemptedRelative);
            throw new IllegalArgumentException("文件不存在或不可读: " + file
                + " (工作目录: " + cwd + ")");
        }

        /* 4. 路径穿越检查（仅在配置了 allowedRoot 时） */
        if (StringUtils.isNotBlank(props.getAllowedRoot())) {
            Path root = Path.of(props.getAllowedRoot()).toAbsolutePath().normalize();
            if (!candidatePath.startsWith(root)) {
                throw new IllegalArgumentException("文件路径越界，须位于 " + root + "，实际: " + candidatePath);
            }
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
        /* 清理待处理的流上线等待 */
        String streamId = props.getZlmStream();
        CompletableFuture<Void> future = pendingStreamReadyFutures.remove(streamId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            log.debug("Lab 取消流上线等待, streamId={}", streamId);
        }
        /* ZLM 中继模式：先停 ZLM RTP 推流 */
        if (StringUtils.isNotBlank(s.getZlmSsrc())) {
            try {
                CloseSendRtpReq req = new CloseSendRtpReq();
                req.setApp(props.getZlmApp());
                req.setStream(props.getZlmStream());
                req.setSsrc(s.getZlmSsrc());
                ZlmRestService.stopSendRtp(props.getZlmHost(), props.getZlmSecret(), req);
            } catch (Exception e) {
                log.warn("Lab ZLM stopSendRtp异常, callId={}", s.getTarget().getCallId(), e);
            }
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
        publishPushStopped(s.getTarget().getCallId());
    }

    // ============================ 推流 SSE（前端实时同步起停） ============================

    /**
     * 推 SSE {@code clientcmd.push.*}，经 {@code SseRelayListener} 中继到 {@code SseEventBus}。
     * 自动推流（{@code LabInviteListener}）与手动推流（{@code /push/start}）走同一入口，
     * 前端无论哪条路径都能实时看到推流起停。{@code eventPublisher} 容错为 null（防纯构造单测）。
     */
    private void publishPushStarted(PushSession s) {
        if (eventPublisher == null) {
            return;
        }
        LabInviteTarget t = s.getTarget();
        java.util.Map<String, Object> d = new java.util.HashMap<>();
        d.put("callId", t.getCallId());
        d.put("mediaIp", t.getMediaIp());
        d.put("mediaPort", t.getMediaPort());
        d.put("ssrc", t.getSsrc());
        d.put("cmd", String.join(" ", s.cmd));
        d.put("ts", System.currentTimeMillis());
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.push.started", d));
    }

    private void publishPushStopped(String callId) {
        if (eventPublisher == null) {
            return;
        }
        java.util.Map<String, Object> d = new java.util.HashMap<>();
        d.put("callId", callId);
        d.put("ts", System.currentTimeMillis());
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.push.stopped", d));
    }

    private void publishPushFailed(LabInviteTarget t, String error) {
        if (eventPublisher == null) {
            return;
        }
        java.util.Map<String, Object> d = new java.util.HashMap<>();
        d.put("callId", t != null ? t.getCallId() : null);
        d.put("error", error);
        d.put("ts", System.currentTimeMillis());
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.push.failed", d));
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
        /** ZLM 中继模式下，ZLM startSendRtp 使用的 ssrc（停流时需要）。 */
        private volatile String      zlmSsrc;

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

        String getZlmSsrc() {
            return zlmSsrc;
        }

        void setZlmSsrc(String ssrc) {
            this.zlmSsrc = ssrc;
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
