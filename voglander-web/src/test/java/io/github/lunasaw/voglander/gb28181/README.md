# GB28181集成测试文档

本文档详细说明GB28181客户端和服务端集成测试的实现，包括测试环境、测试用例、执行方式和验证标准。

## 项目概述

GB28181集成测试框架基于Voglander业务项目，参照`sip-proxy/gb28181-test`的测试环境和模式，实现了完整的GB28181协议客户端和服务端指令测试。

### 测试目标

1. **功能完整性验证**: 确保所有GB28181指令能够正确发送和接收
2. **协议一致性保证**: 验证消息格式符合GB28181标准
3. **真实通信模拟**: 进行端到端的消息传输测试
4. **业务集成验证**: 确保指令在Voglander业务环境中正常工作
5. **性能和稳定性**: 验证系统在各种条件下的表现

## 测试架构

### 目录结构

```
voglander-web/src/test/java/io/github/lunasaw/voglander/gb28181/
├── BaseGb28181IntegrationTest.java              # 测试基类
├── Gb28181ComprehensiveIntegrationTestSuite.java # 综合测试套件
├── client/
│   └── Gb28181ClientCommandIntegrationTest.java # 客户端指令测试
├── server/
│   └── Gb28181ServerCommandIntegrationTest.java # 服务端指令测试
└── simulation/
    └── Gb28181MessageSimulationTest.java        # 真实消息模拟测试
```

### 核心组件

#### 1. BaseGb28181IntegrationTest

- **作用**: 提供GB28181测试的基础环境和工具方法
- **特性**:
    - 完整的Spring Boot应用上下文
    - 真实的SIP协议栈和消息传输
    - 标准化的设备配置和管理
    - 端到端的消息发送和接收验证

#### 2. 客户端指令测试 (Gb28181ClientCommandIntegrationTest)

- **覆盖范围**:
    - 设备信息上报指令
    - 设备状态上报指令
    - 设备目录响应指令
    - 设备告警通知指令
    - 云台控制响应指令
    - 录像信息响应指令

#### 3. 服务端指令测试 (Gb28181ServerCommandIntegrationTest)

- **覆盖范围**:
    - 设备查询指令（信息、状态、目录、预设位）
    - 录像查询指令（录像信息、录像控制）
    - 告警查询指令（告警信息、告警控制）
    - 云台控制指令（方向控制、变焦控制）
    - 设备配置指令（参数配置、重启控制）
    - 媒体流指令（实时流、回放流、会话控制）

#### 4. 真实消息模拟测试 (Gb28181MessageSimulationTest)

- **特性**:
    - 端到端消息传输验证
    - 真实SIP协议栈通信
    - 异步消息处理验证
    - 协议格式一致性检查
    - 并发和错误处理测试

## 配置说明

### SIP协议配置

```yaml
# GB28181服务端配置
sip.gb28181.server.ip=0.0.0.0
sip.gb28181.server.port=5060
sip.gb28181.server.domain=34020000002000000001
sip.gb28181.server.serverId=34020000002000000001
sip.gb28181.server.serverName=GB28181-Server

  # GB28181客户端配置
sip.gb28181.client.clientId=34020000001320000001
sip.gb28181.client.clientName=GB28181-Client
sip.gb28181.client.username=admin
sip.gb28181.client.password=123456

  # SIP性能配置
sip.performance.messageQueueSize=1000
sip.performance.threadPoolSize=200
sip.performance.enableMetrics=true
```

### 设备配置

- **服务端监听端口**: 5060
- **客户端监听端口**: 5061
- **支持协议**: UDP和TCP
- **设备ID格式**: GB28181标准20位数字编码

## 测试用例详解

### 1. 基础环境测试

#### 设备配置验证

```java

@Test
@DisplayName("测试GB28181集成环境完整性")
public void testGb28181EnvironmentIntegrity() {
    // 验证SIP组件注入
    assertNotNull(sipLayer, "SIP协议层应该正确注入");
    assertNotNull(sipListener, "SIP监听器应该正确注入");

    // 验证设备配置
    assertTrue(isDeviceAvailable(), "测试设备应该全部可用");

    // 验证设备ID格式
    assertTrue(testDeviceId.matches("\\d{20}"), "设备ID应该符合GB28181格式");
}
```

