# Voglander × sip-gateway 1.8.0 接入方案（已实施 + 后续工作）

> **版本**：voglander `1.0.2-SNAPSHOT` · sip-gateway `1.8.0`
> **首次撰写**：2026-05-29 · **回写更新**：2026-06-01
> **状态**：Stage 1/2/4 主链路已落地；Stage 3 envelope 改造、Stage 5 清理按本回写版执行
> **关联文档**：[../../README.md](../../README.md) §📡 GB28181 接入方案、[sip-proxy/sip-gateway/README.md](../../../sip-proxy/sip-gateway/README.md)

---

## 〇、为何回写

原方案（2026-05-29 版）按"激进重构 + envelope 全面替换 + 删除底层 jar"路线撰写。实施期发现以下事实与原方案冲突，已在代码侧调整：

1. **`gb28181-client` 不能由 starter 传递依赖**——`sip-gateway-spring-boot-starter` 仅依赖 `gateway-gb28181`，后者仅强依赖 `gb28181-server`，**不带 `gb28181-client`**。voglander 的 client 命令（设备侧主动上报）必须保留 `gb28181-client` 显式依赖。
2. **`ServerStart.java` 不能删除**——`sip-gateway` 的自动配置不调用 `SipLayer.addListeningPoint(...)`，starter **不接管 SIP 端口绑定**。`ServerStart` 是全工程唯一调用 `addListeningPoint` 的地方，删除会导致协议栈无法监听端口。
3. **Command 类总数是 12，不是 14**——实际为 6 server + 6 client（PTZ/Device/Record/Config/Alarm/Media + Catalog/Status 拆分实际 6+6 而非 7+7）。
4. **Envelope 仅适用于 server commands**——`CommandHandlerRegistry` 与 `@CommandMapping` 是平台→设备方向的统一调度面；client 命令（`ClientCommandSender`）是设备→平台方向，**不在 envelope 体系内**。
5. **`CommandHandlerRegistry` 真实 API**：`registry.require(type).handle(cmd)` 返回 `GatewayCommandResult`，而非原方案的 `commandRegistry.handle(cmd)` 单步调用。
6. **`Gb28181CommandSpecs.declare()` 实际声明 30 个 cmdType**（非 39）；剩余 ~20 个由 `@CommandMapping` 注解（`Gb28181WhitelistHandlers` 等）补充，其中部分覆盖 declare 表里的同名 type（`overrideTable=true`）。

---

## 一、现状对比（已对齐）

| 维度 | 旧版（1.2.5-SNAPSHOT） | 当前实施（1.8.0） |
|---|---|---|
| **依赖** | `gb28181-client` + `gb28181-server`（integration）+ `gb28181-common`（manager）显式声明 | 顶层引入 `sip-gateway-bom` + `sip-gateway-spring-boot-starter`；`gb28181-client` 显式保留（client 命令依赖）；`gb28181-server` 由 starter 传递可省（已验证）；`gb28181-common` **必须保留显式声明**（manager 是 integration 上游模块，传递依赖反向不可达，2026-06-01 实测确认） |
| **协议栈启动** | `ServerStart` 手工 `addListeningPoint` | **保留 `ServerStart`**（starter 不接管端口绑定）；启动类加 `@EnableSipServer` 触发自动配置 |
| **设备供应** | `Voglander*DeviceSupplier`（SPI） | **保留并标注 `@Primary`**（避免与框架默认 `DefaultServerDeviceSupplier`/`DefaultClientDeviceSupplier` 冲突）；新增 `VoglanderDeviceSessionCache`（`ServerCommandSender` 构造前置） |
| **入站事件** | 21 个 `Voglander*Handler` 散落四个目录 | 1 个 `VoglanderBusinessNotifier`（直接 `implements BusinessNotifier`，**不**继承 `AbstractProtocolBusinessNotifier`，因后者 `notify()` 为 `final` 自调用导致 `@Async` 失效） |
| **出站命令（Server 侧）** | `VoglanderServer*Command` 直接调 `ServerCommandSender` | **本期 Stage 3 改造**：注入 `CommandHandlerRegistry`，构造 `GatewayCommand` 走 envelope；6 个 server command 逐个 TDD 改造 |
| **出站命令（Client 侧）** | `VoglanderClient*Command` 直接调 `ClientCommandSender` | **保持不变**——envelope 仅服务平台→设备方向，client 命令为设备→平台方向，不进入 envelope 体系 |
| **INVITE 上下文** | 进程内 Dialog | `RedisInviteContextStore`（`@ConditionalOnProperty(gateway.gb28181.store.type=redis)`）+ 默认 `InMemoryInviteContextStore` 单机 |
| **配置前缀** | `sip.server.*` / `sip.client.*` / `sip.common.time-sync.*` | 新增 `gateway.node-id` / `gateway.gb28181.store.type` / `gateway.gb28181.invite-context-ttl-ms`；保留 `sip.server.*` 与 `sip.client.*`；`sip.common.time-sync.*` 暂保留兼容（待 Stage 5 评估关闭） |
| **type 命名** | 隐式 | 三段式 `gb28181.Group.Name`（已在 Notifier 中按 30+ 个事件型分发） |
| **错误码** | 无统一约定 | HTTP 400/404/410/502/503 各自语义清晰（未来 envelope HTTP 暴露时） |

