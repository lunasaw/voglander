# GB28181客户端指令包完整实现

本包为GB28181客户端指令发送提供了完整的封装实现，将底层的`ClientCommandSender`方法封装为更易用的业务接口。

## 🎯 项目概述

### 设计目标

- **简化调用**：对上游业务层提供简化的API接口，屏蔽底层复杂的参数构建和通信细节
- **统一标准**：提供一致的调用接口、错误处理机制和日志记录格式
- **模块化管理**：按功能模块将不同类型的指令分类到独立的子包中
- **易于扩展**：采用面向对象设计，便于新增指令类型和功能扩展

### 架构特点

- **继承抽象基类**：所有指令类继承`AbstractVoglanderClientCommand`，获得统一的功能
- **函数式接口**：使用Lambda表达式简化指令执行逻辑的封装
- **统一返回格式**：所有方法返回`ResultDTO<T>`格式，保证接口一致性
- **完善异常处理**：统一的异常捕获和转换机制，避免异常泄露

## 📦 包结构

```
io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command/
├── AbstractVoglanderClientCommand.java           # 统一的基础抽象类
├── package-info.java                            # 包说明文档
├── alarm/                                       # 告警指令包
│   └── VoglanderClientAlarmCommand.java
├── device/                                      # 设备信息指令包
│   └── VoglanderClientDeviceCommand.java
├── ptz/                                         # 云台控制指令包
│   └── VoglanderClientPtzCommand.java
├── record/                                      # 录像控制指令包
│   └── VoglanderClientRecordCommand.java
├── catalog/                                     # 设备目录查询指令包
│   └── VoglanderClientCatalogCommand.java
└── status/                                      # 设备状态指令包
    └── VoglanderClientStatusCommand.java
```

## 🔧 核心功能

### 1. 基础抽象类 (`AbstractVoglanderClientCommand`)

**功能特性：**

- 统一的设备信息获取（`getClientFromDevice()`, `getToDevice()`）
- 标准化的指令执行模板方法（`executeCommand()`）
- 一致的参数校验工具（`validateDeviceId()`, `validateNotNull()`）
- 完善的异常处理和日志记录

**使用示例：**

```java

@Component
public class CustomCommand extends AbstractVoglanderClientCommand {
    public ResultDTO<Void> sendCustomCommand(String deviceId, Object param) {
        validateDeviceId(deviceId, "设备ID不能为空");
        validateNotNull(param, "参数不能为空");

        return executeCommand("sendCustomCommand", deviceId,
                () -> ClientCommandSender.someMethod(getClientFromDevice(), getToDevice(deviceId), param),
                param);
    }
}
```

### 2. 告警指令包 (`alarm`)

**支持功能：**

- 基础告警发送 - `sendAlarmCommand()`
- 告警通知发送 - `sendAlarmNotifyCommand()`
- 简化告警发送 - `sendSimpleAlarmCommand()`

**使用示例：**

```java
@Autowired
private VoglanderClientAlarmCommand alarmCommand;

// 发送基础告警
DeviceAlarm alarm = new DeviceAlarm();
alarm.setAlarmType("1");
alarm.setAlarmPriority("3");
ResultDTO<Void> result = alarmCommand.sendAlarmCommand("34020000001320000001", alarm);

// 发送简化告警
ResultDTO<Void> result2 = alarmCommand.sendSimpleAlarmCommand(
    "34020000001320000001", "1", "3", "1", "设备离线告警");
```

### 3. 设备信息指令包 (`device`)

**支持功能：**

- 设备信息响应 - `sendDeviceInfoCommand()`
- 设备状态上报 - `sendDeviceStatusCommand()`
- 设备上线/离线通知 - `sendDeviceOnlineNotify()`, `sendDeviceOfflineNotify()`
- 简化设备信息发送 - `sendSimpleDeviceInfoCommand()`

**使用示例：**

```java

@Autowired
private VoglanderClientDeviceCommand deviceCommand;

// 发送设备信息
DeviceInfo deviceInfo = deviceCommand.createDeviceInfo(
        "34020000001320000001", "摄像头001", "海康威视", "DS-2CD2020-I", "V5.4.5");
ResultDTO<Void> result = deviceCommand.sendDeviceInfoCommand("34020000001320000001", deviceInfo);

// 发送设备上线通知
ResultDTO<Void> result2 = deviceCommand.sendDeviceOnlineNotify("34020000001320000001");
```

