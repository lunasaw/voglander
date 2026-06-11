# 直播流缓存与媒体回调一致性 — 幽灵会话治理技术方案

> 版本 1.0.7 · 分支 `0608_dev` · 文档日期 2026-06-11 · **本文为实施方案，按此分阶段 TDD 落地**
> 问题定位：`/api/v1/live/start` 复用分支返回了**指向已死流的旧播放地址**，前端拿到地址播不出来。
> 根因一句话：**直播会话缓存（Redis `live:session:*`）的"建"由 ZLM 回调驱动，但"删"没有对称地跟回调走** —— 上线信号用了，下线信号被丢弃，缓存与流媒体真实状态脱节。
> 关联：`doc/1.0.5/`（直播闭环联调）、[[voglander-1.0.5-visualization-acceptance]]、[[voglander-1.0.6-protocol-lab-progress]]。

---

## 0. TL;DR

**心智模型（这是对的，方案围绕它展开）**：
1. 流是否真实存活，**唯一权威是流媒体（ZLM）**，不是我们的 DB / Redis。
2. 我们这边的 `live:session:*` 只是一份**缓存副本**，用于"多路复用秒返回"——命中缓存就直接返回播放地址，不必重新 INVITE。
3. 缓存副本要和真实流保持一致，就**必须靠流媒体回调来驱动其生命周期**：流上线→建缓存，流下线→删缓存。回调是这套机制的命脉。

**现状缺陷**：上线半边闭环了，下线半边断了。

| 信号 | ZLM 回调 | 当前处理 | 结果 |
|------|---------|---------|------|
| 流上线 `onStreamChanged(regist=true)` | ✅ 触发 | 发 `StreamReadyEvent` → 唤醒首播 future + 建缓存 | ✅ 闭环 |
| 流下线 `onStreamChanged(regist=false)` | ✅ 触发 | **只更新 `StreamProxy` 表，rtp 直播流查不到记录 → 丢弃** | ❌ 缓存不清 |
| 无人观看 `onStreamNoneReader` | ✅ 触发 | **TODO 空壳** | ❌ 不兜底 |

**后果链路**：流在 ZLM 侧死了（设备停推 / RTP 超时 / ZLM 重启 / 网络断）→ 下线回调到了但被丢弃 → `live:session:{streamId}` 这个 `status=ACTIVE` 的缓存**僵在 Redis 最多 3600s** → 下一次 `/live/start` 命中复用分支 → 返回死流的旧 `playUrls` → **前端播放失败**。

**修复策略（对称闭环 + 兜底对账）**：
1. **S1（必做）**：下线回调对称清缓存。`onStreamChanged(regist=false)` 无条件按 `stream` 发 `StreamOfflineEvent`，service 层 `liveStreamRegistry.remove()` **+ DB 标 CLOSED** + 推 `live.closed` SSE。和上线的 `StreamReadyEvent` 完全对称。
2. **S2（必做）**：GC 兜底对账。回调是 best-effort（网络抖动 / ZLM 重启会丢），GC 周期性拿 DB 里 ACTIVE 会话去 ZLM **`isMediaOnline`** 核实，**以流媒体为准**，查不到的清掉。
3. **S3（健壮性加固）**：`onStreamNoneReader` 落地 + 复用前可选轻量探活（默认不开，避免热路径加 ZLM 往返）。

改动集中在 **2 个模块**：`voglander-integration`（下线回调发事件）、`voglander-service`（监听清缓存 + GC 对账）。新增 1 个事件类于 `voglander-common`。**不改** `sip-proxy` / `zlm-starter`（无需上游重建）。

> **⚠️ 落地前必读（源码核对后修正，2026-06-11）**：原始草案在 S2 判活上有两处会直接翻车的硬伤，已在下文订正，务必按订正版实现：
> 1. **判活不能用 `getMediaInfo`**：`ZlmRestService.getMediaInfo` 把整个响应无条件 `JSON.parseObject` 成 `MediaInfo` 再包成 `ServerResponse.success(...)`，且 `MediaInfo` **没有 `online` 字段**——`resp.getData().getOnline()` 既编译不过，即便补字段，流不存在时 `getData()` 仍是非 null 空壳对象 → 判活恒为"活"，兜底失效。**改用 `isMediaOnline`**（返回 `MediaOnlineStatus`，含 `Boolean online`）或 `getMediaList`（列表非空即存活）。
> 2. **S1 必须同时标 DB CLOSED**：仅清 Redis 不动 `tb_media_session`，会让 S2 的 `getActiveSessions()`（查 DB ACTIVE）下一轮再次捞到该会话、重复 `closeStream`、重复推 SSE。S1 监听器内必须 best-effort 标 CLOSED（见 §3.2 ③）。
> 3. **多节点 reconcile 需防重**：`reconcileActiveSessions` 用全量 `getActiveSessions()`，多实例部署时同一死流会被多节点重复处理。需按本节点 serverId 过滤或加分布式锁（见 §4.3）。

