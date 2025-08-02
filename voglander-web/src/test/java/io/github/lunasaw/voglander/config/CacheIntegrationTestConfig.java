package io.github.lunasaw.voglander.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 缓存集成测试专用配置
 * 复用主应用的Redis配置，只提供Redis不可用时的回退机制
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableCaching
@ComponentScan(basePackages = {
    "io.github.lunasaw.voglander.manager",
    "io.github.lunasaw.voglander.repository",
    "io.github.lunasaw.voglander.service"
})
public class CacheIntegrationTestConfig {

    /**
     * 测试环境专用的简单缓存管理器
     * 设置为Primary确保在测试环境中优先使用
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple", matchIfMissing = true)
    public CacheManager simpleCacheManager() {
        log.info("使用简单内存缓存管理器用于测试环境");
        return new ConcurrentMapCacheManager("mediaNode", "device");
    }

    /**
     * 检查Redis连接状态的Bean
     * 用于在测试开始前验证Redis是否可用
     */
    @Bean
    public RedisConnectionChecker redisConnectionChecker(RedisConnectionFactory redisConnectionFactory) {
        return new RedisConnectionChecker(redisConnectionFactory);
    }

    /**
     * Redis连接检查器
     */
    public static class RedisConnectionChecker {
        private final RedisConnectionFactory redisConnectionFactory;
        private Boolean                      isRedisAvailable;

        public RedisConnectionChecker(RedisConnectionFactory redisConnectionFactory) {
            this.redisConnectionFactory = redisConnectionFactory;
        }

        public boolean isRedisAvailable() {
            if (isRedisAvailable == null) {
                try {
                    redisConnectionFactory.getConnection().ping();
                    isRedisAvailable = true;
                    log.info("Redis连接正常");
                } catch (Exception e) {
                    isRedisAvailable = false;
                    log.warn("Redis连接失败，将使用内存缓存: {}", e.getMessage());
                }
            }
            return isRedisAvailable;
        }
    }
}