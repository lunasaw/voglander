# GB28181 可视化阶段 — 后端服务/接口 + 前端技术方案（1.0.5）

> 协议链路（下级接入 + 上级级联）E2E 测试已完成（见 `GB28181-2022-TDD-PLAN.md`）。
> 本阶段目标：把已就绪的 GB28181 能力**编排成可视化业务接口**，驱动 `vue-vben-admin` 前端。
> 核心非功能要求：**高并发 / 安全 / 可靠**。
>
> **代码核验（2026-06-04）**：方案已对照仓库代码进行逐项核验，共发现 7 处偏差，已在本文档修正。
> 完整落地实施方案见 [VISUALIZATION-BACKEND-IMPL.md](VISUALIZATION-BACKEND-IMPL.md)。

## 决策基线（已与需求方确认）

| 维度 | 决策 | 影响 |
|------|------|------|
| 实时推送 | **SSE**（Redis Pub/Sub 跨节点扇出） | 异步事件单向 server→前端；复用现有 JWT 鉴权；过 Nginx 友好 |
| 直播协议 | **HTTP-FLV** 默认（flv.js 已装） | 延迟 1-3s；前端零新增库；多协议 PlayUrls 由后端返回，前端自适应回退 |
| 多观看复用 | **单 INVITE 多路复用** + 引用计数 | 一通道一路拉流，N 观看者共享；最后一个退出才 BYE |
| 部署形态 | **多节点集群** | 会话/上下文外置 Redis；SSE 跨节点广播；媒体节点亲和 |

---

## 1. 现状与差距（Evidence-based）

### 1.1 已就绪（无需重建）

| 能力 | 位置 | 状态 |
|------|------|------|
| GB28181 出站命令（PTZ/Play/Playback/Query/Record/Config） | `GbDeviceCommandService` + 6 个 `VoglanderServer*Command` | ✅ 编译通过、命令可发 |
| 会话状态机 | `MediaSessionManager`（callId 业务主键，ACTIVE/CLOSED/INVITING/FAILED） | ✅ onInviteOk/onBye/onAck/onMediaStatus |
| 入站事件统一入口 | `VoglanderBusinessNotifier` → `Gb28181ProtocolHandler`（35 事件 switch） | ✅ |
| ZLM REST 全量 | starter `ZlmRestService`：`openRtpServer` / `getMediaList` / `getRtpInfo` / `getPlaybackUrls`→`PlayUrl(httpFlv/hls/rtmp/...)` / `startSendRtp` | ✅ |
| ZLM 节点负载均衡 | starter `NodeService.selectNode(key)` / `selectNode()` | ✅ |
| 设备/通道/媒体节点/拉流/推流 CRUD REST | `DeviceController` / `DeviceChannelController` / `MediaNodeController` / `StreamProxyController` / `PushProxyController`（均 `/api/v1/*`） | ✅ |
| 前端播放器基建 | `MediaPlayerManager` + `FlvPlayer`/`HlsPlayer`/`VideoJsPlayer`，`PlayUrls` 抽象（httpFlv/wsFlv/hls/rtmp/webrtc/...） | ✅ |
| 前端 media 页面 | node / list / stream-proxy / push-proxy（含 `StreamDetailModal`、`NodeSelector`） | ✅ |
| INVITE→ZLM 推流参照实现 | `CascadeMediaInviteListener`（级联转推：接收上级 INVITE→调 `startSendRtp` 把流转发给上级）| ✅ 可借鉴 inviteRealTimePlay 调用姿势；直播编排用 `openRtpServer` 被动收流，流向相反，不可混淆 |

### 1.2 核心差距（本阶段要补）

| # | 差距 | 证据 | 严重度 |
|---|------|------|--------|
| G1 | **GB28181 命令能力完全未暴露 HTTP** | `web/api/` 下无 ptz/play/playback/live 任何控制器 | 🚨 阻塞 |
| G2 | **直播无编排** —`GbDeviceCommandService.startPlay()` 返回 `null`，未选节点、未 openRtpServer、未拼 sdpIp/mediaPort、未关联 callId | `GbDeviceCommandService.java:74-79` 注释 "callId 通过 onInviteOk 异步产生，暂返回 null" | 🚨 阻塞 |
| G3 | **零实时推送** — 无 WebSocket/SSE，InviteOk/设备上下线/告警/流就绪无法到前端 | 全局 grep `SseEmitter/WebSocket/@MessageMapping` 命中 0 | 🚨 阻塞 |
| G4 | **无播放地址生成** — 后端无 getPlayUrl，前端目前直连 starter `/zlm/api/media/play-urls` | grep `getPlayUrl/PlayUrl` 在 web/integration/manager 命中 0 | 🔴 高 |
| G5 | **鉴权未在网关层强制** — `/api/v1/**` 仅挂 `RepeatSubmitInterceptor`，无全局 JWT 校验拦截器 | `ResourcesConfig.java:46-49` 仅注册 repeatSubmit | 🚨 安全 |
| G6 | **CORS 过宽** — `addAllowedOriginPattern("*")` + `allowCredentials(true)` | `ResourcesConfig.java:57-71` | 🔴 安全 |
| G7 | **告警未落库** — `Notify.Alarm` 仅日志，无 `tb_alarm`/`AlarmMapper` | `GB28181-2022-TDD-PLAN.md` 陷阱 11 | 🟡 中 |
| G8 | **前端无设备/通道/直播页面与 API 绑定** | `views/` 下仅 media 系列，无 device/channel/live | 🔴 高 |

