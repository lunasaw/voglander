package io.github.lunasaw.voglander.repository.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis配置
 * 
 * @author luna
 */
@EnableCaching
@Configuration
public class RedisConfig {
    @Bean
    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public DefaultRedisScript<Long> limitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(limitScriptText());
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 限流脚本
     */
    private String limitScriptText() {
        return "local key = KEYS[1]\n" +
            "local count = tonumber(ARGV[1])\n" +
            "local time = tonumber(ARGV[2])\n" +
            "local current = redis.call('get', key);\n" +
            "if current and tonumber(current) > count then\n" +
            "    return tonumber(current);\n" +
            "end\n" +
            "current = redis.call('incr', key)\n" +
            "if tonumber(current) == 1 then\n" +
            "    redis.call('expire', key, time)\n" +
            "end\n" +
            "return tonumber(current);";
    }

    /**
     * 往容器中添加RedisCacheManager容器，并设置序列化方式
     * 只在Redis可用且非测试环境时创建
     * 
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    @Qualifier("redisCacheManager")
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
        FastJson2JsonRedisSerializer<Object> serializer = new FastJson2JsonRedisSerializer<>(Object.class);
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1L))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        redisCacheConfiguration.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        // Phase 1：为高频写的设备缓存区设置有界短 TTL（收敛 cache-aside 脏读窗口，修 H4）。
        // device 单对象缓存与 device:list 列表缓存隔离，单对象写不连坐列表；其余缓存区沿用默认 1h。
        Map<String, RedisCacheConfiguration> initialConfigs = new HashMap<>();
        deviceCacheTtls().forEach((name, ttl) -> initialConfigs.put(name, redisCacheConfiguration.entryTtl(ttl)));

        return RedisCacheManager.builder(redisCacheWriter)
            .cacheDefaults(redisCacheConfiguration)
            .withInitialCacheConfigurations(initialConfigs)
            .build();
    }

    /**
     * 设备相关缓存区的短 TTL 配置（Phase 1：修 H4 脏读窗口）。
     * <p>
     * 提取为静态方法以便单元测试；与 {@code DeviceCacheKey.CACHE_NAME}/{@code LIST_CACHE_NAME} 对齐，
     * 但因 repository 模块不依赖 manager 模块，此处用字面量并以注释说明对应关系。
     * </p>
     * <ul>
     * <li>{@code device}（单对象）：3min —— 短 TTL 收敛脏读窗口；</li>
     * <li>{@code device:list}（列表/分页）：60s —— 更短，与单对象隔离。</li>
     * </ul>
     *
     * @return 缓存区名 → TTL 映射
     */
    public static Map<String, Duration> deviceCacheTtls() {
        Map<String, Duration> ttls = new HashMap<>();
        ttls.put("device", Duration.ofMinutes(3L));
        ttls.put("device:list", Duration.ofSeconds(60L));
        return ttls;
    }

}
