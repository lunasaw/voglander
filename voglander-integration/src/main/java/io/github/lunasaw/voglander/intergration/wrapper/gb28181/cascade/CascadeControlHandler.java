package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gbproxy.client.api.ControlListener;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
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

    private final CascadeChannelManager   cascadeChannelManager;
    private final VoglanderServerPtzCommand serverPtzCommand;

    /**
     * 上级平台下发 PTZ 控制 → 找到真实下级设备 → 透传 hex 指令
     */
    @Override
    public void onPtzControl(String platformId, DeviceControlPtz cmd) {
        String cascadeChannelId = cmd.getDeviceId();
        CascadeChannelDTO channel = cascadeChannelManager.getByPlatformAndCascadeChannelId(platformId, cascadeChannelId);
        if (channel == null) {
            log.warn("PTZ 转发失败：未找到级联通道 platformId={}, cascadeChannelId={}", platformId, cascadeChannelId);
            return;
        }
        String ptzCmd = cmd.getPtzCmd();
        log.info("PTZ 转发: {} → localDevice={}, hex={}", cascadeChannelId, channel.getLocalDeviceId(), ptzCmd);
        serverPtzCommand.controlDevicePtz(channel.getLocalDeviceId(), ptzCmd);
    }
}
