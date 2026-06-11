# Voglander 架构设计文档

> 版本：1.0.3 ｜ 修订日期：2026-06-02 ｜ 对应分支：`dev_merge_sip`
> 适用代码基线：sip-gateway 1.8.0 接入完成 + GB28181 出站命令 envelope 化 + 事件分片管线（Phases 0/1/2a/3/4/8）落地

---

## 目录

1. [文档目的与范围](#1-文档目的与范围)
2. [系统概述](#2-系统概述)
3. [技术栈](#3-技术栈)
4. [模块架构](#4-模块架构)
5. [分层架构](#5-分层架构)
6. [核心设计模式](#6-核心设计模式)
7. [GB28181 集成架构（sip-gateway 1.8.0）](#7-gb28181-集成架构sip-gateway-180)
8. [ZLM 媒体流集成架构](#8-zlm-媒体流集成架构)
9. [数据模型](#9-数据模型)
10. [缓存与并发](#10-缓存与并发)
11. [Web 层与安全](#11-web-层与安全)
12. [异步与线程模型](#12-异步与线程模型)
13. [配置与部署](#13-配置与部署)
14. [测试架构](#14-测试架构)
15. [关键约束与易踩坑清单](#15-关键约束与易踩坑清单)
16. [演进路线](#16-演进路线)

---

## 1. 文档目的与范围

本文件描述 Voglander 视频监控平台后端的**当前实际架构**，重点覆盖：

- 多模块工程的依赖关系与分层职责边界；
- 四大核心设计模式（Assembler / Manager 模板 / Wrapper / Template）的落地方式；
- GB28181 协议从「直连 handler」到 sip-gateway 1.8.0「命令 envelope + 统一回调」的现行架构；
- ZLM 媒体流的代理、推流、Hook 回调闭环；
- 数据模型、缓存、并发、安全、异步、测试等横切面。

文档以**代码现状**为准（已对照源码逐项核验），不是规划稿。前端（vue-vben-admin）与 sip-proxy 框架本身不在本文范围内，仅在交互边界处提及。

---

## 2. 系统概述

Voglander 是基于 **Spring Boot 3.5.3 + Java 17** 的企业级视频监控平台，对接多种视频监控协议（GB28181、GT1078、ONVIF），提供设备接入、实时点播/回放、媒体流转发与录制、设备状态监控、权限管理等能力。

### 2.1 系统上下文

```
                          ┌───────────────────────────┐
                          │   前端 (vue-vben-admin)    │
                          │   管理后台 / 监控大屏      │
                          └─────────────┬─────────────┘
                                        │ HTTP/REST (AjaxResult)
                                        ▼
        ┌───────────────────────────────────────────────────────────┐
        │                    Voglander 后端 (本工程)                  │
        │                                                             │
        │   ┌──────────┐   GB28181/SIP    ┌─────────────────────┐    │
        │   │  SIP 网关 │◀────────────────▶│  IPC / NVR / 下级平台 │    │
        │   │(sip-gw1.8)│   注册/心跳/INVITE└─────────────────────┘    │
        │   └──────────┘                                              │
        │                                                             │
        │   ┌──────────┐   HTTP API/Hook  ┌─────────────────────┐    │
        │   │ ZLM 集成  │◀────────────────▶│   ZLMediaKit 媒体服务 │    │
        │   └──────────┘   拉流/推流/回调  └─────────────────────┘    │
        │                                                             │
        │   ┌─────────────────┐   ┌─────────┐   ┌────────────────┐   │
        │   │ MySQL / SQLite  │   │  Redis  │   │  SkyWalking    │   │
        │   └─────────────────┘   └─────────┘   └────────────────┘   │
        └───────────────────────────────────────────────────────────┘
```

### 2.2 三条核心业务链路

| 链路 | 入口 | 关键组件 | 出口 |
|------|------|----------|------|
| **GB28181 设备入站** | 设备 SIP 报文 | sip-gateway → `VoglanderBusinessNotifier`(翻译) → `ShardDispatcher`(分片) → `InboundEventDispatcher`(协议路由) → `Gb28181ProtocolHandler` → `DeviceRegisterService`/`DeviceManager`/`MediaSessionManager` | 设备/通道/会话状态落库 |
| **GB28181 平台出站指令** | REST/业务调用 | `VoglanderServer*Command` → `dispatchEnvelope` → `CommandHandlerRegistry`(envelope) → SIP 下发 | 设备执行（PTZ/点播/回放/配置） |
| **媒体流代理** | 前端 `/zlm/api/*` | `StreamProxyController` → `StreamProxyManager` → `StreamProxyZlmWrapperService` | ZLM 拉流 + Hook 回写状态 |

---

## 3. 技术栈

### 3.1 核心框架

| 类别 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | 全工程 `jakarta.*`，**注意** integration 模块 pom 仍有历史遗留 `<source>9</source>` 约束（详见 §15） |
| 基础框架 | Spring Boot | 3.5.3 | 自动配置、Web、AOP、Async |
| ORM | MyBatis-Plus | 3.5.5 | `IService` 基础 CRUD + `LambdaQueryWrapper` |
| 多数据源 | dynamic-datasource | 4.3.1 | `master` 默认源，支持多库切换 |
| 连接池 | HikariCP | — | max-pool=20, min-idle=10 |
| 公共库 | luna-common | 2.6.8 | `ResultDTO`/`ResultDTOUtils`/工具集 |

### 3.2 数据与缓存

| 类别 | 选型 | 说明 |
|------|------|------|
| 生产数据库 | MySQL 8.2.0 | `sql/voglander.sql` 建库 |
| 开发/测试数据库 | SQLite | 默认 `app.db`；测试 `test-app.db` + `schema-sqlite.sql` |
| 缓存/锁 | Redis | `RedisCache` 手动缓存 + `RedisLockUtil` 分布式锁 + `@Cached` 注解 |

### 3.3 视频与集成

| 类别 | 选型 | 版本 | 说明 |
|------|------|------|------|
| SIP 网关 | sip-gateway-spring-boot-starter | 1.8.0 | GB28181 接入主框架，**取代** 1.2.5 旧版 handler 直连 |
| GB28181 协议 | gb28181 client/server | 1.8.0 | 设备端/平台端协议栈 |
| 媒体服务 | ZLMediaKit-Starter | 1.0.10-SNAPSHOT | 拉流/推流/录制/Hook |
| Excel | EasyExcel | 4.0.1 | 导入导出 |
| JSON | FastJSON2 | — | **唯一**序列化方案，禁用 Jackson/Gson |

### 3.4 监控与文档

| 类别 | 选型 | 版本 |
|------|------|------|
| 链路追踪 | SkyWalking | 9.1.0 |
| API 文档 | SpringDoc OpenAPI | 2.8.9 |
| 测试 | JUnit 5 + Mockito | — |
| 覆盖率 | JaCoCo | 0.8.11（聚合报告模块） |

---

## 4. 模块架构

### 4.1 Maven 多模块清单

```
voglander/
├── voglander-common/          # 共享：常量、枚举、异常、工具、注解
├── voglander-client/          # 对外服务接口契约 + DTO/VO/QO
├── voglander-repository/      # 数据访问：DO 实体、Mapper、缓存
├── voglander-manager/         # 业务编排：Manager 模板、Service、Assembler、异步、线程池
├── voglander-service/         # 领域服务：登录注册、流业务、设备协议
├── voglander-integration/     # 外部集成：GB28181、ZLM、EasyExcel、IP 解析
├── voglander-web/             # 应用主模块：Controller、过滤器、拦截器、启动类、全部测试
├── voglander-test/            # 测试配置与多环境 profile
└── voglander-coverage-report/ # JaCoCo 跨模块聚合覆盖率
```

### 4.2 模块依赖关系（经 pom 核验，无环）

```
                 voglander-common  (基础，无依赖)
                        ▲
                        │
                 voglander-client  ──────────┐
                        ▲                     │
                        │                     │
                 voglander-repository         │
                        ▲                     │
                        │                     │
                 voglander-manager            │
                        ▲                     │
            ┌───────────┴───────────┐         │
   voglander-integration ───────────┘         │
            ▲    (integration → manager,       │
            │     client, common)              │
   voglander-service ─────────────────────────┘
   (service → manager, integration, client, common)
            ▲
            │
   voglander-web  (web → service, integration)
```

**关键依赖方向**：

- `integration → manager`：集成层（如 `VoglanderBusinessNotifier`）回调时直接调用 Manager 完成落库；
- `manager → repository`：Manager 只依赖 repository，不依赖 integration（避免环）；
- `web` 是唯一可执行模块，含 `ApplicationWeb` 启动类与**全部测试代码**。

### 4.3 模块职责矩阵

| 模块 | 核心职责 | 关键产物 |
|------|----------|----------|
| common | 跨层共享，零业务依赖 | `DeviceConstant`、`MediaSessionConstant`、`DeviceAgreementEnum`、`@RateLimiter`、`@RepeatSubmit`、`ServiceException` |
| client | 服务契约与传输模型 | `DeviceRegisterService` 接口、`DeviceEvent`（协议无关事件模型）、`DeviceRegisterReq`/`DeviceChannelReq`/`DeviceInfoReq` |
| repository | 持久化与缓存 | 14 个 `*DO`、15 个 `*Mapper`（含 `AbstractMapper`）、`RedisCache`、`RedisLockUtil`、`@Cached`/`CachedAspect` |
| manager | 业务编排 + 模板方法 + **事件分片管线** | 12 个 `*Manager`、`*Service`/Impl、`*Assembler`、`event/ShardDispatcher`、`event/EventShard`、`event/InboundEventDispatcher`、`event/ProtocolEventHandler`、`event/NoopProtocolHandler`、`cache/DelayedCacheEviction`、`AsyncManager`、`ThreadPoolConfig` |
| service | 领域服务 | `DeviceRegisterServiceImpl`、`StreamProxyBizService`、`PushProxyBizService`、`DeviceAgreementService` |
| integration | 外部系统包装 | GB28181 notifier/handler/command/store/supplier、ZLM wrapper/hook、EasyExcel、IP |
| web | REST 入口 | 14 个 `*Controller`、XSS/Trace/Repeat 过滤器、`WebAssembler`、`ApplicationWeb` |

---

## 5. 分层架构

### 5.1 调用链与数据流

```
HTTP 请求
  │
  ▼
┌──────────────────────────────────────────────────────────────┐
│ Web 层 (Controller)                                          │
│  - 入参：*CreateReq / *UpdateReq / *QueryReq （禁止直用 DTO） │
│  - WebAssembler: Req ──▶ DTO                                 │
│  - 出参：DTO ──▶ VO（Unix 时间戳），包装 AjaxResult<T>        │
└───────────────────────────┬─────────────────────────────────┘
                            │ DTO
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Manager 层（复杂编排 + 模板方法）                             │
│  - 对外只暴露 DTO；内部 Assembler: DTO ◀──▶ DO               │
│  - 统一缓存清理 clearCache(id, oldKey, newKey)               │
│  - LambdaQueryWrapper（强制 condition 参数）                 │
└───────────────────────────┬─────────────────────────────────┘
                            │ DO
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Service 层（IService<DO> 基础 CRUD / 领域逻辑）              │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Repository 层（Mapper + @Cached 缓存）                       │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
                       MySQL / SQLite + Redis
```

### 5.2 各层职责边界

| 层 | 入参类型 | 出参类型 | 允许的职责 | 禁止的职责 |
|----|----------|----------|-----------|-----------|
| Web | `*Req` | `AjaxResult<VO>` | 参数校验 `@Valid`、格式转换、Swagger 文档 | 业务逻辑、直接访问 DO |
| Manager | `*DTO` | `*DTO` / `Page<DTO>` | 多服务编排、缓存一致性、事务、DTO↔DO 转换 | 暴露 DO（历史方法 `@Deprecated`） |
| Service | `*DTO`（主）/ 基本类型（便利） | 领域对象 | 单一职责业务、`Assert` 校验、`ServiceException` | — |
| Repository | DO / 主键 | DO | 持久化、`@Cached` 单对象缓存 | 跨表复杂编排 |
| Integration | 外部模型 | `ResultDTO<T>` | 参数校验、异常捕获、结果包装 | 业务逻辑（委托 Service/Manager） |

### 5.3 数据模型命名规约

| 后缀 | 含义 | 所在层 | 时间字段 |
|------|------|--------|----------|
| `*DO` | 数据库实体 | repository | `LocalDateTime` |
| `*DTO` | 层间传输 | manager/service/client | `LocalDateTime` |
| `*VO` | 视图对象 | web | **Unix 毫秒时间戳** |
| `*Req` | 请求入参 | web/client | — |
| `*Resp` | 嵌套响应（含 `items`） | web | — |
| `*QO` | 查询对象 | client | — |

---

## 6. 核心设计模式

### 6.1 Assembler 模式（层间数据转换）

- **WebAssembler**：`Req → DTO`、`DTO → VO`（VO 中 `LocalDateTime` 经 `fieldNameToEpochMilli()` 转 Unix 时间戳）；
- **Manager Assembler**：`DTO ↔ DO` 双向转换；
- **类型转换铁律**：所有类型转换通过 **FastJSON2 正反序列化**完成，禁止手写字符串解析。

  ```java
  TargetType target = JSON.parseObject(JSON.toJSONString(source), TargetType.class);
  ```

### 6.2 Manager 模板方法模式

每个 `*Manager` 实现一组标准模板方法，保证统一的校验/转换/缓存/日志：

```
add(DTO)                          → 新增，返回 ID
update(queryDTO, updateDTO)       → 查询条件与更新内容分离
updateById(id, updateDTO)         → 最常用，按主键更新
get(DTO)                          → LambdaQueryWrapper + limit 1
deleteOne(DTO) / deleteBatch(DTO) → 条件删除 + 缓存清理
getPage(DTO, page, size)          → 默认 createTime 降序
clearCache(id, oldKey, newKey)    → 统一缓存清理（主键 + 业务键双清）
```

> 实例：`DeviceManager`、`StreamProxyManager`、`MediaSessionManager` 均严格遵循该套接口，并在模板之上扩展业务方法（如 `DeviceManager.updateStatus`、`StreamProxyManager.updateStreamProxyOnlineStatus`）。

**LambdaQueryWrapper 强制规范**：必须使用带 `condition` 参数的重载，禁止手写 `if (x != null)` 拼条件，避免 `new LambdaQueryWrapper<>(entity)` 因 null 字段污染查询。

### 6.3 Wrapper 模式（外部系统集成）

Integration 层所有外部调用统一返回 `ResultDTO<T>`，通过 `WrapperExceptionHandler.executeWithExceptionHandling(...)` 模板：

```java
return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
    WrapperExceptionHandler.validateRequest(request, "请求参数");   // 1. 参数校验（断言）
    T result = ExternalService.call(request);                       // 2. 业务调用
    if (result == null || !isValid(result)) return null;           // 3. 结果校验（失败 null）
    return result;                                                  // 4. 成功返回模型
}, "操作描述");
```

- 成功 → `data` 为具体模型；失败/异常 → `data` 为 null，外层 `ResultDTO` 统一包装；
- Wrapper **只做**参数校验与异常捕获，业务逻辑委托 Service 层；
- 领域专用校验下沉到专用类（如 `ZlmWrapperValidator`）。

### 6.4 Template 模式（统一内部方法）

数据修改统一走 Manager 内部方法入口，保证缓存失效与日志记录一致；UNIQUE 约束场景在 `saveOrUpdate` 前先按业务主键（如 `app+stream`、`callId`）查询既有记录。

### 6.5 响应协议

| 场景 | 返回类型 | 取错误信息 |
|------|----------|-----------|
| Web 层 REST | `AjaxResult<T>`（`code`/`msg`/`data`，成功 `code=0`） | `getMsg()` |
| Integration 外部集成 | `ResultDTO<T>` | `getMessage()` |

---

## 7. GB28181 集成架构（sip-gateway 1.8.0）

> 本节是 1.0.3 相对历史版本变化最大的部分。GB28181 已完成两步重大演进：
> 1. **协议接入**：从「直接 handler 分发」迁移到 sip-gateway 1.8.0 的「**出站命令 envelope + 入站统一回调**」双通道；
> 2. **入站管线**：Notifier 从「直调业务服务」演进为「**轻量翻译 → 分片调度 → 协议路由 → 协议处理器**」四层管线（Phase 1/2a/3/4/8），实现**协议解耦**、**单设备串行**、**SIP 线程零阻塞**。
>
> `wrapper/gb28181/` 下不再有任何 `*RequestHandler`/`*ResponseHandler`。

### 7.1 包结构

```
intergration/wrapper/gb28181/
├── notifier/
│   ├── VoglanderBusinessNotifier          # 主入口（implements BusinessNotifier，@Async）
│   │                                      # 翻译 GatewayEvent → DeviceEvent，提交 ShardDispatcher
│   └── VoglanderBusinessNotifierFallback  # 灰度回退入口（shard.enabled=false 激活，直调 Dispatcher）
├── handler/
│   └── Gb28181ProtocolHandler             # GB28181 协议处理器（implements ProtocolEventHandler）
│                                          # 承接原 switch 业务逻辑，不依赖任何 sip-gateway 类型
├── server/command/                        # 平台端出站命令（envelope 化）
│   ├── AbstractVoglanderServerCommand     # 抽象基类：dispatchEnvelope 模板
│   └── ptz/ record/ device/ alarm/ config/ media/   # 6 个业务域
├── client/command/                        # 设备端出站命令
│   ├── AbstractVoglanderClientCommand
│   └── ptz/ record/ device/ alarm/ catalog/ status/  # 6 个业务域
├── supplier/
│   ├── VoglanderServerDeviceSupplier      # @Primary，按 deviceId 提供 ToDevice
│   ├── VoglanderClientDeviceSupplier      # @Primary
│   └── VoglanderDeviceSessionCache        # 实现框架 DeviceSessionCache（ServerCommandSender 强依赖）
├── store/
│   └── RedisInviteContextStore            # 多节点 INVITE 上下文（@ConditionalOnProperty）
├── start/
│   ├── ServerStart                        # CommandLineRunner，绑定 SIP 监听端口
│   └── SipServerConfig
└── config/properties/                     # VoglanderSipServerProperties / VoglanderSipClientProperties
```

事件分片管线在 manager 模块（**与协议解耦**）：

```
voglander-manager/event/
├── ShardDispatcher        # 按 shardKey 哈希到 N 个槽（默认 16），单槽单线程串行
├── EventShard             # 单分片：BlockingQueue + 单线程消费 → 调 InboundEventDispatcher
├── InboundEventDispatcher # 按 event.protocol() 路由到对应 ProtocolEventHandler
├── ProtocolEventHandler   # 协议处理器接口（新增协议只需新增实现，本类零改动）
└── NoopProtocolHandler    # 可插拔验证 / 协议降级用（安全吞掉事件）
```

### 7.2 入站：四层事件管线

```
设备 SIP 报文
   │
   ▼
sip-gateway 协议栈 → Forwarder（发出 35 种 GatewayEvent）
   │
   ▼ ① 框架回调
VoglanderBusinessNotifier.notify(GatewayEvent)   @Async("sipNotifierExecutor")
   │   职责：仅做轻量翻译，payload 保持 Map 引用（不反序列化），立即归还 SIP 线程
   │   产物：DeviceEvent(protocol, group, name, deviceId, correlationId, ts, payload:Map, nodeId)
   ▼
   ▼ ② 分片调度
ShardDispatcher.dispatch(DeviceEvent)
   │   shardKey = deviceId 优先，null 时退回 correlationId
   │   哈希到 N 个 EventShard（默认 16 个），每槽 BlockingQueue(2000) + 单线程消费
   │   → 同设备/同会话事件串行处理，跨设备并发
   ▼
   ▼ ③ 协议路由
InboundEventDispatcher.dispatch(DeviceEvent)
   │   按 event.protocol() 路由：建 protocol → ProtocolEventHandler 的映射表
   │   未注册协议安全丢弃
   ▼
   ▼ ④ 协议处理
Gb28181ProtocolHandler.handle(DeviceEvent)         （或其它协议 handler）
   │   按 groupName（"Lifecycle.Register" 等）switch；
   │   payload 此时才用 FastJSON2 反序列化为 GB28181 实体；
   │   不 import 任何 sip-gateway 类型（与具体 gateway 产品解耦）
   ▼ 业务调用
   ├── Lifecycle.Register          → DeviceRegisterService.login(DeviceRegisterReq)
   ├── Lifecycle.Online            → DeviceManager.patchLiveness(deviceId, ONLINE, null)   ← Phase 2a 定向更新
   ├── Lifecycle.Offline           → DeviceRegisterService.offline(deviceId)
   ├── Lifecycle.RemoteAddrChanged → DeviceRegisterService.updateRemoteAddress(...)
   ├── Notify.Keepalive            → DeviceRegisterService.keepalive(deviceId)
   ├── Notify.MediaStatus          → MediaSessionManager.onMediaStatus(deviceId, type)
   ├── Response.Catalog            → 逐 DeviceItem → DeviceRegisterService.addChannel(...)
   ├── Response.DeviceInfo         → DeviceRegisterService.updateDeviceInfo(...)
   ├── Session.InviteOk            → MediaSessionManager.onInviteOk(callId, deviceId)
   ├── Session.InviteFailure       → MediaSessionManager.onInviteFailure(callId, statusCode)
   ├── Session.Ack                 → MediaSessionManager.onAck(callId)
   └── Session.Bye                 → MediaSessionManager.onBye(deviceId)
```

**关键约束与设计要点**：

- **SIP 线程零阻塞**：`notify(GatewayEvent)` 标注 `@Async("sipNotifierExecutor")`，且仅做翻译（O(1) 字符串切分 + 构造 record），重活全部下沉到分片槽。若同步处理或在 Notifier 里做反序列化/DB 写，设备端 SIP 会超时重传。
- **禁止继承 `AbstractProtocolBusinessNotifier`**：其 `notify()` 为 `final` 且内部自调用 `onProtocolEvent`，`@Async` 代理失效。本工程直接 `implements BusinessNotifier`。
- **同设备串行**：`ShardDispatcher` 用 `deviceId` 哈希分片，保证同设备事件串行；跨设备无锁并发。修复**乱序导致的状态翻转**（如 Offline→Online 被错序写）。
- **payload 延迟反序列化**：Notifier 阶段保持 `payload:Map` 原引用，FastJSON2 反序列化推迟到 `Gb28181ProtocolHandler` 中执行——既减小 SIP 线程开销，也让昂贵的解码并行散落在 16 个槽中。
- **协议解耦**：`Gb28181ProtocolHandler` 不 import `GatewayEvent`/`BusinessNotifier` 等任何 sip-gateway 框架类型；新增 GT1078/ONVIF 协议只需新增 `ProtocolEventHandler` 实现，分片管线**零改动**。
- **灰度开关**：
  - `voglander.event.shard.enabled=true`（默认）→ 主入口 `VoglanderBusinessNotifier`，走分片管线；
  - `voglander.event.shard.enabled=false` → 回退入口 `VoglanderBusinessNotifierFallback`，跳过分片直调 `InboundEventDispatcher`（小流量/调试场景）。
- **NoopProtocolHandler**（Phase 8）：用于协议可插拔验证或临时降级——`@Component` 注入后安全吞掉指定协议的所有事件。
- **GatewayEvent 结构**：record `(type, deviceId, correlationId, timestampMs, payload:Map, nodeId)`；type 为三段式 `gb28181.Group.Name`。

### 7.3 出站：命令 envelope 通道（2026-06-01 改造完成）

平台端命令不再直调 `ServerCommandSender`，统一经 `CommandHandlerRegistry`：

```
业务调用 VoglanderServerMediaCommand.inviteRealTimePlay(deviceId, sdpIp, port, mode)
   │
   ▼  AbstractVoglanderServerCommand.dispatchEnvelope(type, deviceId, payload)
   │   1. 构造 GatewayCommand(type, deviceId, payload, requestId=null)
   │   2. commandHandlerRegistry.require(type)   // type 不存在 → 404
   │   3. handler.handle(cmd) → GatewayCommandResult.correlationId()(sn/callId)
   │   4. 异常 → ResultDTOUtils.failure
   ▼
协议适配层 (Gb28181WhitelistHandlers / Gb28181CommandSpecs)
   │   按 type 反查 payload schema → 转 SIP 消息下发
   ▼
设备
```

**命令类型映射**（`VoglanderServerMediaCommand` 示例）：

| 业务方法 | envelope type | payload |
|----------|---------------|---------|
| `inviteRealTimePlay` | `gb28181.Invite.Play` | `{mediaIp, mediaPort:int, streamMode}` |
| `invitePlayBack` | `gb28181.Invite.Playback` | 上述 + `{startTime, endTime}` |
| `controlPlayBack` | `gb28181.Invite.PlaybackControl` | `{action: PlayActionEnums.name()}` |
| `sendAck` | `gb28181.Invite.Ack` | `{callId?}` |
| `sendBye` | `gb28181.Invite.Bye` | `{callId}`（deviceId 留空） |
| `sendBroadcast` | `gb28181.Device.Broadcast` | `{}` |

> 注意：白名单 handler 的 SDP IP 字段名为 `mediaIp`（非 `sdpIp`）。
> `serverCommandSender` 实例 Bean 仍保留作过渡降级，业务逻辑严禁直调。

各业务域命令（ptz/record/device/alarm/config/media）均已改走 envelope（见近期 commit `499be7f`~`a855f86`）。客户端命令 `ClientCommandSender` 保持 `@Component` + 静态方法，7 个设备端命令大体不变（PTZ 改用 `PTZControlEnum`+`PTZInstructionBuilder`）。

### 7.4 会话状态管理 `MediaSessionManager`

INVITE 点播/回放会话状态独立建模：

- 实体 `MediaSessionDO`（表 `tb_media_session`），业务主键 **SIP callId**（UNIQUE）；
- 状态 `MediaSessionConstant.Status`：`INVITING=2`（默认）/ `ACTIVE=1` / `CLOSED=0` / `FAILED=3`；
- 类型 `Type`：`PLAY` / `PLAYBACK` / `DOWNLOAD` / `TALK`；
- 由 `Gb28181ProtocolHandler` 的 `Session.*` 与 `Notify.MediaStatus` 分支驱动：`onInviteOk`/`onInviteFailure`/`onAck`/`onBye`/`onMediaStatus`；
- 模板方法 CRUD（`add`/`update`/`getByCallId`/`getPage`...）齐全；
- **同设备 callId 并发**由 §7.2 的分片串行保证，重复 INVITE 走幂等 upsert。

### 7.5 多节点 INVITE 上下文 `RedisInviteContextStore`

```
@ConditionalOnProperty(gateway.gb28181.store.type=redis)
key = sip:invite:ctx:{callId}  →  InviteContext(nodeId, ctxKey)  [FastJSON2, TTL]
```

- 支撑跨节点 INVITE 回包路由；单机默认走框架 `InMemoryInviteContextStore`（`type=memory`）；
- 契约：`find()==null` → 业务回包 410；Redis 后端故障 **必须抛 503**（`ResponseStatusException`），不可静默吞。

### 7.6 启动装配（强约束）

| 组件 | 必要性 | 原因 |
|------|--------|------|
| `ApplicationWeb` 上 `@EnableSipServer` | **必需** | `@Import` 了 `Gb28181CommonAutoConfig`（提供 `CommandStrategyFactory`）等；这些配置无 `.imports` 注册，不加注解则 `ServerCommandSender`/`ClientCommandSender` 无法实例化，进而 `Gb28181GatewayAutoConfiguration`（`@ConditionalOnBean(ServerCommandSender)`）静默关闭整条事件管线 |
| `VoglanderDeviceSessionCache` | **必需** | 实现框架 `DeviceSessionCache`，`ServerCommandSender` 构造强依赖 |
| `VoglanderServerDeviceSupplier`/`ClientDeviceSupplier` 标 `@Primary` | **必需** | `@EnableSipServer` 会激活框架 `Default*DeviceSupplier`，不加 `@Primary` 触发 `NoUniqueBeanDefinitionException` |
| `ServerStart`（CommandLineRunner） | **不可删** | 全工程唯一绑定 SIP 监听端口处；1.8.0 须用 `sipLayer.addListeningPoint(ip, port, sipListener, enableLog)` |
| `ShardDispatcher` + `EventShard` 池 | **必需**（默认启用） | `voglander.event.shard.enabled=true` 时主入口依赖；关闭时回退入口 `Fallback` 仍依赖 `InboundEventDispatcher` |

---

## 8. ZLM 媒体流集成架构

### 8.1 包结构

```
intergration/wrapper/zlm/
├── impl/VoglanderZlmHookServiceImpl   # extends AbstractZlmHookService，处理 ZLM 全量 Hook
├── service/
│   ├── StreamProxyZlmWrapperService   # 拉流代理
│   ├── PushProxyZlmWrapperService     # 推流代理
│   └── dto/                           # StreamProxyRequest / PushProxyRequest / ...
├── supplier/VoglanderNodeSupplier     # ZLM 节点选择
├── config/ZlmIntegrationConfig        # @ConfigurationProperties(prefix="zlm")
└── common/ZlmWrapperValidator         # ZLM 专用参数校验
```

### 8.2 拉流代理闭环

```
前端 → StreamProxyController (/zlm/api/proxy/add)
   │  WebAssembler: Req → StreamProxyDTO
   ▼
StreamProxyManager.add/createStreamProxy        ← 落库 tb_stream_proxy
   │
   ▼
StreamProxyZlmWrapperService.addStreamProxy     ← 调 ZLM HTTP API
   │
   ▼
ZLMediaKit 创建拉流
   │
   ▼ Hook 回调
VoglanderZlmHookServiceImpl.onStreamChanged     ← 回写在线状态/流信息
```

### 8.3 ZLM Hook 事件覆盖

`VoglanderZlmHookServiceImpl` 覆盖 ZLM 全量回调：

| Hook | 处理 |
|------|------|
| `onServerKeepLive` | `MediaNodeManager.saveOrUpdateNodeStatus(...)` 心跳更新节点状态 |
| `onPlay` | 播放鉴权（当前放行，TODO 业务鉴权） |
| `onPublish` | 推流鉴权 + 启用 HLS/MP4 |
| `onStreamChanged` | 流注册/注销 → 回写 `StreamProxyManager` 在线状态 |
| `onStreamNoneReader` | 无人观看 → 可触发关流 |
| `onStreamNotFound` | 流未找到 → 可触发按需拉流 |
| `onFlowReport` | 流量统计 |
| `onRecordMp4` | 录制完成回调 |
| `onRtspRealm`/`onRtspAuth`/`onHttpAccess` | 访问鉴权 |
| `onSendRtpStopped`/`onRtpServerTimeout` | RTP 推流/超时 |

### 8.4 节点管理

- `MediaNodeDO`（表 `tb_media_node`）：`server_id`(UNIQUE) / `host` / `secret` / `enabled` / `hook_enabled` / `weight`(负载权重) / `keepalive` / `status`；
- 支持多 ZLM 节点配置（`zlm.servers[]`），`VoglanderNodeSupplier` 负责节点选择。

---

## 9. 数据模型

### 9.1 实体清单（14 个 DO / 15 个 Mapper）

| 领域 | 实体 | 表 | 说明 |
|------|------|----|----|
| 设备 | `DeviceDO` | `tb_device` | 设备主表 |
| 设备 | `DeviceChannelDO` | `tb_device_channel` | 设备通道 |
| 设备 | `DeviceConfigDO` | `tb_device_config` | 设备配置 |
| 媒体 | `MediaNodeDO` | `tb_media_node` | ZLM 节点 |
| 媒体 | `MediaSessionDO` | `tb_media_session` | INVITE 会话状态（**1.0.3 新增**） |
| 媒体 | `StreamProxyDO` | `tb_stream_proxy` | 拉流代理 |
| 媒体 | `PushProxyDO` | `tb_push_proxy` | 推流代理 |
| 任务 | `ExportTaskDO` | `tb_export_task` | 导出任务 |
| 权限 | `UserDO`/`RoleDO`/`MenuDO`/`DeptDO` | — | RBAC 主体 |
| 权限 | `UserRoleDO`/`RoleMenuDO` | — | RBAC 关联 |

### 9.2 统一字段约定

所有业务表遵循统一规约：

| 字段 | 类型 | 约定 |
|------|------|------|
| `id` | BIGINT AUTO | 主键 |
| `create_time` / `update_time` | DATETIME | 审计字段，自动时间戳；DO 中为 `LocalDateTime` |
| `enabled` | INT/BOOL | 启用/禁用开关 |
| `extend` | TEXT | JSON 扩展字段（FastJSON2 存取） |

### 9.3 设备实体示例（`DeviceDO`）

```
id, createTime, updateTime,
deviceId,        // GB28181 设备编号
status,          // 1在线 0离线（DeviceConstant.Status）
name, ip, port,
registerTime, keepaliveTime,
serverIp,        // 注册节点
type,            // 协议类型 DeviceAgreementEnum
extend           // JSON 扩展
```

### 9.4 会话实体（`MediaSessionDO` / `tb_media_session`）

```sql
call_id      VARCHAR(255) NOT NULL UNIQUE,  -- 业务主键
device_id    VARCHAR(64),
channel_id   VARCHAR(64),
ssrc         VARCHAR(32),
stream       VARCHAR(255),
status       INTEGER DEFAULT 2,             -- INVITING
session_type VARCHAR(32),                   -- PLAY/PLAYBACK/DOWNLOAD/TALK
extend       TEXT
```

---

## 10. 缓存与并发

### 10.1 缓存分层策略

| 机制 | 适用场景 | 位置 |
|------|----------|------|
| `@Cached` 注解 | Repository 层**单对象**按 ID/唯一键查询 | repository（`CachedAspect`） |
| `RedisCache` | 复杂场景手动键管理，值以 JSON 存储 | repository |
| `LocalCacheBase` | 本地缓存 | repository/cache/local |
| `DelayedCacheEviction` | **延迟双删**（Phase 1）：写后投入 Redis ZSet，500ms 后跨节点二次 evict | manager/cache |

- 缓存键使用主键或唯一字段；
- Manager 统一通过 `clearCache(id, oldKey, newKey)` 做**主键 + 业务键双重清理**，缓存清理异常不阻断主流程；
- **延迟双删**：写操作先立即 evict，再把 `cacheName::key` 投入 Redis ZSet `cache:evict:delay`（score = now+500ms），由所有节点共消费的扫描器到点 `drainDue` 再 evict 一次，修复**JVM 定时器进程崩溃后脏读到 TTL** 的窗口（A4 缺陷）。

### 10.2 分布式锁

- `RedisLockUtil` / `RedisLockUtils` 提供并发操作的分布式锁，保证多实例下数据一致性。

### 10.3 事件分片串行（替代锁的轻量方案）

跨实例的强一致仍依赖 Redis 锁；**实例内的同设备并发**则由 §7.2 的 `ShardDispatcher` 哈希分片解决：

- 同 `deviceId`/`callId` 的事件被路由到同一槽，单线程消费，**无需锁**即可保证状态机正确推进；
- 修复 GB28181 协议下「`Offline→Online` 报文交错导致状态翻转」之类的乱序问题；
- 队列容量默认 2000，溢出时记日志（不阻塞 Notifier 线程）。

---

## 11. Web 层与安全

### 11.1 Controller 模板方法规范

| 优先级 | 方法 | 说明 |
|--------|------|------|
| Essential（必须） | `add`/`update`/`get`/`deleteOne`/`deleteBatch`/`getPage` | 对应 Manager 标准 CRUD |
| Optional（可选） | `createXxx`/`updateXxx`/`deleteXxx` | 含操作日志的业务方法 |
| Avoid（避免） | 状态更新/Key 更新等细粒度 | 一般不在 Controller 暴露 |

- 入参必须用 Web 层 `*Req`（禁止直用 DTO）；update 必须携带主键 ID 走 `updateById`；
- 分页响应包装为 `*ListResp`（`total` + `items`）；
- 查询采用「全量分页条件查询」，前端自由组装条件。

### 11.2 过滤器与拦截器

| 组件 | 职责 |
|------|------|
| `XssFilter` + `XssHttpServletRequestWrapper` | XSS 防护（排除 `/zlm/**`、`/index/hook/**`、Swagger） |
| `TraceFilter` | 链路追踪 ID 透传（配合 SkyWalking） |
| `RepeatableFilter` + `RepeatedlyRequestWrapper` | 请求体可重复读取 |
| `LoggerInterceptor` | 访问日志 |
| `RepeatSubmitInterceptor` + `SameUrlDataInterceptor` | 防重复提交（`@RepeatSubmit`） |

### 11.3 AOP 切面

| 切面 | 职责 |
|------|------|
| `RateLimiterAspect` | 限流（`@RateLimiter` + `LimitType`） |
| `ControllerExceptionLogAspect` | 控制器异常日志 |

### 11.4 认证与权限

- Token 机制：`Authorization` 头 + 自定义 secret，默认 30 分钟过期（`application-dev.yml` `token` 段）；
- `AuthController`：`/login`、`/logout`、`/refresh`、`/codes`（权限码）；
- RBAC：User-Role-Menu-Dept 五张表 + 两张关联表；
- 全局异常：`GlobalExceptionHandler` + `ServiceException`/`ServiceExceptionEnum`。

> 注意：ZLM Hook 接口（`/zlm/**`、`/index/hook/**`）已从 XSS 过滤排除，属内部回调端点，部署时应做网络隔离或独立鉴权。

---

## 12. 异步与线程模型

### 12.1 线程池配置（`ThreadPoolConfig`，manager 模块）

| 线程池 Bean | 配置 | 用途 |
|-------------|------|------|
| `threadPoolTaskExecutor` | core=50 / max=200 / queue=1000 | 通用后台任务 |
| `sipNotifierExecutor` | core=8 / max=32 / queue=1000，CallerRuns | **GB28181 翻译层专用**（`VoglanderBusinessNotifier.notify` 的 `@Async`） |
| `ShardDispatcher.executor` | FixedThreadPool，N=`voglander.event.shard.count`（默认 16） | **事件分片单线程槽**：每个 EventShard 一个固定线程，保证同设备串行 |

- `@EnableAsync` 开启；`sipNotifierExecutor` 满载时 CallerRuns 回退（不丢回调，但产生背压）；
- `ShardDispatcher` 用 16 个固定线程承载重活（payload 反序列化、业务编排、DB 写），与 `sipNotifierExecutor`（只做翻译）解耦；
- `AsyncManager` 处理通用后台任务；
- 消息队列：RabbitMQ（可选 RocketMQ）。

### 12.2 异步设计要点

- GB28181 `notify` 必须异步且仅做轻量翻译，否则设备 SIP 超时重传（见 §7.2）；
- 分片管线的 **重活全部在 `EventShard` 单线程槽中执行**，包括 FastJSON2 反序列化、Manager 调用、DB 写入；
- 异步/Hook/外部 HTTP 调用相关测试**不使用** `@Transactional`（事务无法跨线程回滚）。

---

## 13. 配置与部署

### 13.1 Profile 组合

`application.yml` 激活 `dev,repo,inte` 三段：

| Profile | 文件位置 | 内容 |
|---------|----------|------|
| 主配置 | `voglander-web` `application.yml` | 应用名、端口 8081、SpringDoc、XSS |
| `dev` | `voglander-web` `application-dev.yml` | token、`local.sip.*`、`local.zlm.*` 本地参数 |
| `repo` | `voglander-repository` `application-repo.yml` | dynamic-datasource、HikariCP、Redis、MyBatis-Plus |
| `inte` | `voglander-integration` `application-inte.yml` | SIP server/client、ZLM servers、**gateway 网关段** |

### 13.2 gateway 网关配置（1.8.0 新增）

```yaml
gateway:
  node-id: ${local.gateway.node-id:node-1}              # 多节点部署唯一标识
  forward-timeout-ms: 3000                               # 跨节点转发超时
  gb28181:
    store:
      type: ${...:memory}                                # memory(单机) | redis(多节点)
    invite-context-ttl-ms: 30000                         # INVITE 上下文 TTL（超时回包 410）
    invite-idempotency-window-ms: 5000                   # UDP 重传幂等窗口

# 事件分片管线（Phase 4）
voglander:
  event:
    shard:
      enabled: true        # true(默认)→主入口分片；false→回退入口直调 Dispatcher
      count: 16            # 分片数（FixedThreadPool 槽数）
```

### 13.3 数据源切换

```
默认：SQLite  (jdbc:sqlite:app.db)  — 开箱即用
生产：MySQL   — 取消 application-repo.yml 中 MySQL 注释，执行 sql/voglander.sql
```

### 13.4 部署形态

- **单机**：`store.type=memory`，单节点 `node-id`；
- **多节点集群**：`store.type=redis`，各节点唯一 `node-id`，依赖 Redis 做 INVITE 跨节点回包路由；前端经负载均衡接入。

---

## 14. 测试架构

### 14.1 测试分层策略

| 层 | 测试类型 | 注解 | 依赖处理 | 事务 |
|----|----------|------|----------|------|
| Controller | 纯单元 | `@ExtendWith(MockitoExtension.class)` | `@Mock`+`@InjectMocks` | 无 |
| Service | 纯单元 | `@ExtendWith(MockitoExtension.class)` | `@Mock`+`@InjectMocks` | 无 |
| Manager | 集成 | `@SpringBootTest` + `BaseTest` | 真实注入 + `@MockitoBean` Assembler | `@Transactional` |
| Repository | 集成 | `@SpringBootTest` + `BaseTest` | 真实数据库 | `@Transactional` |
| HTTP API | 集成 | `BaseStreamProxyIntegrationTest` | 真实 Bean | 无，手动清理 |
| 异步/Hook | 集成 | 自定义基类 | 真实 Bean | 无，手动清理 |

### 14.2 Manager 集成测试依赖分层

- 被测 Manager / 基础 `IService<DO>` Service / `CacheManager` → `@Autowired`（真实）；
- Assembler / Converter → `@MockitoBean`（用 `thenAnswer` 动态双向转换）；
- 外部集成服务 → `@MockitoBean`。

### 14.3 测试位置与数据库

- **全部测试集中**在 `voglander-web/src/test/java/io/github/lunasaw/voglander`，其他模块不放测试；
- 测试库 SQLite `test-app.db`（已提交，`schema-sqlite.sql` 非自动执行，含 `tb_media_session`）；
- Redis 集成测试通过运行时探测自动跳过（`Assumptions.assumeTrue`）。

### 14.4 覆盖率

```bash
./generate-coverage-report.sh
# 输出：voglander-coverage-report/target/site/jacoco-aggregate/index.html
```

---

## 15. 关键约束与易踩坑清单

| # | 约束 | 后果 |
|---|------|------|
| 1 | `ApplicationWeb` 必须 `@EnableSipServer` | 缺失 → `CommandStrategyFactory`/`ServerCommandSender` 无法实例化 → 整条 GB28181 事件管线静默关闭 |
| 2 | `VoglanderBusinessNotifier.notify` 必须 `@Async` 且不继承 `AbstractProtocolBusinessNotifier`；只做翻译，重活下沉到 `ShardDispatcher` | 同步或继承基类 → 设备 SIP 超时重传或 `@Async` 失效；Notifier 内做反序列化/DB 写 → SIP 线程被阻塞 |
| 3 | `Voglander*DeviceSupplier` 须标 `@Primary` | 与框架 `Default*DeviceSupplier` 冲突 → `NoUniqueBeanDefinitionException` |
| 4 | `VoglanderDeviceSessionCache` 必须存在 | `ServerCommandSender` 构造强依赖 |
| 5 | `ServerStart` 不可删 | 唯一 SIP 端口绑定处 |
| 6 | 出站命令走 `dispatchEnvelope`，禁止直调 `ServerCommandSender` | 绕过协议适配层的 payload schema 校验 |
| 7 | 白名单 handler SDP IP 字段名为 `mediaIp`（非 `sdpIp`） | payload 字段错配 → 命令下发失败 |
| 8 | integration 模块 pom 历史 `<source>9</source>` | 该模块**不可用** Java16+ 模式匹配 `instanceof X x`，须经典 cast（manager 等其他模块为 17） |
| 9 | 根 pom 中 `spring-boot-dependencies` 必须在 `sip-gateway-bom` **之前** import | 顺序颠倒 → Spring Boot 被降级到 3.3.1 → `@MockitoBean`（需 Spring 6.2+）消失，测试编译失败 |
| 10 | 所有类型转换用 FastJSON2 正反序列化 | 禁止手写字符串解析 |
| 11 | `RedisInviteContextStore` 后端故障必须抛 503 | 静默吞异常 → 跨节点回包路由错误 |
| 12 | 新增 `ServiceException` 前先在 `ServiceExceptionEnum` 登记 | 否则无标准错误码 |
| 13 | 新增协议入站：实现 `ProtocolEventHandler` 并 `@Component` 即可，**禁止改动 `ShardDispatcher`/`InboundEventDispatcher`** | 改动分片层会破坏协议解耦原则 |
| 14 | `Gb28181ProtocolHandler` 禁止 import 任何 sip-gateway 框架类型（`GatewayEvent`/`BusinessNotifier` 等） | 协议层与 gateway 产品耦合 → 后续无法独立替换 gateway |
| 15 | `voglander.event.shard.enabled` 默认 `true`，关闭仅用于灰度回退/小流量调试 | 关闭后失去同设备串行保证，可能触发状态翻转 |

---

## 16. 演进路线

### 16.1 已完成（1.0.3 基线）

- ✅ **协议接入**：GB28181 从 gb28181 1.2.5-SNAPSHOT 直连 handler 迁移到 sip-gateway 1.8.0；
- ✅ **入站统一回调**：35 种 `GatewayEvent` 全部覆盖（`VoglanderBusinessNotifier`）；
- ✅ **出站 envelope 化**：6 个业务域命令（ptz/record/device/alarm/config/media）全部经 `dispatchEnvelope`；
- ✅ **会话状态建模**：`MediaSessionManager` + `tb_media_session`；
- ✅ **多节点 INVITE 上下文**：`RedisInviteContextStore`；
- ✅ **Phase 1 延迟双删**：`DelayedCacheEviction`，修复 JVM 崩溃后脏读 TTL 窗口（A4）；
- ✅ **Phase 2a 定向更新**：`DeviceManager.patchLiveness` 仅写 status 列，消除整行读+全行写放大（P3）；
- ✅ **Phase 3 协议解耦**：`InboundEventDispatcher` + `ProtocolEventHandler` + `Gb28181ProtocolHandler`，Notifier 与 GB28181 业务解耦；
- ✅ **Phase 4 事件分片**：`ShardDispatcher` + `EventShard`，按 deviceId 哈希分片，同设备串行 + 跨设备并发；
- ✅ **Phase 8 可插拔验证**：`NoopProtocolHandler`，证明新协议接入零改动分片层；
- ✅ **灰度开关**：`voglander.event.shard.enabled` 控制主入口 / 回退入口。

### 16.2 已知 TODO / 待完善

- ZLM `onPlay`/`onPublish` 鉴权逻辑当前放行（占位），需补全播放/推流鉴权；
- Integration 模块 pom `<source>9</source>` 建议统一升至 17；
- ZLM Hook 端点（`/zlm/**`）的独立鉴权/网络隔离；
- `Response.*`（DeviceStatus/RecordInfo/PtzPosition 等 16 类）多数仅日志记录，按需补落库；
- `DeviceRegisterService` 外部系统集成已 `ResultDTO` 包装，业务侧仅需判 null（不直接处理外部错误）；
- 新协议接入（GT1078/ONVIF）落地：参考 `Gb28181ProtocolHandler` 模式新增 `ProtocolEventHandler` 实现即可，分片管线无需改动。

---

## 附录 A：缩写表

| 缩写 | 全称 | 说明 |
|------|------|------|
| GB28181 | 公共安全视频监控联网系统信息传输、交换、控制技术要求 | 中国国标 |
| SIP | Session Initiation Protocol | 会话初始化协议 |
| INVITE | SIP INVITE 方法 | 媒体点播/回放会话建立 |
| ZLM | ZLMediaKit | 流媒体服务器 |
| DO/DTO/VO/Req/Resp/QO | 见 §5.3 | 数据模型命名 |
| SSRC | Synchronization Source | RTP 同步源标识 |
| RBAC | Role-Based Access Control | 基于角色的访问控制 |

## 附录 B：参考文档

- `CLAUDE.md`（根 / voglander）— 编码规范与分层约定（权威）；
- `doc/1.0.2/SIP-GATEWAY-INTEGRATION-PLAN.md` — sip-gateway 1.8.0 接入设计稿；
- `README.md` — 项目介绍与快速开始。