### 4. 云台控制指令包 (`ptz`)

**支持功能：**

- 方向控制 - `moveUp()`, `moveDown()`, `moveLeft()`, `moveRight()` 等
- 变焦控制 - `zoomIn()`, `zoomOut()`
- 停止控制 - `stopMove()`
- 自定义控制 - `customPtzControl()`, `sendPtzControlCommand()`

**使用示例：**

```java
@Autowired
private VoglanderClientPtzCommand ptzCommand;

// 向上移动，速度为5
ResultDTO<Void> result = ptzCommand.moveUp("34020000001320000001", 5);

// 放大变焦，速度为3
ResultDTO<Void> result2 = ptzCommand.zoomIn("34020000001320000001", 3);

// 停止云台移动
ResultDTO<Void> result3 = ptzCommand.stopMove("34020000001320000001");

// 自定义控制
ResultDTO<Void> result4 = ptzCommand.customPtzControl("34020000001320000001", 8, 128, 0, 0);
```

### 5. 录像控制指令包 (`record`)

**支持功能：**

- 录像查询响应 - `sendDeviceRecordCommand()`
- 录像文件列表响应 - `sendRecordItemsCommand()`
- 录像控制 - `startRecord()`, `stopRecord()`
- 简化录像响应 - `sendSimpleRecordResponse()`

**使用示例：**

```java
@Autowired
private VoglanderClientRecordCommand recordCommand;

// 发送录像查询响应
DeviceRecord deviceRecord = recordCommand.createDeviceRecord(
    "34020000001320000001", "录像查询", 10, recordItems);
ResultDTO<Void> result = recordCommand.sendDeviceRecordCommand("34020000001320000001", deviceRecord);

// 开始录像
ResultDTO<Void> result2 = recordCommand.startRecord("34020000001320000001");
```

### 6. 设备目录指令包 (`catalog`)

**支持功能：**

- 设备目录响应 - `sendCatalogCommand()`
- 设备列表响应 - `sendDeviceItemsCommand()`
- 单个设备项响应 - `sendSingleDeviceItemCommand()`
- 设备状态通知 - `sendDeviceOnlineNotify()`, `sendDeviceOfflineNotify()`

**使用示例：**

```java

@Autowired
private VoglanderClientCatalogCommand catalogCommand;

// 发送设备目录响应
DeviceResponse deviceResponse = catalogCommand.createDeviceResponse(
        "34020000001320000001", "目录查询", 5, deviceItems);
ResultDTO<Void> result = catalogCommand.sendCatalogCommand("34020000001320000001", deviceResponse);

// 发送设备上线通知
ResultDTO<Void> result2 = catalogCommand.sendDeviceOnlineNotify("34020000001320000001", "摄像头001");
```

### 7. 设备状态指令包 (`status`)

**支持功能：**

- 心跳保活 - `sendKeepaliveCommand()`, `sendNormalKeepalive()`
- 位置信息上报 - `sendMobilePositionCommand()`, `sendSimplePositionCommand()`
- 媒体状态通知 - `sendMediaStatusCommand()`, `sendMediaStartNotify()`
- 批量操作 - `sendBatchKeepaliveCommand()`

**使用示例：**

```java
@Autowired
private VoglanderClientStatusCommand statusCommand;

// 发送心跳保活
ResultDTO<Void> result = statusCommand.sendNormalKeepalive("34020000001320000001");

// 发送位置信息
ResultDTO<Void> result2 = statusCommand.sendSimplePositionCommand(
    "34020000001320000001", 116.397128, 39.916527);

// 发送媒体开始通知
ResultDTO<Void> result3 = statusCommand.sendMediaStartNotify("34020000001320000001");
```

## 🧪 测试支持

### 单元测试覆盖

- **基础抽象类测试** - `AbstractVoglanderClientCommandTest`
- **告警指令测试** - `VoglanderClientAlarmCommandTest`
- **测试套件** - `GB28181ClientCommandTestSuite`

### 测试特性

- 使用Mockito进行依赖模拟
- 覆盖正常流程和异常情况
- 参数校验测试
- 完整的边界条件测试

### 运行测试

