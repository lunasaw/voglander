# Voglander 1.0.3 合并实施清单（协议可扩展 × 高并发优化）

> 版本：1.0.3 ｜ 编写日期：2026-06-01 ｜ 对应分支：`dev_merge_sip`
> 基线：sip-gateway 1.8.0 接入完成 + GB28181 出站命令 envelope 化完成
> 上游设计：[OPTIMIZATION-DESIGN.md](./OPTIMIZATION-DESIGN.md)（高并发/多节点）｜ [PROTOCOL-EXTENSIBILITY-DESIGN.md](./PROTOCOL-EXTENSIBILITY-DESIGN.md)（协议/Gateway 可插拔）｜ [ARCHITECTURE.md](./ARCHITECTURE.md)（现状）
>
> **文档性质**：可执行落地清单。把两份正交设计稿合并为**一条串行实施路线**，逐阶段给出改动点、验收项、重叠协调纪律。所有源码事实已对照 `dev_merge_sip` 逐项核验（见 §2）。

---

## 目录

1. [为什么需要这份合并清单](#1-为什么需要这份合并清单)
2. [源码事实核验（落地前提）](#2-源码事实核验落地前提)
3. [两个物理重叠点与协调纪律](#3-两个物理重叠点与协调纪律)
4. [合并实施路线总览](#4-合并实施路线总览)
5. [Phase 0：调度启用（硬阻塞）](#phase-0调度启用硬阻塞)
6. [Phase 1：缓存正确性](#phase-1缓存正确性)
7. [Phase 2a：心跳定向更新 + 单调写](#phase-2a心跳定向更新--单调写)
8. [Phase 3：入站协议缝（PROTOCOL S1~S2）](#phase-3入站协议缝protocol-s1s2)
9. [Phase 4：事件管线（OPTIMIZATION Stage 3，叠加到 handler）](#phase-4事件管线optimization-stage-3叠加到-handler)
10. [Phase 5：出站门面 + 命令路由（PROTOCOL S3~S4 × OPTIMIZATION §10.4）](#phase-5出站门面--命令路由protocol-s3s4--optimization-104)
11. [Phase 6：外部韧性 + Redis 隔离](#phase-6外部韧性--redis-隔离)
12. [Phase 7：Redis 热存（按需）](#phase-7redis-热存按需)
13. [Phase 8：可插拔验证 + 可观测 + 异常规范](#phase-8可插拔验证--可观测--异常规范)
14. [并行/串行矩阵](#14-并行串行矩阵)
15. [全局验收门槛](#15-全局验收门槛)

---

## 1. 为什么需要这份合并清单

两份设计稿**没有设计层面的硬冲突**，但在两个代码位置物理重叠，必须串行实施、约定先后：

- **PROTOCOL-EXTENSIBILITY = 结构轴**：类型放哪、入站如何分发、框架类型隔离。编译期关注点。
- **OPTIMIZATION = 运行时轴**：并发有序、单调一致、降写频、多节点。运行时关注点。

二者在 **入站 `VoglanderBusinessNotifier`** 与 **出��命令下发路径** 物理重叠。若两条分支并行改同一段 switch / 同一下发入口，必然合并冲突。本清单把它们排成单一线性顺序，"结构先于优化"，让每个 Phase 独立可编译、可灰度、可回滚。

**正向协同**：PROTOCOL 的归一化 `DeviceEvent(... timestampMs, nodeId ...)` 恰好提供 OPTIMIZATION 单调写所需的 `timestampMs` 与命令路由所需的 `nodeId`——先立这道缝，后续优化更顺。

---

## 2. 源码事实核验（落地前提）

> 已对照 `dev_merge_sip` 源码逐项核验。✅=与设计稿一致；⚠️=与设计稿措辞有偏差，落地以此处为准。

### 2.1 入站（两文共用）

| 事实 | 状态 | 证据 |
|------|------|------|
| `VoglanderBusinessNotifier implements BusinessNotifier`，`notify` 标 `@Async("sipNotifierExecutor")` | ✅ | `wrapper/gb28181/notifier/VoglanderBusinessNotifier.java:53,65` |
| 含约 35 分支大 switch（Lifecycle 5 / Notify 7 / Response 16 / Session 7） | ✅ | 同上 `:84-179` |
| 硬编码 `DeviceAgreementEnum.GB28181_IPC` | ✅ | 同上 `:194` |
| 目录走 N+1 `addChannel` 逐条 | ✅ | 同上 `:260-281` |
| `Gb28181InviteResponseController` / `POST /gateway/gb28181/invite/response` | ✅ 悬空 | 仅注释 `:171`，全仓库无类无端点（G2 缺口属实） |
| `ApplicationWeb` **无** `@EnableScheduling`，有 `@EnableSipServer` | ✅ | `voglander-web/.../web/ApplicationWeb.java:15-20`（B1 硬阻塞属实） |
| `sipNotifierExecutor` = 8/32/1000 + **CallerRunsPolicy** | ✅ | `manager/thread/ThreadPoolConfig.java:54-66`（H2 属实） |
| `MediaSessionManager.onInviteOk/onAck/onInviteFailure` 先查后插 | ✅ | `manager/manager/MediaSessionManager.java:223-265`（B3 属实） |
| `DeviceManager.clearCache` 调 `cache.clear()`，evict 裸 `id`+`do:`/`dto:`×{id,deviceId} 共 5 类；`@Cacheable(key="#deviceId")` 写裸 deviceId → 三套 key 互不命中 | ✅ | `manager/manager/DeviceManager.java:391-420,525-530`（P1/P2 属实） |

### 2.2 出站（PROTOCOL 重点）

| 事实 | 状态 | 证据 |
|------|------|------|
| `DeviceCommandService` 仅 `queryChannel`/`queryDevice` 两方法 | ✅ | `voglander-client/.../service/device/DeviceCommandService.java` |
| `GbDeviceCommandService` 类**不存在** | ✅ 悬空 | 全仓库无该类 |
| 常量 `DEVICE_AGREEMENT_SERVICE_NAME_GB28181 = "GbDeviceCommandService"` | ✅ | `voglander-common/.../constant/device/DeviceConstant.java:10` |
| `DeviceAgreementService.getCommandService(type)` 对 GB 走 `SpringBeanFactory.getBean("GbDeviceCommandService")` | ✅ | `voglander-service/.../service/command/DeviceAgreementService.java:19-31` |
| `AbstractVoglanderServerCommand.dispatchEnvelope` import 框架类��� `GatewayCommand`/`CommandHandlerRegistry` | ✅ | `wrapper/gb28181/server/command/AbstractVoglanderServerCommand.java`（H3 属实） |
| 跨节点命令路由（`/internal/sip/command`、`dev:node:`）未实现，仅 `node-id`/`forward-timeout-ms` 配置占位 | ✅ | `application-inte.yml:55-59`（A1 属实） |
| 6 个命令 bean 仅 integration 层使用，上层无直接注入 | ✅ | S4「收口」确无现存调用者 |

### 2.3 两处偏差（落地以此为准）

- **⚠️-1 命令类是 6 个**：`VoglanderServerDeviceCommand` / `Ptz` / `Media` / `Config` / `Alarm` / **`Record`**。无独立 Status/Catalog 类（目录/状态查询在 `VoglanderServerDeviceCommand` 内）。PROTOCOL §7.2 示例按 6 个委托即可。
- **⚠️-2 出站门面是"修缺陷"而非"补整洁"**：`getCommandService()` 对 GB 设备查 `"GbDeviceCommandService"` bean，**该 bean 不存在 → 命中即抛 RuntimeException**。落地前须确认 `DeviceRegisterServiceImpl` 等是否真走到此分支；若走到，**该出站路径目前就是坏的**，这会抬高 Phase 5 的优先级（从"可扩展性增强"升级为"缺陷修复"）。

> 行动项 ☐：在 Phase 5 启动前，grep `getCommandService(` 全部调用点，确认当前是否有活跃调用命中 GB 分支。

---

## 3. 两个物理重叠点与协调纪律

### 3.1 重叠点 A：入站 notifier

- PROTOCOL S2：`notify` 退化为纯翻译器 `GatewayEvent→DeviceEvent→dispatcher.dispatch()`，switch 搬进 `Gb28181ProtocolHandler`（不含框架类型）。
- OPTIMIZATION Stage 3：`notify` 改极轻 ingress 池（仅 `hash(deviceId)+offer`），重活进单线程分片槽；业务分支加单调写/batchUpsert。

**合并后唯一形态**：

```
notify(GatewayEvent) [ingress池, @Async, 2~4线程, 禁CallerRuns]
  → 轻量翻译 DeviceEvent（仅切三段 type + 拷 Map 引用，不做 FastJSON2 反序列化）
  → shard = floorMod(shardKey.hashCode(), N) → shards[shard].offer(DeviceEvent)  // 立即归还 SIP 线程
       ↓（每槽单线程消费）
  → InboundEventDispatcher.dispatch → Gb28181ProtocolHandler.handle
       // 重活在此：payload 反序列化 + 单调写 + batchUpsert
```

**纪律**：
- `DeviceEvent.payload` 保持原始 `Map`，昂贵的 `toEntity(payload, X.class)` 留在 handler（分片线程）——同时满足 PROTOCOL"框架类型隔离"与 OPTIMIZATION"入队只放轻量事件"。
- **分片 key 的 null 兜底（必须约定）**：Session 事件（Invite/Ack/Bye）以 callId 为主、deviceId 可空。约定 `shardKey = deviceId != null ? deviceId : correlationId`。
- `dispatcher` 与 `handler` 运行在**分片消费线程**内，dispatcher 本身无需感知分片。

### 3.2 重叠点 B：出站命令路径

- PROTOCOL S3/S4：建 `GbDeviceCommandService` 门面，委托 6 命令 bean；`DeviceAgreementService` 按协议选门面。门面在命令 bean **之上**。
- OPTIMIZATION §10.4：命令下发入口加 `dev:node` 路由表 + `/internal/sip/command` HTTP 转发。

**合并后唯一形态**：`门面 → 命令 bean → dispatchEnvelope(+路由拦截)`

**纪律**：
- **路由拦截点放在 `dispatchEnvelope` 层（或其包装）**，对门面透明。节点路由是 voglander 自有逻辑，非框架类型，不破坏 PROTOCOL"框架类型零外泄"。
- 门面不感知路由，路由不感知门面。

### 3.3 G2 / ServerInvite 悬空引用

两文都引用悬空的 `Gb28181InviteResponseController` / invite-response 端点，均列为**独立修**。指定**单一 owner** 处理（删注释或补端点），避免两边都改注释引冲突。本清单将其挂在 Phase 8。

---

## 4. 合并实施路线总览

| Phase | 来源 | 目标 | 重叠点 | 风险 | 灰度开关 |
|-------|------|------|--------|------|----------|
| **0** | OPT Stage 0 | 启用 `@EnableScheduling` | 无 | 极低 | — |
| **1** | OPT Stage 1 | 缓存 key 统一 + 去 clear | 无 | 低 | `cache.precise-evict.enabled` |
| **2a** | OPT Stage 2a | 心跳定向更新 + 单调写 | 无 | 低 | `device.liveness.coalesce.enabled` |
| **3** | PROTOCOL S1~S2 | 入站协议缝（DeviceEvent/dispatcher/handler） | **A** | 中 | 行为等价，无需开关 |
| **4** | OPT Stage 3 | 分片 + 单调 + batchUpsert（叠加到 handler） | **A** | 中 | `event.shard.enabled` |
| **5** | PROTOCOL S3~S4 + OPT §10.4/Stage5 | 出站门面 + 命令亲和路由 | **B** | 中高 | `command.affinity-route.enabled` |
| **6** | OPT Stage 4 | Resilience4j + Redis-A/B 物理隔离 | 无 | 中 | 熔断器配置 |
| **7** | OPT Stage 2b | Redis 热存（仅当 2a 不足） | 无 | 高 | `device.state.hot-store.enabled` |
| **8** | PROTOCOL S5 + OPT Stage 6 + G2 | 可插拔验证 + 可观测 + 异常规范 + 修 G2 | 无 | 低 | — |

**铁律**：Phase 3 必须先于 Phase 4（结构先于优化，A 点串行）；Phase 5 内部门面先于路由（B 点）。Phase 0/1/2a 零重叠，可最先并尽快落地。

---

## Phase 0：调度启用（硬阻塞）

> 来源 OPT Stage 0 / B1。后续所有 `@Scheduled` 兜底任务的前置；不做则 Phase 4~7 周期任务静默失败。

**改动清单（单 PR）**
- ☐ `ApplicationWeb` 加 `@EnableScheduling`（与 `@EnableSipServer` 同级）。
- ☐ 核查既有 2 处 `@Scheduled` 启用后开始执行（此前从未运行）：
  - `StreamProxyBizServiceImpl.syncAllEnabledStreamProxyStatus()` `@Scheduled(fixedRate=30000)`（30s）；
  - `SpringDynamicTask.execute()` `@Scheduled(cron="0 0/5 * * * ?")`（5min）。
- ☐ PR 描述**点名**上述两任务，请评审人确认其语义（尤其是否批量写库）；如有副作用，同 PR 加开关或临时停用。

**验收**
- ☐ 启动日志可见 `TaskScheduler` 初始化。
- ☐ 两个既有任务按周期触发（日志/埋点确认）。
- ☐ 无未预期的批量写库。

---

## Phase 1：缓存正确性

> 来源 OPT Stage 1 / P1+P2+B2。**P1、P2 必须同提交**（单独去 clear 立即暴露脏读）。

**改动清单（单 PR）**
- ☐ 抽 `DeviceCacheKey` 工具：`byId(id)` / `byDeviceId(deviceId)`，三处（`@Cacheable`、手动 evict、`clearCache`）共用。
- ☐ 改 `@Cacheable` key 表达式：`getDtoByDeviceId` 等所有缓存读取点 → `key="'deviceId:'+#deviceId"`。
- ☐ 删除 `clearCache` 内**全部 5 类**旧 evict（裸 `id` + `do:`/`dto:`×{id,deviceId}），换 `DeviceCacheKey`。
- ☐ 去掉 `cache.clear()`，改精确 evict（`DeviceManager.java:415`）。
- ☐ 列表缓存独立 cacheName（`device:list`）+ 短 TTL，单对象写不连坐列表。
- ☐ device 缓存 TTL 降到 2~3min + 随机抖动。
- ☐ 延迟双删放 **Redis ZSet** `cache:evict:delay`（非 JVM 定时器），全节点共消费（`@Scheduled(fixedDelay=200)` ZREM 原子取出）。
- ☐ 一次性清 Redis 残留：`redis-cli --scan --pattern '<key-prefix>:cache:device::*' | xargs -r redis-cli DEL`（前缀按 `spring.cache.redis.key-prefix` 实际值）。
- ☐ 排查 `StreamProxyManager` / `MediaNodeManager` 是否同模式（同模板复制，大概率同病），一并修。

**验收**
- ☐ 缓存命中率压测 > 80%。
- ☐ 并发读写一致性测试无脏读。
- ☐ 测试用例覆盖"裸 id key 是否仍残留"。

---

## Phase 2a：心跳定向更新 + 单调写

> 来源 OPT Stage 2a。不引入 Redis 真相源，仅消写放大，**零新增一致性风险**。

**改动清单**
- ☐ `DeviceManager.patchLiveness(deviceId, status, keepaliveTime)`：单条两列 `UPDATE`，命中 `device_id` 唯一索引；**含单调条件** `AND (keepalive_time IS NULL OR keepalive_time < ?)`，旧时间戳不覆盖。
- ☐ 离线终态 `patchOfflineTerminal`：**独立、无单调条件**同步写（终态优先）。
- ☐ 心跳合并：节点本地 `lastPersistTs[deviceId]`，距上次 < `coalesceWindow`（30s）则只刷缓存 keepaliveTime、不写 DB。**仅作加速，不承载持久语义**（漂移/重启后补写一条无害）。
- ☐ `DeviceRegisterServiceImpl.keepalive` / 上线 / 离线改调 `patchLiveness`。
- ☐ `VoglanderBusinessNotifier` 心跳/上线/离线分支改调 `patchLiveness`。
  - ⚠️ **与 Phase 3 顺序绑定**：此处改的是 notifier 分支体；Phase 3 会把这些分支整体平移到 handler。线性顺序无碍，但**禁止与 Phase 3 并行分支**。
- ☐ 补索引（MySQL，`pt-online-schema-change` 在线加）：
  - `tb_device ADD INDEX idx_status_keepalive (status, keepalive_time)`；
  - `tb_device ADD INDEX idx_server_ip (server_ip)`；
  - `tb_device_channel ADD INDEX idx_device_id (device_id)`（复合 UNIQUE 最左前缀是 channel_id，无法按 device_id 扫）。
  - ⚠️ **不要** ADD `uk_channel_device`（已有 `UNIQUE KEY channel_id (channel_id, device_id)`，仅命名不同）；**不要** ADD `call_id` UNIQUE（`uk_call_id` 已存在）。

**验收**
- ☐ 心跳 DB 写频次降 ≥ 50%，心跳 P99 < 50ms。
- ☐ 单调条件专项：旧 ts 注入不覆盖新 keepalive。
- ☐ DB 仍为唯一真相源，无新一致性风险。
- ☐ 灰度开关 `device.liveness.coalesce.enabled=false` 可回退旧路径。

---

## Phase 3：入站协议缝（PROTOCOL S1~S2）

> 来源 PROTOCOL S1~S2。立两道 voglander 自有端口，把框架类型挡进适配器。**行为等价改造**，不引入新行为。

**S1：立端口（空注册表也能启动）**
- ☐ 新增 `voglander-client/.../domain/event/DeviceEvent.java`（record，含 `protocol/group/name/deviceId/correlationId/timestampMs/payload(Map)/nodeId` + `type()`）。**不含任何 sip-gateway 类型**。
- ☐ 新增 `voglander-manager/.../event/ProtocolEventHandler.java`（`protocol()` + `handle(DeviceEvent)`）。
- ☐ 新增 `voglander-manager/.../event/InboundEventDispatcher.java`：注入 `List<ProtocolEventHandler>`，建 `protocol→handler` 映射，按 `event.protocol()` 路由；无 handler 则告警丢弃。
- ☐ 验收：`mvn compile` 通过；dispatcher bean 起得来（空注册表）。

**S2：GB 入站搬迁**
- ☐ 新增 `wrapper/gb28181/handler/Gb28181ProtocolHandler implements ProtocolEventHandler`（`protocol()="gb28181"`）：**平移** `VoglanderBusinessNotifier` 整段 switch（Lifecycle/Notify/Response/Session 全部分支），只认 `event.group()+"."+event.name()`，**不 import 任何框架类型**；payload 仍走 FastJSON2。
- ☐ `VoglanderBusinessNotifier` 退化为纯翻译器：`@Async("sipNotifierExecutor")` 保留；`GatewayEvent.type()` 三段切分 → `DeviceEvent` → `dispatcher.dispatch(...)`。
- ☐ 硬编码 `GB28181_IPC` 随分支平移进 handler（仍是 GB 专属逻辑，合理）。
- ☐ 逐条对照 `ARCHITECTURE.md §7.2` 事件表，确认无漏分支；保留原 notifier git 历史以比对。

**验收**
- ☐ GB 回调集成测试绿，事件仍落到原业务服务（`DeviceRegisterService`/`DeviceManager`/`MediaSessionManager`），行为不变。
- ☐ handler 内无 `io.github.lunasaw.sipgateway.*` / `GatewayEvent` import。
- ☐ `mvn compile` + 全量 envelope 测试绿。

---

## Phase 4：事件管线（OPTIMIZATION Stage 3，叠加到 handler）

> 来源 OPT Stage 3。在 Phase 3 抽出的结构上叠加并发控制。**A 点在此最终成形**。

**改动清单**
- ☐ 新增 ingress 专用池：核心 2~4 线程 + 大队列（10000），**禁用 CallerRuns**，满则丢冗余 Keepalive（保 Register/Invite/Offline）。
- ☐ `VoglanderBusinessNotifier.notify` `@Async` 改用 ingress 池；仅做轻量翻译 + `shard = floorMod(shardKey.hashCode(), N)` + `offer`，立即归还 SIP 线程。
  - ☐ **shardKey null 兜底**：`shardKey = deviceId != null ? deviceId : correlationId`（§3.1 纪律）。
- ☐ 新增 16~32 个单线程分片槽 + 有界队列（每槽 2000，满则丢冗余 Keepalive 并计数告警）；同 shardKey 恒定同槽。
- ☐ 分片槽消费者内调 `InboundEventDispatcher.dispatch` → handler；**payload 反序列化在此**（不在 ingress）。
- ☐ 单调写贯彻（跨节点正确性唯一依靠）：handler 内所有状态写经 `patchLiveness`（Phase 2a 已含 DB 单调条件）；不得以"已分片"为由省略单调条件。
- ☐ 目录批量化：`Gb28181ProtocolHandler.handleCatalog` 改 `DeviceChannelManager.batchUpsert(channels)`（取代 N+1 `addChannel`）。
  - MySQL `INSERT ... ON DUPLICATE KEY UPDATE`（命中 `(channel_id, device_id)`）；SQLite `INSERT ... ON CONFLICT DO UPDATE`；单事务批量。
- ☐ **B3 upsert 改造（必含）**：`MediaSessionManager.onInviteOk/onAck/onInviteFailure` 从"先查后插"改 DB 原生 upsert + `catch DuplicateKeyException` 转 UPDATE；不依赖框架 `invite-idempotency-window-ms` 做跨节点幂等。
- ☐ "设备不存在"优雅处理：`markOnline`/`heartbeat` 命中未注册设备 → 降级补登记，不抛 `RuntimeException`。

**验收**
- ☐ 乱序注入（Online 早于 Register、旧心跳后到）→ 最终状态正确。
- ☐ 节点间漂移（Register→node-1、旧 Online→node-2）→ 最终 status=ONLINE 且 keepalive 取较新值（验证分片串行 ≠ 跨节点、单调写兜底）。
- ☐ 64 路目录单次落库；跨节点重传不撞 UNIQUE。
- ☐ 双节点并发 upsert 同 callId 不抛 UNIQUE 异常（B3 专项）。
- ☐ SIP 线程不被 ingress 拖慢；监控单槽队列深度。

---

## Phase 5：出站门面 + 命令路由（PROTOCOL S3~S4 × OPTIMIZATION §10.4）

> 来源 PROTOCOL S3~S4 + OPT §10.4/Stage5。**B 点在此成形**。门面先、路由后。

**前置核查**
- ☐ grep `getCommandService(` 全部调用点，确认当前是否有活跃调用命中 GB 分支（⚠️-2：命中即抛 `bean 不存在`，是潜在缺陷）。

**S3：立门面 + 修缺陷**
- ☐ 扩展 `DeviceCommandService` 接口（协议无关语义，入参用协议无关 Req/DTO，不含 SDP/streamMode 裸参）：`queryDeviceInfo` / `queryCatalog` / `ptzControl(DevicePtzReq)` / `startPlay(DevicePlayReq):callId` / `startPlayback(DevicePlaybackReq):callId` / `stopPlay(callId)` / `reboot`。
- ☐ 新增协议无关 Req：`voglander-client/.../domain/device/qo/{DevicePtzReq,DevicePlayReq,DevicePlaybackReq}.java`。
- ☐ 新建 `GbDeviceCommandService`（`@Service("GbDeviceCommandService")`，对齐常量 `DEVICE_AGREEMENT_SERVICE_NAME_GB28181`），**委托现有 6 命令 bean**（Device/Ptz/Media/Config/Alarm/Record）；不支持的能力返回 `failure(NOT_SUPPORTED)`。
- ☐ 验收：`DeviceAgreementService.getCommandService(GB).startPlay(...)` 走通（修掉 bean 缺失缺陷）。

**S4：收口上层**
- ☐ 上层若有直接注入 `VoglanderServer*Command` 处改走门面（核查确认当前**无**生产调用者，主要是预防性约定 + 文档）。

**§10.4：命令亲和路由（必修 A1）**
- ☐ 路由表 `dev:node:{deviceId}→nodeId`（Redis-A），注册/上线/心跳写入并随心跳续期。
- ☐ 路由拦截点放 **`dispatchEnvelope` 层（或其包装）**，对门面透明（§3.2 纪律）：查 `dev:node`，本节点/未知则本地下发，否则 HTTP 转发。
- ☐ 接收端 `POST /internal/sip/command`（仅集群内网），收到调本地 `ServerCommandSender`。
- ☐ **B5(a) 鉴权强约束（缺一不可合并）**：IP 白名单（仅 `gateway.nodes` 节点 IP）+ 共享密钥 HMAC/token 头 + 路径限定 `/internal/**`，禁外网 LB 透出。
- ☐ 节点存活表 `node:alive:{nodeId}`（Redis-A，TTL 15s 续期）。
- ☐ **B5(b) 长心跳漂移收敛**：`onLifecycleOnline` 显式续期 `dev:node`，把收敛窗口从"下一心跳"压到"上线事件"。

**验收**
- ☐ 双节点：命令打非设备节点能正确转发并回包。
- ☐ `dev:node` 缺失时回退本地、502/503 触发 200ms×3 短重试后成功。
- ☐ 伪造来源 IP / 缺密钥 / 错密钥 的 `/internal/sip/command` 均返回 401/403（B5 安全 3 路径专项）。

---

## Phase 6：外部韧性 + Redis 隔离

> 来源 OPT Stage 4。

**改动清单**
- ☐ 引入 Resilience4j：ZLM 调用 `@CircuitBreaker` + `@Bulkhead(THREADPOOL)` + `@TimeLimiter` + fallback。
- ☐ 补全 ZLM `onPlay`/`onPublish` 鉴权（密钥/IP 白名单/会话票据），关默认放行空洞（P10）。
- ☐ **A5/B6 Redis-A/B 物理隔离（必含）**：
  - 新增 `gateway.gb28181.store.redis.*` 配置（独立于 `spring.data.redis.*`）；
  - 定义 `inviteRedisConnectionFactory` + `inviteStringRedisTemplate`（独占）；
  - `RedisInviteContextStore` 改 `@Qualifier("inviteStringRedisTemplate")` 注入，不再共享默认 bean；
  - `HealthCheckController` 分别上报 Redis-A / Redis-B。
- ☐ Redis-A 故障读写对称降级回 DB + 恢复预热（含 `dev:node` 重建）。

**验收**
- ☐ 混沌断 Redis-A / ZLM，核心链路存活、降级对称。
- ☐ 断 Redis-A 时 INVITE（Redis-B）不受影响（需独立实例部署后验证）。
- ☐ ZLM 抖动不传导 SIP。

---

## Phase 7：Redis 热存（按需）

> 来源 OPT Stage 2b。**仅当 Phase 2a 实测仍不足**（50K+ 设备、DB 写仍瓶颈）才做。

**改动清单**
- ☐ Redis 数据结构：`dev:online:{id}`(String+TTL) / `dev:online:idx`(ZSet) / `dev:dirty`(Set) / `dev:dirty:inflight:{node}`(Set) / `dev:node:{id}`。
- ☐ 热写 Lua：SET+EX+ZADD(GT)+SADD+路由续期单 RTT，旧 ts `return 0`（单调）。
- ☐ 终态条件删除 Lua `markOfflineIfStale`：仅当 score 仍 ≤ deadline 才 DEL+ZREM+落 DB OFFLINE（防并发误判）。
- ☐ 批量回写 `SSCAN→SMOVE inflight→写→SREM`（原子瓜分、无写放大）+ `sweeper` 兜底僵死节点 inflight。
- ☐ 离线检测 ZSet 轮询为主（`@Scheduled(fixedDelay=30000)`），传 deadline 非 now。
- ☐ 读侧降级：`isOnline` Redis 熔断回退 DB；`onlineCount` 用 ZCARD（近似值，文档标注）。

**验收**
- ☐ 50K 设备心跳压测，DB 写 QPS 降 ≥ 90%。
- ☐ 无丢终态、无误判离线、多节点 flush 无写放大无重复。
- ☐ inflight sweeper 专项：搬入 inflight 后 kill 节点 → sweeper 还回 → 他节点落库。
- ☐ 灰度开关 `device.state.hot-store.enabled=false` 回退 2a。

---

## Phase 8：可插拔验证 + 可观测 + 异常规范

> 来源 PROTOCOL S5 + OPT Stage 6 + G2 收尾。

**PROTOCOL S5：可插拔可执行证明**
- ☐ 写 `NoopProtocolHandler`（`protocol()="test"`）+ 单测：发 `test.*` 事件被路由到 Noop、gb28181 不受影响。证明新增协议零改核心。

**OPT Stage 6：可观测 + 异常**
- ☐ 移除 Manager 模板 `catch(Exception)→throw RuntimeException`，改抛 `ServiceException`+`ServiceExceptionEnum`（无对应枚举先补，遵循 CLAUDE.md）。
- ☐ SkyWalking 指标埋点 + 告警（心跳 P99、队列深度、缓存命中率、Redis-A/B 熔断、ZLM 失败率、`dev:node` 命中率、跨节点转发 502/503 率）。
- ☐ `HealthCheckController` 依赖级健康检查（Redis-A/B / DB / SIP / ZLM 各 UP/DOWN/DEGRADED）。

**G2 收尾（单一 owner）**
- ☐ 处理悬空的 `Gb28181InviteResponseController` / invite-response：删注释**或**补端点（`gb28181.Session.ServerInvite` 回包路径），二选一，单 PR 单 owner。

**验收**
- ☐ Noop 单测绿；§12.2 指标全可见；异常码映射回归正确。

---

## 14. 并行/串行矩阵

| | P0 | P1 | P2a | P3 | P4 | P5 | P6 | P7 | P8 |
|---|---|---|---|---|---|---|---|---|---|
| 可与前序并行？ | — | ✅ 与 P0 | ✅ 与 P1 | ⚠️ 须待 P2a 的 notifier 改动落地 | ❌ **必须待 P3** | 部分（门面可早，路由须 Redis 就绪） | ✅ 独立 | ❌ 须待 P2a | ✅ 独立 |
| 触碰重叠点 | — | — | A（轻，改分支体） | **A（重，搬 switch）** | **A（重，分片）** | **B** | — | — | — |

**关键串行边**：
- **P2a → P3 → P4** 三者都触碰 `VoglanderBusinessNotifier`，**必须线性、单分支**。P2a 改分支体 → P3 把分支搬进 handler → P4 在 handler 上叠加分片/upsert。
- **P5 内部**：门面（S3）→ 路由（§10.4）。
- P0/P1/P6/P8 与重叠点无关，排期灵活。

---

## 15. 全局验收门槛

- ☐ 每个 Phase 独立可编译、带灰度开关、可一键回退旧路径。
- ☐ 遵循 CLAUDE.md 测试分层：Manager 集成测试（`BaseTest`）、HTTP/异步/Hook 不用 `@Transactional` 手动清理、Controller/Service 纯单测。
- ☐ §1.2 SLO 全达标（注册→可用 P99 < 500ms、心跳 P99 < 50ms、REST P99 < 200ms、可用性 99.9%、事件丢失 < 0.01%）。
- ☐ 单依赖故障演练通过（Redis-A/ZLM/DB 之一故障降级运行且读写一致）。
- ☐ 压测无 OOM、无连接耗尽、无终态丢失、无误判离线。
- ☐ 框架类型零外泄：`GatewayEvent`/`CommandHandlerRegistry` 仅存在于 `wrapper/gb28181/` 适配器子包，不进 voglander 自有端口签名。

---

## 附：与上游文档的对应关系

| 本清单 Phase | OPTIMIZATION | PROTOCOL-EXTENSIBILITY |
|--------------|--------------|------------------------|
| Phase 0 | Stage 0（B1） | — |
| Phase 1 | Stage 1（P1/P2/B2） | — |
| Phase 2a | Stage 2a | — |
| Phase 3 | — | S1~S2（解 H1） |
| Phase 4 | Stage 3（H2/H3/M1/B3） | （叠加到 S2 的 handler） |
| Phase 5 | §10.4 / Stage 5（A1/B5） | S3~S4（解 H2/H3 出站，修 GbDeviceCommandService 缺失） |
| Phase 6 | Stage 4（A5/B6） | — |
| Phase 7 | Stage 2b（H1/A3/A4/H5/H6） | — |
| Phase 8 | Stage 6（P6） | S5（可插拔验证） + G2 |

> 二者在 PROTOCOL §11 风险表「与 OPTIMIZATION-DESIGN 的交叉」已达成共识：dispatcher 介于 notifier 与业务之间、分片入队点仍在适配器侧，两设计正交、落地排期协调即可。本清单即该协调的可执行展开。