---

## 2. 总体架构

```
┌────────────────────────── 前端 vue-vben-admin (web-antd) ──────────────────────────┐
│  设备树 │ 多屏直播墙 │ 回放 │ PTZ 控制盘 │ 告警中心 │ 电子地图(可选)                  │
│        ↑ REST(/api/v1/*)            ↑ SSE(/api/v1/stream/events)                   │
└──────────┼──────────────────────────────┼────────────────────────────────────────┘
           │                              │
┌──────────┼──────────────────────────────┼──── voglander-web (多实例) ──────────────┐
│  JwtAuthInterceptor(新, 全局)  ┌─ LivePlayController ─┐  ┌─ SseController(新) ─┐    │
│  ┌─ DeviceCmdController(新) ──┤  PtzController(新)    │  │  emitter 注册/心跳   │    │
│  └─ PlaybackController(新) ───┴─ AlarmController(新) ─┘  └──────────┬──────────┘    │
└──────────┬───────────────────────────────────────────────────────┼───────────────┘
           │ DTO                                                    │ 事件投递
┌──────────┼──── voglander-service (业务编排, 新增 live 包) ──────────┼───────────────┐
│  MediaPlayService(新): 选节点→openRtpServer→INVITE→等就绪→拼URL→引用计数            │
│  LiveStreamRegistry(新): streamId→会话/refCount/节点亲和 (Redis 外置)              │
│  SseEventBus(新): 本地 emitter + Redis Pub/Sub 跨节点扇出                          │
│  GbDeviceCommandService(改): startPlay 返回真实 streamId/callId                    │
└──────────┬───────────────────────────────────────────────────────┬───────────────┘
           │                                                        │
┌──────────┼── voglander-integration ──┐          ┌─ voglander-manager ─────────────┐
│ VoglanderServerMediaCommand(INVITE)  │          │ MediaSessionManager(会话状态机)  │
│ ZlmRestService(openRtp/playUrl/LB)   │          │ Device/Channel/MediaNodeManager │
│ VoglanderZlmHookService(流就绪回调)──┼──事件───→│ AlarmManager(新)                 │
│ VoglanderBusinessNotifier(SIP回调)───┼──事件───→│ → SseEventBus                    │
└──────────────────────────────────────┘          └──────────────────────────────────┘
                    │                                          │
              ┌─────┴─────┐                            ┌───────┴───────┐
              │ ZLM 集群   │                            │ Redis(A业务/B会话) │
              │ (多节点)   │                            │ Pub/Sub + 上下文  │
              └───────────┘                            └───────────────┘
```

**关键原则**：
- Controller 只做参数校验 + Req→DTO（`@Valid`），不写业务逻辑。
- 编排集中在 `voglander-service` 新增 `live` 包（`MediaPlayService` / `LiveStreamRegistry` / `SseEventBus`），与现有分层一致（Service 编排多 Manager）。
- 框架类型（GatewayEvent / ZLM 实体）隔离在 integration，不外泄到 web。

---

## 3. 直播编排：核心流程（解决 G2/G4）

### 3.1 callId 异步关联问题的解法

GB28181 的 `callId` 在 SIP 栈发 INVITE 时才生成，且 `onInviteOk` 异步回来。前端发起直播时拿不到同步标识。

**解法：以 `streamId` 作为前端可见的稳定业务主键**，callId 内部关联。
- 直播 streamId（多路复用键）：`gb_live_{deviceId}_{channelId}`（确定式，幂等）
- 回放 streamId（**不复用**，每会话独立）：`gb_back_{deviceId}_{channelId}_{ts}`

