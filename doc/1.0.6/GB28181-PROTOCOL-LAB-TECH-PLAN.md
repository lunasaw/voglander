# GB28181 协议可视化验证台（Protocol Lab）技术方案

> 版本 1.0.6 · 分支 `dev_merge_sip` · 文档日期 2026-06-05
> 目标：提供一个左右分栏的前端页面 —— **左侧作为 GB28181 客户端（设备 UA）向平台注册**，**右侧作为服务端（平台）实时感知设备注册、下发指令**；指令经底层真实 SIP 交互后**回到左侧展示"收到的指令"**。用于直接可视化验证 GB/T 28181 协议链路的完整度。

---

> ## ✅ 代码核验结论（2026-06-05）
>
> 本方案的全部核心架构断言已对照 `voglander` + `sip-proxy` 真实代码逐条核验，**整体可行**：同进程 SIP 自环在 JAIN-SIP 协议栈层面能正确路由（依据见 §1.4）。核验同时发现并已在本文修正 3 处与代码不符之处：
>
> | # | 性质 | 修正 |
> |---|------|------|
> | **C1** | 🔴 阻断启动 | §5.1 启动命令缺 `inte` profile —— `sip:` 配置块只存在于 `application-inte.yml`，不激活则 `ServerStart` 不绑定任何端口。已改为 `dev,repo,inte,lab`。 |
> | **C2** | 🟡 隐性依赖（非配置 bug） | 右→左指令依赖 `tb_device.ip/port` = 客户端回环地址。经核验 `application-inte.yml:42` 已将 `sip.client.domain ← local.sip.client.ip`，方案 `client.ip:127.0.0.1` 即足够，**无需新增 `client.domain`**；但须写明"右侧能力以左侧注册落库为前提"。见 §4.5 / §5.1。 |
> | **C3** | 🟢 已修复（删工作量） | PTZ D6 词表不匹配**已在代码修复**：`GbDeviceCommandService` 已含 `PTZ_VOCAB` 映射（`UP→TILT_UP/...`）。前端直发 `UP/DOWN` 即可，S3 不再需要此项。 |
>
> 核验证据索引见 **附录 C**。

---

## 0. TL;DR（一句话架构）

voglander 进程内 `ServerStart` 已同时绑定 **SIP 服务端（5060）** 与 **SIP 客户端（5061）** 两个监听点。本方案利用这一既有事实，构建一条 **同进程 SIP 自环（loopback）**：

```
┌────────────── 浏览器（一个页面，左右分栏）──────────────┐
│  左：设备 UA 控制台                 右：平台控制台          │
│  [注册] [心跳] [上报目录] [告警]     设备列表(实时) [PTZ][点播][查目录][校时] │
│  收到指令时间线 ◀──────────┐        SIP 消息时间线 / 事件流  │
└──────│REST────────│SSE─────┼──────────│REST────────│SSE──┘
       ▼            ▲        │          ▼            ▲
┌──────────────────┴────────┼──────────┴───────────────────┐
│            voglander-web（REST 控制器 + SseController）       │
│  /api/v1/lab/client/*  (左)   /api/v1/ptz|live|device-cmd/* (右)│
│            SseEventBus  topics: device.* / clientcmd.* / sip.* │
├───────────────────────────┼───────────────────────────────┤
│ ClientCommandSender(静态)  │  ServerCommandSender(envelope)  │
│ + Client*Listener(接收指令)│  + VoglanderBusinessNotifier(接收上报)│
│        SIP:5061  ───────── 真实 UDP/TCP SIP ───────── SIP:5060 │
└──────────────────────────────────────────────────────────────┘
```

- **真实 SIP**：左侧"注册/心跳/目录"通过 `ClientCommandSender` 真正发 SIP 报文到 5060；右侧"PTZ/点播/查询"通过 `ServerCommandSender` 真正发 SIP 报文到 5061。**不是 mock**。
- **右侧"感知注册"**：复用既有入站管线 `VoglanderBusinessNotifier → Gb28181ProtocolHandler`，新增把 `Lifecycle.*`/`Notify.*` 事件经 `SseEventBus` 推到 `device.*` topic。
- **左侧"展示收到指令"**：新增 5 个客户端接收监听器（`ControlListener`/`ConfigListener`/`QueryListener`/`NotifyListener`/`SubscribeListener` 的 Lab 实现），把平台下发到设备的指令经 `SseEventBus` 推到 `clientcmd.*` topic。

> **关键设计判断**：用户说"都需要走底层 SIP 交互"。因为 voglander 进程已同时是 SIP 服务端 + SIP 客户端，最稳妥、零额外部署的实现是**同进程自环**——左右两侧是同一个 JVM 内的两个 SIP 角色，报文真实穿过 JAIN-SIP 协议栈（经 5061→5060→5061 回环），既满足"真实协议交互"，又无需另起独立设备模拟器进程。详见 §1.3 的方案对比与 §9 待确认项。

---

## 1. 背景与架构定位

### 1.1 现状（已逐条核验，2026-06-05）

| 能力 | 现状 | 文件:行 |
|------|------|------|
| SIP 双角色绑定 | `ServerStart`(CommandLineRunner) 同时 `addListeningPoint` 绑��� server(5060)+client(5061)，**两者共用同一个 `sipListener` 单例 bean** | `ServerStart.java:46,51` |
| 客户端发指令 | `ClientCommandSender`（静态方法，单例委托 `CommandStrategyFactory`）：`sendRegisterCommand(from,to,Integer expires)`/`sendUnregisterCommand(from,to)`/`sendKeepaliveCommand(from,to,String status)`/`sendCatalogCommand(from,to,DeviceResponse)`/`sendDeviceInfoCommand`/`sendAlarmCommand`，方案所需方法**全部存在** | `gb28181-client/.../cmd/ClientCommandSender.java:712,741,126,153,235,95` |
| 客户端收指令 | 5 个 Listener SPI：`ControlListener`(PTZ等13类+Keepalive,**多实例观察者**)、`ConfigListener`(多实例)、`QueryListener`(目录/设备信息,**全局唯一,≥2 fail-fast**)、`NotifyListener`(多实例)、`SubscribeListener`(多实例) | `gbproxy/client/api/{Control,Query,...}Listener.java`（`ControlListener.java:24`、`QueryListener.java:38`） |
| 服务端发指令 | `ServerCommandSender`(实例,按 deviceId) → `GbDeviceCommandService` 门面：`ptzControl/startPlay/queryCatalog/queryDeviceInfo/reboot`。**门面已含 `PTZ_VOCAB` 前端词表→枚举映射** | `server/command/*` + `service/command/impl/GbDeviceCommandService.java:71`（PTZ_VOCAB） |
| 服务端收上报 | `VoglanderBusinessNotifier`(`@Async`) → `ShardDispatcher` → `Gb28181ProtocolHandler.handle(DeviceEvent)`（35 事件 switch，当前**全程不发 SSE**） | `notifier/VoglanderBusinessNotifier.java` + `handler/Gb28181ProtocolHandler.java:79`（switch 起点） |
| 跨层事件解耦先例 | `handleAlarm` → `eventPublisher.publishEvent(AlarmCreatedEvent)`；`AlarmCreatedEvent` 在 **voglander-common**，service 层 `LiveStreamEventListener` 消费。方案 §4.4 的 `DeviceVisualEvent` 与之**完全同构** | `Gb28181ProtocolHandler.java:350` + `common/event/AlarmCreatedEvent.java` + `service/live/LiveStreamEventListener.java` |
| SSE 实时推送 | `GET /api/v1/stream/events?token=&topics=` → `RedisBackedSseEventBus`(Redis Pub/Sub 扇出, 15s 心跳)；**发送时 `event` 名 = topic**（`.name(event.getTopic())`）；当前只推 `alarm.new`/`live.ready`/`live.closed` | `web/api/sse/controller/SseController.java:29` + `service/sse/RedisBackedSseEventBus.java:92` |
| 鉴权 | `POST /auth/login` (admin/admin123) → JWT；`/api/v1/**` 经 JwtAuthInterceptor；SSE 用 `?token=`（`SseController:34` 强制校验 token） | `web/api/auth/*` + `SseController.java:34` |

