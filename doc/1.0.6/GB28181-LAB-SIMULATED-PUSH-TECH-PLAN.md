# GB28181 协议验证台 — 模拟设备「ffmpeg 模拟推流」技术方案

> 目标：在平台端发起**实时点播（INVITE Play）**后，模拟设备（Lab Client）按 GB28181 流程
> **回 200 OK 应答**，并**用 ffmpeg 把指定视频文件按 PS/RTP 推到平台 ZLM 收流端口**，
> 让 ZLM 触发 `on_stream_changed` → 平台首播 future 唤醒 → 拉到 `flv/hls/rtmp` 播放地址，
> **完成 SIP 信令 + 媒体流的整条闭环**。
>
> ffmpeg 地址与待推视频文件路径：**配置可指定 + 前端可动态修改**。

---

## 0. TL;DR

| 维度 | 结论 |
|------|------|
| 现状最大缺口 | **Lab 收到 INVITE 后从不回 200 OK**（`InviteRequestProcessor` 只发 100 Trying + 抛 `ClientInviteEvent`，200 OK 须业务层异步补发）。所以当前点播必然在 8s 后超时失败——此前只验了信令到达，从未验过媒体闭环。 |
| 本方案核心 | ①补 200 OK 应答；②`LabInviteListener` 从 INVITE SDP 解析收流目标（ip/port/ssrc/transport）；③`LabMediaPushService` 拉起 ffmpeg 推 RTP；④`ClientByeEvent` 收到 BYE 停 ffmpeg。 |
| ffmpeg 封装 | ffmpeg **无 GB28181 PS/RTP 原生 muxer**，用 `-f rtp_mpegts`（TS-over-RTP）。ZLM `openRtpServer` 对单端口流**自动探测 PS/TS**，故 TS-over-RTP 可被正常解码。**这是务实折中，非字节级 PS 仿真**（见 §7）。 |
| 推流触发 | **自动**（推荐，默认）：收到 INVITE 即起 ffmpeg，稳进 8s 窗口；**手动**：前端「模拟推流」按钮推到最近一次 INVITE 缓存的目标（须在平台 8s 超时窗内点击）。两者都实现。 |
| 配置 | `voglander.protocol-lab.push.{ffmpeg-path, media-file, auto, loop, transcode, ssrc-mode}`，dev/lab profile 提供默认值。 |
| 前端动态改 | 「模拟推流」面板：ffmpeg 路径 + 文件路径输入框 + 自动推流开关 + 启停按钮 + 状态展示，调 `/lab/client/push/*`。 |
| 改动模块 | `voglander-integration`（核心：Listener/Service）、`voglander-web`（Controller + Req）、`vue-vben-admin`（ClientPanel 面板）。 |
| 安全红线 | ffmpeg 路径/文件路径来自前端 → **命令注入 + 路径穿越**风险；用 `ProcessBuilder` 数组传参（绝不拼 shell 串）+ 文件白名单根目录校验 + lab profile 才注册。 |
| 可行性核查 | ✅ **已核查通过**（见 §0.5）：voglander 现状缺口属实、gb28181/sip-common **1.8.1** 框架 API 全部坐实、签名吻合。3 处类型细节需注意（SDP 父类返回值、Integer port、StringBuffer 返回），方案代码已自洽处理。 |

---

## 0.5 可行性核查结论（实现前必读）

> 本节为方案落地前对 voglander 现状代码 + gb28181/sip-common **1.8.1** 框架 jar 的逐项核查记录，确认所有依赖真实存在。

### 现状缺口 — 全部属实

| 断言 | 核查 | 证据 |
|------|------|------|
| `LabInviteListener` 只推 SSE、不回 200 OK、丢 SDP | ✅ | `intergration/.../gb28181/lab/LabInviteListener.java` |
| `InviteRequestProcessor` 只发 100 Trying + publish 事件 | ✅ | 框架源码，200 OK 交业务层异步补发 |
| `startLive` future.get(8s) 超时→cleanupFailed | ✅ | `MediaPlayServiceImpl.java:55` 常量 `FUTURE_TIMEOUT_SEC = 8` |
| ZLM hook→StreamReadyEvent→completeFuture 唤醒 | ✅ | `LiveStreamEventListener.onStreamReady` |
| `SseRelayEvent(topic, Map)` 构造 + 前端订阅 `clientcmd.*` 前缀域 | ✅ | `common/.../event/SseRelayEvent.java`，前缀域匹配生效 |
| `VoglanderSipClientProperties.getDomain()/getPort()` | ✅ | `domain` 默认 `127.0.0.1`，`port` 默认 `5061`（int） |
| lab 组件条件注解 `voglander.protocol-lab.enabled=true` | ✅ | 现有 `LabInviteListener`/`LabClientController` 通用模式 |
| integration 包名拼写 `intergration`（项目历史错拼） | ✅ | 新增类**必须沿用**此拼写 |

### 框架 API — 1.8.1 全部存在，签名如下（实现按此为准）

