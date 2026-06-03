# GB28181 端到端注册链路测试方案

> 覆盖范围：Client 发起注册 → SIP 协议交互 → BusinessNotifier 回调 → Voglander DB 入库

---

## 1. 测试目标

| 层次 | 验证点 |
|------|--------|
| SIP 协议层 | Client REGISTER → 401 Challenge → 携带 Digest 重注册 → 200 OK |
| 网关回调层 | `VoglanderBusinessNotifier.notify()` → 翻译为 `DeviceEvent` → `ShardDispatcher.dispatch()` |
| 事件路由层 | `ShardDispatcher` → 分片槽单线程 → `InboundEventDispatcher` → `Gb28181ProtocolHandler.handle()` |
| 业务逻辑层 | `handleRegister()` → `DeviceRegisterService.login()` → `tb_device` 入库（状态=在线） |
| 通道层（可选）| 目录查询 → `DeviceChannelManager.batchUpsertWithStatus()` → `tb_device_channel` |

---

## 2. 测试架构

```
┌─────────────────────────────────────────────────────────────────────┐
│  同一 JVM / 同一 Spring Context                                      │
│                                                                     │
│  ┌──────────────────────────────┐                                   │
│  │  SIP Client (5061)           │                                   │
│  │  ClientCommandSender         │                                   │
│  │   .sendRegisterCommand()     │                                   │
│  │  TestClientDeviceSupplier    │──── REGISTER ──────────────────▶ │
│  └──────────────────────────────┘                                   │
│                                        ┌──────────────────────────┐ │
│  ◀──── 401 Unauthorized ───────────── │  SIP Server (5060)       │ │
│  ──── REGISTER+Digest ──────────────▶ │  @EnableSipServer        │ │
│  ◀──── 200 OK ─────────────────────── │  SipBootstrap            │ │
│                                        └──────────┬───────────────┘ │
│                                                   │ BusinessNotifier │
│                                                   ▼  @Async         │
│                                        ┌──────────────────────────┐ │
│                                        │ VoglanderBusinessNotifier │ │
│                                        │  GatewayEvent             │ │
│                                        │   → DeviceEvent           │ │
│                                        │  ShardDispatcher.dispatch │ │
│                                        └──────────┬───────────────┘ │
│                                                   │ 分片槽单线程    │
│                                                   ▼                 │
│                                        ┌──────────────────────────┐ │
│                                        │ Gb28181ProtocolHandler   │ │
│                                        │  handleRegister()        │ │
│                                        │  → DeviceRegisterService │ │
│                                        │    .login(req)           │ │
│                                        └──────────┬───────────────┘ │
│                                                   ▼                 │
│                                        ┌──────────────────────────┐ │
│                                        │  SQLite (test-app.db)    │ │
│                                        │  tb_device               │ │
│                                        │  tb_device_channel       │ │
│                                        └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

**关键决策：单进程双角色**  
Server(5060) + Client(5061) 跑在同一 Spring Context。SIP 栈通过 `SipBootstrap` 在 `@PostConstruct` 中按配置分别绑定两个监听点。  
`CountDownLatch` / `Awaitility` 监听异步回调完成，**不用 `@Transactional`**。

---

## 3. 依赖与配置

### 3.1 测试模块依赖

在 `voglander-web/pom.xml` 的 `<dependencies>` 中补充：

```xml
<!-- SIP 客户端（E2E 测试用） -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-client</artifactId>
    <scope>test</scope>
</dependency>
<!-- GB28181 测试工具（TestClientDeviceSupplier / SipBootstrap 等） -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 3.2 测试配置

E2E 测试**直接复用 `BaseTest` 的 `@TestPropertySource`**，无需额外 yml：

```
sip.server.ip=127.0.0.1        sip.server.port=5060
sip.server.serverId=34020000002000000001
sip.server.password=<见 application-test.yml>

sip.client.clientId=34020000001320000001
sip.client.domain=127.0.0.1    sip.client.port=5061
sip.client.enabled=true

voglander.event.shard.enabled=true（matchIfMissing=true，无需配置）
```

