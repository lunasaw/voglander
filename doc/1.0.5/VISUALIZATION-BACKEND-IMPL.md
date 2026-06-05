# GB28181 可视化后端 — 完整详细实施方案（1.0.5）

> 本文档是 [VISUALIZATION-TECH-PLAN.md](VISUALIZATION-TECH-PLAN.md) 的后端落地细化，
> 基于 2026-06-04 代码核验结果编写，所有文件路径均经过验证。
>
> **约定**：新增类放在对应模块的包下；方法签名与现有模式一致；不跳步骤。

---

## 一、前置改动（Sprint 1 必须最先完成）

### 1.1 Gb28181ProtocolHandler — 提取 InviteOk channelId

**文件**：`voglander-integration/.../handler/Gb28181ProtocolHandler.java`（当前约第 143 行）

**当前代码**：
```java
case "Session.InviteOk":
    mediaSessionManager.onInviteOk(event.correlationId(), event.deviceId());
    // pre-check 代码 ...
    break;
```

**修改为**：
```java
case "Session.InviteOk":
    String inviteOkChannelId = stringValue(
        event.payload() != null ? event.payload().get("channelId") : null);
    mediaSessionManager.onInviteOk(event.correlationId(), event.deviceId(), inviteOkChannelId);
    log.info("会话建立, callId={}, deviceId={}, channelId={}",
        event.correlationId(), event.deviceId(), inviteOkChannelId);
    break;
```

---

### 1.2 MediaSessionManager — onInviteOk 增加 channelId 回填

**文件**：`voglander-manager/.../manager/MediaSessionManager.java`（当前约第 226 行）

**修改 onInviteOk 签名并增加按 (deviceId, channelId, INVITING) 匹配占位行的逻辑**：

```java
/**
 * 会话建立成功（SIP 200 OK）。
 * 先按 callId 找行；找不到时用 (deviceId, channelId, INVITING) 找 startLive 预写的占位行，回填 callId。
 */
public Long onInviteOk(String callId, String deviceId, String channelId) {
    Assert.hasText(callId, "callId不能为空");

    // 1. 先按 callId 找（正常路径 / 重复 OK 场景）
    MediaSessionDTO existing = getByCallId(callId);
    if (existing != null) {
        MediaSessionDTO update = new MediaSessionDTO();
        update.setStatus(MediaSessionConstant.Status.ACTIVE);
        if (deviceId != null) update.setDeviceId(deviceId);
        return updateById(existing.getId(), update);
    }

    // 2. 按占位行关联（startLive 预写 INVITING 行，callId 尚未回填）
    if (deviceId != null && channelId != null) {
        LambdaQueryWrapper<MediaSessionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(MediaSessionDO::getDeviceId, deviceId)
          .eq(MediaSessionDO::getChannelId, channelId)
          .eq(MediaSessionDO::getStatus, MediaSessionConstant.Status.INVITING)
          .orderByDesc(MediaSessionDO::getCreateTime)
          .last("LIMIT 1");
        MediaSessionDO placeholder = mediaSessionService.getOne(qw);
        if (placeholder != null) {
            MediaSessionDTO update = new MediaSessionDTO();
            update.setCallId(callId);
            update.setStatus(MediaSessionConstant.Status.ACTIVE);
            if (deviceId != null) update.setDeviceId(deviceId);
            return updateById(placeholder.getId(), update);
        }
    }

    // 3. 均未找到：创建新行（兜底，保持原有行为）
    MediaSessionDTO dto = new MediaSessionDTO();
    dto.setCallId(callId);
    dto.setDeviceId(deviceId);
    dto.setChannelId(channelId);
    dto.setStatus(MediaSessionConstant.Status.ACTIVE);
    return insertOrUpdateOnDuplicate(dto, MediaSessionConstant.Status.ACTIVE, deviceId);
}
```

> **注意**：同时将所有现有的 `onInviteOk(callId, deviceId)` 两参数调用（如测试）更新为三参数，
> 或在方法上保留一个两参数的兼容重载 `onInviteOk(callId, deviceId)` 内部调 `onInviteOk(callId, deviceId, null)`。

---

## 二、数据库变更

### 2.1 tb_media_session 新增三列

同时修改以下两个文件：

**`sql/voglander.sql`**（tb_media_session 表定义末尾，PRIMARY KEY 之前）：
```sql
ALTER TABLE `tb_media_session`
  ADD COLUMN `stream_id`      VARCHAR(128)  NULL COMMENT '前端稳定主键 gb_live_{deviceId}_{channelId}',
  ADD COLUMN `node_server_id` VARCHAR(64)   NULL COMMENT '媒体节点 serverId，亲和路由',
  ADD COLUMN `ref_count`      INT           NOT NULL DEFAULT 0 COMMENT '观看者计数，权威值以 Redis 为准，DB 为快照';

CREATE UNIQUE INDEX `uk_stream_id`   ON `tb_media_session`(`stream_id`);
CREATE INDEX `idx_status_node`       ON `tb_media_session`(`status`, `node_server_id`);
CREATE INDEX `idx_device_channel`    ON `tb_media_session`(`device_id`, `channel_id`, `status`);
```

