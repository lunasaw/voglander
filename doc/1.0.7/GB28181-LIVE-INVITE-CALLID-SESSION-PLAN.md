# GB28181 实时点播 INVITE 通道寻址 — Call-ID 会话关联完美方案

> 版本 1.0.7 · 分支 `0608_dev` · 文档日期 2026-06-12 · **本文为已落地方案的设计定稿 + 收敛说明**
> 角色定位：voglander 作为 **GB28181 平台端（上级 / SIP Server）**，向下级设备发起实时点播 INVITE。
> 依赖版本：**sip-gateway / gb28181 1.8.1**（pom 已钉）、zlm 1.0.11。本文事实已对 1.8.1 源码 + jar 核对。
> 关联：[[gb28181-live-invite-channel-addressing]]（记忆）、`doc/1.0.7/GB28181-DEVICE-MANAGEMENT-TECH-PLAN.md`（命令/事件全景）、`doc/1.0.7/LIVE-STREAM-CACHE-CALLBACK-SYNC-TECH-PLAN.md`（关流/缓存同步）。

---

## 0. TL;DR

**起因**：实时点播关流时刷大量 `DialogNotFoundException: no dialog for callId=gb_live_xxx`，BYE 发不出去（用 streamId 当 callId 找 dialog，必然落空）。

**根因（两层）**：
1. **会话关联错位**：`startLive` 预写占位会话时 callId 暂用 streamId，等异步 `Session.InviteOk` 回填真实 Call-ID。但该事件 payload 无 channelId，按 `(deviceId, channelId, INVITING)` 匹配占位行失败 → 真实 callId 落到孤儿行，关流 `getByStreamId` 只能取到占位行的假 callId。
2. **报文寻址 + 下游污染的两难**：GB28181 实时点播 INVITE 标准应寻址到**通道**（Request-URI/To/SDP/Subject 用 channelId）。但框架 `InviteResponseProcessor` 从 200 OK 的 **To 头**反解 deviceId，若 To=channelId 则下游 `Session.InviteOk.deviceId` 被污染成 channelId。

**完美方案一句话**：**协议层用 channelId 合规寻址，会话用 Call-ID 唯一关联，业务身份从 voglander 自己的会话表取——不再从被回显污染的 SIP 报文头反解。**

**改动边界（已锁定，极小）**：
- 框架 `ServerCommandSender`��1 个 channel 寻址重载 + `to.setUserId(channelId)`。
- voglander `VoglanderServerMediaCommand` / `MediaPlayServiceImpl` / `MediaSessionManager`：发 INVITE 同步拿真实 callId、即刻回填占位行。
- voglander `Gb28181ProtocolHandler` 的 `Session.InviteOk`/`Bye`/`MediaStatus`：改用 callId 查会话表，**不读 `event.deviceId()`**。
- **其他所有命令路径（PTZ/控制/查询/目录/配置/报警）一行都不动**——见 §4 系统性论证。

---

## 1. 问题链路（理解后再看方案）

### 1.1 完整因果链

```
startLive(deviceId, channelId)
  │  streamId = gb_live_{deviceId}_{channelId}
  ├─ 5. 预写占位会话: callId=streamId(占位), status=INVITING   ← callId 此刻是假的
  ├─ 7. �� INVITE 给设备
  │       设备回 200 OK ──► 框架建立 dialog(挂在真实 Call-ID 下)
  │                     ──► InviteResponseProcessor 从 To 头反解 deviceId
  │                     ──► 发 Session.InviteOk(deviceId, callId), payload 无 channelId
  │                            │
  │                            └─► onInviteOk 按 (deviceId, channelId=null, INVITING) 匹配占位行
  │                                  └─► channelId=null → 跳过匹配 → 兜底新建孤儿行
  │                                        (真实 callId + ACTIVE + streamId=null)
  └─ 关流: getByStreamId(streamId) → 只命中占位行(假 callId=streamId)
            └─► sendBye(streamId) → 真实 dialog 挂在真实 Call-ID 下 → no dialog ❌
```

### 1.2 关键事实（已源码核对）

| 事实 | 出处 | 含义 |
|------|------|------|
| `deviceInvitePlay(...)` 同步返回真实 INVITE Call-ID | `InviteRequestStrategy.sendRequest`（新生成并返回） | **发 INVITE 当场即可拿真实 callId，无需等异步事件** |
| `ToDevice` 寻址 ID 与传输地址分离 | `AbstractSipRequestBuilder`：`createSipUri(userId, hostAddress)` | `setUserId(channelId)` + 保留父设备 ip:port = 标准「寻址通道、发往父设备」|
| INVITE 200 OK 的 deviceId 来自 **To 头** | `InviteResponseProcessor`：`getUserIdFromToHeader(response)` | To 是平台自填、设备回显 → 填 channelId 就回 channelId（污染源） |
| `Session.InviteOk` 事件只带一个 ID、payload 空 | `Gb28181EventForwarder.onInviteOk` emit `Map.of()` | 框架无法同时给出父 deviceId 和 channelId |
| 其他命令响应 deviceId 来自 **From 头** | Notify/Message/Info/Bye/Register/Subscribe/Ack 各 Processor | From 是设备真身，不被平台回显污染 |

