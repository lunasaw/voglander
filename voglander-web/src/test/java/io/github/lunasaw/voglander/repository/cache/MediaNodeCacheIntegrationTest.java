package io.github.lunasaw.voglander.repository.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import io.github.lunasaw.voglander.BaseTest;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 真实连接集成测试。
 * Redis 不可用时通过 Assumptions.assumeTrue 自动跳过。
 */
@Slf4j
@DisplayName("Redis 真实连接集成测试")
public class MediaNodeCacheIntegrationTest extends BaseTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    private static final String TEST_KEY = "voglander:test:ping";

    @BeforeEach
    public void checkRedis() {
        boolean available;
        try {
            connectionFactory.getConnection().ping();
            available = true;
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过测试: {}", e.getMessage());
            available = false;
        }
        assumeTrue(available, "Redis 不可用，跳过");
    }

    @Test
    @DisplayName("set/get 基本读写")
    public void testSetAndGet() {
        redisTemplate.opsForValue().set(TEST_KEY, "hello");
        String value = redisTemplate.opsForValue().get(TEST_KEY);
        assertEquals("hello", value);
        redisTemplate.delete(TEST_KEY);
    }

    @Test
    @DisplayName("delete 后 key 不存在")
    public void testDelete() {
        redisTemplate.opsForValue().set(TEST_KEY, "val");
        redisTemplate.delete(TEST_KEY);
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(TEST_KEY)));
    }
}
