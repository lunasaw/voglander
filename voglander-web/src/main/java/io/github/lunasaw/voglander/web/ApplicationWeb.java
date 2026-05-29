package io.github.lunasaw.voglander.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import io.github.lunasaw.gbproxy.server.config.EnableSipServer;

/**
 * @author luna
 * @version 1.0
 * @date 2023/12/15
 * @description:
 */
@SpringBootApplication
@ComponentScan("io.github.lunasaw.voglander")
// 启用 GB28181 平台服务端能力：导入 Gb28181CommonAutoConfig（提供 CommandStrategyFactory）、
// SipProxyServerAutoConfig 及 SipProxyAutoConfig。这些 @Import 配置无 .imports 注册，
// 不会被 classpath 自动激活，因此该注解为必需（缺失则 ServerCommandSender/ClientCommandSender 无法实例化）。
@EnableSipServer
public class ApplicationWeb {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationWeb.class, args);
    }
}