---

## 二、模块迁移映射（最终态）

| 路径 | 状态 | 说明 |
|---|---|---|
| `voglander-integration/.../wrapper/gb28181/start/ServerStart.java` | 🔒 **必须保留** | 全工程唯一 `addListeningPoint` 调用处，删除会导致 SIP 端口不监听 |
| `voglander-integration/.../wrapper/gb28181/start/SipServerConfig.java` | 🔒 保留 | 配置 binding |
| `voglander-integration/.../wrapper/gb28181/config/properties/VoglanderSip*Properties.java` | 🔒 保留 | 业务侧自有属性 |
| `voglander-integration/.../wrapper/gb28181/supplier/Voglander*DeviceSupplier.java` | 🔒 保留 + `@Primary` | starter SPI 入口 |
| `voglander-integration/.../wrapper/gb28181/supplier/VoglanderDeviceSessionCache.java` | ➕ **新增** | 委托 `VoglanderServerDeviceSupplier` 提供 `ToDevice` 寻址，`ServerCommandSender` 构造前置门控 |
| `voglander-integration/.../wrapper/gb28181/server/request/**`（6 个） | ➖ **已删除**（Stage 2） | 收敛进 Notifier |
| `voglander-integration/.../wrapper/gb28181/server/response/**`（3 个） | ➖ **已删除**（Stage 2） | 同上 |
| `voglander-integration/.../wrapper/gb28181/client/request/**`（7 个） | ➖ **已删除**（Stage 2） | 同上 |
| `voglander-integration/.../wrapper/gb28181/client/response/**`（5 个） | ➖ **已删除**（Stage 2） | 同上 |
| `voglander-integration/.../wrapper/gb28181/notifier/VoglanderBusinessNotifier.java` | ➕ **新增**（Stage 2 完成） | 唯一回调入口，`@Async("sipNotifierExecutor")` |
| `voglander-integration/.../wrapper/gb28181/store/RedisInviteContextStore.java` | ➕ **新增**（Stage 4 完成） | 多节点 INVITE 上下文 |
| `voglander-integration/.../wrapper/gb28181/server/command/**`（6 个） | 🔁 **本期改造** | 注入 `CommandHandlerRegistry` 走 envelope（Stage 3） |
| `voglander-integration/.../wrapper/gb28181/client/command/**`（6 个） | 🔒 **不改造** | envelope 不覆盖此方向，保留 `ClientCommandSender` 直接调用 |
| `voglander-manager/manager/MediaSessionManager.java` | ➕ **新增**（Stage 2 配套） | 媒体会话状态机（INVITE/Bye/Ack/MediaStatus） |
| `voglander-service/login/DeviceRegisterServiceImpl.java` | 🔒 保留 | 由 Notifier 在 Lifecycle 事件时调用 |
| `voglander-manager/manager/DeviceManager / DeviceChannelManager` | 🔒 保留 | 业务层无感（部分 onAlarm/onPosition 路由暂不实现，见第七章） |

