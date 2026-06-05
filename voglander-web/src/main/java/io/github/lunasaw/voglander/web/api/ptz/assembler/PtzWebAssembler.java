package io.github.lunasaw.voglander.web.api.ptz.assembler;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.client.domain.device.qo.DevicePtzReq;
import io.github.lunasaw.voglander.web.api.ptz.domain.PtzControlReq;
import io.github.lunasaw.voglander.web.api.ptz.domain.PtzStopReq;

@Component
public class PtzWebAssembler {

    public DevicePtzReq toReq(PtzControlReq req) {
        DevicePtzReq r = new DevicePtzReq();
        r.setDeviceId(req.getDeviceId());
        r.setControl(req.getCommand());
        r.setSpeed(req.getSpeed());
        return r;
    }

    public DevicePtzReq toStopReq(PtzStopReq req) {
        DevicePtzReq r = new DevicePtzReq();
        r.setDeviceId(req.getDeviceId());
        r.setControl("STOP");
        r.setSpeed(0);
        return r;
    }
}
