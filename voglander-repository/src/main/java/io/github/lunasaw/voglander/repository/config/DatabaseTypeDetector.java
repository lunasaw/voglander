package io.github.lunasaw.voglander.repository.config;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * 数据库类型检测器
 *
 * @author luna
 * @date 2024/12/27
 */
@Slf4j
public class DatabaseTypeDetector {

    /**
     * 根据数据源自动检测数据库类型
     *
     * @param dataSource 数据源
     * @return DbType 数据库类型
     */
    public static DbType detectDbType(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName().toLowerCase();
            String url = metaData.getURL().toLowerCase();

            log.debug("数据库产品名称: {}, URL: {}", databaseProductName, url);

            // 优先通过产品名称判断
            DbType dbType = detectByProductName(databaseProductName);
            if (dbType != null) {
                log.info("通过产品名称检测到数据库类型: {}", dbType);
                return dbType;
            }

            // 通过URL判断
            dbType = detectByUrl(url);
            if (dbType != null) {
                log.info("通过URL检测到数据库类型: {}", dbType);
                return dbType;
            }

            log.warn("未识别的数据库类型，产品名称: {}, URL: {}, 使用默认类型: MYSQL",
                databaseProductName, url);
            return DbType.MYSQL;

        } catch (Exception e) {
            log.error("获取数据库类型失败，使用默认类型: MYSQL", e);
            return DbType.MYSQL;
        }
    }

    /**
     * 通过数据库产品名称检测类型
     *
     * @param productName 产品名称
     * @return DbType 数据库类型，如果无法识别返回null
     */
    private static DbType detectByProductName(String productName) {
        if (productName.contains("mysql")) {
            return DbType.MYSQL;
        } else if (productName.contains("sqlite")) {
            return DbType.SQLITE;
        } else if (productName.contains("postgresql") || productName.contains("postgres")) {
            return DbType.POSTGRE_SQL;
        } else if (productName.contains("oracle")) {
            return DbType.ORACLE;
        } else if (productName.contains("sql server") || productName.contains("microsoft")) {
            return DbType.SQL_SERVER;
        } else if (productName.contains("h2")) {
            return DbType.H2;
        } else if (productName.contains("mariadb")) {
            return DbType.MARIADB;
        } else if (productName.contains("clickhouse")) {
            return DbType.CLICK_HOUSE;
        }
        return null;
    }

    /**
     * 通过JDBC URL检测数据库类型
     *
     * @param url JDBC URL
     * @return DbType 数据库类型，如果无法识别返回null
     */
    private static DbType detectByUrl(String url) {
        if (url.startsWith("jdbc:mysql")) {
            return DbType.MYSQL;
        } else if (url.startsWith("jdbc:sqlite")) {
            return DbType.SQLITE;
        } else if (url.startsWith("jdbc:postgresql")) {
            return DbType.POSTGRE_SQL;
        } else if (url.startsWith("jdbc:oracle")) {
            return DbType.ORACLE;
        } else if (url.startsWith("jdbc:sqlserver")) {
            return DbType.SQL_SERVER;
        } else if (url.startsWith("jdbc:h2")) {
            return DbType.H2;
        } else if (url.startsWith("jdbc:mariadb")) {
            return DbType.MARIADB;
        } else if (url.startsWith("jdbc:clickhouse")) {
            return DbType.CLICK_HOUSE;
        }
        return null;
    }
}