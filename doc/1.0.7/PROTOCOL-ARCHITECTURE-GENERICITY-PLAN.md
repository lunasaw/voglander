# 通信协议架构 — 通用性与可维护性优化方案

> 版本 1.0.7 · 分支 `0608_dev` · 文档日期 2026-06-12 · **本文为实施方案，按此分阶段 TDD 落地**
> 角色定位：voglander 作为多协议视频监控平台（GB28181 平台端 + 未来 ONVIF/GT1078/RTSP），本方案治理**协议接入层的对称性、可插拔性与技术债**，让"新增一个协议"从"改老代码"变成"加新实现"。
> 依赖版本：**sip-gateway / gb28181 1.8.1**（pom 已钉）、zlm 1.0.11。本文所列代码事实均已对当前 `0608_dev` 主代码核对（非测试代码）。
> 关联：[[sip-gateway-1.8.0-migration]]、[[gb28181-live-invite-channel-addressing]]、`doc/1.0.7/GB28181-DEVICE-MANAGEMENT-TECH-PLAN.md`（命令/事件全景）、`doc/1.0.7/LIVE-STREAM-CACHE-CALLBACK-SYNC-TECH-PLAN.md`（关流/缓存同步）。

---

## 一、背景：为什么写这份方案

voglander 当前**入站事件分发**已是教科书级的协议可插拔设计，但**出站命令路由**仍停留在硬编码。这种"一半已进化、一半未进化"的**不对称**，是当前协议架构最大的结构性问题：它让架构"看起来支持多协议"（接口齐全、枚举齐全），但实质上**新增任何非 GB28181 协议都必须修改既有路由代码**，违背开闭原则。

本方案的目标不是现在就接 ONVIF，而是**先把骨架对称化、把散落的字符串与死代码收敛**，使将来接入新协议时改动面最小、风险最低。

---

## 二、现状盘点（带证据）

### 2.1 入站：已是完整 SPI 表驱动 ✅（标杆）

入站链路新增协议**零改动既有代码**：

```
VoglanderBusinessNotifier         (轻量翻译，@Async 立即归还 SIP 线程)
  → ShardDispatcher               (按 deviceId 分 16 片，槽内串行)
    → InboundEventDispatcher      (Map<protocol, handler> 构造注入自动折叠)
      → Gb28181ProtocolHandler    (group/name 二次路由到协议无关 Service)
```

