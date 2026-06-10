# GB28181 协议验证台 — 模拟设备「注册到任意平台」最终方案（定稿）

> 版本 1.0.6 · 分支 `0608_dev` · 文档日期 2026-06-10 · **本文为定稿且完整自包含，按此实现**
> 角色定位：voglander 在本页扮演**单个模拟设备（UAC / 下级设备）**，不是级联平台。
> 角色辨析见 §1：本方案 ≠ 级联「平台角色」（`CascadeClientScheduler` / `CascadeQueryHandler` 那套真实通道转发），二者用 `@ConditionalOnExpression` 运行时互斥。

---

## 0. TL;DR

协议验证台左侧「设备 UA」当前永远注册到**本进程平台**（`127.0.0.1:5060` 自环）。本方案让前端**在发起注册时直接填写注册信息**（目标平台 serverId/IP/端口/域/传输协议 + 可选设备身份 clientId/密码），即可把这个模拟设备注册到**任意外部 GB28181 平台**做联调。

**关键定位（用户已确认）**：

- **模拟设备角色**：注册后回应上级查目录/PTZ/点播的，仍是现有 `LabQueryListener` 等 Lab 监听器（模拟通道、`model=LabDevice`），**不是**级联的真实通道转发。
- **不要平台管理表**：临时测试默认就是当前平台（和现在一样）；前端发起注册时改参数即可，注册动作本身带参数，注册成功后右侧设备列表自然出数据。
- **身份默认 sip.client**，注册时前端可填写覆盖。
- **下行回应**：本期增强「模拟通道可配」（通道数/编码/名称不再写死 4 个 `Lab-ch`）。

改动 **后端 6 处 + 前端 1 处**（全部内联在 §4，本文完整自包含）：

| # | 文件 | 改动 |
|---|------|------|
| B1 | `LabRegisterReq` 扩展（web）+ `LabSessionHolder`（新增，integration） | 注册请求带目标+身份参数；holder 记住本次会话注册参数供 keepalive 等复用 |
| B2 | `LabSipClient` | 注入 holder，`buildTo()`/`buildFrom()` 读 holder（回退 props/自环） |
| B3 | `VoglanderClientDeviceSupplier` | 401 重发兜底增加「匹配 holder 里的外部 serverId」分支（**关键**），`ObjectProvider` 软注入 |
| B4 | `LabChannelHolder`（新增）+ `LabQueryListener` + `LabSipClient.pushCatalog` + `LabCatalogPushReq` | 模拟通道数/名称可配，被动回应与主动上报共享同一配置 |
| B5 | `LabClientController` | `register` 写 holder（带参 apply / 不带 reset）；`unregister` reset；`catalog/push` 写 `LabChannelHolder`；`/config` 增 `serverDomain`/`transport`/`targetCustomized` 返回当前生效值 |
| F1 | `ClientPanel.vue` + `protocol-lab.ts` + i18n | 注册前可展开「注册信息」表单填目标+身份 |

> **B4 持有者定案**：通道配置（数量/名称前缀）独立于「目标+身份」，且 `onCatalogQuery`（被动回应）与 `pushCatalog`（主动上报）是两条独立路径，必须共享同一份配置，故新增轻量 **`LabChannelHolder`**（lab 条件化），不并入 `LabSessionHolder`。

---

## 1. 为什么不是级联（角色辨析，关键）

代码里已有两套对「上级查目录」的回应，用 `@ConditionalOnExpression` **互斥**，正说明它们是两个不同角色：

| | **本方案：模拟设备（UAC）** | 级联：平台角色（下级平台） |
|---|---|---|
| 我是谁 | 一个摄像头/设备 | 一个平台，下挂真实设备 |
| 查目录回什么 | `LabQueryListener` → 模拟通道 | `CascadeQueryHandler` → `tb_cascade_channel` 真实通道转发 |
| 设备信息 | `model=LabDevice` | `model=CascadePlatform` |
| 收到 INVITE | 模拟设备出流 | 平台转发下挂设备的流 |
| 身份/端口 | `clientId@5061`（sip.client） | `localClientId@5070` + `CascadeClientScheduler` 自动注册 |

你要的是**模拟设备**：`LabQueryListener` 那套模拟行为**全部保留**，只是把注册目标从「本进程自环」放开为「可填任意平台」。级联的 `CascadeClientScheduler`（5070、平台身份、`@PostConstruct` 自动注册真实通道）**不参与**，复用其表反而会被它抢注册，故不用。

---

## 2. 现状链路与两个写死点

```
LabClientController.register(expires)
        │
        ▼
LabSipClient.register(expires)
        │  from = buildFrom()  ← 设备身份（clientProps，5061）
        │  to   = buildTo()    ← 目标平台（serverProps，5060）★写死本进程自环
        ▼
ClientCommandSender.sendRegisterCommand(from, to, expires)
        │   平台返回 401（realm/nonce）
框架重发 → VoglanderClientDeviceSupplier.getToDevice(To头userId=目标serverId)
        │   DB miss → buildLabServerDevice() ★仅当 userId==本地serverId 才返回地址
        ▼
带 digest 的二次 REGISTER
```

两个把目标钉死在本进程的点：

