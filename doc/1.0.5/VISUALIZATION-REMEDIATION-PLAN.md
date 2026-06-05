# GB28181 可视化后端 1.0.5 — 缺陷修复与全链路验收方案

> 制定日期：2026-06-05
> 依据：[VISUALIZATION-ACCEPTANCE-REPORT.md](VISUALIZATION-ACCEPTANCE-REPORT.md)（commit `2e2fea7` 验收）
> 目标：闭合验收报告 §六 全部缺陷（D1–D8），并建立**可重复执行的全链路验收检查**（自动化 E2E + 真实联调清单），把一次性人工验收沉淀为 CI 级回归保护。
> 分支：`dev_merge_sip`

---

## 〇、总览

### 缺陷与处置矩阵

| 编号 | 缺陷 | 严重度 | 当前态 | 处置 | 阶段 |
|------|------|--------|--------|------|------|
| D1 | `application-inte.yml` 重复 YAML 键 → 启动 `DuplicateKeyException` | 🔴 P0 启动阻断 | 已改未提交 | 提交 | S0 |
| D2 | `gateway.nodes` Map 占位符无法绑定 | 🔴 P0 启动阻断 | 已改未提交 | 提交 | S0 |
| D3 | SQLite 建表脚本经 Spring 执行半途中断（17 张 tb_ 表只建出 15，`tb_cascade_platform`/`tb_cascade_channel` 缺失） | 🔴 P0 启动阻断 | 未修复 | 精准删诊断 SELECT + 建表后校验 + sentinel 改判 + 调度器容错 + 确定性测试 | S1 |
| D6 | PTZ 指令词表与枚举不匹配（`UP/DOWN/...` 全失败） | 🟡 P1 功能不可用 | 未修复 | 门面层翻译 + 预置位独立命令 | S2 |
| D4 | 回放 `/control` 占位桩，未真正下发 | 🟡 P1 误导前端 | 未修复 | 接通暂停/继续；seek/倍速分级实现或显式不支持 | S2 |
| D5 | GC 空闲回收定时窗口缺陷 + 未真正 BYE/关流 | 🟡 P2 资源悬挂 | 未修复 | TTL > GC 间隔 + 真实 `closeRtpServer`/BYE | S3 |
| D7 | 告警入站链路无单测 | 🟡 P2 回归裸奔 | 未修复 | 补 `Notify.Alarm` 路由断言 | S3 |
| D8 | 单机 SSE 事件重复投递 | 🟢 观察项 | 未修复 | 发布端回路抑制（origin 标记） | S3 |

### 发布门槛（Release Gate）
- **必修（阻断）**：D1、D2、D3 —— 否则 `dev,repo,inte` 生产 profile / 空库部署起不来。
- **强烈建议**：D6、D4 —— 否则控制/回放对前端不可用或误导。
- **可随后迭代**：D5、D7、D8。

### 全链路验收检查目标
报告 §四 的人工验收（真实 ZLM + ffmpeg + 手动 curl）不可重复、不入 CI。本方案在 S4 建立两层验收：
1. **确定性自动化 E2E**（无需真实 ZLM/SIP 设备，复用 `VoglanderBusinessNotifier` 注入事件 + 模拟 ZLM Hook POST）—— 进 CI，每次构建运行。
2. **真实联调清单**（真实 ZLM + ffmpeg）—— 脚本化 `doc/1.0.5/acceptance/`，供本机/预发回归，不进 CI。

---

## 一、S0：提交已就绪的启动阻断修复（D1 / D2）

`git status` 显示 `voglander-integration/src/main/resources/application-inte.yml` 已修改未提交（合并重复 `zlm:`/`gateway:` 键、删除无法绑定的 `gateway.nodes` 死配置行，14+/15-）。验收时已验证修复有效。

**动作**
1. 复核 `git diff voglander-integration/src/main/resources/application-inte.yml`，确认只含 D1/D2 两处合并/删除，无误植内容。
2. **`config.ini` 处置**：`git status` 显示 `config.ini` 同样被修改（61 行变更），属本机联调（ZLM Hook 地址/端口）残留，**不随本次缺陷修复提交**——提交前 `git checkout -- config.ini` 还原，或确认其为环境必需配置后单独说明。避免把本机联调值混入 D1/D2 提交。
3. 随 S1 一并提交（见末尾提交计划），commit message 标注「fix(1.0.5): application-inte.yml 重复键 + gateway.nodes 绑定（D1/D2 启动阻断）」。

**验证**：S4 的 `ConfigBootSmokeTest`（见四）以 `inte` 相关 profile 加载上下文不抛 `DuplicateKeyException`/`ConverterNotFoundException`。

