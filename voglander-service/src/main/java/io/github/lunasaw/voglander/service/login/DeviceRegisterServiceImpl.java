package io.github.lunasaw.voglander.service.login;

import io.github.lunasaw.voglander.client.domain.device.qo.DeviceChannelReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceInfoReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceReq;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.client.service.device.DeviceRegisterService;
import io.github.lunasaw.voglander.common.constant.DeviceConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.service.command.DeviceAgreementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;

/**
 * @author luna
 * @date 2023/12/29
 */
@Slf4j
@Service
public class DeviceRegisterServiceImpl implements DeviceRegisterService {

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private DeviceChannelManager deviceChannelManager;

    @Autowired
    private DeviceAgreementService deviceAgreementService;

    @Override
    public void login(DeviceReq deviceReq) {
        DeviceDTO dto = DeviceDTO.req2dto(deviceReq);
        Long deviceId = deviceManager.saveOrUpdate(dto);
        log.info("login::deviceReq = {}, deviceId = {}", deviceReq, deviceId);

        DeviceCommandService deviceCommandService = deviceAgreementService.getCommandService(dto.getType());
        DeviceQueryReq deviceQueryReq = new DeviceQueryReq();
        deviceQueryReq.setDeviceId(dto.getDeviceId());
        // 查下设备信息
        deviceCommandService.queryDevice(deviceQueryReq);

        // 通道查查询
        deviceCommandService.queryChannel(deviceQueryReq);
    }


    @Override
    public void addChannel(DeviceChannelReq req) {
        DeviceChannelDTO deviceChannelDTO = DeviceChannelDTO.req2dto(req);
        Long addId = deviceChannelManager.saveOrUpdate(deviceChannelDTO);
    }

    @Override
    public void updateDeviceInfo(DeviceInfoReq req) {
        Assert.notNull(req, "req is null");
        DeviceDTO dtoByDeviceId = deviceManager.getDtoByDeviceId(req.getDeviceId());
        if (dtoByDeviceId == null) {
            return;
        }
        DeviceDTO.ExtendInfo extendInfo = dtoByDeviceId.getExtendInfo();
        extendInfo.setDeviceInfo(req.getDeviceInfo());
        deviceManager.saveOrUpdate(dtoByDeviceId);
    }

    @Override
    public void keepalive(String deviceId) {
        Assert.notNull(deviceId, "userId is null");
        DeviceDTO byDeviceId = deviceManager.getDtoByDeviceId(deviceId);
        if (byDeviceId == null) {
            log.info("keepalive::deviceId 找不到设备 = {}", deviceId);
            return;
        }
        byDeviceId.setKeepaliveTime(new Date());
        byDeviceId.setStatus(DeviceConstant.Status.ONLINE);
        deviceManager.saveOrUpdate(byDeviceId);
    }

    @Override
    public void updateRemoteAddress(String deviceId, String ip, Integer port) {
        Assert.notNull(deviceId, "deviceId is null");
        Assert.notNull(ip, "ip is null");
        Assert.notNull(port, "port is null");
        DeviceDTO byDeviceId = deviceManager.getDtoByDeviceId(deviceId);
        if (byDeviceId == null) {
            log.info("keepalive::deviceId 找不到设备 = {}", deviceId);
            return;
        }
        byDeviceId.setIp(ip);
        byDeviceId.setPort(port);
        deviceManager.saveOrUpdate(byDeviceId);
    }

    @Override
    public void offline(String userId) {
        deviceManager.updateStatus(userId, DeviceConstant.Status.OFFLINE);
    }
}
