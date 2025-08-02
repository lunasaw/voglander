package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.request.ack;

import io.github.lunasaw.gbproxy.client.transmit.request.ack.AckRequestHandler;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * @author luna
 * @date 2025/7/31
 */
@Component
public class VoglanderClientAckRequestHandler implements AckRequestHandler {
    @Override
    public void processAck(RequestEvent evt) {

    }
}