| API | 确认签名 | 注意 |
|-----|----------|------|
| `ClientInviteEvent.getSessionDescription()` | 返回 **`SdpSessionDescription`**（父类） | ⚠️ 非 `GbSessionDescription`，须 `instanceof` 向下转型取 GB 私有字段；运行时实际就是 `GbSessionDescription`，转型成功 |
| `ClientInviteEvent.getCallId()/getUserId()/getTransactionContextKey()` | 均返回 `String` | — |
| `GbSessionDescription.getPort()` | 返回 **`Integer`**（可能 null） | ⚠️ 须 null 兜底，方案已写 `!= null ? : 0` |
| `GbSessionDescription.getAddress()/getSsrc()` | `String` | — |
| `GbSessionDescription.getTransport()` | `TransportEnum`，`.name()`→"UDP"/"TCP" | — |
| `GbSessionDescription.getSessionType()` | `InviteSessionNameEnum`，`.getType()`→"Play"/"PlayBack"/"Talk"/"Download" | — |
| `InviteResponseEntity.getAckPlayBody(String userId, String mediaIp, int localPort, String ssrc)` | 返回 **`StringBuffer`** | ⚠️ `localPort` 是原始 **`int`**（非 Integer）；返回值须 `.toString()`；`clientProps.getPort()` 正好是 int，直配 |
| `ResponseCmd.sendResponse(int, String content, ContentTypeHeader, RequestEvent)` | ✅ 4 参重载真实存在 | ⚠️ **无** `Event` 基类重载，只接 `RequestEvent`；`ctx.getOriginalEvent()` 正返回 `RequestEvent`，精确匹配 |
| `SipTransactionRegistry.getContext(String)` | 返回 `TransactionContextInfo` | `.getOriginalEvent()`→`RequestEvent`、`.getContextKey()`→`String` 均存在 |
| `ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader()` | 返回 `ContentTypeHeader`（带缓存） | — |
| `ClientByeEvent.getCallId()` | `String`；构造 `(source, callId, int statusCode)` | ✅ 存在 |

### 落地前 3 个必记类型约束

1. `getSessionDescription()` 静态类型是 `SdpSessionDescription`——`parseTarget` 用 `instanceof GbSessionDescription gb` 转型（方案 B5 已正确写法）。
2. `getPort()` 是 `Integer`——null 兜底为 0（方案 B5 已处理）。
3. `getAckPlayBody` 第 3 参 `localPort` 是 `int`、返回 `StringBuffer`——传 `clientProps.getPort()` + `.toString()`（方案 B5 已处理）。

---

## 1. 现状链路与缺口定位

### 1.1 平台端实时点播（已实现，`MediaPlayServiceImpl.startLive`）

```
前端 /live/start
  └─ MediaPlayServiceImpl.startLive(deviceId, channelId)
       1. 选 ZLM 节点（LB）
       2. resolveMediaIp(node)            → sdpIp（自环=127.0.0.1）
       3. ZlmRestService.openRtpServer    → 拿到收流端口 rtpPort（port=0 自动分配）
       4. writePlaceholder(INVITING)
       5. registerFuture(streamId, future)
       6. voglanderServerMediaCommand.inviteRealTimePlay(deviceId, sdpIp, rtpPort, streamMode)
          └─ envelope gb28181.Invite.Play {mediaIp, mediaPort, streamMode} → 框架发 INVITE（含 SDP y=ssrc）
       7. future.get(8s)                  ← 等 ZLM on_stream_changed
       8. 超时 → cleanupFailed(closeRtpServer) + 抛 LIVE_INVITE_TIMEOUT
       9. 成功 → fetchPlayUrls → 写 ACTIVE 会话 → 返回 flv/hls/rtmp
```

future 由谁唤醒：ZLM `on_stream_changed` hook → `StreamReadyEvent` → `LiveStreamEventListener.onStreamReady` → `liveStreamRegistry.completeFuture(streamId)`（`LiveStreamEventListener.java:47`）。
**即：只要 ffmpeg 把流喂进 ZLM 那个 rtpPort，ZLM 自然 hook 回调，平台 future 就唤醒。**

### 1.2 设备端 INVITE 处理（框架，`InviteRequestProcessor`）

```java
// sip-gb28181/gb28181-client .../request/invite/InviteRequestProcessor.java
public void process(RequestEvent evt) {
    if (!clientDeviceSupplier.checkDevice(evt)) return;
    // 1. 立即回 100 Trying
    ResponseCmd.sendResponse(Response.TRYING, evt);
    // 2. 存事务上下文（key 供异步回包）
    var ctx = SipTransactionRegistry.createContext(evt, evt.getServerTransaction());
    // 3. 解析 SDP（GbSessionDescription：含 ssrc/address/port/transport/sessionType）
    GbSessionDescription sdp = sdpParser.parse(...);
    // 4. 抛事件，业务方异步取回 ctx 完成 200 OK
    publisher.publishEvent(new ClientInviteEvent(this, callId, userId, sdp, ctx.getContextKey()));
}
```

**框架不发 200 OK**，把回包责任交给业务层（`transactionContextKey` → `SipTransactionRegistry.getContext`）。

### 1.3 当前 Lab 监听器（`LabInviteListener`，**只推 SSE，不回包**）

```java
@EventListener
public void onInvite(ClientInviteEvent e) {
    Map<String,Object> d = new HashMap<>();
    d.put("callId", e.getCallId()); d.put("clientId", e.getUserId()); d.put("ts", ...);
    eventPublisher.publishEvent(new SseRelayEvent("clientcmd.invite", d));   // ← 仅此
}
```

