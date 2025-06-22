package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.register;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.gbproxy.server.transimit.request.register.RegisterInfo;
import io.github.lunasaw.gbproxy.server.transimit.request.register.RegisterProcessorServer;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRegisterReq;
import io.github.lunasaw.voglander.client.service.device.DeviceRegisterService;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author luna
 * @date 2023/10/18
 */
@Slf4j
@Component
public class DefaultRegisterProcessorServer implements RegisterProcessorServer {

    public static Map<String, SipTransaction> sipTransactionMap = new ConcurrentHashMap<>();
    @Autowired
    private DeviceRegisterService deviceRegisterService;

    @Override
    public void responseUnauthorized(String userId) {

    }

    @Override
    public SipTransaction getTransaction(String userId) {
        return sipTransactionMap.get(userId);
    }

    @Override
    @Trace
    public void updateRegisterInfo(String userId, RegisterInfo registerInfo) {
        log.info("国标设备注册更新::userId = {}, registerInfo = {}", userId, JSON.toJSONString(registerInfo));

        DeviceRegisterReq deviceRegisterReq = new DeviceRegisterReq();
        deviceRegisterReq.setDeviceId(userId);
        deviceRegisterReq.setRegisterTime(registerInfo.getRegisterTime());
        deviceRegisterReq.setExpire(registerInfo.getExpire());
        deviceRegisterReq.setTransport(registerInfo.getTransport());
        deviceRegisterReq.setLocalIp(registerInfo.getLocalIp());
        deviceRegisterReq.setRemoteIp(registerInfo.getRemoteIp());
        deviceRegisterReq.setRemotePort(registerInfo.getRemotePort());
        deviceRegisterReq.setType(DeviceAgreementEnum.GB28181.getType());
        deviceRegisterService.login(deviceRegisterReq);
    }

    @Override
    public void updateSipTransaction(String userId, SipTransaction sipTransaction) {
        sipTransactionMap.put(userId, sipTransaction);
    }

    @Override
    public void deviceOffLine(String userId, RegisterInfo registerInfo, SipTransaction sipTransaction) {
        log.info("国标设备注销::userId = {}, sipTransaction = {}", userId, sipTransaction);
        deviceRegisterService.offline(userId);
        sipTransactionMap.remove(userId);
    }
}