1. **[`LabSipClient.buildTo()`](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/lab/LabSipClient.java)** 直接用 `VoglanderSipServerProperties`（本进程 5060）。
2. **[`VoglanderClientDeviceSupplier.buildLabServerDevice()`](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/supplier/VoglanderClientDeviceSupplier.java)** 的 401 兜底只认本地 serverId。注册到任何外部平台 → DB miss + 兜底不匹配 → `null` → 二次 REGISTER 发不出（这是真正拦路虎，仅改 buildTo 不够）。

---

## 3. 设计

### 3.1 参数随注册请求传入 + 会话 holder 记忆

用户心智：「发起注册时填参数，注册后就有数据」。所以参数**随 `register` 请求传入**，但 keepalive/catalog 是后续独立动作，需要记住「上次注册用的目标+身份」，故引入一个轻量会话 holder。

**`LabSessionHolder`**（lab 条件化，`AtomicReference` 持有不可变快照；为空=自环默认）：

```java
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议验证台「当前注册会话」参数持有者（Lab 模式专用）。
 * <p>
 * 由 register 请求写入，供后续 keepalive/catalog/deviceInfo/alarm 复用同一目标与身份。
 * 快照为 null 表示未自定义 → 全部回退 sip.server.*/sip.client.*（本进程自环，行为同现状）。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabSessionHolder {

    /** 不可变快照；字段 null/空 = 该项不覆盖，回退 props。 */
    @Data
    public static class Snapshot {
        // 目标平台（To）
        private final String  serverId;
        private final String  serverIp;
        private final Integer serverPort;
        private final String  serverDomain;   // realm 来源
        private final String  transport;       // UDP / TCP
        // 设备身份（From）覆盖，可选
        private final String  clientId;
        private final String  clientPassword;
    }

    private final AtomicReference<Snapshot> ref = new AtomicReference<>();

    public void apply(Snapshot s)   { ref.set(s); log.info("Lab 注册会话已更新: serverId={}, target={}:{}, clientId={}",
                                        s.getServerId(), s.getServerIp(), s.getServerPort(), s.getClientId()); }
    public void reset()             { ref.set(null); log.info("Lab 注册会话已重置为自环"); }
    public Snapshot current()       { return ref.get(); }
}
```

### 3.2 取值优先级（holder 优先，回退现状）

| 取值 | holder.current() 覆盖 | 回退 |
|------|----------------------|------|
| 目标 serverId/IP/port/domain | snapshot 对应字段 | `sip.server.*`（自环） |
| 传输协议 | `snapshot.transport` | `sip.client.transport` |
| 设备 clientId | `snapshot.clientId` | `sip.client.clientId` |
| 设备密码 | `snapshot.clientPassword` | `voglander.protocol-lab.device-password` → `sip.client.password` |

holder 为 null（未填任何参数注册，或点了「重置自环」）→ 全部回退 → **行为与现状完全一致**。

### 3.3 改造后链路（★=关键修复）

```
POST /lab/client/register {expires, target?:{serverId,ip,port,domain,transport}, identity?:{clientId,password}}
        │
        ▼
若带 target/identity → LabSessionHolder.apply(snapshot)；否则 reset()（自环）
        │
LabSipClient.register(expires)
        │  buildTo()  ← holder（回退 props）
        │  buildFrom()← holder（回退 props）
        ▼
外部平台 401 → 框架重发 → VoglanderClientDeviceSupplier.getToDevice(外部serverId)
        │  DB miss
        ▼  ★ 新分支：deviceId == holder.snapshot.serverId → 用 holder 构造外部 ToDevice
带 digest 二次 REGISTER → 外部平台收到注册 ✅ → SSE device.register/online → 右侧列表出数据
```

---

## 4. 后端改动（完整代码，可直接落地）

### B1. `LabRegisterReq` 扩展（web 层）

[`voglander-web/.../web/api/lab/domain/LabRegisterReq.java`](../../voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/lab/domain/LabRegisterReq.java)

```java
package io.github.lunasaw.voglander.web.api.lab.domain;

import lombok.Data;

/** 设备注册请求（Lab 用）。target/identity 全空 = 注册到本进程自环，行为同现状。 */
@Data
public class LabRegisterReq {
    private int     expires = 3600;
    /** 目标平台覆盖。 */
    private String  serverId;
    private String  serverIp;
    private Integer serverPort;
    private String  serverDomain;
    private String  transport;       // UDP / TCP
    /** 设备身份覆盖（空=用 sip.client）。 */
    private String  clientId;
    private String  clientPassword;
}
```

### B1. `LabSessionHolder`（新增，integration 层）

[`voglander-integration/.../gb28181/lab/LabSessionHolder.java`](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/lab/LabSessionHolder.java)