**关联机制（代码核验后修正）**：`onInviteOk` 事件的 payload 包含 `channelId`，但当前实现未提取。需做两处改动：
1. `Gb28181ProtocolHandler` Session.InviteOk 分支从 `event.payload()` 额外提取 `channelId`，传给 `onInviteOk(callId, deviceId, channelId)`
2. `MediaSessionManager.onInviteOk` 增加 `channelId` 参数，当找不到 callId 对应行时按 `(deviceId, channelId, status=INVITING)` 匹配唯一占位行 → 回填 callId，置 ACTIVE

关联键选 `deviceId + channelId`（分布式锁保证同通道同时仅一路直播 INVITE，联合唯一成立）。不依赖 ssrc（voglander 无法在发 INVITE 前控制 ssrc，由框架内部生成）。

### 3.2 直播首播流程（无现存流）

```
前端 POST /api/v1/live/start {deviceId, channelId, protocol=FLV}
  │
  ▼ LivePlayController → MediaPlayService.startLive(dto)
  1. streamId = gb_live_{deviceId}_{channelId}
  2. 分布式锁 RedisLockUtil.lock("live:lock:"+streamId, 10s)   ← 防并发重复 INVITE
  3. LiveStreamRegistry.get(streamId):
       命中 ACTIVE → refCount++ → 直接返回已缓存 PlayUrls (秒开, 不发 INVITE)  ★多路复用
       未命中 → 继续首播
  4. zlmNode = NodeService.selectNode(streamId)               ← 节点亲和:同streamId总落同节点
  5. mediaNodeDTO = mediaNodeManager.getDTOByServerId(zlmNode.getServerId())
       sdpIp = mediaNodeDTO.getHost()                         ← 从 tb_media_node 取，非 ZlmNode 字段
       [NAT 环境] 若 extend JSON 含 "mediaIp" 则优先使用
     openRtpResult = ZlmRestService.openRtpServer(zlmNode, {stream_id=streamId, port=0(自动), tcp_mode})
       → 得 ZLM 分配的 rtpPort
  6. 写 INVITING 占位会话(stream_id=streamId, deviceId, channelId, node_server_id=zlmNode.serverId, status=INVITING)
  7. VoglanderServerMediaCommand.inviteRealTimePlay(deviceId, sdpIp, rtpPort, streamMode)
       → SIP 栈发 INVITE, callId 生成
  8. 异步等待: CompletableFuture 注册到 LiveStreamRegistry, key=streamId
       超时 8s (DeferredResult / future.get(8,SECONDS))
  │
  ▼ (设备推流 → ZLM 收到 RTP → ZLM Hook on_stream_changed regist=true)
  9. VoglanderZlmHookService.onStreamChanged:
       → LiveStreamRegistry.markReady(streamId) → 完成 future
       → SseEventBus.publish(StreamReadyEvent{streamId})
  │
  ▼ (SIP 200 OK → VoglanderBusinessNotifier Session.InviteOk)
 10. MediaSessionManager.onInviteOk(callId, deviceId) → 按 streamId 回填 callId, status=ACTIVE
  │
  ▼ future 完成 / 或 step 9 先到
 11. playUrls = ZlmRestService.getPlaybackUrls(node, {app=rtp, stream=streamId})
       → 缓存进 LiveStreamRegistry + 返回前端 {streamId, callId, playUrls, status=ACTIVE}
 12. 释放锁
```

**降级**：若 8s 内 future 未完成 → 返回 `{streamId, status=PENDING}`，前端订阅 SSE `StreamReady` 后再取 PlayUrls（SSE 断连时 1.5s 轮询 `GET /api/v1/live/{streamId}` 兜底）。

### 3.3 停止流程（引用计数）

```
前端 POST /api/v1/live/stop {streamId}  (或前端断连/SSE 心跳超时触发)
  → MediaPlayService.stopLive(streamId):
      refCount-- (Redis DECR)
      if refCount <= 0:
        延迟 keepAliveSec(默认30s) 无人观看 → 真正 BYE:
          VoglanderServerMediaCommand.sendBye(callId)
          ZlmRestService.closeRtpServer(node, streamId)
          MediaSessionManager.onBye → status=CLOSED
          LiveStreamRegistry.remove(streamId)
      else: 仅减计数, 流保活        ← "复用+空闲保活" 策略
```

### 3.4 回放流程（不复用，独立会话）

与直播一致，但：streamId 带时间戳唯一；`invitePlayBack(deviceId, sdpIp, rtpPort, mode, startTime, endTime)`；额外暴露 `POST /api/v1/playback/control {streamId, action=PLAY/PAUSE/SEEK/SPEED, param}` → `VoglanderServerMediaCommand.controlPlayBack` / `ZlmRestService.seekRecordStamp/setRecordSpeed`。

