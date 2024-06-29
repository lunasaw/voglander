package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server;

import io.github.lunasaw.sip.common.transmit.SipProcessorInject;
import io.github.lunasaw.voglander.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.EventObject;

/**
 * @author luna
 * @date 2023/12/29
 */
@Component
@Slf4j
public class DefaultSipProcessorInject implements SipProcessorInject {


    @Override
    public void before(EventObject eventObject) {
        MDC.put(Constants.SKY_WALKING_TID, TraceContext.traceId());
    }

    @Override
    public void after() {

    }
}