### 1.2 需要新增/补齐的能力

1. **左侧设备控制台 REST**（新增 `LabClientController`）：触发设备 UA 主动发 SIP（注册/注销/心跳/上报目录/上报告警/上报设备信息）。
2. **左侧"收到指令"采集**（新增 5 个 `Lab*Listener`）：把平台下发到设备 UA 的指令捕获并推 SSE `clientcmd.*`。
3. **右侧"感知注册"补齐**：`Gb28181ProtocolHandler` 在 `Lifecycle.Register/Online/Offline` 与 `Notify.Keepalive`、`Session.*` 处增发 SSE `device.*`（当前缺失，只有 `alarm.new`）。
4. **可选 SIP 报文级时间线**（`sip.trace`）：把真实 SIP 请求/响应行（method/status/Call-ID）作为半结构化事件推送，用于"SIP 阶梯图"。
5. **前端新页面**：`vue-vben-admin` 的 `apps/web-antd` 下新增协议验证台路由 + 组件 + SSE composable + API 模块 + i18n。
6. **专用启动 Profile**：`application-lab.yml`，开启 `sip.server.enabled=true` + `sip.client.enabled=true` 的自环配置。

### 1.3 方案对比（为什么选"同进程自环"）

| 方案 | 描述 | 真实 SIP | 部署成本 | 选择 |
|------|------|----------|----------|------|
| **A. 同进程自环（推荐）** | 同一 voglander JVM 内 client(5061) ↔ server(5060) 互发 | ✅ 报文真过协议栈 | 零（仅开 profile） | ✅ |
| B. 双进程 | 起两个 voglander 实例分别只开 client / server | ✅ | 高（两套配置/端口/DB） | ❌ 过度设计 |
| C. 纯前端模拟 | 前端伪造事件，后端不发 SIP | ❌ 违背"走底层 SIP" | 低 | ❌ 不满足需求 |

> 方案 A 的唯一注意点：client 与 server 用**不同 deviceId/clientId** 与不同端口，避免自己给自己注册时的身份冲突（§5.1 配置）。**此约束不是建议，而是自环正确性的前提**——原因见 §1.4。

### 1.4 自环为何能正确路由（make-or-break 核验）

这是整个方案的地基，已顺着 JAIN-SIP 分发链路核验到底：

1. **分发只按 SIP method，且处理器表是 `static`**。`AbstractSipListener.processRequest` 用 `REQUEST_PROCESSOR_MAP.get(method)` 取处理器（`sip-common/.../AbstractSipListener.java:232`），而该 Map 是 `static`（:47/:52）——**跨监听点、跨角色共享，不按端口/角色路由**。因此一条 `MESSAGE` 不论从 5060 还是 5061 进来，`ServerMessageRequestProcessor` 与 `ClientMessageRequestProcessor` **都会被调用**（请求分发无 `isNeedProcess` 过滤，:285-288）。
2. **两侧各自按 SIP 身份自过滤**，这才让自环不串：
   - 客户端处理器：`toUserId != clientFromDevice.getUserId()` 直接 return（`ClientMessageRequestProcessor.java:46`）。
   - 服务端处理器：`serverDeviceSupplier.checkDevice(evt)` 不过就 return（`ServerMessageRequestProcessor.java:43`）。
3. **REGISTER 是 server 独有方法**，无歧义；其 401/200 响应按 CSeq method=`REGISTER` 回到 client 侧的 `ClientRegisterResponseProcessor` 消化。

**结论**：只要 `clientId ≠ serverId`（§5.1 已要求），两个 MESSAGE 处理器不会交叉处理，自环路由成立。**代价**：每条跨向 MESSAGE 会触发对侧一次冗余 `checkDevice` + 一行 `WARN` 日志（"目标用户ID与客户端不匹配"）——属预期噪音，非缺陷；lab 模式可酌情把该 logger 调到 ERROR 级以降噪。

> ⚠ **自环是新行为，非现网已跑路径**：现有 Cascade 虽也用 `ClientCommandSender.sendRegisterCommand` 注册（`CascadeClientScheduler.java:125`），但其 `to` 指向**外部**上级平台，不是本机 5060。把 client 的 `to` 指回本机 server 是本方案的新动作——代码分析证明路由正确，但**无现成测试覆盖**。故 S1 的首个 DoD（curl 注册 → SSE 收到 `device.register`）应作为第一个真实验证闸口优先打通。

---

## 2. 端到端数据流

### 2.1 注册闭环（左发起 → 右感知）

```
[左:点击"注册"]
  → POST /api/v1/lab/client/register {clientId, expires}
  → LabClientController → ClientCommandSender.sendRegisterCommand(from(5061), to(5060), expires)
  → 真实 SIP REGISTER 5061→5060
  → 服务端 SIP 栈：401 Challenge → 客户端 Digest 重发 → 200 OK
  → 框架 forwarder 产出 GatewayEvent("gb28181.Lifecycle.Register", clientId, payload)
  → VoglanderBusinessNotifier.notify(@Async) → ShardDispatcher → Gb28181ProtocolHandler.handleRegister()
      → DeviceRegisterService.login()  [既有：落库 tb_device status=1]
      → 【新增】sseEventBus.publish(SseEvent("device.register", {deviceId, remoteIp, transport, expire, ts}))
  → [右:设备列表实时出现该设备 + 时间线"⬇ REGISTER 200 OK"]
  ← 同时客户端注册成功事件 ClientRegisterSuccessEvent → 【新增】推 SSE "clientcmd.register.ok" → [左:时间线"⬆ 注册成功"]
```

