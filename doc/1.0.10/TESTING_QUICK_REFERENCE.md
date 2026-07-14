# Voglander 测试规范快速参考卡片

## 🎯 测试层级决策树

```
开始测试
    │
    ├─ 是协议层代码（GB28181 Command/Handler/Notifier）？
    │   └─ YES → 使用 @ExtendWith(MockitoExtension.class)
    │             纯单元测试，全部 @Mock
    │
    ├─ 是 Controller 代码？
    │   └─ YES → 使用 @ExtendWith(MockitoExtension.class)
    │             Mock Manager + WebAssembler
    │
    ├─ 是 Service 代码？
    │   └─ YES → 使用 @ExtendWith(MockitoExtension.class)
    │             Mock Mapper
    │
    ├─ 是 Manager 代码？
    │   ├─ 有异步操作（@Async/CountDownLatch/并发）？
    │   │   └─ YES → 继承 BaseAsyncTest
    │   │             无 @Transactional，手动清理
    │   └─ NO → 继承 BaseTest
    │             自动回滚，无需清理
    │
    └─ 是端到端场景？
        └─ YES → 继承 BaseE2eTest
                  完整流程验证
```

## 📋 测试基类选择表

| 测试对象 | 基类/注解 | 事务回滚 | 外部依赖 | 数据清理 |
|---------|----------|---------|---------|---------|
| GB28181 Handler | `@ExtendWith(Mockito)` | 无 | 全Mock | 无需 |
| Controller | `@ExtendWith(Mockito)` | 无 | 全Mock | 无需 |
| Service | `@ExtendWith(Mockito)` | 无 | 全Mock | 无需 |
| Manager (同步) | `BaseTest` | ✅ 有 | Mock外部 | 自动 |
| Manager (异步) | `BaseAsyncTest` | ❌ 无 | Mock外部 | 手动 |
| E2E | `BaseE2eTest` | ❌ 无 | Mock外部 | 手动 |

## 🔧 核心配置组件

### TestRedisConfig（Mock Redis）
```java
@SpringBootTest
@Import(TestRedisConfig.class)  // BaseTest 已默认导入
public class MyTest extends BaseTest {
    // 自动使用 Mock RedisTemplate
    // connection.ping() 抛异常 → Assumptions 跳过依赖真实 Redis 的测试
}
```

### CacheTestConfig（动态缓存）
```java
@SpringBootTest
@Import(CacheTestConfig.class)  // BaseTest 已默认导入
public class MyTest extends BaseTest {
    // 自动使用 ConcurrentMapCacheManager（动态创建缓存区）
    // SQLite WAL 模式自动启用（减少锁冲突）
}
```

### SSE 事件总线（本地模式）
```yaml
# application-test.yml
sse:
  type: local  # 使用 LocalSseEventBus，无需 Redis
```

## 🎲 唯一标识生成

```java
// 避免并发测试数据冲突
String deviceId = UniqueKeyFactory.deviceId();
// → "34020000001320000001_1719123456789_42"
//                          ↑时间戳    ↑线程ID

String callId = UniqueKeyFactory.callId();
// → "call_1719123456789_42"

String channelId = UniqueKeyFactory.channelId();
String streamKey = UniqueKeyFactory.streamKey("live");
```

## 📝 测试模板速查

### 1. 协议层单元测试
```java
@Slf4j
@ExtendWith(MockitoExtension.class)
public class Gb28181HandlerTest {
    @Mock private DeviceManager deviceManager;
    @InjectMocks private Gb28181ProtocolHandler handler;
    
    @Test
    public void testRouting() {
        handler.handle(createEvent("Lifecycle", "Register", DEVICE_ID));
        verify(deviceManager, times(1)).method(any());
    }
}
```

### 2. Controller 单元测试
```java
@Slf4j
@ExtendWith(MockitoExtension.class)
public class DeviceControllerTest {
    @Mock private DeviceManager deviceManager;
    @Mock private WebAssembler webAssembler;
    @InjectMocks private DeviceController controller;
    
    @Test
    public void testAdd() {
        when(webAssembler.createReqToDto(any())).thenReturn(dto);
        when(deviceManager.add(dto)).thenReturn(1L);
        
        AjaxResult<Long> result = controller.add(req);
        assertTrue(result.isSuccess());
    }
}
```

### 3. Manager 集成测试（同步）
```java
@Slf4j
public class DeviceManagerTest extends BaseTest {
    @Autowired private DeviceManager deviceManager;
    @MockitoBean private Gb28181Wrapper gb28181Wrapper;
    @MockitoBean private ManagerAssembler managerAssembler;
    
    @BeforeEach
    public void setUp() {
        when(managerAssembler.dtoToDo(any(DeviceDTO.class)))
            .thenAnswer(inv -> JSON.parseObject(
                JSON.toJSONString(inv.getArgument(0)), DeviceDO.class));
    }
    
    @Test
    public void testAdd() {
        String deviceId = UniqueKeyFactory.deviceId();
        Long id = deviceManager.add(createDTO(deviceId));
        assertNotNull(id);
    }
    // @Transactional 自动回滚
}
```

### 4. Manager 集成测试（异步）
```java
@Slf4j
public class MediaSessionAsyncTest extends BaseAsyncTest {
    @Autowired private MediaSessionManager manager;
    @Autowired private MediaSessionService service;
    private List<String> callIdsToClean = new ArrayList<>();
    
    @Test
    public void testAsync() throws Exception {
        String callId = UniqueKeyFactory.callId();
        callIdsToClean.add(callId);
        
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
            try { manager.asyncMethod(callId); }
            finally { latch.countDown(); }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
    
    @AfterEach
    public void tearDown() {
        for (String callId : callIdsToClean) {
            LambdaQueryWrapper<MediaSessionDO> qw = new LambdaQueryWrapper<>();
            qw.eq(MediaSessionDO::getCallId, callId);
            service.remove(qw);
        }
        callIdsToClean.clear();
    }
}
```