**`voglander-web/src/test/resources/schema-sqlite.sql`**（tb_media_session 建表块）：
```sql
-- 在 UNIQUE (call_id) 行后追加：
,stream_id      VARCHAR(128)
,node_server_id VARCHAR(64)
,ref_count      INTEGER NOT NULL DEFAULT 0
,UNIQUE (stream_id)
```

> 还需手工在持久化 `voglander-web/test-app.db` 中执行 ALTER（见测试基建备忘 [[voglander-test-infra-gotchas]]）。

**`MediaSessionDO.java`**（新增三个字段）：
```java
/** 前端稳定主键，直播=gb_live_{deviceId}_{channelId}，回放=gb_back_{...}_{ts} */
private String streamId;
/** 媒体节点 serverId */
private String nodeServerId;
/** 观看者引用计数（DB快照，Redis为权威） */
private Integer refCount;
```

### 2.2 tb_alarm 新建

```sql
CREATE TABLE `tb_alarm` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `device_id`   VARCHAR(64)     NOT NULL,
  `channel_id`  VARCHAR(64),
  `alarm_type`  INT             COMMENT '1移动侦测 2设备告警 3故障 4视频丢失',
  `alarm_level` INT             COMMENT '1-4，4最高',
  `alarm_time`  DATETIME,
  `description` VARCHAR(512),
  `ack_status`  INT             NOT NULL DEFAULT 0 COMMENT '0未确认 1已确认',
  `extend`      TEXT,
  PRIMARY KEY (`id`),
  INDEX `idx_device_time`  (`device_id`, `alarm_time`),
  INDEX `idx_level_status` (`alarm_level`, `ack_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

SQLite 版本同步加入 `schema-sqlite.sql`（DATETIME 保持，去掉 ENGINE/CHARSET）。

---

## 三、ServiceExceptionEnum 补充

**文件**：`voglander-common/.../enums/ServiceExceptionEnum.java`

```java
LIVE_INVITE_TIMEOUT(700001, "直播拉流超时，设备未在规定时间内推流"),
LIVE_NODE_UNAVAILABLE(700002, "无可用媒体节点"),
STREAM_NOT_READY(700003, "流尚未就绪"),
LIVE_STREAM_NOT_FOUND(700004, "直播会话不存在"),
PLAYBACK_CONTROL_FAILED(700005, "回放控制指令发送失败"),
```

---

## 四、LiveStreamRegistry（新建）

**模块**：`voglander-service`
**包**：`io.github.lunasaw.voglander.service.live`
**文件**：`LiveStreamRegistry.java`（接口）+ `RedisLiveStreamRegistry.java`（实现）

### 接口

```java
public interface LiveStreamRegistry {
    /** 增加引用计数，返回新值 */
    long incRef(String streamId);
    /** 减少引用计数，返回新值（不低于0） */
    long decRef(String streamId);
    /** 读取引用计数 */
    long getRef(String streamId);
    /** 存储会话信息 */
    void putSession(String streamId, LiveSessionInfo info);
    /** 读取会话信息 */
    LiveSessionInfo getSession(String streamId);
    /** 删除（流关闭时） */
    void remove(String streamId);
    /** 注册 future（首播等待流就绪） */
    void registerFuture(String streamId, CompletableFuture<Void> future);
    /** 触发 future 完成（onStreamChanged 调用） */
    void completeFuture(String streamId);
    /** 续约 TTL */
    void keepAlive(String streamId, long ttlSeconds);
}
```

### 关键实现细节

