# Voglander Manager & Repository TDD 测试方案

> 版本：1.0.3 ｜ 编写日期：2026-06-02 ｜ 范围：`voglander-manager` + `voglander-repository`
> 配套架构：参见 [ARCHITECTURE.md](./ARCHITECTURE.md)

---

## 目录

1. [目标与范围](#1-目标与范围)
2. [测试分层策略](#2-测试分层策略)
3. [基础设施与约定](#3-基础设施与约定)
4. [现状盘点与差距分析](#4-现状盘点与差距分析)
5. [测试矩阵（必做）](#5-测试矩阵必做)
6. [TDD 节奏与 PR 切分](#6-tdd-节奏与-pr-切分)
7. [关键测试用例设计](#7-关键测试用例设计)
8. [测试反模式与红线](#8-测试反模式与红线)
9. [验收标准与覆盖率门槛](#9-验收标准与覆盖率门槛)

---

## 1. 目标与范围

### 1.1 目标

为 `voglander-manager` 和 `voglander-repository` 两个核心模块建立**完整、可复跑、可信**的 TDD 测试网，覆盖：

- **业务正确性**：Manager 模板方法（add/update/get/delete/getPage）+ 全部业务方法行为。
- **缓存一致性**：`@Cached` AOP 命中/失效 / Manager 双键清理 / `DelayedCacheEviction` 延迟双删。
- **并发安全**：`MediaSessionManager.onInviteOk` / `DeviceChannelManager.batchUpsert` 跨节点 UNIQUE 冲突幂等。
- **状态机正确**：`DeviceManager.patchLiveness` 单调条件、`MediaSessionManager` 五态转换。
- **事件管线**：`ShardDispatcher` 哈希分布 + 同 key 串行 + `InboundEventDispatcher` 协议路由 + `NoopProtocolHandler` 可插拔。
- **持久化契约**：`AbstractMapper.insertBatch` 行为、所有 UNIQUE 约束、默认值。

### 1.2 范围边界

| 包含 | 不包含 |
|------|--------|
| `voglander-manager/**` 全部 main 源码 | Web 层 Controller（另立 plan） |
| `voglander-repository/**` 全部 main 源码 | Integration 模块（已有 envelope/handler 测试） |
| 现有测试的**补缺**与**改造** | sip-gateway 框架内部 |

---

## 2. 测试分层策略

严格遵循 [CLAUDE.md](../../CLAUDE.md) §测试策略，**不允许**跨层混用注解：

| 层 | 测试类型 | 注解 | 依赖 | 事务 | 数据库 |
|----|----------|------|------|------|--------|
| **Mapper / Entity 约束** | 集成 | `@SpringBootTest` + `BaseTest` | 真实 SQLite | `@Transactional` | `test-app.db` |
| **Cache 切面 / Redis 工具** | 集成（条件跳过） | `@SpringBootTest` + `BaseTest` + `Assumptions.assumeTrue(redisUp)` | 真实 Redis | 无（缓存不走事务） | 真实 Redis |
| **Manager（业务编排）** | 集成 | `@SpringBootTest` + `BaseTest` | 真实 `IService<DO>` + `@MockitoBean` Assembler | `@Transactional` | SQLite |
| **Event 分片组件** | 纯单元（推荐） | `@ExtendWith(MockitoExtension.class)` 或裸 JUnit5 | 手工 new + `@Mock` | 无 | 无 |
| **Assembler / 纯转换** | 纯单元 | `@ExtendWith(MockitoExtension.class)` | 直接 new | 无 | 无 |
| **AsyncManager / Spring 辅助** | 集成 | `@SpringBootTest` + `BaseTest` | 真实 Bean | 无（异步无法回滚） | 视场景 |
| **跨线程并发场景** | 集成 | `@SpringBootTest`（**不加** `@Transactional`） | 真实 Bean | **无**（手动清理） | SQLite |

### 2.1 选型决策树

```
被测对象有 Spring 注入？
├── 否 → 纯 Mockito 单元测试
└── 是
    ├── 涉及缓存读写验证？
    │   └── 是 → BaseTest + CacheTestConfig（动态 ConcurrentMapCacheManager 替代固定名 sip-common 默认）
    ├── 涉及 Redis 操作（RedisLockUtil/DelayedCacheEviction/RedisCache）？
    │   └── 是 → BaseTest + Assumptions.assumeTrue(redisAvailable)，本地无 Redis 自动跳过
    ├── 涉及多线程 / @Async / Hook 回调？
    │   └── 是 → BaseTest + 不加 @Transactional + @BeforeEach/@AfterEach 手动清理 + 唯一键加线程后缀
    └── 默认 → BaseTest + @Transactional
```

---

## 3. 基础设施与约定

### 3.1 既有基础设施（直接复用）

| 组件 | 路径 | 作用 |
|------|------|------|
| `BaseTest` | `voglander-web/src/test/java/io/github/lunasaw/voglander/BaseTest.java` | `@SpringBootTest` + `@ActiveProfiles("test")` + `@Import(CacheTestConfig.class)` + `@Transactional` |
| `CacheTestConfig` | 同上 `config/CacheTestConfig.java` | **关键基础**：用 `@Primary` 动态 `ConcurrentMapCacheManager` 覆盖 sip-common 的固定名 CacheManager（否则 `device`/`mediaNode`/`streamProxy` 缓存区在测试中恒为 null） |
| `schema-sqlite.sql` | `voglander-web/src/test/resources/` | 测试库 DDL，含全部 14 张表 UNIQUE/默认值 |
| `test-app.db` | 工程根/工作目录 | SQLite 测试库（已提交） |

### 3.2 新增基础设施（按需）

**必须新建的工具类**（在 `voglander-web/src/test/java/io/github/lunasaw/voglander/support/`）：

| 工具 | 职责 | 用法 |
|------|------|------|
| `RedisAvailableExtension` | JUnit5 扩展，启动时 `ping` Redis，失败则 `Assumptions.assumeTrue(false)` | `@ExtendWith(RedisAvailableExtension.class)` |
| `TestDataCleaner` | 跨线程测试结束后清表（按白名单），避免 `@Transactional` 缺失留脏 | `@AfterEach cleaner.cleanAll()` |
| `UniqueKeyFactory` | 生成 `prefix + timestamp + threadIndex` 唯一键，并发测试隔离 | `UniqueKeyFactory.deviceId("dev")` |
| `CacheInspector` | 包装 `CacheManager`，提供 `get(name, key)` / `entryCount(name)` / `assertHit(name, key)` 等便利方法 | Manager 缓存语义验证 |
| `EventEmitter` | 构造 `DeviceEvent` 的 builder，统一测试数据 | 事件分片场景 |

### 3.3 命名与位置约定

```
voglander-web/src/test/java/io/github/lunasaw/voglander/
├── BaseTest.java                              # 已存在
├── config/CacheTestConfig.java                # 已存在
├── support/                                   # ★ 新增工具集
│   ├── RedisAvailableExtension.java
│   ├── TestDataCleaner.java
│   ├── UniqueKeyFactory.java
│   ├── CacheInspector.java
│   └── EventEmitter.java
├── manager/                                   # Manager 层测试
│   ├── assembler/  *AssemblerTest             # 纯单元
│   ├── cache/      DelayedCacheEvictionTest（已有，补强）
│   ├── event/      *Test（已有 5 个，补强）
│   ├── manager/    *ManagerTest（已有 7 个，补全 12 个 Manager）
│   ├── async/      AsyncManagerTest
│   └── spring/     SpringUtilsTest, SpringDynamicTaskTest
└── repository/                                # ★ Repository 层测试（当前几乎空白）
    ├── entity/     SchemaConstraintTest       # UNIQUE / NOT NULL / DEFAULT 验证
    ├── mapper/     *MapperTest                # 14 个 Mapper
    ├── cache/      CachedAspectTest / RedisCacheTest / RedisLockUtilTest / LocalCacheBaseTest
    ├── manager/    BizUniqueManagerTest / ConcurrentProcessHelperTest
    └── tair/       TairManagerTest
```

### 3.4 命名规则

- 测试类：`{被测类}Test`；并发场景加后缀 `ConcurrentTest`；缓存场景加 `CacheTest`；状态机加 `StateMachineTest`。
- 测试方法：`should_预期行为_when_前置条件`（中文 @DisplayName）。
- 一个测试**只断言一件事**；多场景用 `@Nested` 分组或 `@ParameterizedTest`。

---

## 4. 现状盘点与差距分析

### 4.1 现有测试清单（共 18 个测试类与 manager/repository 相关）

| 已有 | 覆盖点 | 评价 |
|------|--------|------|
| `MediaSessionManagerTest` | MediaSession CRUD + 事件方法 | ✅ 完整 |
| `MediaSessionConcurrentUpsertTest` | UNIQUE 冲突跨节点幂等 | ✅ 关键 |
| `DeviceManagerCacheTest` | DeviceManager 缓存读写/失效 | ✅ 已有 |
| `DeviceManagerLivenessTest` | `patchLiveness` 单调条件 | ✅ 已有 |
| `DeviceLivenessCoalescerTest` | 心跳合并 30s 窗口 | ✅ Phase 2a |
| `DeviceOfflineCoalesceCacheTest` | 离线终态写入 + 缓存 | ✅ 已有 |
| `DeviceChannelBatchUpsertTest` | batchUpsert UNIQUE 兜底 | ✅ Phase 4 |
| `DelayedCacheEvictionTest` | 延迟双删 schedule + drain | ✅ Phase 1 |
| `ShardDispatcherTest` / `EventShardTest` / `InboundEventDispatcherTest` / `NoopProtocolHandlerPluggableTest` | 事件分片管线 | ✅ Phase 3/4/8 |
| `RedisConfigCacheTtlTest` | Redis 配置 TTL | ⚠️ 浅 |

### 4.2 差距清单（按优先级）

#### 🔴 P0 - 必须补齐（核心业务路径未覆盖）

| 类 | 缺失测试 | 风险 |
|----|----------|------|
| `MediaNodeManager` | `saveOrUpdateNodeStatus`（ZLM Hook 心跳唯一入口）、`updateNodeOffline`、按状态查询 4 法 | ZLM 节点心跳错乱不可观测 |
| `StreamProxyManager` | `createStreamProxy` 默认值、`updateStreamProxyOnlineStatus`、`updateStreamProxyKey`、`deleteByProxyKey`、Hook 回调写状态 | 流代理状态错乱 |
| `PushProxyManager` | 同上 | 同上 |
| `ExportTaskManager` | 任务状态机（pending→running→success/failed）、过期清理 | 导出任务卡死 |
| `DeviceChannelManager` | 模板方法 6 件 + `updateStatus` + `saveOrUpdate` 单条 | 通道增删错乱 |
| `DeviceConfigManager` | 模板方法 + 配置项 upsert（device_id+config_key UNIQUE） | 配置覆盖 / 重复键 |
| `DeviceManager` | 模板方法 add/update/get/deleteOne/deleteBatch/getPage（现仅覆盖 liveness 与缓存） | 标准 CRUD 路径无回归网 |
| `MediaSessionManager` | `getPage` 默认 createTime 降序、`get` 多条件、`deleteBatch` 边界 | 查询走偏 |
| `AbstractMapper` | `insertBatch` 逐条插入行为、`lambdaWrapper`/`updateWrapper`/`selectOneEq` 等 | 基类静默回归 |
| `CachedAspect` | 注解读 → 命中跳过执行、写 → 落 Redis、null 不缓存、超过 maxValueSize 拒缓存 | 缓存穿透 / 膨胀 |
| `RedisLockUtil` / `RedisLockUtils` | 加锁/重入/过期/Lua 解锁防误删 | 分布式锁失效不可知 |
| `BizUniqueManager` | check 命中抛异常、createUniqueRecord 幂等、TTL 失效 | 幂等机制失效 |
| `ConcurrentProcessHelper` | 同一 bizKey 二次进入直接返回、回调失败不写幂等键 | 重复处理 |

#### 🟡 P1 - 建议补齐（增强信心）

| 类 | 缺失测试 |
|----|----------|
| 全部 Assembler（10+ 个） | DTO↔DO 双向幂等性 + 边界（null/空集合/extend JSON）|
| `LocalCacheBase` | 容量上限驱逐 + TTL 过期 |
| `TairManager` | 三级（local→redis→db）查询路径、未命中 DB 回写 |
| `RedisCache` | `setCacheObjectIfAbsent` 互斥、`executeLuaScript` 原子性 |
| `UserManager` / `RoleManager` / `MenuManager` / `DeptManager` | RBAC 模板方法 + 关联表（user_role / role_menu） |
| `MediaNodeServiceImpl` | 节点权重排序 / 负载选择 |
| `ApplicationWebSchedulingTest` | 已有但需确认 `@Scheduled` 调度覆盖 |

#### 🟢 P2 - 可选（覆盖率冲分）

- DTO 类 getter/setter（用 `@Data` 自动生成，覆盖率工具可豁免）。
- 常量/枚举类。

---

## 5. 测试矩阵（必做）

### 5.1 Repository 层

#### A. Mapper（14 张表）

每个 Mapper 必须有 `*MapperTest`，**集成测试 + `@Transactional`**：

| 测试用例 | 期望 |
|----------|------|
| `insert_应填充自增id` | 成功插入，DO.id 非 null |
| `insert_应填充默认值字段` | 例：StreamProxyDO.enabled=true、MediaSessionDO.status=2、MediaNodeDO.weight=100 |
| `insert_命中UNIQUE约束应抛异常` | DataIntegrityViolationException / DuplicateKeyException |
| `selectById_存在` | 返回非空 DO |
| `selectById_不存在` | 返回 null |
| `selectOne_按业务键` | 唯一字段查询命中 |
| `update_按主键_部分字段` | 验证 LambdaUpdateWrapper 只更新指定列 |
| `update_乐观锁/version` | 如使用 @Version（若有） |
| `delete_按主键` | 影响行 1 |
| `deleteByMap_批量条件` | 影响行 N |
| `pageQuery_默认排序` | createTime 降序 |
| `insertBatch（AbstractMapper）` | 100 条数据全部成功；遇到中间 UNIQUE 冲突的行为（当前是逐条，应记录并断言） |

#### B. Cache 切面与工具

**`CachedAspectTest`**（最关键，目前空白）：

```
@Nested 单对象缓存
  ├─ 首次调用_查DB并写缓存
  ├─ 二次调用_命中缓存_不查DB（用 Mockito.verify(mapper, never()).selectById）
  ├─ 返回null且cacheNull=false_不写缓存
  ├─ 返回null且cacheNull=true_写NULL占位
  ├─ 超过maxValueSize_拒绝写入
  └─ 缓存反序列化_FastJSON2_类型一致

@Nested 集合缓存（Cache-Through）
  ├─ 部分命中部分未命中_合并DB回写
  ├─ 全命中_零DB调用
  └─ 全未命中_全DB回写

@Nested key生成
  ├─ keyParamIndex指定参数
  ├─ keyNameInReturnObject多字段拼接
  └─ key前缀隔离不同方法
```

**`RedisLockUtilTest`** + **`RedisLockUtilsTest`**（带 `@ExtendWith(RedisAvailableExtension.class)`）：

- 同 key 二次加锁互斥（线程 B 应失败或阻塞）
- 过期自动释放
- `unLock` 仅释放本人锁（Lua 脚本校验 value，伪造 value 解锁失败）
- 异常路径不死锁（持锁线程 throw 后，锁仍按 TTL 释放）

**`DelayedCacheEvictionTest`**（已有，补强项）：

- ✅ scheduleEvict 立即 evict（已有）
- ➕ Redis 不可用时降级（不抛异常，仅日志）
- ➕ drainDue 跨节点幂等（两节点同时 drain，ZREM 原子保证只有一方拿到 member）
- ➕ member 解码：包含 `::` 的 cacheName 或 key 不应混淆（建议在 cacheName 加白名单校验）

#### C. 辅助 Manager

**`BizUniqueManagerTest`**：
- 首次 check 通过 + createUniqueRecord 写入 + TTL=86400
- 二次 check 命中抛 ServiceException（标准错误码）
- 显式 delete 后再次 check 通过

**`ConcurrentProcessHelperTest`**：
- 同 bizKey 两线程并发，只有一方回调被执行
- 回调返回 false 时，幂等键应保留还是删除？（断言当前实现行为，避免误改）
- 事务回调版本：回调内 DB 异常，事务回滚 + 幂等键不应保留（行为待确认）

#### D. Schema 约束

**`SchemaConstraintTest`**（一个测试类覆盖全部 14 表）：

```java
@DisplayName("schema-sqlite.sql 约束契约")
class SchemaConstraintTest extends BaseTest {
    @Test void tb_device_deviceId应为UNIQUE() { ... }
    @Test void tb_device_channel复合UNIQUE应生效() { ... }
    @Test void tb_stream_proxy_app_stream复合UNIQUE应生效() { ... }
    @Test void tb_media_session_callId应为UNIQUE() { ... }
    @Test void tb_media_session_status默认值应为2_INVITING() { ... }
    @Test void tb_media_node_weight默认值应为100() { ... }
    @Test void tb_push_proxy_默认字段应符合定义() { ... }
    // ...
}
```

> **目的**：DDL 漂移检测。任何人修改 schema-sqlite.sql 必须同步更新此测试，反之亦然。

### 5.2 Manager 层

#### A. 模板方法回归（12 个 Manager 各一个 `*ManagerTemplateTest`）

每个 Manager **必须**覆盖以下 6 个模板方法的核心场景：

| 用例 | 通用断言 |
|------|----------|
| `add_应落库并返回id` | DB 有记录，缓存被填充（若适用） |
| `add_必填字段缺失应抛异常` | Assert / ServiceException |
| `updateById_应只更新指定字段` | 未传字段保持原值，缓存被清理 |
| `update_条件分离应支持` | 用 queryDTO 定位 + updateDTO 改值 |
| `get_应支持LambdaQueryWrapper带条件` | 多字段组合查询 |
| `deleteOne_应清理双键缓存` | 主键 + 业务键缓存均失效 |
| `deleteBatch_条件删除应生效` | 影响行数正确 |
| `getPage_默认按createTime降序` | 第一行为最新插入 |
| `clearCache_异常不阻断主流程` | 模拟缓存清理 throw，DB 操作仍成功 |

#### B. 业务方法专项

**DeviceManager**（已有 liveness/cache，补 CRUD 模板 + 边界）：
- ✅ `patchLiveness` 单调条件（已有）
- ✅ 心跳合并窗口（已有）
- ➕ `batchCreateDevice` 部分失败回滚 / 不回滚的契约
- ➕ `updateStatus` 与 `patchLiveness` 的边界（前者粗粒度，后者带时间戳）

**MediaSessionManager**（已有 onInviteOk 并发 + 标准 CRUD，补全态转换图）：

```
状态机：
    null ──add──► INVITING(2)
    INVITING ──onInviteOk──► ACTIVE(1)
    INVITING ──onInviteFailure──► FAILED(3)
    ACTIVE ──onAck──► ACTIVE（幂等）
    ACTIVE ──onBye──► CLOSED(0)
    ACTIVE ──onMediaStatus(101)──► CLOSED
    FAILED/CLOSED ──任何事件──► 不变（终态）
```

- 必测：终态不可逆（`onInviteOk` 对已 CLOSED 的 callId 应**不**回到 ACTIVE）
- 必测：`onBye` 设备级（按 deviceId）批量关闭所有 ACTIVE 会话
- 必测：`getByCallId` 命中缓存（@Cached）

**MediaNodeManager**（关键，目前未覆盖）：
- `saveOrUpdateNodeStatus(serverId, ...)`：
  - 首次（DB 无记录）→ 新增 + `enabled=1`/`hookEnabled=1`/`weight=100`
  - 已存在 → 更新 keepalive/host/name 但保留 enabled、weight 不变
  - 并发同 serverId 两次心跳 → UNIQUE 冲突转更新（无重复行）
- `updateNodeOffline` → status=0 但 enabled 保持
- `getEnabledNodes` / `getOnlineNodes` / `getEnabledAndOnlineNodes` 三种过滤组合

**StreamProxyManager / PushProxyManager**（关键，对接 ZLM）：
- `createStreamProxy`：默认 enabled=1、status=1、online_status=0
- `(app, stream)` 复合 UNIQUE：重复创建应**先查再更新**（不抛 UNIQUE 异常）
- `updateStreamProxyOnlineStatus`：仅写 online_status 列（Phase 2a 思路）
- `deleteByProxyKey(app, stream)`：删除 + 清缓存

**DeviceChannelManager**（已有 batchUpsert，补单条）：
- ✅ `batchUpsert` UNIQUE 冲突兜底（已有）
- ➕ `saveOrUpdate` 单条 = 先查后写
- ➕ `updateStatus(deviceId, channelId, status)` 只动 status 列

#### C. Assembler（10+ 个）

```java
@DisplayName("DeviceAssembler 双向幂等")
class DeviceAssemblerTest {
    @Test void dtoToDo_then_doToDto_应等值() { ... }
    @Test void extend为null时不应抛NPE() { ... }
    @Test void extend为合法JSON时双向解析() { ... }
    @Test void extend为非法JSON时应记录但不抛() { ... }
    @Test void 列表转换_空集合_应返回空集合非null() { ... }
}
```

每个 Assembler 一个测试类，纯 JUnit5（无 Spring）。

### 5.3 Event 分片管线（补强已有）

已有测试覆盖了基本路径，**补强项**：

| 已有 | 补强 |
|------|------|
| `ShardDispatcherTest` | ➕ shardKey=null（deviceId 与 correlationId 都为 null）的安全丢弃 |
| | ➕ 优雅关闭：shutdown 后 dispatch 应丢弃 + 计数 |
| | ➕ 同 deviceId 100 条事件验证严格 FIFO 顺序 |
| `EventShardTest` | ➕ 队列满时 Keepalive 类被丢弃（白名单），其他类型阻塞或落地 |
| | ➕ 消费 handler.handle 抛异常时，下一条事件应继续处理（不卡死） |
| `InboundEventDispatcherTest` | ➕ 注册两种协议（gb28181 + noop），路由正确 |
| | ➕ 未注册协议事件 → 安全丢弃（不抛） |
| `NoopProtocolHandlerPluggableTest` | ➕ 注入 Noop 替换 Gb28181Handler，验证业务方法（Manager）零调用 |

---

## 6. TDD 节奏与 PR 切分

### 6.1 红绿重构循环

每个测试用例严格走：

```
1. RED    新增测试 → mvn test 失败（NPE / AssertionError / 编译错）
2. GREEN  最小改动让测试通过（生产代码或测试自身修正）
3. REFACTOR  在测试保护下重构，绿灯保持
4. COMMIT  原子提交：feat(test): 或 test: 前缀
```

**强制规定**：

- 一次 PR 不超过 **5 个测试类**，便于 review；
- 测试与被测代码改动**同 commit**（若被测代码已存在则纯 test commit）；
- 禁用 `--no-verify`；禁止 `@Disabled` 没有 issue 链接的测试。

### 6.2 推荐迭代顺序（按价值/风险排序）

| 迭代 | 范围 | 预计测试类数 | 价值 |
|------|------|--------------|------|
| **Iter 1** | `support/` 工具（RedisAvailableExtension、TestDataCleaner、UniqueKeyFactory、CacheInspector、EventEmitter） | 5 | 基础设施，无 RED |
| **Iter 2** | `SchemaConstraintTest`（一次性钉住 DDL 契约） | 1 | 防 schema 漂移 |
| **Iter 3** | `CachedAspectTest` + `RedisLockUtilTest` | 2 | 缓存与锁的真实验证 |
| **Iter 4** | `MediaNodeManagerTest`（saveOrUpdateNodeStatus 并发） | 1 | ZLM 心跳关键路径 |
| **Iter 5** | `StreamProxyManagerTest` + `PushProxyManagerTest` | 2 | 流代理生命周期 |
| **Iter 6** | `DeviceManagerTemplateTest` + `DeviceChannelManagerTemplateTest` 模板回归 | 2 | 标准 CRUD 网 |
| **Iter 7** | `MediaSessionStateMachineTest`（五态转换图） | 1 | 状态机完整闭环 |
| **Iter 8** | `BizUniqueManagerTest` + `ConcurrentProcessHelperTest` | 2 | 幂等基础设施 |
| **Iter 9** | 各 Assembler 双向幂等（10 个） | 10 | 转换层防退化 |
| **Iter 10** | 剩余 Mapper / Manager 模板回归 | ~15 | 全量覆盖 |
| **Iter 11** | Event 分片补强 + Cache 工具补强 | 5 | 边界场景 |
| **Iter 12** | 覆盖率门槛达成 + 报告评审 | - | 收尾 |

**总计**：约 50~60 个新增测试类（不含改造），分 12 次 PR。

---

## 7. 关键测试用例设计

### 7.1 跨节点 UNIQUE 冲突幂等（MediaNodeManager）

> 风险：ZLM 多节点同时心跳同一 serverId，UNIQUE 冲突可能抛异常，导致心跳丢失。

```java
@Test
@DisplayName("两节点并发上报同 serverId 应转更新而非抛异常")
void shouldUpsertOnDuplicate_whenConcurrentSameServerId() throws Exception {
    String serverId = UniqueKeyFactory.serverId();
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(2);
    AtomicReference<Throwable> err = new AtomicReference<>();

    Runnable task = () -> {
        try {
            start.await();
            mediaNodeManager.saveOrUpdateNodeStatus(
                serverId, "secret-" + Thread.currentThread().getId(),
                System.currentTimeMillis(), "127.0.0.1", "node");
        } catch (Throwable t) { err.set(t); }
        finally { done.countDown(); }
    };

    new Thread(task).start();
    new Thread(task).start();
    start.countDown();
    assertTrue(done.await(5, TimeUnit.SECONDS));
    assertNull(err.get(), "并发不应抛 UNIQUE 异常");

    // DB 应只有一条记录
    long count = mediaNodeMapper.selectCount(
        new LambdaQueryWrapper<MediaNodeDO>().eq(MediaNodeDO::getServerId, serverId));
    assertEquals(1, count);
}
```

**关键技术点**：
- ❌ **不能** `@Transactional`（跨线程不会回滚）
- ✅ `@AfterEach` 手动清表（按 serverId 删除）
- ✅ `UniqueKeyFactory` 加时间戳避免与历史脏数据冲突

### 7.2 缓存 AOP 命中验证（CachedAspect）

> 风险：sip-common 的固定名 CacheManager 会让缓存静默失效（详见 CacheTestConfig 注释）。

```java
@Test
@DisplayName("@Cached 注解二次调用应命中缓存不查 DB")
void shouldHitCache_whenSecondCall() {
    Long id = createDeviceAndReturnId(); // 落库 + 写缓存
    DeviceDO first = deviceMapper.selectById(id);     // 第一次：进 AOP
    DeviceDO second = deviceMapper.selectById(id);    // 第二次：应直接返回缓存

    CacheInspector ci = new CacheInspector(cacheManager);
    assertTrue(ci.assertHit("device", DeviceCacheKey.byId(id)));

    // 验证 DB 仅被查一次（用 Mockito spy 包装 mapper 或用 SQL 计数器）
    // 此处用 jdbc query counter
    assertEquals(1, queryCounter.get("tb_device"));
}
```

### 7.3 状态机终态不可逆（MediaSession）

```java
@ParameterizedTest
@EnumSource(value = Status.class, names = {"CLOSED", "FAILED"})
@DisplayName("终态不可被任何事件回退")
void terminalStatesAreImmutable(Status terminal) {
    String callId = createSessionAtStatus(terminal);

    mediaSessionManager.onInviteOk(callId, "dev-1");
    mediaSessionManager.onAck(callId);
    mediaSessionManager.onMediaStatus("dev-1", "101");

    MediaSessionDTO after = mediaSessionManager.getByCallId(callId);
    assertEquals(terminal.code, after.getStatus(), "终态不应被回退");
}
```

### 7.4 事件分片同 key 严格 FIFO（ShardDispatcher）

```java
@Test
@DisplayName("同 deviceId 100 条事件应严格 FIFO 处理")
void sameDeviceIdShouldBeStrictlyFifo() throws Exception {
    List<Integer> processed = new CopyOnWriteArrayList<>();
    ProtocolEventHandler handler = mock(ProtocolEventHandler.class);
    when(handler.protocol()).thenReturn("test");
    doAnswer(inv -> {
        DeviceEvent e = inv.getArgument(0);
        processed.add((Integer) e.payload().get("seq"));
        return null;
    }).when(handler).handle(any());

    InboundEventDispatcher dispatcher = new InboundEventDispatcher(List.of(handler));
    ShardDispatcher sd = new ShardDispatcher(16, 2000, dispatcher);
    sd.start();

    for (int i = 0; i < 100; i++) {
        sd.dispatch(EventEmitter.of("test", "Group.Name", "dev-1")
            .payload(Map.of("seq", i)).build());
    }
    sd.shutdown(); // 等待消费完毕

    assertEquals(100, processed.size());
    for (int i = 0; i < 100; i++) {
        assertEquals(i, processed.get(i).intValue(), "顺序应严格 FIFO");
    }
}
```

### 7.5 Schema 默认值契约（SchemaConstraintTest）

```java
@Test
@DisplayName("tb_media_session.status 默认值应为 2 (INVITING)")
void mediaSessionStatusDefaultIsInviting() {
    // 用最小字段插入（绕过 DO 默认值，直接执行 SQL）
    jdbcTemplate.update("INSERT INTO tb_media_session(call_id) VALUES(?)", "test-call-1");

    Integer status = jdbcTemplate.queryForObject(
        "SELECT status FROM tb_media_session WHERE call_id = ?", Integer.class, "test-call-1");

    assertEquals(2, status); // MediaSessionConstant.Status.INVITING
}
```

### 7.6 延迟双删降级（DelayedCacheEviction）

```java
@Test
@DisplayName("Redis 不可用时 scheduleEvict 应静默降级不抛")
void shouldDegradeGracefully_whenRedisDown() {
    StringRedisTemplate dead = mock(StringRedisTemplate.class);
    when(dead.opsForZSet()).thenThrow(new RedisConnectionFailureException("down"));

    DelayedCacheEviction d = new DelayedCacheEviction(dead, cacheManager);

    assertDoesNotThrow(() -> d.scheduleEvict("device", "1"));
    // 此时仅有立即 evict，无延迟 evict（已知降级，运维需告警）
}
```

---

## 8. 测试反模式与红线

### 8.1 红线（一票否决）

| ❌ 禁止 | ✅ 正确做法 |
|---------|------------|
| Controller/Service 用 `@SpringBootTest` | 用 `@ExtendWith(MockitoExtension.class)` 纯单元 |
| 使用 `@MockBean`（已废弃） | 用 `@MockitoBean`（Spring 6.2+） |
| Manager 跨线程测试加 `@Transactional` | 用 `BaseTest` 不加 `@Transactional`，手动清理 |
| `Thread.sleep(N)` 等异步完成 | 用 `CountDownLatch` / `Awaitility` |
| 测试间共享可变状态（static 字段、未清理的 DB 行） | `@BeforeEach`/`@AfterEach` 隔离 |
| 测试名只描述方法（`testSave`） | `should_X_when_Y` + 中文 `@DisplayName` |
| 用 `@Disabled` 跳过失败测试 | 修复或删除；保留必须挂 issue 链接 |
| 直接断言 `assertTrue(list.size() > 0)` | `assertThat(list).hasSize(N)` 或具体内容断言 |
| 在测试里硬编码绝对时间（`assertEquals(1717200000L, dto.getTime())`） | 用相对断言（`assertThat(time).isCloseTo(now, within(1, SECONDS))`） |

### 8.2 反模式

- **测试覆盖过深**：一个测试方法既测插入又测查询又测删除 → 拆成三个。
- **断言无意义**：仅 `assertNotNull(result)` 不验证内容 → 至少验证关键字段。
- **Mock 过度**：把 Mapper 都 Mock 掉去测 Manager 业务 → 用 BaseTest 真实库。
- **流程依赖**：测试 B 依赖测试 A 的副作用 → 每个测试独立准备数据。
- **隐式数据**：依赖 schema-sqlite.sql 的脏数据 → 显式 setup，避免假设。

### 8.3 已知陷阱（来自 MEMORY）

1. **IDE stale-class corruption**：测试前**始终** `mvn clean`（详见 `voglander-test-infra-gotchas`）。
2. **CacheTestConfig 必要性**：BaseTest 已 `@Import`，自定义集成测试若不继承必须显式 `@Import`。
3. **schema-sqlite.sql 非自动执行**：测试库 `test-app.db` 已提交，新增表需同步更新 SQL + DO + 测试库。
4. **gb28181 envelope 兼容**：测试出站命令时不要 mock `serverCommandSender`，应 mock `CommandHandlerRegistry`。

---

## 9. 验收标准与覆盖率门槛

### 9.1 数量门槛

| 模块 | 行覆盖 | 分支覆盖 |
|------|--------|----------|
| `voglander-manager/manager/**` | ≥ 85% | ≥ 75% |
| `voglander-manager/event/**` | ≥ 90% | ≥ 80% |
| `voglander-manager/cache/**` | ≥ 90% | ≥ 80% |
| `voglander-manager/assembler/**` | ≥ 80% | ≥ 70% |
| `voglander-repository/cache/**` | ≥ 85% | ≥ 75% |
| `voglander-repository/manager/**` | ≥ 85% | ≥ 75% |
| `voglander-repository/mapper/**` | ≥ 70% | ≥ 60% |
| **聚合（jacoco-aggregate）** | **≥ 80%** | **≥ 70%** |

> 通过 `./generate-coverage-report.sh` 生成报告 → `voglander-coverage-report/target/site/jacoco-aggregate/index.html`。

### 9.2 质量门槛

- ✅ 全部测试 `mvn clean test -P test` 一次通过；
- ✅ 不依赖外部 Redis 时（CI 默认无 Redis），通过 `Assumptions.assumeTrue` 优雅跳过 Redis 测试，不应失败；
- ✅ 单测平均耗时 < 50ms，集成测试单类 < 5s；
- ✅ 无 `@Disabled` 无 issue 链接；
- ✅ 无 `Thread.sleep` 用于异步等待（必须用 `Awaitility` 或 `CountDownLatch`）；
- ✅ `mvn pmd:check` / `mvn checkstyle:check` 零警告（如已启用）。

### 9.3 评审清单（每个 PR）

- [ ] 新增/修改测试遵循 §2 分层
- [ ] 测试与生产代码同 commit
- [ ] `@DisplayName` 中文清晰描述场景
- [ ] 断言关键字段而非仅 `notNull`
- [ ] 无 `Thread.sleep` 等待异步
- [ ] 跨线程场景未加 `@Transactional`
- [ ] 唯一键带时间戳/线程后缀避免并发污染
- [ ] 若涉及 Redis：有 `RedisAvailableExtension`
- [ ] 覆盖率不下降（CI gate）
- [ ] 提交信息：`test(scope): 描述`

---

## 附录 A：测试类落地清单

> 完整清单（按模块、按优先级）。✅=已存在，🆕=本方案新增，🔧=需补强。

### A.1 voglander-repository（共 ~25 个）

| # | 测试类 | 状态 | 优先级 |
|---|--------|------|--------|
| 1 | `SchemaConstraintTest` | 🆕 | P0 |
| 2 | `AbstractMapperTest` | 🆕 | P0 |
| 3 | `DeviceMapperTest` | 🆕 | P0 |
| 4 | `DeviceChannelMapperTest` | 🆕 | P0 |
| 5 | `MediaSessionMapperTest` | 🆕 | P0 |
| 6 | `MediaNodeMapperTest` | 🆕 | P0 |
| 7 | `StreamProxyMapperTest` | 🆕 | P0 |
| 8 | `PushProxyMapperTest` | 🆕 | P0 |
| 9 | `ExportTaskMapperTest` | 🆕 | P1 |
| 10 | `DeviceConfigMapperTest` | 🆕 | P0 |
| 11~14 | User/Role/Menu/Dept MapperTest | 🆕 | P1 |
| 15 | `CachedAspectTest` | 🆕 | **P0 关键** |
| 16 | `RedisCacheTest` | 🆕 | P1 |
| 17 | `RedisLockUtilTest` | 🆕 | P0 |
| 18 | `RedisLockUtilsTest` | 🆕 | P0 |
| 19 | `LocalCacheBaseTest` | 🆕 | P1 |
| 20 | `BizUniqueManagerTest` | 🆕 | P0 |
| 21 | `ConcurrentProcessHelperTest` | 🆕 | P0 |
| 22 | `TairManagerTest` | 🆕 | P1 |
| 23 | `RedisConfigCacheTtlTest` | ✅ | - |

### A.2 voglander-manager（共 ~35 个）

| # | 测试类 | 状态 | 优先级 |
|---|--------|------|--------|
| 1 | `DeviceManagerCacheTest` | ✅ | - |
| 2 | `DeviceManagerLivenessTest` | ✅ | - |
| 3 | `DeviceLivenessCoalescerTest` | ✅ | - |
| 4 | `DeviceOfflineCoalesceCacheTest` | ✅ | - |
| 5 | `DeviceManagerTemplateTest`（add/update/get/delete/getPage） | 🆕 | P0 |
| 6 | `DeviceChannelBatchUpsertTest` | ✅ | - |
| 7 | `DeviceChannelManagerTemplateTest` | 🆕 | P0 |
| 8 | `DeviceConfigManagerTest`（UNIQUE composite） | 🆕 | P0 |
| 9 | `MediaSessionManagerTest` | ✅ | - |
| 10 | `MediaSessionConcurrentUpsertTest` | ✅ | - |
| 11 | `MediaSessionStateMachineTest`（五态转换） | 🆕 | **P0 关键** |
| 12 | `MediaNodeManagerTest`（含 saveOrUpdateNodeStatus 并发） | 🆕 | **P0 关键** |
| 13 | `StreamProxyManagerTest` | 🆕 | **P0 关键** |
| 14 | `PushProxyManagerTest` | 🆕 | **P0 关键** |
| 15 | `ExportTaskManagerTest`（任务状态机） | 🆕 | P0 |
| 16~19 | User/Role/Menu/Dept ManagerTest | 🆕 | P1 |
| 20 | `ShardDispatcherTest` | ✅ | 🔧 补强 |
| 21 | `EventShardTest` | ✅ | 🔧 补强 |
| 22 | `InboundEventDispatcherTest` | ✅ | 🔧 补强 |
| 23 | `NoopProtocolHandlerPluggableTest` | ✅ | - |
| 24 | `DelayedCacheEvictionTest` | ✅ | 🔧 补强 |
| 25~34 | 10 个 `*AssemblerTest` | 🆕 | P1 |
| 35 | `AsyncManagerTest` | 🆕 | P1 |
| 36 | `SpringDynamicTaskTest` | 🆕 | P2 |

### A.3 support 工具（共 5 个）

| # | 工具类 | 状态 |
|---|--------|------|
| 1 | `RedisAvailableExtension` | 🆕 |
| 2 | `TestDataCleaner` | 🆕 |
| 3 | `UniqueKeyFactory` | 🆕 |
| 4 | `CacheInspector` | 🆕 |
| 5 | `EventEmitter` | 🆕 |

**新增测试总数**：约 50~55 个，按 12 次 PR 完成，预计 3~4 周（按每周 4 个工作日、每天 3~4 个测试类节奏）。

---

## 附录 B：参考文档

- [ARCHITECTURE.md](./ARCHITECTURE.md) — 1.0.3 架构基线
- [OPTIMIZATION-DESIGN.md](./OPTIMIZATION-DESIGN.md) — Phase 设计稿
- [PROTOCOL-EXTENSIBILITY-DESIGN.md](./PROTOCOL-EXTENSIBILITY-DESIGN.md) — 协议解耦设计
- [MERGED-IMPLEMENTATION-CHECKLIST.md](./MERGED-IMPLEMENTATION-CHECKLIST.md) — 实施清单
- 根 `CLAUDE.md` §测试策略 — 权威分层规范
- `voglander/CLAUDE.md` §测试 — Manager 集成测试细则
