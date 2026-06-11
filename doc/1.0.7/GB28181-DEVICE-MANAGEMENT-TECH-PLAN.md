# GB28181 设备管理 — 平台端完整设备操作技术方案

> 版本 1.0.7 · 分支 `0608_dev` · 文档日期 2026-06-11 · **本文为实施方案，按此分阶段 TDD 落地**
> 角色定位：voglander 在本方案中扮演 **GB28181 平台端（上级 / SIP Server）**，对下级设备做完整管理：列表展示与筛选、生命周期接入、主动指令下发、被动响应回填。
> 依赖版本：**sip-gateway / gb28181 1.8.1**（pom 已钉，`0be6487` 从 1.8.0 升级）、zlm 1.0.11。本方案 §1.5 全部事实已对 1.8.1 jar 反编译核对。
> 关联：[[sip-gateway-1.8.0-migration]]（命令/事件 API 事实）、`doc/1.0.6/GB28181-LAB-DEVICE-REGISTER-TARGET-PLAN.md`（模拟设备侧，互为对端）。

---

## 0. TL;DR

需求三句话：
1. **GB28181 设备管理**：设备列表支持多条件筛选（deviceId / 名称 / 状态 / 协议 / IP / 时间范围）+ 基础展示（在线状态、心跳、注册时间、通道数）。
2. **完整作为 GB28181 平台端**：平台端可对下级设备下发**全部** GB28181 指令（查询 / PTZ / 录像 / 报警 / 配置 / 媒体），不再只暴露 4 个。
3. **对设备的完整操作响应**：下级设备回上来的**全部** `Response.*` 事件（DeviceStatus / RecordInfo / PtzPosition / Config / PresetQuery …）要落库或回填，不再"只打日志"。

现状一句话总结：**底层能力齐全，上层暴露残缺**。

| 维度 | 现状 | 缺口 |
|------|------|------|
| 出站指令 bean（integration） | 6 个 Server Command 全部就绪（Device/Ptz/Config/Record/Alarm/Media，**65 方法**，已源码核对） | — 已完整 |
| 服务门面 `DeviceCommandService` | 仅暴露 **10 方法**（queryChannel/queryDevice/queryDeviceInfo/queryCatalog/ptzControl/startPlay/startPlayback/stopPlay/reboot/controlPlayback） | ❌ 缺 status/preset/mobilePosition/config 下载+下发/record 查询控制/alarm 查询控制/broadcast |
| Web 控制器 `DeviceCmdController` | 仅 4 端点（query-catalog/query-info/reboot/record），入参 `Map<String,String>`，**无 `/ptz`** | ❌ 无 PTZ/录像/报警/配置端点；违反 `*Req` 规范 |
| Web 控制器 `DeviceController` | 旧式：直接收 `DeviceDO`、返回裸 `Page`、无模板方法 | ❌ 无条件筛选 `*QueryReq`、无 `*ListResp`、不符模板方法规范 |
| 入站响应 `Gb28181ProtocolHandler` | Catalog/DeviceInfo 已落库；Lifecycle/Keepalive/Alarm 已处理 | ❌ 14 个 `Response.*` 仅 `log.info`，无落库/无回填/无 SSE |
| 设备筛选模型 `DeviceQueryReq`(client qo) | 仅 `deviceId` 一个字段 | ❌ 无法支撑列表筛选 |

改动覆盖 **4 个模块**：`voglander-client`（DTO/接口扩展）、`voglander-service`（门面补全）、`voglander-integration`（入站响应回填）、`voglander-web`（���制器重构 + 筛选）。
**不改** `voglander-integration` 的 6 个 Server Command（已完整），**不改** `sip-proxy` / `zlm-starter`（无需上游重建）。

---

## 1. 现状链路梳理（关键，理解后再动手）

### 1.1 出站：平台 → 设备（指令下发）

```
前端 → DeviceCmdController(Map入参)        ← ❌ 残缺：仅 4 端点（无 /ptz）
        │
        ▼
DeviceCommandService 门面(@Service "GbDeviceCommandService")  ← ❌ 残缺：仅暴露 10 方法
        │  委托
        ▼
6 个 VoglanderServerXxxCommand(integration)  ← ✅ 完整：65 方法全部就绪
        │  dispatchEnvelope(type, deviceId, payload)
        ▼
ServerCommandSender(实例 Bean, 按 deviceId) → sip-gateway 1.8.1 → 下级设备
```

