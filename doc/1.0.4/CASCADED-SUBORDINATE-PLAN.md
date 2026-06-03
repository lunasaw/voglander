# GB28181 级联下级技术方案 (Cascade Subordinate Implementation Plan)

本方案设计 Voglander 作为级联下级（Subordinate Platform），借助同进程（同 JVM）中启用的 `gb28181-client` 客户端能力实现与上级平台的对接。

对于上级平台的入站请求（目录查询、设备控制、视频取流等），我们将基于 `gb28181-client` 提供的声明式 Listener 接口与 Spring Application Event 机制，细化消息接收后的业务流转与处理方案。

---

## 1. 级联下级双角色部署架构

Voglander 将以 **中转平台（Relay）** 模式运行，即同进程内同时开启双向 SIP 协议引擎：
1. **服务端角色 (`@EnableSipServer`)**：用于接收下级物理设备（IPCs / NVRs）的注册、心跳及事件。监听端口如 `5060`。
2. **客户端角色 (`@EnableSipClient`)**：用于将自身虚拟为“设备”向上级平台发起 `REGISTER` 及心跳，并应答上级请求。监听端口如 `5070`。

由于是**同 JVM 部署**，上级平台发送给下级客户端的 SIP 请求可以直接在 JVM 内部通过 Spring 依赖注入与事件订阅进行处理，无需经由外部 HTTP Webhook 转发。

---

## 2. 数据库设计 (Database Schema)

为了管理和维护级联上级平台的信息以及需要上报的通道，设计新增以下两张表。

### 2.1 级联上级平台表 `tb_cascade_platform`
本表存储上级级联平台的 SIP 配置、本地客户端配置以及注册状态。

```sql
DROP TABLE IF EXISTS tb_cascade_platform;
CREATE TABLE tb_cascade_platform
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- 上级平台配置
    platform_id        VARCHAR(64)                            NOT NULL, -- 上级国标ID (SIP Server ID)
    platform_ip        VARCHAR(64)                            NOT NULL, -- 上级IP
    platform_port      INTEGER                                NOT NULL, -- 上级端口
    platform_domain    VARCHAR(64)                            NOT NULL, -- 上级域 (Realm)
    username           VARCHAR(64)  DEFAULT ''                NOT NULL, -- 认证用户名
    password           VARCHAR(128) DEFAULT ''                NOT NULL, -- 认证密码
    
    -- 本地模拟客户端配置
    local_client_id    VARCHAR(64)                            NOT NULL, -- 本地模拟级联通道的网关ID
    local_ip           VARCHAR(64)  DEFAULT NULL,                       -- 本地绑定IP (可为空，默认网卡IP)
    local_port         INTEGER      DEFAULT 5070              NOT NULL, -- 本地SIP客户端监听端口 (如 5070)
    
    -- 状态与策略
    enabled            INTEGER      DEFAULT 1                 NOT NULL, -- 1-启用, 0-禁用
    register_status    INTEGER      DEFAULT 0                 NOT NULL, -- 0-离线, 1-在线, 2-注册中, 3-失败
    keepalive_interval INTEGER      DEFAULT 60                NOT NULL, -- 心跳间隔 (秒)
    register_expires   INTEGER      DEFAULT 3600              NOT NULL, -- 注册有效期 (秒)
    
    -- 附加选项
    charset            VARCHAR(10)  DEFAULT 'GB2312'          NOT NULL, -- 字符集: UTF-8 / GB2312
    transport          VARCHAR(10)  DEFAULT 'UDP'             NOT NULL, -- 传输协议: UDP / TCP
    extend             TEXT                                             -- 扩展 JSON，存储高级配置
);

CREATE UNIQUE INDEX uk_platform_id ON tb_cascade_platform (platform_id);
CREATE INDEX idx_platform_enabled ON tb_cascade_platform (enabled);
```

### 2.2 级联上报通道表 `tb_cascade_channel`
本表用于配置和管理需要推送给上级平台的通道列表。并非本地所有的通道都需要向所有上级平台上报，通过此表进行多对多映射。同时它还可以支持将本地通道的 ID 映射为上级平台要求的特定通道国标 ID。