```java
@Component
@RequiredArgsConstructor
public class RedisLiveStreamRegistry implements LiveStreamRegistry {

    private static final String PREFIX_SESSION  = "live:session:";
    private static final String PREFIX_REF      = "live:refcount:";
    private static final long   DEFAULT_TTL_SEC = 3600;

    // 注意：用主 Redis-A（stringRedisTemplate），不混入 invite Redis-B
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ConcurrentHashMap<String, CompletableFuture<Void>> localFutures
        = new ConcurrentHashMap<>();

    @Override
    public long incRef(String streamId) {
        Long v = stringRedisTemplate.opsForValue().increment(PREFIX_REF + streamId);
        return v == null ? 0 : v;
    }

    @Override
    public long decRef(String streamId) {
        // Lua 脚本保证不低于 0
        Long v = stringRedisTemplate.execute(DECR_NOT_NEGATIVE_SCRIPT,
            List.of(PREFIX_REF + streamId));
        return v == null ? 0 : v;
    }

    // Lua: local v=redis.call('DECR',KEYS[1]); if v<0 then redis.call('SET',KEYS[1],'0') end; return v
    private static final RedisScript<Long> DECR_NOT_NEGATIVE_SCRIPT = RedisScript.of(
        "local v=redis.call('DECR',KEYS[1]) if v<0 then redis.call('SET',KEYS[1],'0') end return v",
        Long.class);

    @Override
    public void putSession(String streamId, LiveSessionInfo info) {
        String json = JSON.toJSONString(info);
        stringRedisTemplate.opsForValue().set(PREFIX_SESSION + streamId, json,
            DEFAULT_TTL_SEC, TimeUnit.SECONDS);
    }

    @Override
    public LiveSessionInfo getSession(String streamId) {
        String json = stringRedisTemplate.opsForValue().get(PREFIX_SESSION + streamId);
        return json == null ? null : JSON.parseObject(json, LiveSessionInfo.class);
    }

    @Override
    public void registerFuture(String streamId, CompletableFuture<Void> future) {
        localFutures.put(streamId, future);
    }

    @Override
    public void completeFuture(String streamId) {
        CompletableFuture<Void> f = localFutures.remove(streamId);
        if (f != null) f.complete(null);
    }

    @Override
    public void remove(String streamId) {
        stringRedisTemplate.delete(List.of(PREFIX_SESSION + streamId, PREFIX_REF + streamId));
        localFutures.remove(streamId);
    }

    @Override
    public void keepAlive(String streamId, long ttlSeconds) {
        stringRedisTemplate.expire(PREFIX_SESSION + streamId, ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(PREFIX_REF + streamId,     ttlSeconds, TimeUnit.SECONDS);
    }
}
```

**LiveSessionInfo**（值对象，放同包）：
```java
@Data
public class LiveSessionInfo {
    private String callId;
    private String nodeServerId;
    private String sdpIp;
    private Integer rtpPort;
    /** MediaSessionConstant.Status */
    private Integer status;
    /** MediaSessionConstant.Type */
    private String sessionType;
    private long createMs;
    /** FastJSON2 序列化的 PlayUrl */
    private String playUrlsJson;
}
```

---

## 五、SseEventBus（新建）

**模块**：`voglander-service`，包：`...service.sse`

### 接口

```java
public interface SseEventBus {
    SseEmitter register(String userId, Set<String> topics);
    void publish(SseEvent event);
    void publishLocal(SseEvent event);  // 仅下发本节点，供 Pub/Sub 监听器调用
}
```

### SseEvent 值对象

```java
@Data
@AllArgsConstructor
public class SseEvent {
    private String topic;   // 如 "live.ready"、"device.online"
    private Object data;    // 序列化为 JSON 再整体包装
}
```

