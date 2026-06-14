---
name: debug-sip-comm
description: 排查 voglander GB28181/SIP 通信问题（注册失败、设备不上线、server 端收不到推送、信令握手中断、回调报错）的系统化方法论。当用户说「设备注册不上」「server 端没看到消息/推送」「SIP 报错」「401 后没反应」「Cannot find cache」「lab 自环不通」「发起注册无响应」时使用本 skill。
---

# GB28181 / SIP 通信问题排查

voglander 的 SIP 链路跨 **运行实例 + ~/.m2 框架 jar + 多 profile 配置 + DB/缓存**，问题往往不在第一眼看到的报错处。本 skill 给出**先定位真正阻塞点、再动手**的排查顺序，避免在表象上空转。

核心心法：**报错信息 ≠ 根因。先读日志建时间线，再用反编译核实框架真实行为，最后才改代码。**

---

## 第 0 步（最易被忽略，最先查）：运行的到底是哪份代码？

⚠️ **voglander-web 运行时从 `~/.m2` 的 jar 加载兄弟模块（repository/manager/service/integration…），不是从它们的 `target/classes`。**

后果：你改了 `voglander-integration` 的源码、`mvn compile` 甚至看到 `target/classes` 已更新，但**运行实例仍是旧行为** —— 因为它吃的是 m2 里的旧 jar。这会让你误判"修复没生效 / 根因没找对"，浪费整轮排查。

```bash
# 1) 运行实例从哪加载兄弟模块？(看 classpath 里 voglander-* 是 target/classes 还是 .m2 jar)
ps aux | grep ApplicationWeb | grep -v grep | tr ':' '\n' | grep "voglander-"

# 2) m2 jar 是否含你的修复？(以 application-repo.yml 或某个类为例)
unzip -p ~/.m2/repository/io/github/lunasaw/voglander-repository/*/voglander-repository-*.jar application-repo.yml | grep -A1 cache
unzip -p ~/.m2/repository/io/github/lunasaw/voglander-integration/*/voglander-integration-*.jar \
  'io/.../YourClass.class' > /tmp/c.class && javap -p /tmp/c.class | grep yourNewMethod

# 3) jar 重建时间 vs 进程启动时间 —— 进程启动早于 jar 重建 = 跑的是旧 jar
stat -f "%Sm %N" ~/.m2/repository/io/github/lunasaw/voglander-integration/*/voglander-integration-*.jar
ps -o lstart= -p <PID>
```

**改了非 web 模块（repository/manager/service/integration/common），必须 `mvn -pl <module> -am install` 重装 jar 后重启**。光编译/光重启 IDE 无效。详见 [[voglander-cache-manager-hijack]] 的 build-order gotcha。

---

## 第 1 ���：读日志，建时间线

voglander 日志在 `~/logs/voglander/`：

| 文件 | 内容 |
|---|---|
| `voglander-info.log` | 启动、active profile、业务 INFO、ZLM hook |
| `voglander-error.log` | 异常栈、WARN（设备不存在/鉴权失败等） |
| `voglander-sip.log` | **SIP 原始报文**（REGISTER/MESSAGE/INVITE 全文 + 401/200） |

```bash
# active profile —— 决定哪些 application-*.yml 生效，是配置类问题的第一线索
grep -i "profiles are active\|Started ApplicationWeb\|Tomcat started" ~/logs/voglander/voglander-info.log

# 报错时间 + 类型；注意区分"旧实例关闭噪音"(LettuceConnectionFactory STOPPING/STOPPED)与真错误
grep -n "ERROR\|Exception\|Caused by" ~/logs/voglander/voglander-error.log | head

# 关键：把 [进程启动] [jar 重建] [报错] 三个时间戳排成线
# 报错在"旧实例"窗口内 → 可能修复已就绪只是没重启(回第 0 步)；报错在最新实例 → 是真问题
```

**建时间线的意义**：本类问题最常见的假象是"代码已修但运行没变"。先确认报错来自**当前实例**，再继续往下查。

---

## 第 2 步：SIP 注册握手走到哪一步？（看 sip.log 原始报文）

GB28181 注册是 **挑战-应答** 两轮，缺任何一环 server 都不会上线：

```
Client → REGISTER (无 Authorization)           ← 第 1 轮
Server → 401 Unauthorized + WWW-Authenticate(nonce)
Client → REGISTER (带 Authorization: Digest…)   ← 第 2 轮（带鉴权重发）
Server → 200 OK                                 ← 注册成功，此后才写 tb_device、发 register/online 事件
```

