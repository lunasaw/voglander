package io.github.lunasaw.voglander.intergration.wrapper.image.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;

import io.github.lunasaw.voglander.intergration.wrapper.image.storage.LocalImageStorageService;

/** Registers image configuration and the default local provider. */
@Configuration
@EnableConfigurationProperties(ImageProperties.class)
public class ImageStorageConfiguration {

    @Bean
    @Conditional(LocalImageStorageCondition.class)
    public LocalImageStorageService localImageStorageService(ImageProperties properties) {
        return new LocalImageStorageService(Path.of(properties.getStorage().getLocalRoot()),
            properties.getStorage().getWorkerNode());
    }
}
