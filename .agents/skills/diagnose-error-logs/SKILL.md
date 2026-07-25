---
name: diagnose-error-logs
description: 'voglander 日志分诊与跨应用、前后端关联排查。用于 error/WARN 激增、启动失败、页面/API 4xx/5xx、上传失败、Spring 参数绑定、请求头/请求体或前后端契约异常；区分既有噪音、当前实例、前端、voglander、sip-proxy、ZLM 与外部依赖根因。'
---

# error 日志排查 + 多应用关联分析

voglander 工作区是 **一个集成应用（voglander）+ 三个上游依赖（sip-proxy / zlm-starter 以 jar 嵌入，vue-vben-admin 前端）+ 若干外部进程（ZLMediaKit / Redis / MySQL / SIP 对端设备）**。一条 ERROR 的"出处 logger"未必是它的"应用归属"，更未必是"根因应用"。

核心心法：**ERROR 行数 ≠ 问题严重度。绝大多数 ERROR 是既有的真实‑SIP 测试/运行噪音；先建基线做差集，找出"新增/异常"的那几条，再按 logger 前缀归因、跨应用关联。**

> 本 skill 是**通用日志分诊**方法论。若已确定是 GB28181/SIP 信令链路问题（注册不上、握手中断、收不到推送），直接用 [[debug-sip-comm]]，那里有 SIP 专项排查。本 skill 负责"先分诊出是哪类、哪个应用的问题"。

---

## 第 0 步（最关键认知）：先建基线，再谈"有没有问题"

⚠️ **本工作区的 error 日志天然有数千行 ERROR——这是既有噪音，不是回归。**

实测：`mvn clean test` 跑完，`~/logs/voglander-test/voglander-test-error.log` 在**未改动任何代码的 baseline** 下就有 **~6566 行 ERROR**，绝大部分来自真实‑SIP 测试基架故意走的失败路径：

| 主要噪音 logger | 行数量级 | 性质 |
|---|---|---|
| `i.g.l.g.c.t.r.m.ClientMessageRequestProcessor`（处理 MESSAGE 异常） | 数千 | 真实‑SIP 跨设备 MESSAGE 失败路径，**既有噪音** |
| `i.g.l.g.s.t.r.r.ServerRegisterRequestProcessor`（发送认证挑战失败/处理 REGISTER 异常） | 数千 | 注册挑战‑应答的中间态，**既有噪音** |
| `i.g.l.v.s.l.DeviceRegisterServiceImpl`（设备操作失败） | 数百 | 测试负路径（故意触发失败），**既有噪音** |
| `i.g.l.v.m.manager.DeviceManager`（新增/更新设备失败） | 数百 | 同上，**既有噪音** |
| `Transaction exists -- cannot send response statelessly` | **136 → 6001 不等** | 与传输无关、**逐次运行剧烈波动**的 SIP 重传噪音，**纯红鲱鱼，别追** |

**∴ 看到"几千行 ERROR"先别慌**。正确动作是**做差集**，只看相对基线**新增的 / 来自当前实例的 / 非已知噪音 logger 的**那几条。

```bash
# 1) 总量（先有数量级感觉，但数量本身不说明问题）
grep -c " ERROR " ~/logs/voglander-test/voglander-test-error.log

# 2) 按 logger 聚类——立刻看出是不是全是已知噪音
grep " ERROR " ~/logs/<dir>/<app>-error.log \
  | sed -E 's/.* ERROR +([^ ]+) +-.*/\1/' | sort | uniq -c | sort -rn | head -15

# 3) 差集：排除已知噪音 logger，剩下的才值得查
grep " ERROR " ~/logs/<dir>/<app>-error.log \
  | grep -vE "ClientMessageRequestProcessor|ServerRegisterRequestProcessor|ServerMessageRequestProcessor|RedisInviteContextStore|CascadeClientScheduler|CascadeMediaInviteListener|DeviceRegisterServiceImpl|DeviceManager|VoglanderServerDeviceSupplier|AbstractVoglanderServerCommand|EventShard|RedisLockUtil|SqliteSchemaInitializer" \
  | head
#   ↑ 这条返回空 = 没有新错误，日志"干净"（按既有噪音口径）。返回内容 = 真正要查的。
```

> 想知道"某条 ERROR 是不是我刚引入的"？**stash 改动、重跑、数同口径**，差集即归因。本工作区曾靠这招证明 4639 条 `Transaction exists` 是既有的而非新增（baseline 6001 反而更多）。

---

## 日志地图：哪个文件、哪个应用

所有 JVM 日志走 logback，落在 `${user.home}/logs/${spring.application.name}/`：

