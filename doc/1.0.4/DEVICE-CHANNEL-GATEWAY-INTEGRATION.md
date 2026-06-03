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

## 4. 并发安全设计（新增）

### 4.1 锁策略（R6）

| 操作类型 | 锁粒度 | Key 形式 | 超时 | 冲突动作 |
|----------|--------|----------|------|----------|
| 单通道操作（`add`/`update`/`deleteOne`/`patchChannelStatus`） | 通道粒度 | `device:channel:lock:{deviceId}:{channelId}` | 5s | 抛 `ServiceException("系统繁忙")` |
| 批量操作（`batchUpsertWithStatus`/`cascadeOffline`/`markMissingChannels`） | 设备粒度 | `device:channel:lock:{deviceId}` | 10s | **丢弃并记录日志**（不阻塞，下次 catalog 补偿） |

**为何批量用"丢弃"而非"阻塞"**：
1. catalog 事件本身是幂等可重试的——丢弃一次不影响正确性
2. 阻塞会让 `@Async("sipNotifierExecutor")` 线程池积压，引发雪崩
3. 单设备 catalog 频率有限（注册时 1 次 + REST 触发），并发锁竞争概率极低（< 1%）

**为何两种锁不交叉**：单条操作锁带 `channelId`，批量锁只到 `deviceId`，**两者 key 不同**，理论上不会相互阻塞。但实际语义上仍是冲突的——单条操作命中的行可能被批量 UPDATE 覆盖。这通过 **R5 + R2 单调条件**在数据层兜底：批量 UPDATE 的 `lastSeenTime` 单调条件会让"过期"的单条更新被挡下。

### 4.2 兜底重试策略（C2 修正）

`DuplicateKeyException` 兜底**不再用 SELECT + updateById**，而是直接条件 UPDATE，避免 N+1：

```java
catch (DuplicateKeyException dup) {
    log.warn("batchUpsertWithStatus 命中 UNIQUE 冲突 - deviceId={}, size={}", deviceId, toInsert.size());
    metrics.duplicateFallback.increment();
    // 抖动 50-200ms，错开多节点同时重试
    try { Thread.sleep(50 + ThreadLocalRandom.current().nextInt(150)); } 
    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return affected; }
    for (DeviceChannelDO ins : toInsert) {
        LambdaUpdateWrapper<DeviceChannelDO> uw = new LambdaUpdateWrapper<DeviceChannelDO>()
            .eq(DeviceChannelDO::getDeviceId, ins.getDeviceId())
            .eq(DeviceChannelDO::getChannelId, ins.getChannelId())
            .set(DeviceChannelDO::getStatus, ins.getStatus())
            .set(DeviceChannelDO::getLastSeenTime, ins.getLastSeenTime())
            .set(DeviceChannelDO::getStatusSource, ins.getStatusSource())
            .set(DeviceChannelDO::getName, ins.getName())
            .set(DeviceChannelDO::getExtend, ins.getExtend())
            .set(DeviceChannelDO::getUpdateTime, LocalDateTime.now());
        boolean updated = deviceChannelService.update(null, uw);
        if (!updated) {
            // 极端场景：另一节点又删了这行 → 重新尝试 INSERT
            try { deviceChannelService.save(ins); } 
            catch (DuplicateKeyException ignored) { /* 再次冲突即放弃，等下次 catalog */ }
        }
    }
}
```

### 4.3 终态保护条件（C3/R4 修正）

`DeviceManager.patchLiveness` 内部 `LambdaUpdateWrapper` 补充终态保护：

```java
// DeviceManager.patchLiveness 修正
LambdaUpdateWrapper<DeviceDO> uw = new LambdaUpdateWrapper<>();
uw.eq(DeviceDO::getDeviceId, deviceId)
  .set(status != null, DeviceDO::getStatus, status)
  .set(keepaliveTime != null, DeviceDO::getKeepaliveTime, keepaliveTime)
  .set(DeviceDO::getUpdateTime, LocalDateTime.now())
  // 原有：keepaliveTime 单调条件
  .and(keepaliveTime != null, w -> w
      .isNull(DeviceDO::getKeepaliveTime)
      .or().lt(DeviceDO::getKeepaliveTime, keepaliveTime))
  // 🔴 R4 新增：当目标是 ONLINE 时，加"非终态"保护
  .and(status != null && DeviceConstant.Status.ONLINE.equals(status),
       w -> w.ne(DeviceDO::getStatus, DeviceConstant.Status.OFFLINE)
              .or().isNull(DeviceDO::getStatus));
```

`Gb28181ProtocolHandler` 的 `Lifecycle.Online` 分支同步修正：
```java
case "Lifecycle.Online":
    // 🔴 C3 修正：携带时间戳，使 patchLiveness 内部的单调条件 + 终态保护生效
    deviceManager.patchLiveness(event.deviceId(), DeviceConstant.Status.ONLINE, LocalDateTime.now());
    if (deviceNodeRouteService != null) {
        deviceNodeRouteService.renewDevice(event.deviceId());
    }
    log.info("设备上线, deviceId={}", event.deviceId());
    break;
```

**注意 R1 不受影响**：`patchOfflineTerminal` 走完全独立的 LambdaUpdateWrapper，无任何 WHERE 条件，终态强写不被 R4 影响。

### 4.4 SELECT 复用（R5）

`batchUpsertWithStatus` 入口处一次 SELECT，复用给三个逻辑：

```java
// 一次查出整个设备的 snapshot，select 必要字段裁剪
LambdaQueryWrapper<DeviceChannelDO> qw = new LambdaQueryWrapper<>();
qw.eq(DeviceChannelDO::getDeviceId, deviceId)
  .select(DeviceChannelDO::getId, DeviceChannelDO::getChannelId,
          DeviceChannelDO::getLastSeenTime, DeviceChannelDO::getStatus);
Map<String, DeviceChannelDO> snapshot = deviceChannelService.list(qw).stream()
    .collect(Collectors.toMap(DeviceChannelDO::getChannelId, d -> d, (a, b) -> a));

// 用途 1（Stage 1 分流）：判断 toInsert / toUpdate
// 用途 2（Stage 3 单调比较）：跳过 lastSeenTime 比 snapshot 旧的 DTO
// 用途 3（Stage 4 失踪 diff）：snapshot.keySet() - presentChannelIds = missing
```

---

## Stage 0：Schema 与缓存基础设施（前置）

> 🔴 **修订说明**：原方案 Stage 6 的内容前置为 Stage 0，因为所有其他 Stage 都依赖新增列 `last_seen_time / status_source / missing_count` 和 `DeviceChannelCacheKey`。

### 0.1 Schema 演进

**MySQL `sql/voglander.sql`**：
```sql
ALTER TABLE tb_device_channel
  ADD COLUMN last_seen_time   datetime    NULL COMMENT '通道最近一次被目录/会话感知的时间',
  ADD COLUMN status_source    varchar(32) NULL COMMENT '当前 status 的来源：CATALOG / OFFLINE_CASCADE / SESSION / MANUAL / MISSING',
  ADD COLUMN missing_count    int         NOT NULL DEFAULT 0 COMMENT '连续目录响应中未出现的次数（Stage 4）';

-- 复合索引：cascadeOffline + 列表过滤 + Stage 3 单调查询共用
ALTER TABLE tb_device_channel ADD KEY idx_device_status (device_id, status);
```

**SQLite `voglander-web/src/test/resources/schema-sqlite.sql`**：同步加列（SQLite ADD COLUMN 仅支持简单默认值；纯 NULL + INTEGER DEFAULT 0 即可）+ 索引。

**既有 `test-app.db`**：手动 `ALTER TABLE` 一次性升级（参考 1.0.2 sqlite3 操作约定）。

