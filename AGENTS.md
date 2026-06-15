# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## 项目概述

Voglander 是基于 Spring Boot 3.5.3 + Java 17 的企业级视频监控平台，支持 GB28181、GT1078、ONVIF 等协议，提供设备管理、实时监控和视频流处理能力。整体采用严格的分层架构与统一的模板方法/装配器模式。

## 开发命令

### 构建和运行
```bash
mvn clean compile                          # 编译
mvn spring-boot:run -pl voglander-web       # 运行主应用（入口 ApplicationWeb）
mvn clean install -pl voglander-common      # 构建特定模块
mvn clean package -pl voglander-web         # 打包
```

### 测试
```bash
mvn test                                              # 全部测试
mvn test -Dtest=DeviceManagerTest                     # 单个测试类
mvn test -Dtest=DeviceManagerTest#testCreateDevice    # 单个测试方法
mvn test -Dspring.profiles.active=test                # 指定 profile

# Redis 集成测试：运行时探测 Redis，不可用则 Assumptions.assumeTrue 自动跳过，无需专用脚本
# 先启动：brew services start redis  或  docker run -p 6379:6379 -d redis
mvn test -Dtest=MediaNodeCacheIntegrationTest
```

> **测试集中放置**：所有测试（manager / integration / service / controller）统一在
> `voglander-web/src/test/java/io/github/lunasaw/voglander` 下实现，其他模块不建测试目录。

### 覆盖率（JaCoCo 聚合）
```bash
./generate-coverage-report.sh   # clean → test → verify，自动打开报告
# 输出：voglander-coverage-report/target/site/jacoco-aggregate/index.html
```

### 数据库
- 默认 **SQLite**（`app.db` 自动创建���，测试用 `test-app.db` + `schema-sqlite.sql`
- 可选 **MySQL**：建库 `voglander` → 执行 `sql/voglander.sql` → 改 `application-dev.yml`

## 架构概述

### 多模块结构与依赖
```
voglander-web         # REST 控制器、过滤器、拦截器；应用主模块，承载所有测试
voglander-manager     # 业务编排、复杂多服务协调（含 MediaSessionManager 等）
voglander-service     # 核心业务服务，extends IService<DO>，单表单一职责
voglander-repository  # 实体(DO)、Mapper、缓存
voglander-integration # 外部系统集成（GB28181/ZLM/Excel），统一 ResultDTO
voglander-client      # 外部服务客户端与 DTO
voglander-common      # 常量、枚举、异常、工具
voglander-test        # 测试配置与工具
voglander-coverage-report  # JaCoCo 聚合报告（聚合上述模块）
```
依赖方向：`web → manager → service → repository → common`；`integration/client → common`。

根包：`io.github.lunasaw.voglander`。

### 分层职责
- **Web**：REST 控制器、参数校验、Req/VO 格式转换（**入参必须用 Web 层 `*Req`，不得直接收 DTO**）
- **Manager**：复杂业务流编排、多服务协调（**对外方法参数/返回一律用 DTO，不暴露 DO**）
- **Service**：`extends IService<DO>`，单表核心逻辑
- **Repository**：持久化与缓存
- **Integration**：外部系统包装器，统一 `ResultDTO` 响应

#### 分层调用选择
- **复杂业务**（多表/复杂逻辑）：Controller → 专用 Service → 多个 Manager；该 Service 也可直接调 `IService` 基础方法
- **简单单表**：Controller 可直接调 `IService`，无需经 Manager
- 避免过度设计与不必要的层级嵌套

## Web 层 Controller 模板方法规范

Controller 对应 Manager 标准 CRUD，按优先级实现：
- **必须（核心模板）**：`add` / `update` / `get` / `deleteOne` / `deleteBatch` / `getPage`
- **可选（增强业务）**：含操作日志的 `createXxx` / `updateXxx` / `deleteXxx`
- **避免（专用细粒度）**：状态更新、Key 更新等通常不在 Controller 暴露