> **注**：`TestClientDeviceSupplier` 读取的是 `${sip.client.clientId}` / `${sip.client.domain}` / `${sip.client.port}`，与 BaseTest 属性 key 已对齐。

---

## 4. 测试夹具（Fixture）

### 4.1 `E2eTestClientConfig`——激活 SIP Client 双栈

从 `sip-proxy` 的 `gb28181-test` 模块可以看到，SIP 双栈（Server + Client）的激活路径是：

1. `@EnableSipServer`（已在 `ApplicationWeb` 上声明）
2. `SipBootstrap`（`@PostConstruct`，`@ConditionalOnBean(SipLayer.class)`）——读取 `SipClientProperties`，若 `sip.client.enabled=true` 则自动追加 Client 监听点

因此 E2E 测试**不需要额外 `@EnableSipClient` 注解**，只需：

```java
// voglander-web/src/test/java/.../e2e/E2eTestClientConfig.java
@TestConfiguration
public class E2eTestClientConfig {

    /** 覆盖框架默认 supplier，提供客户端身份 + 目标服务端密码 */
    @Bean
    @Primary
    public ClientDeviceSupplier e2eClientDeviceSupplier(
            @Value("${sip.client.clientId}") String clientId,
            @Value("${sip.client.domain}") String clientIp,
            @Value("${sip.client.port}") int clientPort,
            @Value("${sip.server.serverId}") String serverId,
            @Value("${sip.server.ip}") String serverIp,
            @Value("${sip.server.port}") int serverPort,
            @Value("${sip.server.password}") String password) {
        return new TestClientDeviceSupplier(
                clientId, clientIp, clientPort, serverId, serverIp, serverPort, password);
    }
}
```

### 4.2 `EventCaptor`——捕获业务事件供断言

`ShardDispatcher` 是 `final` 的简单 Bean（无继承约束），最简做法是在 `InboundEventDispatcher` 层拦截，但为保持最小侵入性，改用 **Spring AOP Advisor** 或直接在 `VoglanderBusinessNotifier` 外围用 `@SpyBean` 捕获。

实际上，E2E 测试的断言点已经是 **DB 状态**（`Awaitility` 轮询），因此不强制需要事件捕获器。若需精确断言事件流，用以下简单 Spy 包装：

```java
// voglander-web/src/test/java/.../e2e/EventCaptor.java
@TestComponent
public class EventCaptor {

    private final List<DeviceEvent> events = new CopyOnWriteArrayList<>();
    private volatile CountDownLatch latch;

    public void expect(int count) {
        events.clear();
        latch = new CountDownLatch(count);
    }

    /** 由测试在 @BeforeEach 注册到 ShardDispatcher（通过 BeanPostProcessor 或直接引用） */
    public void onEvent(DeviceEvent event) {
        events.add(event);
        if (latch != null) latch.countDown();
    }

    public boolean await(long ms) throws InterruptedException {
        return latch == null || latch.await(ms, TimeUnit.MILLISECONDS);
    }

    public List<DeviceEvent> getEvents() { return Collections.unmodifiableList(events); }
}
```

> **简化方案**：若只断言 DB 状态，可完全省略 `EventCaptor`，仅用 `Awaitility.await().atMost(5, SECONDS).untilAsserted(...)` 轮询 DB 即可。以下测试用例采用此方案。

---

## 5. 测试用例

### TC-SIP-01：完整 SIP 端到端注册 → DB 入库（**核心链路验证**）

**链路**：`ClientCommandSender.sendRegisterCommand` → SIP/UDP → Server 5060 → `VoglanderBusinessNotifier` → `ShardDispatcher` → `Gb28181ProtocolHandler.handleRegister` → `DeviceRegisterService.login` → `tb_device`

**路径**：`voglander-web/src/test/java/io/github/lunasaw/voglander/e2e/DeviceRegistrationE2eTest.java`

