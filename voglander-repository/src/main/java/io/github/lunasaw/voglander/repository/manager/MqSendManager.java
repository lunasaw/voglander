package io.github.lunasaw.voglander.repository.manager;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.luna.common.text.CharsetUtil;
import com.luna.common.text.RandomStrUtil;

import io.github.lunasaw.voglander.repository.mq.listener.RabbitMqProducerAck;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2024/7/1
 */
@Slf4j
@Component
public class MqSendManager {

    @Autowired
    private RabbitTemplate      rabbitTemplate;

    @Autowired
    private RabbitMqProducerAck rabbitMqProducerAck;

    public void convertAndSend(String exchange, String routingKey, String message, MessagePostProcessor messagePostProcessor) {
        rabbitTemplate.setEncoding(CharsetUtil.UTF_8);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback(rabbitMqProducerAck);
        rabbitTemplate.setReturnsCallback(rabbitMqProducerAck);

        rabbitTemplate.convertAndSend(exchange, routingKey, message, messagePostProcessor);
    }

    public void convertAndSend(String exchange, String routingKey, String message) {
        convertAndSend(exchange, routingKey, message, (msg) -> {
            MessageProperties messageProperties = msg.getMessageProperties();
            String uuid = RandomStrUtil.getUUID();
            messageProperties.setMessageId(uuid);
            log.info("【Producer】发送的消息 exchange = {}, routingKey = {}, id = {}, message = {}", exchange, routingKey, uuid, message);
            return msg;
        });
    }

    public void convertAndSend(String exchange, String message) {
        convertAndSend(exchange, null, message, (msg) -> msg);
    }
}
