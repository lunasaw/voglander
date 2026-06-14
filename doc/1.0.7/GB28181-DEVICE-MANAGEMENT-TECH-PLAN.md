# GB28181 设备管理 — 平台端完整设备操作技术方案

> 版本 1.0.7 · 分支 `0608_dev` · 文档日期 2026-06-11（2026-06-13 按 1.0.7 架构基线重审修订）· **本文为实施方案，按此分阶段 TDD 落地**
> 角色定位：voglander 在本方案中扮演 **GB28181 平台端（上级 / SIP Server）**，对下级设备做完整管理：列表展示与筛选、生命周期接入、主动指令下发、被动响应回填。
> 依赖版本：**sip-gateway / gb28181 1.8.2**（root pom 实测 `sip-gateway.version=1.8.2`/`gb28181-proxy.version=1.8.2`）、zlm 1.0.11。本方案 §1.5 实体事实原对 1.8.1 jar 反编译核对，**1.8.2 落地时需对 1.8.2 jar 重核**（大概率不变，§1.5 已标注待复核点）。
> 关联：[[sip-gateway-1.8.0-migration]]（命令/事件 API 事实）、`doc/1.0.6/GB28181-LAB-DEVICE-REGISTER-TARGET-PLAN.md`（模拟设备侧，互为对端）。
> **架构基准：`doc/1.0.7/ARCHITECTURE.md`（已取代 1.0.3，含 S1~S6 协议接入层对称化）**——本方案所有分层/命名/管线设计严格遵循该文，落点章节见 §0.5。
> **⚠️ 重要修订（2026-06-13）**：本方案初版锚定 `doc/1.0.3/ARCHITECTURE.md`，而 1.0.7 架构在「出站命令通道」做了结构性改动——`DeviceCommandService` 已重写为**协议无关 SPI 接口**（`supportProtocols()` + `DeviceAgreementService` 表驱动路由，见 1.0.7 §8.1）。这直接影响 S2 设计：**GB28181 专属支链方法不得塞入协议无关 SPI**（详见 §4.3 修订）。本次按 1.0.7 §8/§9 重锚全部章节号并重写 S2。

---

## 0.5 架构对齐基线（落地前必读，2026-06-13 按 ARCHITECTURE.md **1.0.7** 重锚）

> 本方案是 1.0.7 架构基线上的**增量实现**，不引入任何新架构。下表把本期每类改动锚定到 1.0.7 ARCHITECTURE.md 的既有约束章节，落地时逐条对表，**不得偏离**。
> **节号已从 1.0.3 重锚到 1.0.7**；新增 §8（协议接入层对称化 / 出站 SPI）、§9（媒体协议选路）两节作为 S2/S1.6 的**新约束来源**。

| 本期改动 | 锚定 ARCHITECTURE.md(1.0.7) 章节 | 必须遵循的约束 |
|---------|--------------------------|---------------|
| S1 `DeviceController.getPage` + `*ListResp` | §14.1 Controller 模板方法 / §5.1 各层职责边界 | Essential 模板含 `getPage`；入参 `*Req`（禁直收 DTO/DO）；分页包 `*Resp(total+items)`；查询走"全量分页条件查询" |
| S1 web 筛选 Req 命名 | §5.2 命名规约 | web 入参用 `*Req`；client 查询对象用 `*QO`。**现有 `DeviceQueryReq` 在 client `qo` 包**（被 `queryChannel/queryDevice` 用），web 新筛选请求**必须改名**避免碰撞（见 §3.2 B1.1） |
| S1 `DeviceManager.getPage` 多条件 | §6.2 Manager 模板 + LambdaQueryWrapper 强制规范 | 扩展现有 `getPage(DTO,page,size)`；**禁** `new LambdaQueryWrapper<>(entity)`，必须带 `condition` 参数；对外只用 DTO |
| S1 时间字段 | §5.2 / §12.2 | DO/DTO 用 `LocalDateTime`；VO 出参 Unix 毫秒（字段以 `Time` 结尾）；Req→DTO 毫秒↔`LocalDateTime` 由 `DeviceWebAssembler` 转 |
| **S2 GB 专属支链方法归属（⚠️ 本次重大修订）** | **§8.1 出站命令 SPI / §6.5 SPI 表驱动 / §5.1 Integration 边界** | `DeviceCommandService` 已是**协议无关 SPI**（含 `supportProtocols()`），**GB28181 专属动作（preset/status/mobile-position/config 下载下发/record 控制/alarm/broadcast）禁止塞入该接口**（否则强迫未来 ONVIF/RTSP 实现桩方法，违反 §8 对称化目标）。落到 **GB 专属子接口/服务**（见 §4.3 方向 A）；出站底层走 `Gb28181CommandType` 枚举 + `dispatchEnvelope`（§7.3/§8.2），**禁直调 `ServerCommandSender`**（§18#6） |
| S2 媒体字段名 | §7.3 出站 envelope / §18#7 | 白名单 handler SDP IP 字段名是 **`mediaIp`**（非 `sdpIp`），经媒体协议层封装，门面委托即可不碰此坑 |
| **S2/S1.6 媒体方法收口** | **§9.2 媒体协议选路（S5）** | `MediaPlayService` 现经 `MediaProtocolRouter`/`MediaProtocolHandler` 选路（§9.2），委托它仍是正确入口；但 `startPlay/stopPlay` 是 **SPI 接口方法**，改其语义须评估对协议无关契约的波及（见 §1.6 修订） |
| S3 `Gb28181ProtocolHandler` 补响应 | §7.1/§7.2 四层管线 / §18#14/§18#15 | handler **禁 import 任何 sip-gateway 类型**（`GatewayEvent`/`BusinessNotifier`）；**禁改** `ShardDispatcher`/`InboundEventDispatcher`；payload 在 handler 内才用 FastJSON2 反序列化 |
| S3 `patchExtendInfo` 读改写 | §13.3 事件分片串行 / §7.2 | 入站在 `EventShard` 单线程槽执行，同 `deviceId` 串行，读-改-写**无需加锁**；走 Manager 统一入口触发缓存清理（§6.4） |
| S3 录像缓存 | §13.1 缓存分层 | 列表型结果用 `RedisCache`（值 FastJSON 字符串），**不** `@Cached`（后者仅 Repository 单对象） |
| S3 handler 模式匹配 | §18#9（**本方案已纠正为过时**） | integration pom **实为 Java 17**（`<source>17>`/`maven.compiler.source=17`，2026-06-13 复核 pom 99-100 行），**可用** `instanceof X x`。**1.0.7 ARCHITECTURE.md §3.1/§18#9/§19.2 仍写"钉 source 9" 为过时错误，应同步订正** |
| 全期类型转换/JSON | §6.1 / §18#11 | 一律 FastJSON2 正反序列化（`JSON.parseObject(JSON.toJSONString(src),T.class)`），禁手写解析、禁 Jackson/Gson |
| 全期响应协议 | §6.6 | Web 层返回 `AjaxResult<T>`（取错误 `getMsg()`）；Integration 返回 `ResultDTO<T>`（取错误 `getMessage()`），不可混用 |
| 全期测试分层 | §17.1 | Controller/Service 纯 Mockito 单元（无 `@SpringBootTest`）；Manager/Repository 集成 `BaseTest`+`@Transactional`；入站/异步**不用** `@Transactional`（§15.2） |

**与 ARCHITECTURE.md(1.0.7) 的两处需同步订正（本方案为准）：**

1. **§3.1 / §18#9 / §19.2 Java 源码级别**：1.0.7 ARCHITECTURE.md 仍称 integration 钉 `<source>9>` 禁用模式匹配——**经 2026-06-13 实地核对 `voglander-integration/pom.xml` 第 15-16 行 `maven.compiler.source=17` + 第 99-100 行 `<source>17</source>`，全模块统一 17**。S3 handler 可放心用 Java 16+ `instanceof X x`。**此订正本应在 1.0.7 落实却遗漏，建议回写 ARCHITECTURE.md §3.1 表格、§18#9、§19.2 TODO**。
2. **§19.2 TODO "Response.* 多数仅日志，按需补落库"**：本方案 S3 正是落地该 TODO 的前 5 类核心 + ConfigDownload；完成后应从 §19.2 移除或更新进度。

> **架构红线复述（1.0.7 §18 精选，逐条本期都踩得到）**：#6 出站禁直调 ServerCommandSender（走 `dispatchEnvelope`）；#8 出站 type 必来自 `Gb28181CommandType` 枚举；#11 类型转换走 FastJSON2；#14 新协议禁改分片层（本期不新增协议，但 handler 改动同样禁碰分片）；#15 handler 禁 import gateway 类型。
> **新增红线（1.0.7 §8 引入）**：`DeviceCommandService` 是协议无关 SPI，**GB 专属方法不进该接口**——这是 S2 落地的硬约束（见 §4.3）。

---

## 0. TL;DR

需求三句话：
1. **GB28181 设备管理**：设备列表支持多条件筛选（deviceId / 名称 / 状态 / 协议 / IP / 时间范围）+ 基础展示（在线状态、心跳、注册时间、通道数）。
2. **完整作为 GB28181 平台端**：平台端可对下级设备下发**全部** GB28181 指令（查询 / PTZ / 录像 / 报警 / 配置 / 媒体），不再只暴露 4 个。
3. **对设备的完整操作响应**：下级设备回上来的**全部** `Response.*` 事件（DeviceStatus / RecordInfo / PtzPosition / Config / PresetQuery …）要落库或回填，不再"只打日志"。

现状一句话总结：**底层能力齐全，命令下发与实时点播主链路已在协议验证台联调中打通，上层只剩"查询/配置/报警/广播"几条支链未暴露 + 列表筛选 + 入站响应回填**。

> **⚠️ 2026-06-11 第三次复核（去重，落地前必读）**：经源码核对，本方案初稿严重低估了现状——把命令下发当成"只有 DeviceCmdController 4 端点、PTZ/录像全缺"。**实际 web 层命令下发已拆为 4 个独立 controller，PTZ / 实时点播 / 录像回放主链路全部已存在并由协议验证台 ServerPanel 验证通过**。本次更新据此大幅削减 S2 范围（见 §1.1 / §4），并新增「协议验证台复用清单」（§1.7）。