---

## 4. 后端接口清单（voglander-web，新增）

> 全部挂 `/api/v1`，遵循现有 Controller 模板：`@Valid` + `*Req` 入参 → `WebAssembler` → DTO → Service/Manager → VO；统一 `AjaxResult`。

### 4.1 LivePlayController `/api/v1/live`

| Method | Path | Req | Resp | 说明 |
|--------|------|-----|------|------|
| POST | `/start` | `LiveStartReq{deviceId,channelId,protocol?,streamMode?}` | `LivePlayVO{streamId,callId,status,playUrls}` | 首播/复用，幂等 |
| POST | `/stop` | `LiveStopReq{streamId}` | `Boolean` | refCount-- |
| GET | `/{streamId}` | path | `LivePlayVO` | 轮询兜底/状态查询 |
| POST | `/keepalive` | `{streamId}` | `Boolean` | 前端心跳续约（防误回收） |
| GET | `/list` | `deviceId?` | `List<LivePlayVO>` | 当前活跃会话（含 refCount） |

### 4.2 PlaybackController `/api/v1/playback`

| Method | Path | Req | Resp |
|--------|------|-----|------|
| POST | `/start` | `PlaybackStartReq{deviceId,channelId,startTime,endTime,streamMode?}` | `LivePlayVO` |
| POST | `/stop` | `{streamId}` | `Boolean` |
| POST | `/control` | `PlaybackControlReq{streamId,action,param?}` | `Boolean` |
| POST | `/records` | `RecordQueryReq{deviceId,channelId,startTime,endTime}` | `Void`（异步，结果走 SSE `RecordInfo`）|

### 4.3 PtzController `/api/v1/ptz`

| Method | Path | Req | Resp |
|--------|------|-----|------|
| POST | `/control` | `PtzControlReq{deviceId,channelId,command,speed?}`（command=UP/DOWN/LEFT/RIGHT/ZOOM_IN/ZOOM_OUT/八方向） | `Boolean` |
| POST | `/stop` | `{deviceId,channelId}` | `Boolean` |
| POST | `/preset` | `PresetReq{deviceId,channelId,action=SET/GOTO/DEL,presetId}` | `Boolean` |

> 复用 `GbDeviceCommandService.ptzControl` / `VoglanderServerPtzCommand`（**11 个 PTZ 动作**：4 基础方向 + 4 对角线 + 2 变焦 + 1 停止，代码核验结果）。

### 4.4 DeviceCmdController `/api/v1/device-cmd`（设备主动指令）

| Method | Path | Req | 说明 |
|--------|------|-----|------|
| POST | `/query-catalog` | `{deviceId}` | 触发目录拉取，结果走 SSE `CatalogUpdated` |
| POST | `/query-info` | `{deviceId}` | DeviceInfo 查询 |
| POST | `/reboot` | `{deviceId}` | 重启（需二次确认 + 操作日志）|
| POST | `/record` | `{deviceId,channelId,action=START/STOP}` | 设备端录像 |

### 4.5 SseController `/api/v1/stream`（G3）

| Method | Path | 说明 |
|--------|------|------|
| GET | `/events` | `text/event-stream`；query 带 `topics=device,live,alarm`；返回 `SseEmitter`（超时 0=不超时，心跳 15s）|

事件类型（`event:` 字段）：`device.online` / `device.offline` / `channel.updated` / `live.ready` / `live.failed` / `live.closed` / `alarm.new` / `record.info` / `playback.eof`。data 为 FastJSON2 序列化的事件体。

### 4.6 AlarmController `/api/v1/alarm`（G7，依赖 §6.4 建表）

| Method | Path | Req | Resp |
|--------|------|-----|------|
| POST | `/getPage` | `AlarmQueryReq{deviceId?,level?,type?,startTime?,endTime?}` + page/size | `AlarmListResp` |
| GET | `/get/{id}` | path | `AlarmVO` |
| POST | `/ack` | `{id}` | `Boolean`（告警确认）|

---

## 5. 后端服务设计（voglander-service，新增 `live` 包）

### 5.1 MediaPlayService（编排核心）

```java
public interface MediaPlayService {
    LivePlayDTO startLive(LiveStartDTO dto);        // §3.2，幂等+多路复用
    boolean     stopLive(String streamId);          // §3.3，refCount
    LivePlayDTO getLive(String streamId);           // 状态查询
    void        keepAlive(String streamId);         // 续约
    LivePlayDTO startPlayback(PlaybackStartDTO dto);
    boolean     controlPlayback(String streamId, String action, String param);
}
```
- 依赖：`NodeService`(LB)、`ZlmRestService`(openRtp/playUrl)、`VoglanderServerMediaCommand`(INVITE)、`MediaSessionManager`(会话)、`LiveStreamRegistry`(refCount/亲和)、`RedisLockUtil`(并发锁)。
- 异常：抛 `ServiceException`（先在 `ServiceExceptionEnum` 补 `LIVE_INVITE_TIMEOUT` / `LIVE_NODE_UNAVAILABLE` / `STREAM_NOT_READY`）。