| 目录 / 文件 | 来自 | 看什么 |
|---|---|---|
| `~/logs/voglander/voglander-{info,error,sip}.log` | voglander **生产运行**实例 | 真实运行问题 |
| `~/logs/voglander-test/voglander-test-{info,error,sip}.log` | `mvn test`（profile=test，app name=voglander-test） | 测试问题 |
| `~/logs/spring.application.name_IS_UNDEFINED/...` | **`spring.application.name` 未绑定**时的兜底目录 | ⚠️ 它的存在本身=某次启动 profile/配置没加载到 name，是**配置漏加载的症状**，不是正常日志 |
| `*-info.log` | — | 启动、active profile、`Started ApplicationWeb`、业务 INFO、ZLM hook |
| `*-error.log` | — | 异常栈 + **WARN**（注意：error appender 收 WARN 级以上，所以这里混着 WARN） |
| `*-sip.log` | — | **SIP 原始报文**（REGISTER/MESSAGE/INVITE 全文 + 401/200），SIP 问题主战场 |

```bash
ls -dt ~/logs/*/                                  # 有哪些应用名的日志目录（按时间）
ls -lt ~/logs/voglander/                          # 哪个 *.log 最新（对上你这次运行）
```

**外部进程的日志不在这里**：ZLMediaKit 有自己的日志/控制台；Redis、MySQL 各自的日志；SIP 对端设备的报文要在 `voglander-sip.log` 里看本端收发。多应用排查时这些是"对端视角"。

---

## logger 前缀 → 应用归因（多应用排查的钥匙）

voglander 进程内同时跑着自身代码 + 上游框架 jar，**靠 logger 包前缀区分是谁报的**：

| logger 前缀（缩写形式） | 实际包 | 归属应用 | 改它要做什么 |
|---|---|---|---|
| `i.g.l.v.*` | `io.github.lunasaw.voglander.*` | **voglander 自身** | 直接改源码；非 web 模块需 `mvn -pl <m> -am install` 重装 |
| `i.g.l.g.*` | `io.github.lunasaw.gbproxy.*` | **sip-proxy**（gb28181-client/server，jar） | 本地源码≠jar！pom 钉版本，要 `javap` 反编译 jar 核实 |
| `i.g.l.s.*` | `io.github.lunasaw.sip.*` | **sip-proxy**（sip-common，jar） | 同上 |
| zlm 相关（`...zlm...`、`VoglanderZlmHook*`） | zlm-starter（jar）+ 外部 ZLMediaKit | **zlm-starter / 外部媒体** | 区分是 starter 代码报错还是外部 ZLM 进程不可达 |
| 框架/Spring/Lettuce/JAIN-SIP | 第三方 | **外部依赖** | 多为对端不可达（Redis/DB/ZLM/SIP 设备） |

```bash
# 一眼看清这批 ERROR 主要是哪个应用引发的
grep " ERROR " ~/logs/<dir>/<app>-error.log \
  | sed -E 's/.* ERROR +(i\.g\.l\.[a-z])\.[^ ]+.*/\1/' | sort | uniq -c | sort -rn
#  i.g.l.v 多 → voglander 业务；i.g.l.g / i.g.l.s 多 → SIP 框架链路（转 debug-sip-comm）
```

⚠️ **"出处 ≠ 根因"**：`i.g.l.g.*`（sip-proxy 框架）报的异常，根因常在 voglander 喂给它的参数 / supplier 返回 null / 配置没绑上。框架行为存疑时务必 `javap` 反编译**实际加载的 jar 版本**（见 [[debug-sip-comm]] 第 3 步），别信本地 sip-proxy 源码（pom 钉 1.8.0，可能不同步）。

---

## 第 1 步：建时间线，锁定"当前实例"的错误

本工作区头号假象是"改了代码但运行/日志没变"——因为 **voglander-web 运行时从 `~/.m2` jar 加载兄弟模块**，跑的可能是旧字节码。

```bash
# active profile（决定哪些 application-*.yml 生效，配置类问题第一线索）
grep -i "profiles are active\|Started ApplicationWeb" ~/logs/voglander/voglander-info.log | tail

# 三时间戳排线：进程启动 / jar 重建 / 报错时间
ps -o lstart=,pid= -p $(pgrep -f ApplicationWeb)                          # 进程启动
stat -f "%Sm %N" ~/.m2/repository/io/github/lunasaw/voglander-*/*/*.jar   # jar 重建
grep -n "ERROR\|Caused by" ~/logs/voglander/voglander-error.log | tail    # 报错时间
#  报错时间 < 进程启动 → 旧实例残留噪音，排除；改了非 web 模块没重装 jar → 你看的修复根本没跑
```