| 维度 | 现状 | 缺口 |
|------|------|------|
| 出站指令 bean（integration） | 6 个 Server Command 全部就绪（Device 6/Ptz 20/Config 10/Record 6/Alarm 10/Media 14，**56 个 public 方法**，2026-06-12 逐 bean 源码核对） | — 已完整 |
| 服务门面 `DeviceCommandService`（**1.0.7 已是协议无关 SPI**） | 暴露 **11 方法**（**supportProtocols**/queryChannel/queryDevice/queryDeviceInfo/queryCatalog/**ptzControl**/startPlay/**startPlayback**/stopPlay/reboot/**controlPlayback**）；其中 ptz/playback/queryDevice 已被对应 controller 用上并跑通 | ❌ 缺 status/preset/mobilePosition/config 下载+下发/record 控制(start/stop)/alarm 查询控制/broadcast。**⚠️ 这些是 GB 专属动作，1.0.7 §8.1 把本接口 SPI 化后禁止塞入（否则强迫未来 ONVIF/RTSP 实现桩方法）——落到 GB 专属子接口/服务（§4.3 方向 A）**；⚠️ `startPlay` 是裸 INVITE 残桩（`inviteRealTimePlay` 返回 null）但**无人调用**（实时点播走 `/live/*`），删或委托 `MediaPlayService`（§1.6） |
| Web 命令下发控制器（**已拆 4 个**） | `PtzController` `/api/v1/ptz/{control,stop,preset}`（preset 返回"不支持"）✅；`LivePlayController` `/api/v1/live/{start,stop,{id},keepalive}` ✅；`PlaybackController` `/api/v1/playback/{start,stop,control,records}` ✅；`DeviceCmdController` `/api/v1/device-cmd/{query-catalog,query-info,reboot,record}`（入参 `Map`） | ❌ 仅 `DeviceCmdController` 4 端点入参是 `Map<String,String>` 需 Req 化；**缺** query-status/preset/mobile-position、config 下载+下发、record 控制、alarm 下发、broadcast 端点。**PTZ/点播/回放端点已存在，严禁重复新建** |
| Web 控制器 `DeviceController` | 旧式：直接收 `DeviceDO`、返回 `Page<DeviceVO>`、无 `/getPage` 模板端点 | ❌ 无条件筛选 `*QueryReq`、无 `*ListResp`、不符模板方法规范 |
| 入站响应 `Gb28181ProtocolHandler` | Catalog/DeviceInfo 已落库；Lifecycle/Keepalive/Alarm 已处理 | ❌ 14 个 `Response.*` 仅 `log.info`，无落库/无回填/无 SSE |
| 设备筛选模型 `DeviceQueryReq`(client qo) | 仅 `deviceId` 一个字段 | ❌ 无法支撑列表筛选 |

改动覆盖 **4 个模块**：`voglander-client`（DTO/接口扩展）、`voglander-service`（门面补全剩余支链）、`voglander-integration`（入站响应回填）、`voglander-web`（`DeviceController` 模板化 + `DeviceCmdController` Req 化 + 补查询/配置/报警/广播端点）。
**不改** `voglander-integration` 的 6 个 Server Command（已完整），**不改** 已跑通的 `PtzController`/`LivePlayController`/`PlaybackController`（仅在其上补缺口端点），**不改** `sip-proxy` / `zlm-starter`（无需上游重建）。

---

## 1. 现状链路梳理（关键，理解后再动手）

### 1.1 出站：平台 → 设备（指令下发）

```
                  ┌─ PtzController      /api/v1/ptz/{control,stop,preset}      ✅ 已通(preset 返回"不支持")
                  ├─ LivePlayController /api/v1/live/{start,stop,{id},keepalive} ✅ 已通(走 MediaPlayService)
前端 ──────────────┼─ PlaybackController /api/v1/playback/{start,stop,control,records} ✅ 已通
                  └─ DeviceCmdController /api/v1/device-cmd/{query-catalog,query-info,reboot,record}
                                          ← ⚠️ 4 端点入参 Map，需 Req 化；缺 status/preset/config/record控制/alarm/broadcast
        │
        ▼
DeviceCommandService 门面(@Service "GbDeviceCommandService")  ← 暴露 11 方法；ptz/playback/queryDevice 已被上面 controller 用上
        │  委托
        ▼
6 个 VoglanderServerXxxCommand(integration)  ← ✅ 完整：56 个 public 方法全部就绪
        │  dispatchEnvelope(type, deviceId, payload)
        ▼
ServerCommandSender(实例 Bean, 按 deviceId) → sip-gateway 1.8.2 → 下级设备
```

**结论（修正）**：出站主链路（PTZ / 实时点播 / 录像回放）**已存在且经协议验证台 ServerPanel 联调通过**，初稿"PTZ 需新建 /ptz、录像全缺"为误判。真实缺口仅两类：①`DeviceCmdController` 4 端点入参 `Map` → Req 化；②**查询补全**（status/preset/mobile-position）、**配置**（下载/下发）、**record 控制**（start/stop）、**alarm 下发**、**broadcast** 这几条支链门面未透出。全部"门面 + 控制器透出"问题，**不需要碰协议层，也不要重建已有 controller**。

### 1.2 入站：设备 → 平台（事件回调）

```
sip-gateway → VoglanderBusinessNotifier.notify(GatewayEvent)  [@Async sipNotifierExecutor]
        │  轻量翻译 GatewayEvent → DeviceEvent，立即归还 SIP 线程
        ▼
ShardDispatcher.dispatch(DeviceEvent)  [按 deviceId 分片，单线程 FIFO]
        ▼
InboundEventDispatcher  [按 protocol 段路由]
        ▼
Gb28181ProtocolHandler.handle(DeviceEvent)  ← switch over 35 事件（5 Lifecycle + 5 Notify + 14 Response + 8 Session + default）
        ├── Lifecycle.{Register/Online/Offline/RemoteAddressChanged}  ✅ 已落库
        ├── Notify.{Keepalive/Alarm/...}                              ✅ 已处理
        ├── Response.Catalog → batchUpsertWithStatus                  ✅ 已落库
        ├── Response.DeviceInfo → updateDeviceInfo                    ✅ 已回填 extend
        ├── Response.{DeviceStatus/RecordInfo/PtzPosition/Config/     ❌ 仅 log.info
        │            ConfigDownload/PresetQuery/SdCardStatus/             （**12 个** Response.* 仅日志：扣除
        │            HomePosition/CruiseTrack*/Subscribe/...}              已落库的 Catalog/DeviceInfo）
        └── Session.{InviteOk/InviteFailure/Ack/Bye/...}             ✅ 已驱动 MediaSession
```

**结论**：入站缺口是"`Response.*` 分支只打日志"。要做到"完整设备操作响应"，需对**有业务价值的响应**落库或回填到设备/通道扩展信息，并按现有约定推 SSE（`device.*`）。

### 1.3 已验证的底层事实（避免踩坑）

- `ServerCommandSender` 是**实例 Bean**，按 `deviceId` 调用；6 个 Server Command 已封装 envelope（`dispatchEnvelope(type, deviceId, payload)`），`type` 严格对齐 `Gb28181CommandSpecs.declare()`。
- 入站 `Response.*` 的 `payload` 是 `Map<String,Object>`，需用 FastJSON2 反序列化为 `gb28181-common` 的实体（如 `DeviceResponse`/`DeviceInfo`），**禁止手写字符串解析**（项目强制规范）。
- 设备状态常量 `DeviceConstant.Status.ONLINE=1 / OFFLINE=0`。
- 设备扩展信息存 `tb_device.extend`（JSON 字符串），`DeviceDTO.ExtendInfo` 已有 `deviceInfo` 字段承载设备信息 JSON；新增响应类型（状态/配置）建议同样并入 `ExtendInfo`，避免改表。
- 时间：DO/DTO 用 `LocalDateTime`，VO 出参用 Unix 毫秒（字段以 `Time` 结尾）。
- 入站重活在分片槽**单线程**执行，handler 内可安全做 DB 写，无需额外加锁。

### 1.4 方案可行性结论（先说结论）

**方案整体可行（S1/S3）**，三层缺口都是"上层透出/回填"问题，不触碰协议层，不需上游重建（sip-proxy/zlm 不动）。验证中**纠正了初稿的 4 处事实错误 + 补充了 3 个原方案漏掉的功能缺口**，全部并入下文对应 Sprint。
> **⚠️ 2026-06-13 按 1.0.7 架构重审**：**S2 不可照初版照搬落地**——`DeviceCommandService` 在 1.0.7 已重写为协议无关 SPI（§8.1），初版往该接口塞 9 个 GB 专属方法会破坏 SPI 对称化。S2 需按 §4.3 方向 A 重做（GB 专属方法落 GB 子接口/服务）。S1/S3 与 1.0.7 对齐良好、可直接落地。

> **2026-06-11 二次源码审核（落地前复核）**：对四层（数据模型 / 出站链路 / 入站 handler / 实体）逐一源码+反编译核对，结论 **可实施**，并据此修正：①integration 源码级别实为 **17**（非 9，模式匹配可用，2026-06-13 再核 pom `<source>17>` 确认）；②`/ptz` 端点**已存在**（`PtzController`，初稿"需新建"为误）；③`getPageByCondition` 改为**扩展现有 `getPage`**；④门面实为 **11 方法**（含 `supportProtocols`，1.0.7 SPI 抽象方法）、底层实为 **56 个 public 方法**（2026-06-12 复核订正，初稿 65 偏高）。残留实测盲点：`DeviceStatus` 字段 JSON key 大小写，S3 落地抓真实样本确认；实体事实原对 **1.8.1** jar 反编译，**1.8.2 落地时需对 1.8.2 jar 重核**（§1.5 标注）。

| 验证项 | 结论 |
|--------|------|
| 27 个事件 type 是否真由框架发出 | ✅ 全部确认：反编译 `gateway-gb28181-1.8.1.jar` 的 `Gb28181EventForwarder`，常量池含全部 5 Lifecycle + 7 Notify + 16 Response/含 Catalog + 7 Session（**1.8.2 待重核，预期不变**） |
| `Response.*` payload 结构（原 R4 最大风险） | ✅ **已消除风险**：forwarder `emit()` = `JSON.toJSONString(响应实体)` → 回解析成 `Map` → 塞 `GatewayEvent.payload`。**payload Map 就是响应实体扁平化本身**，现有 `handleCatalog`/`handleDeviceInfo` 的 `toEntity(payload, X.class)` 即正确用法，照搬即可 |
| 6 个 Server Command 底层方法是否齐全 | ✅ 齐全（**56 个 public 方法**，2026-06-12 逐 bean 源码核对：Device 6 / Config 10 / Ptz 20 / Media 14 / Alarm 10 / Record 6），门面只透出 11 个（含 `supportProtocols`） |
| `DeviceCommandService` 接口/实现可扩展性 | ⚠️ **1.0.7 已是协议无关 SPI**（[DeviceCommandService.java](../../voglander-client/src/main/java/io/github/lunasaw/voglander/client/service/device/DeviceCommandService.java)：`supportProtocols()` + 10 协议无关方法），由 `DeviceAgreementService` 表驱动路由（§8.1）。单实现 `GbDeviceCommandService`（`@Service("GbDeviceCommandService")`）；`alarmCommand`/`recordCommand` 已注入但标 `@SuppressWarnings("unused")`，本期启用即移除。**GB 专属方法不进该接口**（§4.3 方向 A） |

### 1.5 验证中纠正的事实 & 新发现的缺口（关键）

**A. 实体类名与字段——初稿写错，以下为 1.8.1 反编译实测（按此实现；⚠️ 1.8.2 落地时对 1.8.2 jar 重核，预期不变）：**

| 事件 | **正确实体类**（`io.github.lunasaw.gb28181.common.entity.response.*`） | 关键字段（实测） |
|------|----------------------------------------------------------------|------------------|
| `Response.DeviceStatus` | `DeviceStatus` | `cmdType, result, online, status`（**字段大小写为落地前唯一实测盲点**：getter 是 `getResult()/getOnline()`，但反编译显示实际字段/JSON key 偏小写。回填用 `JSON.toJSONString(实体)` 原样序列化即可，**不要手映射**；S3 落地时用 `SipMessageTracer.recv` 抓真实样本最终确认 key 大小写） |
| `Response.PtzPosition` | `PTZPositionResponse`（**不是** `PtzPosition`） | `cmdType, sn, deviceId, pan, tilt, zoom(Double), horizontalFieldAngle, verticalFieldAngle, maxViewDistance` |
| `Response.PresetQuery` | `PresetQueryResponse` | `cmdType, sn, deviceId, presetList`（内 `PresetItem{presetId, presetName}`） |
| `Response.Config` | `DeviceConfigResponse` | `cmdType, sn, deviceId, result` |
| `Response.ConfigDownload` | `DeviceConfigDownloadResponse`（**与 Config 不同实体**） | `cmdType, sn, deviceId, result, basicParam, videoParamOpt, ...11 个 cfg 子对象` |
| `Response.RecordInfo` | `DeviceRecord`（**不是** `RecordInfoQueryResponse`） | `cmdType, sumNum(int), recordList`（内 `RecordItem{deviceId, name, startTime, endTime(String!), secrecy, type, fileSize, filePath}`） |
| `Response.SdCardStatus` | `SDCardStatusResponse` | `cmdType, sn, deviceId, ...` |
| `Response.DeviceInfo`（既有） | `DeviceInfo` | `cmdType, deviceName, result, manufacturer, model, firmware, channel(int)` |

> **forwarder 入参提示**：`onPtzPositionResponse(deviceId, PTZPositionResponse)`、`onConfigDownloadResponse(deviceId, ...)`、`onSdCardStatusResponse`、`onPresetQueryResponse`、`onCruiseTrack*` 这几个 **无独立 correlationId/sn 入参**（sn 在实体内）；而 `onCatalogResponse/onDeviceInfoResponse/onDeviceStatusResponse/onConfigResponse` 带独立 sn 入参（落到 `GatewayEvent.correlationId`）。落库取 sn 时优先用实体内字段，更稳。

**B. 新发现缺口 1 — `DeviceRecord.RecordItem.startTime/endTime 是 String**（非毫秒/非 LocalDateTime）。S3 录像结果缓存时**原样存 String**，前端展示直接用；若要排序/筛选需另解析（GB28181 标准格式 `yyyy-MM-ddTHH:mm:ss`），本期不解析，列展示为主。

