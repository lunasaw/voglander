# Voglander 1.0.4 设计：DeviceChannelManager × gb-gateway 流程融合（高并发版）

> 版本：1.0.4 ｜ 编写日期：2026-06-02 ｜ 修订：高并发/高稳定性审查后版本
> 对应分支：`dev_merge_sip` ｜ 基线：1.0.3 已完成（Phase 0/1/2a/3/4/8 部分）
> 上游依据：[1.0.3/ARCHITECTURE.md](../1.0.3/ARCHITECTURE.md) ｜ [1.0.3/PROTOCOL-EXTENSIBILITY-DESIGN.md](../1.0.3/PROTOCOL-EXTENSIBILITY-DESIGN.md)
>
> **文档性质**：技术方案。把 DB 的设备通道管理（`DeviceChannelManager`）与 gb-gateway 入站事件流（`VoglanderBusinessNotifier → ShardDispatcher → Gb28181ProtocolHandler`）整体打通——通道生命周期不再只跟"目录响应"耦合，而是显式建模为**`发现 → 在线 → 离线 → 清理`**四态闭环，与设备生命周期同步、与 GB28181 真实信号对齐。
>
> **🔴 重要修订（2026-06-02）**：增加 7 项并发缺陷修正（C1-C4/R4-R7），调整 Stage 实施顺序（Schema 先行），补充监控埋点。

---

## 目录