### 2.2 指令回显闭环（右下发 → 左展示收到）

```
[右:点击"PTZ 上"]
  → POST /api/v1/ptz/control {deviceId=clientId, channelId, command:"TILT_UP", speed:128}   [既有端点]
  → GbDeviceCommandService.ptzControl() → ServerCommandSender → 真实 SIP MESSAGE(PTZCmd) 5060→5061
  → 客户端 SIP 栈解析 → ClientControlEvent → ClientListenerAdapter 分发
  → 【新增】LabControlListener.onPtzControl(platformId, DeviceControlPtz cmd)
      → sseEventBus.publish(SseEvent("clientcmd.ptz", {platformId, channelId, ptzCmd(hex), parsed:{方向,速度}, ts}))
  → [左:"收到指令"时间线出现 "⬇ PTZ 上 speed=128 (hex:A50F01...)"]   ← ★ 核心需求达成
  → 协议层自动回 200 OK（fire-and-forget）→ [右:时间线"⬆ 200 OK"]
```

### 2.3 查询-响应闭环（右查目录 → 左收到并回包 → 右收到目录）

```
[右:点击"查询目录"]
  → POST /api/v1/device-cmd/query-catalog {deviceId=clientId}   [既有端点]
  → ServerCommandSender 发 SIP MESSAGE(Catalog) 5060→5061
  → 客户端 → ClientQueryEvent → 【新增】LabQueryListener.onCatalogQuery(platformId, query)
      ① sseEventBus.publish("clientcmd.query.catalog")  → [左:展示"收到目录查询"]
      ② return 预置的 DeviceResponse(模拟 N 个通道)  → 框架自动 sendCatalogCommand 回包 5061→5060
  → 服务端入站 GatewayEvent("gb28181.Response.Catalog") → Gb28181ProtocolHandler.handleCatalog()
      → DeviceChannelManager.batchUpsertWithStatus()  [既有：落库通道]
      → 【新增】sseEventBus.publish("device.catalog", {deviceId, channelCount})
  → [右:通道列表实时刷新 + 时间线"⬇ Catalog N 通道"]
```

> ⚠ **唯一性约束**：`QueryListener` 是**唯一 bean**（框架 `ObjectProvider#getIfUnique()`，≥2 个 fail fast）。voglander 已有 `CascadeQueryHandler implements QueryListener`。Lab 的 `QueryListener` 不能与之共存 —— 解决方案见 §4.3。

---

## 3. 事件目录（SSE Topic 设计）

复用既有 `SseEvent{topic, data}` 与 `SseController`（topics 前缀匹配：订阅 `device` 收到 `device.register`/`device.online`/...，匹配逻辑见 `RedisBackedSseEventBus.matches()`）。

> ✅ **事件名传递已证实**：`RedisBackedSseEventBus.publishLocal` 用 `SseEmitter.event().name(event.getTopic())` 发送（`:92-94`），即 **SSE `event:` 名 = topic**。故前端可直接 `es.addEventListener(topic, ...)` 分流，§6.2 原存疑点已消除，无需 `onmessage` 兜底。

| topic | 方向 | data 关键字段 | 触发点（新增/既有） |
|-------|------|---------------|---------------------|
| `device.register` | 右收 | deviceId, remoteIp, remotePort, transport, expire, ts | 新增 @ handleRegister |
| `device.online` | 右收 | deviceId, ts | 新增 @ Lifecycle.Online |
| `device.offline` | 右收 | deviceId, ts | 新增 @ Lifecycle.Offline |
| `device.keepalive` | 右收 | deviceId, ts | 新增 @ handleKeepalive（建议节流，见 §4.4） |
| `device.catalog` | 右收 | deviceId, channelCount, ts | 新增 @ handleCatalog |
| `device.info` | 右收 | deviceId, manufacturer, model, firmware | 新增 @ handleDeviceInfo |
| `session.invite_ok` | 右收 | callId, deviceId, channelId | 新增 @ Session.InviteOk |
| `session.bye` | 右收 | deviceId, callId | 新增 @ Session.Bye |
| `clientcmd.register.ok` / `.fail` / `.challenge` | 左收 | clientId, reason? | 新增 @ Client 注册 ApplicationEvent 监听 |
| `clientcmd.ptz` | 左收 | platformId, channelId, ptzCmd(hex), parsed | 新增 @ LabControlListener.onPtzControl |
| `clientcmd.record` / `.guard` / `.reboot` / `.iframe` / `.alarmreset` | 左收 | platformId, cmd 内容 | 新增 @ LabControlListener.* |
| `clientcmd.query.catalog` / `.deviceinfo` / `.devicestatus` | 左收 | platformId, query | 新增 @ LabQueryListener.* |
| `clientcmd.config.*` | 左收 | platformId, cfg | 新增 @ LabConfigListener.* |
| `clientcmd.invite` | 左收 | callId, ssrc, sdp 摘要 | 新增 @ ClientInviteEvent 监听 |
| `alarm.new` | 右收 | deviceId, payload | **既有** |
| `sip.trace`（可选 Phase 3） | 左右 | dir(in/out), method/status, callId, cseq, from, to | 新增 @ SIP 日志切面 |

> **去重提示（D8）**：单机 `SseEventBus.publish` 会本地直发 + Redis 回环导致双投。前端按 `(topic, callId/ts, seq)` 去重；或后端关闭单机 Redis 回环（见 §8 风险）。

---

## 4. 后端改造清单

> 模块依赖方向：`web → manager → service → repository → common`；`integration → common`。SSE 推送统一调用 `service` 层的 `SseEventBus`。

### 4.1 新增：左侧设备 UA 控制台 REST —— `LabClientController`

位置：`voglander-web/src/main/java/.../web/api/lab/controller/LabClientController.java`
仅在 `@ConditionalOnProperty("voglander.protocol-lab.enabled"=true)` 下注册。