```java
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议验证台「当前注册会话」参数持有者（Lab 模式专用）。
 * <p>
 * 由 register 请求写��，供后续 keepalive/catalog/deviceInfo/alarm 复用同一目标与身份。
 * 快照为 null 表示未自定义 → 全部回退 sip.server.* / sip.client.*（本进程自环，行为同现状）。
 * keepalive 调度线程与 REST 注册线程并发读写，{@link AtomicReference} 整体替换保证一致快照。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabSessionHolder {

    /** 不可变快照；字段 null/空 = 该项不覆盖，回退 props。 */
    @Getter
    @ToString
    @AllArgsConstructor
    public static class Snapshot {
        // 目标平台（To）
        private final String  serverId;
        private final String  serverIp;
        private final Integer serverPort;
        private final String  serverDomain;   // realm 初始构造来源
        private final String  transport;       // UDP / TCP
        // 设备身份（From）覆盖，可选
        private final String  clientId;
        private final String  clientPassword;
    }

    private final AtomicReference<Snapshot> ref = new AtomicReference<>();

    /** 是否带了任何目标或身份覆盖（controller 用以决定 apply / reset）。 */
    public static boolean hasOverride(String serverId, String serverIp, Integer serverPort,
        String serverDomain, String transport, String clientId, String clientPassword) {
        return StringUtils.isNotBlank(serverId) || StringUtils.isNotBlank(serverIp) || serverPort != null
            || StringUtils.isNotBlank(serverDomain) || StringUtils.isNotBlank(transport)
            || StringUtils.isNotBlank(clientId) || StringUtils.isNotBlank(clientPassword);
    }

    public void apply(Snapshot s) {
        ref.set(s);
        log.info("Lab 注册会话已更新: serverId={}, target={}:{}, transport={}, clientId={}",
            s.getServerId(), s.getServerIp(), s.getServerPort(), s.getTransport(), s.getClientId());
    }

    public void reset() {
        ref.set(null);
        log.info("Lab 注册会话已重置为自环");
    }

    public Snapshot current() {
        return ref.get();
    }
}
```

### B2. `LabSipClient` 读 holder（回退 props）

注入 `LabSessionHolder`；`buildTo()` 整段读 holder，`buildFrom()` **只覆盖身份字段**（`userId`/`password`/`realm`），`ip`/`port` 始终保持本机 5061 监听绑定地址（改了收不到平台回包，见 §7-2）。`pick()` 取非空者。

[`voglander-integration/.../gb28181/lab/LabSipClient.java`](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/lab/LabSipClient.java)

完整替换 `buildFrom()` / `buildTo()`，并在类顶部注入 holder：

```java
// 字段区新增（与现有 clientProps/serverProps 并列）：
private final LabSessionHolder labSessionHolder;   // @RequiredArgsConstructor 自动注入

/** 取非空覆盖，否则回退。 */
private static String pick(String override, String fallback) {
    return StringUtils.isNotBlank(override) ? override : fallback;
}

/** Lab 客户端身份（5061 → 目标平台）。holder 覆盖身份，ip/port 恒为本机监听地址。 */
public FromDevice buildFrom() {
    LabSessionHolder.Snapshot s = labSessionHolder.current();
    FromDevice from = new FromDevice();
    from.setUserId(pick(s != null ? s.getClientId() : null, clientProps.getClientId()));
    // ip/port 不随目标改：是本机 5061 监听绑定地址，改了收不到平台回包
    from.setIp(clientProps.getDomain());
    from.setPort(clientProps.getPort());
    from.setRealm(clientProps.getRealm());
    // 密码优先级：holder > voglander.protocol-lab.device-password > sip.client.password
    String pwd = s != null && StringUtils.isNotBlank(s.getClientPassword())
        ? s.getClientPassword()
        : (StringUtils.isNotBlank(labDevicePassword) ? labDevicePassword : clientProps.getPassword());
    from.setPassword(pwd);
    return from;
}

/** Lab 目标：holder 优先，回退本进程平台（5060 自环）。 */
public ToDevice buildTo() {
    LabSessionHolder.Snapshot s = labSessionHolder.current();
    String  serverId = pick(s != null ? s.getServerId()    : null, serverProps.getServerId());
    String  ip       = pick(s != null ? s.getServerIp()    : null, serverProps.getIp());
    int     port     = (s != null && s.getServerPort() != null) ? s.getServerPort() : serverProps.getPort();
    String  domain   = pick(s != null ? s.getServerDomain(): null, serverProps.getDomain());
    String  transport= pick(s != null ? s.getTransport()   : null, clientProps.getTransport());

    ToDevice to = new ToDevice();
    to.setUserId(serverId);
    to.setIp(ip);
    to.setPort(port);
    to.setHostAddress(ip + ":" + port);
    to.setRealm(extractRealm(domain));
    to.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
    to.setCharset("UTF-8");
    return to;
}
```

> 其余方法（`register`/`keepalive`/`pushDeviceInfo`/`pushAlarm`）无需改动——它们已调用 `buildFrom()`/`buildTo()`，自动获得 holder 能力。`pushCatalog` 的改动见 B4。

### B3. `VoglanderClientDeviceSupplier` 401 兜底认外部 serverId（**关键**）

`buildLabServerDevice(deviceId)` 增加分支：除「== 本地 serverId（自环）」外，再认「== `LabSessionHolder.current().serverId`（外部目标）」，命中则用 holder 快照构造外部 `ToDevice`。