### 实现骨架

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisBackedSseEventBus implements SseEventBus, InitializingBean {

    private static final String REDIS_CHANNEL = "sse:broadcast";
    private static final int    MAX_EMITTERS  = 5000;
    private static final long   HEARTBEAT_MS  = 15_000;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private final ConcurrentHashMap<String, EmitterHolder> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter register(String userId, Set<String> topics) {
        if (emitters.size() >= MAX_EMITTERS) {
            throw new ServiceException(ServiceExceptionEnum.LIVE_NODE_UNAVAILABLE); // 复用或新增枚举
        }
        String emitterId = userId + ":" + System.nanoTime();
        SseEmitter emitter = new SseEmitter(0L); // 不超时，由心跳维持
        emitters.put(emitterId, new EmitterHolder(emitter, userId, topics));

        Runnable cleanup = () -> emitters.remove(emitterId);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        return emitter;
    }

    @Override
    public void publish(SseEvent event) {
        // 本节点直发 + 广播给其他节点
        publishLocal(event);
        try {
            stringRedisTemplate.convertAndSend(REDIS_CHANNEL, JSON.toJSONString(event));
        } catch (Exception e) {
            log.warn("SSE Redis 广播失败, topic={}", event.getTopic(), e);
        }
    }

    @Override
    public void publishLocal(SseEvent event) {
        String data = JSON.toJSONString(event.getData());
        emitters.forEach((id, holder) -> {
            if (!holder.topics.contains(event.getTopic())) return;
            try {
                holder.emitter.send(SseEmitter.event()
                    .name(event.getTopic())
                    .data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitters.remove(id);
            }
        });
    }

    /** 15s 心跳，防止 Nginx/代理超时断连 */
    @Scheduled(fixedDelay = HEARTBEAT_MS)
    public void heartbeat() {
        emitters.forEach((id, holder) -> {
            try {
                holder.emitter.send(SseEmitter.event().name("ping").data(""));
            } catch (Exception e) {
                emitters.remove(id);
            }
        });
    }

    /** 订阅 Redis 跨节点广播 */
    @Override
    public void afterPropertiesSet() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                SseEvent event = JSON.parseObject(new String(message.getBody()), SseEvent.class);
                publishLocal(event);   // 仅本地分发，不再 publish（避免回路）
            } catch (Exception e) {
                log.warn("SSE 消息解析失败", e);
            }
        }, new ChannelTopic(REDIS_CHANNEL));
        container.afterPropertiesSet();
        container.start();
    }

    @Data @AllArgsConstructor
    private static class EmitterHolder {
        SseEmitter emitter;
        String userId;
        Set<String> topics;
    }
}
```

---

## 六、MediaPlayService（新建，编排核心）

**模块**：`voglander-service`，包：`...service.live`

### 接口

```java
public interface MediaPlayService {
    LivePlayDTO startLive(LiveStartDTO dto);
    boolean     stopLive(String streamId);
    LivePlayDTO getLive(String streamId);
    void        keepAlive(String streamId);
    LivePlayDTO startPlayback(PlaybackStartDTO dto);
    boolean     stopPlayback(String streamId);
    boolean     controlPlayback(PlaybackControlDTO dto);
}
```

### startLive 实现（关键流程）

```java
@Override
public LivePlayDTO startLive(LiveStartDTO dto) {
    String streamId = "gb_live_" + dto.getDeviceId() + "_" + dto.getChannelId();

    // 分布式锁防并发重复 INVITE
    String lockKey   = "live:lock:" + streamId;
    String lockValue = UUID.randomUUID().toString();
    boolean locked = redisLockUtil.tryLock(lockKey, lockValue, 10);
    if (!locked) {
        // 等待锁释放后复用已有会话
        LiveSessionInfo session = liveStreamRegistry.getSession(streamId);
        if (session != null && session.getStatus() == MediaSessionConstant.Status.ACTIVE) {
            liveStreamRegistry.incRef(streamId);
            return buildVO(streamId, session);
        }
        throw new ServiceException(ServiceExceptionEnum.LIVE_INVITE_TIMEOUT);
    }
    try {
        // 多路复用判定
        LiveSessionInfo existing = liveStreamRegistry.getSession(streamId);
        if (existing != null && existing.getStatus() == MediaSessionConstant.Status.ACTIVE) {
            liveStreamRegistry.incRef(streamId);
            return buildVO(streamId, existing);
        }

        // 选节点（一致性哈希亲和）
        ZlmNode node = nodeService.selectNode(streamId);
        if (node == null) throw new ServiceException(ServiceExceptionEnum.LIVE_NODE_UNAVAILABLE);

        // 取媒体 IP（tb_media_node.host）
        MediaNodeDTO mediaNode = mediaNodeManager.getDTOByServerId(node.getServerId());
        String sdpIp = resolveMediaIp(mediaNode);

        // 开 RTP 接收端口
        OpenRtpServerResult rtpResult = ZlmRestService.openRtpServer(
            node.getHost(), node.getSecret(),
            buildOpenRtpReq(streamId, dto.getStreamMode()));
        Assert.notNull(rtpResult, "openRtpServer 失败");

        // 预写 INVITING 占位行
        MediaSessionDTO placeholder = new MediaSessionDTO();
        placeholder.setStreamId(streamId);
        placeholder.setDeviceId(dto.getDeviceId());
        placeholder.setChannelId(dto.getChannelId());
        placeholder.setNodeServerId(node.getServerId());
        placeholder.setStatus(MediaSessionConstant.Status.INVITING);
        placeholder.setSessionType(MediaSessionConstant.Type.PLAY);
        mediaSessionManager.add(placeholder);

        // 注册 future（最先注册，避免 onStreamChanged 先到找不到 future）
        CompletableFuture<Void> future = new CompletableFuture<>();
        liveStreamRegistry.registerFuture(streamId, future);

        // 发 INVITE
        ResultDTO<Void> inviteResult = voglanderServerMediaCommand
            .inviteRealTimePlay(dto.getDeviceId(), sdpIp, rtpResult.getPort(),
                toStreamModeEnum(dto.getStreamMode()));
        if (!inviteResult.isSuccess()) {
            cleanupFailed(streamId, node);
            throw new ServiceException(ServiceExceptionEnum.LIVE_INVITE_TIMEOUT);
        }

        // 等待流就绪（8s）
        try {
            future.get(8, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            cleanupFailed(streamId, node);
            sseEventBus.publish(new SseEvent("live.failed",
                Map.of("streamId", streamId, "reason", "timeout")));
            throw new ServiceException(ServiceExceptionEnum.LIVE_INVITE_TIMEOUT);
        }

        // 拉 PlayUrls
        PlayUrl playUrl = ZlmRestService.getPlaybackUrls(
            node.getHost(), node.getSecret(), buildPlayUrlReq(streamId));

        // 写 Registry
        LiveSessionInfo info = new LiveSessionInfo();
        info.setNodeServerId(node.getServerId());
        info.setSdpIp(sdpIp);
        info.setRtpPort(rtpResult.getPort());
        info.setStatus(MediaSessionConstant.Status.ACTIVE);
        info.setSessionType(MediaSessionConstant.Type.PLAY);
        info.setCreateMs(System.currentTimeMillis());
        info.setPlayUrlsJson(JSON.toJSONString(playUrl));
        liveStreamRegistry.putSession(streamId, info);
        liveStreamRegistry.incRef(streamId);
        liveStreamRegistry.keepAlive(streamId, 3600);

        return buildVO(streamId, info);
    } finally {
        redisLockUtil.unLock(lockKey, lockValue);
    }
}

private String resolveMediaIp(MediaNodeDTO node) {
    // NAT 环境：extend JSON 里可配 {"mediaIp":"x.x.x.x"}
    if (node.getExtend() != null) {
        try {
            JSONObject ext = JSON.parseObject(node.getExtend());
            String mediaIp = ext.getString("mediaIp");
            if (mediaIp != null && !mediaIp.isBlank()) return mediaIp;
        } catch (Exception ignored) { }
    }
    // 默认从 host 解析 IP（去掉端口部分）
    String host = node.getHost();
    return host.contains(":") ? host.substring(0, host.lastIndexOf(':')) : host;
}
```

### stopLive 实现（引用计数）

```java
@Override
public boolean stopLive(String streamId) {
    long ref = liveStreamRegistry.decRef(streamId);
    if (ref > 0) return true;  // 仍有观看者，保活

    // 延迟 30s 无人 → 真正关流（通过 ScheduledExecutorService 或 @Scheduled 扫 refCount=0 的流）
    redisTemplate.opsForValue().set("live:pending_close:" + streamId, "1", 30, TimeUnit.SECONDS);
    return true;
}

/** @Scheduled 每 10s 扫一次 pending_close */
@Scheduled(fixedDelay = 10_000)
public void drainPendingClose() {
    // scan "live:pending_close:*"，对每个 key 检查 refCount，若仍=0 则 BYE + closeRtpServer
    // 实现参见 §九 僵尸 GC
}
```

---

## 七、VoglanderZlmHookService — onStreamChanged 接 live.ready

**文件**：`voglander-integration/.../impl/VoglanderZlmHookServiceImpl.java`

在 `onStreamChanged` 的**流上线分支**（`regist=true`）末尾追加：

```java
if (Boolean.TRUE.equals(param.getRegist())) {
    // 现有逻辑 ...

    // 新增：唤醒本节点等待 future（跨节点由 Pub/Sub 通知）
    String streamId = param.getStream();  // ZLM 流 ID 即 streamId
    liveStreamRegistry.completeFuture(streamId);
    sseEventBus.publish(new SseEvent("live.ready",
        Map.of("streamId", streamId)));
}
```

> `liveStreamRegistry` 和 `sseEventBus` 通过 `@Autowired` 注入（在 integration 模块引入 service 依赖，
> 或通过 Spring 事件 `ApplicationEventPublisher` 解耦，优先用事件解耦避免循环依赖）。

---

## 八、新增 Controller 层

### 8.1 JwtAuthInterceptor（修改 ResourcesConfig）

**新建**：`voglander-web/.../web/interceptor/JwtAuthInterceptor.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String token = extractToken(req);
        if (token == null || authService.getUserByToken(token) == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(JSON.toJSONString(AjaxResult.error(401, "未登录或 token 已过期")));
            return false;
        }
        return true;
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return req.getParameter("token");  // SSE query param 兜底
    }
}
```

**修改 ResourcesConfig**：
```java
// 注入
@Autowired
private JwtAuthInterceptor jwtAuthInterceptor;

