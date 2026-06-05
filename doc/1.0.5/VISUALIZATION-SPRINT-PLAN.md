# GB28181 可视化 Sprint 任务分解（1.0.5）

> TDD 原则：先写测试（红）→ 最小实现（绿）→ 重构；每个 commit 必须编译通过且测试通过。
> 详细实现参见 [VISUALIZATION-BACKEND-IMPL.md](VISUALIZATION-BACKEND-IMPL.md)。

---

## Sprint 1 — 直播闭环（P0）

**目标**：前端能发 `/live/start` 拿到 FLV 地址并播放，第二观看者秒开，停流引用计数正确归零。

### 任务清单

| ID | 任务 | 模块 | 依赖 | 测试类 |
|----|------|------|------|--------|
| S1-1 | **【前置】** `Gb28181ProtocolHandler` InviteOk 提取 `channelId`；`MediaSessionManager.onInviteOk` 增加 channelId 参数，按 (deviceId,channelId,INVITING) 匹配占位行回填 callId | integration / manager | — | `MediaSessionManagerTest`（扩展：onInviteOk 3参数 + 占位行回填） |
| S1-2 | `tb_media_session` 增列 `stream_id/node_server_id/ref_count` + 索引；同步 `sql/voglander.sql`、`schema-sqlite.sql`、`test-app.db`（手工 sqlite3）、`MediaSessionDO` | repository | S1-1 完成后 | 无新测试；S1-1 测试兜底 |
| S1-3 | `tb_alarm` 建表（两处 schema + SQLite）；`AlarmDO/AlarmMapper/AlarmService/AlarmManager/AlarmDTO/AlarmAssembler`（模板方法） | repository/service/manager | — | `AlarmManagerTest`（BaseTest：CRUD + ack） |
| S1-4 | `ServiceExceptionEnum` 补充 5 个 live 域枚举（700001-700005） | common | — | 无（枚举常量） |
| S1-5 | `LiveSessionInfo`（值对象）+ `LiveStreamRegistry` 接口 + `RedisLiveStreamRegistry` 实现（StringRedisTemplate INCR/DECR Lua 脚本） | service | S1-4 | `LiveStreamRegistryTest`（单测：8 线程并发 incRef=8，decRef 不低于 0，completeFuture 唤醒） |
| S1-6 | `SseEvent` + `SseEventBus` 接口 + `RedisBackedSseEventBus` 实现（本地 emitter + Redis Pub/Sub + 15s 心跳） | service | — | `SseEventBusTest`（单节点集成：publish → 同 topic emitter 收到；不同 topic 不收） |
| S1-7 | `MediaPlayService` 接口 + `MediaPlayServiceImpl.startLive/stopLive/getLive/keepAlive`（选节点→resolveMediaIp→openRtp→INVITING 占位→future→inviteRealTimePlay→等就绪→PlayUrls→Registry） | service | S1-1…S1-6 | `MediaPlayServiceIntegrationTest`（BaseTest：首播建会话；第二次调用 refCount+1；stopLive refCount 归零） |
| S1-8 | `VoglanderZlmHookService.onStreamChanged` 流上线分支：`liveStreamRegistry.completeFuture(streamId)` + `sseEventBus.publish(live.ready)` | integration | S1-5/S1-6 | 补充 `VoglanderZlmHookServiceImplTest` mock 断言 completeFuture 调用 |
| S1-9 | `LiveStartReq/LiveStopReq/LivePlayVO/LiveWebAssembler/LivePlayController`（5 个接口） | web | S1-7 | `LivePlayControllerTest`（MockMvc 纯 Mockito：start/stop/get 参数校验 + 返回结构） |
| S1-10 | `SseController`（SSE 订阅端点，内部校验 query token） | web | S1-6 | `SseControllerTest`（MockMvc：无 token→401；有效 token→返回 SseEmitter） |

**Sprint 1 验收标准**：
- [ ] `mvn clean test` 全绿（Redis 需运行）
- [ ] 前端发 `POST /api/v1/live/start` → ≤8s 返回 httpFlv 地址
- [ ] 同通道第二次 start → refCount=2，立即返回缓存地址
- [ ] `POST /api/v1/live/stop` → refCount=1 → 再 stop → refCount=0 → 30s 后 BYE 发出
- [ ] SSE `/api/v1/stream/events?token=xxx&topics=live` 收到 `live.ready` 事件