```java
@SpringBootTest(classes = ApplicationWeb.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Import(E2eTestClientConfig.class)
// ⚠️ 不加 @Transactional：BusinessNotifier @Async + ShardDispatcher 分片槽均在独立线程写 DB
class DeviceRegistrationE2eTest {

    @Autowired private ClientDeviceSupplier clientDeviceSupplier;
    @Autowired private DeviceMapper         deviceMapper;

    @Value("${sip.client.clientId}") private String clientId;
    @Value("${sip.server.serverId}") private String serverId;

    @AfterEach
    void cleanup() {
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery()
            .eq(DeviceDO::getDeviceId, clientId));
    }

    @Test
    void fullSipRegister_shouldPersistOnlineDevice() throws Exception {
        FromDevice from = clientDeviceSupplier.getClientFromDevice();
        ToDevice   to   = (ToDevice) clientDeviceSupplier.getDevice(serverId);

        // 发起完整 SIP 注册（REGISTER → 401 Challenge → REGISTER+Digest → 200 OK）
        ClientCommandSender.sendRegisterCommand(from, to, 3600);

        // 等待异步链路写入 DB：BusinessNotifier(@Async) → ShardDispatcher → handler → DB
        await().atMost(5, SECONDS).untilAsserted(() -> {
            DeviceDO device = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, clientId));
            assertThat(device).as("设备记录应已写入 tb_device").isNotNull();
            assertThat(device.getStatus()).as("设备状态应为在线(1)").isEqualTo(1);
            assertThat(device.getIp()).as("设备 IP 应为 127.0.0.1").isEqualTo("127.0.0.1");
            assertThat(device.getPort()).as("设备端口应为 5061").isEqualTo(5061);
        });
    }
}
```

---

### TC-SIP-02：SIP 注销 → 设备下线

```java
@Test
void fullSipUnregister_shouldSetDeviceOffline() throws Exception {
    FromDevice from = clientDeviceSupplier.getClientFromDevice();
    ToDevice   to   = (ToDevice) clientDeviceSupplier.getDevice(serverId);

    // 先注册
    ClientCommandSender.sendRegisterCommand(from, to, 3600);
    await().atMost(5, SECONDS).until(() ->
        deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
            .eq(DeviceDO::getDeviceId, clientId)) != null);

    // 发送 expires=0 注销
    ClientCommandSender.sendUnregisterCommand(from, to);

    // 等待下线事件处理：deviceRegisterService.offline() 将状态置 0
    await().atMost(5, SECONDS).untilAsserted(() -> {
        DeviceDO device = deviceMapper.selectOne(
            Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, clientId));
        assertThat(device).isNotNull();
        assertThat(device.getStatus()).as("注销后状态应为离线(0)").isEqualTo(0);
    });
}
```

---

### TC-SIP-03：注册幂等性——重复注册不产生重复记录

```java
@Test
void duplicateRegister_shouldNotCreateDuplicateRecord() throws Exception {
    FromDevice from = clientDeviceSupplier.getClientFromDevice();
    ToDevice   to   = (ToDevice) clientDeviceSupplier.getDevice(serverId);

    // 第一次注册
    ClientCommandSender.sendRegisterCommand(from, to, 3600);
    await().atMost(5, SECONDS).until(() ->
        deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
            .eq(DeviceDO::getDeviceId, clientId)) != null);

    Long firstId = deviceMapper.selectOne(
        Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, clientId)).getId();

    // 第二次注册（模拟设备重启重注册）
    ClientCommandSender.sendRegisterCommand(from, to, 3600);
    Thread.sleep(2000); // 等异步处理完成

    List<DeviceDO> records = deviceMapper.selectList(
        Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, clientId));
    assertThat(records).as("重复注册不应产生多条记录").hasSize(1);
    assertThat(records.get(0).getId()).as("upsert 应复用同一条记录").isEqualTo(firstId);
}
```

---

### TC-05：`VoglanderDeviceSessionCache` 寻址（单元测试）

```java
// voglander-integration/src/test/java/.../supplier/VoglanderDeviceSessionCacheTest.java
@ExtendWith(MockitoExtension.class)
class VoglanderDeviceSessionCacheTest {

    @Mock VoglanderServerDeviceSupplier supplier;
    @InjectMocks VoglanderDeviceSessionCache cache;

    @Test
    void getToDevice_delegates_to_supplier() {
        ToDevice expected = ToDevice.getInstance("34020000001310000001", "127.0.0.1", 5061);
        when(supplier.getToDevice("34020000001310000001")).thenReturn(expected);
        assertThat(cache.getToDevice("34020000001310000001")).isSameAs(expected);
    }

    @Test
    void getToDevice_returns_null_for_unregistered_device() {
        when(supplier.getToDevice("unknown")).thenReturn(null);
        assertThat(cache.getToDevice("unknown")).isNull();
    }
}
```