### 5.2 LiveStreamRegistry（会话注册中心，集群外置）

Redis 结构（**使用主 Redis-A**，SSE 是业务事件域，不混入 invite Redis-B）：
```
live:session:{streamId}  → Hash{callId, nodeServerId, sdpIp, rtpPort, status, sessionType, createMs, playUrlsJson}
live:refcount:{streamId} → String(int)，原子 INCR/DECR（注意：RedisCache 无此方法，需直接用 StringRedisTemplate.opsForValue().increment/decrement）
live:ready:{streamId}    → 临时 future 协调（本地 ConcurrentHashMap<streamId,CompletableFuture> + Redis Pub/Sub 跨节点唤醒）
live:node:{streamId}     → nodeServerId（节点亲和显式校验）
```
- TTL：session/refcount 设 `keepAlive + 心跳冗余`；流活跃期由 keepalive 续约。
- 多路复用判定 + refCount 增减必须在 `live:lock:{streamId}` 分布式锁（`RedisLockUtil`）内，避免竞态。

### 5.3 SseEventBus（实时推送，集群扇出）

```java
public interface SseEventBus {
    SseEmitter register(String userId, Set<String> topics);   // SseController 调用
    void publish(SseEvent event);                              // 业务侧调用（任意节点）
}
```
实现：
- **本地**：`Map<emitterId, EmitterHolder{emitter, userId, topics}>`；15s 心跳 `event:ping`；`onCompletion/onTimeout/onError` 清理。
- **跨节点**：`publish` 先 `redisTemplate.convertAndSend("sse:broadcast", json)`（用主 Redis-A `stringRedisTemplate`，通过 `RedisMessageListenerContainer` 订阅，不阻塞 Spring 容器）；每节点收到后向本地匹配 topic 的 emitter 下发。
- 事件来源接线：`Gb28181ProtocolHandler`（设备上下线、CatalogUpdated）、`VoglanderZlmHookService`（live.ready）、`AlarmManager`（alarm.new）、`MediaPlayService`（live.closed）统一调 `SseEventBus.publish`。

### 5.4 GbDeviceCommandService 改造（G2）

`startPlay` 不再返回 null：改为内部不直接编排（保持协议无关门面职责单一），由 `MediaPlayService` 调用 `inviteRealTimePlay` 并自行管理 streamId。`startPlay(DevicePlayReq)` 保留为"纯发 INVITE"语义供 `MediaPlayService` 复用，新流程的 streamId/节点选择上移到 `MediaPlayService`。

---

## 6. 数据模型变更

### 6.1 tb_media_session 增补（live 复用与亲和）
```sql
ALTER TABLE tb_media_session ADD COLUMN stream_id     VARCHAR(128);  -- 前端稳定主键
ALTER TABLE tb_media_session ADD COLUMN node_server_id VARCHAR(64);  -- 媒体节点亲和
ALTER TABLE tb_media_session ADD COLUMN ref_count     INT DEFAULT 0; -- 观看者计数(权威值仍以Redis为准,DB做持久快照)
CREATE UNIQUE INDEX uk_stream_id ON tb_media_session(stream_id);
CREATE INDEX idx_status_node ON tb_media_session(status, node_server_id);
```
> 同步更新 `sql/voglander.sql` 与 `voglander-web/src/test/resources/schema-sqlite.sql`（测试 `.db` 需手工建表，见测试基建备忘）。

### 6.2 tb_alarm 新建（G7）
```sql
CREATE TABLE tb_alarm (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id   VARCHAR(64)  NOT NULL,
  channel_id  VARCHAR(64),
  alarm_type  INT,           -- 1移动侦测/2设备/3故障/4视频丢失...
  alarm_level INT,           -- 1-4
  alarm_time  DATETIME,
  description VARCHAR(512),
  ack_status  INT DEFAULT 0, -- 0未确认/1已确认
  extend      TEXT,
  create_time DATETIME, update_time DATETIME,
  INDEX idx_device_time(device_id, alarm_time),
  INDEX idx_level(alarm_level)
);
```
配套：`AlarmDO`/`AlarmMapper`/`AlarmService`/`AlarmManager`(模板方法)/`AlarmDTO`/`AlarmAssembler`；`Gb28181ProtocolHandler.handleAlarm` 改为落库 + `SseEventBus.publish(alarm.new)`，按 `alarm.min-level` 过滤。

