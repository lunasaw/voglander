package io.github.lunasaw.voglander.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

/**
 * 测试环境专用配置
 *
 * 注意：MyBatis Plus分页插件配置使用生产环境的自动检测配置，
 * 无需在测试环境重复配置
 *
 * @author luna
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
    RedisAutoConfiguration.class,
    WebMvcAutoConfiguration.class
})
@ComponentScan("io.github.lunasaw.voglander")
@org.springframework.cache.annotation.EnableCaching
@MapperScan("io.github.lunasaw.voglander.repository.mapper")
public class TestConfig {


}