## 🚀 测试执行命令

```bash
# 本地快速测试（跳过外部服务依赖）
mvn test

# 强制执行所有集成测试（需外部服务）
mvn test -Pintegration-tests

# 单个测试类
mvn test -Dtest=DeviceManagerTest

# 单个测试方法
mvn test -Dtest=DeviceManagerTest#testAdd

# 并行执行（加速）
mvn test -DforkCount=2

# 覆盖率报告
./generate-coverage-report.sh
```

## ✅ 必须遵守的规则

1. ✅ **协议层禁用 @SpringBootTest**
2. ✅ **Controller 禁用 @SpringBootTest**
3. ✅ **Manager 必须集成测试**（BaseTest/BaseAsyncTest）
4. ✅ **异步测试必须手动清理**（BaseAsyncTest + @AfterEach）
5. ✅ **必须使用唯一标识**（UniqueKeyFactory）
6. ✅ **必须使用 RANDOM_PORT**
7. ✅ **默认测试不依赖外部服务**

## ❌ 禁止的反模式

1. ❌ 单元测试启动 Spring 容器
2. ❌ 测试中硬编码 ID/端口
3. ❌ 依赖测试执行顺序
4. ❌ BaseTest 中使用异步操作
5. ❌ 使用 `Thread.sleep()` 等待
6. ❌ 跳过测试数据清理
7. ❌ 使用生产环境配置

## 🐛 故障排查速查

| 错误 | 原因 | 解决方案 |
|-----|------|---------|
| `NoUniqueBeanDefinitionException` | 缺少 @Primary CacheManager | 导入 `CacheTestConfig` |
| `Redis connection refused` | 依赖真实Redis但未启动 | 导入 `TestRedisConfig` 或启动 Redis |
| `SQLITE_BUSY` | 并发写入冲突 | 确保导入 `CacheTestConfig`（WAL模式）|
| 事务未回滚 | 使用了 BaseAsyncTest | 改用 BaseTest 或手动清理 |
| 端口冲突 | 未使用 RANDOM_PORT | 继承 BaseTest/BaseE2eTest |
| SSE 事件未收到 | 使用 Mock Redis | 配置 `sse.type=redis` + 真实 Redis |

## 📊 覆盖率目标

| 层级 | 行覆盖率 | 分支覆盖率 |
|-----|---------|-----------|
| Controller | ≥ 80% | ≥ 70% |
| Service | ≥ 90% | ≥ 80% |
| Manager | ≥ 85% | ≥ 75% |
| Protocol Handler | ≥ 95% | ≥ 90% |
| Wrapper | ≥ 70% | ≥ 60% |

## 📚 相关文档

- **完整规范**：`TESTING_SPECIFICATION.md`
- **执行指南**：`../1.0.9/TEST_EXECUTION_GUIDE.md`
- **迁移指南**：`../1.0.9/MIGRATION_GUIDE.md`
- **问题排查**：`../1.0.9/TROUBLESHOOTING.md`

---

**快速开始**：复制对应模板 → 替换类名 → 运行 `mvn test`

---

## 🔌 外部依赖测试过滤

### 默认行为

```bash
mvn test
# ↓ Assumptions 自动跳过需要 Redis/PostgreSQL/ZLM 的测试
# ✅ 2-3 分钟快速反馈

mvn test -Pintegration-tests  
# ↓ 强制执行所有外部依赖测试
# ❌ 服务不可用时失败
```

### 标准模板：Redis 依赖测试

```java
@Tag("requires-redis")
@SpringBootTest
@TestPropertySource(properties = "sse.type=redis")
public class RedisIntegrationTest {
    
    @BeforeEach
    void checkRedisAvailable() {
        boolean integrationMode = Boolean.getBoolean("integration.tests.enabled");
        
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.ping();
        } catch (Exception e) {
            if (integrationMode) {
                fail("集成测试模式要求 Redis 可用: " + e.getMessage());
            } else {
                Assumptions.assumeTrue(false, 
                    "Redis 不可用，跳过测试: " + e.getMessage());
            }
        }
    }
    
    @Test
    public void testRedisFeature() {
        // 仅在 Redis 可用时执行
    }
}
```

### 外部依赖标签

| 标签 | 外部依赖 | 示例 |
|-----|---------|------|
| `@Tag("requires-redis")` | Redis | RedisBackedSseEventBusIntegrationTest |
| `@Tag("requires-postgresql")` | PostgreSQL | PostgreSQLIntegrationTest |
| `@Tag("requires-zlm")` | ZLMediaKit | ZlmMediaIntegrationTest |

### 过滤命令

```bash
# 排除所有外部依赖测试
mvn test -DexcludedGroups="requires-redis,requires-postgresql,requires-zlm"

# 仅执行 Redis 依赖测试
mvn test -Dgroups="requires-redis"
```

### 检查清单

编写外部依赖测试：
- [ ] 添加 `@Tag("requires-xxx")`
- [ ] 实现 `@BeforeEach` 服务检测
- [ ] 使用 `Assumptions.assumeTrue()`
- [ ] 不导入 `TestRedisConfig`

验证：
- [ ] `mvn test` 自动跳过
- [ ] `mvn test -Pintegration-tests` 通过
- [ ] 跳过原因显示在报告中

**详细文档**：`EXTERNAL_DEPENDENCY_TESTING.md`