| 方法 | 路径 | 请求体 | 委托 | 说明 |
|------|------|--------|------|------|
| POST | `/api/v1/lab/client/register` | `{expires=3600}` | `ClientCommandSender.sendRegisterCommand(from,to,expires)` | 设备主动注册 |
| POST | `/api/v1/lab/client/unregister` | `{}` | `sendUnregisterCommand`（expires=0） | 注销 |
| POST | `/api/v1/lab/client/keepalive` | `{}` | `sendKeepaliveCommand(from,to,"OK")` | 单次心跳 |
| POST | `/api/v1/lab/client/keepalive/auto` | `{intervalSec, enabled}` | 内部 ScheduledFuture | 周期心跳开关（参考 `CascadeClientScheduler`） |
| POST | `/api/v1/lab/client/catalog/push` | `{channelCount=4}` | `sendCatalogCommand(...DeviceResponse)` | 主动上报目录 |
| POST | `/api/v1/lab/client/device-info/push` | `{manufacturer,model,firmware}` | `sendDeviceInfoCommand` | 主动上报设备信息 |
| POST | `/api/v1/lab/client/alarm/push` | `{alarmType,priority,channelId}` | `sendAlarmCommand` | 主动上报告警（右侧 `alarm.new` 验证） |
| GET | `/api/v1/lab/client/config` | - | 读 properties | 返回当前 lab 身份/端口/topic 列表，供前端初始化展示 |

实现要点：
- `from()`=客户端身份（5061），`to()`=平台身份（5060），均从 `VoglanderSipClientProperties`/`VoglanderSipServerProperties` 读取，**不硬编码**。
- 入参为 Web 层 `*Req`（如 `LabRegisterReq`），经 WebAssembler/直接构造调用；遵循项目"入参用 Req、统一 `AjaxResult`"规范。
- `ClientCommandSender` 是静态方法，但需要 `from/to` 的设备身份；`from` 由 `VoglanderClientDeviceSupplier.getClientFromDevice()` 提供，`to` 由 `createDefaultToDevice(serverId)` 或读 server props 构造。

### 4.2 新增：左侧"收到指令"监听器（多实例 SPI，安全）

位置：`voglander-integration/src/main/java/.../wrapper/gb28181/lab/`
均 `@ConditionalOnProperty("voglander.protocol-lab.enabled"=true)` + `@ConditionalOnProperty("sip.client.enabled"=true)`。

| 类 | 实现 SPI | 多实例? | 捕获→SSE |
|----|----------|---------|----------|
| `LabControlListener` | `ControlListener` | ✅ 观察者，��与 `CascadeControlHandler` 共存 | onPtzControl/onRecord/onGuard/onIFrame/onAlarmReset/... → `clientcmd.*` |
| `LabConfigListener` | `ConfigListener` | ✅ | onBasicParamConfig/onOsdConfig/... → `clientcmd.config.*` |
| `LabNotifyListener` | `NotifyListener` | ✅ | onBroadcastNotify → `clientcmd.broadcast` |
| `LabInviteListener` | `@EventListener ClientInviteEvent` | ✅ Spring 事件 | 设备收到点播 INVITE → `clientcmd.invite` |
| `LabRegisterStatusListener` | `@EventListener ClientRegisterSuccess/Failure/ChallengeEvent` | ✅ Spring 事件 | → `clientcmd.register.ok/fail/challenge` |

`LabControlListener` 把 `DeviceControlPtz.getPtzCmd()`（hex 串）解析为人类可读方向/速度（参考 `PTZInstructionBuilder`/`PTZControlEnum` 的反向解析），SSE data 同时带 `hex` 原文与 `parsed`，前端两者都展示——直观体现"底层 SIP 报文内容"。

### 4.3 关键约束：`QueryListener` 唯一性处理

`QueryListener` 全局唯一。两条可选路线：

- **路线 1（推荐，互斥）**：Lab 模式与级联模式互斥运行。
  - `CascadeQueryHandler` 增加 `@ConditionalOnProperty(name="voglander.protocol-lab.enabled", havingValue="false", matchIfMissing=true)`。
  - 新增 `LabQueryListener implements QueryListener`，`@ConditionalOnProperty("voglander.protocol-lab.enabled"=true)`。它对 `onCatalogQuery/onDeviceInfoQuery/onDeviceStatusQuery` ①推 `clientcmd.query.*`，②返回预置模拟响应（通道列表/设备信息），驱动 §2.3 回包闭环。
  - 风险：Lab 与级联不能同时启用——对"验证台"用途完全可接受（lab 是独立调试 profile）。
- **路线 2（组合委托）**：保留 `CascadeQueryHandler` 为唯一 bean，新增可选注入的 `LabQuerySink`，在其每个 `onXxxQuery` 开头调用 sink 推 SSE。侵入级联代码，**不推荐**（违背"未提交新代码可重构、已提交需兼容"——级联已提交）。

> 决策：采用**路线 1**。文档 §9 列为待确认（是否接受 lab/cascade 互斥）。

### 4.4 修改：`Gb28181ProtocolHandler` 增发 `device.*` SSE

在既有 switch 分支的业务调用**之后**追加 SSE 推送（解耦：经 `ApplicationEventPublisher` 或直接注入 `SseEventBus`；与既有 `handleAlarm` 用 `eventPublisher.publishEvent(AlarmCreatedEvent)` 的风格保持一致，建议同样走 `ApplicationEventPublisher` + 一个 `LabSseRelayListener` 收敛，避免 integration 直依赖 service 的 SSE 实现）。

- `handleRegister` 末尾 → `device.register`
- `Lifecycle.Online` → `device.online`；`Lifecycle.Offline` → `device.offline`
- `handleKeepalive` → `device.keepalive`（**节流**：心跳 30s 合并写已存在，SSE 同理按 deviceId 做 ≥5s 节流，避免刷屏）
- `handleCatalog` → `device.catalog`；`handleDeviceInfo` → `device.info`
- `Session.InviteOk/Bye` → `session.*`

依赖方向保护：`Gb28181ProtocolHandler` 在 `voglander-integration`，`SseEventBus` 在 `voglander-service`。为不破坏依赖方向，**新增 `common` 层事件**（如 `DeviceVisualEvent`）由 handler `publishEvent`，在 `voglander-web`/`voglander-service` 侧用 `@EventListener` 转 `SseEventBus.publish`。（与 `AlarmCreatedEvent` 完全同构。）

### 4.5 右侧下发：复用既有端点

右侧控制台**不新增后端**，直接调既有：
- 查目录：`POST /api/v1/device-cmd/query-catalog {deviceId}`
- 查设备信息：`POST /api/v1/device-cmd/query-info {deviceId}`
- 重启：`POST /api/v1/device-cmd/reboot {deviceId}`
- PTZ：`POST /api/v1/ptz/control {deviceId, channelId, command, speed}`
  - ✅ **D6 已修复（C3）**：`GbDeviceCommandService` 已含 `PTZ_VOCAB` 映射表（`GbDeviceCommandService.java:71`），把前端约定词 `UP/DOWN/LEFT/RIGHT/...` 翻译为规范枚举 `TILT_UP/TILT_DOWN/PAN_LEFT/PAN_RIGHT/...`。**前端直发 `UP/DOWN` 即可**，本方案无需再做 PTZ 兼容工作（原 Phase 2 该项删除）。
