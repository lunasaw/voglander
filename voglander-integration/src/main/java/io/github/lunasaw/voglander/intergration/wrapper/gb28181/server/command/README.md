# GB28181服务端指令集成

本文档说明GB28181服务端指令业务接入的实现，该实现参照客户端接入方式，为Voglander系统提供了完整的GB28181服务端指令封装。

## 项目结构

```
voglander-integration/src/main/java/io/github/lunasaw/voglander/intergration/wrapper/gb28181/server/command/
├── AbstractVoglanderServerCommand.java          # 抽象基类
├── package-info.java                           # 包说明文档
├── alarm/
│   └── VoglanderServerAlarmCommand.java        # 告警查询指令
├── config/
│   └── VoglanderServerConfigCommand.java       # 设备配置指令
├── device/
│   └── VoglanderServerDeviceCommand.java       # 设备查询指令
├── media/
│   └── VoglanderServerMediaCommand.java        # 媒体流指令
├── ptz/
│   └── VoglanderServerPtzCommand.java          # 云台控制指令
└── record/
    └── VoglanderServerRecordCommand.java       # 录像查询指令
```

## 实现特点

### 1. 统一的设计模式

- **抽象基类**: `AbstractVoglanderServerCommand` 提供通用功能
- **模板方法**: 统一的指令执行模板和异常处理
- **参数校验**: 统一的参数验证机制
- **日志记录**: 完整的执行日志和错误日志

### 2. 按功能模块分类

- **设备管理** (`device`): 设备信息、状态、目录查询
- **云台控制** (`ptz`): 云台移动、变焦、预置位控制
- **录像管理** (`record`): 录像查询、录像控制
- **告警管理** (`alarm`): 告警查询、告警控制
- **设备配置** (`config`): 参数配置、配置下载、设备重启
- **媒体流** (`media`): 实时流、回放流、会话控制

### 3. 统一的返回格式

- 所有方法返回 `ResultDTO<Void>` 格式
- 统一的成功/失败状态处理
- 详细的错误信息和异常堆栈

## 使用示例

### 基本设备操作

```java
@Autowired
private VoglanderServerDeviceCommand deviceCommand;

// 查询设备信息
ResultDTO<Void> result = deviceCommand.queryDeviceInfo("34020000001320000001");

// 查询设备状态
ResultDTO<Void> status = deviceCommand.queryDeviceStatus("34020000001320000001");

// 查询设备目录
ResultDTO<Void> catalog = deviceCommand.queryDeviceCatalog("34020000001320000001");
```

### 云台控制

```java
@Autowired
private VoglanderServerPtzCommand ptzCommand;

// 向上移动（指定速度）
ResultDTO<Void> moveResult = ptzCommand.moveUp("34020000001320000001", 128);

// 放大变焦（默认速度）
ResultDTO<Void> zoomResult = ptzCommand.zoomIn("34020000001320000001");

// 停止云台移动
ResultDTO<Void> stopResult = ptzCommand.stopDevicePtz("34020000001320000001");
```

### 录像查询

```java
@Autowired
private VoglanderServerRecordCommand recordCommand;

// 查询今日录像
ResultDTO<Void> todayRecord = recordCommand.queryTodayDeviceRecord("34020000001320000001");

// 查询指定时间范围录像
Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
Date endTime = new Date();
ResultDTO<Void> rangeRecord = recordCommand.queryDeviceRecord("34020000001320000001", startTime, endTime);

// 开始录像
ResultDTO<Void> startRecord = recordCommand.startDeviceRecord("34020000001320000001");
```

### 媒体流操作

```java
@Autowired
private VoglanderServerMediaCommand mediaCommand;

// 请求实时流
ResultDTO<Void> liveStream = mediaCommand.inviteRealTimePlay("34020000001320000001", 
    "192.168.1.100", 10000);

// 请求回放流
ResultDTO<Void> playback = mediaCommand.invitePlayBack("34020000001320000001", 
    "192.168.1.100", 10000, "2024-01-01T08:00:00", "2024-01-01T18:00:00");

// 控制回放播放
ResultDTO<Void> control = mediaCommand.playBack("34020000001320000001");
```

### 设备配置

```java

@Autowired
private VoglanderServerConfigCommand configCommand;

// 配置设备基本参数
ResultDTO<Void> config = configCommand.configDevice("34020000001320000001", "摄像头01");

// 下载设备配置
ResultDTO<Void> download = configCommand.downloadBasicConfig("34020000001320000001");

// 重启设备
ResultDTO<Void> reboot = configCommand.rebootDevice("34020000001320000001");
```

### 告警管理

```java

@Autowired
private VoglanderServerAlarmCommand alarmCommand;

// 查询今日告警
ResultDTO<Void> todayAlarm = alarmCommand.queryTodayDeviceAlarm("34020000001320000001");

// 查询紧急告警
Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
Date endTime = new Date();
ResultDTO<Void> emergency = alarmCommand.queryEmergencyDeviceAlarm("34020000001320000001",
        startTime, endTime);

// 启用网络告警
ResultDTO<Void> enableAlarm = alarmCommand.enableNetworkAlarm("34020000001320000001", "1");
```

## 集成说明

### 1. 依赖关系

本实现基于以下模块：

- `sip-proxy/gb28181-server` - 提供底层GB28181服务端命令发送器
- `voglander-integration` - Voglander集成层，提供统一的外部系统封装

### 2. 配置要求

- 需要配置 `ServerDeviceSupplier` 以提供服务端设备信息
- 确保GB28181服务端正确初始化和配置
- 需要有效的SIP连接和设备注册

### 3. 异常处理

- 所有指令都有完整的异常处理机制
- 参数校验失败会抛出 `IllegalArgumentException`
- 执行失败会返回包含错误信息的 `ResultDTO`

### 4. 日志记录

- DEBUG级别：记录指令开始执行的详细参数
- INFO级别：记录指令执行成功的结果
- ERROR级别：记录指令执行失败的详细错误信息

## 扩展指南

### 添加新的指令类型

1. 在相应的功能包下创建新的指令类
2. 继承 `AbstractVoglanderServerCommand`
3. 实现具体的业务方法
4. 使用 `@Component` 注解标记为Spring组件

```java
@Component
public class VoglanderServerCustomCommand extends AbstractVoglanderServerCommand {
    
    public ResultDTO<Void> customCommand(String deviceId, Object params) {
        validateDeviceId(deviceId, "自定义指令时设备ID不能为空");
        
        return executeCommand("customCommand", deviceId,
            () -> ServerCommandSender.customMethod(getServerFromDevice(), getToDevice(deviceId), params),
            deviceId, params);
    }
}
```

### 添加新的功能模块

1. 在 `command` 包下创建新的子包
2. 实现相应的指令类
3. 更新包文档说明

## 注意事项

1. **设备ID格式**: 确保设备ID符合GB28181标准格式
2. **时间格式**: 时间字符串需要符合 `yyyy-MM-ddTHH:mm:ss` 格式
3. **参数范围**: 云台速度等参数需要在有效范围内（1-255）
4. **网络配置**: 确保SDP IP地址和媒体端口配置正确
5. **并发安全**: 所有指令方法都是线程安全的

## 参考资料

- [GB28181-2016标准](http://www.gb688.cn/bzgk/gb/newGbInfo?hcno=469659DC56B9B8187AD20A4B9FA76C9F)
- [SIP-proxy项目文档](../../../../../../../sip-proxy/README.md)
- [Voglander集成文档](../../README.md)