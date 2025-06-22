package io.github.lunasaw.voglander.repository.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @version 1.0
 * @date 2023/12/15
 * @description: MyBatis Plus配置类，支持自动检测数据库类型
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
        DbType dbType = getDbType();
        log.info("检测到数据库类型: {}", dbType);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }

    /**
     * 自动检测数据库类型
     *
     * @return DbType
     */
    private DbType getDbType() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName().toLowerCase();

            log.debug("数据库产品名称: {}", databaseProductName);

            if (databaseProductName.contains("mysql")) {
                return DbType.MYSQL;
            } else if (databaseProductName.contains("sqlite")) {
                return DbType.SQLITE;
            } else if (databaseProductName.contains("postgresql")) {
                return DbType.POSTGRE_SQL;
            } else if (databaseProductName.contains("oracle")) {
                return DbType.ORACLE;
            } else if (databaseProductName.contains("sql server")) {
                return DbType.SQL_SERVER;
            } else if (databaseProductName.contains("h2")) {
                return DbType.H2;
            } else {
                log.warn("未识别的数据库类型: {}, 使用默认类型: MYSQL", databaseProductName);
                return DbType.MYSQL;
            }
        } catch (Exception e) {
            log.error("获取数据库类型失败，使用默认类型: MYSQL", e);
            return DbType.MYSQL;
        }
    }
}