### 0.2 DTO / DO 加字段

```java
// DeviceChannelDO 加 3 列
private LocalDateTime lastSeenTime;
private String        statusSource;
private Integer       missingCount;

// DeviceChannelDTO 同步加（保留与 DO 1:1）
private LocalDateTime lastSeenTime;
private String        statusSource;
private Integer       missingCount;
```

**G-CH6 旧静态转换处理**：`DeviceChannelDTO.convertDO/convertDTO/req2dto` 加 `@Deprecated` 注解，指向 `DeviceChannelAssembler`。本期不强删（兼容外部调用），1.0.5 再下线。新增字段也同步加到静态方法的映射逻辑里（保持新旧路径一致）。

### 0.3 `DeviceChannelCacheKey`（仿 `DeviceCacheKey`）

新增 `voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/cache/DeviceChannelCacheKey.java`：

```java
public final class DeviceChannelCacheKey {
    /** Spring Cache 命名空间：单对象缓存 */
    public static final String CACHE_NAME      = "deviceChannel";
    /** Spring Cache 命名空间：列表缓存（独立 TTL=60s） */
    public static final String LIST_CACHE_NAME = "deviceChannel:list";

    private static final String ID_PREFIX  = "id:";
    private static final String BIZ_PREFIX = "biz:";
    private static final String DEV_PREFIX = "device:";

    private DeviceChannelCacheKey() {}

    public static String byId(Long id) {
        return ID_PREFIX + id;
    }
    public static String byBizKey(String deviceId, String channelId) {
        return BIZ_PREFIX + deviceId + ":" + channelId;
    }
    public static String byDevice(String deviceId) {
        return DEV_PREFIX + deviceId;
    }
}
```

### 0.4 `RedisConfig` 配置独立 TTL

**核验结论**：1.0.3 `DeviceCacheKey` 走的是 Spring `@Cacheable` + `RedisCacheManager.defaultTTL`，**没有 per-cache-name 配置**。要给 `deviceChannel:list` 配 60s 独立 TTL，**必须**在 `RedisConfig` 显式注册：

```java
// RedisConfig.java 修订
@Bean
public RedisCacheManager redisCacheManager(RedisConnectionFactory cf) {
    RedisCacheConfiguration defaultCfg = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(3))  // 默认 3min（与 DeviceCacheKey 一致）
        .serializeValuesWith(/* FastJSON2 */);

    Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
    // 列表缓存独立 60s TTL（频繁失效，不能用 3min）
    perCache.put(DeviceChannelCacheKey.LIST_CACHE_NAME,
                 defaultCfg.entryTtl(Duration.ofSeconds(60)));

    return RedisCacheManager.builder(cf)
        .cacheDefaults(defaultCfg)
        .withInitialCacheConfigurations(perCache)
        .build();
}
```

### 0.5 缓存清理 API 改造

`DeviceChannelManager` 替换缓存清理路径：

```java
// 废弃：private static final String DEVICE_CHANNEL_CACHE_PREFIX = ...
// 废弃：private static final String DEVICE_CHANNEL_LIST_CACHE_KEY = ...
// 废弃：private void clearCache(Long id, String oldKey, String newKey)

/** 精确 evict 单通道缓存（用于 add/update/deleteOne 等单条操作） */
private void clearCacheByChannel(Long id, String deviceId, String channelId) {
    try {
        Optional.ofNullable(cacheManager.getCache(DeviceChannelCacheKey.CACHE_NAME))
            .ifPresent(cache -> {
                if (id != null) cache.evict(DeviceChannelCacheKey.byId(id));
                if (deviceId != null && channelId != null) {
                    cache.evict(DeviceChannelCacheKey.byBizKey(deviceId, channelId));
                }
            });
        // 单条改动同步 evict 该设备的列表缓存
        if (deviceId != null) {
            Optional.ofNullable(cacheManager.getCache(DeviceChannelCacheKey.LIST_CACHE_NAME))
                .ifPresent(cache -> cache.evict(DeviceChannelCacheKey.byDevice(deviceId)));
        }
    } catch (Exception e) {
        log.warn("clearCacheByChannel 异常 - id={}, deviceId={}, channelId={}: {}",
                 id, deviceId, channelId, e.getMessage());
    }
}

/** 精确 evict 整设备列表缓存（用于批量操作） */
private void clearCacheByDevice(String deviceId) {
    try {
        Optional.ofNullable(cacheManager.getCache(DeviceChannelCacheKey.LIST_CACHE_NAME))
            .ifPresent(cache -> cache.evict(DeviceChannelCacheKey.byDevice(deviceId)));
        // 注意：不连坐清所有单对象缓存，避免雪崩；单对象走 60s 列表 TTL 自然过期
    } catch (Exception e) {
        log.warn("clearCacheByDevice 异常 - deviceId={}: {}", deviceId, e.getMessage());
    }
}
```

**🔴 C4 修正**：`batchCreateDeviceChannel` 不再循环触发缓存清理：
```java
@Transactional(rollbackFor = Exception.class)
public int batchCreateDeviceChannel(List<DeviceChannelDTO> dtoList) {
    // ... 循环 createDeviceChannel（内部走 deviceChannelInternal）
    // 🔴 修正：循环内不调 clearCache；事务外统一调
    int successCount = ...;
    // 按 deviceId 分组，每个 deviceId 调一次 clearCacheByDevice
    dtoList.stream().map(DeviceChannelDTO::getDeviceId).distinct()
           .forEach(this::clearCacheByDevice);
    return successCount;
}
```

同时 `deviceChannelInternal`（L336-376）也要改：**不在事务内调缓存清理**，改为返回后由调用方统一处理（参考 1.0.3 `DeviceManager.clearCache` 的位置）。

### 0.6 验收

| 测试 | 期望 |
|------|------|
| Schema migration 后字段查询 | `last_seen_time / status_source / missing_count` 列存在 |
| `DeviceChannelCacheKeyTest` | `byId(1L)="id:1"`, `byBizKey("d","c")="biz:d:c"`, `byDevice("d")="device:d"` |
| `RedisCacheConfigTest` | `cacheManager.getCache("deviceChannel:list")` 的 TTL = 60s |

---

## Stage 1：通道在线态从 catalog 落库

### 1.1 目标

让 `Response.Catalog` 响应里 `DeviceItem.status`（"ON"/"OFF"）显式落到 `tb_device_channel.status`，并填充 `lastSeenTime / statusSource` 元数据。

### 1.2 改动点

**a) `Gb28181ProtocolHandler.handleCatalog` 改造**

```java
private void handleCatalog(DeviceEvent event) {
    DeviceResponse catalog = toEntity(event.payload(), DeviceResponse.class);
    if (catalog == null || catalog.getDeviceItemList() == null || catalog.getDeviceItemList().isEmpty()) {
        log.info("目录响应为空, deviceId={}", event.deviceId());
        return;
    }
    LocalDateTime now = LocalDateTime.now();
    List<DeviceItem> items = catalog.getDeviceItemList();
    List<DeviceChannelDTO> channels = new ArrayList<>(items.size());
    for (DeviceItem item : items) {
        if (item == null || item.getDeviceId() == null) continue;
        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setDeviceId(event.deviceId());
        dto.setChannelId(item.getDeviceId());
        dto.setName(item.getName());
        dto.setStatus(mapItemStatus(item.getStatus()));    // "ON"→1, "OFF"→0, null→null（不覆盖）
        dto.setLastSeenTime(now);
        dto.setStatusSource("CATALOG");
        DeviceChannelDTO.ExtendInfo ext = new DeviceChannelDTO.ExtendInfo();
        ext.setChannelInfo(JSON.toJSONString(item));
        dto.setExtendInfo(ext);
        channels.add(dto);
    }
    if (!channels.isEmpty()) {
        // 🔴 R6：传 deviceId，方法内部加设备粒度锁
        deviceChannelManager.batchUpsertWithStatus(event.deviceId(), channels);
    }
    log.info("目录响应处理完成, deviceId={}, 通道数={}", event.deviceId(), channels.size());
}

/** "ON"→1, "OFF"→0, 其他/null→null（保持原值不覆盖） */
private Integer mapItemStatus(String s) {
    if (s == null) return null;
    String up = s.trim().toUpperCase();
    if ("ON".equals(up) || "ONLINE".equals(up)) return DeviceConstant.Status.ONLINE;
    if ("OFF".equals(up) || "OFFLINE".equals(up)) return DeviceConstant.Status.OFFLINE;
    return null;
}
```