---

## 2. 为什么前两个方案都不完美

矛盾根源只有一个：**框架的 `Session.InviteOk` 从 To 头反解身份，只能得到一个值。**

| 方案 | Request-URI/To | 报文合规 | 下游 `event.deviceId()` | 缺陷 |
|------|---------------|---------|------------------------|------|
| A：To=channelId | channelId | ✅ 合标准 | ❌ 被污染成 channelId | `promoteOnlineIfOffline(deviceId, channelId)` 拿到两个 channelId |
| B：To=deviceId，channelId 只进 SDP | deviceId | ❌ 多通道 NVR 偏离 | ✅ 干净 | 严格按 Request-URI 选通道的 NVR 会推错流 |

两难的本质：**让「协议报文头」去承担「业务身份」的职责**——这正是语义不清的来源。

---

## 3. 完美方案：三层职责彻底分离

### 3.1 核心理念

SIP 会话的唯一标识是 **Call-ID**，不是 To 头里的谁。而 voglander 在 `startLive` 发起点播那��刻，已把完整上下文写进会话表：

```
占位行 = (streamId, deviceId, channelId, callId)   ← 业务身份的权威来源
```

接收侧要确定「这个 callId 属于哪个设备的哪个通道」，**查自己的会话表即可，不该从 SIP 报文头反解**。接受这点后，两难当场消失。

### 3.2 三层各司其职

| 层 | 职责 | 用什么 | 不碰什么 |
|----|------|--------|---------|
| **框架协议层** | SIP 逻辑寻址 | `ToDevice.userId=channelId`（寻址视频源，合标准）；`ip/port`=父设备（传输地址） | 不关心「父设备是谁」 |
| **关联桥梁** | 会话唯一标识 | **Call-ID**（SIP 本质，发起同步拿到） | — |
| **voglander 业务层** | 设备/通道身份 | 按 callId 查会话表拿 deviceId+channelId | **不读 `event.deviceId()`** |

这正是 SIP 标准里 **AOR（寻址）vs Contact（传输）** 的天然分离——`userId=channelId` 语义就是「逻辑寻址到这个视频源」，`ip/port` 是「报文发到这个传输地址」。名副其实，不是 hack。

### 3.3 报文形态（GB28181-2016 §9.2 合规）

```
INVITE sip:{channelId}@{父设备域}  SIP/2.0       ← Request-URI: channelId
To: <sip:{channelId}@{父设备域}>                  ← To: channelId
Call-ID: {真实唯一 Call-ID}
Subject: {channelId}:{ssrc},{平台编码}:0          ← Subject 第一段: channelId
  (报文实际经传输层发往父设备注册的 ip:port)
v=0
o={channelId} 0 0 IN IP4 {收流IP}                 ← SDP o=: channelId
s=Play
u={channelId}:0                                   ← SDP u=: channelId
...
```

设备 200 OK 原样回显 To=channelId → 框架事件 deviceId 字段=channelId（**被污染，但业务层不读它，无害**）。

---

## 4. 系统性论证：污染为何 INVITE 独有

> 关键结论：**该污染不蔓延到任何其他命令路径。** 完美方案的改动面因此被精确锁死。

### 4.1 只有 INVITE 逻辑寻址到「通道」

| 命令类 | 标准寻址目标 | voglander 传参 | 目标 ID 落在哪 |
|--------|-------------|---------------|---------------|
| **INVITE 点播** | **通道**（视频源，§7.2/§9.2） | deviceId + channelId | **报文头**（矛盾点） |
| PTZ / 控制 / 查询 / 配置 | **设备** | 只有 deviceId | MANSCDP **XML 体 `<DeviceID>`**（`new DeviceControlPtz(..., deviceId)`） |
| 注册 / 心跳 / 目录 / 位置 | **设备** | — | 设备自填 |

PTZ 等命令的控制目标写在 XML 体里、且本就是设备级（To=deviceId 完全正确），不存在「该放 channelId 却放 deviceId」的纠结。

### 4.2 只有 INVITE 响应从「To 头」反解身份