---

## Sprint 2 — 控制 + 安全（P0 安全 + P1 控制）

**目标**：无 token 访问 API 返回 401；PTZ 控制设备转动；回放可拖进度倍速。

### 任务清单

| ID | 任务 | 模块 | 依赖 | 测试类 |
|----|------|------|------|--------|
| S2-1 | `JwtAuthInterceptor`（Bearer + query token）；修改 `ResourcesConfig.addInterceptors`（注册 JWT 拦截器 + 白名单）；修改 CORS（从配置读 `voglander.cors.allowed-origins`，dev profile 保持 `*`，生产收敛） | web | — | `JwtAuthInterceptorTest`（MockMvc：无 token→401；login 白名单→200；health→200；Bearer 有效→放行） |
| S2-2 | `PtzControlReq/PtzStopReq/PresetReq/PtzWebAssembler/PtzController`（control/stop/preset 三接口） | web | — | `PtzControllerTest`（MockMvc Mockito：参数校验；command 枚举非法值→400） |
| S2-3 | `DeviceCommandService` 补充 `ptzPreset` 方法（SET/GOTO/DEL 三个 action 调 `VoglanderServerPtzCommand`）；`GbDeviceCommandService` 实现 | client/service | — | 补充 `GbDeviceCommandServiceTest`（Mockito：3 种 action 均路由到 ptzCommand） |
| S2-4 | `PlaybackStartReq/PlaybackControlReq/RecordQueryReq/LiveWebAssembler（回放部分）/PlaybackController` | web | S2-5 | `PlaybackControllerTest`（MockMvc Mockito） |
| S2-5 | `MediaPlayService.startPlayback/stopPlayback/controlPlayback`（独立 streamId 含 ts；controlPlayback 调 `VoglanderServerMediaCommand.controlPlayBack` / `ZlmRestService.seekRecordStamp/setRecordSpeed`） | service | S1-7 | `PlaybackServiceIntegrationTest`（BaseTest：独立 streamId 不复用；control PAUSE/PLAY/SEEK/SPEED 均发出命令） |
| S2-6 | `DeviceCmdController`（queryCatalog/queryInfo/reboot/record）；reboot 记录操作日志（log.info AUDIT 行） | web | — | `DeviceCmdControllerTest`（MockMvc Mockito：reboot 调用验证 + 日志） |
| S2-7 | PlayUrls 防盗链：`VoglanderZlmHookService.onPlay` 接入 `ZlmHookAuthService.validatePlay`（已实现，确认接线正确） | integration | — | `ZlmHookAuthServiceTest`（已有，补充 onPlay 集成路径断言） |

**Sprint 2 验收标准**：
- [ ] 未带 token 访问 `/api/v1/device/*` 返回 401
- [ ] 登录后带 token，PTZ UP 指令发出（抓包/日志验证）
- [ ] 回放 start → control SEEK → ZLM `seekRecordStamp` 被调用
- [ ] 生产 profile CORS 仅允许白名单 origin

---

## Sprint 3 — 告警 + 可靠性 + 前端完善（P1/P2）

**目标**：告警实时弹窗+落库；ZLM 节点故障流自动迁移；监控墙 16 屏稳定。

### 任务清单