```sql
DROP TABLE IF EXISTS tb_cascade_channel;
CREATE TABLE tb_cascade_channel
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    platform_id        VARCHAR(64)                            NOT NULL, -- 目标级联上级ID
    local_device_id    VARCHAR(64)                            NOT NULL, -- 本地物理设备ID (tb_device.device_id)
    local_channel_id   VARCHAR(64)                            NOT NULL, -- 本地通道ID (tb_device_channel.channel_id)
    
    -- 上报映射
    cascade_channel_id VARCHAR(64)                            NOT NULL, -- 上报给上级平台时的通道国标ID (默认同 local_channel_id)
    cascade_name       VARCHAR(255) DEFAULT NULL,                       -- 上报给上级平台时的名称 (默认同本地通道名称)
    
    enabled            INTEGER      DEFAULT 1                 NOT NULL  -- 是否上报 1-是, 0-否
);

CREATE UNIQUE INDEX uk_platform_local ON tb_cascade_channel (platform_id, local_channel_id);
CREATE INDEX idx_cascade_platform ON tb_cascade_channel (platform_id);
```

---

## 3. 消息接收与后处理细化方案

当上级平台对 Voglander 的本地客户端发起请求时，我们将分别通过 **Listener 实现** 和 **事件订阅** 来处理消息。

```
                    +------------------------------------+
                    |        上级平台 (Superior)         |
                    +------------------------------------+
                      /                |              \
           REGISTER  /       MESSAGE  /       INVITE   \  200 OK (SDP)
         (Digest)   /      (Keepalive/        (RTP IP)  \ (Push Port)
                   /        Catalog) /                   \
                  v                 v                     v
          +---------------+  +---------------+  +------------------+
          |  SipClient    |  |  QueryListener|  |  EventListener   |
          |  RegisterTask |  | (Catalog/Info)|  | (ClientInviteEvt)|
          +---------------+  +---------------+  +------------------+
                  |                 |                     |
                  | 读平台信息       | 查询级联通道        | 1. 获取本地流 (ZLM)
                  | (DB)            | (DB)                | 2. 调用 ZLM 推流
                  v                 v                     v
          +--------------------------------------------------------+
          |                   Voglander 级联模块                   |
          |           (Manager / Service / Repository)             |
          +--------------------------------------------------------+
```

### 3.1 目录与状态查询应答：`CascadeQueryHandler` (实现 `QueryListener`)
我们实现 `QueryListener`，注册为 Spring Bean，由客户端框架自动扫描。当收到上级的 Catalog/DeviceInfo/DeviceStatus 查询时，直接通过返回值实现同步应答：

```
上级平台 (Superior)                              Voglander (Client 侧)
     |                                                    |
     | ----- MESSAGE (Catalog Query XML) -------------->  | (JAIN-SIP 接收并解析)
     |                                                    |
     |                                                    v  自动调用
     |                                             CascadeQueryHandler
     |                                             .onCatalogQuery(...)
     |                                                    |
     |                                                    |-- 1. 解析 platformId
     |                                                    |-- 2. 查 tb_cascade_channel 获取对应通道
     |                                                    |-- 3. 联表 tb_device_channel 补齐状态和名称
     |                                                    |-- 4. 拼装 List<DeviceItem>
     |                                                    v
     | <---- 200 OK (DeviceResponse XML) ---------------- | (框架自动序列化为 XML 回应)
     |                                                    |
```

*   **目录查询 `onCatalogQuery(String platformId, DeviceQuery query)`**：
    1.  从入参 `platformId` 定位上级平台。
    2.  查询 `tb_cascade_channel` 获取关联的 `local_channel_id` 与映射后的 `cascade_channel_id`。
    3.  查询本地数据库中这些通道的状态（`tb_device_channel.status`）。
    4.  构建 `DeviceResponse`，设置 `SumNum`（总通道数），将通道列表转换为 `List<DeviceItem>` 塞入。
    5.  **国标状态映射**：将本地的 `0/1` 状态映射为上报所需要的 `OFF/ON` 字符串。
    6.  **返回**：`DeviceResponse`（框架会自动将该对象序列化为国标要求的 Response XML 答复上级）。
*   **信息查询 `onDeviceInfoQuery(String platformId, DeviceQuery query)`**：
    *   返回本地客户端的基本信息（厂商：`Voglander`，型号：`CascadePlatform`，固件版本：`v1.0.4`）。
*   **状态查询 `onDeviceStatusQuery(String platformId, DeviceQuery query)`**：
    *   返回本地客户端的运行状态。因自身平台一直运行，所以直接返回在线（`status="OK"`, `online="ONLINE"`）。

### 3.2 设备控制命令转发：`CascadeControlHandler` (实现 `ControlListener`)
当上级平台向 Voglander 模拟的通道下发控制指令（如 PTZ 云台控制）时，我们需要在收到消息后将其 **下刷** 转发给实际的下级设备。