**b) `DeviceChannelManager.batchUpsertWithStatus`（核心方法，遵循 R5/R6/R3）**

```java
/**
 * 目录响应批量幂等 upsert，带 status / lastSeenTime / statusSource 显式落地。
 *
 * 不变量：
 *  R3 同一 deviceId 下的所有 channel 一次事务内落库。
 *  R5 一次 SELECT snapshot 复用给：分流 / 单调比较 / 失踪 diff（Stage 4）。
 *  R6 入口加设备粒度锁，锁竞争丢弃（不阻塞）。
 *  R2 status 单调：仅当 dto.lastSeenTime > snapshot.lastSeenTime 时才覆盖；null 时保留原值。
 *  C2 跨节点 UNIQUE 冲突走条件 UPDATE 兜底（不退化为 N+1）。
 *
 * @return 实际处理的有效记录数（新增 + 更新）；锁竞争或异常返回 0
 */
public int batchUpsertWithStatus(String deviceId, List<DeviceChannelDTO> dtoList) {
    if (dtoList == null || dtoList.isEmpty() || deviceId == null) return 0;

    // 🔴 R6：设备粒度锁
    String lockKey = DEVICE_CHANNEL_LOCK_PREFIX + deviceId;
    if (!redisLockUtil.tryLock(lockKey, 10)) {
        log.warn("batchUpsertWithStatus 锁竞争，丢弃本次 catalog - deviceId={}, size={}", 
                 deviceId, dtoList.size());
        // metrics.batchUpsertLockSkip.increment();
        return 0;
    }
    try {
        return doBatchUpsertWithStatus(deviceId, dtoList);
    } finally {
        redisLockUtil.unLock(lockKey);
    }
}

@Transactional(rollbackFor = Exception.class)
protected int doBatchUpsertWithStatus(String deviceId, List<DeviceChannelDTO> dtoList) {
    // 同批内去重（保留后者）
    Map<String, DeviceChannelDTO> dedup = new LinkedHashMap<>();
    for (DeviceChannelDTO dto : dtoList) {
        if (dto == null || dto.getChannelId() == null) continue;
        dedup.put(dto.getChannelId(), dto);
    }
    if (dedup.isEmpty()) return 0;

    // 🔴 R5：一次 SELECT snapshot
    LambdaQueryWrapper<DeviceChannelDO> qw = new LambdaQueryWrapper<>();
    qw.eq(DeviceChannelDO::getDeviceId, deviceId)
      .select(DeviceChannelDO::getId, DeviceChannelDO::getChannelId,
              DeviceChannelDO::getLastSeenTime, DeviceChannelDO::getStatus,
              DeviceChannelDO::getMissingCount);
    Map<String, DeviceChannelDO> snapshot = deviceChannelService.list(qw).stream()
        .collect(Collectors.toMap(DeviceChannelDO::getChannelId, d -> d, (a, b) -> a));

    List<DeviceChannelDO> toInsert = new ArrayList<>();
    List<DeviceChannelDO> toUpdate = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    for (Map.Entry<String, DeviceChannelDTO> e : dedup.entrySet()) {
        DeviceChannelDTO dto = e.getValue();
        DeviceChannelDO exist = snapshot.get(e.getKey());
        DeviceChannelDO target = deviceChannelAssembler.dtoToDo(dto);
        target.setUpdateTime(now);

        if (exist != null) {
            // 🔴 R2 单调判定：dto.lastSeenTime 比 snapshot 旧 → 跳过 status 字段更新（但 name/extend 仍可更新）
            if (dto.getLastSeenTime() != null && exist.getLastSeenTime() != null
                && dto.getLastSeenTime().isBefore(exist.getLastSeenTime())) {
                target.setStatus(null);          // 不覆盖 status
                target.setLastSeenTime(null);    // 不覆盖时间戳
                target.setStatusSource(null);
            }
            // 通道再次出现 → missing_count 归零
            target.setMissingCount(0);
            target.setId(exist.getId());
            toUpdate.add(target);
        } else {
            target.setCreateTime(now);
            target.setMissingCount(0);
            toInsert.add(target);
        }
    }

    int affected = 0;
    if (!toUpdate.isEmpty()) {
        deviceChannelService.updateBatchById(toUpdate);
        affected += toUpdate.size();
    }
    if (!toInsert.isEmpty()) {
        try {
            deviceChannelService.saveBatch(toInsert);
            affected += toInsert.size();
        } catch (DuplicateKeyException dup) {
            // 🔴 C2 修正：条件 UPDATE 兜底 + 抖动
            affected += duplicateKeyFallback(toInsert);
        }
    }

    // 🔴 Stage 4 入口：失踪扫描（同事务 R3）
    if (missingScanEnabled) {
        markMissingChannelsInternal(deviceId, dedup.keySet(), snapshot, now);
    }

    // 🔴 R6：事务结束后一次性清缓存（C4）
    clearCacheByDevice(deviceId);
    log.info("batchUpsertWithStatus 完成 - deviceId={}, 新增={}, 更新={}, 合计={}",
             deviceId, toInsert.size(), toUpdate.size(), affected);
    return affected;
}

/** C2：条件 UPDATE 兜底，不退化为 N+1 SELECT */
private int duplicateKeyFallback(List<DeviceChannelDO> toInsert) {
    log.warn("batchUpsertWithStatus UNIQUE 冲突 - size={}", toInsert.size());
    // metrics.duplicateFallback.increment();
    try { Thread.sleep(50 + ThreadLocalRandom.current().nextInt(150)); }
    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return 0; }

    int n = 0;
    for (DeviceChannelDO ins : toInsert) {
        LambdaUpdateWrapper<DeviceChannelDO> uw = new LambdaUpdateWrapper<DeviceChannelDO>()
            .eq(DeviceChannelDO::getDeviceId, ins.getDeviceId())
            .eq(DeviceChannelDO::getChannelId, ins.getChannelId())
            .set(ins.getStatus() != null, DeviceChannelDO::getStatus, ins.getStatus())
            .set(ins.getLastSeenTime() != null, DeviceChannelDO::getLastSeenTime, ins.getLastSeenTime())
            .set(ins.getStatusSource() != null, DeviceChannelDO::getStatusSource, ins.getStatusSource())
            .set(ins.getName() != null, DeviceChannelDO::getName, ins.getName())
            .set(ins.getExtend() != null, DeviceChannelDO::getExtend, ins.getExtend())
            .set(DeviceChannelDO::getMissingCount, 0)
            .set(DeviceChannelDO::getUpdateTime, LocalDateTime.now());
        if (deviceChannelService.update(null, uw)) {
            n++;
        } else {
            // 极端：另一节点又删了 → 重新尝试 INSERT，再冲突就放弃
            try { deviceChannelService.save(ins); n++; }
            catch (DuplicateKeyException ignored) { 
                log.warn("二次冲突，放弃 - deviceId={}, channelId={}", ins.getDeviceId(), ins.getChannelId());
            }
        }
    }
    return n;
}
```