---

## 1. 现状链路梳理（关键，理解后再动手）

### 1.1 复用判断到底看什么

`MediaPlayServiceImpl.startLive` 进来后，复用分支只做一件事 —— **读 Redis 一个字符串，判 status 是否 ACTIVE**：

```java
// MediaPlayServiceImpl.java:114-119
LiveSessionInfo existing = liveStreamRegistry.getSession(streamId);   // GET live:session:{streamId} + 反序列化
if (existing != null && isActive(existing.getStatus())) {             // 只判 status == 1(ACTIVE)
    liveStreamRegistry.incRef(streamId);
    liveStreamRegistry.keepAlive(streamId, KEEPALIVE_SEC);
    return buildDTO(streamId, existing);                              // 把缓存里首播那刻的 playUrls 原样吐回
}
```

`getSession` 即 `RedisLiveStreamRegistry.java:82` 的 `GET live:session:xxx`。**全程不向 ZLM 核实流是否还在**，`playUrls` 也是 `LiveSessionInfo.playUrlsJson`（首播时缓存的旧值）反序列化吐出。

> 复用本身的设计是对的（命中缓存秒返回是性能优化）。问题不在"复用"，在于"**缓存没有在流死时被清掉**"，导致复用命中了一份过期副本。

### 1.2 缓存生命周期：建了，但删的路径不全

会话缓存 key `live:session:{streamId}`，TTL **3600s**（`RedisLiveStreamRegistry.java:36`）。它被 `remove()` 清掉的路径目前只有：

```
正常关流路径（依赖前端显式 stop）：
  前端 /live/stop → decRef → 归零 → 写 live:pending_close:{id} (TTL 90s)
        ▼ (GC 每 60s 扫一次)
  LiveSessionGcService.drainPendingClose → refCount<=0 → MediaPlayService.closeStream
        ▼
  closeStream: closeRtpServer + sendBye + 标 CLOSED + remove(streamId) + 删 pending key

节点退出路径：
  ZLM onServerExited → NodeExitedEvent → LiveStreamEventListener.onNodeExited
        ▼ 遍历该节点 ACTIVE 会话 → forceClose + remove(streamId)
```

**缺失的路径** —— "流在 ZLM 侧死了，但没人正常 stop"：

- 浏览器直接关闭 / 刷新 → 前端没调 `/live/stop` → `refCount` 卡在 >0 → 永不进 pending_close。
- 设备停推、RTP 接收超时、推流网络断 → ZLM 流消失，但 voglander 这边 `refCount` 仍 >0。
- 这两种情况下，ZLM **发了 `onStreamChanged(regist=false)`**，但如 §1.3 所述被丢弃，缓存无人清。

→ 缓存僵死，最多熬到 3600s TTL 自然过期。期间所有 `/live/start` 复用全部命中死流。

### 1.3 下线回调被丢弃的精确断点

`VoglanderZlmHookServiceImpl.onStreamChanged`（`:95`）对上线/下线**都会被 ZLM 触发**，但处理逻辑只认 `StreamProxy` 表：

```java
// :110-140 —— 拿 app+stream 查 StreamProxy 表
StreamProxyDTO existingProxy = streamProxyManager.get(queryDTO);
if (existingProxy != null) {
    // 更新 onlineStatus = regist?1:0
} else {
    log.warn("流{}状态更新失败，未找到匹配的流代理记录...");   // ← rtp 直播流走这里，打个 warning 就结束
}

// :143-144 —— 只有上线发事件，下线啥也不做
if (param.isRegist()) {
    eventPublisher.publishEvent(new StreamReadyEvent(param.getStream()));
}
```

两个问题叠加：
1. **`StreamProxy` 表是给"拉流代理"用的，直播 rtp 流（INVITE 收流）不在这张表里** → 走 else 分支被忽略。
2. **即便走到这，下线分支也从不发任何事件** → `liveStreamRegistry` 永远收不到"流没了"的通知。

> 纠正一个容易误解的说法：不是"没有 StreamProxy 记录所以收不到回调"。**回调是收得到的**（ZLM 对所有流都回调，rtp 流也能在 `getMediaList` 查到），只是这个处理器**把权威的下线信号当成了 StreamProxy 表的更新事件**，对不在表里的直播流直接丢弃。

### 1.4 已验证的底层事实（避免踩坑）

逐一源码 / jar 核对，确保方案落地不返工：

