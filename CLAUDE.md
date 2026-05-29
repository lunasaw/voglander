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

# 运行单个测试方法
mvn test -Dtest=DeviceManagerTest#testCreateDevice

# 运行需要缓存的 Redis 集成测试
# 这类测试通过 CacheIntegrationTestConfig.RedisConnectionChecker 在运行时探测 Redis，
# 不可用时自动跳过（Assumptions.assumeTrue），无需专用脚本
# 先启动 Redis：brew services start redis  或  docker run -p 6379:6379 -d redis
mvn test -Dtest=MediaNodeCacheIntegrationTest

# 使用特定配置文件运行测试
mvn test -Dspring.profiles.active=test
```

### 测试覆盖率（JaCoCo 聚合报告）
```bash
# 一键生成跨模块聚合覆盖率报告（clean test verify）
./generate-coverage-report.sh

# 报告输出位置
# voglander-coverage-report/target/site/jacoco-aggregate/index.html
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
├── voglander-web/          # REST API 控制器、过滤器、拦截器（应用主模块，含所有测试）
├── voglander-manager/      # 业务逻辑编排、复杂操作
├── voglander-service/      # 核心业务服务、领域逻辑
├── voglander-repository/   # 数据访问、实体、映射器、缓存
├── voglander-integration/  # 外部系统集成（GB28181、ZLM、Excel）
├── voglander-client/       # 外部服务客户端和 DTOs
├── voglander-common/       # 共享工具、常量、枚举、异常
├── voglander-test/         # 测试配置和工具
└── voglander-coverage-report/ # JaCoCo 聚合覆盖率报告（聚合上述模块）
```

### 分层架构

- **Web 层**：REST 控制器、请求/响应处理、参数验证
- **Manager 层**：复杂业务工作流、多服务协调
- **Service 层**：核心业务逻辑、单一职责操作
- **Repository 层**：数据持久化、缓存、数据库操作
- **Integration 层**：外部系统包装器，统一 `ResultDTO` 响应

### Web 层 Controller 模板方法规范

**Controller 层模板方法实现原则**：

- **核心模板方法（必须实现）**：对应Manager层的标准CRUD操作，提供基础数据管理功能
    - `add()`、`update()`、`get()`、`deleteOne()`、`deleteBatch()`、`getPage()`
- **增强业务方法（可选实现）**：包含操作日志和业务逻辑的方法，根据业务需要选择实现
    - `createXxx()`、`updateXxx()`、`deleteXxx()` 等包含日志记录的业务方法
- **专用业务方法（一般不实现）**：处理特定业务场景的方法，通常不需要在Controller层暴露
    - 如状态更新、Key更新、特殊字段更新等细粒度操作方法

**Web层入参标准规范**：

- **必须使用Web层模型**：Controller方法的入参必须使用Web层的Req对象，不能直接使用DTO
    - 创建操作：使用 `*CreateReq` 对象
  - 更新操作：使用 `*UpdateReq` 对象，必须包含`@NotNull`验证的ID字段
    - 查询操作：使用 `*QueryReq` 对象
- **数据转换责任**：通过WebAssembler将Req对象转换为DTO后传递给Manager层
- **层次分离原则**：Web层负责参数验证和格式转换，业务层负责逻辑处理
- **更新操作约定**：Controller层的update方法必须要求携带主键ID，使用`updateById(id, updateDTO)`扩展方法

**查询实现策略**：

- **全量分页条件查询**：实现一个支持所有查询条件的分页接口，让Web端自行构造查询条件
- **条件灵活性**：通过DTO参数支持复杂的多条件组合查询
- **前端主导**：查询逻辑由前端根据业务需求灵活组装，后端提供统一的查询能力

**模板方法优先级**：

1. **Essential（必须）**：核心CRUD模板方法
2. **Optional（可选）**：增强业务方法
3. **Avoid（避免）**：专用业务方法

**Controller模板方法代码示例**：

```java
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/proxy")
@Tag(name = "拉流代理管理", description = "拉流代理增删改查等相关接口")
public class StreamProxyController {

    @Autowired
    private StreamProxyManager streamProxyManager;

    @Autowired
    private StreamProxyWebAssembler streamProxyWebAssembler;

    // ================================
    // 核心模板方法（必须实现）
    // ================================

