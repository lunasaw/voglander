# Lab 外部平台 SSE 无实时消息排查方案

> 场景：Lab 模拟设备注册到**外部 GB28181 平台**后，前端 ClientPanel 的"收到指令时间线"没有任何 `clientcmd.*` 事件。  
> 自环（注册到本进程 SIP 服务端）工作正常，`device.*` 事件可正常显示在 ServerPanel。

---

## 一、问题分类

排查前先区分两类 `clientcmd.*` 事件，它们的触发路径**不同**：

| 事件类型 | 触发路径 | 经过 `checkDevice`？ | 经过 `isLabDevice`？ |
|---------|---------|---------------------|---------------------|
| `clientcmd.register.ok/fail/challenge` | `ClientRegisterResponseProcessor` → Spring 事件 → `LabRegisterStatusListener` | **否** | **否** |
| `clientcmd.invite/bye/ptz/query.*` 等 | 外部平台发来的 SIP 请求 → `InviteRequestProcessor` 等 → Spring 事件 → `Lab*Listener` | **是**（框架层） | **是**（Listener层二次） |

> ⚠️ 入站指令存在**两道独立过滤**：框架层 `checkDevice`（`VoglanderClientDeviceSupplier`）用动态 effective clientId；Listener 层 `LabInviteListener.isLabDevice()`（及同类 Listener）用静态 `clientProps.getClientId()`。两者 clientId 来源不同，任一过滤失败事件即丢弃。

因此排查分三个独立方向。

---

## 二、排查步骤

### Step 1：确认注册结果事件（最简单验证点）

触发条件：Lab 点击"注册"按钮，外部平台返回 200 OK。

**验证：前端 ClientPanel 左下时间线是否出现 `clientcmd.register.challenge` 或 `clientcmd.register.ok`？**