**注入方式（必读）**：该 supplier 是全局 `@Primary`、**非** lab 条件化，lab 关闭时 `LabSessionHolder` Bean 不存在，故必须用 `ObjectProvider` 软注入，且**用 `@Autowired` 字段注入**。`ObjectProvider.getIfAvailable()` 在 Bean 缺失时返回 `null`，天然兼容 lab 关闭。

> **关于 `@AllArgsConstructor`（勘误）**：该类同时标了 `@NoArgsConstructor @AllArgsConstructor`，Lombok 会把**所有实例字段**（含新增的 `labSessionHolderProvider`）纳入 all-args 构造器，加 `@Autowired(required=false)` 也不例外——all-args 构造签名**一定会变**。但这**不破坏任何东西**：①Spring 多构造器且无构造器标注时走 no-arg + 字段注入，无调用方依赖 all-args；②测试用 `@InjectMocks`，Mockito 挑最大构造器、给不上的参数填 `null` → `labSessionHolderProvider=null` → `currentLabSnapshot()` 安全返回 `null` → 现有测试全绿，**无需提供该 mock、无需改测试**。即：用字段注入即可，**不必**手写构造器或排斥 Lombok 的字段并入，影响为零。

[`voglander-integration/.../gb28181/supplier/VoglanderClientDeviceSupplier.java`](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/supplier/VoglanderClientDeviceSupplier.java)

```java
// import 新增：
import org.springframework.beans.factory.ObjectProvider;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabSessionHolder;

// 字段区新增（@Autowired 字段注入，required=false）：
@Autowired(required = false)
private ObjectProvider<LabSessionHolder> labSessionHolderProvider;
```

`buildLabServerDevice` 完整替换为：

```java
/**
 * Lab 401 鉴权重发兜底：当 REGISTER 响应 To 头指向的目标设备查不到 DB 时，
 * 命中以下两种之一即用 properties / holder 构造目标 ToDevice，使二次 REGISTER 能拿到目标地址：
 * <ol>
 *   <li>deviceId == 本地 serverId（自环，目标=本进程平台 5060）；</li>
 *   <li>deviceId == LabSessionHolder 当前快照的外部 serverId（注册到外部平台）。</li>
 * </ol>
 * 普通外部设备查不到仍返回 {@code null}，不污染常规路径。
 *
 * @param deviceId 目标设备 ID（来自 REGISTER 响应 To 头）
 * @return 目标 ToDevice；不命中返回 {@code null}
 */
private ToDevice buildLabServerDevice(String deviceId) {
    if (deviceId == null) {
        return null;
    }
    // 分支二（外部目标）：lab 开启且 holder 有外部 serverId 快照命中
    LabSessionHolder.Snapshot snapshot = currentLabSnapshot();
    if (snapshot != null && deviceId.equals(snapshot.getServerId())) {
        return buildToFromSnapshot(snapshot);
    }
    // 分支一（自环）：目标=本进程平台自身
    if (serverProperties != null && deviceId.equals(serverProperties.getServerId())) {
        ToDevice toDevice = new ToDevice();
        toDevice.setUserId(serverProperties.getServerId());
        toDevice.setIp(serverProperties.getIp());
        toDevice.setPort(serverProperties.getPort());
        toDevice.setHostAddress(serverProperties.getIp() + ":" + serverProperties.getPort());
        toDevice.setRealm(extractRealm(serverProperties.getDomain()));
        String transport = clientProperties != null ? clientProperties.getTransport() : null;
        toDevice.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
        toDevice.setCharset("UTF-8");
        log.info("Lab 自环目标解析为本进程平台: serverId={}, host={}:{}, transport={}",
            serverProperties.getServerId(), serverProperties.getIp(), serverProperties.getPort(), toDevice.getTransport());
        return toDevice;
    }
    return null;
}

/** lab 关闭时 provider 为 null（Bean 不存在）→ 返回 null，等价无外部覆盖。 */
private LabSessionHolder.Snapshot currentLabSnapshot() {
    if (labSessionHolderProvider == null) {
        return null;
    }
    LabSessionHolder holder = labSessionHolderProvider.getIfAvailable();
    return holder != null ? holder.current() : null;
}

/** 用 holder 快照构造外部目标 ToDevice；serverId 必非空（调用前已校验命中）。 */
private ToDevice buildToFromSnapshot(LabSessionHolder.Snapshot s) {
    String ip = s.getServerIp() != null ? s.getServerIp()
        : (serverProperties != null ? serverProperties.getIp() : "127.0.0.1");
    int port = s.getServerPort() != null ? s.getServerPort()
        : (serverProperties != null ? serverProperties.getPort() : 5060);
    String domain = s.getServerDomain() != null ? s.getServerDomain() : s.getServerId();
    String transport = s.getTransport() != null ? s.getTransport()
        : (clientProperties != null ? clientProperties.getTransport() : null);

    ToDevice toDevice = new ToDevice();
    toDevice.setUserId(s.getServerId());
    toDevice.setIp(ip);
    toDevice.setPort(port);
    toDevice.setHostAddress(ip + ":" + port);
    toDevice.setRealm(extractRealm(domain));
    toDevice.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
    toDevice.setCharset("UTF-8");
    log.info("Lab 外部目标解析: serverId={}, host={}:{}, transport={}",
        s.getServerId(), ip, port, toDevice.getTransport());
    return toDevice;
}
```