@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");

    // 新增 JWT 全局鉴权
    registry.addInterceptor(jwtAuthInterceptor)
        .addPathPatterns("/api/v1/**")
        .excludePathPatterns(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/health",
            "/api/v1/stream/events",  // SSE 内部自己校验 query token
            "/swagger-ui/**",
            "/v3/api-docs/**"
        );
}

// 修改 CORS：生产环境收敛来源
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    // 从配置读取，dev profile 设 *，生产设白名单
    List<String> origins = Arrays.asList(
        StringUtils.split(allowedOrigins, ","));
    origins.forEach(config::addAllowedOriginPattern);
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    config.setMaxAge(1800L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}

@Value("${voglander.cors.allowed-origins:*}")
private String allowedOrigins;
```

---

### 8.2 SseController

**包**：`voglander-web/.../web/api/sse/controller/SseController.java`

```java
@RestController
@RequestMapping("/api/v1/stream")
@Tag(name = "SSE 实时事件")
@RequiredArgsConstructor
public class SseController {

    private final SseEventBus sseEventBus;
    private final AuthService authService;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅实时事件流")
    public SseEmitter subscribe(
            @RequestParam(defaultValue = "device,live,alarm") String topics,
            @RequestParam(required = false) String token) {

        // SSE 不能带 Authorization header，从 query 校验
        if (authService.getUserByToken(token) == null) {
            throw new ServiceException(ServiceExceptionEnum.USER_NOT_LOGIN);
        }
        String userId = JwtUtils.getUserId(token).toString();
        Set<String> topicSet = new HashSet<>(Arrays.asList(topics.split(",")));
        return sseEventBus.register(userId, topicSet);
    }
}
```

---

### 8.3 LivePlayController

**包**：`voglander-web/.../web/api/live/controller/LivePlayController.java`

```java
@RestController
@RequestMapping("/api/v1/live")
@Tag(name = "直播管理")
@RequiredArgsConstructor
public class LivePlayController {