| 事实 | 来源 | 用途 |
|------|------|------|
| `OnStreamChangedHookParam extends HookParam`，含 `stream` / `regist` + 父类 `getMediaServerId()` / `totalReaderCount` / `aliveSecond`（`mediaServerId` 定义在父类 `HookParam`） | zlm-starter `OnStreamChangedHookParam.java` / `HookParam.java` | 下线回调能拿到流标识 + 节点，足够发事件 |
| `StreamReadyEvent(String streamId)` 置于 `voglander-common`，单 streamId 构造，打破 service→integration 循环依赖 | `voglander-common/.../event/StreamReadyEvent.java` | 新增 `StreamOfflineEvent` 照此模式 |
| `LiveStreamRegistry.remove(streamId)` 清 session + refcount + 本地 future，对不存在 key 幂等 | `RedisLiveStreamRegistry.java:88` | 下线清缓存直接调它 |
| `LiveSessionInfo.nodeServerId` / `createMs` 是 **Redis 缓存对象**字段，**不是** GC 对账数据源（对账读 DB） | `LiveSessionInfo.java` | 区分缓存对象与 DB 会话，避免误用字段 |
| `MediaSessionManager.getActiveSessions()` 读 **DB**（`tb_media_session` status=ACTIVE），返回 `List<MediaSessionDTO>` | `MediaSessionManager.java:232` | GC 对账枚举 ACTIVE 会话的数据源 |
| `MediaSessionDTO` 字段为 `createTime`（**LocalDateTime**）/ `streamId` / `nodeServerId` / `callId`，**无 `createMs`** | `MediaSessionDTO.java` | GC 宽限期判定须用 `getCreateTime()`，非 `createMs` |
| **⚠️ 判活订正**：`getMediaInfo` 把响应整体解析成 `MediaInfo` 并**无条件** `success(...)`，且 `MediaInfo` **无 `online` 字段** → 不可判活。改用 `isMediaOnline(host,secret,MediaReq) → MediaOnlineStatus(Boolean online)` 或 `getMediaList(...) → List<MediaData>`（非空即存活） | `ZlmRestService.java:313/284/156`、`MediaInfo.java`、`MediaOnlineStatus.java` | 对账向 ZLM 核实流存活的**正确**权威 API |
| `MediaReq` 默认 `vhost=__defaultVhost__`、`schema=null`、`app`/`stream` 标 `@NotBlank` | `MediaReq.java` | 判活构造请求 app=`rtp` + stream=streamId 即可 |
| `nodeService.getAvailableNode(serverId)` 按 serverId 取节点；`ZlmNode` 有 `getHost()`/`getSecret()`/`getServerId()` | `NodeService.java`、`ZlmNode.java`、`closeStream`（`:234`） | 对账时定位节点连接信息 |
| SSE 事件名已用 `live.ready` / `live.closed` / `live.failed` / `alarm.new`；`SseEvent(String topic, Object data)` 构造 | `LiveStreamEventListener` / `MediaPlayServiceImpl` / `SseEvent.java` | 下线复用 `live.closed`，reason 区分来源 |
| `closeStream` 幂等：closeRtpServer + sendBye + 标 CLOSED + SSE + remove + 删 pending key，任一步异常不阻断；内部按 `getByStreamId` 查 DB 会话，无会话则只做收尾 | `MediaPlayServiceImpl.java:227` | GC 对账确认死流后，直接复用 `closeStream` 收尾 |
| `MediaSessionManager.forceClose(id)` 把会话标 CLOSED | `MediaSessionManager.java:293` | S1 监听器内 best-effort 标 DB CLOSED（先 `getByStreamId` 取 id） |

### 1.5 可行性结论（先说结论）

**方案整体可行，纯应用层闭环，不触碰协议层 / 流媒体库**。三处缺口都是"把已经到达的回调信号用起来 + 加一道以流媒体为准的对账兜底"。

**源码核对后的关键订正（已贯穿全文）**：
1. **判活 API**：原草案用 `getMediaInfo` 判活不可行（无 `online` 字段 + 无条件 success）→ 全部改为 `isMediaOnline`。
2. **S1 必须标 DB CLOSED**：否则 S2 重复处理 + 重复 SSE。
3. **多节点 reconcile 加锁防重**；宽限期判定用 `createTime`（非 `createMs`）。

需落地时确认的实测盲点：
- ZLM `onStreamChanged(regist=false)` 回调里 `stream` 字段是否与我们的 `streamId`（`gb_live_{deviceId}_{channelId}`）**完全一致**。上线分支 `StreamReadyEvent(param.getStream())` 已隐含依赖一致（否则首播 future 唤不醒），故大概率一致；S1 落地时打日志抓一次真实回调确认。
- `isMediaOnline` 在 `schema=null` 时是否跨协议正确返回 rtp 流的在线状态。若发现 schema 缺省导致查不到，落地时显式 `req.setSchema(...)` 或改用 `getMediaList` 列表比对。

---

## 2. 设计目标与范围切分

**目标**：让"缓存副本"严格跟随"流媒体真实状态"，复用分支永不返回死流地址。

**设计原则**：
- **对称闭环**：缓存的建（上线）与删（下线）都由 ZLM 回调驱动，路径对称。
- **以流媒体为准**：任何"流是否存活"的最终裁决，都向 ZLM 查证（`isMediaOnline`），不信任本地缓存的自我声明。
- **回调实时 + 对账兜底**：回调走实时路径（秒级清理）；回调可能丢失，故加 GC 对账作为最终一致性兜底（分钟级）。
- **不污染热路径**：复用分支默认不加 ZLM 探活（每次复用一次往返代价过高），把核对责任交给"下线回调"和"GC 对账"两条旁路。

