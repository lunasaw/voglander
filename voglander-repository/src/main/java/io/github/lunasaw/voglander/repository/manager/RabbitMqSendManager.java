package io.github.lunasaw.voglander.repository.manager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.luna.common.text.CharsetUtil;
import com.luna.common.text.RandomStrUtil;

import io.github.lunasaw.voglander.common.constant.mq.RabbitMqConstant;
import io.github.lunasaw.voglander.repository.message.rabbitmq.listener.RabbitMqProducerAck;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2024/7/1
 */
@Slf4j
@Component
public class RabbitMqSendManager {

    @Autowired
    private RabbitTemplate      rabbitTemplate;

    @Autowired
    private RabbitMqProducerAck rabbitMqProducerAck;

    public void convertAndSend(String exchange, String routingKey, String message, MessagePostProcessor messagePostProcessor,
        CorrelationData correlationData) {

        rabbitTemplate.setEncoding(CharsetUtil.UTF_8);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback(rabbitMqProducerAck);
        rabbitTemplate.setReturnsCallback(rabbitMqProducerAck);

        rabbitTemplate.convertAndSend(exchange, routingKey, message, messagePostProcessor, correlationData);
    }

    public void convertAndSend(String exchange, String routingKey, String message, boolean useCorrelationData) {
        CorrelationData correlationData = null;
        if (useCorrelationData) {
            correlationData = new CorrelationData();

            Message returnMessage = new Message(message.getBytes());
            correlationData.setReturned(new ReturnedMessage(returnMessage, 500, StringUtils.EMPTY,
                RabbitMqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT_ERROR, RabbitMqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY_ERROR));
        }

        convertAndSend(exchange, routingKey, message, (msg) -> {
            MessageProperties messageProperties = msg.getMessageProperties();
            String uuid = RandomStrUtil.getUUID();
            messageProperties.setMessageId(uuid);
            log.info("【Producer】发送的消息 exchange = {}, routingKey = {}, id = {}, message = {}", exchange, routingKey, uuid, message);
            return msg;
        }, correlationData);
    }

    public void convertAndSend(String exchange, String routingKey, String message) {
        convertAndSend(exchange, routingKey, message, false);
    }
}
