# GB28181 端到端注册链路测试方案

> 覆盖范围：Client 发起注册 → SIP 协议交互 → BusinessNotifier 回调 → Voglander DB 入库

---

## 1. 测试目标

| 层次 | 验证点 |
|------|--------|
| SIP 协议层 | Client REGISTER → 401 Challenge → 携带 Digest 重注册 → 200 OK |
| 网关回调层 | `BusinessNotifier.notify()` 被触发，`GatewayEvent` 三段式类型正确 |
| 事件路由层 | `VoglanderBusinessNotifier` → `ShardDispatcher` → `Gb28181ProtocolHandler` |
| 业务逻辑层 | `DeviceManager` 将 Device 写入 `tb_device`；状态=在线 |
| 通道层（可选）| 后续目录查询触发 `DeviceChannelManager` batchUpsert 写入 `tb_device_channel` |

---

## 2. 测试架构

```
┌─────────────────────────────────────────────────────────────────────┐
│  同一 JVM / 同一 Spring Context                                      │
│                                                                     │
│  ┌──────────────┐   SIP/UDP    ┌──────────────────────────────────┐ │
│  │  SIP Client  │ ──────────▶  │  SIP Server (sip-gateway)        │ │
│  │ (gb28181     │              │  ServerDeviceSupplier            │ │
│  │  -client)    │  ◀────────── │  DeviceSessionCache              │ │
│  └──────────────┘              └──────────┬───────────────────────┘ │
│                                           │ BusinessNotifier.notify() │
│                                           ▼                          │
│                               ┌──────────────────────┐              │
│                               │ VoglanderBusinessNotifier           │
│                               │  ShardDispatcher                    │
│                               │  Gb28181ProtocolHandler             │
│                               └──────────┬───────────┘              │
│                                          │ handleRegister()         │
│                                          ▼                          │
│                               ┌──────────────────────┐              │
│                               │   DeviceManager       │              │
│                               │   DeviceChannelManager│              │
│                               └──────────┬───────────┘              │
│                                          │                          │
│                                          ▼                          │
│                               ┌──────────────────────┐              │
│                               │  SQLite (test-app.db) │              │
│                               │  tb_device            │              │
│                               │  tb_device_channel    │              │
│                               └──────────────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

**关键决策：单进程双角色**  
Server 和 Client 跑在同一 Spring Context 内，监听不同端口（Server: 5060，Client: 5070）。无需启动两个进程，`CountDownLatch` 监听回调完成。

---

## 3. 依赖与配置

### 3.1 测试模块依赖

在 `voglander-web/pom.xml` 的 `<dependencies>` 中确认以下测试依赖已存在（或补充）：

```xml
<!-- SIP 客户端 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-client</artifactId>
    <scope>test</scope>
</dependency>
<!-- SIP 客户端 starter（启用 @EnableSipClient）-->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>sip-gateway-spring-boot-starter</artifactId>
    <scope>test</scope>
</dependency>
```

### 3.2 测试配置属性

在 `BaseTest.@TestPropertySource` 基础上，追加以下属性（或放入 `application-e2e.yml`）：

```yaml
# SIP Server（Voglander 已有配置，确认值）
sip.server.serverId: 34020000002000000001
sip.server.ip: 127.0.0.1
sip.server.port: 5060
sip.server.password: 12345678

# SIP Client（新增，用于测试设备模拟）
sip.client.clientId: 34020000001310000001
sip.client.domain: 127.0.0.1
sip.client.port: 5070

# 异步回调等待超时（毫秒）
test.e2e.timeout-ms: 5000
```

---

## 4. 测试夹具（Fixture）

### 4.1 `E2eTestClientConfig`（测试专用 Bean，`@TestConfiguration`）

```java
@TestConfiguration
@EnableSipClient  // 激活 SIP Client 端协议栈
public class E2eTestClientConfig {

