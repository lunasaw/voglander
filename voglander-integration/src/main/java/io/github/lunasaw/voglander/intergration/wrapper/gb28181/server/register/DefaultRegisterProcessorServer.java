package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.register;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.gbproxy.server.transimit.request.register.RegisterInfo;
import io.github.lunasaw.gbproxy.server.transimit.request.register.RegisterProcessorServer;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import io.github.lunasaw.voglander.client.domain.qo.DeviceReq;
import io.github.lunasaw.voglander.client.service.DeviceRegisterService;
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
    public SipTransaction getTransaction(String userId) {
        return sipTransactionMap.get(userId);
    }

    @Override
    @Trace
    public void updateRegisterInfo(String userId, RegisterInfo registerInfo) {

        DeviceReq deviceReq = new DeviceReq();
        deviceReq.setDeviceId(userId);
        deviceReq.setRegisterTime(registerInfo.getRegisterTime());
        deviceReq.setExpire(registerInfo.getExpire());
        deviceReq.setTransport(registerInfo.getTransport());
        deviceReq.setLocalIp(registerInfo.getLocalIp());
        deviceReq.setRemoteIp(registerInfo.getRemoteIp());
        deviceReq.setRemotePort(registerInfo.getRemotePort());
        deviceReq.setType(DeviceAgreementEnum.GB28181.getType());
        deviceRegisterService.login(deviceReq);

        log.info("国标设备注册更新::userId = {}, registerInfo = {}", userId, JSON.toJSONString(registerInfo));
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
