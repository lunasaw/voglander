package io.github.lunasaw.voglander.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 测试专用缓存配置 + SQLite 并发优化（WAL）。
 * <p>
 * 背景：sip-common 的 {@code io.github.lunasaw.sip.common.cache.CacheConfig} 以
 * {@code @ConditionalOnMissingBean} 提供了一个<b>非动态</b>的 {@link ConcurrentMapCacheManager}，
 * 仅含固定 4 个缓存区 {@code [devices, subscribes, transactions, sipMessages]}。
 * 由于它是 classpath 上唯一的 {@code CacheManager} bean，voglander 自有缓存区
 * （{@code device}/{@code mediaNode}/{@code streamProxy}/...）在测试中 {@code getCache} 恒返回 null，
 * 被各 Manager 的 {@code Optional.ofNullable(getCache).ifPresent(...)} 静默跳过 ——
 * 导致缓存读写与精确 evict 行为在测试中<b>从未被真正验证</b>。
 * </p>
 * <p>
 * 生产环境使用动态 {@code RedisCacheManager}（按需创建缓存区），不存在此问题。
 * 本配置提供一个 {@code @Primary} 的<b>动态</b> {@link ConcurrentMapCacheManager}（无固定名集合，
 * 按需创建），使测试缓存语义与生产一致，从而可真实验证 {@code @Cacheable} 与精确 evict。
 * 它优先于 sip-common 的 {@code @ConditionalOnMissingBean} bean 生效。
 * </p>
 * <p>
 * SQLite WAL 模式：默认 journal_mode=DELETE 使用排他文件锁，多线程并发写入易触发 SQLITE_BUSY。
 * WAL 模式允许读写并发，配合 busy_timeout（URL 参数设置）大幅减少锁冲突。
 * 数据库级别配置，只需在测试启动时执行一次 PRAGMA。
 * </p>
 *
 * @author luna
 */
@Slf4j
@TestConfiguration
public class CacheTestConfig {

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * 动态缓存管理器：无预设名集合，{@code getCache} 按需创建任意缓存区，
     * 语义对齐生产的 {@code RedisCacheManager}。
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        // 默认构造 = 动态模式（dynamic=true），任意 name 首次访问即创建
        return new ConcurrentMapCacheManager();
    }

    /**
     * 启用 SQLite WAL 模式 —— 必须通过 PRAGMA 设置（JDBC URL 参数 journal_mode=WAL 不被识别）。
     * 数据库级别配置，整个测试套件只需执行一次。
     */
    @PostConstruct
    public void enableSqliteWal() {
        if (dataSource == null) return;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            try (ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
                if (rs.next()) {
                    log.info("SQLite journal_mode = {} (WAL 模式已启用)", rs.getString(1));
                }
            }
        } catch (Exception e) {
            log.warn("SQLite WAL 模式启用失败，回退默认 journal_mode：{}", e.getMessage());
        }
    }
}

