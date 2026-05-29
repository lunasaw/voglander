# Voglander × sip-gateway 1.8.0 接入方案

> **目标版本**：voglander `1.0.2-SNAPSHOT` → sip-gateway `1.8.0`
> **撰写日期**：2026-05-29
> **状态**：方案确定，待按 Stage 实施
> **关联文档**：[../../README.md](../../README.md) §📡 GB28181 接入方案、[sip-proxy/sip-gateway/README.md](../../../sip-proxy/sip-gateway/README.md)

---

## 〇、背景

`sip-proxy` 工程已在 `1.8.0` 版本完成父聚合重构，新增 `sip-gateway` 模块作为 GB28181 协议的统一接入门面：

- 父聚合：`sip-common` / `sip-gb28181` / `sip-gateway` / `sip-test`
- 接入面：`sip-gateway-spring-boot-starter`（一行依赖 + `@EnableSipProxy`）
- 协议中立内核：`gateway-core`（envelope/SPI/notifier/dispatch）
- 协议适配器：`gateway-gb28181`（39 个 cmdType + 35 个 emit 事件）

voglander 当前仍直连 `gb28181-client` / `gb28181-server` `1.2.5-SNAPSHOT`，通过 `ServerStart` 手工启栈、实现 33 个 `Voglander*Handler` 类。本方案描述如何切换到 `sip-gateway-spring-boot-starter`，把 33 个 handler 收敛到 1 个 `BusinessNotifier`。

---

## 一、现状 vs. 目标对比

| 维度 | 现状（旧）| 目标（新 sip-gateway 1.8.0）|
|---|---|---|
| **Maven 依赖** | `gb28181-client` + `gb28181-server`（voglander-integration）+ `gb28181-common`（voglander-manager），版本 `1.2.5-SNAPSHOT` | 顶层引入 `sip-gateway-bom`（`<scope>import</scope>`）+ 单一 `sip-gateway-spring-boot-starter:1.8.0` |
| **协议栈启动** | [ServerStart.java](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/start/ServerStart.java) 实现 `CommandLineRunner`，���工 `sipLayer.addListeningPoint()` | 启动类加 `@EnableSipProxy`（别名 `@EnableSipServer`），监听点由 starter 自动注册 |
| **设备供应** | [VoglanderServerDeviceSupplier](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/supplier/VoglanderServerDeviceSupplier.java) + [VoglanderClientDeviceSupplier](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/supplier/VoglanderClientDeviceSupplier.java)，框架按 SPI 回调 | 不变（保留），仍为业务 ↔ 框架的设备转换层 |
| **入站事件**（注册/心跳/Catalog 上报/Notify/Session 状态） | 33 个 Voglander*Handler 类各自处理一个 SIP 方法/响应，散落在 `client/request`、`client/response`、`server/request`、`server/response` 四个目录 | **全部收敛到一个** `BusinessNotifier`（继承 `AbstractProtocolBusinessNotifier`），按 `GatewayEvent.type()` 路由到业务 Manager；35 个 emit 类型由 starter 内的 `Gb28181EventForwarder` 自动产生 |
| **出站命令**（点播/PTZ/录像/订阅/广播） | 业务调用 `VoglanderServer*Command` / `VoglanderClient*Command`，再转发给 `ServerCommandSender` / `ClientCommandSender` | 业务发 envelope `GatewayCommand` 走 `POST /gateway/command`（或在进程内直接调 `CommandHandlerRegistry`），39 个 cmdType 由 `Gb28181CommandSpecs` 声明，复杂的 ~20 个 `@CommandMapping` 自动扫描 |
| **INVITE 上下文** | 进程内 `Dialog` 隐式持有 | 显式 `InviteContextStore`（callId → nodeId+ctxKey），单机用默认 `InMemoryInviteContextStore`，**多节点必须替换为 Redis 实现** |
| **配置前缀** | `sip.server.*` / `sip.client.*` / `sip.common.time-sync.*` | 新增 `gateway.node-id` / `gateway.nodes.*` / `gateway.gb28181.invite-context-ttl-ms`，保留 `sip.server.*`（多节点必填 `external-ip`） |
| **type 命名** | 隐式（按方法名）| 三段式 `protocol.Group.Name`，如 `gb28181.Query.Catalog` / `gb28181.Invite.Play` / `gb28181.Lifecycle.Online` |
| **错误码** | 无统一约定 | HTTP 400/404/410/502/503 各自语义清晰（410 禁止重试；502/503 短重试） |
| **多节点部署** | 单机 | 节点路由 + VIP 部署（`sip.server.external-ip` 必填），`InviteContextStore` Redis 化 |

