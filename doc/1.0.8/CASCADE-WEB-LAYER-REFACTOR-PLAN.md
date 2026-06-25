# GB28181 级联 Web 层规范化重构技术方案

> 版本：1.0.8 ｜ 拟定日期：2026-06-16 ｜ 模块：`voglander-web` / `voglander-manager`
> 关联文档：`doc/1.0.8/GB28181-CASCADE-CLIENT-TECH-PLAN.md`（级联客户端协议主方案，本方案是其 **Web 管理层** 的规范化补强）
> 权威范式：`device` 模块（Controller → WebAssembler → VO → ListResp 链路）
> 契约规约：`vue-vben-admin/CLAUDE.md`「前后端集成与契约」节（VO 出参统一 Unix 毫秒、分页统一 `POST .../getPage` 返回 `*ListResp{total,items}`、后端先行）

---

## 目录

1. [背景与问题](#1-背景与问题)
2. [device 权威范式](#2-device-权威范式)
3. [字段权威对照表](#3-字段权威对照表)
4. [重构内容清单](#4-重构内容清单)
5. [registerStatus 枚举](#5-registerstatus-枚举)
6. [前后端契约表](#6-前后端契约表)
7. [影响与验收](#7-影响与验收)

---

## 1. 背景与问题

`voglander-web` 下的级联管理 Web 层（`CascadePlatformController` / `CascadeChannelController`）是在协议主���案推进过程中临时落地的脚手架，**未遵循项目统一 Web 层规范**，直接把 Manager 层的 DTO 透传给前端，导致字段名、分页结构、时间格式三个维度全部与项目契约错位。前端 `vue-vben-admin` 的级联页（`apps/web-antd/src/views/cascade/`）据此臆造了一批后端根本不存在的字段，页面无法跑通。

### 1.1 五条违规对照

以 `device` 模块为权威范式，cascade Web 层的违规点如下：

| # | 规范（device 范式） | cascade 现状 | 违规文件 |
|---|---|---|---|
| V1 | 分页用 `POST /getPage` + `@RequestBody PageReq` | `GET /page` + `@RequestParam page/size/enabled` | `CascadePlatformController#page` (L69-79) |
| V2 | Controller 出参统一返回 **VO** | 直接返回 **DTO**（`AjaxResult<CascadePlatformDTO>`） | 两个 Controller 的 `getById` / `page` |
| V3 | 有 `WebAssembler`（Req→DTO）+ `VO`（DTO→VO） | **完全没有** Assembler / VO / Req | `web/api/cascade/` 仅有两个 Controller |
| V4 | 时间字段统一 **Long 毫秒** | DTO 裸 `LocalDateTime` 直出 | `CascadePlatformDTO` (L12-13)、`CascadeChannelDTO` (L12-13) |
| V5 | 分页返回 `*ListResp{total, items}` | 返回 MyBatis-Plus `Page<DTO>{records, ...}` | 两个 Controller 的 `page` 端点 |

> 现状端点核对（实测自源码）：
> - `CascadePlatformController`：`@RequestMapping("/api/v1/cascade/platform")`，端点 `POST /`（add）、`PUT /{id}`、`DELETE /{id}`、`GET /{id}`、`GET /page`、`POST /{id}/enable`、`POST /{id}/disable`、`POST /refresh`。
> - `CascadeChannelController`：`@RequestMapping("/api/v1/cascade/channel")`，端点 `POST /`、`PUT /{id}`、`DELETE /{id}`、`GET /{id}`、`GET /page`。

### 1.2 后果

1. **字段名对不上**：前端按 `platformName / serverIp / serverPort / serverDomain / localClientIp / localClientPort` 取数与提交（见 `api/cascade/platform.ts`），但后端 DO/DTO 实际字段是 `platformIp / platformPort / platformDomain / localIp / localPort`，且**没有** `platformName`。表单提交后这些字段不会被持久化，列表展示一片空白。
2. **分页结构对不上**：前端按项目统一约定消费 `{ total, items }`，后端 `GET /page` 返回的是 MyBatis-Plus `Page` 对象（`{ records, current, size, pages, total }`），前端 `proxyConfig.ajax.query` 取不到 `items`，列表渲染不出数据。
3. **时间格式对不上**：前端约定 `createTime/updateTime` 为 `number`（毫秒），后端直出 `LocalDateTime`，FastJSON2 序列化为数组/字符串，前端时区格式化逻辑直接报错或显示异常。
4. **分页入参方式对不上**：前端列表统一走 `POST .../getPage` body 传查询条件，后端是 `GET /page` 读 `@RequestParam`，请求方法与参数位置均不一致。

---

## 2. device 权威范式

`device` 模块是项目 Web 层的标准实现，链路为 **Controller → WebAssembler（Req→DTO）→ Manager → DTO → VO（DTO→VO，毫秒转换）→ ListResp{total,items}**。

### 2.1 Controller 分页（POST /getPage 返回 ListResp）

摘自 `DeviceController#getPage`（L64-87）：

```java
@PostMapping("/getPage")
public AjaxResult getPage(
    @RequestBody(required = false) DevicePageReq pageReq,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size) {
    DeviceQueryDTO query = deviceWebAssembler.pageReqToQueryDto(pageReq);
    Page<DeviceDTO> dtoPage = deviceManager.getPage(query, page, size);

    List<DeviceVO> items = dtoPage.getRecords().stream()
        .map(DeviceVO::convertVO)
        .collect(Collectors.toList());

    DeviceListResp resp = new DeviceListResp();
    resp.setTotal(dtoPage.getTotal());
    resp.setItems(items);
    return AjaxResult.success(resp);
}
```

关键点：`@RequestBody PageReq` 接条件、`page/size` 仍走 `@RequestParam`；Manager 返回 `Page<DTO>`，Controller 负责把 `records` 逐条 `convertVO` 后塞进 `ListResp`，**Manager/DTO 不出 Web 层**。

### 2.2 ListResp（total + items）

摘自 `DeviceListResp`（全文）：

```java
@Data
public class DeviceListResp implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long           total;   // 总记录数
    private List<DeviceVO> items;   // 列表
}
```

### 2.3 VO（Long 毫秒 + 静态 convertVO + 派生名称）

摘自 `DeviceVO`（L26-31, L100-118）：

```java
private Long createTime;   // unix 毫秒
private Long updateTime;   // unix 毫秒
private String statusName; // 由 status 派生的中文名

public static DeviceVO convertVO(DeviceDTO dto) {
    if (dto == null) return null;
    DeviceVO vo = new DeviceVO();
    vo.setId(dto.getId());
    vo.setCreateTime(dto.createTimeToEpochMilli());   // ← DTO 领域方法做毫秒转换
    vo.setUpdateTime(dto.updateTimeToEpochMilli());
    vo.setStatus(dto.getStatus());
    vo.setStatusName(getStatusName(dto.getStatus())); // ← 枚举派生展示字段
    // ...
    return vo;
}
```

时间毫秒转换收敛在 DTO 领域方法（摘自 `DeviceDTO` L217-227）：

```java
public Long createTimeToEpochMilli() {
    return createTime != null ? createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
}
public Long updateTimeToEpochMilli() {
    return updateTime != null ? updateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
}
```

### 2.4 WebAssembler（Req→DTO + 毫秒→LocalDateTime）

摘自 `DeviceWebAssembler`（L152-179）：

```java
public DeviceQueryDTO pageReqToQueryDto(DevicePageReq req) {
    DeviceQueryDTO dto = new DeviceQueryDTO();
    if (req == null) return dto;
    dto.setDeviceId(req.getDeviceId());
    dto.setName(req.getName());
    // ... 入参 Unix 毫秒 → LocalDateTime（系统时区）
    dto.setRegisterTimeStart(toLocalDateTime(req.getRegisterTimeStart()));
    return dto;
}

private LocalDateTime toLocalDateTime(Long epochMilli) {
    if (epochMilli == null) return null;
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
}
```

`Req` 是纯 Web 入参对象，时间字段统一 `Long`（摘自 `DevicePageReq`，时间字段为 `keepaliveTimeStart/End`、`registerTimeStart/End`，类型 `Long`，注释明确「Unix 毫秒」）。

> **范式四层职责小结**
> | 层 | 类型 | 职责 |
> |---|---|---|
> | `req/*Req` | Web 入参 | 承接前端 body，时间用 `Long` 毫秒 |
> | `assembler/*WebAssembler` | `@Component` | `Req → DTO`，毫秒 → `LocalDateTime` |
> | `vo/*VO` | Web 出参 | `静态 convertVO(DTO)`，`LocalDateTime → Long` 毫秒、枚举派生 `*Name` |
> | `resp/*ListResp` | 分页���装 | `{ total: Long, items: List<VO> }` |

---

## 3. 字段权威对照表

> **唯一事实来源 = `CascadePlatformDO`（`@TableName("tb_cascade_platform")`）与 `CascadeChannelDO`（`tb_cascade_channel`）的 DB 字段。** 任何前端/DTO/VO 字段必须从这里派生，不得新增不存在的列。

### 3.1 CascadePlatformDO 实际字段（权威）

摘自 `CascadePlatformDO`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键（自增） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |
| `platformId` | `String` | 上级国标 ID（SIP Server ID） |
| `platformIp` | `String` | 上级平台 IP |
| `platformPort` | `Integer` | 上级平台端口 |
| `platformDomain` | `String` | 上级平台域 |
| `username` | `String` | 注册用户名 |
| `password` | `String` | 注册密码 |
| `localClientId` | `String` | 本地模拟客户端 ID |
| `localIp` | `String` | 本地 IP |
| `localPort` | `Integer` | 本地端口 |
| `enabled` | `Integer` | 1-启用 0-禁用 |
| `registerStatus` | `Integer` | 0-离线 1-在线 2-注册中 3-失败 |
| `keepaliveInterval` | `Integer` | 保活间隔（秒） |
| `registerExpires` | `Integer` | 注册有效期（秒） |
| `charset` | `String` | 字符集（GB2312/UTF-8） |
| `transport` | `String` | 传输协议（UDP/TCP） |
| `extend` | `String` | 扩展字段 |

### 3.2 前端臆造字段（错误，必须废弃）

前端 `api/cascade/platform.ts` 出现了一批后端**不存在**的字段，须在重构落地后由前端按 §6 契约表逐一改回：

| 前端臆造字段（❌ 不存在） | 后端正确字段（✅ 权威） |
|---|---|
| `platformName` | （无此字段；如需展示名称应另议加列，本期不加） |
| `serverIp` | `platformIp` |
| `serverPort` | `platformPort` |
| `serverDomain` | `platformDomain` |
| `localClientIp` | `localIp` |
| `localClientPort` | `localPort` |

> ⚠️ `platformName` 在 `tb_cascade_platform` 中**无对应列**，后端不会持久化也不会返回。前端表单/列表若需要"平台名称"，要么复用 `platformId`，要么走正式建表变更流程新增列——本期方案**不引入**该列，VO 中也不出现 `platformName`。

### 3.3 CascadeChannelDO 实际字段（权威）

摘自 `CascadeChannelDO`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键（自增） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |
| `platformId` | `String` | 所属上级平台国标 ID |
| `localDeviceId` | `String` | 本地设备 ID |
| `localChannelId` | `String` | 本地通道 ID |
| `cascadeChannelId` | `String` | 上报给上级使用的通道 ID（默认同 `localChannelId`） |
| `cascadeName` | `String` | 上报通道名称 |
| `enabled` | `Integer` | 1-上报 0-不上报 |

---

## 4. 重构内容清单

按 device 范式补齐 cascade Web 层的四层结构，并重构两个 Controller。**全部为后端改动，前端待后端落地后另行对齐。**

### 4.1 DTO 补毫秒转换领域方法

给 `CascadePlatformDTO` / `CascadeChannelDTO` 各补两个领域方法（参照 `DeviceDTO` L217-227），把毫秒转换收敛进 DTO：

```java
// CascadePlatformDTO / CascadeChannelDTO 各加：
public Long createTimeToEpochMilli() {
    return createTime != null ? createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
}
public Long updateTimeToEpochMilli() {
    return updateTime != null ? updateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
}
```

> DTO 内部仍保留 `LocalDateTime` 字段（与 Manager/DO 一致），只是新增"取毫秒"的领域方法供 VO 调用。

### 4.2 新增 VO（`web/api/cascade/vo/`）

| 文件 | 字段（时间 `Long` 毫秒） | 派生字段 |
|---|---|---|
| `CascadePlatformVO` | `id` / `createTime` / `updateTime` / `platformId` / `platformIp` / `platformPort` / `platformDomain` / `username` / `password` / `localClientId` / `localIp` / `localPort` / `enabled` / `registerStatus` / `keepaliveInterval` / `registerExpires` / `charset` / `transport` / `extend` | `registerStatusName`（由 `registerStatus` 派生，见 §5）、可选 `enabledName` |
| `CascadeChannelVO` | `id` / `createTime` / `updateTime` / `platformId` / `localDeviceId` / `localChannelId` / `cascadeChannelId` / `cascadeName` / `enabled` | 可选 `enabledName` |

`CascadePlatformVO` 静态转换方法（仿 `DeviceVO.convertVO`）：

```java
public static CascadePlatformVO convertVO(CascadePlatformDTO dto) {
    if (dto == null) return null;
    CascadePlatformVO vo = new CascadePlatformVO();
    vo.setId(dto.getId());
    vo.setCreateTime(dto.createTimeToEpochMilli());
    vo.setUpdateTime(dto.updateTimeToEpochMilli());
    vo.setPlatformId(dto.getPlatformId());
    vo.setPlatformIp(dto.getPlatformIp());
    vo.setPlatformPort(dto.getPlatformPort());
    vo.setPlatformDomain(dto.getPlatformDomain());
    vo.setUsername(dto.getUsername());
    vo.setPassword(dto.getPassword());
    vo.setLocalClientId(dto.getLocalClientId());
    vo.setLocalIp(dto.getLocalIp());
    vo.setLocalPort(dto.getLocalPort());
    vo.setEnabled(dto.getEnabled());
    vo.setRegisterStatus(dto.getRegisterStatus());
    vo.setRegisterStatusName(registerStatusName(dto.getRegisterStatus()));
    vo.setKeepaliveInterval(dto.getKeepaliveInterval());
    vo.setRegisterExpires(dto.getRegisterExpires());
    vo.setCharset(dto.getCharset());
    vo.setTransport(dto.getTransport());
    vo.setExtend(dto.getExtend());
    return vo;
}
```

> 字段一一对应 §3.1 权威列，**不出现** `platformName/serverIp/serverPort/serverDomain/localClientIp/localClientPort`。

### 4.3 新增 ListResp（`web/api/cascade/resp/`）

仿 `DeviceListResp`：

```java
// CascadePlatformListResp
@Data
public class CascadePlatformListResp implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long total;
    private List<CascadePlatformVO> items;
}

// CascadeChannelListResp
@Data
public class CascadeChannelListResp implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long total;
    private List<CascadeChannelVO> items;
}
```

### 4.4 新增 Req（`web/api/cascade/req/`）

| 文件 | 字段 | 说明 |
|---|---|---|
| `PlatformPageReq` | `enabled?` / `platformId?` / `registerStatus?` | 分页查询条件（body） |
| `PlatformCreateReq` | `platformId` / `platformIp` / `platformPort` / `platformDomain` / `username` / `password` / `localClientId` / `localIp` / `localPort` / `enabled` / `keepaliveInterval` / `registerExpires` / `charset` / `transport` / `extend` | 新增入参，字段对齐 §3.1（无 `id`、无 `registerStatus`——状态由系统维护） |
| `PlatformUpdateReq` | `PlatformCreateReq` 全部字段 + `id` | 更新入参 |
| `ChannelPageReq` | `platformId?` / `enabled?` | 分页查询条件 |
| `ChannelCreateReq` | `platformId` / `localDeviceId` / `localChannelId` / `cascadeChannelId` / `cascadeName` / `enabled` | 新增入参，对齐 §3.3 |
| `ChannelUpdateReq` | `ChannelCreateReq` 全部字段 + `id` | 更新入参 |

> Req 不含时间字段（cascade 当前无按时间范围筛选需求）；若后续要加时间筛选，按 device 范式用 `Long` 毫秒 + Assembler 转 `LocalDateTime`。

### 4.5 新增 WebAssembler（`web/api/cascade/assembler/CascadeWebAssembler.java`）

`@Component`，承担 `Req → DTO` 转换：

```java
@Component
public class CascadeWebAssembler {
    public CascadePlatformDTO toPlatformDTO(PlatformCreateReq req) { /* 逐字段 set，对齐 §3.1 */ }
    public CascadePlatformDTO toPlatformDTO(PlatformUpdateReq req) { /* 含 id */ }
    public CascadePlatformDTO toPlatformQuery(PlatformPageReq req) { /* 查询条件 */ }
    public CascadeChannelDTO  toChannelDTO(ChannelCreateReq req)   { /* ... */ }
    public CascadeChannelDTO  toChannelDTO(ChannelUpdateReq req)   { /* 含 id */ }
    public CascadeChannelDTO  toChannelQuery(ChannelPageReq req)   { /* ... */ }
}
```

### 4.6 重构两�� Controller

**`CascadePlatformController`**：

| 端点 | 改动 |
|---|---|
| `POST /getPage` | **替换** 原 `GET /page`：`@RequestBody PlatformPageReq` + `@RequestParam page/size` → `assembler.toPlatformQuery` → `manager.getPage` → `records` 逐条 `CascadePlatformVO.convertVO` → 装入 `CascadePlatformListResp{total, items}` |
| `POST /`（add） | 入参改 `@RequestBody PlatformCreateReq` → `assembler.toPlatformDTO` → `manager.add` |
| `PUT /{id}`（update） | 入参改 `@RequestBody PlatformUpdateReq` → 回填 `id` → `assembler.toPlatformDTO` → `manager.update` |
| `GET /{id}`（getById） | 返回值由 `CascadePlatformDTO` 改为 `CascadePlatformVO`（`convertVO`） |
| `DELETE /{id}` | 保持不变 |
| `POST /{id}/enable` `POST /{id}/disable` `POST /refresh` | **保留**（业务调度端点，与范式无冲突） |

**`CascadeChannelController`**：

| 端点 | 改动 |
|---|---|
| `POST /getPage` | **替换** 原 `GET /page`：`@RequestBody ChannelPageReq` + `page/size` → `manager.getPage` → `CascadeChannelVO.convertVO` → `CascadeChannelListResp{total, items}` |
| `POST /`（add） | `@RequestBody ChannelCreateReq` → assembler → `manager.add` |
| `PUT /{id}`（update） | `@RequestBody ChannelUpdateReq` → 回填 `id` → assembler → `manager.update` |
| `GET /{id}`（getById） | 返回 `CascadeChannelVO` |
| `DELETE /{id}` | 保持不变 |

> Manager 层签名不变（`getPage(DTO query, int page, int size)` 实测已存在：`CascadePlatformManager` L101、`CascadeChannelManager` L89），重构只动 Web 层。

### 4.7 同步更新 ControllerTest 断言

`CascadePlatformControllerTest` / `CascadeChannelControllerTest` 现有 `page_ok` 用例打 `GET /page` + `param("page","1")`，需改为：

- 请求：`post("/api/v1/cascade/platform/getPage")` + `contentType(JSON)` + body 查询条件 + `param("page"/"size")`。
- Mock：`when(manager.getPage(any(...), eq(1), eq(10))).thenReturn(new Page<...>(...))`（注意 device 范式默认 `size=10`）。
- 断言：由原来的整体 `$.data` 改为 `jsonPath("$.data.total")` 与 `jsonPath("$.data.items")`（数组）。
- add/update 用例：body 由裸 DTO JSON 改为 `*Req` 字段 JSON（去掉 `platformName` 这类不存在字段，改用 `platformIp` 等真实字段）。

---

## 5. registerStatus 枚举

`registerStatus` 是 cascade 平台特有状态（与 device 的 `status` 不同），权威定义来自 `CascadePlatformDO` 字段注释：

| registerStatus | 含义 | registerStatusName（中文） |
|---|---|---|
| `0` | 离线 | 离线 |
| `1` | 在线 | 在线 |
| `2` | 注册中 | 注册中 |
| `3` | 失败 | 失败 |
| 其它/`null` | 未�� | 未知 |

VO 派生方法：

```java
private static String registerStatusName(Integer s) {
    if (s == null) return "未知";
    switch (s) {
        case 0: return "离线";
        case 1: return "在线";
        case 2: return "注册中";
        case 3: return "失败";
        default: return "未知";
    }
}
```

> 后续如需复用，可抽 `CascadeRegisterStatusEnum` 到 `voglander-common`；本期先在 VO 内静态派生，与 `DeviceVO.getStatusName` 保持同构最小改动。

---

## 6. 前后端契约表

重构后最终 VO 字段 ↔ 前端 TS 字段一一对应。前端须把臆造字段全部改回下表左列。

### 6.1 CascadePlatformVO ↔ 前端

| 后端 VO 字段 | 类型 | 前端 TS 字段 | 前端类型 | 备注 |
|---|---|---|---|---|
| `id` | `Long` | `id` | `number` | |
| `platformId` | `String` | `platformId` | `string` | 上级国标 ID |
| `platformIp` | `String` | `platformIp` | `string` | **替换前端 `serverIp`** |
| `platformPort` | `Integer` | `platformPort` | `number` | **替换前端 `serverPort`** |
| `platformDomain` | `String` | `platformDomain` | `string` | **替换前端 `serverDomain`** |
| `username` | `String` | `username` | `string` | |
| `password` | `String` | `password` | `string` | |
| `localClientId` | `String` | `localClientId` | `string` | |
| `localIp` | `String` | `localIp` | `string` | **替换前端 `localClientIp`** |
| `localPort` | `Integer` | `localPort` | `number` | **替换前端 `localClientPort`** |
| `enabled` | `Integer` | `enabled` | `number` | 1 启用 / 0 禁用 |
| `registerStatus` | `Integer` | `registerStatus` | `number` | 0/1/2/3 |
| `registerStatusName` | `String` | `registerStatusName` | `string` | 派生中文 |
| `keepaliveInterval` | `Integer` | `keepaliveInterval` | `number` | |
| `registerExpires` | `Integer` | `registerExpires` | `number` | |
| `charset` | `String` | `charset` | `string` | |
| `transport` | `String` | `transport` | `string` | |
| `extend` | `String` | `extend` | `string` | |
| `createTime` | `Long` | `createTime` | `number` | Unix **毫秒** |
| `updateTime` | `Long` | `updateTime` | `number` | Unix **毫秒** |

> 前端须删除 `platformName`（后端无此列）。

### 6.2 CascadeChannelVO ↔ 前端

| 后端 VO 字段 | 类型 | 前端 TS 字段 | 前端类型 |
|---|---|---|---|
| `id` | `Long` | `id` | `number` |
| `platformId` | `String` | `platformId` | `string` |
| `localDeviceId` | `String` | `localDeviceId` | `string` |
| `localChannelId` | `String` | `localChannelId` | `string` |
| `cascadeChannelId` | `String` | `cascadeChannelId` | `string` |
| `cascadeName` | `String` | `cascadeName` | `string` |
| `enabled` | `Integer` | `enabled` | `number` |
| `createTime` | `Long` | `createTime` | `number` (毫秒) |
| `updateTime` | `Long` | `updateTime` | `number` (毫秒) |

### 6.3 分页契约（统一）

| 项 | 约定 |
|---|---|
| 请求方法 | `POST /api/v1/cascade/{platform\|channel}/getPage` |
| 请求体 | `*PageReq`（查询条件），`page`/`size` 走 query param |
| 响应 | `AjaxResult<*ListResp>`，`data = { total: number, items: VO[] }` |
| 前端对接 | VXE Grid `proxyConfig.ajax.query` 直接读 `data.items` / `data.total` |

---

## 7. 影响与验收

### 7.1 改动范围

| 模块 | 改动 |
|---|---|
| `voglander-manager` | `CascadePlatformDTO` / `CascadeChannelDTO` 各加 2 个毫秒转换方法（DTO 字段不变） |
| `voglander-web` | 新增 `cascade/vo/`（2）、`cascade/resp/`（2）、`cascade/req/`（6）、`cascade/assembler/CascadeWebAssembler`（1）；重构 2 个 Controller；改 2 个 ControllerTest |
| `vue-vben-admin`（后续） | `api/cascade/platform.ts` / `channel.ts` 字段改名对齐 §6；`views/cascade/*/data.ts` 表单/列同步；分页改 `POST /getPage` 读 `{total,items}` |

> Manager / Repository / DO / DB 表结构**不改**——本方案只规范化 Web 层。

### 7.2 验收清单

1. **后端编译与构建**：
   ```bash
   # 上游 integration jar 若有联动改动需先 install（本方案仅动 web/manager，通常无需）
   (cd voglander && mvn clean install)
   ```
   > 按工作区构建规约，如改动牵涉 `voglander-integration` 须先 `mvn install` 再构建 web；本方案改动限于 `voglander-web` + `voglander-manager`，`mvn clean install` 一次即可。

2. **单测全绿**：
   ```bash
   mvn test -Dtest=CascadePlatformControllerTest
   mvn test -Dtest=CascadeChannelControllerTest
   ```
   断言已改为 `$.data.total` / `$.data.items`，add/update 用真实字段 JSON。

3. **契约自检**：`getById` / `getPage` 返回 VO，时间为毫秒 `Long`；VO 中无 `platformName/serverIp/serverPort/serverDomain/localClientIp/localClientPort`。

4. **前端构建**（后端落地后）：
   ```bash
   cd vue-vben-admin && pnpm build:antd
   ```
   级联平台/通道列表能正常分页、详情字段齐全、时间正常格式化。

### 7.3 执行顺序

```
① manager: DTO 补毫秒方法
② web: 新增 req / vo / resp / assembler
③ web: 重构两个 Controller（分页 POST /getPage + VO + ListResp；add/update 用 Req+Assembler）
④ web: 改两个 ControllerTest 断言
⑤ mvn clean install + 单测验证（后端先行完成）
⑥ 前端按 §6 契约表对齐字段与分页（后端验收通过后）
```

---

> 本文档所��后端字段、端点、方法签名均摘自实际源码核验（`CascadePlatformController` / `CascadeChannelController` / `CascadePlatformDO` / `CascadeChannelDO` / `CascadePlatformDTO` / `CascadeChannelDTO` / `DeviceController` / `DeviceVO` / `DeviceListResp` / `DevicePageReq` / `DeviceWebAssembler` / `DeviceDTO`），未编造 API。