---

## 7. 高并发设计

| 关注点 | 方案 |
|--------|------|
| **拉流复用** | 单 INVITE 多路复用（§3）：N 观看者共享 1 路 GB28181 拉流 + 1 个 ZLM 流。资源占用与设备数（非观看数）成正比 |
| **并发重复 INVITE** | `RedisLockUtil` 按 streamId 加锁；锁内完成"查复用→开端口→发 INVITE"，避免同通道同时多次 INVITE 打爆设备 |
| **媒体节点负载均衡** | `NodeService.selectNode(streamId)` 一致性哈希 → 同流落同节点（亲和），跨流均摊；节点权重在 MediaNode 管理 |
| **事件分片** | 复用既有 `ShardDispatcher`（16 槽单线程，shardKey=deviceId）；SSE 推送走独立线程池，不阻塞 SIP 回调线程 |
| **SSE 连接规模** | 每节点 emitter 上限（如 5000）+ 超限拒绝；心跳 15s 回收死连接；topic 订阅减少无效推送 |
| **DB 写压力** | refCount 权威值在 Redis（INCR/DECR），DB 仅周期快照；心跳走 `patchLivenessWithCoalesce`（已实现，30s 合并写）|
| **播放地址缓存** | PlayUrls 在 `LiveStreamRegistry` 缓存，复用观看者秒取，不重复调 ZLM `getPlaybackUrls` |
| **读多写少 CRUD** | 设备/通道查询走既有 `@Cacheable`（Repository 层单对象）；列表查询短 TTL |

---

## 8. 安全设计

| # | 风险 | 方案 |
|---|------|------|
| S1 | **G5 网关无鉴权** | 新增 `JwtAuthInterceptor` 全局注册，`addPathPatterns("/api/v1/**")`，`excludePathPatterns` 放行 `/api/v1/auth/login`、`/api/v1/check`、`/api/v1/health`、`/api/v1/stream/events`(SSE 用 query token 单独校验)、swagger。校验 `Authorization: Bearer` → `JwtUtils` 解析 → 注入登录上下文。复用现有 `AuthService`/`JwtUtils` |
| S2 | **G6 CORS 过宽** | 生产 `allowedOriginPattern` 收敛为配置化白名单（`voglander.cors.allowed-origins`），保留 `allowCredentials`。开发 profile 才放开 |
| S3 | **SSE 鉴权** | EventSource 不能带 header → `/stream/events?token=xxx` query 传 JWT，SseController 单独校验；或短期一次性 ticket（`/api/v1/stream/ticket` 换发，60s 有效） |
| S4 | **播放地址防盗链** | ZLM 开 `hook.on_play` 鉴权（`VoglanderZlmHookService.onPlay` 已有桩，1.0.3 已加 `ZlmHookAuthService` IP白+HMAC+ts）；PlayUrls 带签名参数（`?token=hmac&expire=ts`），onPlay 校验 |
| S5 | **设备控制越权** | Sprint 2 内：PTZ/reboot/record 等控制指令校验用户**已登录**（JwtAuthInterceptor 保证，认证≠授权）；reboot 强制操作日志（deviceId + 操作人 userId）。**设备级数据权限（按设备分组）推迟到 Sprint 4 可选项**——现有 RoleManager/MenuManager 仅控菜单权限，无设备级过滤机制，Sprint 2 不引入。 |
| S6 | **ZLM Hook 入站鉴权** | Hook 回调走 `ZlmHookAuthService`（IP 白名单 + HMAC token + ts±300s，1.0.3 已实现） |
| S7 | **指令注入/重放** | 内部节点指令转发 `InternalAuthFilter`（IP白+HMAC+ts±60s，已实现）；外部 Req 经 `XssFilter`（已有）|
| S8 | **敏感信息** | ZLM secret / 设备密码不下发前端；PlayUrls 只含必要 host:port + 签名 |

---

## 9. 可靠性设计

