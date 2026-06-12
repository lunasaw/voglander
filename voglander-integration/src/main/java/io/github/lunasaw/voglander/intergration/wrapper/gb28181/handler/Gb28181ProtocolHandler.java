package io.github.lunasaw.voglander.intergration.wrapper.gb28181.handler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceInfoReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRegisterReq;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.client.service.device.DeviceRegisterService;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.event.ProtocolEventHandler;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.manager.manager.AlarmManager;
import io.github.lunasaw.voglander.manager.domaon.dto.AlarmDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.routing.DeviceNodeRouteService;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181 协议事件处理器（Phase 3 / PROTOCOL S2）。
 * <p>
 * 承接原 {@code VoglanderBusinessNotifier} 的整段 switch 逻辑，但只认归一化的
 * {@link DeviceEvent}（{@code group/name}），<strong>不 import 任何 sip-gateway 框架类型</strong>
 * （{@code GatewayEvent}/{@code BusinessNotifier} 等���，从而把协议处理与具体 gateway 产品解耦。
 * payload 仍走 FastJSON2 反序列化为 GB28181 实体（项目类型转换规范）。
 * </p>
 *
 * @author luna
 * @since Phase 3
 */
@Slf4j
@Component
public class Gb28181ProtocolHandler implements ProtocolEventHandler {

    @Autowired
    private DeviceRegisterService deviceRegisterService;

    @Autowired
    private DeviceManager         deviceManager;

    @Autowired
    private DeviceChannelManager  deviceChannelManager;

    @Autowired
    private MediaSessionManager   mediaSessionManager;

    @Autowired(required = false)
    private DeviceNodeRouteService  deviceNodeRouteService;

    @Autowired
    private AlarmManager            alarmManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /** 心跳 SSE 节流：deviceId → 上次推送的毫秒时间戳（≥5s 才推） */
    private final ConcurrentHashMap<String, Long> keepaliveLastSseMs = new ConcurrentHashMap<>();
    private static final long KEEPALIVE_SSE_THROTTLE_MS = 5_000L;

    @Override
    public String protocol() {
        return "gb28181";
    }

