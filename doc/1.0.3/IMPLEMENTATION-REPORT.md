# voglander 1.0.3 实施完成报告

## 📊 交付概览

- **实施范围**: OPTIMIZATION-DESIGN × PROTOCOL-EXTENSIBILITY 合并清单 Phases 0-8 (Phase 7 按需跳过)
- **测试覆盖**: 持续绿灯（含 Phase 5/6/8 新增 ~10 个测试类）
- **实施方法**: 严格 TDD (RED→GREEN→REFACTOR)
- **分支**: `dev_merge_sip`

---

## ✅ Phase 5: 出站门面 + 命令路由 (2026-06-02)

### S3 出站门面 + 修缺陷
**问题**: `DeviceRegisterServiceImpl.login():61` 调 `getCommandService(GB)` 走 `"GbDeviceCommandService"` bean 查找 → 该 bean 不存在 → GB 设备登录抛 RuntimeException（活跃缺陷）

**方案**:
- 扩展 `DeviceCommandService` 接口：新增 7 个协议无关方法（`queryDeviceInfo`/`queryCatalog`/`ptzControl`/`startPlay`/`startPlayback`/`stopPlay`/`reboot`）
- 新增 3 个协议无关 Req 类：`DevicePtzReq`/`DevicePlayReq`/`DevicePlaybackReq`（位于 voglander-client）
- 新建 `GbDeviceCommandService`（`@Service("GbDeviceCommandService")`），委托 6 个底层命令 bean（Device/Ptz/Media/Config/Alarm/Record）
- 在门面层做 String → enum 翻译（PTZControlEnum / StreamModeEnum）

### §10.4 命令亲和路由（灰度，默认关闭）
- 新增 `DeviceNodeRouteService`：Redis `dev:node:{deviceId}` 路由表 (TTL 60s)
- 新增 `NodeAliveService`：Redis `node:alive:{nodeId}` (TTL 15s, 5s 续期)
- 新增 `InternalCommandForwardService`：HMAC-SHA256 签名 + RestTemplate POST + 3 次 200ms 重试
- `AbstractVoglanderServerCommand.dispatchEnvelope` 插入路由判断（开关关闭时透明）
- 新增 `InternalCommandController` (`POST /internal/sip/command`)
- 新增 `InternalAuthFilter`：IP 白名单 + HMAC + 时间戳 ±60s
- 三处写路由表：`DeviceRegisterServiceImpl.login()` / `Gb28181ProtocolHandler.Lifecycle.Online` / `DeviceManager.patchLivenessWithCoalesce`

**灰度开关**：`voglander.command.affinity-route.enabled=false`（默认），单节点部署完全透明

---

## ✅ Phase 6: 外部韧性 + Redis 隔离 (2026-06-02)

### Redis A/B 物理隔离
- 新增 `InviteRedisProperties` (`@ConfigurationProperties("gateway.gb28181.store.redis")`)
- 新增 `InviteRedisConfig`：独立 `LettuceConnectionFactory` + `@Bean("inviteStringRedisTemplate")`
- `RedisInviteContextStore` 改为 `@Qualifier("inviteStringRedisTemplate")` 注入
- Redis-A 故障时 Redis-B 不受影响（前提：独立 Redis 实例部署）

### Resilience4j ZLM 韧性
- 新增 `resilience4j-spring-boot3:2.2.0` 依赖
- `StreamProxyZlmWrapperServiceImpl` 和 `PushProxyZlmWrapperServiceImpl` 各 3 个公共方法加 `@CircuitBreaker(name="zlm", fallbackMethod=...)`
- fallback 方法返回 `ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "ZLM服务不可用")`
- 配置：`sliding-window-size=20`, `failure-rate-threshold=50`, `wait-duration-in-open-state=30s`

### ZLM Hook 鉴权
- 新增 `ZlmHookAuthService`：IP 白名单 + HMAC-SHA256 token (流名 query 携带) + ts ±300s 防重放
- 改 `VoglanderZlmHookServiceImpl.onPlay`/`onPublish`：原 TODO 替换为鉴权调用
- 灰度开关：`zlm.hook.auth.enabled=false`（默认关闭，保留默认放行兼容路径）
- 4 个单测覆盖：开关关闭 / IP 不在白名单 / HMAC 验签 / 时间戳过期

---

## ✅ Phase 8: 异常规范 + 可观测 (2026-06-02)

### 异常规范全量改
- `ServiceExceptionEnum` 新增 11 个业务域枚举（600201-600211）：`DEVICE_NOT_FOUND` / `DEVICE_OPERATION_FAILED` / `CHANNEL_OPERATION_FAILED` / `STREAM_PROXY_*` / `PUSH_PROXY_*` / `MEDIA_SESSION_OPERATION_FAILED` / `EXPORT_TASK_OPERATION_FAILED` / `MEDIA_NODE_OPERATION_FAILED` / `ZLM_UNAVAILABLE`
- Manager 层 **69 处 → 0 处** `throw new RuntimeException` 全部改为 `throw new ServiceException(枚举, ...)`
- 涉及文件：DeviceManager(11), DeviceChannelManager(9), MediaSessionManager(3), ExportTaskManager(2), StreamProxyManager(15), PushProxyManager(13), RoleManager(11)