---

### TC-06：`Gb28181ProtocolHandler.handleRegister` 逻辑（单元测试）

```java
// voglander-integration/src/test/java/.../handler/Gb28181ProtocolHandlerTest.java
@ExtendWith(MockitoExtension.class)
class Gb28181ProtocolHandlerTest {

    @Mock DeviceRegisterService deviceRegisterService;
    @Mock DeviceManager         deviceManager;
    @Mock DeviceChannelManager  deviceChannelManager;
    @Mock MediaSessionManager   mediaSessionManager;
    @InjectMocks Gb28181ProtocolHandler handler;

    @Test
    void handleRegister_calls_login_with_correct_fields() {
        DeviceEvent event = new DeviceEvent("gb28181", "Lifecycle", "Register",
            "34020000001320000001", null, System.currentTimeMillis(),
            Map.of("remoteIp", "127.0.0.1", "remotePort", 5061, "expire", 3600), null);

        handler.handle(event);

        ArgumentCaptor<DeviceRegisterReq> captor = ArgumentCaptor.forClass(DeviceRegisterReq.class);
        verify(deviceRegisterService).login(captor.capture());
        assertThat(captor.getValue().getDeviceId()).isEqualTo("34020000001320000001");
        assertThat(captor.getValue().getRemoteIp()).isEqualTo("127.0.0.1");
        assertThat(captor.getValue().getRemotePort()).isEqualTo(5061);
        assertThat(captor.getValue().getExpire()).isEqualTo(3600);
    }

    @Test
    void handleOffline_calls_offline_and_cascadeOffline() {
        DeviceEvent event = new DeviceEvent("gb28181", "Lifecycle", "Offline",
            "34020000001320000001", null, System.currentTimeMillis(), null, null);

        handler.handle(event);

        verify(deviceRegisterService).offline("34020000001320000001");
        verify(deviceChannelManager).cascadeOffline("34020000001320000001");
    }
}
```

---

### TC-03：目录查询 → 通道批量入库（Manager 集成测试）

```java
// voglander-web/src/test/java/.../manager/CatalogSyncIntegrationTest.java
class CatalogSyncIntegrationTest extends BaseTest {

    @Autowired DeviceChannelManager deviceChannelManager;
    @Autowired DeviceChannelMapper  channelMapper;

    @Test
    void batchUpsert_should_write_channels_with_status() {
        String deviceId = "34020000001320000001";
        List<DeviceChannelDTO> channels = List.of(
            buildChannel(deviceId, "34020000001320000011", "摄像头-1", 1),
            buildChannel(deviceId, "34020000001320000012", "摄像头-2", 0));

        deviceChannelManager.batchUpsertWithStatus(deviceId, channels);

        List<?> saved = channelMapper.selectList(
            Wrappers.<DeviceChannelDO>lambdaQuery().eq(DeviceChannelDO::getDeviceId, deviceId));
        assertThat(saved).hasSize(2);
    }

    private DeviceChannelDTO buildChannel(String deviceId, String channelId, String name, int status) {
        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setDeviceId(deviceId);
        dto.setChannelId(channelId);
        dto.setName(name);
        dto.setStatus(status);
        dto.setLastSeenTime(LocalDateTime.now());
        dto.setStatusSource("CATALOG");
        return dto;
    }
}
```

---

## 6. 测试执行顺序与隔离策略

