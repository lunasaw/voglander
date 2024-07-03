package io.github.lunasaw.voglander.web.api.backdoor;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.repository.manager.MqSendManager;
import io.github.lunasaw.voglander.repository.rocketmq.MQProducerService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @Autowired
    private MqSendManager  mqSendManager;

    @RequestMapping("/hello")
    @Trace
    public String hello() {
        log.info("hello::" + TraceContext.traceId());

        mqSendManager.convertAndSend(MqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT, MqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            JSON.toJSONString(ImmutableMap.of("hello", "world", Constants.SKY_WALKING_TID, TraceContext.traceId())));

        mqSendManager.convertAndSend(MqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT, MqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            JSON.toJSONString(ImmutableMap.of("hello", "world")));

        mqSendManager.convertAndSend(MqConstant.DirectTopic.VOGLANDER_INNER_EXCHANGE_DIRECT, MqConstant.DirectTopic.VOGLANDER_INNER_ROUTING_KEY,
            JSON.toJSONString(AjaxResult.success("hello direct")));

        mqSendManager.convertAndSend(MqConstant.FanoutTopic.VOGLANDER_INNER_EXCHANGE_FANOUT,
            MqConstant.FanoutTopic.VOGLANDER_INNER_ROUTING_KEY_FANOUT, JSON.toJSONString(AjaxResult.success("hello fanout")));

        return "hello";
    }

    @Autowired
    private MQProducerService mqProducerService;

    @RequestMapping("/hello/v2")
    @Trace
    public String helloV2() {

        mqProducerService.sendTagMsg("hello");
        return "hello";
    }

}
