package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import io.github.lunasaw.gbproxy.server.transmit.cmd.DeviceSessionCache;
import io.github.lunasaw.sip.common.entity.ToDevice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DeviceSessionCache SPI 实现：委托 VoglanderServerDeviceSupplier 查询设备寻址信息。
 * 必须在 Stage 1 最先落地，是 ServerCommandSender 实例化和 Gb28181GatewayAutoConfiguration
 * 事件管线激活的前置门控（@ConditionalOnBean(ServerCommandSender)）。
 *
 * @author luna
 */
@Component
@RequiredArgsConstructor
public class VoglanderDeviceSessionCache implements DeviceSessionCache {

    private final VoglanderServerDeviceSupplier serverDeviceSupplier;

    @Override
    public ToDevice getToDevice(String deviceId) {
        return serverDeviceSupplier.getToDevice(deviceId);
    }
}