---

## 二、模块级迁移映射

| 现状文件/目录 | 迁移动作 | 目标 |
|---|---|---|
| `voglander-integration/wrapper/gb28181/start/ServerStart.java` | **删除** | 由 `@EnableSipProxy` + starter 自动配置取代 |
| `voglander-integration/wrapper/gb28181/start/SipServerConfig.java` | **保留**（如仅做配置 binding 可保留，否则删）| — |
| `voglander-integration/wrapper/gb28181/config/properties/VoglanderSip*Properties.java` | **保留** | 业务侧自有属性，不与 `gateway.*` 冲突 |
| `voglander-integration/wrapper/gb28181/supplier/Voglander*DeviceSupplier.java` | **保留** | starter 通过 SPI 调用，不变 |
| `voglander-integration/wrapper/gb28181/server/request/**`（6 个 handler）| **删除**，逻辑迁入 `VoglanderBusinessNotifier#onProtocolEvent` 按 type 分发 | `gb28181.Lifecycle.Register/Online/Offline`、`gb28181.Notify.*`、`gb28181.Session.*` |
| `voglander-integration/wrapper/gb28181/server/response/**`（3 个 handler）| **删除** | `gb28181.Response.*` / `gb28181.Session.InviteOk/Failure` |
| `voglander-integration/wrapper/gb28181/client/request/**`（7 个 handler）| **删除** | 同上，Notifier 按 type 分发 |
| `voglander-integration/wrapper/gb28181/client/response/**`（5 个 handler）| **删除** | 同上 |
| `voglander-integration/wrapper/gb28181/server/command/**`（7 个 command）| **保留薄包装** 或 **改造为 envelope 调用方**：保留 `VoglanderServerPtzCommand` 类签名给业务上层用，内部改成 `dispatcher.dispatch(new GatewayCommand("gb28181.Control.Ptz", deviceId, payload, requestId))` | — |
| `voglander-integration/wrapper/gb28181/client/command/**`（7 个 command）| 同上 | — |
| `voglander-service/login/DeviceRegisterServiceImpl.java` | **保留** | 由 Notifier 在收到 `gb28181.Lifecycle.Register/Online/Offline` 时调用 |
| `voglander-service/command/DeviceAgreementService.java` | **保留**或简化 | 协议路由不变 |
| `voglander-manager/manager/DeviceManager / DeviceChannelManager / DeviceConfigManager` | **保留** | 业务层无感 |

**净变更**：
- ➖ 删除 21 个 handler 类（6+3+7+5）+ 1 个 `ServerStart`
- ➕ 新增 1 个 `VoglanderBusinessNotifier`
- 🔁 14 个 command 类内部改造为 envelope 薄分发器
- ✅ 业务 Manager / Service / DTO / Controller 全部不动

---

## 三、依赖迁移（精确）

### 3.1 主 `pom.xml`

```xml
<!-- 删除： -->
<gb28181-proxy.version>1.2.5-SNAPSHOT</gb28181-proxy.version>

<!-- 新增： -->
<properties>
    <sip-gateway.version>1.8.0</sip-gateway.version>
</properties>

<dependencyManagement>
    <dependencies>
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
<!-- 删除： -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-client</artifactId>
    <version>${gb28181-proxy.version}</version>
</dependency>
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-server</artifactId>
    <version>${gb28181-proxy.version}</version>
</dependency>

<!-- 替换为（version 由 BOM 管理）： -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>sip-gateway-spring-boot-starter</artifactId>
</dependency>
```

### 3.3 `voglander-manager/pom.xml`

```xml
<!-- 删除： -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-common</artifactId>
    <version>${gb28181-proxy.version}</version>
</dependency>
<!-- 协议数据模型由 starter 通过 gateway-gb28181 → gb28181-common 传递依赖带入，无需显式声明 -->
```

### 3.4 验证命令

```bash
mvn dependency:tree | grep -E "gb28181|sip-gateway|sip-common"
# 期望：仅看到 1.8.0 版本，无 1.2.5-SNAPSHOT 残留
```

---

## 四、配置迁移（`application-inte.yml`）

### 4.1 新增 `gateway` 段

