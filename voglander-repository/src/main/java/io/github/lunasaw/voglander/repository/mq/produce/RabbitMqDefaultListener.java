package io.github.lunasaw.voglander.repository.mq.produce;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.constant.mq.MqConstant;
import io.github.lunasaw.voglander.repository.mq.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RabbitListener(queuesToDeclare = @Queue(MqConstant.DefaultTopic.VOGLANDER_INNER_QUEUE))
public class RabbitMqDefaultListener {

    @Autowired
    private List<MessageHandler> messageHandlerList;

    /**
     * listenerAdapter
     *
     * @param msg 消息内容,当只有一个参数的时候可以不加@Payload注解
     */
    @RabbitHandler
    public void onMessage(@Payload String msg) {
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