**范围切分**：

| Sprint | 内容 | 必要性 | 模块 |
|--------|------|--------|------|
| **S1** | 下线回调对称清缓存（`StreamOfflineEvent`） | 🔴 必做（直接堵住主路径） | integration + service + common |
| **S2** | GC 以 ZLM 为准对账缓存 | 🔴 必做（兜底回调丢失） | service |
| **S3** | `onStreamNoneReader` 落地 + 复用前可选探活开关 | 🟡 健壮性加固 | integration + service |

---

## 3. S1 — 下线回调对称清缓存（主路径）

### 3.1 问题

`onStreamChanged(regist=false)` 这个权威下线信号，在 §1.3 被丢弃，缓存无人清。

### 3.2 改动

**① 新增事件类**（`voglander-common`，照 `StreamReadyEvent` 模式）

```java
// voglander-common/.../event/StreamOfflineEvent.java
package io.github.lunasaw.voglander.common.event;

/**
 * ZLM 流下线事件。
 * <p>
 * 由 {@code VoglanderZlmHookServiceImpl.onStreamChanged(regist=false)} 发布（integration 层），
 * 由 {@code LiveStreamEventListener} 消费（service 层，清直播缓存 + 推 SSE live.closed）。
 * 与 {@link StreamReadyEvent} 对称，置于 voglander-common 打破 service→integration 循环依赖。
 * </p>
 */
public class StreamOfflineEvent {
    private final String streamId;
    private final String serverId;   // ZLM mediaServerId，便于定位/日志，可为 null

    public StreamOfflineEvent(String streamId, String serverId) {
        this.streamId = streamId;
        this.serverId = serverId;
    }
    public String getStreamId() { return streamId; }
    public String getServerId() { return serverId; }
}
```

**② integration 层：下线分支发事件**（`VoglanderZlmHookServiceImpl.onStreamChanged`）

把 `:143-144` 的单边逻辑改为对称双边——**无论 StreamProxy 表是否查到，下线都按 stream 发事件**：

```java
// 流上线：唤醒首播等待 future（保持不变）
if (param.isRegist()) {
    eventPublisher.publishEvent(new StreamReadyEvent(param.getStream()));
} else {
    // 流下线：对称清直播缓存（不依赖 StreamProxy 表，rtp 直播流也覆盖）
    eventPublisher.publishEvent(new StreamOfflineEvent(param.getStream(), param.getMediaServerId()));
}
```

> 注意：StreamProxy 表的 `onlineStatus` 更新逻辑（`:117-140`）保持不变，它服务于拉流代理业务，与直播缓存治理正交，两者互不影响。

**③ service 层：监听清缓存 + 标 DB CLOSED**（`LiveStreamEventListener` 加一个 `@EventListener`）

```java
/**
 * 流下线：清直播缓存 + 标 DB 会话 CLOSED + 推 SSE live.closed（前端据此自动重连或提示）。
 * 幂等：缓存不存在时 remove 是 no-op；会话不存在时跳过标记。
 */
@EventListener
public void onStreamOffline(StreamOfflineEvent event) {
    String streamId = event.getStreamId();
    if (streamId == null || streamId.isBlank()) {
        return;
    }
    log.info("流下线事件, streamId={}, serverId={}", streamId, event.getServerId());

    // 1. 清 Redis 缓存（session + refcount + 本地 future）
    liveStreamRegistry.remove(streamId);

    // 2. 标 DB 会话 CLOSED（best-effort，不阻断）——必做，否则 S2 reconcile 会重复捞到 + 重复 closeStream/SSE
    try {
        MediaSessionDTO session = mediaSessionManager.getByStreamId(streamId);
        if (session != null && session.getId() != null
                && !Objects.equals(session.getStatus(), MediaSessionConstant.Status.CLOSED)) {
            mediaSessionManager.forceClose(session.getId());
        }
    } catch (Exception e) {
        log.warn("流下线标记 DB CLOSED 失败, streamId={}: {}", streamId, e.getMessage());
    }

    // 3. 推 SSE
    sseEventBus.publish(new SseEvent("live.closed",
        Map.of("streamId", streamId, "reason", "stream_offline")));
}
```

> `reason` 用 `stream_offline` 与现有 `idle_gc` / `node_exited` 区分来源，便于前端与排障辨识。
> **为何必须标 DB CLOSED**：S1 只清 Redis 而 DB 仍 ACTIVE 的话，§4 的 `reconcileActiveSessions()` 用 `getActiveSessions()`（查 DB ACTIVE）会在下一轮再次捞到这条已下线会话 → 重复 `closeStream` → 前端收到第二次 `live.closed`。在 S1 处一次标到位，让 DB 与 Redis 同步收敛。

