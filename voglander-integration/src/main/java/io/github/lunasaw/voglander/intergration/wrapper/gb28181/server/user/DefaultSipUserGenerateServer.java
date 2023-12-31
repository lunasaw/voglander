package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.user;

import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.util.GbDeviceServerUtil;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.start.ServerStart;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author luna
 * @date 2023/12/29
 */
@Component
public class DefaultSipUserGenerateServer implements SipUserGenerateServer {

    @Autowired
    private DeviceManager deviceManager;

    @Override
    public Device getToDevice(String userId) {
        DeviceDTO deviceDTO = deviceManager.getDtoByDeviceId(userId);
        return GbDeviceServerUtil.getToDevice(deviceDTO);
    }

    @Override
    public Device getFromDevice() {
        return ServerStart.DEVICE_MAP.get("server_from");
    }


}