```
上级平台 (Superior)            Voglander (Client 侧)           Voglander (Server 侧)            下级 IPC
     |                                  |                                |                        |
     | -- MESSAGE (PTZ hex Cmd) ------> |                                |                        |
     |                                  v 调用                           |                        |
     |                            CascadeControlHandler                  |                        |
     |                            .onPtzControl(...)                     |                        |
     |                                  |                                |                        |
     |                                  |-- 1. 查找通道对应的真实 IPC ID  |                        |
     |                                  |-- 2. 转发给服务端 PtzCommand    |                        |
     |                                  v                                v                        |
     |                                  |------------------------------> | -- INVITE/MESSAGE ---> |
```

*   **云台控制 `onPtzControl(String platformId, String channelId, String hexCmd)`**：
    1.  通过 `channelId` (即上级眼中的通道ID) 检索 `tb_cascade_channel`，找到其映射的 `local_device_id` 和 `local_channel_id`。
    2.  调用 Voglander 平台服务侧的云台指令下发组件 `VoglanderServerPtzCommand.sendPtzControlCommand(localDeviceId, localChannelId, hexCmd)`。
    3.  实现级联到真实设备的透传。

### 3.3 级联媒体取流：`CascadeMediaInviteListener` (事件监听)
上级平台拉取视频流时，会向客户端发送 SIP `INVITE`。框架会将其转换为 `ClientInviteEvent` 事件，业务侧通过 `@EventListener` 异步拦截并进行媒体转发。

```
上级平台 (Superior)            Voglander (Client 侧)           ZLMediaKit (ZLM)            下级设备 (IPC)
     |                                  |                              |                         |
     | ----- INVITE (SDP-上级接收) ---> |                              |                         |
     |                                  v 触发                         |                         |
     |                           ClientInviteEvent                     |                         |
     |                                  |                              |                         |
     |                                  |-- 1. 检查本地流是否已存在  ---|                         |
     |                                  |      (若无，先向 IPC 建立取流) --------------------------> |
     |                                  |-- 2. 调用 ZLM /startSendRtp  |                         |
     |                                  |      将流推送到上级 SDP 端口 |                         |
     |                                  |-- 3. 构造本地回包 SDP-本地   |                         |
     |                                  v                              |                         |
     | <---- 200 OK (SDP-本地) -------- |                              |                         |
     |                                  |                              |                         |
     | ----- ACK ---------------------> |                              |                         |
     |                                  |                              |                         |
     | ========================== RTP 媒体流开始传输 ===========================================> |
```

1.  **监听 `ClientInviteEvent`**：
    *   获得 `callId`、`channelId` (即上报通道国标ID)、`ssrc`、上级接收端媒体 IP (`mediaIp`) 和媒体端口 (`mediaPort`)、传输模式 (TCP/UDP)。
2.  **定位本地流媒体源**：
    *   根据 `channelId` 查找 `tb_cascade_channel`，获取对应的 `local_device_id`。
    *   检查 ZLM 节点上该通道对应的拉流是否已经在线。
    *   **若不在线**：调用 `mediaSessionManager.onInviteOk(...)` 流程，通过国标向真实物理设备发起拉流请求，让 ZLM 先建立与物理摄像头的拉流会话。
    *   **若已在线**：直接复用本地已有流。
3.  **调用 ZLM 推送 RTP 上级**：
    *   流就绪后，构造请求参数调用 ZLM 的 `/index/api/startSendRtp` 接口：
        *   `vhost`: `__defaultVhost__`
        *   `app`: `rtp`
        *   `stream`: `localDeviceId_localChannelId`
        *   `ssrc`: 从上级 INVITE 中解析出的 SSRC
        *   `dst_url`: 上级平台的 `mediaIp`
        *   `dst_port`: 上级平台的 `mediaPort`
        *   `is_udp`: 根据上级协商的传输协议判定 (UDP/TCP)
    *   登记推流状态到 `tb_push_proxy`，状态设置为在线。
4.  **SIP 回包 200 OK**：
    *   构建本地回包 SDP：包含本地媒体 IP、ZLM 分配的接收端口、传输协议等。
    *   调用 Java 客户端 API 回包：
        ```java
        clientCommandSender.send(CommandContext.forInviteResponse(callId, responseSdp));
        ```
5.  **拆线 (BYE)**：
    *   监听上级发送的 `ClientByeEvent`（或通过 `VoglanderBusinessNotifier` 的 `Session.Bye` 桥接）。
    *   接收到 BYE 后，调用 ZLM 的 `/index/api/stopSendRtp` 停止向上级推流。
    *   清理本地会话记录和 `tb_push_proxy` 的推流状态。