详见 [[voglander-web-tests-use-installed-integration-jar]] 与 [[debug-sip-comm]] 第 0 步。

---

## 第 2 步：判定这是"噪音 / 测试负路径 / 真故障"

对差集筛出的每条可疑 ERROR，问三个问题：

1. **是测试故意触发的负路径吗？** 上下文有对应的"应失败"用例（如 `*FailureTest`、断言异常）→ 噪音。`DeviceManager -新增设备失败 - 错误:`（错误为空）多属此类。
2. **是上一个实例关闭的噪音吗？** `LettuceConnectionFactory STOPPING/STOPPED`、context closing → 按时间线落在旧实例窗口 → 排除。
3. **是"对端不可达"吗？** `Unable to connect to Redis` / `Connection refused` / `timed out` → **外部依赖问题**，不是 voglander 代码 bug。转"第 3 步多应用关联"。

只有都答"否"、且来自当前实例、且非已知噪音 logger 的，才是要深挖的真故障。

---

## 第 3 步：多应用关联——一个现象，串起多方日志

很多"报错"是**跨应用的链路断点**，单看一处日志看不全。按链路把多方日志在**同一时间戳**对齐：

### 链路 A：前端点了没反应 / 接口报错
```
vue-vben-admin(浏览器 Network/Console)  →  voglander REST(/api/*, /zlm/api/*)  →  下游
```
- 先从当前实例日志锁定**时间、URI、异常类型和重复次数**，再查浏览器；没有同刻后端记录时，优先查 baseURL、代理、CORS 或请求是否命中别的实例。
- 按固定证据顺序核对：**后端 Controller 契约 → 前端 API 调用 → RequestClient 默认配置/拦截器 → Axios/Fetch 实际转换 → 浏览器 Network**。源码里构造了 `FormData`，不等于线上发出的就是 multipart。
- Controller 参数解析器抛错且业务方法没有入栈，说明请求在参数绑定前失败；先查 method、path、`Content-Type`、header、query/body/multipart，不要跳到 service。
- 4xx/参数错通常是传输或契约问题；5xx 结合 Controller/Service/最底层 `Caused by` 归因；HTTP 200 但 `AjaxResult.code≠0` 是业务拒绝，不当作业务成功。
- 完整命令、异常分类、Axios 转换验证和验收门禁见 [前后端请求关联排查](references/fullstack-request.md)。

### 链路 B：设备不上线 / 收不到推送（SIP）
```
SIP 设备/对端  →(报文)→  voglander-sip.log  →  gbproxy 框架(jar)  →  voglander supplier/notifier  →  DB/缓存
```
- 主战场是 `voglander-sip.log`（握手停在 REGISTER/401/重发/200 哪环）。**直接转 [[debug-sip-comm]]**，那里是 SIP 专项全流程。
- error.log 里 `i.g.l.g.*`/`i.g.l.s.*` 的异常要结合 sip.log 同刻报文看，不要孤立读。

### 链路 C：流代理 / 媒体（ZLM）
```
前端 /zlm/api/proxy/add  →  voglander 代理  →  外部 ZLMediaKit  →  ZLM Hook 回调  →  VoglanderZlmHookServiceImpl 落库
```
- 先分清是 **starter 代码**报错（`i.g.l.*` zlm 相关）还是**外部 ZLM 进程**不可达（Connection refused / 超时）。
- Hook 不回 → 看 ZLM 是否真发了回调（外部 ZLM 日志）vs voglander Hook 端点是否收到（info.log）。
- 配置见 `application-dev.yml` 的 `zlm:`；`ZlmIntegrationConfig#getDefaultServer()` 取默认节点。

### 链路 D：启动就报错 / context 起不来
```
某个 bean 创建失败  →  级联 UnsatisfiedDependency  →  整个 context 加载失败  →  该 context 下所有测试瞬间(0s)失败
```
- **大量测试 0s 失败 = 共享 context 一次加载失败被缓存**，根因是**单个** bean。顺着 `Caused by` 链找到**最底层那条**（往往一句 `Unable to connect to Redis` / `Cannot find cache named` / SQLite 建表不全）。
- 起 Redis：`brew services start redis`；`redisBackedSseEventBus` 是**硬启动依赖**，Redis 不可达会拖垮整个 web context（实测一次 1s 超时即雪崩）。见 [[voglander-cache-manager-hijack]]。

---

## 排查决策树（速查）