**结论**：出站缺口纯粹是"门面 + 控制器没把底层方法透出来"，**不需要碰协议层**。

### 1.2 入站：设备 → 平台（事件回调）

```
sip-gateway → VoglanderBusinessNotifier.notify(GatewayEvent)  [@Async sipNotifierExecutor]
        │  轻量翻译 GatewayEvent → DeviceEvent，立即归还 SIP 线程
        ▼
ShardDispatcher.dispatch(DeviceEvent)  [按 deviceId 分片，单线程 FIFO]
        ▼
InboundEventDispatcher  [按 protocol 段路由]
        ▼
Gb28181ProtocolHandler.handle(DeviceEvent)  ← switch over ~35 事件
        ├── Lifecycle.{Register/Online/Offline/RemoteAddressChanged}  ✅ 已落库
        ├── Notify.{Keepalive/Alarm/...}                              ✅ 已处理
        ├── Response.Catalog → batchUpsertWithStatus                  ✅ 已落库
        ├── Response.DeviceInfo → updateDeviceInfo                    ✅ 已回填 extend
        ├── Response.{DeviceStatus/RecordInfo/PtzPosition/Config/     ❌ 仅 log.info
        │            PresetQuery/SdCardStatus/HomePosition/...}            （14 个事件）
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

**方案整体可行**，三层缺口都是"上层透出/回填"问题，不触碰协议层，不需上游重建（sip-proxy/zlm 不动）。验证中**纠正了初稿的 4 处事实错误 + 补充了 3 个原方案漏掉的功能缺口**，全部并入下文对应 Sprint。

> **2026-06-11 二次源码审核（落地前复核）**：对四层（数据模型 / 出站链路 / 入站 handler / 1.8.1 实体）逐一源码+反编译核对，结论 **方案可实施**，并据此修正：①§7.2 integration 源码级别实为 **17**（非 9，模式匹配可用）；②§4.2 `/ptz` 端点**不存在**需新建（非"改 Map"）；③§3/§8 `getPageByCondition` 改为**扩展现有 `getPage`**；④门面实为 **10 方法**、底层实为 **65 方法**（非 ~11/~50）。唯一残留实测盲点：`DeviceStatus` 字段 JSON key 大小写，S3 落地抓真实样本确认。

| 验证项 | 结论 |
|--------|------|
| 27 个事件 type 是否真由框架发出 | ✅ 全部确认：反编译 `gateway-gb28181-1.8.1.jar` 的 `Gb28181EventForwarder`，常量池含全部 5 Lifecycle + 7 Notify + 16 Response/含 Catalog + 7 Session |
| `Response.*` payload 结构（原 R4 最大风险） | ✅ **已消除风险**：forwarder `emit()` = `JSON.toJSONString(响应实体)` → 回解析成 `Map` → 塞 `GatewayEvent.payload`。**payload Map 就是响应实体扁平化本身**，现有 `handleCatalog`/`handleDeviceInfo` 的 `toEntity(payload, X.class)` 即正确用法，照搬即可 |
| 6 个 Server Command 底层方法是否齐全 | ✅ 齐全（**65 方法**，已逐 bean 源码核对：Device 6 / Config 10 / Ptz 20 / Media 13 / Alarm 10 / Record 6），门面只透出 10 个 |
| `DeviceCommandService` 接口/实现可扩展性 | ✅ 单实现 `GbDeviceCommandService`（`@Service("GbDeviceCommandService")`），扩展无副作用；`alarmCommand`/`recordCommand` 已注入但标 `@SuppressWarnings("unused")`，本期启用即移除 |

### 1.5 验证中纠正的事实 & 新发现的缺口（���键）

**A. 实体类名与字段——初稿写错，以下为 1.8.1 反编译实测（按此实现）：**

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

**C. 新发现缺口 2 — 点播/回放 callId 异步、前端拿不到（影响"完整操作"闭环）。** `DeviceCommandService.startPlay/startPlayback` 当前**返回 null**（注释明示 callId 由 `onInviteOk` 异步产生）。`stopPlay(callId)` 需要 callId，但前端发起 play 时拿不到 → **停流闭环断裂**。`MediaSessionManager` 已有 `getByStreamId(streamId)` / `getByCallId(callId)`。**补救（并入 S2）**：play 请求由前端生成稳定 `streamId` 传入（或后端生成回传），停流走 `controlPlayback`/新增 `stopByStreamId(streamId)` 反查 callId，而非依赖前端持有 callId。这是"完整设备操作"必须补的一环。

**D. 新发现缺口 3 — 列表筛选字段与 DO 不一致。** `DeviceVO` 有 `subType/protocol/protocolName`，但 **`DeviceDO` 只有 `type`**（无 subType/protocol 列）。S1 筛选**只能按 `type` 落 DB**；`subType/protocol` 是 VO 派生展示字段，**不可作 DB 筛选条件**（否则 wrapper 引用不存在的列编译失败）。初稿 §0/§3 "协议筛选"应明确为按 `type` 筛选。

**E. 确认 — 通道数无现成 count 接口。** `DeviceChannelService` 仅 `extends IService`，无 `count group by device_id`。S1 的 `channelCount` 需：要么按当前页 deviceId 集合调 `IService.count(wrapper)` 逐个（页大小通常 ≤20，可接受）、要么新增一个 group-by 的 Mapper 方法（属"复杂查询"例外，允许自定义 SQL）。**本期选逐个 count（页内 ≤20 次，简单且不破"禁自定义 SQL"原则）**，若实测慢再换 group-by Mapper。

---

## 2. 设计目标与范围切分

按"先展示、再下发、后回填"三层推进，每层独立可验收：

| Sprint | 主题 | 模块 | 验收 |
|--------|------|------|------|
| **S1** | 设备列表筛选 + 基础展示 | client / web | `/device/getPage` 支持多条件分页（按 `type` 筛选，非 subType/protocol），VO 含通道数、状态名 |
| **S2** | 平台端完整指令下发 + 停流闭环 | client / service / web | 6 大指令域全部有 REST 端点，入参用 `*Req`，门面方法补齐；play 回传 streamId、停流走 streamId 反查（缺口 C） |
| **S3** | 完整设备操作响应回填 | manager / integration | 5 类核心 `Response.*` 落库/回填 + SSE，录像结果走缓存（`RecordInfoCacheManager`） |
| **S4**（可选） | 前端设备管理页 | vue-vben-admin | 列表筛选页 + 操作面板，严格对齐后端契约 |

下文 S1–S3 为本期必做（后端），S4 列规划。

---

## 3. S1 — 设备列表筛选 + 基础展示

### 3.1 问题

- `DeviceQueryReq`(client qo) 只有 `deviceId`，无法筛选。
- `DeviceController` 旧式：`list(DeviceDO)` / `pageListByEntity(...,DeviceDO)` 直接收 DO，违反"入参必须用 Web 层 `*Req`"；返回裸 MyBatis `Page<DeviceVO>` 而非 `*ListResp`。
- 无"全量分页条件查询"入口（项目模板方法规范要求）。

### 3.2 改动

**B1.1 新增 `DeviceQueryReq`（web 层）** — `web/api/device/req/DeviceQueryReq.java`

筛选维度（全部可选，走 `LambdaQueryWrapper` 带 condition 链式）：

```java
@Data
public class DeviceQueryReq implements Serializable {
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

**B1.2 `DeviceManager` 条件分页方法** — 用带 `condition` 的 `LambdaQueryWrapper`（强制规范，禁止 `new LambdaQueryWrapper<>(entity)`）：

> **已有方法提示**：`DeviceManager.getPage(DeviceDTO, int, int)`（[现有 L376]）已是条件分页，但其条件来源是 DTO 字段、且不覆盖时间范围/like。**优先扩展现有 `getPage` 支持多条件**（接受 `DeviceQueryDTO`），而非另起一个并行的 `getPageByCondition`，避免两个分页入口。下方示例方法名保留 `getPageByCondition` 仅作签名说明，落地时合并进 `getPage` 重载或替换其条件构建段。

```java
public Page<DeviceDTO> getPageByCondition(DeviceQueryDTO q, int page, int size) {
    LambdaQueryWrapper<DeviceDO> qw = new LambdaQueryWrapper<>();
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
    // 复用现有 pageQuery(page,size,wrapper) 的 DO→DTO 装配
    ...
}
```

> Manager 对外用 DTO：新增 `DeviceQueryDTO`（manager.domaon.dto），`LocalDateTime` 时间字段；Web 层 `DeviceQueryReq`（Unix 毫秒）经 `DeviceWebAssembler.queryReqToDto()` 转换（毫秒→`LocalDateTime`，复用 DTO 既有 `xxxToEpochMilli` 的反向）。

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
    @RequestBody(required = false) DeviceQueryReq queryReq,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size) {
    DeviceQueryDTO dto = deviceWebAssembler.queryReqToDto(queryReq);
    Page<DeviceDTO> p = deviceManager.getPageByCondition(dto, page, size);
    DeviceListResp resp = new DeviceListResp();
    resp.setTotal(p.getTotal());
    resp.setItems(p.getRecords().stream().map(DeviceVO::convertVO).collect(toList()));
    return AjaxResult.success(resp);
}
```

**B1.5 基础展示增强 — `DeviceVO` 补通道数**（缺口 E）：
列表需展示"该设备下通道数"。`DeviceChannelService` 仅 `extends IService`，**无 group-by count 接口**。方案：
- A（推荐，本期）：`getPageByCondition` 拿到当前页 DTO 列表后，对页内 deviceId（通常 ≤ 页大小，默认 ≤10）逐个 `deviceChannelService.count(new LambdaQueryWrapper<DeviceChannelDO>().eq(DeviceChannelDO::getDeviceId, id))`，回填 VO `channelCount`。页内 ≤10 次轻量 count，符合"禁自定义 SQL"原则。
- B（实测慢再换）：新增 `DeviceChannelMapper` 的 `count group by device_id`（属"复杂查询"例外，允许自定义 SQL）。

`DeviceVO` 新增字段 `private Integer channelCount;`（已有 `statusName/typeName` 派生展示，无需改）。

### 3.3 S1 验收

- `POST /api/v1/device/getPage` 各条件组合返回正确（含时间范围、状态、名称模糊）。
- VO 返回 `statusName`（在线/离线）、`keepaliveTime`（毫秒）、`channelCount`。
- 旧端点不回归（保留）。
- 测试：`DeviceManagerTest`（集成，`BaseTest`）覆盖 `getPageByCondition` 各筛选分支；`DeviceControllerTest`（纯 Mockito 单元）覆盖 getPage 装配。

---

## 4. S2 — 平台端完整指令下发

### 4.1 问题

底层 6 个 Server Command 共 ~50 方法已就绪，但 `DeviceCommandService` 门面只透出 ~11 个，Web 端点只有 4 个且入参是 `Map`。要"完整作为平台端"，需把指令域**系统性**透出。

### 4.2 指令域盘点（底层已有 → 门面需补 → 端点需建）

| 指令域 | 底层方法（已就绪，integration） | 门面 `DeviceCommandService` | Web 端点 |
|--------|--------------------------------|------------------------------|----------|
| 设备查询 | queryDeviceInfo/Status/Catalog/Preset/MobilePosition（Device cmd 6 方法） | ✅info/catalog；❌status/preset/mobilePosition | 补 `/query-status` `/query-preset` `/query-mobile-position` |
| PTZ | controlDevicePtz / move*/zoom*/stop（带/不带 speed，Ptz cmd 20 方法） | ✅ptzControl（方向+speed） | ❌ **新建 `/ptz`**（控制器现无此端点；门面 `ptzControl` 已就绪，入参 `DevicePtzReq`） |
| 配置 | configDevice / downloadBasic/Video/AudioConfig / rebootDevice（Config cmd 10 方法） | ✅reboot；❌config 下载、configDevice | 补 `/config/download` `/config/set` |
| 录像 | queryDeviceRecord(long/Date/String 三重载) / start/stopDeviceRecord（Record cmd 6 方法） | ❌ 全缺（bean 已注入但 `@SuppressWarnings("unused")`） | 补 `/record/query` `/record/start` `/record/stop` |
| 报警 | queryDeviceAlarm / controlDeviceAlarm / queryToday/Recent...（Alarm cmd 10 方法） | ❌ 全缺（bean 已注入但 `@SuppressWarnings("unused")`） | 补 `/alarm/query` `/alarm/control` |
| 媒体 | inviteRealTimePlay / invitePlayBack / controlPlayBack / sendBye / sendBroadcast（Media cmd 13 方法） | ✅play/playback/stop/controlPlayback；❌broadcast | 补 `/broadcast`（实时点播在媒体模块已有，不重复） |

### 4.3 改动

**B2.1 扩展 `DeviceCommandService` 接口**（client）+ `GbDeviceCommandService` 实现（service）

新增门面方法（协议无关，委托对应 Server Command）：

```java
// 查询补全
ResultDTO<Void> queryDeviceStatus(String deviceId);
ResultDTO<Void> queryPreset(String deviceId);
ResultDTO<Void> queryMobilePosition(String deviceId, String interval);
// 配置
ResultDTO<Void> downloadConfig(String deviceId, String configType);   // BASIC/VIDEO/AUDIO
ResultDTO<Void> setDeviceConfig(DeviceConfigReq req);                  // name/expiration/...
// 录像
ResultDTO<Void> queryRecord(DeviceRecordQueryReq req);                 // deviceId + start/end(毫秒)
ResultDTO<Void> controlRecord(String deviceId, boolean start);
// 报警
ResultDTO<Void> queryAlarm(DeviceAlarmQueryReq req);
ResultDTO<Void> controlAlarm(String deviceId, String method, String type);
// 媒体
ResultDTO<Void> broadcast(String deviceId);
```

实现示例（纯委托，校验由底层 `validateDeviceId` 兜底，门面只做参数翻译）：

```java
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
```

> 注意：`alarmCommand`/`recordCommand` 当前在 `GbDeviceCommandService` 标了 `@SuppressWarnings("unused")` —— 本期它们真正被用上，移除注解。

**B2.2 入参 Req 化**（client domain qo，新增）
- `DeviceConfigReq`（deviceId/name/expiration/heartBeatInterval/heartBeatCount）
- `DeviceRecordQueryReq`（deviceId/startTime/endTime，Unix 毫秒；门面内 `Instant.ofEpochMilli` 转 long 秒或直接用 `long` 重载）
- `DeviceAlarmQueryReq`（deviceId/startTime/endTime/startPriority/endPriority/alarmMethod/alarmType）

**B2.3 重构 `DeviceCmdController`**（web）→ 全量端点 + `*Req`

把 `Map<String,String>` 全部替换为类型化 `*Req`，按指令域分组（仍在 `/device-cmd` 下，或拆 `/device-cmd/ptz` 等子路径）。所有写操作记录 `[AUDIT]` 日志（现有 reboot 已示范）。统一返回 `ResultDTO.isSuccess()→AjaxResult`。

```java
@PostMapping("/ptz")
@Operation(summary = "PTZ 控制（记录操作日志）")
public AjaxResult<Boolean> ptz(@Valid @RequestBody DevicePtzReq req, HttpServletRequest request) {
    log.info("[AUDIT] ptz device={}, control={}, ip={}", req.getDeviceId(), req.getControl(), request.getRemoteAddr());
    ResultDTO<Void> r = deviceCommandService.ptzControl(req);
    return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
}
```

**B2.4 `ServiceExceptionEnum` 补充**：录像/报警/配置下发失败若需专用错误码，先在 `ServiceExceptionEnum` 补类型（项目强制：抛 `ServiceException` 前确认枚举存在）。已有 `DEVICE_OPERATION_FAILED`/`PARAM_ERROR` 可复用，新增 `RECORD_QUERY_FAILED`/`ALARM_QUERY_FAILED`/`CONFIG_DOWNLOAD_FAILED` 视需要。

**B2.5 停流闭环补救（缺口 C，必做）**：`startPlay/startPlayback` 返回 null callId，前端无法据此停流。方案：
- play 请求 `DevicePlayReq` 增 `streamId`（前端生成稳定主键，与 1.0.6 媒体模块约定一致；若已有则复用）；`mediaCommand.inviteRealTimePlay` 下发时把 streamId 关联到会话（`MediaSessionManager.onInviteOk` 落 streamId，已支持 `getByStreamId`）。
- 新增门面 `ResultDTO<Void> stopByStreamId(String streamId)`：`getByStreamId` 反查会话 → 取 callId → `mediaCommand.sendBye(callId)`；会话不存在返回 `LIVE_STREAM_NOT_FOUND`。
- Web 停流端点改收 `streamId`（而非 callId），与现有 `controlPlayback(streamId,...)` 入参一致。
> 注：若媒体点播端点已在 1.0.5/1.0.6 媒体模块实现，则本条仅补"停流按 streamId"这一环，不重复造点播端点（避免与现有 `/media/*` 冲突，落地前先核对现状）。

### 4.4 S2 验收

- 每个指令域至少一个端点联通（envelope 实际发出，`SipMessageTracer` 有 send 记录）。
- 入参全部 `*Req` + `@Valid`，无 `Map<String,String>`。
- 门面方法有对应单测（`GbDeviceCommandServiceTest`，纯 Mockito，mock 6 个 command bean，验证委托正确）。
- Swagger 文档完整（`@Tag`/`@Operation`/`@Parameter`）。

---

## 5. S3 — 完整设备操作响应回填

### 5.1 问题

`Gb28181ProtocolHandler` 中 16 个 `Response.*` 分支仅 `log.info(...)`（不含已实现的 Catalog/DeviceInfo），平台收到了设备应答却"不记账"。"完整设备操作响应"要求：**有业务价值的响应必须落库或回填，并按既有约定推 SSE**。本期落地 **5 类核心 + ConfigDownload**，低优先（SdCard/HomePosition/CruiseTrack）可暂留 log 分期补。

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

> 入站在分片单线程执行，同一 deviceId 串行，读-改-写无并发风险（[[sip-gateway-1.8.0-migration]] 已述分片 FIFO）。

**B3.3 `Gb28181ProtocolHandler` 补响应处理方法**

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

`vue-vben-admin/apps/web-antd`，严格对齐后端契约（逐字段核对，不发明字段/端点；新增字段先登记 `.cursor/rules/project-rule.mdc`）：
- `api/device.ts`：getPage（筛选）+ 各指令端点。
- `views/device/index.vue`：列表（筛选栏：deviceId/名称/状态/协议/时间范围）+ 状态标签 + 通道数列。
- 操作面板：PTZ 方向盘、查询按钮组、录像查询弹窗、配置下载。
- i18n：`device.entity.action` key 模式。
- SSE：复用 `useSseEvents`（1.0.6 已建），订阅 `device.*` 实时刷新列表状态。

---

## 7. 实施顺序与构建注意

### 7.1 推荐顺序（每步独立编译可测）

1. **S1**（client `DeviceQueryReq` 无依赖；web 控制器 + manager 方法）→ `mvn test -Dtest=DeviceManagerTest,DeviceControllerTest`
2. **S2**（client 接口扩展 → service 实现 → web 端点）→ `GbDeviceCommandServiceTest`
3. **S3**（manager patchExtendInfo → integration handler）→ `Gb28181ProtocolHandlerTest`

### 7.2 构建坑（来自 [[voglander-1.0.6-protocol-lab-progress]] / [[voglander-web-tests-use-installed-integration-jar]]）

- **改了 `voglander-client` / `voglander-integration` / `voglander-service`**：voglander-web 测试从 `~/.m2` 的兄弟模块 jar 加载，**必须先全量 `mvn clean install -DskipTests`** 再跑 web 测试，否则跑的是旧字节码（典型报错"找不到 XxxDTO 类文件"）。`-pl x -am install` 只到目标模块，不重建下游。
- **IDE 易残留旧 class**：跑测试前先 `mvn clean`（[[voglander-test-infra-gotchas]]）。
- **integration 模块源码级别为 Java 17**（已核对 `voglander-integration/pom.xml`：`<source>17</source>`/`<target>17</target>`）：handler/command **可用** Java16+ `instanceof X x` 模式匹配。
  > 更正：旧文档/记忆曾称 integration 钉 `<source>9>` 禁用模式匹配，**经反编译核对为误**，全模块统一 17。
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
| 1 | web | `api/device/req/DeviceQueryReq.java` | 新增（筛选字段） | S1 |
| 2 | web | `api/device/resp/DeviceListResp.java` | 新增（total+items） | S1 |
| 3 | web | `api/device/assembler/DeviceWebAssembler.java` | 加 `queryReqToDto`（毫秒→LocalDateTime）；现仅有 create/update Req→DTO，无 query 转换 | S1 |
| 4 | web | `api/device/DeviceController.java` | 加 `/getPage` 模板端点；旧端点标 `@Deprecated` | S1 |
| 5 | web | `api/device/vo/DeviceVO.java` | 加 `channelCount`（现无此字段）+ ExtendInfoVO 子字段 | S1/S3 |
| 6 | manager | `domaon/dto/DeviceQueryDTO.java` | 新增 | S1 |
| 7 | manager | `manager/DeviceManager.java` | **扩展现有 `getPage` 支持多条件**（非新建 getPageByCondition）；新增 `patchExtendInfo`（现无） | S1/S3 |
| 8 | manager | `domaon/dto/DeviceDTO.java` | ExtendInfo 加快照子字段 | S3 |
| 9 | manager | `manager/RecordInfoCacheManager.java` | 新增（录像结果缓存） | S3 |
| 10 | client | `service/device/DeviceCommandService.java` | 接口加 ~9 方法 + `stopByStreamId`（缺口 C） | S2 |
| 11 | client | `domain/device/qo/Device{Config,RecordQuery,AlarmQuery}Req.java` | 新增 3 个 Req | S2 |
| 11b | client | `domain/device/qo/DevicePlayReq.java` | 加 `streamId` 字段（缺口 C 停流闭环） | S2 |
| 12 | service | `command/impl/GbDeviceCommandService.java` | 实现新方法 + `stopByStreamId`；移除 `@SuppressWarnings` | S2 |
| 13 | common | `exception/ServiceExceptionEnum.java` | 视需补错误码 | S2 |
| 14 | web | `api/device/controller/DeviceCmdController.java` | 全量端点 + `*Req` 化；**新建 `/ptz`**（现无）；停流端点改收 streamId | S2 |
| 15 | integration | `wrapper/gb28181/handler/Gb28181ProtocolHandler.java` | 补 6 响应 handler（DeviceStatus/PtzPosition/Preset/Config/ConfigDownload/RecordInfo） | S3 |

> 实测实体类（§1.5A，2026-06-11 反编译 **1.8.1** jar 复核全部命中）：`DeviceStatus`/`PTZPositionResponse`/`PresetQueryResponse`(内 `PresetQueryResponse$PresetItem{presetId,presetName}`)/`DeviceConfigResponse`/`DeviceConfigDownloadResponse`(11 子项: basicParam/videoParamOpt/svacEncodeConfig/svacDecodeConfig/videoParamAttribute/videoRecordPlan/videoAlarmRecord/pictureMask/frameMirror/alarmReport/osdConfig)/`DeviceRecord`(内 `DeviceRecord$RecordItem`)，均 `io.github.lunasaw.gb28181.common.entity.response` 包。forwarder `io.github.lunasaw.sipgateway.gb28181.forwarder.Gb28181EventForwarder.emit()` 确为 `JSON.toJSONString→parseObject(Map)`。

**不改**：6 个 `VoglanderServerXxxCommand`（已完整）、`ServerCommandSender`/`notifier`/`ShardDispatcher`（链路已通）、`sip-proxy`/`zlm-starter`（无需上游重建）。

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
- [ ] S2：6 大指令域均有 `*Req` 端点 + 门面方法，envelope 实发，测试绿。
- [ ] S3：5 类核心响应落库/缓存 + SSE，既有 Catalog/Session 不回归，测试绿。
- [ ] 全量 `mvn clean install` SUCCESS；新增测试覆盖关键分支。
- [ ] Swagger 文档完整；不破坏前端现有调用。
- [ ] （S4 若做）前端设备页严格对齐契约，门禁 `pnpm check` 绿。
