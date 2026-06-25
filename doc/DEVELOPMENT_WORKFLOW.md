# Voglander 需求开发规范 — AI Agent 工作流

> 本文档面向 **AI Agent（Claude / Cursor / Codex 等）**，描述在 voglander 工程中处理一项需求从接收到交付的完整执行规范。  
> 编码规则与架构约定以 [CLAUDE.md](../CLAUDE.md) 为唯一真相源，本文档仅定义**过程**，不重复规则。

---

## 0. 工作流总览

```
需求输入
   │
   ▼
① 调研（Research）
   • 读相关代码：同类 Manager/Controller/Service/DO
   • 识别影响范围：哪些层、哪些缓存键、哪些协议
   │
   ▼
② Check 关键点 ◀────────────── 人工卡点（必停等确认）
   • 列出：需求理解 / 边界条件 / 影响层 / 协议红线 / 风险
   │
   ▼
┌──────────── 方案收敛循环（AI 自驱）────────────┐
│  ③ 输出技术方案                                  │
│  ④ 对抗式自审核（见 §3.2 检查表）                │
│     通过 → 退出循环                              │
│     未通过 → ⑤ 更新方案 → 回到 ③               │
│  同一问题失败 3 次 → 停止，诊断根因，换思路       │
└──────────────────────────────────────────────────┘
   │
   ▼
⑥ TDD 实施
   • 红：先写测试（失败断言）
   • 绿：最小实现通过
   • 重构：测试保持绿
   • 验收门控（见 §5）
   │
   ▼
⑦ 实施报告
   • 改动清单 / 测试证据 / 遗留项与风险
```

---

## 1. 阶段一：调研

### 1.1 必读文件

| 必读 | 目的 |
|------|------|
| `CLAUDE.md` | 架构��定、编码规则全景 |
| 同业务域的现有 Manager + Service + DO | 识别命名约定、缓存键格式 |
| `voglander-common` 中对应枚举/异常 | 确认 `ServiceExceptionEnum` 是否已有对应类型 |
| `sql/` 或已有 schema | 了解 DB 约束（UNIQUE KEY、索引） |

### 1.2 影响面评估

运行如下搜索，在 codebase 中定位所有相关引用后再动笔：

```bash
# 定位相关类
grep -rn "XxxDO\|XxxDTO\|XxxManager\|XxxService" --include="*.java" .

# 确认缓存键使用
grep -rn "CacheConstant\|CACHE_KEY" --include="*.java" .

# 确认协议相关代码（不得随意改动）
grep -rn "gb28181\|sip-gateway\|SipLayer\|ServerCommandSender" --include="*.java" .
```

### 1.3 协议层侦测

若需求涉及以下任一关键词，**直接进入附录 B（协议红线）**：

`gb28181` / `sip` / `invite` / `register` / `catalog` / `deviceId` / `channelId` / `callId` / `PTZ`

---

## 2. 阶段二：Check 关键点（人工卡点）

给出结构化确认列表，格式如下：

```
## Check 关键点

### 需求理解
[用一段话复述需求的核心目标，明确不包含什么]

### 影响的层
- [ ] Repository（是否需要新表/新字段/新索引）
- [ ] Service（是否新增/修改业务逻辑）
- [ ] Manager（是否需要新的编排方法）
- [ ] Web（是否需要新 API 或修改现有 API）
- [ ] Integration（是否影响 GB28181/ZLM/外部集成）

### 边界与约束
- 哪些现有逻辑不得变动（协议标准 / 公共 API 兼容性）
- 是否触碰 GB28181/SIP 协议层（如是，见附录 B）

### 技术选型 / 风险
- [说明可能的技术风险、破坏性变更、依赖问题]

### 人工确认项
[此处列出需要你拍板的决策点，其余 AI 自驱]
```

**人工确认完成后，AI 继续进入方案循环。**

---

## 3. 阶段三：方案设计循环（AI 自驱）

### 3.1 方案模板