```yaml
gateway:
  node-id: ${local.gateway.node-id:node-1}
  # 多节点部署时填写各节点 HTTP 地址；单节点可省略
  nodes:
    node-1: ${local.gateway.node1-url:}
    node-2: ${local.gateway.node2-url:}
  forward-timeout-ms: 3000
  gb28181:
    invite-context-ttl-ms: 30000        # INVITE 上下文 TTL，超时后回包返回 410
    invite-idempotency-window-ms: 5000  # UDP 重传幂等窗口
```

### 4.2 `sip` 段保留并补充 `external-ip`

```yaml
sip:
  enable: ${local.sip.enable}
  server:
    enabled: ${local.sip.server.enabled:false}
    ip: ${local.sip.server.ip:0.0.0.0}
    port: ${local.sip.server.port:5060}
    # 多节点 + VIP 部署必填，否则设备回包绕过 VIP 导致源 IP 哈希失效
    external-ip: ${local.sip.server.external-ip:}
    domain: ${local.sip.server.domain:34020000002000000001}
    serverId: ${local.sip.server.serverId:34020000002000000001}
    serverName: ${local.sip.server.serverName:GB28181-Server}
  client:
    enabled: ${local.sip.client.enabled:false}
    # ... 原有字段保留
```

### 4.3 删除项

- `sip.common.time-sync.*`：starter 已内置时钟校准，业务侧无需手工配置（如有特殊需求保留）

---

## 五、落地步骤

> 建议按 5 个 Stage 推进，每个 Stage 单独提交便于回滚。建议在 [IMPLEMENTATION_PLAN.md](../../IMPLEMENTATION_PLAN.md) 记录状态。

### Stage 1：依赖切换 & starter 接入（半天）

**Goal**：完成 BOM/starter 引入，启动类切换到 `@EnableSipProxy`。

**Tasks**：
1. 主 `pom.xml`：移除 `gb28181-proxy.version`，新增 `sip-gateway.version` 与 BOM 导入
2. `voglander-integration/pom.xml`：`gb28181-client` + `gb28181-server` → `sip-gateway-spring-boot-starter`
3. `voglander-manager/pom.xml`：移除 `gb28181-common`
4. 启动类 `VoglanderApplication` 添加 `@EnableSipProxy`
5. 删除 `ServerStart.java`
6. `application-inte.yml` 新增 `gateway.*` 段

**Success Criteria**：
- `mvn clean compile` 通过
- 启动后 `GET /gateway/whoami` 返回 `{"nodeId": "node-1"}`
- `POST /gateway/command` 通路打通（非法 type 返回 404 而非 connection refused）
- 设备可发起 REGISTER 但业务回调缺失（默认 NoopBusinessNotifier，仅日志）

### Stage 2：BusinessNotifier 实现（1-2 天）

**Goal**：把 21 个 `Voglander*Handler` 收敛到 `VoglanderBusinessNotifier`。

**Tasks**：
1. 新增 `VoglanderBusinessNotifier extends AbstractProtocolBusinessNotifier`
2. 新增 `sipNotifierExecutor` 线程池（`ThreadPoolConfig` 中配置，建议 core=8/max=32/queue=1000）
3. 按下表顺序逐 type 迁移业务（按依赖关系排序，每完成一类型可单独提测）：

| 顺序 | 事件 type | 现 handler | 目标 Manager 方法 |
|---|---|---|---|
| 1 | `gb28181.Lifecycle.Register` | `VoglanderServerRegisterRequestHandler` | `DeviceRegisterServiceImpl.onRegister(event)` |
| 2 | `gb28181.Lifecycle.Online` / `Offline` | （注册/心跳衍生）| `DeviceManager.markOnline/markOffline(deviceId)` |
| 3 | `gb28181.Notify.Keepalive` | `VoglanderServerNotifyRequestHandler#keepalive` | `DeviceManager.heartbeat(deviceId)` |
| 4 | `gb28181.Notify.Alarm` | `VoglanderServerNotifyRequestHandler#alarm` | `DeviceManager.onAlarm(event)` |
| 5 | `gb28181.Notify.MobilePosition` | 同上 | `DeviceManager.onPosition(event)` |
| 6 | `gb28181.Notify.MediaStatus` | 同上 | `MediaSessionManager.onMediaStatus(event)` |
| 7 | `gb28181.Response.Catalog` | `VoglanderClientMessageRequestHandler#catalog` | `DeviceChannelManager.onCatalog(event)` |
| 8 | `gb28181.Response.DeviceInfo` | 同上 | `DeviceManager.onDeviceInfo(event)` |
| 9 | `gb28181.Response.RecordInfo` | 同上 | `DeviceChannelManager.onRecordInfo(event)` |
| 10 | `gb28181.Response.PtzPosition` / `SdCardStatus` / `HomePosition` | 同上 | 对应 Manager |
| 11 | `gb28181.Session.InviteOk` | `VoglanderServerInviteResponseHandler` | `MediaSessionManager.onInviteOk(event)` |
| 12 | `gb28181.Session.InviteFailure` | 同上 | `MediaSessionManager.onInviteFailure(event)` |
| 13 | `gb28181.Session.Bye` / `Ack` | `VoglanderServerByeRequestHandler` / `VoglanderServerAckResponseHandler` | `MediaSessionManager.onBye/onAck(event)` |