> `getDevice` / `getToDevice` 现有对 `buildLabServerDevice` 的调用点**不变**，自动获得外部目标能力。

### B4. 模拟通道可配（`LabChannelHolder` 新增 + 两条路径共享）

新增 lab 条件化 `LabChannelHolder` 持有「被查询时回多少通道、名称前缀」，由 `/catalog/push` 写入，`onCatalogQuery`（被动）与 `pushCatalog`（主动）共同读取。

[`voglander-integration/.../gb28181/lab/LabChannelHolder.java`](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/lab/LabChannelHolder.java)（新增）

```java
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议验证台「模拟通道」配置持有者（Lab 模式专用）。
 * <p>
 * 被动回应（{@code LabQueryListener.onCatalogQuery}）与主动上报（{@code LabSipClient.pushCatalog}）
 * 共享同一配置：通道数量 + 名称前缀。默认 4 通道、前缀 "Lab-ch"，与现状回包完全一致。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabChannelHolder {

    public static final int    DEFAULT_COUNT       = 4;
    public static final String DEFAULT_NAME_PREFIX = "Lab-ch";

    @Getter
    @AllArgsConstructor
    public static class Config {
        private final int    count;
        private final String namePrefix;
    }

    private final AtomicReference<Config> ref =
        new AtomicReference<>(new Config(DEFAULT_COUNT, DEFAULT_NAME_PREFIX));

    public void update(Integer count, String namePrefix) {
        int c = (count != null && count > 0) ? count : DEFAULT_COUNT;
        String p = StringUtils.isNotBlank(namePrefix) ? namePrefix : DEFAULT_NAME_PREFIX;
        ref.set(new Config(c, p));
        log.info("Lab 模拟通道配置已更新: count={}, namePrefix={}", c, p);
    }

    public Config current() {
        return ref.get();
    }
}
```

`LabQueryListener.onCatalogQuery` 注入 `LabChannelHolder`，按配置生成（替换写死的 4 个 `Lab-ch`）：

```java
// 字段区新增（@RequiredArgsConstructor 自动注入）：
private final LabChannelHolder labChannelHolder;

@Override
public DeviceResponse onCatalogQuery(String platformId, DeviceQuery query) {
    publish("clientcmd.query.catalog", platformId, query.getSn());
    LabChannelHolder.Config cfg = labChannelHolder.current();
    int count = cfg.getCount();
    DeviceResponse resp = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), query.getSn(), clientProps.getClientId());
    resp.setSumNum(count);
    java.util.List<io.github.lunasaw.gb28181.common.entity.response.DeviceItem> items = new java.util.ArrayList<>(count);
    for (int i = 1; i <= count; i++) {
        io.github.lunasaw.gb28181.common.entity.response.DeviceItem it =
            new io.github.lunasaw.gb28181.common.entity.response.DeviceItem();
        it.setDeviceId(clientProps.getClientId() + String.format("%02d", i));
        it.setName(cfg.getNamePrefix() + i);
        it.setStatus("ON");
        it.setParental(0);
        it.setRegisterWay(1);
        it.setSafetyWay(0);
        items.add(it);
    }
    resp.setDeviceItemList(items);
    return resp;
}
```

`LabSipClient.pushCatalog` 改为读 `LabChannelHolder`（注入 holder；签名保持 `pushCatalog(int channelCount, String catalogName)`，传入值优先、否则用配置）。**拼接格式与 `onCatalogQuery` 保持一致**（同为 `prefix + i`，不再用 `prefix + "-ch" + i`），确保被动回应与主动上报回包的通道名完全相同：

```java
// 字段区新增：
private final LabChannelHolder labChannelHolder;   // @RequiredArgsConstructor 自动注入

public String pushCatalog(int channelCount, String catalogName) {
    LabChannelHolder.Config cfg = labChannelHolder.current();
    int count = channelCount > 0 ? channelCount : cfg.getCount();
    String prefix = StringUtils.isNotBlank(catalogName) ? catalogName : cfg.getNamePrefix();
    List<DeviceItem> items = new ArrayList<>(count);
    for (int i = 1; i <= count; i++) {
        DeviceItem item = new DeviceItem();
        item.setDeviceId(clientProps.getClientId() + String.format("%02d", i));
        item.setName(prefix + i);   // 与 onCatalogQuery 同格式（去掉 "-ch"）
        item.setStatus("ON");
        item.setParental(0);
        item.setRegisterWay(1);
        item.setSafetyWay(0);
        items.add(item);
    }
    DeviceResponse resp = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), "0", clientProps.getClientId());
    resp.setSumNum(count);
    resp.setDeviceItemList(items);
    log.info("Lab CATALOG → server, channelCount={}", count);
    return ClientCommandSender.sendCatalogCommand(buildFrom(), buildTo(), resp);
}
```

`LabCatalogPushReq` 已有 `channelCount`/`catalogName`，无需改字段；语义升级为「既主动上报、也更新被查询时通道配置」，由 controller（B5）在 `/catalog/push` 时写入 `LabChannelHolder`。