| 用例 | 类型 | 隔离方式 | 清理 |
|------|------|----------|------|
| TC-05 | 纯单元（Mockito） | 无 DB | N/A |
| TC-06 | 纯单元（Mockito） | 无 DB | N/A |
| TC-03 | Manager 集成 | `@Transactional` 自动回滚 | 自动 |
| TC-SIP-01 | **真实 SIP E2E** | `@AfterEach` 手动 DELETE | 删 clientId 设备记录 |
| TC-SIP-02 | **真实 SIP E2E** | `@BeforeEach` 注册 + `@AfterEach` DELETE | 删设备记录 |
| TC-SIP-03 | **真实 SIP E2E** | `@AfterEach` 手动 DELETE | 同 TC-SIP-01 |

**E2E 测试为什么不能用 `@Transactional`**：  
`VoglanderBusinessNotifier` 标注 `@Async("sipNotifierExecutor")`，`ShardDispatcher` 分片槽也在独立线程执行，DB 写入在独立事务完成。测试线程的事务边界覆盖不到异步线程，必须手动清理。

---

## 7. 执行命令

```bash
# 单元测试（无需 Redis / SIP 端口，立即可跑）
mvn test -pl voglander-integration \
    -Dtest="VoglanderDeviceSessionCacheTest,Gb28181ProtocolHandlerTest"

# TC-03 Manager 集成测试
mvn test -pl voglander-web -Dtest="CatalogSyncIntegrationTest" \
    -Dspring.profiles.active=test

# 真实 SIP E2E 集成测试（需要 5060/5061 端口可用）
mvn test -pl voglander-web \
    -Dtest="DeviceRegistrationE2eTest" \
    -Dspring.profiles.active=test
```

---

## 8. 已知约束与风险

### 8.1 实施前提条件

| 前提 | 状态 | 处理 |
|------|------|------|
| `voglander-web/pom.xml` 缺 `gb28181-client` 依赖 | ❌ 缺失 | 补 test scope |
| `voglander-web/pom.xml` 缺 `gb28181-test` 依赖 | ❌ 缺失 | 补 test scope（含 `TestClientDeviceSupplier` / `SipBootstrap`） |
| `SipBootstrap` 激活 Client 监听点 | ✅ 已确认 | `sip.client.enabled=true` + `@PostConstruct` 自动追加 5061 监听点；无需 `@EnableSipClient` |
| `ClientCommandSender` 静态方法 | ✅ 已确认 | `sendRegisterCommand(from, to, expires)` / `sendUnregisterCommand(from, to)` |
| `ShardDispatcher` 非 final，无需继承 | ✅ 已确认 | 构造依赖 `shardCount` + `InboundEventDispatcher`，E2E 测试直接用真实 Bean |
| `VoglanderBusinessNotifier` 默认激活 | ✅ 已确认 | `@ConditionalOnProperty(matchIfMissing=true)` |
| `BaseTest` 含 SIP 属性（5060/5061） | ✅ 已确认 | E2E 测试类继承 BaseTest 属性即可 |
| ClientId 统一 | ✅ 已对齐 | `34020000001320000001`（BaseTest 实际值） |

### 8.2 运行时约束

| 约束 | 说明 | 缓解 |
|------|------|------|
| 双 SIP 栈端口绑定 | Server:5060 + Client:5061，同 JVM 两个 SipLayer 监听点 | `SipBootstrap` 已验证支持；CI 上可改随机端口防冲突 |
| 异步时序 | `@Async` 线程 + 分片槽时间不确定 | `Awaitility.await().atMost(5, SECONDS)` 轮询 DB |
| `@Transactional` 不覆盖异步写入 | 已在第 6 节说明 | E2E 测试类**不加** `@Transactional`，`@AfterEach` 手动 DELETE |
| `EventCaptor` 可选 | E2E 已改为直接断言 DB 状态，不强依赖事件拦截 | 若需事件级断言，按第 4.2 节补充 |

### 8.3 实施顺序

1. **TC-05 / TC-06**：纯 Mockito，零依赖，立即可跑
2. **TC-03**：复用 `BaseTest`，验证通道批量写入
3. **补 pom 依赖**：`gb28181-client` + `gb28181-test` test scope，验证编译
4. **TC-SIP-01**：核心链路验证，去掉 `@Transactional`，`@AfterEach` 手动清理
5. **TC-SIP-02 / TC-SIP-03**：复用 TC-SIP-01 上下文，顺序实施