> **结论**：`e.getSessionDescription()`（含收流 ip/port/ssrc）被直接丢弃，`transactionContextKey` 也没用。
> 这正是闭环缺的两块：**回 200 OK + 拿到推流目标**。

### 1.4 GB28181 实时点播完整时序（本方案补齐 ★ 处）

```
平台(Server,5060)                              模拟设备(Lab Client,5061)            ffmpeg            ZLM
   │  openRtpServer → rtpPort                        │                                │                │
   │ ─ INVITE (SDP: c=127.0.0.1 m=video rtpPort, y=ssrc) ─►                           │                │
   │                                          100 Trying ◄─ (框架)                     │                │
   │                                          ★解析SDP→{ip,port,ssrc,transport}        │                │
   │                          ★200 OK (SDP sendonly, y=ssrc 回显) ◄─                   │                │
   │ ─ ACK ──────────────────────────────────►(框架 ClientAck)                        │                │
   │                                          ★起 ffmpeg ──────────────► PS/TS-over-RTP ──────────────►│
   │                                                                                  │   on_stream_changed
   │ ◄──────────────── on_stream_changed hook ────────────────────────────────────────────────────────┤
   │  completeFuture → fetchPlayUrls → ACTIVE                                          │                │
   │  (前端 flv.js 播放)                                                                │                │
   │                                                                                  │                │
   │ ─ BYE (停止点播) ────────────────────────►★ClientByeEvent→停 ffmpeg              │                │
```

---

## 2. 设计

### 2.1 组件总览（新增/改造）

| 组件 | 模块 | 类型 | 职责 |
|------|------|------|------|
| `LabPushProperties` | integration | 新增 | `voglander.protocol-lab.push.*` 配置绑定 |
| `LabInviteTarget` | integration | 新增 | 不可变值对象：callId/userId/mediaIp/mediaPort/ssrc/transport/sessionType/ctxKey |
| `LabMediaPushService` | integration | 新增 | 核心：解析目标→回 200 OK→拉起/停止 ffmpeg→持有 Process 与状态；自动/手动两入口 |
| `LabInviteListener` | integration | **改造** | 收 `ClientInviteEvent`：解析 SDP→缓存目标→（auto 时）调用 service 自动推流；仍推 SSE |
| `LabByeListener` | integration | 新增 | 收 `ClientByeEvent`：按 callId 停 ffmpeg |
| `LabClientController` | web | **改造** | 增 `/push/start`、`/push/stop`、`/push/status`，`/config` 回显推流配置 |
| `LabPushStartReq` | web | 新增 | 前端可传 ffmpegPath/mediaFile/callId 覆盖配置 |
| `ClientPanel.vue` | 前端 | **改造** | 「模拟推流」折叠面板：路径输入 + 自动开关 + 启停 + 状态 |

> **不碰 sip-proxy / 框架**：200 OK 回包用框架既有的 `ResponseCmd` + `SipTransactionRegistry`，推流走外部 ffmpeg 进程，纯 voglander 侧落地。

### 2.2 回 200 OK：SDP 应答构造

设备应答 SDP 用框架现成工具 `InviteResponseEntity.getAckPlayBody(userId, mediaIp, localPort, ssrc)`：

```
v=0
o=<deviceId> 0 0 IN IP4 <deviceMediaIp>
s=Play
c=IN IP4 <deviceMediaIp>
t=0 0
m=video <devicePort> RTP/AVP 96
a=sendonly
a=rtpmap:96 PS/90000
y=<ssrc>          ← 回显平台 INVITE 里的 ssrc
f=
```

- `mediaIp`：设备侧媒体 IP。自环=`127.0.0.1`；注册到外部平台时=本机对该平台可达的 IP（取 `sip.client.domain` 或可配 `push.media-ip`）。
- `localPort`：设备「源端口」，UDP 下不强相关（设备主动推到平台端口），填 ffmpeg 本地端口或 `InviteResponseEntity` 的随机端口即可。
- `ssrc`：**必须回显** INVITE SDP 的 `y=`，否则严格平台会拒。

回包调用：

```java
var ctx = SipTransactionRegistry.getContext(target.getCtxKey());
if (ctx == null) { /* 事务已超时(32s/Timer B)，放弃 */ }
String sdp = InviteResponseEntity.getAckPlayBody(deviceId, mediaIp, localPort, ssrc).toString();
ResponseCmd.sendResponse(Response.OK, sdp,
    ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader(), ctx.getOriginalEvent());
```

> ⚠️ `SipTransactionRegistry` 事务默认 32s（Timer B）有效期。**自动推流必须在收到 INVITE 后立刻回包**，
> 不要等用户点按钮再回 200 OK——否则平台 8s 早超时、事务也可能失效。
> 设计上：**200 OK 永远在 `onInvite` 同步回**（auto/manual 都回），ffmpeg 启动才分自动/手动。

### 2.3 ffmpeg 推流命令

ZLM `openRtpServer(streamId, port)` 开的是**单端口、绑定该 streamId** 的收流口，ZLM 对该口**自动探测 PS/PS-over-RTP/TS-over-RTP**，因此用 ffmpeg 的 `rtp_mpegts`（TS-over-RTP）即可被解码。

**UDP（默认，`streamMode=UDP`）：**