> 本期范围：通道**数量 + 名称前缀**可配；PTZ/点播仍走现有 `LabControlListener`/`LabInviteListener`（模拟设备回应，发 SSE 时间线），不扩展真实出流。

### B5. `LabClientController` 写 holder + `/config` 增字段

[`voglander-web/.../web/api/lab/controller/LabClientController.java`](../../voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/lab/controller/LabClientController.java)

```java
// 字段区新增注入：
@Autowired private LabSessionHolder labSessionHolder;
@Autowired private LabChannelHolder labChannelHolder;

@PostMapping("/register")
@Operation(summary = "设备主动注册（可带目标平台/身份覆盖；不带=自环）")
public AjaxResult<Void> register(@RequestBody(required = false) LabRegisterReq req) {
    if (req != null && LabSessionHolder.hasOverride(req.getServerId(), req.getServerIp(), req.getServerPort(),
        req.getServerDomain(), req.getTransport(), req.getClientId(), req.getClientPassword())) {
        labSessionHolder.apply(new LabSessionHolder.Snapshot(
            req.getServerId(), req.getServerIp(), req.getServerPort(),
            req.getServerDomain(), req.getTransport(),
            req.getClientId(), req.getClientPassword()));
    } else {
        labSessionHolder.reset();   // 不填参数 = 自环
    }
    labSipClient.register(req != null ? req.getExpires() : 3600);
    return AjaxResult.success();
}

@PostMapping("/unregister")
@Operation(summary = "设备注销（expires=0），同时 holder 重置回自环")
public AjaxResult<Void> unregister() {
    labSipClient.register(0);
    labSessionHolder.reset();
    return AjaxResult.success();
}

@PostMapping("/catalog/push")
@Operation(summary = "主动上报目录，并更新被查询时回应的通道配置")
public AjaxResult<Void> pushCatalog(@RequestBody(required = false) LabCatalogPushReq req) {
    int count = req != null ? req.getChannelCount() : LabChannelHolder.DEFAULT_COUNT;
    // 默认 name 对齐 LabChannelHolder.DEFAULT_NAME_PREFIX("Lab-ch")，
    // 否则点一次默认上报会把被动回应通道名从 Lab-ch1 改成 Lab-Channel1
    String name = (req != null && StringUtils.isNotBlank(req.getCatalogName()))
        ? req.getCatalogName() : LabChannelHolder.DEFAULT_NAME_PREFIX;
    labChannelHolder.update(count, name);   // 被动回应同步用此配置
    labSipClient.pushCatalog(count, name);
    return AjaxResult.success();
}
```

> **`LabCatalogPushReq` 默认值同步**：现状 `catalogName` 字段默认 `"Lab-Channel"`（[LabCatalogPushReq.java](../../voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/lab/domain/LabCatalogPushReq.java)）。请一并把字段默认值改为 `"Lab-ch"`，使「前端不传 / 传默认」两条路径与 `LabChannelHolder` 默认前缀一致，保证自环零回归（未 push 时是 `Lab-ch`，默认 push 后仍是 `Lab-ch`）。

`/config` 在现有 Map 基础上增 3 个键（返回**当前生效值**，含 holder 覆盖后的真实目标）：

```java
@GetMapping("/config")
@Operation(summary = "返回当前 Lab 身份与端口配置（含 holder 覆盖后的生效值）")
public AjaxResult<Map<String, Object>> config() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("clientId",   clientProps.getClientId());
    info.put("clientIp",   clientProps.getDomain());
    info.put("clientPort", clientProps.getPort());

    LabSessionHolder.Snapshot s = labSessionHolder.current();
    boolean customized = s != null;
    // 目标返回生效值：holder 优先，否则 sip.server.*
    info.put("serverId",     customized && s.getServerId()     != null ? s.getServerId()     : serverProps.getServerId());
    info.put("serverIp",     customized && s.getServerIp()     != null ? s.getServerIp()     : serverProps.getIp());
    info.put("serverPort",   customized && s.getServerPort()   != null ? s.getServerPort()   : serverProps.getPort());
    info.put("serverDomain", customized && s.getServerDomain() != null ? s.getServerDomain() : serverProps.getDomain());
    info.put("transport",    customized && s.getTransport()    != null ? s.getTransport()    : clientProps.getTransport());
    info.put("targetCustomized", customized);

    info.put("topics", new String[]{
        "device.register","device.online","device.offline","device.keepalive",
        "device.catalog","device.info","session.invite_ok","session.bye",
        "clientcmd.register.ok","clientcmd.register.fail","clientcmd.register.challenge",
        "clientcmd.ptz","clientcmd.record","clientcmd.reboot","clientcmd.iframe",
        "clientcmd.alarmreset","clientcmd.query.catalog","clientcmd.query.deviceinfo",
        "clientcmd.query.devicestatus","clientcmd.config.basicparam",
        "clientcmd.broadcast","clientcmd.invite","alarm.new"
    });
    return AjaxResult.success(info);
}
```

> `LabClientController` 已是 lab 条件化（`@ConditionalOnProperty`），与 `LabSessionHolder`/`LabChannelHolder` 同生命周期，可硬 `@Autowired`，不需 `ObjectProvider`。`@Autowired` 字段注入与现有写法一致。

