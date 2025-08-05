# StreamProxy 集成测试设计

## 概述

本文档描述了针对 StreamProxy 拉流代理管理系统的完整集成测试架构。测试覆盖从 zlm-starter 接口调用到后端 Hook
回调处理再到数据库存储的完整流程。

## 测试架构

### 测试结构

```
voglander-web/src/test/java/io/github/lunasaw/voglander/zlm/
├── BaseStreamProxyIntegrationTest.java          # 集成测试基类
├── StreamProxyEndToEndIntegrationTest.java      # 端到端流程测试
├── StreamProxyHttpApiIntegrationTest.java       # HTTP API 集成测试
├── StreamProxyPerformanceIntegrationTest.java   # 性能和并发测试
├── StreamProxyIntegrationTestSuite.java         # 测试套件
├── mock/                                        # Mock 实现
│   ├── ZlmServiceMock.java                     # ZLM 服务 Mock
│   ├── ZlmHookSimulator.java                   # Hook 回调模拟器
│   ├── OnProxyAddedHookRequest.java            # Hook 请求参数
│   └── OnServerStartedHookRequest.java         # 服务器启动参数
└── util/
    └── StreamProxyTestDataUtil.java            # 测试数据工具类
```

### 测试流程覆盖

#### 完整集成流程

```
1. REST API 调用 (Controller)
   ↓
2. Manager 业务协调层
   ↓
3. Service 核心业务逻辑
   ↓
4. ZLM 外部服务调用 (Mock)
   ↓
5. 数据库存储
   ↓
6. Hook 回调处理
   ↓
7. 状态更新入库
   ↓
8. 缓存更新验证
```

## 核心测试类说明

### 1. BaseStreamProxyIntegrationTest

**作用**: 集成测试基类
**特性**:

- Spring Boot 完整上下文
- ZLM 服务 Mock 配置
- 测试数据管理
- 公共工具方法
- 自动清理机制

**关键配置**:

```yaml
# 禁用不必要组件
local.sip.server.enabled=false
local.sip.client.enabled=false
sip.enable=false

# 启用ZLM测试
zlm.enable=true
zlm.hook.enable=true
zlm.hook.admin.enable=true
```

### 2. StreamProxyEndToEndIntegrationTest

**作用**: 端到端流程集成测试
**测试场景**:

- ✅ 完整创建流程测试
- ✅ 并发 Hook 回调处理测试
- ✅ Hook 回调异常处理测试
- ✅ 缓存一致性验证测试

**关键测试方法**:

```java
// 测试完整创建流程：API -> Manager -> Service -> Hook -> 数据库
test01_CompleteCreateFlow()

// 测试并发Hook回调处理
test02_ConcurrentHookCallbackTest()

// 测试异常处理机制
test03_HookCallbackExceptionHandling()

// 测试缓存一致性
test04_CacheConsistencyTest()
```

### 3. StreamProxyHttpApiIntegrationTest

**作用**: HTTP API 集成测试
**测试场景**:

- ✅ 创建拉流代理 API
- ✅ 根据 ID 获取代理 API
- ✅ 根据 ProxyKey 获取代理 API
- ✅ 分页查询代理 API
- ✅ 更新代理状态 API
- ✅ 删除代理 API
- ✅ API 参数验证测试

**测试特点**:

- 使用 `TestRestTemplate` 进行真实 HTTP 请求
- 验证 HTTP 状态码和响应格式
- 测试请求参数验证
- 验证 API 与 Hook 回调的联动

### 4. StreamProxyPerformanceIntegrationTest

**作用**: 性能和并发测试
**测试场景**:

- ✅ 批量创建代理性能测试
- ✅ 高并发 Hook 回调处理测试
- ✅ 缓存性能测试
- ✅ 大数据量分页查询性能测试
- ✅ 内存使用和清理测试

**性能基准**:

- 代理创建: < 100ms/个
- Hook 回调并发处理成功率: > 90%
- 缓存查询: < 5ms/次
- 分页查询: < 50ms/页
- 内存增长: < 50MB

## Mock 实现说明

### ZlmServiceMock

**功能**: 模拟 ZLMediaKit 外部服务
**Mock 接口**:

```java
// 添加拉流代理
addStreamProxy(app, stream, url, params) → 成功响应

// 删除拉流代理  
delStreamProxy(app, stream, key) → 成功响应

// 获取代理列表
getStreamProxyList() → 空列表响应
```

### ZlmHookSimulator

**功能**: 模拟 ZLM Hook 回调
**模拟场景**:

```java
// 模拟代理添加成功回调
simulateProxyAddedHook(baseUrl, app, stream, url, proxyKey)

// 模拟服务器启动回调
simulateServerStartedHook(baseUrl, serverId, apiSecret, httpPort)

// 模拟流状态变化回调
simulateStreamChangedHook(baseUrl, app, stream, regist)
```

## 测试数据管理

### StreamProxyTestDataUtil

**功能**: 测试数据生命周期管理
**核心方法**:

```java
// 创建基础测试代理
createBasicTestProxy(suffix)

// 批量创建测试代理
createBatchTestProxies(count)

// 创建完整状态测试代理
createFullStatusTestProxy(suffix, proxyKey)

// 清理所有测试数据
cleanAllTestData()

// 验证数据完整性
verifyProxyDataIntegrity(proxy)
```

## 运行测试

### 单独运行测试类

```bash
# 运行端到端集成测试
mvn test -Dtest=StreamProxyEndToEndIntegrationTest

# 运行API集成测试  
mvn test -Dtest=StreamProxyHttpApiIntegrationTest

# 运行性能测试
mvn test -Dtest=StreamProxyPerformanceIntegrationTest
```

### 运行完整测试套件

```bash
# 运行所有StreamProxy集成测试
mvn test -Dtest=StreamProxyIntegrationTestSuite
```

### 指定测试环境

```bash  
# 使用测试配置文件
mvn test -Dspring.profiles.active=test -Dtest=StreamProxy*IntegrationTest
```

## 测试环境要求

### 基础要求

- ✅ Java 17
- ✅ Spring Boot 3.5.3
- ✅ SQLite 测试数据库 (test-app.db)
- ✅ 内存缓存配置

### 依赖组件

- ✅ ZLM Spring Boot Starter (Mock)
- ✅ Spring Boot Test
- ✅ JUnit 5
- ✅ Mockito
- ✅ TestRestTemplate

### 配置要求

```yaml
spring:
  profiles:
    active: test
  datasource:
    url: jdbc:sqlite:test-app.db
  cache:
    type: simple
```

## 测试覆盖率

### 业务流程覆盖

- ✅ 拉流代理完整生命周期 (创建→Hook回调→状态更新→删除)
- ✅ HTTP API 端点全覆盖
- ✅ 异常处理和边界条件
- ✅ 并发和性能场景
- ✅ 缓存一致性验证

### 组件集成覆盖

- ✅ Controller → Manager → Service → Repository 层级
- ✅ ZLM 外部服务集成 (Mock)
- ✅ Hook 回调处理机制
- ✅ 数据库事务和一致性
- ✅ 缓存操作和失效机制

### 质量保证

- ✅ 自动数据清理
- ✅ 事务隔离
- ✅ 并发安全性
- ✅ 内存泄漏检测
- ✅ 性能基准验证

## 问题排查

### 常见问题

1. **端口冲突**: 测试使用随机端口，确保端口可用
2. **数据库锁**: 确保测试数据库文件可写
3. **缓存问题**: 测试间缓存隔离配置
4. **Mock 配置**: 确保 Mock 服务正确注入

### 调试建议

```yaml
# 开启调试日志
logging:
  level:
    io.github.lunasaw.voglander.zlm: DEBUG
    io.github.lunasaw.voglander.intergration: DEBUG
    io.github.lunasaw.voglander.manager.manager.StreamProxyManager: DEBUG
```

### 性能调优

- 适当调整测试数据量
- 根据环境调整超时时间
- 合理设置并发数量
- 监控内存使用情况

---

**总结**: 该集成测试架构提供了对 StreamProxy 系统的全面测试覆盖，从接口调用到数据库存储的完整流程验证，包含性能测试、并发测试和异常处理测试，确保系统的稳定性和可靠性。