| 机制 | 实现 |
|------|------|
| **INVITE 超时回收** | future 8s 超时 → 标 FAILED + closeRtpServer + 不残留占位会话；前端收 `live.failed` |
| **僵尸会话 GC** | `@Scheduled` 周期（如 60s）扫 `tb_media_session` status=ACTIVE/INVITING：①INVITING 超 N 分钟 → FAILED；②ACTIVE 但 ZLM `getMediaList`/`getRtpInfo` 查无此流 → 强制 CLOSED + 清 Registry（对账）|
| **refCount 对账** | GC 时以 ZLM 实际流状态为准修正 Redis refCount，防泄漏导致流永不回收 |
| **空闲保活回收** | refCount=0 后延迟 `keepAliveSec` 仍无人 → BYE（§3.3），避免快速切换反复点播开销 |
| **ZLM 节点故障** | starter LB 自动剔除离线节点（Hook `on_server_exited`→`updateNodeOffline` 已实现）；`StreamProxyZlmWrapperService` 已加 Resilience4j `@CircuitBreaker(name=zlm)`，扩展到 MediaPlayService 的 ZLM 调用 |
| **节点故障流迁移** | 节点掉线 → 该节点 ACTIVE 会话标 CLOSED + SSE `live.closed` → 前端自动重新 `/live/start`（落到新节点）|
| **SSE 断线重连** | 前端 EventSource 原生自动重连；后端 emitter 超时清理；关键状态（live 就绪）SSE + 轮询双通道，SSE 丢失不致命 |
| **跨节点一致性** | 会话/refCount/亲和全在 Redis；INVITE 上下文 `RedisInviteContextStore`（已实现）保证回包路由到正确节点 |
| **优雅停机** | 实例下线前 BYE 本节点 ACTIVE 会话 + 关 emitter，避免悬挂 |

---

## 10. 前端实现（vue-vben-admin / web-antd，解决 G828181）

### 10.1 API 绑定（`apps/web-antd/src/api/`，镜像后端）
- `api/device/device.ts` → `/api/v1/device/*`
- `api/device/channel.ts` → `/api/v1/deviceChannel/*`
- `api/live/live.ts` → `/api/v1/live/*`、`/api/v1/playback/*`
- `api/live/ptz.ts` → `/api/v1/ptz/*`
- `api/device/device-cmd.ts` → `/api/v1/device-cmd/*`
- `api/alarm/alarm.ts` → `/api/v1/alarm/*`
- `api/sse/event-source.ts` → SSE 封装（带 token、自动重连、topic、事件分发到 pinia store）

> 同步在 `vue-vben-admin/apps/web-antd/api/Voglander.openapi-*.json` 与 cursor-rule 登记新字段（前后端契约对齐，禁止前端造字段）。

### 10.2 页面（`views/`）

| 页面 | 路径 | 说明 |
|------|------|------|
| 设备管理 | `views/device/list` | 设备 CRUD、在线状态、触发目录查询 |
| 通道树 | `views/device/channel` 或左侧树组件 | 设备→通道层级，状态实时（SSE）|
| **实时监控墙** | `views/live/wall` | 1/4/9/16 多屏；拖通道入屏 → `live/start` → `MediaPlayerManager` 播 FLV；屏内 PTZ 控制盘叠加 |
| 录像回放 | `views/live/playback` | 时间轴 + 录像检索 + `playback/control`(进度/倍速/暂停) |
| PTZ 控制盘 | 组件 `components/PtzPanel.vue` | 八方向 + 变倍 + 预置位；按下发 control，松开发 stop |
| 告警中心 | `views/alarm/list` | 告警列表 + 实时弹窗（SSE `alarm.new`）+ 确认 |
| 电子地图（可选） | `views/live/map` | 设备点位 + 点击调流 |

### 10.3 复用既有基建
- 播放：`MediaPlayerManager`（已支持 FLV→HLS 自适应回退）直接吃后端返回的 `PlayUrls`。
- 多节点：`NodeSelector` + `X-Node-Key` 头模式可复用到通道选节点。
- 详情：`StreamDetailModal` 适配为通道直播弹窗。

---

## 11. 实施阶段（TDD，每阶段独立可验收）

> 遵循全局规范：先红后绿、增量提交、Manager 集成测试 + Controller 纯 Mockito。

### Sprint 1 — 直播闭环（P0，打通 G1/G2/G4 + G3 最小集）
1. **【前置，必须最先做】** `Gb28181ProtocolHandler` Session.InviteOk 分支提取 payload.channelId；`MediaSessionManager.onInviteOk` 增加 channelId 参数，按 (deviceId,channelId,INVITING) 匹配占位行回填 callId（§3.1 callId 关联机制）
2. DB：`tb_media_session` 增列 stream_id/node_server_id/ref_count + 索引（两处 schema 同步）
3. `LiveStreamRegistry`（Redis refCount 用 StringRedisTemplate.opsForValue().increment/decrement 原子操作）+ 单测（并发 refCount 幂等，8 线程）
4. `MediaPlayService.startLive/stopLive`（选节点→取 mediaNodeDTO.host 作 sdpIp→openRtp→INVITE→等就绪→拼URL→复用）+ Manager 集成测试
5. `SseEventBus`（本地 emitter + Redis Pub/Sub，RedisMessageListenerContainer 订阅）+ `SseController` + 集成测试（事件投递）
6. `LivePlayController` + `LiveWebAssembler`（命名与既有各域 XxxWebAssembler 保持一致）+ Controller 纯单测
7. `VoglanderZlmHookService.onStreamChanged` 接 `live.ready`；`MediaPlayService` 接 future
8. **验收**：前端发 `/live/start` → 8s 内拿到 httpFlv 地址 → flv.js 播放；第二观看者秒开（refCount=2）；停流 refCount 归零后 BYE