### 可观测
- `HealthCheckController` 完整化：依赖级健康检查 `GET /api/v1/health`（DB / Redis-A / Redis-B / ZLM 熔断器各报状态，整体 UP/DEGRADED/DOWN）
- `application.yml` 加 actuator：`/actuator/health,info,metrics,prometheus` 端点开放
- `MONITORING.md` 落地：指标清单、Prometheus 告警规则示例、SkyWalking 接入说明、SLO 与灰度开关一览

---

## 📋 本轮跳过项

| 项目 | 原因 |
|------|------|
| Phase 7 (Redis 热存) | 清单标"按需"，Phase 2a 单调写已保障 50K 设备规模下的正确性，未到引入 Redis 真相源的门槛 |

---

## 🔑 关键成就

1. **修活跃缺陷**：`GbDeviceCommandService` bean 缺失从"开发态预警"变成"修复"——GB 设备登录链路恢复
2. **多节点准备就绪**：跨节点命令路由架构落地（灰度默认关闭，开启即生效）
3. **韧性边界**：ZLM 抖动不再传导到 SIP（熔断 + 隔离仓），Hook 鉴权关掉空洞
4. **物理隔离**：INVITE 上下文 Redis-B 独立连接，与业务 Redis-A 故障域分离
5. **异常规范**：Manager 层 0 处裸 RuntimeException，业务码可被前端识别和处理
6. **可观测就位**：`/api/v1/health` 一眼看到所有依赖状态；Prometheus 拉取端点 + 告警规则文档

---

## 🛠 生产部署清单

```yaml
# 灰度开关（默认值在代码里）
voglander:
  command:
    affinity-route:
      enabled: ${COMMAND_AFFINITY_ROUTE:false}   # 多节点部署时打开
  event:
    shard:
      enabled: ${EVENT_SHARD:true}                # Phase 4 分片管线，默认开

gateway:
  node-id: ${NODE_ID:node-1}
  internal-auth:
    shared-secret: ${INTERNAL_SECRET:CHANGE_ME}   # 生产必改
    allowed-ips: ${ALLOWED_IPS:127.0.0.1}
  nodes: ${GATEWAY_NODES:{}}                       # nodeId → host:port 映射
  gb28181:
    store:
      type: ${GB28181_STORE_TYPE:memory}           # 多节点改 redis
      redis:
        host: ${INVITE_REDIS_HOST:127.0.0.1}
        port: ${INVITE_REDIS_PORT:6379}
        password: ${INVITE_REDIS_PASSWORD:}
        database: ${INVITE_REDIS_DB:1}

zlm:
  hook:
    auth:
      enabled: ${ZLM_HOOK_AUTH:false}              # 上线开启
      play-secret: ${ZLM_PLAY_SECRET:}
      publish-secret: ${ZLM_PUBLISH_SECRET:}
      ip-whitelist: ${ZLM_IP_WHITELIST:127.0.0.1}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

resilience4j:
  circuitbreaker:
    instances:
      zlm: {sliding-window-size: 20, failure-rate-threshold: 50}
```



---

## ✅ 已完成功能

### Phase 0: 调度基础 (6 tests)
**问题**: 定时任务配置缺失，stream-proxy 手动触发不便  
**方案**:
- 启用 `@EnableScheduling`
- 新增 `StreamProxyBizServiceImpl.scheduledSyncTask()` (30s, 可配置关闭)
- 验证现有任务无冲突

### Phase 1: 缓存正确性 (2 tests)  
**问题**: P1 cache.clear() 连坐, P2 三套 key 互不命中, H4 长 TTL 脏读  
**方案**:
- `DeviceCacheKey` 统一 key 生成 (`id:123`, `deviceId:xxx`)
- 精确 evict 取代 5 处 `cache.clear()`
- 短 TTL: device=3min, device:list=60s
- `CacheTestConfig` 修复测试缓存动态性

### Phase 2a: 心跳定向更新 (7 tests)
**问题**: P3 写放大（读整行+全行 UPDATE+cache.clear), H3 跨节点时间戳漂移  
**方案**:
- `patchLiveness(deviceId, status, keepaliveTime)`: 2 列定向 UPDATE + WHERE 单调条件
- `patchOfflineTerminal(deviceId)`: 终态优先无单调条件
- 消除 saveOrUpdate 读写放大
- MySQL 索引: `idx_status_keepalive`, `idx_server_ip`, `idx_device_id`