---

## 5. API 契约（前端对齐）

| 方法 | 路径 | 请求体 | 返回 |
|------|------|--------|------|
| POST | `/api/v1/lab/client/register` | `LabRegisterReq`（扩展：target+identity 字段，全可选） | `AjaxResult<Void>` |
| GET | `/api/v1/lab/client/config` | — | `AjaxResult<Map>`（增 `serverDomain`/`transport`/`targetCustomized`，目标字段返回 holder 覆盖后的**当前生效值**） |
| POST | `/api/v1/lab/client/unregister` | — | 注销（expires=0），同时 holder.reset() 回自环 |
| POST | `/api/v1/lab/client/catalog/push` | `LabCatalogPushReq`（channelCount/catalogName，**兼做被查询时通道配置**，写入 `LabChannelHolder`） | `AjaxResult<Void>` |

> `/config` 返回体是 `Map<String,Object>`（非独立 DTO 类型），前端按 key 取值；下方 TS `LabConfig` 接口仅为前端类型约束，与后端 Map 的 key 对齐即可。

前端 `ProtocolLabApi.RegisterReq` 扩展，并扩展 `LabConfig`（对齐后端 `/config` Map key）：

```ts
export interface RegisterReq {
  expires?: number;
  serverId?: string;
  serverIp?: string;
  serverPort?: number;
  serverDomain?: string;
  transport?: 'TCP' | 'UDP';
  clientId?: string;
  clientPassword?: string;
}

export interface LabConfig {
  clientId: string;
  clientIp: string;
  clientPort: number;
  // 目标：均为 holder 覆盖后的当前生效值
  serverId: string;
  serverIp: string;
  serverPort: number;
  serverDomain: string;          // 新增
  transport: 'TCP' | 'UDP';      // 新增
  targetCustomized: boolean;     // 新增：true=当前非自环
  topics: string[];
}
```

---

## 6. 前端改动（`vue-vben-admin/apps/web-antd`）

`ClientPanel.vue` 在「注册」按钮区增加一个**可折叠的「注册信息」表单**（默认折叠＝自环，与现状一致）：

- 折叠时：直接点「注册」= 现状自环行为。
- 展开「自定义注册信息」后填：目标 serverId / IP / 端口 / 域(选填) / 传输协议；设备 clientId(选填) / 密码(选填)；通道数(选填)。
- 「注册」按钮 → `labRegister(form)`（带填写的参数）。
- 「注销」→ `labUnregister()`（后端 holder.reset 回自环）。
- 身份卡片「目标」行：`targetCustomized=true` 时显示橙色 `Tag`「自定义」徽标，提示当前不是自环。
- 页面加载用 `getLabConfig()` 预填表单默认值（当前生效的 server/client 值）。

i18n：`protocolLab.register.*`（zh-CN/en-US）新增表单字段文案。

---

## 7. 边界与坑（必读）

1. **B3 必须改**：外部平台几乎都要鉴权，不修 401 兜底则卡在二次握手。**先用一个真实/模拟外部平台端到端验证**，别只信单测。
2. **本机监听点不变**：`from.ip/port`（5061 绑定）不随目标改，否则收不到平台回包。外部平台在另一台机时需保证网络可达、本机 5061 可收包（NAT/防火墙）——物理前提，非代码能解。
3. **传输协议要与外部平台一致**：401 重发按目标 transport 构造 Via，TCP/UDP 不符会握手失败 → 表单暴露 transport 选择。
4. **digest realm/密码**：digest 以平台 401 下发 realm 为准，`serverDomain` 主要影响初始构造/日志；`clientPassword` 必须与外部平台为该 clientId 配的密码一致。
5. **自环零回归**：不填参数注册 / 点注销 → holder 为 null → 全部回退 props → 等价现状，既有 LAB-01→LAB-10 链路不受影响。
6. **并发**：keepalive 调度线程与 REST 注册线程并发读写 holder，`AtomicReference` 整体替换保证一致快照。
7. **`@Primary` supplier 软依赖**：`VoglanderClientDeviceSupplier` 非 lab 条件化，必须用 `@Autowired(required=false) ObjectProvider<LabSessionHolder>` **字段注入**，经 `getIfAvailable()` 取用（lab 关闭时 Bean 不存在 → 返回 null）。不可硬 `@Autowired`（lab 关闭时启动炸全局 SIP）。**勘误**：该类标了 `@NoArgsConstructor @AllArgsConstructor`，新字段必然被 Lombok 并入 all-args 构造器、构造签名会变——但无调用方依赖 all-args（Spring 走 no-arg + 字段注入），`@InjectMocks` 也会给不上的参数填 null（`currentLabSnapshot()` 安全返回 null）→ 现有测试全绿、无需改测试。即用字段注入即可，**不必**为「保持构造签名」而排斥 Lombok 字段并入。`LabClientController`/`LabSipClient`/`LabQueryListener` 本身是 lab 条件化，与 holder 同生命周期，可硬 `@Autowired`/构造注入。
8. **B4 通道名拼接格式统一**：`onCatalogQuery`（被动）与 `pushCatalog`（主动）均用 `prefix + i`（不要 `prefix + "-ch" + i`）；且 `LabCatalogPushReq.catalogName` 默认值、controller `/catalog/push` 默认 name、`LabChannelHolder.DEFAULT_NAME_PREFIX` **三者都用 `Lab-ch`**。否则点一次「默认上报」会把被动回应通道名从 `Lab-ch1` 漂移成 `Lab-Channel1`，破坏自环零回归。
9. **构建顺序**：B1(Req 在 web)、B5(controller 在 web)；B1-Holder/B2/B3/B4 改 `voglander-integration`。改完 `mvn clean -pl voglander-integration -am install` 重装 jar 再跑 web（见 memory `voglander-web-tests-use-installed-integration-jar`；先 `mvn clean` 防 IDE 旧类）。
10. **不持久化**：holder 内存态，重启回自环——符合用户「临时测试」��位。若日后要存常用平台，再加表（非本期）。