- 实时点播：`POST /api/v1/live/start {deviceId, channelId, streamMode}`（返回 9 路 playUrls；可在右侧嵌 `MediaPlayer` 组件播放 httpFlv）。

> 🟡 **C2 依赖前提（右→左指令的硬约束）**：右侧所有按 `deviceId` 下发的命令，其目标地址由 `VoglanderServerDeviceSupplier.getToDevice(deviceId)` 从 `deviceManager.getDtoByDeviceId()` 取 `device.getIp()/getPort()` 构造��`VoglanderServerDeviceSupplier.java:95-127`）。因此**右侧任何下发都以"左侧已注册成功、且 `tb_device` 落库的 ip/port = 客户端回环地址（127.0.0.1:5061）"为前提**。
> - 落库地址来自注册时的 remote address（`DeviceRegisterService.updateRemoteAddress`），自环下即为 `127.0.0.1:5061`。
> - 客户端监听点绑定地址 = `sip.client.domain`，而 `application-inte.yml:42` 已配 `domain: ${local.sip.client.ip:127.0.0.1}`——即 **`client.ip` 即可控制绑定，无需额外配 `client.domain`**（核验修正：此前担心的"回落局域网 IP"不成立，前提是 `inte` profile 激活，见 §5.1 C1）。
> - **UI 约束**：右侧命令区在该 deviceId 出现在设备列表（即 `device.register` 已到达）之前应禁用/置灰，避免对未注册设备发指令（必失败）。这同时是验收 LAB-01 → LAB-05 的天然时序。

---

## 5. 配置（新增 Profile：`application-lab.yml`）

### 5.1 SIP 自环身份与端口

```yaml
# application-lab.yml —— 协议验证台专用，仅覆盖 inte 中 local.sip.* 默认值，开启同进程 SIP 自环
# 启动 profile 必须为 dev,repo,inte,lab（inte 提供 sip:/gateway: 块，lab 覆盖其默认值）
local:
  sip:
    enable: true
    server:
      enabled: true
      ip: 127.0.0.1
      port: 5060
      serverId: 34020000002000000001      # 平台编码（右）
      domain: 34020000002000000001
    client:
      enabled: true
      clientId: 34020000001320000001      # 设备编码（左），与 serverId 不同（自环正确性前提，见 §1.4）
      ip: 127.0.0.1                        # ← 同时决定客户端监听点绑定地址（inte.yml:42 domain←此值）
      port: 5061
      realm: 34020000
      registerExpires: 3600
      password: "123456"                   # 与 server 端校验口令一致（对齐 inte 默认值）
  gateway:
    node-id: node-lab
    gb28181:
      store: { type: memory }               # 单机自环用内存 InviteContextStore

voglander:
  protocol-lab:
    enabled: true                            # ★ 总开关：激活 LabClientController + Lab*Listener，互斥级联 QueryHandler
  event:
    shard:
      enabled: true
```

启动（🔴 **C1 修正**：必须带 `inte`）：

```bash
mvn spring-boot:run -pl voglander-web -Dspring-boot.run.profiles=dev,repo,inte,lab
```

> 🔴 **C1 —— 为什么不能漏 `inte`**：`sip:` / `gateway:` 配置块（连同 `${local.sip.*}`、`${local.gateway.*}` 占位符映射）**只存在于 `application-inte.yml`**。若只用 `dev,repo,lab`，则 `sip.enable` 等键根本不存在 → `ServerStart`（`@ConditionalOnProperty("sip.enable"="true")`）不激活 → 5060/5061 都不绑定 → 自环无从谈起。`lab` 的角色是**覆盖 `inte` 里 `local.sip.*` 的默认值**（开启 server+client、设回环身份），不是替代 `inte`。
>
> 配置键链路（已核验）：`lab.yml` 设 `local.sip.client.ip:127.0.0.1` → `inte.yml:42` `sip.client.domain ← ${local.sip.client.ip}` → `ServerStart.java:51` 用 `sipClientProperties.getDomain()` 绑定客户端监听点到 `127.0.0.1:5061`。**链路自洽，无需新增 `client.domain`（C2 澄清）**。

> ⚠ **REGISTER 鉴权对齐**：自环 REGISTER 会走 server 端的 401 Challenge → client Digest 重发。务必确认 `lab.yml` 的 `client.password` 与 server 端校验口令一致（`inte.yml` 默认 `local.sip.client.password:123456`，本方案 §5.1 示例用 `12345678`——二者取其一，保持两端一致即可；若 server 未启用鉴权则直接 200，无需对齐）。

> 注意 1.0.5 验收发现的启动阻断项（见 [[voglander-1-0-5-visualization-acceptance]]）：`application-inte.yml` 重复键（D1/D2 已修）、SQLite 建表 D3。lab profile 若复用 SQLite，需确保 `sql/voglander-sqlite.sql` 经 `sqlite3 app.db < ...` 完整建表（DBA 路径），规避 `ResourceDatabasePopulator` 分句缺陷。

### 5.2 安全注意

- `LabClientController` 与 `Lab*Listener` 全部受 `voglander.protocol-lab.enabled` 门控，**生产 profile 不激活**，杜绝"任意触发设备指令"风险。
- SSE 仍需 JWT（`?token=`），不放宽鉴权。

---

## 6. 前端实现（vue-vben-admin / apps/web-antd）

### 6.1 文件结构

```
apps/web-antd/src/
├── router/routes/modules/protocol-lab.ts          # 新增路由（menu order, icon, title=$t）
├── api/protocol-lab.ts                             # 新增 API 模块（client/* + 复用 ptz/live/device-cmd）
├── composables/useSseEvents.ts                     # 新增 EventSource 封装（带 token、topic、自动重连、去重）
├── locales/langs/{zh-CN,en-US}/protocol-lab.json   # i18n: protocolLab.{client|server|event|action}.*
└── views/protocol-lab/
    ├── index.vue                                   # 两栏布局容器 + SSE 订阅 + 状态分发
    ├── components/ClientPanel.vue                  # 左：设备 UA（注册/心跳/上报按钮 + "收到指令"时间线）
    ├── components/ServerPanel.vue                  # 右：平台（设备列表实时 + PTZ/点播/查询 + 事件时间线）
    ├── components/SipTimeline.vue                  # 复用：SIP/事件阶梯时间线（in/out 箭头 + 报文摘要）
    └── data.ts                                     # 按钮元数据、PTZ 枚举映射、topic→展示文案
```

