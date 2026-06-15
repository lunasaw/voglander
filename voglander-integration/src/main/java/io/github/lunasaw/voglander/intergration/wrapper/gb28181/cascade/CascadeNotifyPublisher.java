package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant.SubType;
import io.github.lunasaw.voglander.common.event.LocalMobilePositionEvent;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeSubscribeDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.github.lunasaw.voglander.manager.manager.CascadeSubscribeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联主动推送发布器（C3/C4/C5，出站推送单点收口）。
 *
 * <p>本地数据变更（通道上下线 / 告警 / 移动位置）时，遍历订阅了对应信息类型的上级，
 * 逐个主动 NOTIFY 推送。推送给上级的实体 deviceId 必须重映射为上级认识的
 * {@code cascade_channel_id}（不是本地 {@code local_channel_id}），重映射逻辑单点收口于本类。
 *
 * <p>dialog（V2 核验）：框架按 From/To 自管订阅 dialog，推送命令无需 callId 入参。
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeNotifyPublisher {

    private final CascadeSubscribeManager cascadeSubscribeManager;
    private final CascadePlatformManager  cascadePlatformManager;
    private final CascadeChannelManager   cascadeChannelManager;
    private final CascadeDeviceSupplier   cascadeDeviceSupplier;

    /**
     * 目录变更 → 推送所有订阅了 Catalog 的上级。
     *
     * @param event ON/OFF/ADD/DEL/UPDATE
     */
    public void pushCatalogChange(String localDeviceId, String localChannelId, String event) {
        for (CascadeSubscribeDTO sub : cascadeSubscribeManager.listActiveByType(SubType.CATALOG)) {
            CascadeChannelDTO ch = cascadeChannelManager.getByPlatformAndChannel(sub.getPlatformId(), localChannelId);
            if (ch == null) {
                continue; // 该上级未暴露此通道
            }
            Endpoint ep = endpoint(sub.getPlatformId());
            if (ep == null) {
                continue;
            }
            DeviceOtherUpdateNotify.OtherItem item = new DeviceOtherUpdateNotify.OtherItem();
            item.setDeviceId(ch.getCascadeChannelId()); // 重映射为上级编码
            item.setEvent(event);
            ClientCommandSender.sendCatalogChangeNotify(ep.from, ep.to, sub.getSn(), List.of(item));
            log.info("推送目录变更: platformId={}, cascadeChannelId={}, event={}",
                sub.getPlatformId(), ch.getCascadeChannelId(), event);
        }
    }

    /** 告警 → 推送所有订阅了 Alarm 的上级。 */
    public void pushAlarm(String localDeviceId, String localChannelId, String alarmType,
        String alarmPriority, String alarmTime) {
        for (CascadeSubscribeDTO sub : cascadeSubscribeManager.listActiveByType(SubType.ALARM)) {
            CascadeChannelDTO ch = cascadeChannelManager.getByPlatformAndChannel(sub.getPlatformId(), localChannelId);
            // 通道级告警须找到映射；设备级告警(channelId 为空)用平台 localClientId 兜底
            String cascadeDeviceId = ch != null ? ch.getCascadeChannelId() : null;
            if (cascadeDeviceId == null && localChannelId != null) {
                continue; // 指明了通道但该上级未暴露 → 跳过
            }
            Endpoint ep = endpoint(sub.getPlatformId());
            if (ep == null) {
                continue;
            }
            String targetDeviceId = cascadeDeviceId != null ? cascadeDeviceId : ep.to.getUserId();
            DeviceAlarmNotify notify = new DeviceAlarmNotify();
            notify.setCmdType(CmdTypeEnum.ALARM.getType());
            notify.setSn(sub.getSn());
            notify.setDeviceId(targetDeviceId); // 重映射为上级编码
            notify.setAlarmPriority(alarmPriority);
            notify.setAlarmTime(alarmTime);
            ClientCommandSender.sendAlarmNotify(ep.from, ep.to, notify);
            log.info("推送告警: platformId={}, deviceId={}, type={}", sub.getPlatformId(), targetDeviceId, alarmType);
        }
    }

    /** 移动位置 → 推送所有订阅了 MobilePosition 的上级。 */
    public void pushMobilePosition(LocalMobilePositionEvent e) {
        for (CascadeSubscribeDTO sub : cascadeSubscribeManager.listActiveByType(SubType.MOBILE_POSITION)) {
            CascadeChannelDTO ch = cascadeChannelManager.getByPlatformAndChannel(sub.getPlatformId(), e.getChannelId());
            String cascadeDeviceId = ch != null ? ch.getCascadeChannelId() : null;
            if (cascadeDeviceId == null && e.getChannelId() != null) {
                continue;
            }
            Endpoint ep = endpoint(sub.getPlatformId());
            if (ep == null) {
                continue;
            }
            MobilePositionNotify notify = new MobilePositionNotify();
            notify.setDeviceId(cascadeDeviceId != null ? cascadeDeviceId : ep.to.getUserId());
            notify.setTime(e.getTime());
            notify.setLongitude(parseDouble(e.getLongitude()));
            notify.setLatitude(parseDouble(e.getLatitude()));
            notify.setSpeed(parseDouble(e.getSpeed()));
            notify.setDirection(parseDouble(e.getDirection()));
            notify.setAltitude(parseDouble(e.getAltitude()));
            ClientCommandSender.sendMobilePositionNotify(ep.from, ep.to, notify);
            log.info("推送移动位置: platformId={}, deviceId={}", sub.getPlatformId(), notify.getDeviceId());
        }
    }

    /** 构造某上级的 From/To 端点；平台不存在则 null。 */
    private Endpoint endpoint(String platformId) {
        CascadePlatformDTO platform = cascadePlatformManager.getByPlatformId(platformId);
        if (platform == null) {
            log.warn("级联推送找不到上级平台配置: platformId={}", platformId);
            return null;
        }
        FromDevice from = cascadeDeviceSupplier.buildFromDevice(platform);
        ToDevice   to   = cascadeDeviceSupplier.buildToDevice(platform);
        return new Endpoint(from, to);
    }

    private Double parseDouble(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class Endpoint {
        final FromDevice from;
        final ToDevice   to;

        Endpoint(FromDevice from, ToDevice to) {
            this.from = from;
            this.to = to;
        }
    }
}