    @Override
    public void handle(DeviceEvent event) {
        String groupName = event.groupName();
        switch (groupName) {
            // ========== Lifecycle ==========
            case "Lifecycle.Register":
                handleRegister(event);
                break;
            case "Lifecycle.Online":
                // 🔴 C3（1.0.4）：携带时间戳，使 patchLiveness 内部单调条件 + R4 终态保护生效
                deviceManager.patchLiveness(event.deviceId(), DeviceConstant.Status.ONLINE, LocalDateTime.now());
                // B5(b)：上线显式续期路由 TTL
                if (deviceNodeRouteService != null) {
                    deviceNodeRouteService.renewDevice(event.deviceId());
                }
                log.info("设备上线, deviceId={}", event.deviceId());
                publishVisual("device.online", event.deviceId(), null, null);
                break;
            case "Lifecycle.Offline":
                deviceRegisterService.offline(event.deviceId());
                // 🔴 Stage 2（1.0.4）：级联通道下线
                deviceChannelManager.cascadeOffline(event.deviceId());
                log.info("设备离线 + 通道级联下线, deviceId={}", event.deviceId());
                publishVisual("device.offline", event.deviceId(), null, null);
                break;
            case "Lifecycle.RemoteAddressChanged":
                handleRemoteAddressChanged(event);
                break;
            case "Lifecycle.RegisterChallenge":
                log.debug("设备注册挑战, deviceId={}", event.deviceId());
                break;

            // ========== Notify ==========
            case "Notify.Keepalive":
                handleKeepalive(event);
                break;
            case "Notify.Alarm":
                handleAlarm(event);
                break;
            case "Notify.MobilePosition":
                log.info("设备移动位置通知, deviceId={}, payload={}", event.deviceId(), event.payload());
                break;
            case "Notify.MediaStatus":
                handleMediaStatus(event);
                break;
            case "Notify.UpgradeResult":
            case "Notify.SnapShotFinished":
            case "Notify.VideoUpload":
                log.info("设备通知事件, type={}, deviceId={}", event.type(), event.deviceId());
                break;

            // ========== Response ==========
            case "Response.Catalog":
                handleCatalog(event);
                break;
            case "Response.DeviceInfo":
                handleDeviceInfo(event);
                break;
            case "Response.DeviceStatus":
            case "Response.RecordInfo":
            case "Response.PtzPosition":
            case "Response.SdCardStatus":
            case "Response.HomePosition":
            case "Response.CruiseTrackList":
            case "Response.CruiseTrack":
            case "Response.Config":
            case "Response.ConfigDownload":
            case "Response.PresetQuery":
            case "Response.Subscribe":
            case "Response.NotifyUpdate":
            case "Response.DeviceInfoError":
            case "Response.DeviceInfoRequest":
                log.info("设备响应事件, type={}, deviceId={}, sn={}", event.type(), event.deviceId(), event.correlationId());
                break;

            // ========== Session ==========
            case "Session.InviteOk":
                // 标准通道寻址下 event.deviceId() 被 To 头回显污染为 channelId，不可信；
                // 改以 callId 关联会话（startLive 已回填真实 callId 到占位行），从会话表取权威 deviceId/channelId。
                String okCallId = event.correlationId();
                mediaSessionManager.onInviteOk(okCallId);
                MediaSessionDTO okSession = mediaSessionManager.getByCallId(okCallId);
                if (okSession != null && okSession.getDeviceId() != null && okSession.getChannelId() != null) {
                    boolean promoted = deviceChannelManager.promoteOnlineIfOffline(
                        okSession.getDeviceId(), okSession.getChannelId(), LocalDateTime.now());
                    if (promoted) {
                        log.info("会话建立提升通道在线 - deviceId={}, channelId={}",
                            okSession.getDeviceId(), okSession.getChannelId());
                    }
                }
                log.info("会话建立, callId={}, deviceId={}, channelId={}", okCallId,
                    okSession != null ? okSession.getDeviceId() : null,
                    okSession != null ? okSession.getChannelId() : null);
                publishVisual("session.invite_ok",
                    okSession != null ? okSession.getDeviceId() : null, "callId", okCallId);
                break;
            case "Session.InviteFailure":
                mediaSessionManager.onInviteFailure(event.correlationId(), intFromPayload(event, "statusCode"));
                break;
            case "Session.Ack":
                mediaSessionManager.onAck(event.correlationId());
                break;
            case "Session.Bye":
                if (event.deviceId() != null) {
                    mediaSessionManager.onBye(event.deviceId());
                }
                log.info("会话结束(BYE), deviceId={}", event.deviceId());
                publishVisual("session.bye", event.deviceId(), "callId", event.correlationId());
                break;
            case "Session.InviteTrying":
                log.debug("会话尝试中, callId={}, deviceId={}", event.correlationId(), event.deviceId());
                break;
            case "Session.ByeError":
                log.warn("会话结束异常, deviceId={}, payload={}", event.deviceId(), event.payload());
                break;
            case "Session.ServerInvite":
                /*
                 * 平台收到设备主动 INVITE（级联/语音对讲场景）。
                 * G2 收尾：纯拉流场景不受影响；级联/语音对讲需在此处路由回包，
                 * 通过框架 InviteContextStore 找到原 INVITE 节点并 HTTP 转发。
                 * 当前只记录日志，级联/对讲场景的回包逻辑留待后续扩展。
                 */
                log.info("收到设备 INVITE, callId={}", event.correlationId());
                break;

            default:
                log.debug("未处理的 gb28181 事件: {}, deviceId={}", event.type(), event.deviceId());
                break;
        }
    }

    // ================================
    // Lifecycle 处理
    // ================================

    /**
     * 设备注册：payload 为 RegisterInfo（registerTime/expire/transport/localIp/remoteIp/remotePort）。
     */
    private void handleRegister(DeviceEvent event) {
        String deviceId = event.deviceId();
        Map<String, Object> payload = event.payload();

        DeviceRegisterReq req = new DeviceRegisterReq();
        req.setDeviceId(deviceId);
        req.setType(DeviceAgreementEnum.GB28181_IPC.getType());
        if (payload != null) {
            req.setExpire(intValue(payload.get("expire")));
            req.setTransport(stringValue(payload.get("transport")));
            req.setLocalIp(stringValue(payload.get("localIp")));
            req.setRemoteIp(stringValue(payload.get("remoteIp")));
            req.setRemotePort(intValue(payload.get("remotePort")));
            req.setRegisterTime(epochToLocalDateTime(payload.get("registerTime")));
        }
        if (req.getRegisterTime() == null) {
            req.setRegisterTime(LocalDateTime.now());
        }
        deviceRegisterService.login(req);
        log.info("设备注册, deviceId={}, remoteIp={}, remotePort={}", deviceId, req.getRemoteIp(), req.getRemotePort());
        /* SSE device.register */
        Map<String, Object> sseData = new HashMap<>();
        sseData.put("deviceId", deviceId);
        sseData.put("remoteIp", req.getRemoteIp() != null ? req.getRemoteIp() : "");
        sseData.put("remotePort", req.getRemotePort() != null ? req.getRemotePort() : 0);
        sseData.put("transport", req.getTransport() != null ? req.getTransport() : "");
        sseData.put("expire", req.getExpire() != null ? req.getExpire() : 0);
        sseData.put("ts", System.currentTimeMillis());
        eventPublisher.publishEvent(new SseRelayEvent("device.register", sseData));
    }