4. 每迁完一类，删对应的 handler 类
5. 跑 `Gb28181ComprehensiveIntegrationTestSuite`

**Success Criteria**：
- 21 个 `Voglander*Handler` 全部删除
- `Gb28181ComprehensiveIntegrationTestSuite` 全绿
- SkyWalking 看 `sipNotifierExecutor` 队列长度 < 100、平均延迟 < 50ms

### Stage 3：出站命令切换到 envelope（1 天）

**Goal**：14 个 command 类内部改造为 envelope 薄分发器。

**Tasks**：
1. 注入 `CommandHandlerRegistry` 到 `AbstractVoglanderServerCommand` / `AbstractVoglanderClientCommand`
2. 改造模板（以 `VoglanderServerPtzCommand` 为例）：

   ```java
   public String ptz(String deviceId, int cmdCode, int hSpeed, int vSpeed, int zSpeed) {
       Map<String, Object> payload = Map.of(
           "cmdCode", cmdCode,
           "horizonSpeed", hSpeed,
           "verticalSpeed", vSpeed,
           "zoomSpeed", zSpeed);
       GatewayCommand cmd = new GatewayCommand(
           "gb28181.Control.Ptz", deviceId, payload, MDC.get("traceId"));
       return commandRegistry.handle(cmd).correlationId();
   }
   ```

3. 业务 Web Controller 不变，对外 API 保持兼容
4. 业务自定义命令（如 voglander 私有扩展）改用 `@CommandMapping("gb28181.Custom.Foo")`

**Success Criteria**：
- PTZ、Catalog 查询、Invite Play 三条主链路压测 QPS ≥ 旧版 90%
- `mvn dependency:tree` 中无 `ServerCommandSender` / `ClientCommandSender` 直接依赖（仅 starter 内部使用）

### Stage 4：INVITE 上下文 Redis 化（多节点准备，1 天）

**Goal**：替换默认 `InMemoryInviteContextStore` 为 Redis 实现，支撑多节点部署。

**Tasks**：
1. 新增 `RedisInviteContextStore implements InviteContextStore`：

   ```java
   @Component
   @ConditionalOnProperty(prefix = "gateway.gb28181.store", value = "type", havingValue = "redis")
   @RequiredArgsConstructor
   public class RedisInviteContextStore implements InviteContextStore {

       private final StringRedisTemplate redisTemplate;
       private static final String KEY_PREFIX = "sip:invite:ctx:";

       @Override
       public void save(String callId, InviteContext value, long ttlMs) {
           redisTemplate.opsForValue().set(KEY_PREFIX + callId,
               value.nodeId() + ":" + value.ctxKey(),
               Duration.ofMillis(ttlMs));
       }

       @Override
       public InviteContext find(String callId) {
           try {
               String value = redisTemplate.opsForValue().get(KEY_PREFIX + callId);
               if (value == null) return null;
               int sep = value.indexOf(':');
               return new InviteContext(value.substring(0, sep), value.substring(sep + 1));
           } catch (RedisConnectionFailureException e) {
               throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "redis unavailable", e);
           }
       }

       @Override
       public void remove(String callId) {
           redisTemplate.delete(KEY_PREFIX + callId);
       }
   }
   ```

2. `application-inte.yml` 新增：

   ```yaml
   gateway:
     gb28181:
       store:
         type: ${local.gateway.gb28181.store.type:memory}  # memory | redis
   ```

3. 部署双节点 + VIP，配置 `sip.server.external-ip` 与 `gateway.nodes`

**Success Criteria**：
- 双节点压测：跨节点 INVITE 回包成功率 > 99%
- Redis 故障注入：返回 503，业务侧重试 3 次后失败（符合错误码契约）

### Stage 5：清理与文档（半天）

**Goal**：清理过渡期残留，更新文档。