```
## 技术方案 vN

### 改动范围
[模块 → 文件 → 方法级清单]

### 数据模型
[新表/新字段/索引，或说明"无变动"]

### 分层实施顺序
1. voglander-common（枚举、异常类型、常量）
2. voglander-repository（DO、Mapper、缓存）
3. voglander-service（Service 接口 + 实现）
4. voglander-manager（Manager 模板方法）
5. voglander-web（Controller、Assembler、Req/VO）
6. voglander-integration（外部集成 Wrapper，如涉及）

### 关键代码变更
[伪代码或关键片段]

### 测试方案
[每层测试类型 + 关键断言]
```

### 3.2 对抗式自审核检查表

每版方案必须过以下检查表，**所有 ❌ 须修复后才能退出循环**：

#### 架构合规

- [ ] 分层调用方向正确：`web→manager→service→repository`，不跨层
- [ ] Manager 公开方法入参/返回均为 `*DTO`，无 `DO` 泄露
- [ ] Controller 入参为 `*Req`，经 `WebAssembler` 转 `DTO`，不直接接收 `DTO`
- [ ] 简单单表操作（无多表逻辑）可直接走 `IService`，无需强行经 Manager

#### 代码规范

- [ ] `LambdaQueryWrapper` 用 `new LambdaQueryWrapper<>()` + condition 链式，**不用** `new LambdaQueryWrapper<>(entity)`
- [ ] 时间字段：DO/DTO 层用 `LocalDateTime`，VO 层用 Unix 毫秒 + `Time` 后缀
- [ ] JSON：全部用 `FastJSON2`，无 Jackson/Gson 引入
- [ ] 类型转换经 FastJSON2 序列化新建对象，**不手写**字段赋值
- [ ] 使用 `jakarta.*`，无 `javax.*`

#### 缓存与一致性

- [ ] Manager 中增/改/删后调用 `clearCache(id, oldKey, newKey)` 双清
- [ ] Repository 层缓���只用 `@Cached`/`@Cacheable`，key 用主键或唯一字段，存 DO 的 JSON
- [ ] `saveOrUpdate` 前先按业务主键查重，避免 UNIQUE 约束冲突

#### 异常与校验

- [ ] 抛 `ServiceException` 前确认 `ServiceExceptionEnum` 已有对应类型（无则先补）
- [ ] 用 `Assert`（apache-commons-lang3）做参数校验
- [ ] 无静默吞异常（catch 后 log.error + 继续处理，或 re-throw）

#### Integration 层（如涉及）

- [ ] Wrapper 统一走 `WrapperExceptionHandler.executeWithExceptionHandling`
- [ ] 成功返回模型，异常返回 `null`，外层包成 `ResultDTO`
- [ ] 无业务逻辑混入 Wrapper（职责单一：仅参数校验 + 异常捕获）

#### 协议层（如涉及，参见附录 B）

- [ ] GB28181 编码/字段/寻址严格对齐标准，**不猜测**
- [ ] lab 路径下的非标约定 **未渗透** 到非 lab 路径
- [ ] `VoglanderBusinessNotifier.notify()` 保持 `@Async("sipNotifierExecutor")`

### 3.3 退出条件

方案满足以下全部条件方可退出循环：

1. 3.2 检查表全部通过（无 ❌）
2. 方案完整覆盖需求范围（含边界场景）
3. 不引入新的架构违规或依赖问题
4. 测试方案明确，每层测试类型已定

---

## 4. 阶段四：TDD 实施

### 4.1 实施顺序

严格按分层从底向上实施，每层"红绿重构"后再进入下一层：

```
Repository → Service → Manager → Web (Controller)
```

> Integration 层如涉及，在 Service 之后单独处理。

### 4.2 测试位置（强制）

**所有测试统一在：**

```
voglander-web/src/test/java/io/github/lunasaw/voglander/
```

对应包路径示例：

```
.../voglander/
  manager/         ← Manager 集成测试
  service/         ← Service 单元测试
  web/controller/  ← Controller 单元测试
  integration/     ← 外部集成测试
  repository/      ← Repository 集成测试（如需）
```

### 4.3 分层测试规范

| 层 | 测试类型 | 注解 | 依赖方式 | 事务 |
|----|---------|------|---------|------|
| Controller | 纯单元 | `@ExtendWith(MockitoExtension.class)` | `@Mock` + `@InjectMocks` | 无 |
| Service | 纯单元 | `@ExtendWith(MockitoExtension.class)` | `@Mock` + `@InjectMocks` | 无 |
| Manager | 集成 | `@SpringBootTest` + `BaseTest` | 真实 Bean | `@Transactional` |
| Repository | 集成 | `@SpringBootTest` + `BaseTest` | 真实 DB | `@Transactional` |