### 1.3 验收（RED → GREEN）

| 测试用例 | 期望 |
|----------|------|
| `Gb28181ProtocolHandlerCatalogStatusTest#shouldMapOnToOnlineStatus` | DeviceItem.status="ON" → DB row.status=1 |
| `#shouldMapOffToOfflineStatus` | "OFF" → 0 |
| `#shouldKeepExistingStatusWhenItemStatusNull` | item.status=null → DB 原值保留 |
| `#shouldFillLastSeenTimeFromEventTimestamp` | last_seen_time 写入 |
| `DeviceChannelManagerBatchUpsertWithStatusTest#shouldRollbackAllOnPartialFailure` | 同 deviceId 一次事务内全部成功或全部回滚（R3） |
| `#shouldOnlySelectOnceForLargeCatalog` | mock 验证：1000 通道 catalog 只触发一次 SELECT（R5） |
| `#shouldFallbackWithConditionalUpdateOnDuplicateKey` | 模拟 DuplicateKeyException → 走条件 UPDATE 兜底，不调 getByDeviceId（C2） |
| `#shouldSkipWhenLockHeldByPeer` | mock `redisLockUtil.tryLock` 返回 false → 方法返回 0 且不抛异常（R6） |

### 1.4 性能验收

- 1 设备 1000 通道 batchUpsertWithStatus：**< 300ms**（基线：原 batchUpsert 100 通道 ≈ 200ms；R5 减少 1 次 SELECT，字段裁剪后传输量降低）
- 1000 冲突的兜底路径：**< 500ms**（含 150ms 抖动）

---

## Stage 2：设备离线级联通道下线

### 2.1 目标

解决 **G-CH2**：`Lifecycle.Offline` 触发时，把该设备所有 channel 写 OFFLINE。
🆕 **R7**：超大通道数设备分批 UPDATE，防止单一长事务锁定大量行。

### 2.2 改动点

**a) `DeviceChannelManager.cascadeOffline(String deviceId)`**

```java
@Value("${voglander.device-channel.cascade-offline-batch-size:500}")
private int cascadeOfflineBatchSize;

/**
 * 设备级离线级联：把该设备下所有 channel 写 OFFLINE（终态，无单调条件 R1）。
 *
 *  R1 无单调条件，强写 OFFLINE
 *  R6 设备粒度锁（与 batchUpsertWithStatus 共用同一锁，互斥）
 *  R7 超过 batch-size 时分批 UPDATE，每批独立事务
 *
 * @return 累计更新行数
 */
public int cascadeOffline(String deviceId) {
    Assert.hasText(deviceId, "设备ID不能为空");
    String lockKey = DEVICE_CHANNEL_LOCK_PREFIX + deviceId;
    if (!redisLockUtil.tryLock(lockKey, 10)) {
        log.warn("cascadeOffline 锁竞争，丢弃 - deviceId={}", deviceId);
        return 0;
    }
    try {
        // 先 count 决定是否分批
        long total = deviceChannelService.count(
            new LambdaQueryWrapper<DeviceChannelDO>()
                .eq(DeviceChannelDO::getDeviceId, deviceId)
                .ne(DeviceChannelDO::getStatus, DeviceConstant.Status.OFFLINE));
        if (total == 0) return 0;

        int totalAffected = 0;
        if (total <= cascadeOfflineBatchSize) {
            // 单批：一次 UPDATE
            totalAffected = doCascadeOfflineSingleBatch(deviceId);
        } else {
            // 🔴 R7 分批：按 id 范围分页 UPDATE，每批独立事务
            totalAffected = doCascadeOfflineMultiBatch(deviceId);
        }
        if (totalAffected > 0) {
            clearCacheByDevice(deviceId);
            log.info("cascadeOffline 完成 - deviceId={}, 行数={}", deviceId, totalAffected);
            // metrics.cascadeOfflineRows.record(totalAffected);
        }
        return totalAffected;
    } finally {
        redisLockUtil.unLock(lockKey);
    }
}

@Transactional(rollbackFor = Exception.class)
protected int doCascadeOfflineSingleBatch(String deviceId) {
    LambdaUpdateWrapper<DeviceChannelDO> uw = new LambdaUpdateWrapper<>();
    uw.eq(DeviceChannelDO::getDeviceId, deviceId)
      .ne(DeviceChannelDO::getStatus, DeviceConstant.Status.OFFLINE)  // 只更新非 OFFLINE
      .set(DeviceChannelDO::getStatus, DeviceConstant.Status.OFFLINE)
      .set(DeviceChannelDO::getStatusSource, "OFFLINE_CASCADE")
      .set(DeviceChannelDO::getUpdateTime, LocalDateTime.now());
    return deviceChannelService.getBaseMapper().update(null, uw);
}

protected int doCascadeOfflineMultiBatch(String deviceId) {
    int total = 0;
    Long lastId = 0L;
    while (true) {
        // 每批独立事务：通过 Spring TransactionTemplate 或 self-invoke 走代理
        int rows = doCascadeOfflineBatch(deviceId, lastId, cascadeOfflineBatchSize);
        if (rows == 0) break;
        total += rows;
        // 注意：分批查询需在每批返回最后一行 id（这里简化为 ORDER BY id LIMIT，需要 mapper 增加方法）
        lastId = getMaxIdAfter(deviceId, lastId, cascadeOfflineBatchSize);
        if (lastId == null) break;
    }
    return total;
}

@Transactional(rollbackFor = Exception.class)
protected int doCascadeOfflineBatch(String deviceId, Long lastId, int batchSize) {
    // 实际实现：自定义 mapper 方法 UPDATE ... WHERE device_id=? AND id > ? AND status != 0 LIMIT ?
    // 或：先 SELECT id LIMIT batchSize → UPDATE ... WHERE id IN (...)
    // 前者更高效，但需 mapper.xml；后者纯 LambdaUpdateWrapper 可达
    List<Long> ids = deviceChannelService.listObjs(
        new LambdaQueryWrapper<DeviceChannelDO>()
            .select(DeviceChannelDO::getId)
            .eq(DeviceChannelDO::getDeviceId, deviceId)
            .gt(DeviceChannelDO::getId, lastId)
            .ne(DeviceChannelDO::getStatus, DeviceConstant.Status.OFFLINE)
            .orderByAsc(DeviceChannelDO::getId)
            .last("LIMIT " + batchSize),
        o -> (Long) o);
    if (ids.isEmpty()) return 0;
    LambdaUpdateWrapper<DeviceChannelDO> uw = new LambdaUpdateWrapper<DeviceChannelDO>()
        .in(DeviceChannelDO::getId, ids)
        .set(DeviceChannelDO::getStatus, DeviceConstant.Status.OFFLINE)
        .set(DeviceChannelDO::getStatusSource, "OFFLINE_CASCADE")
        .set(DeviceChannelDO::getUpdateTime, LocalDateTime.now());
    return deviceChannelService.getBaseMapper().update(null, uw);
}
```

**b) `Gb28181ProtocolHandler` Offline 分支**

```java
case "Lifecycle.Offline":
    deviceRegisterService.offline(event.deviceId());
    deviceChannelManager.cascadeOffline(event.deviceId());   // ★ Stage 2
    log.info("设备离线 + 通道级联下线, deviceId={}", event.deviceId());
    break;
```

> **为何不放进 `deviceRegisterService.offline`**：保持调用链可观测——通道侧改动集中在 `Gb28181ProtocolHandler`（协议侧）与 `DeviceChannelManager`（数据侧），`DeviceRegisterService` 始终是设备维度的语义。两层调用都 idempotent，调用顺序无关紧要。

