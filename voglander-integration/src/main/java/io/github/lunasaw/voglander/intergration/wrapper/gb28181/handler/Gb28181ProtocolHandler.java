package io.github.lunasaw.voglander.intergration.wrapper.gb28181.handler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

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
                // Phase 2a：上线走定向更新（仅 status 一列），消除读整行+全行写放大（修 P3）
                deviceManager.patchLiveness(event.deviceId(), DeviceConstant.Status.ONLINE, null);
                log.info("设备上线, deviceId={}", event.deviceId());
                break;
            case "Lifecycle.Offline":
                deviceRegisterService.offline(event.deviceId());
                log.info("设备离线, deviceId={}", event.deviceId());
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
                log.info("设备告警通知, deviceId={}, payload={}", event.deviceId(), event.payload());
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
                mediaSessionManager.onInviteOk(event.correlationId(), event.deviceId());
                log.info("会话建立, callId={}, deviceId={}", event.correlationId(), event.deviceId());
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
                break;
            case "Session.InviteTrying":
                log.debug("会话尝试中, callId={}, deviceId={}", event.correlationId(), event.deviceId());
                break;
            case "Session.ByeError":
                log.warn("会话结束异常, deviceId={}, payload={}", event.deviceId(), event.payload());
                break;
            case "Session.ServerInvite":
                // 平台收到设备 INVITE（级联/语音对讲场景）。G2 缺口：回包路径未落地（见 doc/1.0.3）。
                log.info("收到设备 INVITE, callId={}, payload={}", event.correlationId(), event.payload());
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
     * 心跳：刷新 keepaliveTime 并置 ONLINE。
     */
    private void handleKeepalive(DeviceEvent event) {
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
     * 目录响应：批量幂等 upsert 通道（Phase 4：修 P4 N+1 → 单次 batchUpsert）。
     * catalog.deviceItemList 中每个 DeviceItem 对应一个通道。
     */
    private void handleCatalog(DeviceEvent event) {
        DeviceResponse catalog = toEntity(event.payload(), DeviceResponse.class);
        if (catalog == null || catalog.getDeviceItemList() == null || catalog.getDeviceItemList().isEmpty()) {
            log.info("目录响应为空, deviceId={}", event.deviceId());
            return;
        }
        List<DeviceItem> items = catalog.getDeviceItemList();
        List<DeviceChannelDTO> channels = new java.util.ArrayList<>(items.size());
        for (DeviceItem item : items) {
            if (item == null || item.getDeviceId() == null) {
                continue;
            }
            DeviceChannelDTO dto = new DeviceChannelDTO();
            dto.setDeviceId(event.deviceId());
            dto.setChannelId(item.getDeviceId());
            dto.setName(item.getName());
            DeviceChannelDTO.ExtendInfo extendInfo = new DeviceChannelDTO.ExtendInfo();
            extendInfo.setChannelInfo(JSON.toJSONString(item));
            dto.setExtendInfo(extendInfo);
            channels.add(dto);
        }
        if (!channels.isEmpty()) {
            deviceChannelManager.batchUpsert(channels);
        }
        log.info("目录响应处理完成, deviceId={}, 通道数={}", event.deviceId(), channels.size());
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
}
