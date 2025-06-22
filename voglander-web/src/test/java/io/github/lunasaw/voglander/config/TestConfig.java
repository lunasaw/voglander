package io.github.lunasaw.voglander.config;

import io.github.lunasaw.voglander.manager.manager.DeviceManager;
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

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

/**
 * 测试环境专用配置
 *
 * @author luna
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
    RedisAutoConfiguration.class,
    WebMvcAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "io.github.lunasaw.voglander.manager",
    "io.github.lunasaw.voglander.repository"
})
@org.springframework.cache.annotation.EnableCaching
@MapperScan("io.github.lunasaw.voglander.repository.mapper")
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

    /**
     * 测试环境使用SQLite数据库的MyBatis Plus配置
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor testMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 使用SQLite数据库类型
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        return interceptor;
    }
}