### 3.3 边界与一致性考量

- **幂等**：`remove` 对不存在的 key 是 no-op；同一流多协议（flv/hls/rtsp）可能各发一次下线回调，重复 remove + 重复 forceClose（已 CLOSED 时被 `status` 判断短路）无害。
- **DB 收尾在 S1 完成**：监听器内 best-effort 标 `tb_media_session` 为 CLOSED（见 ③ 第 2 步），确保 Redis 与 DB 同步收敛，避免 S2 重复处理。不主动 BYE/closeRtpServer——流已在 ZLM 侧消失，SIP 侧的 BYE 收尾交给既有 `closeStream` / GC 路径；本事件只负责"清缓存 + 标 CLOSED + 通知前端"。
- **跨节点**：`remove` 操作 Redis 主库（集群共享），任一节点收到回调清理后全集群可见；`forceClose` 改 DB 同样全局可见；本地 future map 仅本节点，remove 对其他节点 future 无影响（首播 future 仅在发起节点存在）。

### 3.4 S1 验收

- 单测（`@ExtendWith(MockitoExtension.class)`，纯 Mockito）：构造 `StreamOfflineEvent`，验证 `liveStreamRegistry.remove(streamId)` 被调用一次、`mediaSessionManager.getByStreamId` 返回 ACTIVE 会话时 `forceClose(id)` 被调用、`sseEventBus.publish` 推 `live.closed`/`stream_offline`。
- 单测（DB 幂等）：`getByStreamId` 返回 null 或已 CLOSED 的会话时，验证 `forceClose` **不**被调用，且仍推 SSE（清缓存动作不受 DB 影响）。
- integration 单测：mock `eventPublisher`，`onStreamChanged(regist=false)` 时验证发布了 `StreamOfflineEvent` 且 `streamId`=param.stream、`serverId`=param.mediaServerId；`regist=true` 时仍只发 `StreamReadyEvent`。
- 集成验证（手动 / 协议验证台）：建立直播 → 停掉模拟设备推流 → 观察日志出现"流下线事件" → `redis-cli get live:session:{streamId}` 返回 nil + `tb_media_session` 该行 status=CLOSED(0) → 再次 `/live/start` 走首播重建而非复用死流。

---

## 4. S2 — GC 以 ZLM 为准对账缓存（兜底）

### 4.1 问题

回调是 best-effort：ZLM 重启、Hook HTTP 请求丢包、voglander 重启错过回调窗口，都会导致下线信号丢失。仅靠 S1 仍可能残留幽灵会话。需要一道**周期性对账**作为最终一致性保障——**以流媒体的真实流列表为准**校正本地缓存。

### 4.2 改动

`LiveSessionGcService.gc()` 增加第 3 步：对账 ACTIVE 会话。

