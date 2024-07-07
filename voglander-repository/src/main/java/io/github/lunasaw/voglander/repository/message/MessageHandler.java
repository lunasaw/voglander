package io.github.lunasaw.voglander.repository.message;

/**
 * @author luna
 * @date 2024/6/26
 */
public interface MessageHandler {

    void handle(String message);

    boolean accept(String topic, String message);

}