### Sprint 2 — 控制 + 安全（P0 安全 + P1 控制）
8. `JwtAuthInterceptor` 全局鉴权（S1）+ CORS 收敛（S2）+ SSE token（S3）+ 单测（放行/拦截矩阵）
9. `PtzController` + `PlaybackController`(start/stop/control) + 单测
10. 回放编排（独立会话、不复用、MANSRTSP 控制）
11. PlayUrls 签名 + `onPlay` 校验（S4）
12. **验收**：未带 token 访问 `/api/v1/device/*` 返回 401；PTZ 控制设备转动；回放可拖进度/倍速

### Sprint 3 — 告警 + 可靠性 + 前端完善（P1/P2）
13. `tb_alarm` + Alarm 全链路（DO/Mapper/Service/Manager/Controller）+ `handleAlarm` 落库 + SSE（G7）
14. 僵尸会话 GC + refCount 对账 + 节点故障流迁移（§9）
15. `DeviceCmdController`（目录/信息/重启/录像）+ 操作日志（S5）
16. 前端：设备管理 / 通道树 / 监控墙 / 回放 / 告警中心页面 + SSE store 接线
17. **验收**：告警实时弹窗 + 落库可查；杀 ZLM 节点流自动迁移；监控墙 16 屏稳定

### Sprint 4 — 加固（P2）
18. 优雅停机、SSE 连接上限压测、媒体节点权重调优
19. 集群双实例联调（SSE 跨节点扇出、会话亲和、INVITE 跨节点回包）
20. 监控埋点（live 成功率/首播延迟/refCount 分布）接入既有 `MONITORING.md` 指标体系

---

## 12. 风险与缓解

| 风险 | 缓解 |
|------|------|
| `openRtpServer` 端口/SDP 与设备协商失败（TCP/UDP 模式） | 默认 UDP，支持 streamMode 切换；失败降级重试 TCP_PASSIVE |
| ZLM 流就绪 Hook 与 SIP InviteOk 时序竞争 | 双触发（Hook on_stream_changed 与 onInviteOk 任一就绪即可拼 URL），以 ZLM 实际流为权威 |
| 集群 SSE 风暴（高频设备上下线） | 事件合并（debounce）+ topic 过滤 + 分片线程池 |
| 多路复用下个别观看者网络差影响整路 | ZLM 侧每观看者独立播放连接，拉流单路不受播放端影响（ZLM 原生支持）|
| 旧 `startPlay` 调用方（如登录链路）受改造影响 | `startPlay` 保留"纯发 INVITE"语义；编排上移不破坏既有签名 |
| 测试 `.db` schema 不自动执行 | 新增表手工建入持久化 `test-app.db`（见测试基建备忘）|

---

## 13. 验收标准（Definition of Done）

- [ ] 直播：首播 ≤ 8s 出流，复用观看 ≤ 1s 秒开，停流引用计数正确归零并 BYE
- [ ] 安全：`/api/v1/**`（除白名单）无 token 一律 401；CORS 白名单生效；PlayUrls 防盗链生效
- [ ] 实时：设备上下线/告警/流就绪经 SSE 到前端 ≤ 1s；多节点任一节点事件全网可达
- [ ] 高并发：单通道 N 观看仅 1 路拉流；并发 start 同通道不重复 INVITE
- [ ] 可靠：INVITE 超时不残留；僵尸会话被 GC；ZLM 节点故障流自动迁移
- [ ] 回放：检索/播放/拖进度/倍速可用
- [ ] 告警：实时弹窗 + 落库分页查询 + 确认
- [ ] 前端：设备树 / 监控墙(≥9屏) / 回放 / PTZ / 告警中心可用，契约与后端字段一致
- [ ] 测试：Sprint 内 P0 用例 100%，Manager 集成测试 `@Transactional` 正确，Controller 纯 Mockito
- [ ] 集群：双实例联调通过（SSE 扇出 / 会话亲和 / INVITE 跨节点回包）
```