- ✅ 出现 → 注册事件链路通，跳到 [Step 3](#step-3checkdevice-过滤排查) 排查入站指令
- ❌ 不出现 → 注册事件链路断，继续 Step 2

---

### Step 2：注册事件链路排查

#### 2a. 检查 Bean 是否激活

`LabRegisterStatusListener` 有 `@ConditionalOnProperty(name="voglander.protocol-lab.enabled", havingValue="true")`。

**验证方式**：启动日志搜索：
```
grep "LabRegisterStatusListener" logs/app.log
```
应有 `Creating bean...` 记录。或在 Actuator beans 端点查：
```
curl http://localhost:8080/actuator/beans | grep labRegisterStatusListener
```

- ❌ Bean 不存在 → `voglander.protocol-lab.enabled=true` 未配置，检查 `application-dev.yml`

#### 2b. 确认 `ClientRegisterSuccessEvent` 是否 fire

临时在 `ClientRegisterResponseProcessor.handleRegisterSuccess()` 加日志（或确认已有的 `INFO` 日志）：
```
grep "Register成功" logs/app.log
```
有"Register成功"则事件一定发布了。没有则二次认证（re-auth）失败。

**re-auth 失败可能原因**（已有修复 `VoglanderClientDeviceSupplier.getClientFromDevice()` 密码缓存问题）：
- `toDevice == null`：`buildLabServerDevice()` 两个分支都没命中
  - 分支1（**先查**）`deviceId.equals(snapshot.getServerId())`：外部平台；**若 `snapshot` 为 null（未调用 `labSessionHolder.apply()`）则此分支永远 false**
  - 分支2（**后查**）`deviceId.equals(serverProperties.getServerId())`：自环（本进程平台）

**验证**：日志搜索：
```
grep "设备信息获取失败\|Lab 外部目标解析\|Lab 自环目标解析" logs/app.log
```

#### 2c. 检查 SSE 连接是否建立且包含 `clientcmd` 主题

**方式一（浏览器 Network 面板）**：
打开 DevTools → Network → Filter `events` → 找到 SSE 请求，查看 URL 参数：
```
/api/v1/stream/events?topics=clientcmd%2Cdevice%2Csession%2Calarm%2C...&token=xxx
```
确认 `topics` 包含 `clientcmd`。

**方式二（curl 手动测试）**：
```bash
# 替换 TOKEN 为实际值
curl -N "http://localhost:8080/api/v1/stream/events?topics=clientcmd&token=TOKEN"
# 另一个终端触发注册，观察 curl 输出
```
若 curl 收到 `clientcmd.register.ok` 数据，说明 SSE 链路通，问题在前端订阅。

**方式三（后端 emitter 数量）**：
```bash
curl http://localhost:8080/actuator/metrics/sse.emitters 2>/dev/null
# 或查看日志
grep "SSE 注册成功" logs/app.log | tail -5
```

#### 2d. SSE 连接时序问题

`useSseEvents` 在 `onMounted` 时 topics 为空（config 未拿到），`connect()` 直接 return。  
等 `getLabConfig()` 返回后 `restart()` 才真正建立连接。

**问题场景**：用户在页面打开后**立即注册**，此时 `getLabConfig()` 还未完成，SSE 尚未连接，注册事件丢失。

**验证**：打开页面，等 SSE 状态角标变为绿色"已连接"后再操作注册。

---

### Step 3：`checkDevice` 过滤排查

这是**入站指令**（INVITE/PTZ/目录查询等）全部静默丢弃的根因。

#### 3a. 查看 `checkDevice` 日志

`VoglanderClientDeviceSupplier.checkDevice()` 有 `log.warn("=== DEBUG: ...")` 输出：
```
grep "DEBUG: VoglanderClientDeviceSupplier.checkDevice" logs/app.log | tail -20
```

关注两个字段：
- `toUserId`：外部平台发来请求的 `To:` 头 userId（外部平台自己配置的通道ID）
- `clientId`：本端 Lab 的 clientId

若 `toUserId != clientId` 且 `ownsChannel=false` → 请求被丢弃。

#### 3b. 理解通道 ID 规则

`LabChannelHolder.ownsChannel(clientId, channelId)` 的匹配规则（非标准 GB28181，lab 约定）：
```java
// channelId 必须满足：clientId 前 18 位 + 两位序号（01~count，默认 count=4）
channelIdOf(clientId, i) == clientId.substring(0, 18) + String.format("%02d", i)
```
> ⚠️ 是截取 clientId **前 18 位**作前缀，不是全长 clientId。例：clientId=`34020000001320000011`（20位），通道1 = `340200000013200000` + `01` = `34020000001320000001`。

**外部平台的通道 ID 来自哪里？**

外部平台通过**目录查询（Catalog）**获知设备的通道列表。Lab 需要先向外部平台推送目录，外部平台才知道用哪些通道 ID 发送 INVITE。

**排查流程**：
1. 注册到外部平台后，点击"主动上报目录"（`/lab/client` → "上报目录"按钮）
2. 等外部平台刷新通道列表
3. 外部平台再发 INVITE 时，使用的通道 ID 应与 Lab 上报的一致

若外部平台在目录刷新前就发 INVITE，用的是旧的/不匹配的通道 ID → `ownsChannel=false`。

#### 3c. 验证外部平台发来的通道 ID

从日志中找 `checkDevice` 的 `toUserId`，与 `LabChannelHolder.channelIdOf(clientId, 1~N)` 生成的 ID 对比。

```bash
# 查看 Lab 通道 ID 规则（根据 clientId 推算）
# 假设 clientId = "34020000001000000001"（20位），channelCount = 4
# 前18位 = "340200000010000000"，则通道 ID 为：
# 34020000001000000001  (前18位 + "01")
# 34020000001000000002  (前18位 + "02")
# 34020000001000000003  (前18位 + "03")
# 34020000001000000004  (前18位 + "04")
```

> **注意**：这是 Lab 的非标约定，仅用于 lab 内自环测试。外部平台若用 GB28181 标准（独立20位国标码），ID 格式不同，`ownsChannel` 永远 false。

#### 3d. 直接在 Lab 测试不经 checkDevice 的事件

`clientcmd.register.ok` 不经过 `checkDevice`，是最好的验证基线：
- 注册到外部平台 → 若出现 `clientcmd.register.ok` → SSE 链路本身没问题，入站指令是 `checkDevice` 或 `isLabDevice` 问题
- 若也不出现 → Step 2 还有断点未解决

#### 3e. `LabInviteListener.isLabDevice()` 二次过滤排查

即使 `checkDevice` 通过，`LabInviteListener`（及同类 Listener）还有一道 `isLabDevice()` 过滤。其 clientId 来源与 `checkDevice` **不同**：

| | clientId 来源 | 随 Lab UI 动态变化？ |
|--|-------------|---------------------|
| `checkDevice` | `getClientFromDevice()` → 优先 `LabSessionHolder.snapshot.clientId` | ✅ |
| `LabInviteListener.isLabDevice()` | `clientProps.getClientId()`（`application.yml` 静态值） | ❌ |

**触发场景**：在 Lab UI 填入与 `application.yml` 不同的 clientId → `checkDevice` 用新 clientId 通过 → 框架 dispatch `ClientInviteEvent` → `isLabDevice()` 仍用旧 static clientId → **二次过滤丢弃** → `clientcmd.invite` 永远不推出。

**验证方式**：
```bash
# 对比两个值是否一致
grep "sip.client.client-id\|Lab INVITE过滤" logs/app.log | head -20
```
若日志出现 `"Lab INVITE过滤: userId=xxx 不是Lab设备"` 说明命中此问题。

**修复方向**：`LabInviteListener.isLabDevice()` 改为从 `VoglanderClientDeviceSupplier.getClientFromDevice().getUserId()` 取 effective clientId，与 `checkDevice` 保持同源。

---

## 三、根因总结与对应修复

| 根因 | 现象 | 修复方向 |
|------|------|---------|
| **`isLabDevice()` 二次过滤**：Lab UI clientId 与 `application.yml` 静态值不同，`LabInviteListener` 用 static clientId 判否 | `clientcmd.register.ok` 正常；`clientcmd.invite` 等**永远不出现**；日志有 `"Lab INVITE过滤: userId=xxx 不是Lab设备"` | `isLabDevice()` 改用 `supplier.getClientFromDevice().getUserId()` 取 effective clientId |
| `checkDevice` 过滤：外部平台通道 ID 与 Lab 生成规则不一致 | 入站指令（INVITE/PTZ等）全部丢弃，`clientcmd.register.ok` 正常 | 先推目录再操作；或扩展 `ownsChannel` 接受任意通道 |
| `voglander.protocol-lab.enabled` 未配置 | `LabRegisterStatusListener` 等 Bean 不存在，所有 `clientcmd.*` 均无 | 配置文件加 `voglander.protocol-lab.enabled=true` |
| SSE 时序：页面刚打开就注册，SSE 未连接 | 偶发性丢失，刷页后正常 | 等"已连接"角标绿色后再操作 |
| re-auth 密码缓存（已修复）：`getClientFromDevice()` 缓存命中但密码过期 | 外部平台 digest 验证失败，`clientcmd.register.ok` 不出现 | 已在 `VoglanderClientDeviceSupplier` 修复 |

---

## 四、快速验证脚本

```bash
# 1. 确认 Bean 存在
curl -s http://localhost:8080/actuator/beans | python3 -c "import sys,json; d=json.load(sys.stdin); print([k for k in d['contexts']['application']['beans'] if 'lab' in k.lower() or 'Lab' in k])"

# 2. 手动 SSE 订阅验证
TOKEN="your-token-here"
curl -N "http://localhost:8080/api/v1/stream/events?topics=clientcmd&token=$TOKEN"

# 3. 触发注册后查 checkDevice 日志
grep -E "checkDevice|Register成功|Lab.*目标解析|设备信息获取失败" logs/app.log | tail -30
```

---

## 五、推荐排查顺序

```
打开前端 → 等 SSE 角标绿色 → 注册到外部平台
      ↓
ClientPanel 是否出现 clientcmd.register.challenge 或 ok？
      ├── 否 → Step 2 逐层排查（Bean/re-auth/SSE连接）
      └── 是 → 注册事件链路通，触发入站指令（如外部平台发 INVITE）
                    ↓
            clientcmd.invite 是否出现？
                    ├── 否，且日志有 "Lab INVITE过滤" → Step 3e（isLabDevice 二次过滤，clientId 不同步）
                    ├── 否，且日志有 "checkDevice返回false" → Step 3a/3b/3c（checkDevice 过滤）
                    └── 是 → 链路全通
```
