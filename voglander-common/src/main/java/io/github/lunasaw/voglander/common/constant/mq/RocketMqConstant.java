package io.github.lunasaw.voglander.common.constant.mq;

/**
 * @author luna
 * @date 2024/7/6
 */
public interface RocketMqConstant {

    interface VOGLANDER_INNER_TOPIC {
        String TOPIC = "voglander_inner_topic";

        interface TAGS {
            String MESSAGE = "message";
        }
    }

    interface CONSUMER {
        String GROUP = "voglander_group";
        String TOPIC = "voglander_inner_topic";
    }
}
