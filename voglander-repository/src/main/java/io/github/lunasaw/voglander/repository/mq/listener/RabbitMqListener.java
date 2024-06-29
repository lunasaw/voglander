package io.github.lunasaw.voglander.repository.mq.listener;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.constant.mq.MqConstant;
import io.github.lunasaw.voglander.repository.mq.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RabbitMqListener {

    @Autowired
    private List<MessageHandler> messageHandlerList;

    /**
     * listenerAdapter
     *
     * @param msg 消息内容,当只有一个参数的时候可以不加@Payload注解
     */
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(MqConstant.FanoutTopic.VOGLANDER_INNER_QUEUE_FANOUT),
        exchange = @Exchange(value = MqConstant.FanoutTopic.VOGLANDER_INNER_EXCHANGE_FANOUT, type = ExchangeTypes.FANOUT),
        key = MqConstant.FanoutTopic.VOGLANDER_INNER_ROUTING_KEY_FANOUT)

    )
    public void onMessageFanout(String msg) {
        if (StringUtils.isEmpty(msg)) {
            return;
        }

        if (CollectionUtils.isEmpty(messageHandlerList)) {
            return;
        }

        for (MessageHandler messageHandler : messageHandlerList) {
            if (messageHandler.accept(msg)) {
                messageHandler.handle(msg);
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(MqConstant.DirectTopic.VOGLANDER_INNER_QUEUE_DIRECT),
        exchange = @Exchange(value = MqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT, type = ExchangeTypes.DIRECT),
        key = MqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY)

    )
    public void onMessageDirect(String msg) {
        if (StringUtils.isEmpty(msg)) {
            return;
        }

        if (CollectionUtils.isEmpty(messageHandlerList)) {
            return;
        }

        for (MessageHandler messageHandler : messageHandlerList) {
            if (messageHandler.accept(msg)) {
                messageHandler.handle(msg);
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(MqConstant.TopicTopic.VOGLANDER_INNER_QUEUE_TOPIC),
        exchange = @Exchange(value = MqConstant.TopicTopic.VOGLANDER_INNER_EXCHANGE_TOPIC, type = ExchangeTypes.TOPIC),
        key = MqConstant.TopicTopic.VOGLANDER_INNER_ROUTING_KEY_TOPIC)

    )
    public void onMessageTopic(String msg) {
        if (StringUtils.isEmpty(msg)) {
            return;
        }

        if (CollectionUtils.isEmpty(messageHandlerList)) {
            return;
        }

        for (MessageHandler messageHandler : messageHandlerList) {
            if (messageHandler.accept(msg)) {
                messageHandler.handle(msg);
            }
        }
    }

    @RabbitListener(bindings = {

        @QueueBinding(
            value = @Queue(MqConstant.TopicTopic.VOGLANDER_INNER_QUEUE_TOPIC),
            exchange = @Exchange(value = MqConstant.TopicTopic.VOGLANDER_INNER_EXCHANGE_TOPIC, type = ExchangeTypes.TOPIC),
            key = MqConstant.TopicTopic.VOGLANDER_INNER_ROUTING_KEY_TOPIC),

        @QueueBinding(
            value = @Queue(MqConstant.TopicTopic.VOGLANDER_INNER_QUEUE_TOPIC_MESSAGE),
            exchange = @Exchange(value = MqConstant.TopicTopic.VOGLANDER_INNER_EXCHANGE_TOPIC_MESSAGE, type = ExchangeTypes.TOPIC),
            key = {
                MqConstant.TopicTopic.VOGLANDER_INNER_ROUTING_KEY_TOPIC_MESSAGE,
                MqConstant.TopicTopic.VOGLANDER_INNER_ROUTING_KEY_TOPIC
            })
    })
    public void onMessageTopic2(String msg) {
        if (StringUtils.isEmpty(msg)) {
            return;
        }

        if (CollectionUtils.isEmpty(messageHandlerList)) {
            return;
        }

        for (MessageHandler messageHandler : messageHandlerList) {
            if (messageHandler.accept(msg)) {
                messageHandler.handle(msg);
            }
        }
    }
}