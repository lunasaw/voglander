package io.github.lunasaw.voglander.repository.service;

import io.github.lunasaw.voglander.repository.mq.produce.MyDirectListener;
import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author luna
 * @date 2024/6/26
 */
@Slf4j
@SpringBootTest(classes = ApplicationWeb.class)
public class RabbitmqTest {

    @Autowired
    private MyDirectListener myDirectListener;

    @Test
    public void atest() {
        myDirectListener.send();
        while (true) {

        }
    }
}