```java
/** 宽限期：会话建立后 N 秒内跳过对账，避免与首播窗口竞态 */
private static final int RECONCILE_GRACE_SEC = 30;
/** 对账分布式锁，多节点只让一个实例执行整轮对账，避免重复 closeStream/SSE */
private static final String RECONCILE_LOCK_KEY = "live:gc:reconcile:lock";

@Autowired private NodeService    nodeService;
@Autowired private RedisLockUtil  redisLockUtil;

@Scheduled(fixedDelay = 60_000)
public void gc() {
    // 1. INVITING 超时 → FAILED（保持不变）
    // 2. drainPendingClose（保持不变）
    // 3. 新增：reconcileActiveSessions —— 以 ZLM 为准核实 ACTIVE 会话存活
    reconcileActiveSessions();
}

/**
 * 对账：枚举 DB 中 ACTIVE 会话，逐一向其所在 ZLM 节点核实流是否存活；
 * ZLM 查不到（流已死但回调丢失）→ 委托 closeStream 收尾（清缓存 + 标 CLOSED + SSE）。
 * 以流媒体真实状态为权威，纠正本地缓存漂移。
 * <p>多节点防重：整轮对账加分布式锁，只让一个实例执行（死流幂等收尾即可，无需多实例并发）。</p>
 */
void reconcileActiveSessions() {
    // 多节点防重：拿不到锁说明别的实例在对账，本轮跳过
    String lockValue = redisLockUtil.generateLockValue();
    if (!Boolean.TRUE.equals(redisLockUtil.tryLock(RECONCILE_LOCK_KEY, lockValue, 30, 0))) {
        return;
    }
    try {
        List<MediaSessionDTO> actives = mediaSessionManager.getActiveSessions();
        for (MediaSessionDTO s : actives) {
            String streamId = s.getStreamId();
            String serverId = s.getNodeServerId();
            if (streamId == null || serverId == null) {
                continue;
            }
            // 宽限：刚建立的会话（createTime 太近）跳过，避免与首播窗口竞态
            if (isWithinGracePeriod(s)) {
                continue;
            }
            ZlmNode node = nodeService.getAvailableNode(serverId);
            if (node == null) {
                // 节点都没了，交给 NodeExitedEvent 路径，这里跳过
                continue;
            }
            if (!streamAliveOnZlm(node, streamId)) {
                log.info("[GC] 对账发现死流(ZLM 查无), 收尾, streamId={}, serverId={}", streamId, serverId);
                mediaPlayService.closeStream(streamId);   // 幂等收尾
            }
        }
    } catch (Exception e) {
        log.warn("[GC] reconcileActiveSessions 异常，跳过本轮", e);
    } finally {
        redisLockUtil.unLock(RECONCILE_LOCK_KEY, lockValue);
    }
}

/** 会话创建时间在 RECONCILE_GRACE_SEC 内则跳过（用 DB 的 createTime，LocalDateTime） */
private boolean isWithinGracePeriod(MediaSessionDTO s) {
    if (s.getCreateTime() == null) {
        return false;   // 无创建时间，不豁免，照常对账
    }
    return s.getCreateTime().isAfter(LocalDateTime.now().minusSeconds(RECONCILE_GRACE_SEC));
}

/**
 * 以 ZLM 为准判活：用 isMediaOnline（专用判活接口，返回 online 布尔），
 * 而非 getMediaInfo（无 online 字段且无条件 success，无法判活）。
 * 查询异常（网络抖动）保守返回 true，避免误杀正常流，下轮再对。
 */
private boolean streamAliveOnZlm(ZlmNode node, String streamId) {
    try {
        MediaReq req = new MediaReq();
        req.setApp("rtp");          // 直播流统一落在 rtp 应用
        req.setStream(streamId);
        // vhost 默认 __defaultVhost__，schema 留空由 ZLM 跨协议匹配
        MediaOnlineStatus st = ZlmRestService.isMediaOnline(node.getHost(), node.getSecret(), req);
        return st != null && Boolean.TRUE.equals(st.getOnline());
    } catch (Exception e) {
        // 查询失败不轻易判死（避免网络抖动误杀），返回 true 保守保留，下轮再对
        log.warn("[GC] 探活异常，本轮保守保留, streamId={}: {}", streamId, e.getMessage());
        return true;
    }
}
```

> **判活 API 选型**：首选 `isMediaOnline`（语义最直接，返回 `MediaOnlineStatus.online`）。若 ACTIVE 会话很多、想省往返，可改用 `getMediaList(host, secret, MediaReq{app=rtp})` 一次拉全节点 rtp 流列表，本地按 `streamId` 比对（见 §8 风险表）。**切勿**用 `getMediaInfo` 判活（详见 §1.4 订正条目）。

### 4.3 关键设计点

- **数据源选 DB 而非 Redis**：`getActiveSessions()` 读 `tb_media_session`（`MediaSessionManager.java:232`），是持久化的权威会话表；Redis `live:session:*` 是性能缓存。对账以"DB 里声称 ACTIVE 的会话"为待核对象，核对结果再驱动 `closeStream` 同时清两边。
- **判活用 `isMediaOnline`，不用 `getMediaInfo`**：`getMediaInfo` 把整个响应解析成 `MediaInfo`（该类无 `online` 字段）并无条件 `ServerResponse.success(...)`，流不存在时 `getData()` 是非 null 空壳 → 判活恒真。必须用 `isMediaOnline`（返回 `MediaOnlineStatus.online`）或 `getMediaList`（列表比对）。
- **以 ZLM 为准，但失败保守**：`isMediaOnline` 明确 `online=false` 才判死；**查询异常（网络抖动）时返回 true 保留**，避免误杀正常流，下一轮（60s 后）再对。死流多熬一轮无害，误杀活流影响体验。
- **宽限期防竞态（用 `createTime`）**：首播 future 唤醒到 `putSession` 之间有窗口，新建会话可能短暂"DB ACTIVE 但 ZLM 刚注册"。用 `MediaSessionDTO.getCreateTime()`（**LocalDateTime**，非 `createMs`）设宽限（30s 内跳过对账），避免 GC 误杀正在建立的流。
- **多节点防重（分布式锁）**：`reconcileActiveSessions` 用全量 `getActiveSessions()`，多实例部署时每个节点都会扫到同一批 ACTIVE 会话。整轮对账加 `RedisLockUtil` 分布式锁（`tryLock(..., wait=0)` 拿不到即跳过本轮），保证同一时刻只有一个实例对账，避免对同一死流重复 `closeStream` / 重复推 SSE。死流被晚一轮处理无害。
- **复用幂等 closeStream**：确认死流后不另写收尾逻辑，直接调 `MediaPlayService.closeStream`（已幂等：closeRtpServer + sendBye + 标 CLOSED + SSE + remove + 删 pending key），与 `drainPendingClose` 收尾路径统一。

### 4.4 S2 验收

