package io.github.lunasaw.voglander.repository.manager;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RocketMqSendManager {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private static String getTopic(String topic, String tags) {
        if (StringUtils.isNotBlank(tags)) {
            topic += ":" + tags;
        }
        return topic;
    }

    public SendResult sendMessage(String topic, String data) {
        return sendMessage(topic, null, data, new HashMap<>());
    }

    public SendResult sendMessage(String topic, String tags, String data) {
        return sendMessage(topic, tags, data, new HashMap<>());
    }

    public SendResult sendMessage(String topic, String tags, String data, Map<String, Object> headers) {
        Message<String> build = MessageBuilder.withPayload(data).build();
        if (MapUtils.isNotEmpty(headers)) {
            MessageHeaders messageHeaders = new MessageHeaders(headers);
            build = MessageBuilder.createMessage(data, messageHeaders);
        }
        String sendTopic = getTopic(topic, tags);
        SendResult sendResult = rocketMQTemplate.syncSend(sendTopic, build);
        log.info("sendAsync::topic = {}, tags = {}, data = {}, headers = {}, msgId = {}", topic, tags, data, headers, sendResult.getMsgId());
        return sendResult;
    }

    public void sendAsync(String topic, String data) {
        sendAsync(topic, null, data);
    }

    public void sendAsync(String topic, String tags, String data) {
        sendAsync(topic, tags, data, new HashMap<>(), null);
    }

    public void sendAsync(String topic, String tags, String data,
        @Nullable MessagePostProcessor postProcessor) {
        sendAsync(topic, tags, data, new HashMap<>(), postProcessor);
    }

    public void sendAsync(String topic, String tags, String data, Map<String, Object> headers,
        MessagePostProcessor postProcessor) {

        String sendTopic = getTopic(topic, tags);
        rocketMQTemplate.convertAndSend(sendTopic, data, headers, postProcessor);
    }
}
