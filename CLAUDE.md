# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在处理此代码仓库时提供指导。

## 项目概述

Voglander 是一个基于 Spring Boot 3 和 Java 17 构建的企业级视频监控平台。它支持多种视频监控协议（GB28181、GT1078、ONVIF），并提供设备管理、实时监控和视频流处理功能。

## 开发命令

### 构建和运行
```bash
# 编译项目
mvn clean compile

# 运行应用程序（主模块）
mvn spring-boot:run -pl voglander-web

# 构建特定模块
mvn clean install -pl voglander-common

# 打包应用程序
mvn clean package -pl voglander-web
```

### 测试
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=DeviceManagerTest

# 运行需要缓存的集成测试（需要 Redis）
./start-redis-for-test.sh
mvn test -Dtest=MediaNodeCacheIntegrationTest

# 使用特定配置文件运行测试
mvn test -Dspring.profiles.active=test
```

### 数据库配置
```bash
# 使用 SQLite（默认）
# 数据库文件自动创建为 app.db

# 使用 MySQL（可选）
# 1. 创建数据库：CREATE DATABASE voglander;
# 2. 执行 SQL：sql/voglander.sql
# 3. 在 application-dev.yml 中更新连接详情
```

## 架构概述

### 多模块结构
```
voglander/
├── voglander-web/          # REST API 控制器、过滤器、拦截器
├── voglander-manager/      # 业务逻辑编排、复杂操作
├── voglander-service/      # 核心业务服务、领域逻辑
├── voglander-repository/   # 数据访问、实体、映射器、缓存
├── voglander-integration/  # 外部系统集成（GB28181、ZLM、Excel）
├── voglander-client/       # 外部服务客户端和 DTOs
├── voglander-common/       # 共享工具、常量、枚举、异常
└── voglander-test/         # 测试配置和工具v
```

### 分层架构

- **Web 层**：REST 控制器、请求/响应处理、参数验证
- **Manager 层**：复杂业务工作流、多服务协调
- **Service 层**：核心业务逻辑、单一职责操作
- **Repository 层**：数据持久化、缓存、数据库操作
- **Integration 层**：外部系统包装器，统一 `ResultDTO` 响应

### 关键设计模式

- **Assembler 模式**：层间数据转换（DTO ↔ DO ↔ VO）
- **Manager 模式**：复杂业务逻辑协调
- **Wrapper 模式**：外部系统集成，统一错误处理
- **Template 模式**：数据操作的统一内部方法，包含缓存/日志

## 技术栈

### 核心框架

- **Java 17**（使用 `jakarta.*` 包，不是 `javax.*`）
- **Spring Boot 3.5.3** 自动配置
- **MyBatis Plus 3.5.5** 数据访问
- **Dynamic DataSource 4.3.1** 多数据库支持

### 数据和缓存

- **MySQL 8.2.0**（生产环境）/ **SQLite**（开发/测试环境）
- **Redis** 分布式缓存和锁
- **HikariCP** 连接池

### 视频和集成

- **GB28181-Proxy 1.2.4** 监控协议支持
- **ZLMediaKit-Starter 1.0.6** 媒体流
- **EasyExcel 4.0.1** Excel 处理

### 测试和监控

- **JUnit 5** 配合 **Mockito** 测试
- **SkyWalking 9.1.0** 分布式链路追踪
- **SpringDoc 2.8.9** API 文档

## 编码标准

### 命名约定

- **实体类**：`*DO`（Data Object）- 数据库实体
- **传输对象**：`*DTO` - 层间数据传输
- **视图对象**：`*VO` - API 响应视图对象
- **请求对象**：`*Req` - API 请求对象
- **响应对象**：`*Resp` - 嵌套响应对象，包含 `items` 数组
- **服务类**：`*Service` / `*ServiceImpl`
- **管理器**：`*Manager` - 业务逻辑协调
- **控制器**：`*Controller` - REST 端点

### API 响应标准

- 所有 API 返回 `AjaxResult` 包装器，包含 `code`、`msg`、`data`
- **响应代码协议**：正常成功响应的 `code` 值为 `0`，错误响应使用非 0 值
- **AjaxResult 泛型要求**：必须根据接口返回的数据类型添加合适的泛型参数
  - 返回单个对象：`AjaxResult<ObjectVO>`
  - 返回分页列表：`AjaxResult<ListResp>`
  - 返回基本类型数据：`AjaxResult<Long>`、`AjaxResult<String>`、`AjaxResult<Boolean>`
  - 简单操作消息返回：`AjaxResult<Void>` - 用于删除、启用/禁用等操作，只返回操作结果消息（如"删除成功"、"操作失败"）
- 时间字段在 VO 中返回 Unix 时间戳（毫秒）
- 分页响应在 `data` 内的 `items` 字段中包装数据
- 使用 `@Operation`、`@Parameter`、`@Tag` 进行完整的 Swagger 文档

### 数据访问模式

- 对简单 CRUD 操作使用 **MyBatis Plus** `IService` 方法
- Manager 层提供统一内部方法：`xxxInternal()`、`deleteXxxInternal()`
- 所有数据修改通过统一入口点进行，确保缓存/日志一致性
- 复杂查询和多表操作仅在 Manager 层
- **UNIQUE 约束处理**：统一内部方法在执行 `saveOrUpdate` 前先根据业务主键（如 `app+stream`）查询现有记录，避免违反唯一约束

**示例模式**：

```java
private Long updateStreamProxyInternal(StreamProxyDO streamProxyDO, String operationDesc) {
    // 1. 先根据ID查询
    if (streamProxyDO.getId() != null) {
        targetProxy = streamProxyService.getById(streamProxyDO.getId());
    }
    
    // 2. 如果没有ID，根据业务主键查询现有记录
    if (targetProxy == null && hasBusinessKey(streamProxyDO)) {
        targetProxy = getByBusinessKey(streamProxyDO);
        if (targetProxy != null) {
            streamProxyDO.setId(targetProxy.getId()); // 设置ID确保更新
        }
    }
    
    // 3. 执行saveOrUpdate，现在不会违反UNIQUE约束
    return streamProxyService.saveOrUpdate(streamProxyDO);
}
```

### 缓存策略

- **@Cached 注解**：仅用于 Repository 层的基本单对象数据库查询
- **RedisCache**：复杂场景的手动缓存管理
- **RedisLockUtil**：并发操作的分布式锁
- 缓存键使用主键或唯一字段，值以 JSON 字符串存储

### 时间处理（关键）

- **DO/DTO 层**：专门使用 `LocalDateTime`（不使用 Date/Timestamp）
- **VO 响应**：通过 `fieldNameToEpochMilli()` 方法转换为 Unix 时间戳
- **数据库**：存储为 DATETIME/TIMESTAMP，通过装配器转换

### JSON 处理

- 专门使用 **FastJSON2** 进行所有 JSON 序列化/反序列化
- 在 Spring Boot 中配置为默认 JSON 处理器
- 避免使用 Jackson、Gson 或其他 JSON 库

## 业务领域知识

### 设备管理

- **设备类型**：支持多种协议的摄像头设备（GB28181、ONVIF 等）
- **设备状态**：在线（1）、离线（0），带心跳监控
- **注册**：基于 SIP 的设备注册和认证
- **通道**：每个设备可以有多个视频通道

### 协议支持

- **GB28181**：中国视频监控国家标准
- **ONVIF**：开放网络视频接口标准
- **SIP**：设备通信的会话初始化协议
- **ZLMediaKit**：用于流转发和录制的媒体服务器

### 关键服务

- `DeviceRegisterService`：设备 SIP 注册和认证
- `DeviceCommandService`：PTZ 控制和设备命令
- `MediaNodeService`：媒体服务器节点管理
- `ExportTaskService`：批量数据导出操作

## 集成指南

### 外部系统包装器

- 所有集成层包装器必须返回 `ResultDTO` 格式
- 包含全面的异常处理和日志记录
- 使用 `@Slf4j` 进行详细操作跟踪
- 方法：`ResultDTOUtils.success()` / `ResultDTOUtils.failure()`

### 缓存集成

- 仅对通过 ID/唯一字段的基本数据库实体查询使用 `@Cached`
- 通过 `RedisCache` 进行复杂缓存，手动键管理
- 分布式操作使用 `RedisLockUtil` 确保一致性

### 异步处理

- 使用 `AsyncManager` 处理后台任务
- 通过 `ThreadPoolConfig` 配置线程池
- RabbitMQ 用于消息队列（可选 RocketMQ 支持）

## 测试策略

### 测试组织

- **业务层单元测试**：Controller 层和 Service 层使用纯单元测试，不依赖其他组件
- **集成测试**：Manager 层使用完整 Spring 上下文，使用 `@SpringBootTest`
- **基础配置**：`TestConfig` 排除 Redis/WebMvc 以进行快速单元测试

### 业务层测试规范（Controller & Service）

**Controller 层测试**：

- **使用 `@ExtendWith(MockitoExtension.class)`** - 纯单元测试，不启动 Spring 上下文
- **使用 `@Mock` 模拟所有依赖** - Manager、Assembler 等全部模拟
- **使用 `@InjectMocks` 创建控制器实例** - 让 Mockito 自动注入模拟依赖
- **不使用 `@WebMvcTest`** - 避免 Spring 上下文启动和其他控制器干扰
- **专注业务逻辑测试** - 验证控制器方法调用、参数传递、返回值处理

```java

