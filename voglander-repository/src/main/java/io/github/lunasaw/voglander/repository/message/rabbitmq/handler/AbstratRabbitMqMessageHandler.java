package io.github.lunasaw.voglander.repository.message.rabbitmq.handler;

import io.github.lunasaw.voglander.repository.message.MessageHandler;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2024/6/26
 */
@Component
@Slf4j
public class AbstratRabbitMqMessageHandler implements MessageHandler {
    @Override
    public void handle(String message) {
        log.info("handle::message = {}", message);
    }

    @Override
    public boolean accept(String topic, String message) {
        return true;
    }
}