**C. 新发现缺口 2 — 点播在系统里有两套实现，门面残桩需收口到 `MediaPlayService`（方向已纠正，见 §1.6）。**

初稿把这条写成"`startPlay` 返回 null callId、需新增 `stopByStreamId` 反查 callId"——这是**在给残桩打补丁**。2026-06-11 源码复核发现：点播**早已有完整实现**，残桩只是没接上去：

| 实现 | 位置 | 能力 |
|------|------|------|
| `MediaPlayService.startLive`（完整编排） | `voglander-service/.../live/impl/MediaPlayServiceImpl.java:92` | 选节点→`openRtpServer`→预写 INVITING 占位→发 INVITE→等 `on_stream_changed` future(15s)→拼 PlayUrl→引用计数/会话注册/GC 回收；以 **streamId** 为主键，`stopLive(streamId)` 引用计数停流、`closeStream` 真实关流 |
| `DeviceCommandService.startPlay`（裸 INVITE 残桩） | `voglander-service/.../command/impl/GbDeviceCommandService.java:149` | 仅 `mediaCommand.inviteRealTimePlay`，**返回 null callId**，不选节点/不开 RTP/不等流/不计数 |

**关键事实：前端协议台 ServerPanel 的实时点播调的是 `/live/start`（→`MediaPlayService`），不是残桩。** 即测试 tab 已经替我们验证了正确链路。

**所以缺口 C 的"新增 `stopByStreamId` 反查 callId"基本是重复造轮子**——`MediaPlayService` 用 streamId 当主键、`stopLive(streamId)` 已是引用计数停流。**正确做法**：`DeviceCommandService.startPlay/startPlayback/stopPlay` 改为**委托 `MediaPlayService`**（详见 §1.6 设备控制层收口），而非自己重发 INVITE。这把缺口 C 从"补一环"降级为"删残桩 + 接门面"。

**D. 新发现缺口 3 — 列表筛选字段与 DO 不一致。** `DeviceVO` 有 `subType/protocol/protocolName`，但 **`DeviceDO` 只有 `type`**（无 subType/protocol 列）。S1 筛选**只能按 `type` 落 DB**；`subType/protocol` 是 VO 派生展示字段，**不可作 DB 筛选条件**（否则 wrapper 引用不存在的列编译失败）。初稿 §0/§3 "协议筛选"应明确为按 `type` 筛选。

**E. 确认 — 通道数无现成 count 接口。** `DeviceChannelService` 仅 `extends IService`，无 `count group by device_id`。S1 的 `channelCount` 需：要么按当前页 deviceId 集合调 `IService.count(wrapper)` 逐个（页大小通常 ≤20，可接受）、要么新增一个 group-by 的 Mapper 方法（属"复杂查询"例外，允许自定义 SQL）。**本期选逐个 count（页内 ≤20 次，简单且不破"禁自定义 SQL"原则）**，若实测慢再换 group-by Mapper。

---

## 1.6 设备控制层收口（关键架构决策，2026-06-11 复核新增）

> 本节回应"点播功能加入后，设备管理页与协议验证台测试 tab 能否复用同一套设备控制能力"。结论：**能，且应收口到 `DeviceCommandService` 单一门面**——它一半已存在，缺的是把媒体方法接到 `MediaPlayService`。

### 1.6.1 现状：三方各自为政

```
设备管理页(规划) ──► （未定）
协议验证台 ServerPanel ─┬─ 查目录/查信息/重启 → /device-cmd/*      → DeviceCommandService 门面
                        └─ 实时点播           → /live/start          → MediaPlayService（完整编排）
```

- 非媒体指令（查询/PTZ/重启…）已走 `DeviceCommandService` 门面 → 6 个 `ServerXxxCommand`，链路正确。
- **媒体点播两套并存**：`DeviceCommandService.startPlay` 是裸 INVITE 残桩（返回 null），`MediaPlayService.startLive` 是完整编排。ServerPanel 用的是后者（`/live/start`），残桩无人调用。

### 1.6.2 决策：门面委托 MediaPlayService，收敛为唯一设备控制层

`DeviceCommandService` 定位为**协议无关的设备控制层门面**，设备管理页与测试 tab **共用它**。媒体类方法（startPlay/startPlayback/stopPlay）**改为委托 `MediaPlayService`**，删除裸 INVITE 残桩：

```
设备管理页 ─┐
            ├─► DeviceCommandService（设备控制层门面，协议无关）
测试台 tab ─┘        ├─ 查询/PTZ/录像/配置/报警 → 6 个 VoglanderServerXxxCommand（裸指令，无状态）
                     └─ 点播/回放/停流          → MediaPlayService（完整编排：选节点/开RTP/等流/计数/GC）
                                                   └─ 1.0.7 §9.2：内部经 MediaProtocolRouter/MediaProtocolHandler 选路
```

委托后三方收敛到一条链路，**设备页点播直接复用 ServerPanel 已验证的 `/live/start` + `/live/stop`，无需新造点播端点**。

> **⚠️ 1.0.7 §9.2 新增认知（S5 媒体协议选路）**：1.0.7 在 `MediaPlayServiceImpl` 之下新增了 `MediaProtocolRouter`/`MediaProtocolHandler` 协议选路层（`Gb28181MediaProtocolHandler` 等）——`startLive` 现经 `resolveForDevice(deviceId)` 选协议 handler 再 `establish/terminate`。**委托 `MediaPlayService` 仍是正确入口**（它是协议无关编排层，选路对门面透明），但本节示意图中"MediaPlayService 完整编排"在 1.0.7 下游已是 router 选路，不再是 1.0.3 的直发 INVITE。门面无感知，无需改委托代码。

### 1.6.3 委托改动要点（service 模块内部，无跨模块新依赖）

`MediaPlayService` 与 `GbDeviceCommandService` 同在 `voglander-service` 模块，门面注入它无新增模块依赖。

```java
// GbDeviceCommandService 注入 MediaPlayService，改写 3 个媒体方法（删除裸 mediaCommand.invite* 残桩）
@Autowired private MediaPlayService mediaPlayService;

@Override
public ResultDTO<String> startPlay(DevicePlayReq req) {
    LiveStartDTO dto = new LiveStartDTO();
    dto.setDeviceId(req.getDeviceId());
    dto.setChannelId(req.getChannelId());        // ← DevicePlayReq 需补 channelId 字段
    dto.setStreamMode(req.getStreamMode());
    LivePlayDTO play = mediaPlayService.startLive(dto);
    return ResultDTOUtils.success(play.getStreamId());   // 返回稳定 streamId（非 null callId）
}

@Override
public ResultDTO<Void> stopPlay(String streamId) {       // 语义：参数从 callId 改为 streamId
    boolean ok = mediaPlayService.stopLive(streamId);
    return ok ? ResultDTOUtils.success(null)
              : ResultDTOUtils.failure(ServiceExceptionEnum.LIVE_STREAM_NOT_FOUND.getCode(),
                                       ServiceExceptionEnum.LIVE_STREAM_NOT_FOUND.getMessage());
}
```

> **语义变更提示（兼容性 + 1.0.7 SPI 影响）**：`stopPlay(String)` 入参语义由 callId 改为 streamId。现有调用方仅 ServerPanel（走 `/live/stop`，不经此方法）+ 残桩无人用，影响面可控。`controlPlayback(streamId,...)` 已是 streamId 语义（实测 [GbDeviceCommandService.java:175](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/command/impl/GbDeviceCommandService.java) `getByStreamId` 反查），无需改。`DevicePlayReq` 增 `channelId` 为新增字段，向后兼容。
> **⚠️ 注意 `startPlay/stopPlay` 是 1.0.7 SPI 接口方法**（`DeviceCommandService` 协议无关契约，§8.1）：改它们的语义/签名等于改**所有协议实现**共享的契约。当前仅 GB 单实现、无副作用；但落地时须确认改动不破坏 `DeviceAgreementService` 路由调用方对返回语义（streamId vs callId）的假设——若未来 ONVIF 实现也走此方法，streamId 语义须协议无关成立（流主键概念在 ONVIF/RTSP 同样适用，成立）。

### 1.6.4 协议验证台测试 tab 的复用定位