@Slf4j
@ExtendWith(MockitoExtension.class)
public class StreamProxyControllerTest {

  @Mock
  private StreamProxyManager streamProxyManager;

  @Mock
  private StreamProxyWebAssembler streamProxyWebAssembler;

  @InjectMocks
  private StreamProxyController streamProxyController;

  @Test
  public void testGetById() {
    // 测试控制器业务逻辑
  }
}
```

**Service 层测试**：

- **使用 `@ExtendWith(MockitoExtension.class)`** - 纯单元测试
- **使用 `@Mock` 模拟所有依赖** - Repository、外部服务等全部模拟
- **使用 `@InjectMocks` 创建服务实例** - 自动注入模拟依赖
- **不使用 `@SpringBootTest`** - 避免 Spring 上下文和数据库依赖
- **专注单一服务逻辑测试** - 验证业务逻辑、异常处理、数据转换

```java

@Slf4j
@ExtendWith(MockitoExtension.class)
public class StreamProxyServiceTest {

  @Mock
  private StreamProxyMapper streamProxyMapper;

  @Mock
  private RedisCache redisCache;

  @InjectMocks
  private StreamProxyServiceImpl streamProxyService;

  @Test
  public void testCreateStreamProxy() {
    // 测试服务业务逻辑
  }
}
```

### Manager 测试规则

- **所有 Manager 类必须使用集成测试** - 扩展 `BaseTest` 进行真实数据处理
- **Manager 测试需要完整的 Spring 上下文**，包含实际数据库事务
- **在 Manager 测试中不模拟 Service/Repository 依赖** - 测试真实数据流
- **使用 `@Transactional` 进行自动回滚**，在每个测试方法后

### HTTP API 集成测试规则

- **HTTP API 集成测试不使用 `@Transactional`** - 避免 MySQL 锁等待超时问题
- **继承 `BaseStreamProxyIntegrationTest`** - 提供 HTTP 测试专用基类，不包含事务管理
- **手动数据清理** - 在 `@BeforeEach` 和 `@AfterEach` 中手动清理测试数据
- **原因**：`TestRestTemplate` 的 HTTP 调用在事务外执行，长时间事务会导致数据库锁竞争
- **UNIQUE 约束处理**：由于没有事务回滚，必须彻底清理测试数据避免主键/唯一键冲突

### 异步测试和事务使用规则

- **异步测试不使用 `@Transactional`** - 异步操作通常在事务边界外执行，事务无法管理异步线程中的数据操作
- **包含 Hook 回调的测试不使用 `@Transactional`** - Hook 回调通常是异步执行，事务无法控制回调中的数据库操作
- **外部 HTTP 调用测试不使用 `@Transactional`** - 外部服务调用和回调在测试事务外执行
- **数据清理策略**：
  - 使用 `@BeforeEach` 和 `@AfterEach` 进行手动数据清理
  - 清理时覆盖所有可能的测试数据模式，避免遗漏
  - 使用 try-catch 包围删除操作，避免不存在的数据导致测试失败
- **测试隔离**：通过唯一的测试数据标识符确保不同测试间的数据隔离

### 测试数据库

- **SQLite**（`test-app.db`）轻量级测试
- **模式**：`schema-sqlite.sql` 包含所有必需表
- **清理**：`@BeforeEach`/`@AfterEach` 进行测试隔离

### 模拟策略

- **Controller 层**：使用 `@ExtendWith(MockitoExtension.class)` 配合 `@Mock`、`@InjectMocks` 进行纯单元测试
- **Service 层**：使用 `@ExtendWith(MockitoExtension.class)` 配合 `@Mock`、`@InjectMocks` 进行纯单元测试
- **Manager 层**：使用 `BaseTest` 配合真实 bean 和数据库事务进行集成测试
  - **基础Service层（继承IService<DO>的Service）**：使用 `@Autowired` 进行真实注入，因为这些是数据访问层的基础服务
  - **业务Assembler层**：使用 `@MockitoBean` 进行模拟，因为这些是数据转换逻辑
  - **外部集成服务**：使用 `@MockitoBean` 进行模拟，避免外部依赖
- **集成测试**：真实数据库配合事务隔离
- **重要**：业务层（Controller/Service）禁止使用 `@SpringBootTest`、`@WebMvcTest`、`@MockitoBean` 等 Spring 测试注解

**Manager层依赖分层原则**：

```java

