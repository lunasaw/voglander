# 端到端测试方案编写模板

> 基于 GB28181 注册链路 E2E 实施经验总结（voglander 1.0.4）

---

## 1. 测试分层决策

| 层次 | 适用场景 | 框架 | 隔离方式 |
|------|----------|------|----------|
| 纯单元测试 | 单类逻辑、无 Spring 容器 | `@ExtendWith(MockitoExtension.class)` | 无 DB |
| Manager 集成测试 | 跨层写库、事务回滚验证 | `extends BaseTest` + `@Transactional` | 自动回滚 |
| 事件边界 E2E | 异步事件链路、不启 SIP 协议栈 | `@SpringBootTest` + `@MockitoSpyBean` | `@AfterEach` 手动 DELETE |
| 真实协议 E2E | 完整 SIP/TCP/UDP 协议交互 | `@SpringBootTest` + 真实端口 | `@AfterEach` 手动 DELETE |

**选择原则**：能用低层次覆盖的不用高层次；异步写库链路不加 `@Transactional`。

---

## 2. 项目基础设施

### 2.1 `BaseTest`（集成测试基类）

```java
@SpringBootTest(classes = ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(CacheTestConfig.class)
@Transactional  // Manager 集成测试自动回滚
@TestPropertySource(properties = {
    "sip.enable=true",
    "sip.server.ip=127.0.0.1",
    "sip.server.port=5060",
    "sip.server.serverId=34020000002000000001",
    "sip.client.enabled=true",
    "sip.client.clientId=34020000001320000001",
    "sip.client.domain=127.0.0.1",
    "sip.client.port=5061"
})
public abstract class BaseTest { }
```

### 2.2 测试数据库

- SQLite 文件：`test-app.db`，schema 来自 `schema-sqlite.sql`
- 不支持并发写：多个 E2E 测试类**不能同时运行**，需分批执行

### 2.3 `UniqueKeyFactory`（避免测试数据冲突）

```java
// 生成隔离的设备/通道 ID，防止并行测试数据互相干扰
String deviceId  = UniqueKeyFactory.deviceId();
String channelId = UniqueKeyFactory.channelId();
```

---

## 3. 各层测试模板

### 3.1 纯单元测试（Mockito）

```java
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {

    @Mock  YyyDependency dep;
    @InjectMocks XxxService sut;

    @Test
    void doSomething_givenInput_expectedBehavior() {
        // Arrange
        when(dep.query("id")).thenReturn(expected);
        // Act
        Result r = sut.doSomething("id");
        // Assert
        assertThat(r).isEqualTo(expected);
        verify(dep).query("id");
    }
}
```

**关键点**：
- 不启动 Spring 容器，执行速度 < 100ms
- 用 `ArgumentCaptor` 验证传入参数字段

---

### 3.2 Manager 集成测试

```java
@DisplayName("XxxManager 集成测试")
@ExtendWith(RedisAvailableExtension.class)   // 可选：Redis 不可用时跳过
class XxxManagerTest extends BaseTest {

    @Autowired XxxManager manager;
    @Autowired XxxMapper  mapper;

    private String lastId;   // 用于 @AfterEach 补充清理（@Transactional 回滚已覆盖大部分情况）

    @Test
    void upsert_shouldPersistWithCorrectFields() {
        String id = UniqueKeyFactory.deviceId();
        // Act
        manager.upsert(buildDto(id));
        // Assert — 直接查库，事务未提交但同一连接可见
        XxxDO saved = mapper.selectOne(Wrappers.<XxxDO>lambdaQuery().eq(XxxDO::getDeviceId, id));
        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    private XxxDTO buildDto(String id) { /* ... */ }
}
```

**关键点**：
- 继承 `BaseTest`，`@Transactional` 自动回滚，无需手动清理
- 测试结束后数据库还原，不影响其他测试

---

### 3.3 事件边界 E2E（推荐替代真实协议 E2E）

```java
@Slf4j
@SpringBootTest(classes = ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
// 不加 @Transactional：@Async 线程写库在独立事务
class XxxEventE2eTest {

    private static final String DEVICE_ID = "34020000001320000099";

    @Autowired VoglanderBusinessNotifier notifier;
    @Autowired XxxMapper mapper;

    /** Spy 拦截 ShardDispatcher，捕获事件同时保留真实逻辑 */
    @MockitoSpyBean ShardDispatcher shardSpy;

    @AfterEach
    void cleanup() {
        mapper.delete(Wrappers.<XxxDO>lambdaQuery().eq(XxxDO::getDeviceId, DEVICE_ID));
    }

    private CopyOnWriteArrayList<DeviceEvent> armLatch(CountDownLatch latch) {
        CopyOnWriteArrayList<DeviceEvent> captured = new CopyOnWriteArrayList<>();
        doAnswer(inv -> {
            captured.add(inv.getArgument(0));
            latch.countDown();
            inv.callRealMethod();
            return null;
        }).when(shardSpy).dispatch(any(DeviceEvent.class));
        return captured;
    }

    @Test
    @DisplayName("Register 事件 → 异步分片 → handler → DB 在线")
    void register_event_persists_online_record() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        var captured = armLatch(latch);

        // 直接注入 GatewayEvent，绕过真实 SIP 协议栈
        notifier.notify(new GatewayEvent("gb28181.Lifecycle.Register", DEVICE_ID, null,
            System.currentTimeMillis(),
            Map.of("remoteIp", "127.0.0.1", "remotePort", 5061, "expire", 3600,
                   "localIp", "127.0.0.1", "transport", "UDP"), "node-local"));

        assertThat(latch.await(5, SECONDS)).as("ShardDispatcher 超时").isTrue();

        await().atMost(3, SECONDS).untilAsserted(() -> {
            XxxDO d = mapper.selectOne(...);
            assertThat(d).isNotNull();
            assertThat(d.getStatus()).isEqualTo(1);
        });
    }
}
```