### 6.2 SSE composable（核心）

```ts
// useSseEvents.ts （要点，非完整代码）
export function useSseEvents(topics: string[]) {
  const events = ref<LabEvent[]>([]);
  const seen = new Set<string>();           // 去重 (topic+ts+seq)，规避 D8 双投
  let es: EventSource | null = null;
  const token = useAccessStore().accessToken;

  function connect() {
    const url = `${apiURL}/api/v1/stream/events?topics=${topics.join(',')}&token=${token}`;
    es = new EventSource(url);
    // 后端 SseEmitter 用 event 名=topic 发送 → 按 topic addEventListener
    topics.forEach(t => es!.addEventListener(t, (e) => onEvent(t, e)));
    es.onerror = () => { es?.close(); setTimeout(connect, 3000); };  // 自动重连
  }
  onMounted(connect);
  onUnmounted(() => es?.close());
  return { events, /* 按 topic 分组的 computed */ };
}
```

> ✅ **已证实（原存疑点关闭）**：`RedisBackedSseEventBus.publishLocal` 用 `SseEmitter.event().name(event.getTopic())` 发送（`:92-94`），SSE `event:` 名即 topic，data 是 `event.getData()` 的 JSON。故上面 `addEventListener(topic)` 的写法直接可用，**无需** `onmessage` + 手动 `JSON.parse(e.data).topic` 兜底。注意 data 是 `event.getData()` 序列化结果（不含外层 `{topic,data}` 包裹），前端事件回调里直接 `JSON.parse(e.data)` 得到 data 本体。

### 6.3 两栏布局（Tailwind grid + Ant Design Vue）

沿用项目约定（`@vben/common-ui` 的 `Page`、`ant-design-vue` 组件、Tailwind 栅格）：

```vue
<template>
  <Page>
    <div class="grid grid-cols-2 gap-4 h-full">
      <ClientPanel :received="clientcmdEvents" @action="onClientAction" />
      <ServerPanel :devices="deviceList" :events="deviceEvents" @command="onServerCommand" />
    </div>
  </Page>
</template>
```

- 左 `ClientPanel`：身份卡片（clientId/port）+ 操作按钮组（注册/注销/心跳/自动心跳/上报目录/上报设备信息/上报告警）+ **"收到指令"时间线**（`clientcmd.*` 实时流，显示 hex + 解析）。
- 右 `ServerPanel`：**设备列表**（`device.register`/`online`/`offline` 实时增删改 + 状态徽标）+ 选中设备后的命令区（PTZ 方向盘/点播/查目录/查设备信息/重启）+ **平台事件时间线**（`device.*`/`session.*`/`alarm.new`）+ 可选视频播放器。
- 中部可选 `SipTimeline`：把左右两路事件按时间合并成一条 SIP 阶梯图（⬆ 设备→平台 / ⬇ 平台→设备），直观呈现协议往返。

### 6.4 i18n（`module.entity.action` 模式）

```json
{ "protocolLab": {
    "title": "GB28181 协议验证台",
    "client": { "title":"设备端(Client)", "register":"注册", "keepalive":"心跳", "pushCatalog":"上报目录", "received":"收到指令" },
    "server": { "title":"平台端(Server)", "devices":"在线设备", "ptz":"云台控制", "queryCatalog":"查询目录", "play":"实时点播" },
    "event":  { "register":"设备注册", "online":"上线", "ptz":"云台指令", "catalogQuery":"目录查询" }
} }
```

---

## 7. 数据契约（关键 Payload）

### 7.1 左侧注册请求/SSE
```jsonc
// POST /api/v1/lab/client/register
{ "expires": 3600 }
// → AjaxResult.success(callId)

// SSE device.register (右收)
{ "topic":"device.register", "data":{ "deviceId":"34020000001320000001","remoteIp":"127.0.0.1","remotePort":5061,"transport":"UDP","expire":3600,"ts":1733...} }

// SSE clientcmd.register.ok (左收)
{ "topic":"clientcmd.register.ok", "data":{ "clientId":"34020000001320000001","ts":1733...} }
```

### 7.2 右侧 PTZ → 左侧收到
```jsonc
// POST /api/v1/ptz/control
{ "deviceId":"34020000001320000001","channelId":"3402000001320000001","command":"TILT_UP","speed":128 }

// SSE clientcmd.ptz (左收) —— ★ "展示收到指令"
{ "topic":"clientcmd.ptz",
  "data":{ "platformId":"34020000002000000001","channelId":"...","ptzCmd":"A50F01...","parsed":{"direction":"UP","speed":128},"ts":1733...} }
```

### 7.3 右侧查目录 → 左侧收到并回包 → 右侧收到
```jsonc
// POST /api/v1/device-cmd/query-catalog  { "deviceId":"34020000001320000001" }
// SSE clientcmd.query.catalog (左收): { "platformId":"...","sn":"1" }
// LabQueryListener 返回模拟 DeviceResponse(4 通道) → 框架回包
// SSE device.catalog (右收): { "deviceId":"...","channelCount":4 }
```

---

## 8. 风险与缓解

| # | 风险 | 缓解 |
|---|------|------|
| R1 | `QueryListener` 唯一性 → Lab 与级联冲突 | §4.3 路线 1：`@ConditionalOnProperty` 互斥；lab 为独立 profile。**已核验**：`QueryListener.java:38` 明确 ≥2 bean fail-fast |
| R2 | SSE 单机双投（D8） | 前端 `(topic,ts,seq)` 去重；或后端 publish 加 origin 抑制回环。**已核验**：`RedisBackedSseEventBus.publish:74-82` 确为本地直发 + Redis 广播 |
| R3 | ~~PTZ 词表不匹配（D6）~~ | ✅ **已在代码修复**：`GbDeviceCommandService.java:71` `PTZ_VOCAB` 映射 `UP→TILT_UP/...`。前端直发 `UP/DOWN` 即可，本项关闭 |
| R4 | 心跳/事件刷屏 | `device.keepalive` 按 deviceId ≥5s 节流；时间线虚拟滚动 |
| R5 | 自环身份冲突（client 给自身 server 注册） | clientId≠serverId、5061≠5060、realm 对齐（§5.1）。**这是 §1.4 自环路由正确性的硬前提，非可选** |
| R6 | 依赖方向破坏（integration→service） | handler 只 `publishEvent(common 事件)`，web/service 侧 `@EventListener` 转 SSE。**已核验**：`AlarmCreatedEvent`（common）+ `Gb28181ProtocolHandler.java:350` 已是同构先例 |
| R7 | SQLite 建表 D3 阻断启动 | lab profile 用 `sqlite3 app.db < sql/voglander-sqlite.sql` 预建，或切 MySQL（见 [[voglander-1-0-5-visualization-acceptance]] D3） |
| R8 | 异步时序：注册 200 OK 与 SSE 到达有延迟 | 前端时间线容忍乱序，按 ts 排序；右侧设备列表用 upsert 语义 |
| R9 | EventSource 不支持自定义 header（token 只能放 URL） | 既有 `SseController.java:34` 已支持 `?token=` 且强制校验，与现状一致 |
| R10 | 🔴 **自环为新行为，无现成测试覆盖** | §1.4 已从代码分析证明���由正确，但 S1 必须以 "curl 注册 → SSE 收 `device.register` + 设备落库 status=1" 作为第一个真实验证闸口，先于任何前端工作 |
| R11 | 🔴 **启动 profile 漏 `inte` → SIP 不绑定（C1）** | 启动命令固定 `dev,repo,inte,lab`；§5.1 已说明配置键链路。可加一条启动自检日志（`ServerStart.run` 打印两个 LP 绑定结果）便于排障 |
| R12 | 跨向 MESSAGE 的冗余 `checkDevice` WARN 刷屏 | §1.4 已说明为预期噪音；lab 模式可将 `Client/ServerMessageRequestProcessor` 的"不匹配" logger 调至更低级别或加采样 |