```bash
ffmpeg -re [-stream_loop -1] -i <mediaFile> \
       -an -c:v libx264 -preset ultrafast -tune zerolatency -bsf:v h264_mp4toannexb \
       -f rtp_mpegts "rtp://<mediaIp>:<mediaPort>?ssrc=<ssrcDecimal>&pkt_size=1316"
```

- `-re`：按帧率实时推（不加会瞬间推完）。
- `-stream_loop -1`：循环推（`push.loop=true` 时加），便于长时间联调。
- `-c:v copy`：源已是 H264/H265 且 ZLM 支持时用 copy 省 CPU；为兼容任意文件，**默认 `libx264` 转码**（`push.transcode=true`）。`copy` 模式去掉 `-preset/-tune/-bsf`。
- `ssrc`：GB28181 `y=` 是 10 位十进制 SSRC，直接传 `?ssrc=<十进制>`。ZLM 单端口绑定 streamId，SSRC 不匹配也能收，但应正确回显。
- `pkt_size=1316`：TS 188×7，避免 IP 分片。

**TCP（`streamMode=TCP_PASSIVE/TCP_ACTIVE`）：**
ffmpeg 对 RTP-over-TCP 推流支持弱。本方案**第一期 UDP 优先**；TCP 模式在 `/push/start` 直接返回「暂不支持，请用 UDP」并记日志（见 §7）。lab 自环默认 UDP，不阻塞主目标。

### 2.4 自动 vs 手动 触发模型

| 模式 | `push.auto` | 行为 | 适用 |
|------|-------------|------|------|
| **自动**（默认 true） | `onInvite` 回 200 OK 后立即起 ffmpeg | 平台点「实时点播」→ 一气呵成闭环，无时序压力 | 联调/演示常态 |
| **手动** | `onInvite` 只回 200 OK + 缓存目标 + 推 SSE | 用户点前端「模拟推流」→ 推到最近缓存目标 | 演示「设备主动推流」这一步 |

> 手动模式坑：平台 `future.get(8s)`。用户须在点播后 **8s 内**点「模拟推流」，否则平台已超时 `cleanupFailed`（closeRtpServer），此时推流喂不进已关闭的端口。
> 缓解：①前端收到 `clientcmd.invite` SSE 后高亮「模拟推流」按钮并显示倒计时；②文档明确「手动须速点」；③推荐默认用自动模式。

### 2.5 进程与状态管理

`LabMediaPushService` 单例持有：

```java
private final AtomicReference<PushSession> current = new AtomicReference<>();
// PushSession { Process process; String callId; String streamId; LabInviteTarget target;
//               String[] cmd; long startMs; volatile String state; }  // STARTING/RUNNING/STOPPED/FAILED
```

- **单流模型**：一次只维护一路推流（lab 自环够用）。起新流前先 `stop()` 旧流。
- ffmpeg stderr 起独立 daemon 线程消费（防缓冲区写满阻塞），最后 N 行存进 `PushSession.lastLog` 供 `/push/status` 回显。
- `stop()`：`process.destroy()` → 等 2s → `destroyForcibly()`；幂等。
- `@PreDestroy`：应用关闭时杀残留 ffmpeg。
- 推流起止各发一条 SSE（`clientcmd.push.started` / `clientcmd.push.stopped` / `clientcmd.push.failed`），前端时间线可见。

---

## 3. 后端改动（可直接落地）

### B1. `LabPushProperties`（新增，integration）

```java
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 协议验证台模拟推流配置。前缀 voglander.protocol-lab.push。 */
@Data
@Component
@ConfigurationProperties(prefix = "voglander.protocol-lab.push")
public class LabPushProperties {
    /** ffmpeg 可执行文件绝对路径。 */
    private String  ffmpegPath = "ffmpeg";
    /** 待推视频文件绝对路径。 */
    private String  mediaFile;
    /** 收到 INVITE 是否自动起 ffmpeg（true=闭环无时序压力）。 */
    private boolean auto       = true;
    /** 是否循环推流（长联调）。 */
    private boolean loop       = true;
    /** true=转码 libx264（兼容任意文件）；false=copy（源须 H264/H265）。 */
    private boolean transcode  = true;
    /** 设备侧应答 SDP 的媒体 IP；留空回退 sip.client.domain。 */
    private String  mediaIp;
    /** 允许推流的文件根目录（路径穿越防护）；留空=仅校验文件存在。 */
    private String  allowedRoot;
}
```

### B2. `LabInviteTarget`（新增，integration）

```java
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/** INVITE 解析出的收流目标（不可变）。 */
@Getter @AllArgsConstructor @ToString
public class LabInviteTarget {
    private final String  callId;
    private final String  userId;     // 本端 deviceId
    private final String  mediaIp;    // 平台收流 IP（SDP c=）
    private final int     mediaPort;  // 平台收流端口（SDP m=）
    private final String  ssrc;       // SDP y=
    private final String  transport;  // UDP / TCP
    private final String  sessionType;// Play / PlayBack
    private final String  ctxKey;     // SipTransactionRegistry 上下文键
}
```

### B3. `LabInviteListener` 改造（解析目标 + 自动推流；200 OK 由 service 同步回）