```
"日志一堆 ERROR / 报错排查"
├─ 还没建基线？ → 第 0 步：grep -c + 按 logger 聚类 + 差集排除已知噪音
│                  剩 0 条 = 干净(既有噪音口径)，别再追数量
├─ 页面/API/上传报错 → 链路 A + references/fullstack-request.md
│   ├─ 后端无同刻日志 → baseURL/代理/CORS/错误实例/旧前端产物
│   ├─ 绑定前异常或 400/415 → 对照 Controller 与实际 method/path/header/body/Content-Type
│   ├─ 5xx 且进入业务栈 → 追最底层 Caused by 和下游依赖
│   └─ HTTP 成功但页面失败 → AjaxResult 解包/业务码/类型映射/状态管理
├─ 差集后仍有可疑条目
│   ├─ logger 是 i.g.l.v.* → voglander 自身，读栈改码(非web模块记得重装jar)
│   ├─ logger 是 i.g.l.g.*/i.g.l.s.* → SIP 框架链路 → [[debug-sip-comm]]
│   ├─ zlm/ZLM 相关 → 链路 C：分清 starter 代码 vs 外部 ZLM 进程
│   └─ Redis/DB/Connection refused/timeout → 外部依赖不可达，非代码 bug
├─ 大量测试 0s 失败 → 链路 D：单个 bean 拖垮共享 context，找最底层 Caused by
└─ "改了没变化 / 修复没生效" → 同时核对 m2 后端 jar、前端 dist、运行进程和浏览器缓存
```

---

## 检查清单

- [ ] 先 `grep -c " ERROR "` + 按 logger 聚类，对"几千行"有基线认知，**没把既有噪音当回归**
- [ ] 做**差集**（排除已知噪音 logger），确认是否真有"新增/异常"条目
- [ ] 三时间戳（进程启动/jar 重建/报错）排线，确认报错来自**当前实例**而非旧实例残留
- [ ] 对每条可疑 ERROR 判定：测试负路径？关闭噪音？对端不可达？——再决定深挖
- [ ] 用 logger 前缀把错误**归因到应用**（v=voglander / g,s=sip-proxy / zlm / 外部）
- [ ] 跨应用现象按链路（A 前端 / B SIP / C ZLM / D 启动）在**同一时间戳**对齐多方日志
- [ ] Web 请求问题已逐项对照 Controller 与实际请求的 method、path、Content-Type、header、query/body 和响应状态/业务码
- [ ] 已追到前端调用组件、API 封装、RequestClient 默认头/拦截器和 Axios/Fetch 转换，没把“源码对象正确”当成“线上报文正确”
- [ ] 前后端源码均正确时已核对后端进程/JAR、前端 dist、请求命中实例和浏览器缓存
- [ ] 框架（i.g.l.g/i.g.l.s）行为存疑时 `javap` 反编译**实际 jar 版本**核实，别信本地源码
- [ ] 启动雪崩追到**最底层 Caused by**（单个 bean），而非满屏 UnsatisfiedDependency 表象

## 常见坑

- **被 ERROR 总量吓到**：baseline 就有 ~6.5k 行，全是真实‑SIP 测试噪音。先差集，再判断。
- **追 `Transaction exists` 红鲱鱼**：与传输无关、逐次运行 136~6001 剧烈波动的既有噪音，不是 bug，别追。
- **把"出处 logger"当"根因应用"**：`i.g.l.g.*` 报错根因常在 voglander 喂的参数/supplier 返回 null/配置没绑上。
- **error.log 里混着 WARN**：error appender 收 WARN 级以上，别把 WARN（设备不存在/鉴权失败）一律当致命错误。
- **`spring.application.name_IS_UNDEFINED` 目录**：它存在本身=某次启动没绑上 app name（profile/配置漏加载），是症状不是正常日志。
- **跑的是 m2 旧 jar**：改了非 web 模块没 `mvn -pl <m> -am install`，日志反映的是旧字节码，误判"没修好"。
- **把 `FormData` 源码当 multipart 证据**：默认 JSON 请求头或拦截器可能在 Axios 转换阶段把它序列化；必须看转换后配置或浏览器 Network。
- **参数绑定失败却追 service**：栈停在 `HandlerMethodArgumentResolver` 时业务方法尚未执行，先查传输契约。
- **只核对源码不核对部署产物**：前端可能仍服务旧 dist，后端可能仍运行旧 jar；两边代码都“正确”也不能证明线上请求正确。
- **外部进程当成 voglander bug**：Redis/ZLM/MySQL/SIP 设备不可达是"对端"问题，看对端日志，别在 voglander 代码里空耗。
- **孤立读一处日志**：跨应用链路（前端↔后端↔框架↔外部）要同时间戳对齐多方，单看一处永远缺一环。