---

## 8. 测试计划

| 类型 | 用例 | 方式 |
|------|------|------|
| 单元 | `LabSessionHolderTest`：apply/current/reset、空回退、`hasOverride` 各字段判定 | 纯 JUnit |
| 单元 | `LabChannelHolderTest`：默认 4/`Lab-ch`、update 后取值、count≤0 回落默认、namePrefix 空白回落默认 | 纯 JUnit |
| 单元 | `LabSipClientTest`：holder 空→buildTo 用 props（自环）；holder 设外部→用 holder；buildFrom 身份覆盖与回退、ip/port 恒为 5061；`pushCatalog` 通道名 `prefix + i`（与被动同格式，**断言不含 "-ch"**） | `@Mock LabSessionHolder` + `@Mock LabChannelHolder` + Mockito |
| 单元 | `VoglanderClientDeviceSupplierTest` 扩展：①provider 不注入（字段 null）仅匹配本地 serverId（**现有 9 测试不改即过**）②holder 设外部→返回外部 ToDevice ③普通设备→null。新增 mock 经 `ReflectionTestUtils.setField(supplier, "labSessionHolderProvider", mockProvider)` 设入，桩 `getIfAvailable()` 返回 holder | Mockito + `ReflectionTestUtils` |
| 单元 | `LabQueryListenerTest`：通道数/名称按 `LabChannelHolder` 回包（如配 8 通道→回 8、名为 `前缀+序号`，前缀生效） | `@Mock LabChannelHolder` + Mockito，验证 DeviceResponse |
| 单元 | `LabClientControllerTest`：register 带参→holder.apply；不带→reset；unregister→reset；catalog/push→channelHolder.update（默认 name=`Lab-ch`） | 纯 Mockito（业务层禁 @SpringBootTest） |
| 前端 | `protocol-lab.test.ts`：labRegister 带扩展字段走对路径 | Vitest |
| 联调 | 启 dev,repo,inte,lab → 前端展开表单填外部平台 → 注册 → 外部平台收到 register/online + 右侧列表出数据；不填→自环仍通；上级查目录→回配置的通道数与通道名 | 手工（需外部平台） |

后端测试统一在 `voglander-web/src/test/java/io/github/lunasaw/voglander`。

---

## 9. 落地清单

- [ ] B1 `LabRegisterReq` 扩展 target/identity 字段（web）
- [ ] B1 `LabSessionHolder`（新增，integration）：Snapshot + apply/reset/current + `hasOverride`
- [ ] B2 `LabSipClient` 注入 `LabSessionHolder`，`buildTo()`/`buildFrom()` holder 优先回退 props（ip/port 恒 5061）
- [ ] B3 `VoglanderClientDeviceSupplier` `@Autowired(required=false) ObjectProvider<LabSessionHolder>` 字段注入 + 401 兜底认外部 serverId（**关键**）
- [ ] B4 `LabChannelHolder`（新增，integration）+ `LabQueryListener`/`LabSipClient.pushCatalog` 读 holder（默认 4/`Lab-ch` 零回归）
- [ ] B4 通道名拼接统一为 `prefix + i`（去掉 `pushCatalog` 旧的 `-ch`）；`LabCatalogPushReq.catalogName` 默认值改 `Lab-ch`；controller `/catalog/push` 默认 name 用 `LabChannelHolder.DEFAULT_NAME_PREFIX`
- [ ] B5 `LabClientController`：register 写 holder（带参 apply/不带 reset）、unregister reset、catalog/push 写 `LabChannelHolder`、`/config` 增 serverDomain/transport/targetCustomized 生效值
- [ ] `mvn clean -pl voglander-integration -am install` 重装
- [ ] 后端测试（session-holder/channel-holder/sipclient/supplier/querylistener/controller）全绿
- [ ] F1 `protocol-lab.ts` 扩展 `RegisterReq` + `LabConfig`（对齐 Map key）
- [ ] F1 `ClientPanel.vue` 可折叠「注册信息」表单 + 自定义徽标
- [ ] i18n `protocolLab.register.*`（zh-CN/en-US）
- [ ] 前端门禁：`pnpm typecheck` / `eslint` / 根目录 `pnpm build:antd`
- [ ] 手工联调：自环回归 + 外部平台对接 + 模拟通道可配各一遍
```