```java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabInviteListener {

    private final ApplicationEventPublisher eventPublisher;
    private final LabMediaPushService       pushService;

    @EventListener
    public void onInvite(ClientInviteEvent e) {
        // 1. 解析收流目标（SDP 可能为 null，做兜底）
        LabInviteTarget target = pushService.parseTarget(e);

        // 2. 同步回 200 OK + 缓存目标（无论自动/手动都要回，否则平台拿不到应答）
        pushService.acceptInvite(target);

        // 3. 推 SSE（保留现状字段 + 增收流目标，便于前端展示/手动推流）
        Map<String, Object> d = new HashMap<>();
        d.put("callId", target.getCallId());
        d.put("clientId", target.getUserId());
        d.put("mediaIp", target.getMediaIp());
        d.put("mediaPort", target.getMediaPort());
        d.put("ssrc", target.getSsrc());
        d.put("ts", System.currentTimeMillis());
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.invite", d));

        // 4. 自动模式：立即起 ffmpeg
        if (pushService.isAutoPush()) {
            pushService.startPush(target, null, null);   // 用配置默认 ffmpeg/file
        }
    }
}
```

### B4. `LabByeListener`（新增，BYE 停流）

```java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabByeListener {
    private final LabMediaPushService pushService;

    @EventListener
    public void onBye(ClientByeEvent e) {
        log.info("Lab 收到 BYE, callId={} → 停止推流", e.getCallId());
        pushService.stopByCallId(e.getCallId());
    }
}
```

### B5. `LabMediaPushService`（新增，核心）

```java
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.client.entity.InviteResponseEntity;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabMediaPushService {

    private final LabPushProperties             props;
    private final VoglanderSipClientProperties  clientProps;

    /** 最近一次 INVITE 目标（供手动推流）。 */
    private final AtomicReference<LabInviteTarget> lastTarget = new AtomicReference<>();
    /** 当前推流会话（单流）。 */
    private final AtomicReference<PushSession>     current    = new AtomicReference<>();

    public boolean isAutoPush() { return props.isAuto(); }

    // ---------- INVITE 目标解析 ----------
    // 注意：getSessionDescription() 静态返回类型是父类 SdpSessionDescription，
    // 运行时实例为 GbSessionDescription，须 instanceof 向下转型才能取 ssrc/transport/sessionType 等 GB 私有字段。
    public LabInviteTarget parseTarget(ClientInviteEvent e) {
        String mediaIp = null, ssrc = null, transport = "UDP", sessionType = "Play";
        int port = 0;
        if (e.getSessionDescription() instanceof GbSessionDescription gb) {
            mediaIp     = gb.getAddress();
            port        = gb.getPort() != null ? gb.getPort() : 0;
            ssrc        = gb.getSsrc();
            transport   = gb.getTransport() != null ? gb.getTransport().name() : "UDP";
            sessionType = gb.getSessionType() != null ? gb.getSessionType().getType() : "Play";
        }
        return new LabInviteTarget(e.getCallId(), e.getUserId(), mediaIp, port, ssrc,
            transport, sessionType, e.getTransactionContextKey());
    }

    // ---------- 回 200 OK（同步，必须及时） ----------
    public void acceptInvite(LabInviteTarget t) {
        lastTarget.set(t);
        try {
            var ctx = SipTransactionRegistry.getContext(t.getCtxKey());
            if (ctx == null) { log.warn("Lab INVITE 事务已失效, callId={}", t.getCallId()); return; }
            String mediaIp = StringUtils.firstNonBlank(props.getMediaIp(), clientProps.getDomain());
            String sdp = InviteResponseEntity.getAckPlayBody(
                t.getUserId(), mediaIp, clientProps.getPort(),
                StringUtils.defaultString(t.getSsrc(), "0")).toString();
            ResponseCmd.sendResponse(Response.OK, sdp,
                ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader(), ctx.getOriginalEvent());
            log.info("Lab 已回 200 OK, callId={}, 推流目标={}:{} ssrc={}",
                t.getCallId(), t.getMediaIp(), t.getMediaPort(), t.getSsrc());
        } catch (Exception ex) {
            log.error("Lab 回 200 OK 失败, callId={}", t.getCallId(), ex);
        }
    }

    // ---------- 起 ffmpeg ----------
    public synchronized PushStatus startPush(LabInviteTarget target, String ffmpegOverride, String fileOverride) {
        if (target == null) target = lastTarget.get();
        if (target == null) throw new IllegalStateException("无 INVITE 目标，请先在平台端发起实时点播");
        if (StringUtils.isBlank(target.getMediaIp()) || target.getMediaPort() <= 0)
            throw new IllegalStateException("INVITE SDP 未解析出收流地址/端口");
        if (!"UDP".equalsIgnoreCase(target.getTransport()))
            throw new IllegalStateException("当前仅支持 UDP 推流，平台请用 streamMode=UDP 点播");

        String ffmpeg = StringUtils.firstNonBlank(ffmpegOverride, props.getFfmpegPath());
        String file   = StringUtils.firstNonBlank(fileOverride, props.getMediaFile());
        validateFile(file);

        stopInternal();  // 单流：先停旧
        String[] cmd = buildCmd(ffmpeg, file, target);
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            Process p = pb.start();
            PushSession s = new PushSession(p, target, cmd, System.currentTimeMillis());
            current.set(s);
            drainLogAsync(s);          // 独立线程吞 stderr
            log.info("Lab ffmpeg 推流启动, callId={}, cmd={}", target.getCallId(), String.join(" ", cmd));
            return s.toStatus();
        } catch (Exception ex) {
            log.error("Lab ffmpeg 启动失败", ex);
            throw new IllegalStateException("ffmpeg 启动失败: " + ex.getMessage(), ex);
        }
    }

    private String[] buildCmd(String ffmpeg, String file, LabInviteTarget t) {
        List<String> c = new ArrayList<>();
        c.add(ffmpeg); c.add("-re");
        if (props.isLoop()) { c.add("-stream_loop"); c.add("-1"); }
        c.add("-i"); c.add(file);
        c.add("-an");
        if (props.isTranscode()) {
            c.add("-c:v"); c.add("libx264"); c.add("-preset"); c.add("ultrafast");
            c.add("-tune"); c.add("zerolatency"); c.add("-bsf:v"); c.add("h264_mp4toannexb");
        } else {
            c.add("-c:v"); c.add("copy");
        }
        c.add("-f"); c.add("rtp_mpegts");
        String ssrc = StringUtils.isNotBlank(t.getSsrc()) ? ("&ssrc=" + t.getSsrc()) : "";
        c.add("rtp://" + t.getMediaIp() + ":" + t.getMediaPort() + "?pkt_size=1316" + ssrc);
        return c.toArray(new String[0]);
    }

    /** 路径穿越 + 存在性校验。 */
    private void validateFile(String file) {
        if (StringUtils.isBlank(file)) throw new IllegalArgumentException("未指定推流文件路径");
        Path real = Path.of(file).toAbsolutePath().normalize();
        if (StringUtils.isNotBlank(props.getAllowedRoot())) {
            Path root = Path.of(props.getAllowedRoot()).toAbsolutePath().normalize();
            if (!real.startsWith(root)) throw new IllegalArgumentException("文件路径越界，须位于 " + root);
        }
        File f = real.toFile();
        if (!f.isFile() || !f.canRead()) throw new IllegalArgumentException("文件不存在或不可读: " + real);
    }

    public synchronized void stopByCallId(String callId) {
        PushSession s = current.get();
        if (s != null && StringUtils.equals(s.target.getCallId(), callId)) stopInternal();
    }
    public synchronized PushStatus stop() { stopInternal(); return PushStatus.stopped(); }

    private void stopInternal() {
        PushSession s = current.getAndSet(null);
        if (s == null || s.process == null) return;
        s.process.destroy();
        try { if (!s.process.waitFor(2, TimeUnit.SECONDS)) s.process.destroyForcibly(); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); s.process.destroyForcibly(); }
        log.info("Lab ffmpeg 推流停止, callId={}", s.target.getCallId());
    }

    public PushStatus status() {
        PushSession s = current.get();
        return s == null ? PushStatus.idle() : s.toStatus();
    }

    private void drainLogAsync(PushSession s) { /* daemon 线程 readLine → s.lastLog 环形缓冲；进程退出更新 state */ }

    // PushSession / PushStatus 内部类略（state: IDLE/RUNNING/STOPPED/FAILED + 最近日志 + 目标摘要）
}
```