测试台天然是设备控制层的**免硬件验证对端**，复用关系分两向：

- **平台→设备（出站）**：测试 tab ServerPanel 点"PTZ/查询/点播" → 走 `DeviceCommandService` → Lab 的 `Lab*Listener` 收到并推 `clientcmd.*` SSE，可断言指令真实下发。PTZ/点播/查目录已通；S2 新增 `/query-status`、`/config/*`、`/alarm/*`、`/broadcast` 等支链端点后，ServerPanel 直接加按钮即可验证。
- **设备→平台（入站）**：Lab `LabQueryListener` 回包（DeviceStatus/Catalog…）→ 平台 S3 `handleXxx` 落库 → 推 `device.*` SSE。**缺口**：`LabQueryListener` 当前只回 Catalog/DeviceInfo/DeviceStatus，要完整验证 S3 的 5 类响应，需补 record/preset/config-download 回包（属 Lab 扩展，不污染平台代码，列为 S3 可选配套）。

### 1.6.5 不纳入本次收口的部分（避免过度抽象）

- `MediaPlayService` 自身不动（编排已完整、ServerPanel 已验证）。
- Lab 侧 `LabSipClient`/`Lab*Listener`/`LabMediaPushService`（ffmpeg 模拟推流）是**设备 UA 镜像对端**，与平台控制层语义相反，**不抽公共父类**——强行合并只增耦合。
- `SseRelayEvent`/`SseEventBus`/前端 `useSseEvents` 已是共享基建，继续复用，无需改。

---

## 1.7 协议验证台复用清单（去重核心，2026-06-11 复核新增）

> 1.0.6 协议验证台已沉淀大量可复用资产。本节明确"哪些**直接复用、严禁重写**"、"哪些**提取共享**"、"哪些**设备页才新增**"。设备管理 = 协议验证台已验证能力的**生产化封装**，不是另起炉灶。

### 1.7.1 后端 —— 直接复用，严禁重写

| 能力 | 实现位置 | 复用方式 |
|------|---------|---------|
| **实时点播全编排** | `MediaPlayService.startLive/stopLive`（已含选节点/开 RTP/等流 15s/引用计数/GC 回收） | 设备页点播**直接调 `/api/v1/live/start`**，与 ServerPanel 同源，不另写 |
| **PTZ 下发** | `PtzController` `/api/v1/ptz/{control,stop}` → `DeviceCommandService.ptzControl` | 设备页 PTZ **直接调 `/api/v1/ptz/control`**，已通 |
| **录像回放** | `PlaybackController` `/api/v1/playback/{start,stop,control,records}` | 直接调，已通（control 仅支持暂停/继续，seek/倍速服务层显式不支持） |
| **查目录/查信息/重启** | `DeviceCmdController` `/api/v1/device-cmd/{query-catalog,query-info,reboot}` | 直接调（本期把入参 `Map` 换成 `*Req`，路径不变） |
| **SSE 实时推送** | `SseEventBus` + `RedisBackedSseEventBus` + `SseRelayListener`（integration 发 `SseRelayEvent`→service 转 `SseEvent`，跨节点 Redis 广播 + 回路抑制 + 15s 心跳） | S3 入站响应回填**复用 `publishVisual`/`SseRelayEvent`** 推 `device.*`，不新建总线 |
| **6 个 Server Command** | `voglander-integration` `server/command/*`（56 个 public 方法） | 门面补缺口方法时委托它们，bean 不动 |

### 1.7.2 前端 —— 直接复用 / 提取共享

| 资产 | 路径（`vue-vben-admin/apps/web-antd/src`） | 复用定位 |
|------|------|---------|
| **MediaPlayer 播放器门面** | `components/MediaPlayer.vue`（包 `MediaPlayerManager`，多格式自动选 HLS/FLV/RTMP/RTSP/WS-FLV，Modal 弹窗） | 设备页点播**直接 import**，传 `liveStart()` 返回的 `playUrls`（GB 直播建议 `format="hls"`） |
| **useSseEvents** | `composables/useSseEvents.ts`（EventSource + 前缀过滤 + 去重 + 自动重连） | 设备页**直接调**，传 `() => config.topics`，监听 `device.register/online/offline/catalog/info` 实时刷新列表 |
| **SipTimeline** | `views/protocol-lab/components/SipTimeline.vue`（事件时间线，按方向渲染箭头） | 设备详情"事件时间线"**直接复用** |
| **PTZ 方向盘 UI** | 当前内嵌 `views/protocol-lab/components/ServerPanel.vue` | **提取** `components/PtzControl.vue`（props: deviceId/channelId/speed/disabled/@command），供 ServerPanel 与设备页共用 |
| **设备列表 upsert 逻辑** | 当前内嵌 `ServerPanel.vue`（SSE 驱动 Map upsert + 选中） | **提取** `composables/useDeviceList.ts` 复用（设备页从 DB 拉初始列表 + SSE 增量） |
| **设备命令 API** | 当前全在 `api/protocol-lab.ts`（`ptzControl/queryCatalog/queryDeviceInfo/rebootDevice/liveStart`） | **提取** `api/device.ts`，迁移这些通用命令；Lab 专属（`labRegister/labPush*` 等）留在 `protocol-lab.ts` |

### 1.7.3 设备页才新增（协议验证台没有的生产能力）

- 设备列表**条件筛选 + 持久化分页**（Lab 设备列表是 SSE 内存态，无 DB 筛选）→ S1。
- 门面剩余**查询/配置/报警/广播支链端点**（Lab ServerPanel 只用了 catalog/info/reboot/ptz/live）→ S2。
- 入站 `Response.*` **落库/回填**（Lab 只做 SSE 透传不落库）→ S3。
- 前端 `views/device/` 页面骨架 + 路由菜单 + `api/device.ts` → S4。

> **一句话**：本期后端不重写任何已跑通的下发/点播链路，只"补门面剩余支链 + 列表筛选 + 入站回填"；前端最大化复用播放器 / SSE / 时间线，新增的只是设备列表页与 API 提取。

---

## 2. 设计目标与范围切分

按"先展示、再下发、后回填"三层推进，每层独立可验收：

| Sprint | 主题 | 模块（ARCHITECTURE.md §4） | 验收 |
|--------|------|------|------|
| **S1** | 设备列表筛选 + 基础展示 | web / manager | `/device/getPage`（§14.1 模板 Essential）支持多条件分页（按 `type` 筛选，非 subType/protocol），VO 含通道数、状态名 |
| **S2** | 补全 GB 专属指令支链（**落 GB 子接口，不污染协议无关 SPI；不重写已通的 PTZ/点播/回放**） | client / service / web | 查询(status/preset/mobile-position)/配置(下载+下发)/录像控制(start/stop)/报警(查询+控制)/广播 有 `*Req` 端点 + **GB 专属服务方法**（出站走 `dispatchEnvelope` + `Gb28181CommandType`，§7.3/§8.2）；`DeviceCmdController` 入参 `Map`→`*Req`；`startPlay` 残桩删除或委托 `MediaPlayService`（§1.6）。**GB 专属方法不进 `DeviceCommandService` SPI（§4.3 方向 A）** |
| **S3** | 完整设备操作响应回填 | manager / integration | 5 类核心 `Response.*` 落库/回填 + SSE（复用 `publishVisual`），录像结果走 `RedisCache`（`RecordInfoCacheManager`，§13.1）；handler 不碰分片层（§18#14/#15） |
| **S4**（可选） | 前端设备管理页 | vue-vben-admin | 列表筛选页 + 操作面板，**最大化复用** MediaPlayer/useSseEvents/SipTimeline，提取 `api/device.ts`，严格对齐后端契约 |

下文 S1–S3 为本期必做（后端），S4 列规划。

> **范围提醒**：S2 **已不含** PTZ 端点新建、实时点播端点新建、录像回放端点新建——这三者 §1.1/§1.7 已确认存在并跑通。S2 只补"门面没透出的支链"。

---

## 3. S1 — 设备列表筛选 + 基础展示

### 3.1 问题

- `DeviceQueryReq`(client `qo`) 只有 `deviceId`，无法筛选；**且它已被 `DeviceCommandService.queryChannel/queryDevice` 占用**，不能直接扩字段（会污染命令查询语义），web 列表筛选需用**独立的新请求类**。
- `DeviceController` 旧式：`pageListByEntity(...,DeviceDO)` / `pageList(...)` 直接收 DO，违反"入参必须用 Web 层 `*Req`"（ARCHITECTURE.md §5.1）；返回裸 MyBatis `Page<DeviceVO>` 而非 `*ListResp`。
- 无"全量分页条件查询"入口（ARCHITECTURE.md §14.1 模板方法 Essential 要求 `getPage`）。

### 3.2 改动

**B1.1 新增 `DevicePageReq`（web 层）** — `web/api/device/req/DevicePageReq.java`

> **⚠️ 命名碰撞（架构对齐）**：`DeviceQueryReq` 已存在于 `voglander-client` 的 `domain/device/qo` 包（被命令门面 `queryChannel/queryDevice` 使用）。按 ARCHITECTURE.md §5.2 命名规约，web 入参用 `*Req`、client 查询对象用 `*QO`，但二者**同名不同包会造成 import 混淆与误用**。本期 web 列表筛选请求**改名为 `DevicePageReq`**（语义=分页条件查询请求），与 client 既有 `DeviceQueryReq`(qo) 解耦，互不影响。

筛选维度（全部可选，走 `LambdaQueryWrapper` 带 condition 链式）：

```java
@Data
public class DevicePageReq implements Serializable {
    private Long    id;
    private String  deviceId;       // 精确
    private String  name;           // like
    private Integer status;         // 1在线/0离线
    private Integer type;           // 协议类型 DeviceAgreementEnum（DO 仅有 type 列，可筛）
    private String  ip;             // like
    private String  serverIp;       // 注册节点，精确
    private Long    keepaliveTimeStart;  // 心跳时间范围（Unix 毫秒）
    private Long    keepaliveTimeEnd;
    private Long    registerTimeStart;   // 注册时间范围
    private Long    registerTimeEnd;
    // 注意（缺口 D）：DeviceVO 的 subType/protocol 是派生展示字段，DeviceDO 无对应列，
    //               不可作筛选条件（否则 wrapper 引用不存在的列编译失败）。
}
```

**B1.2 `DeviceManager` 条件分页方法** — 用带 `condition` 的 `LambdaQueryWrapper`（ARCHITECTURE.md §6.2 强制规范，禁止 `new LambdaQueryWrapper<>(entity)`）：

> **扩展现有 `getPage`，不另起入口**：`DeviceManager.getPage(DeviceDTO, int, int)`（现有 L376，符合 §6.2 模板方法）已是条件分页，但条件来源是 DTO 字段、不覆盖时间范围/like。**本期扩展它支持多条件**——新增重载 `getPage(DeviceQueryDTO, int, int)`（或在条件构建段并入新字段），**不新建并行的 `getPageByCondition`**，避免两个分页入口违背模板方法单一性。下方示例仅作条件构建段的签名说明，落地时合并进 `getPage` 重载。

