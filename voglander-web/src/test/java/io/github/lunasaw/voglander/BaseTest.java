package io.github.lunasaw.voglander;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2025/6/22
 */
@Slf4j
@SpringBootTest(classes = {
    io.github.lunasaw.voglander.web.ApplicationWeb.class,
    io.github.lunasaw.voglander.config.TestConfig.class
}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class BaseTest {

    @Test
    public void atest() {}
}
