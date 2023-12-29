package io.github.lunasaw.voglander.intergration.wrapper.gb28181.start;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author luna
 * @date 2023/12/29
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sip")
public class SipServerConfig {

    private String ip;
    private Integer port;
    private Boolean enableLog;

}