### 2.3 配置项

```yaml
voglander:
  device-channel:
    cascade-offline-batch-size: 500       # 超过此通道数走分批 UPDATE（R7）
```

### 2.4 验收

| 测试 | 期望 |
|------|------|
| `Gb28181ProtocolHandlerOfflineCascadeTest#shouldOfflineAllChannelsOfDevice` | 1 设备 + 3 通道 → Offline 后 3 通道全部 status=0 |
| `#shouldNotAffectOtherDeviceChannels` | 其他设备的通道不变 |
| `DeviceChannelManagerCascadeOfflineTest#shouldBeIdempotent` | 重复调用不抛错，第二次行数=0 |
| `#shouldNotApplyMonotonicGuard` | 即使 lastSeenTime 在未来，也强写 OFFLINE（R1） |
| `#shouldBatchWhenChannelsExceedThreshold` | 配 batch-size=100，造 250 通道 → 分 3 批 UPDATE（R7） |
| `#shouldSkipWhenLockHeldByBatchUpsert` | mock 锁占用 → 返回 0 且不抛（R6） |

### 2.5 性能验收

- 单批 1000 通道（< batch-size）：**< 50ms**
- 分批 2000 通道（batch-size=500）：**< 200ms**（4 批 × ~50ms）

---

## Stage 3：通道单调写 + 心跳跟随

### 3.1 目标

为通道引入与设备同源的单调写约束（R2），并把 `lastSeenTime` 作为"通道侧的 keepaliveTime"。
🆕 **C3 修正**：`Lifecycle.Online` 改为携带时间戳调 `patchLiveness`，使设备侧单调条件 + R4 终态保护生效。

### 3.2 改动点

**a) `DeviceChannelManager.patchChannelStatus`（替代旧 `updateStatus`）**

```java
/**
 * 通道在线态轻量定向更新（不读整行、不整行 UPDATE）。
 *
 *  R2 lastSeenTime 单调条件：仅当传入时间戳 > 库内时才更新
 *  R6 通道粒度锁
 *
 * @return 是否实际更新（false=不存在或被单调条件挡下）
 */
public boolean patchChannelStatus(String deviceId, String channelId,
                                  Integer status, LocalDateTime lastSeenTime, String source) {
    Assert.hasText(deviceId, "设备ID不能为空");
    Assert.hasText(channelId, "通道ID不能为空");
    String lockKey = DEVICE_CHANNEL_LOCK_PREFIX + deviceId + ":" + channelId;
    if (!redisLockUtil.tryLock(lockKey, 5)) {
        throw new ServiceException("系统繁忙，请稍后重试");
    }
    try {
        LambdaUpdateWrapper<DeviceChannelDO> uw = new LambdaUpdateWrapper<>();
        uw.eq(DeviceChannelDO::getDeviceId, deviceId)
          .eq(DeviceChannelDO::getChannelId, channelId)
          .set(status != null, DeviceChannelDO::getStatus, status)
          .set(lastSeenTime != null, DeviceChannelDO::getLastSeenTime, lastSeenTime)
          .set(source != null, DeviceChannelDO::getStatusSource, source)
          .set(DeviceChannelDO::getUpdateTime, LocalDateTime.now())
          // 🔴 R2：lastSeenTime 单调条件
          .and(lastSeenTime != null, w -> w
              .isNull(DeviceChannelDO::getLastSeenTime)
              .or().lt(DeviceChannelDO::getLastSeenTime, lastSeenTime));
        boolean updated = deviceChannelService.update(null, uw);
        if (updated) {
            // 需先查 id 才能精确 evict；此处直接调 byBizKey + byDevice
            clearCacheByChannel(null, deviceId, channelId);
        } else {
            // metrics.monotonicBlocked.increment();
            log.debug("patchChannelStatus 未更新（不存在或单调挡下）- deviceId={}, channelId={}", 
                      deviceId, channelId);
        }
        return updated;
    } finally {
        redisLockUtil.unLock(lockKey);
    }
}
```

**b) 旧 `updateStatus` 标 `@Deprecated`**

```java
/**
 * @deprecated 改用 {@link #patchChannelStatus(String, String, Integer, LocalDateTime, String)}
 */
@Deprecated
@Transactional(rollbackFor = Exception.class)
public void updateStatus(String deviceId, String channelId, int status) {
    patchChannelStatus(deviceId, channelId, status, null, "MANUAL");
}
```

**c) 🔴 C3 修正：`Gb28181ProtocolHandler.Lifecycle.Online`**

```java
case "Lifecycle.Online":
    // 🔴 C3：携带时间戳，使设备侧 patchLiveness 的单调条件 + R4 终态保护生效
    deviceManager.patchLiveness(event.deviceId(), DeviceConstant.Status.ONLINE, LocalDateTime.now());
    if (deviceNodeRouteService != null) {
        deviceNodeRouteService.renewDevice(event.deviceId());
    }
    log.info("设备上线, deviceId={}", event.deviceId());
    break;
```

**d) 🔴 R4：`DeviceManager.patchLiveness` 内部加终态保护**

参见 §4.3 完整代码。关键点：当 `status == ONLINE` 时，附加 `AND (status ≠ OFFLINE OR status IS NULL)` 防乱序复活。

### 3.3 与 Stage 2 的非对称性

- `cascadeOffline`：**终态**，无单调条件（R1）
- `patchChannelStatus`/`batchUpsertWithStatus`：**自愈态**，带单调条件（R2）

两者放一起，跨节点 UDP 乱序场景下：旧的 ONLINE 包到来时不会"复活"已经离线的通道（被单调挡下），新的 OFFLINE 永远能落地。

### 3.4 验收

| 测试 | 期望 |
|------|------|
| `DeviceChannelManagerMonotonicTest#shouldRejectOlderLastSeenTime` | 先写 t=100 → 再写 t=50 → status 保留 t=100 时的值 |
| `#shouldAcceptNewerLastSeenTime` | 先 t=100 → 后 t=200 → 覆盖成功 |
| `#cascadeOfflineShouldBypassMonotonic` | 即使 lastSeenTime=未来时间，cascadeOffline 仍写 OFFLINE |
| `DeviceManagerLivenessR4Test#shouldRejectOnlineAfterOfflineTerminal` | patchOfflineTerminal → patchLiveness(ONLINE, now) → status 保持 OFFLINE（R4） |
| `Gb28181ProtocolHandlerLifecycleOnlineTest#shouldPassTimestampToPatchLiveness` | mock 验证：handler 传入 patchLiveness 的第 3 参数非 null（C3） |

---

## Stage 4：通道发现/失踪与软删除

### 4.1 目标

解决 **G-CH7**：catalog 重传时把"本次目录未出现的旧通道"判定为"失踪"，连续多次失踪后软下线（不物理删除，便于回溯）。

🔴 **重要修订**：由于当前 catalog 是"注册时拉一次 + REST 手动触发"（非周期轮询），原方案 `missing-threshold: 3` 在实践中**几乎永远不会触发**。修正为：
- 默认 `missing-threshold: 1`（单次缺失即下线）
- 默认 `enable-missing-scan: false`（关闭开关，确认 catalog 全量触发后再灰度开启）

### 4.2 改动点

**a) `DeviceChannelManager.markMissingChannelsInternal`（私有，被 `doBatchUpsertWithStatus` 同事务调用）**