---

## 9. 待确认项（需用户拍板）

1. **运行形态**：接受"同进程 SIP 自环（方案 A）"吗？还是要求左侧设备是真正独立的第三方/外部摄像头/另一进程？（影响是否需要双进程部署）— 默认按方案 A 实施。**【核验补充】方案 A 在协议栈层面已证实可行（§1.4），无技术阻碍；但它是新行为、无现成测试，建议先按 A 打通 S1 闸口再决定是否还需要 B。**
2. **Lab 与级联互斥**：`voglander.protocol-lab.enabled=true` 时禁用 `CascadeQueryHandler`（§4.3 路线 1）可接受吗？
3. **SIP 报文级时间线**（`sip.trace`，§3 可选）：是否需要 Phase 3 实现真实 SIP 请求/响应行抓取（需在 SIP 日志层加切面），还是语义事件时间线（注册/PTZ/目录…）已足够？
4. **视频联调**：右侧点播是否要嵌入实时播放器（依赖 ZLM 真实节点，1.0.5 验收 ZLM 在 8082）？还是本期只验证 SIP 信令、播放留待后续？
5. **前端落位**：放在现有"媒体管理"菜单下，还是新建顶级"协议验证"菜单？

---

## 10. 实施阶段（建议）

| Sprint | 范围 | 交付 | DoD |
|--------|------|------|-----|
| **S1 后端·感知注册** | §4.4 `device.*` SSE + §5 lab profile + §4.1 register/keepalive/unregister | 右侧能实时看到设备注册/上线/离线 | curl 注册 → SSE 收到 `device.register`；设备落库 status=1 |
| **S2 后端·指令回显** | §4.2 `Lab*Listener` + §4.3 `LabQueryListener` + register 状态事件 | 左侧能展示收到的 PTZ/目录查询/配置 | 右发 PTZ → SSE `clientcmd.ptz`（带 hex+解析）；右查目录 → 左回包 → 右 `device.catalog` |
| **S3 后端·补全** | catalog/device-info/alarm 上报端点（PTZ D6 已修，无需再做） | 左侧主动上报全链路 | 左上报告警 → 右 `alarm.new`；左上报目录 → 右通道列表刷新 |
| **S4 前端·骨架** | 路由 + `useSseEvents` + 两栏布局 + 设备列表 + 操作按钮 | 页面可用，双向闭环可视 | 注册/PTZ/查目录三条闭环在 UI 上可见 |
| **S5 前端·打磨** | SipTimeline 阶梯图 + hex 解析展示 + i18n + 去重/重连 + 可选播放器 | 验收级可视化 | §11 验收矩阵全绿 |

> 每个 Sprint 后端遵循 TDD：先 `@MockitoSpyBean SseEventBus` 验证 publish 调用（参考 `doc/1.0.5/GB28181-2022-TDD-PLAN.md` 的事件边界 E2E 模式），再前端联调。

---

## 11. 验收矩阵（Definition of Done）

| 编号 | 场景 | 操作 | 期望 | 协议点 |
|------|------|------|------|--------|
| LAB-01 | 设备注册 | 左[注册] | 右设备列表出现该设备(在线) + 时间线 REGISTER 200 OK；左 `register.ok` | §7.1 REGISTER |
| LAB-02 | 设备注销 | 左[注销] | 右设备列表标离线 | Expires:0 |
| LAB-03 | 心跳保活 | 左[自动心跳] | 右 `device.keepalive` 周期刷新(节流后) | Keepalive |
| LAB-04 | 目录查询回环 | 右[查目录] | 左[收到目录查询] → 右通道列表出现 N 通道 | Catalog 双向 |
| LAB-05 | PTZ 回显 ★ | 右[PTZ 上 speed=128] | 左[收到 PTZ 上, hex+解析] + 右 200 OK。**前端直发 `UP`（D6 已修，`PTZ_VOCAB` 映射）** | §7.2 DeviceControl/PTZCmd |
| LAB-06 | 设备信息查询 | 右[查设备信息] | 左[收到查询]→回包→右 `device.info`(厂商/型号) | DeviceInfo 双向 |
| LAB-07 | 告警上报 | 左[上报告警] | 右 `alarm.new` + 告警落库 | Notify.Alarm |
| LAB-08 | 重启下发 | 右[重启] | 左[收到 TeleBoot 指令] | Control.Reboot |
| LAB-09 | 实时点播(可选) | 右[点播] | 左[收到 INVITE] + (可选)右视频可播 | Invite.Play |
| LAB-10 | 断线重连 | 杀掉 SSE | 前端 3s 自动重连，事件不丢序 | EventSource |

- [ ] 全部 SSE 经 JWT 鉴权
- [ ] lab profile 关闭时，所有 Lab 端点/监听器不注册（生产安全）
- [ ] 前端事件去重生效（无 D8 双显）
- [x] PTZ `UP/DOWN` 可用（D6 已修，`PTZ_VOCAB` 映射，见 C3）

---

## 附录 A：35 个 GB28181 事件类型（框架 forwarder 全集，已验证）

