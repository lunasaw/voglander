package io.github.lunasaw.voglander.intergration.wrapper.gb28181.start;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @author luna
 * @date 2023/12/29
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sip")
public class SipServerConfig {

    private Boolean enableLog = true;
    private Boolean enable;

}
