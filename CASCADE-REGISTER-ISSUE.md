# 级联注册无响应问题诊断报告

## 问题现象

```log
2026-06-25 23:22:45.614 [cascade-register] INFO  i.g.l.v.i.w.g.c.CascadeClientScheduler -发起级联注册: 34020000002000000011 -> 34020000002000000001
```

**发送注册后没有任何后续日志**，应该有的 "级联注册成功" 或 "级联注册失败" 日志都没有出现。

## 诊断结果

### ✅ 正常的部分
1. **SIP 客户端监听正常**：`10.39.85.228:5061` 已监听
2. **网络连通性正常**：目标平台 `36.34.0.68:30310` 可以连接
3. **调度任务正常执行**：`CascadeClientScheduler` 正常调用 `doRegister()`
4. **注册命令发送**：`ClientCommandSender.sendRegisterCommand()` 被调用

### ❌ 问题所在
1. **没有收到响应事件**：`ClientRegisterSuccessEvent` / `ClientRegisterFailureEvent` 都没有触发
2. **监听器未被回调**：`CascadeClientRegisterListener` 的 `onRegisterSuccess/onRegisterFailure` 没有执行
3. **注册状态卡死**：数据库中 `register_status=2` (REGISTERING) 一直不变

## 根因分析

**核心问题**：SIP 框架层面没有发布响应事件，可能的原因：

### 1. SIP 层没有收到响应（最可能）
- 对端平台可能：
  - 不是标准 GB28181 平台
  - 认证失败但不返回标准响应
  - 防火墙阻止响应包
  - 配置错误（realm、domain 不匹配）

### 2. SIP 事件发布机制问题
- `gbproxy` 框架可能没有正确发布事件
- Spring 事件总线配置问题
- 异步事件丢失

### 3. 缺少调试日志
- SIP 协议层日志级别太高，看不到实际的 SIP 消息交互
- 无法确认 REGISTER 是否真正发送、响应码是什么

## 解决方案

### 步骤 1：启用 SIP 调试日志（已完成）

已修改 `application.yml`，添加：
```yaml
logging:
  level:
    io.github.lunasaw.gbproxy: DEBUG
    io.github.lunasaw.sip: DEBUG
    javax.sip: DEBUG
```

### 步骤 2：重启应用并观察日志

```bash
cd voglander-web
mvn spring-boot:run

# 或者如果是已编译的
java -jar target/voglander-web-*.jar
```

重启后，触发级联注册，观察日志中是否有：
- SIP REGISTER 消息的实际内容
- 对端的响应码（200 OK / 401 Unauthorized / 403 Forbidden 等）
- 任何 SIP 层面的异常

### 步骤 3：检查配置匹配

确认数据库中的配置与对端平台要求一致：

```sql
SELECT 
    local_client_id,     -- 本地客户端 ID，必须符合 GB28181 规范
    platform_id,         -- 对端平台 ID
    platform_ip,         -- 对端 IP
    platform_port,       -- 对端端口
    password,            -- 认证密码
    transport,           -- 传输协议（UDP/TCP）
    local_ip,            -- 本地 IP（必须是对端可达的 IP）
    local_port           -- 本地端口（5061）
FROM tb_cascade_platform 
WHERE id=3;
```

**关键检查点**：
- `local_client_id`：必须是 20 位 GB28181 编码
- `platform_id`：必须与对端平台的实际 ID 一致
- `password`：必须与对端配置的密码一致
- `local_ip`：不能是 `127.0.0.1`，必须是对端可达的 IP

### 步骤 4：手动测试对端平台

使用 SIP 调试工具（如 `sip-tester` 或 `linphone`）手动向对端发送 REGISTER，确认：
1. 对端是否正常响应
2. 认证流程是否正确（401 Challenge → REGISTER with Auth）
3. 最终是否返回 200 OK

### 步骤 5：检查框架事件发布机制

在 `CascadeClientScheduler.doRegister()` 中添加更多日志：