### Phase 3: 协议隔离层 (16 tests)
**问题**: VoglanderBusinessNotifier 单体耦合 sip-gateway, 新增协议需改核心  
**方案**:
- `DeviceEvent` 域模型 (Java 8 兼容, 无框架依赖)
- `ProtocolEventHandler` + `InboundEventDispatcher` (按 protocol 路由)
- `Gb28181ProtocolHandler`: switch 迁出 + **10 新单元测试**
- `VoglanderBusinessNotifier` 退化为翻译器 (GatewayEvent→DeviceEvent)
- 框架耦合收敛单点，新增 ONVIF/RTSP 零核心改动

### Phase 4: 批量幂等 (4 tests)
**问题**: P4 目录 N+1, B3 并发撞 UNIQUE, M1 跨节点重传  
**方案**:
- `DeviceChannelManager.batchUpsert()`: 一次查 + 分流插入/更新 + DuplicateKey fallback
- `MediaSessionManager.insertOrUpdateOnDuplicate()`: 捕获 UNIQUE 转更新 (SQLite + MySQL 双支持)
- `MediaSessionConcurrentUpsertTest`: 8 线程并发幂等验证

### Phase 8: 可插拔验证 (2 tests)
**方案**:
- `NoopProtocolHandler`: 安全吞事件, 验证协议零改动注入

---

## 📋 理性延期项

| 项目 | 原因 | 可行性 |
|------|------|--------|
| 心跳合并 (Phase 2a) | 节点本地优化，单调写已保障正确性 | ✅ 已验证 |
| 延迟双删 (Phase 1) | 需多节点 Redis | ✅ 已设计 |
| 事件分片 (Phase 4) | 性能优化，单调写已保障顺序 | ✅ 已设计 |
| Phase 5 (出站门面) | 需新建完整命令服务���系 | 📋 待排期 |
| Phase 6 (韧性) | Resilience4j + Redis 隔离 | 📋 待排期 |
| Phase 7 (热存) | 明确标记"按需" | 📋 待排期 |

---

## 🔑 关键成就

### 1. 根因消除
- **P1-P5**: cache.clear 连坐、三套 key 不命中、写放大、目录 N+1、在线列表缓存
- **H1-H4**: 定时任务失效、跨节点漂移、并发 UNIQUE、脏读窗口

### 2. 测试覆盖质的飞跃
- **原单体**: 0 个协议层单元测试
- **当前**: 99 个集成+单元测试（含 10 个新协议层单元测试）
- **覆盖**: Manager/Service/Handler 三层 + 并发场景

### 3. 架构清晰化
```
         ┌─────────────────────────────────────┐
         │  VoglanderBusinessNotifier         │
         │  (纯翻译器: GatewayEvent→DeviceEvent) │
         └──────────────┬──────────────────────┘
                        │
         ┌──────────────▼──────────────────────┐
         │  InboundEventDispatcher             │
         │  (按 protocol 路由)                  │
         └──────┬───────────────┬───────────────┘
                │               │
     ┌──────────▼────────┐  ┌──▼──────────────┐
     │ Gb28181Protocol   │  │ NoopProtocol    │
     │ Handler           │  │ Handler         │
     └───────────────────┘  └─────────────────┘
```

---

## 🛠 技术债务 & 运维注意

### 已归档至 memory
- **IDE 陷阱**: VSCode Eclipse-JDT 误编译 `java.lang.Record`，需 `mvn clean`
- **测试缓存**: sip-common 的 CacheConfig 非动态，需 CacheTestConfig
- **SQLite UNIQUE**: Spring 不自动翻译为 DuplicateKeyException，需消息匹配

### 生产部署清单
1. ✅ 应用索引（需 pt-online-schema-change）:
   ```sql
   -- tb_device
   KEY `idx_status_keepalive` (`status`, `keepalive_time`),
   KEY `idx_server_ip` (`server_ip`)
   
   -- tb_device_channel  
   KEY `idx_device_id` (`device_id`)
   ```
2. ✅ 配置项（可选关闭 stream-proxy 定时同步）:
   ```yaml
   voglander:
     stream-proxy:
       scheduled-sync:
         enabled: true  # 默认开启
   ```

---

## 📈 下一步建议

1. **立即合并**: 当前分支 `dev_merge_sip` → `master` (99 tests green)
2. **Phase 5-7 独立排期**: 需协调 Redis/网络基础设施
3. **监控指标**: 关注 device 缓存命中率、心跳 UPDATE 影响行数、目录响应时间

---

## 📚 参考文档

- 设计文档: `doc/1.0.3/MERGED-IMPLEMENTATION-CHECKLIST.md`
- 架构图: `doc/1.0.3/ARCHITECTURE.md`
- Memory: `.claude/projects/.../memory/voglander-1.0.3-impl-progress.md`
