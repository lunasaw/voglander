# Voglander 视频监控平台

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lunasaw/voglander)](https://mvnrepository.com/artifact/io.github.lunasaw/voglander)
[![GitHub license](https://img.shields.io/badge/MIT_License-blue.svg)](https://raw.githubusercontent.com/lunasaw/voglander/master/LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen.svg)](https://spring.io/projects/spring-boot)

[🌐 项目主页](http://lunasaw.github.io) | [📖 架构文档](doc/1.0.3/ARCHITECTURE.md) | [🚀 快速开始](#快速开始)

## 📋 项目介绍

Voglander 是基于 **Spring Boot 3.5.3 + Java 17 ** 的企业级视频监控平台，对接 GB28181[Sip-Proxy](https://github.com/lunasaw/Sip-Proxy)、GT1078、ONVIF 等协议，提供设备接入、实时点播/回放、媒体流转发与录制、设备状态监控和权限管理能力。

![Stream操作演示](./code-log/stream.gif)

### ✨ 核心特性

- 🎯 **多协议支持** — GB28181（sip-gateway 1.8.0）、GT1078、ONVIF
- 🏭 **设备兼容** — 海康、大华、宇视、中维等主流厂商
- 🔧 **严格分层** — Web / Manager / Service / Repository / Integration 五层，职责边界清晰
- 📊 **四层事件管线** — 翻译 → 分片 → 协议路由 → 协议处理，SIP 线程零阻塞
- 🚀 **高并发** — 按 `deviceId` 哈希分片（16 槽），同设备串行 + 跨设备无锁并发
- 🛡️ **安全可靠** — RBAC 权限、XSS 过滤、防重复提交、限流切面

---

## 🏗️ 系统架构

### 整体上下文

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
        │   │  SIP 网关 │◀────────────────▶│  IPC / NVR / 下级平台│    │
        │   │(sip-gw1.8)│   注册/心跳/INVITE└─────────────────────┘    │
        │   └──────────┘                                              │
        │                                                             │
        │   ┌──────────┐   HTTP API/Hook  ┌─────────────────────┐    │
        │   │ ZLM 集成  │◀────────────────▶│   ZLMediaKit 媒体服务│    │
        │   └──────────┘   拉流/推流/回调  └─────────────────────┘    │
        │                                                             │
        │   ┌─────────────────┐   ┌─────────┐   ┌────────────────┐   │
        │   │ MySQL / SQLite  │   │  Redis  │   │  SkyWalking    │   │
        │   └─────────────────┘   └─────────┘   └────────────────┘   │
        └───────────────────────────────────────────────────────────┘
```

### 三条核心业务链路

| 链路 | 入口 | 关键组件 | 出口 |
|------|------|----------|------|
| **GB28181 设备入站** | 设备 SIP 报文 | sip-gateway → `VoglanderBusinessNotifier` → `ShardDispatcher` → `InboundEventDispatcher` → `Gb28181ProtocolHandler` | 设备/通道/会话状态落库 |
| **GB28181 平台出站** | REST / 业务调用 | `VoglanderServer*Command` → `dispatchEnvelope` → `CommandHandlerRegistry` | SIP 下发（PTZ/点播/回放/配置） |
| **媒体流代理** | 前端 `/zlm/api/*` | `StreamProxyController` → `StreamProxyManager` → `StreamProxyZlmWrapperService` | ZLM 拉流 + Hook 回写状态 |

### Maven 多模块结构

```
voglander/
├── voglander-common/          # 共享：常量、枚举、异常、工具、注解
├── voglander-client/          # 服务契约：接口 + DTO/VO/QO
├── voglander-repository/      # 数据访问：DO 实体、Mapper、缓存
├── voglander-manager/         # 业务编排：Manager 模板、事件分片管线、异步、线程池
├── voglander-service/         # 领域服务：登录注册、流业务、设备协议
├── voglander-integration/     # 外部集成：GB28181、ZLM、EasyExcel、IP 解析
├── voglander-web/             # 应用主模块：Controller、过滤器、拦截器、启动类、全部测试
├── voglander-test/            # 测试配置与多环境 profile
└── voglander-coverage-report/ # JaCoCo 跨模块聚合覆盖率
```

依赖方向：`web → service → manager → repository → common`；`integration → manager`（回调直调 Manager 落库）。全部测试集中在 `voglander-web/src/test/java/`。

### 分层职责

| 层 | 入参 | 出参 | 核心职责 |
|----|------|------|---------|
| Web | `*Req` | `AjaxResult<VO>` | 参数校验、格式转换、Swagger 文档 |
| Manager | `*DTO` | `*DTO` / `Page<DTO>` | 多服务编排、缓存一致性、DTO↔DO 转换 |
| Service | `*DTO` / 基本类型 | 领域对象 | 单一职责业务、Assert 校验 |
| Repository | DO / 主键 | DO | 持久化、`@Cached` 单对象缓存 |
| Integration | 外部模型 | `ResultDTO<T>` | 参数校验、异常捕获、结果包装 |

---

## 🔧 技术栈

| 类别 | 选型 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 基础框架 | Spring Boot | 3.5.3 |
| ORM | MyBatis-Plus | 3.5.5 |
| 多数据源 | dynamic-datasource | 4.3.1 |
| 生产数据库 | MySQL | 8.2.0 |
| 开发/测试数据库 | SQLite | — |
| 缓存/锁 | Redis | — |
| SIP 网关 | sip-gateway-spring-boot-starter | 1.8.0 |
| 媒体服务 | ZLMediaKit-Starter | 1.0.10-SNAPSHOT |
| JSON | FastJSON2 | — |
| 链路追踪 | SkyWalking | 9.1.0 |
| API 文档 | SpringDoc OpenAPI | 2.8.9 |
| 覆盖率 | JaCoCo | 0.8.11 |

---

## 🚀 快速开始

### 环境要求

- **Java**: JDK 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+（可选，默认 SQLite）
- **Redis**: 6.0+（可选，多节点必须）

### 启动后端

```bash
# 1. 编译
mvn clean compile

# 2. 启动（SQLite 自动创建 app.db，开箱即用）
mvn spring-boot:run -pl voglander-web

# 3. 访问
# 应用: http://localhost:8081
# API 文档: http://localhost:8081/swagger-ui.html
```

**切换 MySQL**（可选）：

```bash
# 1. 建库
CREATE DATABASE voglander;

# 2. 执行建表脚本
mysql -u root -p voglander < sql/voglander.sql

# 3. 修改 application-repo.yml 中的数据源配置
```

---

## 📡 GB28181 接入（sip-gateway 1.8.0）

### 四层入站管线

```
设备 SIP 报文
   ▼ ① 框架回调（@Async，仅翻译，O(1)）
VoglanderBusinessNotifier.notify()
   ▼ ② 分片调度（deviceId 哈希 → 16 个 EventShard，同设备串行）
ShardDispatcher
   ▼ ③ 协议路由（按 event.protocol() 派发）
InboundEventDispatcher
   ▼ ④ 协议处理（FastJSON2 反序列化 + 业务调用）
Gb28181ProtocolHandler
   ├── Lifecycle.Register   → DeviceRegisterService.login()
   ├── Lifecycle.Online     → DeviceManager.patchLiveness()
   ├── Lifecycle.Offline    → DeviceRegisterService.offline()
   ├── Notify.Keepalive     → DeviceRegisterService.keepalive()
   ├── Notify.MediaStatus   → MediaSessionManager.onMediaStatus()
   ├── Response.Catalog     → DeviceRegisterService.addChannel()
   ├── Session.InviteOk     → MediaSessionManager.onInviteOk()
   ├── Session.InviteFailure→ MediaSessionManager.onInviteFailure()
   └── Session.Bye          → MediaSessionManager.onBye()
```

> ⚠️ **禁止继承 `AbstractProtocolBusinessNotifier`**：其 `notify()` 为 `final` 且内部自调用，`@Async` 代理失效。实现直接 `implements BusinessNotifier`。

灰度开关：`voglander.event.shard.enabled=false` 可跳过分片直调 Dispatcher（调试用）。

### 出站命令（envelope 模式）

所有出站命令统一经 `dispatchEnvelope`，type 三段式 `protocol.Group.Name`：

| 业务场景 | cmdType | payload 关键字段 |
|---------|---------|----------------|
| 设备目录查询 | `gb28181.Query.Catalog` | — |
| 实时点播 | `gb28181.Invite.Play` | `mediaIp`、`mediaPort`、`streamMode` |
| 录像回放 | `gb28181.Invite.Playback` | `startTime`、`endTime` |
| PTZ 控制 | `gb28181.Control.Ptz` | `cmdCode`、`horizonSpeed`、`verticalSpeed` |
| 终止会话 | `gb28181.Invite.Bye` | `callId`（deviceId 留空） |

> ⚠️ 白名单 handler 中 SDP IP 字段名为 `mediaIp`（非 `sdpIp`），字段错配命令下发失败。

### 启动装配关键约束

| 组件 | 必要性 | 缺失后果 |
|------|--------|---------|
| `ApplicationWeb` 上 `@EnableSipServer` | **必需** | `CommandStrategyFactory`/`ServerCommandSender` 无法实例化，整条 GB28181 管线静默关闭 |
| `VoglanderDeviceSessionCache` | **必需** | `ServerCommandSender` 构造强依赖 |
| `Voglander*DeviceSupplier` 标 `@Primary` | **必需** | `NoUniqueBeanDefinitionException` |
| `ServerStart`（CommandLineRunner） | **不可删** | 唯一 SIP 端口绑定处 |

### 多节点部署

```yaml
gateway:
  node-id: ${local.gateway.node-id:node-1}
  gb28181:
    store:
      type: redis          # 多节点必须改为 redis
    invite-context-ttl-ms: 30000

voglander:
  event:
    shard:
      enabled: true
      count: 16
```

> ⚠️ 多节点必须填 `sip.server.external-ip: <VIP>`，并将 `store.type` 改为 `redis`，否则跨节点 INVITE 回包失败。

---

## 📖 配置说明

配置文件由 4 个 profile 组合：

| Profile | 文件 | 内容 |
|---------|------|------|
| 主配置 | `application.yml` | 应用名、端口 8081、SpringDoc、XSS |
| `dev` | `application-dev.yml` | token、本地 SIP/ZLM 参数 |
| `repo` | `application-repo.yml` | dynamic-datasource、HikariCP、Redis、MyBatis-Plus |
| `inte` | `application-inte.yml` | SIP server/client、ZLM servers、`gateway:` 网关段 |

---

## 🧪 测试

### 测试分层策略

| 层 | 类型 | 注解 | 事务 |
|----|------|------|------|
| Controller / Service | 纯单元 | `@ExtendWith(MockitoExtension.class)` | 无 |
| Manager / Repository | 集成 | `@SpringBootTest` + `BaseTest` | `@Transactional` |
| HTTP API / 异步 Hook | 集成 | 自定义基类 | 无，手动清理 |

> 异步、Hook、外部 HTTP 相关测试**不使用** `@Transactional`，事务无法跨线程回滚。

### 运行测试

```bash
# 全量测试
mvn test

# 单个测试类
mvn test -Dtest=DeviceManagerTest

# Redis 集成测试（运行时自动探测，不可用则跳过）
brew services start redis
mvn test -Dtest=MediaNodeCacheIntegrationTest

# 生成覆盖率报告
./generate-coverage-report.sh
# 输出：voglander-coverage-report/target/site/jacoco-aggregate/index.html
```

---

## 🗃️ 数据模型速览

| 领域 | 实体 | 表 |
|------|------|----|
| 设备 | `DeviceDO` | `tb_device` |
| 设备 | `DeviceChannelDO` | `tb_device_channel` |
| 媒体 | `MediaSessionDO` | `tb_media_session`（1.0.3 新增，业务主键 `call_id`） |
| 媒体 | `MediaNodeDO` | `tb_media_node` |
| 媒体 | `StreamProxyDO` / `PushProxyDO` | `tb_stream_proxy` / `tb_push_proxy` |
| 权限 | `UserDO` / `RoleDO` / `MenuDO` / `DeptDO` | RBAC 主体 |

所有业务表统一字段：`id`（BIGINT）、`create_time` / `update_time`（LocalDateTime）、`enabled`（开关）、`extend`（JSON 扩展）。

---

## ⚠️ 关键约束

| # | 约束 | 后果 |
|---|------|------|
| 1 | `@EnableSipServer` 不可缺失 | GB28181 整条管线静默关闭 |
| 2 | `notify()` 必须 `@Async` 且仅做轻量翻译 | 同步处理 → 设备 SIP 超时重传 |
| 3 | 出站命令走 `dispatchEnvelope`，禁止直调 `ServerCommandSender` | 绕过 payload schema 校验 |
| 4 | SDP IP 字段名为 `mediaIp`（非 `sdpIp`） | 命令下发失败 |
| 5 | `spring-boot-dependencies` 必须在 `sip-gateway-bom` **之前** import | Spring Boot 被降级 → `@MockitoBean` 消失 |
| 6 | 新增协议入站：实现 `ProtocolEventHandler` + `@Component`，禁止改动 `ShardDispatcher` | 破坏协议解耦原则 |
| 7 | 所有类型转换用 FastJSON2 正反序列化 | 禁止手写字符串解析 |

---

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 `git checkout -b feature/xxx`
3. 提交更改 `git commit -m 'feat: xxx'`
4. 推送并打开 Pull Request

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源协议发布。

## 👥 维护者

- **Luna** — [GitHub](https://github.com/lunasaw) | iszychen@gmail.com

## 🔗 相关链接

- [架构设计文档](doc/1.0.3/ARCHITECTURE.md)
- [问题反馈](https://github.com/lunasaw/voglander/issues)
- [更新日志](CHANGELOG.md)

---

<p align="center">
  <b>如果这个项目对您有帮助，请给我们一个 ⭐️ Star!</b>
</p>
[![Star History Chart](https://api.star-history.com/svg?repos=lunasaw/voglander&type=Date)](https://star-history.com/#lunasaw/voglander&Date)
