package io.github.lunasaw.voglander.web.api.backdoor;

import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.ImmutableMap;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.constant.Constants;
import io.github.lunasaw.voglander.common.constant.mq.RabbitMqConstant;
import io.github.lunasaw.voglander.common.constant.mq.RocketMqConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.repository.manager.RabbitMqSendManager;
import io.github.lunasaw.voglander.repository.manager.RocketMqSendManager;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2024/6/26
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/test")
@Slf4j
public class TestController {

    @Autowired
    private RabbitTemplate      rabbitTemplate;

    @Autowired
    private RabbitMqSendManager rabbitMqSendManager;
    @Autowired
    private RocketMqSendManager rocketMqSendManager;

    @RequestMapping("/hello")
    @Trace
    public String hello() {
        log.info("hello::" + TraceContext.traceId());

        rabbitMqSendManager.convertAndSend(RabbitMqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT,
            RabbitMqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            JSON.toJSONString(ImmutableMap.of("hello", "world", Constants.SKY_WALKING_TID, TraceContext.traceId())));

        rabbitMqSendManager.convertAndSend(RabbitMqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT,
            RabbitMqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            JSON.toJSONString(ImmutableMap.of("hello", "world")));

        rabbitMqSendManager.convertAndSend(RabbitMqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT,
            RabbitMqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            JSON.toJSONString(AjaxResult.success("hello direct")));

        rabbitMqSendManager.convertAndSend(RabbitMqConstant.FanoutTopic.VOGLANDER_INNER_EXCHANGE_FANOUT,
            RabbitMqConstant.FanoutTopic.VOGLANDER_INNER_ROUTING_KEY_FANOUT, JSON.toJSONString(AjaxResult.success("hello fanout")));

        return "hello";
    }

    @RequestMapping("/hello/v2")
    @Trace
    public String helloV2() {

        rocketMqSendManager.sendAsync(RocketMqConstant.VOGLANDER_INNER_TOPIC.TOPIC, RocketMqConstant.VOGLANDER_INNER_TOPIC.TAGS.MESSAGE, "hello");
        return "hello";
    }

}