```java
// 新增重载：getPage(DeviceQueryDTO, page, size)；条件构建段如下
public Page<DeviceDTO> getPage(DeviceQueryDTO q, int page, int size) {
    LambdaQueryWrapper<DeviceDO> qw = new LambdaQueryWrapper<>();   // ✅ 空构造 + condition 链式（§6.2）
    qw.eq(q.getDeviceId() != null, DeviceDO::getDeviceId, q.getDeviceId())
      .like(q.getName() != null, DeviceDO::getName, q.getName())
      .eq(q.getStatus() != null, DeviceDO::getStatus, q.getStatus())
      .eq(q.getType() != null, DeviceDO::getType, q.getType())
      .like(q.getIp() != null, DeviceDO::getIp, q.getIp())
      .eq(q.getServerIp() != null, DeviceDO::getServerIp, q.getServerIp())
      .ge(q.getKeepaliveTimeStart() != null, DeviceDO::getKeepaliveTime, q.getKeepaliveTimeStart())
      .le(q.getKeepaliveTimeEnd() != null, DeviceDO::getKeepaliveTime, q.getKeepaliveTimeEnd())
      .ge(q.getRegisterTimeStart() != null, DeviceDO::getRegisterTime, q.getRegisterTimeStart())
      .le(q.getRegisterTimeEnd() != null, DeviceDO::getRegisterTime, q.getRegisterTimeEnd())
      .orderByDesc(DeviceDO::getCreateTime);
    // 复用现有 pageQuery(page,size,wrapper) 的 DO→DTO 装配（Assembler，§6.1）
    ...
}
```

> Manager 对外用 DTO（ARCHITECTURE.md §5.1）：新增 `DeviceQueryDTO`（manager.domaon.dto），`LocalDateTime` 时间字段；Web 层 `DevicePageReq`（Unix 毫秒）经 `DeviceWebAssembler.pageReqToQueryDto()` 转换（毫秒→`LocalDateTime`），符合 §5.2 时间字段规约。

**B1.3 `DeviceListResp`（web 层 resp）** — 统一分页包装 `{ total, items }`：

```java
@Data
public class DeviceListResp implements Serializable {
    private Long total;
    private List<DeviceVO> items;
}
```

**B1.4 `DeviceController` 增标准模板端点**（保留旧端点 `@Deprecated`，新增不破坏前端）：

```java
@PostMapping("/getPage")
@Operation(summary = "分页条件查询", description = "全量分页条件搜索，前端灵活组装条件")
public AjaxResult<DeviceListResp> getPage(
    @RequestBody(required = false) DevicePageReq pageReq,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size) {
    DeviceQueryDTO dto = deviceWebAssembler.pageReqToQueryDto(pageReq);
    Page<DeviceDTO> p = deviceManager.getPage(dto, page, size);
    DeviceListResp resp = new DeviceListResp();
    resp.setTotal(p.getTotal());
    resp.setItems(p.getRecords().stream().map(DeviceVO::convertVO).collect(toList()));
    return AjaxResult.success(resp);
}
```

**B1.5 基础展示增强 — `DeviceVO` 补通道数**（缺口 E）：
列表需展示"该设备下通道数"。`DeviceChannelService` 仅 `extends IService`，**无 group-by count 接口**。方案：
- A（推荐，本期）：`getPage(DeviceQueryDTO,...)` 拿到当前页 DTO 列表后，对页内 deviceId（通常 ≤ 页大小，默认 ≤10）逐个 `deviceChannelService.count(new LambdaQueryWrapper<DeviceChannelDO>().eq(DeviceChannelDO::getDeviceId, id))`，回填 VO `channelCount`。页内 ≤10 次轻量 count，符合"禁自定义 SQL"原则。
- B（实测慢再换）：新增 `DeviceChannelMapper` 的 `count group by device_id`（属"复杂查询"例外，允许自定义 SQL）。

`DeviceVO` 新增字段 `private Integer channelCount;`（已有 `statusName/typeName` 派生展示，无需改）。

### 3.3 S1 验收

- `POST /api/v1/device/getPage` 各条件组合返回正确（含时间范围、状态、名称模糊）。
- VO 返回 `statusName`（在线/离线）、`keepaliveTime`（毫秒）、`channelCount`。
- 旧端点不回归（保留）。
- 测试：`DeviceManagerTest`（集成，`BaseTest`，ARCHITECTURE.md §14.1）覆盖 `getPage(DeviceQueryDTO,...)` 各筛选分支；`DeviceControllerTest`（纯 Mockito 单元，§14.1）覆盖 getPage 装配。

---

## 4. S2 — 平台端完整指令下发（**1.0.7 SPI 重审：GB 专属方法落 GB 子接口**）

### 4.1 问题

底层 6 个 Server Command 共 **56 个 public 方法**已就绪，门面 `DeviceCommandService` 透出 11 个（含 `supportProtocols`），其中 **PTZ / 实时点播 / 录像回放 + 查询的 3 端点（catalog/info/reboot）已有 controller 并跑通**（§1.1）。真正缺口收敛为：①`DeviceCmdController` 4 端点入参 `Map`→`*Req`；②门面没透出的支链（设备状态/预置位/移动位置查询、配置下载+下发、录像 start/stop 控制、报警查询+控制、广播）。

> **⚠️ 1.0.7 架构重审（关键）**：初版把这些支链方法直接加进 `DeviceCommandService` 接口。但 1.0.7 已把该接口**重写为协议无关 SPI**（§8.1：`supportProtocols()` + `DeviceAgreementService` 表驱动路由）。上述支链全是 **GB28181 专有动作**（预置位/SD 卡/报警复位/广播都是国标特有），塞进协议无关接口会**强迫未来每个 `OnvifDeviceCommandService`/`RtspDeviceCommandService` 实现一堆空桩**——正是 §8 对称化要消灭的 ISP 违反。**S2 据此改为方向 A：GB 专属支链落到 GB 专属子接口/服务，不进协议无关 SPI。** 出站底层 type 已就绪（§8.2 `Gb28181CommandType` 枚举已声明 `QUERY_DEVICE_STATUS`/`QUERY_PRESET`/`QUERY_MOBILE_POSITION`/`QUERY_ALARM`/`QUERY_RECORD_INFO`/`CONTROL_RECORD`/`CONTROL_ALARM_RESET`/`CONFIG_DOWNLOAD`/`DEVICE_BROADCAST`，全部命中），所以"功能能下发"无障碍，**只是这些方法该挂在哪**的设计问题。

**S2 = 补 GB 专属支链（落 GB 子接口）+ Req 化 `DeviceCmdController`，不重写已有端点，不污染协议无关 SPI。**

### 4.2 指令域盘点（✅已通 / ⚠️改造 / ❌补缺，逐项标注现状）

> "门面"列说明：✅ = 已在协议无关 SPI `DeviceCommandService` 中（11 方法之一）；❌补 = **新增到 GB 专属子接口/服务**（不进协议无关 SPI，§4.3 方向 A）。

| 指令域 | 底层方法（已就绪，integration） | 现有门面 / GB 专属服务归属 | Web 端点现状与改动 |
|--------|--------------------------------|------------------------------|----------|
| 设备查询 | queryDeviceInfo/Status/Catalog/Preset/MobilePosition（Device cmd 6 方法） | ✅info/catalog（协议无关 SPI）；❌status/preset/mobilePosition → **GB 子接口** | ✅`/device-cmd/query-catalog`、`/query-info` 已通（Req 化）；❌**补** `/query-status` `/query-preset` `/query-mobile-position` |
| PTZ | controlDevicePtz / move*/zoom*/stop（Ptz cmd 20 方法） | ✅ptzControl（协议无关 SPI，方向+speed） | ✅**`PtzController` `/api/v1/ptz/{control,stop}` 已存在且跑通**，**严禁重建**；preset 端点已存在但返回"不支持"（PTZControlEnum 缺 PRESET 常量 + wrapper 无 SET/GOTO/DEL），如需预置位下发须先补底层枚举与命令，本期不做 |
| 配置 | configDevice / downloadBasic/Video/AudioConfig / rebootDevice（Config cmd 10 方法） | ✅reboot（协议无关 SPI）；❌config 下载/configDevice → **GB 子接口** | ✅`/device-cmd/reboot` 已通（Req 化）；❌**补** `/config/download` `/config/set` |
| 录像 | queryDeviceRecord(long/Date/String 三重载) / start/stopDeviceRecord（Record cmd 6 方法） | ✅queryDevice（录像查询，被 `PlaybackController` `/records` 用上）；❌start/stop 控制 → **GB 子接口** | ✅**录像查询 `/playback/records` 已存在且跑通**（走 `queryDevice`），**不重建**；❌**补** 录像 start/stop 控制端点（`recordCommand` 现标 `@SuppressWarnings("unused")`，启用即移除） |
| 报警 | queryDeviceAlarm / controlDeviceAlarm / queryToday/Recent...（Alarm cmd 10 方法） | ❌ 全缺 → **GB 子接口**（bean 已注入但 `@SuppressWarnings("unused")`） | ❌**补** `/alarm/query` `/alarm/control`（注意区分：`AlarmController` `/api/v1/alarm` 是**告警事件管理/查询**，非下发；下发端点本期新建） |
| 媒体 | inviteRealTimePlay / invitePlayBack / controlPlayBack / sendBye / sendBroadcast（Media cmd 14 方法，含 inviteRealTimePlayWithCallId） | ✅play(残桩)/playback/stop/controlPlayback（协议无关 SPI）；❌broadcast → **GB 子接口** | ✅**实时点播 `/live/*`、回放 `/playback/{start,stop,control}` 已通**，**不在 device-cmd 重复造**；`startPlay` 残桩无人调用，删或委托 `MediaPlayService`（§1.6）；❌**补** `/broadcast` |

### 4.3 改动

**B2.1 新增 GB 专属命令子接口 `Gb28181DeviceCommandService`**（client）+ `GbDeviceCommandService` 实现它（service）

> **⚠️ 架构对齐（ARCHITECTURE.md(1.0.7) §8.1 SPI / §6.5 / §5.1）——本次重大修订**：
> - `DeviceCommandService` 是**协议无关 SPI**，由 `DeviceAgreementService` 按 `supportProtocols()` 表驱动路由（§8.1）。**禁止**把 GB 专属支链方法塞进它（会强迫未来 ONVIF/RTSP 实现空桩，破坏对称化）。
> - **方向 A（采纳）**：新增 GB 专属子接口 `Gb28181DeviceCommandService extends DeviceCommandService`（client 层），把 GB 专有动作声明在子接口上；`GbDeviceCommandService` 改为 `implements Gb28181DeviceCommandService`（自动同时是协议无关 SPI 的 GB 实现，`DeviceAgreementService` 仍能路由到它，零影响）。
> - Web 控制器对 **GB 专属端点**直接注入 `Gb28181DeviceCommandService`（按 bean 名 `@Qualifier("GbDeviceCommandService")` 或类型注入），**不经** `DeviceAgreementService` 多协议路由（这些动作本无多协议语义）。协议无关端点（已有的 catalog/info/reboot/ptz）维持现状走 SPI。
> - 方法**只做参数翻译 + 委托** 6 个 `VoglanderServerXxxCommand`；命令 bean 内部走 `dispatchEnvelope(type, deviceId, payload)`（envelope 通道，type 来自 `Gb28181CommandType` 枚举），**门面与 controller 均禁直调 `ServerCommandSender`**（§18#6）。返回 `ResultDTO<T>`（取错误用 `getMessage()`，§6.6）。