1. [问题与目标](#1-问题与目标)
2. [源码事实核验](#2-源码事实核验)
3. [整体融合架构](#3-整体融合架构)
4. [并发安全设计](#4-并发安全设计-新增)
5. [Stage 0：Schema 与缓存基础设施](#stage-0schema-与缓存基础设施-前置)
6. [Stage 1：通道在线态从 catalog 落库](#stage-1通道在线态从-catalog-落库)
7. [Stage 2：设备离线级联通道下线](#stage-2设备离线级联通道下线)
8. [Stage 3：通道单调写 + 心跳跟随](#stage-3通道单调写--心跳跟随)
9. [Stage 4：通道发现/失踪与软删除](#stage-4通道发现失踪与软删除)
10. [Stage 5：媒体会话挂钩通道状态](#stage-5媒体会话挂钩通道状态)
11. [验收门槛 & TDD 测试矩阵](#11-验收门槛--tdd-测试矩阵)
12. [监控埋点（生产保障）](#12-监控埋点生产保障-新增)
13. [回滚与灰度](#13-回滚与灰度)

---

## 1. 问题与目标

### 1.1 当前缺口（源码已核验）

| # | 缺口 | 证据 | 影响 |
|---|------|------|------|
| **G-CH1** | 目录 upsert 不写 `status` | `Gb28181ProtocolHandler.java:280-287` 仅 set `deviceId/channelId/name/extendInfo`；DB 字段 `NOT NULL DEFAULT 0` → 新通道默认**离线** | 设备已在线但通道全部 OFFLINE，UI 误判 |
| **G-CH2** | 设备 Offline 不级联通道 | `Gb28181ProtocolHandler.java:86-89` 只调 `deviceRegisterService.offline()` → `patchOfflineTerminal(deviceId)` 只动 `tb_device`；`tb_device_channel.status` 不变 | 设备断网后通道仍显示在线 |
| **G-CH3** | `DeviceChannelManager.updateStatus` 无调用方 | 全仓库 `grep` 仅出现在自身定义；入站事件流无任何 channel-level 写状态 | 在线/离线信号无落地路径 |
| **G-CH4** | `batchUpsert` 重传 catalog 不刷 `status` 字段 | `DeviceChannelManager.java:521-533`（`dtoToDo` 时 dto.status=null → DO.status=null → updateById 不覆盖） | 新通道默认 OFFLINE 且 catalog 无法将其升 ONLINE |
| **G-CH5** | `DeviceItem.status`（"ON"/"OFF"）未映射到列 | `Gb28181ProtocolHandler.handleCatalog` 只把整条 `DeviceItem` JSON 灌到 `extend.channelInfo` | 业务查询 `WHERE status=1` 时拿不到目录返回的真实在线态 |
| **G-CH6** | 重复实现：`DeviceChannelDTO.convertDO/convertDTO`（静态）与 `DeviceChannelAssembler.dtoToDo/doToDto`（Bean）同形 | `DeviceChannelDTO.java:54-85` vs `DeviceChannelAssembler.java` | 两份转换逻辑，扩展时易遗漏一边 |
| **G-CH7** | 通道删除 = 物理删除，无软删；catalog 重传若漏报旧通道，无"失踪"判定 | `DeviceChannelManager.deleteDeviceChannel*` | 设备下挂通道动态变化（拔卡/换镜头）时旧通道残留 |
| **G-CH8** | 入站 catalog 解析失败/部分失败无补偿 | `handleCatalog` 单次 try-catch；payload 异常即整批丢失 | 一次解析失败 = 一次同步丢失，下次目录请求才能恢复 |
| **G-CH9** | `DeviceChannelManager` 缓存清理混用两路径 | `clearCache(id, oldKey, newKey)` 内部对 Spring Cache `"deviceChannel"` 精确 evict，但同时调 `redisCache.deleteKey(DEVICE_CHANNEL_LIST_CACHE_KEY)` 连坐列表缓存 | 与 1.0.3 Phase 1 `DeviceCacheKey` 单一来源 + 精确 evict 未对齐 |
| **🔴 C1** | `batchUpsert` 绕过分布式锁，与单条写形成无保护并发 | `deviceChannelInternal` 在通道粒度加锁，但 `batchUpsert` 不走此入口 | catalog 事件与 REST 管理端同时操作同一通道时产生 ABA 竞争 |
| **🔴 C2** | `DuplicateKeyException` 兜底退化为 N+1 | L543-555 整批失败后循环 `getByDeviceId + updateById` | 1000 通道冲突 = 2000 次 SQL |
| **🔴 C3** | `Lifecycle.Online` 不带时间戳可覆盖 OFFLINE 终态 | `Gb28181ProtocolHandler.java:79` 调 `patchLiveness(deviceId, ONLINE, null)`，单调条件不生效 | 乱序到达的 Online 包复活已离线设备 |
| **🔴 C4** | `clearCache` 在 `batchCreateDeviceChannel` 内循环触发 | L258-277 循环调 `createDeviceChannel → clearCache → deleteKey("device:channel:list")` | 1000 通道 = 1000 次 Redis DEL 同一个 key |

### 1.2 设计目标

1. **目录响应即通道状态源**：catalog 中每个 `DeviceItem` 的 `status` 字段（"ON"/"OFF"）显式映射到 `tb_device_channel.status`，且批量 upsert 不再把"目录里未列出的旧通道"留在原状态。
2. **设备生命周期下沉到通道**：设备 Offline → 同事务级联把该设备所有 channel 置 OFFLINE；设备 Online 不强推（避免目录还没回来时假在线），仅在 catalog/MediaStatus 显式说明时改通道。
3. **单调写不被乱序回灌**：通道与设备保持同样的"`keepaliveTime` 单调条件 + 离线终态强写"语义（对齐 1.0.3 Phase 2a，红线 R2）。
4. **零侵入既有出站命令路径**：本期不动 `commandService.queryChannel`、不改 sip-gateway 命令栈；只在入站侧补齐"信号 → 状态"的最后一公里。
5. **对齐 1.0.3 缓存红线**：仿 `DeviceCacheKey`，引入 `DeviceChannelCacheKey` 收敛唯一 key 生成；废弃 `DEVICE_CHANNEL_CACHE_PREFIX` 裸字符串，去 `redisCache.deleteKey()` 连坐风险。
6. **🆕 高并发安全**：设备粒度锁保护批量操作、冲突兜底不退化为 N+1、终态不被乱序覆盖、缓存清理不循环触发（修正 C1-C4）。
7. **TDD 全程**：每个 Stage 先 RED 后 GREEN，测试集中在 `voglander-web/src/test`（项目硬约定）。

### 1.3 非目标

- 不引入"通道心跳"概念。GB28181 协议层无 channel-level keepalive，伪造一个只会引入幻象。通道在线态完全派生自：① 目录响应、② 设备级生命周期、③ 媒体会话存在性（Stage 5）。
- 不改 `tb_device_channel` 表名/主键/UNIQUE 约束；只**新增**列与索引。
- 不动 catalog 主动订阅（SIP SUBSCRIBE）——当前是注册时拉一次 + REST 手动触发，本期沿用。

---

## 2. 源码事实核验

> 与 `dev_merge_sip` 现状逐项核验。✅=与本文设计前提一致。

### 2.1 入站事件管线

| 事实 | 状态 | 证据 |
|------|------|------|
| `VoglanderBusinessNotifier.notify` 仅做 `GatewayEvent → DeviceEvent` 翻译，@Async 后交 `ShardDispatcher` | ✅ | `wrapper/gb28181/notifier/VoglanderBusinessNotifier.java:32-44` |
| 关 `voglander.event.shard.enabled` 时由 `VoglanderBusinessNotifierFallback` 直接走 `InboundEventDispatcher.dispatch` | ✅ | 同包 `Fallback.java` |
| `Gb28181ProtocolHandler.handle` switch 已分离 Lifecycle/Notify/Response/Session，35 个分支 | ✅ | `Gb28181ProtocolHandler.java:70-176` |
| `handleCatalog` 已用 `DeviceChannelManager.batchUpsert(List)` 替代 N+1 | ✅ | 同上 `:268-293` |
| `Lifecycle.Offline` 仅调 `deviceRegisterService.offline(deviceId)` → `deviceManager.patchOfflineTerminal(deviceId)` | ✅ | 同上 `:86-89` + `DeviceRegisterServiceImpl.java:248-273` |
| 🔴 **`Lifecycle.Online` 调 `patchLiveness(deviceId, ONLINE, null)` 不带时间戳** | ⚠️ 需修正 | `Gb28181ProtocolHandler.java:79` — 单调条件失效 |

### 2.2 数据层

| 事实 | 状态 | 证据 |
|------|------|------|
| `tb_device_channel`：`id/create_time/update_time/status/channel_id/device_id/name/extend` + UNIQUE `(channel_id, device_id)` + `idx_device_id` | ✅ | `sql/voglander.sql:50-70` |
| `status` `NOT NULL DEFAULT 0` → 默认 OFFLINE | ✅ | 同上 `:58` |
| `DeviceChannelDO` / `DeviceChannelDTO` 字段一致；DTO 多出 `ExtendInfo{channelInfo}` | ✅ | 两文件 1:1 |
| `DeviceChannelManager.batchUpsert` 已实现并发兜底（`DuplicateKeyException` → 逐条转更新） | ⚠️ 需优化 | `DeviceChannelManager.java:492-562` — 兜底退化为 N+1 |
| `updateStatus(deviceId, channelId, status)` 已就绪但**无入站调用方** | ✅ | 同上 `:99-113`；`grep` 仅本类与 controller 引用 |
| 🔴 **`batchUpsert` 不加分布式锁** | ⚠️ 需修正 | 同上 `:492-563` — 与 `deviceChannelInternal` 锁不交叉 |

### 2.3 设备侧已有"参考实现"

| 事实 | 状态 | 证据 |
|------|------|------|
| `DeviceManager.patchLiveness(deviceId, status, keepaliveTime)`：单调条件 `LambdaUpdateWrapper`，仅 2 列定向更新 | ✅ | `DeviceManager.java:439-460` |
| `DeviceManager.patchOfflineTerminal(deviceId)`：无单调条件的终态强写 | ✅ | 同上 `:521-537` |
| `DeviceCacheKey` 收敛 key 生成；`@Cacheable` + 精确 evict + 短 TTL（device=3min） | ✅ | `manager/cache/DeviceCacheKey.java` + `DeviceManager.clearCache():551-571` |
| 🔴 **`patchLiveness` 的 `keepaliveTime=null` 路径不受单调条件保护** | ⚠️ 需明确 | L445-450 — `set(keepaliveTime != null, ...)` 跳过单调 WHERE |

**结论**：1.0.3 Phase 2a 在设备侧已经把"定向单调更新 + 终态强写"打磨完毕，本期只需在通道侧**复制同一套模板**，并把它接入入站 switch。**但设备侧的 `Lifecycle.Online` 分支需补充时间戳参数，防 C3 缺陷**。

---

## 3. 整体融合架构

### 3.1 信号 → 状态映射总览

```
GB28181 信号                  voglander 入站点                  对 tb_device_channel 的写
──────────────────────────    ────────────────────────────      ──────────────────────────────
Response.Catalog              Gb28181ProtocolHandler            batchUpsertWithStatus(...)
  └ DeviceItem.status="ON"      .handleCatalog                    → status=1（覆盖，受单调条件 R5）
  └ DeviceItem.status="OFF"                                       → status=0
  └ 未出现的旧 channel                                            → status=0 + missingCount++（Stage 4）

Lifecycle.Offline             Gb28181ProtocolHandler            cascadeChannelsOffline(deviceId)
  （单设备离线信号）             .handle("Lifecycle.Offline")      → WHERE device_id=? SET status=0
                                                                  （无单调条件，终态强写，R1）

Lifecycle.Online              Gb28181ProtocolHandler            🔴 修正 C3：携带 LocalDateTime.now()
  （单设备上线信号）             .handle("Lifecycle.Online")        传入 patchLiveness，单调条件生效
                                                                  + DeviceManager 内部加终态保护（R4）

Session.InviteOk              Gb28181ProtocolHandler            promoteChannelOnline(...)
                              .handle("Session.InviteOk")        （Stage 5，条件于 payload 验证通过）
                                                                 基于 MediaSession 反向推断通道在线

Session.Bye / Notify.MediaStatus     同上                       通道不改状态（流断 ≠ 通道离线）

设备主动 addChannel REST       DeviceChannelController           走 saveOrUpdate（不动）
管理端 CRUD                    DeviceChannelController           走 add/update/deleteOne（不动）
```

### 3.2 类图（新增/改动）

```
┌────────────────────────────────────────────────────────────────────────┐
│  voglander-integration                                                 │
│  wrapper/gb28181/handler/Gb28181ProtocolHandler                        │
│   ├─ handleCatalog()        ← 改：调 batchUpsertWithStatus()           │
│   ├─ handleLifecycleOnline  ← 🔴 改：传 LocalDateTime.now() 给 patchLiveness│
│   ├─ handleLifecycleOffline ← 改：除 deviceRegister.offline()，         │
│   │                              再调 channelManager.cascadeOffline()   │
│   └─ handleSessionInviteOk  ← 新：可选，promoteChannelOnline()         │
└────────────────────────────────────────────────────────────────────────┘
                              ↓ DTO
┌────────────────────────────────────────────────────────────────────────┐
│  voglander-manager                                                     │
│  manager/manager/DeviceChannelManager                                  │
│   ├─ batchUpsertWithStatus(String deviceId, List<DTO>)  ← 新（核心）     │
│   │    🔴 入口加设备粒度锁（C1）+ 一次 SELECT 复用（R5）                   │
│   ├─ cascadeOffline(String deviceId)                    ← 新           │
│   │    🔴 支持分批 UPDATE（R7）                                          │
│   ├─ promoteOnlineIfOffline(deviceId, channelId)        ← 新（Stage 5） │
│   ├─ patchChannelStatus(...)                            ← 改：单调条件  │
│   ├─ markMissingChannels(deviceId, presentIds)          ← 新（Stage 4） │
│   └─ batchUpsert(...)                                    ← 废弃，内联到上面│
│                                                                        │
│  manager/cache/DeviceChannelCacheKey                    ← 新（Stage 0） │
│   ├─ byId(Long id)                                                     │
│   ├─ byBizKey(String deviceId, String channelId)                       │
│   └─ byDevice(String deviceId)                                         │
│                                                                        │
│  manager/manager/DeviceManager                          ← 改（C3 修正） │
│   └─ patchLiveness(...)  🔴 内部加 R4 终态保护条件                       │
└────────────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────────────┐
│  voglander-repository                                                  │
│  entity/DeviceChannelDO                                                │
│   ├─ + lastSeenTime        (LocalDateTime)                             │
│   ├─ + statusSource        (String, CATALOG/OFFLINE_CASCADE/SESSION/...)│
│   └─ + missingCount        (Integer, Stage 4)                          │
│                                                                        │
│  sql/voglander.sql + schema-sqlite.sql                                 │
│   ├─ ALTER TABLE tb_device_channel ADD COLUMN ...                      │
│   └─ KEY idx_device_status (device_id, status)                         │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.3 不变量（红线 + 🆕 并发红线）

| 编号 | 不变量 | 类别 | 说明 |
|------|--------|------|------|
| **R1** | 终态优先 | 数据一致性 | `cascadeOffline` 写 OFFLINE 不带单调条件——离线是终态，必须落地 |
| **R2** | 自愈字段单调 | 数据一致性 | `patchChannelStatus`/`batchUpsertWithStatus` 在写 ONLINE 时，附 `lastSeenTime` 单调 WHERE（旧时间戳不覆盖新状态） |
| **R3** | 一次事务覆盖一次目录 | 数据一致性 | `batchUpsertWithStatus` 与 `markMissingChannels` 必须同事务——目录响应是"快照"语义 |
| **🆕 R4** | `Lifecycle.Online` 不覆盖 OFFLINE 终态 | 并发安全 | `patchLiveness(ONLINE)` 必须携带当前时间戳，且在 DB 层加 `AND (status ≠ OFFLINE OR status IS NULL)` 保护条件（修正 C3） |
| **🆕 R5** | 一次 SELECT 复用 | 性能优化 | `batchUpsertWithStatus` 内部 SELECT 只执行一次，结果复用给分流、单调比较、失踪 diff |
| **🆕 R6** | 锁粒度分离 | 并发安全 | 单条操作 = 通道粒度锁；批量操作 = 设备粒度锁；两者不交叉竞争（修正 C1） |
| **🆕 R7** | 大设备级联分批 | 性能保护 | 级联离线（通道数 > `cascade-offline-batch-size`）分批提交，不产生单一长事务 |

---
