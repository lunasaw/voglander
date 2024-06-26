package io.github.lunasaw.voglander.web.api.backdoor;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.constant.mq.MqConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author luna
 * @date 2024/6/26
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/test")
public class TestController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RequestMapping("/hello")
    public String hello() {
        // (交换机,routingKey,消息内容)
        rabbitTemplate.convertAndSend(MqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT, MqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            "this is a message");
        return "hello";
    }

}
