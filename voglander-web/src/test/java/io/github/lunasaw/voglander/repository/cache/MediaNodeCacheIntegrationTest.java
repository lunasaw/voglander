package io.github.lunasaw.voglander.repository.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import io.github.lunasaw.voglander.test.util.ServiceAvailabilityChecker;
import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 真实连接集成测试
 *
 * <h3>运行要求</h3>
 * <ul>
 *   <li>需要 Redis 服务运行在 localhost:6379</li>
 *   <li>Redis 不可用时测试自动跳过（不算失败）</li>
 * </ul>
 *
 * <h3>启动 Redis</h3>
 * <pre>
 * # macOS (Homebrew)
 * brew services start redis
 *
 * # Docker
 * docker run -d --name redis-test -p 6379:6379 redis:latest
 *
 * # Linux (systemd)
 * sudo systemctl start redis
 * </pre>
 *
 * <h3>测试执行</h3>
 * <pre>
 * # 默认：Redis 不可用���自动跳过
 * mvn test -Dtest=MediaNodeCacheIntegrationTest
 *
 * # 使用 integration-tests profile（服务不可用时失败）
 * mvn test -Pintegration-tests -Dtest=MediaNodeCacheIntegrationTest
 * </pre>
 */
@Slf4j
@DisplayName("Redis 真实连接集成测试")
@SpringBootTest(classes = ApplicationWeb.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "local.sip.enable=false",
        "local.sip.server.enabled=false",
        "local.sip.client.enabled=false",
        "sip.enable=false",
        "sse.type=local",
        "voglander.protocol-lab.enabled=false",
        "voglander.test.mock-redis=false"
    })
@ActiveProfiles("test")
public class MediaNodeCacheIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String TEST_KEY = "voglander:test:ping";

    /**
     * 检测 Redis 服务可用性
     * 使用 JUnit 5 Assumptions：服务不可用时测试自动跳过
     */
    @BeforeEach
    public void checkRedisAvailable() {
        boolean available = ServiceAvailabilityChecker.isRedisAvailable();
        if (!available) {
            log.warn("Redis integration test skipped: {}", ServiceAvailabilityChecker.getRedisSkipMessage());
        }
        assumeTrue(available, ServiceAvailabilityChecker.getRedisSkipMessage());
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