**净变更**：
- ➖ 已删除 21 个 handler（Stage 2）
- ➕ 已新增 `VoglanderBusinessNotifier`、`MediaSessionManager`、`RedisInviteContextStore`、`VoglanderDeviceSessionCache`
- 🔁 本期改造 6 个 server command 走 envelope
- 🔒 保留 `ServerStart`、12 个 supplier/property 类、6 个 client command

---

## 三、依赖迁移（最终态）

### 3.1 主 `pom.xml`

```xml
<properties>
    <!-- 旧 1.2.5-SNAPSHOT 仅遗留为 client 显式依赖版本号 -->
    <gb28181-proxy.version>1.8.0</gb28181-proxy.version>
    <sip-gateway.version>1.8.0</sip-gateway.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- spring-boot-dependencies BOM 之后导入 -->
        <dependency>
            <groupId>io.github.lunasaw</groupId>
            <artifactId>sip-gateway-bom</artifactId>
            <version>${sip-gateway.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 3.2 `voglander-integration/pom.xml`

```xml
<dependencies>
    <!-- starter：自动激活 sip-gateway 1.8.0 全部能力，传递 gateway-gb28181/gb28181-server/gb28181-common -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>sip-gateway-spring-boot-starter</artifactId>
    </dependency>

    <!-- gb28181-client：starter 不传递（gateway-gb28181 仅强依赖 server）；
         本工程的 6 个 client command 直接使用 ClientCommandSender，必须显式保留 -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>gb28181-client</artifactId>
        <version>${gb28181-proxy.version}</version>
    </dependency>

    <!-- 不需要显式声明 gb28181-server / gb28181-common（由 starter 传递） -->
</dependencies>
```

### 3.3 `voglander-manager/pom.xml`

```xml
<!-- gb28181-common：必须显式声明，无法删除。
     voglander-manager 是 voglander-integration 的【上游】模块（integration 反向依赖 manager），
     因此无法经 integration → starter → gateway-gb28181 → gb28181-server → gb28181-common 传递获取。
     DeviceDTO 用到 StreamModeEnum，本模块直接用 1.8.0 版本声明。
     2026-06-01 已通过删除-编译-失败实测验证此约束。 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-common</artifactId>
    <version>${gb28181-proxy.version}</version>
</dependency>
```

### 3.4 验证

```bash
mvn dependency:tree | grep -E "gb28181|sip-gateway|sip-common"
# 预期：
# - sip-gateway-spring-boot-starter:1.8.0（compile）
# - gateway-core:1.8.0、gateway-gb28181:1.8.0
# - gb28181-server:1.8.0（starter 传递）
# - gb28181-common:1.8.0（gb28181-server 传递）
# - gb28181-client:1.8.0（voglander-integration 显式声明）
# - 无任何 1.2.5-SNAPSHOT 残留
```

---

## 四、配置（最终态）

### 4.1 `voglander-integration/src/main/resources/application-inte.yml`

```yaml
sip:
  enable: ${local.sip.enable}
  enable-log: ${local.sip.enable-log}
  common:
    # 时间同步：1.8.0 starter 内置时钟校准，但保留兼容；如确认无副作用可在 Stage 5 关闭
    time-sync:
      enabled: true
      mode: BOTH
      offset-threshold: 1000
      ntp-server: "pool.ntp.org"
      ntp-sync-interval: 3600
  server:
    enabled: ${local.sip.server.enabled:false}
    ip: ${local.sip.server.ip:127.0.0.1}
    port: ${local.sip.server.port:5060}
    # 多节点 + VIP 部署必填
    external-ip: ${local.sip.server.external-ip:}
    external-port: ${local.sip.server.external-port:0}
    # ... 其他
  client:
    enabled: ${local.sip.client.enabled:false}
    # ... 其他

