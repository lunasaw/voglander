package io.github.lunasaw.voglander.intergration.wrapper.gb28181.store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("gateway.gb28181.store.redis")
@Data
public class InviteRedisProperties {
    private String host = "127.0.0.1";
    private int port = 6379;
    private String password = "";
    private int database = 1;
    private int timeout = 30;
}
