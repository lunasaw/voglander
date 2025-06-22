package io.github.lunasaw.voglander.config;

import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableAutoConfiguration(exclude = {
    RedisAutoConfiguration.class,
    WebMvcAutoConfiguration.class
})
@org.springframework.cache.annotation.EnableCaching
public class TestConfig {

    @Bean("CacheManagerTest")
    @Primary
    @Qualifier
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("device");
    }

    @Bean("DeviceManagerTest")
    public DeviceManager deviceManager() {
        return new DeviceManager();
    }
}