    @PostMapping("/add")
    @Operation(summary = "新增数据", description = "标准数据创建，校验参数并插入数据库")
    public AjaxResult<Long> add(@Valid @RequestBody StreamProxyCreateReq createReq) {
        StreamProxyDTO dto = streamProxyWebAssembler.createReqToDto(createReq);
        Long id = streamProxyManager.add(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新数据", description = "通过主键ID更新指定字段，要求必须携带ID")
    public AjaxResult<Long> updateProxy(@Valid @RequestBody StreamProxyUpdateReq updateReq) {
        StreamProxyDTO updateDTO = streamProxyWebAssembler.updateReqToDto(updateReq);
        Long id = streamProxyManager.updateById(updateReq.getId(), updateDTO);
        return AjaxResult.success(id);
    }

    @PostMapping("/get")
    @Operation(summary = "灵活单条查询", description = "支持多种条件查询")
    public AjaxResult<StreamProxyVO> getProxy(@RequestBody StreamProxyQueryReq queryReq) {
        StreamProxyDTO queryDTO = streamProxyWebAssembler.queryReqToDto(queryReq);
        StreamProxyDTO result = streamProxyManager.get(queryDTO);
        if (result == null) {
            return AjaxResult.error("记录不存在");
        }
        StreamProxyVO vo = streamProxyWebAssembler.dtoToVo(result);
        return AjaxResult.success(vo);
    }

    @DeleteMapping("/deleteOne")
    @Operation(summary = "单条记录删除", description = "支持多种删除策略")
    public AjaxResult<Void> deleteOne(@RequestBody StreamProxyUpdateReq deleteReq) {
        StreamProxyDTO deleteDTO = streamProxyWebAssembler.updateReqToDto(deleteReq);
        Boolean success = streamProxyManager.deleteOne(deleteDTO);
        if (success) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    @PostMapping("/getPage")
    @Operation(summary = "分页条件查询", description = "全量分页条件搜索")
    public AjaxResult<StreamProxyListResp> getPageWithConditions(
        @RequestBody(required = false) StreamProxyQueryReq queryReq,
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int size) {

        StreamProxyDTO queryDTO = streamProxyWebAssembler.queryReqToDto(queryReq);
        Page<StreamProxyDTO> pageResult = streamProxyManager.getPage(queryDTO, page, size);

        List<StreamProxyVO> voList = pageResult.getRecords().stream()
            .map(streamProxyWebAssembler::dtoToVo)
            .collect(Collectors.toList());

        StreamProxyListResp resp = new StreamProxyListResp();
        resp.setTotal(pageResult.getTotal());
        resp.setItems(voList);
        return AjaxResult.success(resp);
    }

    // ================================
    // 增强业务方法（可选实现）
    // ================================

    @DeleteMapping("/deleteStreamProxy")
    @Operation(summary = "业务删除", description = "业务删除，包含操作日志记录")
    public AjaxResult<Boolean> deleteStreamProxy(@RequestBody StreamProxyUpdateReq streamProxyUpdateReq) {
        StreamProxyDTO streamProxyDTO = streamProxyWebAssembler.updateReqToDto(streamProxyUpdateReq);
        Boolean success = streamProxyManager.deleteStreamProxy(streamProxyDTO, "删除操作");
        return AjaxResult.success(success);
    }
}
```

**关键实现模式**：

- **入参转换**：`WebAssembler.xxxReqToDto(xxxReq)` → Manager层
- **出参转换**：Manager层返回DTO → `WebAssembler.dtoToVo(dto)` → 前端
- **错误处理**：统一使用 `AjaxResult.error()` 和 `AjaxResult.success()`
- **分页响应**：包装为 `*ListResp` 对象，包含 `total` 和 `items` 字段
- **参数验证**：使用 `@Valid` 进行请求参数校验

### 关键设计模式

- **Assembler 模式**：层间数据转换（DTO ↔ DO ↔ VO）
- **Manager 模式**：复杂业务逻辑协调
- **Wrapper 模式**：外部系统集成，统一错误处理
- **Template 模式**：数据操作的统一内部方法，包含缓存/日志

#### Wrapper 层设计模式

**核心原则**：Wrapper层只处理参数验证和异常捕获，业务逻辑应在Service层实现

**模板方法结构**：

```java
public ResultDTO<T> operation(RequestDTO request) {
    return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
        // 1. 参数验证 - 使用断言直接异常
        WrapperExceptionHandler.validateRequest(request, "请求参数描述");
        WrapperExceptionHandler.validateZlmConnection(request.getHost(), request.getSecret());
        // ... 其他参数验证

        // 2. 业务逻辑调用
        T result = ExternalService.callApi(request.getParams());

        // 3. 结果验证
        if (result == null || !isValidResult(result)) {
            log.error("操作失败：结果无效");
            return null; // 异常时返回null
        }

        // 4. 成功返回
        return result; // 成功时直接返回模型

    }, "操作描述");
}
```

**Wrapper层规范**：

- **参数验证**：使用`WrapperExceptionHandler`统一验证方法和断言
- **异常处理**：通过`executeWithExceptionHandling`模板方法统一处理
- **返回约定**：成功返回具体模型，异常返回null，外层包装为`ResultDTO`
- **日志规范**：关键操作节点记录日志，包含必要的业务上下文信息
- **业务隔离**：仅处理参数验证和异常捕获，具体业务逻辑委托给Service层

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

- **sip-gateway-spring-boot-starter 1.8.0** GB28181 SIP 网关接入（取代旧版直接 handler 接入方式）
- **GB28181 client/server 1.8.0** 设备端/平台端协议支持
- **ZLMediaKit-Starter 1.0.10-SNAPSHOT** 媒体流
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

**API 响应类型一致性规则**：

- **ResultDTO** (Integration层外部系统集成)：使用 `getMessage()` 方法获取错误消息
- **AjaxResult** (Web层REST API响应)：使用 `getMsg()` 方法获取错误消息
- **关键区别**：Integration层的外部系统包装器统一返回 `ResultDTO`，Web层控制器统一返回 `AjaxResult`
- **测试规范**：在测试代码中必须根据实际返回类型使用对应的方法，不能混用

### 数据访问模式

- 对简单 CRUD 操作使用 **MyBatis Plus** `IService` 方法
- Manager 层提供统一内部方法：`xxxInternal()`、`deleteXxxInternal()`
- 所有数据修改通过统一入口点进行，确保缓存/日志一致性
- 复杂查询和多表操作仅在 Manager 层
- **UNIQUE 约束处理**：统一内部方法在执行 `saveOrUpdate` 前先根据业务主键（如 `app+stream`）查询现有记录，避免违反唯一约束

### Service 层接口设计规范（重要）

**核心原则**：Service 层所有接口都应使用 DTO 模型作为参数，避免使用基本类型和 DO 对象

**设计规范**：

- **主要接口**：使用 `*DTO` 作为参数，支持灵活的查询和操作条件
- **便利方法**：可以提供 `id`、`key` 等基本参数的重载方法，但作为主要 DTO 方法的补充
- **参数校验**：在 Service 方法中使用 `Assert` 进行必要的参数校验
- **业务异常**：使用 `ServiceException` 抛出业务相关的异常

**标准设计模式**：

```java

@Service
public class XxxBizServiceImpl implements XxxBizService {

    // ================================
    // 主要接口 - 使用 DTO 模型参数（推荐）
    // ================================

