# Voglander 高并发·高稳定架构优化方案

> 版本：1.0.3（v4，落地前可执行性审计修订）｜ 更新日期：2026-06-01 ｜ 基线：sip-gateway 1.8.0（分支 `dev_merge_sip`）
> 配套：[ARCHITECTURE.md](./ARCHITECTURE.md)（现状架构）
> 文档性质：优化设计稿（非已落地代码）。所有方案基于已核实的现有构件，向后兼容、可灰度、可回滚。
> **核心承诺（v3 钉死）**：接入层无状态、任意节点宕机不丢终态、跨节点并发不错乱状态。
> **v4 增量**：补齐落地前置条件（`@EnableScheduling` / 旧 key 全清单 / upsert 改造 / 索引清单纠偏 / 内部端点鉴权 / Redis 模板隔离）。

---

## 变更记录

### v3 → v4（落地前可执行性审计 —— 不改变设计，只钉死前置条件）

逐源码二次核对发现 v3 的设计正确但**有一处硬阻塞会让所有 `@Scheduled` 兜底任务静默失败**，另有四处假设需收紧。本版仅做"落地前必修项"补丁，**不调整 §1~§12 的设计本体**：

| 编号 | v3 的疏漏 | v4 修正 | 影响章节 |
|------|-----------|---------|----------|
| **B1（硬阻塞）** | 方案的 flush / detectOffline / sweeper / 延迟双删扫描 / 节点存活续期全部基于 `@Scheduled`，但项目当前**没有 `@EnableScheduling`**（`ApplicationWeb` 仅 `@SpringBootApplication` + `@ComponentScan` + `@EnableSipServer`） | **新增 [Stage 0](#stage-0-启用调度---硬阻塞前置必做)**：`ApplicationWeb` 加 `@EnableScheduling`，必须在 Stage 1 之前完成；同时校验既有的 `StreamProxyBizServiceImpl @Scheduled` 与 `SpringDynamicTask` 已生效 | §13 / §14 |
| B2 | §6.1 "三步必须同 PR" 仅列了 `do:/dto:` 前缀残留，遗漏了**裸 `id` 的 evict** | 实际 `DeviceManager.clearCache` evict **5 种 key**：裸 `id`、`do:`/`dto:` × {id, deviceId}；§6.1 改造清单补齐，并加 Redis 残留清理脚本 | §6.1 |
| B3 | §7.3 / §8 把"`ON DUPLICATE KEY UPDATE` + catch DuplicateKey" 当成"已设计完毕的修正"叙述 | 现状 `MediaSessionManager.onInviteOk/onAck` 仍是**先查后插**；`tb_media_session.call_id` 已 UNIQUE（✅ 前提具备），但代码未改 → Stage 3 验收必须包含 upsert 改造 + 跨节点并发 upsert 专项测试 | §7.3 / §8 / §13 Stage 3 / §15.1 |
| B4 | §11.1 索引清单两处偏差 | (a) `tb_device_channel` 已有 `UNIQUE KEY channel_id (channel_id, device_id)`（命名差异：非 `uk_channel_device`），**无需 ADD**；(b) 仍需新增 `idx_device_id` 但理由是"复合 UNIQUE 最左前缀是 `channel_id`，无法支撑按 `device_id` 扫描"；(c) `tb_media_session.call_id` 已是 `uk_call_id` UNIQUE，无需 ADD | §11.1 |
| B5 | §10.4 命令亲和路由两个实施门槛未提 | (a) `/internal/sip/command` 必须配置**鉴权 + IP 白名单**（Spring Security Filter / 拦截器），否则集群内任意网络可达节点都成 RCE 入口；(b) `dev:node:{id}` 的漂移收敛窗口与心跳间隔强相关 —— 设备首次心跳即续期路由表，长心跳（如 5min）场景需在文档明确收敛上界 | §10.4 / §16 |
| B6 | §9.3 / §10.4 / 现状 `RedisInviteContextStore` 实际复用默认 `StringRedisTemplate` | A5 物理隔离要求落地代码改造：`RedisInviteContextStore` 显式注入独立 `StringRedisTemplate`（独立 `RedisConnectionFactory` 指向 Redis-B），与缓存/状态默认模板分离 | §9.1 / §13 Stage 4 |

### v2 → v3（多节点无状态并发安全审计）

本版针对 v2 的**"无状态多节点"承诺**做逐源码核对（核对了 `gateway-core`/`gateway-gb28181`/`gb28181-server` 1.8.0 sources、`RedisLockUtil`、`DeviceManager.clearCache`），修正了与现实不符或破坏无状态的设计：

| 编号 | v2 的问题 | v3 修正 | 影响章节 |
|------|-----------|---------|----------|
| **A1（致命）** | §10.1 称"依赖 sip-gateway 1.8.0 自带跨节点转发"分发所有命令 | **已核实框架跨节点转发仅覆盖 INVITE 异步回包**（`Gb28181InviteResponseController`）。PTZ/点播/查询等命令全走本地 `DeviceSessionCache.getToDevice→SipSender`，**无设备-节点路由**。新增 [§10.4 命令下发的跨节点亲和路由](#104-命令下发的跨节点亲和路由必修-a1)（复用 `gateway.nodes` HTTP 转发模式） | §10.1/§10.4 |
| **A2（致命）** | §7.1 "同 deviceId 恒定同槽 → 严格有序"被当作跨节点保证 | 分片槽是**节点本地进程内状态**，仅保证**单节点内**有序。设备跨节点漂移（重连/灰度）即失序 → 跨节点正确性**必须由 §7.2 单调性兜底**。红线 2 改写 | §3.2/§7.1 |
| **A3（高）** | §5.2-4 `SSCAN→写→SREM` 多节点无锁瓜分 | `SSCAN` 游标在节点间重叠 → 同一 deviceId 被 N 节点重复读取 → **DB 写放大 N 倍**（与"降 DB 写"矛盾）。改为 **`SSCAN→SMOVE inflight→写→SREM` + sweeper 兜底宕机**，真正无重复无丢失 | §5.2 |
| **A4（高）** | §6.3 延迟双删用 `ScheduledExecutorService` | JVM 内定时器是节点本地状态，进程崩在两次 evict 间 → 脏读到 TTL。改为 **Redis ZSet 延迟队列**，全节点共消费 | §6.3 |
| **A5（中）** | §9 状态热存与 INVITE 上下文共用 Redis | Redis 挂则两者同挂：状态可降级 DB，但 INVITE 仍 503 → 媒体功能全废。要求 **INVITE 上下文 Redis 物理隔离 / HA** | §9.1/§9.3 |
| M1 | §7.3 仅靠框架 `invite-idempotency-window-ms` 去重 | 那是**单节点框架内**幂等。跨节点并发 upsert 同 `callId` 仍撞 UNIQUE → Manager 层 `ON DUPLICATE KEY UPDATE` + catch DuplicateKey | §7.3/§8 |
| M4 | §10.2 备选"任务内自续期" | `RedisLockUtil` 确无续期 API（已核实源码）。续期周期 ≤ TTL/3 且续期失败必须任务自杀；推荐直接删除备选、强制无锁瓜分 | §10.2 |

### v1 → v2（一致性评审）

| 编号 | v1 的问题 | v2 修正 |
|------|-----------|---------|
| 事实-1 | 误以为缓存是本地缓存，设计了 L1 Caffeine + pub/sub | **已核实当前是 `RedisCacheManager`**（[RedisConfig.java:90](../../voglander-repository/src/main/java/io/github/lunasaw/voglander/repository/config/RedisConfig.java)）。**删除 L1/pub/sub 过度设计**，缓存优化收敛为"修 key + 去全清" |
| 事实-2 | 称"分布式锁加看门狗续期" | **已核实 `RedisLockUtil` 无续期能力**（仅 lock/tryLock/unLock）。改为**无锁原子瓜分** + 短任务，不依赖不存在的看门狗 |
| H1 | flush 用 `SPOP` 先弹后写，崩溃丢数据 | 终态（离线）**同步直写 DB**；批量回写改 inflight 暂存且仅用于可自愈字段 |
| H2 | `notify` 的 `@Async` 池在分片前，破坏有序 | `notify` **不用通用 @Async 池**，直接算 shard 非阻塞入队，单线程槽做重活 |
| H3 | 串行化无法解决 UDP 乱序"生成" | 增加**单调性保护**（时间戳条件更新，Lua `GT` 比较） |
| H4 | cache-aside 读写竞态 + TTL 10m → 脏读窗口过大 | 缩短 TTL + 延迟双删；明确为**最终一致** |
| H5 | 把不可靠的 keyspace notification 列为"推荐" | **ZSet 轮询为主**（可靠），notification 仅作加速 |
| H6 | DB-status 对非 Redis 读路径滞后未界定 | 明确**状态读统一走 Redis**，DB 仅最终态；界定滞后上界 |
| 路径 | 直接 Redis 作真相源，风险大 | 拆为 **Stage 2a（DB 真相源，低风险，先做）+ Stage 2b（Redis 热存，按需）** |

---

## 目录

1. [优化目标与 SLO](#1-优化目标与-slo)
2. [现状瓶颈定级](#2-现状瓶颈定级)
3. [一致性模型与设计红线](#3-一致性模型与设计红线)
4. [优化总体架构](#4-优化总体架构)
5. [专题一：设备状态热点重构（分两阶段）](#5-专题一设备状态热点重构分两阶段)
6. [专题二：缓存正确性修复（不引入 L1）](#6-专题二缓存正确性修复不引入-l1)
7. [专题三：事件管线（有序·单调·幂等·背压）](#7-专题三事件管线有序单调幂等背压)
8. [专题四：目录回调批量化（幂等 upsert）](#8-专题四目录回调批量化幂等-upsert)
9. [专题五：外部系统韧性（熔断·双向降级）](#9-专题五外部系统韧性熔断双向降级)
10. [专题六：多节点一致性（命令亲和路由 + 无锁瓜分）](#10-专题六多节点一致性命令亲和路由--无锁瓜分)
11. [数据库与持久层优化](#11-数据库与持久层优化)
12. [统一异常与可观测性](#12-统一异常与可观测性)
13. [分阶段实施路线](#13-分阶段实施路线)
14. [风险评估与回滚预案](#14-风险评估与回滚预案)
15. [验证与压测方案](#15-验证与压测方案)
16. [附录：关键参数基线](#16-附录关键参数基线)

---

## 1. 优化目标与 SLO

### 1.1 容量目标

| 维度 | 当前（估算） | 目标 |
|------|-------------|------|
| 在线设备规模 | ~1K（缓存全清后退化明显） | **50K** 在线设备 |
| 心跳吞吐 | 受限于读-改-写整行 + 缓存全清 | **≥ 2000 TPS** |
| 目录上报 | 64 路 NVR ≈ 64 轮读写 | 单设备目录 **1 次批量幂等落库** |
| 媒体会话并发 | 未压测 | **5K** 并发 INVITE 会话 |
| 单节点 → 集群 | 单机 memory store | **N 节点水平扩展** |

### 1.2 服务质量目标（SLO）

| 指标 | 目标 |
|------|------|
| 设备注册→可用 P99 | < 500ms |
| 心跳处理 P99 | < 50ms |
| REST 查询 P99 | < 200ms |
| 可用性 | 99.9% |
| 单依赖故障（Redis/ZLM/DB 之一）| 不导致整体不可用（降级运行） |
| 设备事件丢失率 | < 0.01%（幂等 + 重试兜底） |

### 1.3 设计原则

- **热路径降 DB**：高频写改为定向更新/合并/批量，必要时 Redis 热存；
- **故障隔离**：任一外部依赖故障可降级，核心链路存活；
- **有序 + 单调 + 幂等**：同设备串行 + 时间戳单调 + 写操作幂等可重放；
- **背压优先于雪崩**：队列有界 + 拒绝策略；
- **终态可靠落地**：离线等终态变更同步持久化，不依赖可丢的批量；
- **零破坏迁移**：新路径与旧模板方法并存，开关灰度。

---

## 2. 现状瓶颈定级

> 依据对 `DeviceManager` / `DeviceRegisterServiceImpl` / `VoglanderBusinessNotifier` / `ThreadPoolConfig` / `RedisConfig` / `RedisLockUtil` 的逐行核验。

| # | 问题 | 位置 | 影响 | 级别 |
|---|------|------|------|------|
| P1 | `clearCache` 每次写都 `cache.clear()`，且缓存是 **Redis**，等于每次写对整个 device 缓存区做 **SCAN+DEL** | `DeviceManager.java:415` + `RedisConfig.java:90` | 高并发心跳直接压垮 Redis，命中率趋 0 | 🔴 致命 |
| P2 | 缓存 key 不一致（`@Cacheable(#deviceId)` vs evict `do:/dto:` 前缀），精确 evict 从不命中 | `DeviceManager.java:525` vs `:404` | 修 P1 后立即暴露脏读 | 🔴 致命 |
| P3 | 心跳/状态走 `saveOrUpdate`：2~3 次 DB 读 + 整行 UPDATE + 全缓存清 | `DeviceRegisterServiceImpl.keepalive` → `DeviceManager` | 心跳写放大，限制在线规模 | 🔴 致命 |
| P4 | 目录回调 N+1：逐 `DeviceItem` 单独 `saveOrUpdate` | `VoglanderBusinessNotifier.java:260-281` | 64 路 NVR = 64 轮读写 | 🟠 高 |
| P5 | 异步事件无序：`sipNotifierExecutor` 8~32 线程并发消费同设备事件 | `ThreadPoolConfig` | Online 早于 Register → 抛错丢事件 | 🟠 高 |
| P6 | 异常统一降级为 `RuntimeException`，丢失 `ServiceException` 语义 | 各 Manager 模板 catch | 错误码映射失效 | 🟡 中 |
| P7 | `listDeviceDTO` 用 `getPage(1, Integer.MAX_VALUE)` 全表入内存 | `DeviceManager.java:543` | 规模上升 → OOM | 🟡 中 |
| P8 | `tb_device.status`/`keepalive_time` 无索引 | `sql/voglander.sql` | 全表扫 | 🟡 中 |
| P9 | 单机 INVITE memory store，无法多节点 | `gateway...store.type=memory` | 不可水平扩展 | 🟡 中 |
| P10 | ZLM `onPlay`/`onPublish` 放行 + 无熔断 | `VoglanderZlmHookServiceImpl` | 安全空洞 + 抖动传导 | 🟡 中 |

---

## 3. 一致性模型与设计红线

> 本章是 v2 新增的核心。高并发优化最大的风险不是性能不够，而是**为了性能牺牲了正确性而不自知**。先把一致性边界钉死。

### 3.1 数据按一致性等级分类

| 数据 | 一致性要求 | 真相源 | 落地策略 |
|------|-----------|--------|----------|
| 设备注册信息（deviceId/type/name/ip） | 强一致 | **DB** | 注册同步写 DB（不进热路径） |
| 设备**终态变更**（离线、删除、禁用） | 强一致（不可丢） | **DB** | **同步直写 DB**，不走可丢批量（修 H1） |
| 设备**在线态/心跳时间**（高频、可自愈） | 最终一致（秒级） | Stage2a:DB / Stage2b:Redis | 合并写 / 批量回写；丢失可由下次心跳自愈 |
| 设备**所在节点**（命令路由用） | 最终一致 | **Redis** `dev:node:{id}` | 注册/上线写，心跳续期；命令下发按此 HTTP 转发（§10.4，修 A1） |
| INVITE 会话上下文（跨节点路由） | 强一致 | **Redis（独立实例 / HA，修 A5）** | `find/save` 故障**抛 503**，绝不降级（已实现）；`remove` 失败仅告警，由 TTL 兜底 |
| 缓存（device/mediaNode 等） | 最终一致（TTL 内） | DB | 精确 evict + 短 TTL + 延迟双删（Redis ZSet 延迟队列，修 A4） |
| 媒体会话状态（callId） | 强一致幂等 | DB | `call_id` UNIQUE，**`ON DUPLICATE KEY UPDATE` 跨节点幂等**（修 M1） |

> **INVITE 上下文的 `remove` 是弱保证**：`Gb28181InviteResponseController#safeRemove` 已核实为"失败仅告警、不抛错"，靠 `inviteContextTtlMs` 兜底过期。`find/save` 才是强一致（故障 503）。不要把 `remove` 也设计成必须成功，否则回包成功后却因清理失败而误报错。

### 3.2 六条设计红线（违反即视为 bug）

1. **终态不可丢**：离线/删除/禁用必须同步落 DB，禁止只写 Redis 或只入可丢队列（H1）。
2. **同设备有序仅在单节点内成立**：分片槽是节点本地状态，只保证**单节点内**按到达顺序串行；设备跨节点漂移（重连/灰度滚动）后，节点间**无序**——跨节点正确性**必须由红线 3 单调性兜底**，不得把"分片串行"当作跨节点有序保证（A2，修正 H2 的适用边界）。
3. **乱序网络下必须单调**：状态写入按时间戳条件生效，旧时间戳不得覆盖新状态（H3）。这是跨节点正确性的**唯一**依靠，Lua `GT` 与 DB 条件 UPDATE 二者皆不可省。
4. **缓存只做最终一致**：不得宣称缓存路径强一致；TTL 必须有界，且处理 cache-aside 回填竞态；延迟双删的"延迟"动作**不得依赖 JVM 内定时器**，须放 Redis 共享设施（H4 + A4）。
5. **降级要双向且对称**：写降级 DB 时，读也必须能从 DB 读；恢复时先预热再切回（H6 + §9.3）。
6. **节点本地状态不得承载持久语义**：进程内 Map / `ScheduledExecutorService` / 分片队列**只能做加速或排序**，绝不能作为"待写 DB"的唯一记录。任何"还没落库"的标记必须放 Redis（`dev:dirty`/`inflight`），否则节点宕机即丢（A2/A3/A4 的共同根因）。

### 3.3 一致性保证总览（实现后对外承诺）

| 场景 | 保证 |
|------|------|
| 设备注册后立即查询 | 强一致（DB 同步） |
| 心跳后查在线态 | 最终一致，滞后 ≤ flush 周期（默认 5s，受 dirty 积压监控兜底） |
| 设备离线后查在线态 | 强一致（离线同步写 + Redis 即时删 key） |
| 并发改设备资料后查缓存 | 最终一致，≤ 缓存 TTL（默认 2~3min）；竞态由延迟双删收敛 |
| 多节点查在线设备数 | 近似值（ZCARD，过期成员扫描周期内可能略高）；逐设备 `isOnline` 精确 |
| 设备跨节点漂移后查状态 | 最终正确（红线 3 单调写保证：旧节点的迟到事件不覆盖新节点的新状态） |
| 任意节点宕机后查终态 | 强一致（终态同步落 DB）；自愈字段最多丢 ≤1 个 flush 周期的精度，由 inflight sweeper 兜底重写 |
| 设备所在节点变更后下发命令 | 最终正确：路由表 `dev:node:{id}` 收敛后命中新节点；收敛窗口内可能 502（建议 200ms×3 重试，与框架 INVITE 回包错误约定一致） |

---

## 4. 优化总体架构

```
                         ┌─────────── LB / 网关（无状态，任意节点可收 REST）───────────┐
                         ▼                                                          ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 接入层  REST(8081) │ SIP(5060/5061) │ ZLM Hook(/index/hook)            │  ← 每节点对等
│  命令下发：查 dev:node:{id} → 非本节点则 HTTP 转发到设备所在节点(§10.4) │
└───────────────┬──────────────────┬─────────────────┬──────────────────┘
                ▼                  ▼                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 事件接入与整形                                                          │
│  Web Controller(限流/防重/校验)                                        │
│  VoglanderBusinessNotifier ──▶ DeviceEventDispatcher                   │
│   ingress(2~4线程,大队列,仅算shard+offer,禁CallerRuns) → 立即归还SIP线程 │
│   16~32 个【单线程】分片槽：同 deviceId 同槽 → ★仅本节点内有序           │
│   ★跨节点漂移后顺序不保证 → 正确性靠业务层单调写(红线3)兜底              │
└────────────────┬──────────────────────────────────────────────────────┘
                ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 业务编排层（热写/冷写/终态分离）                                        │
│  ┌──────────────────────────┐  ┌──────────────────────────┐  ┌──────┐ │
│  │ 心跳/在线态（可自愈）      │  │ 终态：离线/删除/禁用       │  │ 资料  │ │
│  │ 2a: DB定向更新(单调)+合并  │  │ 同步直写 DB（不可丢）      │  │ CRUD │ │
│  │ 2b: Redis热存(Lua GT)      │  │ 离线即时删Redis+同步落DB   │  │ 缓存  │ │
│  └────────────┬─────────────┘  └─────────────┬────────────┘  └───┬──┘ │
└───────────────┼──────────────────────────────┼───────────────────┼────┘
                ▼ (SMOVE inflight→写→SREM,sweeper兜底)  ▼ (同步)       ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 协调层 Redis-A (共享,可降级回DB)                                        │
│  在线态(Hash+TTL/ZSet) │ device缓存(精确key+短TTL+ZSet延迟双删)         │
│  dev:dirty / dev:dirty:inflight:{node} │ dev:node:{id}(命令路由表)      │
├──────────────────────────────────────────────────────────────────────┤
│ 协调层 Redis-B (独立实例/HA, 不可降级)  INVITE 上下文 {nodeId,ctxKey}    │  ← 修A5物理隔离
└───────────────┬──────────────────────────────────────────┬───────────┘
                ▼                                            ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 持久层 MySQL/SQLite  + 补索引 + 批量幂等 upsert  (终态强一致真相源)     │
└──────────────────────────────────────────────────────────────────────┘

横切：熔断/隔离(Resilience4j) · 双向降级 · 统一异常(ServiceException) · 可观测(SkyWalking)
跨节点：INVITE回包(框架自带) + 命令亲和路由(自建§10.4) + 状态/缓存共享Redis-A
```

---

## 5. 专题一：设备状态热点重构（分两阶段）

> 解决 P1/P2/P3/P8。**关键修订**：拆为低风险的 2a 与按需的 2b，并贯彻"终态同步、自愈字段批量、读写降级对称"。

### 5.1 Stage 2a：DB 真相源 + 定向更新 + 心跳合并（先做，低风险）

不引入 Redis 真相源，仅消除写放大，**零新增一致性风险**。

**(1) 定向两列更新，替代读-改-写整行（含单调保护，红线 3）**

```java
// DeviceManager 新增——心跳/状态轻量更新，不读、不整行、不清列表缓存
// keepalive 走单调条件 UPDATE：旧时间戳不覆盖新状态（跨节点漂移下唯一正确性保证）
public void patchLiveness(String deviceId, Integer status, LocalDateTime keepaliveTime) {
    LambdaUpdateWrapper<DeviceDO> uw = new LambdaUpdateWrapper<>();
    uw.eq(DeviceDO::getDeviceId, deviceId)
      .set(status != null, DeviceDO::getStatus, status)
      .set(keepaliveTime != null, DeviceDO::getKeepaliveTime, keepaliveTime)
      // 单调条件：仅当传入时间戳比库里新才更新 keepalive（防 UDP/跨节点乱序回灌）
      .and(keepaliveTime != null, w -> w
          .isNull(DeviceDO::getKeepaliveTime)
          .or().lt(DeviceDO::getKeepaliveTime, keepaliveTime));
    deviceService.update(uw);                 // 单条 UPDATE，命中 device_id 唯一索引
    deviceCache.evictByDeviceId(deviceId);    // 仅精确 evict 该设备（见专题二）
}
```

> ⚠️ 离线终态（`status=OFFLINE`）走**独立、无单调条件**的同步写（见 (3)）——终态不可被"时间戳更旧"挡住。单调条件**只**管 keepalive 这种自愈字段。

**(2) 心跳合并（coalescing）—— 降低持久化频次（节点本地加速，非持久语义）**

并非每个心跳都写 DB：维护内存里 `lastPersistTs[deviceId]`，距上次持久化 < `coalesceWindow`（如 30s）则**只刷新缓存里的 keepaliveTime，不写 DB**。设备 60s 心跳、合并窗 30s → DB 写频次砍半以上；窗口可调。

> **多节点定位（红线 6）**：`lastPersistTs` 是**节点本地 JVM 状态**，仅作"少写 DB"的加速，**绝不承载持久语义**。
> - 设备漂移到新节点 / 灰度重启后，新节点 `lastPersistTs` 为空 → 立即补写一条 DB（无害，最多多写一条）；
> - 合并窗内进程崩溃，最多丢 ≤ window 的 keepalive 精度，**状态(ONLINE)不丢**（DB 已是 ONLINE），下次心跳自愈；
> - **禁止**把"待写 DB 的标记"存进程内 Map 替代 Stage 2b 的 `dev:dirty`——那会在宕机时真丢数据（A2/A3 同类错误）。

**(3) 离线/上线终态**：`patchLiveness` 同步写 DB（status 为强一致终态）。

**收益（2a 即可达成）**：单次心跳从「2~3 读+整行写+全清」降到「0 读 + 1 条两列 UPDATE（且合并后大量跳过）+ 1 次精确 evict」。多数中小规模到此即够。

### 5.2 Stage 2b：Redis 热存 + 批量回写（仅当 2a 实测不足，按需启用）

50K+ 设备、2a 的 DB 写仍是瓶颈时，再引入 Redis 作在线态真相源。**此阶段必须连同 H1/H3/H5/H6 + A3（SMOVE inflight）+ A5（INVITE Redis 隔离）修正一起做**。

**(1) Redis 数据结构**

| Key | 类型 | 值 | TTL | 用途 |
|-----|------|----|----|------|
| `dev:online:{deviceId}` | String(JSON) | `{status, keepaliveTs, ip, port}` | = `deviceTimeout`（每心跳续期） | 在线态热存；TTL 到期即离线 |
| `dev:online:idx` | ZSet | member=deviceId, score=keepaliveTs | — | 在线索引；分页/计数/超时扫描 |
| `dev:dirty` | Set | deviceId | — | 待回写 DB 的脏设备（仅 keepalive 这类自愈字段） |
| `dev:dirty:inflight:{nodeId}` | Set | deviceId | — | **本节点正在回写的暂存集**（修 A3）；写完 SREM；节点宕机由 sweeper 还原 |
| `dev:node:{deviceId}` | String | nodeId | = `deviceTimeout`（每心跳续期） | **命令亲和路由表**（§10.4，修 A1）；下发命令查此表决定本地下发或 HTTP 转发 |

**(2) 热写：Lua 原子 + 单调保护（修 H3）**

```lua
-- KEYS[1]=dev:online:{id}  KEYS[2]=dev:online:idx  KEYS[3]=dev:dirty  KEYS[4]=dev:node:{id}
-- ARGV[1]=ts  ARGV[2]=stateJson  ARGV[3]=ttlSec  ARGV[4]=deviceId  ARGV[5]=nodeId
local cur = redis.call('GET', KEYS[1])
if cur then
  local o = cjson.decode(cur)
  if tonumber(o.keepaliveTs) >= tonumber(ARGV[1]) then
    return 0          -- 旧时间戳，丢弃（单调性：不让迟到心跳覆盖新状态）
  end
end
redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
redis.call('ZADD', KEYS[2], 'GT', ARGV[1], ARGV[4])   -- GT：仅当更大才更新
redis.call('SADD', KEYS[3], ARGV[4])
redis.call('SET', KEYS[4], ARGV[5], 'EX', ARGV[3])    -- 命令亲和路由表，随心跳续期到当前节点
return 1
```

单 RTT 完成 SET+EX+ZADD(GT)+SADD+路由续期，且**旧时间戳不覆盖新状态**。经 `RedisCache.executeLuaScript(...)` 调用（已具备）。

> 路由表 `dev:node:{id}` 在此一并续期：设备漂移到本节点后，下一次心跳即把路由指向本节点，命令下发自动跟随（§10.4）。**单调性同样适用**：因为整段在旧时间戳分支已 `return 0`，迟到心跳不会把路由错误回指旧节点。

**(3) 终态同步直写（修 H1 核心）+ 离线的条件原子删除（修并发误判）**

**离线绝不走 dirty 批量**。但**直接 `DEL + ZREM + 落 DB OFFLINE` 有并发竞态**：节点 A 的 `detectOffline` 判定设备过期，与此同时节点 B 收到该设备的新心跳并写了 ONLINE+新 ts；若 A 无条件删 key 并写 DB OFFLINE，会把**刚上线的设备误判离线**，直到下一次心跳才纠正。

故离线用 **Lua 条件删除**：仅当 ZSet 里的分数仍 ≤ deadline（即"确实还旧、期间没被新心跳刷新"）才执行删除与落库标记：

```lua
-- markOfflineIfStale.lua
-- KEYS[1]=dev:online:{id} KEYS[2]=dev:online:idx KEYS[3]=dev:node:{id}
-- ARGV[1]=deviceId  ARGV[2]=deadlineTs
local score = redis.call('ZSCORE', KEYS[2], ARGV[1])
if (not score) or tonumber(score) > tonumber(ARGV[2]) then
  return 0          -- 期间已被新心跳刷新（score 变大）或已被他人移除 → 放弃离线
end
redis.call('DEL', KEYS[1])
redis.call('ZREM', KEYS[2], ARGV[1])
redis.call('DEL', KEYS[3])         -- 同步清路由表
return 1
```

```java
public void markOffline(String deviceId, long deadlineTs) {
    Long removed = redisCache.executeLuaScript(MARK_OFFLINE_IF_STALE,
        List.of(onlineKey(deviceId), ONLINE_IDX, nodeKey(deviceId)),
        deviceId, String.valueOf(deadlineTs));
    if (removed != null && removed > 0) {
        // 仅在 Redis 端确认确实下线后，才同步落 DB 终态（无单调条件，终态优先）
        deviceManager.patchOfflineTerminal(deviceId);   // status=OFFLINE 强一致同步写
    }
}
```

> 由设备主动离线（收到 SIP 注销 / `Lifecycle.Offline` 事件）触发时，`deadlineTs` 传 `now`（必然 ≥ score，强制离线）；由 `detectOffline` 周期扫描触发时传 `now - timeoutMs`，受条件保护，**杜绝与并发心跳的 race**。

**(4) 批量回写：SSCAN→SMOVE inflight→写→SREM + sweeper（修 H1 + A3 写放大）**

`SSCAN` 游标在多节点间**会重叠**——同一 deviceId 被 N 个节点各读一次、各写一次 DB，写放大 N 倍，违背"降 DB 写"初衷。改为用 **`SMOVE` 把候选原子搬入本节点专属 inflight 集**实现真正瓜分（`SMOVE` 是原子的：一个 deviceId 只可能被一个节点搬成功）：

```java
@Scheduled(fixedDelay = 5000)
public void flushDirty() {
    List<String> candidates = scanDirty(BATCH_SIZE);             // SSCAN dev:dirty，只取候选
    List<String> mine = new ArrayList<>(candidates.size());
    for (String id : candidates) {
        // SMOVE dev:dirty -> dev:dirty:inflight:{thisNode}；返回1才归我，原子瓜分、无重复
        if (Boolean.TRUE.equals(redisCache.sMove(DIRTY, inflightKey(thisNodeId), id))) {
            mine.add(id);
        }
    }
    if (mine.isEmpty()) return;
    List<DeviceStatePatch> patches = mine.stream()
        .map(this::readOnlineState).filter(Objects::nonNull).toList();
    deviceManager.batchPatchLiveness(patches);                   // 批量两列单调 UPDATE
    redisCache.sRem(inflightKey(thisNodeId), mine);              // 成功落库后才从 inflight 移除
}

// sweeper：兜底"搬进 inflight 后、SREM 前宕机"——把僵死节点的 inflight 还回 dev:dirty
@Scheduled(fixedDelay = 60000)
public void sweepStaleInflight() {
    for (String node : discoverInflightNodes()) {                // SCAN dev:dirty:inflight:*
        if (node.equals(thisNodeId)) continue;
        if (isNodeAlive(node)) continue;                         // 见 §10.4 节点心跳表
        // 抢占式接管：把僵死节点 inflight 的成员搬回主脏集，等待下轮正常瓜分
        redisCache.sMoveAll(inflightKey(node), DIRTY);           // 逐个 SMOVE，原子
    }
}
```

崩溃矩阵：① 搬进 inflight 前宕机 → 仍在 `dev:dirty`，他节点下轮处理；② 搬进 inflight、写 DB 前宕机 → sweeper 还回 `dev:dirty`；③ 写 DB 后、SREM 前宕机 → sweeper 还回，下轮重复写（单调 UPDATE 幂等，无害）。**任何节点宕机都不丢、不重复成功落库（重复写也幂等）**。

**(5) 离线检测：ZSet 轮询为主（修 H5）+ 多节点条件离线（防并发误判）**

```java
@Scheduled(fixedDelay = 30000)
public void detectOffline() {
    long deadline = nowMs() - timeoutMs;
    Set<String> stale = redisTemplate.opsForZSet()
        .rangeByScore("dev:online:idx", 0, deadline);        // 可靠轮询取候选
    // 传 deadline（非 now）：markOffline 内 Lua 仅当 score 仍 ≤ deadline 才离线，
    // 期间被任意节点的新心跳刷新过（score 变大）即放弃 → 杜绝"刚上线被误判离线"
    for (String id : stale) markOffline(id, deadline);
}
```

- **多节点防重**：N 个节点都跑 `detectOffline` 无害——`markOffline` 的 Lua 条件删除是原子的，同一设备只会被一个节点成功离线一次，其余返回 0；故**无需对 detectOffline 加分布式锁**。
- keyspace notification（方案A）**不作为离线判定依据**（fire-and-forget 会丢事件，DB 永久停 ONLINE）。如需更快感知，可作为"加速器"叠加，但 ZSet 轮询始终兜底。

**(6) 读侧降级对称 + 计数语义（修 H6）**

- `isOnline(deviceId)`：读 `dev:online:{id}`（TTL 精确）；**Redis 熔断时回退查 DB**。
- `onlineCount()`：`ZCARD`，**近似值**（过期成员在 `detectOffline` 周期内才清，可能略高），对外文档标注；需精确时以逐设备 `isOnline` 为准。
- 所有"按 status 查设备"的旧路径（`getPage` 等）统一**约定读 Redis 在线态**或接受 ≤flush 周期滞后（二选一，见 §3.3）。

### 5.3 收益对比

| 指标 | 旧 | Stage 2a | Stage 2b |
|------|----|----------|----------|
| 单次心跳 DB 操作 | 2~3 读+整行写 | 0 读 + 1 两列写（合并后多数跳过） | 0（仅 Redis） |
| 对设备缓存影响 | 全清 | 仅精确 evict | 无 |
| 在线计数 | 全表 COUNT | 全表 COUNT（+索引） | ZCARD O(1) |
| 离线检测 | 全表扫 keepalive | 索引扫 | ZSet 范围 + 条件原子离线 |
| 多节点回写 | — | — | SMOVE 原子瓜分，无写放大、无重复 |
| 新增一致性风险 | — | **无** | 需 H1/H3/H5/H6 + A3/A5 修正后为可控最终一致 |

---

## 6. 专题二：缓存正确性修复（不引入 L1）

> 解决 P1/P2。**修订**：当前已是 `RedisCacheManager`，**不引入 Caffeine L1**（避免 pub/sub 失效一致性新坑）。只做三件事。

### 6.1 统一缓存 Key（修 P2）

抽 `DeviceCacheKey` 工具，`@Cacheable`、手动 evict、`clearCache` 三处共用：

```
cacheName "device"      key = "id:{id}"  |  "deviceId:{deviceId}"
cacheName "device:list" 列表/分页（与单对象隔离，短 TTL）
```

`@Cacheable(value="device", key="'deviceId:'+#deviceId")`，evict 同源生成，杜绝散落字符串。

> ⚠️ **迁移期必须清掉旧 key（实测核对 `DeviceManager.java` line 391-420 + line 525）**：现状 `clearCache` 实际 evict **5 种 key**——裸 `id`、`"do:"+id`、`"dto:"+id`、`"do:"+deviceId`、`"dto:"+deviceId`；而 `@Cacheable(value="device", key="#deviceId")` 实际写入的是**裸 deviceId**（既非裸 id，也非任一 `do:/dto:` 前缀）—— **三套 key 互不命中**，正是 P2 脏读根因。切到 `DeviceCacheKey` 时，**改造提交内必须同时**：
> ① 改 `@Cacheable` 的 key 表达式（`getDtoByDeviceId` 等所有缓存读取点）；
> ② 删除 `clearCache` 内**全部 5 种**旧 evict（裸 `id` + `do:`/`dto:` × {id, deviceId}），换为 `DeviceCacheKey.byId(id)` / `DeviceCacheKey.byDeviceId(deviceId)`；
> ③ 同 PR 添加一次性 Redis 残留清理（`redis-cli --scan --pattern 'voglander:cache:device::*' | xargs -r redis-cli DEL`，命名前缀按实际 `spring.cache.redis.key-prefix` 调整），否则新旧 key 共存、命中率仍为 0；
> ④ 同步排查 `StreamProxyManager`/`MediaNodeManager` 是否存在同模式 evict（同套模板复制而来，大概率同病）。
> 这四步**必须同一 PR**。

### 6.2 去 `cache.clear()`（修 P1）

```java
private void evictDevice(Long id, String oldDeviceId, String newDeviceId) {
    Cache cache = cacheManager.getCache("device");
    if (cache == null) return;
    if (id != null)          cache.evict(DeviceCacheKey.byId(id));
    if (oldDeviceId != null) cache.evict(DeviceCacheKey.byDeviceId(oldDeviceId));
    if (newDeviceId != null && !newDeviceId.equals(oldDeviceId))
                             cache.evict(DeviceCacheKey.byDeviceId(newDeviceId));
    // 不再 clear 整个区；列表缓存独立、靠短 TTL 自然失效
}
```

单对象写不再连坐列表缓存；杜绝高并发心跳触发 Redis SCAN+DEL。

### 6.3 cache-aside 回填竞态处理（修 H4 + A4 多节点）

读写并发下"旧值在 evict 后回填"会脏读。三道防护：

1. **短 TTL**：device 缓存 TTL 设 2~3min（不再是 v1 的 10min），脏读窗口有界且小；
2. **延迟双删放 Redis ZSet 延迟队列（修 A4）**：写操作先 `evict` 一次，再把 `deviceId` 投入 `cache:evict:delay`（ZSet，score=now+500ms）；由所有节点共消费的扫描器到点取出再 evict 一次。**不用 JVM 内 `ScheduledExecutorService`**——后者是节点本地状态，进程崩在两次 evict 之间，旧值会脏到 TTL（因 `RedisCacheManager` 是共享层，任一节点完成第二次 evict 即对全集群生效）；
3. **强一致绕缓存**：必要场景走 `getFromDbDirect` 直查 DB。

```java
// 写路径：第一次删 + 投递延迟删
cache.evict(DeviceCacheKey.byDeviceId(deviceId));
redisTemplate.opsForZSet().add("cache:evict:delay",
    "device|deviceId:" + deviceId, nowMs() + 500);

// 全节点共消费（条件原子取出，谁取到谁删，无重复）
@Scheduled(fixedDelay = 200)
public void runDelayedEvict() {
    Set<String> due = redisTemplate.opsForZSet().rangeByScore("cache:evict:delay", 0, nowMs());
    for (String m : due) {
        // ZREM 返回 1 才归本节点处理，避免多节点重复（重复 evict 本也幂等，仅省调用）
        if (redisTemplate.opsForZSet().remove("cache:evict:delay", m) > 0) {
            String[] p = m.split("\\|", 2);     // p[0]=cacheName p[1]=key
            Optional.ofNullable(cacheManager.getCache(p[0])).ifPresent(c -> c.evict(p[1]));
        }
    }
}
```

> 明确对外：缓存为**最终一致**（≤TTL），非强一致。延迟双删只把竞态窗口从"≤TTL"收敛到"≤延迟间隔(500ms)"，不改变最终一致定性。

---

## 7. 专题三：事件管线（有序·单调·幂等·背压）

> 解决 P5。**关键修订 H2**：分片前不得有多线程并行，否则有序性失效。

### 7.1 入口改造：notify 入口轻量分片入队（修 H2）

现状 `VoglanderBusinessNotifier.notify` 标了 `@Async("sipNotifierExecutor")`（核对源码确认），框架要求 notify 必须异步否则设备超时重传。**改造不是简单删 @Async**，而是把"通用业务池"换成"只做入队的极轻 ingress 池"，把重活移进分片单线程：

```
sip-gateway → VoglanderBusinessNotifier.notify(event)
   │  ★ @Async 改用 ingress 专用池：核心 2~4 线程 + 大队列，仅做下面两步（几乎不耗 CPU）
   │    shard = Math.floorMod(deviceId.hashCode(), N)
   │    shards[shard].offer(rawEvent)     // O(1) 非阻塞入队，立即归还 SIP/ingress 线程
   ▼
shards[0..N-1]  每槽【单线程】消费者 + 有界队列
   同 deviceId 恒定同槽 → ★仅本节点内按入队顺序串行；反序列化 + DB/Redis 重活在此做
```

- `offer` O(1) 非阻塞，不阻塞 SIP 线程（满足框架约束）；反序列化也放槽内，入队只放原始 `GatewayEvent`；
- **ingress 池满载策略禁用 CallerRuns**：现状 `sipNotifierExecutor` 是 CallerRuns（`8/32/1000`），照搬会在高峰把 SIP/调用线程拉死回退到同步、引发重传雪崩。ingress 池用**大队列 + 满则丢冗余 Keepalive**（见 §7.4），绝不 CallerRuns；
- **跨设备并行**（N 路），吞吐不降；**同设备仅本节点内串行**。

> ⚠️ **多节点边界（红线 2 / A2）**：分片只保证**单节点内**有序。设备跨节点漂移（重连/灰度滚动）后，事件可能先后落到不同节点的不同槽，节点间**无序**。所以"分片串行"**不是**跨节点正确性保证，跨节点正确性**完全依赖 §7.2 的单调写**。实施时不得以"已分片"为由省略单调条件。

### 7.2 单调性保护（修 H3）—— 跨节点正确性的唯一依靠

串行只保证"按到达顺序"，UDP 下到达 ≠ 发生。故所有状态写**按时间戳条件生效**：

- Stage 2b 热写 Lua 已含 `GT` 比较（§5.2-2），且路由表续期同受其单调分支保护；
- Stage 2a DB 路径：`UPDATE ... SET keepalive_time=? WHERE device_id=? AND (keepalive_time IS NULL OR keepalive_time < ?)`，旧时间戳不覆盖（已写入 §5.1-1 的 `patchLiveness`）；
- 终态（OFFLINE/删除/禁用）**不加单调条件**，走独立同步写，终态优先（§5.2-3）。

> 这是节点间无序场景下的**唯一**正确性保证：node-1 处理 Register 后 node-2 收到迟到的旧心跳，单调条件让旧 ts 被库里更新的 ts 挡下，最终状态正确。

### 7.3 幂等（含跨节点 M1 修正）

- 注册/上线/心跳：upsert + 状态幂等（重复 ONLINE 无副作用）；
- INVITE 会话：`callId` 幂等键（`tb_media_session.call_id` UNIQUE，✅ 已存在 `uk_call_id`）；
- **跨节点并发 upsert 同 callId（修 M1）**：`invite-idempotency-window-ms` 仅在**单节点框架内**去重（核对源码：`Gb28181EventForwarder` 用节点本地 Caffeine `processedInvites`）。多节点下 node-1 处理 `InviteOk`、node-2 几乎同时处理 `Ack` 会并发 insert 同一 callId → 必有一方撞 UNIQUE。故 `MediaSessionManager` 的 upsert 必须用 `INSERT ... ON DUPLICATE KEY UPDATE`（MySQL）/ `ON CONFLICT DO UPDATE`（SQLite），并 catch `DuplicateKeyException` 转 UPDATE 兜底，**不依赖框架窗口做跨节点幂等**。

> ⚠️ **现状（B3）**：`MediaSessionManager.onInviteOk/onAck/onInviteFailure` 当前仍是**先查后插**（Stage 3 SIP 迁移产物），**未实现 upsert + DuplicateKey 兜底**。Stage 3 验收必须包含：① 全部 upsert 路径改为数据库原生 upsert；② catch `org.springframework.dao.DuplicateKeyException` 转 UPDATE；③ 跨节点并发 upsert 同 callId 的集成测试（§15.1 已列）。`DeviceChannelManager.batchUpsert` 同理（§8）。

### 7.4 背压

| 队列 | 上界 | 满载策略 |
|------|------|----------|
| ingress 入队池 | 核心 2~4 + 大队列(如 10000) | 满 → 丢弃冗余 Keepalive，保 Register/Invite/Offline；**禁用 CallerRuns**（否则拖死 SIP 线程） |
| 分片槽队列 | 每槽如 2000 | 满 → 丢弃**低优先级重复事件**（如冗余 Keepalive），保 Register/Invite/Offline；并计数告警 |
| 批量回写 | SSCAN 分批 + SMOVE 瓜分 | 批大小限制 + 周期兜底 + inflight sweeper |

> 不用无界队列；监控单槽深度，持续高水位即扩槽或排查热点设备。

### 7.5 "设备不存在"优雅处理（消除 P5 残留）

`markOnline`/`heartbeat` 命中未注册设备 → **降级为补登记**（用事件 payload upsert 最小记录），不抛 `RuntimeException`。

---

## 8. 专题四：目录回调批量化（幂等 upsert）

> 解决 P4。**修订**：用幂等 upsert，且必须经专题三分片（同设备目录落同槽，避免并发撞 UNIQUE）。

```java
private void handleCatalog(GatewayEvent event) {            // 在分片单线程内执行
    DeviceResponse catalog = toEntity(event.payload(), DeviceResponse.class);
    if (catalog == null || isEmpty(catalog.getDeviceItemList())) return;
    List<DeviceChannelDTO> channels = catalog.getDeviceItemList().stream()
        .filter(it -> it != null && it.getDeviceId() != null)
        .map(it -> toChannelDTO(event.deviceId(), it)).toList();
    deviceChannelManager.batchUpsert(channels);             // 幂等批量
}
```

`DeviceChannelManager.batchUpsert`：

- MySQL 用 `INSERT ... ON DUPLICATE KEY UPDATE`（命中 UNIQUE `(channel_id, device_id)`），天然幂等，免"查-分流"竞态；
- SQLite 用 `INSERT ... ON CONFLICT(...) DO UPDATE`；
- 单事务批量。

> 64 路 NVR：64 轮 →（同设备串行下）1 次批量幂等 upsert。重传两份目录也不会撞唯一键。
>
> ⚠️ **跨节点（M1 同源）**：同设备目录在**单节点内**串行落同槽；但若该设备在两节点都收到目录（漂移/重传打到不同节点），仍可能并发 upsert。`ON DUPLICATE KEY UPDATE` / `ON CONFLICT DO UPDATE` 在 DB 层保证幂等，跨节点并发也不撞 UNIQUE——这正是必须用 upsert 而非"先查后插"的原因。

---

## 9. 专题五：外部系统韧性（熔断·双向降级）

> 解决 P10 及抖动传导。引入 Resilience4j（Spring Boot 3 友好）。

### 9.1 依赖隔离矩阵

| 依赖 | 调用点 | 隔离 | 降级 |
|------|--------|------|------|
| ZLM HTTP API | `StreamProxyZlmWrapperService` 等 | 熔断 + 超时 + 独立线程池(bulkhead) | 返回 `ResultDTO` 错误，不阻塞 SIP |
| SIP 命令下发 | `dispatchEnvelope` / 跨节点转发 | 超时 + 有限重试 | 返回失败，记录待重试；502/503 短重试（§10.4） |
| **Redis-A**（状态热存/缓存/路由/dirty） | `DeviceStateService`/缓存 | 超时 + 熔断 | **可降级回 DB（读写对称，§9.3）** |
| **Redis-B**（INVITE 上下文，独立实例/HA） | `RedisInviteContextStore` | 物理隔离，不与 Redis-A 共用实例（修 A5） | **不可降级，`find/save` 故障抛 503**（已实现）；`remove` 仅告警 |
| DB | 持久层 | HikariCP 超时 + 慢查询监控 | 快速失败 |

> **A5 物理隔离要点**：状态热存"可降级回 DB"，但 INVITE 上下文"故障必 503"。若二者共用同一 Redis 实例，则该实例一挂——状态虽降级到 DB 仍可用，**但所有新 INVITE 全部 503，媒体点播/回放整体瘫痪**。因此 INVITE 上下文必须落**独立 Redis 实例或哨兵/集群 HA**，使其故障率独立于状态热存。配置上 `RedisInviteContextStore` 应注入独立的 `StringRedisTemplate`（独立连接工厂指向 Redis-B），不复用缓存用的默认模板。

> ⚠️ **B6 落地代码改造（Stage 4 必修）**：现状 `RedisInviteContextStore`（voglander-integration，Stage 5 SIP 迁移产物）使用 `@Autowired StringRedisTemplate` —— 注入的是默认 bean，与 `RedisCacheManager` / `RedisCache` / `RedisLockUtil` **共享同一连接工厂**，A5 物理隔离形同虚设。落地清单：
> ① 新增 `gateway.gb28181.store.redis.*` 一组配置（`host/port/database/password/timeout` 等），独立于 `spring.data.redis.*`；
> ② 在 `RedisInviteContextStore` 所在配置类（或新建 `InviteStoreRedisConfig`）下，按上述配置定义 `LettuceConnectionFactory inviteRedisConnectionFactory` + `StringRedisTemplate inviteStringRedisTemplate`（独占）；
> ③ `RedisInviteContextStore` 改为 `@Qualifier("inviteStringRedisTemplate")` 注入，不再共享默认 bean；
> ④ 默认配置仍指向同一实例（向后兼容），但**生产部署必须将 Redis-B 配置指向独立实例 / 哨兵集群**，运维清单中明列；
> ⑤ 健康检查 `HealthCheckController` 分别上报 Redis-A / Redis-B 状态（§12.3）。

### 9.2 ZLM 熔断 + 鉴权补全

```java
@CircuitBreaker(name="zlm", fallbackMethod="addProxyFallback")
@Bulkhead(name="zlm", type=THREADPOOL)
@TimeLimiter(name="zlm")
public ResultDTO<StreamProxyItem> addStreamProxy(StreamProxyRequest req) { ... }
```

并补全 `onPlay`/`onPublish` 鉴权（密钥/IP 白名单/会话票据），关闭默认放行空洞（P10）。

### 9.3 Redis 状态热存的双向降级 + 恢复预热（修 H6）

```
熔断 OPEN 期间（仅 Redis-A 状态热存）：
  写：heartbeat/markOnline → 直接 DeviceManager.patchLiveness（DB，2a 路径）
  读：isOnline/onlineCount → 直接查 DB（status 列 + 索引）
  命令路由：dev:node:{id} 不可读 → 回退"本地直接下发 + 失败由设备重连自愈"
           （降级期不保证命中设备所在节点，属可接受的临时降级，§10.4）
  ★ 状态读写都走 DB，保持对称，不会"写了 DB 却从空 Redis 读出离线"

HALF_OPEN → CLOSED（恢复）：
  1. 先从 DB 预热：把 ONLINE 设备批量写回 dev:online:* / ZSet / dev:node:{id}（带 TTL）
  2. 预热完成再把读写切回 Redis
  ★ 避免恢复瞬间 Redis 为空导致全体误判离线
```

> **Redis-A 与 Redis-B 语义不同，降级策略绝不能共用**：Redis-B（INVITE 上下文）关乎跨节点路由正确性，丢失会路由错乱，故障必须 503；Redis-A（状态热存/缓存/命令路由）可由 DB 兜底，故障可降级。物理隔离（§9.1 A5）后，Redis-A 熔断不影响 Redis-B，反之亦然。

---

## 10. 专题六：多节点一致性（命令亲和路由 + 无锁瓜分）

> 解决 P9。**v3 关键修订 A1**：框架跨节点转发**仅覆盖 INVITE 回包**，命令下发需自建亲和路由；flush 无锁 SMOVE 瓜分。

### 10.1 跨节点能力（核对框架源码后的真实边界）

逐源码核对 `gateway-core 1.8.0` / `gateway-gb28181 1.8.0` 后确认：

| 关注点 | 框架是否自带 | 方案 |
|--------|------------|------|
| INVITE 跨节点回包 | ✅ **唯一自带** | `Gb28181InviteResponseController#inviteResponse`：按 `InviteContextStore` 的 `{nodeId,ctxKey}`，本节点处理 / 否则按 `gateway.nodes` 表 HTTP 转发到原 INVITE 节点。`RedisInviteContextStore`（已实现）+ Redis-B |
| **命令到达设备所在节点**（PTZ/点播/查询/录像…） | ❌ **框架不提供** | `ServerCommandSender.xxx(deviceId)` 一律走本地 `DeviceSessionCache.getToDevice→SipSender`，**没有**"找出设备注册节点再转发"的逻辑。**必须自建 §10.4** |
| 设备状态共享 | — | Redis-A 在线态全局可见 |
| 缓存一致 | — | Redis-A 缓存共享（无 L1，无 pub/sub） |

> **为什么不能不做 §10.4**：设备 A 的 UDP 注册通道绑定在 node-1 的 SIP socket。LB 把"对 A 下发 PTZ"的 REST 打到 node-2 → node-2 `ServerCommandSender.devicePtzCmd("A")` 通过**自己的** SIP socket 发 MESSAGE → 设备虽可能收到，但**响应/事务上下文在 node-2 不存在**，回包对不上，命令静默失败。即使两节点对外同一 VIP，UDP 也只会送达内核真实绑定的那台。所以"接入层无状态"不能靠框架兑现，必须补命令路由。

### 10.2 定时任务多节点防重（修事实-2 + M4：确无看门狗）

已核对 `RedisLockUtil` 源码：仅 `lock/tryLock/unLock`，**确无续期 API**（固定 TTL + Lua 校值解锁）。

- **唯一推荐·无锁原子瓜分**：flush 不抢全局锁；各节点 `SSCAN dev:dirty → SMOVE 到本节点 inflight → 写 → SREM`（§5.2-4）。`SMOVE` 原子性保证一个 deviceId 只被一个节点搬走，**天然多节点安全、无写放大、无需锁**。`detectOffline` 同理用条件原子离线（§5.2-5），多节点并跑无害。
- **不推荐·选主续期**：若某任务确需单点执行（本方案无此类任务），用 `tryLock(key,value,ttl)` 抢锁后**必须在任务内自行循环 `expire` 续期**（周期 ≤ TTL/3），且**续期失败立即中止任务**（否则会"自以为持锁、实际已被他人抢走"导致双跑）；`unLock(key,value)` 必须带 value 防误删。**因复杂易错，本方案一律改用无锁瓜分，删除此备选的实际使用**。

> 结论：**不存在自动看门狗，不要依赖它**。本方案所有周期任务（flush / detectOffline / 延迟双删 / sweeper）均用"原子操作 + 幂等"实现多节点安全，**全程无分布式锁**。

### 10.3 扩展性边界

- 接入层无状态（命令经 §10.4 路由）→ 水平扩展；
- 状态收敛 Redis-A/DB、INVITE 收敛 Redis-B → 节点宕机后设备重连其他节点，在线态/INVITE 上下文不丢；分片队列/合并窗等节点本地状态丢失无害（红线 6）；
- 瓶颈前移到 Redis/DB → Redis 可集群分片，DB 可读写分离（§11.3）。

### 10.4 命令下发的跨节点亲和路由（必修 A1）

复用框架 INVITE 回包同款"`nodeId` + `gateway.nodes` 表 + HTTP 转发"模式，把它推广到**所有**命令下发。

**(1) 路由表来源**：`dev:node:{deviceId} → nodeId`（Redis-A），由心跳 Lua（§5.2-2）/ 注册 / 上线时写入并随心跳续期。设备漂移后下一心跳即指向新节点。

**(2) 下发入口统一加一层路由判断**（Web 控制器 → 命令下发处）：

```java
public <T> ResultDTO<T> dispatchCommand(String deviceId, CommandPayload payload) {
    String targetNode = redisCache.getCacheObject(nodeKey(deviceId));   // dev:node:{id}
    if (targetNode == null || targetNode.equals(thisNodeId)) {
        // 路由未知或就在本节点 → 本地下发（路由未知时本地尝试，失败由重连自愈）
        return localCommandExecutor.execute(deviceId, payload);
    }
    String baseUrl = gatewayProps.getNodes().get(targetNode);
    if (baseUrl == null) {
        // 节点表未刷新到该 nodeId → 502，调用方 200ms×3 短重试（与框架 INVITE 错误约定一致）
        return ResultDTO.fail(502, "unknown node: " + targetNode);
    }
    try {
        return forwardRest.postForObject(baseUrl + "/internal/sip/command",
            new ForwardCmd(deviceId, payload), ResultDTO.class);          // 转发到设备所在节点
    } catch (RestClientException e) {
        return ResultDTO.fail(503, "forward failed: " + e.getMessage());  // 503 短重试
    }
}
```

**(3) 接收端**：各节点暴露内部端点 `POST /internal/sip/command`（仅集群内网可达），收到后调本地 `ServerCommandSender`。**与框架 `/gateway/gb28181/invite/response` 同构**，错误码沿用 410/502/503 语义。

> ⚠️ **B5(a) 鉴权强约束**：`/internal/sip/command` **必须**在 Spring Security/Filter 链或拦截器层做：① IP 白名单（仅 `gateway.nodes` 表内节点 IP 可达）；② 共享密钥（HMAC 或固定 token，与框架 INVITE 转发同款）；③ 路径限制为 `/internal/**` 子树，禁止被外网 LB 透出。**缺一不可** —— 无认证的内部命令端点等同于"任意网络可达者皆可对任意设备发 PTZ/INVITE"，是 RCE 等级的安全空洞。配置示例需在 Stage 5 验收清单中具体列出。

**(4) 节点存活表**（供 sweeper §5.2-4 与路由健康判断）：`gateway.nodes` 是静态配置；动态存活用 `node:alive:{nodeId}`（Redis-A，每节点定时续期 TTL 如 15s）。`isNodeAlive(nodeId)` = 该 key 是否存在。

**(5) 降级**：Redis-A 熔断期 `dev:node` 不可读 → 回退本地直接下发（§9.3），不保证命中设备节点，属临时降级；恢复预热时重建 `dev:node`。

**(6) 边界与代价**：

- 新增 1 个内部转发端点 + 1 个 Redis key + 1 张存活表，复用既有 `GatewayProperties.nodes` 与 `gatewayForwardRestTemplate`，与框架风格一致；
- 路由收敛窗口内（设备刚漂移、`dev:node` 未及时续期）可能转发到旧节点 → 旧节点本地下发失败 → 502/503 触发调用方短重试，重试时路由已收敛。属可接受的最终一致；
- **不引入设备粘性 LB**（耦合运维、要求调用方带 deviceId hint），改用 Redis 路由表，接入层保持完全对等无状态。

> ⚠️ **B5(b) 漂移收敛窗口与心跳间隔强相关**：路由表 `dev:node:{id}` 由心跳 Lua 续期，TTL = `deviceTimeout`。设备漂移到新节点后，**下一次心跳到达前** `dev:node` 仍指向旧节点（key 未续期到新节点）。故漂移收敛上界 ≈ "设备到新节点首次心跳的间隔"：
> - 默认配置（设备 60s 心跳、`deviceTimeout=5min`）：收敛 ≤ 60s，期间 §16 的 "200ms × 3 短重试"足以覆盖；
> - **长心跳设备（如 5min）**：收敛可能达 5min，期间所有命令转发到旧节点失败。如客户有此类设备，须在产品侧约束：**设备重连/上线后立即发首次心跳**（GB28181 规范允许），或在 `VoglanderBusinessNotifier.onLifecycleOnline` 中**主动调用 `markOnline` 续期一次** `dev:node`，把收敛窗口从"下一心跳"压到"上线事件"瞬间。该约束写入 §16 参数表。

---

## 11. 数据库与持久层优化

> 解决 P7/P8。

### 11.1 索引补充（MySQL）

> ⚠️ **B4 纠偏**：v3 索引清单与现状有偏差，落地前以下面修正后的清单为准。

```sql
-- ① tb_device：当前仅 PRIMARY KEY + uk_device(device_id)，状态/心跳查询全表扫描
ALTER TABLE tb_device         ADD INDEX idx_status_keepalive (status, keepalive_time);
ALTER TABLE tb_device         ADD INDEX idx_server_ip (server_ip);

-- ② tb_device_channel：复合 UNIQUE 最左前缀是 channel_id，无法支撑"按 device_id 查通道"
--    （voglander.sql 现状：UNIQUE KEY channel_id (channel_id, device_id) USING BTREE，已存在，无需重建）
ALTER TABLE tb_device_channel ADD INDEX idx_device_id (device_id);

-- ③ tb_media_session：call_id 已是 uk_call_id UNIQUE（M1 幂等键前提 ✅ 满足，无需 ADD）；
--    已存在 idx_media_session_device_id / idx_media_session_status，无需补充
```

线上用 `pt-online-schema-change` 加索引避免锁表。

> ⚠️ **删除 v3 的两条多余 DDL**：
> - ~~`ADD UNIQUE KEY uk_channel_device (channel_id, device_id)`~~ —— 现状已有 `UNIQUE KEY channel_id (channel_id, device_id)`，仅命名不同；再 ADD 会重复定义。如需统一命名，单独走"重命名/重建"PR，与本优化方案解耦；
> - ~~`call_id 须为 UNIQUE`~~ —— `uk_call_id` 已存在。

> ⚠️ **B3 关联**：`tb_device_channel.uk_channel_id (channel_id, device_id)` 与 `tb_media_session.uk_call_id` 是 §8 / §7.3 跨节点幂等 upsert（`ON DUPLICATE KEY UPDATE`）的**数据库层前提**。前提已满足，但**代码侧 upsert 改造**（Stage 3 / B3）仍待落地。

> 若线上历史数据中存在重复（无 UNIQUE 时期写入），加 UNIQUE 前先用一次性脚本去重；本方案的 `tb_device_channel`/`tb_media_session` UNIQUE 已存在，跳过该步。

### 11.2 批量与大结果集

| 场景 | 方案 |
|------|------|
| 状态批量回写 | 两列 `UPDATE`，`<foreach>` 或 `IN` 批 |
| 目录落库 | `INSERT ... ON DUPLICATE KEY UPDATE`（§8） |
| 大结果集 | **禁止** `Integer.MAX_VALUE` 分页（修 P7）；`listDeviceDTO` 重构为显式分页/游标 |

### 11.3 连接池与读写分离

- HikariCP `maximum-pool-size`（当前 20）按"批量回写并发 + REST 并发"压测重定；
- dynamic-datasource 4.3.1 已具读写分离基础：查询走从库、写走主库（规模到位再开）。

---

## 12. 统一异常与可观测性

> 解决 P6 + 高稳定必备。

### 12.1 异常规范回归（修 P6）

- 移除 Manager 模板 `catch(Exception)→throw new RuntimeException` 反模式；
- 业务校验失败抛 `ServiceException` + `ServiceExceptionEnum`（无对应枚举先补，遵循 CLAUDE.md）；
- 仅系统异常才包装并保留 cause；`GlobalExceptionHandler` 统一映射错误码。

### 12.2 关键指标（SkyWalking 9.1.0）

| 指标 | 告警阈值 |
|------|----------|
| 心跳处理延迟 P99 | > 50ms |
| ingress 入队池/分片队列深度 | > 80% 容量 |
| 设备缓存命中率 | < 80% |
| Redis-A / Redis-B 熔断状态 | open 即告警（分别监控） |
| ZLM 调用失败率 | > 5% |
| `dev:dirty` 积压 / inflight 滞留 | 持续增长（回写跟不上 / sweeper 未生效） |
| 状态回写滞后 | > 2×flush 周期 |
| 命令跨节点转发 502/503 率 | > 1%（路由表收敛异常 / 节点存活表失准） |
| `dev:node` 路由命中率 | < 95%（漂移频繁或续期滞后） |
| 在线设备数突变 | 异常波动 |

### 12.3 依赖级健康检查

`HealthCheckController` 扩展：Redis / DB / SIP 监听 / ZLM 节点各自 UP/DOWN/DEGRADED，供 LB 摘流。

---

## 13. 分阶段实施路线

> 每阶段独立可验收、灰度、回滚；遵循"小步提交、可编译、可测试"。

### Stage 0：启用调度 + 现状校验（🔴 硬阻塞前置，必做）

**B1 修复**：方案中 flush / detectOffline / sweeper / 延迟双删扫描 / 节点存活续期等周期任务**全部基于 `@Scheduled`**，但项目当前**没有 `@EnableScheduling`**（`ApplicationWeb` 只有 `@SpringBootApplication` + `@ComponentScan` + `@EnableSipServer`）—— Stage 1~6 任一周期任务上线都会**沉默失败**。

落地清单（单 PR）：

1. `ApplicationWeb` 加注解：`@EnableScheduling`（与 `@EnableSipServer` 同级）；
2. 校验**既有的 2 处** `@Scheduled` 在启用后开始执行：
   - `StreamProxyBizServiceImpl @Scheduled(fixedRate=30000)` —— 每 30s 执行（用日志或埋点确认）；
   - `SpringDynamicTask @Scheduled(cron="0 0/5 * * * ?")` —— 每 5min 执行；
3. 若两者启用后产生未预期副作用（例如清理任务批量写库），同 PR 内做开关或临时降级；
4. 验收：启动日志可见 `TaskScheduler` 初始化；两个既有任务按周期执行；新调度任务的 fixedDelay 探测埋点可看到周期触发。

**风险**：极低；唯一已知风险是上面两个既有任务从未运行过、启用后立即开始写库，需要在 PR 描述里点名提示评审人确认其语义。

### Stage 1：缓存正确性修复（🔴 必须先做，P1+P2 同提交）

- 修 P1+P2：统一 key（`DeviceCacheKey`）+ 去 `cache.clear()` + 短 TTL + 延迟双删；
- 范围：`DeviceManager`（及同模式 `StreamProxyManager`/`MediaNodeManager`）；
- 验收：命中率压测 > 80%；并发读写一致性测试无脏读；
- **铁律**：P1、P2 同提交，单独去 clear 会立即暴露脏读。

### Stage 2a：心跳定向更新 + 合并（🔴 核心收益，低风险）

- `DeviceManager.patchLiveness` + 心跳合并 + 离线同步直写；
- 范围：`DeviceManager`、`DeviceRegisterServiceImpl`、`VoglanderBusinessNotifier` 心跳/上线/离线改调；
- 灰度：开关 `device.liveness.coalesce.enabled`，false 回退旧路径；
- 验收：心跳 DB 写频次降 ≥ 50%，心跳 P99 < 50ms，**DB 仍为唯一真相源、无新一致性风险**。

### Stage 3：事件有序化 + 目录批量（🟠）

- `DeviceEventDispatcher` 分片（**notify 改 ingress 轻量池、入队后立即归还，禁 CallerRuns**，修 H2/A2）+ 单调写（H3，跨节点正确性唯一依靠）+ `batchUpsert`（§8，含 M1 跨节点幂等）+ 补登记；
- **B3 同步落地（必含）**：`MediaSessionManager.onInviteOk/onAck/onInviteFailure` + `DeviceChannelManager.batchUpsert` 全部从"先查后插"改为**数据库原生 upsert** —— MySQL `INSERT ... ON DUPLICATE KEY UPDATE` / SQLite `INSERT ... ON CONFLICT(...) DO UPDATE`；并 `try { ... } catch (DuplicateKeyException e) { 转 UPDATE }` 兜底；不再依赖框架 `invite-idempotency-window-ms` 做跨节点幂等；
- 验收：乱序注入（Online 早于 Register、旧心跳后到）状态最终正确；**节点间漂移**（Register→node-1、旧 Online→node-2）最终正确；64 路目录单次落库且跨节点重传不撞 UNIQUE；**双节点并发 upsert 同 callId 不抛 UNIQUE 异常**（B3 专项）。

### Stage 4：外部韧性 + 双向降级（🟠）

- Resilience4j 熔断/隔离；ZLM 鉴权补全；
- **A5 / B6 物理隔离落地（必含）**：新增 `gateway.gb28181.store.redis.*` 配置 + `inviteRedisConnectionFactory` + `inviteStringRedisTemplate`；`RedisInviteContextStore` 改 `@Qualifier("inviteStringRedisTemplate")` 注入；`HealthCheckController` 分别上报 Redis-A / Redis-B；
- Redis-A 故障**读写对称降级 + 恢复预热**（H6，含 `dev:node` 重建）；
- 验收：混沌测试断 Redis-A/ZLM，核心链路存活；**断 Redis-A 时 INVITE（Redis-B）不受影响**（A5/B6 专项，需独立实例部署后验证）；ZLM 抖动不传导 SIP；降级期读写一致。

### Stage 2b：Redis 热存（🟡 按需，仅当 2a 不足）

- Redis-A 在线态真相源 + Lua 单调热写（含路由表续期）+ **SSCAN→SMOVE inflight→写→SREM 批量回写 + sweeper**（修 H1/A3）+ ZSet 轮询 + 条件原子离线（修 H5 + 并发误判）+ 读侧降级（H6）；
- 灰度：开关 `device.state.hot-store.enabled`，false 回退 2a；
- 验收：50K 设备心跳压测，DB 写 QPS 降 ≥ 90%，无丢终态、无误判离线、**多节点 flush 无写放大无重复**。

### Stage 5：多节点 + DB 优化（🟡）

- Redis-B INVITE store 切换；**自建命令亲和路由 `/internal/sip/command` + `dev:node` 路由表 + 节点存活表（必修 A1，§10.4）**；周期任务**全程无锁原子瓜分**（修事实-2/M4）；索引补充（按 §11.1 v4 修正后清单，已删除重复 UNIQUE DDL）；`listDeviceDTO` 重构；
- **B5(a) 鉴权落地（必含）**：`/internal/sip/command` 配置 IP 白名单（仅 `gateway.nodes` 节点 IP）+ 共享密钥 HMAC/token 头校验 + 路径限定 `/internal/**`，与外网 REST 路由隔离；安全测试用例覆盖"伪造来源 IP / 缺密钥 / 错密钥"三种拒绝路径；
- **B5(b) 漂移收敛**：在 `VoglanderBusinessNotifier.onLifecycleOnline` 显式触发 `dev:node:{id}` 续期一次，把长心跳设备的漂移收敛窗口从"下一心跳"压到"上线事件"；
- 验收：双节点跨节点 INVITE 回包成功；**命令打到非设备节点能正确转发并回包**；伪造来源 IP / 缺密钥的 `/internal/sip/command` 返回 401/403（B5 安全专项）；kill 一节点后 inflight 被 sweeper 还原、flush 不重复成功处理；无全表扫描慢查询。

### Stage 6：可观测 + 异常规范（🟡）

- 指标埋点 + 告警 + 异常回归 `ServiceException` + 依赖级健康检查；
- 验收：§12.2 指标全可见，故障先于用户被发现。

---

## 14. 风险评估与回滚预案

| 变更 | 风险 | 回滚/缓解 |
|------|------|----------|
| **`@EnableScheduling` 启用(B1/Stage 0)** | 既有 2 个 `@Scheduled` 从未运行、启用后立即开始写库 | PR 描述点名既有任务、评审人确认语义；如有副作用，同 PR 加开关或注释临时停用，再独立 PR 处理 |
| 去 `cache.clear()` | key 修复不彻底→脏读 | 开关 `cache.precise-evict.enabled`，false 回退全清（退化但安全）；延迟双删兜底竞态 |
| **5 类旧 evict key 残留(B2)** | 仅删 `do:/dto:` 漏掉裸 `id` → 旧值留到 TTL | §6.1 改造 PR 内**同时**删除 5 类 evict + 改 `@Cacheable` key + redis-cli 清残留；测试用例需覆盖"裸 id key 是否仍存在" |
| 心跳合并(2a) | 合并窗内崩溃丢 keepalive 精度（节点本地状态） | 状态(ONLINE)不丢可自愈；窗口可调小；离线同步写不受影响；漂移后新节点补写一条无害 |
| 分片 ingress 化 | 入队前处理拖慢 SIP 线程 / CallerRuns 拖死 SIP | 入队只放原始事件、重活在槽内；**ingress 池禁 CallerRuns**、满则丢冗余 Keepalive；监控 SIP 线程耗时 |
| 分片跨节点失序(A2) | 误以为分片=跨节点有序 | 红线 2 钉死"仅节点内有序"；正确性靠单调写(红线 3)；漂移专项测试兜底 |
| Redis-A 热存(2b) | Redis-A 成新单点 | 双向降级回 DB（Stage4 已含）+ 恢复预热；Redis 集群化 |
| 批量回写(2b) | 节点崩溃丢未回写自愈字段 | **SMOVE inflight + sweeper** 不丢；终态本就同步写；重启从 Redis/DB 重建 |
| SMOVE 写放大(A3) | SSCAN 多节点重复写 DB | 改 SMOVE 原子瓜分，一个 deviceId 只一节点处理；重复写也幂等(单调 UPDATE) |
| 延迟双删本地定时(A4) | 进程崩在两次 evict 间→脏到 TTL | 改 Redis ZSet 延迟队列、全节点共消费；短 TTL 兜底 |
| 命令亲和路由(A1) | 路由表滞后→转发到旧节点失败 | 502/503 触发调用方 200ms×3 短重试；收敛后命中；降级期回退本地下发 |
| **`/internal/sip/command` 鉴权(B5a)** | 未鉴权→任意网络可达者发命令(RCE 等级) | Stage 5 同 PR 必含 IP 白名单 + 密钥 HMAC + 路径限定；安全测试 3 种拒绝路径；缺一不可合并 |
| **长心跳设备漂移收敛(B5b)** | 路由收敛窗口达 5min，期间命令转发失败 | `onLifecycleOnline` 显式续期 `dev:node`；产品侧约束设备重连立即发首次心跳 |
| INVITE/状态共用 Redis(A5) | Redis 挂→INVITE 全 503 媒体瘫痪 | **物理隔离 Redis-B + HA**；Redis-A 故障不波及 Redis-B |
| **`RedisInviteContextStore` 共享默认 RedisTemplate(B6)** | A5 物理隔离形同虚设 | Stage 4 同 PR 必含独立 `inviteRedisConnectionFactory` + `inviteStringRedisTemplate` + `@Qualifier` 注入；运维清单要求生产 Redis-B 指向独立实例 |
| 跨节点会话 upsert(M1/B3) | 并发撞 callId/channel UNIQUE | **代码改造未完成**：Stage 3 同 PR 必含 `ON DUPLICATE KEY UPDATE` + catch DuplicateKey；不依赖框架单节点窗口 |
| 单调时间戳 | 设备时钟漂移致误判 | 用网关接收时间戳(`GatewayEvent.timestampMs`)而非设备时钟 |
| flush 多节点 | 无看门狗→锁失效 | **全程无锁原子瓜分**（修 M4），不依赖锁；删除"自续期选主"备选 |
| 索引新增 | 写放大 | 热路径已降 DB；`pt-osc` 在线加；`tb_device_channel` UNIQUE / `tb_media_session.uk_call_id` 已存在无需 ADD(B4) |

通用：**每个 Stage 带开关**，生产先灰度，指标正常再全量；异常一键回旧路径。

---

## 15. 验证与压测方案

### 15.1 测试（遵循 CLAUDE.md 分层）

- `DeviceManager.patchLiveness`/`batchUpsert`：Manager 集成测试（`BaseTest`）；单调条件 UPDATE 专项（旧 ts 不覆盖）；
- 缓存一致性：并发读写后校验缓存与 DB 一致；延迟双删（Redis ZSet）覆盖回填竞态；
- **有序+单调**：注入乱序(Online 早于 Register)、旧时间戳后到 → 断言最终状态正确（专项，对应 H2/H3）；
- **节点间漂移（A2 专项）**：node-1 注入 Register，node-2 注入旧 ts Online/Keepalive → 断言最终 status=ONLINE 且 keepalive 取较新值（验证分片串行≠跨节点、单调写兜底）；
- **终态不丢**：flush 前 kill 进程 → 重启后离线态仍在 DB（对应 H1）；
- **inflight sweeper（A3 专项）**：搬入 inflight 后 kill 节点 → sweeper 将其还回 `dev:dirty`、他节点最终落库，无丢无重复成功；
- **离线并发误判**：detectOffline 判定瞬间并发注入新心跳 → 断言设备保持 ONLINE（条件原子离线生效）；
- **命令亲和路由（A1 专项）**：双节点，设备注册 node-1，REST 命令打 node-2 → 断言经 `/internal/sip/command` 转发到 node-1 并正确回包；`dev:node` 缺失时回退本地、502/503 重试后成功；
- 目录批量：64 路 + 重传两份 + 跨节点各收一份 → 通道数正确、无 UNIQUE 冲突（对应 §8/M1）；
- 降级对称：模拟 Redis-A 熔断 → 状态读写都走 DB、结果一致、`dev:node` 回退本地；恢复 → 预热(含 `dev:node`)后无误判（对应 H6）；断 Redis-A 时 INVITE(Redis-B) 不受影响（A5）；
- 幂等：重投同 callId/心跳 → 无副作用；跨节点并发 upsert 同 callId → 不撞 UNIQUE（M1）。

### 15.2 压测场景

| 场景 | 目标 |
|------|------|
| 心跳风暴（50K/60s） | DB 写 QPS、心跳 P99、缓存命中率 |
| 注册风暴（1 万并发） | 无丢事件、背压生效 |
| 目录上报（1000×64 路） | 批量落库、DB 压力、无唯一键冲突 |
| 媒体并发（5K INVITE） | 会话成功率、`tb_media_session` 一致 |
| 双节点命令路由（命令打非设备节点） | 转发成功率、回包正确率、502/503 重试收敛 |
| 设备漂移风暴（节点滚动重启） | 漂移后状态最终正确、命令路由收敛时间、无误判离线 |
| 混沌（随机断 Redis-A/Redis-B/ZLM/DB） | 核心链路存活、降级对称、Redis-A 断不影响 INVITE、自动恢复 |
| 乱序/重传专项 | 有序、单调、幂等、终态不丢 |

### 15.3 验收门槛

- §1.2 SLO 全达标；
- 单依赖故障演练通过（降级运行且读写一致）；
- 压测无 OOM、无连接耗尽、无终态丢失、无误判离线。

---

## 16. 附录：关键参数基线

| 参数 | 建议初值 | 说明 |
|------|----------|------|
| 心跳合并窗(2a) | 30s | 距上次持久化 < 窗则不写 DB |
| `dev:online:{id}` TTL(2b) | = `deviceTimeout`(默认 5m) | 每心跳续期 |
| flush 周期(2b) | 5s | 自愈字段批量回写 |
| flush 批大小 | 500~1000 | 单批 IN 上限 |
| 离线扫描周期(2b) | 30s | ZSet `ZRANGEBYSCORE` 取超时 |
| 分片槽数 N | 16~32 | ≈ CPU 核数倍数；同设备恒定槽（仅节点内有序） |
| ingress 入队池 | 核心 2~4 / 队列 10000 / **禁 CallerRuns** | 只做 hash+offer，满则丢冗余 Keepalive |
| 单槽队列上界 | 2000 | 满 → 丢冗余 Keepalive 保关键事件 |
| device 缓存 TTL | 2~3min + 随机抖动 | 短 TTL 收敛脏读窗口（修 H4） |
| 延迟双删间隔 | 500ms | Redis ZSet 延迟队列，全节点共消费（修 A4） |
| 延迟双删扫描周期 | 200ms | `cache:evict:delay` ZSet 到点扫描 |
| `dev:node:{id}` TTL(2b) | = `deviceTimeout` | 命令路由表，随心跳续期（§10.4） |
| `node:alive:{id}` TTL | 15s | 节点存活表，每节点定时续期；sweeper/路由判活 |
| inflight TTL | 2×flush 周期(默认 10s) | `dev:dirty:inflight:{node}` 兜底；sweeper 据此判僵死 |
| sweeper 周期 | 60s | 扫 `dev:dirty:inflight:*`，僵死节点成员还回 dev:dirty |
| 跨节点转发超时 | `gateway.forward-timeout-ms`(默认 3000) | `/internal/sip/command` 与 INVITE 回包共用 |
| 命令转发重试 | 200ms × 3 | 502/503 短重试，与框架 INVITE 错误约定一致 |
| **路由表漂移收敛上界(B5b)** | ≈ 设备心跳间隔 | 默认 60s 心跳→≤60s；长心跳设备须靠 `onLifecycleOnline` 显式续期把窗口压到上线事件瞬间 |
| **内部端点鉴权(B5a)** | IP 白名单 + 共享密钥 HMAC | `/internal/sip/command` 必须同时启用；缺一即视为安全空洞 |
| ZLM 熔断 | 失败率 50%/10s 窗 | Resilience4j 起步 |
| ZLM 超时 | 3s | TimeLimiter |
| HikariCP max-pool | 压测定（当前 20） | 回写并发 + REST 并发 |

---

## 结语

v4 在 v3"多节点无状态并发安全"基础上，针对**落地前可执行性**做了硬阻塞补齐：方案核心的 flush / detectOffline / sweeper / 延迟双删 / 节点存活续期全部依赖 `@Scheduled`，而项目当前没有 `@EnableScheduling`（5 字符的修改却会让所有兜底机制静默失败）—— 故新增 **Stage 0** 作为硬阻塞前置；同步把 B2~B6 五处隐藏门槛（5 类旧 evict key 全清、`MediaSessionManager` upsert 改造尚未落地、索引清单与现状偏差、`/internal/sip/command` 鉴权强约束、`RedisInviteContextStore` 共享默认 RedisTemplate 使 A5 形同虚设）从"隐含假设"提升为各 Stage 验收清单的显式必含项。

**最终对外承诺**（v3 不变，v4 补全落地条件）：
- **接入层无状态**：任意节点可收任意 REST，命令经 `dev:node` 路由表 + HTTP 转发到设备所在节点；
- **任意节点宕机不丢终态**：终态同步落 DB；自愈字段经 inflight + sweeper 不丢；节点本地状态丢失无害；
- **跨节点并发不错乱**：分片保证节点内有序，单调写（Lua GT / DB 条件 UPDATE）保证跨节点正确，幂等 upsert 保证并发不撞唯一键。

实施次序铁律（v4 更新）：
**Stage 0（启用调度，硬阻塞）→ Stage 1（缓存正确性，P1+P2+B2 同提交）→ Stage 2a（心跳定向更新 + 单调写，零新一致性风险）** 最先落地；多节点相关的 A1（命令路由 + B5 鉴权）/A3（SMOVE）/A5+B6（Redis 隔离 + 独立模板）/B3（upsert 改造）随 Stage 4/2b/5 落地，且必须带齐开关灰度与上述专项测试。

> 需要的话，我可从 Stage 0 开始编码（5 字符改动 + 既有 2 个 `@Scheduled` 副作用核查），然后 Stage 1（风险最低、收益立竿见影）；或对任一 Stage（尤其 §10.4 命令亲和路由 + B5 鉴权、§5.2 SMOVE inflight 改造、B6 独立 RedisTemplate 配置）出类图/接口签名/Lua 脚本/测试用例的详细技术设计。
