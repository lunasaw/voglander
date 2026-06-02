# Voglander 通信协议可扩展性设计（多 Gateway 可插拔）

> 版本：1.0.3 ｜ 编写日期：2026-06-01 ｜ 对应分支：`dev_merge_sip`
> 基线：sip-gateway 1.8.0 接入完成 + GB28181 出站命令 envelope 化完成
> 配套：[ARCHITECTURE.md](./ARCHITECTURE.md)（现状架构）｜ [OPTIMIZATION-DESIGN.md](./OPTIMIZATION-DESIGN.md)（高并发优化）｜ [BUSINESS-INTEGRATION-GUIDE.md](../../../sip-proxy/doc/plans/1.8.0/BUSINESS-INTEGRATION-GUIDE.md)（sip-gateway 接入指南）
>
> **文档性质**：可扩展性设计稿（**非已落地代码**）。目标是让底层通信协议（乃至底层 gateway 产品）可插拔，新增一种协议/接入一个新 gateway 方案时，核心业务层（manager/service/web）零改动。所有接口签名为**建议定义**，落地前以本文为准、以源码现状为对照。

---

## 目录

1. [文档目的与范围](#1-文档目的与范围)
2. [sip-gateway 1.8.0 完整性复核](#2-sip-gateway-180-完整性复核)
3. [现状可扩展性诊断](#3-现状可扩展性诊断)
4. [设计目标与非目标](#4-设计目标与非目标)
5. [目标架构：端口与适配器](#5-目标架构端口与适配器)
6. [入站可扩展设计](#6-入站可扩展设计)
7. [出站可扩展设计](#7-出站可扩展设计)
8. [迁移后目录结构](#8-迁移后目录结构)
9. [分阶段迁移步骤](#9-分阶段迁移步骤)
10. [新协议 / 新 Gateway 接入 Checklist](#10-新协议--新-gateway-接入-checklist)
11. [风险与权衡](#11-风险与权衡)

---

## 1. 文档目的与范围

回答两个问题：

1. **当前是否对 sip-gateway 1.8.0 完整支持？**（§2）
2. **架构能否做到底层通信协议可扩展——后续接入另一个 gateway 方案能否快速落地？**（§3~§10）

范围：仅 voglander 后端的 integration 层与其向上的端口边界。sip-gateway 框架本身、前端不在范围内。

**两个正交的扩展轴**（本文都覆盖）：

| 轴 | 含义 | 典型 | 触达层 |
|----|------|------|--------|
| **协议轴** | 同一 gateway 产品下增加协议 | sip-gateway 增 `gateway-onvif` / `gateway-gt1078`（指南 §八 1.10/1.11） | 仅入站分发 + 出站工厂加分支 |
| **Gateway 产品轴** | 换一个完全不同的接入 SDK/产品 | 非 sip-gateway 的第三方国标/私有网关 | 新增一个适配器，复用同一组 voglander 端口 |

---

## 2. sip-gateway 1.8.0 完整性复核

对照 [BUSINESS-INTEGRATION-GUIDE §2.8](../../../sip-proxy/doc/plans/1.8.0/BUSINESS-INTEGRATION-GUIDE.md) 服务端（角色 A）Checklist，逐项核验源码：

| # | 项 | 状态 | 证据 |
|---|---|------|------|
| 1 | 引 `sip-gateway-spring-boot-starter` | ✅ | `voglander-integration/pom.xml` |
| 2 | `@EnableSipServer` | ✅ | `ApplicationWeb.java`（注释已说明为何必需） |
| 3 | `gateway.node-id` / `sip.server.*` | ✅ | `application-inte.yml` |
| 4 | `ServerDeviceSupplier` | ✅ | `VoglanderServerDeviceSupplier`（`@Primary`） |
| 5 | `DeviceSessionCache` | ✅ 多节点安全 | `VoglanderDeviceSessionCache` 走 DB，非内存版 |
| 6 | `BusinessNotifier` | ✅ | `VoglanderBusinessNotifier`（`@Async`，覆盖 Noop） |
| 7 | `InviteContextStore` | ✅ | `RedisInviteContextStore`（`@ConditionalOnProperty`） |
| 8 | `sip.server.external-ip` | ✅ | `application-inte.yml` |
| 9 | `gateway.nodes` 节点表 | ❌ **缺** | 仅有 `node-id`/`forward-timeout-ms` |

**结论：单节点拉流平台场景已完整支持。** 三个生产/多节点缺口（与本文可扩展性改造**解耦**，可独立修复）：

- **G1（多节点）**：`gateway.nodes` 缺失 → 跨节点 INVITE 回包返 502（指南 §6.3）。单机无影响。
- **G2（功能）**：设备主动 INVITE（`gb28181.Session.ServerInvite`）回包路径未落地。`VoglanderBusinessNotifier` 中该分支仅打日志，注释提到的 `Gb28181InviteResponseController` 与 `POST /gateway/gb28181/invite/response` 在仓库内**不存在**——属悬空引用，需删注释或补端点。语音对讲/级联取流场景不可用；纯拉流平台可接受。
- **G3（安全）**：`VoglanderServerDeviceSupplier#checkDevice` 仅查库存在性 + 在线状态，未做 Digest 明文密码校验（指南 §6.2 要求生产必做）。

> 说明：指南中的 HTTP API（`/gateway/command`、`/gateway/whoami`）voglander **不需要**——它是单体进程内集成，业务层直接调命令 bean，不走 HTTP 跳。这是合法接入风格，非缺口。

---

## 3. 现状可扩展性诊断

一句话：**"出站半通、入站锁死、框架类型未藏"**。

### 3.1 框架耦合图（实际 import，非注释）

| 框架类型 | 真正 import 的生产代码 | 评价 |
|----------|----------------------|------|
| `BusinessNotifier` | `VoglanderBusinessNotifier`（1 处） | 收敛良好 |
| `GatewayEvent` | `VoglanderBusinessNotifier`（1 处） | 收敛良好 |
| `CommandHandlerRegistry` / `GatewayCommand` | `AbstractVoglanderServerCommand` + `VoglanderServerPtzCommand`（2 处） | 收敛良好 |
| `ServerCommandSender` | `AbstractVoglanderServerCommand` + `VoglanderDeviceSessionCache`（降级用） | 收敛良好 |

> manager / repository 仅在 **javadoc 注释**里提及 `VoglanderBusinessNotifier`，无真实 import。框架类型已基本被关在 integration 模块内——这是好基础。

### 3.2 三个硬伤（阻塞新协议 / 新 gateway 接入）

- **H1（入站没有协议分发层）**：`VoglanderBusinessNotifier` 直接 `implements BusinessNotifier`，一个大 `switch` 全是 `gb28181.*` 字面量，且写死 `DeviceAgreementEnum.GB28181_IPC`。框架只允许**一个** `BusinessNotifier` bean，新协议事件（如 `onvif.*`）会落 `default` 被丢弃。**这是最大障碍。**
- **H2（出站门面是空壳）**：协议中立门面 `DeviceCommandService`（`voglander-client`）只有 `queryChannel`/`queryDevice` 两个方法；其 GB 实现 `GbDeviceCommandService`（常量 `DeviceConstant.DeviceCommandService.DEVICE_AGREEMENT_SERVICE_NAME_GB28181`）**在仓库内不存在**。真正可用的是 6 个 GB 专属 `VoglanderServer*Command` bean，但它们**没接到门面上**，上层只能直接注入 GB 命令类。
- **H3（框架类型未藏到 voglander 自有端口后）**：虽然收敛在 integration，但 `GatewayEvent`/`CommandHandlerRegistry` 直接出现在业务实现里。换一个**非 sip-gateway** 的 gateway 产品时，notifier 与 command 基类需整体重写，没有可复用的稳定边界。

### 3.3 已具备的扩展基础（好的部分）

- 协议建模就绪：`DeviceProtocolEnum`（GB28181=1 / ONVIF=2 / RTSP=3 / HTTP=4 / RTMP=5 / PRIVATE=6）、`DeviceAgreementEnum`（GB28181_IPC=1 / GB28181_NVR=2 / ONVIF_IPC=3，含 `protocol` 字段）。
- 出站已有按协议选 bean 的策略工厂：`DeviceAgreementService#getCommandService(Integer type)`。
- 入站业务能力已抽象为协议中立服务：`DeviceRegisterService`（login/keepalive/offline/updateRemoteAddress/addChannel/updateDeviceInfo）、`DeviceManager`、`MediaSessionManager`——这些都不含任何 GB 字面量，可被任意协议复用。

---

## 4. 设计目标与非目标

### 4.1 目标

1. **协议可插拔**：新增一种协议 = 新增一个 `ProtocolEventHandler` + 一个 `DeviceCommandService` 实现 + 一段 `@ConditionalOnProperty` 配置，核心层零改动。
2. **Gateway 产品可替换**：新接一个非 sip-gateway 的网关 = 新增一个 inbound 适配器（把它的原生事件翻译成 voglander 自有 `DeviceEvent`）+ 一组 outbound 命令实现，复用同一组端口。
3. **框架类型零外泄**：`GatewayEvent`/`CommandHandlerRegistry` 等 sip-gateway 类型只存在于「sip-gateway 适配器」子包内，不进入 voglander 自有端口签名。
4. **向后兼容、可灰度**：现有 GB28181 链路保持可用，迁移分阶段、每步可编译可回滚。

### 4.2 非目标

- 不替换 sip-gateway（它仍是 GB28181 的默认适配器实现）。
- 不引入跨进程消息总线（保持进程内 SPI 风格，符合现状）。
- 不在本文解决 §2 的 G1/G2/G3（独立修，互不阻塞）。
- 不改 `DeviceRegisterService`/`DeviceManager`/`MediaSessionManager` 的对外语义。

---

## 5. 目标架构：端口与适配器

核心思路：在「业务核心」与「任意 gateway」之间立**两道 voglander 自有端口**（Port），把框架类型挡在适配器（Adapter）内。

```
┌──────────────────────────────────────────────────────────────────┐
│                        业务核心（协议无关）                          │
│  DeviceRegisterService · DeviceManager · MediaSessionManager       │
│  DeviceChannelManager · ...（已存在，零改动）                       │
└───────────▲───────────────────────────────────────┬───────────────┘
            │ 入站端口                                 │ 出站端口
   InboundEventDispatcher                     DeviceCommandService（门面，扩展后）
   + ProtocolEventHandler                     ← DeviceAgreementService 按 protocol 选实现
            ▲（消费 voglander 自有 DeviceEvent）          │
            │                                            ▼
┌───────────┴───────────────┐              ┌────────────────────────────┐
│  Gb28181ProtocolHandler    │              │  GbDeviceCommandService     │
│  （把 DeviceEvent 路由到核心）│              │  （委托 VoglanderServer*Command）│
└───────────▲───────────────┘              └────────────▲───────────────┘
            │ DeviceEvent                                │ GatewayCommand
┌───────────┴────────────────────────────────────────────┴───────────┐
│            sip-gateway 适配器（唯一 import 框架类型的地方）            │
│  SipGatewayInboundAdapter implements BusinessNotifier               │
│     GatewayEvent ──翻译──▶ DeviceEvent ──▶ InboundEventDispatcher    │
│  AbstractVoglanderServerCommand（dispatchEnvelope → CommandRegistry）│
└─────────────────────────────────────────────────────────────────────┘
                         ▲                                  ▲
            ┌────────────┘                                  │
┌───────────┴───────────────┐         （未来）┌─────────────┴──────────────┐
│  未来：XGatewayInboundAdapter │                │  未来：XProtocolCommandService │
│  把 X 产品事件翻译成 DeviceEvent│                │  对接 X 产品出站 API           │
└────────────────────────────┘                └────────────────────────────┘
```

要点：

- **DeviceEvent** 是 voglander 自有的归一化事件模型，**不是** `GatewayEvent`。适配器负责翻译。
- **入站**：任意 gateway 适配器把原生事件 → `DeviceEvent` → `InboundEventDispatcher`；dispatcher 按 `protocol` 段路由到 `ProtocolEventHandler`。
- **出站**：上层只依赖 `DeviceCommandService` 门面；`DeviceAgreementService` 已能按设备协议类型选实现。

---

## 6. 入站可扩展设计

### 6.1 voglander 自有事件模型 `DeviceEvent`

放 `voglander-client`（协议无关、可被各层引用）。**不含任何 sip-gateway 类型**。

```java
package io.github.lunasaw.voglander.client.domain.event;

import java.util.Map;

/**
 * Voglander 自有归一化设备事件。各 gateway 适配器负责把原生事件翻译成本模型，
 * 与具体 gateway 产品（sip-gateway 等）解耦。
 *
 * @param protocol      协议标识，对应 DeviceProtocolEnum 的 code 名（小写），如 "gb28181"、"onvif"
 * @param group         事件分组，如 "Lifecycle"、"Notify"、"Response"、"Session"
 * @param name          事件名，如 "Register"、"Keepalive"、"InviteOk"
 * @param deviceId      设备 ID（可空，如纯 callId 类会话事件）
 * @param correlationId 关联 ID（sn / callId）
 * @param timestampMs   事件时间戳（毫秒）
 * @param payload       原始负载（FastJSON2 可反序列化的 Map），由各 handler 自行解析
 * @param nodeId        产生事件的网关节点 ID
 */
public record DeviceEvent(
        String protocol,
        String group,
        String name,
        String deviceId,
        String correlationId,
        long timestampMs,
        Map<String, Object> payload,
        String nodeId) {

    /** 三段式类型，如 "gb28181.Lifecycle.Register"。 */
    public String type() {
        return protocol + "." + group + "." + name;
    }
}
```

### 6.2 入站端口：`ProtocolEventHandler` + `InboundEventDispatcher`

放 `voglander-manager`（业务编排层；可调 DeviceRegisterService/DeviceManager/MediaSessionManager）。

```java
package io.github.lunasaw.voglander.manager.event;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;

/**
 * 协议事件处理器。每种协议一个实现，按 protocol 注册。
 */
public interface ProtocolEventHandler {

    /** 本 handler 支持的协议标识，对应 DeviceEvent.protocol，如 "gb28181"。 */
    String protocol();

    /** 处理本协议下的一条事件。实现内部按 group/name 二次路由到业务服务。 */
    void handle(DeviceEvent event);
}
```

```java
package io.github.lunasaw.voglander.manager.event;

import java.util.Map;
import java.util.List;

import org.springframework.stereotype.Component;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 入站事件分发器：按 protocol 段路由到对应 ProtocolEventHandler。
 * 注入所有 ProtocolEventHandler bean，建立 protocol → handler 映射。
 * 新增协议只需新增一个 ProtocolEventHandler 实现，本类零改动。
 */
@Slf4j
@Component
public class InboundEventDispatcher {

    private final Map<String, ProtocolEventHandler> handlers;

    public InboundEventDispatcher(List<ProtocolEventHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProtocolEventHandler::protocol, h -> h));
        log.info("InboundEventDispatcher 就绪，已注册协议: {}", handlers.keySet());
    }

    public void dispatch(DeviceEvent event) {
        if (event == null || event.protocol() == null) {
            log.warn("收到空事件或缺协议标识，忽略");
            return;
        }
        ProtocolEventHandler handler = handlers.get(event.protocol());
        if (handler == null) {
            log.warn("无 {} 协议的事件处理器，丢弃事件 type={}", event.protocol(), event.type());
            return;
        }
        handler.handle(event);
    }
}
```

### 6.3 GB28181 协议处理器（搬迁现有 switch）

`Gb28181ProtocolHandler` 承接 `VoglanderBusinessNotifier` 现在的整段 `switch` 逻辑（调用 `DeviceRegisterService`/`DeviceManager`/`MediaSessionManager`），但**只认 `event.group`/`event.name`，不再 import 任何 sip-gateway 类型**。放 `voglander-integration/.../wrapper/gb28181/handler/`。

```java
@Slf4j
@Component
public class Gb28181ProtocolHandler implements ProtocolEventHandler {

    @Autowired private DeviceRegisterService deviceRegisterService;
    @Autowired private DeviceManager         deviceManager;
    @Autowired private MediaSessionManager   mediaSessionManager;

    @Override public String protocol() { return "gb28181"; }

    @Override
    public void handle(DeviceEvent event) {
        switch (event.group() + "." + event.name()) {
            case "Lifecycle.Register" -> handleRegister(event);
            case "Lifecycle.Online"   -> deviceManager.updateStatus(event.deviceId(), DeviceConstant.Status.ONLINE);
            // ... 原 VoglanderBusinessNotifier 的全部分支照搬，payload 仍走 FastJSON2 ...
            default -> log.debug("未处理 gb28181 事件: {}", event.type());
        }
    }
    // handleRegister/handleKeepalive/handleCatalog/... 从 VoglanderBusinessNotifier 平移
}
```

### 6.4 sip-gateway 入站适配器（唯一 import 框架类型）

`VoglanderBusinessNotifier` 退化为**纯翻译器**：`GatewayEvent` → `DeviceEvent` → `dispatcher.dispatch(...)`。`@Async` 约束保留在此。

```java
@Slf4j
@Component
public class VoglanderBusinessNotifier implements BusinessNotifier {

    @Autowired private InboundEventDispatcher dispatcher;

    @Override
    @Async("sipNotifierExecutor")
    public void notify(GatewayEvent event) {
        if (event == null || event.type() == null) { return; }
        // 三段式 type 切分：gb28181.Lifecycle.Register → (gb28181, Lifecycle, Register)
        String[] seg = event.type().split("\\.", 3);
        if (seg.length < 3) { log.warn("非三段式 type: {}", event.type()); return; }
        dispatcher.dispatch(new DeviceEvent(
                seg[0], seg[1], seg[2],
                event.deviceId(), event.correlationId(),
                event.timestampMs(), event.payload(), event.nodeId()));
    }
}
```

> 收益：当 sip-gateway 1.10 增加 `onvif.*` 事件时，**同一个** `VoglanderBusinessNotifier` 自动把它们翻译成 `DeviceEvent` 并按 `onvif` 路由——只需新增一个 `OnvifProtocolHandler`，notifier 零改动（解 H1）。

---

## 7. 出站可扩展设计

### 7.1 扩展协议中立门面 `DeviceCommandService`

在 `voglander-client` 现有接口上补齐设备控制类方法（**入参用协议无关 Req/DTO，不含 SDP/streamMode 等 GB 专属裸参**）。建议新增：

```java
public interface DeviceCommandService {

    // —— 已存在 ——
    ResultDTO<Void> queryChannel(DeviceQueryReq req);
    ResultDTO<Void> queryDevice(DeviceQueryReq req);

    // —— 建议新增（协议无关语义）——
    ResultDTO<Void>   queryDeviceInfo(String deviceId);
    ResultDTO<Void>   queryCatalog(String deviceId);
    ResultDTO<Void>   ptzControl(DevicePtzReq req);          // 方向/速度/停止，协议内部翻译
    ResultDTO<String> startPlay(DevicePlayReq req);          // 实时点播，返回 callId
    ResultDTO<String> startPlayback(DevicePlaybackReq req);  // 回放，返回 callId
    ResultDTO<Void>   stopPlay(String callId);               // BYE
    ResultDTO<Void>   reboot(String deviceId);
}
```

> `DevicePtzReq`/`DevicePlayReq`/`DevicePlaybackReq` 为新建协议无关 Req（`voglander-client/.../domain/device/qo/`）。GB 实现内部把它们翻译成 `gb28181.Invite.Play` 等 envelope；ONVIF 实现翻译成 ONVIF SOAP 调用。**门面签名不出现任何协议字面量。**
>
> 非所有协议都支持全部能力（如 ONVIF 无国标 callId 语义）——不支持的方法返回 `ResultDTOUtils.failure(NOT_SUPPORTED)`，由门面契约约定。

### 7.2 GB 实现接到门面上（补 H2 的空壳）

新建 `GbDeviceCommandService`（bean 名对齐常量 `DEVICE_AGREEMENT_SERVICE_NAME_GB28181 = "GbDeviceCommandService"`），**委托现有 6 个 `VoglanderServer*Command`**，不重复造轮子：

```java
@Service(DeviceConstant.DeviceCommandService.DEVICE_AGREEMENT_SERVICE_NAME_GB28181) // "GbDeviceCommandService"
public class GbDeviceCommandService implements DeviceCommandService {

    @Autowired private VoglanderServerDeviceCommand deviceCommand;
    @Autowired private VoglanderServerPtzCommand    ptzCommand;
    @Autowired private VoglanderServerMediaCommand  mediaCommand;
    @Autowired private VoglanderServerConfigCommand configCommand;

    @Override public ResultDTO<Void> queryDeviceInfo(String deviceId) { return deviceCommand.queryDeviceInfo(deviceId); }
    @Override public ResultDTO<Void> ptzControl(DevicePtzReq req)     { return ptzCommand.controlDevicePtz(req.getDeviceId(), toPtzEnum(req), req.getSpeed()); }
    @Override public ResultDTO<String> startPlay(DevicePlayReq req)   { /* 适配 inviteRealTimePlay → callId */ }
    // ... 其余委托 ...
}
```

### 7.3 工厂按协议选实现（已存在，仅扩展分支）

`DeviceAgreementService#getCommandService(type)` 现已支持 GB；接 ONVIF 时仅加一个分支：

```java
public DeviceCommandService getCommandService(Integer type) {
    Assert.notNull(type, "协议类型不能为空");
    DeviceCommandService svc = null;
    if (type.equals(GB28181_IPC.getType()) || type.equals(GB28181_NVR.getType())) {
        svc = SpringBeanFactory.getBean(DEVICE_AGREEMENT_SERVICE_NAME_GB28181);
    } else if (type.equals(ONVIF_IPC.getType())) {                 // ← 新增
        svc = SpringBeanFactory.getBean(DEVICE_AGREEMENT_SERVICE_NAME_ONVIF);
    }
    AssertUtil.notNull(svc, PARAMETER_ERROR, "该协议没有对应的实现方法");
    return svc;
}
```

> 上层（如 `DeviceRegisterServiceImpl`）已是 `deviceAgreementService.getCommandService(device.getType()).queryDevice(...)` 的用法——出站调用天然协议无关，**新增协议时上层零改动**（解 H2/H3 出站侧）。

---

## 8. 迁移后目录结构

```
voglander-client/
└── domain/event/DeviceEvent.java                         # 新增：协议无关事件模型
└── domain/device/qo/{DevicePtzReq,DevicePlayReq,DevicePlaybackReq}.java  # 新增：协议无关 Req
└── service/device/DeviceCommandService.java              # 扩展：补设备控制方法

voglander-manager/
└── event/
    ├── ProtocolEventHandler.java                         # 新增：入站端口
    └── InboundEventDispatcher.java                       # 新增：协议路由

voglander-integration/wrapper/
├── gb28181/                                              # 现有，sip-gateway 适配器
│   ├── notifier/VoglanderBusinessNotifier.java           # 改：退化为 GatewayEvent→DeviceEvent 翻译器
│   ├── handler/Gb28181ProtocolHandler.java               # 新增：承接原 switch（不含框架类型）
│   ├── server/command/...                                # 现有 6 个命令，不变
│   └── service/GbDeviceCommandService.java               # 新增：门面实现，委托命令类
└── onvif/                                                # 未来：新协议子包（与 gb28181 平级）
    ├── handler/OnvifProtocolHandler.java                 # implements ProtocolEventHandler
    ├── adapter/OnvifInboundAdapter.java                  # ONVIF 原生事件 → DeviceEvent → dispatcher
    └── service/OnvifDeviceCommandService.java            # implements DeviceCommandService
```

---

## 9. 分阶段迁移步骤

每个 Stage 独立可编译、可回滚；GB28181 现有链路全程可用。

| Stage | 目标 | 改动 | 验收 |
|-------|------|------|------|
| **S1** | 立入站端口 | 新增 `DeviceEvent`、`ProtocolEventHandler`、`InboundEventDispatcher`（空注册表也能启动） | `mvn compile` 通过；dispatcher bean 起得来 |
| **S2** | GB 入站搬迁 | 新增 `Gb28181ProtocolHandler`（平移 switch）；`VoglanderBusinessNotifier` 改为翻译器 | 现有 `VoglanderBusinessNotifier` 行为不变（事件仍落到原业务服务）；GB 回调测试绿 |
| **S3** | 立出站门面 | 扩展 `DeviceCommandService` 接口；新增 `GbDeviceCommandService` 委托现有命令类 | `DeviceAgreementService.getCommandService(GB).startPlay(...)` 走通 |
| **S4** | 收口上层调用 | 上层若有直接注入 `VoglanderServer*Command` 处，改走门面（当前**无**直接调用者，主要是预防性约定 + 文档） | 全量 envelope 测试绿；无新增直连 |
| **S5** | 验证可插拔（不实际接 ONVIF） | 写一个 `NoopProtocolHandler`（protocol="test"）+ 单测，证明新增协议零改核心 | 单测：发 `test.*` 事件被路由到 Noop，gb28181 不受影响 |

> S1~S2 解 H1，S3~S4 解 H2/H3。S5 是「可扩展性」的可执行证明，不引入真实 ONVIF 依赖。

---

## 10. 新协议 / 新 Gateway 接入 Checklist

### 10.1 接入「同 gateway 下的新协议」（如 sip-gateway 增 ONVIF）

| # | 步骤 | 必填 |
|---|------|------|
| 1 | `DeviceProtocolEnum` / `DeviceAgreementEnum` 加枚举值（多已预留 ONVIF） | ✅ |
| 2 | 新建 `onvif/handler/OnvifProtocolHandler implements ProtocolEventHandler`（`protocol()="onvif"`） | ✅ |
| 3 | `OnvifProtocolHandler.handle()` 内按 group/name 调 `DeviceRegisterService`/`DeviceManager` 等协议无关服务 | ✅ |
| 4 | 新建 `onvif/service/OnvifDeviceCommandService implements DeviceCommandService`（bean 名加入 `DeviceConstant`） | ✅ |
| 5 | `DeviceAgreementService.getCommandService` 加 ONVIF 分支 | ✅ |
| 6 | `application-inte.yml` 加该协议开关（`@ConditionalOnProperty`） | 建议 |
| 7 | **核心层（manager/service/web/notifier/dispatcher）零改动** | — |

### 10.2 接入「全新 gateway 产品」（非 sip-gateway）

| # | 步骤 | 必填 |
|---|------|------|
| 1 | 新建独立子包 `wrapper/<vendor>/`（与 gb28181 平级） | ✅ |
| 2 | inbound 适配器：消费该产品的原生回调，翻译成 `DeviceEvent`，调 `InboundEventDispatcher.dispatch(...)`（必要时 `@Async`） | ✅ |
| 3 | 为每个该产品承载的协议提供 `ProtocolEventHandler` | ✅ |
| 4 | outbound：实现 `DeviceCommandService`，对接该产品的出站 API；框架类型只在本子包出现 | ✅ |
| 5 | 装配隔离：`@ConditionalOnProperty` 控制该 vendor 适配器开关，与 sip-gateway 互不影响 | ✅ |
| 6 | **业务核心端口（DeviceEvent / ProtocolEventHandler / DeviceCommandService）不动** | — |

---

## 11. 风险与权衡

| 项 | 说明 | 缓解 |
|----|------|------|
| `DeviceEvent.payload` 仍是 `Map` | 归一化未到强类型，各 handler 仍�� FastJSON2 解析 | 与现状一致；强类型化是后续可选项，不阻塞可扩展性 |
| 门面方法的「协议能力差异」 | ONVIF 无 callId/国标语义，部分门面方法不适用 | 门面契约：不支持返回 `failure(NOT_SUPPORTED)`；调用方判 `ResultDTO` |
| 单一 `BusinessNotifier` 限制 | 框架只允许一个 → 多 vendor 同进程时，sip-gateway 的 notifier 仍是它专属入口 | 不同 vendor 用各自 inbound 适配器（非 BusinessNotifier），统一汇入 dispatcher，无冲突 |
| 搬迁 switch 的回归风险 | S2 平移逻辑可能漏分支 | 平移后逐条对照 `ARCHITECTURE.md §7.2` 事件表；保留原 notifier git 历史可比对 |
| 与 OPTIMIZATION-DESIGN 的交叉 | 该文 §7/§10 涉及 notifier 分片与命令亲和路由 | dispatcher 介于 notifier 与业务之间，分片入队点不变（仍在适配器侧）；两设计正交，落地排期需协调 |

---

## 附：与现有 1.0.3 文档的关系

- 本文是 [ARCHITECTURE.md §16](./ARCHITECTURE.md) 演进路线的**补充**：现状 §16.2 TODO 未列「协议可扩展性」，本文补上设计。
- 与 [OPTIMIZATION-DESIGN.md](./OPTIMIZATION-DESIGN.md) **正交**：后者解高并发/无状态，本文解可插拔；二者都改 notifier 边界，落地时合并排期。
- §2 的 G1/G2/G3 是 1.8.0 完整性缺口，**独立于**本文可扩展性改造，可单独修复。