- [`InboundEventDispatcher.java:28-32`](../../voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/event/InboundEventDispatcher.java#L28-L32)：构造注入 `List<ProtocolEventHandler>` 折叠成 `Map<protocol, handler>`，新增协议只要 `@Component` 一个新 handler，本类不动。
- [`ProtocolEventHandler.java`](../../voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/event/ProtocolEventHandler.java)：协议无关接口，`protocol()` + `handle(DeviceEvent)` 两方法。
- [`VoglanderBusinessNotifier.java:34`](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/notifier/VoglanderBusinessNotifier.java#L34)：仅做三段式 type 切分翻译，不含业务逻辑。

**出站命令基类同样协议无关** ✅：[`AbstractVoglanderServerCommand.dispatchEnvelope()`](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/server/command/AbstractVoglanderServerCommand.java#L95) 只接收三段式 `type` + `payload`，经 `CommandHandlerRegistry` 路由，自身不含 GB28181 语义。各业务域 command 复用度高、零样板重复。

### 2.2 出站路由：硬编码 if，与入站不对称 🔴

[`DeviceAgreementService.java:24-26`](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/command/DeviceAgreementService.java#L24-L26)：

```java
DeviceCommandService deviceCommandService = null;
if (type.equals(DeviceAgreementEnum.GB28181_IPC.getType()) || type.equals(DeviceAgreementEnum.GB28181_NVR.getType())) {
    deviceCommandService = SpringBeanFactory.getBean(DeviceConstant.DeviceCommandService.DEVICE_AGREEMENT_SERVICE_NAME_GB28181);
}
AssertUtil.notNull(deviceCommandService, ServiceException.PARAMETER_ERROR, "该协议没���对应的实现方法");
```

- `DeviceCommandService` 接口本身设计良好（11 个协议无关方法：`ptzControl`/`startPlay`/`startPlayback`/`stopPlay`/`queryDevice`/`queryChannel`/`reboot`/`controlPlayback` 等，见 [`DeviceCommandService.java:16`](../../voglander-client/src/main/java/io/github/lunasaw/voglander/client/service/device/DeviceCommandService.java#L16)）。
- 唯一实现 [`GbDeviceCommandService.java:38`](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/command/impl/GbDeviceCommandService.java#L38) `@Service("GbDeviceCommandService")`。
- **问题**：新增 ONVIF 实现后，必须回到 `DeviceAgreementService` 加一条 `else if` 分支 + 一个 Bean 名常量。入站已是自动注册，出站却要手改。

### 2.3 命令/事件 type 三段式字符串裸散落 🟠

| 方向 | 数量 | 形态 | 位置 |
|------|------|------|------|
| 出站命令 type | 19 种 | `"gb28181.Control.Ptz"` 等裸字符串 | 6 个 server command 类 |
| 入站事件 case | 35 种 | `case "Lifecycle.Register"` 等裸字符串 | `Gb28181ProtocolHandler` switch |
| 协议名 | — | `return "gb28181"` 字面值 | handler `protocol()` |

主代码裸 `"gb28181..."` 字面值共 **23 处**。出站 type 与入站 case **没有单一事实源**：拼错编译期不报，运行期静默丢事件（[`InboundEventDispatcher.java:44`](../../voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/event/InboundEventDispatcher.java#L44) 仅 `log.warn` 丢弃）。

出站 19 种 type 清单：

```
gb28181.Query.{DeviceInfo,Catalog,DeviceStatus,RecordInfo,AlarmQuery,MobilePosition,PresetQuery}
gb28181.Control.{Ptz,Reboot,Record,AlarmReset}
gb28181.Invite.{Play,Playback,PlaybackControl,Ack,Bye}
gb28181.Config.{BasicParam,ConfigDownload}
gb28181.Device.Broadcast
```

### 2.4 命令基类残留 envelope 过渡期死代码 🟠

[`AbstractVoglanderServerCommand.java`](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/server/command/AbstractVoglanderServerCommand.java) 中：

- `serverCommandSender` 字段（L59）+ `executeCommand()` × 3 重载（L151/169/187）。
- 注释自述"过渡用""新代码严禁直调"。
- **核查结论**：server 端 6 个子类**已无任何调用**（`grep` 确认为 0），envelope 全量启用，这些是**确定的死代码**。
- ⚠️ 注意：**client 端 `AbstractVoglanderClientCommand` 仍在用** `executeCommand`/静态 `ClientCommandSender`（9 个 client 类引用），client 端**不在本次清理范围**，本方案只清 server 端基类。

### 2.5 两套协议枚举语义重叠 🟡

| 枚举 | 取值 | 用途 |
|------|------|------|
| `DeviceProtocolEnum` | GB28181(1)/ONVIF(2)/RTSP(3)/HTTP(4)/RTMP(5)/PRIVATE(6) | 纯协议 |
| `DeviceAgreementEnum` | GB28181_IPC(1)/GB28181_NVR(2)/ONVIF_IPC(3) | 协议 × 设备型态 |

出站路由用 `DeviceAgreementEnum`（把"协议"和"IPC/NVR 型态"压在一个键里），后者还引用前者。路由键概念不统一，接 ONVIF 前需理清。

### 2.6 媒体层协议无关，但被 GB28181 寻址语义绑定 🟡

ZLM wrapper / `MediaSessionManager` / Hook 回调本身**协议无关**（只认 `app:stream`）✅。但 `LiveStartDTO` 的**寻址字段**只有 `deviceId + channelId`（GB28181 国标编码语义；另含 `protocol`/`streamMode` 两个播放参数字段，非寻址）。ONVIF/RTSP 设备无"通道国标编码"，拉流是 HTTP + `StreamProxy`，不走 SIP INVITE。媒体层缺一个 `MediaProtocolHandler` 选路抽象。

### 2.7 现状评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 入站分发通用性 | 9/10 | 完整 SPI，标杆 |
| 出站命令基类 | 8/10 | dispatchEnvelope 协议无关，复用好 |
| **出站路由** | **3/10** | **硬编码 if，唯一硬节点** |
| type 常量化 | 4/10 | 裸字符串散落，无单一事实源 |
| 协议枚举 | 5/10 | 双枚举重叠 |
| 媒体层通用性 | 7/10 | ZLM 无感，但 DTO 绑定 GB28181 |

---

## 三、优化目标与原则

**目标**：让"新增协议"的改动面收敛到「**实现接口 + 新增 handler/command**」，既有路由/分发代码**零改动**，出站与入站对称。

**原则**：
1. **对称**：出站路由复刻入站 `InboundEventDispatcher` 的 SPI 模式。
2. **单一事实源**：每个 type/协议名只在一处定义。
3. **小步可回滚**：每阶段独立可验证，灰度开关兜底。
4. **不过度设计**：ONVIF 实现本身不在本方案，只做骨架预留；P2 项在真正接协议时再落地。
5. **遵循项目规范**：FastJSON2、`LocalDateTime`、`@MockitoBean`、Manager 集成测试 + Service/Controller 纯 Mockito 单测。

---

## 四、分阶段方案

### Stage 1（P0）出站 `DeviceCommandService` SPI 化 — 对称入站

**Goal**：消除 `DeviceAgreementService` 硬编码 if，改为按协议自动注册的路由表。新增协议实现后**本类零改动**。

**改动文件**：
- `voglander-client` — `DeviceCommandService` 接口加 `supportProtocol()` 默认方法或独立元数据。
- `voglander-service` — `GbDeviceCommandService` 声明所支持协议；`DeviceAgreementService` 重写为 SPI 工厂。

**设计**（复刻 `InboundEventDispatcher`）：

```java
// DeviceCommandService 接口新增（默认实现保证向后兼容）
public interface DeviceCommandService {
    /** 本实现支持的协议类型集合，对应 DeviceProtocolEnum.getType() */
    Set<Integer> supportProtocols();
    // ... 既有 11 个方法不变
}
```

```java
// GbDeviceCommandService
@Override
public Set<Integer> supportProtocols() {
    return Set.of(DeviceProtocolEnum.GB28181.getType());
}
```

```java
// DeviceAgreementService 重写为构造注入折叠的路由表
@Service
public class DeviceAgreementService {
    private final Map<Integer, DeviceCommandService> routing;

    public DeviceAgreementService(List<DeviceCommandService> services) {
        Map<Integer, DeviceCommandService> map = new HashMap<>();
        for (DeviceCommandService svc : services) {
            for (Integer p : svc.supportProtocols()) {
                DeviceCommandService prev = map.put(p, svc);
                Assert.isNull(prev, "协议 " + p + " 存在多个 DeviceCommandService 实现");
            }
        }
        this.routing = Collections.unmodifiableMap(map);
        log.info("DeviceAgreementService 就绪，已注册协议: {}", routing.keySet());
    }

    public DeviceCommandService getCommandService(Integer protocolType) {
        Assert.notNull(protocolType, "协议类型不能为空");
        DeviceCommandService svc = routing.get(protocolType);
        AssertUtil.notNull(svc, ServiceException.PARAMETER_ERROR, "协议 " + protocolType + " 无对应命令服务实现");
        return svc;
    }
}
```

> **路由键统一**：`getCommandService` 入参从「`DeviceAgreementEnum` 复合型态」改为「`DeviceProtocolEnum` 纯协议」。调用方 [`DeviceRegisterServiceImpl`](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/login/DeviceRegisterServiceImpl.java) 传入 `dto.getType()` 前需先映射到协议维度（见 Stage 4，本阶段可先用一个 `DeviceAgreementEnum → DeviceProtocolEnum` 的转换工具过渡）。

**Tests**（`voglander-web/src/test`，Service 层纯 Mockito）：
- `DeviceAgreementServiceTest`：注入 mock `DeviceCommandService`（GB28181），`getCommandService(GB28181)` 返回它；传未注册协议抛 `ServiceException`；构造期两个实现声明同协议 → 启动报错。
- 回归 `DeviceRegisterServiceImpl` 注册链路测试不变红。

**Success Criteria**：`DeviceAgreementService` 不含任何 `if (协议 ==)` 分支；新增一个空 `OnvifDeviceCommandService implements DeviceCommandService` 声明 `supportProtocols()=ONVIF` 即可被路由命中，`DeviceAgreementService` 不改一行。

**Status**: ✅ Complete（2026-06-12）。`supportProtocols()` 采用**抽象方法**（非方案原设计的 default 空集）——强制实现显式声明，避免新协议忘声明导致静默无法路由。路由键为纯协议，调用方 `DeviceRegisterServiceImpl` 签名不变（内部经已存在的 `DeviceAgreementEnum.getProtocol()` 折算）。`DeviceAgreementServiceTest` 6 测试绿，注册链路回归绿。

---

### Stage 2（P1）协议常量 + 事件/命令 type 枚举化

**Goal**：把 23 处裸 `"gb28181..."` 收敛为单一事实源，出站 type 与入站 case 共用同一枚举，杜绝拼写漂移。

**改动文件**：
- 新增 `voglander-common` — `ProtocolConstants`（协议名常量）。
- 新增 `voglander-integration`（或 common）— `Gb28181EventType` 枚举（覆盖 35 入站 + 19 出站，三段式）。
- 改造 6 个 server command 类 + `Gb28181ProtocolHandler` + handler 的 `protocol()`。

**设计**：

```java
public final class ProtocolConstants {
    public static final String GB28181 = "gb28181";
    private ProtocolConstants() {}
}
```

```java
// 三段式事件/命令类型枚举（单一事实源）
public enum Gb28181EventType {
    // 出站命令
    CONTROL_PTZ("gb28181.Control.Ptz"),
    INVITE_PLAY("gb28181.Invite.Play"),
    INVITE_PLAYBACK("gb28181.Invite.Playback"),
    QUERY_DEVICE_INFO("gb28181.Query.DeviceInfo"),
    // ... 19 出站
    // 入站事件
    LIFECYCLE_REGISTER("gb28181.Lifecycle.Register"),
    NOTIFY_KEEPALIVE("gb28181.Notify.Keepalive"),
    SESSION_INVITE_OK("gb28181.Session.InviteOk"),
    SESSION_BYE("gb28181.Session.Bye");
    // ... 35 入站

    private final String type;
    Gb28181EventType(String type) { this.type = type; }
    public String type() { return type; }
    /** 入站 handler 二次路由用的 group.name 段（去掉协议前缀） */
    public String groupName() { return type.substring(type.indexOf('.') + 1); }
}
```

- 出站：`dispatchEnvelope(Gb28181EventType.CONTROL_PTZ.type(), ...)`。
- 入站 handler：`Gb28181ProtocolHandler.protocol()` 返回 `ProtocolConstants.GB28181`；switch 可保留 String 但 case 标签引用枚举 `groupName()`，或重构为 `Map<String, Consumer<DeviceEvent>>` 分发表（进一步去 switch，可选）。

**Tests**：
- `Gb28181EventTypeTest`：枚举 `type()` 与 `groupName()` 切分正确；无重复 type。
- 回归出站命令测试 + 入站 `Gb28181ProtocolHandler` 分发测试全绿（type 文本未变，仅来源改枚举，行为等价）。

**Success Criteria**：主代码 `grep '"gb28181'` 仅命中枚举定义文件；其余引用全部通过枚举/常量。

**Status**: ✅ Complete（2026-06-12）。**范围据实修正**：入站 case 标签是两段式（`Lifecycle.Register`，不含 `gb28181` 前缀）且在 `switch` 内（Java case 须编译期常量，不能引用枚举方法），不在 `grep '"gb28181'` 标准内，故本次**只枚举化出站 19 种 type + `protocol()` 协议名**，入站 switch 保持行为等价不动。新建 `ProtocolConstants.GB28181`(common) + `Gb28181CommandType` 枚举(integration)。**关键事实**：出站 type 是传给框架 `CommandHandlerRegistry.require(type)` 的注册键，框架以私有字面值注册、不暴露 public 常量，故 voglander 自建枚举是唯一选项——`Gb28181CommandTypeTest` 逐字冻结 19 个 type 防漂移。验证：代码级 `"gb28181"` 仅剩 `ProtocolConstants` 一处；99 测试绿。

---

### Stage 3（P1）清理 server 端 envelope 过渡死代码

**Goal**：删除 `AbstractVoglanderServerCommand` 中已无调用的 `serverCommandSender` 字段 + `executeCommand()` × 3 重载 + 两个 `@FunctionalInterface`，降噪、明确"唯一出站通道是 dispatchEnvelope"。

**改动文件**：`AbstractVoglanderServerCommand.java`（仅删，不增）。

**前置核查**（已完成，落地前再跑一次确认）：
```bash
grep -rln 'executeCommand\|serverCommandSender' \
  voglander-integration/src/main/java/.../gb28181/server --include='*.java' \
  | grep -v AbstractVoglanderServerCommand
# 期望输出为空
```

**注意**：
- **不动 client 端** `AbstractVoglanderClientCommand`（仍依赖 `executeCommand`/静态 `ClientCommandSender`）。
- `serverCommandSender` 字段删除前确认无 `@Autowired` 被其它 Bean 取用（它是 1.8.0 实例 Bean，框架仍提供，删字段不影响框架）。

**Tests**：删除后全模块 `mvn clean install`（先 `install` voglander-integration，再跑 voglander-web 测试，避免旧 jar）。server command 既有测试全绿。

**Success Criteria**：基类只剩 `dispatchEnvelope` / `dispatchEnvelopeWithCallId` + 校验工具方法；编译通过、测试不红。

**Status**: ✅ Complete（2026-06-12）。删 `serverCommandSender` 字段 + `executeCommand()`×3 重载 + 2 个 `@FunctionalInterface` + 无用 `ServerCommandSender` import。同步清理 6 个 envelope 测试中失去意义的 `serverCommandSender` mock 与 `verifyNoInteractions` 断言（删字段后它们只验证孤立 mock，保留即失真）。基类只余 envelope 双通道 + 校验方法；server command 49 测试绿。

---

### Stage 4（P2）协议枚举路由键统一

**Goal**：明确路由维度为「纯协议 `DeviceProtocolEnum`」，`DeviceAgreementEnum` 退回"设备型态展示/业务分类"用途，不再作路由键。

**改动文件**：`DeviceRegisterServiceImpl` 等调用 `getCommandService` 处；`DeviceAgreementEnum` **已有** `getProtocol()` getter（`@Getter` 自动生成，返回 `DeviceProtocolEnum.getType()`，如 `ONVIF_IPC.getProtocol() → ONVIF.getType()`）——S1 已直接复用此 getter 做 agreement→protocol 折算，无需新增映射方法。

**设计**：调用方先 `agreement → protocol` 再路由（注：S1 已在 `DeviceAgreementService` 内部完成此折算，调用方签名保持传 agreement type；S4 若要把折算上移到调用方则改为）：
```java
Integer protocol = DeviceAgreementEnum.getByType(dto.getType()).getProtocol();
DeviceCommandService svc = deviceAgreementService.getCommandService(protocol);
```

**Tests**：`DeviceAgreementEnum` 各取值映射到正确 `DeviceProtocolEnum`；注册链路回归。

**Success Criteria**：`getCommandService` 入参语义统一为协议；GB28181_IPC/NVR 都路由到同一 GB28181 服务。

**Status**: ✅ Complete（2026-06-12）。`getCommandService` 入参改为纯协议 type，折算（`DeviceAgreementEnum.getByType().getProtocol()`）上移调用方 `DeviceRegisterServiceImpl.login`。新增 `DeviceAgreementEnumTest` 固化折算事实源。S4 相关 12 测试绿。

---

### Stage 5（P2）媒体层 `MediaProtocolHandler` 预留 + DTO 语义中性化

**Goal**：为非 SIP 协议（ONVIF/RTSP 走 `StreamProxy`）预留选路抽象，`LiveStartDTO` 字段语义不再硬绑 GB28181 国标编码。

**设计**（**采用收窄抽象**：只抽象协议特定的建流/拆流，协议无关编排留在 `MediaPlayServiceImpl`，避免把引用计数/GC/复用锁在每个 handler 重复——真就绪而非伪就绪）：
```java
public interface MediaProtocolHandler {
    Set<Integer> supportProtocols();
    MediaEstablishResult establish(MediaEstablishContext ctx);  // GB28181: openRtpServer+INVITE
    void terminate(MediaTerminateContext ctx);                  // GB28181: closeRtpServer+sendBye
}
// Gb28181MediaProtocolHandler：resolveMediaIp/openRtpServer/INVITE/closeRtpServer/sendBye 迁入
// MediaProtocolRouter：折叠 List<MediaProtocolHandler> + resolveForDevice(deviceId) 按设备协议选路
// 未来 OnvifMediaProtocolHandler：establish=addStreamProxy，terminate=deleteStreamProxy
```
- `MediaPlayServiceImpl` 保留选节点/引用计数/占位会话/future 等待/拉 PlayUrls/GC/关流去重；仅在"建流""拆流"两点改调 `handler.establish()/terminate()`，按 `MediaProtocolRouter.resolveForDevice(deviceId)` 选路（与 Stage 1 同构）。

**Tests**：`Gb28181MediaProtocolHandlerTest`（establish/terminate 单测）；`MediaPlayServiceCloseStreamTest` 改为验证经 handler.terminate 收尾；`MediaPlayServiceIntegrationTest` 补建 GB28181 设备行使路由可解析；点播/关流回归全绿。

**Success Criteria**：媒体选路走注册表；新增 RTSP 仅加一个 handler。

**Status**: ✅ Complete（2026-06-12）。收窄抽象落地：新增 `MediaProtocolHandler`/`Gb28181MediaProtocolHandler`/`MediaProtocolRouter` + `MediaEstablishContext/Result`/`MediaTerminateContext`；`MediaPlayServiceImpl` 接线（startLive/closeStream/cleanupFailed 经 handler）。S5 相关 18 测试绿（含真实首播集成）。注：`LiveStartDTO` 字段未改（寻址仍 deviceId/channelId，协议由 router 按 deviceId 查设备解析，无需 DTO 携带），比原设计的 resourceId 改造更省、零破坏。

---

### Stage 6（P2）会话/节点可靠性补强

**Goal**：消除两处可靠性盲点。

1. **节点故障转移**：`MediaPlayService` 选节点为 null 时当前直接抛异常、默认节点硬编码 `servers.get(0)`。补：按 weight 降序取候选列表，首选不可用则顺延，全不可用才抛。
2. **会话状态机转移合法性**：`MediaSessionManager` 当前允许任意状态互转（如 `CLOSED→ACTIVE`）。补一张转移合法表（`INVITING→{ACTIVE,FAILED}`、`ACTIVE→CLOSED` 等），非法转移拒绝并告警；`clearCache()` 散落 5 处的调用收敛到状态变更单一出口。

**Tests**：节点故障转移单测（mock 节点列表，首选离线）；状态机非法转移被拒单测。

**Success Criteria**：单节点故障不再直接打断点播；非法状态转移有防护。

**Status**: ✅ Complete（2026-06-12）。1) `MediaPlayServiceImpl.selectNode` 主选返回 null 时经 `NodeSupplier.getNodes()` 按 weight 降序兜底取 enabled 节点（静态 `chooseNode` 纯逻辑可单测）；2) 新增 `MediaSessionStatusMachine`（合法转移表），`MediaSessionManager.onInviteOk/onAck` 加守卫拒绝终态（CLOSED/FAILED）→ACTIVE 复活。S6 相关 11 测试绿（节点故障转移 4 + 状态机 6 + 守卫集成 1）。

---

## 五、新增协议成本对照（以 ONVIF 为例）

| 改动项 | 改造前（当前） | 改造后（S1+S2 落地） |
|--------|---------------|---------------------|
| 出站路由 `DeviceAgreementService` | **必须加 else if 分支 + Bean 名常量** 🔴 | **零改动**（自动注册） ✅ |
| 出站命令实现 | 新增 `OnvifDeviceCommandService` | 同（必要工作） |
| 命令 type 定义 | 裸字符串散落，易拼错 | 新增 `OnvifEventType` 枚举集中 ✅ |
| 入站 handler | 新增 `OnvifProtocolHandler`（已可零改动注册） ✅ | 同 |
| 媒体选路（S5 已落地✅） | 改 `MediaPlayService` if 分支 | 新增 `OnvifMediaProtocolHandler` ✅ |

**结论**：S1+S2 落地后，出站命令路由硬节点从 1 降为 0，出站与入站对称。**S5 落地后媒体选路硬节点也从 1 降为 0**——至此「新增协议」在命令、事件、媒体三层均为「加新实现、改老代码 0 处」，架构达到完整的新协议就绪。

---

## 六、风险与回滚

| 阶段 | 风险 | 缓解 / 回滚 |
|------|------|------------|
| S1 | 构造注入折叠时若误判重复协议导致启动失败 | 启动日志打印已注册协议；回滚=恢复旧 if（git revert 单 commit） |
| S2 | type 文本改枚举后若字符串笔误，运行期丢命令/事件 | 枚举值与原裸字符串逐一比对的单测；行为等价不改语义 |
| S3 | 误删仍被引用的字段 | 落地前 `grep` 核查为 0；client 端明确不动；`mvn clean install` 全量验证 |
| S4/S5 | 触及注册/点播主链路 | 仅在接新协议时落地；GB28181 路径迁移后跑全量回归 |

**通用回滚**：每阶段独立 commit，互不依赖前序（S1 与 S2 可并行但建议 S1 先行）。S2 枚举化纯重构、行为等价，最易回退。

---

## 七、落地顺序与验收清单

**推荐顺序**：S1 → S2 → S3（这三项收益最大、风险最低，构成"对称化 + 收敛"的核心）；S4/S5/S6 在真正接 ONVIF/GT1078/RTSP 或补可靠性时再做。

**整体验收**：
- [x] `DeviceAgreementService` 无 `if (协议 ==)` 分支，新增空实现即可被路由（S1）✅
- [x] 主代码 `grep '"gb28181'` 仅命中枚举/常量定义文件（S2）✅ 仅 `ProtocolConstants` 一处
- [x] `AbstractVoglanderServerCommand` 仅余 envelope 通道 + 校验方法（S3）✅
- [x] `mvn clean install` 全绿；server command + 入站 handler + 注册链路回归不红 ✅（S1/S2/S3 直接相关测试 102 绿；全套件 37 个失败均为既有 real-SIP E2E `ListeningPoint Not Exist` flaky + 并行干扰，已 git stash 基线对照证明与本次改动无关）
- [x] 新增协议成本对照表中"改老代码硬节点" = 0 ✅