# sip-gateway 1.8.0 ���关配置
gateway:
  node-id: ${local.gateway.node-id:node-1}
  forward-timeout-ms: ${local.gateway.forward-timeout-ms:3000}
  gb28181:
    store:
      type: ${local.gateway.gb28181.store.type:memory}        # memory(单机) | redis(多节点)
    invite-context-ttl-ms: ${local.gateway.gb28181.invite-context-ttl-ms:30000}
    invite-idempotency-window-ms: ${local.gateway.gb28181.invite-idempotency-window-ms:5000}
```

---

## 五、落地步骤（按本回写版重新规划）

### Stage 1：依赖切换 & starter 接入【已完成】

**实际成果**：
- ✅ 主 pom 引入 `sip-gateway-bom`
- ✅ `voglander-integration/pom.xml` 引入 `sip-gateway-spring-boot-starter`
- ✅ `ApplicationWeb` 标注 `@EnableSipServer`
- ✅ `application-inte.yml` 新增 `gateway.*` 段
- ✅ **依赖可用性自检测试** `SipGatewayDependencyAvailabilityTest`（2026-06-01 落地）：覆盖 6 大类共 12 个关键类的加载验证
- ⚠️ **`gb28181-common` 必须显式保留**（不能从 voglander-manager 删除）：实测验证 manager 是 integration 上游模块，传递依赖反向不可达；文档第三章已记录

### Stage 2：BusinessNotifier 实现【已完成（主链路）】

**实际成果**：
- ✅ `VoglanderBusinessNotifier` 直接 `implements BusinessNotifier` + `@Async("sipNotifierExecutor")`
- ✅ `sipNotifierExecutor` 在 `voglander-manager/.../ThreadPoolConfig.java:54` 定义
- ✅ 21 个旧 handler 全部删除
- ✅ 主链路事件路由：Lifecycle.Register/Online/Offline/RemoteAddressChanged、Notify.Keepalive、Notify.MediaStatus、Response.Catalog、Response.DeviceInfo、Session.InviteOk/InviteFailure/Ack/Bye
- ⚠️ **暂保持 log 不路由**：Notify.Alarm、Notify.MobilePosition、Response.RecordInfo、Response.PtzPosition、Response.SdCardStatus 等（见第七章「已知未实现项」）

### Stage 3：出站 Server 命令切换到 envelope【本期实施】

**Goal**：6 个 `VoglanderServer*Command` 内部由直接 `ServerCommandSender` 调用改为构造 `GatewayCommand` 走 `CommandHandlerRegistry`。

**实施策略（TDD，每个 Command 类一个独立提交）**：
1. 注入 `CommandHandlerRegistry` 到 `AbstractVoglanderServerCommand`
2. 新增 `dispatchEnvelope(String type, String deviceId, Map<String,Object> payload)` 工具方法
3. 子类内部把原 `serverCommandSender.deviceXxx(...)` 调用改为：
   ```java
   Map<String, Object> payload = Map.of("cmdCode", cmdCode, "horizonSpeed", h, ...);
   return dispatchEnvelope("gb28181.Control.Ptz", deviceId, payload);
   ```
4. 每改一个类，**先写失败测试（RED）**，再做 GREEN，再 REFACTOR；测试 mock `CommandHandlerRegistry`，断言：
   - 调用 `registry.require(<expected-type>)` 一次
   - 传给 `handler.handle(GatewayCommand)` 的 `cmd.type()` / `cmd.deviceId()` / `cmd.payload()` 关键字段匹配 spec

**改造顺序与 cmdType 映射**：

| 顺序 | 类 | 关键 cmdType | 提交点 |
|---|---|---|---|
| 1 | `VoglanderServerPtzCommand` | `gb28181.Control.Ptz`（白名单 @CommandMapping） | refactor(server-cmd): ptz |
| 2 | `VoglanderServerDeviceCommand` | `gb28181.Query.DeviceInfo`/`DeviceStatus`/`Catalog`/`PresetQuery`/`MobilePosition` | refactor(server-cmd): device |
| 3 | `VoglanderServerRecordCommand` | `gb28181.Query.RecordInfo`（白名单）+ `gb28181.Control.Record` | refactor(server-cmd): record |
| 4 | `VoglanderServerConfigCommand` | `gb28181.Config.BasicParam`/`ConfigDownload` + `gb28181.Control.Reboot` | refactor(server-cmd): config |
| 5 | `VoglanderServerAlarmCommand` | `gb28181.Query.AlarmQuery`（白名单）+ `gb28181.Control.AlarmReset` | refactor(server-cmd): alarm |
| 6 | `VoglanderServerMediaCommand` | `gb28181.Invite.Play`/`Playback`/`Bye`/`Ack` + `gb28181.Device.Broadcast` | refactor(server-cmd): media |

**Success Criteria**：
- 6 个 server command 类内部不再直接引用 `ServerCommandSender` 实例方法（仅 envelope dispatch）
- 每个类的单元测试覆盖每个 cmdType，断言 type/payload schema 正确
- `mvn test` 全绿
- `grep "serverCommandSender\." voglander-integration/.../server/command` 仅余 `AbstractVoglanderServerCommand` 内部转发或为空

**关键约束**：
- 保留所有 public method 签名不变（业务上层无感）
- payload 字段名/类型严格对照 `Gb28181CommandSpecs.declare()` 与 `Gb28181WhitelistHandlers` 的 `@CommandMapping` 实现，错位会导致 `ClassCastException`

### Stage 4：INVITE 上下文 Redis 化【已完成】

**实际成果**：
- ✅ `RedisInviteContextStore.java` 已实现（98 行）
- ✅ `@ConditionalOnProperty(gateway.gb28181.store.type=redis)`
- ✅ 异常时抛 `ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)`
- ⏳ **本回写版补做**：集成测试 `RedisInviteContextStoreIntegrationTest`（Redis 不可用时通过 `Assumptions.assumeTrue` 自动跳过）

### Stage 5：清理与文档同步【本期收尾】

**Tasks**：
1. **本文档（你正在读的这份）已按现状回写**
2. `voglander-manager/pom.xml` 删除 `gb28181-common`（Stage 1 补做）
3. 评估 `sip.common.time-sync.*` 是否可关闭（与 starter 内置时钟校准的关系），决策记录到此文档
4. 全量 `mvn clean install -DskipITs` 确认零警告

**Success Criteria**：
- `grep -rn "Voglander.*Handler" voglander-integration/src/main/java` 无结果（已达成）
- `grep -rn "1.2.5-SNAPSHOT" voglander*/pom.xml` 无结果（已达成）
- `mvn dependency:tree` 输出干净

---

## 六、风险与回滚

| 风险 | 缓解 | 回滚 |
|---|---|---|
| `BusinessNotifier#notify` 同步阻塞 → 设备超时重传 | 直接 `implements BusinessNotifier` + `@Async("sipNotifierExecutor")`，避免 final 自调用陷阱 | 已落地，此风险消除 |
| 多节点忘配 `sip.server.external-ip` → 回包 IP 哈希失效 | 部署文档明确要求；单节点降级直通 | 配置回滚 |
| 旧依赖 `1.2.5-SNAPSHOT` 与 `1.8.0` 共存 | `mvn dependency:tree` 校验，本期已无残留 | pom 回滚 |
| INVITE 上下文 Redis 故障 → 跨节点回包失败 | `RedisInviteContextStore` 抛 503，业务侧 502/503 短重试 | 切回 `gateway.gb28181.store.type=memory`（仅单节点可用） |
| envelope payload schema 与协议栈不匹配 → ClassCastException | 严格按 `Gb28181CommandSpecs.declare()` 与 `@CommandMapping` 方法实现取字段，TDD 单元测试覆盖 | Stage 3 每个 Command 独立提交独立可回退 |
| Stage 1 删除 `gb28181-common` 导致编译失败 | 删除前 `mvn compile` 验证；失败则保留显式依赖 | 还原 pom |