#### 基础连通性测试

```java

@Test
@DisplayName("测试基础连通性验证")
public void testBasicConnectivity() {
    // 测试客户端到服务端通信
    ResultDTO<Void> clientResult = clientDeviceCommand.sendDeviceOnlineStatus(testDeviceId);
    assertTrue(clientResult.isSuccess(), "客户端指令应该能够成功发送");

    // 测试服务端到客户端通信
    ResultDTO<Void> serverResult = serverDeviceCommand.queryDeviceInfo(testDeviceId);
    assertTrue(serverResult.isSuccess(), "服务端指令应该能够成功发送");
}
```

### 2. 客户端指令测试

#### 设备信息上报

```java

@Test
@DisplayName("测试发送设备信息指令")
public void testSendDeviceInfoCommand() throws Exception {
    TestServerMessageProcessorHandler.resetTestState();

    // 构建设备信息
    DeviceInfo deviceInfo = createTestDeviceInfo();

    // 发送指令
    ResultDTO<Void> result = deviceCommand.sendDeviceInfoCommand(testDeviceId, deviceInfo);
    assertTrue(result.isSuccess(), "指令应该发送成功");

    // 验证服务端接收
    boolean received = TestServerMessageProcessorHandler.waitForDeviceInfo(5, TimeUnit.SECONDS);
    assertTrue(received, "服务端应该在5秒内接收到设备信息");

    // 验证接收内容
    var receivedInfo = TestServerMessageProcessorHandler.getReceivedDeviceInfo();
    assertEquals(testDeviceId, receivedInfo.getDeviceId(), "设备ID应该一致");
}
```

#### 设备状态上报

```java

@Test
@DisplayName("测试发送设备状态指令")
public void testSendDeviceStatusCommand() throws Exception {
    // 构建设备状态
    DeviceStatus deviceStatus = createTestDeviceStatus();

    // 发送指令并验证接收
    ResultDTO<Void> result = statusCommand.sendDeviceStatusCommand(testDeviceId, deviceStatus);
    assertTrue(result.isSuccess(), "指令应该发送成功");

    // 等待和验证服务端接收
    boolean received = TestServerMessageProcessorHandler.waitForDeviceStatus(5, TimeUnit.SECONDS);
    assertTrue(received, "服务端应该接收到设备状态");
}
```

### 3. 服务端指令测试

#### 设备信息查询

```java

@Test
@DisplayName("测试设备信息查询指令")
public void testQueryDeviceInfo() throws Exception {
    TestClientMessageProcessorHandler.resetTestState();

    // 发送查询指令
    ResultDTO<Void> result = deviceCommand.queryDeviceInfo(testDeviceId);
    assertTrue(result.isSuccess(), "指令应该发送成功");

    // 验证客户端接收
    boolean received = TestClientMessageProcessorHandler.waitForDeviceInfo(5, TimeUnit.SECONDS);
    assertTrue(received, "客户端应该接收到设备信息查询");
}
```

#### 云台控制指令

```java

@Test
@DisplayName("测试云台控制指令")
public void testControlDevicePtz() throws Exception {
    // 测试自定义云台指令
    ResultDTO<Void> customResult = ptzCommand.controlDevicePtz(testDeviceId, "A50F01010600FF");
    assertTrue(customResult.isSuccess(), "自定义云台控制应该发送成功");

    // 测试枚举云台指令
    ResultDTO<Void> enumResult = ptzCommand.controlDevicePtz(testDeviceId, PtzCmdEnum.UP, 128);
    assertTrue(enumResult.isSuccess(), "枚举云台控制应该发送成功");
}
```

### 4. 真实消息模拟测试

#### 设备注册流程

```java

@Test
@DisplayName("测试设备注册流程模拟")
public void testDeviceRegistrationSimulation() throws Exception {
    FromDevice clientFromDevice = getClientFromDevice();
    ToDevice clientToDevice = getClientToDevice();

    // 模拟设备注册
    String registerCallId = ClientCommandSender.sendRegisterCommand(
            clientFromDevice, clientToDevice, 3600);

    assertNotNull(registerCallId, "注册请求应该返回CallId");
}
```

#### 端到端通信验证

