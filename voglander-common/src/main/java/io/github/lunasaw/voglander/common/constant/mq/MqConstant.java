package io.github.lunasaw.voglander.common.constant.mq;

/**
 * @author luna
 * @date 2024/6/26
 */
public interface MqConstant {

    interface DirectTopic {
        String VOGLANDER_INNER_QUEUE_DIRECT    = "voglander.inner.queue.direct";
        String VOGLANDER_INNER_EXCHANGE_DIRECT = "voglander.inner.exchange.direct";
        String VOGLANDER_INNER_ROUTING_KEY     = "voglander.inner.routing-key.direct";
    }

    interface TopicTopic {
        String VOGLANDER_INNER_QUEUE_TOPIC               = "voglander.inner.queue.topic";
        String VOGLANDER_INNER_EXCHANGE_TOPIC            = "voglander.inner.exchange.topic";
        String VOGLANDER_INNER_ROUTING_KEY_TOPIC         = "voglander.inner.routing-key.topic.*";

        String VOGLANDER_INNER_QUEUE_TOPIC_MESSAGE       = "voglander.inner.queue.topic.message";
        String VOGLANDER_INNER_EXCHANGE_TOPIC_MESSAGE    = "voglander.inner.exchange.topic.message";
        String VOGLANDER_INNER_ROUTING_KEY_TOPIC_MESSAGE = "voglander.inner.routing-key.topic.message";
    }

    interface FanoutTopic {
        String VOGLANDER_INNER_QUEUE_FANOUT       = "voglander.inner.queue.fanout";
        String VOGLANDER_INNER_EXCHANGE_FANOUT    = "voglander.inner.exchange.fanout";
        String VOGLANDER_INNER_ROUTING_KEY_FANOUT = "voglander.inner.routing-key.fanout";

    }

    interface DefaultTopic {
        String VOGLANDER_INNER_QUEUE = "voglander.inner.queue";
    }
}