**Tasks**：
1. 删除：`ServerStart.java`、21 个 handler、过渡期 `@Deprecated` 标记的方法
2. 清理：`VoglanderSipClientProperties` / `VoglanderSipServerProperties` 中已不再使用的字段
3. 更新：[CLAUDE.md](../../CLAUDE.md) 集成层章节、本次更新过的 [README.md](../../README.md)、`config.ini`（如涉及）
4. 删除：`IMPLEMENTATION_PLAN.md`

**Success Criteria**：
- `grep -rn "Voglander.*Handler" voglander-integration/src/main/java` 无结果
- `grep -rn "ServerStart" voglander-integration/src/main/java` 无结果

---

## 六、风险与回滚

| 风险 | 缓解 | 回滚 |
|---|---|---|
| `BusinessNotifier#notify` 同步阻塞 → 设备超时重传 | 强制 `@Async("sipNotifierExecutor")`，独立线程池，加 SkyWalking 队列长度告警 | Stage 2 单独可回退（恢复对应 handler 类） |
| 多节点忘配 `sip.server.external-ip` → 回包 IP 哈希失效 | starter 启动期校验：缺失时单节点直通、多节点 fail-fast | 配置项回滚 |
| 旧依赖 `1.2.5-SNAPSHOT` 与 `1.8.0` 共存 → ClassLoader 冲突 | Stage 1 后必须跑 `mvn dependency:tree \| grep gb28181`，确认仅剩 1.8.0 | pom 回滚 |
| INVITE 上下文 Redis 故障 → 跨节点回包失败 | `RedisInviteContextStore` 异常抛 503，配合 502/503 短重试契约 | 切回 `gateway.gb28181.store.type=memory`（仅单节点可用） |
| envelope payload schema 与协议栈不匹配 → 400 错误 | 严格按 `Gb28181CommandSpecs.declare()` 约束 payload 字段名/类型，单测覆盖 | Stage 3 可分命令回退 |

**整体回滚策略**：每个 Stage 单独提交。Stage 1/2/3 可独立回退（保留旧 handler 类直到 Stage 5 才物理删除）。

---

## 七、关键约束（实施期必须遵守）

- ⚠️ **必须与 sip-proxy 同 JVM**：`SipTransactionRegistry`、`Dialog` 都是进程内对象，跨进程后无法回包。voglander 不得把 sip-gateway 拆成独立服务调用。
- ⚠️ **`BusinessNotifier#notify` 必须异步**：使用 `@Async` + 独立线程池。
- ⚠️ **多节点必须 Redis**：`InMemoryInviteContextStore` 仅单机演示用，生产环境必须切到 `RedisInviteContextStore`。
- ⚠️ **`gateway-core` 协议中立**：voglander 业务代码不得直接依赖 `gateway-gb28181` 内部类，仅通过 envelope 与 `BusinessNotifier` 交互。
- ⚠️ **设备供应器保留**：`VoglanderServerDeviceSupplier` / `VoglanderClientDeviceSupplier` 仍为业务 ↔ 协议栈的设备转换层，starter 通过 SPI 调用。

---

## 附录 A：cmdType 全表（39 个）

> 来源：`sip-gateway/gateway-gb28181/handler/Gb28181CommandSpecs.declare()` + `Gb28181WhitelistHandlers`

### Query（10 个）

| cmdType | 业务含义 | payload 关键字段 |
|---|---|---|
| `gb28181.Query.DeviceInfo` | 设备信息查询 | — |
| `gb28181.Query.DeviceStatus` | 设备状态查询 | — |
| `gb28181.Query.Catalog` | 设备目录查询 | — |
| `gb28181.Query.PresetQuery` | 预置位查询 | — |
| `gb28181.Query.MobilePosition` | 移动位置查询 | `interval` |
| `gb28181.Query.PtzPosition` | PTZ 位置查询 | — |
| `gb28181.Query.SdCardStatus` | SD 卡状态查询 | — |
| `gb28181.Query.HomePosition` | 看守位查询 | — |
| `gb28181.Query.CruiseTrackList` | 巡航轨迹列表查询 | — |
| `gb28181.Query.CruiseTrack` | 巡航轨迹查询 | `number:int` |
| `gb28181.Query.RecordInfo` ⭐ | 录像查询（白名单）| `startTime` / `endTime` / `type` |
| `gb28181.Query.AlarmQuery` ⭐ | 告警查询（白名单）| `startAlarmPriority` / `endAlarmPriority` / `alarmMethod` 等 |