> 设计约束遵循 CLAUDE.md：`@Slf4j`、`@ConditionalOnProperty` lab 条件化、FastJSON2（状态序列化在 Controller 层）、不暴露 DO、apache `StringUtils`。

### B6. `LabClientController` 增推流端点 + `/config` 回显

```java
@PostMapping("/push/start")
@Operation(summary = "模拟推流：用 ffmpeg 把视频推到最近一次 INVITE 的收流目标")
public AjaxResult<Object> pushStart(@RequestBody(required = false) LabPushStartReq req) {
    return AjaxResult.success(labMediaPushService.startPush(
        null,                                   // 目标=最近 INVITE 缓存
        req != null ? req.getFfmpegPath() : null,
        req != null ? req.getMediaFile()  : null));
}

@PostMapping("/push/stop")
@Operation(summary = "停止模拟推流")
public AjaxResult<Object> pushStop() { return AjaxResult.success(labMediaPushService.stop()); }

@GetMapping("/push/status")
@Operation(summary = "查询当前推流状态")
public AjaxResult<Object> pushStatus() { return AjaxResult.success(labMediaPushService.status()); }
```

`/config` 追加：`pushAuto / ffmpegPath / mediaFile / pushTransport`（回显当前配置，前端表单初值）。

### B7. `LabPushStartReq`（新增，web）

```java
@Data
@Schema(description = "模拟推流启动请求（不传则用配置默认值）")
public class LabPushStartReq {
    @Schema(description = "ffmpeg 可执行文件绝对路径，覆盖配置")
    private String ffmpegPath;
    @Schema(description = "待推视频文件绝对路径，覆盖配置")
    private String mediaFile;
}
```

---

## 4. 配置

### application-lab.yml / application-dev.yml 追加