    /**
     * 远程地址变更：payload 为 RemoteAddressInfo（ip/port）。
     */
    private void handleRemoteAddressChanged(DeviceEvent event) {
        Map<String, Object> payload = event.payload();
        if (payload == null) {
            return;
        }
        String ip = stringValue(payload.get("ip"));
        Integer port = intValue(payload.get("port"));
        if (ip != null && port != null) {
            deviceRegisterService.updateRemoteAddress(event.deviceId(), ip, port);
            log.info("设备远程地址变更, deviceId={}, ip={}, port={}", event.deviceId(), ip, port);
        }
    }

    // ================================
    // Notify 处理
    // ================================

    /**
     * 心跳：刷新 keepaliveTime 并置 ONLINE（Phase 2a：使用心跳合并优化）。
     * <p>
     * 通过 {@link DeviceManager#patchLivenessWithCoalesce} 实现 30s 窗口内的本地合并：
     * <ul>
     *   <li>首次心跳或超出 30s → 写 DB + 刷缓存</li>
     *   <li>30s 内重复心跳 → 仅刷缓存，跳过 DB 写（减少写放大）</li>
     * </ul>
     * </p>
     */
    private void handleKeepalive(DeviceEvent event) {
        DeviceKeepLiveNotify notify = toEntity(event.payload(), DeviceKeepLiveNotify.class);
        String deviceId = notify != null && notify.getDeviceId() != null ? notify.getDeviceId() : event.deviceId();
        if (deviceId != null) {
            // Phase 2a：使用心跳合并版本，减少 30s 内的重复 DB 写
            LocalDateTime keepaliveTime = LocalDateTime.now();
            deviceManager.patchLivenessWithCoalesce(deviceId, DeviceConstant.Status.ONLINE, keepaliveTime);
            log.debug("设备心跳, deviceId={}", deviceId);
            /* SSE device.keepalive：≥5s 节流，避免高频心跳刷屏 */
            long now = System.currentTimeMillis();
            Long last = keepaliveLastSseMs.get(deviceId);
            if (last == null || now - last >= KEEPALIVE_SSE_THROTTLE_MS) {
                keepaliveLastSseMs.put(deviceId, now);
                publishVisual("device.keepalive", deviceId, null, null);
            }
        }
    }

    /**
     * 媒体状态通知：媒体发送结束等价于会话结束。
     */
    private void handleMediaStatus(DeviceEvent event) {
        MediaStatusNotify notify = toEntity(event.payload(), MediaStatusNotify.class);
        String notifyType = notify != null ? notify.getNotifyType() : null;
        if (event.deviceId() != null) {
            mediaSessionManager.onMediaStatus(event.deviceId(), notifyType);
        }
    }

    // ================================
    // Response 处理
    // ================================

