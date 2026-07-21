package io.github.lunasaw.voglander.repository.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @version 1.0
 * @date 2023/12/15
 * @description: MyBatis Plus配置类，自动检测数据库类型
 */
@Slf4j
@MapperScan("io.github.lunasaw.voglander.repository.mapper")
@Configuration
public class MybatisPlusConfig {

    @Autowired
    private DataSource dataSource;

    /**
     * 添加分页插件，自动检测数据库类型
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        DbType dbType = DatabaseTypeDetector.detectDbType(dataSource);
        log.info("MyBatis Plus分页插件配置完成，数据库类型: {}", dbType);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }

    /** Selects the native idempotent-insert statement declared in task Mapper XML files. */
    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties aliases = new Properties();
        aliases.setProperty("MySQL", "mysql");
        aliases.setProperty("SQLite", "sqlite");
        aliases.setProperty("PostgreSQL", "postgresql");
        provider.setProperties(aliases);
        return provider;
    }
}
