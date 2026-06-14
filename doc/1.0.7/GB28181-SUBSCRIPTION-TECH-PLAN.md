# GB28181-2022 设备订阅技术方案（目录 / 位置 / 告警）

> 版本：1.0.7 ｜ 拟定日期：2026-06-14 ｜ 分支：`0608_dev`
> 代码基线：sip-gateway / gb28181 **1.8.2**、zlm **1.0.11**
> 关联文档：`doc/1.0.7/ARCHITECTURE.md`（§7 GB28181 集成）、`GB28181-DEVICE-MANAGEMENT-TECH-PLAN.md`、`PROTOCOL-ARCHITECTURE-GENERICITY-PLAN.md`
> 协议依据：GB/T 28181-2022 §9.11（订阅与通知）、附录 A.2.4（订阅请求/通知 XML）、附录 A.3（MANSCDP 协议）

---

## 目录

1. [需求与范围](#1-需求与范围)
2. [协议背景（GB28181-2022 §9.11）](#2-协议背景gb28181-2022-911)
3. [现状核验与缺口](#3-现状核验与缺口)
4. [总体设计](#4-总体设计)
5. [数据模型](#5-数据模型)
6. [后端实现](#6-后端实现)
   - 6.1 出站命令层（枚举 + 封装）
   - 6.2 订阅状态持久化（Manager + 实体）
   - 6.3 订阅编排服务（Service）
   - 6.4 注册即订阅钩子
   - 6.5 入站通知处理（落地 NotifyUpdate / 位置 / 告警）
   - 6.6 订阅续订与过期清理（定时任务）
   - 6.7 Web 层接口
   - 6.8 lab 模拟设备订阅应答
7. [前端实现（设备列表 3 个开关）](#7-前端实现设备列表-3-个开关)
8. [配置项](#8-配置项)
9. [时序图](#9-时序图)
10. [测试方案](#10-测试方案)
11. [实施阶段与验收](#11-实施阶段与验收)
12. [风险与协议合规](#12-风险与协议合规)

---

## 1. 需求与范围

### 1.1 用户需求（原话拆解）

1. 在**设备列表**为每台设备提供三个订阅开关：**位置订阅 / 目录订阅 / 告警订阅**；
2. 完整且标准地支持三类订阅的 **SUBSCRIBE/NOTIFY** 全链路（server 发起订阅，设备变更时主动推送）；
3. 服务端发起订阅后，**设备如有��更应主动向服务端推送信息**（位置变化、目录变化、告警发生）；
4. **每次设备注册进来时，都要按当前订阅配置重新向设备发起订阅**（订阅状态随注册重建）；
5. **前端仅需在列表增加三个开关**（不做独立详情页/弹窗，开关即下发/撤销订阅）。

### 1.2 范围边界

| 纳入 | 不纳入 |
|------|--------|
| 目录订阅（Catalog）SUBSCRIBE + 目录变更 NOTIFY 落库 | PTZ 位置精确订阅（PtzPosition，框架支持但本期不接前端开关） |
| 移动位置订阅（MobilePosition）SUBSCRIBE + 位置 NOTIFY 落库 | 级联上级平台向本平台订阅（本平台作为下级被订阅） |
| 告警订阅（Alarm）SUBSCRIBE + 告警 NOTIFY 落库 | 订阅审计报表 / 历史轨迹回放页面 |
| 注册即重订阅、订阅续订（refresh）、撤销订阅（unsubscribe） | 多协议（ONVIF/RTSP）订阅（架构预留，不实现） |
| 设备列表 3 个开关 + 开关状态回显 | 订阅参数（expires/interval/告警过滤）的前端可配置（用后端默认值） |

---

## 2. 协议背景（GB28181-2022 §9.11）

### 2.1 订阅模型

GB28181 订阅基于 SIP `SUBSCRIBE`/`NOTIFY`（RFC 6665）+ MANSCDP（附录 A.3）XML 体。平台（SIP UAC，本系统 server 端）向设备（SIP UAS）发起 `SUBSCRIBE`，设备 `200 OK` 应答并建立订阅对话（dialog），之后设备在状态变更时通过该对话主动发送通知。

> **本框架的传输实现**：sip-gateway 1.8.2 的目录变更通知走 **SIP MESSAGE** 通道承载 `<Notify>` 体（框架注释明确：「GB28181-2022 §9.11.4 同时允许 SIP NOTIFY 方法，但本项目走 MESSAGE 通道」）。位置/告警通知同样以 MESSAGE 承载。**这是框架既定实现，非本方案臆造**，我方按框架事件类型对接即可。

### 2.2 三类订阅的协议要点

| 订阅类型 | Event 头 | SUBSCRIBE 体 CmdType | 通知体 CmdType | 关键参数 |
|----------|----------|----------------------|----------------|----------|
| 目录订阅 | `Catalog` | `Catalog` | `Catalog`（`<Notify>` 根，含 `Event`=ON/OFF/ADD/DEL/UPDATE 项） | `expires`（订阅有效期秒） |
| 位置订阅 | `presence.mobile`（MobilePosition） | `MobilePosition` | `MobilePosition`（经纬度/速度/方向/时间） | `interval`（上报间隔秒）、`expires` |
| 告警订阅 | `presence.alarm`（Alarm） | `Alarm` | `Alarm`（告警级别/方式/类型/时间/描述） | `expires`、可选告警过滤（优先级/方式/类型/时间段） |

### 2.3 订阅生命周期

```
平台 ──SUBSCRIBE(expires=3600)──▶ 设备
平台 ◀────── 200 OK ──────────── 设备     （建立 dialog，平台记录 callId）
平台 ◀── NOTIFY/MESSAGE(变更) ─── 设备     （设备主动推送，可多次）
平台 ──SUBSCRIBE(expires=3600,刷新)─▶ 设备  （过期前 refresh，复用 dialog/callId）
平台 ──SUBSCRIBE(expires=0)──────▶ 设备     （撤销订阅 unsubscribe）
```

订阅会在 `expires` 后自动失效；平台须在过期前 **refresh** 维持订阅；设备重启/重新注册后旧 dialog 失效，**须重新发起 SUBSCRIBE**（对应需求 4）。

---

## 3. 现状核验与缺口

> 以下均为对 `0608_dev` 源码与 1.8.2 框架 jar 的逐项核验结果（见 memory `gb28181-subscription-framework-facts`）。
> **2026-06-14 复核**：5 个订阅 envelope 键已用反编译字节码（`gateway-gb28181-1.8.2.jar` 的 `Gb28181CommandSpecs` / `Gb28181WhitelistHandlers`）逐字坐实，且 handler 实际读取的 payload 键与本方案下发键完全一致（见下表「已核验」列）。

### 3.1 框架能力（已��备，无需改框架）

**出站 SUBSCRIBE 命令**（`ServerCommandSender`，返回 `String`=callId，同步）：

| 框架命令 type | 注册处 | 方法签名 | handler 读取的 payload keys（字节码核验） |
|---------------|--------|----------|--------------|
| `gb28181.Subscribe.Catalog` | `Gb28181CommandSpecs`（静态表） | `deviceCatalogSubscribe(deviceId, expires:Integer, eventType)` | `deviceId`,`expires`,`eventType` ✅ |
| `gb28181.Subscribe.MobilePosition` | `Gb28181WhitelistHandlers`（`@CommandMapping("gb28181.Subscribe.MobilePosition")`） | `deviceMobilePositionSubscribe(deviceId, interval:String, expires:Integer, eventType, eventId)` | `interval`,`expires`,`eventType`,`eventId?` ✅ |
| `gb28181.Subscribe.Alarm` | `Gb28181WhitelistHandlers`（`@CommandMapping("gb28181.Subscribe.Alarm")`） | `deviceAlarmSubscribe(deviceId, expires:Integer, eventType, startPriority, endPriority, alarmMethod, alarmType, startAlarmTime, endAlarmTime)` | `expires`,`eventType`,`startPriority?`,`endPriority?`,`alarmMethod?`,`alarmType?`,`startAlarmTime?`,`endAlarmTime?` ✅ |
| `gb28181.Subscribe.Refresh` | `Gb28181WhitelistHandlers`（`@CommandMapping("gb28181.Subscribe.Refresh")`） | `refreshSubscribe(callId, [content,] expires:int)` | `callId`,`expires`,`content?` ✅ |
| `gb28181.Subscribe.Unsubscribe` | `Gb28181CommandSpecs`（静态表） | `unsubscribe(callId)` | `callId` ✅ |

> **核验补记（避免重蹈覆辙）**：这 5 个键是 **envelope 层**（`CommandHandlerRegistry.require(type)` 路由）的注册键，与 SIP 层的 `CmdTypeEnum`（`"Catalog"`/`"MobilePosition"`…，无前缀）是两套东西。核验框架订阅能力时**必须看 envelope 层的 `@CommandMapping` / `Gb28181CommandSpecs`**，只看 `CmdTypeEnum` 会误判为「框架无此键」。`gb28181.Subscribe.PtzPosition` 也在 `CommandSpecs` 中注册（本期不接前端，预留）。

**入站通知事件**（`Gb28181EventForwarder.class` 常量池核验）：

| 设备推送 | 框架 GatewayEvent type | voglander 当前处理 |
|----------|------------------------|--------------------|
| 告警 | `gb28181.Notify.Alarm` | ✅ `handleAlarm` → `AlarmManager.add` 落 `tb_alarm` + SSE |
| 移动位�� | `gb28181.Notify.MobilePosition` | ⚠️ `handleMobilePosition` 仅 log + SSE，**未落库** |
| 目录变更 | `gb28181.Response.NotifyUpdate`（载 `DeviceOtherUpdateNotify`） | ❌ 仅 `log.info`，**未处理** |
| 目录全量应答 | `gb28181.Response.Catalog`（载 `DeviceResponse`） | ✅ `handleCatalog` → `batchUpsertWithStatus` |
| SUBSCRIBE 200OK | `gb28181.Response.Subscribe` | ⚠️ 仅 log（callId 已从 send 返回拿到，此事件非必需） |

**客户端（设备侧，供 lab 模拟）**：`SubscribeListener`（onCatalogSubscribe/onMobilePositionSubscribe/onAlarmSubscribe）+ `ClientCommandSender.sendCatalogChangeNotify`/`sendMobilePositionNotify`/`sendAlarmNotify`。

### 3.2 voglander 缺口（本方案要补的）

| # | 缺口 | 位置 |
|---|------|------|
| G1 | `Gb28181CommandType` 19 项无任何 Subscribe 枚举 | `voglander-integration/.../gb28181/command/Gb28181CommandType.java` |
| G2 | 无出站订阅命令封装类 | `server/command/` 缺 `subscribe/` 包 |
| G3 | 无订阅状态持久化（哪些设备开了哪类订阅、callId、过期时间） | 缺 `tb_device_subscription` 表 + Manager + 实体 |
| G4 | 无订阅编排服务（开/关/续订/重订阅） | 缺 `DeviceSubscriptionService` |
| G5 | 注册流 `login()` 未触发重订阅 | `DeviceRegisterServiceImpl.login()` L64 后 |
| G6 | 目录变更 `Response.NotifyUpdate` 未落地；位置未落库 | `Gb28181ProtocolHandler` |
| G7 | 无续订/过期定时任务 | 缺 `SubscriptionRefreshScheduler` |
| G8 | 无订阅开关 Web 接口 | 缺 `DeviceSubscriptionController` |
| G9 | 设备列表 VO 无订阅开关状态字段 | `DeviceVO` + 列表查询 |
| G10 | lab 设备不应答 SUBSCRIBE、不推 NOTIFY | `lab/` 缺 `LabSubscribeListener` + 推送调度 |
| G11 | 前端设备列表无三个开关 | `views/device/list.vue` + `data.ts` + `api/device.ts` |

`DeviceConstant` 已有 `SUBSCRIBE_CYCLE_FOR_CATALOG`/`_MOBILE_POSITION`/`_ALARM` + `MOBILE_POSITION_SUBMISSION_INTERVAL` 常量（当前未被引用），本方案复用为默认值键。

---

## 4. 总体设计

### 4.1 架构分层落点

```
前端设备列表 (3 个 CellSwitch)
   │ PUT /api/v1/device/subscription/toggle  {deviceId, type, enabled}
   ▼  ── Web ──
DeviceSubscriptionController
   ▼  ── Service（编排，新增） ──
DeviceSubscriptionService
   ├─ enable(deviceId, type)   → 持久化意图 + 若设备在线则即刻 SUBSCRIBE
   ├─ disable(deviceId, type)  → unsubscribe + 持久化关闭
   ├─ resubscribeOnRegister(deviceId)  ◀── 注册钩子调用
   └─ refreshExpiring()        ◀── 定时任务调用
   │
   ├──▶ DeviceSubscriptionManager（状态持久化，新增） → tb_device_subscription
   └──▶ VoglanderServerSubscribeCommand（出站封装，新增）
            ▼ dispatchEnvelopeWithCallId(Gb28181CommandType.SUBSCRIBE_*, deviceId, payload)
         框架 CommandHandlerRegistry → ServerCommandSender.deviceXxxSubscribe → SIP SUBSCRIBE
   ═══════════════════════════════════════════════════════════════════
   设备变更主动推送（入站，已有管线 §7.2 ARCHITECTURE）
         sip-gateway → VoglanderBusinessNotifier → ShardDispatcher → Gb28181ProtocolHandler
            ├─ Notify.Alarm          → handleAlarm（已有，落 tb_alarm）
            ├─ Notify.MobilePosition → handleMobilePosition（补落库 tb_device_position）
            └─ Response.NotifyUpdate → handleCatalogNotifyUpdate（新增，更新通道状态）
```

### 4.2 设计原则

1. **不改框架**：框架已注册全部 5 个订阅命令，voglander 侧只加枚举 + 封装 + 编排（与 §8 SPI 表驱动对称化精神一致——加新实现，不改老路由）。
2. **意图与运行态分离**：`tb_device_subscription` 同时存「是否开启（意图，用户开关）」与「运行态（callId / 过期时间 / 状态）」。开关 = 改意图；意图驱动 SUBSCRIBE 下发。
3. **离线容忍**：设备离线时开订阅 → 仅持久化意图（status=PENDING），等下次注册由钩子补发 SUBSCRIBE。
4. **callId 同步回填**：SUBSCRIBE 经 `dispatchEnvelopeWithCallId` 同步拿 callId 存库（复刻 INVITE callId 回填模式，见 ARCHITECTURE §7.4），供 refresh/unsubscribe 复用 dialog。
5. **注册即重订阅**（需求 4）：每次 `login()` 成功后，查该设备所有「意图开启」的订阅，逐类重新 SUBSCRIBE 并刷新 callId（旧 dialog 已随设备重启失效）。
6. **协议合规**（CLAUDE.md 铁律）：所有字段/事件类型严格对齐框架既定 type 与 GB28181-2022；lab 模拟的非标简化显式标注、单点收口。

---

## 5. 数据模型

### 5.1 新增表 `tb_device_subscription`

每台设备 × 每类订阅一行（`device_id + sub_type` 唯一）。

```sql
-- ----------------------------
-- Table structure for tb_device_subscription  (GB28181-2022 订阅状态)
-- ----------------------------
DROP TABLE IF EXISTS tb_device_subscription;
CREATE TABLE tb_device_subscription
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    device_id   VARCHAR(64) NOT NULL,
    sub_type    VARCHAR(32) NOT NULL,                 -- CATALOG / MOBILE_POSITION / ALARM
    enabled     INTEGER     DEFAULT 0  NOT NULL,        -- 意图：1=开启 0=关闭（前端开关）
    status      INTEGER     DEFAULT 0  NOT NULL,        -- 运行态：0=INACTIVE 1=ACTIVE 2=PENDING 3=FAILED
    call_id     VARCHAR(255) DEFAULT NULL,             -- SUBSCRIBE dialog callId（refresh/unsubscribe 用）
    expires     INTEGER     DEFAULT NULL,               -- 订阅有效期(秒)
    interval_sec INTEGER    DEFAULT NULL,               -- 位置上报间隔(秒)，仅 MOBILE_POSITION
    expire_time DATETIME    DEFAULT NULL,               -- 本次订阅过期时间(=最后下发时间+expires)，refresh 判定用
    last_notify_time DATETIME DEFAULT NULL,             -- 最近一次收到该类通知的时间
    extend      TEXT                                    -- 告警过滤等扩展(FastJSON2)
);
CREATE UNIQUE INDEX uk_device_subscription ON tb_device_subscription (device_id, sub_type);
CREATE INDEX idx_device_subscription_expire ON tb_device_subscription (status, expire_time);
```

> 同步加入 `sql/voglander.sql`（MySQL，列类型对应 `BIGINT`/`TINYINT`/`VARCHAR`/`DATETIME`）、`sql/voglander-sqlite.sql` 与测试 `schema-sqlite.sql`。

### 5.2 新增表 `tb_device_position`（位置落库）

位置订阅会高频推送（默认 5s/次），单独建表存最新位置 + 可选轨迹；设备最新位置冗余进 `tb_device.extend` 便于列表展示。

```sql
DROP TABLE IF EXISTS tb_device_position;
CREATE TABLE tb_device_position
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    device_id   VARCHAR(64) NOT NULL,
    channel_id  VARCHAR(64) DEFAULT NULL,
    longitude   VARCHAR(32) DEFAULT NULL,
    latitude    VARCHAR(32) DEFAULT NULL,
    speed       VARCHAR(32) DEFAULT NULL,
    direction   VARCHAR(32) DEFAULT NULL,
    altitude    VARCHAR(32) DEFAULT NULL,
    position_time DATETIME  DEFAULT NULL,               -- 设备上报的定位时间
    extend      TEXT
);
CREATE INDEX idx_device_position_device ON tb_device_position (device_id, position_time);
```

> 告警已有 `tb_alarm`（`handleAlarm` 已落库），目录变更复用 `tb_device_channel`（更新通道 status），均无需新表。
> **类型说明**：`MobilePositionNotify` 的经纬度/速度/方向/海拔在框架实体中为 `Double`（已字节码核验），存入上表 `VARCHAR` 列时统一 `String.valueOf` 转换；如后续需地理计算，可改 `DECIMAL`/`DOUBLE` 列类型。

### 5.3 枚举与常量

`voglander-common`：

```java
// constant/device/SubscriptionConstant.java（新增）
public interface SubscriptionConstant {
    /** 订阅类型 */
    enum Type { CATALOG, MOBILE_POSITION, ALARM }
    /** 运行态 */
    interface Status { int INACTIVE = 0, ACTIVE = 1, PENDING = 2, FAILED = 3; }
    /** 默认有效期(秒)：目录/告警 3600，位置 3600 */
    int DEFAULT_EXPIRES = 3600;
    /** 位置默认上报间隔(秒) */
    int DEFAULT_POSITION_INTERVAL = 5;
    /** 续订提前量(秒)：过期前 N 秒 refresh */
    int REFRESH_AHEAD_SECONDS = 120;
    /** GB28181 Event 头取值 */
    String EVENT_CATALOG = "Catalog";
    String EVENT_MOBILE_POSITION = "presence.mobile";
    String EVENT_ALARM = "presence.alarm";
}
```

---

## 6. 后端实现

### 6.1 出站命令层（G1 + G2）

**G1** `Gb28181CommandType` 新增 5 项（Subscribe 分组）：

```java
// 新增分组 Subscribe (5)
SUBSCRIBE_CATALOG("Subscribe.Catalog"),
SUBSCRIBE_MOBILE_POSITION("Subscribe.MobilePosition"),
SUBSCRIBE_ALARM("Subscribe.Alarm"),
SUBSCRIBE_REFRESH("Subscribe.Refresh"),
SUBSCRIBE_UNSUBSCRIBE("Subscribe.Unsubscribe"),
```

> `type()` 拼为 `gb28181.Subscribe.Catalog` 等，与框架注册键逐字一致（已核验）。**同步更新冻结测试** `Gb28181CommandTypeTest`：19 → 24，逐字断言 5 个新 type。

**G2** 新增 `server/command/subscribe/VoglanderServerSubscribeCommand.java`，继承 `AbstractVoglanderServerCommand`，全部走 `dispatchEnvelopeWithCallId` 取 callId：

```java
@Slf4j
@Component
public class VoglanderServerSubscribeCommand extends AbstractVoglanderServerCommand {

    /** 目录订阅：返回 callId */
    public ResultDTO<String> subscribeCatalog(String deviceId, int expires) {
        validateDeviceId(deviceId, "目录订阅设备ID不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("expires", expires);
        payload.put("eventType", SubscriptionConstant.EVENT_CATALOG);
        return dispatchEnvelopeWithCallId(Gb28181CommandType.SUBSCRIBE_CATALOG.type(), deviceId, payload);
    }

    /** 移动位置订阅 */
    public ResultDTO<String> subscribeMobilePosition(String deviceId, int interval, int expires) {
        validateDeviceId(deviceId, "位置订阅设备ID不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("interval", String.valueOf(interval));
        payload.put("expires", expires);
        payload.put("eventType", SubscriptionConstant.EVENT_MOBILE_POSITION);
        return dispatchEnvelopeWithCallId(Gb28181CommandType.SUBSCRIBE_MOBILE_POSITION.type(), deviceId, payload);
    }

    /** 告警订阅（本期不带过滤，全量订阅） */
    public ResultDTO<String> subscribeAlarm(String deviceId, int expires) {
        validateDeviceId(deviceId, "告警订阅设备ID不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("expires", expires);
        payload.put("eventType", SubscriptionConstant.EVENT_ALARM);
        return dispatchEnvelopeWithCallId(Gb28181CommandType.SUBSCRIBE_ALARM.type(), deviceId, payload);
    }

    /** 续订（复用 dialog callId） */
    public ResultDTO<String> refresh(String callId, int expires) {
        validateNotNull(callId, "续订callId不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("callId", callId);
        payload.put("expires", expires);
        return dispatchEnvelopeWithCallId(Gb28181CommandType.SUBSCRIBE_REFRESH.type(), callId, payload);
    }

    /** 撤销订阅 */
    public ResultDTO<Void> unsubscribe(String callId) {
        validateNotNull(callId, "撤销订阅callId不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("callId", callId);
        return dispatchEnvelope(Gb28181CommandType.SUBSCRIBE_UNSUBSCRIBE.type(), callId, payload);
    }
}
```

> 注意 refresh/unsubscribe 的「deviceId」实参传 callId 仅用于亲和路由 key 与 trace（这两个命令按 dialog 寻址，框架内部以 callId 找 dialog），不影响下发。

### 6.2 订阅状态持久化（G3）

遵循 Manager 模板方法（CLAUDE.md）：

- 实体 `voglander-repository/.../entity/DeviceSubscriptionDO.java` + `DeviceSubscriptionMapper`（仅 `BaseMapper`，无自定义 SQL）；
- DTO `voglander-manager/.../domaon/dto/DeviceSubscriptionDTO.java`（`LocalDateTime`，含 `expireTimeToEpochMilli()` 等领域方法）；
- `DeviceSubscriptionManager` 标准模板方法 + 业务方法：

```java
@Component
public class DeviceSubscriptionManager {
    // 模板方法 add/updateById/get/deleteOne/getPage + clearCache（省略）

    /** 按 device+type 取（UNIQUE） */
    public DeviceSubscriptionDTO getByDeviceAndType(String deviceId, SubscriptionConstant.Type type) { ... }

    /** 该设备所有订阅行 */
    public List<DeviceSubscriptionDTO> listByDevice(String deviceId) { ... }

    /** 意图开启的订阅（重订阅用） */
    public List<DeviceSubscriptionDTO> listEnabledByDevice(String deviceId) { ... }

    /** 即将过期的 ACTIVE 订阅（续订用） */
    public List<DeviceSubscriptionDTO> listExpiring(LocalDateTime before) { ... }

    /** upsert 意图：UNIQUE(device,type) 约束前先查（CLAUDE.md 规范） */
    public Long upsertIntent(String deviceId, SubscriptionConstant.Type type, boolean enabled) { ... }

    /** 回填运行态：callId + expireTime + status */
    public void markActive(String deviceId, SubscriptionConstant.Type type, String callId, int expires) { ... }
    public void markPending(String deviceId, SubscriptionConstant.Type type) { ... }
    public void markFailed(String deviceId, SubscriptionConstant.Type type) { ... }
    public void markInactive(String deviceId, SubscriptionConstant.Type type) { ... }
}
```

### 6.3 订阅编排服务（G4）

`voglander-service/.../subscription/DeviceSubscriptionService.java`（service 层，可调 manager + integration command）：

```java
@Slf4j
@Service
public class DeviceSubscriptionService {
    @Autowired private DeviceSubscriptionManager subscriptionManager;
    @Autowired private DeviceManager deviceManager;
    @Autowired private VoglanderServerSubscribeCommand subscribeCommand;

    /** 开启订阅：持久化意图；设备在线则即刻下发 SUBSCRIBE */
    public boolean enable(String deviceId, SubscriptionConstant.Type type) {
        subscriptionManager.upsertIntent(deviceId, type, true);
        DeviceDTO device = deviceManager.getDtoByDeviceId(deviceId);
        if (device == null || !Objects.equals(device.getStatus(), DeviceConstant.Status.ONLINE)) {
            subscriptionManager.markPending(deviceId, type);   // 离线：等注册钩子补发
            return true;
        }
        return doSubscribe(deviceId, type);
    }

    /** 关闭订阅：撤销 dialog + 关闭意图 */
    public boolean disable(String deviceId, SubscriptionConstant.Type type) {
        DeviceSubscriptionDTO sub = subscriptionManager.getByDeviceAndType(deviceId, type);
        if (sub != null && sub.getCallId() != null
                && Objects.equals(sub.getStatus(), SubscriptionConstant.Status.ACTIVE)) {
            subscribeCommand.unsubscribe(sub.getCallId());      // 尽力撤销，失败不阻断
        }
        subscriptionManager.upsertIntent(deviceId, type, false);
        subscriptionManager.markInactive(deviceId, type);
        return true;
    }

    /** 注册即重订阅（需求 4）：遍历意图开启的订阅，重新 SUBSCRIBE 刷新 callId */
    public void resubscribeOnRegister(String deviceId) {
        for (DeviceSubscriptionDTO sub : subscriptionManager.listEnabledByDevice(deviceId)) {
            doSubscribe(deviceId, SubscriptionConstant.Type.valueOf(sub.getSubType()));
        }
    }

    /** 续订：过期前 refresh */
    public void refreshExpiring() {
        LocalDateTime threshold = LocalDateTime.now().plusSeconds(SubscriptionConstant.REFRESH_AHEAD_SECONDS);
        for (DeviceSubscriptionDTO sub : subscriptionManager.listExpiring(threshold)) {
            ResultDTO<String> r = subscribeCommand.refresh(sub.getCallId(), defaultExpires(sub));
            if (r != null && r.getSuccess()) {
                subscriptionManager.markActive(sub.getDeviceId(),
                    SubscriptionConstant.Type.valueOf(sub.getSubType()), sub.getCallId(), defaultExpires(sub));
            } else {
                // refresh 失败 → 重发完整 SUBSCRIBE
                doSubscribe(sub.getDeviceId(), SubscriptionConstant.Type.valueOf(sub.getSubType()));
            }
        }
    }

    private boolean doSubscribe(String deviceId, SubscriptionConstant.Type type) {
        ResultDTO<String> r;
        int expires = SubscriptionConstant.DEFAULT_EXPIRES;
        switch (type) {
            case CATALOG:         r = subscribeCommand.subscribeCatalog(deviceId, expires); break;
            case MOBILE_POSITION: r = subscribeCommand.subscribeMobilePosition(
                                        deviceId, SubscriptionConstant.DEFAULT_POSITION_INTERVAL, expires); break;
            case ALARM:           r = subscribeCommand.subscribeAlarm(deviceId, expires); break;
            default: return false;
        }
        if (r != null && r.getSuccess() && r.getData() != null) {
            subscriptionManager.markActive(deviceId, type, r.getData(), expires);
            return true;
        }
        subscriptionManager.markFailed(deviceId, type);
        return false;
    }
}
```

> 默认参数从 `application-inte.yml` 的 `gateway.gb28181.subscription.*` 读取（§8），上面用常量演示。

### 6.4 注册即订阅钩子（G5）

在 `DeviceRegisterServiceImpl.login()` 持久化成功后（L64 之后、与既有 async 查询并列）加一段，**异步、容错、绝不阻断注册主流程**：

```java
Long deviceId = deviceManager.saveOrUpdate(dto);
// ... 既有 queryDevice/queryChannel async 块 ...

// 需求 4：注册即按当前订阅意图重新发起 SUBSCRIBE
try {
    deviceSubscriptionService.resubscribeOnRegister(dto.getDeviceId());
} catch (Exception e) {
    log.warn("注册重订阅失败，设备ID：{}，错误：{}", dto.getDeviceId(), e.getMessage());
}
```

> `DeviceSubscriptionService` 在 service 层，`DeviceRegisterServiceImpl` 同在 service 层，直接 `@Autowired` 注入无跨层问题。重订阅本身经 `@Async`（订阅命令下发可慢），不占注册线程。

### 6.5 入站通知处理（G6）

`Gb28181ProtocolHandler` 三处改动：

**(a) 目录变更 `Response.NotifyUpdate`**（当前仅 log）→ 新增处理：

```java
case "Response.NotifyUpdate":
    handleCatalogNotifyUpdate(event);
    break;
// 从原合并 log 的 case 列表中移出 Response.NotifyUpdate

private void handleCatalogNotifyUpdate(DeviceEvent event) {
    DeviceOtherUpdateNotify notify = toEntity(event.payload(), DeviceOtherUpdateNotify.class);
    // 字节码核验：列表字段为 deviceItemList（getDeviceItemList()），OtherItem 仅含 deviceId/event
    if (notify == null || notify.getDeviceItemList() == null) {
        return;
    }
    LocalDateTime now = LocalDateTime.now();
    for (DeviceOtherUpdateNotify.OtherItem item : notify.getDeviceItemList()) {
        // Event ∈ {ON, OFF, ADD, DEL, UPDATE}，按标准更新通道状态
        String evt = item.getEvent() == null ? "" : item.getEvent().trim().toUpperCase();
        switch (evt) {
            case "ON":  case "ADD":  case "UPDATE":
                deviceChannelManager.patchChannelStatus(event.deviceId(), item.getDeviceId(),
                    DeviceConstant.Status.ONLINE, now, "CATALOG_NOTIFY");
                break;
            case "OFF":
                deviceChannelManager.patchChannelStatus(event.deviceId(), item.getDeviceId(),
                    DeviceConstant.Status.OFFLINE, now, "CATALOG_NOTIFY");
                break;
            case "DEL":
                deviceChannelManager.deleteDeviceChannel(event.deviceId(), item.getDeviceId());
                break;
            default:
                log.debug("未知目录变更事件 event={}, channelId={}", evt, item.getDeviceId());
        }
    }
    subscriptionTouch(event.deviceId(), Type.CATALOG, now);    // 更新 last_notify_time
    publishVisual("device.catalog_notify", event.deviceId(), "changes", notify.getDeviceItemList().size());
}
```

> **字段名已字节码核验**（`gb28181-common-1.8.2.jar`）：`DeviceOtherUpdateNotify` 含顶层 `event`/`deviceId`/`sumNum` + `deviceItemList: List<OtherItem>`；`OtherItem` 仅 `deviceId`/`event` 两字段。**注意是 `getDeviceItemList()` 而非早先误写的 `getOtherItemList()`。**

**(b) 移动位置 `Notify.MobilePosition`**（当前仅 log+SSE）→ 补落库：

```java
private void handleMobilePosition(DeviceEvent event) {
    MobilePositionNotify pos = toEntity(event.payload(), MobilePositionNotify.class);
    if (pos == null) return;
    // 落库 tb_device_position（最新位置 + 轨迹）
    // 字节码核验：longitude/latitude/speed/direction/altitude 均为 Double，time 为 String
    // 入 VARCHAR 列时 String.valueOf 转换，record() 内部统一处理
    devicePositionManager.record(event.deviceId(), pos);
    subscriptionTouch(event.deviceId(), Type.MOBILE_POSITION, LocalDateTime.now());
    String position = nz(pos.getLongitude()) + "," + nz(pos.getLatitude());
    publishVisual("device.mobileposition", event.deviceId(), "position", position);
}
```

> **字段类型已核验**（`gb28181-common-1.8.2.jar`）：`MobilePositionNotify` 的 `longitude`/`latitude`/`speed`/`direction`/`altitude` 为 `Double`，`time` 为 `String`。`tb_device_position` 列设计为 `VARCHAR`，`DevicePositionManager.record` 内用 `String.valueOf(Double)` 转换（null 安全）；`nz()` 辅助方法对 null 返回空串。

**(c) 告警 `Notify.Alarm`**（已落 `tb_alarm`）→ 仅补 `subscriptionTouch(deviceId, ALARM, now)` 更新最近通知时间，落库逻辑不动。

> `subscriptionTouch` 是轻量更新 `last_notify_time` 的辅助方法，失败不影响主流程。`Gb28181ProtocolHandler` 在 integration 层调 manager（依赖方向 integration→manager 合法，见 ARCHITECTURE §4.2）。

### 6.6 订阅续订与过期清理（G7）

`voglander-manager/.../subscription/SubscriptionRefreshScheduler.java`：

```java
@Component
@ConditionalOnProperty(name = "gateway.gb28181.subscription.refresh-enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionRefreshScheduler {
    @Autowired private DeviceSubscriptionService subscriptionService;

    /** 每 60s 续订即将过期的订阅 */
    @Scheduled(fixedDelayString = "${gateway.gb28181.subscription.refresh-interval-ms:60000}")
    public void refresh() {
        try { subscriptionService.refreshExpiring(); }
        catch (Exception e) { log.warn("订阅续订任务异常: {}", e.getMessage()); }
    }
}
```

> 多节点部署时续订应只由设备所属节点执行——本期单机优先，多节点可复用命令亲和路由（refresh 命令本就按 callId dialog 寻址，由框架/转发处理）。`@Scheduled` 依赖 `ApplicationWeb` 的 `@EnableScheduling`——**已核验存在（L26），无需补**。

### 6.7 Web 层接口（G8 + G9）

新增 `DeviceSubscriptionController`（或并入既有 `DeviceController`），仅一个开关接口 + 列表已带状态：

```java
@RestController
@RequestMapping("/api/v1/device/subscription")
@Tag(name = "设备订阅")
public class DeviceSubscriptionController {

    @PutMapping("/toggle")
    @Operation(summary = "开关订阅")
    public AjaxResult<Boolean> toggle(@Valid @RequestBody SubscriptionToggleReq req) {
        SubscriptionConstant.Type type = SubscriptionConstant.Type.valueOf(req.getType());
        boolean ok = Boolean.TRUE.equals(req.getEnabled())
            ? subscriptionService.enable(req.getDeviceId(), type)
            : subscriptionService.disable(req.getDeviceId(), type);
        return AjaxResult.success(ok);
    }
}
```

`SubscriptionToggleReq`：`@NotBlank deviceId`、`@NotNull type`（CATALOG/MOBILE_POSITION/ALARM）、`@NotNull enabled`。

**G9 列表回显**：设备分页查询时附带三类订阅意图。两种取法择一：
- **推荐**：`DeviceVO` 加 `subscription` 字段（`{catalog:boolean, position:boolean, alarm:boolean}`），列表查询后按 `deviceId` 批量查 `tb_device_subscription`（`listByDevice` 改批量 `listByDeviceIds`）填充，避免 N+1；
- 备选：前端列表渲染时按行懒查（不推荐，N+1）。

`WebAssembler.dtoToVo` 扩展填充该字段。

### 6.8 lab 模拟设备订阅应答（G10）

> **lab 为联调便利，非 GB28181 标准实现，须显式标注、单点收口**（CLAUDE.md 铁律）。

新增 `lab/LabSubscribeListener implements SubscribeListener`：

```java
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabSubscribeListener implements SubscribeListener {

    @Override
    public void onCatalogSubscribe(String platformId, Integer expires, DeviceQuery query) {
        // lab 约定（非标）：立即回一次目录全量，并登记定时目录变更推送
        labSubscribePushService.startCatalogPush(platformId, expires);
    }
    @Override
    public void onMobilePositionSubscribe(String platformId, Integer expires, DeviceMobileQuery query) {
        // lab 约定：按 query 的 interval 定时推模拟 GPS（复用 LabQueryListener 的北京坐标）
        labSubscribePushService.startPositionPush(platformId, expires, query.getInterval());
    }
    @Override
    public void onAlarmSubscribe(String platformId, Integer expires, DeviceAlarmQuery query) {
        // lab 约定：登记后由 /lab/alarm/trigger 或定时器推模拟告警
        labSubscribePushService.startAlarmPush(platformId, expires);
    }
}
```

`LabSubscribePushService`（调度 + 推送，单点收口推送规则）：
- `startPositionPush` → `ScheduledExecutor` 按 interval 调 `ClientCommandSender.sendMobilePositionNotify(from, to, mockPosition())`；
- `startCatalogPush` → 立即 `sendCatalogChangeNotify`（一条 UPDATE）+ 可选定时；
- `startAlarmPush` → 提供 `/lab/subscribe/alarm/trigger?deviceId=` 手动触发 `sendAlarmNotify`，便于测试；
- expires 到期停推；从复用 `LabChannelHolder` 的通道编码（lab 非标约定单点收口处）。

> lab 设备的 SUBSCRIBE 接收依赖框架 client 端把 `SubscribeListener` 装配进去——**已核验**：现有 lab listener（`LabControlListener`/`LabQueryListener`/`LabNotifyListener`/`LabConfigListener`）均为 `@Component implements XxxListener` + `@ConditionalOnProperty(name="voglander.protocol-lab.enabled")`，框架 client 端按多实例观察者自动收集。`LabSubscribeListener` 照此模式即可，**无需额外 `@Bean` 注册**。注意统一用 `voglander.protocol-lab.enabled`（lab 全模块统一键，共 12 处），非 `sip.client.lab.enabled`。

---

## 7. 前端实现（设备列表 3 个开关）

> 仅在设备列表加三个开关（需求 5），复用既有 `CellSwitch` + `beforeChange` 模式（参考 `media/stream-proxy`）。

### 7.1 API（`apps/web-antd/src/api/device.ts`）

```typescript
// 订阅类型
export type SubscriptionType = 'CATALOG' | 'MOBILE_POSITION' | 'ALARM';

// DeviceVO 扩展
export interface DeviceVO {
  // ...既有字段
  subscription?: {
    catalog: boolean;
    position: boolean;
    alarm: boolean;
  };
}

/** 开关订阅 */
export async function toggleDeviceSubscription(
  deviceId: string, type: SubscriptionType, enabled: boolean,
) {
  return requestClient.put<boolean>('/api/v1/device/subscription/toggle', {
    deviceId, type, enabled,
  });
}
```

### 7.2 列定义（`views/device/data.ts`，`useColumns` 内追加三列）

```typescript
function subColumn(field: 'catalog' | 'position' | 'alarm', titleKey: string,
                   onChange: (v: number, row: DeviceVO) => Promise<boolean>) {
  return {
    field: `subscription.${field}`,
    title: $t(titleKey),
    width: 110,
    align: 'center',
    cellRender: {
      name: 'CellSwitch',
      attrs: {
        beforeChange: onChange,
        disabled: () => !hasAccessByCodes(['Device:Subscription:Edit']),
      },
      props: { checkedValue: true, unCheckedValue: false },
    },
  };
}
// 加入列数组：
subColumn('catalog',  'device.subscription.catalog',  onToggleCatalog),
subColumn('position', 'device.subscription.position', onTogglePosition),
subColumn('alarm',    'device.subscription.alarm',    onToggleAlarm),
```

> `CellSwitch` 取 `row[column.field]`——`subscription.catalog` 这种点路径 vxe 支持嵌套字段访问；若适配器不支持嵌套，列表填充时拍平为 `subCatalog`/`subPosition`/`subAlarm` 三个顶层布尔字段（更稳，推荐）。

### 7.3 开关回调（`views/device/list.vue`）

```typescript
function makeToggle(type: SubscriptionType, label: string) {
  return async (newVal: boolean, row: DeviceVO): Promise<boolean> => {
    if (!hasAccessByCodes(['Device:Subscription:Edit'])) {
      message.error($t('common.noPermission')); return false;
    }
    try {
      await toggleDeviceSubscription(row.deviceId, type, newVal);
      message.success($t(newVal ? 'device.msg.subscribeOn' : 'device.msg.subscribeOff', [label]));
      return true;
    } catch { return false; }
  };
}
const onToggleCatalog  = makeToggle('CATALOG',         $t('device.subscription.catalog'));
const onTogglePosition = makeToggle('MOBILE_POSITION', $t('device.subscription.position'));
const onToggleAlarm    = makeToggle('ALARM',           $t('device.subscription.alarm'));
```

### 7.4 i18n（已有部分键，补齐）

`locales/langs/{zh-CN,en-US}/device.json` `subscription` 段：

```json
"subscription": {
  "catalog": "目录订阅",
  "position": "位置订阅",
  "alarm": "告警订阅"
},
"msg": {
  "subscribeOn": "已开启{0}",
  "subscribeOff": "已关闭{0}"
}
```

> en-US 对应 "Catalog Sub." / "Position Sub." / "Alarm Sub."。新增字段须在 `voglander/.cursor/rules/project-rule.mdc` 与 `vue-vben-admin/.cursorrules` 登记（CLAUDE.md 前后端契约规则）。

---

## 8. 配置项

`application-inte.yml` `gateway.gb28181` 段新增：

```yaml
gateway:
  gb28181:
    subscription:
      enabled: true                 # 订阅总开关（关闭则 toggle 接口仅持久化意图、不下发）
      default-expires: 3600         # 目录/告警/位置订阅有效期(秒)
      position-interval: 5          # 位置上报间隔(秒)
      refresh-enabled: true         # 续订定时任务开关
      refresh-interval-ms: 60000    # 续订扫描周期
      refresh-ahead-seconds: 120    # 过期前多少秒续订
```

`ApplicationWeb` 已含 `@EnableScheduling`（L26 核验，续订任务依赖已满足）。

---

## 9. 时序图

### 9.1 开启目录订阅（设备在线）

```
前端开关 ON
  ▼ PUT /device/subscription/toggle {CATALOG, enabled:true}
DeviceSubscriptionController.toggle
  ▼ DeviceSubscriptionService.enable
  ├─ subscriptionManager.upsertIntent(enabled=true)
  ├─ device 在线 ?
  ▼ subscribeCommand.subscribeCatalog(deviceId, 3600)
     ▼ dispatchEnvelopeWithCallId(gb28181.Subscribe.Catalog) → 框架 → SIP SUBSCRIBE → 设备
     ◀── 200 OK（同步返回 callId）
  ▼ subscriptionManager.markActive(callId, expireTime=now+3600)
  ◀── AjaxResult.success(true)
─────────（之后设备目录变更）─────────
设备 ──MESSAGE<Notify cmdType=Catalog>──▶ 平台
  ▼ Gb28181EventForwarder → gb28181.Response.NotifyUpdate
  ▼ VoglanderBusinessNotifier → ShardDispatcher → Gb28181ProtocolHandler
  ▼ handleCatalogNotifyUpdate → deviceChannelManager.patchChannelStatus(ON/OFF/DEL)
  ▼ publishVisual(device.catalog_notify) → 前端 SSE 增量刷新
```

### 9.2 设备重新注册 → 自动重订阅（需求 4）

```
设备 ──REGISTER──▶ 平台
  ▼ Gb28181ProtocolHandler.handleRegister → DeviceRegisterService.login
  ├─ deviceManager.saveOrUpdate
  ├─ async queryDevice / queryChannel（既有）
  ▼ deviceSubscriptionService.resubscribeOnRegister(deviceId)  @Async
     ▼ listEnabledByDevice → [CATALOG, ALARM]（意图开启的）
     ├─ subscribeCatalog → 新 callId → markActive
     └─ subscribeAlarm   → 新 callId → markActive
```

### 9.3 续订

```
SubscriptionRefreshScheduler（每 60s）
  ▼ listExpiring(now + 120s)
  ▼ 逐条 subscribeCommand.refresh(callId, 3600)
     成功 → markActive(刷新 expireTime)
     失败 → doSubscribe 重发完整 SUBSCRIBE
```

---

## 10. 测试方案

遵循 ARCHITECTURE §17 分层策略，测试全部置于 `voglander-web/src/test/...`。

| 层 | 测试类 | 类型 | 重点 |
|----|--------|------|------|
| 命令枚举 | `Gb28181CommandTypeTest`（改） | 单元 | 24 项冻结，5 个 Subscribe type 逐字断言 |
| 出站命令 | `VoglanderServerSubscribeCommandTest` | 纯单元(Mockito) | payload 键齐全、走 WithCallId、type 正确；mock CommandHandlerRegistry |
| Manager | `DeviceSubscriptionManagerTest` | 集成(`BaseTest`+`@Transactional`) | upsertIntent UNIQUE、markActive/Pending、listExpiring 边界 |
| Service | `DeviceSubscriptionServiceTest` | 纯单元(Mockito) | enable 在线下发/离线 PENDING、disable 撤销、resubscribeOnRegister 遍历、refresh 失败回退 |
| 入站处理 | `Gb28181ProtocolHandlerSubscriptionTest` | 纯单元 | NotifyUpdate ON/OFF/DEL → 对应 channel 操作；位置落库；告警 touch |
| Web | `DeviceSubscriptionControllerTest` | 纯单元 | toggle 参数校验、enable/disable 分发 |
| 注册钩子 | 扩展 `DeviceRegisterServiceImplTest` | 单元 | login 成功后调 resubscribeOnRegister、异常不阻断注册 |
| E2E（可选） | `SubscriptionLabE2ETest` | 集成(无事务,手动清理) | lab 真 SIP：toggle→SUBSCRIBE→lab 推 NOTIFY→落库；20s await，甄别既有 flaky |

> E2E 受既有 real-SIP flaky 影响（memory `sip-e2e-flakiness`），用 `git stash` 基线对照，勿追逐既有噪声。

---

## 11. 实施阶段与验收

| 阶段 | 内容 | 交付 | 验收 |
|------|------|------|------|
| **P0 数据与枚举** | `tb_device_subscription`/`tb_device_position` 三套 SQL；`SubscriptionConstant`；`Gb28181CommandType` +5；冻结测试改 24 | DDL + 枚举 | `Gb28181CommandTypeTest` 绿 |
| **P1 出站命令** | `VoglanderServerSubscribeCommand` | 命令封装 | 命令单测绿，payload/type 正确 |
| **P2 持久化** | `DeviceSubscriptionDO/Mapper/DTO/Manager` + `DevicePositionManager` | repository+manager | Manager 集成测试绿 |
| **P3 编排+钩子** | `DeviceSubscriptionService` + `login()` 重订阅钩子 + 续订调度 | service | service 单测绿、注册不阻断 |
| **P4 入站落地** | `handleCatalogNotifyUpdate` + 位置落库 + 告警 touch | handler | 入站单测绿 |
| **P5 Web** | `DeviceSubscriptionController` + 列表 VO 订阅状态批量填充 | controller | controller 单测绿 |
| **P6 lab** | `LabSubscribeListener` + `LabSubscribePushService` | lab | 手动/ E2E 联调推送可达 |
| **P7 前端** | 3 个 CellSwitch 列 + API + i18n + cursor-rule 登记 | 前端 | 开关下发成功、状态回显 |
| **P8 集成** | `mvn clean install`（先 install integration）；前端 `pnpm build:antd` | — | 全套件除既有 flaky 无新增失败 |

**总体验收标准**：
1. 列表三开关可开/关，刷新后状态正确回显；
2. 开启后设备变更（lab 触发）能落库并经 SSE 刷新列表/通道；
3. lab 设备重启重注册后，订阅自动重建（callId 刷新，通知继续到达）；
4. 续订任务在过期前刷新订阅，订阅不中断；
5. 关闭订阅后下发 unsubscribe，设备停止推送。

---

## 12. 风险与协议合规

| 风险 | 缓解 |
|------|------|
| ~~框架订阅命令 payload 键名/方法签名与核验有出入~~ **（2026-06-14 已字节码核验消除）** | 5 个 envelope 键 + handler 读取的 payload 键已用 `gateway-gb28181-1.8.2.jar` 反编译逐字坐实（§3.1 表「已核验」列），与本方案下发键完全一致 |
| ~~`DeviceOtherUpdateNotify` 实体字段名不确定~~ **（已核验消除）** | 列表字段确为 `deviceItemList`（`getDeviceItemList()`），`OtherItem` 仅 `deviceId`/`event`；§6.5 伪代码已据此改正（原 `getOtherItemList()` 为误写） |
| `MobilePositionNotify` 经纬度为 `Double` 写入 VARCHAR 列 | 已核验字段类型；`DevicePositionManager.record` 内 `String.valueOf` 转换；如需地理计算改列类型为 DOUBLE |
| 位置高频推送（5s）写库压力 | `tb_device_position` 仅存最新 + 限频轨迹；最新位置冗余 `tb_device.extend`；可加 `last_notify_time` 节流 |
| 多节点续订重复/错节点 | 本期单机优先；refresh 按 callId dialog 寻址，多节点由命令亲和路由转发到设备所属节点（ARCHITECTURE §11） |
| 设备不支持某类订阅（返回 4xx） | `doSubscribe` 失败 markFailed + log，不影响其他订阅；前端开关回退 false |
| ~~续订任务依赖 `@EnableScheduling`~~ **（已核验消除）** | `ApplicationWeb` 已含 `@EnableScheduling`（L26 核验），无需补 |
| 入站事件 groupName 字符串与框架转发不一致 | 实现 §6.5 时比照已通的 `handleAlarm`（`Notify.Alarm`）确认 `Response.NotifyUpdate`/`Notify.MobilePosition` 的 `event.groupName()` 逐字匹配 |
| lab 非标简化 | `LabSubscribePushService` 单点收口推送规则，注释标「非 GB28181 标准，仅 lab 自环���，复用 `LabChannelHolder` 通道编码 |

**协议合规铁律**（CLAUDE.md §GB28181 协议合规铁律）：
- 出站命令 type 逐字对齐框架注册键（`gb28181.Subscribe.*`），不拼错；
- Event 头、CmdType、订阅有效期/间隔均按 GB28181-2022 §9.11 / 附录 A.2.4；
- 设备/通道仍为独立 20 位国标编码，目录变更按 `DeviceID` 定位通道，不做前缀猜测；
- lab 模拟的非标约定显式标注 + 单点收口，非 lab 路径不复用。

---

## 附录：改动文件清单

**新增**
- `sql/`：三套 SQL 加两张表
- common：`constant/device/SubscriptionConstant.java`
- integration：`gb28181/command/Gb28181CommandType.java`(改)、`server/command/subscribe/VoglanderServerSubscribeCommand.java`、`gb28181/handler/Gb28181ProtocolHandler.java`(改)、`gb28181/lab/LabSubscribeListener.java`、`gb28181/lab/LabSubscribePushService.java`
- repository：`entity/DeviceSubscriptionDO.java`、`entity/DevicePositionDO.java` + 两个 Mapper
- manager：`domaon/dto/DeviceSubscriptionDTO.java`、`manager/DeviceSubscriptionManager.java`、`manager/DevicePositionManager.java`、`subscription/SubscriptionRefreshScheduler.java`
- service：`subscription/DeviceSubscriptionService.java`、`login/DeviceRegisterServiceImpl.java`(改)
- web：`controller/device/DeviceSubscriptionController.java`、`request/.../SubscriptionToggleReq.java`、`DeviceVO`(改)、`WebAssembler`(改)、`ApplicationWeb`(确认 @EnableScheduling)
- 前端：`api/device.ts`(改)、`views/device/data.ts`(改)、`views/device/list.vue`(改)、`locales/langs/{zh-CN,en-US}/device.json`(改)、cursor-rule 登记
- 测试：见 §10

**配置**：`application-inte.yml` `gateway.gb28181.subscription.*`