    /**
     * 删除实体（主要接口）
     * <p>
     * 支持多种删除策略：ID优先、业务键备用、灵活条件查询
     * </p>
     *
     * @param deleteDTO 删除条件DTO，支持ID、业务键、复合条件
     * @return 删除结果
     */
    @Override
    public boolean deleteEntityWithTermination(EntityDTO deleteDTO) {
        Assert.notNull(deleteDTO, "删除条件不能为空");

        // 业务逻辑：查找记录
        EntityDTO existingEntity = entityManager.get(deleteDTO);
        if (existingEntity == null) {
            log.warn("删除实体：记录不存在 - 条件: {}", JSON.toJSONString(deleteDTO));
            return true;
        }

        // 业务逻辑：执行清理操作
        performCleanupOperations(existingEntity);

        // 业务逻辑：删除数据库记录
        return entityManager.deleteEntity(existingEntity, "删除实体");
    }

    /**
     * 更新实体（主要接口）
     *
     * @param updateDTO 更新内容和查询条件
     * @return 更新结果
     */
    @Override
    public boolean updateEntityWithValidation(EntityDTO updateDTO) {
        Assert.notNull(updateDTO, "更新信息不能为空");

        // 业务验证逻辑
        validateBusinessRules(updateDTO);

        // 执行更新
        return entityManager.updateEntity(updateDTO, "更新实体");
    }

    // ================================
    // 便利方法 - 基本参数重载（补充）
    // ================================

    /**
     * 根据ID删除（便利方法）
     * <p>
     * 内部调用主要的DTO方法，提供便利的API
     * </p>
     *
     * @param id 实体ID
     * @return 删除结果
     */
    @Override
    public boolean deleteEntityWithTermination(Long id) {
        Assert.notNull(id, "实体ID不能为空");

        // 构造DTO并调用主要方法
        EntityDTO deleteDTO = new EntityDTO();
        deleteDTO.setId(id);

        return deleteEntityWithTermination(deleteDTO);
    }