### Subscribe（5 个）

| cmdType | 业务含义 | payload 关键字段 |
|---|---|---|
| `gb28181.Subscribe.Catalog` | 目录订阅 | `expires:int` / `eventType` |
| `gb28181.Subscribe.PtzPosition` | PTZ 位置订阅 | `expires:int` |
| `gb28181.Subscribe.Unsubscribe` | 取消订阅 | callId（envelope 顶层 `deviceId` 留空）|
| `gb28181.Subscribe.MobilePosition` ⭐ | 移动位置订阅（白名单） | `expires:int` / `interval:int` |
| `gb28181.Subscribe.Alarm` ⭐ | 告警订阅（白名单） | `expires:int` / 告警筛选字段 |
| `gb28181.Subscribe.Refresh` ⭐ | 订阅刷新（白名单） | callId |

### Control（15 个）

| cmdType | 业务含义 | payload 关键字段 |
|---|---|---|
| `gb28181.Control.Reboot` | 设备重启 | — |
| `gb28181.Control.Record` | 录像控制 | `recordCmd` |
| `gb28181.Control.Guard` | 布防/撤防 | `guardCmd` |
| `gb28181.Control.IFrame` | 强制 I 帧 | — |
| `gb28181.Control.HomePosition` | 看守位设置 | `enabled` / `resetTime` / `presetIndex` |
| `gb28181.Control.PtzPrecise` | 精准 PTZ | `pan:double` / `tilt:double` / `zoom:double` |
| `gb28181.Control.FormatSDCard` | 格式化 SD 卡 | `sdNumber:int` |
| `gb28181.Control.ScanSpeed` | 扫描速度 | `groupNumber:int` / `speed:int` |
| `gb28181.Control.DragZoomIn` | 拉框放大 | `dragZoom:DragZoom` |
| `gb28181.Control.DragZoomOut` | 拉框缩小 | `dragZoom:DragZoom` |
| `gb28181.Control.Ptz` ⭐ | 标准 PTZ（白名单）| `cmdCode` / `horizonSpeed` / `verticalSpeed` / `zoomSpeed` |
| `gb28181.Control.AlarmReset` ⭐ | 告警复位（白名单）| `alarmMethod` / `alarmType` |
| `gb28181.Control.FI` ⭐ | FI 控制（白名单）| `cmdCode` / `param1` / `param2` |
| `gb28181.Control.Preset` ⭐ | 预置位（白名单）| `cmdCode` / `presetId` |
| `gb28181.Control.Cruise` ⭐ | 巡航（白名单）| `cmdCode` / `cruiseId` |
| `gb28181.Control.CruiseSpeedOrTime` ⭐ | 巡航速度/驻留时间（白名单）| `cmdCode` / `cruiseId` / `value` |
| `gb28181.Control.Scan` ⭐ | 扫描（白名单）| `cmdCode` / `scanId` |
| `gb28181.Control.Auxiliary` ⭐ | 辅助开关（白名单）| `cmdCode` / `switchId` |
| `gb28181.Control.TargetTrack` ⭐ | 目标跟踪（白名单）| `cmdCode` / `targetId` |

### Config（3 个）

| cmdType | 业务含义 | payload 关键字段 |
|---|---|---|
| `gb28181.Config.BasicParam` | 基本参数配置 | `name` / `expiration` / `heartBeatInterval` / `heartBeatCount` |
| `gb28181.Config.Osd` | OSD 配置 | `osdInfo:OsdConfig$OsdInfo` |
| `gb28181.Config.ConfigDownload` | 配置下载 | `configType` |

### Invite / Session（5 个）

| cmdType | 业务含义 | payload 关键字段 |
|---|---|---|
| `gb28181.Invite.Bye` | 终止会话 | callId（envelope 顶层 `deviceId` 留空）|
| `gb28181.Invite.Play` ⭐ | 实时点播（白名单）| `ssrc` / `mediaServer` / `mediaPort` |
| `gb28181.Invite.Playback` ⭐ | 录像回放（白名单）| `startTime` / `endTime` / `ssrc` |
| `gb28181.Invite.Talk` ⭐ | 语音对讲（白名单）| `ssrc` / `mediaServer` |
| `gb28181.Invite.Download` ⭐ | 录像下载（白名单）| `startTime` / `endTime` / `downloadSpeed` |
| `gb28181.Invite.PlaybackControl` ⭐ | 回放控制（白名单）| callId / `controlCmd` / `scale` |
| `gb28181.Invite.Ack` ⭐ | INVITE ACK（白名单）| callId / `sdp` |