**整体回滚策略**：每个 Stage 单独提交，TDD 单元独立可回退。

---

## 七、关键约束（实施期必须遵守）

- ⚠️ **必须与 sip-proxy 同 JVM**：`SipTransactionRegistry`/`Dialog` 都是进程内对象，跨进程后无法回包。
- ⚠️ **`BusinessNotifier#notify` 必须异步**：直接 `implements BusinessNotifier`，**不**继承 `AbstractProtocolBusinessNotifier`（`final notify()` 自调用导致 `@Async` 失效）。
- ⚠️ **多节点必须 Redis**：生产环境必须切到 `RedisInviteContextStore`。
- ⚠️ **`gateway-core` 协议中立**：voglander 业务代码不得直接依赖 `gateway-gb28181` 内部类，仅通过 envelope 与 `BusinessNotifier` 交互。
- ⚠️ **设备供应器保留 `@Primary`**：`Voglander*DeviceSupplier` 必须 `@Primary`，避免与框架默认 supplier `NoUniqueBeanDefinitionException`。
- ⚠️ **client 命令不进入 envelope**：`ClientCommandSender` 是设备→平台方向，envelope 是平台→设备方向；改造仅针对 server commands。
- ⚠️ **`ServerStart` 不能删除**：starter 不接管端口绑定，必须保留 `addListeningPoint` 调用。

