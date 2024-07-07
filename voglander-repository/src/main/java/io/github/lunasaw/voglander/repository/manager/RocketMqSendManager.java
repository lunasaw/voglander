package io.github.lunasaw.voglander.repository.manager;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty("rocketmq.name-server")
public class RocketMqSendManager {
    @Value("${rocketmq.producer.send-message-timeout}")
    private long             timeout;

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
        return sendMessage(topic, tags, data, headers, null, null);
    }

    public SendResult sendMessage(String topic, String tags, String data, Map<String, Object> headers, Long delayTime) {
        return sendMessage(topic, tags, data, headers, delayTime, null);
    }

    public SendResult sendMessage(String topic, String tags, String data, Map<String, Object> headers, String hashKey) {
        return sendMessage(topic, tags, data, headers, null, hashKey);
    }

    public SendResult sendMessage(String topic, String tags, String data, Map<String, Object> headers, Long delayTime, String hashKey) {
        Message<String> build = MessageBuilder.withPayload(data).build();
        if (MapUtils.isNotEmpty(headers)) {
            MessageHeaders messageHeaders = new MessageHeaders(headers);
            build = MessageBuilder.createMessage(data, messageHeaders);
        }
        String sendTopic = getTopic(topic, tags);
        SendResult sendResult;
        if (delayTime != null) {
            sendResult = rocketMQTemplate.syncSendDelayTimeSeconds(sendTopic, build, delayTime);
        } else if (StringUtils.isNotBlank(hashKey)) {
            sendResult = rocketMQTemplate.syncSendOrderly(sendTopic, build, hashKey);
        } else {
            sendResult = rocketMQTemplate.syncSend(sendTopic, build);
        }
        log.info("发送同步消息 sendsync::topic = {}, tags = {}, data = {}, headers = {}, delayTime ={}, msgId = {}", topic, tags, data, headers, delayTime,
            sendResult.getMsgId());
        return sendResult;
    }

    public void sendAsync(String topic, String data) {
        sendAsync(topic, null, data);
    }

    public void sendAsync(String topic, String tags, String data) {
        sendAsync(topic, tags, null, data);
    }

    public void sendAsync(String topic, String tags, String keys, String data) {
        Map<String, Object> headers = new HashMap<>();
        if (StringUtils.isNotBlank(keys)) {
            headers.put(RocketMQHeaders.KEYS, keys);
        }
        sendAsync(topic, tags, data, headers, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("onSuccess::sendResult = {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("onException::throwable", throwable);
            }
        });
    }

    public void sendAsync(String topic, String tags, String data, Map<String, Object> headers) {
        sendAsync(topic, tags, data, headers, null, timeout, null);
    }

    public void sendAsync(String topic, String tags, String data, Map<String, Object> headers, SendCallback sendCallback) {
        sendAsync(topic, tags, data, headers, sendCallback, timeout, null);
    }

    public void sendAsync(String topic, String tags, String data, Map<String, Object> headers, SendCallback sendCallback, long timeout,
        String hashKey) {
        Message<String> build = MessageBuilder.withPayload(data).build();
        if (MapUtils.isNotEmpty(headers)) {
            MessageHeaders messageHeaders = new MessageHeaders(headers);
            build = MessageBuilder.createMessage(data, messageHeaders);
        }
        String sendTopic = getTopic(topic, tags);
        if (StringUtils.isNotBlank(hashKey)) {
            rocketMQTemplate.asyncSendOrderly(sendTopic, build, hashKey, sendCallback);
        } else {
            rocketMQTemplate.asyncSend(sendTopic, build, sendCallback, timeout);
        }
        log.info("发送异步消息 sendAsync::topic = {}, tags = {}, data = {}, headers = {}, timeout = {}, hashKey = {}", topic, tags, data,
            headers, timeout, hashKey);
    }
}