关键约定：
- 入参：`*CreateReq` / `*UpdateReq`（含 `@NotNull` 的 ID）/ `*QueryReq`，经 `WebAssembler.xxxReqToDto()` 转 DTO
- 出参：Manager 返回 DTO → `WebAssembler.dtoToVo()` → 前端
- 更新：必带主键 ID，走 `manager.updateById(id, updateDTO)`
- 查询：实现一个**全量分页条件查询**接口，由前端灵活组装条件
- 统一返回 `AjaxResult.success()` / `AjaxResult.error()`；分页包装为 `*ListResp`（含 `total` + `items`）
- 用 `@Valid` 校验；类加 `@Tag`，方法加 `@Operation`，参数加 `@Parameter`

```java
@PostMapping("/add")
public AjaxResult<Long> add(@Valid @RequestBody StreamProxyCreateReq req) {
    return AjaxResult.success(streamProxyManager.add(webAssembler.createReqToDto(req)));
}

@PutMapping("/update")
public AjaxResult<Long> update(@Valid @RequestBody StreamProxyUpdateReq req) {
    return AjaxResult.success(streamProxyManager.updateById(req.getId(), webAssembler.updateReqToDto(req)));
}
```

## 关键设计模式

- **Assembler**：层间数据转换（DTO ↔ DO ↔ VO）
- **Manager 模板方法**：统一 CRUD + 缓存/日志
- **Wrapper**：外部系统集成，统一异常处理与 `ResultDTO`
- **Template**：统一内部方法承载缓存/日志等横切关注点

### Manager 层设计与模板方法

每个 Manager 实现统一模板方法签名：
```java
public Long add(XxxDTO dto)
public Long update(XxxDTO queryDTO, XxxDTO updateDTO)   // 查询条件与更新内容分离
public Long updateById(Long id, XxxDTO updateDTO)        // 最常用
public XxxDTO get(XxxDTO dto)
public Boolean deleteOne(XxxDTO dto)
public Boolean deleteBatch(XxxDTO dto)
public Page<XxxDTO> getPage(XxxDTO dto, int page, int size)  // 默认按 createTime 降序
private void clearCache(Long id, String oldKey, String newKey)  // 主键 + 业务键双清理
```

规范：
- **统一入口**：所有增/改/删走内部统一方法，集中做参数校验、缓存清理、日志，禁止散落直接操作 DB
- **对外用 DTO**：公开方法参数/返回均为 DTO；内部可用 DO 但须 private；查询方法命名 `getXxxDTOByYyy`
- **默认值**：依赖数据库字段默认值，不在代码硬编码
- **UNIQUE 约束**：`saveOrUpdate` 前先按业务主键（如 `app+stream`）查现有记录，避免唯一约束冲突
- 历史暴露 DO 的方法标 `@Deprecated` 并提供 DTO 替代

#### LambdaQueryWrapper 查询构建标准（强制）
必须用带 `condition` 参数的链式方法，禁止手动 null 判断，也**禁止** `new LambdaQueryWrapper<>(entityObject)`（会带入 null 字段污染条件）：
```java
LambdaQueryWrapper<DeviceDO> qw = new LambdaQueryWrapper<>();
qw.eq(dto.getId() != null, DeviceDO::getId, dto.getId())
  .like(dto.getName() != null, DeviceDO::getName, dto.getName())
  .orderByDesc(DeviceDO::getCreateTime);
```

### Service 层接口设计
- 主要接口用 `*DTO` 参数；可提供 `id`/`key` 等基本参数重载作为补充
- 用 `Assert` 校验、`ServiceException` 抛业务异常