```java
@Value("${voglander.device-channel.missing-threshold:1}")
private int     missingThreshold;

@Value("${voglander.device-channel.enable-missing-scan:false}")
private boolean missingScanEnabled;

/**
 * 同事务标记本次目录响应未出现的通道（R3 不脱离 doBatchUpsertWithStatus 事务）。
 *
 * 复用 snapshot（R5），不再额外 SELECT。
 * 用 IN UPDATE（缺失通道 id 列表），避免 NOT IN 大列表问题。
 *
 * 阈值策略：
 *   missing_count + 1 >= threshold → status=0, statusSource="MISSING"
 *   否则只 +1
 */
private int markMissingChannelsInternal(String deviceId, Set<String> presentChannelIds,
                                         Map<String, DeviceChannelDO> snapshot, LocalDateTime now) {
    if (!missingScanEnabled || snapshot.isEmpty()) return 0;

    // Java Set diff：snapshot 中存在但本次目录未出现的通道
    List<Long> missingIds = snapshot.entrySet().stream()
        .filter(e -> !presentChannelIds.contains(e.getKey()))
        .map(e -> e.getValue().getId())
        .collect(Collectors.toList());

    if (missingIds.isEmpty()) return 0;

    // 🔴 安全保护：SQLite SQLITE_LIMIT_VARIABLE_NUMBER=999，MySQL 大 IN 失索引 → 分批 IN
    int totalAffected = 0;
    int batchSize = 500;  // 与 cascade-offline-batch-size 对齐
    for (int i = 0; i < missingIds.size(); i += batchSize) {
        List<Long> batch = missingIds.subList(i, Math.min(i + batchSize, missingIds.size()));
        totalAffected += updateMissingBatch(batch, now);
    }
    log.info("markMissingChannels - deviceId={}, missing={}, affected={}", 
              deviceId, missingIds.size(), totalAffected);
    return totalAffected;
}

private int updateMissingBatch(List<Long> ids, LocalDateTime now) {
    // 用 SQL CASE WHEN 实现"+1 且达阈值则置 OFFLINE"
    // MyBatis-Plus 的 LambdaUpdateWrapper 不直接支持 CASE WHEN
    // → 拆为两步：先批量 +1，再查出达到阈值的 id 写 OFFLINE
    
    // 步骤 1：missing_count += 1
    LambdaUpdateWrapper<DeviceChannelDO> incrWrapper = new LambdaUpdateWrapper<DeviceChannelDO>()
        .in(DeviceChannelDO::getId, ids)
        .setSql("missing_count = missing_count + 1")
        .set(DeviceChannelDO::getUpdateTime, now);
    int affected = deviceChannelService.getBaseMapper().update(null, incrWrapper);

    // 步骤 2：missing_count >= threshold 的写 OFFLINE
    LambdaUpdateWrapper<DeviceChannelDO> offlineWrapper = new LambdaUpdateWrapper<DeviceChannelDO>()
        .in(DeviceChannelDO::getId, ids)
        .ge(DeviceChannelDO::getMissingCount, missingThreshold)
        .ne(DeviceChannelDO::getStatus, DeviceConstant.Status.OFFLINE)  // 仅非 OFFLINE 才改
        .set(DeviceChannelDO::getStatus, DeviceConstant.Status.OFFLINE)
        .set(DeviceChannelDO::getStatusSource, "MISSING")
        .set(DeviceChannelDO::getUpdateTime, now);
    deviceChannelService.getBaseMapper().update(null, offlineWrapper);
    return affected;
}
```

**b) `doBatchUpsertWithStatus` 已在 Stage 1 的 §1.2(b) 末尾自动调用 `markMissingChannelsInternal`**

**c) 通道再次出现时 `missing_count` 归零**：在 Stage 1 §1.2(b) 的分流逻辑里，`toUpdate.add(target)` 前已显式 `target.setMissingCount(0)`，保证通道再次出现时计数器重置。

### 4.3 配置项

```yaml
voglander:
  device-channel:
    missing-threshold: 1          # 单次目录缺失即软下线（前提：catalog 是全量快照）
    enable-missing-scan: false    # 🔴 默认关闭，灰度开启
```

**为何默认关闭**：1.0.4 上线初期 catalog 触发逻辑不稳定（注册拉 + 手动 REST），可能出现"未列出 ≠ 真的失踪"误判。生产环境确认 catalog 是稳定全量快照后再灰度开启。

### 4.4 验收

| 测试 | 期望 |
|------|------|
| `DeviceChannelMissingScanTest#shouldNotRunWhenDisabled` | `enable-missing-scan=false` 时，markMissing 内部直接 return 0 |
| `#shouldSoftOfflineWhenThresholdReached` | `threshold=1` + 通道首次缺失 → status=0, statusSource="MISSING" |
| `#shouldIncrementMissingCountWithoutOfflineWhenThresholdNotReached` | `threshold=3`, 通道首次缺失 → missing_count=1, status 不变 |
| `#shouldResetMissingCountOnReappearance` | 失踪 1 次后再出现 → missing_count=0 |
| `#shouldHandleLargeMissingListWithBatching` | 1000 缺失通道 → 分 2 批（batch=500） |
| `#shouldShareSnapshotWithBatchUpsert` | mock 验证：markMissing 不额外 SELECT（R5 验证） |

---

## Stage 5：媒体会话挂钩通道状态

### 5.1 目标

媒体会话建立（`Session.InviteOk`）本身就是"通道必然能出流"的强证据。若 DB 中该通道仍标 OFFLINE，应升 ONLINE。

🔴 **重要修订**：Stage 5 实施前**强制进行 payload 验证**，三条路径根据验证结果二选一/三选一。

### 5.2 实施前置验证（Go/No-Go 门槛）

**Step 1：观察 payload 结构**

在 `Gb28181ProtocolHandler.handle` 的 `Session.InviteOk` 分支临时加日志：

```java
case "Session.InviteOk":
    // 🔴 Stage 5 pre-check：观察 payload 结构
    log.info("[Stage 5 pre-check] InviteOk payload keys={}, payload={}", 
             event.payload() != null ? event.payload().keySet() : null, event.payload());
    mediaSessionManager.onInviteOk(event.correlationId(), event.deviceId());
    break;
```

**Step 2：触发实际点播/回放，看日志**

**Step 3：根据 payload 内容选路径**

| payload 内容 | 选择路径 | 说明 |
|--------------|----------|------|
| **A：含 `channelId` 或 `channel`** | 直接实施 §5.3 完整代码 | 最直接 |
| **B：不含但可从 `MediaSessionManager` 扩展** | 在 `tb_media_session` 加 `channel_id` 列；`onInviteOk` 写入；`promoteOnlineIfOffline` 从 DO 读 | 需改 schema |
| **C：完全无法关联** | Stage 5 降级为"仅打日志"，验收矩阵移除 Stage 5 测试用例 | 留 1.0.5 |

**🔴 验收门槛**：本 Stage 仅在情况 A 或 B 完成时纳入正式上线；情况 C 时 §11 验收矩阵移除 Stage 5 行。

### 5.3 改动点（情况 A 实施代码）

**a) `DeviceChannelManager.promoteOnlineIfOffline`**