```java
private void doRegister(CascadePlatformDTO platform) {
    try {
        FromDevice from = cascadeDeviceSupplier.buildFromDevice(platform);
        ToDevice   to   = cascadeDeviceSupplier.buildToDevice(platform);
        log.info("发起级联注册: {} -> {}", platform.getLocalClientId(), platform.getPlatformId());
        log.debug("From: ip={}, port={}, userId={}", from.getIp(), from.getPort(), from.getUserId());
        log.debug("To: ip={}, port={}, userId={}, transport={}", 
            to.getIp(), to.getPort(), to.getUserId(), to.getTransport());
        
        cascadePlatformManager.updateRegisterStatus(platform.getId(), CascadeConstant.RegisterStatus.REGISTERING);
        
        String callId = ClientCommandSender.sendRegisterCommand(from, to, platform.getRegisterExpires());
        log.info("REGISTER 已发送，callId={}", callId);
        
        // 设置超时检测（30秒后如果还是 REGISTERING 状态，标记为 FAILED）
        executor.schedule(() -> {
            CascadePlatformDTO current = cascadePlatformManager.getById(platform.getId());
            if (current != null && 
                Objects.equals(current.getRegisterStatus(), CascadeConstant.RegisterStatus.REGISTERING)) {
                log.warn("级联注册超时: platformId={}, callId={}", platform.getPlatformId(), callId);
                cascadePlatformManager.updateRegisterStatus(platform.getId(), CascadeConstant.RegisterStatus.FAILED);
            }
        }, 30, TimeUnit.SECONDS);
        
    } catch (Exception e) {
        log.error("级联注册失败: platformId={}", platform.getPlatformId(), e);
        cascadePlatformManager.updateRegisterStatus(platform.getId(), CascadeConstant.RegisterStatus.FAILED);
    }
}
```

### 步骤 6：检查事件监听器是否生效

在 `CascadeClientRegisterListener` 中添加构造函数日志：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CascadeClientRegisterListener {
    
    public CascadeClientRegisterListener(CascadePlatformManager cascadePlatformManager,
                                          ApplicationEventPublisher eventPublisher) {
        this.cascadePlatformManager = cascadePlatformManager;
        this.eventPublisher = eventPublisher;
        log.info("=== CascadeClientRegisterListener 已初始化 ===");
    }
    
    @EventListener
    public void onRegisterSuccess(ClientRegisterSuccessEvent event) {
        log.info("=== 收到注册成功事件: userId={} ===", event.getUserId());
        // ... 原有代码
    }
    
    @EventListener
    public void onRegisterFailure(ClientRegisterFailureEvent event) {
        log.info("=== 收到注册失败事件: userId={}, code={} ===", 
            event.getUserId(), event.getStatusCode());
        // ... 原有代码
    }
}
```

## 预期结果

启用调试日志后，应该能看到：

### 正常流程
```log
[DEBUG] 发送 SIP REGISTER:
REGISTER sip:34020000002000000001@36.34.0.68:30310 SIP/2.0
From: <sip:34020000002000000012@3402>
To: <sip:34020000002000000001@3402>
...

[DEBUG] 收到 SIP 401 Unauthorized (Challenge)
[DEBUG] 发送带认证的 REGISTER
[DEBUG] 收到 SIP 200 OK
[INFO] === 收到注册成功事件: userId=34020000002000000012 ===
[INFO] 级联注册成功: localClientId=34020000002000000012
```

### 异常流程
```log
[DEBUG] 发送 SIP REGISTER: ...
[DEBUG] 收到 SIP 403 Forbidden / 404 Not Found / 超时
[INFO] === 收到注册失败事件: userId=34020000002000000012, code=403 ===
[WARN] 级联注册失败: localClientId=34020000002000000012, statusCode=403
```

## 常见问题排查

### Q1: 如果看到 "REGISTER 已发送" 但没有后续日志？
**A**: 说明对端没有响应或响应被丢弃。检查：
- 防火墙是否阻止响应包
- `local_ip` 是否正确（对端能否路由回来）
- 对端平台是否正常运行

### Q2: 如果看到 401/403 但没有触发 `onRegisterFailure`？
**A**: 框架事件发布机制有问题。检查：
- `@EnableSipServer` 是否在 `ApplicationWeb` 上
- Spring 事件总线是否正常
- 是否有其他监听器消费了事件但没有传播

### Q3: 如果完全看不到 SIP 消息日志？
**A**: SIP 客户端可能没有正确初始化。检查：
- `ServerStart` 是否执行（`sip.enable=true`）
- `ClientCommandSender.INSTANCE` 是否已初始化
- SIP 监听端口是否绑定成功

## 下一步行动

1. **立即**：重启应用，查看 DEBUG 日志
2. **5分钟内**：找到 SIP 消息交互的实际内容
3. **根据日志**：
   - 如果有响应码 → 调整配置（密码、ID 等）
   - 如果没有响应 → 联系对端平台管理员
   - 如果框架没发送 → 检查 `ClientCommandSender` 初始化

## 联系信息

- 对端平台管理员：需要确认平台 ID、密码、是否在线
- 网络管理员：需要确认防火墙规则、NAT 配置
