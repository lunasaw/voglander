# GB28181 端到端注册链路测试方案

> 覆盖范围：Client 发起注册 → SIP 协议交互 → BusinessNotifier 回调 → Voglander DB 入库

---

## 1. 测试目标

| 层次 | 验证点 |
|------|--------|
| SIP 协议层 | Client REGISTER → 401 Challenge → 携带 Digest 重注册 → 200 OK |
| 网关回调层 | `VoglanderBusinessNotifier.notify()` → 翻译为 `DeviceEvent` → `ShardDispatcher.dispatch()` |
| 事件路由层 | `ShardDispatcher` → `Gb28181ProtocolHandler.handle()` |
| 业务逻辑层 | `handleRegister()` → `DeviceRegisterService.login()` → `tb_device` 入库（状态=在线） |
| 通道层（可选）| 目录查询 → `DeviceChannelManager.batchUpsertWithStatus()` → `tb_device_channel` |

---

## 2. 测试架构

```
┌─────────────────────────────────────────────────────────────────────┐
│  同一 JVM / 同一 Spring Context                                      │
│                                                                     │
│  ┌──────────────┐   SIP/UDP    ┌──────────────────────────────────┐ │
│  │  SIP Client  │ ──────────▶  │  SIP Server (sip-gateway)        │ │
│  │ (gb28181     │              │  VoglanderDeviceSessionCache      │ │
│  │  -client)    │  ◀────────── │  VoglanderServerDeviceSupplier   │ │
│  └──────────────┘              └──────────┬───────────────────────┘ │
│                                           │ BusinessNotifier.notify() │
│                                           ▼                          │
│                               ┌──────────────────────┐              │
│                               │ VoglanderBusinessNotifier           │
│                               │  @Async("sipNotifierExecutor")      │
│                               │  GatewayEvent → DeviceEvent         │
│                               │  ShardDispatcher.dispatch()         │
│                               └──────────┬───────────┘              │
│                                          │ 分片槽单线程              │
│                                          ▼                          │
│                               ┌──────────────────────┐              │
│                               │ Gb28181ProtocolHandler│              │
│                               │  handleRegister()     │              │
│                               │  → DeviceRegisterService.login()   │
│                               └──────────┬───────────┘              │
│                                          ▼                          │
│                               ┌──────────────────────┐              │
│                               │  SQLite (test-app.db) │              │
│                               │  tb_device            │              │
│                               │  tb_device_channel    │              │
│                               └──────────────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

**关键决策：单进程双角色**  
Server(5060) + Client(5061) 跑在同一 Spring Context，端口与 `BaseTest` 配置对齐。  
`CountDownLatch` / `Awaitility` 监听异步回调完成，**不用 `@Transactional`**。

---

## 3. 依赖与配置

### 3.1 测试模块依赖（⚠️ 当前缺失，需补充）

在 `voglander-web/pom.xml` 的 `<dependencies>` 中补充：

```xml
<!-- SIP 客户端（E2E 测试用） -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-client</artifactId>
    <scope>test</scope>
</dependency>
<!-- GB28181 测试工具（TestClientDeviceSupplier 等） -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 3.2 测试配置

E2E 测试**直接复用 `BaseTest` 的 `@TestPropertySource`**，无需额外 `application-e2e.yml`：

```
# BaseTest 已包含（关键值确认）
sip.server.ip=127.0.0.1        sip.server.port=5060
sip.server.serverId=34020000002000000001
sip.server.password 通过 application-test.yml 配置

local.sip.client.clientId=34020000001320000001
local.sip.client.ip=127.0.0.1    local.sip.client.port=5061

voglander.event.shard.enabled 默认 true（matchIfMissing=true，无需配置）
```

> **注意**：`BaseTest` 已有 `sip.client.enabled=true`，`@EnableSipClient` 的激活需通过测试专用 `@TestConfiguration` 引入，详见第 4 节。

---

## 4. 测试夹具（Fixture）

### 4.1 `E2eTestClientConfig`（激活 SIP Client 协议栈）

