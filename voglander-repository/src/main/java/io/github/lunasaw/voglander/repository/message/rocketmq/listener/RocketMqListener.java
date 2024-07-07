package io.github.lunasaw.voglander.repository.message.rocketmq.listener;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.constant.mq.RocketMqConstant;
import io.github.lunasaw.voglander.repository.message.rocketmq.handler.AbstratRocketMqMessageHandler;

/**
 * @author luna
 * @date 2024/7/6
 */
@Component
@RocketMQMessageListener(
    consumerGroup = RocketMqConstant.CONSUMER.GROUP,
    topic = RocketMqConstant.CONSUMER.TOPIC,
    enableMsgTrace = true,
    selectorExpression = "* || " + RocketMqConstant.VOGLANDER_INNER_TOPIC.TAGS.MESSAGE)
public class RocketMqListener implements RocketMQListener<String> {

    @Autowired
    private List<AbstratRocketMqMessageHandler> messageHandlerList;

    @Override
    public void onMessage(String msg) {
        if (StringUtils.isEmpty(msg)) {
            return;
        }

        if (CollectionUtils.isEmpty(messageHandlerList)) {
            return;
        }

        for (AbstratRocketMqMessageHandler messageHandler : messageHandlerList) {
            if (messageHandler.accept(RocketMqConstant.CONSUMER.TOPIC, msg)) {
                messageHandler.handle(msg);
            }
        }
    }
}