| deviceId 来源 | 路径 | 是否污染源 |
|--------------|------|-----------|
| `getUserIdFromToHeader`（平台自填、设备回显） | **仅 `InviteResponseProcessor`** | **是** ✅ 唯一 |
| `getUserIdFromFromHeader`（设备自填真身） | Notify / Message / Info / Bye / Register / Subscribe / Ack 响应 | 否 |

### 4.3 下游 voglander 印证

消费 `event.deviceId()` 的 30+ 处中：设备上线/离线/目录/位置/注册全部来自 From 头或 XML 体的设备真身，不受影响。**唯一受 To 头污染影响的是 `Session.InviteOk` / `Session.Bye` / `Notify.MediaStatus` 这组会话事件**——它们恰好都能用 callId 关联，正是本方案改造目标。

---

## 5. 实施清单

> 改动覆盖 2 仓库、5 个文件。框架改动需 `mvn install` sip-proxy 再重建 voglander。

### 5.1 框架（sip-proxy 1.8.1）

**① `ServerCommandSender.deviceInvitePlay(deviceId, channelId, sdpIp, mediaPort, streamMode)`** — 新增 channel 寻址重载

```java
public String deviceInvitePlay(String deviceId, String channelId, String sdpIp, Integer mediaPort, StreamModeEnum streamMode) {
    if (channelId == null || channelId.isBlank()) {
        return deviceInvitePlay(deviceId, sdpIp, mediaPort, streamMode);   // 退化设备寻址，向后兼容
    }
    ToDevice to = getToDevice(deviceId);   // 传输地址=父设备注册 ip:port（通道不独立注册）
    to.setUserId(channelId);               // 寻址 ID=通道 → Request-URI/To 合标准
    to.setStreamMode(streamMode.name());
    FromDevice from = deviceSupplier.getServerFromDevice();
    InviteRequest req = new InviteRequest(channelId, streamMode, sdpIp, mediaPort);   // SDP o=/u=/Subject 用 channelId
    return factory.getStrategy("server", "INVITE")
        .execute(CommandContext.builder().role("server").commandType("INVITE")
            .fromDevice(from).toDevice(to).content(req.getContent()).build());
}
```

**② `Gb28181WhitelistHandlers.invitePlay`** — 从 payload 取可选 channelId

```java
@CommandMapping("gb28181.Invite.Play")
public String invitePlay(GatewayCommand cmd) {
    Map<String, Object> p = cmd.payload();
    return sender.deviceInvitePlay(cmd.deviceId(),
            (String) p.get("channelId"),                       // 可选；空则退化设备寻址
            (String) requireField(p, "mediaIp", cmd.type()),
            ((Number) requireField(p, "mediaPort", cmd.type())).intValue(),
            streamMode(p));
}
```

### 5.2 voglander 出站 + 回填

**③ `VoglanderServerMediaCommand.inviteRealTimePlayWithCallId(deviceId, channelId, sdpIp, mediaPort, streamMode)`** — payload 带 channelId，走 `dispatchEnvelopeWithCallId` 返回真��� Call-ID（`ResultDTO<String>`）。

**④ `MediaPlayServiceImpl.startLive`** — 发 INVITE 后即刻回填：

```java
ResultDTO<String> inviteResult = voglanderServerMediaCommand.inviteRealTimePlayWithCallId(
    dto.getDeviceId(), dto.getChannelId(), sdpIp, rtpPort, toStreamModeEnum(dto.getStreamMode()));
if (inviteResult == null || !inviteResult.isSuccess()) { cleanupFailed(streamId, node); throw ...; }
String realCallId = inviteResult.getData();
if (realCallId != null && !realCallId.isBlank()) {
    mediaSessionManager.backfillCallIdByStreamId(streamId, realCallId);   // 占位行 callId: streamId → 真实值
}
```

**⑤ `MediaSessionManager.backfillCallIdByStreamId(streamId, realCallId)`** — 新增，幂等：按 streamId 找占位行，callId 替换为真实值（真实 Call-ID 由本次 INVITE 新生成，表中不存在，不触发 UNIQUE 冲突）。

### 5.3 voglander 接收侧（去污染核心）

**⑥ `Gb28181ProtocolHandler` 的 `Session.InviteOk` 分支** — 改用 callId 查会话表，**不读 `event.deviceId()`**：

```java
case "Session.InviteOk":
    String callId = event.correlationId();
    MediaSessionDTO sess = mediaSessionManager.getByCallId(callId);   // ← 权威身份来源(发起时已回填)
    mediaSessionManager.onInviteOk(callId);                            // 按 callId 置 ACTIVE(必命中, 简化)
    if (sess != null && sess.getDeviceId() != null && sess.getChannelId() != null) {
        deviceChannelManager.promoteOnlineIfOffline(sess.getDeviceId(), sess.getChannelId(), LocalDateTime.now());
    }
    break;
// Session.Bye / Notify.MediaStatus 同理：用 callId 查会话表拿 deviceId/channelId，不读 event.deviceId()
```