```java
/**
 * 仅当通道当前 OFFLINE 时升 ONLINE（条件 UPDATE）。
 *
 *  R2 同时带 lastSeenTime 单调条件
 *  R6 通道粒度锁
 *
 * @return 是否实际更新（false = 本来就 ONLINE 或不存在）
 */
public boolean promoteOnlineIfOffline(String deviceId, String channelId, LocalDateTime now) {
    Assert.hasText(deviceId, "设备ID不能为空");
    Assert.hasText(channelId, "通道ID不能为空");
    Assert.notNull(now, "时间戳不能为空");
    String lockKey = DEVICE_CHANNEL_LOCK_PREFIX + deviceId + ":" + channelId;
    if (!redisLockUtil.tryLock(lockKey, 5)) return false;
    try {
        LambdaUpdateWrapper<DeviceChannelDO> uw = new LambdaUpdateWrapper<DeviceChannelDO>()
            .eq(DeviceChannelDO::getDeviceId, deviceId)
            .eq(DeviceChannelDO::getChannelId, channelId)
            .eq(DeviceChannelDO::getStatus, DeviceConstant.Status.OFFLINE)   // 仅 OFFLINE → ONLINE
            .set(DeviceChannelDO::getStatus, DeviceConstant.Status.ONLINE)
            .set(DeviceChannelDO::getLastSeenTime, now)
            .set(DeviceChannelDO::getStatusSource, "SESSION")
            .set(DeviceChannelDO::getUpdateTime, now)
            // R2 单调条件
            .and(w -> w.isNull(DeviceChannelDO::getLastSeenTime)
                       .or().lt(DeviceChannelDO::getLastSeenTime, now));
        boolean updated = deviceChannelService.update(null, uw);
        if (updated) {
            clearCacheByChannel(null, deviceId, channelId);
            // metrics.sessionPromote.increment();
        }
        return updated;
    } finally {
        redisLockUtil.unLock(lockKey);
    }
}
```

**b) `Gb28181ProtocolHandler.Session.InviteOk` 分支（情况 A）**

```java
case "Session.InviteOk":
    mediaSessionManager.onInviteOk(event.correlationId(), event.deviceId());
    String channelId = stringValue(event.payload() != null ? event.payload().get("channelId") : null);
    if (channelId != null && event.deviceId() != null) {
        boolean promoted = deviceChannelManager.promoteOnlineIfOffline(
            event.deviceId(), channelId, LocalDateTime.now());
        if (promoted) {
            log.info("会话建立提升通道在线 - deviceId={}, channelId={}, callId={}",
                     event.deviceId(), channelId, event.correlationId());
        }
    }
    log.info("会话建立, callId={}, deviceId={}", event.correlationId(), event.deviceId());
    break;
```

### 5.4 不做的事

- `Session.Bye` 不写通道 OFFLINE：流断只代表"本次会话结束"，不代表通道掉线。OFFLINE 的权威信号是 `Lifecycle.Offline` 或下一次 Catalog 返回 "OFF"。

### 5.5 验收（仅情况 A/B 适用）

| 测试 | 期望 |
|------|------|
| `Gb28181ProtocolHandlerSessionPromotionTest#shouldPromoteOfflineChannelOnInviteOk` | 通道 status=0 → InviteOk → status=1 |
| `#shouldNotChangeAlreadyOnlineChannel` | 通道 status=1 → InviteOk → 无 UPDATE 行 |
| `#shouldNotOfflineOnBye` | Session.Bye 不写通道状态 |
| `#shouldSkipWhenChannelIdMissing` | payload 无 channelId → 不调 promoteOnlineIfOffline，不抛 |

---

## 11. 验收门槛 & TDD 测试矩阵

### 11.1 测试集中位置（项目硬约定）

所有新测试放 `voglander-web/src/test/java/io/github/lunasaw/voglander/`，分两层：

| 层级 | 测试类型 | 注解 | 范围 |
|------|----------|------|------|
| `Gb28181ProtocolHandler*Test` | 集成（`@SpringBootTest + BaseTest`） | 真实 Bean + `@MockitoBean` for `DeviceRegisterService` | 入站事件 → DB 状态闭环 |
| `DeviceChannelManager*Test` | 集成（`@SpringBootTest + BaseTest`） | `@Autowired DeviceChannelService` 真实 DB | Manager 模板 + 单调条件 + 级联 |
| `DeviceManagerLivenessR4Test` | 集成 | 真实 DB | C3/R4 验证 |
| `RedisCacheConfigTest` | 集成 | `@Autowired CacheManager` | Stage 0 TTL 配置 |

### 11.2 端到端验收（按 Stage 顺序，RED → GREEN）

| Stage | RED 验收用例 | 期望 GREEN 行为 |
|-------|--------------|------------------|
| 0 | Schema migration 后字段查询 | `last_seen_time / status_source / missing_count` 列存在 |
| 0 | `cacheManager.getCache("deviceChannel:list")` TTL | = 60s |
| 1 | catalog 含 status="ON" 的 item → DB.status=0 | DB.status=1 |
| 1 | catalog 同 item 第二次到达且 lastSeenTime 更早 | 单调挡下，status 不变（R2） |
| 1 | mock 锁占用 | batchUpsertWithStatus 返回 0 不抛（R6） |
| 1 | mock DuplicateKeyException | 走条件 UPDATE 兜底，不调 getByDeviceId（C2） |
| 1 | 1000 通道 catalog | 只触发 1 次 SELECT（R5） |
| 2 | 设备 Offline 后通道仍 status=1 | 全部 status=0（含 statusSource="OFFLINE_CASCADE"） |
| 2 | cascadeOffline 当 lastSeenTime 在未来 | 仍写 OFFLINE（R1） |
| 2 | 2000 通道 + batch=500 | 分 4 批 UPDATE（R7） |
| 3 | 旧时间戳 ONLINE 抵达 | 单调挡下（R2） |
| 3 | patchOfflineTerminal 后 patchLiveness(ONLINE, now) | status 保持 OFFLINE（R4） |
| 3 | Gb28181ProtocolHandler.Lifecycle.Online | 传 patchLiveness 第 3 参数非 null（C3） |
| 4 | enable-missing-scan=false | markMissing 无副作用 |
| 4 | enable-missing-scan=true + threshold=1 + 通道首次缺失 | status=0, statusSource="MISSING" |
| 4 | 通道再次出现 | missing_count=0 |
| 5 (A) | InviteOk + 通道 OFFLINE | 升 ONLINE，statusSource="SESSION" |
| 5 (A) | Bye | 通道状态不变 |

### 11.3 性能验收

| 场景 | 指标 | 修正后基线 |
|------|------|-----------|
| 1 设备 1000 通道 batchUpsertWithStatus | < 300ms | R5 + 字段裁剪 |
| 1000 冲突的 DuplicateKey 兜底 | < 500ms | 含 150ms 抖动（C2） |
| 单批 1000 通道 cascadeOffline | < 50ms | 命中 idx_device_status |
| 分批 2000 通道 cascadeOffline（batch=500）| < 200ms | 4 批 × ~50ms（R7） |
| 锁竞争 catalog 丢弃率 | < 1% | 监控指标（R6） |

### 11.4 不引入回归

- `mvn clean test -pl voglander-web` 全绿
- `MediaSessionManagerTest` / `DeviceManagerLivenessTest` / `DeviceManagerCacheTest` 不受影响
- 1.0.3 已有 `Gb28181ProtocolHandlerTest` / `InboundEventDispatcherTest` / `VoglanderBusinessNotifierTranslatorTest` 全绿
- 🔴 新增：`DeviceChannelBatchUpsertTest`（覆盖 C1/C2 修正）全绿

---

## 12. 监控埋点（生产保障，新增）

下表为本期上线必备的 5 个监控指标。建议接入 SkyWalking Meter API 或 `MeterRegistry`（Micrometer）。

