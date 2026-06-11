package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Lab BYE 监听器：平台结束点播下发 BYE 时，按 callId 停止对应 ffmpeg 推流。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabByeListener {

    private final LabMediaPushService pushService;

    @EventListener
    public void onBye(ClientByeEvent e) {
        log.info("Lab 收到 BYE, callId={} → 停止推流", e.getCallId());
        pushService.stopByCallId(e.getCallId());
    }
}