    /** 模拟设备的 ClientDeviceSupplier，复用 TestClientDeviceSupplier 逻辑 */
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

> **为什么 `@TestConfiguration`**：避免污染主应用上下文，Bean 仅在引入该配置的测试类中生效。

### 4.2 回调捕获器 `NotifierCaptor`

```java
/**
 * 拦截 VoglanderBusinessNotifier 的事件，供测试断言。
 * 在测试上下文中以 @Primary 覆盖真实 notifier。
 */
@Component
@Primary
@Slf4j
public class NotifierCaptor implements BusinessNotifier {

    private final VoglanderBusinessNotifier delegate;  // 仍委托真实处理
    private final List<GatewayEvent> captured = new CopyOnWriteArrayList<>();
    private volatile CountDownLatch latch;

    public NotifierCaptor(VoglanderBusinessNotifier delegate) {
        this.delegate = delegate;
    }

    public void expectEvents(int count) {
        captured.clear();
        latch = new CountDownLatch(count);
    }

    public boolean await(long ms) throws InterruptedException {
        return latch.await(ms, TimeUnit.MILLISECONDS);
    }

    public List<GatewayEvent> getCaptured() {
        return Collections.unmodifiableList(captured);
    }

    @Override
    @Async("sipNotifierExecutor")
    public void notify(GatewayEvent event) {
        captured.add(event);
        if (latch != null) latch.countDown();
        delegate.notify(event);  // 真实业务逻辑继续执行
    }
}
```

---

## 5. 测试用例

### TC-01：设备注册 → DB 入库

**测试类**：`DeviceRegistrationE2eTest`

```
路径：voglander-web/src/test/java/io/github/lunasaw/voglander/e2e/DeviceRegistrationE2eTest.java
```

#### 5.1.1 前置条件

| 项目 | 值 |
|------|----|
| DB 状态 | `tb_device` 不存在 `deviceId=34020000001310000001` 的记录 |
| SIP Server | 已启动（随应用上下文启动） |
| SIP Client | 就绪（`@EnableSipClient` 激活） |

#### 5.1.2 步骤

```
1. captor.expectEvents(1)          // 期待 1 个 Lifecycle.Register 事件
2. clientCommandSender.register()  // 触发 Client 发送 REGISTER
3. captor.await(5000ms)            // 等待回调
4. 断言 GatewayEvent 内容
5. 查询 DB，断言设备记录
```

#### 5.1.3 断言清单

```
// GatewayEvent 断言
event.getType()     == "gb28181.Lifecycle.Register"
event.getDeviceId() == "34020000001310000001"

// DB 断言（DeviceMapper 查询）
device != null
device.getDeviceId()  == "34020000001310000001"
device.getStatus()    == 1  (在线)
device.getRegisterTime() != null
device.getIp()        == "127.0.0.1"
device.getPort()      == 5070
```

#### 5.1.4 示例代码框架

```java
@SpringBootTest(classes = ApplicationWeb.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Import({E2eTestClientConfig.class, NotifierCaptor.class})
@Transactional
class DeviceRegistrationE2eTest {

    @Autowired private ClientCommandSender clientCommandSender;
    @Autowired private NotifierCaptor captor;
    @Autowired private DeviceMapper deviceMapper;

    @Value("${sip.client.clientId}") private String clientId;
    @Value("${test.e2e.timeout-ms:5000}") private long timeoutMs;

    @Test
    void device_register_should_persist_online_record() throws Exception {
        captor.expectEvents(1);

        clientCommandSender.register();

        boolean notified = captor.await(timeoutMs);
        assertThat(notified).as("BusinessNotifier 未在超时内收到回调").isTrue();

        // 事件断言
        GatewayEvent event = captor.getCaptured().get(0);
        assertThat(event.getType()).isEqualTo("gb28181.Lifecycle.Register");
        assertThat(event.getDeviceId()).isEqualTo(clientId);

        // DB 断言（@Transactional 保证事务可见性，需等异步任务提交）
        await().atMost(3, SECONDS).untilAsserted(() -> {
            DeviceDO device = deviceMapper.selectOne(
                    Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, clientId));
            assertThat(device).isNotNull();
            assertThat(device.getStatus()).isEqualTo(1);
            assertThat(device.getIp()).isEqualTo("127.0.0.1");
        });
    }
}
```

> **注意**：`@Async` 回调在独立线程提交事务，测试事务无法直接感知。使用 `Awaitility.await()` 轮询 DB，而非在同一事务内断言。测试结束后 `@Transactional` 回滚测试自身写入（非异步线程写入），需在 `@AfterEach` 手动清理异步写入的设备记录。

---

### TC-02：设备下线 → DB 状态级联更新

**测试类**：`DeviceOfflineE2eTest`

#### 前提

TC-01 已完成（设备在线），或在 `@BeforeEach` 中预置在线设备记录。

#### 步骤

```
1. @BeforeEach 预置设备记录（status=1）
2. captor.expectEvents(1)
3. clientCommandSender.unregister() / 停止心跳超时触发下线
4. captor.await(5000ms)
5. 断言 event.type == "gb28181.Lifecycle.Offline"
6. await DB: device.status == 0
```

#### 断言清单

```
event.getType()   == "gb28181.Lifecycle.Offline"
device.getStatus() == 0
// Stage 2 级联：tb_device_channel.status == 0（若有通道记录）
```

---

### TC-03：目录查询 → 通道批量入库

**测试类**：`CatalogSyncE2eTest`

> 此用例依赖 TC-01（设备已注册在线）。

#### 步骤

```
1. @BeforeEach 预置在线设备
2. captor.expectEvents(1)  // 期待 Response.Catalog 事件
3. 构造模拟目录响应（NOTIFY SIP MESSAGE，含 DeviceList/Item）
   或调用 serverCommandSender.queryCatalog(deviceId) + client 侧模拟回应
4. captor.await(5000ms)
5. 断言 event.type == "gb28181.Response.Catalog"
6. await DB: tb_device_channel 中对应通道记录写入
```

#### 断言清单

```
event.getType()               == "gb28181.Response.Catalog"
channelList.size()            >= 1
channel.getDeviceId()         == clientId
channel.getStatus()           == 1  (ONLINE)
channel.getStatusSource()     == "catalog"
channel.getLastSeenTime()     != null
```

---

### TC-04：注册幂等性 —— 重复注册不重复写入

**测试类**：`RegistrationIdempotencyTest`

#### 步骤

```
1. clientCommandSender.register()  // 第一次
2. await DB: device 存在
3. Long firstId = device.getId()

4. clientCommandSender.register()  // 第二次（模拟设备重启重注册）
5. await 500ms

6. 查询 DB 同一 deviceId 的记录数
```

#### 断言清单

```
count(tb_device WHERE device_id=clientId) == 1  // 没有重复记录
device.getId() == firstId                         // 是同一条记录（upsert）
device.getRegisterTime() updated               // 注册时间已更新
```

---

### TC-05：`VoglanderDeviceSessionCache` 寻址正确性（单元测试）

**测试类**：`VoglanderDeviceSessionCacheTest`（`voglander-integration` 模块）

> 纯单元测试，不需要完整 Spring Context。

```java
@ExtendWith(MockitoExtension.class)
class VoglanderDeviceSessionCacheTest {

    @Mock VoglanderServerDeviceSupplier supplier;
    @InjectMocks VoglanderDeviceSessionCache cache;

    @Test
    void getToDevice_delegates_to_supplier() {
        ToDevice expected = ToDevice.getInstance("34020000001310000001", "127.0.0.1", 5070);
        when(supplier.getToDevice("34020000001310000001")).thenReturn(expected);

        ToDevice result = cache.getToDevice("34020000001310000001");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getToDevice_returns_null_for_unregistered_device() {
        when(supplier.getToDevice("unknown")).thenReturn(null);
        assertThat(cache.getToDevice("unknown")).isNull();
    }
}
```

---

### TC-06：`Gb28181ProtocolHandler.handleRegister` 逻辑单元测试

**测试类**：`Gb28181ProtocolHandlerTest`（`voglander-integration` 模块）

> 验证事件到 Manager 调用的映射，不需要真实 SIP 栈。

```java
@ExtendWith(MockitoExtension.class)
class Gb28181ProtocolHandlerTest {

    @Mock DeviceManager deviceManager;
    @InjectMocks Gb28181ProtocolHandler handler;

    @Test
    void handleRegister_calls_deviceManager_with_correct_args() {
        DeviceEvent event = buildRegisterEvent("34020000001310000001", "127.0.0.1", 5070);

        handler.handle(event);

        ArgumentCaptor<DeviceDO> captor = ArgumentCaptor.forClass(DeviceDO.class);
        verify(deviceManager).register(captor.capture());  // 或 saveOrUpdate，视实现而定
        DeviceDO saved = captor.getValue();
        assertThat(saved.getDeviceId()).isEqualTo("34020000001310000001");
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    void handleOffline_calls_deviceManager_offline_and_cascades() {
        DeviceEvent event = buildOfflineEvent("34020000001310000001");

        handler.handle(event);

        verify(deviceManager).offline("34020000001310000001");
    }
}
```

---

## 6. 测试执行顺序与隔离策略

| 用例 | 类型 | 隔离方式 | 清理 |
|------|------|----------|------|
| TC-05 | 单元 | Mockito，无 DB | N/A |
| TC-06 | 单元 | Mockito，无 DB | N/A |
| TC-01 | 集成（E2E） | `@AfterEach` 手动删 `tb_device` | 删除 deviceId 记录 |
| TC-02 | 集成（E2E） | `@BeforeEach` 插入 + `@AfterEach` 清理 | 同上 |
| TC-03 | 集成（E2E） | `@BeforeEach` 插入设备 + `@AfterEach` 清理通道 | 删通道 + 设备 |
| TC-04 | 集成（E2E） | `@AfterEach` 清理 | 同 TC-01 |

**为什么不用 `@Transactional` 回滚代替手动清理**：  
`BusinessNotifier` 是 `@Async`，DB 写入在独立线程独立事务中完成，测试事务回滚不覆盖异步写入。必须在 `@AfterEach` 手动 `DELETE`。

---

## 7. 执行命令

```bash
# 仅跑单元测试（快速，无需 Redis/SIP）
mvn test -pl voglander-integration -Dtest="*CacheTest,*HandlerTest"

# 跑全部 E2E 集成测试
mvn test -pl voglander-web -Dtest="*E2eTest,*IdempotencyTest" \
    -Dspring.profiles.active=test

# 跑完整测试套件
mvn test -pl voglander-web -Dspring.profiles.active=test
```

---

## 8. 已知约束与风险

### 8.1 实现前提条件（已审核）

实施本方案前必须确认以下前提，否则会编译失败或运行时报错：

| 前提 | 当前状态 | 处理方式 |
|------|----------|----------|
| `voglander-web/pom.xml` 缺少 `gb28181-client` 依赖 | ❌ 缺失 | 添加 `<dependency>` test scope（artifact 在 sip-proxy 本地已存在） |
| `voglander-web/pom.xml` 缺少 `gb28181-test` 依赖 | ❌ 缺失 | 同上，用于复用 `TestClientDeviceSupplier` |
| `TestClientDeviceSupplier` 类 | ❌ 项目内不存在 | 从 `sip-proxy/sip-test/gb28181-test` 复制或直接在测试目录内新建 |
| `ClientCommandSender` 为静态方法，无需注入 | ✅ 确认 | 调用方式：`ClientCommandSender.sendRegisterCommand(from, to, 3600)` |
| `VoglanderBusinessNotifier` 带 `@ConditionalOnProperty` | ⚠️ 需确认 | 确保 `application-test.yml` 中对应属性已开启，否则 `NotifierCaptor` 构造注入失败 |
| `BaseTest` 已包含 `sip.client.enabled` 等属性 | ✅ 确认 | 无需在 `application-e2e.yml` 重复配置，直接复用 |
| `@EnableSipClient` 需在 `ApplicationWeb` 或测试 `@SpringBootApplication` 上声明 | ⚠️ 需确认 | `sip-proxy` 的做法是在 `TestApplication` 上同时加 `@EnableSipServer` + `@EnableSipClient`；Voglander 的 `ApplicationWeb` 当前只有 `@EnableSipServer`，测试用 `@Import(E2eTestClientConfig.class)` 加 `@EnableSipClient` 是否足够需验证 |

### 8.2 运行时约束

| 约束 | 说明 | 缓解 |
|------|------|------|
| 端口冲突 | Server:5060 / Client:5070 在 CI 上可能被占用 | 改用随机端口 + `@TestPropertySource` 覆盖 |
| 异步时序 | `@Async` 回调时间不确定 | `Awaitility` 轮询，最大等待 5s |
| `@Transactional` 不覆盖异步写入 | `BusinessNotifier` 在独立线程提交事务，测试事务回滚不覆盖 | `@AfterEach` 手动 `DELETE`，已在第 6 节说明 |
| Redis 依赖 | E2E 测试默认不需要 Redis（`DeviceSessionCache` 用内存实现） | `@ExtendWith(RedisAvailableExtension.class)` 条件跳过（如引入多节点用例） |
| SIP Client 端口复用 | 多测试类并发启动时端口冲突 | 同一 Context 复用（`@SpringBootTest` 默认缓存 Context） |
| 目录回应模拟（TC-03） | 需要 Client 侧响应目录查询，代码实现复杂 | 先手动构造 NOTIFY SIP 消息注入，或降级为 Manager 层集成测试 |

### 8.3 实施顺序建议

1. **先跑 TC-05/TC-06**（纯 Mockito 单元测试，零依赖，立即可跑）
2. **补 pom 依赖 + 复制 `TestClientDeviceSupplier`**，验证编译通过
3. **确认 `@ConditionalOnProperty` 条件** 和 `@EnableSipClient` 激活方式后跑 TC-01
4. TC-02/TC-04 复用 TC-01 上下文，TC-03 最后处理
