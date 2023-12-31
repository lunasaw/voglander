package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.util;

import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;

/**
 * @author luna
 * @date 2023/12/30
 */
public class GbDeviceServerUtil {

    public static ToDevice getToDevice(DeviceDTO deviceDTO) {
        if (deviceDTO == null) {
            return null;
        }
        ToDevice device = new ToDevice();

        device.setLocalIp(deviceDTO.getServerIp());
        device.setUserId(deviceDTO.getDeviceId());
        device.setIp(deviceDTO.getIp());
        device.setPort(deviceDTO.getPort());
        device.setHostAddress(device.getHostAddress());

        DeviceDTO.ExtendInfo extendInfo = deviceDTO.getExtendInfo();

        if (extendInfo != null) {
            device.setPassword(extendInfo.getPassword());
            device.setRealm(deviceDTO.getDeviceId().substring(0, 9));
            device.setTransport(extendInfo.getTransport());
            device.setStreamMode(extendInfo.getStreamMode());
            device.setCharset(extendInfo.getCharset());
        }

        return device;
    }
}
