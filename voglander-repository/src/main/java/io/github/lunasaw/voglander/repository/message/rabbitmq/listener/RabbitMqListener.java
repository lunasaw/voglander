package io.github.lunasaw.voglander.repository.message.rabbitmq.listener;

import java.io.IOException;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import io.github.lunasaw.voglander.common.constant.mq.RabbitMqConstant;
import io.github.lunasaw.voglander.repository.message.MessageHandler;
import io.github.lunasaw.voglander.repository.message.rabbitmq.handler.AbstratRabbitMqMessageHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 */
@Slf4j
@Component
public class RabbitMqListener {

    @Autowired
    private List<AbstratRabbitMqMessageHandler> messageHandlerList;

    /**
     * listenerAdapter
     *
     * @param msg 消息内容,当只有一个参数的时候可以不加@Payload注解
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(RabbitMqConstant.FanoutTopic.VOGLANDER_INNER_QUEUE_FANOUT),
        exchange = @Exchange(value = RabbitMqConstant.FanoutTopic.VOGLANDER_INNER_EXCHANGE_FANOUT, type = ExchangeTypes.FANOUT),
        key = RabbitMqConstant.FanoutTopic.VOGLANDER_INNER_ROUTING_KEY_FANOUT)

    )
    public void onMessageFanout(String msg, Channel channel, Message message) {
        consumerMessage(RabbitMqConstant.FanoutTopic.VOGLANDER_INNER_QUEUE_FANOUT, msg, channel, message);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(RabbitMqConstant.DirectTopic.VOGLANDER_INNER_QUEUE_DIRECT),
        exchange = @Exchange(value = RabbitMqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT, type = ExchangeTypes.DIRECT),
        key = RabbitMqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY)

    )
    public void onMessageDirect(String msg, Channel channel, Message message) {
        consumerMessage(RabbitMqConstant.DirectTopic.VOGLANDER_INNER_QUEUE_DIRECT, msg, channel, message);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(RabbitMqConstant.TopicTopic.VOGLANDER_INNER_QUEUE_TOPIC),
        exchange = @Exchange(value = RabbitMqConstant.TopicTopic.VOGLANDER_INNER_EXCHANGE_TOPIC, type = ExchangeTypes.TOPIC),
        key = RabbitMqConstant.TopicTopic.VOGLANDER_INNER_ROUTING_KEY_TOPIC)

    )
    public void onMessageTopic(String msg, Channel channel, Message message) {
        consumerMessage(RabbitMqConstant.TopicTopic.VOGLANDER_INNER_QUEUE_TOPIC, msg, channel, message);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(RabbitMqConstant.DirectTopic.VOGLANDER_INNER_QUEUE_DIRECT_ERROR),
        exchange = @Exchange(value = RabbitMqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT_ERROR),
        key = RabbitMqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY_ERROR)

    )
    public void onMessageErrorSend(String msg, Channel channel, Message message) {
        consumerMessage(RabbitMqConstant.DirectTopic.VOGLANDER_INNER_QUEUE_DIRECT_ERROR, msg, channel, message);
    }

    private void doConsumerMessage(String topic, String msg) {
        if (StringUtils.isEmpty(msg)) {
            return;
        }

        if (CollectionUtils.isEmpty(messageHandlerList)) {
            return;
        }

        for (MessageHandler messageHandler : messageHandlerList) {
            if (messageHandler.accept(topic, msg)) {
                messageHandler.handle(msg);
            }
        }
    }

    private void consumerMessage(String topic, String msg, Channel channel, Message message) throws IOException {
        try {
            log.info("收到消息：msg:{}, messageId:{}", msg, message.getMessageProperties().getMessageId());
            doConsumerMessage(topic, msg);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            if (message.getMessageProperties().getRedelivered()) {
                log.error("消息已重复处理失败,拒绝再次接收...");
                // 拒绝消息
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                log.error("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}