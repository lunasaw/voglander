package io.github.lunasaw.voglander.repository.message.rocketmq.listener;

import io.github.lunasaw.voglander.repository.message.MessageHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.constant.mq.RocketMqConstant;

import java.util.List;

/**
 * @author luna
 * @date 2024/7/6
 */
@Component
@RocketMQMessageListener(
    consumerGroup = RocketMqConstant.CONSUMER.GROUP, topic = RocketMqConstant.CONSUMER.TOPIC)
public class RocketMqListener implements RocketMQListener<String> {

    @Autowired
    private List<MessageHandler> messageHandlerList;

    @Override
    public void onMessage(String msg) {
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
