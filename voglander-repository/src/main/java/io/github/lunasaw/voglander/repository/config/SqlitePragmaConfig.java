package io.github.lunasaw.voglander.repository.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 生产环境 SQLite WAL 模式启用配置。
 * <p>
 * 背景：xerial sqlite-jdbc（当前 3.49.1.0）<b>不识别</b> JDBC URL 参数中的
 * {@code journal_mode=WAL}（仅 {@code busy_timeout} 等少数参数 URL 生效）。
 * 因此 {@code application-repo.yml} 里 {@code jdbc:sqlite:app.db?journal_mode=WAL}
 * 实际并未开启 WAL —— 库仍运行在默认 {@code journal_mode=DELETE}：写时加排他文件锁，
 * 多线程并发写极易触发 {@code SQLITE_BUSY}。
 * </p>
 * <p>
 * WAL 必须通过 {@code PRAGMA journal_mode=WAL} 在连接上显式执行。WAL 是<b>数据库文件级别</b>的
 * 持久设置（写入文件头），对任一连接执行一次后，后续所有新连接自动以 WAL 打开，故启动时执行一次即可。
 * </p>
 * <p>
 * 仅当数据源为 SQLite 时生效；MySQL 等其他数据库直接跳过，零副作用。
 * 可通过 {@code voglander.sqlite.wal.enabled=false} 关闭（默认开启）。
 * </p>
 * <p>
 * 注意：测试侧 {@code CacheTestConfig} 亦设置 WAL，二者幂等不冲突；此配置负责<b>生产</b>路径。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "voglander.sqlite.wal.enabled", havingValue = "true", matchIfMissing = true)
public class SqlitePragmaConfig {

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * 启动时启用 SQLite WAL 模式。非 SQLite 数据源跳过；失败仅告警，不阻断启动。
     */
    @PostConstruct
    public void enableSqliteWal() {
        if (dataSource == null) {
            return;
        }

        // 仅 SQLite 生效，复用既有探测器；MySQL 等其他库直接跳过，保证零副作用
        DbType dbType = DatabaseTypeDetector.detectDbType(dataSource);
        if (dbType != DbType.SQLITE) {
            log.debug("当前数据库类型 {} 非 SQLite，跳过 WAL 配置", dbType);
            return;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            try (ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
                if (rs.next()) {
                    String mode = rs.getString(1);
                    if ("wal".equalsIgnoreCase(mode)) {
                        log.info("SQLite journal_mode = {} (WAL 模式已启用)", mode);
                    } else {
                        log.warn("SQLite WAL 模式启用失败，当前 journal_mode = {}（预期 wal）", mode);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("SQLite WAL 模式启用异常，回退默认 journal_mode：{}", e.getMessage());
        }
    }
}
