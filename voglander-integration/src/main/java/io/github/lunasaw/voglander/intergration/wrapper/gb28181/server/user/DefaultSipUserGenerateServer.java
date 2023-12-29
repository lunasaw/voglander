package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.user;

import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.start.ServerStart;
import org.springframework.stereotype.Component;

/**
 * @author luna
 * @date 2023/12/29
 */
@Component
public class DefaultSipUserGenerateServer implements SipUserGenerateServer {


    @Override
    public Device getToDevice(String userId) {
        return ServerStart.DEVICE_SERVER_VIEW_MAP.get(userId);
    }

    @Override
    public Device getFromDevice() {
        return ServerStart.DEVICE_MAP.get("server_from");
    }
}