```yaml
voglander:
  protocol-lab:
    enabled: true
    device-password: "123456"
    push:
      ffmpeg-path: /usr/local/bin/ffmpeg     # macOS Homebrew 默认；按机器改
      media-file:  /Users/weidian/Movies/demo.mp4   # 待推文件绝对路径
      auto: true            # 收到 INVITE 自动起 ffmpeg（闭环无时序压力）
      loop: true            # 循环推流，便于长联调
      transcode: true       # 转码 libx264，兼容任意文件；源已 H264 可设 false 走 copy
      media-ip: 127.0.0.1   # 设备应答 SDP 媒体 IP；自环=本机
      allowed-root: /Users/weidian/Movies   # 可选：推流文件白名单根目录（路径穿越防护）
```

> 生产 profile（dev,repo,inte 无 lab）不含 `protocol-lab.push`，所有 push Bean 因 `@ConditionalOnProperty` 不注册——零生产影响。

---

## 5. API 契约（前端对齐）

| 方法 | 路径 | 入参 | 返回 |
|------|------|------|------|
| POST | `/api/v1/lab/client/push/start` | `{ffmpegPath?, mediaFile?}` | `{state, callId, mediaIp, mediaPort, ssrc, cmd, lastLog}` |
| POST | `/api/v1/lab/client/push/stop`  | — | `{state:"STOPPED"}` |
| GET  | `/api/v1/lab/client/push/status`| — | 同 start 的状态体；空闲为 `{state:"IDLE"}` |
| GET  | `/api/v1/lab/client/config`     | — | 原字段 + `pushAuto/ffmpegPath/mediaFile/pushTransport` |

新增 SSE 主题（`clientcmd.*` 前缀域，前端已订阅该域）：
`clientcmd.push.started` / `clientcmd.push.stopped` / `clientcmd.push.failed`，payload `{callId, mediaIp, mediaPort, ssrc, ts}`。
`clientcmd.invite` payload 扩展：增 `mediaIp/mediaPort/ssrc`。

> 统一 `AjaxResult{code,msg,data}`，`requestClient` 已 `responseReturn:'data'`，前端直接拿 data。

---

## 6. 前端改动（vue-vben-admin/apps/web-antd）

### 6.1 `api/protocol-lab.ts`

```ts
export namespace ProtocolLabApi {
  export interface PushStartReq { ffmpegPath?: string; mediaFile?: string; }
  export interface PushStatus {
    state: 'FAILED' | 'IDLE' | 'RUNNING' | 'STOPPED';
    callId?: string; mediaIp?: string; mediaPort?: number; ssrc?: string;
    cmd?: string; lastLog?: string;
  }
  // LabConfig 追加：pushAuto / ffmpegPath / mediaFile / pushTransport
}
export function labPushStart(data: ProtocolLabApi.PushStartReq) {
  return requestClient.post('/lab/client/push/start', data);
}
export function labPushStop()   { return requestClient.post('/lab/client/push/stop'); }
export function labPushStatus() { return requestClient.get('/lab/client/push/status'); }
```

### 6.2 `ClientPanel.vue` 「模拟推流」面板（与既有「注册信息」折叠面板同风格）

- 折叠区「模拟推流」：
  - ffmpeg 路径输入框（初值来自 `config.ffmpegPath`）
  - 视频文件路径输入框（初值 `config.mediaFile`）
  - 「自动推流」只读标记（展示 `config.pushAuto`；改 auto 走配置，前端只读提示）
  - 按钮：**模拟推流**（`labPushStart`）/ **停止推流**（`labPushStop`）
  - 状态条：订阅 `clientcmd.invite` 后高亮「模拟推流」+ **8s 倒计时**提示（手动模式必读）；`push/status` 轮询或 SSE 驱动展示 `state + lastLog`。
- i18n：`locales/langs/{zh-CN,en-US}/protocol-lab.json` 增 `protocolLab.push.*` 键。

### 6.3 时间线（`SipTimeline.vue`）

`clientcmd.invite`（已收）→ `clientcmd.push.started` → 平台 `live.ready`（右侧）→ `clientcmd.push.stopped`/`session.bye`，串成完整可视闭环。

---

## 7. 边界与坑（必读）