GB 专属子接口（client，**不动协议无关的 `DeviceCommandService`**）：

```java
// voglander-client/.../service/device/Gb28181DeviceCommandService.java
/**
 * GB28181 专属命令服务——承载国标特有动作（预置位/SD卡/报警复位/广播/配置下载等），
 * 这些动作无多协议语义，不上提到协议无关 SPI {@link DeviceCommandService}。
 * GbDeviceCommandService 实现本接口即同时是 DeviceCommandService 的 GB 实现，DeviceAgreementService 仍可路由。
 */
public interface Gb28181DeviceCommandService extends DeviceCommandService {
    // 查询补全
    ResultDTO<Void> queryDeviceStatus(String deviceId);
    ResultDTO<Void> queryPreset(String deviceId);
    ResultDTO<Void> queryMobilePosition(String deviceId, String interval);
    // 配置
    ResultDTO<Void> downloadConfig(String deviceId, String configType);   // BASIC/VIDEO/AUDIO
    ResultDTO<Void> setDeviceConfig(DeviceConfigReq req);                  // name/expiration/...
    // 录像控制
    ResultDTO<Void> controlRecord(String deviceId, boolean start);
    // 报警
    ResultDTO<Void> queryAlarm(DeviceAlarmQueryReq req);
    ResultDTO<Void> controlAlarm(String deviceId, String method, String type);
    // 广播
    ResultDTO<Void> broadcast(String deviceId);
    // 录像查询（结果走 S3 缓存；deviceId + start/end 毫秒）
    ResultDTO<Void> queryRecord(DeviceRecordQueryReq req);
}
```

实现示例（`GbDeviceCommandService implements Gb28181DeviceCommandService`，纯委托）：

```java
// 类签名改为实现 GB 子接口（仍是 @Service("GbDeviceCommandService")，supportProtocols() 不变）
@Service("GbDeviceCommandService")
public class GbDeviceCommandService implements Gb28181DeviceCommandService {

    @Override public ResultDTO<Void> queryDeviceStatus(String deviceId) {
        return deviceCommand.queryDeviceStatus(deviceId);
    }
    @Override public ResultDTO<Void> downloadConfig(String deviceId, String configType) {
        return configCommand.downloadDeviceConfig(deviceId, configType);
    }
    @Override public ResultDTO<Void> queryRecord(DeviceRecordQueryReq req) {
        return recordCommand.queryDeviceRecord(req.getDeviceId(), req.getStartTime(), req.getEndTime());
    }
    @Override public ResultDTO<Void> controlRecord(String deviceId, boolean start) {
        return start ? recordCommand.startDeviceRecord(deviceId) : recordCommand.stopDeviceRecord(deviceId);
    }
    // queryPreset/queryMobilePosition/setDeviceConfig/queryAlarm/controlAlarm/broadcast 同构委托
}
```

> 注意：`alarmCommand`/`recordCommand` 当前在 `GbDeviceCommandService` 标了 `@SuppressWarnings("unused")` —— 本期它们真正被用上，移除注解。
> **为何不直接在 `GbDeviceCommandService` 加 public 方法而要抽子接口**：子接口让 web 层依赖**接口而非实现类**（保持 §5.1 分层、便于测试 mock），也为未来"GB 专属能力契约"留一处单一声明点；若团队认为单实现下子接口略重，可退化为直接在 `GbDeviceCommandService` 上加方法并由 web 按 bean 名注入——但**绝不可上提到 `DeviceCommandService` SPI**（红线）。

**B2.2 入参 Req 化**（client domain qo，新增）
- `DeviceConfigReq`（deviceId/name/expiration/heartBeatInterval/heartBeatCount）
- `DeviceRecordQueryReq`（deviceId/startTime/endTime，Unix 毫秒；门面内 `Instant.ofEpochMilli` 转 long 秒或直接用 `long` 重载）
- `DeviceAlarmQueryReq`（deviceId/startTime/endTime/startPriority/endPriority/alarmMethod/alarmType）

**B2.3 改造 `DeviceCmdController` + 补支链端点**（web）

两件事，注意**不碰已有的 `PtzController`/`LivePlayController`/`PlaybackController`**：

1. **`DeviceCmdController` 现有 4 端点 Req 化**：把 `query-catalog`/`query-info`/`reboot`/`record` 的 `Map<String,String>` 入参换成类型化 `*Req`，路径不变，写操作记 `[AUDIT]` 日志（reboot 已示范），统一返回 `ResultDTO.isSuccess()→AjaxResult`。
2. **补支链端点**（在 `DeviceCmdController` 下分组，或按域拆 `/device-cmd/{config,record,alarm}` 子路径）：查询补全（`/query-status` `/query-preset` `/query-mobile-position`）、配置（`/config/download` `/config/set`）、录像控制（`/record/start` `/record/stop`）、报警下发（`/alarm/query` `/alarm/control`）、广播（`/broadcast`）。

> **⚠️ PTZ 端点已存在，勿在此新建**：初稿这里给了一段 `@PostMapping("/ptz")` 示例并称"控制器现无此端点需新建"——**经核实 `PtzController` `/api/v1/ptz/control` 已存在并跑通**（§1.1/§4.2）。设备页 PTZ 直接调 `/api/v1/ptz/control` 即可，**不要**在 `DeviceCmdController` 另造 `/ptz`。下面以"录像控制"为例示范支链端点写法（**注入 GB 专属子接口，不经 `DeviceAgreementService`**）：

```java
// 注入 GB 专属子接口（按类型或 bean 名）；GB 专属端点不走多协议路由
@Autowired private Gb28181DeviceCommandService gbCommandService;

@PostMapping("/record/start")
@Operation(summary = "开始录像（记录操作日志）")
public AjaxResult<Boolean> recordStart(@Valid @RequestBody DeviceRecordControlReq req, HttpServletRequest request) {
    log.info("[AUDIT] record-start device={}, ip={}", req.getDeviceId(), request.getRemoteAddr());
    ResultDTO<Void> r = gbCommandService.controlRecord(req.getDeviceId(), true);
    return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
}
```

**B2.4 `ServiceExceptionEnum` 补充**：录像/报警/配置下发失败若需专用错误码，先在 `ServiceExceptionEnum` 补类型（项目强制：抛 `ServiceException` 前确认枚举存在）。已有 `DEVICE_OPERATION_FAILED`/`PARAM_ERROR` 可复用，新增 `RECORD_QUERY_FAILED`/`ALARM_QUERY_FAILED`/`CONFIG_DOWNLOAD_FAILED` 视需要。

**B2.5 停流闭环 = 门面委托 MediaPlayService（缺口 C，方向已纠正，见 §1.6）**：

初稿的"新增 `stopByStreamId` 反查 callId + `DevicePlayReq` 加 streamId"是给残桩打补丁，**改为收口委托**：

- `DevicePlayReq` 增 `channelId` 字段（`MediaPlayService.startLive` 以 deviceId+channelId 生成稳定 streamId，无需前端传 streamId）。
- `startPlay` 委托 `mediaPlayService.startLive(...)`，返回真实 `streamId`（非 null callId）。
- `stopPlay(String)` 入参语义由 callId 改为 streamId，委托 `mediaPlayService.stopLive(streamId)`（已是引用计数停流 + GC 真实关流）。**不新增 `stopByStreamId`**——`stopPlay` 收口即可。
- Web 停流端点收 `streamId`，与 `controlPlayback(streamId,...)` 一致。
> 设备页点播**复用 ServerPanel 已验证的 `/live/start` + `/live/stop`，device-cmd 不重复造点播端点**；本条只做"门面媒体方法接到 MediaPlayService"这一收口。

### 4.4 S2 验收

- **架构红线（1.0.7 §8.1）**：协议无关 SPI `DeviceCommandService` 接口**未新增任何 GB 专属方法**（仍 11 方法）；GB 支链方法全在 `Gb28181DeviceCommandService` 子接口；`DeviceAgreementService` 路由不受影响（`DeviceAgreementServiceTest` 仍绿）。
- 新增支链端点（status/preset/mobile-position、config download/set、record start/stop、alarm query/control、broadcast）各至少一个联通（envelope 实际发出，`SipMessageTracer` 有 send 记录）。
- `DeviceCmdController` 4 个旧端点入参全部 `*Req` + `@Valid`，无 `Map<String,String>`；GB 专属端点注入 `Gb28181DeviceCommandService`（不经 `DeviceAgreementService`）。
- 门面新增方法有对应单测（`GbDeviceCommandServiceTest`，纯 Mockito，mock 6 个 command bean，验证委托正确）。
- **回归红线**：`PtzController`/`LivePlayController`/`PlaybackController` 既有端点不改动、不回归（它们已被协议验证台 ServerPanel 验证）。
- Swagger 文档完整（`@Tag`/`@Operation`/`@Parameter`）。

---

## 5. S3 — 完整设备操作响应回填

### 5.1 问题

`Gb28181ProtocolHandler` 中 **12 个** `Response.*` 分支仅 `log.info(...)`（已扣除已落库的 Catalog/DeviceInfo；switch 共 14 个 Response case），平台收到了设备应答却"不记账"。"完整设备操作响应"要求：**有业务价值的响应必须落库或回填，并按既有约定推 SSE**。本期落地 **5 类核心 + ConfigDownload**，低优先（SdCard/HomePosition/CruiseTrack）可暂留 log 分期补。

### 5.2 响应价值分级与处理策略

| 事件 | payload 实体（gb28181-common，§1.5A 实测类名） | 处理 | 落点 |
|------|------------------------------|------|------|
| `Response.DeviceStatus` | `DeviceStatus`（`Online`/`status`/`Result`） | 回填设备实时状态 | `tb_device.extend.deviceStatus`(JSON) + 可选刷新 `status` |
| `Response.RecordInfo` | `DeviceRecord`（`sumNum` + `recordList[RecordItem]`） | **缓存**（录像查询结果，前端要列表） | Redis（见 5.4），**不落表** |
| `Response.PtzPosition` | `PTZPositionResponse`（`pan`/`tilt`/`zoom`） | 回填设备当前云台位置 | `tb_device.extend.ptzPosition` |
| `Response.PresetQuery` | `PresetQueryResponse`（`presetList[PresetItem]`） | 回填 | `tb_device.extend.presets` |
| `Response.Config` | `DeviceConfigResponse`（`result`） | 回填设备配置 | `tb_device.extend.config` |
| `Response.ConfigDownload` | `DeviceConfigDownloadResponse`（`basicParam` 等 11 子项） | 回填 | `tb_device.extend.configDownload` |
| `Response.SdCardStatus` | `SDCardStatusResponse` | 回填 | `tb_device.extend.sdCard` |
| `Response.HomePosition`/`CruiseTrack*` | `HomePositionResponse`/`CruiseTrack*Response` | 回填（低优先，可暂留 log） | `tb_device.extend.*` |
| `Response.Subscribe`/`NotifyUpdate`/`DeviceInfoError`/`DeviceInfoRequest` | — | 保持 log（无落库价值） | — |