```
Lifecycle: Register / Online / Offline / RegisterChallenge / RemoteAddressChanged
Notify:    Keepalive / Alarm / MediaStatus / MobilePosition / SnapShotFinished / UpgradeResult / VideoUpload
Response:  Catalog / DeviceInfo / DeviceStatus / RecordInfo / PtzPosition / PresetQuery / Config / ConfigDownload
           / SdCardStatus / HomePosition / CruiseTrack(List) / Subscribe / NotifyUpdate / DeviceInfoError/Request
Session:   InviteOk / InviteFailure / InviteTrying / Ack / Bye / ByeError / ServerInvite
Query(出): Catalog / DeviceInfo / DeviceStatus / RecordInfo / AlarmQuery / PresetQuery / PtzPosition / MobilePosition / ...
Control(出):Ptz / PtzPrecise / Preset / Guard / Record / Reboot / IFrame / AlarmReset / DragZoomIn/Out / HomePosition
           / Auxiliary / Cruise / Scan / FormatSDCard / TargetTrack
Config(出):BasicParam / ConfigDownload / Osd
Invite(出):Play / Playback / PlaybackControl / Download / Talk / Ack / Bye
Device(出):Broadcast / SnapShot / Upgrade
Subscribe: Alarm
```

## 附录 B：关键文件索引

| 用途 | 文件 |
|------|------|
| SIP 双角色绑定 | `voglander-integration/.../wrapper/gb28181/start/ServerStart.java` (`:46,51`) |
| 入站事件 switch | `voglander-integration/.../wrapper/gb28181/handler/Gb28181ProtocolHandler.java`（改：增发 device.* 事件）(`:79` switch 起点，`:350` AlarmCreatedEvent 先例) |
| 入站 SPI 入口 | `voglander-integration/.../wrapper/gb28181/notifier/VoglanderBusinessNotifier.java` |
| 跨层事件解耦先例 | `voglander-common/.../common/event/AlarmCreatedEvent.java` + `voglander-service/.../service/live/LiveStreamEventListener.java` |
| SIP 消息分发机制 | `sip-proxy/sip-common/.../transmit/AbstractSipListener.java`（`:232` 按 method 分发，`:47/52` static Map） |
| 客户端 MESSAGE 身份过滤 | `sip-proxy/.../client/transmit/request/message/ClientMessageRequestProcessor.java`（`:46`） |
| 服务端 MESSAGE 身份过滤 | `sip-proxy/.../server/transmit/request/message/ServerMessageRequestProcessor.java`（`:43`） |
| 客户端收指令 SPI | `gbproxy/client/api/{Control,Config,Query,Notify,Subscribe}Listener`（`ControlListener.java:24` 多实例；`QueryListener.java:38` 唯一） |
| 客户端发指令 | `gbproxy/client/transmit/cmd/ClientCommandSender`（`:712,741,126,153,235,95`） |
| 服务端命令门面（含 PTZ_VOCAB） | `voglander-service/.../service/command/impl/GbDeviceCommandService.java`（`:71` PTZ_VOCAB） |
| 服务端 ToDevice 地址解析 | `voglander-integration/.../wrapper/gb28181/supplier/VoglanderServerDeviceSupplier.java`（`:95-127`） |
| SSE 总线/控制器 | `voglander-service/.../service/sse/RedisBackedSseEventBus.java`（`:74-82` publish，`:92` event 名=topic）+ `SseController.java`（`:34` token 校验） |
| 既有右侧端点 | `web/api/{ptz/PtzController, device/controller/DeviceCmdController, live/controller/LivePlayController}.java` |
| 既有客户端监听先例 | `voglander-integration/.../wrapper/gb28181/cascade/{CascadeControlHandler,CascadeQueryHandler,CascadeClientScheduler}.java` |
| lab profile | `voglander-integration/src/main/resources/application-lab.yml`（新增）；`application-inte.yml`（`:42` domain←client.ip 映射） |
| 前端基线 | `vue-vben-admin/apps/web-antd/src/{router/routes/modules, api, views/media/*}` |

## 附录 C：核验证据索引

本文各处 ✅/🔴/🟡 标注所依据的代码行，供实施时快速交叉比对。

| 断言 | 文件:行 | 结论 |
|------|---------|------|
| ServerStart 同时绑定 5060+5061 | `ServerStart.java:46,51` | ✅ 属实 |
| 两个端口共用同一 `sipListener` bean | `ServerStart.java:38,46,51` | ✅ 属实，分发无端口区分 |
| processRequest 只按 method 分发 | `AbstractSipListener.java:232` | ✅ 属实，无端口/角色路由 |
| REQUEST_PROCESSOR_MAP 是 static | `AbstractSipListener.java:47,52` | ✅ 属实，跨实例共享 |
| Client MESSAGE 处理器按 toUserId 过滤 | `ClientMessageRequestProcessor.java:46` | ✅ 属实，clientId≠serverId 即不串 |
| Server MESSAGE 处理器按 checkDevice 过滤 | `ServerMessageRequestProcessor.java:43` | ✅ 属实 |
| QueryListener 全局唯一 ≥2 fail-fast | `QueryListener.java:38` | ✅ 属实（javadoc 明确） |
| ControlListener 多实例观察者 | `ControlListener.java:24` | ✅ 属实（javadoc 明确） |
| SSE event 名 = topic | `RedisBackedSseEventBus.java:92-94` | ✅ 属实，§6.2 存疑点关闭 |
| publish 先本地直发再 Redis（D8 来源） | `RedisBackedSseEventBus.java:74-82` | ✅ 属实，单机有 Redis 则双投 |
| AlarmCreatedEvent 在 common 层 | `voglander-common/event/AlarmCreatedEvent.java` | ✅ 属实，§4.4 DeviceVisualEvent 同构先例 |
| handler 用 publishEvent 跨层解耦 | `Gb28181ProtocolHandler.java:350` | ✅ 属实 |
| handler 当前不发 device.* SSE | `Gb28181ProtocolHandler.java:79-203`（全 switch，无 SseEventBus 调用） | ✅ 属实，gap 确认 |
| PTZ_VOCAB 映射已存在（D6 已修） | `GbDeviceCommandService.java:71` | ✅ 属实，C3 关闭 |
| getClientFromDevice() 已有实现 | `VoglanderClientDeviceSupplier.java:44` | ✅ 属实 |
| getToDevice 从 tb_device 取 ip/port | `VoglanderServerDeviceSupplier.java:95-127` | ✅ 属实，C2 隐性依赖确认 |
| sip.client.domain ← local.sip.client.ip | `application-inte.yml:42` | ✅ 属实，无需额外配 client.domain |
| sip: 块只在 inte 中存在 | `application-inte.yml:1-43` | 🔴 **C1**：启动必须带 inte |
| Cascade 的 to 指向外部平台（非自身） | `CascadeClientScheduler.java:125` | ⚠️ 自环为新行为，无现成测试 |