```java

@Test
@DisplayName("测试设备信息查询响应流程模拟")
public void testDeviceInfoQueryResponseSimulation() throws Exception {
    // 第一步：服务端发送查询
    ResultDTO<Void> queryResult = serverDeviceCommand.queryDeviceInfo(testDeviceId);
    assertTrue(queryResult.isSuccess(), "设备信息查询应该发送成功");

    // 等待客户端接收
    boolean queryReceived = TestClientMessageProcessorHandler.waitForDeviceInfo(3, TimeUnit.SECONDS);
    assertTrue(queryReceived, "客户端应该接收到设备信息查询");

    // 第二步：客户端响应信息
    DeviceInfo deviceInfo = createDetailedDeviceInfo(testDeviceId);
    ResultDTO<Void> responseResult = clientDeviceCommand.sendDeviceInfoCommand(testDeviceId, deviceInfo);
    assertTrue(responseResult.isSuccess(), "设备信息响应应该发送成功");

    // 验证服务端接收响应
    boolean responseReceived = TestServerMessageProcessorHandler.waitForDeviceInfo(3, TimeUnit.SECONDS);
    assertTrue(responseReceived, "服务端应该接收到设备信息响应");
}
```

### 5. 协议一致性验证

#### GB28181设备ID格式验证

```java

@Test
@DisplayName("测试GB28181协议标准一致性")
public void testGb28181ProtocolStandardCompliance() {
    String deviceId = generateTestDeviceId();

    // GB28181设备ID格式验证：20位数字编码
    assertTrue(deviceId.matches("^[0-9]{20}$"), "设备ID应该是20位数字编码");

    // 验证设备ID结构
    String devicePrefix = deviceId.substring(0, 8);   // 行政区划码
    String deviceType = deviceId.substring(8, 11);    // 设备类型码
    String networkId = deviceId.substring(11, 12);    // 网络标识
    String serialNumber = deviceId.substring(12, 20); // 序列号

    assertTrue(devicePrefix.matches("^[0-9]{8}$"), "设备前缀应该是8位数字");
    assertTrue(deviceType.matches("^[0-9]{3}$"), "设备类型应该是3位数字");
    assertTrue(networkId.matches("^[0-9]{1}$"), "网络标识应该是1位数字");
    assertTrue(serialNumber.matches("^[0-9]{8}$"), "序列号应该是8位数字");
}
```

### 6. 性能和稳定性测试

#### 性能基准测试

```java

@Test
@DisplayName("测试指令发送性能基准")
public void testCommandPerformanceBenchmark() throws Exception {
    int testCount = 10;

    // 客户端指令性能测试
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < testCount; i++) {
        ResultDTO<Void> result = clientDeviceCommand.sendDeviceOnlineStatus(testDeviceId);
        assertTrue(result.isSuccess(), "客户端指令应该成功发送");
    }
    long duration = System.currentTimeMillis() - startTime;

    double avgTime = (double) duration / testCount;
    assertTrue(avgTime < 1000, "平均耗时应该小于1秒");
}
```

#### 并发测试

```java

@Test
@DisplayName("测试并发消息发送模拟")
public void testConcurrentMessageSimulation() throws Exception {
    int concurrentCount = 5;
    CompletableFuture<ResultDTO<Void>>[] futures = new CompletableFuture[concurrentCount];

    // 创建并发任务
    for (int i = 0; i < concurrentCount; i++) {
        futures[i] = CompletableFuture.supplyAsync(() -> {
            return serverPtzCommand.controlDevicePtz(testDeviceId, PtzCmdEnum.UP, 100);
        });
    }

    // 等待所有任务完成
    CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

    // 验证结果
    for (CompletableFuture<ResultDTO<Void>> future : futures) {
        assertTrue(future.get().isSuccess(), "并发任务应该执行成功");
    }
}
```

## 执行方式

### 1. 单独测试类执行

```bash
# 执行客户端指令测试
mvn test -Dtest=Gb28181ClientCommandIntegrationTest

# 执行服务端指令测试  
mvn test -Dtest=Gb28181ServerCommandIntegrationTest

# 执行消息模拟测试
mvn test -Dtest=Gb28181MessageSimulationTest
```

### 2. 完整测试套件执行