    /**
     * 目录响应：批量幂等 upsert 通道（1.0.4：改调 batchUpsertWithStatus，显式落 status/lastSeenTime）。
     */
    private void handleCatalog(DeviceEvent event) {
        DeviceResponse catalog = toEntity(event.payload(), DeviceResponse.class);
        if (catalog == null || catalog.getDeviceItemList() == null || catalog.getDeviceItemList().isEmpty()) {
            log.info("目录响应为空, deviceId={}", event.deviceId());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<DeviceItem> items = catalog.getDeviceItemList();
        List<DeviceChannelDTO> channels = new java.util.ArrayList<>(items.size());
        for (DeviceItem item : items) {
            if (item == null || item.getDeviceId() == null) continue;
            DeviceChannelDTO dto = new DeviceChannelDTO();
            dto.setDeviceId(event.deviceId());
            dto.setChannelId(item.getDeviceId());
            dto.setName(item.getName());
            dto.setStatus(mapItemStatus(item.getStatus()));  // "ON"→1, "OFF"→0, null→null
            dto.setLastSeenTime(now);
            dto.setStatusSource("CATALOG");
            DeviceChannelDTO.ExtendInfo extendInfo = new DeviceChannelDTO.ExtendInfo();
            extendInfo.setChannelInfo(JSON.toJSONString(item));
            dto.setExtendInfo(extendInfo);
            channels.add(dto);
        }
        if (!channels.isEmpty()) {
            deviceChannelManager.batchUpsertWithStatus(event.deviceId(), channels);
        }
        log.info("目录响应处理完成, deviceId={}, 通道数={}", event.deviceId(), channels.size());
        publishVisual("device.catalog", event.deviceId(), "channelCount", channels.size());
    }

    /** "ON"/"ONLINE"→1, "OFF"/"OFFLINE"→0, 其他/null→null（保持原值不覆盖） */
    private Integer mapItemStatus(String s) {
        if (s == null) return null;
        String up = s.trim().toUpperCase();
        if ("ON".equals(up) || "ONLINE".equals(up)) return DeviceConstant.Status.ONLINE;
        if ("OFF".equals(up) || "OFFLINE".equals(up)) return DeviceConstant.Status.OFFLINE;
        return null;
    }

    /**
     * 设备信息响应：更新设备扩展信息。
     */
    private void handleDeviceInfo(DeviceEvent event) {
        DeviceInfo info = toEntity(event.payload(), DeviceInfo.class);
        if (info == null) {
            return;
        }
        DeviceInfoReq req = new DeviceInfoReq();
        req.setDeviceId(event.deviceId());
        req.setDeviceInfo(JSON.toJSONString(info));
        deviceRegisterService.updateDeviceInfo(req);
        log.info("设备信息更新, deviceId={}, model={}", event.deviceId(), info.getModel());
        publishVisual("device.info", event.deviceId(), "manufacturer", info.getManufacturer() != null ? info.getManufacturer() : "");
    }

    private void handleAlarm(DeviceEvent event) {
        Map<String, Object> ap = event.payload() != null ? event.payload() : java.util.Collections.emptyMap();
        AlarmDTO dto = new AlarmDTO();
        dto.setDeviceId(event.deviceId());
        dto.setChannelId(stringValue(ap.get("channelId")));
        dto.setAlarmType(intValue(ap.get("alarmType")));
        dto.setAlarmLevel(intValue(ap.get("alarmLevel")));
        dto.setAlarmTime(java.time.LocalDateTime.now());
        dto.setDescription(stringValue(ap.get("description")));
        Long id = alarmManager.add(dto);
        log.info("告警落库, deviceId={}, id={}", event.deviceId(), id);
        // 推 SSE alarm.new（通过 ApplicationEventPublisher 解耦）
        eventPublisher.publishEvent(new io.github.lunasaw.voglander.common.event.AlarmCreatedEvent(event.deviceId(), ap));
    }

    // ================================
    // payload 工具方法（FastJSON2）
    // ================================

    /**
     * 将 payload Map 经 FastJSON2 正反序列化转换为目标实体（项目类型转换规范）。
     */
    private <T> T toEntity(Map<String, Object> payload, Class<T> clazz) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(JSON.toJSONString(payload), clazz);
        } catch (Exception e) {
            log.warn("payload 转换为 {} 失败: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private int intFromPayload(DeviceEvent event, String key) {
        Map<String, Object> payload = event.payload();
        Integer v = payload != null ? intValue(payload.get(key)) : null;
        return v != null ? v : 0;
    }

    private Integer intValue(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stringValue(Object o) {
        return o != null ? o.toString() : null;
    }

    /**
     * 将 payload 中的时间值（毫秒时间戳 / Date 序列化）转换为 LocalDateTime。
     */
    private LocalDateTime epochToLocalDateTime(Object o) {
        if (o == null) {
            return null;
        }
        try {
            long epochMs;
            if (o instanceof Number) {
                epochMs = ((Number) o).longValue();
            } else {
                epochMs = Long.parseLong(o.toString());
            }
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 发布 SSE 可视化事件（device.* / session.*）。
     *
     * @param topic    SSE 主题
     * @param deviceId 设备 ID（可 null）
     * @param extraKey 额外字段名（可 null）
     * @param extraVal 额外字段值（可 null）
     */
    private void publishVisual(String topic, String deviceId, String extraKey, Object extraVal) {
        Map<String, Object> data = new HashMap<>();
        if (deviceId != null) {
            data.put("deviceId", deviceId);
        }
        if (extraKey != null) {
            data.put(extraKey, extraVal != null ? extraVal : "");
        }
        data.put("ts", System.currentTimeMillis());
        eventPublisher.publishEvent(new SseRelayEvent(topic, data));
    }
}