### 已知未实现项（暂保持 log，按业务需求驱动后续补齐）

| 事件 type | 当前状态 | 后续目标 |
|---|---|---|
| `gb28181.Notify.Alarm` | log only | `DeviceManager.onAlarm(event)` 落库或发 MQ |
| `gb28181.Notify.MobilePosition` | log only | `DeviceManager.onPosition(event)` 落库 |
| `gb28181.Response.RecordInfo` | log only | `DeviceChannelManager.onRecordInfo(event)` 落库 |
| `gb28181.Response.PtzPosition` | log only | `DeviceChannelManager.onPtzPosition(event)` 落库 |
| `gb28181.Response.SdCardStatus`/`HomePosition`/`CruiseTrack*` | log only | 按业务需求驱动 |

---

## 附录 A：cmdType 全表（30 declare + 20 @CommandMapping）

> 来源：`sip-gateway/gateway-gb28181/handler/Gb28181CommandSpecs.declare()` + `Gb28181WhitelistHandlers` 等 `@CommandMapping`

### Query（10）
`DeviceInfo` / `DeviceStatus` / `Catalog` / `PresetQuery` / `MobilePosition`(`interval`) / `PtzPosition` / `SdCardStatus` / `HomePosition` / `CruiseTrackList` / `CruiseTrack`(`number:int`)
**白名单**：`RecordInfo`(`startTime`/`endTime`/`type`)、`AlarmQuery`(`startAlarmPriority`/`endAlarmPriority`/`alarmMethod` 等)

### Subscribe（3 + 3 白名单）
`Catalog`(`expires:int`/`eventType`) / `PtzPosition`(`expires:int`) / `Unsubscribe`(callId)
**白名单**：`MobilePosition` / `Alarm` / `Refresh`

### Control（10 + 9 白名单）
`Reboot` / `Record`(`recordCmd`) / `Guard`(`guardCmd`) / `IFrame` / `HomePosition` / `PtzPrecise`(`pan/tilt/zoom:double`) / `FormatSDCard`(`sdNumber:int`) / `ScanSpeed` / `DragZoomIn`/`Out`(`dragZoom:DragZoom`)
**白名单**：`Ptz`(`cmdCode/horizonSpeed/verticalSpeed/zoomSpeed`)、`AlarmReset`、`FI`、`Preset`、`Cruise`、`CruiseSpeedOrTime`、`Scan`、`Auxiliary`、`TargetTrack`

### Config（3）
`BasicParam`(`name/expiration/heartBeatInterval/heartBeatCount`) / `Osd`(`osdInfo`) / `ConfigDownload`(`configType`)

### Invite/Session（1 + 6 白名单）
`Bye`(callId)
**白名单**：`Play`/`Playback`/`Talk`/`Download`/`PlaybackControl`/`Ack`

### Device（3）
`Upgrade`(`firmware/fileURL/manufacturer/sessionId`) / `SnapShot`(`snapNum:int/interval:int/uploadURL/sessionId`) / `Broadcast`

---

## 附录 B：事件 type 全表（35 个）

> 来源：`sip-gateway/gateway-gb28181/forwarder/Gb28181EventForwarder`

