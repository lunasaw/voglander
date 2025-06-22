package io.github.lunasaw.voglander.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author luna
 * @version 1.0
 * @date 2023/12/15
 * @description:
 */
@SpringBootApplication
@ComponentScan("io.github.lunasaw.voglander")
public class ApplicationWeb {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationWeb.class, args);
    }
}