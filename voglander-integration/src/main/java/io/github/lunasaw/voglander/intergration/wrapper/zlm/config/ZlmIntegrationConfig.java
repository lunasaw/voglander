package io.github.lunasaw.voglander.intergration.wrapper.zlm.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ZLM集成配置
 * 用于配置ZLMediaKit的相关参数
 *
 * @author luna
 * @date 2025/01/22
 */
@Getter
@Slf4j
@Configuration
@ConditionalOnProperty(value = "zlm.enable", havingValue = "true")
@ConfigurationProperties(prefix = "zlm")
public class ZlmIntegrationConfig {

    /**
     * 是否启用ZLM
     */
    private boolean enable = true;

    public void setEnable(boolean enable) {
        this.enable = enable;
        if (enable) {
            log.info("ZLM集成模块已启用");
        } else {
            log.info("ZLM集成模块已禁用");
        }
    }

    public void method() {

    }
}