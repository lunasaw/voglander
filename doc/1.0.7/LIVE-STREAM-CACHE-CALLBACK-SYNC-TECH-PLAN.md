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

> **⚠️ 协议合规修订（2026-06-11，本次更新）**：原方案 S1 下线只"清缓存+标 CLOSED"、S3 无人观看"恒 `close=false` 不关流"，**违反 SIP/GB28181 标准**——established 对话必须由一方发 `BYE`、对方回 `200 OK` 才算正常终止（RFC 3261 §15）。下线/空闲只删本地缓存，等于平台单方面忘掉会话，**设备侧对话仍 established**，占用设备有限的并发会话槽、泄漏收流上下文。且对 rtp 推流（设备 INVITE 推流），仅给 ZLM 回 `close=true` **不够**——设备仍在往端口怼 RTP，**必须发 BYE 让设备停推**（Lab 的 `LabByeListener` 正是收到 BYE 才停 ffmpeg）。
> 本次按标准订正：**下线回调 + 无人观看到点都走平台主动 BYE 回收**（委托幂等 `closeStream`：`closeRtpServer + sendBye + 标 CLOSED + SSE + remove`），无人观看仅以"首播宽限期"防启动竞态，不再永不关流。

**后果链路**：流在 ZLM 侧死了（设备停推 / RTP 超时 / ZLM 重启 / 网络断）→ 下线回调到了但被丢弃 → `live:session:{streamId}` 这个 `status=ACTIVE` 的缓存**僵在 Redis 最多 3600s** → 下一次 `/live/start` 命中复用分支 → 返回死流的旧 `playUrls` → **前端播放失败**。

**修复策略（对称闭环 + 兜底对账 + 标准 BYE 收尾）**：
1. **S1（必做）**：下线回调对称收尾。`onStreamChanged(regist=false)` 无条件按 `stream` 发 `StreamOfflineEvent`，service 层监听后**委托幂等 `closeStream`**——`closeRtpServer + sendBye(标准对话内 BYE) + 标 CLOSED + remove + 推 live.closed SSE`。和上线的 `StreamReadyEvent` 对称，符合"平台作为 UAC 主动结束会话"的协议语义。
2. **S2（必做）**：GC 兜底对账。回调是 best-effort（网络抖动 / ZLM 重启会丢），GC 周期性拿 DB 里 ACTIVE 会话去 ZLM **`isMediaOnline`** 核实，**以流媒体为准**，查不到的清掉。
3. **S3（健壮性加固，按标准回收）**：`onStreamNoneReader` 落地为**到点主动 BYE 回收**——无人观看且超过首播宽限期 → 发 `StreamOfflineEvent(reason=none_reader)` 走 `closeStream` 收尾。仅用"首播宽限期 + ZLM `stream_none_reader_delay`"两道防线防启动竞态，**不再永不关流**。回收开关与宽限时长做成配置。

改动集中在 **2 个模块**：`voglander-integration`（下线/无人观看回调发事件）、`voglander-service`（监听委托 `closeStream` + GC 对账）。新增 1 个事件类于 `voglander-common`。**不改** `sip-proxy` / `zlm-starter`（无需上游重建）。

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
| **⚠️ 锁 API 订正**：`RedisLockUtil.tryLock(key,value,ttl,getTimeOut)` **校验 `getTimeOut>0`，传 0 抛 `IllegalArgumentException`**——不能用作"单次非阻塞"。单次非阻塞用 `lock(key,value,ttl)`（底层 `setCacheObjectIfAbsent`），拿不到返回 false 即跳过 | `RedisLockUtil.java:52/82-85` | reconcile 整轮防重用 `lock(...)`，**勿** `tryLock(...,0)` |
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

**③ service 层：监听委托 `closeStream`（标准 BYE 收尾）**（`LiveStreamEventListener` 加一个 `@EventListener`）

下线收尾必须包含**对话内 BYE**（平台作为 UAC 主动结束），而 `closeStream` 已把 `closeRtpServer + sendBye + 标 CLOSED + SSE + remove + 删 pending key` 串成幂等收尾。监听器**直接委托它**，不再在监听器里重写一遍清理逻辑：

