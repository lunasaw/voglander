package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.notify;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import io.github.lunasaw.gbproxy.server.transimit.request.notify.NotifyProcessorServer;
import io.github.lunasaw.sip.common.entity.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author luna
 * @version 1.0
 * @date 2023/12/14
 * @description:
 */
@Component
public class DefaultNotifyProcessorServer implements NotifyProcessorServer {


    @Override
    public void deviceNotifyUpdate(String userId, DeviceOtherUpdateNotify deviceOtherUpdateNotify) {

    }
}
