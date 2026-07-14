package io.github.lunasaw.voglander.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

/**
 * 测试环境 Redis 配置：提供 Mock RedisTemplate，避免真实 Redis 连接
 *
 * 解决问题：
 * 1. 测试环境提供 Mock RedisConnectionFactory 和 RedisTemplate，避免真实 Redis 连接
 * 2. SSE 事件总线通过 sse.type=local 配置控制，使用 LocalSseEventBus（无需 @Primary 覆盖）
 *
 * 使用方式：
 * - 默认测试：通过 @Import 导入此配置使用 Mock Redis beans
 * - 集成测试：需要真实 Redis 时，不导入此配置，使用 Assumptions 检测可用性
 *
 * @author luna
 */
@TestConfiguration
public class TestRedisConfig {

    /**
     * 注意：SSE 事件总线现在通过 sse.type 配置控制（application-test.yml: sse.type=local）
     * LocalSseEventBus 会自动激活，无需在此提供 @Primary bean
     */

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        // Mock connection.ping() 抛出异常：依赖真实 Redis 的集成测试通过 Assumptions 探测 ping()，
        // 在无真实 Redis 的默认测试环境下应识别为 Redis 不可用并优雅跳过，而非在 Mock 上误判可用后失败
        when(connection.ping()).thenThrow(new org.springframework.dao.QueryTimeoutException("Mock Redis unavailable (no real Redis)"));
        return factory;
    }

    @Bean
    @Primary
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
        // Mock lifecycle methods - 这些方法会被 RedisBackedSseEventBus.afterPropertiesSet() 调用
        doNothing().when(container).start();
        doNothing().when(container).stop();
        doNothing().when(container).afterPropertiesSet();
        try {
            doNothing().when(container).destroy();
        } catch (Exception e) {
            // Mock destroy 方法可能抛出 Exception
        }
        // Mock 两个重载的 addMessageListener 方法
        doNothing().when(container).addMessageListener(any(org.springframework.data.redis.connection.MessageListener.class),
            any(org.springframework.data.redis.listener.Topic.class));
        doNothing().when(container).addMessageListener(any(org.springframework.data.redis.connection.MessageListener.class),
            any(java.util.Collection.class));
        // Mock setConnectionFactory
        doNothing().when(container).setConnectionFactory(any(RedisConnectionFactory.class));
        return container;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<Object, Object> redisTemplate() {
        RedisTemplate<Object, Object> template = mock(RedisTemplate.class);

        // Mock opsForValue
        ValueOperations<Object, Object> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(any(), any(), any(long.class), any())).thenReturn(true);

        // Mock opsForZSet
        ZSetOperations<Object, Object> zsetOps = mock(ZSetOperations.class);
        when(template.opsForZSet()).thenReturn(zsetOps);

        // Mock opsForHash
        HashOperations<Object, Object, Object> hashOps = mock(HashOperations.class);
        when(template.opsForHash()).thenReturn(hashOps);

        // Mock opsForList
        ListOperations<Object, Object> listOps = mock(ListOperations.class);
        when(template.opsForList()).thenReturn(listOps);

        // Mock opsForSet
        SetOperations<Object, Object> setOps = mock(SetOperations.class);
        when(template.opsForSet()).thenReturn(setOps);

        return template;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);

        // Mock opsForValue
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(any(), any(), any(long.class), any())).thenReturn(true);

        // Mock opsForZSet
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(template.opsForZSet()).thenReturn(zsetOps);

        // Mock opsForHash
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(template.opsForHash()).thenReturn(hashOps);

        // Mock opsForList
        ListOperations<String, String> listOps = mock(ListOperations.class);
        when(template.opsForList()).thenReturn(listOps);

        // Mock opsForSet
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(template.opsForSet()).thenReturn(setOps);

        return template;
    }
}