```bash
# 运行所有测试
mvn test -Dtest=GB28181ClientCommandTestSuite

# 运行单个测试类
mvn test -Dtest=VoglanderClientAlarmCommandTest

# 运行特定测试方法
mvn test -Dtest=VoglanderClientAlarmCommandTest#testSendAlarmCommandSuccess
```

## 🎨 设计模式

### 1. 模板方法模式

`AbstractVoglanderClientCommand`提供了统一的执行模板，子类只需实现具体的业务逻辑：

```java
protected ResultDTO<Void> executeCommand(String methodName, String deviceId, CommandExecutor command, Object... params) {
    try {
        log.debug("{}::开始执行指令, deviceId = {}, params = {}", methodName, deviceId, params);
        String callId = command.execute();
        log.info("{}::指令执行成功, deviceId = {}, callId = {}", methodName, deviceId, callId);
        return ResultDTOUtils.success();
    } catch (Exception e) {
        log.error("{}::指令执行失败, deviceId = {}, params = {}", methodName, deviceId, params, e);
        return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, e.getMessage());
    }
}
```

### 2. 函数式接口

使用函数式接口简化Lambda表达式的使用：

```java
@FunctionalInterface
protected interface CommandExecutor {
    String execute() throws Exception;
}
```

### 3. 建造者模式

提供工具方法快速构建复杂对象：

```java
public DeviceInfo createDeviceInfo(String deviceId, String deviceName, String manufacturer, String model, String firmware) {
    // 构建逻辑
}
```

## 🔍 错误处理

### 统一异常处理

- 所有异常都被捕获并转换为`ResultDTO`格式
- 提供详细的错误信息和堆栈跟踪
- 支持自定义错误码和消息

### 参数校验

- 提供统一的参数校验方法
- 支持自定义错误消息
- 空值和边界条件检查

### 日志记录

- 统一的日志格式：`方法名::操作描述, 参数 = 值`
- 包含调试、信息和错误三个级别
- 支持参数和异常信息的完整记录

## 📈 性能优化

### 1. 依赖注入优化

- 使用`@Autowired`进行依赖注入，避免重复创建对象
- 单例模式减少内存开销

### 2. 日志优化

- 使用`@Slf4j`注解，避免手动创建Logger
- 使用参数化日志，避免字符串拼接

### 3. 异常处理优化

- 统一的异常处理逻辑，避免重复代码
- Lambda表达式减少匿名内部类开销

## 🚀 扩展指南

### 添加新的指令类型

1. **创建新的包目录**

```bash
mkdir -p src/main/java/.../command/{new_package}
```

2. **创建指令实现类**

```java

@Component
public class VoglanderClient {
    New
}

Command extends

AbstractVoglanderClientCommand {
    public ResultDTO<Void> sendNewCommand (String deviceId, Object param){
        validateDeviceId(deviceId, "设备ID不能为空");
        validateNotNull(param, "参数不能为空");

        return executeCommand("sendNewCommand", deviceId,
                () -> ClientCommandSender.newMethod(getClientFromDevice(), getToDevice(deviceId), param),
                param);
    }
}
```

3. **编写单元测试**

```java
@ExtendWith(MockitoExtension.class)
class VoglanderClient{New}CommandTest {
    // 测试实现
}
```

4. **更新包文档**

- 更新`package-info.java`中的包结构说明
- 添加到测试套件`GB28181ClientCommandTestSuite`

### 最佳实践

1. **命名规范**
    - 类名：`VoglanderClient{模块}Command`
    - 方法名：`send{具体功能}Command`
    - 测试类：`{类名}Test`

2. **方法设计**
    - 提供基础方法和简化方法
    - 参数校验在方法开始
    - 使用工具方法构建复杂对象

3. **文档规范**
    - 完整的JavaDoc文档
    - 包含使用示例
    - 说明参数和返回值

4. **测试规范**
    - 覆盖正常流程和异常情况
    - 包含参数校验测试
    - 使用Mock进行依赖隔离

## 📄 许可证

本项目遵循项目整体的许可证协议。

## 👥 贡献者

- **luna** - 初始实现和文档编写

---

**注意**: 本实现严格遵循企业级代码标准，包含完整的异常处理、日志记录、参数校验和单元测试，可以直接用于生产环境。