### Wrapper 层（voglander-integration）
- **职责单一**：仅参数校验 + 异常捕获，业务逻辑委托 Service 层
- 统一走 `WrapperExceptionHandler.executeWithExceptionHandling(...)`；成功返回模型，异常返回 `null`，外层包成 `ResultDTO`
- 通用校验放 `WrapperExceptionHandler`，业务专用校验放专用类（如 `ZlmWrapperValidator`）
- `@Slf4j` 记录操作开始/成功/失败
```java
public ResultDTO<T> op(ReqDTO req) {
    return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
        WrapperExceptionHandler.validateRequest(req, "请求参数");
        T result = ExternalService.call(req);
        if (result == null || !isValid(result)) { log.error("操作失败"); return null; }
        return result;
    }, "操作描述");
}
```

## 数据/时间/JSON 规范（强制）

- **MyBatis Plus**：简单 CRUD 一律用 `BaseMapper`/`IService` 基础方法，禁止自定义 SQL；仅复杂/联表/批量才写 SQL，一般无需新建 Mapper
- **时间类型**：DO/DTO 层统一 `LocalDateTime`（禁止 Date/Timestamp）；VO 返回统一 **Unix 毫秒时间戳**，字段以 `Time` 结尾，在 DTO 提供 `xxxToEpochMilli()` 领域方法转换
- **JSON**：全项目统一 **FastJSON2**，禁止 Jackson/Gson
- **类型转换**：必须经 FastJSON2 正反序列化新建模型（`JSON.parseObject(JSON.toJSONString(src), Target.class)`），禁止手写字符串解析
- **缓存**：`@Cached`/`@Cacheable` 仅用于 Repository 层单对象 DB 查询（key 用主键或唯一字段，存 DO 的 JSON）；复杂缓存用 `RedisCache`（值为 FastJSON 字符串）；分布式锁用 `RedisLockUtil`
- **校验**：用 apache-commons-lang3 的 `Assert`
- **异常**：抛 `ServiceException` 前先确认 `ServiceExceptionEnum` 有对应类型，没有则先补充；全局走 `GlobalExceptionHandler`
- **Lombok**：`@Slf4j` 日志，`@Getter/@Setter/@ToString/@AllArgsConstructor/@NoArgsConstructor`
- **注释**：优先块注释，避免行尾注释
- **重构**：未被 Git 管理的新增代码可直接重构、无需向后兼容；已提交代码（尤其公共 API）需考虑兼容

## 技术栈

- **Java 17**（强制 `jakarta.*`，非 `javax.*`）、Spring Boot 3.5.3、MyBatis Plus 3.5.5、Dynamic DataSource 4.3.1
- MySQL 8.2.0（生产）/ SQLite（开发测试）、Redis、HikariCP
- **sip-gateway-spring-boot-starter 1.8.0** + GB28181 client/server 1.8.0（取代旧版直接 handler 接入）
- ZLMediaKit-Starter 1.0.10-SNAPSHOT、EasyExcel 4.0.1
- JUnit 5 + Mockito、SkyWalking 9.1.0、SpringDoc 2.8.9

## GB28181 集成架构（sip-gateway 1.8.0，含踩坑点）

### 协议合规铁律（强制，最高优先级）

**所有协议层实现必须严格对齐标准协议（GB/T 28181、SIP/RFC 3261、ONVIF 等），禁止凭空臆造规则。**

- **编码/字段/状态机/寻址一律以标准为准**：GB28181 设备与通道都是**独立 20 位国标编码**（行政区划 6 + 行业 2 + 类型 3 + 序号），通道与设备的从属关系由 **Catalog 目录的 `ParentID`** 表达，**不存在 `channelId = deviceId + 后缀` 这种字符串前缀关系**。下发寻址（Request-URI/To/SDP/Subject）、回调解析、状态判定都按标准字段，不靠拼接/截断/前缀猜测。
- **不确定先查标准，不要猜**：拿不准某字段格式/某消息流程时，查 GB/T 28181 原文或框架（sip-gateway/gb28181）实现，禁止"感觉应该这样"就写死规则。
- **测试台（lab）的简化是例外，必须显式标注**：`wrapper/gb28181/lab/` 下为联调便利采用的非标约定（如模拟通道 `clientId + 两位序号`）**仅限 lab 自环**，注释必须写明"**非 GB28181 标准**"，且真相源单点收口（如 `LabChannelHolder.channelIdOf`/`ownsChannel`，生成与判定共用一处规则，禁止散落多份），非 lab 路径不得复用该约定。
- **`sip-common` 协议纯净性**：见 sip-proxy AGENTS.md，`sip-common` 不得掺入业务逻辑。