- 单测（GC 对账命中死流）：mock `mediaSessionManager.getActiveSessions` 返回一条会话（`createTime` 早于宽限期）、mock `ZlmRestService.isMediaOnline`（mockStatic）返回 `online=false`，验证 `mediaPlayService.closeStream(streamId)` 被调用；返回 `online=true` 则不调用。
- 单测（保守保留）：`isMediaOnline` 抛异常，验证 `closeStream` **不**被调用（不误杀）。
- 单测（宽限期）：会话 `createTime` 在 30s 内，验证跳过对账（不调 `isMediaOnline`、不调 `closeStream`）。
- 单测（多节点锁）：`redisLockUtil.tryLock` 返回 false，验证整轮 `getActiveSessions` 都不执行（直接 return）。
- 集成验证：直播建立 → 直接 `kill` ZLM 进程的流（或停推且屏蔽下线回调）→ 等 ≤2 个 GC 周期 → 会话被对账清理、缓存清空、DB status=CLOSED。

---

## 5. S3 — 无人观看兜底 + 复用前可选探活（加固）

### 5.1 `onStreamNoneReader` 落地

当前是 TODO 空壳（`VoglanderZlmHookServiceImpl.java:158`）。直播场景下"无人观看"通常意味着前端全部断开但未正常 stop。策略：

- **保守方案（推荐）**：无人观看时**不立即关流**，仅记录 + 推一个 SSE 提示；真正回收交给 `pending_close` / GC 对账。理由：rtp 收流端口已开，立即关流可能与"用户刚断开马上重连"竞态。
- 若产品要求"无人观看即时回收"，则 `onStreamNoneReader` 返回 `close=true` 让 ZLM 关流，同时发 `StreamOfflineEvent` 清缓存。**需确认 refCount 语义与前端重连策略后再开**，本期默认保守。

### 5.2 复用前可选探活（默认关闭）

为应对"S1 回调 + S2 对账都还没跑到，但缓存已死"的极小窗口，提供一个**配置开关**，在复用分支返回前对 ZLM 做一次 `getMediaInfo` 探活：

```java
// 复用分支，配置开启时才探活（默认 false，不污染热路径）
if (existing != null && isActive(existing.getStatus())) {
    if (reuseVerifyEnabled && !streamAliveOnZlm(node, streamId)) {  // 复用 §4.2 同款 isMediaOnline 判活
        liveStreamRegistry.remove(streamId);   // 缓存已死，清掉，落入首播重建
    } else {
        liveStreamRegistry.incRef(streamId);
        liveStreamRegistry.keepAlive(streamId, KEEPALIVE_SEC);
        return buildDTO(streamId, existing);
    }
}
```

