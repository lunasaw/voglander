package io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceChannelReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceInfoReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRegisterReq;
import io.github.lunasaw.voglander.client.service.device.DeviceRegisterService;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181 网关业务回调，sip-gateway 1.8.0 接入业务层的唯一入口。
 *
 * <p>
 * 直接实现 {@link BusinessNotifier}（覆盖默认 {@code NoopBusinessNotifier}，后者为
 * {@code @ConditionalOnMissingBean}）。<strong>不继承</strong> {@code AbstractProtocolBusinessNotifier}——
 * 其 {@code notify()} 为 {@code final} 且自调用 {@code onProtocolEvent}，无法施加有效 {@code @Async}。
 * 本类在自有 {@link #notify(GatewayEvent)} 上标注 {@code @Async("sipNotifierExecutor")}，
 * Spring 代理可生效，满足框架"notify 必须异步否则设备超时重传"的约束。
 * </p>
 *
 * <p>
 * 事件类型为三段式 {@code gb28181.Group.Name}，payload 为 {@code Map<String,Object>}，
 * 按项目规范用 FastJSON2 反序列化为对应实体。
 * </p>
 *
 * @author luna
 * @since 2025-05-29
 */
@Slf4j
@Component
public class VoglanderBusinessNotifier implements BusinessNotifier {

    @Autowired
    private DeviceRegisterService deviceRegisterService;

    @Autowired
    private DeviceManager         deviceManager;

    @Autowired
    private MediaSessionManager   mediaSessionManager;

    @Override
    @Async("sipNotifierExecutor")
    public void notify(GatewayEvent event) {
        if (event == null || event.type() == null) {
            log.warn("收到空网关事件，忽略");
            return;
        }
        String type = event.type();
        try {
            dispatch(type, event);
        } catch (Exception e) {
            log.error("处理网关事件异常, type={}, deviceId={}, correlationId={}",
                type, event.deviceId(), event.correlationId(), e);
        }
    }

    /**
     * 按事件类型三段式分发。
     */
    private void dispatch(String type, GatewayEvent event) {
        switch (type) {
            // ========== Lifecycle (5) ==========
            case "gb28181.Lifecycle.Register":
                handleRegister(event);
                break;
            case "gb28181.Lifecycle.Online":
                deviceManager.updateStatus(event.deviceId(), DeviceConstant.Status.ONLINE);
                log.info("设备上线, deviceId={}", event.deviceId());
                break;
            case "gb28181.Lifecycle.Offline":
                deviceRegisterService.offline(event.deviceId());
                log.info("设备离线, deviceId={}", event.deviceId());
                break;
            case "gb28181.Lifecycle.RemoteAddressChanged":
                handleRemoteAddressChanged(event);
                break;
            case "gb28181.Lifecycle.RegisterChallenge":
                log.debug("设备注册挑战, deviceId={}", event.deviceId());
                break;

            // ========== Notify (7) ==========
            case "gb28181.Notify.Keepalive":
                handleKeepalive(event);
                break;
            case "gb28181.Notify.Alarm":
                log.info("设备告警通知, deviceId={}, payload={}", event.deviceId(), event.payload());
                break;
            case "gb28181.Notify.MobilePosition":
                log.info("设备移动位置通知, deviceId={}, payload={}", event.deviceId(), event.payload());
                break;
            case "gb28181.Notify.MediaStatus":
                handleMediaStatus(event);
                break;
            case "gb28181.Notify.UpgradeResult":
            case "gb28181.Notify.SnapShotFinished":
            case "gb28181.Notify.VideoUpload":
                log.info("设备通知事件, type={}, deviceId={}", type, event.deviceId());
                break;

            // ========== Response (16) ==========
            case "gb28181.Response.Catalog":
                handleCatalog(event);
                break;
            case "gb28181.Response.DeviceInfo":
                handleDeviceInfo(event);
                break;
            case "gb28181.Response.DeviceStatus":
            case "gb28181.Response.RecordInfo":
            case "gb28181.Response.PtzPosition":
            case "gb28181.Response.SdCardStatus":
            case "gb28181.Response.HomePosition":
            case "gb28181.Response.CruiseTrackList":
            case "gb28181.Response.CruiseTrack":
            case "gb28181.Response.Config":
            case "gb28181.Response.ConfigDownload":
            case "gb28181.Response.PresetQuery":
            case "gb28181.Response.Subscribe":
            case "gb28181.Response.NotifyUpdate":
            case "gb28181.Response.DeviceInfoError":
            case "gb28181.Response.DeviceInfoRequest":
                log.info("设备响应事件, type={}, deviceId={}, sn={}", type, event.deviceId(), event.correlationId());
                break;

            // ========== Session (7) ==========
            case "gb28181.Session.InviteOk":
                mediaSessionManager.onInviteOk(event.correlationId(), event.deviceId());
                log.info("会话建立, callId={}, deviceId={}", event.correlationId(), event.deviceId());
                break;
            case "gb28181.Session.InviteFailure":
                mediaSessionManager.onInviteFailure(event.correlationId(), intFromPayload(event, "statusCode"));
                break;
            case "gb28181.Session.Ack":
                mediaSessionManager.onAck(event.correlationId());
                break;
            case "gb28181.Session.Bye":
                if (event.deviceId() != null) {
                    mediaSessionManager.onBye(event.deviceId());
                }
                log.info("会话结束(BYE), deviceId={}", event.deviceId());
                break;
            case "gb28181.Session.InviteTrying":
                log.debug("会话尝试中, callId={}, deviceId={}", event.correlationId(), event.deviceId());
                break;
            case "gb28181.Session.ByeError":
                log.warn("会话结束异常, deviceId={}, payload={}", event.deviceId(), event.payload());
                break;
            case "gb28181.Session.ServerInvite":
                // 平台收到设备 INVITE（级联/语音对讲场景），由 Gb28181InviteResponseController 处理回包路由
                log.info("收到设备 INVITE, callId={}, payload={}", event.correlationId(), event.payload());
                break;

            default:
                log.debug("未处理的网关事件类型: {}, deviceId={}", type, event.deviceId());
                break;
        }
    }

    // ================================
    // Lifecycle 处理
    // ================================

    /**
     * 设备注册：payload 为 RegisterInfo（registerTime/expire/transport/localIp/remoteIp/remotePort）。
     */
    private void handleRegister(GatewayEvent event) {
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
    }

    /**
     * 远程地址变更：payload 为 RemoteAddressInfo（ip/port）。
     */
    private void handleRemoteAddressChanged(GatewayEvent event) {
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
     * 心跳：刷新 keepaliveTime 并置 ONLINE。
     */
    private void handleKeepalive(GatewayEvent event) {
        DeviceKeepLiveNotify notify = toEntity(event.payload(), DeviceKeepLiveNotify.class);
        String deviceId = notify != null && notify.getDeviceId() != null ? notify.getDeviceId() : event.deviceId();
        if (deviceId != null) {
            deviceRegisterService.keepalive(deviceId);
            log.debug("设备心跳, deviceId={}", deviceId);
        }
    }

    /**
     * 媒体状态通知：媒体发送结束等价于会话结束。
     */
    private void handleMediaStatus(GatewayEvent event) {
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
     * 目录响应：批量保存/更新通道。catalog.deviceItemList 中每个 DeviceItem 对应一个通道。
     */
    private void handleCatalog(GatewayEvent event) {
        DeviceResponse catalog = toEntity(event.payload(), DeviceResponse.class);
        if (catalog == null || catalog.getDeviceItemList() == null || catalog.getDeviceItemList().isEmpty()) {
            log.info("目录响应为空, deviceId={}", event.deviceId());
            return;
        }
        List<DeviceItem> items = catalog.getDeviceItemList();
        int count = 0;
        for (DeviceItem item : items) {
            if (item == null || item.getDeviceId() == null) {
                continue;
            }
            DeviceChannelReq req = new DeviceChannelReq();
            req.setDeviceId(event.deviceId());
            req.setChannelId(item.getDeviceId());
            req.setChannelName(item.getName());
            req.setChannelInfo(JSON.toJSONString(item));
            deviceRegisterService.addChannel(req);
            count++;
        }
        log.info("目录响应处理完成, deviceId={}, 通道数={}", event.deviceId(), count);
    }

    /**
     * 设备信息响应：更新设备扩展信息。
     */
    private void handleDeviceInfo(GatewayEvent event) {
        DeviceInfo info = toEntity(event.payload(), DeviceInfo.class);
        if (info == null) {
            return;
        }
        DeviceInfoReq req = new DeviceInfoReq();
        req.setDeviceId(event.deviceId());
        req.setDeviceInfo(JSON.toJSONString(info));
        deviceRegisterService.updateDeviceInfo(req);
        log.info("设备信息更新, deviceId={}, model={}", event.deviceId(), info.getModel());
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

    private int intFromPayload(GatewayEvent event, String key) {
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
}