```java
@Autowired private MediaPlayService mediaPlayService;

/**
 * 流下线 / 无人观看到点：委托 closeStream 做标准 BYE 收尾
 * （closeRtpServer + sendBye + 标 CLOSED + SSE live.closed + remove + 删 pending key）。
 * 符合 SIP/GB28181：established 对话由平台主动发 BYE 终止，而非单方面删缓存。
 * 幂等：closeStream 内部按 streamId 查会话，已 CLOSED / 无会话也尽力收尾，不阻断。
 */
@EventListener
public void onStreamOffline(StreamOfflineEvent event) {
    String streamId = event.getStreamId();
    if (streamId == null || streamId.isBlank()) {
        return;
    }
    log.info("流下线事件, streamId={}, serverId={}, reason={}",
        streamId, event.getServerId(), event.getReason());
    mediaPlayService.closeStream(streamId, event.getReason());
}
```

其中 `closeStream` 增加一个带 `reason` 的重载（原无参 `closeStream(streamId)` 委托 `closeStream(streamId, "idle_gc")` 保持兼容），让 SSE `live.closed` 的 `reason` 能区分来源（`stream_offline` / `none_reader` / `idle_gc` / `node_exited`）：

```java
@Override
public void closeStream(String streamId) {
    closeStream(streamId, "idle_gc");
}

@Override
public void closeStream(String streamId, String reason) {
    // …closeRtpServer + sendBye + forceClose…（原逻辑不变）
    sseEventBus.publish(new SseEvent("live.closed",
        Map.of("streamId", streamId, "reason", reason == null ? "idle_gc" : reason)));
    liveStreamRegistry.remove(streamId);
    stringRedisTemplate.delete(PENDING_CLOSE_PREFIX + streamId);
}
```

> **依赖方向**：`LiveStreamEventListener` 与 `MediaPlayServiceImpl` 同在 service 层，且 `MediaPlayServiceImpl` 不反向依赖监听器，注入 `MediaPlayService` 无循环依赖。
> **幂等去重**：`closeStream` 内 `getByStreamId` 查到已 CLOSED 会话时，`sendBye(callId)` 仍可能重发——但正常 `/live/stop` 路径已先 `closeStream` 过、会话 CLOSED 且 Redis 已清��下线回调再委托一次 `closeStream` 时 `getByStreamId` 多半已无 ACTIVE 行（被 forceClose），BYE 不会真正重发到设备（callId 对应对话已终止，框架层 no-op / 日志告警）。多协议重复下线回调同理，幂等无害。

### 3.3 边界与一致性考量

- **标准 BYE 收尾**：委托 `closeStream` 后，下线收尾包含 `sendBye(callId)`——平台作为 UAC 在既有对话内主动发 BYE，设备收到后停推（Lab `LabByeListener` 即此行为），符合 RFC 3261 §15 / GB28181 §9。对 rtp 推流，这也是真正让设备停止怼 RTP 的唯一手段。
- **幂等**：`closeStream` 对不存在 / 已 CLOSED 的会话尽力收尾不阻断；同一流多协议（flv/hls/rtsp）各发一次下线回调，重复委托无害（第二次多半已无 ACTIVE 会话，BYE 不重发到设备）。
- **跨节点**：`remove` 操作 Redis 主库（集群共享），全集群可见；`forceClose` 改 DB 全局可见；`closeRtpServer` / `sendBye` 按会话持久化的 `nodeServerId` / `callId` 定位，任一节点收到回调都能正确路由。

### 3.4 S1 验收