### 架构迁移现状

已从「handler 分发」迁移到「command 主动发送 + 统一回调」。`wrapper/gb28181/` 下不再有 `*RequestHandler`/`*ResponseHandler`。

**出站指令（Command）**
- `client/command/`、`server/command/` 按业务域分包（`ptz`/`record`/`device`/`alarm`/`catalog`/`status`/`config`/`media`）
- 继承抽象基类统一设备获取、异常、日志与 `ResultDTO`：客户端 `AbstractVoglanderClientCommand`（走 `ClientCommandSender`），平台端 `AbstractVoglanderServerCommand`（走 `ServerCommandSender`）
- 设备信息由 `supplier/` 的 `VoglanderClientDeviceSupplier`/`VoglanderServerDeviceSupplier` 提供

**入站回调（统一入口）**
- `notifier/VoglanderBusinessNotifier` 是接入业务层的**唯一入口**，直接实现框架 `BusinessNotifier`（覆盖默认 `NoopBusinessNotifier`）
- **必须异步**：`notify(GatewayEvent)` 标注 `@Async("sipNotifierExecutor")`，否则设备超时重传；**不要**继承 `AbstractProtocolBusinessNotifier`——其 `notify()` 为 `final` 且内部自调用，`@Async` 代理失效
- 事件为三段式 `gb28181.Group.Name`，payload 为 `Map<String,Object>`，用 FastJSON2 反序列化；分发到 `DeviceRegisterService`/`DeviceManager`/`DeviceChannelManager`/`MediaSessionManager`

**会话状态（MediaSessionManager）**
- `MediaSessionManager` + `MediaSessionDO`(`tb_media_session`) 管理 INVITE 点播/回放会话，业务主键为 SIP `callId`
- 状态见 `MediaSessionConstant.Status`（ACTIVE=1/CLOSED=0/INVITING=2/FAILED=3），由 `Session.InviteOk/InviteFailure/Ack/Bye` 与 `Notify.MediaStatus` 事件驱动

**多节点 INVITE 上下文**
- `store/RedisInviteContextStore implements InviteContextStore`，`@ConditionalOnProperty(gateway.gb28181.store.type=redis)`，以 `sip:invite:ctx:{callId}` 存 `InviteContext`，支撑跨节点回包路由；单机默认走框架 `InMemoryInviteContextStore`

**启动与配置（易踩坑）**
- `ApplicationWeb` **必须** `@EnableSipServer`：它 `@Import` 了 `Gb28181CommonAutoConfig`(提供 `CommandStrategyFactory`)、`SipProxyServerAutoConfig`、`SipProxyAutoConfig`，这些无 `.imports` 注册不会自动激活；缺失则 `ServerCommandSender`/`ClientCommandSender` 无法实例化，且 `Gb28181GatewayAutoConfiguration`(`@ConditionalOnBean(ServerCommandSender)`) 会静默关闭事件管线
- `supplier/VoglanderDeviceSessionCache`（必需）：实现框架 `DeviceSessionCache`，委托 `VoglanderServerDeviceSupplier` 按 `deviceId` 提供 `ToDevice`，`ServerCommandSender` 构造依赖它
- `VoglanderServerDeviceSupplier`/`VoglanderClientDeviceSupplier` 标 `@Primary`，避免与框架默认 supplier 冲突 `NoUniqueBeanDefinitionException`
- `start/ServerStart`（CommandLineRunner）绑定 SIP 端口，**不可删除**（全工程唯一调用 `addListeningPoint` 处）；1.8.0 `SipLayer` 无 `setSipListener`，须用 `addListeningPoint(ip, port, sipListener, enableLog)`
- SIP 端配置见 `config/properties/` 的 `VoglanderSipServerProperties`/`VoglanderSipClientProperties`；`gateway:` 段见 `application-inte.yml`