    /**
     * 根据业务键删除（便利方法）
     *
     * @param businessKey 业务键
     * @return 删除结果
     */
    @Override
    public boolean deleteEntityByKeyWithTermination(String businessKey) {
        Assert.hasText(businessKey, "业务键不能为空");

        // 构造DTO并调用主要方法
        EntityDTO deleteDTO = new EntityDTO();
        deleteDTO.setBusinessKey(businessKey);

        return deleteEntityWithTermination(deleteDTO);
    }
}
```

**接口设计层次**：

1. **主要接口（必须）**：使用 `*DTO` 参数，提供完整的业务功能
2. **便利接口（可选）**：使用基本参数，内部调用主要 DTO 接口
3. **废弃接口（避免）**：直接使用 DO 对象或过多基本参数的方法

**迁移策略**：

- **新接口**：必须遵循 DTO 参数规范
- **已有接口**：逐步迁移，保持向后兼容，添加 `@Deprecated` 标记
- **重构原则**：先添加新的 DTO 方法，再标记旧方法为废弃

### LambdaQueryWrapper 查询构建标准（重要）

**强制使用 condition 参数方法**：在使用 MyBatis Plus 的 `LambdaQueryWrapper` 构建查询条件时，必须使用带有 condition
参数的方法，避免手动进行 null 值判断。

**标准模式**：

```java
// 正确方式：使用 condition 参数避免空值判断
LambdaQueryWrapper<DeviceDO> queryWrapper = new LambdaQueryWrapper<>();
queryWrapper.

eq(deviceDTO.getId() !=null,DeviceDO::getId,deviceDTO.

getId())
        .

eq(deviceDTO.getDeviceId() !=null,DeviceDO::getDeviceId,deviceDTO.

getDeviceId())
        .

eq(deviceDTO.getStatus() !=null,DeviceDO::getStatus,deviceDTO.

getStatus())
        .

like(deviceDTO.getName() !=null,DeviceDO::getName,deviceDTO.

getName())
        .

eq(deviceDTO.getIp() !=null,DeviceDO::getIp,deviceDTO.

getIp())
        .

eq(deviceDTO.getType() !=null,DeviceDO::getType,deviceDTO.

getType())
        .

orderByDesc(DeviceDO::getCreateTime);

// 错误方式：手动进行 null 判断（禁止使用）
if(deviceDTO.

getId() !=null){
        queryWrapper.

eq(DeviceDO::getId, deviceDTO.getId());
        }
        if(deviceDTO.

getDeviceId() !=null){
        queryWrapper.

eq(DeviceDO::getDeviceId, deviceDTO.getDeviceId());
        }
```

**核心原则**：

- **避免 extend 字段问题**：不使用 `new LambdaQueryWrapper<>(entityObject)` 构造方式，避免包含 null 值字段导致的查询条件不匹配
- **链式调用**：使用流畅的链式调用提高代码可读性和简洁性
- **条件控制**：第一个参数为条件表达式，只有为 true 时才添加该查询条件
- **类型安全**：使用 Lambda 方法引用确保字段名称的类型安全
- **必须应用场景**：所有 Manager 层的查询构建都必须遵循此标准

### Manager 层设计原则

- **Manager 层对外暴露方法规则**：Manager 层向上层暴露的所有公开方法参数必须使用 DTO 类型，不应该暴露 DO 对象
- **DTO 转换责任**：Manager 层负责 DTO 和 DO 之间的转换，通过 Assembler 完成数据转换
- **内部方法设计**：Manager 内部可以使用 DO 对象进行业务处理，但这些方法应为 private
- **统一命名规范**：向外暴露的查询方法使用 `getXxxDTOByYyy` 格式命名，明确返回 DTO 类型
- **方法废弃策略**：对于历史遗留的暴露 DO 对象的方法，应标记 `@Deprecated` 并提供替代方案

### Manager 层模板方法设计模式

为了提供高度复用且易于维护的基础功能，每个 Manager 类都应该实现以下标准模板方法：

#### 核心模板方法

**基础缓存管理**：

```java
/**
 * 模板方法：统一缓存清理
 * 每个Manager都需要的基础方法，提供高度复用且易于维护的缓存管理
 */
private void clearCache(Long id, String oldKey, String newKey)
```

**CRUD 操作模板**：

```java
/**
 * 模板方法：新增数据
 * 标准流程：校验参数 -> 转换DO -> 插入数据库 -> 返回ID
 * 注意：默认值依赖数据库字段默认值，不在代码中设置
 */
public Long add(XxxDTO dto)

/**
 * 模板方法：条件更新数据（通用版本）
 * 标准流程：校验参数 -> 根据查询条件查找记录 -> 应用更新内容 -> 更新数据库
 * 查询条件和更新内容完全分离，提供最大的灵活性
 */
public Long update(XxxDTO queryDTO, XxxDTO updateDTO)

/**
 * 扩展方法：通过ID更新指定字段
 * 最常用的更新方式，直接通过主键ID更新指定字段
 */
public Long updateById(Long id, XxxDTO updateDTO)

/**
 * 模板方法：单条查询
 * 标准流程：校验参数 -> 转换DO条件 -> 查询数据库 -> 转换并返回DTO
 * 查询实现：使用LambdaQueryWrapper进行类型安全的条件构建
 */
public XxxDTO get(XxxDTO dto)

/**
 * 模板方法：删除单条记录
 * 标准流程：校验参数 -> 通过条件查找 -> 删除数据库记录 -> 清理缓存
 * 删除实现：使用LambdaQueryWrapper构建查询条件
 */
public Boolean deleteOne(XxxDTO dto)

/**
 * 模板方法：批量删除记录
 * 标准流程：校验参数 -> 转换DO条件 -> 查询匹配记录 -> 批量删除 -> 清理缓存
 * 批量实现：使用LambdaQueryWrapper构建批量删除条件
 */
public Boolean deleteBatch(XxxDTO dto)

/**
 * 模板方法：分页查询
 * 标准流程：校验参数 -> 转换DO条件 -> 分页查询数据库 -> 转换记录为DTO -> 返回Page<DTO>
 * 分页实现：使用LambdaQueryWrapper + 默认排序（创建时间降序）
 */
public Page<XxxDTO> getPage(XxxDTO dto, int page, int size)
```

#### 查询条件构建标准

**LambdaQueryWrapper 统一模式**：

```java
// 标准查询条件构建流程
StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
LambdaQueryWrapper<StreamProxyDO> queryWrapper = new LambdaQueryWrapper<>(streamProxyDO);

// 分页查询需要添加默认排序
queryWrapper.orderByDesc(StreamProxyDO::getCreateTime);

// 单条查询需要限制结果数量
queryWrapper.last("limit 1");
```

**条件更新策略**：
```java
// 新的条件更新模式：查询条件和更新内容分离
// 1. 构建查询条件DTO（只包含查询用的字段）
StreamProxyDTO queryDTO = new StreamProxyDTO();
queryDTO.

setApp("live");
queryDTO.

setStream("test");

// 2. 构建更新内容DTO（只包含要更新的字段）
StreamProxyDTO updateDTO = new StreamProxyDTO();
updateDTO.

setUrl("rtmp://updated.example.com/live/test");
updateDTO.

setDescription("Updated description");

// 3. 执行条件更新
Long id = streamProxyManager.update(queryDTO, updateDTO);

// 扩展方法：通过ID更新（最常用）
Long id = streamProxyManager.updateById(1L, updateDTO);
```

#### 模板方法设计优势

- **高度复用**：每个 Manager 都实现相同的基础方法接口，提供统一的操作体验
- **易于维护**：业务逻辑变更只需修改对应的模板方法实现
- **标准化流程**：统一的参数校验、数据转换、异常处理和日志记录
- **缓存一致性**：所有修改和删除操作都通过统一的缓存清理逻辑
- **扩展性强**：在模板方法基础上可以进行自定义扩展，满足特定业务需求
- **类型安全**：使用 LambdaQueryWrapper 提供编译时类型检查

#### 实现规范

- **参数校验**：使用 `Assert` 类进行统一的参数校验
- **异常处理**：统一的异常捕获和业务异常转换
- **日志记录**：标准化的操作日志格式，包含关键业务信息
- **事务管理**：依赖 Spring 的 `@Transactional` 进行事务控制
- **缓存策略**：统一的缓存清理逻辑，支持主键和业务键双重清理
- **查询构建**：使用 `LambdaQueryWrapper` 确保类型安全和代码可维护性
- **默认值处理**：依赖数据库字段默认值，避免在业务代码中硬编码默认值

**完整模板方法示例（基于StreamProxyManager实现）**：

```java
/**
 * 模板方法：新增数据
 * 标准流程：校验参数 -> 转换DO -> 插入数据库 -> 返回ID
 * 注意：默认值依赖数据库字段默认值，不在代码中设置
 */
public Long add(StreamProxyDTO streamProxyDTO) {
    // 校验必要参数
    Assert.notNull(streamProxyDTO, "代理信息不能为空");
    Assert.hasText(streamProxyDTO.getApp(), "应用名称不能为空");
    Assert.hasText(streamProxyDTO.getStream(), "流ID不能为空");
    Assert.hasText(streamProxyDTO.getUrl(), "拉流地址不能为空");

    try {
        // 转为DO
        StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
        streamProxyDO.setCreateTime(LocalDateTime.now());
        streamProxyDO.setUpdateTime(LocalDateTime.now());

        // 插入DB（依赖数据库默认值）
        boolean success = streamProxyService.save(streamProxyDO);
        if (!success) {
            throw new RuntimeException("数据库插入失败");
        }

        // 清理相关缓存
        clearCache(streamProxyDO.getId(), null, streamProxyDO.getProxyKey());

        return streamProxyDO.getId();
    } catch (Exception e) {
        log.error("新增流代理失败 - 错误: {}", e.getMessage(), e);
        throw new RuntimeException("新增流代理失败: " + e.getMessage(), e);
    }
}

/**
 * 模板方法：智能更新
 * 更新策略：优先使用ID，ID为null时使用唯一索引(app+stream)
 */
public Long update(StreamProxyDTO streamProxyDTO) {
    Assert.notNull(streamProxyDTO, "代理信息不能为空");

    try {
        StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
        streamProxyDO.setUpdateTime(LocalDateTime.now());

        String oldProxyKey = null;
        StreamProxyDO existingRecord = null;

        // 智能查询策略：ID优先，业务键备用
        if (streamProxyDO.getId() != null) {
            existingRecord = streamProxyService.getById(streamProxyDO.getId());
            if (existingRecord != null) {
                oldProxyKey = existingRecord.getProxyKey();
            }
        } else if (streamProxyDTO.getApp() != null && streamProxyDTO.getStream() != null) {
            LambdaQueryWrapper<StreamProxyDO> queryWrapper = new LambdaQueryWrapper<>(streamProxyDO);
            queryWrapper.eq(StreamProxyDO::getApp, streamProxyDTO.getApp())
                        .eq(StreamProxyDO::getStream, streamProxyDTO.getStream())
                        .last("limit 1");
            existingRecord = streamProxyService.getOne(queryWrapper);
            if (existingRecord != null) {
                oldProxyKey = existingRecord.getProxyKey();
                streamProxyDO.setId(existingRecord.getId());
            }
        }

        if (existingRecord == null) {
            throw new RuntimeException("未找到要更新的记录");
        }

        boolean success = streamProxyService.updateById(streamProxyDO);
        if (!success) {
            throw new RuntimeException("数据库更新失败");
        }

        clearCache(streamProxyDO.getId(), oldProxyKey, streamProxyDO.getProxyKey());
        return streamProxyDO.getId();
    } catch (Exception e) {
        log.error("更新流代理失败 - 错误: {}", e.getMessage(), e);
        throw new RuntimeException("更新流代理失败: " + e.getMessage(), e);
    }
}

/**
 * 模板方法：类型安全查询
 * 使用LambdaQueryWrapper进行类型安全的条件构建
 */
public StreamProxyDTO get(StreamProxyDTO streamProxyDTO) {
    Assert.notNull(streamProxyDTO, "查询条件不能为空");

    try {
        StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
        LambdaQueryWrapper<StreamProxyDO> queryWrapper = new LambdaQueryWrapper<>(streamProxyDO);
        queryWrapper.last("limit 1");

        StreamProxyDO existingRecord = streamProxyService.getOne(queryWrapper);
        if (existingRecord == null) {
            return null;
        }

        return streamProxyAssembler.doToDto(existingRecord);
    } catch (Exception e) {
        log.error("查询流代理失败 - 错误: {}", e.getMessage(), e);
        throw new RuntimeException("查询流代理失败: " + e.getMessage(), e);
    }
}

/**
 * 模板方法：分页查询带排序
 * 默认按创建时间降序排列，支持复杂条件查询
 */
public Page<StreamProxyDTO> getPage(StreamProxyDTO streamProxyDTO, int page, int size) {
    if (page < 1) throw new IllegalArgumentException("页码必须大于0");
    if (size < 1 || size > 1000) throw new IllegalArgumentException("页大小必须在1-1000之间");

    try {
        StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
        LambdaQueryWrapper<StreamProxyDO> queryWrapper = new LambdaQueryWrapper<>(streamProxyDO);
        queryWrapper.orderByDesc(StreamProxyDO::getCreateTime);

        Page<StreamProxyDO> pageQuery = new Page<>(page, size);
        Page<StreamProxyDO> doPage = streamProxyService.page(pageQuery, queryWrapper);

        // 转换为DTO分页结果
        Page<StreamProxyDTO> dtoPage = new Page<>(page, size);
        dtoPage.setTotal(doPage.getTotal());
        dtoPage.setPages(doPage.getPages());
        dtoPage.setCurrent(doPage.getCurrent());
        dtoPage.setSize(doPage.getSize());

        List<StreamProxyDTO> dtoRecords = streamProxyAssembler.doListToDtoList(doPage.getRecords());
        dtoPage.setRecords(dtoRecords);

        return dtoPage;
    } catch (Exception e) {
        log.error("分页查询流代理失败 - 错误: {}", e.getMessage(), e);
        throw new RuntimeException("分页查询流代理失败: " + e.getMessage(), e);
    }
}
```

**核心设计理念**：

```java
/**
 * 统一缓存清理模板方法
 * 支持主键ID、业务键双重缓存清理策略
 */
private void clearCache(Long id, String oldKey, String newKey) {
    try {
        // 根据ID清理缓存
        if (id != null) {
            Optional.ofNullable(cacheManager.getCache("streamProxy"))
                    .ifPresent(cache -> cache.evict(id));
        }

        // 根据旧业务键清理缓存
        if (oldKey != null) {
            Optional.ofNullable(cacheManager.getCache("streamProxy"))
                    .ifPresent(cache -> cache.evict("key:" + oldKey));
        }

        // 根据新业务键清理缓存（如果与旧键不同）
        if (newKey != null && !newKey.equals(oldKey)) {
            Optional.ofNullable(cacheManager.getCache("streamProxy"))
                    .ifPresent(cache -> cache.evict("key:" + newKey));
        }
    } catch (Exception e) {
        log.warn("缓存清理异常，但不影响业务流程: {}", e.getMessage());
    }
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

### 类型转换规则

- **所有类型转换必须使用 FastJSON2 创建新模型**：通过正反序列化实现类型转换
- **禁止使用字符串解析方法**：避免使用如 `VoglanderZlmHookServiceImpl#buildProxyExtendInfo()` 这类手动字符串解析
- **标准转换模式**：
  ```java
  // 正确方式：使用 FastJSON2 序列化/反序列化
  TargetType target = JSON.parseObject(JSON.toJSONString(source), TargetType.class);

  // 错误方式：手动字符串解析（禁止）
  // String jsonStr = source.toString();
  // 手动解析字符串构建对象...
  ```
- **适用场景**：扩展字段转换、复杂对象映射、类型适配等所有需要类型转换的场景

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

### Integration层架构设计

Integration层负责与外部系统的集成，遵循统一的设计模式和错误处理策略。

**目录结构**：

```
voglander-integration/
├── wrapper/                    # 外部系统包装器
│   ├── common/                # 通用工具和异常处理
│   ├── zlm/                   # ZLM媒体服务器集成
│   ├── gb28181/               # GB28181 SIP 网关集成（基于 sip-gateway 1.8.0）
│   ├── ip/                    # IP 解析等工具
│   └── easyexcel/             # Excel导入导出集成
└── config/                    # 集成配置
```

### GB28181 集成架构（基于 sip-gateway 1.8.0）

GB28181 接入已从旧的「handler 分发」模式迁移到「command 主动发送 + 统一回调」模式，
`wrapper/gb28181/` 下不再有 `request/`、`response/` 的 `*RequestHandler`/`*ResponseHandler` 类。

**出站指令（Command 模式）**：

- `client/command/`、`server/command/` 按业务域拆分子包（`ptz`、`record`、`device`、`alarm`、`catalog`、`status`、`config`、`media`）
- 所有指令类继承抽象基类，统一设备获取、异常处理、日志和 `ResultDTO` 返回：
  - 客户端（设备端）：`AbstractVoglanderClientCommand`，通过 `executeCommand(...)` 调用 `ClientCommandSender`
  - 平台端：`AbstractVoglanderServerCommand`，调用 `ServerCommandSender`
- 设备信息由 `supplier/` 下的 `VoglanderClientDeviceSupplier` / `VoglanderServerDeviceSupplier` 提供

**入站回调（统一入口）**：

- `notifier/VoglanderBusinessNotifier` 是 sip-gateway 接入业务层的**唯一入口**，直接实现框架 `BusinessNotifier`
  接口（覆盖默认的 `@ConditionalOnMissingBean` 的 `NoopBusinessNotifier`）
- **关键约束**：`notify(GatewayEvent)` 必须异步执行（标注 `@Async("sipNotifierExecutor")`），否则会导致设备超时重传；
  因此**不要**继承 `AbstractProtocolBusinessNotifier`——其 `notify()` 为 `final` 且内部自调用，`@Async` 代理无法生效
- 事件类型为三段式 `gb28181.Group.Name`，payload 为 `Map<String,Object>`，按规范用 FastJSON2 反序列化为对应实体
- 注册、保活、离线、地址变更、通道目录、设备信息、媒体状态、会话(INVITE/ACK/BYE)等回调统一在此分发到
  `DeviceRegisterService` / `DeviceManager` / `DeviceChannelManager` / `MediaSessionManager`

**会话状态管理（MediaSessionManager）**：

- `voglander-manager` 的 `MediaSessionManager` + `MediaSessionDO`(`tb_media_session`) 管理 INVITE 点播/回放会话状态
- 业务主键为 SIP `callId`；状态见 `MediaSessionConstant.Status`（ACTIVE=1/CLOSED=0/INVITING=2/FAILED=3）
- 由 Notifier 的 `Session.InviteOk/InviteFailure/Ack/Bye` 与 `Notify.MediaStatus` 事件驱动
  （`onInviteOk`/`onInviteFailure`/`onAck`/`onBye`/`onMediaStatus`）

**多节点 INVITE 上下文（RedisInviteContextStore）**：

- `store/RedisInviteContextStore implements InviteContextStore`，`@ConditionalOnProperty(gateway.gb28181.store.type=redis)`
- 以 `sip:invite:ctx:{callId}` 为键存 `InviteContext(nodeId, ctxKey)`，支撑跨节点回包路由；Redis 故障抛 503
- 单机默认走框架 `InMemoryInviteContextStore`（`type=memory`，默认）

**启动与配置（关键，易踩坑）**：

- **`ApplicationWeb` 必须标注 `@EnableSipServer`**：该注解 `@Import` 了 `Gb28181CommonAutoConfig`
  （提供 `CommandStrategyFactory`）、`SipProxyServerAutoConfig`、`SipProxyAutoConfig`。这些 `@Import` 配置
  **没有** `.imports` 注册，不会被 classpath 自动激活——缺失则 `ServerCommandSender`/`ClientCommandSender` 无法实例化，
  且 `Gb28181GatewayAutoConfiguration`（`@ConditionalOnBean(ServerCommandSender)`）会静默关闭整条事件管线
- **`supplier/VoglanderDeviceSessionCache`（必需）**：实现框架 `DeviceSessionCache`，委托 `VoglanderServerDeviceSupplier`
  按 `deviceId` 提供 `ToDevice` 寻址信息——`ServerCommandSender` 构造依赖它
- **`VoglanderServerDeviceSupplier`/`VoglanderClientDeviceSupplier` 标注 `@Primary`**：避免与框架默认
  `DefaultServerDeviceSupplier`/`DefaultClientDeviceSupplier`（基于 bean 名的 `@ConditionalOnMissingBean`，
  组件扫描顺序下可能共存）产生 `NoUniqueBeanDefinitionException`
- `start/ServerStart`（CommandLineRunner）负责绑定 SIP 监听端口，**不可删除**（全工程唯一调用 `addListeningPoint` 处）；
  1.8.0 `SipLayer` 无 `setSipListener`，须用 `addListeningPoint(ip, port, sipListener, enableLog)` 重载
- `config/properties/` 下 `VoglanderSipServerProperties` / `VoglanderSipClientProperties` 提供 SIP 端配置；
  `gateway:` 段（`node-id`/`gb28181.store.type`/`invite-context-ttl-ms`/`invite-idempotency-window-ms`）见 `application-inte.yml`

**命令 API 迁移要点（1.8.0）**：

- `ServerCommandSender` 由静态工具类改为实例 Bean，按 `deviceId` 调用（如 `deviceInfoQuery(deviceId)`）；
  `ClientCommandSender` 仍为 `@Component` + 静态方法
- 已移除：`PtzCmdEnum`/`PtzUtils`（改用 `PTZControlEnum` + `PTZInstructionBuilder`）、
  `ServerCommandSender.sendCommand("INFO",...)`、`deviceConfigDownloadQuery`、`Device.localIp`、
  `ClientCommandSender.sendCommand(...)`（改用实例 `send(CommandContext)`）

### 外部系统包装器

#### Wrapper层统一异常处理

**WrapperExceptionHandler 核心特性**：

- **统一模板方法**：`executeWithExceptionHandling`提供标准的异常捕获和结果包装
- **参数验证工具**：提供常用的参数验证断言方法
- **日志规范化**：统一的日志格式和错误信息记录
- **类型安全**：泛型支持，确保返回类型的一致性

**使用示例（基于StreamProxyZlmWrapperServiceImpl）**：

```java
@Service
public class ExternalServiceWrapperImpl implements ExternalServiceWrapper {

    @Override
    public ResultDTO<ResponseModel> operateExternal(RequestDTO request) {
        return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
            // 参数验证
            WrapperExceptionHandler.validateRequest(request, "请求参数");
            WrapperExceptionHandler.validateZlmConnection(request.getHost(), request.getSecret());

            // 业务逻辑调用
            ResponseModel result = ExternalAPI.call(request);

            // 结果验证
            if (result == null || !result.isSuccess()) {
                log.error("外部调用失败：{}", result != null ? result.getErrorMsg() : "响应为空");
                return null; // 异常时返回null
            }

            return result; // 成功时返回具体模型

        }, "外部系统操作");
    }
}
```

**参数验证工具方法**：

- `validateRequest(Object, String)` - 验证请求对象不为空
- `validateNotEmpty(String, String)` - 验证字符串不为空
- `validateNotNull(Object, String)` - 验证对象不为空

**专用验证类**：对于特定业务领域的验证，创建专用验证类：

```java
// ZLM特定的验证器
public class ZlmWrapperValidator {
    public static void validateZlmConnection(String host, String secret);
    public static void validateAppAndStream(String app, String stream);
    public static void validateProxyKey(String proxyKey);
    public static void validateStreamUrl(String url);
}
```

**设计原则**：

- 通用验证方法放在`WrapperExceptionHandler`中
- 特定业务验证方法放在对应的专用验证类中
- 避免在通用工具类中放置业务特定的验证逻辑
- 保持工具类的职责单一和可复用性

### 外部系统包装器设计原则

- **职责单一**：仅负责参数验证、异常捕获和结果包装，业务逻辑委托给Service层
- **统一返回格式**：所有Wrapper方法返回`ResultDTO`格式，成功时data为具体模型，失败时data为null
- **异常处理策略**：使用断言进行必要参数验证，捕获所有异常并转换为统一的错误响应
- **日志记录标准**：关键操作节点记录详细日志，包含操作描述、参数信息和执行结果
- **配置外部化**：外部系统连接参数通过配置文件管理，支持多环境配置

### ZLM集成配置

**配置结构**：

```yaml
zlm:
  enable: true
  hook-enable: true
  servers:
    - server-id: zlm-default
      host: localhost
      port: 80
      secret: zlm
```

**配置类特性**：

```java
@Configuration
@ConfigurationProperties(prefix = "zlm")
public class ZlmIntegrationConfig {
    private boolean enable = true;
    private boolean hookEnable = true;
    private List<ZlmServerConfig> servers;

    public ZlmServerConfig getDefaultServer() {
        return servers != null && !servers.isEmpty() ? servers.get(0) : null;
    }
}
```

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
- **测试统一位置**：所有的测试，包括manager,integration,service都需要在
  `voglander/voglander-web/src/test/java/io/github/lunasaw/voglander` 这里实现，其他模块不需要做测试模块

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

#### Manager 层测试标准规范

**完整的 Manager 测试类结构**：

```java
@Slf4j
public class XxxManagerTest extends BaseTest {

    // 测试数据常量 - 使用有意义的业务数据
    private static final String TEST_APP = "live";
    private static final String TEST_STREAM = "test";
    private static final String TEST_OPERATION_DESC = "测试操作";

    // 被测试对象 - 使用真实注入
    @Autowired
    private XxxManager xxxManager;

    @Autowired
    private CacheManager cacheManager;

    // 基础Service层 - 继承IService<DO>的服务使用真实注入
    @Autowired
    private XxxService xxxService;

    // 业务组装层 - 数据转换逻辑使用模拟
    @MockitoBean
    private XxxAssembler xxxAssembler;

    // 测试数据对象
    private XxxDO testDO;
    private XxxDTO testDTO;

    @BeforeEach
    public void setUp() {
        log.info("开始设置测试数据");

        // 1. 清理数据库中的测试数据
        cleanupTestData();

        // 2. 创建测试用的DO和DTO对象
        testDO = createTestDO();
        testDTO = createTestDTO();

        // 3. 设置Assembler的模拟行为
        setupAssemblerMocks();

        log.info("测试数据设置完成");
    }

    @AfterEach
    public void tearDown() {
        log.info("开始清理测试数据");
        cleanupTestData();
        log.info("测试数据清理完成");
    }

    /**
     * 清理测试数据 - 必须实现
     */
    private void cleanupTestData() {
        try {
            // 删除测试数据 - 覆盖所有测试数据模式
            QueryWrapper<XxxDO> wrapper = new QueryWrapper<>();
            wrapper.in("key_field", TEST_KEY, TEST_KEY + "2")
                  .or().in("other_field", TEST_VALUE, TEST_VALUE + "2");
            xxxService.remove(wrapper);

            // 清理缓存
            Cache cache = cacheManager.getCache("cacheName");
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            log.warn("清理测试数据时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 设置Assembler的模拟行为 - 必须实现
     */
    private void setupAssemblerMocks() {
        // 模拟DTO转DO
        when(xxxAssembler.dtoToDo(any(XxxDTO.class)))
            .thenAnswer(invocation -> {
                XxxDTO dto = invocation.getArgument(0);
                XxxDO do = new XxxDO();
                // 设置转换逻辑
                return do;
            });

        // 模拟DO转DTO
        when(xxxAssembler.doToDto(any(XxxDO.class)))
            .thenAnswer(invocation -> {
                XxxDO do = invocation.getArgument(0);
                XxxDTO dto = new XxxDTO();
                // 设置转换逻辑
                return dto;
            });
    }

    // ================================
    // 测试方法 - 覆盖所有公有方法
    // ================================

    @Test
    public void testCreateXxx_Success() {
        // Given
        XxxDTO dto = createTestDTO();

        // When
        Long id = xxxManager.createXxx(dto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        // 验证数据库中的记录
        XxxDO saved = xxxService.getById(id);
        assertNotNull(saved);
        assertEquals(TEST_APP, saved.getApp());

        // 验证Assembler被调用
        verify(xxxAssembler).dtoToDo(dto);

        log.info("创建测试通过，ID: {}", id);
    }

    @Test
    public void testCreateXxx_Validation() {
        // Test null DTO
        assertThrows(IllegalArgumentException.class, () ->
            xxxManager.createXxx(null));

        // Test invalid fields
        XxxDTO dto = createTestDTO();
        dto.setRequiredField("");
        assertThrows(IllegalArgumentException.class, () ->
            xxxManager.createXxx(dto));

        log.info("参数校验测试通过");
    }

    @Test
    public void testCompleteLifecycle() {
        // 1. 创建
        Long id = xxxManager.createXxx(testDTO);
        assertNotNull(id);
        log.info("1. 创建成功: {}", id);

        // 2. 查询验证
        XxxDO created = xxxManager.getById(id);
        assertNotNull(created);
        log.info("2. 查询验证成功");

        // 3. 更新
        xxxManager.updateXxx(created, "更新操作");
        log.info("3. 更新成功");

        // 4. 删除
        boolean deleted = xxxManager.deleteXxx(id, "删除操作");
        assertTrue(deleted);
        assertNull(xxxManager.getById(id));
        log.info("4. 删除成功");

        log.info("完整生命周期测试通过");
    }
}
```

#### Manager 测试必备要素

1. **测试数据管理**：
    - 使用有意义的业务常量定义测试数据
    - 实现 `cleanupTestData()` 方法清理测试数据
    - 使用 `@BeforeEach` 和 `@AfterEach` 确保测试隔离

2. **依赖注入策略**：
    - 被测试的 Manager：`@Autowired`
    - 基础 Service（继承 `IService<DO>`）：`@Autowired`
    - CacheManager：`@Autowired`
    - 业务 Assembler：`@MockitoBean`
    - 外部集成服务：`@MockitoBean`

3. **模拟设置**：
    - 实现 `setupAssemblerMocks()` 方法设置数据转换模拟
    - 使用 `thenAnswer()` 而不是 `thenReturn()` 进行动态转换
    - 覆盖双向转换（DTO↔DO）

4. **测试覆盖**：
    - 覆盖 Manager 类的所有公有方法
    - 包含正常流程、异常场景、边界条件
    - 实现完整的生命周期测试
    - 验证 Assembler 调用和数据库操作

5. **断言验证**：
    - 验证返回值的正确性
    - 验证数据库中的实际数据
    - 验证 Mock 对象的调用
    - 验证缓存清理操作

6. **日志记录**：
    - 在关键测试点记录日志
    - 便于问题排查和测试过程跟踪

#### Manager 测试质量标准

- **方法覆盖率**：100% 覆盖所有公有方法
- **场景覆盖率**：正常流程 + 异常处理 + 边界条件
- **数据清理**：完整的测试数据清理机制
- **测试隔离**：每个测试方法独立运行
- **性能验证**：验证缓存和性能优化逻辑

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

### 并发测试和数据隔离规则

**并发测试唯一键策略**：

- **时间戳唯一键生成**：在并发测试中使用 `System.currentTimeMillis()` 生成唯一标识符，避免数据冲突
  ```java
  String stream = TEST_STREAM + "_concurrent_" + index + System.currentTimeMillis();
  ```
- **适用场景**：多线程并发执行的测试方法，需要创建多个相似但唯一的测试数据
- **注意事项**：时间戳精度可能不够，建议结合线程索引或随机数确保唯一性

**测试数据清理策略**：

- **集成测试数据隔离**：每个测试方法必须确保数据独立性，避免测试间相互干扰
- **异步操作等待**：包含异步处理的测试需要适当等待，确保异步操作完成后再验证结果
  ```java
  waitForAsyncOperation(200); // 等待异步操作完成
  ```
- **Hook回调测试**：涉及外部系统回调的测试需要模拟完整的回调流程，验证状态更新
- **并发测试验证**：并发测试完成后需要逐一验证每个并发任务的执行结果

**测试方法执行顺序**：

- 使用 `@TestMethodOrder(MethodOrderder.DisplayName.class)` 确保测试执行顺序
- 测试方法命名采用数字前缀确保执行顺序：`test01_`, `test02_`, `test03_`
- 复杂集成测试建议按照业务流程顺序执行

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

### 异常处理标准

- 对业务错误抛出 `ServiceException`
- 使用 `ServiceExceptionEnum` 进行标准化错误码
- **新异常规则**：在抛出 `ServiceException` 前，必须检查 `ServiceExceptionEnum` 中是否存在对应的异常类型，如果不存在则需要先在
  `ServiceExceptionEnum` 中添加新的异常类型
- 通过 `GlobalExceptionHandler` 进行全局异常处理
- 记录所有错误及上下文信息

### 代码风格规范

- 优先使用块注释而不是行尾注释，保持代码整洁性
- 行尾注释应尽量避免，如有必要的说明应使用块注释形式

### 代码重构规则

- **新增未被Git管理的代码重构规则**：处于新增状态且未被Git管理的代码可以直接重构，不需要考虑向后兼容性
- 此规则适用于：
    - 新创建的类和方法
    - 未提交到版本控制的代码
    - 实验性功能和原型代码
- 已被Git管理且已提交的代码修改时需要考虑向后兼容性，特别是公共API和接口

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