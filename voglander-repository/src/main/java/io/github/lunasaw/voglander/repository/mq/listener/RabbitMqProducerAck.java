package io.github.lunasaw.voglander.repository.mq.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMq 生产者ACK
 */
@Slf4j
@Component
public class RabbitMqProducerAck implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    /**
     * 为发送Exchange（交换器）时回调，成功或者失败都会触发；
     * 
     * @param correlationData
     * @param ack
     * @param cause
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        log.info("confirm::correlationData = {}, ack = {}, cause = {}", JSON.toJSONString(correlationData), ack, cause);
    }

    /**
     * 为路由不到队列时触发，成功则不触发
     * 
     * @param returned the returned message and metadata.
     */
    @Override
    public void returnedMessage(ReturnedMessage returned) {
        Message message = returned.getMessage();
        String exchange = returned.getExchange();
        String routingKey = returned.getRoutingKey();
        log.error("returnedMessage::message = {}, exchange = {}, routingKey = {}", JSON.toJSONString(message), exchange, routingKey);
    }
}