package io.github.lunasaw.voglander;

import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author luna
 * @date 2025/6/22
 */
@SpringBootTest
public class BaseTest {

    @Autowired
    private RedisCache redisCache;

    @Test
    public void atest() {}
}
