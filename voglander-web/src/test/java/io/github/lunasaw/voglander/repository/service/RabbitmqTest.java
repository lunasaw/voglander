package io.github.lunasaw.voglander.repository.service;

import io.github.lunasaw.voglander.common.constant.mq.RabbitMqConstant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2024/6/26
 */
@Slf4j
@SpringBootTest(classes = ApplicationWeb.class)
public class RabbitmqTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send() {
        // (交换机,routingKey,消息内容)
        rabbitTemplate.convertAndSend(RabbitMqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT,
            RabbitMqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            "this is a message");
    }

    @Test
    public void atest() {
        send();
        while (true) {

        }
    }
}