---

## 二、S1：SQLite clone-and-run 建表可靠性（D3，发布门槛）

### 现状与根因
- [SqliteSchemaInitializer.java:85-91](../../voglander-repository/src/main/java/io/github/lunasaw/voglander/repository/config/SqliteSchemaInitializer.java#L85-L91) 用 `ResourceDatabasePopulator(continueOnError=false)` 执行 `classpath:db/voglander-sqlite.sql`。
- 现象：脚本共 **18 个 `CREATE TABLE`（17 张 `tb_` 业务表 + 1 张 `sequence` 序列表）**，但只建出 15 张即中断，`tb_cascade_platform`（[sql/voglander-sqlite.sql:681](../../sql/voglander-sqlite.sql#L681)）与其后的 `tb_cascade_channel`（L709，真正最后一张）未建。
- **隐性坏库**：但 sentinel 表 `tb_user`（建表序列靠前）已建 → 重启判定「已初始化」跳过 → schema 永久残缺。
- **吞异常掩盖**：当前 [SqliteSchemaInitializer.java:93-95](../../voglander-repository/src/main/java/io/github/lunasaw/voglander/repository/config/SqliteSchemaInitializer.java#L93) 是 `catch + log.error` **吞掉异常**，populate 半途失败时应用仍判「启动成功」，仅表残缺。真正的启动失败发生在 `CascadeClientScheduler.@PostConstruct`（[CascadeClientScheduler.java:51](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/cascade/CascadeClientScheduler.java#L51)）调 `cascadePlatformManager.getPage(...)` 查 `tb_cascade_platform` 报 `no such table` → 启动失败。
- **关键证据**：同一脚本经 `sqlite3 app.db < sql/voglander-sqlite.sql` 可建全 17 张 `tb_` 表（EXIT=0）。故脚本 DDL 本身可执行，问题在 Spring `ResourceDatabasePopulator`/`ScriptUtils` 的**语句切分**与脚本中的**纯诊断语句**。

### 脚本污染点（已逐条核查 —— 仅 2 处可删，4 处必须保留）
> ⚠️ 修订：原方案误把 6 处以 `SELECT` 开头的行全列为「诊断语句」，但其中 4 处是跨行 `INSERT ... SELECT` 种子语句的 SELECT 子句，**删除会破坏 RBAC 种子数据或造成语法错误**。逐条核查结论：

| 行号 | 实际语义 | 处置 |
|------|---------|------|
| [L473](../../sql/voglander-sqlite.sql#L473) `SELECT u.id, r.id` | `INSERT OR IGNORE INTO tb_user_role ... SELECT`（给 admin 分配角色）| **保留** |
| [L481](../../sql/voglander-sqlite.sql#L481) `SELECT r.id, m.id` | `INSERT OR IGNORE INTO tb_role_menu ... SELECT`（管理员角色配菜单）| **保留** |
| [L606](../../sql/voglander-sqlite.sql#L606) `SELECT 1, id` | `INSERT OR REPLACE INTO tb_role_menu ... SELECT`（分配新菜单权限）| **保留** |
| [L642](../../sql/voglander-sqlite.sql#L642) `SELECT 1, id` | `INSERT OR REPLACE INTO tb_role_menu ... SELECT`（分配推流代理菜单）| **保留** |
| [L649-663](../../sql/voglander-sqlite.sql#L649-L663) `SELECT m.id, ... json_extract(...)` | **纯诊断**「验证插入结果」，对建表零贡献 | **删除** |
| [L668-675](../../sql/voglander-sqlite.sql#L668-L675) `SELECT rm.role_id, ...` | **纯诊断**「验证角色权限分配」，对建表零贡献 | **删除** |

- **失败定位佐证**：脚本成功建到 15 张表（即建到 `tb_push_proxy`，[L518](../../sql/voglander-sqlite.sql#L518)），下一张 `tb_cascade_platform`（L681）未建。横亘在两者之间的正是 L649/L668 两个纯诊断 SELECT（含多行 `json_extract(m.meta, '$.title')`）。**前 4 处 `INSERT...SELECT` 均在 L518 之前已成功执行**，反证它们不是中断源、也不应删除。L649/L668 是高度可疑的切分混乱点。
- `PRAGMA foreign_keys` 反复开关：[L7](../../sql/voglander-sqlite.sql#L7) / [L543](../../sql/voglander-sqlite.sql#L543) / [L552](../../sql/voglander-sqlite.sql#L552) / [L723](../../sql/voglander-sqlite.sql#L723)。

### 修复设计（四管齐下，互为兜底）
**(1) 净化脚本 —— 只删 2 处纯诊断 SELECT + 中段重复 PRAGMA**
- 删除 [L649-663](../../sql/voglander-sqlite.sql#L649-L663)、[L668-675](../../sql/voglander-sqlite.sql#L668-L675) 两处纯诊断 `SELECT`（最可能的切分混乱源，含 `json_extract`）。
- **L473/L481/L606/L642 四处 `INSERT...SELECT` 种子语句一律保留**（删则 admin 丢角色/菜单权限或 INSERT 无值语法错误）。
- 保留首尾各一处 `PRAGMA foreign_keys = OFF/ON`（L7 / L723），删除中段重复开关（L543 / L552），减少语句数与状态切换。
- 净化后脚本应为「纯 DDL + 种子 INSERT/INSERT...SELECT」，与 `sqlite3` CLI 行为一致。

**(2) 建表后校验 —— 杜绝「半成品冒充已初始化」**
在 `SqliteSchemaInitializer.initSchemaIfEmpty()` 执行 `populate` 后，查询 `sqlite_master` 统计 `tb_` 开头表数量：
```java
int actual = countUserTables();                 // SELECT count(*) FROM sqlite_master WHERE type='table' AND name LIKE 'tb_%'
if (actual < EXPECTED_TABLE_COUNT) {             // EXPECTED_TABLE_COUNT = 17（tb_ 业务表数；sequence 表不在 tb_% 过滤内）
    throw new IllegalStateException(
        "SQLite 建表不完整：期望 " + EXPECTED_TABLE_COUNT + " 张 tb_ 表，实际 " + actual + "，疑似脚本切分失败");
}
```
> ⚠️ 修订：原方案 `EXPECTED_TABLE_COUNT = 18` 与 `LIKE 'tb_%'` 过滤**不一致**——18 是含 `sequence` 的 `CREATE TABLE` 总数，但 `tb_%` 只匹配 17 张业务表。若按 18 校验，即便建表完全成功也会 `actual=17 < 18` 必抛异常、永久起不来。**正确阈值为 17**（与 [SqliteSchemaInitializer.java:26](../../voglander-repository/src/main/java/io/github/lunasaw/voglander/repository/config/SqliteSchemaInitializer.java#L26) 类注释「17 张」、验收报告「全 17 张 tb_ 表」一致）。

同时把 `catch` 分支由「`log.error` 吞异常」改为 **rethrow（fail-fast）**，绝不留下「sentinel 在、表残缺」的隐性坏库。

**(3) sentinel 改判 + 失败可重试**
- sentinel 由 `tb_user` 改为**建表序列真正的最后一张表 `tb_cascade_channel`**（[L709](../../sql/voglander-sqlite.sql#L709)）：只有全表建成才视为已初始化，半成品库下次启动会重新尝试建表。
> ⚠️ 修订：原方案写 `tb_cascade_platform` 为「最后一张表」有误——它是**倒数第二张**（L681），其后还有 `tb_cascade_channel`（L709）。若以 `tb_cascade_platform` 为 sentinel，脚本恰好建到它而在 `tb_cascade_channel` 失败时仍会误判「已初始化」。**应取 `tb_cascade_channel`。**
- 重建前对残缺库执行幂等清理（脚本本身含 `DROP TABLE IF EXISTS`，天然幂等），保证重试安全。

**(4) `CascadeClientScheduler` 启动查询容错（次生问题）**
- [CascadeClientScheduler.@PostConstruct](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/cascade/CascadeClientScheduler.java#L51) 与 `SqliteSchemaInitializer` 无依赖顺序保证。加 `@DependsOn("sqliteSchemaInitializer")`（或查询前 try-catch 表不存在则跳过并告警），避免空库 + 二者 `@PostConstruct` 竞态。

### 涉及文件
- `sql/voglander-sqlite.sql`（净化；构建期会拷入 repository 模块 classpath，单一源）
- `voglander-repository/.../config/SqliteSchemaInitializer.java`（建表后校验 + fail-fast + sentinel 改判 + 类注释更新）
- `voglander-integration/.../gb28181/cascade/CascadeClientScheduler.java`

### 根因兜底说明
真正导致 `ResourceDatabasePopulator` 中断的语句**尚未逐条定位**（`json_extract` 为首要怀疑，但 xerial sqlite-jdbc 现代版本支持 JSON1，未必是它）。净化 L649/L668 后大概率消除；若仍中断，靠 `SqliteSchemaInitializerTest`（见四）复现并暴露具体缺表——**前提是上面阈值已改为 17，否则该测试在完美库上也会误红**。

### 验证
- **确定性测试**（新增 `SqliteSchemaInitializerTest`，见四）：对一个全新临时 SQLite 文件运行真实建表流程，断言 17 张 `tb_` 表全部存在、admin 种子存在。这把「sqlite3 CLI 能、Spring 不能」的差异变成 CI 红线。
- 手动：删 `app.db` → `mvn spring-boot:run -pl voglander-web`（默认 SQLite）→ 启动成功 + `tb_` 表数=17。

---

## 三、S2：控制链路修复（D6 PTZ 词表 / D4 回放 control）

### D6 — PTZ 指令词表翻译（P1，强烈建议）

**现状**
- [GbDeviceCommandService.ptzControl:69](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/command/impl/GbDeviceCommandService.java#L69) 直接 `PTZControlEnum.valueOf(req.getControl())`。
- 真实枚举（1.8.0 已反编译核对 `gb28181-common-1.8.0`）：`STOP / PAN_LEFT / PAN_RIGHT / TILT_UP / TILT_DOWN / PAN_LEFT_TILT_UP / PAN_RIGHT_TILT_UP / PAN_LEFT_TILT_DOWN / PAN_RIGHT_TILT_DOWN / ZOOM_IN / ZOOM_OUT`（另有组合 `PAN_RIGHT_TILT_UP_ZOOM_OUT`）。注意 `getByName(String)` 查的是枚举**中文 `name` 字段**（"停止"/"向左"…）而非枚举常量名，故不能用它来解析 `STOP`/`TILT_UP` 这类规范名（须用 `valueOf`）。
- [PtzControlReq](../../voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/ptz/domain/PtzControlReq.java) 注释词表为 `UP/DOWN/LEFT/RIGHT/UP_LEFT/...`，前端按此发送 → `valueOf("UP")` 抛 `No enum constant`。

**修复：门面层翻译（符合 CLAUDE.md「门面层翻译」约定）**
在 `GbDeviceCommandService.ptzControl` 引入显式映射表，先翻译再下发，并兼容传入规范枚举名：
```java
private static final Map<String, PTZControlEnum> PTZ_VOCAB = Map.ofEntries(
    Map.entry("UP",         PTZControlEnum.TILT_UP),
    Map.entry("DOWN",       PTZControlEnum.TILT_DOWN),
    Map.entry("LEFT",       PTZControlEnum.PAN_LEFT),
    Map.entry("RIGHT",      PTZControlEnum.PAN_RIGHT),
    Map.entry("UP_LEFT",    PTZControlEnum.PAN_LEFT_TILT_UP),
    Map.entry("UP_RIGHT",   PTZControlEnum.PAN_RIGHT_TILT_UP),
    Map.entry("DOWN_LEFT",  PTZControlEnum.PAN_LEFT_TILT_DOWN),
    Map.entry("DOWN_RIGHT", PTZControlEnum.PAN_RIGHT_TILT_DOWN),
    Map.entry("ZOOM_IN",    PTZControlEnum.ZOOM_IN),
    Map.entry("ZOOM_OUT",   PTZControlEnum.ZOOM_OUT),
    Map.entry("STOP",       PTZControlEnum.STOP));

private PTZControlEnum resolvePtz(String control) {
    if (control == null) {
        throw new ServiceException(ServiceExceptionEnum.PTZ_COMMAND_INVALID, "PTZ 指令为空");
    }
    String key = control.trim().toUpperCase();
    PTZControlEnum c = PTZ_VOCAB.get(key);
    if (c == null) {
        try {
            c = PTZControlEnum.valueOf(key);            // 兼容直接传规范枚举名（如 "TILT_UP"）
        } catch (IllegalArgumentException ignore) {
            // 落到下方统一异常
        }
    }
    if (c == null) {
        throw new ServiceException(ServiceExceptionEnum.PTZ_COMMAND_INVALID, "未知 PTZ 指令: " + control);
    }
    return c;
}
```
> ⚠️ 修订：原方案 fallback 用 `PTZControlEnum.getByName(control)` 是错的——`getByName` 查的 `NAME_MAP` 键是枚举的**中文 `name` 字段**（"停止"/"向左"/"向上"…），`getByName("STOP")` 必返回 `null`，无法兼容规范枚举名。要兼容枚举名须用 `PTZControlEnum.valueOf(key)` 并 try-catch（`valueOf` 对非法名抛 `IllegalArgumentException`，不返回 null）。已据此改正上方代码。
- 校验 `ServiceExceptionEnum` 是否已有 PTZ 非法枚举类型，无则按 CLAUDE.md 规范先补充（避免裸 `IllegalArgumentException` 直接 500）。
- 同步修正 `PtzControlReq` 注释，使契约文档与实现一致。

**预置位（preset）—— 本次按「档 B 保底」处置（已决策 2026-06-05）**
- 现状 [PtzWebAssembler.toPresetReq](../../voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/ptz/assembler/PtzWebAssembler.java) 构造 `control="PRESET_"+action`（`PRESET_SET/GOTO/DEL`），但 `PTZControlEnum` **无任何 PRESET 常量**，且 wrapper 仅有 `VoglanderServerDeviceCommand.queryDevicePreset`（查询），**无 SET/GOTO/DEL 下发命令**。预置位实为未实现。
- **决策：先保底返回不支持**——`PtzController.preset` 直接返回明确「预置位暂不支持」（`AjaxResult.error("预置位暂不支持")`），不再经 `ptzControl`/`PTZControlEnum.valueOf`（杜绝 500 假成功误导前端）。`PtzWebAssembler.toPresetReq` 可一并废弃或留空，避免构造无效 `PRESET_*` 串。
- **后续单独排期（档 A，非本次）**：在 `VoglanderServerPtzCommand` 新增 `preset(deviceId, action, presetId)`，用 gb28181 指令构造器（`PTZInstructionBuilder` 体系）生成 SET/GOTO/DEL 字节经 16 进制通道下发——涉及上游构造器能力确认，留待预置位需求正式排期时落地。

**涉及文件**：`GbDeviceCommandService.java`、`DevicePtzReq`/`PtzControlReq` 注释、（preset）`VoglanderServerPtzCommand.java` + `PtzController.java` + `PtzWebAssembler.java`、`ServiceExceptionEnum`。

### D4 — 回放 control 接通（P1，强烈建议）

**现状** [PlaybackController.control:49-54](../../voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/live/controller/PlaybackController.java#L49-L54) 仅 `log.info` 后 `return true`。

**已具备能力**
- `VoglanderServerMediaCommand.controlPlayBack(deviceId, PlayActionEnums)` 存在；`PlayActionEnums = PLAY_RESUME / PLAY_RANGE / PLAY_SPEED / PLAY_NOW`，与 `PlaybackControlReq.action` 一致。
- **限制**：envelope `gb28181.Invite.PlaybackControl` 的 payload **仅含 `action`**，不含 seek 位置/倍速值。故 `param`（seek 时间 / 倍速）当前无法透传。

**修复（分级）——本次只做 L1（已决策 2026-06-05：先接通暂停/继续）**
- **L1（本次实现，确定可行）**：暂停/继续 —— `PLAY_NOW`/`PLAY_RESUME` 无需 param。
  1. `DeviceCommandService` 新增协议无关方法 `ResultDTO<Void> controlPlayback(String streamId, String action, String param)`。
  2. `GbDeviceCommandService` 实现：由 `streamId` 经 `MediaSessionManager.getByStreamId/getByCallId` 反查 `deviceId`（会话不存在 → `LIVE_STREAM_NOT_FOUND`），再 `mediaCommand.controlPlayBack(deviceId, PlayActionEnums.valueOf(action))`。
  3. `PlaybackController.control` 改为调用该方法，按 `ResultDTO` 返回真实成败。
  4. **`PLAY_RANGE`/`PLAY_SPEED` 本次显式返回「暂不支持 seek/倍速」**（`AjaxResult.error`），不假成功。
- **L2（seek/倍速，后续单独排期，非本次）**：需把 `param` 透传进 envelope payload —— 须先确认 sip-gateway 1.8.0 `PlaybackControl` handler 是否接受 range/scale 字段。若支持：扩展 `controlPlayBack` 重载携带 param；留待该能力确认后落地。

**涉及文件**：`DeviceCommandService.java`、`GbDeviceCommandService.java`、`PlaybackController.java`、（L2）`VoglanderServerMediaCommand.java`。

---

## 四、S3：可靠性与回归保护（D5 / D7 / D8）

### D5 — GC 空闲回收定时窗口 + 真实关流（P2）

**现状**
- [MediaPlayServiceImpl.stopLive:196-197](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/live/impl/MediaPlayServiceImpl.java#L196-L197)：refCount 归零写 `live:pending_close:{streamId}` TTL=**30s**（`PENDING_CLOSE_SEC=30`）。
- [LiveSessionGcService.gc:40](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/live/LiveSessionGcService.java#L40)：`@Scheduled(fixedDelay=60_000)`，间隔 **60s** > TTL 30s → key 常在 GC tick 前过期 → `drainPendingClose` 扫不到 → SSE `live.closed` 不触发、ZLM 端口/会话悬挂。
- [drainPendingClose:53-69](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/live/LiveSessionGcService.java#L53-L69)：命中也只 `registry.remove()` + SSE，**未调 `closeRtpServer` / 未发 BYE**。

**修复**
1. **消除定时窗口**：`PENDING_CLOSE_SEC` 提至 **90s**（> GC 间隔 60s），保证至少一次 GC tick 能命中；TTL 仅作多节点兜底，不再是回收主路径。
2. **真实关流下沉到编排层**：在 `MediaPlayService` 新增 `void closeStream(String streamId)`，复用其节点解析（`getByStreamId → nodeServerId → ZlmNode`）：
   - `ZlmRestService.closeRtpServer(node.host, node.secret, streamId)`；
   - `voglanderServerMediaCommand.sendBye(callId)`（callId 取自会话）；
   - `mediaSessionManager` 标会话 `CLOSED`；
   - `sseEventBus.publish(live.closed)`；`liveStreamRegistry.remove(streamId)`；删 pending_close key。
3. `LiveSessionGcService.drainPendingClose` 命中 refCount=0 时改调 `mediaPlayService.closeStream(streamId)`（GC 保持瘦身，复用编排层节点能力）。

> （可选，§十 gap）ACTIVE-无流检测：GC 周期探测 ZLM `getMediaList`，ACTIVE 会话在 ZLM 已无流则触发 `closeStream`。本次可不做，列入后续。

**涉及文件**：`MediaPlayService.java` + `MediaPlayServiceImpl.java`、`LiveSessionGcService.java`。

### D7 — 告警入站路由单测（P2）

**现状** [Gb28181ProtocolHandler.handleAlarm:338-350](../../voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/handler/Gb28181ProtocolHandler.java#L338-L350) 链路完整（`Notify.Alarm → alarmManager.add + AlarmCreatedEvent`），但 `Gb28181ProtocolHandlerTest`（11 用例）无告警路由断言。

**修复**：补 2 个用例：
1. `Notify.Alarm` 事件 → 验证 `alarmManager.add(AlarmDTO)` 被调用且字段（deviceId/alarmType/alarmLevel/alarmTime）映射正确。
2. → 验证 `eventPublisher.publishEvent(AlarmCreatedEvent)` 被触发（Mockito `verify` / `ArgumentCaptor`）。

**涉及文件**：`Gb28181ProtocolHandlerTest.java`。

### D8 — 单机 SSE 重复投递（观察项）

**现状** [RedisBackedSseEventBus.publish:74-82](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/sse/RedisBackedSseEventBus.java#L74-L82) 先 `publishLocal` 再 Redis 广播；本节点 Redis 监听器（[afterPropertiesSet:148](../../voglander-service/src/main/java/io/github/lunasaw/voglander/service/sse/RedisBackedSseEventBus.java#L148)）再次 `publishLocal` → 同一 emitter 收到 2 份。

**修复：origin 回路抑制**
- `SseEvent` 增 `originId` 字段；本实例启动生成 UUID `nodeId`（`@PostConstruct`/常量）。
- `publish` 设 `event.originId = nodeId` 后再广播。
- Redis 监听器收到后：`if (nodeId.equals(event.originId)) return;`（本节点发出的，已本地直发，跳过）。
- 该方案多节点同样正确（异节点 originId 不同，照常分发）。

**涉及文件**：`SseEvent.java`、`RedisBackedSseEventBus.java`、`SseEventBusTest`（补回路抑制断言）。

---

## 五、S4：全链路验收检查体系（核心交付）

把报告 §四/§五 的人工验收沉淀为**两层**可重复验收。

### 5.1 确定性自动化 E2E（进 CI）

复用既有 E2E 基座 [BaseE2eTest](../../voglander-web/src/test/java/io/github/lunasaw/voglander/e2e/BaseE2eTest.java)（`@SpringBootTest` + `TestRestTemplate`，手动清理 + 唯一键隔离）。事件入口用 `VoglanderBusinessNotifier.notify(GatewayEvent)`，ZLM 流就绪用**模拟 `on_stream_changed` POST**（报告已验证等价覆盖 1.0.5 代码路径），不依赖真实 SIP 设备与真实推流。

| 测试类（新增/扩展） | 覆盖链路 | 闭合缺陷 |
|---------------------|---------|---------|
| `ConfigBootSmokeTest` | 以含 `inte` 的 profile 加载 ApplicationContext 不抛 `DuplicateKeyException`/`ConverterNotFoundException` | D1/D2 |
| `SqliteSchemaInitializerTest` | 全新临时 SQLite → 真实建表流程 → 断言 17 张 `tb_` 表 + admin 种子全建成（阈值 17，非 18） | D3 |
| `LiveStartE2eTest` | `POST /live/start` → INVITING 占位行 → 模拟 `on_stream_changed` → future 完成 → ACTIVE + 9 路 PlayUrls + SSE `live.ready`；二次 start refCount+1 秒返 | Sprint1 回归 |
| `LiveStopGcE2eTest` | stop×N → refCount 递减 → 归零写 pending_close → 推进 GC → `closeStream` 真实 `closeRtpServer`+BYE + SSE `live.closed` + 会话 CLOSED | D5 |
| `PtzVocabE2eTest` | `POST /ptz/control` 逐一发 `UP/DOWN/LEFT/RIGHT/UP_LEFT/.../ZOOM_IN/ZOOM_OUT/STOP` → 均 200 + 下发 envelope 的 `cmd` 为正确规范枚举名 | D6 |
| `PlaybackControlE2eTest` | `POST /playback/control` PLAY_RESUME/PLAY_NOW → 真实下发（`controlPlayBack` 被调）；seek/倍速按 L2 决策断言（接通或显式不支持） | D4 |
| `AlarmRouteE2eTest` + `Gb28181ProtocolHandlerTest` 扩展 | `Notify.Alarm` → 落库 `tb_alarm` + SSE `alarm.new`（E2E）；handler 单测断言 add+event（单测） | D7 |
| `SseNoDuplicateE2eTest` | 单机订阅 `/stream/events` → publish 一次 → 仅收 1 份（origin 抑制生效） | D8 |
| `JwtAuthE2eTest`（已有 SseControllerTest 部分覆盖） | `/api/v1/**` 无 token→401；`/api/v1/health` 白名单 200；`/auth/login` 取 token | 安全回归 |

**约束**（遵 CLAUDE.md 测试策略）
- E2E/异步/Hook 类**不得用** `@Transactional`（跨线程/外部进程事务无法回滚）；用 `@BeforeEach/@AfterEach` 手动清理 + 唯一键（时间戳+线程索引，见 `UniqueKeyFactory`）隔离并发。
- 需 Redis 的用例运行期探测，不可用则 `Assumptions.assumeTrue` 跳过（对齐 `MediaNodeCacheIntegrationTest` 既有约定）。
- 凡涉真实 SIP 端口（5060/5061）的既有 E2E 满载抖动问题（报告 §四 3 Errors），新增用例**不绑真实端口**，规避同类 flakiness。

### 5.2 真实联调清单（不进 CI，预发/本机回归）

脚本化沉淀到 `doc/1.0.5/acceptance/`：
- `acceptance/README.md`：环境前置（ZLM HTTP 端口须与 `local.zlm.port` 对齐——验收机为 **8082**，dev 默认 9092 需覆盖；Redis `localhost:6379` pwd `luna`；至少一条 `tb_media_node` `enabled=1`，否则 `/live/start` 返 700002）。
- `acceptance/run-live-acceptance.sh`：登录取 token → `/live/start` → 真实 ffmpeg 推 `invite.mp4` → 拉 FLV 校验魔数 `464c5601` → stop → 校验 GC 关流。脚手架取自报告「附录：关键复现命令」。
- 明确标注：ZLM `config.ini` 钩子重配被安全策略拦截，联调以模拟 `on_stream_changed` POST 等价覆盖（报告既定做法）。

### 5.3 覆盖率
- `./generate-coverage-report.sh`（JaCoCo 聚合）确认新增/修改类（GC、PTZ 门面、playback、SSE bus、SqliteSchemaInitializer）有用例覆盖。

---

## 六、执行顺序与提交计划

按依赖与风险分阶段，每阶段独立可编译可测、独立提交（遵循增量提交原则）。

| 阶段 | 内容 | 提交信息建议 |
|------|------|-------------|
| **S0** | 复核并提交 D1/D2（`application-inte.yml`） | `fix(1.0.5): application-inte.yml 重复键+gateway.nodes 绑定 (D1/D2 启动阻断)` |
| **S1** | D3 脚本净化 + 建表校验 + 调度器容错 + `SqliteSchemaInitializerTest` | `fix(1.0.5): SQLite 建表脚本净化与完整性校验 (D3 启动阻断)` |
| **S2** | D6 PTZ 词表翻译 + 预置位决策；D4 回放 control 接通 + 用例 | `fix(1.0.5): PTZ 指令词表门面翻译 + 回放 control 接通 (D6/D4)` |
| **S3** | D5 GC 关流 + D7 告警单测 + D8 SSE 去重 | `fix(1.0.5): GC 真实关流 + 告警路由单测 + SSE 去重 (D5/D7/D8)` |
| **S4** | 全链路 E2E 套件 + 真实联调脚本 | `test(1.0.5): 可视化全链路确定性 E2E + 真实联调脚本` |

**每阶段质量门**（CLAUDE.md「Definition of Done」）：
- `mvn clean test -pl voglander-web`（必须 `clean`，规避 IDE stale-class）通过，无新增失败。
- 改了 `sip-proxy`/`zlm-spring-boot-starter` 才需 `mvn install` 上游；本方案仅改 voglander，无需上游重建。
- 不 `--no-verify`、不禁用测试、编译通过、提交信息说明「为什么」。

---

## 七、风险与已定决策

| 项 | 说明 | 决策（2026-06-05） |
|----|------|---------|
| **D6 预置位字节构造** | `PTZControlEnum` 无 PRESET；SET/GOTO/DEL 需上游指令构造器能力 | ✅ **先保底返回不支持**：`/ptz/preset` 返回明确「预置位暂不支持」，杜绝假成功；档 A 真实下发后续单独排期 |
| **D4 seek/倍速 param** | envelope `PlaybackControl` payload 当前仅 `action`，无 range/scale | ✅ **本次只做 L1**：接通 pause/resume（`PLAY_NOW`/`PLAY_RESUME`）；`PLAY_RANGE`/`PLAY_SPEED` 显式返回「暂不支持」，L2 后续排期 |
| **D3 精确失败语句** | 净化 + 校验后大概率消除，但未逐条定位 `ResourceDatabasePopulator` 失败语句（`json_extract` 为疑点，但 xerial 现代版支持 JSON1，未必是它）| `SqliteSchemaInitializerTest` 作为红线：若净化后仍 <17 表，测试会复现并暴露具体缺表，再针对性处理 |
| **D3 表数阈值（审核修正）** | 脚本 18 个 `CREATE TABLE` = 17 张 `tb_` + 1 张 `sequence`；`LIKE 'tb_%'` 只匹配 17 | ✅ **`EXPECTED_TABLE_COUNT = 17`**：原方案 18 与 tb_% 过滤不一致，会令完美库也 17<18 永久启动失败，已改 17 |
| **D3 净化清单（审核修正）** | 原方案拟删 6 处 SELECT，其中 4 处是 `INSERT...SELECT` 种子子句 | ✅ **只删 L649/L668 两处纯诊断 SELECT**；L473/L481/L606/L642 必须保留（删则破坏 RBAC 种子或语法错误）|
| **D3 sentinel（审核修正）** | 原方案以 `tb_cascade_platform` 为「最后一张表」有误，它是倒数第二 | ✅ **sentinel 改 `tb_cascade_channel`**（真正最后一张 L709），确保全表建成才判已初始化 |
| **D6 fallback（审核修正）** | 原方案 `getByName(control)` 查的是中文 `name`，无法解析规范枚举名 | ✅ **fallback 改 `valueOf(key)` + try-catch**，正确兼容直接传枚举名 |
| **config.ini 未纳入提交** | `git status` 显示其同被修改，疑本机 ZLM 联调残留 | ✅ **不随缺陷修复提交**：提交前还原或单独说明，避免污染 D1/D2 提交 |
| **真实 ZLM 端口对齐** | dev 默认 `local.zlm.port=9092` ≠ 真实 8082 | 联调清单显式要求覆盖；不改默认值（避免影响他人环境） |

---

## 附：缺陷 ↔ 验收用例 ↔ 文件 速查

| 缺陷 | 修复文件 | 验收用例 |
|------|---------|---------|
| D1/D2 | `application-inte.yml` | `ConfigBootSmokeTest` |
| D3 | `voglander-sqlite.sql`、`SqliteSchemaInitializer`、`CascadeClientScheduler` | `SqliteSchemaInitializerTest` |
| D4 | `DeviceCommandService`、`GbDeviceCommandService`、`PlaybackController`、(`VoglanderServerMediaCommand`) | `PlaybackControlE2eTest` |
| D5 | `MediaPlayService(Impl)`、`LiveSessionGcService` | `LiveStopGcE2eTest` |
| D6 | `GbDeviceCommandService`、`PtzControlReq`/`DevicePtzReq`、(`VoglanderServerPtzCommand`/`PtzController`/`PtzWebAssembler`)、`ServiceExceptionEnum` | `PtzVocabE2eTest` |
| D7 | `Gb28181ProtocolHandlerTest` | `Gb28181ProtocolHandlerTest`、`AlarmRouteE2eTest` |
| D8 | `SseEvent`、`RedisBackedSseEventBus` | `SseNoDuplicateE2eTest`、`SseEventBusTest` |
</content>
</invoke>