> **策略要点**：除"录像查询结果（RecordInfo）"需要独立列表外，其余响应都是**设备/通道的最新快照**，统一回填进 `tb_device.extend`（JSON 子字段），**不新增表**，避免表爆炸。`extend` 已是 JSON，`DeviceDTO.ExtendInfo` 扩展子字段即可（未提交代码可直接改，无兼容包袱）。

### 5.3 改动

**B3.1 `DeviceDTO.ExtendInfo` 扩展子字段**（manager.domaon.dto）

```java
public static class ExtendInfo {
    // 既有
    private String serialNumber, transport, password, streamMode, charset, deviceInfo;
    private int expires;
    // 新增（设备应答快照，FastJSON2 序列化原始响应实体 → JSON 字符串）
    private String deviceStatus;    // Response.DeviceStatus  (DeviceStatus)
    private String ptzPosition;     // Response.PtzPosition    (PTZPositionResponse)
    private String presets;         // Response.PresetQuery    (PresetQueryResponse)
    private String config;          // Response.Config         (DeviceConfigResponse)
    private String configDownload;  // Response.ConfigDownload (DeviceConfigDownloadResponse)
    private String sdCardStatus;    // Response.SdCardStatus   (SDCardStatusResponse)
}
```

`DeviceVO.ExtendInfoVO` 对称扩展（出参展示）。

**B3.2 `DeviceManager` 新增"扩展信息合并回填"方法**（避免覆盖整个 extend）：

```java
/** 读现有 extend → 合并指定子字段 → 写回（按 deviceId） */
public void patchExtendInfo(String deviceId, java.util.function.Consumer<DeviceDTO.ExtendInfo> patch) {
    DeviceDTO dto = getDtoByDeviceId(deviceId);
    if (dto == null) return;
    DeviceDTO.ExtendInfo ext = dto.getExtendInfo() != null ? dto.getExtendInfo() : new DeviceDTO.ExtendInfo();
    patch.accept(ext);
    dto.setExtendInfo(ext);
    saveOrUpdate(dto);  // 走统一入口，触发缓存清理
}
```

> 入站在 `EventShard` 单线程槽执行，同一 deviceId 串行，读-改-写**无并发风险、无需加锁**（ARCHITECTURE.md §7.2/§13.3 分片 FIFO；[[sip-gateway-1.8.0-migration]]）。`saveOrUpdate` 走 Manager 统一入口，触发 `clearCache` 双键清理（§6.4）。

**B3.3 `Gb28181ProtocolHandler` 补响应处理方法**

> **架构红线（ARCHITECTURE.md §7.1/§7.2/§18#14/#15，落地必守）**：
> - handler **禁 import 任何 sip-gateway 框架类型**——入参是协议无关的 `DeviceEvent`（manager/client 层模型），不是 `GatewayEvent`；payload 是 `Map`，**在 handler 内才用 FastJSON2 `toEntity` 反序列化**（§7.2 payload 延迟反序列化）。
> - **禁改** `ShardDispatcher`/`InboundEventDispatcher`——本期只在 `Gb28181ProtocolHandler` 的 switch 内补 case 与私有 `handleXxx`，分片层零改动（§18#14）。
> - `toEntity(payload, X.class)` 已是 `handleCatalog`/`handleDeviceInfo` 的既有用法（§1.4 验证），照搬即可。

```java
case "Response.DeviceStatus":
    handleDeviceStatus(event);
    break;
case "Response.PtzPosition":
    handlePtzPosition(event);
    break;
case "Response.PresetQuery":
    handlePreset(event);
    break;
case "Response.Config":
    handleConfig(event);          // DeviceConfigResponse → extend.config
    break;
case "Response.ConfigDownload":
    handleConfigDownload(event);  // DeviceConfigDownloadResponse → extend.configDownload（不同实体）
    break;
case "Response.RecordInfo":
    handleRecordInfo(event);
    break;
// SdCardStatus/HomePosition/CruiseTrack* 视优先级补，或暂留 log
```

```java
private void handleDeviceStatus(DeviceEvent event) {
    // payload Map 即 DeviceStatus 扁平化（forwarder JSON 往返），直接 toEntity
    DeviceStatus st = toEntity(event.payload(), DeviceStatus.class);
    if (st == null) return;
    deviceManager.patchExtendInfo(event.deviceId(), ext -> ext.setDeviceStatus(JSON.toJSONString(st)));
    publishVisual("device.status", event.deviceId(), "online", st.getOnline());  // getter getOnline()；JSON key 大小写 S3 抓样本确认
    log.info("设备状态回填, deviceId={}", event.deviceId());
}

private void handlePtzPosition(DeviceEvent event) {
    PTZPositionResponse pos = toEntity(event.payload(), PTZPositionResponse.class);
    if (pos == null) return;
    deviceManager.patchExtendInfo(event.deviceId(), ext -> ext.setPtzPosition(JSON.toJSONString(pos)));
    publishVisual("device.ptz_position", event.deviceId(), "pan", pos.getPan());
}
// handlePreset / handleConfig / handleConfigDownload 同构（各对应 1.5A 的实体类）
```

（其余 handlePreset/handleConfig/handleConfigDownload 同构。）

**B3.4 录像查询结果缓存（`Response.RecordInfo` → `DeviceRecord`）**

录像查询返回的是"列表数据"（`DeviceRecord.recordList[RecordItem]`，注意 `RecordItem.startTime/endTime` 是 **String**，§1.5B），前端要分页展示，**不能塞 extend**。两选：
- A（推荐，本期）：**缓存方案** —— 以 `(deviceId, sn)` 为 key（`DeviceRecord` 无 channelId 字段，sn 取实体内或 `correlationId`），把 `recordList` 存 `RedisCache`（FastJSON 字符串，TTL 如 10 分钟）；前端先发 `/record/query`（S2）触发查询，再轮询 `/record/result?deviceId=&sn=` 读缓存。无新表、无 schema 变更，RecordItem 时间原样存 String 前端直接展示。
- B（重方案，不选本期）：新增 `tb_record_info` + `RecordInfoManager` + 完整 CRUD。

选 A。新增 `RecordInfoCacheManager`（manager）封装存取；`handleRecordInfo` 写缓存；S2 的 `/record/query` 端点配套加 `/record/result` 读端点（归属 S2/S3 交界，实现放 S3）。

### 5.4 S3 验收

- 5 类核心响应（DeviceStatus/PtzPosition/PresetQuery/Config/RecordInfo）落库或缓存成功，`tb_device.extend` 子字段可见。
- `device.*` SSE 推送（复用 `publishVisual`，前端协议台/设备页可观测）。
- 测试：`Gb28181ProtocolHandlerTest`（纯 Mockito，mock manager，喂构造的 payload Map，验证落库调用）；`RecordInfoCacheManagerTest`。
- **不破坏** Catalog/DeviceInfo/Session 既有处理（回归）。

---

## 6. S4（规划，本期可不做）— 前端设备管理页

`vue-vben-admin/apps/web-antd`，严格对齐后端契约（逐字段核对，不发明字段/端点；新增字段先登记 `.cursor/rules/project-rule.mdc`）。**核心是复用协议验证台已沉淀的资产（§1.7.2），新增的只是设备列表页与 API 提取**：

**直接复用（import 即用，勿重写）：**
- `components/MediaPlayer.vue`（多格式播放器门面）：点播按钮调 `/live/start` 拿 `playUrls` → 传入即播（GB 直播建议 `format="hls"`）。
- `composables/useSseEvents.ts`：订阅 `device.*` 实时刷新列表状态。
- `views/protocol-lab/components/SipTimeline.vue`：设备详情事件时间线。

**提取共享（从 ServerPanel 抽出，供设备页与协议台共用）：**
- `components/PtzControl.vue`：PTZ 方向盘（props: deviceId/channelId/speed/disabled/@command），调 `/api/v1/ptz/control`。
- `composables/useDeviceList.ts`：设备列表 upsert 逻辑（设备页从 DB `/device/getPage` 拉初始 + SSE 增量；ServerPanel 纯 SSE）。
- `api/device.ts`：从 `protocol-lab.ts` 迁出 `ptzControl/queryCatalog/queryDeviceInfo/rebootDevice/liveStart`，再加 `getPage`（筛选）+ S2 新增支链端点；Lab 专属（`labRegister/labPush*`）留在 `protocol-lab.ts`。

**设备页新增：**
- `views/device/index.vue`：列表（筛选栏 deviceId/名称/状态/类型(type)/时间范围）+ 状态标签 + 通道数列。
- `views/device/components/DeviceDetail.vue`：操作面板（复用 `PtzControl` + 查询按钮组 + 录像查询弹窗 + 配置下载）+ `SipTimeline`。
- `router/routes/modules/device.ts`：路由菜单。
- i18n：`device.entity.action` key 模式。

> **去重红线**：实时点播 / PTZ / 录像回放前端**只调已存在的 `/live/*`、`/ptz/*`、`/playback/*`**，与 ServerPanel 同源；播放器、SSE、时间线一律复用，**不另写一套**。设备页与协议台测试 tab 的"平台侧命令区"是同一组操作，抽 `PtzControl`/`DeviceControlPanel` 公共组件共用。

---

## 7. 实施顺序与构建注意

### 7.1 推荐顺序（每步独立编译可测）

1. **S1**（web `DevicePageReq` + `DeviceListResp` 无依赖；web 控制器 + manager `getPage` 重载）→ `mvn test -Dtest=DeviceManagerTest,DeviceControllerTest`
2. **S2**（client 接口扩展 → service 实现 → web 端点）→ `GbDeviceCommandServiceTest`
3. **S3**（manager patchExtendInfo → integration handler）→ `Gb28181ProtocolHandlerTest`

### 7.2 构建坑（来自 [[voglander-1.0.6-protocol-lab-progress]] / [[voglander-web-tests-use-installed-integration-jar]]）