> ⚠️ 探活分支需要 `node`，但复用判定在**选节点之前**（[MediaPlayServiceImpl.java:114](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/live/impl/MediaPlayServiceImpl.java#L114)）。开关开启时，应据 `existing.getNodeServerId()` 经 `nodeService.getAvailableNode(serverId)` 取回会话原节点（而非 `selectNode()` 重新负载均衡），再对该节点探活——否则可能查错节点恒判死。落地时按此调整取节点顺序。

- 开关 `live.reuse-verify-enabled`（默认 `false`）。每次复用加一次 ZLM 往返（通常 <50ms 同机房），对延迟敏感场景默认不开。
- 判活复用 §4.2 的 `streamAliveOnZlm`（`isMediaOnline`），**勿**另起 `getMediaInfo`。
- 默认关闭的依据：S1（秒级）+ S2（分钟级）已能覆盖绝大多数死流；探活是"绝对实时正确性 vs 复用延迟"的权衡，留给运维按需开启。

### 5.3 S3 验收

- `onStreamNoneReader` 保守方案：单测验证返回不关流 + 记录日志。
- 复用探活开关：`reuseVerifyEnabled=true` 且 ZLM 查无 → 验证走首播重建；`=false` → 验证直接复用（不调 getMediaInfo）。

---

## 6. 实施顺序与构建注意

### 6.1 推荐顺序（每步独立编译可测）

1. **S1**（common 新增事件 → integration 发事件 → service 监听）：堵主路径，收益最大，先上。
2. **S2**（GC 对账）：兜底，独立于 S1，可并行。
3. **S3**（加固）：S1+S2 稳定后按需。

### 6.2 构建坑（来自 [[voglander-web-tests-use-installed-integration-jar]] / [[voglander-test-infra-gotchas]]）

- 改了 `voglander-common`（新增 `StreamOfflineEvent`）或 `voglander-integration`（onStreamChanged）后，**跑 voglander-web 测试前必须先 `mvn install` 这两个模块**（或 `-am` 连带构建），否则 web 测试跑的是 `~/.m2` 里的**旧 jar 字节码**，看不到新事件类 / 新逻辑。
- 任何疑似"改了没生效"的诡异现象，先 `mvn clean`（IDE 增量编译会残留过期 class）。

### 6.3 分层与规范红线（强制）

- 事件类置于 `voglander-common`，**严禁** service 直接依赖 integration（循环依赖），照 `StreamReadyEvent` 既有模式。
- GC 对账里向 ZLM 的查询走 `ZlmRestService` 静态方法（与 `closeStream` 一致），节点连接信息经 `nodeService.getAvailableNode`。
- 死流收尾**复用** `MediaPlayService.closeStream`，**禁止**在 GC 里另写一套关流逻辑。
- 业务层（监听器 / GC）单测用纯 Mockito（`@ExtendWith(MockitoExtension.class)`），**禁用** `@SpringBootTest`。

---

## 7. 文件改动清单（汇总）

| 模块 | 文件 | 改动 | Sprint |
|------|------|------|--------|
| common | `event/StreamOfflineEvent.java` | **新增**，照 `StreamReadyEvent`（streamId + serverId 两参） | S1 |
| integration | `wrapper/zlm/impl/VoglanderZlmHookServiceImpl.java` | `onStreamChanged` 下线分支发 `StreamOfflineEvent(stream, mediaServerId)`（`:143` 附近） | S1 |
| service | `live/LiveStreamEventListener.java` | 加 `@EventListener onStreamOffline` → `remove` + **`forceClose` 标 DB CLOSED** + SSE | S1 |
| service | `live/LiveSessionGcService.java` | `gc()` 加 `reconcileActiveSessions`（**`isMediaOnline` 判活** + 宽限期 `createTime` + **分布式锁防重**）；注入 `NodeService` / `RedisLockUtil` | S2 |
| integration | `wrapper/zlm/impl/VoglanderZlmHookServiceImpl.java` | `onStreamNoneReader` 落地（保守方案） | S3 |
| service | `live/impl/MediaPlayServiceImpl.java` | 复用分支加 `reuseVerifyEnabled` 可选探活（按 `nodeServerId` 取原节点 + `isMediaOnline`） | S3 |
| web | `voglander-web/src/test/.../live/*` | 上述各项单测 | S1-S3 |

---

## 8. 风险与回归点

| 风险 | 说明 | 缓解 |
|------|------|------|
| **判活 API 用错** | `getMediaInfo` 无 `online` 字段且无条件 success → 编译失败/判活恒真 | **已订正**：用 `isMediaOnline`（§1.4 / §4.2），落地时确保不残留 `getMediaInfo` |
| **S1 只清 Redis、DB 残留 ACTIVE** | S2 reconcile 重复捞到 → 重复 closeStream + 重复 SSE | **已订正**：S1 监听器内 `forceClose` 标 DB CLOSED（§3.2 ③） |
| **多节点 reconcile 重复执行** | 多实例对同一死流并发 closeStream/SSE | **已订正**：整轮对账加分布式锁，拿不到即跳过（§4.3） |
| 下线回调 stream 字段与 streamId 不一致 | 若不一致，`remove` 清错 / 清不到 | S1 落地抓真实回调确认；上线分支已隐式依赖一致（首播 future 能唤醒即证一致） |
| GC 对账误杀正在建立的流 | 首播窗口内 DB ACTIVE 但 ZLM 刚注册 | 宽限期（30s，用 `createTime`）+ 查询异常保守保留 |
| 多协议重复下线回调 | 一条流 flv/hls 各发一次下线 | `remove` 幂等、`forceClose` 经 status 判断短路，无害 |
| GC 对账给 ZLM 加查询压力 | ACTIVE 会话多时每轮 N 次 `isMediaOnline` | 60s 一轮、宽限期减少对象、单次查询轻量；必要时改 `getMediaList` 批量一次拉全节点流列表本地比对 |
| 复用探活查错节点 | 复用判定在选节点前，探活需会话原节点 | S3 开关开启时按 `existing.getNodeServerId()` 取原节点（§5.2 注） |
| 复用探活增加首播延迟 | S3 开关开启时每次复用一次往返 | 默认关闭，留运维权衡 |

---

## 9. 验收总览（Definition of Done）

- [ ] S1：流下线回调清缓存，`redis-cli` 验证 `live:session:*` 被清、`tb_media_session` 该行 status=CLOSED，前端收到 `live.closed/stream_offline`。
- [ ] S2：GC 对账以 ZLM `isMediaOnline` 为准，回调丢失时 ≤2 周期内清死流；异常不误杀；宽限期（`createTime` 30s）不杀新流；多实例下分布式锁保证单实例对账。
- [ ] S3（按需）：`onStreamNoneReader` 不再空壳；复用探活开关可配且按原节点判活。
- [ ] 复现验证：停设备推流 / 关浏览器不 stop → 再次 `/live/start` **不再返回死流地址**，走首播重建。
- [ ] 全部新增单测绿；改动模块（common + integration）`mvn clean install` 后 voglander-web 测试通过。
- [ ] 代码无残留 `getMediaInfo` 判活、无 `createMs` 误用；不触碰 `sip-proxy` / `zlm-starter`，无上游重建。
