package io.github.lunasaw.voglander.service.login;

import io.github.lunasaw.voglander.common.constant.DeviceConstant;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.start.SipServerConfig;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.domain.dto.DeviceDTO.ExtendInfo;

import java.util.Date;

import io.github.lunasaw.voglander.client.domain.qo.DeviceReq;
import io.github.lunasaw.voglander.client.service.LoginService;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.repository.domain.dto.DeviceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author luna
 * @date 2023/12/29
 */
@Slf4j
@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SipServerConfig sipServerConfig;

    @Override
    public void login(DeviceReq deviceReq) {

        DeviceDTO dto = new DeviceDTO();

        dto.setDeviceId(deviceReq.getUserId());
        dto.setStatus(DeviceConstant.Status.ONLINE);
        dto.setIp(deviceReq.getRemoteIp());
        dto.setPort(deviceReq.getRemotePort());
        dto.setRegisterTime(deviceReq.getRegisterTime());
        dto.setKeepaliveTime(new Date());
        dto.setServerIp(deviceReq.getLocalIp());
        ExtendInfo extendInfo = new ExtendInfo();
        extendInfo.setTransport(deviceReq.getTransport());
        extendInfo.setExpires(deviceReq.getExpire());
        dto.setExtendInfo(extendInfo);

        Long deviceId = deviceManager.saveOrUpdate(dto);
        log.info("login::deviceReq = {}, deviceId = {}", deviceReq, deviceId);
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
