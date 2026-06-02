# Voglander 1.0.3 监控与告警

## Actuator 端点

- `GET /actuator/health` — Spring Boot 默认健康检查
- `GET /actuator/metrics` — 全指标列表
- `GET /actuator/metrics/{name}` — 指定指标值
- `GET /actuator/prometheus` — Prometheus 拉取格式
- `GET /api/v1/health` — voglander 自定义依赖级健康（Redis-A/B + DB + ZLM 熔断器）

## 关键指标

### 事件管线（Phase 4）
- `event.shard.queue.size{shard=N}` — 每槽队列深度（Gauge）
- `event.shard.keepalive.dropped` — Keepalive 丢弃总数（Counter）
- `event.shard.processed` — 处理总数（Counter）

### 设备状态（Phase 2a）
- `device.cache.hit.rate` — device 缓存命中率（Spring Cache 自动暴露）
- 心跳 P99 通过 `http.server.requests{uri=/keepalive}` 观察

### 出站命令（Phase 5）
- `command.forward.success{target_node=...}` — 跨节点转发成功次数（Counter）
- `command.forward.fail{target_node=...,reason=...}` — 转发失败次数（Counter）

### ZLM 韧性（Phase 6）
- `resilience4j.circuitbreaker.state{name=zlm}` — 熔断器状态（0=CLOSED, 1=HALF_OPEN, 2=OPEN）
- `resilience4j.circuitbreaker.calls{name=zlm,kind=successful|failed|not_permitted}` — 调用统计
- `resilience4j.bulkhead.available.concurrent.calls{name=zlm}` — 隔离仓可用槽位

### Redis A/B（Phase 6）
- `lettuce.command.completion{command=...}` — Lettuce 客户端指标
- 自定义：`/api/v1/health` 包含 redis-A / redis-B 各自 ping_ms

## Prometheus 告警规则示例

```yaml
groups:
- name: voglander_critical
  rules:
  - alert: ZlmCircuitOpen
    expr: resilience4j_circuitbreaker_state{name="zlm"} == 2
    for: 1m
    annotations:
      summary: "ZLM 熔断器开启 - {{ $labels.instance }}"

  - alert: EventShardQueueHigh
    expr: event_shard_queue_size > 1500
    for: 30s
    annotations:
      summary: "事件分片队列接近溢出（满 2000 即丢 Keepalive）"

  - alert: CommandForwardFailureRateHigh
    expr: rate(command_forward_fail[5m]) / rate(command_forward_success[5m]) > 0.1
    for: 2m
    annotations:
      summary: "跨节点命令转发失败率 > 10%"

  - alert: DeviceCacheHitLow
    expr: device_cache_hit_rate < 0.6
    for: 5m
    annotations:
      summary: "device 缓存命中率持续低于 60%"

  - alert: RedisADown
    expr: up{job="voglander", redis="A"} == 0
    for: 30s
    annotations:
      summary: "Redis-A 不可用 - 核心业务受影响"

  - alert: RedisBDown
    expr: up{job="voglander", redis="B"} == 0
    for: 30s
    annotations:
      summary: "Redis-B 不可用 - INVITE 上下文持久化降级"
```

## SkyWalking 接入

项目已集成 `apm-toolkit-trace` (SkyWalking 9.1.0)。关键链路自动捕获：
- 入站事件管线：`VoglanderBusinessNotifier.notify` → `ShardDispatcher.dispatch` → `EventShard.start` → `Gb28181ProtocolHandler.handle`
- 出站命令：`AbstractVoglanderServerCommand.dispatchEnvelope` → 路由判断 → 本地发送 / HTTP 转发
- ZLM 调用：`StreamProxyZlmWrapperServiceImpl.*` / `PushProxyZlmWrapperServiceImpl.*`（带 Resilience4j 熔断标签）

如需自定义追踪标签，可在方法上添加：
```java
@org.apache.skywalking.apm.toolkit.trace.Trace
@org.apache.skywalking.apm.toolkit.trace.Tag(key = "deviceId", value = "arg[0]")
public void someMethod(String deviceId) { ... }
```

## 容量预算与 SLO

| 指标 | 目标 | 实测渠道 |
|------|------|----------|
| 注册→可用 P99 | < 500ms | `http.server.requests{uri=/login}` |
| 心跳 P99 | < 50ms | `http.server.requests{uri=/keepalive}` |
| REST P99 | < 200ms | `http.server.requests` 整体 |
| 可用性 | 99.9% | `up{job="voglander"}` |
| 事件丢失率 | < 0.01% | `event.shard.keepalive.dropped / event.shard.processed` |
| ZLM 调用失败率 | < 1% | `resilience4j.circuitbreaker.calls{name=zlm,kind=failed}` |

## 灰度开关一览（运维侧）

| 开关 | 默认 | 作用 |
|------|------|------|
| `voglander.event.shard.enabled` | true | Phase 4 事件分片管线 |
| `voglander.command.affinity-route.enabled` | false | Phase 5 跨节点命令路由 |
| `gateway.gb28181.store.type` | memory | Phase 6 Redis-B 切换（memory/redis） |
| `zlm.hook.auth.enabled` | false | Phase 6 ZLM Hook 鉴权 |
| `voglander.stream-proxy.scheduled-sync.enabled` | true | Phase 0 流代理 30s 同步任务 |

紧急回退：任一开关切回 false 即恢复旧路径，无需重新编译。
