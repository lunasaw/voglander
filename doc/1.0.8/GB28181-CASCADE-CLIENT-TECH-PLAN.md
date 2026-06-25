# GB28181-2022 级联下级平台完整客户端技术方案

> 版本：1.0.8 ｜ 拟定日期：2026-06-14 ｜ 分支：`0608_dev`
> 代码基线：sip-gateway / gb28181 **1.8.5**、zlm **1.0.11**
> 关联文档：`doc/1.0.7/ARCHITECTURE.md`（§7 GB28181 集成）、`GB28181-SUBSCRIPTION-TECH-PLAN.md`（设备订阅——本平台作上级订设备，本方案是其**镜像反向**）、`PROTOCOL-ARCHITECTURE-GENERICITY-PLAN.md`
> 协议依据：GB/T 28181-2022 §9（互联）、§10（控制）、§11（信息查询）、附录 A（MANSCDP）、RFC 3261/6665

---

## 目录

1. [需求与角色定位](#1-需求与角色定位)
2. [核心概念：上下级方向](#2-核心概念上下级方向)
3. [协议背景（级联下级平台）](#3-协议背景级联下级平台)
4. [现状核验与缺口](#4-现状核验与缺口)
5. [总体设计](#5-总体设计)
6. [数据模型](#6-数据模型)
7. [后端实现](#7-后端实现)
8. [前端实现](#8-前端实现)
9. [配置项](#9-配置项)
10. [时序图](#10-时序图)
11. [测试方案](#11-测试方案)
12. [实施阶段与验收](#12-实施阶段与验收)
13. [风险与协议合规](#13-风险与协议合规)
14. [附录：框架 API 核验表 + 改动清单](#14-附录框架-api-核验表--改动清单)

---

## 1. 需求与角色定位

### 1.1 用户需求（原话拆解）

> 「围绕 GB28181-2022 作为级联的下级平台；完整的客户端所有协议实现；包括但不限于 通道目录订阅推送；告警推送；点播转发；录像查询返回标准协议」

拆解为 5 项核心能力 + 1 项总纲：

| # | 能力 | 协议方向 | GB28181-2022 章节 |
|---|------|----------|-------------------|
| R0 | **完整客户端所有协议实现**（总纲） | 本平台作为 SIP UAC/UAS 客户端，对上级暴露全部下级平台行为 | §9~§11 全集 |
| R1 | **通道目录订阅推送** | 上级 SUBSCRIBE(Catalog) → 本平台应答 + 目录变更时主动 NOTIFY 推送上级 | §11.2.2 + §9.11 |
| R2 | **告警推送** | 上级 SUBSCRIBE(Alarm) → 本平台应答 + 下级设备告警时主动 NOTIFY 推送上级 | §11.2.4 + §9.11 |
| R3 | **点播转发** | 上级 INVITE(实时/回放) → 本平台向真实下级 IPC 拉流 → 经 ZLM 转推上级 | §9.2 + §9.3 |
| R4 | **录像查询返回标准协议** | 上级查询 RecordInfo → 本平台向真实设备查录像 → 标准 `<RecordInfo>` 应答上级 | §11.2.3 |

### 1.2 范围边界

| 纳入（本方案 P0~P3） | 不纳入（明确排除） |
|------|------|
| 级联注册/注销/保活（Register/Keepalive）全生命周期 | 本平台作为**上级**去订阅真实设备（已在 1.0.7 实现） |
| 目录查询应答（Catalog）+ 目录订阅 + 目录变更主动推送 | 多级级联（下级的下级，N 级树形）——架构预留，本期两级 |
| 设备信息/状态查询应答（DeviceInfo/DeviceStatus） | 语音对讲（Broadcast/AudioTalk）——预留 |
| **录像查询应答**（RecordInfo，含向真实设备转查 + 聚合回包） | GB35114 安全加密（SVAC/数字证书）——预留 |
| 告警订阅应答 + 告警主动推送（Alarm SUBSCRIBE/NOTIFY） | 上级对本平台的远程配置写入（SetConfig）——只读 ConfigDownload |
| 移动位置订阅应答 + 位置主动推送（MobilePosition） | 前端独立级联拓扑可视化大屏——本期仅管理列表 |
| **实时点播转发**（INVITE play → ZLM startSendRtp，已有雏形需补全） | |
| **回放点播转发**（INVITE playback + 回放控制 PLAY/PAUSE/SCALE） | |
| 设备控制透传（PTZ + 录像控制 + 布撤防 + 远程启动等 ControlListener 全集） | |
| 配置查询/预置位查询等 QueryListener 全集应答 | |
| 级联平台 + 级联通道的 Web 管理（CRUD）+ 前端管理页 | |

### 1.3 与 1.0.7 订阅方案的关系（镜像对称）

1.0.7 方案中 voglander 是**上级**，向真实设备**发起** SUBSCRIBE、**接收** NOTIFY 并落库。
本方案中 voglander 是**下级**，**接收**上级的 SUBSCRIBE、在本地数据变更时**发起** NOTIFY 推送上级。

两者复用同一套框架命令/事件，但方向相反、角色互换。**关键代码不复用**（一个用 `ServerCommandSender`，一个用 `ClientCommandSender`），但**数据来源同源**（同一套 `tb_device`/`tb_device_channel`/`tb_alarm`），通过事件驱动桥接。

---

## 2. 核心概念：上下级方向

GB28181 级联中，**上级平台（superior）**通过 SIP 信令管理**下级平台（inferior）**。下级平台对上级而言，行为等同于一个「超级设备」：它注册到上级、应答上级的查询、被上级订阅、接受上级的点播/控制。

```
┌─────────────────────────────────────────────────────────────┐
│  上级平台 (Superior, e.g. 公安部 / 省级监控中心)               │
│  - 我方作为 SIP 客户端(UAC)注册到它                            │
│  - 它向我方发: REGISTER应答 / SUBSCRIBE / INVITE / 查询 / 控制 │
└───────────────▲───────────────────────────┬──────────────────┘
                │ ① REGISTER/Keepalive(我方主动)│ ②③④ 上级下发
                │                             ▼
┌───────────────┴────────────────────────────────────────────┐
│  voglander (作为级联下级平台 Inferior)                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ gb28181-client 客户端栈 (sip.client.enabled=true)      │  │
│  │  CascadeClientScheduler  → 注册/保活上级               │  │
│  │  CascadeQueryHandler      → 应答 Catalog/Info/Record   │  │
│  │  CascadeSubscribeHandler  → 接收订阅 + 登记主动推送     │  │
│  │  CascadeControlHandler    → PTZ/录像控制透传            │  │
│  │  CascadeMediaInviteListener→ 点播转发(实时/回放)        │  │
│  │  CascadeNotifyPublisher   → 目录/告警/位置主动推送上级  │  │
│  └──────────────────────────────────────────────────────┘  │
│  数据源: tb_device / tb_device_channel / tb_alarm           │
│  级联映射: tb_cascade_platform / tb_cascade_channel         │
└───────────────▲─────────────────────────────────────────────┘
                │ 我方作为上级(server栈)管理真实设备(已有)
                ▼
┌─────────────────────────────────────────────────────────────┐
│  真实下级设备 (GB28181 IPC / NVR, 注册到 voglander server栈)  │
└─────────────────────────────────────────────────────────────┘
```

> **voglander 同时扮演两个角色**：对真实 IPC 是「上级平台」（server 栈，1.0.6/1.0.7 已实现），对省级中心是「下级平台」（client 栈，本方案）。两个 SIP 栈在同一进程内通过 `sip.server.enabled` / `sip.client.enabled` 分别启停。

### 2.1 编码与寻址（协议合规铁律）

- 上级看到的通道是 **`cascade_channel_id`**（本平台对上暴露的 20 位国标编码），与真实设备的 `local_channel_id` 通过 `tb_cascade_channel` 一对一映射。
- 默认 `cascade_channel_id = local_channel_id`（透传），但允许重映射（上级编码体系与本地不同时）。
- **禁止** `channelId = platformId + 后缀` 的前缀猜测（CLAUDE.md 铁律）；通道与平台的从属关系**只**由 `tb_cascade_channel.platform_id` 表达。
- 主动推送上级时，From=本平台 `local_client_id`，To=上级 `platform_id`，寻址走 `tb_cascade_platform` 配置，**不拼接**。

---

## 3. 协议背景（级联下级平台）

### 3.1 级联注册（§9.1）

下级以 SIP `REGISTER` 注册到上级，Digest 鉴权（上级回 401 带 nonce，下级带 Authorization 重发）。注册成功后周期性 `REGISTER`（expires 续期）+ `MESSAGE(Keepalive)` 心跳（默认 60s，连续 3 次超时上级判定下级离线）。

> **协议要点**：注册续期（REGISTER expires）与保活心跳（Keepalive MESSAGE）是**两个独立机制**。当前 `CascadeClientScheduler` 仅做了周期 REGISTER（55s），**缺独立 Keepalive MESSAGE 心跳**——本方案 P1 补全。

### 3.2 上级查询应答（§11.2，QueryListener 全集）

上级向下级发 MANSCDP 查询（SIP MESSAGE 承载 XML），下级应答对应响应。框架 `QueryListener` 暴露 13 个查询回调，下级平台需实现的核心子集：

| 查询 | 回调 | 应答实体 | 下级平台语义 |
|------|------|----------|--------------|
| 目录查询 | `onCatalogQuery` | `DeviceResponse`（DeviceItem 列表） | 返回本平台对上暴露的全部级联通道 ✅已有 |
| 设备信息 | `onDeviceInfoQuery` | `DeviceInfo` | 返回本平台厂商/型号/通道数 ✅已有 |
| 设备状态 | `onDeviceStatusQuery` | `DeviceStatus` | 平台自身在线 ✅已有 |
| **录像查询** | `onRecordInfoQuery` | `DeviceRecord`（RecordItem 列表） | **向真实设备转查录像 → 聚合应答**（R4，❌缺） |
| 配置查询 | `onConfigDownloadQuery` | `DeviceConfigResponse` | 返回通道基本配置（可选，P3） |
| 预置位查询 | `onPresetQuery` | `PresetQueryResponse` | 透传真实设备预置位（可选，P3） |
| 移动位置查询 | `onMobilePositionQuery` | `MobilePositionNotify` | 返回通道最新 GPS（P3） |

### 3.3 上级订阅 + 下级主动推送（§9.11 + §11.2，R1/R2）

上级向下级 `SUBSCRIBE`（Event: Catalog / presence.alarm / presence.mobile），下级 `200 OK` 建立 dialog 并**记录 dialog 信息**；之后下级在数据变更时通过该 dialog 主动 `NOTIFY`（框架以 MESSAGE 承载）推送上级。

框架 `SubscribeListener`（client 栈）暴露 4 个订阅回调：`onCatalogSubscribe` / `onAlarmSubscribe` / `onMobilePositionSubscribe` / `onPtzPositionSubscribe`，回调为 `void`——**下级须自行登记订阅、在变更时调 `ClientCommandSender.sendXxxNotify` 主动推送**。

> **关键技术点**：订阅回调返回 `void`，框架只把订阅请求转给我方。我方必须：(a) 持久化「哪个上级订了哪类信息 + dialog 标识」；(b) 在本地数据变更（设备告警/通道上下线/位置更新）时，遍历订阅了该类信息的上级，逐个主动推送。

### 3.4 上级点播（§9.2/§9.3，R3）

上级向下级 `INVITE`（SDP 含上级的收流地址/端口/SSRC），下级：
1. 向真实下级设备发起拉流（复用 server 栈 `inviteRealTimePlay` / `invitePlayback`）；
2. 流到达 ZLM 后，调 ZLM `startSendRtp` 把流转推到上级 SDP 指定地址；
3. 回 `200 OK`（SDP 含本平台的发流参数）；上级回 `ACK`，媒体流建立；
4. 上级 `BYE` → 下级 `stopSendRtp` + 停止真实设备拉流。

回放额外支持 `INFO`（PLAY/PAUSE/SCALE 倍速/拖动），下级透传给真实设备。

> **现状**：`CascadeMediaInviteListener` 已有实时点播雏形（INVITE→ensureStream→startSendRtp→200OK），但：(a) 回 200OK **未带 SDP**（注释「被动推流无需 SDP」存疑，标准要求带本端发流 SDP）；(b) **无回放点播**；(c) **无回放控制 INFO 透传**。本方案 P2 补全。

### 3.5 上级控制（§10，ControlListener 全集）

上级向下级 `MESSAGE(DeviceControl)` 下发控制，下级透传给真实设备。框架 `ControlListener` 15 个回调：PTZ / 录像控制 / 布撤防 / 远程启动 / 看守位 / 设备升级 / 抓图 / 目标跟踪 等。**现状仅实现 `onPtzControl`**，本方案 P3 补录像控制 + 布撤防等核心控制透传。

---

## 4. 现状核验与缺口

> 对 `0608_dev` 源码 + `gb28181-client-1.8.5.jar`（实际包 `io.github.lunasaw.gbproxy.client.*`）逐项核验（见附录 §14.1 API 核验表，2026-06-14 已坐实）。

### 4.1 已具备（无需重建）

**数据与持久化**：`tb_cascade_platform` / `tb_cascade_channel` 两表 + `CascadePlatformDO/DTO/Mapper/Service/Manager` + `CascadeChannelDO/DTO/Mapper/Service/Manager` + `CascadeAssembler` 齐全。

**级联客户端骨架**（`wrapper/gb28181/cascade/`）：
| 类 | 职责 | 状态 |
|----|------|------|
| `CascadeClientScheduler` | 多平台独立注册 + 周期续期 | ✅ 注册有，⚠️缺独立 Keepalive |
| `CascadeClientRegisterListener` | 注册成功/失败 → 更新 register_status | ✅ |
| `CascadeDeviceSupplier` | 构造 From/To Device | ✅ 但 buildFromDevice 端口写死 5070 |
| `CascadeQueryHandler` | 应答 Catalog/Info/Status 查询 | ⚠️ 仅 3/13，缺 Record 等 |
| `CascadeControlHandler` | PTZ 控制透传 | ⚠️ 仅 PTZ 1/15 |
| `CascadeMediaInviteListener` | 实时点播 INVITE/BYE 转发 | ⚠️ 实时有雏形，缺回放、缺 SDP 回包、缺真实拉流闭环 |

### 4.2 voglander 缺口（本方案要补）

| # | 缺口 | 对应需求 | 落点 |
|---|------|----------|------|
| C1 | 缺独立 Keepalive MESSAGE 心跳（仅周期 REGISTER） | R0 | `CascadeClientScheduler` 补 keepalive 任务 |
| C2 | 缺级联订阅接收 + 持久化（上级订了哪类信息/dialog） | R1/R2 | 新增 `CascadeSubscribeHandler` + `tb_cascade_subscribe` 表 + Manager |
| C3 | 缺目录变更主动推送（通道上下线时 NOTIFY 上级） | R1 | 新增 `CascadeNotifyPublisher.pushCatalogChange` + 事件桥接 |
| C4 | 缺告警主动推送（下级设备告警时 NOTIFY 上级） | R2 | `CascadeNotifyPublisher.pushAlarm` + 监听 `Notify.Alarm` 事件桥接 |
| C5 | 缺移动位置主动推送 | R1（位置） | `CascadeNotifyPublisher.pushMobilePosition` + 监听位置事件 |
| C6 | 缺录像查询应答（onRecordInfoQuery 未实现） | R4 | `CascadeQueryHandler.onRecordInfoQuery` + 录像转查聚合服务 |
| C7 | 回放点播转发缺失（仅实时） | R3 | `CascadeMediaInviteListener` 补 playback 分支 |
| C8 | 实时点播 200OK 未带 SDP + 真实拉流未闭环校验 | R3 | 修正 `sendInviteOk` 带本端发流 SDP；ensureStream 等流就绪 |
| C9 | 回放控制 INFO（PLAY/PAUSE/SCALE）未透传 | R3 | `CascadeMediaInviteListener.onInfo` 或框架 InfoEvent 监听 |
| C10 | 控制透传仅 PTZ，缺录像控制/布撤防等 | R0 | `CascadeControlHandler` 补 onRecord/onGuard/onAlarmReset 等 |
| C11 | QueryListener 其余应答缺失（Config/Preset/MobilePosition 查询） | R0 | `CascadeQueryHandler` 补对应回调 |
| C12 | 级联平台/通道无 Web 管理接口 | R0（运维） | 新增 `CascadePlatformController` + `CascadeChannelController` |
| C13 | 无前端级联管理页 | R0（运维） | 新增 `views/cascade/` 平台列表 + 通道映射 |
| C14 | `CascadeQueryHandler` 用 `ConditionalOnExpression` 与 lab 互斥，但订阅/推送类未明确 lab 共存策略 | R0 | 统一 lab 共存条件注解 |

### 4.3 框架能力（已具备，无需改框架）

`ClientCommandSender`（静态方法，已字节码核验，见 §14.1）提供下级平台所需全部出站能力：
- 注册/保活：`sendRegisterCommand` / `sendKeepaliveCommand` / `sendUnregisterCommand`
- 查询应答：`sendCatalogCommand`(+Batched) / `sendDeviceInfoCommand` / `sendDeviceStatusCommand` / **`sendDeviceRecordCommand`**(+Batched) / `sendDeviceConfigCommand` / `sendPresetQueryResponse`
- 主动推送：**`sendCatalogChangeNotify`**(+Batched) / **`sendAlarmNotify`** / **`sendMobilePositionNotify`** / `sendMediaStatusCommand`
- 媒体：`sendInvitePlayCommand` / `sendInvitePlayBackCommand` / `sendByeCommand`

`SubscribeListener`（client 栈，多实例观察者）：`onCatalogSubscribe` / `onAlarmSubscribe` / `onMobilePositionSubscribe` / `onPtzPositionSubscribe`。

> **核验补记（已坐实）**：`SubscribeListener` 回调签名 `(String platformId/sipId, Integer expires, XxxQuery query)`；`ClientSubscribeEvent` 提供 `getUserId/getSipId/getExpires/getBody`。主动推送方法（`sendCatalogChangeNotify`/`sendAlarmNotify`/`sendMobilePositionNotify`）**均无 callId/dialog 入参** → 框架按 From/To 自管订阅 dialog（V2）。`tb_cascade_subscribe.call_id` 列保留但允许空。**P1 仍需运行时实测** NOTIFY 是否真落在订阅 dialog 内（签名无法断定框架订阅 dialog 路由是否生效）。

---

## 5. 总体设计

### 5.1 架构分层落点

```
                    ┌──── 上级平台 ────┐
   REGISTER/Keepalive│                 │ SUBSCRIBE / INVITE / 查询 / 控制
            ▲        │                 ▼
┌───────────┴──────────────────────────────────────────────────────┐
│ wrapper/gb28181/cascade/  (client 栈, sip.client.enabled=true)     │
│                                                                    │
│  出站(主动)                          入站(被动应答)                  │
│  ┌─────────────────────┐            ┌──────────────────────────┐  │
│  │CascadeClientScheduler│            │CascadeQueryHandler        │  │
│  │ - register(续期)     │            │ (QueryListener)           │  │
│  │ - keepalive(C1新增)  │            │ onCatalog/Info/Status     │  │
│  ├─────────────────────┤            │ onRecordInfo(C6新增)      │  │
│  │CascadeNotifyPublisher│            ├──────────────────────────┤  │
│  │(C3/C4/C5新增)        │            │CascadeSubscribeHandler    │  │
│  │ pushCatalogChange    │◀──────┐    │(SubscribeListener,C2新增) │  │
│  │ pushAlarm            │       │    │ 登记订阅→tb_cascade_subscribe│
│  │ pushMobilePosition   │       │    ├──────────────────────────┤  │
│  └─────────────────────┘       │    │CascadeControlHandler      │  │
│            ▲                    │    │ (ControlListener)         │  │
│            │ 事件桥接            │    │ onPtz✅ +onRecord/Guard.. │  │
│  ┌─────────┴──────────┐         │    ├──────────────────────────┤  │
│  │CascadeEventBridge   │         │    │CascadeMediaInviteListener │  │
│  │(C3/C4/C5新增)       │         │    │ onInvite(play✅/playback) │  │
│  │ @EventListener      │         │    │ onInfo(回放控制C9)        │  │
│  │ 本地告警/通道变更    │─────────┘    │ onBye → stopSendRtp       │  │
│  │ →遍历订阅上级→推送   │              └──────────────────────────┘  │
│  └────────────────────┘                                            │
└────────────────────────────────────────────────────────────────────┘
        ▲ 复用既有 server 栈向真实设备拉流/查录像/控制
        ▼
   真实下级 IPC/NVR (注册在 voglander server 栈)
```

### 5.2 设计原则

1. **不改框架**：框架已提供全部出站命令 + 订阅/查询/控制监听，voglander 侧只加 cascade 包下的实现类 + 持久化 + 事件桥接。
2. **数据同源、事件桥接**：主动推送的数据来自既有 `tb_device`/`tb_device_channel`/`tb_alarm`。复用既有入站管线（`Gb28181ProtocolHandler.handleAlarm` 落 `tb_alarm` 后发 Spring 事件），`CascadeEventBridge` 监听该事件 → 查订阅了告警的上级 → 主动推送。**告警/通道变更落库与级联推送解耦**。
3. **订阅意图持久化**：`tb_cascade_subscribe` 存「platformId + subType + dialog标识 + expires + expireTime」。上级订阅时登记，过期/退订时清理。本平台重启后订阅丢失需上级重订（标准行为，上级会在 dialog 失效后重订）。
4. **录像查询：转查 + 聚合 + 主动回**（C6 关键模式）：`onRecordInfoQuery` 回调**同步**返回会阻塞，但真实设备录像查询是**异步**的（设备经 SIP 回 RecordInfo 响应）。方案：回调先返回空壳/不返回，转发查询给真实设备 → 真实设备 RecordInfo 响应到达 `Gb28181ProtocolHandler.Response.RecordInfo` → 经事件桥接用 `sendDeviceRecordCommand` **主动回包**给上级（按 sn 关联请求上下文）。
5. **协议合规**（CLAUDE.md 铁律）：所有 type/字段/编码严格对齐框架与 GB28181-2022；级联通道用独立国标编码，映射走 `tb_cascade_channel`，不前缀猜测；lab 非标简化不掺入 cascade 生产路径。
6. **lab 共存**：cascade 生产路径（`CascadeQueryHandler`/`CascadeSubscribeHandler` 等）与 lab 模拟设备（`LabQueryListener`/`LabSubscribeListener`）通过 `@ConditionalOnExpression("${sip.client.enabled} and not ${voglander.protocol-lab.enabled}")` 互斥，避免 `QueryListener` 唯一性冲突；多实例的 `ControlListener`/`SubscribeListener` 可共存但 lab 路径不复用 cascade 映射。

---

## 6. 数据模型

### 6.1 新增表 `tb_cascade_subscribe`（上级订阅状态）

记录「哪个上级平台订阅了本平台哪类信息」，是主动推送的目标清单来源。

```sql
-- ----------------------------
-- 级联上级订阅表（上级订本平台 → 本平台据此主动推送）
-- ----------------------------
DROP TABLE IF EXISTS tb_cascade_subscribe;
CREATE TABLE tb_cascade_subscribe
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    platform_id   VARCHAR(64)                            NOT NULL, -- 发起订阅的上级平台ID
    sub_type      VARCHAR(32)                            NOT NULL, -- CATALOG / ALARM / MOBILE_POSITION
    call_id       VARCHAR(255) DEFAULT NULL,                       -- 订阅 dialog 标识(主动 NOTIFY 复用)
    sn            VARCHAR(64)  DEFAULT NULL,                       -- 订阅请求 SN
    expires       INTEGER      DEFAULT 3600 NOT NULL,              -- 订阅有效期(秒)
    interval_sec  INTEGER      DEFAULT NULL,                       -- 位置上报间隔(秒)，仅 MOBILE_POSITION
    expire_time   DATETIME     DEFAULT NULL,                       -- 过期时间(=最后订阅时间+expires)
    status        INTEGER      DEFAULT 1   NOT NULL,               -- 1=ACTIVE 0=EXPIRED
    extend        TEXT
);
CREATE UNIQUE INDEX uk_cascade_subscribe ON tb_cascade_subscribe (platform_id, sub_type);
CREATE INDEX idx_cascade_subscribe_expire ON tb_cascade_subscribe (status, expire_time);
```

### 6.2 新增表 `tb_cascade_record_request`（录像查询请求上下文）

录像查询异步聚合用：上级发查询 → 登记请求（sn 关联）→ 真实设备 RecordInfo 响应到达 → 按 sn 找回上级 From/To → 主动回包。短生命周期，可用 Redis（多节点）或内存（单机）；为可观测与跨节点，建表持久化。

```sql
DROP TABLE IF EXISTS tb_cascade_record_request;
CREATE TABLE tb_cascade_record_request
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    platform_id        VARCHAR(64)                            NOT NULL, -- 发起查询的上级
    superior_sn        VARCHAR(64)                            NOT NULL, -- 上级查询的 SN(回包须原样带回)
    cascade_channel_id VARCHAR(64)                            NOT NULL, -- 上级请求的通道(级联编码)
    local_device_id    VARCHAR(64)                            NOT NULL, -- 转查的真实设备
    local_channel_id   VARCHAR(64)                            NOT NULL,
    local_sn           VARCHAR(64)  DEFAULT NULL,                       -- 诊断字段(server 录像命令不回传 sn,关联走 deviceId+时间窗)
    start_time         VARCHAR(32)  DEFAULT NULL,
    end_time           VARCHAR(32)  DEFAULT NULL,
    status             INTEGER      DEFAULT 0  NOT NULL,                -- 0=PENDING 1=RESPONDED 2=TIMEOUT
    extend             TEXT
);
CREATE INDEX idx_cascade_record_local_sn ON tb_cascade_record_request (local_device_id, status);
CREATE INDEX idx_cascade_record_created ON tb_cascade_record_request (create_time);
```

> 三套 SQL 同步：`sql/voglander.sql`（MySQL，INTEGER→BIGINT/TINYINT，DATETIME 不变）、`sql/voglander-sqlite.sql`、测试 `voglander-web/src/test/resources/schema-sqlite.sql`。

### 6.3 枚举与常量（`voglander-common`）

```java
// constant/cascade/CascadeConstant.java（新增）
public interface CascadeConstant {
    /** 注册状态 */
    interface RegisterStatus { int OFFLINE = 0, ONLINE = 1, REGISTERING = 2, FAILED = 3; }
    /** 订阅类型（与 GB28181 Event 头映射） */
    enum SubType {
        CATALOG("Catalog"), ALARM("presence.alarm"), MOBILE_POSITION("presence.mobile");
        private final String event;
        SubType(String event) { this.event = event; }
        public String event() { return event; }
    }
    /** 订阅状态 */
    interface SubStatus { int EXPIRED = 0, ACTIVE = 1; }
    /** 录像查询请求状态 */
    interface RecordReqStatus { int PENDING = 0, RESPONDED = 1, TIMEOUT = 2; }
    /** 默认订阅有效期(秒) */
    int DEFAULT_SUBSCRIBE_EXPIRES = 3600;
    /** Keepalive 心跳间隔(秒) */
    int DEFAULT_KEEPALIVE_INTERVAL = 60;
    /** 录像查询请求超时(秒)，超时清理 PENDING */
    int RECORD_REQUEST_TIMEOUT_SEC = 30;
    /** 级联 INVITE 媒体 app */
    String CASCADE_MEDIA_APP = "rtp";
}
```

### 6.4 CascadeChannelDTO 增强

录像/点播转发需 `local_device_id` + `local_channel_id`（已有）+ 通道在线状态（从 `tb_device_channel` 关联取）。本期不改表结构，查询时关联补充。

---

## 7. 后端实现

### 7.1 注册保活补全（C1）

`CascadeClientScheduler` 拆分「续期」与「保活」两个独立周期任务：

```java
/** 续期：按 registerExpires 周期重发 REGISTER（保持注册有效） */
private void scheduleRegister(CascadePlatformDTO p) {
    long expires = p.getRegisterExpires() != null ? p.getRegisterExpires() : 3600L;
    // 提前 1/3 expires 续期，避免边界过期
    long period = Math.max(60L, expires * 2 / 3);
    registerTasks.put(p.getPlatformId(),
        executor.scheduleWithFixedDelay(() -> doRegister(p), 0, period, TimeUnit.SECONDS));
}

/** 保活：独立 Keepalive MESSAGE 心跳（默认 60s） */
private void scheduleKeepalive(CascadePlatformDTO p) {
    long interval = p.getKeepaliveInterval() != null
        ? p.getKeepaliveInterval() : CascadeConstant.DEFAULT_KEEPALIVE_INTERVAL;
    keepaliveTasks.put(p.getPlatformId(),
        executor.scheduleWithFixedDelay(() -> doKeepalive(p), interval, interval, TimeUnit.SECONDS));
}

private void doKeepalive(CascadePlatformDTO p) {
    // 仅在 ONLINE 时发心跳；离线由 register 任务恢复
    if (!Objects.equals(p.getRegisterStatus(), CascadeConstant.RegisterStatus.ONLINE)) return;
    try {
        FromDevice from = cascadeDeviceSupplier.buildFromDevice(p);
        ToDevice to = cascadeDeviceSupplier.buildToDevice(p);
        ClientCommandSender.sendKeepaliveCommand(from, to, p.getLocalClientId());
    } catch (Exception e) {
        log.warn("级联保活失败: platformId={}, err={}", p.getPlatformId(), e.getMessage());
    }
}
```

> `sendKeepaliveCommand(from, to, String)` 第三参为 deviceId（本平台 localClientId）。`stopPlatform` 须同时取消 register + keepalive 两个 future。`CascadeDeviceSupplier.buildFromDevice` 端口写死 5070 改为读 `platform.getLocalPort()`（已有列，默认 5070）。

### 7.2 级联订阅接收 + 持久化（C2）

新增 `CascadeSubscribeHandler implements SubscribeListener`：

```java
@Slf4j
@Component
@ConditionalOnExpression("${sip.client.enabled:false} and not ${voglander.protocol-lab.enabled:false}")
@RequiredArgsConstructor
public class CascadeSubscribeHandler implements SubscribeListener {

    private final CascadeSubscribeManager cascadeSubscribeManager;

    @Override
    public void onCatalogSubscribe(String platformId, Integer expires, DeviceQuery query) {
        registerOrCancel(platformId, CascadeConstant.SubType.CATALOG, expires, query.getSn(), null);
    }

    @Override
    public void onAlarmSubscribe(String platformId, Integer expires, DeviceAlarmQuery query) {
        registerOrCancel(platformId, CascadeConstant.SubType.ALARM, expires, query.getSn(), null);
    }

    @Override
    public void onMobilePositionSubscribe(String platformId, Integer expires, DeviceMobileQuery query) {
        Integer interval = parseInterval(query); // query.getInterval() String→int
        registerOrCancel(platformId, CascadeConstant.SubType.MOBILE_POSITION, expires, query.getSn(), interval);
    }

    /** expires<=0 = 退订(RFC6665/GB28181 §9.11)，否则登记/续订 */
    private void registerOrCancel(String platformId, CascadeConstant.SubType type,
                                  Integer expires, String sn, Integer interval) {
        if (expires == null || expires <= 0) {
            cascadeSubscribeManager.expire(platformId, type);
            log.info("上级退订: platformId={}, type={}", platformId, type);
            return;
        }
        cascadeSubscribeManager.upsertActive(platformId, type, sn, expires, interval);
        log.info("上级订阅登记: platformId={}, type={}, expires={}", platformId, type, expires);
    }
}
```

`CascadeSubscribeManager`（标准模板方法 + 业务方法）：
- `upsertActive(platformId, type, sn, expires, interval)` — UNIQUE(platform,type) 前查后插，回填 expireTime=now+expires，status=ACTIVE
- `expire(platformId, type)` — status=EXPIRED
- `listActiveByType(SubType)` — 主动推送时取目标上级清单
- `listActiveByPlatform(platformId)` — 平台维度
- `cleanExpired(now)` — 过期清理（定时任务）

> **dialog/callId 复用（已核验 V2）**：`sendCatalogChangeNotify`/`sendAlarmNotify`/`sendMobilePositionNotify` 签名**均无 callId 入参**，框架按 From/To 自管订阅 dialog。故 `tb_cascade_subscribe.call_id` 列**保留但允许空**，登记订阅时不强制存 callId。P1 运行时实测 NOTIFY 落点。

### 7.3 主动推送发布器（C3/C4/C5）

新增 `CascadeNotifyPublisher`（出站推送单点收口）：

```java
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeNotifyPublisher {

    private final CascadeSubscribeManager cascadeSubscribeManager;
    private final CascadePlatformManager  cascadePlatformManager;
    private final CascadeChannelManager   cascadeChannelManager;
    private final CascadeDeviceSupplier   cascadeDeviceSupplier;

    /** 目录变更 → 推送所有订阅了 Catalog 的上级 */
    public void pushCatalogChange(String localDeviceId, String localChannelId, String event) {
        for (CascadeSubscribeDTO sub : cascadeSubscribeManager.listActiveByType(SubType.CATALOG)) {
            CascadeChannelDTO ch = cascadeChannelManager.getByPlatformAndChannel(sub.getPlatformId(), localChannelId);
            if (ch == null) continue; // 该上级未暴露此通道
            From/To = build(sub.getPlatformId());
            List<OtherItem> items = List.of(buildOtherItem(ch.getCascadeChannelId(), event)); // ON/OFF/ADD/DEL/UPDATE
            ClientCommandSender.sendCatalogChangeNotify(from, to, "1", items);
        }
    }

    /** 告警 → 推送所有订阅了 Alarm 的上级 */
    public void pushAlarm(String localDeviceId, String localChannelId, DeviceAlarmNotify alarm) {
        for (CascadeSubscribeDTO sub : cascadeSubscribeManager.listActiveByType(SubType.ALARM)) {
            CascadeChannelDTO ch = cascadeChannelManager.getByPlatformAndChannel(sub.getPlatformId(), localChannelId);
            if (ch == null) continue;
            From/To = build(sub.getPlatformId());
            DeviceAlarmNotify out = remap(alarm, ch.getCascadeChannelId()); // deviceId 改级联编码
            ClientCommandSender.sendAlarmNotify(from, to, out);
        }
    }

    /** 移动位置 → 推送所有订阅了 MobilePosition 的上级 */
    public void pushMobilePosition(String localDeviceId, String localChannelId, MobilePositionNotify pos) {
        for (CascadeSubscribeDTO sub : cascadeSubscribeManager.listActiveByType(SubType.MOBILE_POSITION)) {
            CascadeChannelDTO ch = cascadeChannelManager.getByPlatformAndChannel(sub.getPlatformId(), localChannelId);
            if (ch == null) continue;
            From/To = build(sub.getPlatformId());
            MobilePositionNotify out = remap(pos, ch.getCascadeChannelId());
            ClientCommandSender.sendMobilePositionNotify(from, to, out);
        }
    }
}
```

> **编码重映射**：推送给上级的实体 `deviceId` 必须改成上级认识的 `cascade_channel_id`（不是本地 `local_channel_id`）。重映射逻辑单点收口在 `CascadeNotifyPublisher.remap`。

### 7.4 本地事件桥接（C3/C4/C5 触发源）

主动推送的触发来自本地数据变更。复用既有入站管线发 Spring 事件，`CascadeEventBridge` 监听：

**(a) 告警桥接**：`Gb28181ProtocolHandler.handleAlarm` 落 `tb_alarm` 后，发布 `LocalAlarmEvent(deviceId, channelId, alarmNotify)`（新增轻量事件）。
**(b) 目录变更桥接**：通道上下线（`DeviceChannelManager.patchChannelStatus` / 设备 `Lifecycle.Offline`）后发布 `LocalChannelChangeEvent(deviceId, channelId, event)`。
**(c) 位置桥接**：`handleMobilePosition` 后发布 `LocalMobilePositionEvent`。

```java
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeEventBridge {

    private final CascadeNotifyPublisher notifyPublisher;

    @EventListener
    @Async("sipNotifierExecutor")
    public void onAlarm(LocalAlarmEvent e) {
        notifyPublisher.pushAlarm(e.getDeviceId(), e.getChannelId(), e.getAlarm());
    }

    @EventListener
    @Async("sipNotifierExecutor")
    public void onChannelChange(LocalChannelChangeEvent e) {
        notifyPublisher.pushCatalogChange(e.getDeviceId(), e.getChannelId(), e.getEvent());
    }

    @EventListener
    @Async("sipNotifierExecutor")
    public void onMobilePosition(LocalMobilePositionEvent e) {
        notifyPublisher.pushMobilePosition(e.getDeviceId(), e.getChannelId(), e.getPosition());
    }
}
```

> **解耦优势**：`Gb28181ProtocolHandler`（integration 层）只管落库 + 发本地事件，不直接依赖 cascade 推送；`CascadeEventBridge` 在 `sip.client.enabled=false`（无级联）时整个不加载，零开销。事件发布用 Spring `ApplicationEventPublisher`，与既有 `publishVisual`（SSE）模式一致。

### 7.5 录像查询应答（C6，R4 关键）

录像查询是「转查 + 异步聚合 + 主动回包」三段式。

**(a) 接收上级查询** — `CascadeQueryHandler.onRecordInfoQuery`：

```java
@Override
public DeviceRecord onRecordInfoQuery(String platformId, DeviceRecordQuery query) {
    String cascadeChannelId = query.getDeviceId();
    CascadeChannelDTO ch = cascadeChannelManager.getByPlatformAndCascadeChannelId(platformId, cascadeChannelId);
    if (ch == null) {
        // 找不到通道：回空录像列表（sumNum=0），不阻塞上级
        return emptyRecord(query);
    }
    // 登记请求上下文（superiorSn 关联回包），向真实设备转查
    cascadeRecordService.forwardRecordQuery(platformId, query, ch);
    // 同步回调返回 null：真实录像经异步 Response.RecordInfo 到达后主动回包
    return null; // 已核验(V1): ClientListenerAdapter 对 null 不回包,支持异步补发
}
```

> **协议要点**：录像查询响应可能很大（多条录像），GB28181 允许设备分多条 RecordInfo 消息回（每条带 sumNum 总数 + 本批 RecordItem）。同步 `onRecordInfoQuery` 返回单个 `DeviceRecord` 无法表达异步多批，故采用「同步返回 null + 异步主动 `sendDeviceRecordCommand`」。**已核验（V1，§14.1）**：`ClientListenerAdapter:153-157` 对 null 返回不回包，方案直接成立，无需「sumNum=0 占位」降级。

**(b) 转查真实设备 + 登记上下文** — `CascadeRecordService`（service 层）：

```java
@Service
public class CascadeRecordService {
    @Autowired private VoglanderServerRecordCommand serverRecordCommand; // server 栈向真实设备查
    @Autowired private CascadeRecordRequestManager  requestManager;

    public String forwardRecordQuery(String platformId, DeviceRecordQuery q, CascadeChannelDTO ch) {
        long start = parseGbTime(q.getStartTime());
        long end   = parseGbTime(q.getEndTime());
        // 登记请求（superiorSn 原样回带）。关联键=(localDeviceId+时间窗)，见下方核验结论
        // local_sn 仅作可选诊断字段，不依赖它与设备响应关联
        requestManager.create(platformId, q.getSn(), ch, q.getStartTime(), q.getEndTime());
        // 向真实设备发录像查询（server 栈，复用既有命令；该命令不回传生成的 sn）
        serverRecordCommand.queryDeviceRecord(ch.getLocalDeviceId(), start, end);
        return null;
    }
}
```

> **sn 关联（已核验，V5）**：`VoglanderServerRecordCommand.queryDeviceRecord(deviceId,start,end)` **不向调用方暴露生成的 sn**；框架在 `Response.RecordInfo` 用 `correlationId` 当 sn，`RecordInfoCacheManager` 以 `(deviceId, sn)` 为键缓存。因此**无法**用 `local_sn` 关联。**关联键改为 `(local_device_id + start_time + end_time)`**：登记请求时记下时���窗，`Response.RecordInfo` 事件到达时按 `localDeviceId` + 录像项时间窗（或就近 PENDING 请求）匹配回 `tb_cascade_record_request`。`local_sn` 列保留作诊断。**P3 第一步固化关联键再写聚合回包。**

**(c) 真实响应到达 → 主动回包上级** — `Gb28181ProtocolHandler.Response.RecordInfo` 增强 + 桥接：

```java
case "Response.RecordInfo":
    handleRecordInfo(event);   // 既有：落 RecordInfoCacheManager
    publishRecordInfoEvent(event); // 新增：发 LocalRecordInfoEvent 供级联回包
    break;
```

`CascadeEventBridge.onRecordInfo`：

```java
@EventListener @Async("sipNotifierExecutor")
public void onRecordInfo(LocalRecordInfoEvent e) {
    cascadeRecordService.respondToSuperior(e); // 按 (localDeviceId+时间窗) 找请求 → sendDeviceRecordCommand 回上级
}
```

`CascadeRecordService.respondToSuperior`：按 `(local_device_id + 时间窗)` 查 `tb_cascade_record_request` 的 PENDING 请求 → 取 superiorSn/platformId → 构造 `DeviceRecord`（deviceId 改 cascadeChannelId，sn 用 superiorSn，RecordItem 列表来自真实响应）→ `ClientCommandSender.sendDeviceRecordCommand(from, to, deviceRecord)` 或 `Batched` 分批 → 标记请求 RESPONDED。

**(d) 超时清理**：`CascadeRecordRequestScheduler` 定时清理 `RECORD_REQUEST_TIMEOUT_SEC`(30s) 未响应的 PENDING（标 TIMEOUT，可选回上级空列表）。

### 7.6 点播转发补全（C7/C8/C9，R3）

**(a) 实时点播 200OK 带 SDP（C8）**：当前 `sendInviteOk` 回 `Response.OK` 不带 SDP。标准要求 200OK 带本平台的发流 SDP（c= 行本端 IP，m= 行发流端口，a=sendonly，y= 行 SSRC）。改为构造 GB SDP 应答：

```java
private void sendInviteOk(ClientInviteEvent event, GbSessionDescription upSdp,
                          String localSendIp, int localSendPort, String ssrc) {
    // 构造本端发流 SDP（sendonly，PS/H264，SSRC 与 startSendRtp 一致）
    String sdpBody = buildCascadeAnswerSdp(localSendIp, localSendPort, ssrc, upSdp);
    // 已核验(V3): ResponseCmd.sendResponse(int, String body, RequestEvent, ServerTransaction) 第二参=SDP body
    ResponseCmd.sendResponse(Response.OK, sdpBody, ctx.getOriginalEvent(), ctx.getServerTransaction());
}
```

> **已核验（V3）**：`sip-common` 的 `ResponseCmd.sendResponse(int, String body, RequestEvent, ServerTransaction)` 带 body 重载存在，第二参即 SDP body（默认 ContentType=application/sdp）。手拼 SDP 严格对齐 GB28181-2022 附录 F（c=/m=/a=sendonly/y=SSRC/f=）。

**(b) 回放点播（C7）**：上级 INVITE 的 SDP `s=` 行为 `Playback`（实时为 `Play`），且 `u=` 行或时间参数含回放时间段。`onInvite` 增加分支：

```java
String sessionType = parseSessionName(sdp); // Play / Playback / Download
if ("Playback".equalsIgnoreCase(sessionType)) {
    // 向真实设备发回放 INVITE（server 栈 invitePlayBack，已核验 V6），时间段来自上级 SDP 的 t= 或 u= 行
    serverMediaCommand.invitePlayBack(ch.getLocalDeviceId(), startTime, endTime, ...);
} else {
    serverMediaCommand.inviteRealTimePlay(...); // 既有实时
}
// 之后 ZLM startSendRtp 转推上级（实时/回放一致）
```

**(c) 回放控制 INFO 透传（C9）**：上级在回放会话内发 `INFO`（RTSP 风格 PLAY/PAUSE/SCALE/拖动）。client 栈 `ClientInfoEvent`（已核验存在）携带已解析的 RTSP 指令，监听后透传给真实设备（server 栈 `controlPlayBack`）。

```java
@EventListener @Async("sipNotifierExecutor")
public void onInfo(ClientInfoEvent event) {
    // 已核验(V4): event.getParsed():ManSrtspRequest 提供已解析 RTSP; getContent() 为原始 body
    // PLAY(恢复)/PAUSE(暂停)/PLAY Range(拖动)/Scale(倍速)
    // 透传给真实设备：serverMediaCommand.controlPlayBack(localDeviceId, PlayActionEnums.PLAY_RESUME/...) 
}
```

> **已核验（V4/V6）**：`ClientInfoEvent` 含 `getParsed():ManSrtspRequest`（已解析 RTSP）+ `getContent()`（原始 body）；server 栈回放控制 `controlPlayBack(deviceId, PlayActionEnums)`（含 `playBack`/`pauseBack` 便捷方法）存在。**仍需注意**：回放控制涉及双向会话状态（上级↔本平台↔真实设备两段 dialog 联动），是本方案最复杂点，列 P4 末；首版可降级仅支持 PLAY/PAUSE，SCALE/拖动后续迭代。

### 7.7 控制透传补全（C10）

`CascadeControlHandler` 补充核心控制回调，全部「找级联通道 → 透传真实设备」：

```java
@Override public void onRecord(String platformId, DeviceControlRecordCmd cmd) {
    CascadeChannelDTO ch = resolve(platformId, cmd.getDeviceId());
    if (ch != null) serverControlCommand.controlRecord(ch.getLocalDeviceId(), cmd.getRecordCmd());
}
@Override public void onGuard(String platformId, DeviceControlGuard cmd) { ... 布撤防透传 ... }
@Override public void onAlarmReset(String platformId, DeviceControlAlarm cmd) { ... 告警复位 ... }
@Override public void onTeleBoot(String platformId, DeviceControlTeleBoot cmd) { ... 远程重启 ... }
```

> 透传依赖 server 栈对应控制命令（`server/command/` 下各类）。若某控制 server 栈无对应命令，先在 server 栈补命令封装（参照既有 PTZ 命令模式），再在 cascade 透传。本期优先 PTZ(已有)/录像控制/布撤防三类核心。

### 7.8 查询应答补全（C11）

`CascadeQueryHandler` 补 `onConfigDownloadQuery`（返回通道基本配置）/`onMobilePositionQuery`（返回通道最新 GPS，从 `tb_device_position` 或缓存取）/`onPresetQuery`（透传真实设备预置位，异步同录像模式）。优先级 P3，非核心需求。

### 7.9 Web 管理层（C12）

新增 `CascadePlatformController`（级联上级平台 CRUD）+ `CascadeChannelController`（级联通道映射 CRUD），遵循 Controller 模板方法规范（add/update/get/deleteOne/deleteBatch/getPage）：

```java
@RestController
@RequestMapping("/api/v1/cascade/platform")
@Tag(name = "级联上级平台管理")
public class CascadePlatformController {
    @PostMapping("/add")    // 新增上级平台配置 → 触发 scheduler.startPlatform
    @PutMapping("/update")  // 改配置 → 重启注册任务
    @GetMapping("/get")
    @DeleteMapping("/deleteOne") // → scheduler.stopPlatform
    @PostMapping("/page")   // 分页（带 register_status 回显）
    @PutMapping("/toggle")  // enabled 开关 → start/stopPlatform
}
```

> 平台增删改须联动 `CascadeClientScheduler.startPlatform/stopPlatform`（运行时生效，无需重启）。通道映射 CRUD 决定「本平台对哪个上级暴露哪些通道」，是目录应答/推送的数据来源。

---

## 8. 前端实现

> 新增级联管理菜单（`views/cascade/`），两个页面：上级平台列表 + 通道映射。复用既有 vxe-grid + Schema 表单模式（参照 `views/device/`、`views/media/stream-proxy`）。

### 8.1 路由与菜单

`views/cascade/` 下：`platform/list.vue`（上级平台）、`channel/list.vue`（通道映射）。菜单 SQL 加 `tb_menu`（参照设备菜单），权限码 `Cascade:Platform:*` / `Cascade:Channel:*`。

### 8.2 API（`apps/web-antd/src/api/cascade.ts` 新增）

```typescript
export interface CascadePlatformVO {
  id: number; platformId: string; platformIp: string; platformPort: number;
  platformDomain: string; localClientId: string; enabled: number;
  registerStatus: number; // 0离线 1在线 2注册中 3失败
  keepaliveInterval: number; registerExpires: number;
  transport: string; charset: string; createTime: number; updateTime: number;
}
export interface CascadeChannelVO {
  id: number; platformId: string; localDeviceId: string; localChannelId: string;
  cascadeChannelId: string; cascadeName: string; enabled: number;
}
export const getCascadePlatformPage = (params) =>
  requestClient.post('/api/v1/cascade/platform/page', params);
export const toggleCascadePlatform = (id: number, enabled: boolean) =>
  requestClient.put('/api/v1/cascade/platform/toggle', { id, enabled });
// addCascadePlatform / updateCascadePlatform / deleteCascadePlatform ...
// getCascadeChannelPage / addCascadeChannel / batchBindChannels ...
```

### 8.3 平台列表（`platform/list.vue`）

列：平台ID、IP:端口、域、本端客户端ID、注册状态(Tag 标签：在线绿/离线灰/失败红/注册中蓝)、保活间隔、启停开关、操作(编辑/删除/通道映射跳转)。新增/编辑用 Schema 表单弹窗。注册状态用 SSE 或轮询刷新（复用既有 `useSseEvents` 或定时 refresh）。

### 8.4 通道映射（`channel/list.vue`）

按 platformId 过滤，展示本地通道 ↔ 级联编码映射。提供「从本地设备批量绑定通道」入口（选真实设备/通道 → 生成级联通道记录，cascadeChannelId 默认同 localChannelId 可改）。

### 8.5 i18n + cursor-rule 登记

`locales/langs/{zh-CN,en-US}/cascade.json` 新增；新增字段在 `voglander/.cursor/rules/project-rule.mdc` 与 `vue-vben-admin/.cursorrules` 登记（CLAUDE.md 前后端契约规则）。

---

## 9. 配置项

`application-inte.yml` 新增 `gateway.cascade` 段：

```yaml
sip:
  client:
    enabled: true              # 级联客户端栈总开关(下级平台能力)
    # 注：与 voglander.protocol-lab.enabled 互斥(lab 模拟设备时关闭真实级联应答)

gateway:
  cascade:
    enabled: true              # 级联功能总开关
    keepalive-interval: 60     # 保活心跳间隔(秒)
    subscribe:
      default-expires: 3600    # 上级订阅默认有效期(秒, 上级未指定时)
      clean-interval-ms: 60000 # 过期订阅清理周期
    record:
      request-timeout-sec: 30  # 录像转查请求超时
      batch-size: 8            # 录像回包分批大小(sendDeviceRecordCommandBatched)
    media:
      zlm-app: rtp             # 级联转推 ZLM app
```

> `ServerStart` 已绑定 SIP 端口（client/server 共用监听点，见 CLAUDE.md）。`ApplicationWeb` 已 `@EnableScheduling`（录像超时/订阅清理任务依赖，已核验）+ `@EnableSipServer`。

---

## 10. 时序图

### 10.1 级联注册 + 保活（R0）

```
voglander(下级)                          上级平台
   │── REGISTER(expires=3600) ──────────────▶│
   │◀───────── 401 Unauthorized(nonce) ──────│
   │── REGISTER(Authorization:Digest) ──────▶│
   │◀───────────── 200 OK ───────────────────│  CascadeClientRegisterListener→status=ONLINE
   │                                          │
   │── MESSAGE(Keepalive) ──[每60s]─────────▶│  (C1 新增独立心跳)
   │── REGISTER(续期) ──────[每2/3 expires]─▶│
```

### 10.2 目录订阅 + 通道变更主动推送（R1）

```
上级平台                                  voglander(下级)
   │── SUBSCRIBE(Event:Catalog,expires) ────▶│ CascadeSubscribeHandler.onCatalogSubscribe
   │◀──────────── 200 OK ────────────────────│ → tb_cascade_subscribe upsertActive
   │                                          │
   │            (本地真实设备通道离线)          │ Gb28181ProtocolHandler→LocalChannelChangeEvent
   │                                          │ CascadeEventBridge.onChannelChange
   │◀── MESSAGE(Notify:Catalog,OFF) ─────────│ CascadeNotifyPublisher.pushCatalogChange
   │                                          │   (deviceId 重映射为 cascadeChannelId)
```

### 10.3 告警主动推送（R2）

```
真实设备 ──Alarm──▶ voglander                上级平台(已订阅 Alarm)
            │ Gb28181ProtocolHandler.handleAlarm
            ├─ 落 tb_alarm + SSE (既有)
            └─ publish LocalAlarmEvent
                 │ CascadeEventBridge.onAlarm
                 │ pushAlarm: listActiveByType(ALARM)
                 └── MESSAGE(Notify:Alarm) ──────────────────▶│
```

### 10.4 点播转发（R3，实时）

```
上级                     voglander(下级)                  真实IPC          ZLM
 │─INVITE(SDP:上级收流)─▶│                                  │               │
 │                       │─INVITE(拉流)─────────────────────▶│               │
 │                       │◀────200 OK(SDP:IPC发流)──────────│               │
 │                       │─ACK────────────────────────────▶│               │
 │                       │                          IPC推流 ─────────────▶│(rtp/stream)
 │                       │─startSendRtp(dst=上级SDP)────────────────��───────▶│
 │◀──200 OK(SDP:本端发流)│                                                  │
 │─ACK─────────────────▶│                                                  │
 │◀═══════════════════════════ RTP 媒体流(ZLM转推) ════════════════════════│
 │─BYE─────────────────▶│─stopSendRtp───────────────────────────────────────▶│
 │                       │─BYE(停IPC拉流)──────────────────▶│
```

### 10.5 录像查询转查聚合（R4）

```
上级                  voglander(下级)                    真实设备
 │─MESSAGE(RecordInfoQuery,sn=S1)─▶│
 │                                  │ onRecordInfoQuery: 找通道映射
 │                                  │ tb_cascade_record_request.create(superiorSn=S1,localSn=S2)
 │                                  │─MESSAGE(RecordInfoQuery,sn=S2)─────────▶│
 │◀──(同步返回null,稍后异步回)──────│                                         │
 │                                  │◀──MESSAGE(RecordInfo,sn=S2,records)─────│ Response.RecordInfo
 │                                  │ publish LocalRecordInfoEvent
 │                                  │ respondToSuperior: 按localSn找请求,重映射deviceId
 │◀─MESSAGE(RecordInfo,sn=S1,records,分批)─│ sendDeviceRecordCommandBatched
```

---

## 11. 测试方案

遵循 ARCHITECTURE §17 分层策略，测试全部置于 `voglander-web/src/test/...`。

| 层 | 测试类 | 类型 | 重点 |
|----|--------|------|------|
| 订阅持久化 | `CascadeSubscribeManagerTest` | 集成(`BaseTest`+`@Transactional`) | upsertActive UNIQUE、expire、listActiveByType、cleanExpired 边界 |
| 录像请求 | `CascadeRecordRequestManagerTest` | 集成 | create/找回 localSn、超时标记 |
| 订阅接收 | `CascadeSubscribeHandlerTest` | 纯单元(Mockito) | onCatalog/Alarm/Position 登记；expires<=0 退订；interval 解析 |
| 主动推送 | `CascadeNotifyPublisherTest` | 纯单元 | listActiveByType 遍历；编码重映射 local→cascade；无订阅时不推 |
| 事件桥接 | `CascadeEventBridgeTest` | 纯单元 | 告警/通道/位置事件 → 调对应 publisher |
| 录像转查 | `CascadeRecordServiceTest` | 纯单元 | forwardRecordQuery 登记+转查；respondToSuperior 按 sn 回包重映射 |
| 查询应答 | `CascadeQueryHandlerTest` | 纯单元 | onCatalog 返回级联通道；onRecordInfo 转查；找不到通道回空 |
| 控制透传 | `CascadeControlHandlerTest` | 纯单元 | onPtz/onRecord/onGuard 找通道→透传；通道缺失不抛 |
| 媒体转发 | `CascadeMediaInviteListenerTest` | 纯单元 | 实时/回放分支；200OK 带 SDP；BYE→stopSendRtp；异常回错误响应 |
| 保活调度 | `CascadeClientSchedulerTest`(扩展) | 纯单元 | register/keepalive 双任务；stopPlatform 取消两者；端口取 localPort |
| Web | `CascadePlatformControllerTest`/`CascadeChannelControllerTest` | 纯单元 | CRUD 参数校验；toggle 联动 scheduler |
| E2E(可选) | `CascadeClientE2ETest` | 集成(无事务,手动清理) | 真 SIP：mock 上级 SUBSCRIBE→变更→收 NOTIFY；20s await，甄别既有 flaky |

> E2E 受既有 real-SIP flaky 影响（memory `sip-e2e-flakiness`），用基线对照，勿追逐既有噪声（~6.5k ERROR-log 行 + "Transaction exists" 为预存噪声）。lab 模拟上级平台可作为级联 E2E 的对端（复用 LabSipClient 模式构造 mock 上级）。

---

## 12. 实施阶段与验收

> 严格 TDD：每阶段先写测试（red）→ 实现（green）→ 重构。每阶段 `mvn clean install`（先 install integration，见 memory `voglander-web-tests-use-installed-integration-jar`）。

| 阶段 | 内容 | 对应缺口 | 验收 |
|------|------|----------|------|
| **P0 数据与常量** | 三表 SQL(subscribe/record_request)×3套；`CascadeConstant`；DO/Mapper/Service/Manager(subscribe+record_request)；Assembler | C2/C6 数据 | Manager 集成测试绿；建表脚本三套一致 |
| **P1 注册保活 + 订阅接收** | `CascadeClientScheduler` 拆 register/keepalive(C1)；`CascadeSubscribeHandler`(C2)；`CascadeSubscribeManager` | C1/C2 | 调度双任务测试绿；订阅登记/退订测试绿；**运行时实测 NOTIFY 落订阅 dialog(V2)** |
| **P2 主动推送 + 事件桥接** | `CascadeNotifyPublisher`(C3/C4/C5)；`CascadeEventBridge` + 三个本地事件；`Gb28181ProtocolHandler` 发事件钩子 | C3/C4/C5 | 推送遍历+重映射测试绿；告警落库不阻断 |
| **P3 录像查询转查聚合** | `CascadeQueryHandler.onRecordInfoQuery`(C6)；`CascadeRecordService` 转查+回包；`Response.RecordInfo` 桥接；超时清理 | C6/R4 | **第一步固化关联键 `(localDeviceId+时间窗)`(V5)**；转查登记+异步回包测试绿；关联匹配正确 |
| **P4 点播转发补全** | 实时 200OK 带 SDP(C8)；回放 INVITE 分支(C7)；回放控制 INFO(C9) | C7/C8/C9/R3 | 媒体转发测试绿；回放分支正确（E2E 联调） |
| **P5 控制 + 查询补全** | `CascadeControlHandler` 补录像/布撤防(C10)；`CascadeQueryHandler` 补 Config/Position(C11) | C10/C11 | 控制透传测试绿 |
| **P6 Web 管理** | `CascadePlatformController`/`CascadeChannelController` + scheduler 联动 | C12 | controller 单测绿；增删改运行时生效 |
| **P7 前端** | `views/cascade/` 平台+通道页 + API + i18n + 菜单 SQL + cursor-rule 登记 | C13 | `pnpm build:antd` 过；CRUD + 状态回显可用 |
| **P8 集成** | 全量 `mvn clean install`；`pnpm check`；真 SIP 级联联调 | 全部 | 全套件除既有 flaky 无新增失败 |

**总体验收标准**：
1. 本平台成功注册到上级并保持在线（保活心跳不断）；
2. 上级目录查询返回本平台暴露的级联通道；通道上下线时上级收到目录变更 NOTIFY；
3. 上级订阅告警后，真实设备告警能主动推送到上级；
4. 上级对级联通道点播（实时+回放），能看到真实设备的流（经 ZLM 转推）；
5. 上级录像查询，返回真实设备录像列表（标准 RecordInfo 协议，分批正确）；
6. 上级 PTZ/录像控制能透传到真实设备；
7. 级联平台 + 通道映射可在前端管理，启停运行时生效。

---

## 13. 风险与协议合规

> **核验状态（2026-06-14，对 1.8.5 jar 字节码 + 源码逐项坐实）**：V1/V3/V4/V6 已核实成立；V2/V5 已澄清真相并��整策略（见下表与 §14.1）。

| 风险 | 等级 | 缓解 |
|------|------|------|
| ~~`onRecordInfoQuery` 同步返回 null 框架是否支持异步补发~~ **已核实成立** | ~~高~~ **低** | `ClientListenerAdapter:153-157` 实测：`if (resp != null) sendDeviceRecordCommand(...)`，返回 **null 框架不回包**。方案 §7.5「同步返 null + 异步主动回包」**直接可用，无需降级** |
| 主动 NOTIFY 复用订阅 dialog 机制 | **中** | 已澄清：`sendCatalogChangeNotify` 第三参是 **`sn` 非 callId**，`sendAlarmNotify`/`sendDeviceRecordCommand` **均无 callId/dialog 参数** → 框架按 From/To 自管 dialog。`tb_cascade_subscribe.call_id` 列**保留但允许空**。P1 需**运行时实测** NOTIFY 能否落在订阅 dialog 内（签名无法断定框架是否真维护订阅 dialog 路由） |
| 录像转查 sn 与真实响应关联 | **中** | 已核实：`VoglanderServerRecordCommand.queryDeviceRecord(deviceId,start,end)` **不向调用方暴露生成的 sn**；框架用 `correlationId` 当 sn，`RecordInfoCacheManager` 键为 `(deviceId, sn)`。故 `tb_cascade_record_request.local_sn` **拿不到该 sn** → 关联键改用 `(local_device_id + 时间窗)`；P3 第一步先定关联键，否则聚合回包错配 |
| 回放控制 INFO 双段 dialog 联动复杂 | **中** | `ClientInfoEvent` **已核实存在**（含 `getParsed():ManSrtspRequest` 已解析 RTSP）。列 P4 末；先实现实时+回放点播，回放控制可降级（仅 PLAY/PAUSE）后续迭代 |
| ~~实时 200OK SDP 构造字段不合规~~ **API 已核实** | **中** | `ResponseCmd.sendResponse(int, String body, RequestEvent, ServerTransaction)`（在 `sip-common`）**存在带 body 重载**，第二参即 SDP body。手拼 SDP 严格对齐 GB28181-2022 附录 F（c=/m=/a=sendonly/y=SSRC/f=） |
| ZLM startSendRtp 转推参数（PS/SSRC/TCP-UDP）与上级 SDP 不匹配 | **中** | 参照既有 `CascadeMediaInviteListener.buildStartSendRtpReq`，SSRC 取上级 SDP y= 行，传输模式取 m= 行 |
| 多节点级联（保活/订阅推送在错误节点） | **低** | 本期单机优先；多节点复用命令亲和路由（按平台/dialog 寻址）+ Redis 订阅状态共享 |
| lab 与 cascade 生产路径冲突 | **低** | 统一 `@ConditionalOnExpression("${sip.client.enabled} and not ${voglander.protocol-lab.enabled}")` 互斥（QueryListener 唯一性）；ControlListener/SubscribeListener 多实例 lab 不复用 cascade 映射 |

**协议合规铁律**（CLAUDE.md §GB28181 协议合规铁律）：
- 出站命令/事件 type 逐字对齐框架注册键与 GB28181-2022，不臆造；
- 级联通道为独立 20 位国标编码，本地↔级联映射走 `tb_cascade_channel`，**禁止前缀拼接猜测**；
- 主动推送的实体 deviceId 重映射为上级认识的 `cascade_channel_id`，单点收口于 `CascadeNotifyPublisher.remap`；
- 录像/点播时间、SDP、Event 头、CmdType 按标准；
- lab 模拟上级平台的非标简化显式标注 + 单点收口，不混入 cascade 生产路径。

---

## 14. 附录：框架 API 核验表 + 改动清单

### 14.1 框架 API 核验（`gb28181-client-1.8.5.jar` 字节码，2026-06-14 坐实）

> **包名澄清（重要）**：client/server 栈实际包名为 **`io.github.lunasaw.gbproxy.*`**（如 `io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender`、`io.github.lunasaw.gbproxy.client.api.QueryListener`），**不是** `io.github.lunasaw.gb28181.client.*`；仅协议实体在 `io.github.lunasaw.gb28181.common.*`，`ResponseCmd` 在 `io.github.lunasaw.sip.common.transmit.*`。本文其余处的「gb28181-client 栈」均指此 `gbproxy.client` 包。

**已核验 — `ClientCommandSender` 静态方法（下级平台出站全集）**：

| 用途 | 方法签名 | 本方案使用处 |
|------|----------|--------------|
| 注册 | `sendRegisterCommand(From,To,Integer expires)` | P1 注册 ✅已用 |
| 注销 | `sendUnregisterCommand(From,To)` | stopPlatform |
| 保活 | `sendKeepaliveCommand(From,To,String deviceId)`（另有 `(From,To,DeviceKeepLiveNotify)` 重载） | P1 心跳(C1) |
| 目录应答 | `sendCatalogCommand(From,To,DeviceResponse)`(+Batched) | 目录查询应答(已用 server 端���式) |
| 目录变更推送 | `sendCatalogChangeNotify(From,To,String **sn**,List<OtherItem>)`(+Batched) | P2 推送(C3) |
| 设备信息 | `sendDeviceInfoCommand(From,To,DeviceInfo)` | 信息查询应答 |
| 设备状态 | `sendDeviceStatusCommand(From,To,DeviceStatus/String)` | 状态查询应答 |
| **录像应答** | `sendDeviceRecordCommand(From,To,DeviceRecord)`（另有 `(From,To,List<RecordItem>)`、+Batched）**无 callId 参数** | P3 录像回包(C6) |
| 告警推送 | `sendAlarmNotify(From,To,DeviceAlarmNotify)`（另有 `sendAlarmCommand` 同签名）**无 callId 参数** | P2 告警推送(C4) |
| 位置推送 | `sendMobilePositionNotify(From,To,MobilePositionNotify)` | P2 位置推送(C5) |
| 媒体状态 | `sendMediaStatusCommand(From,To,String)` | 点播媒体状态 |
| 配置应答 | `sendDeviceConfigCommand(From,To,DeviceConfigResponse)` | P5 配置查询(C11) |
| 预置位应答 | `sendPresetQueryResponse(From,To,PresetQueryResponse)` | P5 预置位(C11) |
| 点播/回放 | `sendInvitePlayCommand(From,To,String)` / `sendInvitePlayBackCommand(From,To,String)`（均有带双 Event 回调重载） | 点播转发 |
| BYE/退订 | `sendByeCommand(String callId)` / `unsubscribe(String callId)` / `refreshSubscribe(String[,String],int)` | 订阅/会话 dialog 管理 |

> **关键澄清（V2）**：`sendCatalogChangeNotify` 第三参为目录变更通知的 **`sn`（非 callId）**；`sendAlarmNotify`/`sendDeviceRecordCommand` **均无 callId/dialog 入参** → 框架按 From/To 自管订阅 dialog。故 `tb_cascade_subscribe.call_id` 列**保留但允许空**，P1 运行时实测 NOTIFY 是否落在订阅 dialog 内。

**已核验 — 客户端监听接口（入站，多实例观察者）**：
- `QueryListener`：13 个查询回调（含 `onRecordInfoQuery(String,DeviceRecordQuery):DeviceRecord`、`onCatalogQuery`/`onDeviceInfoQuery`/`onDeviceStatusQuery`/`onAlarmQuery`/`onConfigDownloadQuery`/`onPresetQuery`/`onMobilePositionQuery`/`onPtzPositionQuery`/`onSdCardStatusQuery`/`onHomePositionQuery`/`onCruiseTrackListQuery`/`onCruiseTrackQuery`）
- `SubscribeListener`：4 个 — `onCatalogSubscribe`/`onAlarmSubscribe`/`onMobilePositionSubscribe`/`onPtzPositionSubscribe`（均 `(String,Integer expires,XxxQuery):void`）
- `ControlListener`：15 个控制回调（`onPtzControl`/`onRecord`/`onGuard`/`onAlarmReset`/`onTeleBoot`/`onIFrame`/`onDragIn`/`onDragOut`/`onHomePositionControl`/`onDeviceUpgrade`/`onPtzPrecise`/`onFormatSdCard`/`onTargetTrack`/`onKeepalive`/`onUnknownControl`）
- `ConfigListener`：13 个配置回调
- `NotifyListener`：`onBroadcastNotify`/`onUnknownNotify`
- **回包分发** `ClientListenerAdapter`：`if (resp != null) ClientCommandSender.sendXxx(from, to, resp)` —— Query 回调**返回 null 即不回包**（V1 据此成立）

**已核验 — 实体字段**：
- `DeviceRecordQuery`：`cmdType/sn/deviceId/startTime/endTime/secrecy/type`（startTime/endTime 为 String）
- `DeviceRecord` + `RecordItem`：`setSumNum(int)`/`setRecordList(List<RecordItem>)`；RecordItem `deviceId/name/startTime/endTime/secrecy/type/fileSize/filePath`
- `DeviceAlarmNotify`：`cmdType/sn/deviceId/alarmPriority/alarmMethod/alarmTime/longitude/latitude/getInfo()`
- `MobilePositionNotify`：`time/longitude/latitude/speed/direction/altitude`（均 Double）
- `DeviceOtherUpdateNotify$OtherItem`：`deviceId/event`
- `ClientInfoEvent`：`getUserId/getContent/getContentType/getParsed():ManSrtspRequest`（已解析 RTSP，V4 据此成立）
- `ResponseCmd`（`sip-common`）：`sendResponse(int, **String body**, RequestEvent, ServerTransaction)` 带 body 重载存在（V3 据此成立）

**已核验（原 V1~V6，全部坐实）**：

| # | 核验项 | 结论 | 证据 |
|---|--------|------|------|
| V1 | `onRecordInfoQuery` 返回 null 框架行为 | ✅ **返回 null 不回包**，支持异步补发；§7.5 三段式无需降级 | `ClientListenerAdapter:153-157` `if (resp != null) sendDeviceRecordCommand(from,to,resp)` |
| V2 | 主动 NOTIFY 是否需订阅 dialog callId | ✅ **无需**，框架按 From/To 自管 dialog（送出方法均无 callId 参数）；仍需 P1 运行时实测 NOTIFY 路由 | `sendCatalogChangeNotify` 第三参=sn；`sendAlarmNotify`/`sendDeviceRecordCommand` 无 callId |
| V3 | `ResponseCmd` 带 SDP body 重载 | ✅ **存在**，第二参即 SDP body | `sip-common` `ResponseCmd.sendResponse(int,String,RequestEvent,ServerTransaction)` |
| V4 | client 栈 `ClientInfoEvent`（回放控制 INFO） | ✅ **存在**，含 `getParsed():ManSrtspRequest` 已解析 RTSP | `gbproxy.client.eventbus.event.ClientInfoEvent` |
| V5 | 录像查询 sn 与真实响应关联 | ⚠️ **`queryDeviceRecord` 不暴露 sn**；框架用 correlationId 当 sn，缓存键 `(deviceId,sn)`。`local_sn` 拿不到 → **关联键改 `(local_device_id + 时间窗)`** | `VoglanderServerRecordCommand.queryDeviceRecord(deviceId,start,end)`；`Gb28181ProtocolHandler.handleRecordInfo` 用 correlationId |
| V6 | server 栈回放 INVITE / 回放控制命令 | ✅ **存在** | `VoglanderServerMediaCommand.invitePlayBack(...)` / `controlPlayBack(deviceId, PlayActionEnums)`（playBack/pauseBack 便捷方法） |

> **V5 落地修正**：因 server 录像查询不回传生成的 sn，§6.2 `tb_cascade_record_request.local_sn` 关联失效；改用 `(local_device_id + start_time + end_time)` 作关联键匹配 `Response.RecordInfo` 事件。`local_sn` 列保留作可选诊断字段。P3 第一步先固化关联键再写聚合回包。

### 14.2 改动文件清单

**新增（后端）**
- `sql/`：`tb_cascade_subscribe` + `tb_cascade_record_request`（三套：voglander.sql / voglander-sqlite.sql / 测试 schema-sqlite.sql）+ 级联菜单 SQL
- common：`constant/cascade/CascadeConstant.java`
- repository：`entity/CascadeSubscribeDO.java`、`entity/CascadeRecordRequestDO.java` + 两 Mapper
- manager：`domaon/dto/cascade/CascadeSubscribeDTO.java`、`CascadeRecordRequestDTO.java`；`manager/CascadeSubscribeManager.java`、`CascadeRecordRequestManager.java`；`assembler/CascadeAssembler`(扩展)；本地事件 `event/LocalAlarmEvent`/`LocalChannelChangeEvent`/`LocalMobilePositionEvent`/`LocalRecordInfoEvent`
- service：`cascade/CascadeRecordService.java`；`cascade/CascadeRecordRequestScheduler.java`（超时清理）；`cascade/CascadeSubscribeCleanScheduler.java`（订阅过期清理）
- integration（cascade 包）：`CascadeSubscribeHandler.java`、`CascadeNotifyPublisher.java`、`CascadeEventBridge.java`；扩展 `CascadeClientScheduler`(C1)、`CascadeQueryHandler`(C6/C11)、`CascadeControlHandler`(C10)、`CascadeMediaInviteListener`(C7/C8/C9)、`CascadeDeviceSupplier`(端口)
- integration（handler）：`Gb28181ProtocolHandler` 发本地事件钩子（handleAlarm/handleMobilePosition/Response.RecordInfo 后）
- web：`controller/cascade/CascadePlatformController.java`、`CascadeChannelController.java` + Req/VO + `CascadeWebAssembler`

**新增（前端）**
- `apps/web-antd/src/api/cascade.ts`
- `views/cascade/platform/list.vue`、`views/cascade/channel/list.vue`（+ data.ts/schema）
- `locales/langs/{zh-CN,en-US}/cascade.json`
- cursor-rule 登记（`project-rule.mdc` + `.cursorrules`）

**配置**：`application-inte.yml` `sip.client.enabled` + `gateway.cascade.*`

**测试**：见 §11（全部 `voglander-web/src/test/...`）

---

> **文档状态**：技术方案（已核验，待评审）。§14.1 V1~V6 框架核验已于 2026-06-14 对 1.8.5 jar 全部坐实（V1/V3/V4/V6 成立，V2/V5 已澄清并调整策略）。剩余两项运行时验证：P1 实测 NOTIFY 订阅 dialog 路由(V2)、P3 固化录像关联键(V5)。每阶段 TDD + `mvn clean install`（先 install integration）。前后端字段须先登记 cursor-rule（CLAUDE.md 契约规则）。
