# Voglander 架构设计文档

> 版本：1.0.7 ｜ 修订日期：2026-06-12 ｜ 对应分支：`0608_dev`
> 适用代码基线：sip-gateway / gb28181 **1.8.2** + zlm **1.0.11**；协议接入层对称化（出站 SPI 化 S1 + type 枚举化 S2 + server 死代码清理 S3 + 纯协议路由键 S4 + 媒体协议层抽象 S5 + 节点故障转移/会话状态机 S6）落地完成
>
> 本文取代 `doc/1.0.3/ARCHITECTURE.md`（历史快照，基线 `dev_merge_sip`）。1.0.3 在出站命令通道、协议路由、媒体选路三处已被本版本改动覆盖，仅保留作历史参考。

---

## 目录

1. [文档目的与范围](#1-文档目的与范围)
2. [系统概述](#2-系统概述)
3. [技术栈](#3-技术栈)
4. [模块架构](#4-模块架构)
5. [分层架构](#5-分层架构)
6. [核心设计模式](#6-核心设计模式)
7. [GB28181 集成架构（sip-gateway 1.8.2）](#7-gb28181-集成架构sip-gateway-182)
8. [协议接入层通用化（S1~S4 对称化）](#8-协议接入层通用化s1s4-对称化)
9. [媒体流集成与协议选路（S5）](#9-媒体流集成与协议选路s5)
10. [会话状态机与节点故障转移（S6）](#10-会话状态机与节点故障转移s6)
11. [命令亲和路由（多节点）](#11-命令亲和路由多节点)
12. [数据模型](#12-数据模型)
13. [缓存与并发](#13-缓存与并发)
14. [Web 层与安全](#14-web-层与安全)
15. [异步与线程模型](#15-异步与线程模型)
16. [配置与部署](#16-配置与部署)
17. [测试架构](#17-测试架构)
18. [关键约束与易踩坑清单](#18-关键约束与易踩坑清单)
19. [演进路线](#19-演进路线)

---

## 1. 文档目的与范围

本文件描述 Voglander 视频监控平台后端的**当前实际架构**（以 `0608_dev` 源码逐项核验），重点覆盖：

- 多模块工程的依赖关系与分层职责边界；
- 四大核心设计模式（Assembler / Manager 模板 / Wrapper / Template）；
- GB28181 协议「出站命令 envelope + 入站统一回调」双通道，以及其上的**协议接入层对称化**（出站路由从硬编码 if 进化为 SPI 表驱动，对称入站分发）；
- 媒体流（ZLM）代理、推流、Hook 闭环，以及**媒体协议选路抽象**（为 ONVIF/RTSP 预留）；
- 会话状态机、节点故障转移、命令亲和路由等可靠性补强；
- 数据模型、缓存、并发、安全、异步、测试等横切面。

文档以**代码现状**为准，不是规划稿。规划稿见 `doc/1.0.7/*-PLAN.md`。前端（vue-vben-admin）与 sip-proxy 框架本身不在本文范围，仅在交互边界处提及。

---

## 2. 系统概述

Voglander 是基于 **Spring Boot 3.5.3 + Java 17** 的企业级视频监控平台，对接多种视频监控协议（GB28181 已落地；ONVIF/GT1078/RTSP 为骨架预留），提供设备接入、实时点播/回放、媒体流转发与录制、设备状态监控、权限管理等能力。

### 2.1 系统上下文

```
                          ┌───────────────────────────┐
                          │   前端 (vue-vben-admin)    │
                          │   管理后台 / 监控大屏      │
                          └─────────────┬─────────────┘
                                        │ HTTP/REST (AjaxResult)
                                        ▼
        ┌───────────────────────────────────────────────────────────┐
        │                    Voglander 后端 (本工程)                  ���
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
| **GB28181 平台出站指令** | REST/业务调用 | `DeviceAgreementService`(SPI 选协议服务) → `VoglanderServer*Command` → `dispatchEnvelope` → `CommandHandlerRegistry` → SIP 下发 | 设备执行（PTZ/点播/回放/配置） |
| **媒体流代理** | 前端 `/zlm/api/*` | `StreamProxyController` → `StreamProxyManager` → `StreamProxyZlmWrapperService` | ZLM 拉流 + Hook 回写状态 |

---

## 3. 技术栈

### 3.1 核心框架

| 类别 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | 全工程 `jakarta.*`；**注意** integration 模块 pom 仍有历史 `<source>9</source>` 约束（详见 §18） |
| 基础框架 | Spring Boot | 3.5.3 | 自动配置、Web、AOP、Async |
| ORM | MyBatis-Plus | 3.5.5 | `IService` 基础 CRUD + `LambdaQueryWrapper` |
| 多数据源 | dynamic-datasource | 4.3.1 | `master` 默认源 |
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
| SIP 网关 | sip-gateway-spring-boot-starter | **1.8.2** | GB28181 接入主框架 |
| GB28181 协议 | gb28181 client/server | **1.8.2** | 设备端/平台端协议栈 |
| 媒体服务 | ZLMediaKit-Starter | **1.0.11** | 拉流/推流/录制/Hook |
| Excel | EasyExcel | 4.0.1 | 导入导出 |
| JSON | FastJSON2 | — | **唯一**序列化方案，禁用 Jackson/Gson |

> 版本以根 `pom.xml` 为准：`sip-gateway.version=1.8.2`、`gb28181-proxy.version=1.8.2`、`zlm.version=1.0.11`。

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
├── voglander-manager/         # 业务编排：Manager 模板、Service、Assembler、异步、线程池、事件分片、节点路由
├── voglander-service/         # 领域服务：登录注册、流业务、设备协议、媒体协议选路
├── voglander-integration/     # 外部集成：GB28181、ZLM、EasyExcel、IP、消息追踪
├── voglander-web/             # 应用主模块：Controller、过滤器、拦截器、启动类、全部测试
├── voglander-test/            # 测试配置与多环境 profile
└── voglander-coverage-report/ # JaCoCo 跨模块聚合覆盖率
```

### 4.2 模块依赖方向（经 pom 核验，无环）

```
                 voglander-common  (基础，无依赖)
                        ▲
                 voglander-client
                        ▲
                 voglander-repository
                        ▲
                 voglander-manager
            ┌───────────┴───────────┐
   voglander-integration            │   (integration → manager, client, common)
            ▲                        │
   voglander-service ────────────────┘   (service → manager, integration, client, common)
            ▲
   voglander-web  (web → service, integration)
```

**关键依赖方向**：
- `integration → manager`：集成层（如 `VoglanderBusinessNotifier`）回调时直接调用 Manager 完成落库；
- `manager → repository`：Manager 只依赖 repository，不依赖 integration（避免环）；
- `service → manager + integration`：领域服务（如 `MediaPlayServiceImpl`）编排媒体协议 handler，handler 在 service 层、调 integration 的 command/ZLM；
- `web` 是唯一可执行模块，含 `ApplicationWeb` 启动类与**全部测试代码**。

### 4.3 模块职责矩阵

| 模块 | 核心职责 | 关键产物（含本版本新增） |
|------|----------|----------|
| common | 跨层共享，零业务依赖 | `DeviceConstant`、`MediaSessionConstant`、`DeviceProtocolEnum`、`DeviceAgreementEnum`、**`constant/protocol/ProtocolConstants`**、**`enums/MediaSessionStatusMachine`**、`ServiceException` |
| client | 服务契约与传输模型 | **`DeviceCommandService`（含 `supportProtocols()`）**、`DeviceRegisterService`、`DeviceEvent`（协议无关事件模型） |
| repository | 持久化与缓存 | `*DO`、`*Mapper`、`RedisCache`、`RedisLockUtil`、`@Cached`/`CachedAspect` |
| manager | 业务编排 + 模板方法 + 事件分片 + **节点路由** | `*Manager`、`event/{ShardDispatcher,InboundEventDispatcher,ProtocolEventHandler}`、**`routing/{DeviceNodeRouteService,NodeAliveService,InternalCommandForwardService}`**、`cache/DelayedCacheEviction`、`AsyncManager`、`ThreadPoolConfig` |
| service | 领域服务 + **媒体协议选路** | `DeviceRegisterServiceImpl`、**`command/DeviceAgreementService`（SPI 路由）**、**`command/impl/GbDeviceCommandService`**、**`live/protocol/{MediaProtocolHandler,MediaProtocolRouter,...}`**、`live/impl/MediaPlayServiceImpl` |
| integration | 外部系统包装 | GB28181 notifier/handler/command/store/supplier、**`gb28181/command/Gb28181CommandType`**、**`gb28181/trace/SipMessageTracer`**、ZLM wrapper/hook、EasyExcel |
| web | REST 入口 | `*Controller`、XSS/Trace/Repeat 过滤器、**`InternalAuthFilter`**、**`api/internal/InternalCommandController`**、`WebAssembler`、`ApplicationWeb` |

---

## 5. 分层架构

### 5.1 各层职责边界

| 层 | 入参类型 | 出参类型 | 允许的职责 | 禁止的职责 |
|----|----------|----------|-----------|-----------|
| Web | `*Req` | `AjaxResult<VO>` | 参数校验 `@Valid`、格式转换、Swagger 文档 | 业务逻辑、直接访问 DO |
| Manager | `*DTO` | `*DTO` / `Page<DTO>` | 多服务编排、缓存一致性、事务、DTO↔DO 转换 | 暴露 DO（历史方法 `@Deprecated`） |
| Service | `*DTO`（主）/ 基本类型（便利） | 领域对象 | 单一职责业务、`Assert` 校验、`ServiceException` | — |
| Repository | DO / 主键 | DO | 持久化、`@Cached` 单对象缓存 | 跨表复杂编排 |
| Integration | 外部模型 | `ResultDTO<T>` | 参数校验、异常捕获、结果包装 | 业务逻辑（委托 Service/Manager） |

### 5.2 数据模型命名规约

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
- **类型转换铁律**：所有类型转换通过 **FastJSON2 正反序列化**完成：
  ```java
  TargetType target = JSON.parseObject(JSON.toJSONString(source), TargetType.class);
  ```

### 6.2 Manager 模板方法模式

每个 `*Manager` 实现一组标准模板方法：
```
add(DTO) / update(queryDTO, updateDTO) / updateById(id, updateDTO)
get(DTO) / deleteOne(DTO) / deleteBatch(DTO)
getPage(DTO, page, size)              → 默认 createTime 降序
clearCache(id, oldKey, newKey)        → 主键 + 业务键双清
```
**LambdaQueryWrapper 强制规范**：必须用带 `condition` 参数的重载，禁止手写 `if (x!=null)` 拼条件，禁止 `new LambdaQueryWrapper<>(entity)`（null 字段污染查询）。

### 6.3 Wrapper 模式（外部系统集成）

Integration 层所有外部调用统一返回 `ResultDTO<T>`，经 `WrapperExceptionHandler.executeWithExceptionHandling(...)` 模板：参数校验 → 业务调用 → 结果校验（失败 null）→ 成功返回模型。Wrapper 只做校验与异常捕获，业务逻辑委托 Service。

### 6.4 Template 模式（统一内部方法）

数据修改统一走 Manager 内部方法入口，保证缓存失效与日志一致；UNIQUE 约束场景在 `saveOrUpdate` 前先按业务主键（如 `app+stream`、`callId`）查询既有记录。

### 6.5 SPI 表驱动模式（本版本核心，§8 详述）

入站分发与出站路由、媒体选路统一采用「构造注入 `List<T>` → 按声明能力折叠成 `Map<key,T>`」的 SPI 模式：新增协议只需 `@Component`/`@Service` 一个新实现，路由表自动收编，既有路由代码零改动。落地于：`InboundEventDispatcher`（入站）、`DeviceAgreementService`（出站命令）、`MediaProtocolRouter`（媒体选路）。

### 6.6 响应协议

| 场景 | 返回类型 | 取错误信息 |
|------|----------|-----------|
| Web 层 REST | `AjaxResult<T>`（成功 `code=0`） | `getMsg()` |
| Integration 外部集成 | `ResultDTO<T>` | `getMessage()` |

---
## 7. GB28181 集成架构（sip-gateway 1.8.2）

GB28181 采用「**出站命令 envelope + 入站统一回调**」双通道。`wrapper/gb28181/` 下不再有任何 `*RequestHandler`/`*ResponseHandler`。

### 7.1 包结构

```
intergration/wrapper/gb28181/
├── notifier/
│   ├── VoglanderBusinessNotifier          # 主入口（implements BusinessNotifier，@Async("sipNotifierExecutor")）
│   │                                      # 翻译 GatewayEvent → DeviceEvent，提交 ShardDispatcher
│   └── VoglanderBusinessNotifierFallback  # 灰度回退入口（shard.enabled=false 激活，直调 Dispatcher）
├── handler/
│   └── Gb28181ProtocolHandler             # GB28181 协议处理器（implements ProtocolEventHandler）
│                                          # 不依赖任何 sip-gateway 类型
├── server/command/                        # 平台端出站命令（envelope 化）
│   ├── AbstractVoglanderServerCommand     # 抽象基类：仅 dispatchEnvelope / dispatchEnvelopeWithCallId + 校验
│   └── ptz/ record/ device/ alarm/ config/ media/   # 6 个业务域
├── client/command/                        # 设备端出站命令（仍用静态 ClientCommandSender）
│   ├── AbstractVoglanderClientCommand
│   └── ptz/ record/ device/ alarm/ catalog/ status/
├── command/
│   └── Gb28181CommandType                 # 出站命令 type 枚举（19 种，单一事实源，S2）
├── trace/
│   └── SipMessageTracer                   # 出站/入站消息链路追踪（独立 logger）
├── supplier/
│   ├── VoglanderServerDeviceSupplier      # @Primary，按 deviceId 提供 ToDevice
│   ├── VoglanderClientDeviceSupplier      # @Primary
│   └── VoglanderDeviceSessionCache        # 实现框架 DeviceSessionCache（ServerCommandSender 强依赖）
├── store/
│   └── RedisInviteContextStore            # 多节点 INVITE 上下文（@ConditionalOnProperty）
├── start/ServerStart                      # CommandLineRunner，绑定 SIP 监听端口
└── config/properties/                     # VoglanderSipServerProperties / VoglanderSipClientProperties
```

事件分片管线在 manager 模块（**与协议解耦**）：
```
voglander-manager/event/
├── ShardDispatcher        # 按 shardKey 哈希到 N 个槽（默认 16），单槽单线程串行
├── EventShard             # 单分片：BlockingQueue + 单线程消费 → 调 InboundEventDispatcher
├── InboundEventDispatcher # 按 event.protocol() 路由到对应 ProtocolEventHandler（SPI 折叠）
├── ProtocolEventHandler   # 协议处理器接口（protocol() + handle(DeviceEvent)）
└── NoopProtocolHandler    # 可插拔验证 / 协议降级
```

### 7.2 入站：四层事件管线

```
设备 SIP 报文
   ▼ sip-gateway 协议栈 → Forwarder（发出 35 种 GatewayEvent）
   ▼ ① 框架回调
VoglanderBusinessNotifier.notify(GatewayEvent)   @Async("sipNotifierExecutor")
   │   仅做轻量翻译（O(1) 切分 + 构造 record），payload 保持 Map 引用不反序列化
   │   入站链路追踪：SipMessageTracer.recv(type, deviceId, callId, nodeId, body)
   │   产物：DeviceEvent(protocol, group, name, deviceId, correlationId, ts, payload:Map, nodeId)
   ▼ ② 分片调度
ShardDispatcher.dispatch(DeviceEvent)
   │   shardKey = deviceId 优先，null 退回 correlationId；哈希到 N 个 EventShard（默认 16）
   │   每槽 BlockingQueue(2000) + 单线程消费 → 同设备/同会话串行，跨设备并发
   ��� ③ 协议路由
InboundEventDispatcher.dispatch(DeviceEvent)
   │   按 event.protocol() 路由；未注册协议安全丢弃（log.warn）
   ▼ ④ 协议处理
Gb28181ProtocolHandler.handle(DeviceEvent)
   │   按 groupName（"Lifecycle.Register" 等）switch；payload 此时才 FastJSON2 反序列化
   ▼ 业务调用
   ├── Lifecycle.Register          → DeviceRegisterService.login(...)
   ├── Lifecycle.Online            → DeviceManager.patchLiveness(deviceId, ONLINE, null)
   ├── Lifecycle.Offline           → DeviceRegisterService.offline(deviceId)
   ├── Notify.Keepalive            → DeviceRegisterService.keepalive(deviceId)
   ├── Notify.MediaStatus          → MediaSessionManager.onMediaStatus(deviceId, type)
   ├── Response.Catalog            → 逐 DeviceItem → DeviceRegisterService.addChannel(...)
   ├── Session.InviteOk            → MediaSessionManager.onInviteOk(callId, deviceId)
   ├── Session.InviteFailure       → MediaSessionManager.onInviteFailure(callId, statusCode)
   ├── Session.Ack                 → MediaSessionManager.onAck(callId)
   └── Session.Bye                 → MediaSessionManager.onBye(deviceId)
```

**关键约束**：
- **SIP 线程零阻塞**：`notify` 必须 `@Async` 且仅做翻译，重活下沉到分片槽。同步处理或在 Notifier 里做反序列化/DB 写 → 设备 SIP 超时重传。
- **禁止继承 `AbstractProtocolBusinessNotifier`**：其 `notify()` 为 `final` 且内部自调用，`@Async` 代理失效。本工程直接 `implements BusinessNotifier`。
- **同设备串行**：`ShardDispatcher` 用 `deviceId` 哈希分片，修复乱序导致的状态翻转（如 Offline→Online 错序写）。
- **协议解耦**：`Gb28181ProtocolHandler` 不 import `GatewayEvent`/`BusinessNotifier` 等任何 sip-gateway 类型；新增协议只需新增 `ProtocolEventHandler` 实现，分片管线零改动。

### 7.3 出站：命令 envelope 通道

平台端命令统一经 `CommandHandlerRegistry`，**基类只余 envelope 双通道**（`serverCommandSender` 字段与 `executeCommand()` 过渡死代码已在 S3 全量删除）：

```
业务调用 VoglanderServerMediaCommand.inviteRealTimePlayWithCallId(deviceId, sdpIp, port, mode)
   ▼  AbstractVoglanderServerCommand.dispatchEnvelope(type, deviceId, payload)
   │   ── 命令亲和路由判断（开关开启时，§11）：
   │      lookupNode(deviceId) → 目标非本节点且 alive → internalCommandForwardService.forward()
   │   ── 本地处理：
   │   1. 构造 GatewayCommand(type, deviceId, payload, requestId=null)
   │   2. commandHandlerRegistry.require(type)   // type 不存在 → 404
   │   3. handler.handle(cmd) → GatewayCommandResult.correlationId()(sn/callId)
   │   4. SipMessageTracer.send(type, deviceId, callId, payload)  // 出站链路追踪
   │   5. 异常 → ResultDTOUtils.failure
   ▼ 协议适配层 (Gb28181WhitelistHandlers / Gb28181CommandSpecs) 按 type 反查 payload schema → SIP 下发
```

两个出站模板方法：
- `dispatchEnvelope(type, deviceId, payload)` → `ResultDTO<Void>`
- `dispatchEnvelopeWithCallId(type, deviceId, payload)` → `ResultDTO<String>`（data 为 callId）

出站 type 全部来自 **`Gb28181CommandType` 枚举**（单一事实源，见 §8.2），子类调 `Gb28181CommandType.XXX.type()`。

> 客户端命令 `ClientCommandSender` 保持 `@Component` + 静态方法，client 端基类 `AbstractVoglanderClientCommand` **不在 S3 清理范围**（仍依赖 `executeCommand`/静态 sender）。

### 7.4 会话状态管理 `MediaSessionManager`

INVITE 点播/回放会话状态独立建模：
- 实体 `MediaSessionDO`（表 `tb_media_session`），业务主键 **SIP callId**（UNIQUE）；
- 状态 `MediaSessionConstant.Status`：`CLOSED=0` / `ACTIVE=1` / `INVITING=2`（默认）/ `FAILED=3`；
- 由 `Session.*` 与 `Notify.MediaStatus` 事件驱动：`onInviteOk`/`onInviteFailure`/`onAck`/`onBye`/`onMediaStatus`；
- **状态转移合法性由 `MediaSessionStatusMachine` 守卫**（S6，见 §10）；
- **INVITE callId 关联**：`startLive` 发 INVITE 时经 `inviteRealTimePlayWithCallId` **同步返回真实 SIP Call-ID**，即刻 `backfillCallIdByStreamId` 回填占位会话行，解决关流 `DialogNotFoundException`。

### 7.5 多节点 INVITE 上下文 `RedisInviteContextStore`

```
@ConditionalOnProperty(gateway.gb28181.store.type=redis)
key = sip:invite:ctx:{callId}  →  InviteContext(nodeId, ctxKey)  [FastJSON2, TTL]
```
支撑跨节点 INVITE 回包路由；单机默认走框架 `InMemoryInviteContextStore`（`type=memory`）。契约：`find()==null` → 回包 410；Redis 后端故障**必须抛 503**，不可静默吞。

### 7.6 启动装配（强约束）

| 组件 | 必要性 | 原因 |
|------|--------|------|
| `ApplicationWeb` 上 `@EnableSipServer` | **必需** | `@Import` 了 `Gb28181CommonAutoConfig`（提供 `CommandStrategyFactory`）等；不加则 `ServerCommandSender`/`ClientCommandSender` 无法实例化，`Gb28181GatewayAutoConfiguration`（`@ConditionalOnBean(ServerCommandSender)`）静默关闭事件管线 |
| `VoglanderDeviceSessionCache` | **必需** | 实现框架 `DeviceSessionCache`，`ServerCommandSender` 构造强依赖 |
| `Voglander*DeviceSupplier` 标 `@Primary` | **必需** | 与框架 `Default*DeviceSupplier` 冲突 → `NoUniqueBeanDefinitionException` |
| `ServerStart`（CommandLineRunner） | **不可删** | 唯一 SIP 端口绑定处；须用 `sipLayer.addListeningPoint(ip, port, sipListener, enableLog)` |
| `ShardDispatcher` + `EventShard` 池 | **必需**（默认启用） | `voglander.event.shard.enabled=true` 时主入口依赖 |

## 8. 协议接入层通用化（S1~S4 对称化）

本版本最大的结构性改进：**出站命令路由从硬编码 if 进化为 SPI 表驱动，与入站分发对称**。目标是让「新增一个协议」从"改老代码"变成"加新实现"。

### 8.1 出站命令 SPI 路由（S1）

`DeviceAgreementService` 重写为构造注入折叠的路由表，复刻入站 `InboundEventDispatcher`：

```java
// voglander-service/.../command/DeviceAgreementService.java
@Service
public class DeviceAgreementService {
    private final Map<Integer, DeviceCommandService> routing;  // 纯协议 type → 命令服务

    public DeviceAgreementService(List<DeviceCommandService> services) {
        // 按各实现的 supportProtocols() 折叠；同协议多实现 → 启动报错
    }
    public DeviceCommandService getCommandService(Integer protocolType) { ... }
}
```

- `DeviceCommandService` 接口（voglander-client）含 **`Set<Integer> supportProtocols()` 抽象方法**（强制每个实现显式声明，避免新协议忘声明导致静默无法路由），外加 11 个协议无关方法：`queryChannel`/`queryDevice`/`queryDeviceInfo`/`queryCatalog`/`ptzControl`/`startPlay`/`startPlayback`/`stopPlay`/`reboot`/`controlPlayback`。
- 唯一实现 `GbDeviceCommandService`：`supportProtocols()` 返回 `Set.of(DeviceProtocolEnum.GB28181.getType())`（即 `{1}`）。
- **新增协议成本**：新增一个 `OnvifDeviceCommandService implements DeviceCommandService` 声明 `supportProtocols()=ONVIF` 即被路由命中，`DeviceAgreementService` **零改动**。

### 8.2 命令 type 枚举化（S2）

出站 19 种 type 收敛为单一事实源 `Gb28181CommandType` 枚举（voglander-integration/.../gb28181/command/）。`type()` 返回完整三段式 `gb28181.Group.Name`（构造期由 `ProtocolConstants.GB28181 + "." + groupName` 拼接）：

| 分组 | 枚举项 → type |
|------|--------------|
| Query (7) | `QUERY_DEVICE_INFO`→`gb28181.Query.DeviceInfo`、`QUERY_DEVICE_STATUS`→`...DeviceStatus`、`QUERY_CATALOG`→`...Catalog`、`QUERY_PRESET`→`...PresetQuery`、`QUERY_MOBILE_POSITION`→`...MobilePosition`、`QUERY_ALARM`→`...AlarmQuery`、`QUERY_RECORD_INFO`→`...RecordInfo` |
| Control (4) | `CONTROL_PTZ`→`gb28181.Control.Ptz`、`CONTROL_REBOOT`→`...Reboot`、`CONTROL_RECORD`→`...Record`、`CONTROL_ALARM_RESET`→`...AlarmReset` |
| Invite (5) | `INVITE_PLAY`→`gb28181.Invite.Play`、`INVITE_PLAYBACK`→`...Playback`、`INVITE_PLAYBACK_CONTROL`→`...PlaybackControl`、`INVITE_ACK`→`...Ack`、`INVITE_BYE`→`...Bye` |
| Config (2) | `CONFIG_BASIC_PARAM`→`gb28181.Config.BasicParam`、`CONFIG_DOWNLOAD`→`...ConfigDownload` |
| Device (1) | `DEVICE_BROADCAST`→`gb28181.Device.Broadcast` |

- **协议名常量**：`ProtocolConstants.GB28181 = "gb28181"`（voglander-common/.../constant/protocol/），用作三段式前缀段、`ProtocolEventHandler.protocol()` 返回值的单一引用。
- **关键事实**：出站 type 是传给框架 `CommandHandlerRegistry.require(type)` 的注册键；框架以私有字面值注册、不暴露 public 常量，故 voglander 自建枚举是唯一选项——`Gb28181CommandTypeTest` 逐字冻结 19 个 type 防漂移。
- **入站 case 不枚举化**：入站 `Gb28181ProtocolHandler` 的 switch case 是两段式（`Lifecycle.Register`，无 `gb28181` 前缀）且 Java case 须编译期常量、不能引用枚举方法，故保持 String 不动，行为等价。
- 验收：**代码级** `"gb28181"` 引用仅剩 `ProtocolConstants` 一处（其余命中均为 javadoc 注释里的示例文本）。

### 8.3 server 端死代码清理（S3）

`AbstractVoglanderServerCommand` 删除 `serverCommandSender` 字段 + `executeCommand()`×3 重载 + 2 个 `@FunctionalInterface` + 无用 import。基类只余 `dispatchEnvelope` / `dispatchEnvelopeWithCallId` + 校验工具方法 + 亲和路由依赖（§11）。client 端基类不动。

### 8.4 路由键统一为纯协议（S4）

两套协议枚举的职责厘清：

| 枚举（voglander-common） | 取值 | 用途 |
|------|------|------|
| `DeviceProtocolEnum` | GB28181(1)/ONVIF(2)/RTSP(3)/HTTP(4)/RTMP(5)/PRIVATE(6) | **纯协议**（路由键） |
| `DeviceAgreementEnum` | GB28181_IPC(1,subType=1,protocol=1)/GB28181_NVR(2,subType=2,protocol=1)/ONVIF_IPC(3,subType=1,protocol=2) | 协议×设备型态（展示/分类） |

- 路由维度统一为「纯协议 `DeviceProtocolEnum`」；`getCommandService(Integer protocolType)` 入参为纯协议 type。
- 「协议×型态」→ 纯协议的折算由调用方完成：`DeviceAgreementEnum.getByType(dto.getType()).getProtocol()`（如 `ONVIF_IPC.getProtocol() → 2`），折算上移到 `DeviceRegisterServiceImpl.login`。
- GB28181_IPC / GB28181_NVR 都折算到 protocol=1，路由到同一 `GbDeviceCommandService`。

### 8.5 新增协议成本对照（以 ONVIF 为例）

| 改动项 | 改造前 | 改造后（S1~S5 落地） |
|--------|--------|---------------------|
| 出站路由 `DeviceAgreementService` | 必须加 else if + Bean 名常量 🔴 | **零改动**（自动注册） ✅ |
| 出站命令实现 | 新增 `OnvifDeviceCommandService` | 同（必要工作） |
| 命令 type 定义 | 裸字符串散落 | 新增 `OnvifCommandType` 枚举集中 ✅ |
| 入站 handler | 新增 `OnvifProtocolHandler`（零改动注册） ✅ | 同 |
| 媒体选路 | 改 `MediaPlayService` if 分支 | 新增 `OnvifMediaProtocolHandler` ✅ |

结论：命令、事件、媒体三层均为「加新实现、改老代码 0 处」。

---

## 9. 媒体流集成与协议选路（S5）

### 9.1 ZLM 拉流代理闭环

```
前端 → StreamProxyController (/zlm/api/proxy/add)
   ▼ WebAssembler: Req → StreamProxyDTO
StreamProxyManager.add/createStreamProxy        ← 落库 tb_stream_proxy
   ▼
StreamProxyZlmWrapperService.addStreamProxy     ← 调 ZLM HTTP API
   ▼ ZLMediaKit 创建拉流
   ▼ Hook 回调
VoglanderZlmHookServiceImpl.onStreamChanged     ← 回写在线状态/流信息
```

`VoglanderZlmHookServiceImpl` 覆盖 ZLM 全量回调：`onServerKeepLive`（节点心跳）/`onPlay`（播放鉴权，当前放行 TODO）/`onPublish`/`onStreamChanged`/`onStreamNoneReader`（无人观看触发关流）/`onStreamNotFound`/`onFlowReport`/`onRecordMp4`/`onRtspRealm` 等。节点管理：`MediaNodeDO`（`tb_media_node`，`server_id` UNIQUE + `weight` 负载权重），`VoglanderNodeSupplier` 负责节点选择。

### 9.2 媒体协议选路抽象（S5）

GB28181 点播走 SIP INVITE，ONVIF/RTSP 走 HTTP `StreamProxy`，差异巨大。S5 引入**收窄抽象**：只抽象协议特定的建流/拆流，协议无关编排（选节点、引用计数、占位会话、首播 future、GC、关流去重）留在 `MediaPlayServiceImpl`。

```
voglander-service/.../live/protocol/
├── MediaProtocolHandler          # 接口：supportProtocols() + establish(ctx) + terminate(ctx)
├── MediaProtocolRouter           # SPI 折叠 List<handler>；getHandler(protocol) / resolveForDevice(deviceId)
├── MediaEstablishContext         # 建流上下文：node, streamId, deviceId, channelId, streamMode
├── MediaEstablishResult          # 建流结果：success, callId, sdpIp, rtpPort, failReason
├── MediaTerminateContext         # 拆流上下文：node, streamId, callId, reason
└── impl/Gb28181MediaProtocolHandler   # GB28181 实现
```

- `MediaProtocolHandler.establish(ctx)`：**只下发指令、不等待流就绪**。GB28181 实现 = `resolveMediaIp(node)`（优先 `tb_media_node.extend.mediaIp` NAT 覆盖）+ `openRtpServer`（port=0 自动分配）+ `inviteRealTimePlayWithCallId`（同步��回真实 SIP Call-ID）。
- `MediaProtocolHandler.terminate(ctx)`：须**幂等**。GB28181 实现 = `closeRtpServer` + `sendBye`，缺字段时跳过对应步骤而非报错。
- `MediaProtocolRouter.resolveForDevice(deviceId)`：查设备 → `DeviceAgreementEnum.getProtocol()` 折算纯协议 → 路由表取 handler（与 §8.1 同构）。
- `MediaPlayServiceImpl` 三处接线 handler：
  1. `startLive`：`resolveForDevice` → `handler.establish(ctx)` → 即刻回填 callId → 等流就绪 future；
  2. `startLive` 建流失败：`cleanupFailed()` → `handler.terminate(...)` 回滚；
  3. `closeStream/doCloseStream`：`resolveForDevice` → `handler.terminate(...)` 正常关流。
- **新增 RTSP 仅加一个 handler**，`MediaProtocolRouter` 与 `MediaPlayServiceImpl` 零改动。
- 注：`LiveStartDTO` 字段未改（寻址仍 `deviceId`/`channelId`），协议由 router 按 deviceId 查设备解析，无需 DTO 携带。

## 10. 会话状态机与节点故障转移（S6）

### 10.1 会话状态机 `MediaSessionStatusMachine`

`MediaSessionManager` 此前允许任意状态互转（如 `CLOSED→ACTIVE` 复活）。S6 补一张合法转移表（voglander-common/.../enums/`MediaSessionStatusMachine`）：

```
INVITING → {ACTIVE, FAILED, CLOSED}
ACTIVE   → {ACTIVE, CLOSED, FAILED}
FAILED   → {CLOSED}
CLOSED   → {CLOSED}
```

- **通用收口**：任意状态 → `CLOSED` 始终合法（关流不受阻）。
- **非法转移**（`isLegal` 拒绝）：`CLOSED → {INVITING,ACTIVE,FAILED}`（终态不可复活/回退）、`FAILED → {INVITING,ACTIVE}`、null 源/目标。
- **守卫点**：`onInviteOk` / `onAck` 在转 ACTIVE 前调 `MediaSessionStatusMachine.isLegal(existing.getStatus(), ACTIVE)`，非法则 `log.warn` 并返回原 ID、不执行转移（拒绝终态复活）；`onInviteFailure`（→FAILED）、`onBye`（→CLOSED）在允许列表内。
- **clearCache 收敛**：缓存清理收敛到单一出口 `clearCache()`（基于 Optional + ifPresent 的幂等设计），由 `add`/`update`(经 `applyUpdate`)/`deleteOne`/`onBye`/`markTimeoutInvitingAsFailed` 调用。

### 10.2 节点故障转移（媒体选节点）

`MediaPlayServiceImpl` 此前选节点为 null 时直接抛异常、默认硬编码 `servers.get(0)`。S6 补按 weight 降序兜底：

- `selectNode()`：主选调 `nodeService.selectNode()`（负载均衡）；主选不可用则从候选 enabled 节点按 `weight` 降序兜底；全不可用才返回 null。
- `chooseNode(primary, candidates)`：抽出的**纯逻辑**（可单测）——主选命中即用，否则按 weight 降序取首个 enabled，全不可用返回 null。
- 效果：单节点故障不再直接打断点播。

---

## 11. 命令亲和路由（多节点）

> 这是 1.0.3 文档未覆盖的新机制。用于多节点部署时把出站命令转发到**设备实际注册所在的节点**（会话亲和），由 `voglander.command.affinity-route.enabled=true` 整体开关控制（默认 `false`，关闭时三个服务都不加载，`AbstractVoglanderServerCommand` 中以 `@Autowired(required=false)` 容忍缺失）。

### 11.1 组件（voglander-manager/routing/）

| 组件 | 职责 | 关键语义 |
|------|------|---------|
| `DeviceNodeRouteService` | 维护 deviceId → nodeId 的 Redis 映射 | `registerDevice`(TTL 60s) / `renewDevice`(保活) / `lookupNode`(查所属节点，未注册返 null) / `clear`；key=`dev:node:{deviceId}` |
| `NodeAliveService` | 节点心跳与活性检测 | `heartbeat()`(fixedRate 5s 写 Redis，TTL 15s) / `getLocalNodeId()`(来自 `gateway.node-id`，默认 node-1) / `isAlive(nodeId)`(Redis key 存在性)；key=`node:alive:{nodeId}` |
| `InternalCommandForwardService` | 跨节点 HTTP 命令转发（HMAC 签名） | `forward(targetNodeId, envelope)`：查 `gateway.nodes` 取地址 → POST `/internal/sip/command` → HMAC-SHA256 头 + 重试 3 次 |

### 11.2 转发流程

```
AbstractVoglanderServerCommand.dispatchEnvelope(type, deviceId, payload)
   │ 路由判断（三个服务都注入 && deviceId != null）：
   │   target = deviceNodeRouteService.lookupNode(deviceId)
   │   if (target != null && target != localNodeId && isAlive(target))
   │       → internalCommandForwardService.forward(target, cmd)   // 转发到目标节点
   │   else
   │       → 本地 commandHandlerRegistry.require(type).handle(cmd) // 本地下发
   ▼
[目标节点] InternalCommandController.handleCommand()  (POST /internal/sip/command)
   │ 解析 type/deviceId/payload → 本地 commandHandlerRegistry.require(type).handle(cmd)
   ▲
[鉴权] InternalAuthFilter（拦截 /internal/**）
     IP 白名单 + 时间戳 ±60s 容差 + HMAC-SHA256(secret, nodeId+":"+ts) 校验
```

### 11.3 配置项

| 配置项 | 默认值 | 用途 |
|--------|--------|------|
| `voglander.command.affinity-route.enabled` | `false` | 启用整套亲和路由 |
| `gateway.node-id` | `node-1` | 本节点唯一标识 |
| `gateway.nodes` | `{}` | 节点地址映射（nodeId → host:port） |
| `gateway.internal-auth.shared-secret` | `CHANGE_ME_IN_PROD` | 跨节点 HMAC 秘钥（生产必改） |
| `gateway.internal-auth.allowed-ips` | `127.0.0.1` | 内部请求 IP 白名单 |

## 12. 数据模型

### 12.1 实体清单（14 个 DO / 15 个 Mapper）

| 领域 | 实体 | 表 | 说明 |
|------|------|----|----|
| 设备 | `DeviceDO` | `tb_device` | 设备主表 |
| 设备 | `DeviceChannelDO` | `tb_device_channel` | 设备通道 |
| 设备 | `DeviceConfigDO` | `tb_device_config` | 设备配置 |
| 媒体 | `MediaNodeDO` | `tb_media_node` | ZLM 节点 |
| 媒体 | `MediaSessionDO` | `tb_media_session` | INVITE 会话状态 |
| 媒体 | `StreamProxyDO` | `tb_stream_proxy` | 拉流代理 |
| 媒体 | `PushProxyDO` | `tb_push_proxy` | 推流代理 |
| 任务 | `ExportTaskDO` | `tb_export_task` | 导出任务 |
| 权限 | `UserDO`/`RoleDO`/`MenuDO`/`DeptDO` | — | RBAC 主体 |
| 权限 | `UserRoleDO`/`RoleMenuDO` | — | RBAC 关联 |

### 12.2 统一字段约定

| 字段 | 类型 | 约定 |
|------|------|------|
| `id` | BIGINT AUTO | 主键 |
| `create_time` / `update_time` | DATETIME | 审计字段；DO 中为 `LocalDateTime` |
| `enabled` | INT/BOOL | 启用/禁用开关 |
| `extend` | TEXT | JSON 扩展字段（FastJSON2 存取） |

### 12.3 会话实体（`MediaSessionDO` / `tb_media_session`）

```sql
call_id      VARCHAR(255) NOT NULL UNIQUE,  -- 业务主键
device_id    VARCHAR(64),
channel_id   VARCHAR(64),
ssrc         VARCHAR(32),
stream       VARCHAR(255),
status       INTEGER DEFAULT 2,             -- INVITING（状态机见 §10.1）
session_type VARCHAR(32),                   -- PLAY/PLAYBACK/DOWNLOAD/TALK
extend       TEXT
```

> 设备编码遵循 GB28181 国标：设备与通道均为**独立 20 位国标编码**，从属关系由 Catalog 目录的 `ParentID` 表达，**不存在 `channelId = deviceId + 后缀` 的字符串前缀关系**（协议合规铁律，见 §18 与 CLAUDE.md）。

---

## 13. 缓存与并发

### 13.1 缓存分层策略

| 机制 | 适用场景 | 位置 |
|------|----------|------|
| `@Cached` 注解 | Repository 层**单对象**按 ID/唯一键查询 | repository（`CachedAspect`） |
| `RedisCache` | 复杂场景手动键管理，值以 FastJSON2 字符串存储 | repository |
| `LocalCacheBase` | 本地缓存 | repository/cache/local |
| `DelayedCacheEviction` | **延迟双删**：写后投入 Redis ZSet，500ms 后跨节点二次 evict | manager/cache |

Manager 统一通过 `clearCache(id, oldKey, newKey)` 做**主键 + 业务键双重清理**，清理异常不阻断主流程。延迟双删修复「JVM 定时器进程崩溃后脏读到 TTL」的窗口。

> **CacheManager 抢占坑**：sip-common 的 `CacheConfig` 会抢占 `CacheManager` 导致 "Cannot find cache named 'device'"。修复 = `application-repo.yml` 加 `spring.cache.type=redis` + `RedisConfig` 标 `@Primary`（见 §18）。

### 13.2 分布式锁

`RedisLockUtil` / `RedisLockUtils` 提供并发操作分布式锁，保证多实例数据一致性。

### 13.3 事件分片串行（替代锁的轻量方案）

实例内的同设备并发由 §7.2 的 `ShardDispatcher` 哈希分片解决：同 `deviceId`/`callId` 事件路由到同一槽、单线程消费，**无需锁**即可保证状态机正确推进，修复 GB28181 报文交错导致的状态翻转。队列容量默认 2000，溢出记日志（不阻塞 Notifier 线程）。

---

## 14. Web 层与安全

### 14.1 Controller 模板方法规范

| 优先级 | 方法 | 说明 |
|--------|------|------|
| Essential（必须） | `add`/`update`/`get`/`deleteOne`/`deleteBatch`/`getPage` | 对应 Manager 标准 CRUD |
| Optional（可选） | `createXxx`/`updateXxx`/`deleteXxx` | 含操作日志的业务方法 |
| Avoid（避免） | 状态更新/Key 更新等细粒度 | 一般不在 Controller 暴露 |

入参必须用 Web 层 `*Req`（禁止直用 DTO）；update 必须携带主键 ID 走 `updateById`；分页响应包装为 `*ListResp`（`total` + `items`）；查询采用「全量分页条件查询」，前端自由组装条件。

### 14.2 过滤器与拦截器

| 组件 | 职责 |
|------|------|
| `XssFilter` + `XssHttpServletRequestWrapper` | XSS 防护（排除 `/zlm/**`、`/index/hook/**`、Swagger） |
| `TraceFilter` | 链路追踪 ID 透传（配合 SkyWalking） |
| `RepeatableFilter` + `RepeatedlyRequestWrapper` | 请求体可重复读取 |
| **`InternalAuthFilter`** | 拦截 `/internal/**`：IP 白名单 + 时间戳容差 + HMAC 校验（命令亲和路由，§11） |
| `LoggerInterceptor` | 访问日志 |
| `RepeatSubmitInterceptor` + `SameUrlDataInterceptor` | 防重复提交（`@RepeatSubmit`） |

### 14.3 AOP 切面

`RateLimiterAspect`（`@RateLimiter` + `LimitType` 限流）、`ControllerExceptionLogAspect`（控制器异常日志）。

### 14.4 认证与权限

- Token 机制：`Authorization` 头 + 自定义 secret，默认 30 分钟过期；
- `AuthController`：`/login`、`/logout`、`/refresh`、`/codes`；
- RBAC：User-Role-Menu-Dept 五张表 + 两张关联表；
- 全局异常：`GlobalExceptionHandler` + `ServiceException`/`ServiceExceptionEnum`。

> ZLM Hook 端点（`/zlm/**`、`/index/hook/**`）与内部命令端点（`/internal/**`）属内部端点，部署时应做网络隔离或独立鉴权。`/internal/**` 已有 `InternalAuthFilter`（HMAC + IP 白名单）。

## 15. 异步与线程模型

### 15.1 线程池配置（`ThreadPoolConfig`，manager 模块）

| 线程池 Bean | 配置 | 用途 |
|-------------|------|------|
| `threadPoolTaskExecutor` | core=50 / max=200 / queue=1000 | 通用后台任务 |
| `sipNotifierExecutor` | core=8 / max=32 / queue=1000，CallerRuns | **GB28181 翻译层专用**（`VoglanderBusinessNotifier.notify` 的 `@Async`） |
| `ShardDispatcher.executor` | FixedThreadPool，N=`voglander.event.shard.count`（默认 16） | **事件分片单线程槽**：每槽一个固定线程，保证同设备串行 |

`@EnableAsync` 开启；`sipNotifierExecutor` 满载 CallerRuns 回退（不丢回调，但产生背压）；重活（payload 反序列化、业务编排、DB 写）全部在 `EventShard` 单线程槽执行。

### 15.2 异步设计要点

- GB28181 `notify` 必须异步且仅做轻量翻译，否则设备 SIP 超时重传；
- 异步/Hook/外部 HTTP 调用相关测试**不使用** `@Transactional`（事务无法跨线程回滚），改手动清理。

---

## 16. 配置与部署

### 16.1 Profile 组合

`application.yml` 激活 `dev,repo,inte`：

| Profile | 文件位置 | 内容 |
|---------|----------|------|
| 主配置 | `voglander-web` `application.yml` | 应用名、端口 8081、SpringDoc、XSS |
| `dev` | `voglander-web` `application-dev.yml` | token、`local.sip.*`、`local.zlm.*` |
| `repo` | `voglander-repository` `application-repo.yml` | dynamic-datasource、HikariCP、Redis、MyBatis-Plus、`spring.cache.type=redis` |
| `inte` | `voglander-integration` `application-inte.yml` | SIP server/client、ZLM servers、**gateway 网关段** |

### 16.2 gateway 网关配置

```yaml
gateway:
  node-id: ${local.gateway.node-id:node-1}              # 多节点部署唯一标识
  forward-timeout-ms: 3000                               # 跨节点转发超时
  gb28181:
    store:
      type: ${...:memory}                                # memory(单机) | redis(多节点)
    invite-context-ttl-ms: 30000                         # INVITE 上下文 TTL（超时回包 410）
    invite-idempotency-window-ms: 5000                   # UDP 重传幂等窗口
  # 命令亲和路由（多节点，默认关闭）— §11
  nodes: {}                                              # nodeId → host:port
  internal-auth:
    shared-secret: CHANGE_ME_IN_PROD                     # 生产必改
    allowed-ips: 127.0.0.1

# 事件分片管线
voglander:
  event:
    shard:
      enabled: true        # true(默认)→主入口分片；false→回退入口直调 Dispatcher
      count: 16            # 分片数（FixedThreadPool 槽数）
  command:
    affinity-route:
      enabled: false       # 命令亲和路由整体开关
```

### 16.3 部署形态

- **单机**：`store.type=memory`，`affinity-route.enabled=false`；
- **多节点集群**：`store.type=redis`，各节点唯一 `node-id`，`affinity-route.enabled=true` + 配 `gateway.nodes`，依赖 Redis 做 INVITE 跨节点回包路由 + 设备节点亲和；前端经负载均衡接入。

### 16.4 上游依赖构建顺序

voglander 依赖 zlm starter 的 `-SNAPSHOT`（现 1.0.11）及 SIP 库（1.8.2）。改 sip-proxy / zlm-spring-boot-starter 后须先 `mvn clean install` 上游再构建 voglander，否则跑陈旧 jar；editing voglander-integration 后跑 web 测试同理需先 install integration（或 `-am`）。

---

## 17. 测试架构

### 17.1 测试分层策略

| 层 | 测试类型 | 注解 | 依赖处理 | 事务 |
|----|----------|------|----------|------|
| Controller | 纯单元 | `@ExtendWith(MockitoExtension.class)` | `@Mock`+`@InjectMocks` | 无 |
| Service | 纯单元 | `@ExtendWith(MockitoExtension.class)` | `@Mock`+`@InjectMocks` | 无 |
| Manager | 集成 | `@SpringBootTest` + `BaseTest` | 真实注入 + `@MockitoBean` Assembler | `@Transactional` |
| Repository | 集成 | `@SpringBootTest` + `BaseTest` | 真实数据库 | `@Transactional` |
| HTTP API | 集成 | 自定义基类 | `TestRestTemplate` | 无，手动清理 |
| 异步/Hook | 集成 | 自定义基类 | 真实 Bean | 无，手动清理 |

- 业务层（Controller/Service）**禁用** `@SpringBootTest`/`@WebMvcTest`/`@MockitoBean`，纯 Mockito 单测；
- Manager 必须集成测试：`IService<DO>` 用 `@Autowired`，Assembler/外部集成用 `@MockitoBean`（Assembler 用 `thenAnswer` 双向转换）；
- **全部测试集中**在 `voglander-web/src/test/java/io/github/lunasaw/voglander`，其他模块不放测试；
- 测试库 SQLite `test-app.db`（`schema-sqlite.sql` 含 `tb_media_session`）；Redis 集成测试运行时探测自动跳过（`Assumptions.assumeTrue`）。

### 17.2 已知测试噪声

real-SIP E2E 测试时序 flaky（`ListeningPoint Not Exist` / `Transaction exists` + 约 6.5k ERROR 日志行为既有噪声）；甄别本次改动是否引入回归时，用 `git stash` 基线对照，勿追逐既有 flaky。覆盖率：`./generate-coverage-report.sh` → JaCoCo 聚合报告。

### 17.3 本版本验收结论

S1~S6 直接相关测试全绿（DeviceAgreementService 6 / Gb28181CommandType 冻结 / server command 49 / S4 折算 12 / S5 媒体 18 / S6 节点+状态机 11）；`mvn clean install` 全套件除既有 real-SIP flaky 外无新增失败。

---

## 18. 关键约束与易踩坑清单

| # | 约束 | 后果 |
|---|------|------|
| 1 | `ApplicationWeb` 必须 `@EnableSipServer` | 缺失 → `CommandStrategyFactory`/`ServerCommandSender` 无法实例化 → GB28181 事件管线静默关闭 |
| 2 | `VoglanderBusinessNotifier.notify` 必须 `@Async` 且不继承 `AbstractProtocolBusinessNotifier`；只做翻译 | 同步/继承基类 → 设备 SIP 超时重传或 `@Async` 失效 |
| 3 | `Voglander*DeviceSupplier` 须标 `@Primary` | 与框架 `Default*DeviceSupplier` 冲突 → `NoUniqueBeanDefinitionException` |
| 4 | `VoglanderDeviceSessionCache` 必须存在 | `ServerCommandSender` 构造强依赖 |
| 5 | `ServerStart` 不可删 | 唯一 SIP 端口绑定处 |
| 6 | 出站命令走 `dispatchEnvelope`（server 端基类已无 `serverCommandSender`） | 绕过协议适配层的 payload schema 校验 |
| 7 | 白名单 handler SDP IP 字段名为 `mediaIp`（非 `sdpIp`） | payload 字段错配 → 命令下发失败 |
| 8 | 出站 type 必须来自 `Gb28181CommandType` 枚举；新增协议自建枚举 | 裸字符串拼错编译期不报，运行期 `require(type)` 404 |
| 9 | integration 模块 pom 历史 `<source>9</source>` | 该模块不可用 Java16+ `instanceof X x` 模式匹配，须经典 cast |
| 10 | 根 pom 中 `spring-boot-dependencies` 必须在 `sip-gateway-bom` **之前** import | 顺序颠倒 → Spring Boot 降级到 3.3.1 → `@MockitoBean` 消失，测试编译失败 |
| 11 | 所有类型转换用 FastJSON2 正反序列化 | 禁止手写字符串解析 |
| 12 | `RedisInviteContextStore` 后端故障必须抛 503 | 静默吞 → 跨节点回包路由错误 |
| 13 | sip-common `CacheConfig` 抢占 `CacheManager` | "Cannot find cache named 'device'"；修复 = `spring.cache.type=redis` + `RedisConfig @Primary` |
| 14 | 新增协议入站：实现 `ProtocolEventHandler` 并 `@Component`，**禁止改 `ShardDispatcher`/`InboundEventDispatcher`** | 破坏协议解耦 |
| 15 | `Gb28181ProtocolHandler` 禁止 import 任何 sip-gateway 框架类型 | 协议层与 gateway 产品耦合 |
| 16 | **协议合规铁律**：编码/字段/状态机/寻址严格对齐 GB28181/SIP/ONVIF 标准，禁止臆造（如 `channelId=deviceId+后缀`） | lab 非标简化须显式标注 + 单点收口（`LabChannelHolder`），非 lab 路径不得复用 |
| 17 | INVITE 寻址按通道（`To=channelId`），且 lab 客户端 `checkDevice` 须委托 `LabChannelHolder.ownsChannel` 认通道 | 否则不回 200OK → 点播 408 |
| 18 | 命令亲和路由默认关闭；`gateway.internal-auth.shared-secret` 生产必改 | 默认 `CHANGE_ME_IN_PROD` 不可用于生产 |

---

## 19. 演进路线

### 19.1 已完成（1.0.7 基线）

- ✅ **协议接入对称化（本版本核心）**：
  - S1 出站 `DeviceCommandService` SPI 表驱动（`DeviceAgreementService` 消除硬编码 if）；
  - S2 出站 type 枚举化（`Gb28181CommandType` 19 种 + `ProtocolConstants`）；
  - S3 server 端 envelope 死代码清理（`AbstractVoglanderServerCommand` 只余双通道）；
  - S4 路由键统一为纯协议 `DeviceProtocolEnum`；
- ✅ **媒体层协议选路（S5）**：`MediaProtocolHandler`/`Router`/`Gb28181MediaProtocolHandler`，命令/事件/媒体三层「加新实现、改老代码 0 处」；
- ✅ **可靠性补强（S6）**：`MediaSessionStatusMachine` 状态机守卫 + 媒体选节点 weight 降序故障转移；
- ✅ **命令亲和路由**：多节点出站命令转发到设备所属节点（HMAC + IP 白名单）；
- ✅ **INVITE callId 会话关联**：同步返真实 Call-ID + 占位行回填，修复关流 `DialogNotFoundException`；
- ✅ **消息链路追踪**：`SipMessageTracer` 出/入站统一打印 JSON body + callId/deviceId；
- ✅ 1.0.3 已有：sip-gateway 接入、入站四层管线（翻译/分片/路由/处理）、会话建模、多节点 INVITE 上下文、延迟双删、定向更新。

### 19.2 已知 TODO / 待完善

- ZLM `onPlay`/`onPublish` 鉴权当前放行（占位），需补全；
- integration 模块 pom `<source>9</source>` 建议统一升至 17；
- ZLM Hook 端点独立鉴权/网络隔离；
- `Response.*`（DeviceStatus/RecordInfo/PtzPosition 等）多数仅日志记录，按需补落库；
- 真正接入 ONVIF/GT1078/RTSP：参考 `Gb28181ProtocolHandler`（入站）、`GbDeviceCommandService`（出站）、`Gb28181MediaProtocolHandler`（媒体）三处模式新增实现即可，路由层零改动。

---

## 附录 A：缩写表

| 缩写 | 全称 | 说明 |
|------|------|------|
| GB28181 | 公共安全视频监控联网系统信息传输、交换、控制技术要求 | 中国国标 |
| SIP | Session Initiation Protocol | 会话初始化协议 |
| INVITE | SIP INVITE 方法 | 媒体点播/回放会话建立 |
| SPI | Service Provider Interface | 此处指「构造注入 List 折叠成 Map」的可插拔注册模式 |
| ZLM | ZLMediaKit | 流媒体服务器 |
| HMAC | Hash-based Message Authentication Code | 跨节点命令转发签名 |
| DO/DTO/VO/Req/Resp/QO | 见 §5.2 | 数据模型命名 |
| SSRC | Synchronization Source | RTP 同步源标识 |
| RBAC | Role-Based Access Control | 基于角色的访问控制 |

## 附录 B：参考文档

- `CLAUDE.md`（根 / voglander）— 编码规范与分层约定（权威）；
- `doc/1.0.3/ARCHITECTURE.md` — 上一版架构快照（基线 `dev_merge_sip`，已被本文取代）；
- `doc/1.0.7/PROTOCOL-ARCHITECTURE-GENERICITY-PLAN.md` — S1~S6 实施方案（本文 §8~§11 的规划来源）；
- `doc/1.0.7/GB28181-DEVICE-MANAGEMENT-TECH-PLAN.md` — 命令/事件全景；
- `doc/1.0.7/GB28181-LIVE-INVITE-CALLID-SESSION-PLAN.md` — INVITE callId 会话关联；
- `doc/1.0.7/LIVE-STREAM-CACHE-CALLBACK-SYNC-TECH-PLAN.md` — 关流/缓存同步。