> `onInviteOk` 可顺势简化：callId 已被 backfill 回填，按 callId 必命中占位行，不再需要 `(deviceId, channelId, INVITING)` 兜底匹配分支。

### 5.4 不改清单（明确边界）

- **不改** 框架其他命令 spec / Processor（PTZ/控制/查询/目录/配置/报警/注册/心跳全部设备级，From 头/XML 体取身份，无污染）。
- **不改** voglander 其他事件 handler（设备上线/离线/目录/位置/注册均用设备真身）。
- **不改** zlm-starter。

---

## 6. 验证

| 层级 | 用例 | 断言 |
|------|------|------|
| Manager 集成 | `backfillCallIdByStreamId` 命中占位行 | callId 替换为真实值、可按真实 callId 查到 |
| Manager 集成 | `backfillCallIdByStreamId` 无占位行 | 返回 null，不抛异常 |
| Manager 集成 | `Session.InviteOk` 走 callId 查表 | `promoteOnlineIfOffline` 拿到的是会话表的 deviceId/channelId，非事件字段 |
| Service 集成 | `startLive` 全链 | mock `inviteRealTimePlayWithCallId` 返回 callId，验证回填被调用 |
| 实跑 | 验证台 ffmpeg 点播 → 关流 | 日志出现 `回填真实 callId 成功`；关流**无** `DialogNotFoundException` |

**构建顺序**（框架改动后）：
```bash
(cd sip-proxy && mvn install -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true \
   -pl sip-gb28181/gb28181-server,sip-gateway/gateway-gb28181 -am)
(cd voglander && mvn clean install -DskipTests -pl voglander-integration,voglander-service,voglander-manager -am)
(cd voglander && mvn clean test -pl voglander-web -am \
   -Dtest='MediaSessionManagerTest,MediaPlayServiceIntegrationTest,VoglanderServerMediaCommandEnvelopeTest' \
   -Dsurefire.failIfNoSpecifiedTests=false -Dspring.profiles.active=test)
```

---

## 7. 落地状态

> 截至 2026-06-12，本文定稿的完美方案 **§5.1①② / §5.2③④⑤ / §5.3⑥ 全部已落地并通过测试（验证套件 57 绿：MediaSessionManagerTest 21 + MediaPlayServiceIntegrationTest 6 + VoglanderServerMediaCommandEnvelopeTest 11 + Gb28181ProtocolHandlerTest 19）**，代码与方案完整对齐。

| 项 | 状态 | 说明 |
|----|------|------|
| §5.1① 框架寻址 | ✅ `to.setUserId(channelId)` | Request-URI/To/SDP/Subject 均 channelId，合 GB28181-2016 §9.2 |
| §5.1② whitelist handler | ✅ 取可选 channelId | 空则退化设备点播 |
| §5.2③ 出站带 callId | ✅ `inviteRealTimePlayWithCallId` | 返回真实 Call-ID |
| §5.2④⑤ 即刻回填 | ✅ `backfillCallIdByStreamId` | startLive 发 INVITE 后回填占位行 |
| §5.3⑥ 接收侧 InviteOk | ✅ callId 查表 | `onInviteOk(callId)` 置 ACTIVE；promote 取会话表 deviceId/channelId，不读 `event.deviceId()` |
| Bye/MediaStatus 去污染 | ✅ `onBye` 寻址无关 | 框架这两类事件不带 callId，故按 `device_id OR channel_id` 匹配，From 头携带 deviceId/channelId 均能正确关流 |

### 7.1 与初版设计的一处务实调整

§5.3⑥ 原设想「Bye/MediaStatus 也用 callId 查表」，但核对框架 `Gb28181EventForwarder` 发现 **只有 `Session.InviteOk` 族带 callId，`Session.Bye` 与 `Notify.MediaStatus` 的 correlationId 恒为 null**（`emit(..., null, ...)`）。故 Bye/MediaStatus 无法用 callId，改为 `MediaSessionManager.onBye` 按 `device_id OR channel_id` 匹配——同样达成寻址无关、不被 To 头污染，且无需再改框架。这是比「读 event.deviceId()」更健壮的等价实现。

---

## 8. 一句话收尾

**会话类事件用 Call-ID 关联，命令类事件用 From/XML 的设备真身，各管各的，互不交叉。** 协议层只管合规寻址，业务身份永远从 voglander 自己写入的会话上下文取——这是语义清晰、结构清晰的唯一形态。