- **改了 `voglander-client` / `voglander-integration` / `voglander-service`**：voglander-web 测试从 `~/.m2` 的兄弟模块 jar 加载，**必须先全量 `mvn clean install -DskipTests`** 再跑 web 测试，否则跑的是旧字节码（典型报错"找不到 XxxDTO 类文件"）。`-pl x -am install` 只到目标模块，不重建下游。
- **IDE 易残留旧 class**：跑测试前先 `mvn clean`（[[voglander-test-infra-gotchas]]）。
- **integration 模块源码级别为 Java 17**（已核对 `voglander-integration/pom.xml`：第 15-16 行 `maven.compiler.source=17`、第 99-100 行 `<source>17</source>`/`<target>17</target>`，2026-06-13 复核）：handler/command **可用** Java16+ `instanceof X x` 模式匹配。
  > 更正：旧文档/记忆曾称 integration 钉 `<source>9>` 禁用模式匹配，**经反编译 + pom 实地核对为误**，全模块统一 17。**1.0.7 ARCHITECTURE.md §3.1 表格、§18#9、§19.2 仍记录"钉 source 9"，属过时错误，应同步订正**（见 §0.5）。
- **分层与命名红线（ARCHITECTURE.md §5.1/§5.2）**：handler 改动**禁 import 任何 sip-gateway 框架类型**（`GatewayEvent`/`BusinessNotifier`，§18#15）；**禁改** `ShardDispatcher`/`InboundEventDispatcher`（§18#14）；出站门面**禁直调 `ServerCommandSender`**，走 `dispatchEnvelope`（§18#6）。
- **SPI 红线（ARCHITECTURE.md §8.1）**：GB 专属支链方法落 `Gb28181DeviceCommandService` 子接口，**不进协议无关 SPI `DeviceCommandService`**（见 §4.3 方向 A）。
- **Redis 集成测试**：运行时探测，不可用自动跳过；S3 录像缓存测试需 `brew services start redis` 或 mock `RedisCache`。

### 7.3 分层与规范红线（强制）

- Web 入参 **必须** `*Req`（禁止直收 DTO/DO）；出参 Manager 返回 DTO → `WebAssembler.dtoToVo` → VO。
- Manager 对外 **只用 DTO**，不暴露 DO；查询用带 `condition` 的 `LambdaQueryWrapper`，**禁止** `new LambdaQueryWrapper<>(entity)`。
- 简单单表用 `IService`/`BaseMapper` 基础方法，**禁止自定义 SQL**（通道 count group by 若必须，才考虑 Mapper）。
- JSON 全程 FastJSON2；类型转换走正反序列化；时间 DO/DTO 用 `LocalDateTime`、VO 用 Unix 毫秒。
- 入站 handler 在分片单线程，DB 写无需加锁；异步/Hook/跨线程**不用** `@Transactional`。

---

## 8. 文件改动清单（汇总）

| # | 模块 | 文件 | 动作 | Sprint |
|---|------|------|------|--------|
| 1 | web | `api/device/req/DevicePageReq.java` | 新增（分页筛选请求；**改名避开 client 既有 `DeviceQueryReq`(qo)**，§3.2 B1.1） | S1 |
| 2 | web | `api/device/resp/DeviceListResp.java` | 新增（total+items，§6.6 `*Resp` 规约） | S1 |
| 3 | web | `api/device/assembler/DeviceWebAssembler.java` | 加 `pageReqToQueryDto`（毫秒→LocalDateTime，§5.2）；现仅有 create/update Req→DTO，无 query 转换 | S1 |
| 4 | web | `api/device/DeviceController.java` | 加 `/getPage` 模板端点（§14.1 Essential）；旧端点标 `@Deprecated` | S1 |
| 5 | web | `api/device/vo/DeviceVO.java` | 加 `channelCount`（现无此字段）+ ExtendInfoVO 子字段 | S1/S3 |
| 6 | manager | `domaon/dto/DeviceQueryDTO.java` | 新增（对外 DTO，`LocalDateTime`，§5.1/§5.2） | S1 |
| 7 | manager | `manager/DeviceManager.java` | **扩展现有 `getPage` 支持多条件**（新增 `getPage(DeviceQueryDTO,...)` 重载，非新建 getPageByCondition，§6.2）；新增 `patchExtendInfo`（现无） | S1/S3 |
| 8 | manager | `domaon/dto/DeviceDTO.java` | ExtendInfo 加快照子字段 | S3 |
| 9 | manager | `manager/RecordInfoCacheManager.java` | 新增（录像结果缓存，`RedisCache`，§13.1） | S3 |
| 10 | client | `service/device/Gb28181DeviceCommandService.java` | **新增 GB 专属子接口**（extends `DeviceCommandService`），声明 ~9 GB 专属支链方法（status/preset/mobilePosition/config 下载+下发/record 控制/alarm 查询+控制/broadcast）。**协议无关 SPI `DeviceCommandService.java` 不动**（§4.3 方向 A / §8.1） | S2 |
| 10b | client | `service/device/DeviceCommandService.java` | **不加 GB 专属方法**；仅媒体方法语义收口（`stopPlay` 入参→streamId，是 SPI 方法须评估契约波及，§1.6），不新增 stopByStreamId | S2 |
| 11 | client | `domain/device/qo/Device{Config,RecordQuery,RecordControl,AlarmQuery}Req.java` | 新增支链请求（命令入参，client `qo` 包 `*Req`） | S2 |
| 11b | client | `domain/device/qo/DevicePlayReq.java` | 加 `channelId` 字段（MediaPlayService 以 deviceId+channelId 生成 streamId，缺口 C，§1.6） | S2 |
| 12 | service | `command/impl/GbDeviceCommandService.java` | **改 `implements Gb28181DeviceCommandService`**；实现新支链方法（委托 6 命令，走 `dispatchEnvelope` + `Gb28181CommandType`，§7.3/§8.2/§18#6）；`startPlay` 残桩删除或委托 `MediaPlayService`（§1.6，无人调用）；移除 alarm/record bean 的 `@SuppressWarnings` | S2 |
| 13 | common | `exception/ServiceExceptionEnum.java` | 视需补错误码（`LIVE_STREAM_NOT_FOUND` **已存在 700004**，无需新增；§18#12 异常先登记原则同 1.0.3） | S2 |
| 14 | web | `api/device/controller/DeviceCmdController.java` | 现有 4 端点 `Map`→`*Req` 化；**补支链端点**（query-status/preset/mobile-position、config download/set、record start/stop、alarm query/control、broadcast），**注入 `Gb28181DeviceCommandService`（不经 `DeviceAgreementService`）**。**PTZ 调既有 `PtzController /api/v1/ptz`、点播调 `/live/*`、回放调 `/playback/*`——均不在此重复造** | S2 |
| 15 | integration | `wrapper/gb28181/handler/Gb28181ProtocolHandler.java` | 补 6 响应 handler（DeviceStatus/PtzPosition/Preset/Config/ConfigDownload/RecordInfo）；**禁 import sip-gateway 类型，禁改分片层**（§18#14/#15） | S3 |

> 实测实体类（§1.5A，2026-06-11 反编译 **1.8.1** jar 复核全部命中，**⚠️ 1.8.2 落地时重核**）：`DeviceStatus`/`PTZPositionResponse`/`PresetQueryResponse`(内 `PresetQueryResponse$PresetItem{presetId,presetName}`)/`DeviceConfigResponse`/`DeviceConfigDownloadResponse`(11 子项: basicParam/videoParamOpt/svacEncodeConfig/svacDecodeConfig/videoParamAttribute/videoRecordPlan/videoAlarmRecord/pictureMask/frameMirror/alarmReport/osdConfig)/`DeviceRecord`(内 `DeviceRecord$RecordItem`)，均 `io.github.lunasaw.gb28181.common.entity.response` 包。forwarder `io.github.lunasaw.sipgateway.gb28181.forwarder.Gb28181EventForwarder.emit()` 确为 `JSON.toJSONString→parseObject(Map)`。

**不改**：6 个 `VoglanderServerXxxCommand`（已完整）、`ServerCommandSender`/`notifier`/`ShardDispatcher`（链路已通）、**已跑通的 `PtzController`/`LivePlayController`/`PlaybackController`**（只在其外补支链端点，不动既有）、`sip-proxy`/`zlm-starter`（无需上游重建）。

> **S4 前端改动（复用优先，本期可不做）**：复用 `components/MediaPlayer.vue` / `composables/useSseEvents.ts` / `views/protocol-lab/components/SipTimeline.vue`（直接 import）；提取 `components/PtzControl.vue`、`composables/useDeviceList.ts`、`api/device.ts`（从 `protocol-lab.ts` 迁通用命令）；新增 `views/device/index.vue`、`views/device/components/DeviceDetail.vue`、`router/routes/modules/device.ts`、i18n。详见 §1.7.2 / §6。

---

## 9. 风险与回归点

- **R1 旧 `DeviceController` 端点**：前端可能仍调 `/list`/`/pageListByEntity`。新增 `/getPage` 不删旧端点，标 `@Deprecated`，前端切换后再清理。
- **R2 `extend` 字段膨胀**：多个响应快照塞 `extend` JSON，单设备 extend 可能变大。监控字段大小；若 RecordInfo 之外仍有大列表型响应，再考虑独立表。
- **R3 录像缓存 TTL**：缓存方案下"查询→读结果"有时序窗口；前端需轮询或配合 SSE `device.recordinfo` 通知。TTL 设置过短会丢结果，过长占内存，建议 10 分钟。
- **R4 入站响应 payload schema（已大幅降级）**：§1.5 已确认 forwarder `emit()` 做 `JSON.toJSONString(实体)` 往返，payload Map 即实体扁平化，`toEntity(payload, X.class)` 直接可用；字段名已按 1.8.1 实测列出（§1.5A）。**残留风险仅**：`DeviceStatus.Online/Result` 首字母大写、`RecordItem.startTime` 为 String，落地时用 `SipMessageTracer.recv` 抓一条真实样本最终确认即可。
- **R5 全量 install 遗漏**：跨模块改动后忘记 `mvn clean install` → web 测试跑旧 jar 误判。CI/本地都先全量装。

---

## 10. 验收总览（Definition of Done）

- [ ] S1：`/device/getPage` 多条件筛选 + VO 含 channelCount/statusName，测试绿。
- [ ] S2：GB 专属支链（status/preset/mobile-position、config 下载+下发、record 控制、alarm 查询+控制、broadcast）落 **`Gb28181DeviceCommandService` 子接口** + `*Req` 端点，envelope 实发，测试绿；**协议无关 SPI `DeviceCommandService` 未被污染（仍 11 方法），`DeviceAgreementService` 路由不回归**；`DeviceCmdController` 4 端点 Req 化；**已有 PtzController/LivePlayController/PlaybackController 不回归**。
- [ ] S3：5 类核心响应落库/缓存 + SSE，既有 Catalog/Session 不回归，测试绿。
- [ ] 全量 `mvn clean install` SUCCESS；新增测试覆盖关键分支。
- [ ] Swagger 文档完整；不破坏前端现有调用。
- [ ] （S4 若做）前端设备页**最大化复用**协议验证台资产（MediaPlayer/useSseEvents/SipTimeline，提取 PtzControl/api/device.ts），严格对齐契约，门禁 `pnpm check` 绿。