```bash
# 看握手停在哪一步
grep -nE "REGISTER sip|SIP/2.0 401|SIP/2.0 200|WWW-Authenticate|Authorization" ~/logs/voglander/voglander-sip.log

# 从 To/From 头分清角色(极重要，常被搞反):
#   To:   <sip:平台serverId@...>   ← 注册【目标】=平台自身，平台不注册自己、不在 DB
#   From: <sip:设备clientId@...>   ← 被注册的【设备】，注册成功后才落 tb_device
```

**判读**：
- 只有 REGISTER + 401、**没有第 2 轮带 Authorization 的 REGISTER** → 断在客户端"鉴权重发"。这一步框架会去取目标设备信息（见第 3 步），最易卡。
- 有第 2 轮但无 200 → 服务端鉴权/校验拒绝（看 error.log 的 `鉴权失败`/`设备不存在`）。
- 完全没有 REGISTER → 客户端没发出（端口未绑定/`@EnableSipServer` 缺失/未触发注册）。

> "server 端看不到推送/消息"几乎都是握手没到 200 → 设备没上线 → 后续 keepalive/catalog MESSAGE 被 `checkDevice` 以"设备不存在���未注册"挡掉。**先修注册，下游自然通。**

---

## 第 3 步：核实框架真实行为（反编译，别信源码）

GB28181 框架（`gb28181-client/server`、`sip-common`）以 **jar** 形式从 ~/.m2 引入，**本地 sip-proxy 源码可能与 jar 版本不同步**（pom 钉 1.8.0）。要确认框架到底调了你哪个回调、传了什么参数，**反编译实际加载的 jar**：

```bash
# 定位实际版本(pom 钉的，classpath 里那个)
ls ~/.m2/repository/io/github/lunasaw/gb28181-client/

# 解出类并反编译(javap)，看调用链/参数来源/默认实现
cd /tmp
JAR=~/.m2/repository/io/github/lunasaw/gb28181-client/1.8.0/gb28181-client-1.8.0.jar
unzip -o -q $JAR 'io/.../XxxProcessor.class' -d dec
javap -p -c -l dec/io/.../XxxProcessor.class | grep -iE "invoke|getfield|line [0-9]+|getDevice|authenticate|ireturn"
```

本类问题反编译要重点确认的几件事：
- **客户端 401 重发**：`ClientRegisterResponseProcessor.processReAuthentication` 用 **响应 To 头 userId**（=平台 serverId）去调 `ClientDeviceSupplier.getDevice/getToDevice`。
- **服务端鉴权**：`ServerDeviceSupplier.authenticate()` 的**默认实现字节码可能是 `iconst_1; ireturn`（恒 true，无条件放行）** —— 若如此，密码/realm 配置不影响握手成败，别在凭证上空耗。
- **CacheManager 抢占**：`sip-common` 的 `CacheConfig` 用 `@ConditionalOnMissingBean(CacheManager.class)` 注册非动态 `ConcurrentMapCacheManager`（仅 `devices/subscribes/transactions/sipMessages`），抢占后 voglander 自有缓存区 `device/mediaNode/...` getCache 报 `Cannot find cache named 'device'`。详见 [[voglander-cache-manager-hijack]]。

---

## 第 4 步：DeviceSupplier 与 DB —— "查不到设备"类问题

voglander 用 `VoglanderClientDeviceSupplier` / `VoglanderServerDeviceSupplier`（标 `@Primary`）对接框架，内部走 `DeviceManager.getDtoByDeviceId` 查 `tb_device`。

```bash
# 确认哪份 app.db 是活动库(工作目录通常是 voglander-web)
find . -name "app.db" -not -path "*/target/*"
sqlite3 voglander-web/app.db "SELECT device_id,name,status,ip,port FROM tb_device;"
```

**关键区分（本次 lab 自环踩坑点）**：
- 框架重发鉴权查的是**注册目标 = 平台自身 serverId**。**平台不会注册自己 → DB 永远没这条 → getDevice 返回 null → toDevice=null → 重发构造不出来 → 握手断**。
- 解法：supplier 在 DB miss 且 `deviceId == serverProperties.serverId` 时，从 `VoglanderSipServerProperties` 构造目标 ToDevice 兜底（镜像 `LabSipClient.buildTo()`），仅命中"目标=平台自身"，普通设备查不到仍返回 null（不污染常规路径）��
- 对照：被注册的**设备** ID 注册成功后才写 DB —— 那是握手成功的**结果**，不是前提。别用"种设备到 DB"去解平台自身解析问题。

---

## 第 5 步：配置绑定链 —— "配了但没生效"

voglander 配置有**占位符中继**，前缀容易对不上：

- properties 类前缀是 `sip.client` / `sip.server`（`VoglanderSipClientProperties` 等），
- 但用户在 yml 写的是 `local.sip.client.*`，
- 由 `application-inte.yml` 用 `${local.sip.client.password:123456}` **中继**到 `sip.client.password`。

