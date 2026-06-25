package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlAlarm;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlGuard;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlRecordCmd;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTeleBoot;
import io.github.lunasaw.gbproxy.client.api.ControlListener;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm.VoglanderServerAlarmCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.guard.VoglanderServerGuardCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stage 3 — 接收上级平台下发的控制指令并转发给真实下级设备。
 * ControlListener 允许多实例（观察者模式），无唯一性约束。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeControlHandler implements ControlListener {

    private final CascadeChannelManager        cascadeChannelManager;
    private final VoglanderServerPtzCommand    serverPtzCommand;
    private final VoglanderServerRecordCommand serverRecordCommand;
    private final VoglanderServerGuardCommand  serverGuardCommand;
    private final VoglanderServerAlarmCommand  serverAlarmCommand;
    private final VoglanderServerDeviceCommand serverDeviceCommand;

    /**
     * 解析级联通道，找不到返回 null。
     * <p>
     * ControlListener 是多实例观察者，框架把所有控制指令广播给每个 listener。
     * 非本级联管理的通道（如 Lab 模拟设备、其他 listener 负责的通道）查不到属正常，
     * 仅记 debug，由各自的 listener 处理自身业务，互不耦合。
     */
    private CascadeChannelDTO resolveChannel(String platformId, String cascadeChannelId, String action) {
        CascadeChannelDTO channel = cascadeChannelManager.getByPlatformAndCascadeChannelId(platformId, cascadeChannelId);
        if (channel == null) {
            log.debug("{}非本级联通道，跳过 platformId={}, cascadeChannelId={}", action, platformId, cascadeChannelId);
        }
        return channel;
    }

    /**
     * 上级平台下发 PTZ 控制 → 找到真实下级设备 → 透传 hex 指令
     */
    @Override
    public void onPtzControl(String platformId, DeviceControlPtz cmd) {
        CascadeChannelDTO channel = resolveChannel(platformId, cmd.getDeviceId(), "PTZ");
        if (channel == null) {
            return;
        }
        String ptzCmd = cmd.getPtzCmd();
        log.info("PTZ 转发: {} → localDevice={}, hex={}", cmd.getDeviceId(), channel.getLocalDeviceId(), ptzCmd);
        serverPtzCommand.controlDevicePtz(channel.getLocalDeviceId(), ptzCmd);
    }

    /**
     * 上级平台下发回放控制（C9，INFO 方法携带 PLAY/PAUSE/FAST/SLOW/SCALE 等） → 透传给真实设备。
     */
    @Override
    public void onRecord(String platformId, DeviceControlRecordCmd cmd) {
        CascadeChannelDTO channel = resolveChannel(platformId, cmd.getDeviceId(), "回放控制");
        if (channel == null) {
            return;
        }
        String recordCmd = cmd.getRecordCmd();
        log.info("回放控制转发: {} → localDevice={}, cmd={}", cmd.getDeviceId(), channel.getLocalDeviceId(), recordCmd);
        serverRecordCommand.controlDeviceRecord(channel.getLocalDeviceId(), recordCmd);
    }

    /**
     * 上级平台下发布防/撤防（C10） → 透传给真实设备。
     */
    @Override
    public void onGuard(String platformId, DeviceControlGuard cmd) {
        CascadeChannelDTO channel = resolveChannel(platformId, cmd.getDeviceId(), "布防/撤防");
        if (channel == null) {
            return;
        }
        String guardCmd = cmd.getGuardCmd();
        log.info("布防/撤防转发: {} → localDevice={}, guardCmd={}", cmd.getDeviceId(), channel.getLocalDeviceId(), guardCmd);
        serverGuardCommand.controlDeviceGuard(channel.getLocalDeviceId(), guardCmd);
    }

    /**
     * 上级平台下发告警复位（C10） → 透传给真实设备。
     */
    @Override
    public void onAlarmReset(String platformId, DeviceControlAlarm cmd) {
        CascadeChannelDTO channel = resolveChannel(platformId, cmd.getDeviceId(), "告警复位");
        if (channel == null) {
            return;
        }
        String alarmMethod = cmd.getAlarmInfo() != null ? cmd.getAlarmInfo().getAlarmMethod() : null;
        String alarmType = cmd.getAlarmInfo() != null ? cmd.getAlarmInfo().getAlarmType() : null;
        log.info("告警复位转发: {} → localDevice={}, method={}, type={}",
            cmd.getDeviceId(), channel.getLocalDeviceId(), alarmMethod, alarmType);
        serverAlarmCommand.controlDeviceAlarm(channel.getLocalDeviceId(), alarmMethod, alarmType);
    }

    /**
     * 上级平台下发远程重启（C10） → 透传给真实设备。
     */
    @Override
    public void onTeleBoot(String platformId, DeviceControlTeleBoot cmd) {
        CascadeChannelDTO channel = resolveChannel(platformId, cmd.getDeviceId(), "远程重启");
        if (channel == null) {
            return;
        }
        log.info("远程重启转发: {} → localDevice={}", cmd.getDeviceId(), channel.getLocalDeviceId());
        serverDeviceCommand.teleBoot(channel.getLocalDeviceId());
    }
}
