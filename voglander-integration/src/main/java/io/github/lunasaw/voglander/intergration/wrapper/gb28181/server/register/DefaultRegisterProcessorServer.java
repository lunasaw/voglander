package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.register;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.gbproxy.server.transimit.request.register.RegisterInfo;
import io.github.lunasaw.gbproxy.server.transimit.request.register.RegisterProcessorServer;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.start.ServerStart;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
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


    @Override
    public SipTransaction getTransaction(String userId) {
        return sipTransactionMap.get(userId);
    }

    @Override
    @Trace
    public void updateRegisterInfo(String userId, RegisterInfo registerInfo) {

        ToDevice instance = ToDevice.getInstance(userId, registerInfo.getRemoteIp(), registerInfo.getRemotePort());
        instance.setTransport(registerInfo.getTransport());
        instance.setLocalIp(registerInfo.getLocalIp());

        ServerStart.DEVICE_SERVER_VIEW_MAP.put(userId, instance);

        log.info("设备注册更新::userId = {}, registerInfo = {}", userId, JSON.toJSONString(registerInfo));
    }

    @Override
    public void updateSipTransaction(String userId, SipTransaction sipTransaction) {
        log.info("设备注册::userId = {}, sipTransaction = {}", userId, sipTransaction);
        sipTransactionMap.put(userId, sipTransaction);
    }

    @Override
    public void deviceOffLine(String userId, RegisterInfo registerInfo, SipTransaction sipTransaction) {
        log.info("设备注销::userId = {}, sipTransaction = {}", userId, sipTransaction);
        ServerStart.DEVICE_SERVER_VIEW_MAP.remove(userId);
        sipTransactionMap.remove(userId);
    }
}