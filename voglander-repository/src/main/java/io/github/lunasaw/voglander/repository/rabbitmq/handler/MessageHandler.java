package io.github.lunasaw.voglander.repository.rabbitmq.handler;

/**
 * @author luna
 * @date 2024/6/26
 */
public interface MessageHandler {

    void handle(String message);

    boolean accept(String message);

}