| 指标名 | 类型 | 触发位置 | 告警阈值 | 含义 |
|--------|------|----------|----------|------|
| `voglander.channel.batchUpsert.lock_skip` | Counter | `batchUpsertWithStatus` 锁竞争丢弃 | > 1% / 5min | C1 修正后丢弃率，过高说明锁粒度过粗或事件抖动 |
| `voglander.channel.batchUpsert.duplicate_fallback` | Counter | `duplicateKeyFallback` 触发 | > 100/h | C2 兜底次数，过高说明跨节点并发严重 |
| `voglander.channel.cascadeOffline.rows` | Distribution | `cascadeOffline` 返回 | P95 > 1000 | R7 级联行数分布，超过说明应调小 batch-size |
| `voglander.channel.status_source.dist` | Counter (labeled) | 任何写 status 的方法 | — | label = CATALOG/OFFLINE_CASCADE/SESSION/MISSING/MANUAL，检测信号来源偏移 |
| `voglander.device.patchLiveness.monotonic_blocked` | Counter | `patchLiveness` 单调条件挡下（updated=false 且 status=ONLINE） | > 50/min | R4 阻止乱序 Online 的次数，高频说明上游网络抖动严重 |

**埋点代码示例**：

```java
// 在 DeviceChannelManager 注入
@Autowired
private MeterRegistry meterRegistry;

// batchUpsertWithStatus 锁竞争分支
if (!redisLockUtil.tryLock(lockKey, 10)) {
    meterRegistry.counter("voglander.channel.batchUpsert.lock_skip", 
                          "deviceId", deviceId).increment();
    return 0;
}

// duplicateKeyFallback 入口
private int duplicateKeyFallback(List<DeviceChannelDO> toInsert) {
    meterRegistry.counter("voglander.channel.batchUpsert.duplicate_fallback").increment();
    // ...
}

// cascadeOffline 完成
meterRegistry.summary("voglander.channel.cascadeOffline.rows").record(totalAffected);

// 任何写 status 的方法（建议封装到 statusSource 设置时）
meterRegistry.counter("voglander.channel.status_source.dist",
                      "source", statusSource).increment();
```

---

## 13. 回滚与灰度

### 13.1 灰度开关

```yaml
voglander:
  device-channel:
    enable-status-from-catalog: true    # Stage 1 总开关；关时回到现行行为
    enable-offline-cascade: true        # Stage 2 总开关；关时 Offline 仅动设备
    enable-session-promotion: false     # Stage 5 总开关；默认关，验证 payload 后开
    enable-missing-scan: false          # Stage 4 总开关；默认关，确认 catalog 稳定后开
    missing-threshold: 1
    cascade-offline-batch-size: 500
```

- Stage 1/2 默认开（核心修正）；Stage 4/5 默认关（依赖验证）
- 开关位置：`Gb28181ProtocolHandler` 与 `DeviceChannelManager` 顶层方法入口，用 `@Value` 注入
- 任何 Stage 异常 → 关对应开关回退到 1.0.3 行为

### 13.2 回滚路径

| 风险场景 | 触发条件 | 回滚动作 |
|----------|----------|----------|
| `cascadeOffline` 在大量通道库下打爆 DB | DBA 告警 / 慢 SQL | 关 `enable-offline-cascade`；或调小 `cascade-offline-batch-size` |
| Stage 4 误判失踪导致大面积通道下线 | 用户反馈"通道无故离线" | 关 `enable-missing-scan` |
| 单调条件挡下合法状态变更 | 监控 `monotonic_blocked` 频繁告警 | 走 REST 调 `patchChannelStatus(status, lastSeenTime=now)` 强推 |
| 锁竞争过高 | 监控 `batchUpsert.lock_skip > 1%` | 增大锁超时（10s → 30s），或检查 `ShardDispatcher` 是否同 deviceId 路由到同分片 |
| R4 阻止合法 Online | 设备实际在线但 DB 显示 OFFLINE | 走 REST 主动调 `deviceManager.patchOfflineTerminal` 清终态再 patchLiveness |

### 13.3 Schema 回滚

新增列均为 nullable + 默认值，**前向兼容旧代码**。回滚老 jar 不需要 DROP COLUMN。

### 13.4 上线检查清单

- [ ] `sql/voglander.sql` ALTER 已在生产库执行（先于 jar 部署）
- [ ] `schema-sqlite.sql` + `test-app.db` 同步
- [ ] `RedisConfig` 已注册 `LIST_CACHE_NAME` TTL=60s
- [ ] 配置文件 `voglander.device-channel.*` 默认值已 review（Stage 4/5 默认关）
- [ ] 单测全绿 + 覆盖率不降
- [ ] 5 个监控指标已上 SkyWalking
- [ ] Stage 5 payload 验证已完成（Go/No-Go 决议有 owner 签字）
- [ ] 灰度开关默认值已 review，留有关闭演练记录
- [ ] DBA 已为 `idx_device_status` 建索引

---

## 附录 A：与 1.0.3 文档的关系

- 1.0.3 Phase 4 已完成"目录 N+1 → batchUpsert"。本期是其**语义补全 + 并发加固**：升级为"批量幂等 + 状态权威 + 失踪扫描 + 锁保护 + 终态防护"
- 1.0.3 Phase 2a 在设备侧定下"单调写 + 终态强写"模板。本期把它复制到通道侧，并**反向加固设备侧**（R4 终态保护）
- 1.0.3 Phase 1 在 `DeviceManager` 完成"DeviceCacheKey 单一来源 + 精确 evict + 延迟双删"。本期在通道侧补齐
- 1.0.3 Phase 5/6/7 仍为未完成项，与本期正交，不互相阻塞

## 附录 B：未覆盖项（留待后续）

1. **通道维度的可观测**：channel-level 在线率、平均 lastSeenTime 间隔、missing_count 分布。建议接入 SkyWalking（1.0.3 Phase 8 未完成项）
2. **通道审计流水**：当前 `status_source` 仅记当前状态来源，无变更历史。可在 1.0.5 引入 `tb_device_channel_audit`
3. **主动目录订阅（SIP SUBSCRIBE）**：当前仍是注册时拉一次。订阅周期化需要 sip-gateway 1.8.0+ 的 catalog subscribe 命令暴露——属 1.0.5
4. **`MediaSessionDO.channelId` 字段**：Stage 5 情况 B 路径依赖此扩展，1.0.5 视 payload 验证结果决定
5. **G-CH8 入站 catalog 部分失败补偿**：当前 `handleCatalog` 单次 try-catch，payload 异常即整批丢失。1.0.5 可引入"逐条解析 + 失败计数 + 死信队列"

## 附录 C：本期修正点对照表

| 修正编号 | 来源 | 对应 Stage | 修正点 |
|----------|------|-----------|--------|
| C1 | 并发审查 | Stage 1/2 | `batchUpsert` 加设备粒度锁，竞争丢弃 |
| C2 | 并发审查 | Stage 1 | DuplicateKey 兜底改条件 UPDATE，不退化 N+1 |
| C3 | 并发审查 | Stage 3 | `Lifecycle.Online` 携带时间戳，`patchLiveness` 加 R4 保护 |
| C4 | 并发审查 | Stage 0 | `batchCreate` 不循环触发 `clearCache`，事务外一次清 |
| R4 | 不变量补充 | Stage 3 | `patchLiveness(ONLINE)` 加终态保护条件 |
| R5 | 性能修正 | Stage 1/3/4 | snapshot SELECT 单次复用 |
| R6 | 不变量补充 | Stage 1/2/3/5 | 锁粒度分离（单条=通道，批量=设备） |
| R7 | 不变量补充 | Stage 2/4 | 大设备分批 UPDATE |
| Stage 顺序 | 依赖分析 | 全局 | 原 6→1→2→3→4→5 改为 0(Schema)→1+2→3→4→5 |
| `missing-threshold` 默认值 | 实际场景 | Stage 4 | 3→1，且 `enable-missing-scan` 默认关 |
| `LIST_CACHE_NAME` TTL | 配置遗漏 | Stage 0 | 必须在 `RedisConfig` 显式注册 60s |
