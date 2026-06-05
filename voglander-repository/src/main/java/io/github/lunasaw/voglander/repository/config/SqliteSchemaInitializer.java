package io.github.lunasaw.voglander.repository.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.baomidou.mybatisplus.annotation.DbType;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 生产/开发环境 SQLite 建表与初始化数据导入（clone-and-run 支持）。
 * <p>
 * 背景：项目无 Flyway/Liquibase，亦无程序化建表，历史上 clone-and-run 依赖被提交进 git 的
 * {@code app.db}/{@code test-app.db} 携带 schema。该 db 二进制移出版本控制后，新 clone 将无表可用。
 * 本类在启动时检测空库并执行权威建表脚本 {@code classpath:db/voglander-sqlite.sql}
 * （由 repository 模块构建期从根目录 {@code sql/voglander-sqlite.sql} 拷入，单一源），
 * 一次性建出 17 张表 + 默认 admin/角色/菜单种子。
 * </p>
 * <p>
 * <b>幂等保证</b>：脚本结构段含 {@code DROP TABLE IF EXISTS}（破坏性），故仅在<b>未建全库</b>
 * （sentinel 表 {@code tb_cascade_channel}——建表序列最后一张——不存在）时执行；已建全库直接跳过，
 * 绝不触碰现有数据。因此每个库生命周期内最多执行一次，重启不会清库。半途中断的坏库（末表缺失）
 * 下次启动会因 sentinel 缺失重新建表自愈。
 * </p>
 * <p>
 * 仅 SQLite 生效；MySQL 等其他库由 DBA 用 {@code sql/voglander.sql} 初始化，本类跳过零副作用。
 * 可通过 {@code voglander.sqlite.schema-init.enabled=false} 关闭（默认开启）。
 * </p>
 * <p>
 * 与 {@code SqlitePragmaConfig}（WAL）顺序无关正确性：WAL 是数据库文件级持久设置，
 * 建表在启动期单线程执行，DELETE 模式下亦可成功，之后开 WAL 照常生效——故不引入
 * {@code @DependsOn} 耦合（否则关闭 WAL 开关会因 bean 缺失导致启动失败）。
 * 本类在 {@code @PostConstruct}（容器初始化期）执行，先于 {@code UserInitializer}
 * （{@code CommandLineRunner}，容器就绪后执行）；admin 种子已由脚本以有效 admin123 哈希插入，
 * UserInitializer 检测到 admin 存在自然成 no-op，两者不冲突。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "voglander.sqlite.schema-init.enabled", havingValue = "true", matchIfMissing = true)
public class SqliteSchemaInitializer {

    /** classpath 上的权威建表脚本（构建期由 ../sql/voglander-sqlite.sql 拷入） */
    private static final String SCHEMA_SCRIPT       = "db/voglander-sqlite.sql";

    /**
     * 空库判定的 sentinel 表：建表序列<b>真正最后一张</b>表 {@code tb_cascade_channel}（脚本 L709）。
     * <p>
     * 取最后一张表而非靠前的 {@code tb_user}：只有全表建成才视为已初始化；脚本若半途中断（早建出 tb_user
     * 但末表缺失），sentinel 缺失 → 下次启动重新建表自愈，不会卡死在「sentinel 在、表残缺」的隐性坏库。
     */
    private static final String SENTINEL_TABLE      = "tb_cascade_channel";

    /**
     * 权威脚本应建出的 {@code tb_} 业务表数。脚本共 18 个 {@code CREATE TABLE} = 17 张 {@code tb_} 业务表
     * + 1 张 {@code sequence} 序列表；{@code LIKE 'tb_%'} 只匹配 17 张业务表，故阈值取 <b>17</b>。
     */
    private static final int    EXPECTED_TABLE_COUNT = 17;

    @Autowired(required = false)
    private DataSource          dataSource;

    /**
     * 启动时按需初始化 SQLite schema。非 SQLite 跳过；已建全库跳过。
     * <p>
     * <b>fail-fast</b>：建表脚本执行失败、或建表后校验发现 {@code tb_} 表数不足，均上抛异常阻断启动——
     * 绝不留下「sentinel 在、表残缺」的隐性坏库被后续业务（如 {@code CascadeClientScheduler}）以
     * {@code no such table} 间接暴露。
     */
    @PostConstruct
    public void initSchemaIfEmpty() {
        if (dataSource == null) {
            return;
        }

        // 仅 SQLite 生效；其余库跳过，零副作用
        DbType dbType = DatabaseTypeDetector.detectDbType(dataSource);
        if (dbType != DbType.SQLITE) {
            log.debug("当前数据库类型 {} 非 SQLite，跳过 schema 初始化", dbType);
            return;
        }

        try {
            if (sentinelTableExists()) {
                // sentinel（末表）在 → 正常情况下全表已建。仍做完整性校验作纵深防御：
                // 一旦发现「sentinel 在但 tb_ 表数 < 17」的异常坏库，fail-fast 而非静默放过。
                verifySchemaComplete();
                log.debug("SQLite 已存在 sentinel 表 {} 且表数达标，视为已初始化，跳过建表", SENTINEL_TABLE);
                return;
            }

            log.info("SQLite 缺 sentinel 表 {}（空库或上次建表中断），执行建表脚本 {} ...", SENTINEL_TABLE, SCHEMA_SCRIPT);
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource(SCHEMA_SCRIPT));
            // 脚本含 DROP TABLE IF EXISTS（幂等），单条失败应整体失败以便排查，不静默继续
            populator.setContinueOnError(false);
            try (Connection conn = dataSource.getConnection()) {
                populator.populate(conn);
            }

            // 建表后校验：杜绝「半成品冒充已初始化」——populate 即便未抛异常，也以实际 tb_ 表数为准
            verifySchemaComplete();
            log.info("SQLite schema 初始化完成（建表 {} 张 + 默认 admin/角色/菜单种子）", EXPECTED_TABLE_COUNT);
        } catch (RuntimeException e) {
            // fail-fast：上抛阻断启动，绝不吞异常留隐性坏库
            log.error("SQLite schema 初始化失败，阻断启动：{}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("SQLite schema 初始化失败，阻断启动：{}", e.getMessage(), e);
            throw new IllegalStateException("SQLite schema 初始化失败", e);
        }
    }

    /**
     * 建表后完整性校验：统计 {@code tb_} 开头的业务表数，不足 {@link #EXPECTED_TABLE_COUNT} 即抛异常。
     *
     * @throws IllegalStateException 表数不足（疑似脚本切分失败 / 坏库）
     */
    private void verifySchemaComplete() throws Exception {
        int actual = countUserTables();
        if (actual < EXPECTED_TABLE_COUNT) {
            throw new IllegalStateException(
                "SQLite 建表不完整：期望 " + EXPECTED_TABLE_COUNT + " 张 tb_ 表，实际 " + actual
                    + "，疑似脚本切分失败或坏库");
        }
    }

    /**
     * 统计 {@code tb_} 开头的业务表数量。
     *
     * @return 实际 tb_ 表数
     */
    private int countUserTables() throws Exception {
        String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name LIKE 'tb_%'";
        try (Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * 查询 sqlite_master 判断 sentinel 表是否存在。
     *
     * @return true=已存在（已初始化）
     */
    private boolean sentinelTableExists() throws Exception {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + SENTINEL_TABLE + "'";
        try (Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }
}