**Lifecycle (5)**：`Register` / `RegisterChallenge` / `Online` / `Offline` / `RemoteAddressChanged`
**Notify (7)**：`Alarm` / `Keepalive` / `MediaStatus` / `MobilePosition` / `UpgradeResult` / `SnapShotFinished` / `VideoUpload`
**Session (7)**：`InviteTrying` / `InviteOk` / `InviteFailure` / `Ack` / `Bye` / `ByeError` / `ServerInvite`
**Response (16)**：`Catalog` / `DeviceInfo` / `DeviceInfoError` / `DeviceInfoRequest` / `DeviceStatus` / `RecordInfo` / `PtzPosition` / `SdCardStatus` / `HomePosition` / `CruiseTrackList` / `CruiseTrack` / `Config` / `ConfigDownload` / `PresetQuery` / `Subscribe` / `NotifyUpdate`

---

## 附录 C：HTTP 错误码契约（envelope 远程调用语义）

| HTTP | 场景 | 业务侧动作 |
|---|---|---|
| 400 | payload 字段缺失/类型错误 | 修正请求 |
| 404 | type 不存在 | 修正 type 字符串 |
| 410 | 事务已终止/超时 | **禁止重试** |
| 502 | 跨节点路由失败 | 200ms × 3 短重试 |
| 503 | store 后端不可达 | 短重试 |

---

## 附录 D：关键文件清单（最终态）

### 新建（已完成）
| ���径 | 用途 |
|---|---|
| `voglander-integration/.../gb28181/notifier/VoglanderBusinessNotifier.java` | 统一事件回调 |
| `voglander-integration/.../gb28181/store/RedisInviteContextStore.java` | 多节点 INVITE 上下文 |
| `voglander-integration/.../gb28181/supplier/VoglanderDeviceSessionCache.java` | `ServerCommandSender` 构造前置门控 |
| `voglander-manager/.../manager/MediaSessionManager.java` | 媒体会话状态机 |

### 已删除（Stage 2）
| 路径 | 数量 |
|---|---|
| `voglander-integration/.../gb28181/server/request/**` | 6 |
| `voglander-integration/.../gb28181/server/response/**` | 3 |
| `voglander-integration/.../gb28181/client/request/**` | 7 |
| `voglander-integration/.../gb28181/client/response/**` | 5 |

### 本期改造（Stage 3）
| 路径 | 数量 | 改造内容 |
|---|---|---|
| `voglander-integration/.../gb28181/server/command/**` | 6 | 注入 `CommandHandlerRegistry`，envelope 分发 |
| `voglander-integration/.../gb28181/server/command/AbstractVoglanderServerCommand.java` | 1 | 新增 `dispatchEnvelope(...)` 工具方法 |

### 保留不变
- `voglander-integration/.../gb28181/start/ServerStart.java`（端口绑定唯一处）
- `voglander-integration/.../gb28181/client/command/**`（6 个，envelope 不覆盖此方向）
- `voglander-integration/.../gb28181/supplier/Voglander*DeviceSupplier.java`
- `voglander-integration/.../gb28181/config/properties/VoglanderSip*Properties.java`

---

## 附录 E：参考资料

- [sip-proxy / sip-gateway README](../../../sip-proxy/sip-gateway/README.md) — 接入面总览
- [sip-proxy / SIP-GATEWAY-AGGREGATION-PLAN.md](../../../sip-proxy/doc/plans/1.8.0/SIP-GATEWAY-AGGREGATION-PLAN.md) — 父聚合主纲领
- [sip-proxy / UNIFIED-ENVELOPE-PLAN.md](../../../sip-proxy/doc/plans/1.8.0/UNIFIED-ENVELOPE-PLAN.md) — envelope schema 与全量映射表
- [sip-proxy / GB28181-GATEWAY-MODULE-PLAN.md](../../../sip-proxy/doc/plans/1.8.0/GB28181-GATEWAY-MODULE-PLAN.md) — 代码迁移执行手册
- [voglander / CLAUDE.md](../../CLAUDE.md) §集成层 GB28181 — 业务方实施约束
- [voglander / README.md](../../README.md) §📡 GB28181 接入方案 — 业务方接入文档