| ID | 任务 | 模块 | 依赖 | 测试类 |
|----|------|------|------|--------|
| S3-1 | `AlarmWebAssembler/AlarmQueryReq/AlarmVO/AlarmListResp/AlarmController`（getPage/get/ack） | web | S1-3 | `AlarmControllerTest`（MockMvc Mockito） |
| S3-2 | `Gb28181ProtocolHandler.handleAlarm` 改为落库（调 `alarmManager.add`）+ `sseEventBus.publish(alarm.new)` | integration | S1-3/S1-6 | 补充 `Gb28181ProtocolHandlerTest`（Mockito：Alarm 事件 → alarmManager.add 被调用 + sseEventBus.publish） |
| S3-3 | `LiveSessionGcService`（@Scheduled 60s：INVITING 超时标 FAILED；ACTIVE 但 ZLM 无流标 CLOSED；drainPendingClose BYE） | service | S1-7/S1-8 | `LiveSessionGcServiceTest`（集成：注入 fake ZLM 响应；INVITING 超时行 → FAILED；ACTIVE 无流行 → CLOSED + SSE） |
| S3-4 | 节点故障流迁移：`VoglanderZlmHookService.onServerExited` 扫节点 ACTIVE 会话 → 批量标 CLOSED + SSE `live.closed`（前端重新 start） | integration | S1-6 | `VoglanderZlmHookServiceImplTest`（补充 onServerExited 断言） |
| S3-5 | 前端：设备管理/通道树/监控墙/回放/告警中心页面 + SSE pinia store 接线（前端任务，后端配合接口联调） | vue-vben-admin | Sprint 1/2 后端就绪 | E2E（Playwright，验收手动） |
| S3-6 | 优雅停机：`ApplicationWeb` 实现 `DisposableBean` 或 `@PreDestroy`，BYE 本节点所有 ACTIVE 会话，关闭所有 SseEmitter | web/service | S1-7 | 手工验证（kill 进程，日志确认 BYE 发出） |

**Sprint 3 验收标准**：
- [ ] 设备触发移动侦测 → 告警实时弹窗（SSE）且 `tb_alarm` 落库可查
- [ ] 杀掉 ZLM 节点进程 → 该节点流 SSE 收到 `live.closed` → 前端重新 start 成功
- [ ] 监控墙 16 路同时播放，内存/CPU 无异常增长
- [ ] `AlarmController.ack` 更新 ack_status=1

---

## Sprint 4 — 加固（P2）

**目标**：双实例集群联调；压测 SSE；指标上报。

### 任务清单

| ID | 任务 | 测试方式 |
|----|------|---------|
| S4-1 | SSE 连接上限（5000）+ 超限 503；emitter 泄漏检测 | 并发测试（JMeter/k6）：5001 连接被拒 |
| S4-2 | 集群双实例联调：实例 A 触发 `live.ready`，实例 B 的 SSE 客户端收到（Redis Pub/Sub 扇出） | 手工：启两实例，A 侧触发，B 侧 SSE 验证 |
| S4-3 | INVITE 跨节点回包：INVITE 从实例 A 发出，200 OK 路由到实例 B（`RedisInviteContextStore` 保证，已实现） | 手工：双实例场景验证 |
| S4-4 | 监控埋点：live 首播延迟（ms）、live 成功率、refCount 分布接入既有 Micrometer/Prometheus（`MONITORING.md` 指标体系） | `/actuator/metrics` 确认指标存在 |
| S4-5 | 设备级数据权限（可选）：`tb_device_group` + 用户-分组绑定 + 控制指令校验分组归属 | 待排期 |

**Sprint 4 验收标准**：
- [ ] 双实例 SSE 跨节点事件 100% 到达
- [ ] Prometheus 能抓到 `voglander_live_start_duration_ms` / `voglander_live_start_total` 指标
- [ ] 5001 并发 SSE 连接被正确拒绝（503）

---

## 风险跟踪

| 风险 | 缓解 | Sprint |
|------|------|--------|
| openRtpServer 端口/SDP 协商失败（TCP/UDP 模式） | 默认 UDP，streamMode 支持切换；失败降级重试 TCP_PASSIVE | S1 |
| ZLM on_stream_changed 与 SIP InviteOk 时序竞争 | future 由 onStreamChanged 触发（ZLM 流就绪为权威）；onInviteOk 仅回填 callId | S1 |
| Redis 单点故障导致 LiveStreamRegistry 不可用 | Resilience4j @CircuitBreaker 包裹 Registry 写操作；降级返回 PENDING 状态，前端轮询 | S1 |
| test-app.db schema 不自动同步 | 手工 sqlite3 执行 ALTER（S1-2 任务明确要求） | S1 |
| NAT 环境 sdpIp 与 ZLM API host 不同 | tb_media_node.extend JSON 支持 `{"mediaIp":"..."}` 覆盖 | S1 |
| SSE EventSource 不支持自定义 header | query param `?token=` 传 JWT，SseController 内校验 | S2 |