**关键点**：
- 不需要真实 SIP 端口，CI 无端口冲突风险
- `@MockitoSpyBean` + `CountDownLatch` 精确等待异步事件
- `Awaitility.await()` 轮询 DB，处理异步写入时序

---

### 3.4 真实协议 E2E（SIP / TCP / HTTP）

```java
@Slf4j
@SpringBootTest(classes = ApplicationWeb.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "sip.enable=true",                               // 启动 ServerStart CommandLineRunner
    "sip.server.ip=127.0.0.1",
    "sip.server.port=5060",
    "sip.server.serverId=34020000002000000001",
    "sip.server.password=12345678",
    "sip.client.enabled=true",
    "sip.client.clientId=34020000001320000099",      // 区别于 BaseTest 默认 clientId
    "sip.client.domain=127.0.0.1",
    "sip.client.port=5061"
})
class SipXxxE2eTest {

    private static final String CLIENT_ID = "34020000001320000099";
    private static final String SERVER_ID = "34020000002000000001";
    private static final String PASSWORD  = "12345678";

    @Autowired XxxMapper mapper;

    @AfterEach
    void cleanup() {
        mapper.delete(Wrappers.<XxxDO>lambdaQuery().eq(XxxDO::getDeviceId, CLIENT_ID));
    }

    // 直接构造 SIP 实体，不通过 ClientDeviceSupplier（避免多 @Primary Bean 冲突）
    private FromDevice from() {
        return FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061);
    }

    private ToDevice to() {
        ToDevice to = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        to.setPassword(PASSWORD);
        return to;
    }

    @Test
    @DisplayName("TC-SIP-01 REGISTER → 401 → Digest → 200 OK → DB 在线")
    void fullRegister_persistsOnlineDevice() {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            XxxDO d = mapper.selectOne(...);
            assertThat(d).isNotNull();
            assertThat(d.getStatus()).isEqualTo(1);
        });
        log.info("✅ TC-SIP-01 通过");
    }
}
```

**关键点**：
- 必须设 `sip.enable=true` 触发 `ServerStart.run()` 注册监听点
- `ClientId` 用独立值（如 `...0099`），防止与其他测试的 DB 记录冲突
- 不注入 `ClientDeviceSupplier` Bean，直接 `new FromDevice/ToDevice` 规避多 `@Primary` 冲突
- 不加 `@Transactional`，用 `@AfterEach` 手动 DELETE

---

## 4. 常见陷阱与解法

### 陷阱 1：`@TestConfiguration` 被其他测试 Context 污染

**现象**：嵌套 `@TestConfiguration` 里定义 `@Primary` Bean，导致非关联测试类 Context 加载失败（多个 Primary Bean 冲突）。

**解法**：不用嵌套 `@TestConfiguration`，直接在测试方法中 `new` 出所需对象，或用 `@Profile` 隔离。

---

### 陷阱 2：`ListeningPoint Not Exist`

**现象**：`ClientCommandSender.sendRegisterCommand()` 抛 `RuntimeException: ListeningPoint Not Exist`。

**根因**：`ServerStart`（`@ConditionalOnProperty("sip.enable")`）未激活，SIP 监听点未注册。

**解法**：`@TestPropertySource` 补充 `sip.enable=true`。

---

### 陷阱 3：`SQLITE_BUSY` / `Transaction exists`

**现象**：多个 E2E 测试类在同一 JVM 顺序执行时，SQLite 文件锁冲突。

**根因**：SQLite 不支持并发写，两个 Spring Context 共用同一 `test-app.db`。

**解法**：E2E 测试类单独运行（`-Dtest=SipXxxE2eTest`），不与其他集成测试混跑。

---

### 陷阱 4：`@Transactional` 覆盖不到异步线程

**现象**：`@Async` 线程写入的数据，在测试线程的事务中查不到。

**根因**：Spring 事务与线程绑定，异步线程开启独立事务。

**解法**：E2E 测试类不加 `@Transactional`，用 `Awaitility` 轮询 + `@AfterEach` 手动 DELETE。

---

### 陷阱 5：IDEA 编译报 `toList() undefined`

**现象**：`Stream.toList()` 在 IDEA 中报编译错误，但 `mvn test` 通过。

**根因**：IDEA 项目 SDK 设置低于 Java 16（`Stream.toList()` 是 Java 16 新增）。

**解法**：`File → Project Structure → Project SDK` 设为 17，Language Level 同步设为 17。

---

## 5. 执行命令参考

```bash
# 纯单元测试（无需 Spring，秒级）
mvn test -pl voglander-web -Dtest="XxxUnitTest"

# Manager 集成测试
mvn test -pl voglander-web -Dtest="XxxManagerTest"

# 事件边界 E2E
mvn test -pl voglander-web -Dtest="XxxEventE2eTest"

# 真实协议 E2E（单独运行，避免 SQLite 锁冲突）
mvn test -pl voglander-web -Dtest="SipXxxE2eTest"
```

---

## 6. 测试隔离矩阵

| 用例类型 | 加 `@Transactional` | 清理方式 | 可与其他测试并行 |
|----------|---------------------|----------|-----------------|
| 纯单元（Mockito） | 无 DB | N/A | ✅ |
| Manager 集成 | ✅ | 自动回滚 | ✅ |
| 事件边界 E2E | ❌ | `@AfterEach` DELETE | ⚠️ 同 DB 需串行 |
| 真实协议 E2E | ❌ | `@AfterEach` DELETE | ❌ 单独运行 |
