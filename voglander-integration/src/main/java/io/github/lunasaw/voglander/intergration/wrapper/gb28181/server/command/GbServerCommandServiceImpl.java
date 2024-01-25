package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import io.github.lunasaw.gbproxy.server.transimit.cmd.ServerSendCmd;
import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.constant.DeviceConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author luna
 * @date 2023/12/31
 */
@Service(value = DeviceConstant.DeviceCommandService.DEVICE_AGREEMENT_SERVICE_NAME_GB28181)
public class GbServerCommandServiceImpl implements DeviceCommandService {

    @Autowired
    private SipUserGenerateServer sipUserGenerateServer;

    @Override
    public ResultDTO<Void> queryChannel(DeviceQueryReq deviceQueryReq) {
        FromDevice fromDevice = getDevice();
        ToDevice toDevice = getToDevice(deviceQueryReq);
        ServerSendCmd.deviceCatalogQuery(fromDevice, toDevice);
        return ResultDTOUtils.success();
    }

    @Override
    public ResultDTO<Void> queryDevice(DeviceQueryReq deviceQueryReq) {
        FromDevice fromDevice = getDevice();
        ToDevice toDevice = getToDevice(deviceQueryReq);
        ServerSendCmd.deviceInfo(fromDevice, toDevice);
        return ResultDTOUtils.success();
    }

    private ToDevice getToDevice(DeviceQueryReq deviceQueryReq) {
        Device toDevice = sipUserGenerateServer.getToDevice(deviceQueryReq.getDeviceId());
        return (ToDevice) toDevice;
    }

    private FromDevice getDevice() {
        Device fromDevice = sipUserGenerateServer.getFromDevice();
        return (FromDevice) fromDevice;
    }
}