```java
@TestConfiguration
@EnableSipClient   // 激活 Client 端协议栈；在 @TestConfiguration 上声明是否足够需首次运行验证
public class E2eTestClientConfig {

    @Bean
    @Primary
    public ClientDeviceSupplier e2eClientDeviceSupplier(
            @Value("${local.sip.client.clientId}") String clientId,
            @Value("${local.sip.client.ip}") String clientIp,
            @Value("${local.sip.client.port}") int clientPort,
            @Value("${sip.server.serverId}") String serverId,
            @Value("${sip.server.ip}") String serverIp,
            @Value("${sip.server.port}") int serverPort) {
        return new TestClientDeviceSupplier(
                clientId, clientIp, clientPort, serverId, serverIp, serverPort, "123456");
    }
}
```

> ⚠️ **待验证**：`@EnableSipClient` 在 `@TestConfiguration` 上的激活效果。若无效，需在测试专用 `@SpringBootApplication`（仅 E2E 测试用）上同时声明 `@EnableSipServer` + `@EnableSipClient`。

### 4.2 `ShardDispatcherCaptor`（捕获 DeviceEvent，替代捕获 GatewayEvent）

> **架构修正**：`VoglanderBusinessNotifier` 内部只做轻量翻译后立即委托 `ShardDispatcher.dispatch()`，真正的业务事件是 `DeviceEvent`，因此捕获点应在 `ShardDispatcher` 而非 `BusinessNotifier`。

```java
/**
 * 包装真实 ShardDispatcher，捕获 DeviceEvent 供测试断言。
 * @Primary 在测试上下文中覆盖真实 Bean。
 */
@Component
@Primary
@Slf4j
public class ShardDispatcherCaptor extends ShardDispatcher {

    private final List<DeviceEvent> captured = new CopyOnWriteArrayList<>();
    private volatile CountDownLatch latch;

    public ShardDispatcherCaptor(List<ProtocolEventHandler> handlers) {
        super(handlers);  // 委托真实分片逻辑
    }

    public void expectEvents(int count) {
        captured.clear();
        latch = new CountDownLatch(count);
    }

    public boolean await(long ms) throws InterruptedException {
        return latch != null && latch.await(ms, TimeUnit.MILLISECONDS);
    }

    public List<DeviceEvent> getCaptured() {
        return Collections.unmodifiableList(captured);
    }

    @Override
    public void dispatch(DeviceEvent event) {
        captured.add(event);
        if (latch != null) latch.countDown();
        super.dispatch(event);   // 真实业务逻辑继续
    }
}
```

> **注意**：`ShardDispatcher` 构造依赖 `List<ProtocolEventHandler>`，继承时须保持。若 `ShardDispatcher` 为 `final` 或构造不兼容，改用装饰器模式包装（`@Delegate`）。

---

## 5. 测试用例

### TC-05：`VoglanderDeviceSessionCache` 寻址（单元测试）——**首先实现，零依赖**

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

### TC-06：`Gb28181ProtocolHandler.handleRegister` 逻辑（单元测试）——**首先实现**

