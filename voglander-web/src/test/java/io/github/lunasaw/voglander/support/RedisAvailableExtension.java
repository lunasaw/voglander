package io.github.lunasaw.voglander.support;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import lombok.extern.slf4j.Slf4j;

/**
 * JUnit5 扩展：检测 Redis 是否可用，不可用则跳过整个测试类
 *
 * 用法：@ExtendWith(RedisAvailableExtension.class)
 *
 * @author luna
 */
@Slf4j
public class RedisAvailableExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        Assumptions.assumeTrue(isRedisAvailable(), "Redis 不可用，跳过 Redis 相关测试");
    }

    private boolean isRedisAvailable() {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", 6379), 3000);
            log.info("Redis 可用 (localhost:6379)");
            return true;
        } catch (Exception e) {
            log.info("Redis 不可用: {}，跳过相关测试", e.getMessage());
            return false;
        }
    }
}