### Device（3 个）

| cmdType | 业务含义 | payload 关键字段 |
|---|---|---|
| `gb28181.Device.Upgrade` | 设备升级 | `firmware` / `fileURL` / `manufacturer` / `sessionId` |
| `gb28181.Device.SnapShot` | 抓拍 | `snapNum:int` / `interval:int` / `uploadURL` / `sessionId` |
| `gb28181.Device.Broadcast` | 语音广播 | — |

> ⭐ 标注 = `@CommandMapping` 白名单方法（payload 字段较多/有重载）

---

## 附录 B：事件 type 全表（35 个）

> 来源：`sip-gateway/gateway-gb28181/forwarder/Gb28181EventForwarder`

### Lifecycle（5 个）

| 事件 type | 触发场景 | 关键 payload |
|---|---|---|
| `gb28181.Lifecycle.Register` | 设备发起 REGISTER | `expires` / `userAgent` / `remoteIp` / `remotePort` |
| `gb28181.Lifecycle.RegisterChallenge` | 401/407 鉴权挑战 | `realm` / `nonce` |
| `gb28181.Lifecycle.Online` | 注册成功转上线 | `registeredAt` |
| `gb28181.Lifecycle.Offline` | 心跳超时/主动注销 | `reason` |
| `gb28181.Lifecycle.RemoteAddressChanged` | 设备 IP/端口变化 | `oldRemoteIp` / `newRemoteIp` |

### Notify（7 个）

| 事件 type | 触发场景 | 关键 payload |
|---|---|---|
| `gb28181.Notify.Alarm` | 设备告警上报 | `alarmPriority` / `alarmMethod` / `alarmTime` / `alarmType` |
| `gb28181.Notify.Keepalive` | 设备心跳 | `sn` / `status` |
| `gb28181.Notify.MediaStatus` | 媒体状态变化（推拉流断开等）| `notifyType` / `info` |
| `gb28181.Notify.MobilePosition` | 移动位置上报 | `time` / `longitude` / `latitude` / `speed` |
| `gb28181.Notify.UpgradeResult` | 升级结果 | `result` / `errorCode` |
| `gb28181.Notify.SnapShotFinished` | 抓拍完成 | `sessionId` / `snapNum` / `successCount` |
| `gb28181.Notify.VideoUpload` | 视频上传 | `sessionId` / `fileName` / `progress` |

### Session（7 个）

| 事件 type | 触发场景 | 关键 payload |
|---|---|---|
| `gb28181.Session.InviteTrying` | 100 Trying | callId |
| `gb28181.Session.InviteOk` | 200 OK + SDP | callId / `sdp` |
| `gb28181.Session.InviteFailure` | 4xx/5xx/6xx | callId / `statusCode` / `reason` |
| `gb28181.Session.Ack` | ACK | callId |
| `gb28181.Session.Bye` | 主动 BYE | callId / `reason` |
| `gb28181.Session.ByeError` | BYE 失败 | callId / `errorCode` |
| `gb28181.Session.ServerInvite` | 设备主动 INVITE（语音对讲反向）| callId / `sdp` |

### Response（16 个）

| 事件 type | 对应查询命令 | 关键 payload |
|---|---|---|
| `gb28181.Response.Catalog` | `gb28181.Query.Catalog` | `deviceList[]`（含 channelId、name、status 等）|
| `gb28181.Response.DeviceInfo` | `gb28181.Query.DeviceInfo` | `manufacturer` / `model` / `firmware` |
| `gb28181.Response.DeviceInfoError` | 同上（异常）| `errorCode` / `errorMsg` |
| `gb28181.Response.DeviceInfoRequest` | 设备主动询问 | — |
| `gb28181.Response.DeviceStatus` | `gb28181.Query.DeviceStatus` | `online` / `status` / `encode` / `record` |
| `gb28181.Response.RecordInfo` | `gb28181.Query.RecordInfo` | `recordList[]` |
| `gb28181.Response.PtzPosition` | `gb28181.Query.PtzPosition` | `pan` / `tilt` / `zoom` |
| `gb28181.Response.SdCardStatus` | `gb28181.Query.SdCardStatus` | `sdCards[]` |
| `gb28181.Response.HomePosition` | `gb28181.Query.HomePosition` | `enabled` / `resetTime` / `presetIndex` |
| `gb28181.Response.CruiseTrackList` | `gb28181.Query.CruiseTrackList` | `tracks[]` |
| `gb28181.Response.CruiseTrack` | `gb28181.Query.CruiseTrack` | `points[]` |
| `gb28181.Response.Config` | `gb28181.Config.BasicParam` 等 | `result` |
| `gb28181.Response.ConfigDownload` | `gb28181.Config.ConfigDownload` | `configBody` |
| `gb28181.Response.PresetQuery` | `gb28181.Query.PresetQuery` | `presets[]` |
| `gb28181.Response.Subscribe` | `gb28181.Subscribe.*` | `result` / `expires` |
| `gb28181.Response.NotifyUpdate` | 周期推送 | 因 type 而异 |

