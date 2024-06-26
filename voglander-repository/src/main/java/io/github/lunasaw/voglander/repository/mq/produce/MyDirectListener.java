package io.github.lunasaw.voglander.repository.mq.produce;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(bindings = @QueueBinding(
    value = @Queue("myDirectQueue"),
    exchange = @Exchange(value = "myDirectExchange", type = ExchangeTypes.DIRECT),
    key = "mine.direct"))
public class MyDirectListener {

    /**
     * listenerAdapter
     *
     * @param msg 消息内容,当只有一个参数的时候可以不加@Payload注解
     */
    @RabbitHandler
    public void onMessage(@Payload String msg) {
        System.out.println(msg);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send() {
        // (交换机,routingKey,消息内容)
        rabbitTemplate.convertAndSend("myDirectExchange", "mine.direct", "this is a message");
    }
}