    private final MediaPlayService mediaPlayService;
    private final LiveWebAssembler liveWebAssembler;

    @PostMapping("/start")
    @Operation(summary = "开始直播（首播/复用）")
    public AjaxResult<LivePlayVO> start(@Valid @RequestBody LiveStartReq req) {
        return AjaxResult.success(
            liveWebAssembler.dtoToVo(mediaPlayService.startLive(liveWebAssembler.startReqToDto(req))));
    }

    @PostMapping("/stop")
    @Operation(summary = "停止直播（引用计数）")
    public AjaxResult<Boolean> stop(@Valid @RequestBody LiveStopReq req) {
        return AjaxResult.success(mediaPlayService.stopLive(req.getStreamId()));
    }

    @GetMapping("/{streamId}")
    @Operation(summary = "查询直播状态（轮询兜底）")
    public AjaxResult<LivePlayVO> get(@PathVariable String streamId) {
        return AjaxResult.success(
            liveWebAssembler.dtoToVo(mediaPlayService.getLive(streamId)));
    }

    @PostMapping("/keepalive")
    @Operation(summary = "心跳续约")
    public AjaxResult<Boolean> keepalive(@RequestBody Map<String, String> body) {
        mediaPlayService.keepAlive(body.get("streamId"));
        return AjaxResult.success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "活跃会话列表")
    public AjaxResult<List<LivePlayVO>> list(@RequestParam(required = false) String deviceId) {
        // MediaSessionManager.getActiveSessions(deviceId) → liveStreamRegistry 补充 refCount
        // 实现略，Sprint 1 后期补充
        return AjaxResult.success(Collections.emptyList());
    }
}
```

**LiveStartReq / LiveStopReq / LivePlayVO**（放 `web/api/live/domain` 包）：
```java
@Data public class LiveStartReq {
    @NotBlank private String deviceId;
    @NotBlank private String channelId;
    private String protocol   = "FLV";
    private String streamMode = "UDP";
}

@Data public class LiveStopReq {
    @NotBlank private String streamId;
}

@Data public class LivePlayVO {
    private String  streamId;
    private String  callId;
    private Integer status;
    private PlayUrl playUrls;
    private long    refCount;
}
```

---

### 8.4 PlaybackController

**包**：`voglander-web/.../web/api/live/controller/PlaybackController.java`

```java
@RestController
@RequestMapping("/api/v1/playback")
@Tag(name = "录像回放")
@RequiredArgsConstructor
public class PlaybackController {

    private final MediaPlayService mediaPlayService;
    private final LiveWebAssembler liveWebAssembler;

    @PostMapping("/start")
    public AjaxResult<LivePlayVO> start(@Valid @RequestBody PlaybackStartReq req) {
        return AjaxResult.success(
            liveWebAssembler.dtoToVo(
                mediaPlayService.startPlayback(liveWebAssembler.playbackStartReqToDto(req))));
    }

    @PostMapping("/stop")
    public AjaxResult<Boolean> stop(@RequestBody Map<String, String> body) {
        return AjaxResult.success(mediaPlayService.stopPlayback(body.get("streamId")));
    }

    @PostMapping("/control")
    public AjaxResult<Boolean> control(@Valid @RequestBody PlaybackControlReq req) {
        return AjaxResult.success(
            mediaPlayService.controlPlayback(liveWebAssembler.controlReqToDto(req)));
    }

    @PostMapping("/records")
    @Operation(summary = "查询设备录像（异步，结果走 SSE record.info）")
    public AjaxResult<Void> queryRecords(@Valid @RequestBody RecordQueryReq req) {
        // 调 GbDeviceCommandService.queryRecord → 异步 → Gb28181ProtocolHandler.handleRecord → SseEventBus
        deviceCommandService.queryRecord(liveWebAssembler.recordQueryReqToReq(req));
        return AjaxResult.success(null);
    }
}
```

---

### 8.5 PtzController

**包**：`voglander-web/.../web/api/ptz/controller/PtzController.java`

```java
@RestController
@RequestMapping("/api/v1/ptz")
@Tag(name = "PTZ 控制")
@RequiredArgsConstructor
public class PtzController {

    private final DeviceCommandService deviceCommandService;
    private final PtzWebAssembler ptzWebAssembler;