**禁止在以下场景使用 `@Transactional`：**
异步方法（`@Async`）/ HTTP API 测试 / Hook 回调 / 定时任务  
→ 改用 `@BeforeEach`/`@AfterEach` 手动清理，用时间戳+线程索引保证并发隔离。

### 4.4 Manager 集成测试模板

```java
@SpringBootTest
class XxxManagerTest extends BaseTest {

    @Autowired
    private XxxManager xxxManager;

    @Autowired
    private XxxService xxxService;           // IService 真实 Bean

    @MockitoBean
    private XxxAssembler xxxAssembler;       // Assembler Mock，用 thenAnswer 双向转换

    private Long createdId;

    @BeforeEach
    void setUp() {
        // Mock Assembler 双向转换
        given(xxxAssembler.dtoToDo(any())).thenAnswer(inv -> /* 转换逻辑 */);
        given(xxxAssembler.doToDto(any())).thenAnswer(inv -> /* 转换逻辑 */);
    }

    @AfterEach
    void tearDown() {
        if (createdId != null) {
            xxxService.removeById(createdId);
            createdId = null;
        }
    }

    @Test
    @Transactional
    void testAdd_success() {
        XxxDTO dto = buildTestDto();
        createdId = xxxManager.add(dto);
        assertNotNull(createdId);
        XxxDTO result = xxxManager.get(new XxxDTO().setId(createdId));
        assertEquals(dto.getXxxField(), result.getXxxField());
    }
}
```

### 4.5 测试质量标准

**关键路径必须覆盖（强制）：**
- Manager 层的 add/update/delete/get/getPage
- Service 层的核心业务判断分支
- Wrapper 层的异常处理路径

**可以不覆盖（允许）：**
- 纯 Getter/Setter DTO/DO 模型
- 简单 passthrough 方法（无逻辑直接转发）
- 配置类、常量类

**重构优先于补测试：**
- 逻辑复杂难测 → 先重构拆小方法，再写测试
- 不为刷覆盖率写无意义 assertion

---

## 5. 阶段五：验收门控

每次提交前，顺序执行：

```bash
# Step 1: 编译（不通过不继续）
mvn clean compile

# Step 2: 全量测试
mvn test

# Step 3: 查看失败测试（如有）
# 修复后重回 Step 1；不跳过、不注释、不加 @Disabled

# Step 4: 覆盖率报告（关注关键路径，不追指标）
./generate-coverage-report.sh
```

**通过标准：**

| 门控 | 标准 |
|------|------|
| 编译 | 零错误，零 unchecked warning（新增代码） |
| 现有测试 | 全部通过，不允许因新代码导致原有测试失败 |
| 新增测试 | Manager 关键路径覆盖，Service 核心分支覆盖 |
| 代码质量 | 无 `TODO`（未绑定 issue）、无 `System.out.println`、无魔法字符串 |
| 协议层 | 涉及协议的改动须有对应协议合规说明注释 |

---

## 6. 阶段六：实施报告

完成后输出以下格式：

```markdown
## 实施报告

### 改动清单
| 文件 | 改动类型 | 说明 |
|------|---------|------|
| XxxManager.java | 新增 | 增加 addXxx() 模板方法 |
| ...             | ...  | ... |

### 测试结果
- 新增测试：X 个
- 全量测试通过：✅ / ❌（附失败原因）
- 覆盖关键路径：[列举]

### 验收证据
[关键测试输出截图或日志片段]

### 遗留项与风险
- [未完成项 + 原因]
- [已知风险 + 建议处置]
```

---

## 附录 A：分层开发检查表

开发新功能前，按此清单逐层确认：

### A.1 Repository 层
- [ ] 需要新表 → 在 `sql/` 和 `schema-sqlite.sql` 同步 DDL
- [ ] 新字段 → 加到 DO 并用 `@TableField`，依赖 DB 默认值，不硬编码
- [ ] 缓存键 → 在 `CacheConstant` 或同类常量类定义，格式：`业务:主键/唯一字段`
- [ ] 索引 → 按查询频率评估，UNIQUE 约束须在 Manager saveOrUpdate 前处理