```bash
# 查 properties 类前缀 + 默认值(默认值可能恰好掩盖"绑定其实没生效")
grep -n "@ConfigurationProperties\|private.*=" \
  voglander-integration/.../config/properties/VoglanderSipClientProperties.java

# 查中继链：application-inte.yml 里 sip.* 是怎么从 local.sip.* 取值的
grep -n "local.sip\|password\|realm" voglander-integration/src/main/resources/application-inte.yml
```

⚠️ **profile 专属配置只在对应 profile 激活时加载**（如 `spring.cache.type=redis` 在 `application-repo.yml`，要 `repo` profile；lab 配置要 `lab` profile）。第 1 步查到的 active profile 在此对账。lab 自环联调须 `dev,repo,inte,lab` 全开。

---

## 启动配置必查项（缺一即静默关闭信令管线）

见 voglander/AGENTS.md「GB28181 集成架构」，最常见致命缺失：

- `ApplicationWeb` 必须 `@EnableSipServer`（`@Import` 了 `CommandStrategyFactory`/`ServerCommandSender`/`ClientCommandSender` 等；缺则 `Gb28181GatewayAutoConfiguration` 因 `@ConditionalOnBean(ServerCommandSender)` 静默关闭整条事件管线）。
- `VoglanderBusinessNotifier.notify` 必须 `@Async("sipNotifierExecutor")`（同步会致设备超时重传）；**不要**继承 `AbstractProtocolBusinessNotifier`（其 `notify()` 为 final、内部自调用，`@Async` 代理失效）。
- `start/ServerStart`（CommandLineRunner）绑定 SIP 端口，全工程唯一 `addListeningPoint` 处，**不可删**。
- supplier 标 `@Primary`，否则与框架默认 supplier 冲突 `NoUniqueBeanDefinitionException`。

---

## 排查决策树（速查）

```
报错/不上线
├─ "Cannot find cache named 'device'"
│   → CacheManager 被 sip-common 抢占。查 spring.cache.type=redis 是否在 active profile + RedisConfig @Primary。
│     先确认运行的是新 jar(第 0 步)！[[voglander-cache-manager-hijack]]
├─ server 端收不到推送 / 设备不上线
│   → sip.log 看握手停在哪(第 2 步)
│     ├─ 卡在 401 后无重发 → getDevice 返回 null?(第 4 步) 目标=平台自身→supplier 兜底
│     ├─ 有重发无 200 → 服务端校验(第 3 步 authenticate 默认 true?)
│     └─ 无 REGISTER → @EnableSipServer/端口绑定/未触发
├─ "改了代码没变化"
│   → 第 0 步：跑的是 m2 旧 jar。mvn -pl <module> -am install 重装+重启
└─ "配了 yml 没生效"
    → 第 5 步：前缀中继链 + active profile
```

---

## 检查清单

- [ ] 确认运行实例加载的是含修复的 **m2 jar**（非 target/classes），进程启动晚于 jar 重建（第 0 步）
- [ ] 三时间戳（进程启动/jar 重建/报错）排线，确认报错来自**当前实例**
- [ ] active profile 与所需配置（repo→redis、lab→自环）对账
- [ ] sip.log 定位握手停在 REGISTER / 401 / 重发 / 200 的哪一环
- [ ] 从 To/From 头分清"注册目标(平台)"与"被注册设备"，没搞反
- [ ] 涉及框架行为时已 `javap` 反编译**实际 jar 版本**核实（非本地源码）
- [ ] DeviceSupplier 查 DB 走对了库（哪份 app.db）
- [ ] 改非 web 模块后已 `mvn -pl <module> -am install` 重装并重启

## 常见坑

- **修复看起来没生效**：跑的是 m2 旧 jar。这是本类问题头号时间黑洞，永远先查第 0 步。
- **在凭证上空耗**：服务端 `authenticate()` 默认 `return true`，密码/realm 不影响握手；先 javap 确认再决定要不要碰凭证。
- **把"注册目标"当成"被注册设备"**：平台自身 serverId 不在 DB 是正常的，别去种 DB；要的是 supplier 兜底解析。
- **本地 sip-proxy 源码 ≠ 运行的 jar**：pom 钉 1.8.0，反编译 jar 才是真相，读 sip-proxy/ 源码可能误导。
- **profile 没全开**：lab 自环少了 `lab` profile，或缓存少了 `repo` profile，配置类条件不命中而静默降级。
- **被旧实例关闭噪音误导**：error.log 里 `LettuceConnectionFactory STOPPING/STOPPED` 多为上一个实例关闭过程，按时间线排除。
