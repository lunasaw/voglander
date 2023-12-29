package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server;

import io.github.lunasaw.sip.common.transmit.SipProcessorInject;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.domain.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EventObject;
import java.util.List;

/**
 * @author luna
 * @date 2023/12/29
 */
@Component
@Slf4j
public class DefaultSipProcessorInject implements SipProcessorInject {

    @Autowired
    private DeviceService deviceService;

    @Override
    @Trace(operationName = "sip")
    public void before(EventObject eventObject) {
        MDC.put("tid", TraceContext.traceId());
        List<DeviceDO> list = deviceService.list();
        log.info("before::eventObject = {}", list);
    }

    @Override
    public void after() {

    }
}
