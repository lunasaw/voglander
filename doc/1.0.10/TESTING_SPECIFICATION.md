# Voglander 测试规范 v1.0.10

## 目录

- [1. 概述](#1-概述)
- [2. 测试分层架构](#2-测试分层架构)
- [3. SIP协议层测试规范](#3-sip协议层测试规范)
- [4. 业务层测试规范](#4-业务层测试规范)
- [5. 集成测试规范](#5-集成测试规范)
- [6. 测试环境隔离](#6-测试环境隔离)
- [7. 测试数据管理](#7-测试数据管理)
- [8. 测试执行策略](#8-测试执行策略)

---

## 1. 概述

### 1.1 核心原则

Voglander 测试体系遵循以下核心原则：

1. **内部自洽**：单元测试和集成测试应在无外部依赖的情况下独立运行
2. **分层隔离**：不同层级使用不同的测试策略和依赖替代方案
3. **快速反馈**：默认测试应在本地环境快速执行，无需外部服务
4. **可选验证**：需要真实外部服务的集成测试应可选择性执行

### 1.2 测试金字塔

```
        ┌─────────────────┐
        │   E2E 测试      │  端到端场景验证（最少）
        │  BaseE2eTest    │
        └─────────────────┘
       ┌───────────────────┐
       │  集成测试          │  跨层协作验证（适中）
       │  BaseTest/Async   │
       └───────────────────┘
      ┌─────────────────────┐
      │   单元测试           │  逻辑正确性验证（最多）
      │   @ExtendWith       │
      └─────────────────────┘
```

### 1.3 依赖替代策略

| 外部依赖　　 | 默认测试替代方案　　　　　| 集成测试方案　　　　　　　　　　　 |
| --------------| ---------------------------| ------------------------------------|
| Redis　　　　| Mock RedisTemplate　　　　| 真实Redis + Assumptions　　　　　　|
| PostgreSQL　 | SQLite　　　　　　　　　　| 真实PostgreSQL + Assumptions　　　 |
| SSE 事件总线 | LocalSseEventBus　　　　　| LocalSseEventBus（分布式场景除外） |
| 缓存　　　　 | ConcurrentMapCacheManager | 同左　　　　　　　　　　　　　　　 |
| 外部API　　　| Mock/WireMock　　　　　　 | TestContainers　　　　　　　　　　 |

---

## 2. 测试分层架构

### 2.1 分层定义

Voglander 测试分为三个层级，每层有明确的职责和适用场景：

| 层级 | 职责 | 测试类型 | 外部组件 |
|-----|------|---------|---------|
| **SIP协议层** | GB28181/ONVIF协议处理、Command/Notifier路由 | 纯单元测试 | 无（全Mock）|
| **业务层** | Manager/Service业务逻辑 | 单元+集成测试 | Mock外部依赖 |
| **集成测试层** | 端到端场景、跨模块协作 | 集成测试 | 可选真实服务 |

### 2.2 测试基类选择矩阵

```
┌─────────────────────────────────────────────────────────────────┐
│ 测试场景                        │ 基类/注解              │ 事务  │
├─────────────────────────────────────────────────────────────────┤
│ Controller/Service 单元测试     │ @ExtendWith(Mockito)   │ 无   │
│ Manager 同步集成测试            │ BaseTest               │ 有   │
│ Manager 异步集成测试            │ BaseAsyncTest          │ 无   │
│ Repository 集成测试             │ BaseTest               │ 有   │
│ GB28181 协议层单元测试          │ @ExtendWith(Mockito)   │ 无   │
│ E2E 端到端测试                  │ BaseE2eTest            │ 无   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. SIP协议层测试规范

### 3.1 适用范围

位于 `voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/` 的：

- **Command 层**：`client/command/*`、`server/command/*`（出站指令）
- **Notifier 层**：`notifier/*`（入站事件处理）
- **Handler 层**：`handler/*`（协议路由）
- **Supplier 层**：`supplier/*`（设备信息提供）

### 3.2 测试策略：纯单元测试

**强制规则**：SIP协议层测试**禁止**使用 `@SpringBootTest`，必须使用纯 Mockito。

#### 3.2.1 标准模板

```java
@Slf4j
@ExtendWith(MockitoExtension.class)
public class Gb28181ProtocolHandlerTest {

    @Mock
    private DeviceRegisterService deviceRegisterService;
    
    @Mock
    private DeviceManager deviceManager;
    
    @Mock
    private MediaSessionManager mediaSessionManager;
    
    @InjectMocks
    private Gb28181ProtocolHandler handler;
    
    @Test
    public void testRegisterRoutesToLogin() {
        DeviceEvent event = createEvent("Lifecycle", "Register", DEVICE_ID);
        
        handler.handle(event);
        
        verify(deviceRegisterService, times(1)).login(any());
    }
}
```

#### 3.2.2 依赖隔离

| 依赖类型 | 隔离方式 | 示例 |
|---------|---------|------|
| 业务服务 | `@Mock` + `@InjectMocks` | `DeviceManager`、`MediaSessionManager` |
| 数据访问 | Mock Service层，不直接Mock Mapper | 通过Service Mock返回DTO |
| 外部调用 | Mock Wrapper | `ZlmWrapper`、`OnvifWrapper` |
| 事件发布 | Mock ApplicationEventPublisher | `@Mock ApplicationEventPublisher` |

#### 3.2.3 测试覆盖重点

1. **路由正确性**：验证不同事件类型路由到正确的业务方法
2. **参数映射**：验证协议字段正确映射到DTO
3. **异常处理**：验证异常场景的降级行为
4. **状态转换**：验证状态机转换逻辑

**示例：验证事件路由**

```java
@Test
public void testOnlineRoutesToPatchLiveness() {
    handler.handle(event("Lifecycle", "Online", DEVICE_ID, null, null));
    
    verify(deviceManager, times(1))
        .patchLiveness(eq(DEVICE_ID), eq(DeviceConstant.Status.ONLINE), any(LocalDateTime.class));
}
```

### 3.3 Command 层测试

Command 层负责构造和发送 SIP 指令，测试重点：

```java
@ExtendWith(MockitoExtension.class)
public class ClientDeviceInfoQueryCommandTest {
    
    @Mock
    private ClientCommandSender commandSender;
    
    @Mock
    private VoglanderClientDeviceSupplier deviceSupplier;
    
    @InjectMocks
    private ClientDeviceInfoQueryCommand command;
    
    @Test
    public void testExecute_ValidDevice_ReturnsSuccess() {
        // Given
        when(deviceSupplier.getDeviceById(DEVICE_ID)).thenReturn(mockDevice());
        when(commandSender.send(any())).thenReturn(CompletableFuture.completedFuture(mockResponse()));
        
        // When
        ResultDTO<DeviceInfoDTO> result = command.execute(DEVICE_ID);
        
        // Then
        assertTrue(result.isSuccess());
        verify(commandSender, times(1)).send(any(CommandContext.class));
    }
}
```

### 3.4 禁止事项

1. ❌ **禁止使用 @SpringBootTest**：协议层不依赖Spring容器
2. ❌ **禁止访问真实数据库**：所有数据通过Mock Service提供
3. ❌ **禁止依赖Redis**：状态管理通过内存Map模拟
4. ❌ **禁止真实网络调用**：SIP消息发送全部Mock

---

## 4. 业务层测试规范

### 4.1 适用范围

- **Manager 层**：`voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/`
- **Service 层**：`voglander-service/src/main/java/io/github/lunasaw/voglander/service/`
- **Controller 层**：`voglander-web/src/main/java/io/github/lunasaw/voglander/web/controller/`

### 4.2 Controller 层：纯单元测试

**强制规则**：Controller 测试**禁止**使用 `@SpringBootTest` / `@WebMvcTest`，必须使用纯 Mockito。

#### 4.2.1 标准模板

```java
@Slf4j
@ExtendWith(MockitoExtension.class)
public class DeviceControllerTest {
    
    @Mock
    private DeviceManager deviceManager;
    
    @Mock
    private WebAssembler webAssembler;
    
    @InjectMocks
    private DeviceController controller;
    
    @Test
    public void testAdd_ValidRequest_ReturnsSuccess() {
        // Given
        DeviceCreateReq req = createValidRequest();
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId("test-device");
        
        when(webAssembler.createReqToDto(req)).thenReturn(dto);
        when(deviceManager.add(dto)).thenReturn(1L);
        
        // When
        AjaxResult<Long> result = controller.add(req);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData());
        verify(deviceManager, times(1)).add(dto);
    }
}
```

#### 4.2.2 依赖隔离策略

```
Controller
    ↓ Mock
 Manager (业务编排)
    ↓ Mock
 Assembler (数据转换)
```

### 4.3 Service 层：纯单元测试

Service 层实现单表操作，测试策略：

```java
@ExtendWith(MockitoExtension.class)
public class DeviceServiceTest {
    
    @Mock
    private DeviceMapper deviceMapper;
    
    @InjectMocks
    private DeviceServiceImpl deviceService;
    
    @Test
    public void testSaveOrUpdate_NewDevice_CallsInsert() {
        DeviceDO device = new DeviceDO();
        device.setDeviceId("new-device");
        
        when(deviceMapper.selectOne(any())).thenReturn(null);
        when(deviceMapper.insert(device)).thenReturn(1);
        
        boolean result = deviceService.saveOrUpdate(device);
        
        assertTrue(result);
        verify(deviceMapper, times(1)).insert(device);
    }
}
```

### 4.4 Manager 层：集成测试

Manager 层协调多个Service，需要真实Spring容器支持。

#### 4.4.1 同步操作：BaseTest

```java
@Slf4j
public class DeviceManagerTest extends BaseTest {
    
    @Autowired
    private DeviceManager deviceManager;
    
    @MockitoBean
    private Gb28181Wrapper gb28181Wrapper;  // 外部集成Mock
    
    @MockitoBean
    private ManagerAssembler managerAssembler;  // Assembler Mock
    
    @BeforeEach
    public void setUp() {
        // 配置 Assembler 双向转换
        when(managerAssembler.dtoToDo(any(DeviceDTO.class)))
            .thenAnswer(inv -> JSON.parseObject(
                JSON.toJSONString(inv.getArgument(0)), DeviceDO.class));
                
        when(managerAssembler.doToDto(any(DeviceDO.class)))
            .thenAnswer(inv -> JSON.parseObject(
                JSON.toJSONString(inv.getArgument(0)), DeviceDTO.class));
    }
    
    @Test
    public void testAdd_NewDevice_SavesAndCaches() {
        // Given
        DeviceDTO dto = createDeviceDTO(UniqueKeyFactory.deviceId());
        
        // When
        Long id = deviceManager.add(dto);
        
        // Then
        assertNotNull(id);
        DeviceDTO saved = deviceManager.getById(id);
        assertEquals(dto.getDeviceId(), saved.getDeviceId());
    }
    
    @AfterEach
    public void tearDown() {
        // @Transactional 自动回滚，无需手动清理
    }
}
```

**关键点**：
- 继承 `BaseTest`，自动获得 `@Transactional` 回滚
- 使用 `@MockitoBean` Mock 外部依赖
- Assembler 用 `thenAnswer` + FastJSON 实现双向转换
- 使用 `UniqueKeyFactory` 生成唯一测试数据

#### 4.4.2 异步操作：BaseAsyncTest

```java
@Slf4j
public class MediaSessionManagerAsyncTest extends BaseAsyncTest {
    
    @Autowired
    private MediaSessionManager mediaSessionManager;
    
    @Autowired
    private MediaSessionService mediaSessionService;
    
    private String callId;
    
    @BeforeEach
    public void setUp() {
        callId = UniqueKeyFactory.callId();
    }
    
    @Test
    public void testOnInviteOk_Async_UpdatesStatus() throws Exception {
        // Given
        MediaSessionDTO session = createSession(callId);
        Long id = mediaSessionManager.add(session);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // When：异步方法
        CompletableFuture.runAsync(() -> {
            try {
                mediaSessionManager.onInviteOk(callId);
            } finally {
                latch.countDown();
            }
        });
        
        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        MediaSessionDTO updated = mediaSessionManager.getByCallId(callId);
        assertEquals(MediaSessionConstant.Status.ACTIVE, updated.getStatus());
    }
    
    @AfterEach
    public void tearDown() {
        // BaseAsyncTest 无 @Transactional，必须手动清理
        if (callId != null) {
            LambdaQueryWrapper<MediaSessionDO> qw = new LambdaQueryWrapper<>();
            qw.eq(MediaSessionDO::getCallId, callId);
            mediaSessionService.remove(qw);
        }
    }
}
```

**关键点**：
- 继承 `BaseAsyncTest`，**无** `@Transactional`
- 使用 `CountDownLatch` / `CompletableFuture` 处理异步
- **必须**在 `@AfterEach` 手动清理数据
- 适用场景：`@Async` 方法、并发操作、跨线程数据验证

### 4.5 依赖Mock策略

| 依赖类型　　　　　　| Mock方式　　　　　　　　　　　| 原因　　　　　　　　　　　　|
| ---------------------| -------------------------------| -----------------------------|
| IService<DO>　　　　| `@Autowired`（真实Bean）　　　| Manager测试需验证数据持久化 |
| Assembler　　　　　 | `@MockitoBean` + `thenAnswer` | 避免手写转换逻辑　　　　　　|
| Wrapper（外部集成） | `@MockitoBean`　　　　　　　　| 避免真实SIP/ZLM调用　　　　 |
| Redis　　　　　　　 | TestRedisConfig（Mock）　　　 | 使用本地缓存　　　　　　　　|
| SSE EventBus　　　　| LocalSseEventBus　　　　　　　| 测试配置自动激活　　　　　　|

---

## 5. 集成测试规范

### 5.1 端到端测试（E2E）

E2E 测试验证完整的业务场景，涉及 SIP 协议交互、设备注册、媒体会话等。

#### 5.1.1 BaseE2eTest

```java
@Slf4j
@SpringBootTest(classes = ApplicationWeb.class, 
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({CacheTestConfig.class, TestRedisConfig.class})
public abstract class BaseE2eTest {
    
    @LocalServerPort
    protected int port;
    
    @Autowired
    protected DeviceManager deviceManager;
    
    @Autowired
    protected MediaSessionManager mediaSessionManager;
    
    protected String uniqueDeviceId() {
        return UniqueKeyFactory.deviceId();
    }
}
```

#### 5.1.2 E2E 测试示例

```java
public class DeviceRegistrationE2eTest extends BaseE2eTest {
    
    @Autowired
    private DeviceRegisterService deviceRegisterService;
    
    @Test
    public void testDeviceRegisterLifecycle() throws Exception {
        // Given
        String deviceId = uniqueDeviceId();
        DeviceDTO device = createDevice(deviceId);
        
        // When: 设备注册
        deviceRegisterService.login(createRegisterEvent(deviceId));
        
        // Then: 验证设备在线
        DeviceDTO saved = deviceManager.getByDeviceId(deviceId);
        assertNotNull(saved);
        assertEquals(DeviceConstant.Status.ONLINE, saved.getStatus());
        
        // When: 设备心跳
        deviceRegisterService.keepalive(deviceId);
        
        // Then: 验证心跳时间更新
        DeviceDTO afterKeepalive = deviceManager.getByDeviceId(deviceId);
        assertTrue(afterKeepalive.getLastKeepaliveTime()
            .isAfter(saved.getLastKeepaliveTime()));
        
        // When: 设备离线
        deviceRegisterService.offline(deviceId);
        
        // Then: 验证设备离线
        DeviceDTO afterOffline = deviceManager.getByDeviceId(deviceId);
        assertEquals(DeviceConstant.Status.OFFLINE, afterOffline.getStatus());
    }
}
```

#### 5.1.3 E2E 测试覆盖场景

| 场景类别 | 测试用例 | 验证点 |
|---------|---------|--------|
| **设备生命周期** | DeviceRegistrationE2eTest | 注册→心跳→离线 |
| **媒体会话** | MediaInviteE2eTest | INVITE→ACK→BYE |
| **目录查询** | CatalogQueryE2eTest | 查询→解析→存储 |
| **设备控制** | DeviceControlE2eTest | PTZ控制→状态反馈 |
| **回放** | PlaybackE2eTest | 回放INVITE→时间范围 |
| **语音广播** | VoiceBroadcastE2eTest | 语音下发→状态确认 |

### 5.2 外部服务依赖的集成测试

某些集成测试需要验证与真实外部服务（Redis、PostgreSQL）的交互。

#### 5.2.1 可选执行模式

使用 JUnit 5 `Assumptions` 实现：
- **默认**：`mvn test` — 服务不可用时自动跳过
- **显式**：`mvn test -Pintegration-tests` — 服务不可用时失败

#### 5.2.2 Redis 集成测试

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "sse.type=redis")  // 覆盖默认local
public class RedisBackedSseEventBusIntegrationTest {
    
    @Autowired(required = false)
    private RedisBackedSseEventBus sseEventBus;
    
    @BeforeEach
    void checkRedisAvailable() {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.ping();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, 
                "Redis 不可用，跳过测试: " + e.getMessage());
        }
    }
    
    @Test
    public void testBroadcastAcrossNodes() {
        // 验证 Redis Pub/Sub 事件广播
        CountDownLatch latch = new CountDownLatch(1);
        
        sseEventBus.subscribe("test-topic", message -> {
            assertEquals("test-data", message.get("data"));
            latch.countDown();
        });
        
        sseEventBus.broadcast("test-topic", Map.of("data", "test-data"));
        
        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }
}
```

**关键配置**：
- `@TestPropertySource(properties = "sse.type=redis")` 覆盖默认的 `local`
- 不导入 `TestRedisConfig`，使用真实 Redis 连接
- `@BeforeEach` 检测 Redis 可用性

#### 5.2.3 PostgreSQL 集成测试

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/voglander_test",
    "spring.datasource.username=postgres",
    "spring.datasource.password=postgres"
})
public class PostgreSQLIntegrationTest extends BaseTest {
    
    @Autowired
    private DataSource dataSource;
    
    @BeforeEach
    void checkPostgreSQLAvailable() {
        try (Connection conn = dataSource.getConnection()) {
            assertNotNull(conn);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, 
                "PostgreSQL 不可用，跳过测试: " + e.getMessage());
        }
    }
    
    @Test
    public void testPostgreSQLSpecificFeatures() {
        // 验证 PostgreSQL 特定功能（如 JSONB、数组类型）
    }
}
```

---

## 6. 测试环境隔离

### 6.1 核心隔离配置

Voglander 测试环境通过三个核心配置实现完全隔离：

#### 6.1.1 TestRedisConfig：Mock Redis

**位置**：`voglander-web/src/test/java/io/github/lunasaw/voglander/config/TestRedisConfig.java`

**功能**：
1. 提供 Mock `RedisConnectionFactory` / `RedisTemplate` / `StringRedisTemplate`
2. Mock `RedisMessageListenerContainer`（支持 SSE Pub/Sub）
3. 使 `connection.ping()` 抛出异常，触发 Assumptions 跳过依赖真实 Redis 的测试

**激活方式**：
```java
@SpringBootTest
@Import(TestRedisConfig.class)  // BaseTest 已默认导入
public class MyTest extends BaseTest {
    // Mock Redis 自动生效
}
```

**关键实现**：
```java
@Bean
@Primary
public RedisConnectionFactory redisConnectionFactory() {
    RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
    RedisConnection connection = mock(RedisConnection.class);
    when(factory.getConnection()).thenReturn(connection);
    
    // 关键：使依赖真实 Redis 的测试能通过 Assumptions 探测跳过
    when(connection.ping()).thenThrow(
        new QueryTimeoutException("Mock Redis unavailable"));
    
    return factory;
}
```

#### 6.1.2 CacheTestConfig：动态缓存管理器

**位置**：`voglander-web/src/test/java/io/github/lunasaw/voglander/config/CacheTestConfig.java`

**解决问题**：
- sip-common 提供的 `CacheConfig` 仅支持固定 4 个缓存区 `[devices, subscribes, transactions, sipMessages]`
- Voglander 自有缓存区（`device`、`mediaNode`、`streamProxy`）在测试中 `getCache()` 返回 `null`
- 缓存读写和精确 evict 行为从未被真正验证

**功能**：
1. 提供 `@Primary` 动态 `ConcurrentMapCacheManager`（按需创建任意缓存区）
2. 启用 SQLite WAL 模式（减少并发锁冲突）

**关键实现**：
```java
@Bean
@Primary
public CacheManager cacheManager() {
    // 默认构造 = 动态模式（dynamic=true）
    // 任意 name 首次访问即创建，对齐生产 RedisCacheManager 语义
    return new ConcurrentMapCacheManager();
}

@PostConstruct
public void enableSqliteWal() {
    // SQLite WAL 模式：允许读写并发，减少 SQLITE_BUSY
    stmt.execute("PRAGMA journal_mode=WAL");
}
```

#### 6.1.3 SSE 事件总线隔离

**配置**：`application-test.yml`
```yaml
sse:
  type: local  # 使用 LocalSseEventBus，无需 Redis
```

**效果**：
- `LocalSseEventBus` 自动激活（进程内事件分发）
- `RedisBackedSseEventBus` 被条件排除
- 测试无需真实 Redis 连接

**验证 Redis SSE 特性时**：
```java
@TestPropertySource(properties = "sse.type=redis")
public class RedisBackedSseEventBusIntegrationTest {
    // 显式激活 Redis SSE 实现
}
```

### 6.2 测试隔离检查清单

在编写测试前，确认以下隔离措施：

| 检查项 | 默认测试 | 集成测试 |
|--------|---------|---------|
| ✅ 导入 `TestRedisConfig` | 是（BaseTest已导入）| 否（需真实Redis时）|
| ✅ 导入 `CacheTestConfig` | 是（BaseTest已导入）| 是 |
| ✅ `sse.type=local` | 是 | 否（验证Redis SSE时）|
| ✅ 使用 SQLite | 是 | 否（验证PostgreSQL时）|
| ✅ Mock 外部 Wrapper | 是 | 是 |
| ✅ `@Transactional` 回滚 | 是（BaseTest）| 否（BaseAsyncTest）|
| ✅ `RANDOM_PORT` | 是 | 是 |

### 6.3 外部依赖 Mock 最佳实践

#### 6.3.1 Wrapper 层 Mock

```java
@MockitoBean
private Gb28181Wrapper gb28181Wrapper;

@BeforeEach
public void mockGb28181() {
    // 成功场景
    when(gb28181Wrapper.deviceInfoQuery(anyString()))
        .thenReturn(ResultDTO.success(mockDeviceInfo()));
    
    // 失败场景
    when(gb28181Wrapper.ptzControl(eq("offline-device"), any()))
        .thenReturn(ResultDTO.error("设备离线"));
}
```

#### 6.3.2 Assembler Mock

```java
@MockitoBean
private ManagerAssembler managerAssembler;

@BeforeEach
public void mockAssembler() {
    // 双向转换：使用 FastJSON2 实现
    when(managerAssembler.dtoToDo(any(DeviceDTO.class)))
        .thenAnswer(inv -> JSON.parseObject(
            JSON.toJSONString(inv.getArgument(0)), DeviceDO.class));
            
    when(managerAssembler.doToDto(any(DeviceDO.class)))
        .thenAnswer(inv -> JSON.parseObject(
            JSON.toJSONString(inv.getArgument(0)), DeviceDTO.class));
}
```

**优势**：
- 避免手写转换逻辑
- 保证转换的双向一致性
- 自动处理字段映射

---

## 7. 测试数据管理

### 7.1 唯一标识生成

**工具类**：`UniqueKeyFactory`

**功能**：生成带时间戳和线程ID的唯一标识，避免并发测试数据冲突。

```java
public class UniqueKeyFactory {
    
    /**
     * 生成唯一设备ID
     * 格式: 34020000001320000001_<timestamp>_<threadId>
     */
    public static String deviceId() {
        return String.format("34020000001320000001_%d_%d", 
            System.currentTimeMillis(), 
            Thread.currentThread().getId());
    }
    
    /**
     * 生成唯一通道ID
     */
    public static String channelId() {
        return String.format("34020000001320000001_%d_%d", 
            System.currentTimeMillis(), 
            Thread.currentThread().getId());
    }
    
    /**
     * 生成唯一呼叫ID
     */
    public static String callId() {
        return String.format("call_%d_%d", 
            System.currentTimeMillis(), 
            Thread.currentThread().getId());
    }
    
    /**
     * 生成唯一流代理 app+stream
     */
    public static String streamKey(String prefix) {
        return String.format("%s_%d_%d", 
            prefix,
            System.currentTimeMillis(), 
            Thread.currentThread().getId());
    }
}
```

### 7.2 测试数据清理策略

#### 7.2.1 BaseTest：自动回滚

```java
public class DeviceManagerTest extends BaseTest {
    
    @Test
    public void testAddDevice() {
        String deviceId = UniqueKeyFactory.deviceId();
        DeviceDTO dto = createDevice(deviceId);
        
        Long id = deviceManager.add(dto);
        
        assertNotNull(id);
        // @Transactional 自动回滚，无需手动清理
    }
}
```

#### 7.2.2 BaseAsyncTest：手动清理

```java
public class MediaSessionManagerAsyncTest extends BaseAsyncTest {
    
    private List<String> callIdsToClean = new ArrayList<>();
    
    @Test
    public void testAsyncOperation() {
        String callId = UniqueKeyFactory.callId();
        callIdsToClean.add(callId);
        
        // 测试逻辑
    }
    
    @AfterEach
    public void tearDown() {
        for (String callId : callIdsToClean) {
            LambdaQueryWrapper<MediaSessionDO> qw = new LambdaQueryWrapper<>();
            qw.eq(MediaSessionDO::getCallId, callId);
            mediaSessionService.remove(qw);
        }
        callIdsToClean.clear();
    }
}
```

### 7.3 数据隔离最佳实践

| 场景 | 隔离方式 | 示例 |
|-----|---------|------|
| 单测试方法内 | 使用唯一标识 | `UniqueKeyFactory.deviceId()` |
| 并发测试 | 时间戳+线程ID | `device_${timestamp}_${threadId}` |
| 多设备场景 | 循环生成唯一ID | `for (int i=0; i<3; i++) deviceId()` |
| 主外键关联 | 一次生成，共享使用 | `deviceId` → 多个 `channelId` |

---

## 8. 测试执行策略

### 8.1 测试命令矩阵

| 命令 | 说明 | 适用场景 |
|-----|------|---------|
| `mvn test` | 执行所有测试，外部服务不可用时自动跳过 | 本地开发、CI快速验证 |
| `mvn test -Pintegration-tests` | 强制执行集成测试，外部服务不可用时失败 | CI完整验证、发布前检查 |
| `mvn test -Dtest=DeviceManagerTest` | 执行单个测试类 | 单模块开发 |
| `mvn test -Dtest=DeviceManagerTest#testAdd` | 执行单个测试方法 | 调试特定问题 |
| `mvn test -DforkCount=2` | 并行执行测试（2个进程）| 加速测试执行 |
| `./generate-coverage-report.sh` | 生成JaCoCo覆盖率报告 | 覆盖率分析 |

### 8.2 测试分组策略

#### 8.2.1 Maven Profile 配置

**pom.xml**：
```xml
<profiles>
    <!-- 默认profile：快速测试，跳过需要外部服务的集成测试 -->
    <profile>
        <id>fast-tests</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <skipIntegrationTests>false</skipIntegrationTests>
        </properties>
    </profile>
    
    <!-- 集成测试profile：强制执行所有集成测试 -->
    <profile>
        <id>integration-tests</id>
        <properties>
            <skipIntegrationTests>false</skipIntegrationTests>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <systemPropertyVariables>
                            <integration.tests.enabled>true</integration.tests.enabled>
                        </systemPropertyVariables>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

#### 8.2.2 测试标签（JUnit 5 Tags）

使用 `@Tag` 标记不同类型的测试：

```java
@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class DeviceServiceTest {
    // 纯单元测试
}

@Tag("integration")
public class DeviceManagerTest extends BaseTest {
    // 集成测试
}

@Tag("e2e")
public class DeviceRegistrationE2eTest extends BaseE2eTest {
    // 端到端测试
}

@Tag("requires-redis")
@TestPropertySource(properties = "sse.type=redis")
public class RedisBackedSseEventBusIntegrationTest {
    // 需要真实Redis的测试
}
```

**执行指定标签的测试**：
```bash
# 仅执行单元测试
mvn test -Dgroups="unit"

# 执行集成测试和E2E测试
mvn test -Dgroups="integration | e2e"

# 排除需要外部服务的测试
mvn test -DexcludedGroups="requires-redis,requires-postgresql"
```

### 8.3 并发测试执行

#### 8.3.1 端口隔离

所有 `@SpringBootTest` 使用 `RANDOM_PORT`：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseTest {
    @LocalServerPort
    protected int port;
}
```

**效果**：
- 每个测试类获得独立端口
- 支持并行执行：`mvn test -DforkCount=2`
- 避免端口冲突导致的测试失败

#### 8.3.2 数据隔离

使用 `UniqueKeyFactory` 生成唯一标识：

```java
@Test
public void testConcurrent1() {
    String deviceId = UniqueKeyFactory.deviceId();  // device_${timestamp}_${threadId}
    // 即使并行执行，deviceId 也不会冲突
}

@Test
public void testConcurrent2() {
    String deviceId = UniqueKeyFactory.deviceId();  // 不同的 deviceId
    // 数据完全隔离
}
```

#### 8.3.3 并发测试最佳实践

```bash
# 本地开发：串行执行（便于调试）
mvn test

# CI环境：并行执行（加速构建）
mvn test -DforkCount=4 -DreuseForks=false

# 覆盖率生成：串行执行（确保准确性）
mvn clean test verify -DforkCount=1
```

### 8.4 CI/CD 集成

#### 8.4.1 GitHub Actions 示例

```yaml
name: Test

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Run Unit Tests
        run: mvn test -Dgroups="unit"
      
  integration-tests:
    runs-on: ubuntu-latest
    services:
      redis:
        image: redis:7
        ports:
          - 6379:6379
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: voglander_test
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Run Integration Tests
        run: mvn test -Pintegration-tests
      
      - name: Generate Coverage Report
        run: ./generate-coverage-report.sh
      
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./voglander-coverage-report/target/site/jacoco-aggregate/jacoco.xml
```

#### 8.4.2 测试阶段划分

```
┌─────────────────────────────────────────────────────────┐
│ 阶段1：快速反馈（2-3分钟）                                │
│ - 单元测试（unit tag）                                   │
│ - 不依赖外部服务的集成测试                                │
├─────────────────────────────────────────────────────────┤
│ 阶段2：完整验证（5-10分钟）                               │
│ - 所有集成测试（integration tag）                        │
│ - E2E测试（e2e tag）                                     │
│ - 需要外部服务的测试（requires-* tags）                   │
├─────────────────────────────────────────────────────────┤
│ 阶段3：覆盖率分析（额外3-5分钟）                           │
│ - JaCoCo 聚合报告生成                                    │
│ - 覆盖率报告上传                                         │
└─────────────────────────────────────────────────────────┘
```

### 8.5 故障排查指南

#### 8.5.1 常见问题与解决方案

| 问题 | 原因 | 解决方案 |
|-----|------|---------|
| `NoUniqueBeanDefinitionException` | 多个 CacheManager bean | 确保导入 `CacheTestConfig`（提供@Primary bean）|
| `Redis connection refused` | 测试依赖真实Redis但未启动 | 1. 启动Redis；2. 或使用Mock（导入TestRedisConfig）|
| `SQLITE_BUSY` 锁冲突 | 并发测试写入SQLite | CacheTestConfig已启用WAL模式，检查是否导入 |
| 测试事务未回滚 | 使用了BaseAsyncTest但期望回滚 | 改用BaseTest，或在@AfterEach手动清理 |
| 端口冲突 | 固定端口或未使用RANDOM_PORT | 确保继承BaseTest/BaseE2eTest（已配置RANDOM_PORT）|
| `SSE event not received` | 使用了Mock RedisTemplate但期望真实SSE | 不导入TestRedisConfig，使用真实Redis |
| 缓存未生效 | sip-common的固定缓存区列表 | 确保导入CacheTestConfig（动态缓存管理器）|

#### 8.5.2 调试技巧

**1. 启用详细日志**：
```yaml
# application-test.yml
logging:
  level:
    io.github.lunasaw.voglander: DEBUG
    io.github.lunasaw.sip: DEBUG
    org.springframework.cache: TRACE
```

**2. 单独执行失败的测试**：
```bash
mvn test -Dtest=DeviceManagerTest#testAdd -X
```

**3. 检查测试配置加载**：
```java
@SpringBootTest
@Import({CacheTestConfig.class, TestRedisConfig.class})
public class ConfigBootSmokeTest {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    
    @Test
    public void verifyTestConfigLoaded() {
        // 验证是动态CacheManager
        assertTrue(cacheManager instanceof ConcurrentMapCacheManager);
        
        // 验证是Mock RedisConnectionFactory
        assertTrue(Mockito.mockingDetails(redisConnectionFactory).isMock());
    }
}
```

**4. 验证数据隔离**：
```java
@Test
public void verifyUniqueKeys() {
    String id1 = UniqueKeyFactory.deviceId();
    String id2 = UniqueKeyFactory.deviceId();
    
    assertNotEquals(id1, id2);
    assertTrue(id1.contains("_" + Thread.currentThread().getId()));
}
```

---

## 9. 测试覆盖率目标

### 9.1 覆盖率要求

| 层级 | 行覆盖率 | 分支覆盖率 | 说明 |
|-----|---------|-----------|------|
| **Controller** | ≥ 80% | ≥ 70% | 主要路径覆盖 |
| **Service** | ≥ 90% | ≥ 80% | 业务逻辑核心 |
| **Manager** | ≥ 85% | ≥ 75% | 复杂编排逻辑 |
| **Wrapper** | ≥ 70% | ≥ 60% | 外部调用包装 |
| **Protocol Handler** | ≥ 95% | ≥ 90% | 协议路由关键 |
| **Repository** | ≥ 80% | N/A | 数据访问层 |

### 9.2 覆盖率验证

```bash
# 生成聚合覆盖率报告
./generate-coverage-report.sh

# 报告位置
open voglander-coverage-report/target/site/jacoco-aggregate/index.html
```

### 9.3 覆盖率排除项

某些代码无需测试覆盖：

```java
// 1. 纯数据类（DO/DTO/VO）
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDTO {
    // Lombok生成的getter/setter无需测试
}

// 2. 配置类
@Configuration
public class RedisConfig {
    // Spring配置无需单独测试
}

// 3. 应用入口
@SpringBootApplication
public class ApplicationWeb {
    public static void main(String[] args) {
        // main方法无需测试
    }
}
```

**JaCoCo 排除配置**：
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/*DO.class</exclude>
            <exclude>**/*DTO.class</exclude>
            <exclude>**/*VO.class</exclude>
            <exclude>**/*Config.class</exclude>
            <exclude>**/Application*.class</exclude>
        </excludes>
    </configuration>
</plugin>
```

---

## 10. 测试最佳实践总结

### 10.1 必须遵守的规则（强制）

1. ✅ **协议层测试必须纯单元**：禁止使用 `@SpringBootTest`
2. ✅ **Controller测试必须纯单元**：禁止使用 `@SpringBootTest` / `@WebMvcTest`
3. ✅ **Manager测试必须集成**：使用 `BaseTest` 或 `BaseAsyncTest`
4. ✅ **默认测试必须内部自洽**：不依赖外部Redis/PostgreSQL
5. ✅ **异步测试必须手动清理**：使用 `BaseAsyncTest` + `@AfterEach`
6. ✅ **必须使用唯一标识**：通过 `UniqueKeyFactory` 避免数据冲突
7. ✅ **必须使用RANDOM_PORT**：避免并发测试端口冲突

### 10.2 推荐的实践（建议）

1. 📌 **优先编写单元测试**：快速、稳定、易维护
2. 📌 **集成测试验证协作**：跨层交互、事务行为、缓存逻辑
3. 📌 **E2E测试覆盖关键场景**：设备生命周期、媒体会话、协议交互
4. 📌 **使用Assumptions实现可选测试**：外部服务不可用时优雅跳过
5. 📌 **Mock外部依赖**：Wrapper、Assembler使用 `@MockitoBean`
6. 📌 **使用TestPropertySource覆盖配置**：测试特定场景时动态调整

### 10.3 应该避免的反模式（禁止）

1. ❌ **不要在单元测试中启动Spring容器**：造成测试缓慢
2. ❌ **不要在测试中硬编码ID**：导致并发冲突和数据污染
3. ❌ **不要依赖测试执行顺序**：JUnit不保证顺序
4. ❌ **不要在BaseTest中使用异步操作**：事务回滚对异步线程无效
5. ❌ **不要在测试中使用 `Thread.sleep()`**：使用 `CountDownLatch` / `Awaitility`
6. ❌ **不要跳过测试清理**：导致数据污染影响后续测试
7. ❌ **不要在测试中使用生产配置**：使用 `@ActiveProfiles("test")`

---

## 11. 测试模板快速参考

### 11.1 协议层单元测试模板

```java
@Slf4j
@ExtendWith(MockitoExtension.class)
public class XxxProtocolHandlerTest {
    
    @Mock
    private ServiceA serviceA;
    
    @Mock
    private ServiceB serviceB;
    
    @InjectMocks
    private XxxProtocolHandler handler;
    
    @Test
    public void testEventRouting() {
        // Given
        Event event = createEvent("group", "name", "deviceId");
        
        // When
        handler.handle(event);
        
        // Then
        verify(serviceA, times(1)).method(any());
    }
}
```

### 11.2 Controller单元测试模板

```java
@Slf4j
@ExtendWith(MockitoExtension.class)
public class XxxControllerTest {
    
    @Mock
    private XxxManager xxxManager;
    
    @Mock
    private WebAssembler webAssembler;
    
    @InjectMocks
    private XxxController controller;
    
    @Test
    public void testAdd() {
        // Given
        XxxCreateReq req = createRequest();
        XxxDTO dto = new XxxDTO();
        when(webAssembler.createReqToDto(req)).thenReturn(dto);
        when(xxxManager.add(dto)).thenReturn(1L);
        
        // When
        AjaxResult<Long> result = controller.add(req);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData());
    }
}
```

### 11.3 Manager集成测试模板（同步）

```java
@Slf4j
public class XxxManagerTest extends BaseTest {
    
    @Autowired
    private XxxManager xxxManager;
    
    @MockitoBean
    private ExternalWrapper externalWrapper;
    
    @MockitoBean
    private ManagerAssembler managerAssembler;
    
    @BeforeEach
    public void setUp() {
        // Mock Assembler 双向转换
        when(managerAssembler.dtoToDo(any(XxxDTO.class)))
            .thenAnswer(inv -> JSON.parseObject(
                JSON.toJSONString(inv.getArgument(0)), XxxDO.class));
    }
    
    @Test
    public void testAdd() {
        // Given
        String uniqueKey = UniqueKeyFactory.deviceId();
        XxxDTO dto = createDTO(uniqueKey);
        
        // When
        Long id = xxxManager.add(dto);
        
        // Then
        assertNotNull(id);
        XxxDTO saved = xxxManager.getById(id);
        assertEquals(uniqueKey, saved.getKey());
    }
    
    // @Transactional 自动回滚，无需 @AfterEach
}
```

### 11.4 Manager集成测试模板（异步）

```java
@Slf4j
public class XxxManagerAsyncTest extends BaseAsyncTest {
    
    @Autowired
    private XxxManager xxxManager;
    
    @Autowired
    private XxxService xxxService;
    
    private List<String> keysToClean = new ArrayList<>();
    
    @Test
    public void testAsyncOperation() throws Exception {
        // Given
        String uniqueKey = UniqueKeyFactory.deviceId();
        keysToClean.add(uniqueKey);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // When
        CompletableFuture.runAsync(() -> {
            try {
                xxxManager.asyncMethod(uniqueKey);
            } finally {
                latch.countDown();
            }
        });
        
        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        XxxDTO result = xxxManager.getByKey(uniqueKey);
        assertNotNull(result);
    }
    
    @AfterEach
    public void tearDown() {
        // 必须手动清理
        for (String key : keysToClean) {
            LambdaQueryWrapper<XxxDO> qw = new LambdaQueryWrapper<>();
            qw.eq(XxxDO::getKey, key);
            xxxService.remove(qw);
        }
        keysToClean.clear();
    }
}
```

### 11.5 E2E测试模板

```java
@Slf4j
public class XxxE2eTest extends BaseE2eTest {
    
    @Autowired
    private XxxManager xxxManager;
    
    @Test
    public void testCompleteScenario() throws Exception {
        // Given
        String deviceId = uniqueDeviceId();
        
        // When: 步骤1 - 注册
        xxxManager.register(deviceId);
        
        // Then: 验证注册成功
        XxxDTO registered = xxxManager.getByDeviceId(deviceId);
        assertNotNull(registered);
        assertEquals(Status.REGISTERED, registered.getStatus());
        
        // When: 步骤2 - 激活
        xxxManager.activate(deviceId);
        
        // Then: 验证激活成功
        XxxDTO activated = xxxManager.getByDeviceId(deviceId);
        assertEquals(Status.ACTIVE, activated.getStatus());
    }
}
```

---

## 12. 附录

### 12.1 相关文档

- `TEST_EXECUTION_GUIDE.md` — 测试执行命令详解
- `MIGRATION_GUIDE.md` — 从BaseTest迁移到BaseAsyncTest指南
- `SERVICE_SETUP.md` — Redis/PostgreSQL本地环境搭建
- `TROUBLESHOOTING.md` — 测试问题排查手册

### 12.2 关键类路径

| 类 | 路径 |
|---|------|
| BaseTest | `voglander-web/src/test/.../BaseTest.java` |
| BaseAsyncTest | `voglander-web/src/test/.../BaseAsyncTest.java` |
| BaseE2eTest | `voglander-web/src/test/.../e2e/BaseE2eTest.java` |
| TestRedisConfig | `voglander-web/src/test/.../config/TestRedisConfig.java` |
| CacheTestConfig | `voglander-web/src/test/.../config/CacheTestConfig.java` |
| UniqueKeyFactory | `voglander-test/src/main/.../support/UniqueKeyFactory.java` |

### 12.3 测试规范版本历史

| 版本 | 日期 | 变更说明 |
|-----|------|---------|
| 1.0.10 | 2026-07-14 | 初始版本：分层测试规范、环境隔离、最佳实践 |

---

**文档维护者**：Voglander开发团队  
**最后更新**：2026-07-14  
**适用版本**：Voglander 1.0.10+

---

## 13. 外部依赖测试过滤机制

### 13.1 核心原则

Voglander 遵循"**默认测试内部自洽**"原则：

```
mvn test（默认）
   ↓
自动跳过需要外部服务的测试（Assumptions）
   ↓
2-3 分钟快速反馈

mvn test -Pintegration-tests（显式）
   ↓
强制执行所有外部依赖测试
   ↓
服务不可用时失败
```

### 13.2 实现方式：JUnit 5 Assumptions

推荐使用 **JUnit 5 Assumptions** 实现运行时服务探测：

#### Redis 依赖测试示例

```java
@Tag("requires-redis")
@SpringBootTest
@TestPropertySource(properties = "sse.type=redis")
public class RedisBackedSseEventBusIntegrationTest {
    
    @Autowired(required = false)
    private RedisBackedSseEventBus sseEventBus;
    
    @BeforeEach
    void checkRedisAvailable() {
        boolean integrationMode = Boolean.getBoolean("integration.tests.enabled");
        
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.ping();
        } catch (Exception e) {
            if (integrationMode) {
                // 集成测试模式：必须可用
                fail("集成测试模式要求 Redis 可用: " + e.getMessage());
            } else {
                // 默认模式：优雅跳过
                Assumptions.assumeTrue(false, 
                    "Redis 不可用，跳过测试: " + e.getMessage());
            }
        }
    }
    
    @Test
    public void testRedisPubSub() {
        // 仅在 Redis 可用时执行
    }
}
```

#### PostgreSQL 依赖测试示例

```java
@Tag("requires-postgresql")
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/voglander_test"
})
public class PostgreSQLIntegrationTest extends BaseTest {
    
    @Autowired
    private DataSource dataSource;
    
    @BeforeEach
    void checkPostgreSQLAvailable() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            Assumptions.assumeTrue("PostgreSQL".equalsIgnoreCase(
                meta.getDatabaseProductName()), "当前数据库不是 PostgreSQL");
        } catch (Exception e) {
            Assumptions.assumeTrue(false, 
                "PostgreSQL 不可用: " + e.getMessage());
        }
    }
}
```

### 13.3 Maven Profile 配置

```xml
<profiles>
    <profile>
        <id>integration-tests</id>
        <properties>
            <integration.tests.enabled>true</integration.tests.enabled>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <systemPropertyVariables>
                            <integration.tests.enabled>true</integration.tests.enabled>
                        </systemPropertyVariables>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### 13.4 测试报告差异

**默认模式**：
```
[INFO] Tests run: 150, Failures: 0, Errors: 0, Skipped: 5
[INFO] Skipped:
[INFO]   RedisBackedSseEventBusIntegrationTest.testRedisPubSub
[INFO]     Reason: Redis 不可用，跳过测试: Connection refused
```

**集成测试模式**（服务不可用）：
```
[ERROR] Tests run: 155, Failures: 5, Errors: 0, Skipped: 0
[ERROR] Failures:
[ERROR]   RedisBackedSseEventBusIntegrationTest.testRedisPubSub
[ERROR]     Cause: 集成测试模式要求 Redis 可用，但连接失败
```

### 13.5 外部依赖测试检查清单

编写外部依赖测试时：

- [ ] 添加 `@Tag("requires-xxx")` 标记
- [ ] 在 `@BeforeEach` 实现服务可用性检测
- [ ] 使用 `Assumptions.assumeTrue()` + 清晰的跳过原因
- [ ] 不导入 `TestRedisConfig`（需要真实连接）
- [ ] 使用 `@TestPropertySource` 覆盖配置

验证：

- [ ] `mvn test` 自动跳过（无外部服务）
- [ ] `mvn test -Pintegration-tests` 必须通过（有外部服务）
- [ ] 测试报告中显示跳过原因

**详细文档**：参见 [EXTERNAL_DEPENDENCY_TESTING.md](./EXTERNAL_DEPENDENCY_TESTING.md)

