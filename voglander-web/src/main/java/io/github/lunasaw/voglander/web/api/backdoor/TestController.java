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
import io.github.lunasaw.voglander.common.constant.mq.MqConstant;
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
    private RabbitTemplate rabbitTemplate;

    @RequestMapping("/hello")
    @Trace
    public String hello() {
        log.info("hello::" + TraceContext.traceId());

        rabbitTemplate.convertAndSend(MqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT, MqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            JSON.toJSONString(ImmutableMap.of("hello", "world", Constants.SKY_WALKING_TID, TraceContext.traceId())));

        rabbitTemplate.convertAndSend(MqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT, MqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            JSON.toJSONString(ImmutableMap.of("hello", "world")));
        return "hello";
    }

}