@SpringBootTest
public class XxxManagerTest extends BaseTest {

  @Autowired
  private XxxManager xxxManager; // 被测试的Manager

  // 基础Service层 - 继承IService<DO>的服务使用真实注入
  @Autowired
  private XxxService xxxService; // 真实的数据访问Service

  // 业务组装层 - 数据转换逻辑使用模拟
  @MockitoBean
  private XxxAssembler xxxAssembler; // 模拟的数据转换器

  // 外部集成层 - 外部服务使用模拟
  @MockitoBean
  private ExternalService externalService; // 模拟的外部服务
}
```

**分层判断规则**：

- 如果Service继承 `IService<DO>`：使用 `@Autowired`（基础数据访问层）
- 如果是Assembler、Converter等转换类：使用 `@MockitoBean`（业务逻辑层）
- 如果是外部系统集成类：使用 `@MockitoBean`（集成层）

### 测试策略选择指南

**使用 `@Transactional` 的场景**：

- 同步数据库操作测试
- Manager 层业务逻辑测试
- Repository 层数据访问测试
- 不涉及外部系统调用的集成测试

**不使用 `@Transactional` 的场景**：

- HTTP API 集成测试（使用 `TestRestTemplate`）
- 包含异步操作的测试（线程池、`@Async`）
- 包含外部系统 Hook 回调的测试
- 消息队列相关测试
- 定时任务相关测试
- WebSocket 相关测试

**判断标准**：

- 如果测试涉及**跨线程**或**外部进程**的数据操作 → 不使用 `@Transactional`
- 如果测试的操作**无法被事务回滚控制** → 不使用 `@Transactional`
- 如果测试可能产生**长时间的数据库锁** → 不使用 `@Transactional`

| 层级         | 测试类型  | 测试注解                                                 | 依赖处理                     | 事务管理             | 目的                  |
|------------|-------|------------------------------------------------------|--------------------------|------------------|---------------------|
| Controller | 纯单元测试 | `@ExtendWith(MockitoExtension.class)`                | `@Mock` + `@InjectMocks` | 无                | 验证请求处理逻辑，不依赖其他组件    |
| Service    | 纯单元测试 | `@ExtendWith(MockitoExtension.class)`                | `@Mock` + `@InjectMocks` | 无                | 验证业务逻辑，不依赖数据库和外部系统  |
| Manager    | 集成测试  | `@SpringBootTest` + `BaseTest`                       | 真实 Bean 注入               | `@Transactional` | 验证复杂业务流程和数据库交互      |
| Repository | 集成测试  | `@SpringBootTest` + `BaseTest`                       | 真实数据库                    | `@Transactional` | 验证数据访问和缓存逻辑         |
| HTTP API   | 集成测试  | `@SpringBootTest` + `BaseStreamProxyIntegrationTest` | 真实 Bean 注入               | 无，手动清理           | 验证 HTTP 端点和完整请求响应流程 |
| 异步/Hook    | 集成测试  | `@SpringBootTest` + 自定义基类                            | 真实 Bean 注入               | 无，手动清理           | 验证异步操作、回调和外部系统集成    |

## 常见操作

### 添加新功能

1. 在 `repository` 模块中创建实体（`*DO`）
2. 添加带有 `IService<*DO>` 的服务接口/实现
3. 为业务逻辑协调创建 manager
4. 添加带有完整 Swagger 文档的控制器
5. 创建数据转换装配器
6. 按照现有模式编写单元/集成测试

### 数据库操作

1. 对简单 CRUD 使用 `IService` 基础方法
2. 仅对复杂多表操作使用自定义查询
3. 所有修改通过 Manager 的统一内部方法
4. 通过统一入口点进行缓存失效

### 错误处理

- 对业务错误抛出 `ServiceException`
- 使用 `ServiceExceptionEnum` 进行标准化错误码
- 通过 `GlobalExceptionHandler` 进行全局异常处理
- 记录所有错误及上下文信息

## 配置文件

### 应用配置

- `application.yml`：主配置
- `application-dev.yml`：开发环境
- `application-test.yml`：测试环境
- `application-repo.yml`：数据库配置
- `application-inte.yml`：集成配置

### 开发规则

- **Cursor 规则**：`.cursorrules` - 全面的编码标准
- **项目规则**：`project-rule.md` - 前端开发指南
- 两个文件都包含详细的架构和编码模式

此架构强调清晰的关注点分离、全面的缓存策略和适用于企业视频监控系统的强大集成模式。