    @PostMapping("/control")
    public AjaxResult<Boolean> control(@Valid @RequestBody PtzControlReq req) {
        ResultDTO<Void> r = deviceCommandService.ptzControl(ptzWebAssembler.toReq(req));
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/stop")
    public AjaxResult<Boolean> stop(@Valid @RequestBody PtzStopReq req) {
        ResultDTO<Void> r = deviceCommandService.ptzControl(ptzWebAssembler.toStopReq(req));
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/preset")
    public AjaxResult<Boolean> preset(@Valid @RequestBody PresetReq req) {
        // 调 VoglanderServerPtzCommand.setPreset/gotoPreset/deletePreset
        ResultDTO<Void> r = deviceCommandService.ptzPreset(ptzWebAssembler.toPresetReq(req));
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }
}
```

**PtzControlReq**（command 枚举对应 PTZControlEnum 的 11 项）：
```java
@Data public class PtzControlReq {
    @NotBlank private String deviceId;
    @NotBlank private String channelId;
    /** UP/DOWN/LEFT/RIGHT/UP_LEFT/UP_RIGHT/DOWN_LEFT/DOWN_RIGHT/ZOOM_IN/ZOOM_OUT/STOP */
    @NotBlank private String command;
    private Integer speed = 50;
}
```

---

### 8.6 DeviceCmdController

**包**：`voglander-web/.../web/api/device/controller/DeviceCmdController.java`

```java
@RestController
@RequestMapping("/api/v1/device-cmd")
@Tag(name = "设备主动指令")
@RequiredArgsConstructor
public class DeviceCmdController {

    private final DeviceCommandService deviceCommandService;

    @PostMapping("/query-catalog")
    public AjaxResult<Void> queryCatalog(@RequestBody Map<String, String> body) {
        deviceCommandService.queryCatalog(body.get("deviceId"));
        return AjaxResult.success(null);  // 结果走 SSE channel.updated
    }

    @PostMapping("/query-info")
    public AjaxResult<Void> queryInfo(@RequestBody Map<String, String> body) {
        deviceCommandService.queryDeviceInfo(body.get("deviceId"));
        return AjaxResult.success(null);
    }

    @PostMapping("/reboot")
    @Operation(summary = "重启设备（记录操作日志）")
    public AjaxResult<Boolean> reboot(@RequestBody Map<String, String> body,
                                       HttpServletRequest request) {
        String deviceId = body.get("deviceId");
        // 操作日志（deviceId + userId + 时间）
        log.info("[AUDIT] reboot device={}, operator={}", deviceId, resolveUserId(request));
        ResultDTO<Void> r = deviceCommandService.reboot(deviceId);
        return r.isSuccess() ? AjaxResult.success(true) : AjaxResult.error(r.getMessage());
    }

    @PostMapping("/record")
    public AjaxResult<Boolean> record(@Valid @RequestBody DeviceRecordReq req) {
        // START/STOP 调 GbDeviceCommandService.record
        return AjaxResult.success(true);
    }
}
```

---

### 8.7 AlarmController

**包**：`voglander-web/.../web/api/alarm/controller/AlarmController.java`

```java
@RestController
@RequestMapping("/api/v1/alarm")
@Tag(name = "告警管理")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmManager alarmManager;
    private final AlarmWebAssembler alarmWebAssembler;

    @PostMapping("/getPage")
    public AjaxResult<AlarmListResp> getPage(@Valid @RequestBody AlarmQueryReq req) {
        Page<AlarmDTO> page = alarmManager.getPage(
            alarmWebAssembler.queryReqToDto(req), req.getPage(), req.getSize());
        return AjaxResult.success(alarmWebAssembler.toListResp(page));
    }

    @GetMapping("/get/{id}")
    public AjaxResult<AlarmVO> get(@PathVariable Long id) {
        return AjaxResult.success(alarmWebAssembler.dtoToVo(alarmManager.getById(id)));
    }

    @PostMapping("/ack")
    public AjaxResult<Boolean> ack(@RequestBody Map<String, Long> body) {
        return AjaxResult.success(alarmManager.ack(body.get("id")));
    }
}
```

---

## 九、告警全链路（Alarm 领域）

新增组件清单（均参照 StreamProxy 模式实现，此处给出差异点）：

| 组件 | 模块 | 说明 |
|------|------|------|
| `AlarmDO` | repository | 对应 tb_alarm 表 |
| `AlarmMapper` | repository | extends BaseMapper<AlarmDO> |
| `AlarmService` / `AlarmServiceImpl` | service | extends IService<AlarmDO> |
| `AlarmManager` | manager | 模板方法 CRUD + `ack(id)` |
| `AlarmDTO` / `AlarmAssembler` | manager | DTO ↔ DO |
| `AlarmWebAssembler` | web | Req/VO ↔ DTO |

**Gb28181ProtocolHandler — handleAlarm 改造**：

当前仅日志，改为：
```java
case "Notify.Alarm":
    String alarmDeviceId = event.deviceId();
    Map<String, Object> ap = event.payload() != null ? event.payload() : Map.of();
    AlarmDTO alarmDTO = new AlarmDTO();
    alarmDTO.setDeviceId(alarmDeviceId);
    alarmDTO.setChannelId(stringValue(ap.get("channelId")));
    alarmDTO.setAlarmType(intValue(ap.get("alarmType")));
    alarmDTO.setAlarmLevel(intValue(ap.get("alarmLevel")));
    alarmDTO.setAlarmTime(LocalDateTime.now());
    alarmDTO.setDescription(stringValue(ap.get("description")));
    alarmManager.add(alarmDTO);
    sseEventBus.publish(new SseEvent("alarm.new", alarmDTO));
    break;
```

---

## 十、僵尸会话 GC

**新建**：`voglander-service/.../service.live.LiveSessionGcService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveSessionGcService {

    private static final int INVITING_TIMEOUT_MIN = 2;

    private final MediaSessionManager mediaSessionManager;
    private final LiveStreamRegistry  liveStreamRegistry;
    private final SseEventBus         sseEventBus;

    @Scheduled(fixedDelay = 60_000)
    public void gc() {
        LocalDateTime invitingDeadline = LocalDateTime.now().minusMinutes(INVITING_TIMEOUT_MIN);

        // 1. INVITING 超时 → FAILED
        mediaSessionManager.markTimeoutInvitingAsFailed(invitingDeadline);

        // 2. ACTIVE 但 ZLM 无流 → 强制 CLOSED
        List<MediaSessionDTO> active = mediaSessionManager.getActiveSessions();
        for (MediaSessionDTO s : active) {
            if (s.getStreamId() == null) continue;
            // 检查 ZLM 是否仍有该流
            boolean alive = isStreamAliveOnZlm(s);
            if (!alive) {
                mediaSessionManager.forceClose(s.getId());
                liveStreamRegistry.remove(s.getStreamId());
                sseEventBus.publish(new SseEvent("live.closed",
                    Map.of("streamId", s.getStreamId(), "reason", "gc")));
                log.warn("[GC] 关闭僵尸会话 streamId={}", s.getStreamId());
            }
        }

        // 3. refCount=0 且超过 keepAliveSec 的流执行 BYE
        drainPendingClose();
    }

    private boolean isStreamAliveOnZlm(MediaSessionDTO s) {
        try {
            // 用 ZlmRestService.getRtpInfo 查询
            return ZlmRestService.getRtpInfo(/* node host/secret */, s.getStreamId()) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## 十一、新文件位置汇总

```
voglander-service/src/main/java/.../service/
  live/
    LiveStreamRegistry.java          （接口）
    RedisLiveStreamRegistry.java     （实现）
    LiveSessionInfo.java             （值对象）
    MediaPlayService.java            （接口）
    MediaPlayServiceImpl.java        （实现）
    LiveSessionGcService.java        （GC调度）
  sse/
    SseEventBus.java                 （接口）
    SseEvent.java                    （值对象）
    RedisBackedSseEventBus.java      （实现）

voglander-web/src/main/java/.../web/
  interceptor/
    JwtAuthInterceptor.java
  api/live/
    controller/LivePlayController.java
    controller/PlaybackController.java
    domain/LiveStartReq.java
    domain/LiveStopReq.java
    domain/PlaybackStartReq.java
    domain/PlaybackControlReq.java
    domain/RecordQueryReq.java
    domain/LivePlayVO.java
    assembler/LiveWebAssembler.java
  api/ptz/
    controller/PtzController.java
    domain/PtzControlReq.java
    domain/PtzStopReq.java
    domain/PresetReq.java
    assembler/PtzWebAssembler.java
  api/device/controller/DeviceCmdController.java
  api/alarm/
    controller/AlarmController.java
    domain/AlarmQueryReq.java
    domain/AlarmVO.java
    domain/AlarmListResp.java
    assembler/AlarmWebAssembler.java

voglander-manager/src/main/java/.../manager/
  AlarmManager.java

voglander-service/src/main/java/.../service/
  AlarmService.java / AlarmServiceImpl.java

voglander-repository/src/main/java/.../
  entity/AlarmDO.java
  mapper/AlarmMapper.java

voglander-client/src/main/java/.../
  domain/alarm/AlarmDTO.java
  assembler/AlarmAssembler.java
```

---

## 十二、测试策略

| 新组件 | 测试类型 | 关键断言 |
|--------|---------|---------|
| `MediaSessionManager.onInviteOk(3参数)` | Manager 集成（BaseTest） | INVITING 占位行 → ACTIVE + callId 回填正确 |
| `RedisLiveStreamRegistry` | 单测（内嵌 Redis 或 mock） | 8线程并发 incRef 结果=8；decRef 不低于 0 |
| `MediaPlayService.startLive` | Manager 集成 | 首播建会话；第二次调用 refCount+1 秒返回 |
| `SseEventBus` | 集成（单节点） | publish → 订阅同 topic 的 emitter 收到事件 |
| `JwtAuthInterceptor` | Controller 单测（Mockito） | 无 token → 401；有效 token → 放行；白名单路径 → 放行 |
| `LivePlayController` | Controller 单测（Mockito） | start/stop/get 各接口参数校验+返回结构 |
| `AlarmManager` | Manager 集成 | Alarm CRUD + ack 状态变更 |
| `LiveSessionGcService` | 集成 | INVITING 超时行被标 FAILED；注入 fake ZLM 无流 → CLOSED |