---

## 4. 多上级平台注册与心跳机制

### 4.1 端口共享模型

所有上级平台**共享同一个本地 SIP 客户端端口**（默认 `5070`），这是由 `sip-gateway` 框架单 SipClient 实例决定的。`local_port` 字段在表中保留，但实际约束为所有记录使用同一端口值。

区分身份的方式：

- `local_client_id`（对应 SIP `From` 头的 `device_id`）作为向不同上级注册时的虚拟设备 ID
- 不同上级平台看到的是不同的 `local_client_id`，互相隔离

```
本地 SIP 客户端 (port:5070)
        |
        |-- REGISTER --> 上级A (local_client_id = "34020000001320000001", From: A, To: A@upper-a)
        |-- REGISTER --> 上级B (local_client_id = "34020000001320000002", From: B, To: B@upper-b)
        |-- REGISTER --> 上级C (local_client_id = "34020000001320000003", From: C, To: C@upper-c)
```

### 4.2 独立注册与心跳调度

`CascadeClientScheduler` 在应用启动后从数据库加载所有 `enabled=1` 的平台记录，为**每个平台创建独立的定时任务**：

```
CascadeClientScheduler
├── PlatformTask(platformId=A)  → 注册线程A + 心跳线程A (独立 ScheduledFuture)
├── PlatformTask(platformId=B)  → 注册线程B + 心跳线程B (独立 ScheduledFuture)
└── PlatformTask(platformId=C)  → 注册线程C + 心跳线程C (独立 ScheduledFuture)
```

**核心约束**：
- 某个上级平台注册失败或心跳超时，**不影响其他平台**的调度任务
- 重试策略：指数退避，最大 5 次，之后标记 `register_status=3`（失败），等待人工介入或下一轮定时扫描恢复
- `register_status` 状态机：`0(离线) → 2(注册中) → 1(在线) / 3(失败)`

### 4.3 注册与心跳流程

```
CascadeClientScheduler.start()
        |
        v
加载所有 enabled=1 的平台记录
        |
        v
for each platform:
    ┌─────────────────────────────────────────────────────────┐
    │  1. 更新 register_status = 2 (注册中)                   │
    │  2. 构造 RegisterRequest:                               │
    │       From: sip:{local_client_id}@{local_ip}:{local_port}│
    │       To:   sip:{platform_id}@{platform_domain}        │
    │       Contact: sip:{local_client_id}@{local_ip}:5070   │
    │       Expires: register_expires                         │
    │  3. ClientCommandSender.register(platformInfo)          │
    │  4. 收到 401 → Digest 认证 → 二次 REGISTER              │
    │  5. 收到 200 OK → register_status = 1                   │
    │     启动心跳定时器 (keepalive_interval 秒)              │
    │     心跳: ClientCommandSender.keepalive(platformId)     │
    └─────────────────────────────────────────────────────────┘
```

### 4.4 动态管理（运行时增删上级平台）

当通过 REST API 新增/删除/启用/禁用上级平台时，`CascadeClientScheduler` 需支持运行时热更新：

| 操作 | 调度器行为 |
|------|-----------|
| 新增平台（enabled=1） | 立即为该平台创建并启动注册任务 |
| 禁用平台（enabled=0） | 取消对应 ScheduledFuture，发送注销 REGISTER (Expires=0)，更新状态为离线 |
| 删除平台 | 同禁用，并清理内存中的任务引用 |
| 修改平台配置 | 先禁用旧任务，再以新配置重新注册 |

---

## 5. 实施阶段划分 (Stages)

我们计划分阶段实现该功能：

*   **Stage 1：数据库 Schema 与实体持久化**
    *   新增 `tb_cascade_platform` 和 `tb_cascade_channel` 的 DDL 与 MyBatis-Plus 实体/Mapper。
    *   提供 PlatformManager / CascadeChannelManager 的基础 CRUD 及测试。
*   **Stage 2：级联注册与心跳任务调度 (Outbound Client Registration)**
    *   集成 `ClientDeviceSupplier` 实现对上级平台身份的虚拟映射。
    *   实现 `CascadeClientScheduler`，完成自动注册、保活心跳。
*   **Stage 3：查询与控制转发处理**
    *   编写 `CascadeQueryHandler` 和 `CascadeControlHandler` 并注册为 Spring Bean，处理目录上报和云台转发。
*   **Stage 4：双向 INVITE 媒体转发**
    *   实现 `CascadeMediaInviteListener` 捕获上级拉流事件。
    *   完成对 ZLM `startSendRtp` 和回包 SDP 的对接。