- 单测（`@ExtendWith(MockitoExtension.class)`，纯 Mockito）：构造 `StreamOfflineEvent`，验证 `mediaPlayService.closeStream(streamId, "stream_offline")` 被调用一次。
- 单测（reason 透传）：`StreamOfflineEvent(reason=none_reader)` 时验证 `closeStream(streamId, "none_reader")`。
- 单测（空 streamId）：`streamId` 为空 → 直接返回，`closeStream` **不**被调用。
- `closeStream(reason)` 单测：在既有 `MediaPlayServiceCloseStreamTest` 补一例，验证 SSE `live.closed` 的 `reason` 取传入值；无参 `closeStream` 仍为 `idle_gc`。
- integration 单测：mock `eventPublisher`，`onStreamChanged(regist=false)` 验证发布 `StreamOfflineEvent`（`streamId`=param.stream、`serverId`=param.mediaServerId、`reason`=stream_offline）；`regist=true` 仍只发 `StreamReadyEvent`。
- 集成验证（协议验证台）：建立直播 → 停掉模拟设备推流 → 日志出现"流下线事件" + "sendBye" → `redis-cli get live:session:{streamId}` 返回 nil + `tb_media_session` status=CLOSED(0) → Lab 设备侧收到 BYE 停推 → 再次 `/live/start` 走首播重建。

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
/** 对账锁持有时间（秒），需大于整轮对账耗时 */
private static final int RECONCILE_LOCK_SEC = 30;

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
    // 多节点防重：单次非阻塞尝试拿锁。
    // ⚠️ 不能用 tryLock(key,value,ttl,0)——RedisLockUtil.tryLock 校验 getTimeOut>0，传 0 会抛 IllegalArgumentException。
    // 用 lock(key,value,ttl)（底层 setIfAbsent），语义即"拿不到即跳过"。
    String lockValue = redisLockUtil.generateLockValue();
    if (!Boolean.TRUE.equals(redisLockUtil.lock(RECONCILE_LOCK_KEY, lockValue, RECONCILE_LOCK_SEC))) {
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
- **多节点防重（分布式锁）**：`reconcileActiveSessions` 用全量 `getActiveSessions()`，多实例部署时每个节点都会扫到同一批 ACTIVE 会话。整轮对账加 `RedisLockUtil` 分布式锁（`lock(key,value,ttl)` 单次非阻塞，拿不到即跳过本轮——**注意不能用 `tryLock(...,getTimeOut=0)`，该方法校验 `getTimeOut>0` 会抛 `IllegalArgumentException`**），保证同一时刻只有一个实例对账，避免对同一死流重复 `closeStream` / 重复推 SSE。死流被晚一轮处理无害。
- **复用幂等 closeStream**：确认死流后不另写收尾逻辑，直接调 `MediaPlayService.closeStream`（已幂等：closeRtpServer + sendBye + 标 CLOSED + SSE + remove + 删 pending key），与 `drainPendingClose` 收尾路径统一。

### 4.4 S2 验收

- 单测（GC 对账命中死流）：mock `mediaSessionManager.getActiveSessions` 返回一条会话（`createTime` 早于宽限期）、mock `ZlmRestService.isMediaOnline`（mockStatic）返回 `online=false`，验证 `mediaPlayService.closeStream(streamId)` 被调用；返回 `online=true` 则不调用。
- 单测（保守保留）：`isMediaOnline` 抛异常，验证 `closeStream` **不**被调用（不误杀）。
- 单测（宽限期）：会话 `createTime` 在 30s 内，验证跳过对账（不调 `isMediaOnline`、不调 `closeStream`）。
- 单测（多节点锁）：`redisLockUtil.lock(RECONCILE_LOCK_KEY, ..., RECONCILE_LOCK_SEC)` 返回 false，验证整轮 `getActiveSessions` 都不执行（直接 return）、不调 `unLock`。
- 集成验证：直播建立 → 直接 `kill` ZLM 进程的流（或停推且屏蔽下线回调）→ 等 ≤2 个 GC 周期 → 会话被对账清理、缓存清空、DB status=CLOSED。

---

## 5. S3 — 无人观看到点主动 BYE 回收（按标准协议）+ 推流 SSE

### 5.1 `onStreamNoneReader` 落地为标准 BYE 回收

当前是 TODO 空壳（`VoglanderZlmHookServiceImpl.java:158`）。直播场景下"无人观看"= 下游播放器全部断开（含"关浏览器没调 `/live/stop`"导致 refCount 泄漏的情况）。

**按协议正确做法**：无人观看到达 ZLM 回调阈值（`stream_none_reader_delay`，本身已是空读宽限）后，平台应**主动发 BYE 回收**——这正是 BYE 的本职（RFC 3261 §15：不再需要会话即主动拆对话），也是"客户端主动结束"的语义。对 rtp 推流，仅给 ZLM 回 `close=true` 不足以让设备停推，**必须发 BYE**。

```java
@Value("${live.none-reader.reclaim-enabled:true}")
private boolean noneReaderReclaimEnabled;
/** 首播宽限期（秒）：会话建立 N 秒内即便无人观看也不回收，防"流刚上线、播放器还没连上"竞态 */
@Value("${live.none-reader.grace-sec:30}")
private int     noneReaderGraceSec;

@Autowired private MediaSessionManager mediaSessionManager;   // integration 已依赖 manager 层，无新依赖方向问题

@Override
public HookResultForStreamNoneReader onStreamNoneReader(OnStreamNoneReaderHookParam param, HttpServletRequest request) {
    String streamId = param.getStream();
    log.info("ZLM流无人观看回调 - 应用: {}, 流名: {}", param.getApp(), streamId);

    HookResultForStreamNoneReader result = new HookResultForStreamNoneReader();
    result.setCode(0);

    // 开关关闭 → 保持旧保守行为（不关流）
    if (!noneReaderReclaimEnabled || streamId == null || streamId.isBlank()) {
        result.setClose(false);
        return result;
    }
    // 首播宽限期内 → 不回收，防启动竞态（用 DB 会话 createTime）
    if (isWithinGrace(streamId)) {
        log.info("无人观看但在首播宽限期内，暂不回收, streamId={}", streamId);
        result.setClose(false);
        return result;
    }
    // 到点回收：发 StreamOfflineEvent(reason=none_reader) → service 委托 closeStream（含 sendBye）
    eventPublisher.publishEvent(new StreamOfflineEvent(streamId, param.getMediaServerId(), "none_reader"));
    result.setClose(true);   // 同时让 ZLM 丢弃流对象；真正停设备推流靠上面的 BYE
    return result;
}

/** 会话 createTime 在宽限期内则跳过回收；无会话 / 无时间 → 不豁免（照常回收孤儿流） */
private boolean isWithinGrace(String streamId) {
    try {
        MediaSessionDTO s = mediaSessionManager.getByStreamId(streamId);
        if (s == null || s.getCreateTime() == null) {
            return false;
        }
        return s.getCreateTime().isAfter(java.time.LocalDateTime.now().minusSeconds(noneReaderGraceSec));
    } catch (Exception e) {
        log.warn("无人观看宽限期判定异常，按不豁免处理, streamId={}: {}", streamId, e.getMessage());
        return false;
    }
}
```

> **为何 `close=true` + 发 BYE 双管**：`close=true` 让 ZLM 立即丢弃流对象（释放下游 muxer）；但设备仍在向 RTP 端口推流，唯有 `closeStream` 内的 `sendBye(callId)` 能让设备停推（Lab 收到 BYE 停 ffmpeg）、`closeRtpServer` 释放收流端口。两者职责不同，都要做。`StreamOfflineEvent` 走既有 §3.2 ③ 监听器，复用同一条 `closeStream` 收尾，零重复逻辑。
> **防竞态两道防线**：① ZLM `stream_none_reader_delay`（媒体侧空读宽限，建议 ≥10s）；② 首播宽限期 `live.none-reader.grace-sec`（应用侧，默认 30s，覆盖"流上线→播放器连上"窗口）。两道都过才回收。

### 5.2 配置项

| 配置 | 默认 | 含义 |
|------|------|------|
| `live.none-reader.reclaim-enabled` | `true` | 无人观看到点是否主动 BYE 回收；置 `false` 退回旧保守行为（不关流） |
| `live.none-reader.grace-sec` | `30` | 首播宽限期（秒），会话建立此时长内即便无人观看也不回收 |
| `live.reuse-verify-enabled` | `false` | 复用分支返回前是否按原节点 `isMediaOnline` 探活（§5.3，默认关，不污染热路径） |

### 5.3 复用前可选探活（默认关闭，保持原设计）

为应对"S1 回调 + S2 对账都还没跑到，但缓存已死"的极小窗口，复用分支返回前可选对 ZLM 做一次 `isMediaOnline` 探活（**勿**用 `getMediaInfo`，详见 §1.4）。此项已在 `MediaPlayServiceImpl.reuseStreamAlive` 实现（按 `existing.getNodeServerId()` 取原节点判活），开关 `live.reuse-verify-enabled` 默认 `false`。本次不改动，仅列出供完整性。

### 5.4 推流 SSE（协议验证台前端同步，需求 2）

`config` 的 topics 声明了 `clientcmd.push.started/stopped/failed`，但**全工程从未 publish**，前端只能轮询 `/push/status`。补齐：`LabMediaPushService` 注入 `ApplicationEventPublisher`，在推流起/停/失败时发 `SseRelayEvent`（与 `LabInviteListener` 等同构，经 `SseRelayListener` 中继到 `SseEventBus`）。

- `startPush` 成功 → `clientcmd.push.started`（含 callId / mediaIp / mediaPort / ssrc / cmd）
- `startPush` 失败（抛异常前）→ `clientcmd.push.failed`（含 error）
- `stopInternal` 实际停掉一路推流 → `clientcmd.push.stopped`（含 callId）

> 自动推流（`LabInviteListener` 调 `startPush`）与手动推流（`/push/start`）走同一 `startPush`，SSE 自然一致——前端无论哪条路径都能实时看到推流起停，满足"模拟推流与自动推流同步"。

### 5.5 S3 验收

- `onStreamNoneReader` 单测（回收）：开关开 + 会话 createTime 早于宽限期 → 验证发布 `StreamOfflineEvent(reason=none_reader)` 且 `result.isClose()==true`。
- `onStreamNoneReader` 单测（宽限期内）：会话 createTime 在宽限期内 → 验证**不**发事件、`close==false`。
- `onStreamNoneReader` 单测（开关关）：`reclaim-enabled=false` → 不发事件、`close==false`（退回保守）。
- `onStreamNoneReader` 单测（孤儿流）：`getByStreamId` 返回 null → 不豁免，发事件回收。
- 推流 SSE 单测：`startPush` 成功验证发 `clientcmd.push.started`；`stop` 验证发 `clientcmd.push.stopped`；启动失败验证发 `clientcmd.push.failed`。
- 集成验证：直播建立后关掉所有播放器 → 等过 `stream_none_reader_delay` + 宽限期 → 日志"无人观看…回收" + "sendBye" → 设备停推、会话 CLOSED、缓存清空。

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
| common | `event/StreamOfflineEvent.java` | **新增**，照 `StreamReadyEvent`；含 `reason` 字段（3 参构造 `streamId+serverId+reason`，保留 2 参重载默认 reason=`stream_offline`） | S1 |
| integration | `wrapper/zlm/impl/VoglanderZlmHookServiceImpl.java` | `onStreamChanged` 下线分支发 `StreamOfflineEvent(stream, mediaServerId, "stream_offline")` | S1 |
| service | `live/MediaPlayService.java` | 接口加 `closeStream(String streamId, String reason)` 重载；原 `closeStream(streamId)` 委托 `idle_gc` | S1 |
| service | `live/impl/MediaPlayServiceImpl.java` | `closeStream` 抽出带 `reason` 实现，SSE `live.closed` 用传入 reason | S1 |
| service | `live/LiveStreamEventListener.java` | `onStreamOffline` 改为**委托 `mediaPlayService.closeStream(streamId, reason)`**（含标准 BYE）；注入 `MediaPlayService` | S1 |
| service | `live/LiveSessionGcService.java` | `gc()` 加 `reconcileActiveSessions`（`isMediaOnline` 判活 + 宽限期 `createTime` + 分布式锁防重）；死流委托 `closeStream` | S2 |
| integration | `wrapper/zlm/impl/VoglanderZlmHookServiceImpl.java` | `onStreamNoneReader` 落地**到点 BYE 回收**：超宽限期发 `StreamOfflineEvent(reason=none_reader)` + `close=true`；注入 `MediaSessionManager`，配置 `live.none-reader.*` | S3 |
| integration | `wrapper/gb28181/lab/LabMediaPushService.java` | 注入 `ApplicationEventPublisher`，推流起/停/失败发 `SseRelayEvent` `clientcmd.push.started/stopped/failed` | S3 |
| web | `voglander-web/src/test/.../live/*`、`.../lab/*` | 上述各项单测 | S1-S3 |

---

## 8. 风险与回归��

| 风险 | 说明 | 缓解 |
|------|------|------|
| **判活 API 用错** | `getMediaInfo` 无 `online` 字段且无条件 success → 编译失败/判活恒真 | **已订正**：用 `isMediaOnline`（§1.4 / §4.2），落地时确保不残留 `getMediaInfo` |
| **下线/空闲不发 BYE，设备对话泄漏** | 仅删缓存 → 设备侧对话仍 established，占用并发槽、设备仍推 RTP | **已订正**：下线/无人观看到点均委托 `closeStream`，含标准对话内 `sendBye`（§3.2 ③ / §5.1） |
| 无人观看误杀正在建立的流 | 流上线→播放器连上窗口内 readers 暂为 0 | 双防线：ZLM `stream_none_reader_delay` + 首播宽限期 `live.none-reader.grace-sec`（默认 30s，用 `createTime`） |
| **多节点 reconcile 重复执行** | 多实例对同一死流并发 closeStream/SSE | **已订正**：整轮对账加分布式锁，拿不到即跳过（§4.3） |
| 下线回调 stream 字段与 streamId 不一致 | 若不一致，`closeStream` 清错 / 清不到 | S1 落地抓真实回调确认；上线分支已隐式依赖一致（首播 future 能唤醒即证一致） |
| BYE 对已终止对话重发 | 多协议重复下线 / 正常 stop 后再收下线回调 | `closeStream` 幂等；第二次 `getByStreamId` 多已无 ACTIVE 会话，BYE 不真正下发到设备 |
| GC 对账给 ZLM 加查询压力 | ACTIVE 会话多时每轮 N 次 `isMediaOnline` | 60s 一轮、宽限期减少对象、单次查询轻量；必要时改 `getMediaList` 批量比对 |
| 无人观看回收开关误开/误关 | 开则可能竞态、关则退回泄漏 | 开关 `live.none-reader.reclaim-enabled` 默认 `true`（按标准回收）；竞态由双宽限防线兜底 |

---

## 9. 验收总览（Definition of Done）

- [ ] S1：流下线回调委托 `closeStream` 标准收尾，`redis-cli` 验证 `live:session:*` 被清、`tb_media_session` status=CLOSED、**设备收到 BYE 停推**、前端收到 `live.closed/stream_offline`。
- [ ] S2：GC ��账以 ZLM `isMediaOnline` 为准，回调丢失时 ≤2 周期内 `closeStream` 清死流（含 BYE）；异常不误杀；宽限期（`createTime` 30s）不杀新流；多实例下分布式锁保证单实例对账。
- [ ] S3：`onStreamNoneReader` 无人观看到点主动 BYE 回收，首播宽限期内不杀、开关可关；`clientcmd.push.*` SSE 真正发布，前端实时同步推流起停。
- [ ] 复现验证：停设备推流 / 关浏览器不 stop / 无人观看到点 → 均触发 BYE + 清缓存 + 标 CLOSED；再次 `/live/start` 走首播重建。
- [ ] 全部新增单测绿；改动模块（common + integration）`mvn clean install` 后 voglander-web 测试通过。
- [ ] 代码无残留 `getMediaInfo` 判活；不触碰 `sip-proxy` / `zlm-starter`，无上游重建。
- [ ] 复现验证：停设备推流 / 关浏览器不 stop → 再次 `/live/start` **不再返回死流地址**，走首播重建。
- [ ] 全部新增单测绿；改动模块（common + integration）`mvn clean install` 后 voglander-web 测试通过。
- [ ] 代码无残留 `getMediaInfo` 判活、无 `createMs` 误用；不触碰 `sip-proxy` / `zlm-starter`，无上游重建。