---

## 附录 C：HTTP 错误码契约

| HTTP | 场景 | 业务侧动作 |
|---|---|---|
| 400 | payload 字段缺失/类型错误 | 修正请求 |
| 404 | type 不存在 | 修正 type 字符串 |
| 410 | 事务已终止/超时（INVITE/订阅）| **禁止重试**，重新发起原始命令 |
| 502 | 跨节点路由 nodeAddressMap 暂未刷新 | 200ms × 3 短重试 |
| 503 | 转发失败 / store 后端不可达 | 短重试 |

---

## 附录 D：关键文件清单

### 新建（Stage 2-4）

| 路径 | 用途 |
|---|---|
| `voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/notifier/VoglanderBusinessNotifier.java` | 统一事件回调 |
| `voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/store/RedisInviteContextStore.java` | 多节点 INVITE 上下文 |
| `voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/manager/MediaSessionManager.java` | 媒体会话管理（如尚未拆分）|
| `voglander-test/src/test/java/io/github/lunasaw/voglander/notifier/VoglanderBusinessNotifierTest.java` | Notifier 单元测试 |

### 删除（Stage 1 / 5）

| 路径 | Stage |
|---|---|
| `voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/start/ServerStart.java` | Stage 1 |
| `voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/server/request/**`（6 个）| Stage 2-5 |
| `voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/server/response/**`（3 个）| Stage 2-5 |
| `voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/client/request/**`（7 个）| Stage 2-5 |
| `voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/client/response/**`（5 个）| Stage 2-5 |

### 改造（Stage 3）

| 路径 | 改造内容 |
|---|---|
| `voglander-integration/.../wrapper/gb28181/server/command/**`（7 个）| 内部改 envelope 调用 |
| `voglander-integration/.../wrapper/gb28181/client/command/**`（7 个）| 内部改 envelope 调用 |

### 保留不变

| 路径 | 说明 |
|---|---|
| `voglander-integration/.../wrapper/gb28181/supplier/Voglander*DeviceSupplier.java` | starter 通过 SPI 调用 |
| `voglander-integration/.../wrapper/gb28181/config/properties/VoglanderSip*Properties.java` | 业务自有属性 |
| `voglander-service/login/DeviceRegisterServiceImpl.java` | 业务逻辑 |
| `voglander-service/command/DeviceAgreementService.java` | 协议路由 |
| `voglander-manager/manager/Device*Manager.java` | 业务管理器 |
| `voglander-web/api/device/DeviceController.java` 等 | Web 层 |

---

## 附录 E：参考资料

- [sip-proxy / sip-gateway README](../../../sip-proxy/sip-gateway/README.md) — 接入面总览
- [sip-proxy / SIP-GATEWAY-AGGREGATION-PLAN.md](../../../sip-proxy/doc/plans/1.8.0/SIP-GATEWAY-AGGREGATION-PLAN.md) — 父聚合主纲领
- [sip-proxy / UNIFIED-ENVELOPE-PLAN.md](../../../sip-proxy/doc/plans/1.8.0/UNIFIED-ENVELOPE-PLAN.md) — envelope schema 与全量映射表
- [sip-proxy / GB28181-GATEWAY-MODULE-PLAN.md](../../../sip-proxy/doc/plans/1.8.0/GB28181-GATEWAY-MODULE-PLAN.md) — 代码迁移执行手册
- [sip-proxy / LAYERED-ARCHITECTURE.md](../../../sip-proxy/doc/architecture/LAYERED-ARCHITECTURE.md) — 整体分层架构
- [sip-proxy / HORIZONTAL-SCALING.md](../../../sip-proxy/doc/architecture/HORIZONTAL-SCALING.md) — 多节点部署与 VIP 拓扑
- [voglander / README.md](../../README.md) §📡 GB28181 接入方案 — 业务方接入文档