1. **ffmpeg 无 PS/RTP muxer**：本方案用 `rtp_mpegts`（TS-over-RTP），靠 ZLM 单端口自动探测 PS/TS 解码。**不是字节级 GB28181 PS 仿真**。若需严格 PS（如对接挑剔的第三方平台收流），需外部 PS 打包器或借 ZLM `startSendRtp`——本期不做，文档标注。
2. **TCP 模式不支持**：ffmpeg RTP-over-TCP 推流弱，`/push/start` 对非 UDP 目标直接报错。lab 自环默认 UDP，主目标不受影响。
3. **手动推流 8s 窗口**：平台 `FUTURE_TIMEOUT_SEC=8`。手动模式须在点播后 8s 内点按钮，否则平台已 `cleanupFailed` 关掉收流端口。**默认用自动模式**规避。
4. **200 OK 必须同步及时回**：`SipTransactionRegistry` 事务 32s（Timer B）失效；且平台 8s 超时。`onInvite` 内同步回 200 OK，**绝不等用户点按钮**。
5. **命令注入/路径穿越**：ffmpeg 路径、文件路径来自前端。**必须** `ProcessBuilder(String[])` 数组传参（杜绝 shell 拼接）+ `allowed-root` 白名单 + `normalize().startsWith(root)` 校验 + 文件存在/可读校验。lab profile 才注册端点（生产不可达）。
6. **进程泄漏**：`stop()` 幂等 + `@PreDestroy` 兜底杀进程 + stderr 独立线程消费（防缓冲区写满阻塞 ffmpeg）。单流模型：起新流先停旧流。
7. **SSRC 回显**：应答 SDP `y=` 必须回显 INVITE 的 ssrc；ZLM 单端口绑定 streamId，SSRC 不匹配也能收，但严格平台会校验。
8. **媒体 IP**：自环=`127.0.0.1`；注册到外部平台时设备应答 SDP 的 `c=` 须填本机对该平台可达 IP（`push.media-ip` 覆盖）。
9. **ZLM 收流确认**：闭环成功标志=ZLM `on_stream_changed` → `StreamReadyEvent` → 平台 future 唤醒。若 ffmpeg 在推但平台仍超时，查：①ZLM rtpPort 是否真的 openRtpServer 成功；②ffmpeg 目标 ip/port 是否=平台下发；③ZLM hook 是否配到 voglander（参考 1.0.5 验收：ZLM hook URL 须指向 voglander `/index/hook/*`）。
10. **依赖框架版本**：`InviteResponseEntity`/`ResponseCmd`/`SipTransactionRegistry`/`ClientInviteEvent.getSessionDescription()`/`ClientByeEvent` 均 gb28181/sip-common **1.8.1** 既有 API，已逐项核查存在（见 §0.5），无需改 sip-proxy。三点类型细节：①`getSessionDescription()` 返回父类 `SdpSessionDescription`，须向下转型；②`getPort()` 返回 `Integer`（null 兜底）；③`getAckPlayBody` 第 3 参 `int`、返回 `StringBuffer`。

---

## 8. 测试计划

| 层级 | 用例 | 方式 |
|------|------|------|
| 单元 | `LabMediaPushService.buildCmd` 各开关组合（loop/transcode/ssrc 有无）生成正确数组 | 纯 Mockito，断言 `String[]` |
| 单元 | `parseTarget`：`GbSessionDescription` 各字段填充 / SDP=null 兜底 | 纯 Mockito |
| 单元 | `validateFile`：越界 / 不存在 / 正常 三分支 | 临时文件 + tmpdir |
| 单元 | `startPush`：无目标抛异常 / 非 UDP 抛异常 / 文件非法抛异常；用假 ffmpeg 脚本（`#!/bin/sh sleep 5`）验进程起停 | Mockito + 临时可执行脚本 |
| 单元 | `LabInviteListener.onInvite`：调用 `acceptInvite`、auto=true 时调 `startPush`、推 SSE 含新字段 | `@Mock` service + 验调用 |
| 单元 | `LabByeListener`：callId 匹配才停流 | Mockito |
| 单元 | `LabClientController` push 端点：mock service 验路由与入参透传 | `@ExtendWith(MockitoExtension)` |
| 集成（可选/手动） | dev,repo,inte,lab 启动 → 平台 `/live/start` 点播 → 自动推流 → 断言返回 flv 地址、ZLM 收流、`session.invite_ok`+`live.ready` SSE 到达 | 真实 ffmpeg + ZLM，本地联调 |

> 遵循 CLAUDE.md：Controller/Service **纯 Mockito 单测**（不 `@SpringBootTest`）；不引入新测试框架；测试集中放 `voglander-web/src/test/...`。
> 假 ffmpeg 脚本避免单测依赖真实 ffmpeg/网络。

---

## 9. 落地清单

后端（voglander-integration）：
- [ ] `LabPushProperties` 新增 + `@ConfigurationProperties`
- [ ] `LabInviteTarget` 新增
- [ ] `LabMediaPushService` 新增（parseTarget / acceptInvite / startPush / stop / status / 进程管理 / @PreDestroy）
- [ ] `LabInviteListener` 改造（解析目标 + 同步回 200 OK + auto 推流 + SSE 增字段）
- [ ] `LabByeListener` 新增

后端（voglander-web）：
- [ ] `LabPushStartReq` 新增
- [ ] `LabClientController` 增 `/push/start|stop|status` + `/config` 回显
- [ ] 配置：`application-lab.yml` / `application-dev.yml` 增 `protocol-lab.push.*`

前端（vue-vben-admin/apps/web-antd）：
- [ ] `api/protocol-lab.ts` 增 push API + 类型
- [ ] `ClientPanel.vue` 「模拟推流」面板（路径输入 + 启停 + 状态 + 8s 倒计时）
- [ ] i18n `protocol-lab.json` 增 `protocolLab.push.*`
- [ ] `SipTimeline.vue` 串入 push 事件

构建提醒（来自项目记忆）：
- [ ] 改 voglander-integration 后 **`mvn clean install -DskipTests`** 全量重装（仅 `-pl integration -am` 会让 web 测试编译报「找不到兄弟模块类文件」）
- [ ] 前端构建走根目录 **`pnpm build:antd`**（turbo 先建 @core 包）

验收口径：
- [ ] dev,repo,inte,lab 启动，平台端「实时点播」→ 自动模式下 8s 内拿到 flv 地址且 flv.js 可播
- [ ] 停止点播 → BYE → ffmpeg 自动停 → `clientcmd.push.stopped` + `session.bye` SSE 到达
- [ ] 前端改 ffmpeg 路径/文件路径 → 手动「模拟推流」生效（8s 窗内）
