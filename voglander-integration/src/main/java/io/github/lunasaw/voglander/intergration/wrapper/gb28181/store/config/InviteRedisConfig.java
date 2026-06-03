package io.github.lunasaw.voglander.intergration.wrapper.gb28181.store.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(name = "gateway.gb28181.store.type", havingValue = "redis")
@EnableConfigurationProperties(InviteRedisProperties.class)
public class InviteRedisConfig {

    @Bean("inviteRedisConnectionFactory")
    public LettuceConnectionFactory inviteRedisConnectionFactory(InviteRedisProperties properties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getHost());
        config.setPort(properties.getPort());
        config.setDatabase(properties.getDatabase());
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            config.setPassword(properties.getPassword());
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean("inviteStringRedisTemplate")
    public StringRedisTemplate inviteStringRedisTemplate(LettuceConnectionFactory inviteRedisConnectionFactory) {
        return new StringRedisTemplate(inviteRedisConnectionFactory);
    }
}