```bash
# 执行综合测试套件
mvn test -Dtest=Gb28181ComprehensiveIntegrationTestSuite

# 执行所有GB28181测试
mvn test -Dtest="io.github.lunasaw.voglander.gb28181.**"
```

### 3. 指定测试环境

```bash
# 使用测试配置
mvn test -Dspring.profiles.active=test -Dtest=Gb28181*

# 启用调试日志
mvn test -Dlogging.level.io.github.lunasaw.sip=DEBUG -Dtest=Gb28181*
```

## 验证标准

### 1. 功能验证标准

- **指令发送成功**: `ResultDTO.isSuccess() == true`
- **消息接收验证**: 对应的Handler能在指定时间内接收到消息
- **内容一致性**: 发送和接收的关键字段保持一致
- **异常处理**: 错误场景能够正确处理并返回适当的错误信息

### 2. 协议一致性标准

- **设备ID格式**: 符合GB28181标准的20位数字编码
- **消息格式**: XML格式符合GB28181协议规范
- **SIP头信息**: 包含正确的From、To、CallId等标准SIP头
- **内容类型**: 正确设置Content-Type为application/MANSCDP+xml

### 3. 性能标准

- **响应时间**: 单个指令发送时间 < 1秒
- **并发处理**: 支持至少5个并发指令不出错
- **稳定性**: 连续执行100次指令成功率 > 99%
- **内存使用**: 测试过程中无内存泄漏

### 4. 业务集成标准

- **Spring集成**: 所有组件能正确注入和初始化
- **事务处理**: 支持Spring事务管理
- **配置管理**: 支持多环境配置切换
- **日志记录**: 完整的操作日志和错误日志

## 故障排除

### 常见问题

1. **设备配置错误**
    - 检查TestDeviceSupplier是否正确配置
    - 验证设备ID格式是否符合GB28181标准
    - 确认IP地址和端口配置

2. **消息接收超时**
    - 检查SIP监听器是否正确设置
    - 验证防火墙和网络配置
    - 增加等待超时时间

3. **依赖注入失败**
    - 确认Spring配置正确
    - 检查测试类的@SpringBootTest配置
    - 验证组件扫描路径

4. **协议格式错误**
    - 检查XML消息格式
    - 验证Content-Type设置
    - 确认编码格式

### 调试建议

1. **启用详细日志**:
   ```yaml
   logging:
     level:
       io.github.lunasaw.sip: DEBUG
       io.github.lunasaw.gbproxy: DEBUG
       io.github.lunasaw.voglander.intergration.wrapper.gb28181: DEBUG
   ```

2. **使用测试工具**:
    - SIP抓包工具(如Wireshark)
    - SIP测试客户端
    - 网络连通性测试

3. **分步测试**:
    - 先执行基础环境测试
    - 再执行单个指令测试
    - 最后执行综合场景测试

## 扩展和定制

### 添加新的测试用例

1. **继承基础测试类**:
   ```java
   public class CustomGb28181Test extends BaseGb28181IntegrationTest {
       // 自定义测试逻辑
   }
   ```

2. **实现自定义验证逻辑**:
   ```java
   private void customValidation(String expectedValue, String actualValue) {
       assertEquals(expectedValue, actualValue, "自定义验证失败");
   }
   ```

3. **添加性能监控**:
   ```java
   @Test
   public void testWithPerformanceMonitoring() {
       long startTime = System.currentTimeMillis();
       // 执行测试逻辑
       long duration = System.currentTimeMillis() - startTime;
       assertTrue(duration < EXPECTED_MAX_TIME, "性能要求未满足");
   }
   ```

### 测试数据定制

- 修改`createTestDeviceInfo()`等方法来定制测试数据
- 通过配置文件管理测试参数
- 使用测试数据构建器模式

### 集成其他测试工具

- 集成JMeter进行压力测试
- 使用TestContainers创建隔离测试环境
- 结合Mockito进行单元测试

## 总结

GB28181集成测试框架提供了完整的测试覆盖，从基础功能到复杂业务场景，从协议一致性到性能稳定性，确保了Voglander系统中GB28181集成的可靠性和正确性。通过真实的消息传输模拟和端到端验证，保证了系统在生产环境中的稳定运行。