### A.2 Service 层
- [ ] 继承 `IService<XxxDO>`
- [ ] 主接口用 `*DTO` 参数
- [ ] 业务异常抛 `ServiceException`（先确认 `ServiceExceptionEnum` 已有对应类型）

### A.3 Manager 层
- [ ] 实现标准模板签名：`add` / `updateById` / `get` / `deleteOne` / `deleteBatch` / `getPage`
- [ ] `clearCache` 做主键 + 业务键双清
- [ ] 公开方法无 DO 泄露

### A.4 Web 层
- [ ] Controller 入参为 `*CreateReq` / `*UpdateReq` / `*QueryReq`
- [ ] 经 `WebAssembler.xxxReqToDto()` 转 DTO 后传 Manager
- [ ] 统一返回 `AjaxResult.success()` / `AjaxResult.error()`
- [ ] 类加 `@Tag`，方法加 `@Operation`，参数加 `@Parameter`
- [ ] 用 `@Valid` 开启参数校验

---

## 附录 B：协议红线（不可突破）

> 以下规则优先级高于本文档所有其他内容。

### B.1 GB28181 编码规范

**GB28181 设备编码与通道编码是独立的 20 位国标编码，结构：**
```
行政区划(6) + 行业(2) + 类型(3) + 序号(9) = 20 位
```

**严禁：**
- `channelId = deviceId + "后缀"` 拼接推断
- 通过前缀截断/模式匹配判断从属关系
- 从属关系从 Catalog 目录的 `ParentID` 字段读取，不推断

### B.2 SIP 寻址

- Request-URI / To / SDP / Subject 的字段内容严格按 GB/T 28181 标准
- 不可凭感觉硬编码格式——拿不准查标准原文或 `sip-gateway` 框架实现

### B.3 Lab 路径例外

- `wrapper/gb28181/lab/` 内为非标约定（如模拟通道 `clientId + 两位序号`）
- **仅限 lab 自环使用**
- 注释须写明 "**非 GB28181 标准，仅用于 Protocol Lab 自环测试**"
- 真相源单点收口于 `LabChannelHolder`，禁止在 lab 外路径复用

### B.4 业务通知器

```java
// 必须保持，不得删除此注解
@Async("sipNotifierExecutor")
@Override
public void notify(GatewayEvent event) { ... }
```

不得继承 `AbstractProtocolBusinessNotifier`（其 `notify()` 为 `final`，`@Async` 代理失效）。

### B.5 配置启动

- `ApplicationWeb` 必须保留 `@EnableSipServer`
- `start/ServerStart`（CommandLineRunner）不可删除——全工程唯一的 `addListeningPoint` 调用处

---

## 附录 C：常见陷阱速查

| 陷阱 | 后果 | 正确做法 |
|------|------|---------|
| `new LambdaQueryWrapper<>(entity)` | null 字段污染查询条件 | `new LambdaQueryWrapper<>()` + condition 链 |
| Controller 直接收 DTO | 破坏分层约定 | 用 `*Req`，经 `WebAssembler` 转 DTO |
| 集成测试用 `@Transactional` 覆盖异步场景 | 事务不回滚，测试通过但生产失败 | `@BeforeEach`/`@AfterEach` 手动清理 |
| Manager 公开方法返回 DO | 上层拿到 DO 直接操作 DB | 返回 DTO，DO 保持 private |
| 时间字段用 `Date` / `Timestamp` | 序列化不一致 | DO/DTO 用 `LocalDateTime`，VO 用 Unix 毫秒 |
| 用 Jackson 或 Gson | 与项目统一 FastJSON2 冲突 | 全部用 `FastJSON2` |
| `saveOrUpdate` 不先查重 | UNIQUE 约束异��� | 先按业务主键查，再决定 save 或 update |
| `@Cacheable` 放在 Manager 层 | 缓存粒度过粗，难以精准清理 | 缓存只放 Repository 层单对象查询 |
| SIP 会话 `@Transactional` | 长事务 MySQL 锁等待 | 手动清理 + 唯一键并发隔离 |