**命令 API 迁移要点**
- `ServerCommandSender` 改为实例 Bean，按 `deviceId` 调用（如 `deviceInfoQuery(deviceId)`）；`ClientCommandSender` 仍 `@Component` + 静态方法
- 已移除：`PtzCmdEnum`/`PtzUtils`（改用 `PTZControlEnum` + `PTZInstructionBuilder`）、`ServerCommandSender.sendCommand("INFO",...)`、`deviceConfigDownloadQuery`、`Device.localIp`、`ClientCommandSender.sendCommand(...)`（改用实例 `send(CommandContext)`）

## ZLM 集成

- 后端 `/zlm/api/*` 代理到 ZLM；`VoglanderZlmHookServiceImpl` 处理回调（服务生命周期、流事件、节点心跳/状态）
- 配置 `zlm:`（`enable`/`hook-enable`/`servers[]`），`ZlmIntegrationConfig#getDefaultServer()` 取默认节点
- 流代理流程：前端 → `/zlm/api/proxy/add` → ZLM → Hook 回调 → 落库

## 测试策略

| 层级 | 类型 | 注解 | 依赖 | 事务 |
|------|------|------|------|------|
| Controller | 纯单元 | `@ExtendWith(MockitoExtension.class)` | `@Mock` + `@InjectMocks` | 无 |
| Service | 纯单元 | `@ExtendWith(MockitoExtension.class)` | `@Mock` + `@InjectMocks` | 无 |
| Manager | 集成 | `@SpringBootTest` + `BaseTest` | 真实 Bean | `@Transactional` |
| Repository | 集成 | `@SpringBootTest` + `BaseTest` | 真实 DB | `@Transactional` |
| HTTP API | 集成 | `@SpringBootTest` + 自定义基类 | `TestRestTemplate` | 无，手动清理 |
| 异步/Hook | 集成 | `@SpringBootTest` + 自定义基类 | 真实 Bean | 无，手动清理 |

- **业务层（Controller/Service）禁用** `@SpringBootTest`/`@WebMvcTest`/`@MockitoBean`，纯 Mockito 单元测试
- **Manager 必须集成测试**（`BaseTest`，真实库事务）。依赖分层：`IService<DO>` 用 `@Autowired`；Assembler/外部集成用 `@MockitoBean`（Assembler 用 `thenAnswer` 做双向转换）；用 `@BeforeEach/@AfterEach` 清理数据 + 缓存
- **不要用 `@Transactional` 的场景**：跨线程/外部进程的数据操作、HTTP API、异步/`@Async`、Hook 回调、MQ、定时任务、WebSocket——这些事务无法回滚，且长事务易致 MySQL 锁等待；改为手动清理，并用唯一键（时间戳 + 线程索引）保证并发隔离

## 业务域速览

- **设备**：多协议摄像头（GB28181/ONVIF），状态在线(1)/离线(0) + 心跳，SIP 注册认证，单设备多通道
- **关键服务**：`DeviceRegisterService`(注册认证)、`DeviceCommandService`(PTZ/命令)、`MediaNodeService`(媒体节点)、`MediaSessionManager`(会话)、`ExportTaskService`(批量导出)
- 异步任务用 `AsyncManager` + `ThreadPoolConfig`

## 配置文件

`application.yml`（主）/ `application-dev.yml`（开发）/ `application-test.yml`（测试）/ `application-repo.yml`（数据库）/ `application-inte.yml`（集成，含 SIP `gateway:` 段）

## 编码规则来源

- `.cursorrules` — 完整 Java 编码标准（已在本文件提炼）