> **架构修正**：注册逻辑调用 `DeviceRegisterService.login(DeviceRegisterReq)`，**不是** `DeviceManager.register()`；下线调用 `DeviceRegisterService.offline(deviceId)` + `DeviceChannelManager.cascadeOffline(deviceId)`。

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
    void handleRegister_calls_login_with_correct_deviceId() {
        DeviceEvent event = new DeviceEvent("gb28181", "Lifecycle", "Register",
            "34020000001320000001", null, System.currentTimeMillis(),
            Map.of("remoteIp", "127.0.0.1", "remotePort", 5061, "expire", 3600), null);

        handler.handle(event);

        ArgumentCaptor<DeviceRegisterReq> captor = ArgumentCaptor.forClass(DeviceRegisterReq.class);
        verify(deviceRegisterService).login(captor.capture());
        assertThat(captor.getValue().getDeviceId()).isEqualTo("34020000001320000001");
        assertThat(captor.getValue().getRemoteIp()).isEqualTo("127.0.0.1");
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

### TC-01：设备注册 → DB 入库（E2E 集成测试）

**路径**：`voglander-web/src/test/java/io/github/lunasaw/voglander/e2e/DeviceRegistrationE2eTest.java`

```java
@SpringBootTest(classes = ApplicationWeb.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Import({E2eTestClientConfig.class, ShardDispatcherCaptor.class})
// ⚠️ 不加 @Transactional：BusinessNotifier 是 @Async，DB 写在独立线程独立事务，测试事务回滚覆盖不到
class DeviceRegistrationE2eTest {

    @Autowired private ClientCommandSender clientCommandSender;
    @Autowired private ShardDispatcherCaptor captor;
    @Autowired private DeviceMapper deviceMapper;

    private static final String CLIENT_ID = "34020000001320000001";

    @AfterEach
    void cleanup() {
        // 手动清理异步线程写入的设备记录（@Transactional 无法覆盖）
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery()
            .eq(DeviceDO::getDeviceId, CLIENT_ID));
    }

    @Test
    void device_register_should_persist_online_record() throws Exception {
        captor.expectEvents(1);

        clientCommandSender.register();  // 触发 REGISTER → 401 → 带 Digest 重注册 → 200 OK

        assertThat(captor.await(5000)).as("ShardDispatcher 未在超时内收到 DeviceEvent").isTrue();

        // DeviceEvent 断言
        DeviceEvent event = captor.getCaptured().get(0);
        assertThat(event.groupName()).isEqualTo("Lifecycle.Register");
        assertThat(event.deviceId()).isEqualTo(CLIENT_ID);

        // DB 断言：异步写入，用 Awaitility 轮询
        await().atMost(3, SECONDS).untilAsserted(() -> {
            DeviceDO device = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            assertThat(device).isNotNull();
            assertThat(device.getStatus()).isEqualTo(1);
            assertThat(device.getIp()).isEqualTo("127.0.0.1");
        });
    }
}
```

---

### TC-02：设备下线 → 状态级联（E2E 集成测试）

```java
@BeforeEach
void setupDevice() {
    // 预置在线设备（直接写 DB，绕过 SIP 流程）
    DeviceDO device = new DeviceDO();
    device.setDeviceId(CLIENT_ID);
    device.setStatus(1);
    device.setIp("127.0.0.1");
    device.setPort(5061);
    deviceMapper.insert(device);
}

@Test
void device_offline_should_set_status_zero() throws Exception {
    captor.expectEvents(1);

    clientCommandSender.unregister();  // 或等待心跳超时（建议 unregister 更可控）

    assertThat(captor.await(5000)).isTrue();
    assertThat(captor.getCaptured().get(0).groupName()).isEqualTo("Lifecycle.Offline");

    await().atMost(3, SECONDS).untilAsserted(() -> {
        DeviceDO device = deviceMapper.selectOne(
            Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
        assertThat(device.getStatus()).isEqualTo(0);
    });
}
```

---

### TC-04：注册幂等性（E2E 集成测试）

```java
@Test
void duplicate_register_should_not_create_duplicate_record() throws Exception {
    // 第一次注册
    captor.expectEvents(1);
    clientCommandSender.register();
    captor.await(5000);
    await().atMost(3, SECONDS).until(() ->
        deviceMapper.selectOne(Wrappers.<DeviceDO>lambdaQuery()
            .eq(DeviceDO::getDeviceId, CLIENT_ID)) != null);

    Long firstId = deviceMapper.selectOne(
        Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID)).getId();

    // 第二次注册（模拟重启）
    captor.expectEvents(1);
    clientCommandSender.register();
    captor.await(5000);
    Thread.sleep(500);

    List<DeviceDO> records = deviceMapper.selectList(
        Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
    assertThat(records).hasSize(1);
    assertThat(records.get(0).getId()).isEqualTo(firstId);
}
```

---

### TC-03：目录查询 → 通道批量入库（降级为 Manager 集成测试）

> **决策**：TC-03 原方案需要 Client 侧模拟 NOTIFY SIP 响应，实现成本高且脆。  
> **改为**：直接测试 `Gb28181ProtocolHandler.handleCatalog()` + `DeviceChannelManager.batchUpsertWithStatus()`，覆盖相同的业务语义。

```java
// 路径：voglander-web/src/test/java/.../manager/CatalogSyncIntegrationTest.java
// 继承 BaseTest（@SpringBootTest + @Transactional）
class CatalogSyncIntegrationTest extends BaseTest {

    @Autowired DeviceChannelManager deviceChannelManager;
    @Autowired DeviceChannelMapper channelMapper;

    @Test
    void batchUpsert_should_write_channels_with_status() {
        String deviceId = "34020000001320000001";
        List<DeviceChannelDTO> channels = List.of(
            buildChannel(deviceId, "34020000001320000011", "摄像头-1", 1),
            buildChannel(deviceId, "34020000001320000012", "摄像头-2", 0)
        );

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
| TC-01 | E2E 集成 | `@AfterEach` 手动 DELETE | 删 CLIENT_ID 设备记录 |
| TC-02 | E2E 集成 | `@BeforeEach` 插入 + `@AfterEach` DELETE | 删设备 + 通道 |
| TC-04 | E2E 集成 | `@AfterEach` 手动 DELETE | 同 TC-01 |

**E2E 测试为什么不能用 `@Transactional`**：  
`VoglanderBusinessNotifier` 标注 `@Async("sipNotifierExecutor")`，`ShardDispatcher` 分片槽也在独立线程执行，DB 写入在独立事务完成。测试线程的事务边界覆盖不到异步线程，必须手动清理。

---

## 7. 执行命令

```bash
# 单元测试（无需 Redis / SIP 端口，立即可跑）
mvn test -pl voglander-integration -Dtest="VoglanderDeviceSessionCacheTest,Gb28181ProtocolHandlerTest"

# TC-03 Manager 集成测试
mvn test -pl voglander-web -Dtest="CatalogSyncIntegrationTest" \
    -Dspring.profiles.active=test

# E2E 集成测试（需先验证 @EnableSipClient 激活）
mvn test -pl voglander-web -Dtest="DeviceRegistrationE2eTest,DeviceOfflineE2eTest,RegistrationIdempotencyTest" \
    -Dspring.profiles.active=test
```

---

## 8. 已知约束与风险（更新后）

### 8.1 实施前提条件

| 前提 | 状态 | 处理 |
|------|------|------|
| `voglander-web/pom.xml` 缺 `gb28181-client` 依赖 | ❌ 缺失 | 补 test scope |
| `voglander-web/pom.xml` 缺 `gb28181-test` 依赖 | ❌ 缺失 | 补 test scope（含 `TestClientDeviceSupplier`） |
| `@EnableSipClient` 在 `@TestConfiguration` 上是否足够 | ⚠️ 未验证 | 首次运行 TC-01 时验证；失败则改用测试专用 `@SpringBootApplication` |
| `ShardDispatcher` 是否可继承（非 final） | ⚠️ 未确认 | 若 final 则改装饰器；需读源码确认 |
| `VoglanderBusinessNotifier` 的 `@ConditionalOnProperty` | ✅ `matchIfMissing=true` | 无需配置，默认激活 |
| `BaseTest` 含 `sip.client.enabled=true` 等属性 | ✅ 已确认 | 无需额外配置 |
| ClientId 对齐 | ✅ 已对齐 | 统一用 `34020000001320000001`（BaseTest 实际值） |

### 8.2 运行时约束

| 约束 | 说明 | 缓解 |
|------|------|------|
| 双 SIP 栈端口绑定 | Server:5060 + Client:5061，框架须支持同 JVM 两个独立 SipLayer 实例 | 首次验证；CI 上改随机端口防冲突 |
| 异步时序 | `@Async` + 分片槽时间不确定 | `Awaitility` 最大等待 3s，CountDownLatch 等待 5s |
| `@Transactional` 不覆盖异步写入 | 已在第 6 节说明 | E2E 测试类**不加** `@Transactional`，`@AfterEach` 手动 DELETE |
| `ShardDispatcherCaptor` 并发 | 多测试类并发运行时 captor 状态共享 | 同一 Spring Context 缓存复用，`expectEvents` 在每个 `@Test` 开始时重置 |
| TC-03 SIP 模拟降级 | 原方案 Client 模拟 NOTIFY 成本高 | 改为 Manager 集成测试，已在第 5 节替换 |

### 8.3 实施顺序（修正后）

1. **TC-05 / TC-06**：纯 Mockito，零依赖，立即可跑；注意 TC-06 断言方法名已对齐实际调用链（`login` / `offline` + `cascadeOffline`）
2. **TC-03**（Manager 集成）：复用 `BaseTest`，补 `batchUpsertWithStatus` 测试，验证通道写入
3. **补 pom 依赖**：`gb28181-client` + `gb28181-test` test scope，验证编译
4. **验证 `@EnableSipClient` 激活方式**，确认双 SIP 栈可在同 JVM 绑定
5. **TC-01**（E2E 注册）：去掉 `@Transactional`，`@AfterEach` 手动清理
6. **TC-04 / TC-02**：复用 TC-01 上下文，顺序实施
