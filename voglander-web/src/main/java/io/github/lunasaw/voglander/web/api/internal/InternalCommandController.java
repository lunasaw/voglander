package io.github.lunasaw.voglander.web.api.internal;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * 内部命令接收端点：接收其他节点转发的命令并本地下发。
 */
@Slf4j
@RestController
@RequestMapping("/internal/sip")
@ConditionalOnProperty(name = "voglander.command.affinity-route.enabled", havingValue = "true", matchIfMissing = false)
public class InternalCommandController {

    @Autowired
    private CommandHandlerRegistry commandHandlerRegistry;

    @PostMapping("/command")
    public void handleCommand(@RequestBody Map<String, Object> payload) {
        String type = (String) payload.get("type");
        String deviceId = (String) payload.get("deviceId");
        @SuppressWarnings("unchecked")
        Map<String, Object> cmdPayload = (Map<String, Object>) payload.get("payload");

        log.info("internal::收到转发命令, type={}, deviceId={}", type, deviceId);
        GatewayCommand cmd = new GatewayCommand(type, deviceId, cmdPayload, null);
        commandHandlerRegistry.require(type).handle(cmd);
    }
}
